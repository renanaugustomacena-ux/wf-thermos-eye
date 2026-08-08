/**
 * Hashcat Formatter Implementation
 *
 * Builds hashcat 22000 format records for WPA cracking.
 */

#include "hashcat_formatter.h"
#include "config.h"

#include <stdio.h>
#include <string.h>
#include "esp_log.h"

// Helper: Convert bytes to hex string
static size_t bytes_to_hex(const uint8_t *bytes, size_t len, char *hex_out, size_t hex_size) {
    if (hex_size < len * 2 + 1) {
        return 0;
    }

    for (size_t i = 0; i < len; i++) {
        sprintf(hex_out + i * 2, "%02x", bytes[i]);
    }
    hex_out[len * 2] = '\0';
    return len * 2;
}

// Helper: Convert string to hex
static size_t string_to_hex(const char *str, size_t len, char *hex_out, size_t hex_size) {
    return bytes_to_hex((const uint8_t *)str, len, hex_out, hex_size);
}

size_t hashcat_format_pmkid(
    const uint8_t *bssid,
    const uint8_t *client_mac,
    const uint8_t *pmkid,
    const char *ssid,
    uint8_t ssid_len,
    char *output,
    size_t output_size
) {
    if (!bssid || !client_mac || !pmkid || !ssid || !output) {
        return 0;
    }

    // Minimum size check: WPA*02*PMKID(32)*MAC(12)*MAC(12)*SSID(max64)*00
    if (output_size < 128) {
        return 0;
    }

    char pmkid_hex[33];
    char bssid_hex[13];
    char client_hex[13];
    char ssid_hex[65];

    bytes_to_hex(pmkid, 16, pmkid_hex, sizeof(pmkid_hex));
    bytes_to_hex(bssid, 6, bssid_hex, sizeof(bssid_hex));
    bytes_to_hex(client_mac, 6, client_hex, sizeof(client_hex));
    string_to_hex(ssid, ssid_len, ssid_hex, sizeof(ssid_hex));

    // Format: WPA*02*PMKID*MAC_AP*MAC_STA*ESSID_HEX*00
    // 02 = PMKID type
    // 00 = no message pair info for PMKID
    int written = snprintf(output, output_size,
        "WPA*02*%s*%s*%s*%s*00",
        pmkid_hex,
        bssid_hex,
        client_hex,
        ssid_hex
    );

    if (written < 0 || (size_t)written >= output_size) {
        return 0;
    }

    ESP_LOGD(TAG, "Formatted PMKID record: %zu bytes", (size_t)written);
    return (size_t)written;
}

size_t hashcat_format_eapol(
    const uint8_t *bssid,
    const uint8_t *client_mac,
    const uint8_t *anonce,
    const uint8_t *eapol_frame,
    size_t eapol_len,
    const uint8_t *mic,
    const char *ssid,
    uint8_t ssid_len,
    char *output,
    size_t output_size
) {
    if (!bssid || !client_mac || !anonce || !eapol_frame || !mic || !ssid || !output) {
        return 0;
    }

    // Size check: header + all hex fields + separators
    size_t needed = 10 + 32 + 12 + 12 + 64 + 64 + eapol_len * 2 + 3 + 10;
    if (output_size < needed) {
        return 0;
    }

    char mic_hex[33];
    char bssid_hex[13];
    char client_hex[13];
    char ssid_hex[65];
    char anonce_hex[65];

    bytes_to_hex(mic, 16, mic_hex, sizeof(mic_hex));
    bytes_to_hex(bssid, 6, bssid_hex, sizeof(bssid_hex));
    bytes_to_hex(client_mac, 6, client_hex, sizeof(client_hex));
    string_to_hex(ssid, ssid_len, ssid_hex, sizeof(ssid_hex));
    bytes_to_hex(anonce, 32, anonce_hex, sizeof(anonce_hex));

    // Convert EAPOL frame to hex
    char *eapol_hex = malloc(eapol_len * 2 + 1);
    if (!eapol_hex) {
        return 0;
    }
    bytes_to_hex(eapol_frame, eapol_len, eapol_hex, eapol_len * 2 + 1);

    // Format: WPA*01*MIC*MAC_AP*MAC_STA*ESSID_HEX*NONCE_AP*EAPOL_CLIENT*MESSAGE_PAIR
    // 01 = EAPOL type
    // Message pair: 02 = M1+M2
    int written = snprintf(output, output_size,
        "WPA*01*%s*%s*%s*%s*%s*%s*02",
        mic_hex,
        bssid_hex,
        client_hex,
        ssid_hex,
        anonce_hex,
        eapol_hex
    );

    free(eapol_hex);

    if (written < 0 || (size_t)written >= output_size) {
        return 0;
    }

    ESP_LOGD(TAG, "Formatted EAPOL record: %zu bytes", (size_t)written);
    return (size_t)written;
}

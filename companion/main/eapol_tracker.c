/**
 * EAPOL Tracker Implementation
 *
 * Tracks WPA2 4-way handshake state machine to capture M1+M2 pairs.
 * Does NOT inject deauth frames - waits for natural client reconnection.
 */

#include "eapol_tracker.h"
#include "config.h"
#include "scope_filter.h"
#include "ssid_cache.h"
#include "hashcat_formatter.h"
#include "ble_service.h"

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "esp_timer.h"
#include "esp_log.h"

// EAPOL-Key frame offsets (from start of EAPOL header)
#define EAPOL_VERSION_OFFSET    0
#define EAPOL_TYPE_OFFSET       1
#define EAPOL_LENGTH_OFFSET     2
#define EAPOL_KEY_DESC_OFFSET   4
#define EAPOL_KEY_INFO_OFFSET   5
#define EAPOL_KEY_LEN_OFFSET    7
#define EAPOL_REPLAY_OFFSET     9
#define EAPOL_NONCE_OFFSET      17
#define EAPOL_IV_OFFSET         49
#define EAPOL_RSC_OFFSET        65
#define EAPOL_ID_OFFSET         73
#define EAPOL_MIC_OFFSET        81
#define EAPOL_DATA_LEN_OFFSET   97
#define EAPOL_DATA_OFFSET       99

// EAPOL types
#define EAPOL_TYPE_KEY          3

// Key Info flags
#define KEY_INFO_PAIRWISE       0x0008
#define KEY_INFO_INSTALL        0x0040
#define KEY_INFO_ACK            0x0080
#define KEY_INFO_MIC            0x0100
#define KEY_INFO_SECURE         0x0200

// LLC/SNAP header
#define LLC_SNAP_SIZE           8
#define SNAP_TYPE_EAPOL         0x888E

// QoS data frame header size
#define QOS_DATA_HEADER_SIZE    26

// Handshake tracking state
typedef enum {
    STATE_IDLE = 0,
    STATE_GOT_M1,
    STATE_GOT_M2,
    STATE_COMPLETE
} handshake_state_t;

typedef struct {
    uint8_t bssid[6];
    uint8_t client_mac[6];
    uint8_t anonce[32];           // From M1
    uint8_t snonce[32];           // From M2
    uint8_t mic[16];              // From M2
    uint8_t eapol_frame[256];     // M2 EAPOL data (MIC zeroed)
    uint16_t eapol_len;
    handshake_state_t state;
    int64_t last_update;
    bool valid;
} handshake_tracker_t;

static handshake_tracker_t trackers[MAX_TRACKED_HANDSHAKES];
static SemaphoreHandle_t tracker_mutex = NULL;

void eapol_tracker_init(void) {
    if (tracker_mutex == NULL) {
        tracker_mutex = xSemaphoreCreateMutex();
    }
    memset(trackers, 0, sizeof(trackers));
    ESP_LOGI(TAG, "EAPOL tracker initialized with %d slots", MAX_TRACKED_HANDSHAKES);
}

bool is_eapol_frame(const uint8_t *frame, size_t len) {
    if (len < QOS_DATA_HEADER_SIZE + LLC_SNAP_SIZE) {
        return false;
    }

    // Check LLC/SNAP header for EAPOL type (0x888E)
    const uint8_t *llc = frame + QOS_DATA_HEADER_SIZE;
    uint16_t snap_type = (llc[6] << 8) | llc[7];

    return snap_type == SNAP_TYPE_EAPOL;
}

/**
 * Find or create tracker for BSSID+client pair.
 */
static handshake_tracker_t* get_or_create_tracker(const uint8_t *bssid, const uint8_t *client) {
    handshake_tracker_t *oldest = NULL;
    int64_t oldest_time = INT64_MAX;

    for (int i = 0; i < MAX_TRACKED_HANDSHAKES; i++) {
        if (trackers[i].valid &&
            memcmp(trackers[i].bssid, bssid, 6) == 0 &&
            memcmp(trackers[i].client_mac, client, 6) == 0) {
            return &trackers[i];
        }

        if (!trackers[i].valid || trackers[i].last_update < oldest_time) {
            oldest = &trackers[i];
            oldest_time = trackers[i].valid ? trackers[i].last_update : 0;
        }
    }

    // Create new tracker in oldest slot
    if (oldest) {
        memset(oldest, 0, sizeof(handshake_tracker_t));
        memcpy(oldest->bssid, bssid, 6);
        memcpy(oldest->client_mac, client, 6);
        oldest->valid = true;
        oldest->state = STATE_IDLE;
        oldest->last_update = esp_timer_get_time();
    }

    return oldest;
}

/**
 * Emit completed handshake capture.
 */
static void emit_handshake_capture(handshake_tracker_t *tracker) {
    char ssid[33];
    uint8_t ssid_len;
    if (!ssid_cache_get(tracker->bssid, ssid, &ssid_len)) {
        ssid[0] = '\0';
        ssid_len = 0;
        ESP_LOGW(TAG, "No SSID cached for captured handshake");
    }

    ESP_LOGI(TAG, "Complete handshake captured for %s (%02X:%02X:%02X:%02X:%02X:%02X)",
            ssid,
            tracker->bssid[0], tracker->bssid[1], tracker->bssid[2],
            tracker->bssid[3], tracker->bssid[4], tracker->bssid[5]);

    char hashcat_record[512];
    size_t record_len = hashcat_format_eapol(
        tracker->bssid,
        tracker->client_mac,
        tracker->anonce,
        tracker->eapol_frame,
        tracker->eapol_len,
        tracker->mic,
        ssid, ssid_len,
        hashcat_record, sizeof(hashcat_record)
    );

    if (record_len > 0) {
        ble_service_send_capture(
            CAPTURE_KIND_EAPOL,
            tracker->bssid,
            ssid, ssid_len,
            (const uint8_t *)hashcat_record, record_len
        );
    }

    // Reset tracker for next handshake
    tracker->state = STATE_IDLE;
}

void eapol_process_frame(const uint8_t *frame, size_t len, const uint8_t *bssid) {
    if (!scope_filter_is_authorized(bssid)) {
        return;
    }

    if (len < QOS_DATA_HEADER_SIZE + LLC_SNAP_SIZE + EAPOL_DATA_OFFSET) {
        return;
    }

    // Extract client MAC from frame header
    // To-DS: Addr2 = Source (client), Addr1 = BSSID
    // From-DS: Addr1 = Dest (client), Addr2 = BSSID
    uint8_t client_mac[6];
    uint8_t frame_ctrl_flags = frame[1];
    bool to_ds = (frame_ctrl_flags & 0x01) != 0;
    bool from_ds = (frame_ctrl_flags & 0x02) != 0;

    if (to_ds && !from_ds) {
        // Client to AP
        memcpy(client_mac, frame + 10, 6);
    } else if (!to_ds && from_ds) {
        // AP to Client
        memcpy(client_mac, frame + 4, 6);
    } else {
        return;  // IBSS or WDS, ignore
    }

    // Parse EAPOL header
    const uint8_t *eapol = frame + QOS_DATA_HEADER_SIZE + LLC_SNAP_SIZE;
    size_t eapol_len = len - QOS_DATA_HEADER_SIZE - LLC_SNAP_SIZE;

    uint8_t eapol_type = eapol[EAPOL_TYPE_OFFSET];
    if (eapol_type != EAPOL_TYPE_KEY) {
        return;  // Not EAPOL-Key
    }

    // Parse Key Info
    uint16_t key_info = (eapol[EAPOL_KEY_INFO_OFFSET] << 8) | eapol[EAPOL_KEY_INFO_OFFSET + 1];

    bool pairwise = (key_info & KEY_INFO_PAIRWISE) != 0;
    bool install = (key_info & KEY_INFO_INSTALL) != 0;
    bool ack = (key_info & KEY_INFO_ACK) != 0;
    bool mic = (key_info & KEY_INFO_MIC) != 0;

    if (!pairwise) {
        return;  // Group key, ignore
    }

    // Extract nonce
    const uint8_t *nonce = eapol + EAPOL_NONCE_OFFSET;

    xSemaphoreTake(tracker_mutex, portMAX_DELAY);

    handshake_tracker_t *tracker = get_or_create_tracker(bssid, client_mac);
    if (!tracker) {
        xSemaphoreGive(tracker_mutex);
        return;
    }

    // Determine message number and update state
    if (!mic && ack) {
        // M1: AP -> Client, no MIC, has ACK
        ESP_LOGD(TAG, "M1 detected for %02X:%02X:%02X:%02X:%02X:%02X",
                bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);

        memcpy(tracker->anonce, nonce, 32);
        tracker->state = STATE_GOT_M1;
        tracker->last_update = esp_timer_get_time();
    }
    else if (mic && !install && !ack && tracker->state == STATE_GOT_M1) {
        // M2: Client -> AP, has MIC, no Install, no ACK
        ESP_LOGD(TAG, "M2 detected for %02X:%02X:%02X:%02X:%02X:%02X",
                bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);

        memcpy(tracker->snonce, nonce, 32);
        memcpy(tracker->mic, eapol + EAPOL_MIC_OFFSET, 16);

        // Store EAPOL frame with MIC zeroed (hashcat requirement)
        tracker->eapol_len = (eapol_len < sizeof(tracker->eapol_frame)) ?
                            eapol_len : sizeof(tracker->eapol_frame);
        memcpy(tracker->eapol_frame, eapol, tracker->eapol_len);
        memset(tracker->eapol_frame + EAPOL_MIC_OFFSET, 0, 16);

        tracker->state = STATE_GOT_M2;
        tracker->last_update = esp_timer_get_time();

        // We have M1+M2 - complete handshake!
        emit_handshake_capture(tracker);
    }

    xSemaphoreGive(tracker_mutex);
}

void eapol_tracker_cleanup(void) {
    int64_t now = esp_timer_get_time();
    int cleaned = 0;

    xSemaphoreTake(tracker_mutex, portMAX_DELAY);

    for (int i = 0; i < MAX_TRACKED_HANDSHAKES; i++) {
        if (trackers[i].valid &&
            (now - trackers[i].last_update) > HANDSHAKE_TIMEOUT_US) {
            trackers[i].valid = false;
            trackers[i].state = STATE_IDLE;
            cleaned++;
        }
    }

    xSemaphoreGive(tracker_mutex);

    if (cleaned > 0) {
        ESP_LOGD(TAG, "Cleaned %d stale handshake trackers", cleaned);
    }
}

void eapol_tracker_reset(void) {
    xSemaphoreTake(tracker_mutex, portMAX_DELAY);
    memset(trackers, 0, sizeof(trackers));
    xSemaphoreGive(tracker_mutex);
    ESP_LOGI(TAG, "EAPOL tracker reset");
}

int eapol_tracker_get_count(void) {
    int count = 0;
    xSemaphoreTake(tracker_mutex, portMAX_DELAY);
    for (int i = 0; i < MAX_TRACKED_HANDSHAKES; i++) {
        if (trackers[i].valid && trackers[i].state != STATE_IDLE) {
            count++;
        }
    }
    xSemaphoreGive(tracker_mutex);
    return count;
}

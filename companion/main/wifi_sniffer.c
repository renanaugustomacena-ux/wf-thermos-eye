/**
 * WiFi Sniffer Implementation
 *
 * Promiscuous mode frame capture with channel hopping.
 */

#include "wifi_sniffer.h"
#include "config.h"
#include "scope_filter.h"
#include "pmkid_extractor.h"
#include "eapol_tracker.h"

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"

// Channel lists
static const uint8_t channels_2_4_ghz[] = CHANNELS_2_4_GHZ;
static const uint8_t channels_5_ghz[] = CHANNELS_5_GHZ;

// State
static bool sniffer_running = false;
static int current_channel = 0;
static TaskHandle_t channel_hop_task_handle = NULL;
static TaskHandle_t cleanup_task_handle = NULL;

// Statistics
static wifi_sniffer_stats_t stats;

// Frame type/subtype masks
#define FC_TYPE_MASK      0x0C
#define FC_SUBTYPE_MASK   0xF0
#define FC_TYPE_MGMT      0x00
#define FC_TYPE_DATA      0x08

// Management subtypes
#define FC_SUBTYPE_ASSOC_RESP  0x10
#define FC_SUBTYPE_PROBE_RESP  0x50
#define FC_SUBTYPE_BEACON      0x80

/**
 * Extract BSSID from 802.11 frame header.
 */
static void extract_bssid(const uint8_t *frame, uint8_t *bssid) {
    uint8_t frame_ctrl_flags = frame[1];
    bool to_ds = (frame_ctrl_flags & 0x01) != 0;
    bool from_ds = (frame_ctrl_flags & 0x02) != 0;

    if (!to_ds && !from_ds) {
        // IBSS or Management: BSSID is Address 3
        memcpy(bssid, frame + 16, 6);
    } else if (to_ds && !from_ds) {
        // To-DS (client to AP): BSSID is Address 1
        memcpy(bssid, frame + 4, 6);
    } else if (!to_ds && from_ds) {
        // From-DS (AP to client): BSSID is Address 2
        memcpy(bssid, frame + 10, 6);
    } else {
        // WDS: BSSID is Address 1 (receiver)
        memcpy(bssid, frame + 4, 6);
    }
}

/**
 * Promiscuous mode callback.
 */
static void wifi_sniffer_cb(void *buf, wifi_promiscuous_pkt_type_t type) {
    if (type != WIFI_PKT_MGMT && type != WIFI_PKT_DATA) {
        return;
    }

    wifi_promiscuous_pkt_t *pkt = (wifi_promiscuous_pkt_t *)buf;
    const uint8_t *frame = pkt->payload;
    size_t len = pkt->rx_ctrl.sig_len;

    if (len < 24) {
        return;  // Too short for valid 802.11 frame
    }

    // Extract frame type and subtype
    uint8_t frame_type = (frame[0] & FC_TYPE_MASK);
    uint8_t frame_subtype = (frame[0] & FC_SUBTYPE_MASK);

    // Extract BSSID
    uint8_t bssid[6];
    extract_bssid(frame, bssid);

    if (frame_type == FC_TYPE_MGMT) {
        stats.management_frames++;

        // Process management frames
        switch (frame_subtype) {
            case FC_SUBTYPE_BEACON:
                pmkid_process_beacon(frame, len);
                break;

            case FC_SUBTYPE_PROBE_RESP:
                pmkid_process_probe_response(frame, len);
                break;

            case FC_SUBTYPE_ASSOC_RESP:
                pmkid_process_assoc_response(frame, len);
                break;

            default:
                break;
        }
    }
    else if (frame_type == FC_TYPE_DATA) {
        stats.data_frames++;

        // Check for EAPOL frames
        if (is_eapol_frame(frame, len)) {
            stats.eapol_frames++;
            eapol_process_frame(frame, len, bssid);
        }
    }
}

/**
 * Channel hopping task.
 */
static void channel_hop_task(void *arg) {
    int total_channels = CHANNELS_2_4_GHZ_COUNT + CHANNELS_5_GHZ_COUNT;
    int channel_idx = 0;

    ESP_LOGI(TAG, "Channel hopping started (%d channels, %dms interval)",
            total_channels, CHANNEL_HOP_INTERVAL_MS);

    while (sniffer_running) {
        uint8_t channel;

        if (channel_idx < CHANNELS_2_4_GHZ_COUNT) {
            channel = channels_2_4_ghz[channel_idx];
        } else {
            channel = channels_5_ghz[channel_idx - CHANNELS_2_4_GHZ_COUNT];
        }

        esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
        current_channel = channel;

        channel_idx = (channel_idx + 1) % total_channels;
        vTaskDelay(pdMS_TO_TICKS(CHANNEL_HOP_INTERVAL_MS));
    }

    ESP_LOGI(TAG, "Channel hopping stopped");
    vTaskDelete(NULL);
}

/**
 * Cleanup task - periodically clean stale handshake trackers.
 */
static void cleanup_task(void *arg) {
    while (sniffer_running) {
        vTaskDelay(pdMS_TO_TICKS(10000));  // Every 10 seconds
        eapol_tracker_cleanup();
    }
    vTaskDelete(NULL);
}

void wifi_sniffer_init(void) {
    if (sniffer_running) {
        ESP_LOGW(TAG, "Sniffer already running");
        return;
    }

    // Initialize WiFi
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_NULL));
    ESP_ERROR_CHECK(esp_wifi_start());

    // Configure promiscuous filter
    wifi_promiscuous_filter_t filter = {
        .filter_mask = WIFI_PROMIS_FILTER_MASK_MGMT | WIFI_PROMIS_FILTER_MASK_DATA
    };
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous_filter(&filter));

    // Set promiscuous callback
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous_rx_cb(wifi_sniffer_cb));

    // Enable promiscuous mode
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));

    sniffer_running = true;
    memset(&stats, 0, sizeof(stats));

    // Start channel hopping task
    xTaskCreate(channel_hop_task, "channel_hop", 2048, NULL, 5, &channel_hop_task_handle);

    // Start cleanup task
    xTaskCreate(cleanup_task, "cleanup", 2048, NULL, 3, &cleanup_task_handle);

    ESP_LOGI(TAG, "WiFi sniffer initialized and running");
}

void wifi_sniffer_stop(void) {
    if (!sniffer_running) {
        return;
    }

    sniffer_running = false;

    // Wait for tasks to stop
    vTaskDelay(pdMS_TO_TICKS(CHANNEL_HOP_INTERVAL_MS * 2));

    ESP_ERROR_CHECK(esp_wifi_set_promiscuous(false));
    ESP_ERROR_CHECK(esp_wifi_stop());
    ESP_ERROR_CHECK(esp_wifi_deinit());

    ESP_LOGI(TAG, "WiFi sniffer stopped");
}

bool wifi_sniffer_is_running(void) {
    return sniffer_running;
}

int wifi_sniffer_get_channel(void) {
    return current_channel;
}

void wifi_sniffer_get_stats(wifi_sniffer_stats_t *out_stats) {
    if (out_stats) {
        memcpy(out_stats, &stats, sizeof(wifi_sniffer_stats_t));
    }
}

void wifi_sniffer_reset_stats(void) {
    memset(&stats, 0, sizeof(stats));
}

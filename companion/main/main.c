/**
 * WF-Thermos-Eye ESP32 Sniffer - Main Entry Point
 *
 * WiFi password capture companion device for wf-thermos-eye Android app.
 * Captures PMKID and EAPOL handshakes for offline WPA cracking.
 *
 * IMPORTANT: This device does NOT inject deauth frames.
 * It passively monitors for natural client reconnections.
 */

#include "config.h"
#include "scope_filter.h"
#include "ssid_cache.h"
#include "pmkid_extractor.h"
#include "eapol_tracker.h"
#include "wifi_sniffer.h"
#include "ble_service.h"

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"
#include "esp_event.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_timer.h"

// Status update task
static void status_task(void *arg) {
    uint32_t start_time = esp_timer_get_time() / 1000000;

    while (1) {
        vTaskDelay(pdMS_TO_TICKS(STATUS_HEARTBEAT_INTERVAL_MS));

        uint32_t uptime = (esp_timer_get_time() / 1000000) - start_time;
        uint8_t capture_count = ble_service_get_capture_count() & 0xFF;

        ble_service_send_status(capture_count, uptime);

        // Log stats periodically
        wifi_sniffer_stats_t stats;
        wifi_sniffer_get_stats(&stats);

        ESP_LOGI(TAG, "Status: uptime=%lus, captures=%u, mgmt=%lu, data=%lu, eapol=%lu",
                uptime, capture_count,
                stats.management_frames, stats.data_frames, stats.eapol_frames);
    }
}

void app_main(void) {
    ESP_LOGI(TAG, "=================================");
    ESP_LOGI(TAG, "WF-Thermos-Eye ESP32 Sniffer");
    ESP_LOGI(TAG, "Protocol Version: %d", PROTOCOL_VERSION);
    ESP_LOGI(TAG, "=================================");

    // Initialize NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    // Initialize event loop
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    // Initialize components
    ESP_LOGI(TAG, "Initializing components...");

    scope_filter_init();
    ssid_cache_init();
    pmkid_extractor_init();
    eapol_tracker_init();

    // Initialize BLE service (starts advertising)
    ESP_LOGI(TAG, "Starting BLE service...");
    ble_service_init();

    // Initialize WiFi sniffer (starts promiscuous mode)
    ESP_LOGI(TAG, "Starting WiFi sniffer...");
    wifi_sniffer_init();

    // Start status update task
    xTaskCreate(status_task, "status", 2048, NULL, 3, NULL);

    ESP_LOGI(TAG, "=================================");
    ESP_LOGI(TAG, "Sniffer ready!");
    ESP_LOGI(TAG, "Device name: %s", DEVICE_NAME);
    ESP_LOGI(TAG, "Waiting for Android connection...");
    ESP_LOGI(TAG, "=================================");

    // Main loop - just keep alive
    while (1) {
        vTaskDelay(pdMS_TO_TICKS(1000));

        // Monitor connection status
        if (ble_service_is_connected()) {
            int authorized_count = scope_filter_get_count();
            if (authorized_count == 0) {
                ESP_LOGW(TAG, "Connected but no authorized BSSIDs configured");
            }
        }
    }
}

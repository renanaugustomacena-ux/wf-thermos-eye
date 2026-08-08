/**
 * SSID Cache Implementation
 */

#include "ssid_cache.h"
#include "config.h"

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "esp_log.h"

typedef struct {
    uint8_t bssid[6];
    char ssid[33];
    uint8_t ssid_len;
    bool valid;
    uint32_t last_seen;
} ssid_cache_entry_t;

static ssid_cache_entry_t cache[SSID_CACHE_SIZE];
static SemaphoreHandle_t cache_mutex = NULL;

void ssid_cache_init(void) {
    if (cache_mutex == NULL) {
        cache_mutex = xSemaphoreCreateMutex();
    }
    memset(cache, 0, sizeof(cache));
    ESP_LOGI(TAG, "SSID cache initialized with %d slots", SSID_CACHE_SIZE);
}

void ssid_cache_put(const uint8_t *bssid, const char *ssid, uint8_t ssid_len) {
    if (bssid == NULL || ssid == NULL || ssid_len == 0 || ssid_len > 32) {
        return;
    }

    xSemaphoreTake(cache_mutex, portMAX_DELAY);

    // Check if entry already exists
    int existing_idx = -1;
    int oldest_idx = 0;
    uint32_t oldest_time = UINT32_MAX;
    uint32_t now = xTaskGetTickCount();

    for (int i = 0; i < SSID_CACHE_SIZE; i++) {
        if (cache[i].valid && memcmp(cache[i].bssid, bssid, 6) == 0) {
            existing_idx = i;
            break;
        }
        if (!cache[i].valid || cache[i].last_seen < oldest_time) {
            oldest_time = cache[i].last_seen;
            oldest_idx = i;
        }
    }

    int target_idx = (existing_idx >= 0) ? existing_idx : oldest_idx;

    memcpy(cache[target_idx].bssid, bssid, 6);
    memcpy(cache[target_idx].ssid, ssid, ssid_len);
    cache[target_idx].ssid[ssid_len] = '\0';
    cache[target_idx].ssid_len = ssid_len;
    cache[target_idx].valid = true;
    cache[target_idx].last_seen = now;

    xSemaphoreGive(cache_mutex);

    ESP_LOGD(TAG, "Cached SSID '%s' for BSSID %02X:%02X:%02X:%02X:%02X:%02X",
            cache[target_idx].ssid,
            bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);
}

bool ssid_cache_get(const uint8_t *bssid, char *ssid_out, uint8_t *ssid_len_out) {
    if (bssid == NULL || ssid_out == NULL || ssid_len_out == NULL) {
        return false;
    }

    xSemaphoreTake(cache_mutex, portMAX_DELAY);

    bool found = false;
    for (int i = 0; i < SSID_CACHE_SIZE; i++) {
        if (cache[i].valid && memcmp(cache[i].bssid, bssid, 6) == 0) {
            memcpy(ssid_out, cache[i].ssid, cache[i].ssid_len + 1);
            *ssid_len_out = cache[i].ssid_len;
            found = true;
            break;
        }
    }

    xSemaphoreGive(cache_mutex);
    return found;
}

void ssid_cache_clear(void) {
    xSemaphoreTake(cache_mutex, portMAX_DELAY);
    memset(cache, 0, sizeof(cache));
    xSemaphoreGive(cache_mutex);
    ESP_LOGI(TAG, "SSID cache cleared");
}

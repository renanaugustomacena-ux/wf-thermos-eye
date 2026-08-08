/**
 * Scope Filter Implementation
 *
 * First gate in scope enforcement. Filters at capture source.
 */

#include "scope_filter.h"
#include "config.h"

#include <string.h>
#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "esp_log.h"

static uint8_t authorized_bssids[MAX_AUTHORIZED_BSSIDS][6];
static int authorized_count = 0;
static SemaphoreHandle_t scope_mutex = NULL;

void scope_filter_init(void) {
    if (scope_mutex == NULL) {
        scope_mutex = xSemaphoreCreateMutex();
    }
    authorized_count = 0;
    ESP_LOGI(TAG, "Scope filter initialized");
}

bool scope_filter_is_authorized(const uint8_t *bssid) {
    if (bssid == NULL) {
        return false;
    }

    xSemaphoreTake(scope_mutex, portMAX_DELAY);

    bool found = false;
    for (int i = 0; i < authorized_count; i++) {
        if (memcmp(bssid, authorized_bssids[i], 6) == 0) {
            found = true;
            break;
        }
    }

    xSemaphoreGive(scope_mutex);
    return found;
}

void scope_filter_set_authorized(const char *csv_list) {
    if (csv_list == NULL) {
        return;
    }

    xSemaphoreTake(scope_mutex, portMAX_DELAY);

    authorized_count = 0;
    const char *ptr = csv_list;

    while (*ptr && authorized_count < MAX_AUTHORIZED_BSSIDS) {
        // Skip whitespace and commas
        while (*ptr == ' ' || *ptr == ',' || *ptr == '\n' || *ptr == '\r') {
            ptr++;
        }
        if (!*ptr) {
            break;
        }

        // Parse MAC address: XX:XX:XX:XX:XX:XX
        uint8_t mac[6];
        int parsed = sscanf(ptr, "%02hhx:%02hhx:%02hhx:%02hhx:%02hhx:%02hhx",
                           &mac[0], &mac[1], &mac[2], &mac[3], &mac[4], &mac[5]);

        if (parsed == 6) {
            memcpy(authorized_bssids[authorized_count], mac, 6);
            authorized_count++;

            ESP_LOGD(TAG, "Added authorized BSSID: %02X:%02X:%02X:%02X:%02X:%02X",
                    mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        }

        // Skip to next comma or end
        while (*ptr && *ptr != ',') {
            ptr++;
        }
    }

    xSemaphoreGive(scope_mutex);

    ESP_LOGI(TAG, "Updated authorized BSSIDs: %d entries", authorized_count);
}

void scope_filter_clear(void) {
    xSemaphoreTake(scope_mutex, portMAX_DELAY);
    authorized_count = 0;
    xSemaphoreGive(scope_mutex);

    ESP_LOGI(TAG, "Cleared all authorized BSSIDs");
}

int scope_filter_get_count(void) {
    xSemaphoreTake(scope_mutex, portMAX_DELAY);
    int count = authorized_count;
    xSemaphoreGive(scope_mutex);
    return count;
}

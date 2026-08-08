/**
 * BLE Service Implementation
 *
 * NimBLE GATT server for Android communication.
 */

#include "ble_service.h"
#include "config.h"
#include "scope_filter.h"

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/util/util.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"
#include "esp_log.h"

// Connection state
static uint16_t conn_handle = BLE_HS_CONN_HANDLE_NONE;
static bool ble_initialized = false;
static uint32_t capture_count = 0;

// Characteristic value handles
static uint16_t capture_notify_handle;
static uint16_t config_handle;
static uint16_t status_handle;

// Status data
static uint8_t status_data[5];

// UUIDs (128-bit)
static const ble_uuid128_t service_uuid = BLE_UUID128_INIT(
    0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
    0x0e, 0x4d, 0x4e, 0x1c, 0x01, 0x80, 0x9c, 0x7f
);

static const ble_uuid128_t capture_uuid = BLE_UUID128_INIT(
    0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
    0x0e, 0x4d, 0x4e, 0x1c, 0x02, 0x80, 0x9c, 0x7f
);

static const ble_uuid128_t config_uuid = BLE_UUID128_INIT(
    0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
    0x0e, 0x4d, 0x4e, 0x1c, 0x03, 0x80, 0x9c, 0x7f
);

static const ble_uuid128_t status_uuid = BLE_UUID128_INIT(
    0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
    0x0e, 0x4d, 0x4e, 0x1c, 0x04, 0x80, 0x9c, 0x7f
);

/**
 * Config characteristic write handler.
 * Receives authorized BSSID list from Android.
 */
static int config_write_handler(uint16_t conn_handle, uint16_t attr_handle,
                                struct ble_gatt_access_ctxt *ctxt, void *arg) {
    char config_str[512];
    uint16_t len = OS_MBUF_PKTLEN(ctxt->om);

    if (len >= sizeof(config_str)) {
        ESP_LOGW(TAG, "Config data too large: %d bytes", len);
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    }

    int rc = ble_hs_mbuf_to_flat(ctxt->om, config_str, len, NULL);
    if (rc != 0) {
        return BLE_ATT_ERR_UNLIKELY;
    }
    config_str[len] = '\0';

    ESP_LOGI(TAG, "Received config: %s", config_str);
    scope_filter_set_authorized(config_str);

    return 0;
}

/**
 * Status characteristic read handler.
 */
static int status_read_handler(uint16_t conn_handle, uint16_t attr_handle,
                               struct ble_gatt_access_ctxt *ctxt, void *arg) {
    int rc = os_mbuf_append(ctxt->om, status_data, sizeof(status_data));
    return rc == 0 ? 0 : BLE_ATT_ERR_INSUFFICIENT_RES;
}

/**
 * GATT service definition.
 */
static const struct ble_gatt_svc_def gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &service_uuid.u,
        .characteristics = (struct ble_gatt_chr_def[]) {
            {
                // Capture characteristic (notify only)
                .uuid = &capture_uuid.u,
                .access_cb = NULL,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &capture_notify_handle,
            },
            {
                // Config characteristic (write)
                .uuid = &config_uuid.u,
                .access_cb = config_write_handler,
                .flags = BLE_GATT_CHR_F_WRITE,
                .val_handle = &config_handle,
            },
            {
                // Status characteristic (read + notify)
                .uuid = &status_uuid.u,
                .access_cb = status_read_handler,
                .flags = BLE_GATT_CHR_F_READ | BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &status_handle,
            },
            { 0 }  // Terminator
        },
    },
    { 0 }  // Terminator
};

/**
 * GAP event handler.
 */
static int gap_event_handler(struct ble_gap_event *event, void *arg) {
    switch (event->type) {
        case BLE_GAP_EVENT_CONNECT:
            ESP_LOGI(TAG, "Connection %s (handle=%d)",
                    event->connect.status == 0 ? "established" : "failed",
                    event->connect.conn_handle);
            if (event->connect.status == 0) {
                conn_handle = event->connect.conn_handle;
            }
            break;

        case BLE_GAP_EVENT_DISCONNECT:
            ESP_LOGI(TAG, "Disconnected (reason=%d)", event->disconnect.reason);
            conn_handle = BLE_HS_CONN_HANDLE_NONE;
            // Restart advertising
            ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER,
                             &(struct ble_gap_adv_params){
                                 .conn_mode = BLE_GAP_CONN_MODE_UND,
                                 .disc_mode = BLE_GAP_DISC_MODE_GEN,
                             }, gap_event_handler, NULL);
            break;

        case BLE_GAP_EVENT_ADV_COMPLETE:
            ESP_LOGD(TAG, "Advertising complete");
            break;

        case BLE_GAP_EVENT_MTU:
            ESP_LOGI(TAG, "MTU updated: %d", event->mtu.value);
            break;

        default:
            break;
    }
    return 0;
}

/**
 * BLE host task.
 */
static void ble_host_task(void *param) {
    ESP_LOGI(TAG, "BLE host task started");
    nimble_port_run();
    nimble_port_freertos_deinit();
}

/**
 * BLE sync callback.
 */
static void ble_on_sync(void) {
    int rc;

    // Generate public address
    rc = ble_hs_util_ensure_addr(0);
    if (rc != 0) {
        ESP_LOGE(TAG, "Failed to ensure address");
        return;
    }

    // Start advertising
    struct ble_gap_adv_params adv_params = {
        .conn_mode = BLE_GAP_CONN_MODE_UND,
        .disc_mode = BLE_GAP_DISC_MODE_GEN,
        .itvl_min = BLE_GAP_ADV_ITVL_MS(100),
        .itvl_max = BLE_GAP_ADV_ITVL_MS(150),
    };

    rc = ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER,
                          &adv_params, gap_event_handler, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "Failed to start advertising: %d", rc);
        return;
    }

    ESP_LOGI(TAG, "BLE advertising started");
}

/**
 * BLE reset callback.
 */
static void ble_on_reset(int reason) {
    ESP_LOGW(TAG, "BLE reset (reason=%d)", reason);
}

void ble_service_init(void) {
    if (ble_initialized) {
        ESP_LOGW(TAG, "BLE service already initialized");
        return;
    }

    int rc;

    // Initialize NimBLE
    rc = nimble_port_init();
    if (rc != ESP_OK) {
        ESP_LOGE(TAG, "Failed to init NimBLE: %d", rc);
        return;
    }

    // Configure host
    ble_hs_cfg.reset_cb = ble_on_reset;
    ble_hs_cfg.sync_cb = ble_on_sync;
    ble_hs_cfg.gatts_register_cb = NULL;
    ble_hs_cfg.store_status_cb = NULL;

    // Initialize GATT services
    ble_svc_gap_init();
    ble_svc_gatt_init();

    rc = ble_gatts_count_cfg(gatt_svcs);
    if (rc != 0) {
        ESP_LOGE(TAG, "Failed to count GATT config: %d", rc);
        return;
    }

    rc = ble_gatts_add_svcs(gatt_svcs);
    if (rc != 0) {
        ESP_LOGE(TAG, "Failed to add GATT services: %d", rc);
        return;
    }

    // Set device name
    rc = ble_svc_gap_device_name_set(DEVICE_NAME);
    if (rc != 0) {
        ESP_LOGW(TAG, "Failed to set device name: %d", rc);
    }

    // Start host task
    nimble_port_freertos_init(ble_host_task);

    ble_initialized = true;
    ESP_LOGI(TAG, "BLE service initialized");
}

void ble_service_stop(void) {
    if (!ble_initialized) {
        return;
    }

    int rc = nimble_port_stop();
    if (rc == 0) {
        nimble_port_deinit();
    }

    ble_initialized = false;
    conn_handle = BLE_HS_CONN_HANDLE_NONE;
    ESP_LOGI(TAG, "BLE service stopped");
}

bool ble_service_is_connected(void) {
    return conn_handle != BLE_HS_CONN_HANDLE_NONE;
}

bool ble_service_send_capture(
    uint8_t kind,
    const uint8_t *bssid,
    const char *ssid,
    uint8_t ssid_len,
    const uint8_t *payload,
    size_t payload_len
) {
    if (conn_handle == BLE_HS_CONN_HANDLE_NONE) {
        ESP_LOGD(TAG, "No connection, capture not sent");
        return false;
    }

    // Build capture frame
    size_t frame_len = 1 + 1 + 6 + 1 + ssid_len + 2 + payload_len;
    if (frame_len > MAX_CAPTURE_FRAME_SIZE) {
        ESP_LOGW(TAG, "Capture frame too large: %d bytes", frame_len);
        return false;
    }

    uint8_t frame[MAX_CAPTURE_FRAME_SIZE];
    uint8_t *ptr = frame;

    // Version
    *ptr++ = PROTOCOL_VERSION;

    // Kind
    *ptr++ = kind;

    // BSSID (6 bytes)
    memcpy(ptr, bssid, 6);
    ptr += 6;

    // SSID length + SSID
    *ptr++ = ssid_len;
    memcpy(ptr, ssid, ssid_len);
    ptr += ssid_len;

    // Payload length (little-endian)
    *ptr++ = payload_len & 0xFF;
    *ptr++ = (payload_len >> 8) & 0xFF;

    // Payload
    memcpy(ptr, payload, payload_len);
    ptr += payload_len;

    // Send notification
    struct os_mbuf *om = ble_hs_mbuf_from_flat(frame, frame_len);
    if (om == NULL) {
        ESP_LOGE(TAG, "Failed to allocate mbuf");
        return false;
    }

    int rc = ble_gattc_notify_custom(conn_handle, capture_notify_handle, om);
    if (rc != 0) {
        ESP_LOGE(TAG, "Failed to send notification: %d", rc);
        return false;
    }

    capture_count++;
    ESP_LOGI(TAG, "Capture sent (%s, %d bytes)",
            kind == CAPTURE_KIND_PMKID ? "PMKID" : "EAPOL", frame_len);
    return true;
}

void ble_service_send_status(uint8_t capture_count, uint32_t uptime_sec) {
    status_data[0] = capture_count;
    status_data[1] = uptime_sec & 0xFF;
    status_data[2] = (uptime_sec >> 8) & 0xFF;
    status_data[3] = (uptime_sec >> 16) & 0xFF;
    status_data[4] = (uptime_sec >> 24) & 0xFF;

    if (conn_handle != BLE_HS_CONN_HANDLE_NONE) {
        struct os_mbuf *om = ble_hs_mbuf_from_flat(status_data, sizeof(status_data));
        if (om) {
            ble_gattc_notify_custom(conn_handle, status_handle, om);
        }
    }
}

uint32_t ble_service_get_capture_count(void) {
    return capture_count;
}

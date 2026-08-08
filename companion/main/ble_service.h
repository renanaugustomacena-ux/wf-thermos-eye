/**
 * BLE Service - GATT Server for Android Communication
 *
 * Implements BLE GATT service for transmitting captures to the Android app.
 */

#ifndef BLE_SERVICE_H
#define BLE_SERVICE_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

/**
 * Initialize the BLE service.
 * Sets up NimBLE stack and starts advertising.
 */
void ble_service_init(void);

/**
 * Stop the BLE service.
 */
void ble_service_stop(void);

/**
 * Check if a client is connected.
 */
bool ble_service_is_connected(void);

/**
 * Send a capture frame to the connected client.
 *
 * @param kind Capture kind (CAPTURE_KIND_PMKID or CAPTURE_KIND_EAPOL)
 * @param bssid AP MAC address (6 bytes)
 * @param ssid Network SSID
 * @param ssid_len SSID length
 * @param payload Hashcat record data
 * @param payload_len Payload length
 * @return true if sent successfully, false otherwise
 */
bool ble_service_send_capture(
    uint8_t kind,
    const uint8_t *bssid,
    const char *ssid,
    uint8_t ssid_len,
    const uint8_t *payload,
    size_t payload_len
);

/**
 * Send status update to connected client.
 *
 * @param capture_count Number of captures buffered
 * @param uptime_sec Uptime in seconds
 */
void ble_service_send_status(uint8_t capture_count, uint32_t uptime_sec);

/**
 * Get count of captures sent since last reset.
 */
uint32_t ble_service_get_capture_count(void);

#endif // BLE_SERVICE_H

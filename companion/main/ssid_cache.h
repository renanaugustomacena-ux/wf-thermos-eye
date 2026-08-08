/**
 * SSID Cache - BSSID to SSID Mapping
 *
 * Maintains a cache of BSSID -> SSID mappings learned from beacons.
 * Used to associate SSIDs with captured handshakes.
 */

#ifndef SSID_CACHE_H
#define SSID_CACHE_H

#include <stdint.h>
#include <stdbool.h>

/**
 * Initialize the SSID cache.
 */
void ssid_cache_init(void);

/**
 * Store or update a BSSID -> SSID mapping.
 *
 * @param bssid 6-byte MAC address
 * @param ssid SSID string (null-terminated, max 32 chars)
 * @param ssid_len Length of SSID
 */
void ssid_cache_put(const uint8_t *bssid, const char *ssid, uint8_t ssid_len);

/**
 * Retrieve SSID for a given BSSID.
 *
 * @param bssid 6-byte MAC address
 * @param ssid_out Buffer to store SSID (at least 33 bytes)
 * @param ssid_len_out Pointer to store SSID length
 * @return true if found, false otherwise
 */
bool ssid_cache_get(const uint8_t *bssid, char *ssid_out, uint8_t *ssid_len_out);

/**
 * Clear the SSID cache.
 */
void ssid_cache_clear(void);

#endif // SSID_CACHE_H

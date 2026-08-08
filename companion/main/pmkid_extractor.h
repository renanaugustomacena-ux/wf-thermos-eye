/**
 * PMKID Extractor - Extract PMKID from 802.11 Frames
 *
 * Parses RSN Information Elements from association responses and beacons
 * to extract PMKID values for clientless WPA cracking.
 */

#ifndef PMKID_EXTRACTOR_H
#define PMKID_EXTRACTOR_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

/**
 * Initialize the PMKID extractor.
 */
void pmkid_extractor_init(void);

/**
 * Process an association response frame for PMKID.
 *
 * @param frame Complete 802.11 frame data
 * @param len Frame length
 */
void pmkid_process_assoc_response(const uint8_t *frame, size_t len);

/**
 * Process a beacon frame for PMKID and SSID caching.
 *
 * @param frame Complete 802.11 frame data
 * @param len Frame length
 */
void pmkid_process_beacon(const uint8_t *frame, size_t len);

/**
 * Process a probe response frame for PMKID.
 *
 * @param frame Complete 802.11 frame data
 * @param len Frame length
 */
void pmkid_process_probe_response(const uint8_t *frame, size_t len);

#endif // PMKID_EXTRACTOR_H

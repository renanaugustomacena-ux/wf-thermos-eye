/**
 * EAPOL Tracker - 4-Way Handshake State Machine
 *
 * Tracks WPA2 4-way handshake sequences to capture M1+M2 pairs
 * for offline password cracking.
 */

#ifndef EAPOL_TRACKER_H
#define EAPOL_TRACKER_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

/**
 * Initialize the EAPOL tracker.
 */
void eapol_tracker_init(void);

/**
 * Process an EAPOL frame.
 *
 * @param frame Complete 802.11 data frame containing EAPOL
 * @param len Frame length
 * @param bssid BSSID extracted from frame header
 */
void eapol_process_frame(const uint8_t *frame, size_t len, const uint8_t *bssid);

/**
 * Check if a frame contains EAPOL data.
 *
 * @param frame 802.11 data frame
 * @param len Frame length
 * @return true if frame contains EAPOL (LLC SNAP type 0x888E)
 */
bool is_eapol_frame(const uint8_t *frame, size_t len);

/**
 * Clean up stale handshake tracking entries.
 * Should be called periodically (e.g., every 10 seconds).
 */
void eapol_tracker_cleanup(void);

/**
 * Reset all tracked handshakes.
 */
void eapol_tracker_reset(void);

/**
 * Get count of currently tracked handshakes.
 */
int eapol_tracker_get_count(void);

#endif // EAPOL_TRACKER_H

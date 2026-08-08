/**
 * Scope Filter - BSSID Authorization
 *
 * Filters captured frames at source. Only authorized BSSIDs pass through.
 * This is the first gate in the 4-gate scope enforcement system.
 */

#ifndef SCOPE_FILTER_H
#define SCOPE_FILTER_H

#include <stdint.h>
#include <stdbool.h>

/**
 * Initialize the scope filter.
 * Must be called before any other scope_filter functions.
 */
void scope_filter_init(void);

/**
 * Check if a BSSID is authorized for capture.
 *
 * @param bssid 6-byte MAC address
 * @return true if BSSID is in the authorized list, false otherwise
 */
bool scope_filter_is_authorized(const uint8_t *bssid);

/**
 * Update the authorized BSSID list from a CSV string.
 * Format: "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66,..."
 *
 * @param csv_list Comma-separated list of MAC addresses
 */
void scope_filter_set_authorized(const char *csv_list);

/**
 * Clear all authorized BSSIDs.
 * After this call, scope_filter_is_authorized returns false for all inputs.
 */
void scope_filter_clear(void);

/**
 * Get the count of currently authorized BSSIDs.
 *
 * @return Number of authorized BSSIDs
 */
int scope_filter_get_count(void);

#endif // SCOPE_FILTER_H

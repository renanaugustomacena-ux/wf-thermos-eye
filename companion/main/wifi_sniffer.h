/**
 * WiFi Sniffer - Promiscuous Mode Frame Capture
 *
 * Captures 802.11 management and data frames in promiscuous mode.
 * Implements channel hopping across 2.4 GHz and 5 GHz bands.
 */

#ifndef WIFI_SNIFFER_H
#define WIFI_SNIFFER_H

#include <stdbool.h>

/**
 * Initialize and start the WiFi sniffer.
 * Enables promiscuous mode and starts channel hopping.
 */
void wifi_sniffer_init(void);

/**
 * Stop the WiFi sniffer.
 * Disables promiscuous mode and stops channel hopping.
 */
void wifi_sniffer_stop(void);

/**
 * Check if sniffer is currently running.
 */
bool wifi_sniffer_is_running(void);

/**
 * Get the current channel being monitored.
 */
int wifi_sniffer_get_channel(void);

/**
 * Get statistics about captured frames.
 */
typedef struct {
    uint32_t management_frames;
    uint32_t data_frames;
    uint32_t eapol_frames;
    uint32_t pmkid_captures;
    uint32_t handshake_captures;
} wifi_sniffer_stats_t;

void wifi_sniffer_get_stats(wifi_sniffer_stats_t *stats);

/**
 * Reset capture statistics.
 */
void wifi_sniffer_reset_stats(void);

#endif // WIFI_SNIFFER_H

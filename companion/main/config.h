/**
 * WF-Thermos-Eye ESP32 Sniffer Configuration
 *
 * Build-time configuration for the WiFi sniffer companion device.
 */

#ifndef CONFIG_H
#define CONFIG_H

// ============================================================================
// BLE Configuration
// ============================================================================

#define DEVICE_NAME "WF-Sniffer"

// Service and characteristic UUIDs
#define SERVICE_UUID_STR        "7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01"
#define CAPTURE_CHAR_UUID_STR   "7f9c8002-1c4e-4d0e-9c0d-7e3a6f2b9d01"
#define CONFIG_CHAR_UUID_STR    "7f9c8003-1c4e-4d0e-9c0d-7e3a6f2b9d01"
#define STATUS_CHAR_UUID_STR    "7f9c8004-1c4e-4d0e-9c0d-7e3a6f2b9d01"

// ============================================================================
// WiFi Sniffer Configuration
// ============================================================================

// Channel hopping interval in milliseconds
#define CHANNEL_HOP_INTERVAL_MS 250

// 2.4 GHz channels to monitor (non-overlapping)
#define CHANNELS_2_4_GHZ {1, 6, 11}
#define CHANNELS_2_4_GHZ_COUNT 3

// 5 GHz channels to monitor
#define CHANNELS_5_GHZ {36, 40, 44, 48, 149, 153, 157, 161}
#define CHANNELS_5_GHZ_COUNT 8

// ============================================================================
// Capture Configuration
// ============================================================================

// Maximum number of authorized BSSIDs
#define MAX_AUTHORIZED_BSSIDS 32

// Maximum tracked handshakes simultaneously
#define MAX_TRACKED_HANDSHAKES 32

// Handshake timeout in microseconds (30 seconds)
#define HANDSHAKE_TIMEOUT_US 30000000

// SSID cache size for BSSID -> SSID mapping
#define SSID_CACHE_SIZE 64

// ============================================================================
// BLE Transfer Configuration
// ============================================================================

// Maximum capture frame size
#define MAX_CAPTURE_FRAME_SIZE 512

// Status heartbeat interval in milliseconds
#define STATUS_HEARTBEAT_INTERVAL_MS 10000

// ============================================================================
// Protocol Constants
// ============================================================================

// Protocol version
#define PROTOCOL_VERSION 0x01

// Capture kinds
#define CAPTURE_KIND_PMKID 0x01
#define CAPTURE_KIND_EAPOL 0x02

// ============================================================================
// Debug Configuration
// ============================================================================

// Enable verbose logging (disable in production)
#define DEBUG_LOGGING 1

// Log tag
#define TAG "WF-SNIFFER"

#endif // CONFIG_H

# WF-Thermos-Eye ESP32 Sniffer Companion

WiFi packet capture companion device for the wf-thermos-eye Android app.

## Purpose

Captures PMKID and EAPOL 4-way handshakes from authorized WiFi networks for offline WPA password cracking.

## Hardware

**Recommended:**

- **ESP32-S3 DevKitC** (~€8) - Best CSI performance among the C/S series
- **ESP32-C6 DevKitC** (~€10) - Wi-Fi 6 + dual-band; even better SNR
- **ESP32-C3** (~€5) - Minimal acceptable, single-band 2.4 GHz

**Power:** USB-C from a small power bank (5V / ≥1A) or 18650 cell with LDO regulator. Cooling matters — sustained promiscuous mode heats the SoC.

## Building

Requires ESP-IDF v5.1 or later.

```bash
# Set up ESP-IDF environment
. $IDF_PATH/export.sh

# Build
cd companion
idf.py build

# Flash
idf.py -p /dev/ttyUSB0 flash monitor
```

## BLE Protocol

**Service UUID:** `7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01`

**Characteristics:**

| UUID | Name | Properties | Description |
|------|------|------------|-------------|
| `7f9c8002-...` | CAPTURE | Notify | Capture frames (PMKID/EAPOL) |
| `7f9c8003-...` | CONFIG | Write | Authorized BSSID list (CSV) |
| `7f9c8004-...` | STATUS | Read/Notify | Heartbeat + stats |

**Capture Frame Format (little-endian):**

```
Offset  Size  Field         Description
------  ----  -----         -----------
0       1     version       Protocol version (0x01)
1       1     kind          0x01=PMKID, 0x02=EAPOL
2       6     bssid         Raw MAC bytes
8       1     ssid_len      SSID length (0-32)
9       N     ssid          SSID bytes
9+N     2     payload_len   Payload length (LE uint16)
11+N    M     payload       Hashcat 22000 record bytes
```

## Operation

1. Power on the ESP32 - it starts advertising as "WF-Sniffer"
2. Android app connects via BLE
3. App writes authorized BSSID list to CONFIG characteristic
4. ESP32 monitors WiFi channels in promiscuous mode
5. When PMKID or EAPOL handshake is captured from an authorized BSSID:
   - Frame is formatted as hashcat 22000 record
   - Transmitted to Android via CAPTURE characteristic
6. Android validates scope and stores for cracking

## What It Does

- Monitors 2.4 GHz channels 1, 6, 11 and 5 GHz channels 36-165
- Extracts PMKID from beacon frames and association responses
- Captures EAPOL M1+M2 handshake pairs
- Filters at source: only authorized BSSIDs are transmitted
- Formats captures as hashcat mode 22000 records

## What It Does NOT Do

- **No deauth injection** - Waits for natural client reconnection
- **No transmission of out-of-scope captures** - Filtered at source
- **No firmware OTA over BLE** - Updates via USB only

## Files

```
companion/
├── main/
│   ├── main.c              # Application entry point
│   ├── config.h            # Build-time configuration
│   ├── scope_filter.c/h    # BSSID authorization filter
│   ├── ssid_cache.c/h      # BSSID -> SSID mapping
│   ├── wifi_sniffer.c/h    # Promiscuous mode capture
│   ├── pmkid_extractor.c/h # PMKID extraction from RSN IE
│   ├── eapol_tracker.c/h   # 4-way handshake state machine
│   ├── hashcat_formatter.c/h # Build hashcat records
│   ├── ble_service.c/h     # BLE GATT server
│   └── CMakeLists.txt      # Component build config
├── CMakeLists.txt          # Project build config
└── sdkconfig.defaults      # ESP-IDF configuration
```

## Testing

1. Build and flash to ESP32
2. Power on - verify "WF-Sniffer" appears in BLE scan
3. Connect from Android app Layer B screen
4. Configure authorized BSSID (your test router)
5. Verify captures appear in app when:
   - PMKID: Immediately from beacons (if present)
   - EAPOL: When a client reconnects to the network

## Legal Notice

This firmware is for security auditing of networks you own or have explicit authorization to test. Unauthorized capture of WiFi handshakes is illegal in most jurisdictions.

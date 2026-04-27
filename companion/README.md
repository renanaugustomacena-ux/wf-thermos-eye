# wf-thermos-eye — ESP32 sniffer companion

This directory hosts the firmware for the Layer B sniffer companion. The
phone-side BLE bridge expects a device that follows this protocol; any
firmware that conforms can be paired.

## Hardware

Recommended:

- **ESP32-S3 DevKitC** (~€8) — best CSI performance among the C/S series
- **ESP32-C6 DevKitC** (~€10) — Wi-Fi 6 + dual-band; even better SNR
- **ESP32-C3** (~€5) — minimal acceptable, single-band 2.4 GHz

Power: USB-C from a small power bank (5 V / ≥1 A) or 18650 cell with
LDO regulator. Cooling matters — sustained promiscuous mode heats the
SoC.

## What it does

1. Boot, advertise BLE GATT service `7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01`.
2. Wait for the phone to write to the CONFIG characteristic
   (`7f9c8003-…`) with a comma-separated list of authorized BSSID
   strings, ASCII, uppercase, e.g. `AA:BB:CC:DD:EE:FF,11:22:33:44:55:66`.
3. Enable Wi-Fi promiscuous mode on the 2.4 GHz channels of interest
   (1, 6, 11 hop every 250 ms) plus 5 GHz channels if hardware supports.
4. Inspect every 802.11 frame in software:
   - **EAPOL frames** (subtype = QoS Data, LLC SNAP type 0x888E) →
     parse 4-way handshake state machine, buffer M1–M4 frames per BSSID,
     emit `EAPOL_4WAY` capture once a complete pair is observed.
   - **Beacon / association frames containing PMKID IE** → extract the
     16-byte PMKID, build hashcat 22000 record, emit `PMKID` capture.
5. **Filter at source**: only emit captures whose BSSID matches an entry
   in the configured list. Out-of-scope captures are discarded inside
   the ESP32, never transmitted. This is a redundant gate to the
   phone-side `OffensiveScopeGuard`, but reducing BLE traffic + power
   matters in field use.
6. Send capture frames over BLE notify on characteristic
   `7f9c8002-…`, format below.
7. Heartbeat every 10 s on `7f9c8004-…` (one byte: count of captures
   buffered + uptime seconds).

## Wire protocol

See the canonical definition in
`core/data/src/main/java/com/alexcupsa/wifithermal/core/data/ble/BleSnifferProtocol.kt`.
Phone-side reference parser is in the same file. Tests in
`core/data/src/test/java/.../BleSnifferProtocolTest.kt` are the
contract — keep firmware-side encoding compatible with them.

Capture frame (little-endian where multi-byte):

```
+-------+-------+--------------------+--------+----------+----------+----------+
| ver=1 | kind  | bssid (6 bytes)    | sslen  | ssid     | plen(u16)| payload  |
| u8    | u8    | raw, no separators | u8     | sslen B  | LE       | plen B   |
+-------+-------+--------------------+--------+----------+----------+----------+
```

`kind`:

- `0x01` PMKID — payload is hashcat `-m 22000` PMKID record bytes
- `0x02` EAPOL_4WAY — payload is hashcat `-m 22000` EAPOL record bytes

## What it must NOT do

- **No deauth injection**. The protocol is observation-only. Wait for a
  natural client (re)connection to capture EAPOL; do not force one.
- **No transmission of out-of-scope captures**. Filter at source.
- **No extra characteristics** beyond CAPTURE / CONFIG / STATUS.
- **No firmware OTA over the open BLE connection**. Updates are
  performed via USB-C with the device disconnected from the phone.

## Recommended frameworks

- **esp-idf v5.1+** with the CSI subsystem
- `esp_wifi_set_promiscuous_rx_cb` for frame ingest
- `esp_wifi_80211_tx` is **not** used — write-paths are deliberately
  absent from the firmware
- BLE stack: NimBLE (lighter than Bluedroid)

## Reference projects to crib from

These do most of what we need with light modifications:

- [`espressif/esp-csi`](https://github.com/espressif/esp-csi) — CSI
  framework, useful for adapting frame ingest patterns
- [`spacehuhn/esp8266_deauther`](https://github.com/SpacehuhnTech/esp8266_deauther) —
  **DO NOT use the deauth path**, but the frame parsing is a useful
  reference for 802.11 header decoding
- [`hcxtools` PMKID format spec](https://github.com/ZerBea/hcxtools) —
  exact byte layout for hashcat 22000 PMKID and EAPOL records

## Build + flash

`pio run -t upload --upload-port /dev/ttyUSB0` (PlatformIO) or
`idf.py -p /dev/ttyUSB0 flash monitor` (esp-idf direct).

Firmware source under this directory is intentionally **not yet
committed**; the operator will write the first version once the
testbed router is acquired and the phone-side pipeline has been
exercised end-to-end with synthetic frames in `BleSnifferProtocolTest`.

Last reviewed: 2026-04-27

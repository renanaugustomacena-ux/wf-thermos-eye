# WiFi Thermal Scanner — Complete Engineering Specification
## Personal Advanced Work Tool for Motorola G35

**Version**: 1.0.0-alpha
**Date**: 2026-04-26
**Target Device**: Motorola Moto G35 (Android 14, Unisoc T760)
**Platform**: Native Android (Kotlin + Jetpack Compose)
**Author**: Alex Cupsa
**Classification**: Personal tool — not a public product

---

## Table of Contents

- [PART I: FOUNDATIONS](#part-i-foundations)
  - [1. Project Vision & Goals](#1-project-vision--goals)
  - [2. Target Device Deep Analysis](#2-target-device-deep-analysis)
  - [3. Technology Stack & Justification](#3-technology-stack--justification)
  - [4. Development Environment Setup](#4-development-environment-setup)
- [PART II: ARCHITECTURE](#part-ii-architecture)
  - [5. System Architecture Overview](#5-system-architecture-overview)
  - [6. Module Architecture](#6-module-architecture)
  - [7. Data Flow Architecture](#7-data-flow-architecture)
  - [8. State Management Architecture](#8-state-management-architecture)
  - [9. Dependency Injection Graph](#9-dependency-injection-graph)
- [PART III: CORE ENGINE](#part-iii-core-engine)
  - [10. WiFi Scanner Engine](#10-wifi-scanner-engine)
  - [11. Signal Processing Pipeline](#11-signal-processing-pipeline)
  - [12. Indoor Positioning System](#12-indoor-positioning-system)
  - [13. Heatmap Generation Engine](#13-heatmap-generation-engine)
  - [14. Interpolation Algorithms](#14-interpolation-algorithms)
  - [15. Path Loss Modeling](#15-path-loss-modeling)
- [PART IV: DATA LAYER](#part-iv-data-layer)
  - [16. Database Schema](#16-database-schema)
  - [17. Data Models & Entities](#17-data-models--entities)
  - [18. Repository Pattern & DAOs](#18-repository-pattern--daos)
  - [19. Data Migration Strategy](#19-data-migration-strategy)
- [PART V: UI/UX SPECIFICATION](#part-v-uiux-specification)
  - [20. Screen Map & Navigation](#20-screen-map--navigation)
  - [21. Screen Specifications](#21-screen-specifications)
  - [22. Jetpack Compose Components](#22-jetpack-compose-components)
  - [23. Theming & Design System](#23-theming--design-system)
  - [24. Gesture & Interaction Design](#24-gesture--interaction-design)
- [PART VI: ADVANCED FEATURES](#part-vi-advanced-features)
  - [25. Channel Congestion Analyzer](#25-channel-congestion-analyzer)
  - [26. Network Security Auditor](#26-network-security-auditor)
  - [27. Speed & Throughput Estimator](#27-speed--throughput-estimator)
  - [28. AR Signal Overlay](#28-ar-signal-overlay)
  - [29. Multi-Floor 3D Mapping](#29-multi-floor-3d-mapping)
  - [30. Export & Reporting Engine](#30-export--reporting-engine)
  - [31. Historical Analysis & Diffing](#31-historical-analysis--diffing)
- [PART VII: IMPLEMENTATION](#part-vii-implementation)
  - [32. Complete File Structure](#32-complete-file-structure)
  - [33. Implementation Phases](#33-implementation-phases)
  - [34. Class & Interface Catalog](#34-class--interface-catalog)
  - [35. Build Configuration (Gradle)](#35-build-configuration-gradle)
  - [36. ProGuard / R8 Rules](#36-proguard--r8-rules)
- [PART VIII: QUALITY & OPERATIONS](#part-viii-quality--operations)
  - [37. Testing Strategy](#37-testing-strategy)
  - [38. Performance Budgets](#38-performance-budgets)
  - [39. Battery Optimization](#39-battery-optimization)
  - [40. Error Handling & Resilience](#40-error-handling--resilience)
  - [41. Build, Sign & Deploy](#41-build-sign--deploy)

---

# PART I: FOUNDATIONS

---

## 1. Project Vision & Goals

### 1.1 What This App Does

A WiFi Thermal Scanner is an advanced RF site survey tool that:

1. **Scans** all detectable WiFi access points in the 2.4 GHz and 5 GHz bands
2. **Maps** signal strength measurements to physical positions on a floor plan
3. **Generates** thermal (heatmap) visualizations showing coverage quality
4. **Analyzes** channel utilization, interference, security posture, and network health
5. **Predicts** coverage in unmeasured areas using interpolation and path loss models
6. **Tracks** changes over time for before/after comparisons

### 1.2 Use Cases

| Use Case | Description |
|---|---|
| **Site Survey** | Walk a space with the phone, collecting signal data point by point. Produce a heatmap showing strong/weak zones. |
| **Dead Zone Detection** | Identify areas where WiFi coverage drops below usable thresholds (-75 dBm or worse). |
| **AP Placement Optimization** | Determine optimal access point placement by analyzing current coverage gaps. |
| **Channel Optimization** | Visualize channel congestion to select the least-interfered channels. |
| **Interference Hunting** | Detect co-channel and adjacent-channel interference between networks. |
| **Security Audit** | Identify open networks, WEP-encrypted networks, and rogue access points. |
| **Before/After Comparison** | Compare surveys taken at different times to verify improvements. |
| **Multi-Floor Analysis** | Map signal propagation across floors of a building. |

### 1.3 Design Principles

1. **Accuracy over speed** — A survey that takes 30 minutes but produces reliable data beats a 5-minute scan with garbage results.
2. **Offline-first** — Zero dependency on internet connectivity. Everything runs locally.
3. **Power-user UI** — No hand-holding, no onboarding carousels. Dense information display. Every pixel earns its place.
4. **Deterministic behavior** — Same inputs produce same outputs. No hidden state. No surprises.
5. **Raw data access** — Every measurement is exportable as raw CSV/JSON. The app never hides data.

### 1.4 Non-Goals

- Not a public app. No Google Play Store compliance, no analytics, no telemetry.
- Not a real-time network monitor (though it has real-time capabilities during surveys).
- Not a speed test app (though it estimates throughput from signal metrics).
- Not a network configuration tool (it reads, it doesn't write).

---

## 2. Target Device Deep Analysis

### 2.1 Motorola Moto G35 Hardware Specifications

| Component | Specification | Impact on App |
|---|---|---|
| **SoC** | Unisoc Tiger T760 (6nm) — 4× Cortex-A76 @ 2.2 GHz + 4× Cortex-A55 @ 1.8 GHz | Ample CPU for signal processing and interpolation. Big cores handle heatmap rendering. |
| **GPU** | Mali-G57 MP4 | Adequate for Canvas-based 2D heatmap rendering. Not for heavy 3D. |
| **RAM** | 4 GB or 8 GB (LPDDR4X) | 4 GB model: budget ~150 MB max for app heap. 8 GB: budget ~300 MB. Design for 4 GB. |
| **Display** | 6.72" IPS LCD, 1080×2400, 120 Hz, 20:9 | High resolution means heatmap bitmaps need to be carefully sized. 120 Hz means smooth panning/zooming if we hit frame targets. |
| **WiFi** | 802.11 a/b/g/n/ac (Wi-Fi 5), dual-band 2.4 GHz + 5 GHz | No WiFi 6 (802.11ax) scanning. No 6 GHz band. We scan 2.4 and 5 GHz. |
| **Bluetooth** | Bluetooth 5.3 | Not directly relevant but BT interference affects 2.4 GHz — worth detecting. |
| **Sensors** | Accelerometer, Gyroscope, Magnetometer (compass), Proximity, Ambient Light | Full IMU suite for dead reckoning indoor positioning. |
| **GPS** | GPS, GLONASS, Galileo, BDS | Useful for outdoor surveys and initial position anchoring. |
| **Camera** | 50 MP main + 8 MP ultrawide | Can be used for AR overlay and floor plan photo capture. |
| **Storage** | 128/256 GB (UFS 2.2) | Plenty for survey data. A comprehensive survey is ~5-50 MB. |
| **Battery** | 5000 mAh | Critical: WiFi scanning + sensor polling + screen-on = heavy drain. Budget for ~2-3 hour continuous survey sessions. |
| **OS** | Android 14 (API 34) | Target SDK 34. Min SDK 33 (Android 13) for future-proofing if needed, but 34 is fine for personal use. |

### 2.2 Android WiFi Scanning Constraints (API 34)

This is critical to understand — Android has progressively restricted WiFi scanning:

| Constraint | Details |
|---|---|
| **Scan throttling** | Foreground apps: max 4 scans per 2-minute window. Background: max 1 scan per 30 minutes. |
| **Permissions required** | `ACCESS_FINE_LOCATION` + `ACCESS_WIFI_STATE` + `CHANGE_WIFI_STATE` + Location Services MUST be ON. |
| **Android 13+ (API 33)** | New `NEARBY_WIFI_DEVICES` permission. Required if targeting API 33+. |
| **ScanResult data** | SSID, BSSID, frequency (MHz), level (RSSI in dBm), channelWidth, capabilities (security), timestamp, standard (WiFi generation). |
| **WifiManager.startScan()** | Still functional but officially deprecated since API 28. Returns boolean indicating if scan was successfully started. |
| **Scan results age** | Results may be cached. Check `ScanResult.timestamp` (microseconds since boot) to determine freshness. |
| **Background scanning** | The OS performs periodic background scans. `getScanResults()` returns these even without calling `startScan()`. |

### 2.3 Scan Throttling Mitigation Strategy

The 4-scans-per-2-minutes throttle is the single biggest constraint. Strategies:

1. **Maximize data per scan**: Each `getScanResults()` returns ALL visible APs at once (typically 10-50). That's highly efficient — we get a full snapshot each time.
2. **Optimal scan interval**: 30 seconds between scans = 4 scans per 2 minutes exactly. This is the fastest sustainable rate.
3. **User-paced collection**: The user walks to a point, taps to collect, waits for scan, then moves on. This naturally spaces scans.
4. **Continuous mode fallback**: In "continuous walk" mode, trigger scans at the max rate and collect on each callback. Accept that some points will use cached results.
5. **Scan result freshness validation**: Check `ScanResult.timestamp` — reject results older than 60 seconds.
6. **Companion scanning via WifiManager registered receiver**: Register `SCAN_RESULTS_AVAILABLE_ACTION` broadcast and also poll cached results between scans. The OS's own periodic scans supplement ours.

### 2.4 Memory Budget

For the 4 GB RAM variant (worst case):

| Component | Budget |
|---|---|
| Android OS + system services | ~1.5 GB |
| Other background apps | ~500 MB |
| Available for our app | ~2 GB (but Android will kill us around 256-384 MB) |
| **App heap target** | **≤ 150 MB** |
| Heatmap bitmap (1080×2400 ARGB_8888) | ~10.4 MB per full-screen bitmap |
| Floor plan image (typical) | ~5-20 MB decoded |
| Survey data in memory (500 points × 50 APs) | ~2 MB |
| Room DB + indices | ~5-10 MB |
| UI composables + state | ~20-30 MB |
| **Headroom** | **~80 MB** |

Design decision: heatmap rendering uses tiled bitmaps (512×512 tiles) to avoid a single massive allocation. Floor plans are subsampled on load via `BitmapFactory.Options.inSampleSize`.

---

## 3. Technology Stack & Justification

### 3.1 Why Native Android (Kotlin) — Not React Native, Not Flutter

| Factor | Native Kotlin | React Native | Flutter |
|---|---|---|---|
| WiFi API access | Direct — `WifiManager` is a first-class Android API | Requires native module bridge. Community libraries exist but lag behind API changes. | Requires platform channel. Same bridging overhead. |
| Sensor access | Direct — `SensorManager` with microsecond timestamps | Bridge latency adds 5-15ms jitter to sensor readings. Unacceptable for dead reckoning. | Slightly better than RN but still a bridge. |
| Canvas rendering | Hardware-accelerated `Canvas` + `Bitmap` with zero serialization | Drawing must cross the JS bridge. Complex heatmaps stutter at 60fps. | Skia engine is excellent but adds 5+ MB to APK. |
| APK size | ~5-10 MB | ~25-40 MB (includes JS engine + bridge) | ~15-25 MB (includes Dart VM + Skia) |
| Reliability | One language, one runtime, one toolchain. Nothing to go wrong. | JS thread can crash independently. Bridge can deadlock. Metro bundler issues. | Less fragile than RN but still an abstraction layer. |
| Long-term maintenance | First-party Google support forever. Jetpack Compose is the official UI toolkit. | Facebook's commitment waxes and wanes. Breaking changes between versions. | Google-backed but secondary to native Android. |

**Verdict**: For a power-user personal tool that demands maximum reliability and deep hardware access, native Kotlin is the only defensible choice. Every cross-platform framework would add complexity without adding value for a single-device, single-platform app.

### 3.2 Core Technology Choices

| Layer | Technology | Version | Justification |
|---|---|---|---|
| **Language** | Kotlin | 2.0+ | Null safety, coroutines, extension functions, sealed classes for state modeling. |
| **UI Framework** | Jetpack Compose | BOM 2024.09+ | Declarative, reactive, composable. Superior to XML layouts for data-driven heatmap UIs. |
| **Navigation** | Compose Navigation | 2.8+ | Type-safe routes, deep linking, back stack management. |
| **Async** | Kotlin Coroutines + Flow | 1.9+ | Structured concurrency. `StateFlow` for UI state. `callbackFlow` for sensor streams. |
| **DI** | Hilt (Dagger) | 2.52+ | Compile-time DI. No runtime reflection overhead. Scoped to Android lifecycle. |
| **Database** | Room | 2.6+ | SQLite with compile-time query verification. Flow-based reactive queries. |
| **Image Loading** | Coil 3 | 3.0+ | Compose-native. Efficient memory management. Supports subsampling. |
| **Serialization** | Kotlinx Serialization | 1.7+ | Compile-time, no reflection. Fast JSON/CSV/Protobuf encoding. |
| **Math/DSP** | Custom + Apache Commons Math (ported) | — | Interpolation, matrix ops, statistics. We'll port only what we need to avoid the full dependency. |
| **Charts** | Custom Compose Canvas | — | No charting library matches our needs. We draw channel graphs and signal plots directly on Canvas. |
| **Build** | Gradle (Kotlin DSL) | 8.7+ | Standard Android build system. Version catalogs for dependency management. |
| **Min SDK** | API 34 (Android 14) | — | No need for backward compatibility. G35 ships with Android 14. |
| **Target SDK** | API 35 (Android 15) | — | Latest behavior changes. Future-proof. |

### 3.3 Dependencies — Minimal and Audited

We follow a strict dependency policy: every third-party library must justify its existence against writing the equivalent ourselves. For a personal tool, fewer dependencies = fewer surprises.

**Approved dependencies:**
```
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-compose
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.activity:activity-compose
androidx.compose (BOM) — ui, material3, foundation, animation, tooling
androidx.navigation:navigation-compose
androidx.room:room-runtime + room-ktx + room-compiler (KSP)
androidx.hilt:hilt-navigation-compose
com.google.dagger:hilt-android + hilt-compiler
org.jetbrains.kotlinx:kotlinx-coroutines-android
org.jetbrains.kotlinx:kotlinx-serialization-json
io.coil-kt.coil3:coil-compose
```

**Rejected dependencies (with reasons):**
- MPAndroidChart / Vico — We need custom signal visualizations that no chart lib supports well.
- Mapbox / Google Maps — Overkill for indoor floor plan overlays. We build our own.
- TensorFlow Lite — ML predictions can be done with simpler statistical models.
- OkHttp / Retrofit — No network calls needed. Fully offline app.
- Timber — `Log` wrapper is sufficient for personal use.

---

## 4. Development Environment Setup

### 4.1 Prerequisites Installation (Ubuntu 24.04)

```bash
# Step 1: Install JDK 21 (required for Gradle 8.7+ and AGP 8.5+)
sudo apt update
sudo apt install -y openjdk-21-jdk

# Verify
java -version   # Should show: openjdk 21.x.x
javac -version  # Should show: javac 21.x.x

# Step 2: Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Step 3: Install Android Command-Line Tools
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools

# Download latest command-line tools from Google
# URL: https://developer.android.com/studio#command-tools
# As of 2026, use the latest Linux zip
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# Step 4: Set Android SDK environment variables
cat >> ~/.bashrc << 'ENVEOF'
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0
ENVEOF
source ~/.bashrc

# Step 5: Accept licenses and install SDK components
yes | sdkmanager --licenses

sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "platforms;android-34" \
  "build-tools;35.0.0" \
  "build-tools;34.0.0" \
  "sources;android-35" \
  "emulator" \
  "system-images;android-34;google_apis;x86_64"

# Step 6: Verify ADB sees the Motorola G35
# (Connect phone via USB, enable Developer Options + USB Debugging)
adb devices
# Should show: XXXXXXXX  device

# Step 7: Install Gradle (optional — the wrapper handles this, but useful globally)
# The project will use gradle-wrapper, so this is optional.
# If you want it globally:
sdk install gradle 8.7   # via SDKMAN, or download manually
```

### 4.2 Project Initialization

```bash
# Create project directory
mkdir -p ~/projects/wifi-thermal-scanner
cd ~/projects/wifi-thermal-scanner

# Initialize git
git init
git branch -m main

# Create the Android project structure
# (We'll generate this with Gradle init or manually — detailed in Section 32)
```

### 4.3 IDE Setup (Android Studio or IntelliJ IDEA)

For full Android development:
- **Android Studio Ladybug** (2024.2+) or later
- OR **IntelliJ IDEA Ultimate/Community** with Android plugin

The user has PyCharm Community installed. While PyCharm doesn't support Android development natively, they can:
1. Install Android Studio alongside PyCharm (recommended)
2. Use command-line builds with Gradle (`./gradlew assembleDebug`)

### 4.4 Device Setup for Development

On the Motorola G35:
1. **Settings → About Phone → Tap "Build Number" 7 times** → Enables Developer Options
2. **Settings → System → Developer Options → Enable USB Debugging**
3. **Settings → System → Developer Options → Enable "Install via USB"**
4. **Settings → Location → Enable Location Services** (required for WiFi scanning)
5. **Connect via USB** and authorize the computer when prompted
6. **Verify**: `adb devices` shows the device

### 4.5 Signing Configuration

For a personal app, we use a debug keystore or a personal release keystore:

```bash
# Generate a personal release keystore (one-time)
keytool -genkeypair \
  -alias wifi-thermal-scanner \
  -keyalg RSA \
  -keysize 4096 \
  -validity 36500 \
  -keystore ~/projects/wifi-thermal-scanner/keystore/release.jks \
  -storepass <your-password> \
  -keypass <your-password> \
  -dname "CN=Alex Cupsa, O=Personal, L=Unknown, ST=Unknown, C=IT"
```

---

# PART II: ARCHITECTURE

---

## 5. System Architecture Overview

### 5.1 High-Level Architecture Diagram (Textual)

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Dashboard │ │  Survey  │ │ Heatmap  │ │ Analysis │          │
│  │  Screen   │ │  Screen  │ │  Screen  │ │  Screen  │  ...     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
│       │             │            │             │                 │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐          │
│  │ Dashboard │ │  Survey  │ │ Heatmap  │ │ Analysis │          │
│  │ ViewModel│ │ ViewModel│ │ ViewModel│ │ ViewModel│  ...     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
├───────┼─────────────┼────────────┼─────────────┼────────────────┤
│       │         DOMAIN LAYER     │             │                │
│  ┌────┴─────────────┴────────────┴─────────────┴─────────────┐  │
│  │                      USE CASES                             │  │
│  │  StartSurvey · CollectMeasurement · GenerateHeatmap       │  │
│  │  AnalyzeChannels · AuditSecurity · ExportReport           │  │
│  │  LoadFloorPlan · CalculatePosition · CompareHistorical    │  │
│  └────┬─────────────┬────────────┬─────────────┬─────────────┘  │
│       │             │            │             │                 │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐          │
│  │  WiFi    │ │ Position │ │ Heatmap  │ │ Survey   │          │
│  │Repository│ │Repository│ │Repository│ │Repository│  ...     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
├───────┼─────────────┼────────────┼─────────────┼────────────────┤
│       │          DATA LAYER      │             │                │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐          │
│  │  WiFi    │ │  Sensor  │ │   Room   │ │  File    │          │
│  │ Scanner  │ │  Manager │ │ Database │ │  System  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │               SIGNAL PROCESSING ENGINE                     │  │
│  │  KalmanFilter · Interpolator · PathLossModel               │  │
│  │  StepDetector · HeadingEstimator · SensorFusion             │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Layer Responsibilities

| Layer | Responsibility | Allowed Dependencies |
|---|---|---|
| **Presentation** | UI rendering, user interaction, ViewModel state management | Domain layer only. Never touches data sources directly. |
| **Domain** | Business logic, use cases, domain models | Pure Kotlin. No Android imports. No framework dependencies. |
| **Data** | Data source implementations, API/DB access, sensor wrappers | Android SDK, Room, system services. Implements domain interfaces. |
| **Engine** | Signal processing, math, algorithms | Pure Kotlin. No Android imports. Independently testable. |

### 5.3 Concurrency Model

```
Main Thread (UI)
  └── Compose recomposition
  └── User input handling
  └── ViewModel state emission

IO Dispatcher (background)
  └── Room database operations
  └── File I/O (floor plan loading, CSV export)
  └── JSON serialization

Default Dispatcher (CPU-intensive)
  └── Heatmap interpolation
  └── Signal processing (Kalman filter)
  └── Path loss calculations
  └── Bitmap generation

Custom SingleThreadDispatcher ("SensorThread")
  └── Sensor event processing (accelerometer, gyroscope, magnetometer)
  └── Step detection
  └── Heading estimation
  └── Dead reckoning position updates

Custom SingleThreadDispatcher ("WiFiScanThread")
  └── WiFi scan scheduling
  └── Scan result parsing
  └── RSSI smoothing per BSSID
```

### 5.4 Key Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Architecture pattern | MVVM + Clean Architecture (Use Cases) | ViewModels survive config changes. Use Cases encapsulate business logic for testability. |
| State management | `StateFlow` in ViewModels | Lifecycle-aware, backpressure-safe, Compose-native via `collectAsStateWithLifecycle()`. |
| Navigation | Single-Activity + Compose Navigation | No fragments. Cleaner lifecycle. Type-safe routes. |
| Heatmap rendering | Custom `Canvas` drawing in Compose | No library matches our rendering needs. Full control over tile management and interpolation. |
| Floor plan handling | Load as `Bitmap`, display with custom zoomable `Canvas` | Pinch-zoom, pan, rotation. Overlay heatmap with alpha blending. |
| Data persistence | Room (SQLite) | Relational model fits survey→measurement→AP hierarchy. Reactive queries via Flow. |
| Background processing | Foreground Service + Coroutines | Android 14 requires foreground service for continuous sensor/WiFi access. |

---

## 6. Module Architecture

### 6.1 Gradle Module Structure

```
wifi-thermal-scanner/
├── app/                          # Application module (entry point)
├── core/
│   ├── core-model/               # Domain models (pure Kotlin)
│   ├── core-data/                # Repositories, data sources
│   ├── core-database/            # Room DB, DAOs, entities
│   ├── core-engine/              # Signal processing, math, algorithms
│   ├── core-ui/                  # Shared Compose components, theme
│   └── core-common/              # Utilities, extensions, constants
├── feature/
│   ├── feature-dashboard/        # Home dashboard screen
│   ├── feature-survey/           # Survey recording screen
│   ├── feature-heatmap/          # Heatmap visualization screen
│   ├── feature-analysis/         # Channel/network analysis screen
│   ├── feature-floorplan/        # Floor plan management screen
│   ├── feature-export/           # Export/report screen
│   └── feature-settings/         # App settings screen
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Module declaration
└── gradle.properties             # Gradle config
```

### 6.2 Module Dependency Rules

```
feature-* → app (via Hilt)
feature-* → core-model, core-data, core-ui, core-common
core-data → core-model, core-database, core-engine, core-common
core-database → core-model
core-engine → core-model, core-common
core-ui → core-model, core-common
core-common → (no project dependencies)
```

**Rule**: Feature modules NEVER depend on each other. All cross-feature communication goes through shared state in core-data or via navigation arguments.

---

## 7. Data Flow Architecture

### 7.1 Survey Data Collection Flow

```
[User taps "Collect" button]
       │
       ▼
[SurveyViewModel.collectMeasurement()]
       │
       ▼
[CollectMeasurementUseCase.invoke()]
       │
       ├──► [WiFiScannerDataSource.getLatestScanResults()]
       │         │
       │         ▼
       │    [WifiManager.getScanResults()] → List<ScanResult>
       │         │
       │         ▼
       │    [SignalProcessor.smooth(rawRssi)] → smoothed RSSI per BSSID
       │
       ├──► [PositionTracker.getCurrentPosition()]
       │         │
       │         ▼
       │    [SensorFusion.getPosition()] → (x, y) in floor plan coordinates
       │
       ▼
[MeasurementPoint(position, timestamp, List<ApMeasurement>)]
       │
       ▼
[SurveyRepository.saveMeasurement(point)]
       │
       ▼
[Room DB: measurement_points + ap_measurements tables]
       │
       ▼
[StateFlow emission → UI updates marker on floor plan]
```

### 7.2 Heatmap Generation Flow

```
[User opens Heatmap screen / selects AP to visualize]
       │
       ▼
[HeatmapViewModel.generateHeatmap(surveyId, bssid, band)]
       │
       ▼
[GenerateHeatmapUseCase.invoke()]
       │
       ├──► [SurveyRepository.getMeasurements(surveyId)]
       │         → List<MeasurementPoint> with RSSI values
       │
       ├──► [FloorPlanRepository.getFloorPlan(floorPlanId)]
       │         → FloorPlan with dimensions and scale
       │
       ▼
[HeatmapEngine.generate(measurements, floorPlan, config)]
       │
       ├──► Step 1: Build measurement grid
       │    Map (x,y) → RSSI for selected AP/band
       │
       ├──► Step 2: Interpolation (configurable algorithm)
       │    IDW / Kriging / RBF → fill grid cells
       │
       ├──► Step 3: Gaussian smoothing pass
       │    σ = config.smoothingRadius
       │
       ├──► Step 4: Color mapping
       │    RSSI dBm → ARGB color via gradient LUT
       │
       ├──► Step 5: Bitmap generation
       │    Create tiled Bitmap (512×512 tiles)
       │    Apply floor plan alpha mask
       │
       ▼
[HeatmapResult(tiles: List<BitmapTile>, metadata: HeatmapMetadata)]
       │
       ▼
[StateFlow emission → Compose Canvas renders tiles over floor plan]
```

### 7.3 Sensor Data Flow (Dead Reckoning)

```
[SensorManager registers listeners for ACCEL + GYRO + MAG]
       │
       ▼ (every ~10ms, on SensorThread)
[RawSensorEvent(type, values[], timestamp)]
       │
       ├──► [StepDetector.onAccelerometerEvent(values)]
       │         │
       │         ▼
       │    Peak detection on acceleration magnitude
       │    If step detected → emit StepEvent(timestamp, stepLength)
       │
       ├──► [HeadingEstimator.onGyroAndMagEvent(gyro, mag)]
       │         │
       │         ▼
       │    Complementary filter: α * gyroHeading + (1-α) * magHeading
       │    → emit heading in radians
       │
       ▼
[SensorFusion.onStep(stepLength, heading)]
       │
       ▼
[position.x += stepLength * sin(heading)]
[position.y += stepLength * cos(heading)]
       │
       ▼
[PositionStateFlow.emit(Position(x, y, floor, confidence))]
       │
       ▼
[UI: cursor moves on floor plan in real-time]
```

---

## 8. State Management Architecture

### 8.1 ViewModel State Pattern

Every screen uses a sealed interface for UI state:

```kotlin
// Pattern for every feature
sealed interface SurveyUiState {
    data object Loading : SurveyUiState
    data class Active(
        val surveyId: Long,
        val floorPlan: FloorPlanUi,
        val measurements: List<MeasurementPointUi>,
        val currentPosition: Position?,
        val scanStatus: ScanStatus,
        val elapsedTime: Duration,
        val measurementCount: Int,
        val visibleAps: Int,
    ) : SurveyUiState
    data class Error(val message: String, val retry: (() -> Unit)?) : SurveyUiState
}

sealed interface ScanStatus {
    data object Idle : ScanStatus
    data object Scanning : ScanStatus
    data class Throttled(val nextScanIn: Duration) : ScanStatus
    data class Complete(val resultCount: Int) : ScanStatus
}
```

### 8.2 Event Handling Pattern

One-shot events (navigation, snackbar) use `Channel`:

```kotlin
// In ViewModel
private val _events = Channel<SurveyEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

sealed interface SurveyEvent {
    data class ShowSnackbar(val message: String) : SurveyEvent
    data class NavigateToHeatmap(val surveyId: Long) : SurveyEvent
    data object SurveyCompleted : SurveyEvent
}
```

### 8.3 State Update Rules

1. All state updates happen in the ViewModel via `_state.update { }`.
2. No state mutation in Composables — they are pure functions of state.
3. Side effects (DB writes, scans) are launched in `viewModelScope`.
4. Heavy computation (heatmap generation) runs on `Dispatchers.Default`.
5. The UI observes state via `collectAsStateWithLifecycle()` — automatically pauses when the lifecycle is below STARTED.

---

## 9. Dependency Injection Graph

### 9.1 Hilt Module Structure

```kotlin
// Singleton-scoped (app lifetime)
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideWifiManager(@ApplicationContext ctx: Context): WifiManager
    
    @Provides @Singleton
    fun provideSensorManager(@ApplicationContext ctx: Context): SensorManager
    
    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase
    
    @Provides @Singleton
    fun provideSignalProcessor(): SignalProcessor
    
    @Provides @Singleton
    fun provideHeatmapEngine(): HeatmapEngine
}

// ViewModel-scoped (per-screen lifetime)
// Use @HiltViewModel + @Inject constructor — no explicit module needed.

// Activity-scoped
@Module @InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides
    fun provideLocationManager(@ActivityContext ctx: Context): LocationManager
}
```

### 9.2 Interface Bindings

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindSurveyRepository(impl: SurveyRepositoryImpl): SurveyRepository
    @Binds abstract fun bindWiFiRepository(impl: WiFiRepositoryImpl): WiFiRepository
    @Binds abstract fun bindFloorPlanRepository(impl: FloorPlanRepositoryImpl): FloorPlanRepository
    @Binds abstract fun bindPositionRepository(impl: PositionRepositoryImpl): PositionRepository
}
```

---

# PART III: CORE ENGINE

---

## 10. WiFi Scanner Engine

### 10.1 Scanner Architecture

```kotlin
class WiFiScannerEngine @Inject constructor(
    private val wifiManager: WifiManager,
    private val signalProcessor: SignalProcessor,
    private val context: Context,
) {
    private val _scanResults = MutableStateFlow<List<ProcessedScanResult>>(emptyList())
    val scanResults: StateFlow<List<ProcessedScanResult>> = _scanResults.asStateFlow()
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                processScanResults(success)
            }
        }
    }
}
```

### 10.2 Scan Result Processing

For each scan result, we extract and enrich:

```kotlin
data class ProcessedScanResult(
    val ssid: String,                    // Network name (may be empty for hidden)
    val bssid: String,                   // MAC address of AP (unique identifier)
    val rssi: Int,                       // Raw RSSI in dBm (-30 to -100 typical)
    val smoothedRssi: Double,            // Kalman-filtered RSSI
    val frequency: Int,                  // Center frequency in MHz
    val channel: Int,                    // Derived WiFi channel number
    val channelWidth: ChannelWidth,      // 20/40/80/160 MHz
    val band: WifiBand,                  // BAND_2_4_GHZ or BAND_5_GHZ
    val security: SecurityType,          // OPEN, WEP, WPA, WPA2, WPA3
    val standard: WifiStandard,          // 802.11 a/b/g/n/ac
    val timestamp: Long,                 // Microseconds since boot
    val age: Duration,                   // Time since this measurement
    val signalQuality: SignalQuality,    // EXCELLENT/GOOD/FAIR/WEAK/UNUSABLE
    val estimatedDistance: Double,        // Meters, from path loss model
    val isConnected: Boolean,            // Whether we're currently connected to this AP
    val vendor: String?,                 // OUI lookup for first 3 bytes of BSSID
)
```

### 10.3 Frequency-to-Channel Mapping

```kotlin
object ChannelMapper {
    fun frequencyToChannel(freqMhz: Int): Int = when {
        // 2.4 GHz band: channels 1-14
        freqMhz in 2412..2484 -> {
            if (freqMhz == 2484) 14
            else (freqMhz - 2407) / 5
        }
        // 5 GHz band: channels 32-177
        freqMhz in 5160..5885 -> (freqMhz - 5000) / 5
        // 6 GHz band (future-proofing, G35 can't scan these)
        freqMhz in 5955..7115 -> (freqMhz - 5950) / 5
        else -> -1 // Unknown
    }
    
    fun channelToFrequency(channel: Int, band: WifiBand): Int = when (band) {
        WifiBand.BAND_2_4_GHZ -> {
            if (channel == 14) 2484
            else 2407 + channel * 5
        }
        WifiBand.BAND_5_GHZ -> 5000 + channel * 5
    }
    
    // 2.4 GHz non-overlapping channels
    val NON_OVERLAPPING_2_4 = listOf(1, 6, 11)
    
    // 5 GHz UNII bands
    val UNII_1 = 36..48 step 4      // 5.15-5.25 GHz (indoor)
    val UNII_2 = 52..64 step 4      // 5.25-5.35 GHz (DFS)
    val UNII_2E = 100..144 step 4   // 5.47-5.725 GHz (DFS)
    val UNII_3 = 149..165 step 4    // 5.725-5.85 GHz (outdoor)
}
```

### 10.4 Signal Quality Classification

Based on industry-standard thresholds:

```kotlin
enum class SignalQuality(val label: String, val minRssi: Int, val color: Long) {
    EXCELLENT("Excellent", -50,  0xFF00C853),  // Green
    GOOD("Good",           -60,  0xFF64DD17),  // Light green
    FAIR("Fair",           -70,  0xFFFFD600),  // Yellow
    WEAK("Weak",           -80,  0xFFFF6D00),  // Orange
    UNUSABLE("Unusable",   -100, 0xFFD50000);  // Red
    
    companion object {
        fun fromRssi(rssi: Int): SignalQuality = entries.firstOrNull { rssi >= it.minRssi } ?: UNUSABLE
    }
}
```

| Quality | RSSI Range | Practical Meaning |
|---|---|---|
| Excellent | ≥ -50 dBm | HD video streaming, VoIP, all applications work flawlessly |
| Good | -50 to -60 dBm | Web browsing, email, standard streaming work well |
| Fair | -60 to -70 dBm | Light web browsing works. Video may buffer. |
| Weak | -70 to -80 dBm | Basic connectivity. Frequent packet loss. Unreliable. |
| Unusable | < -80 dBm | Connection drops. Effective dead zone. |

### 10.5 OUI Vendor Lookup

The first 3 bytes (24 bits) of a BSSID (MAC address) identify the manufacturer. We embed a lookup table for the top ~500 vendors (covers 95%+ of consumer APs):

```kotlin
object OuiLookup {
    // Trimmed to top vendors. Full IEEE OUI database is 30MB+ — overkill.
    private val table: Map<String, String> = mapOf(
        "00:1A:2B" to "Cisco",
        "F8:E4:3B" to "ASUS",
        "AC:84:C6" to "TP-Link",
        "78:8A:20" to "Ubiquiti",
        "B0:BE:76" to "TP-Link",
        "C0:25:E9" to "TP-Link",
        "DC:A6:32" to "Raspberry Pi",
        "B8:27:EB" to "Raspberry Pi",
        "00:50:56" to "VMware",
        // ... (full table populated from IEEE MA-L registry, ~500 entries)
    )
    
    fun lookup(bssid: String): String? {
        val oui = bssid.take(8).uppercase()
        return table[oui]
    }
}
```

---

## 11. Signal Processing Pipeline

### 11.1 RSSI Smoothing with Kalman Filter

WiFi RSSI readings are noisy (±6 dBm variance is typical). A Kalman filter produces stable estimates:

```kotlin
class RssiKalmanFilter(
    private val processNoise: Double = 0.008,   // Q: how much we expect RSSI to change per step
    private val measurementNoise: Double = 4.0,  // R: expected measurement noise variance
) {
    private var estimate: Double = 0.0           // Current best estimate (x̂)
    private var errorCovariance: Double = 1.0    // Current estimation error (P)
    private var initialized: Boolean = false
    
    fun update(measurement: Double): Double {
        if (!initialized) {
            estimate = measurement
            errorCovariance = measurementNoise
            initialized = true
            return estimate
        }
        
        // Prediction step
        // x̂ₖ₋ = x̂ₖ₋₁  (static model: we predict RSSI stays the same)
        // Pₖ₋ = Pₖ₋₁ + Q
        val predictedError = errorCovariance + processNoise
        
        // Update step
        // Kₖ = Pₖ₋ / (Pₖ₋ + R)          — Kalman gain
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        
        // x̂ₖ = x̂ₖ₋ + Kₖ(zₖ - x̂ₖ₋)      — Updated estimate
        estimate += kalmanGain * (measurement - estimate)
        
        // Pₖ = (1 - Kₖ)Pₖ₋               — Updated error covariance
        errorCovariance = (1 - kalmanGain) * predictedError
        
        return estimate
    }
    
    fun reset() {
        initialized = false
        estimate = 0.0
        errorCovariance = 1.0
    }
}
```

**Why Kalman over simple EMA**: The Kalman filter adapts its gain based on confidence. When the filter has converged (low P), it trusts the model more and smooths aggressively. When the signal genuinely changes (big innovation), the gain increases and the filter tracks faster. An EMA with fixed α can't do this.

### 11.2 Per-BSSID Filter Management

Each AP gets its own Kalman filter instance:

```kotlin
class SignalProcessor @Inject constructor() {
    private val filters = ConcurrentHashMap<String, RssiKalmanFilter>()
    
    fun processResults(rawResults: List<ScanResult>): List<ProcessedScanResult> {
        val now = SystemClock.elapsedRealtimeNanos()
        
        return rawResults.map { result ->
            val filter = filters.getOrPut(result.BSSID) { RssiKalmanFilter() }
            val smoothed = filter.update(result.level.toDouble())
            val age = Duration.nanoseconds(now - result.timestamp * 1000)
            
            ProcessedScanResult(
                ssid = result.SSID ?: "",
                bssid = result.BSSID,
                rssi = result.level,
                smoothedRssi = smoothed,
                frequency = result.frequency,
                channel = ChannelMapper.frequencyToChannel(result.frequency),
                channelWidth = parseChannelWidth(result.channelWidth),
                band = if (result.frequency < 5000) WifiBand.BAND_2_4_GHZ else WifiBand.BAND_5_GHZ,
                security = parseSecurity(result.capabilities),
                standard = parseStandard(result),
                timestamp = result.timestamp,
                age = age,
                signalQuality = SignalQuality.fromRssi(result.level),
                estimatedDistance = PathLossModel.estimateDistance(smoothed, result.frequency),
                isConnected = false, // Set externally from WifiInfo
                vendor = OuiLookup.lookup(result.BSSID),
            )
        }
    }
    
    fun pruneStaleFilters(maxAge: Duration = 5.minutes) {
        // Remove filters for APs not seen in the last N minutes
        // (Implementation tracks last-seen time per BSSID)
    }
}
```

### 11.3 Outlier Rejection

Before feeding into the Kalman filter, reject extreme outliers:

```kotlin
class OutlierRejector(private val windowSize: Int = 10, private val threshold: Double = 3.0) {
    private val window = ArrayDeque<Double>(windowSize)
    
    fun isOutlier(value: Double): Boolean {
        if (window.size < 3) {
            window.addLast(value)
            return false
        }
        
        val median = window.sorted()[window.size / 2]
        val mad = window.map { abs(it - median) }.sorted()[window.size / 2]
        val modifiedZScore = if (mad > 0) 0.6745 * (value - median) / mad else 0.0
        
        val outlier = abs(modifiedZScore) > threshold
        
        if (!outlier) {
            if (window.size >= windowSize) window.removeFirst()
            window.addLast(value)
        }
        
        return outlier
    }
}
```

The Modified Z-Score using Median Absolute Deviation (MAD) is more robust than standard deviation for the small, non-Gaussian sample sizes we deal with (WiFi scans come in batches of 1-4 per 2 minutes).

---

## 12. Indoor Positioning System

### 12.1 Overview

The Indoor Positioning System (IPS) uses Pedestrian Dead Reckoning (PDR) — it tracks the user's movement by detecting steps and estimating heading from the phone's IMU sensors. This is the most practical approach for our use case because:

1. No infrastructure required (no BLE beacons, no fingerprint database).
2. Works everywhere — indoors and outdoors.
3. Immediate — no calibration phase needed.
4. Accurate enough for room-level granularity when combined with periodic user corrections.

### 12.2 Step Detection Algorithm

We use an accelerometer-based peak detection algorithm:

```kotlin
class StepDetector(
    private val minStepInterval: Duration = 250.milliseconds,  // Max ~4 steps/sec (running)
    private val peakThreshold: Double = 1.2,                    // g-force above which we consider a peak
    private val valleyThreshold: Double = 0.8,                  // g-force below which we consider a valley
) {
    private var lastStepTime: Long = 0
    private var state: DetectionState = DetectionState.WAITING_FOR_PEAK
    private var lastPeakValue: Double = 0.0
    
    // Ring buffer for acceleration magnitude
    private val buffer = DoubleArray(5)  // ~50ms at 100 Hz
    private var bufferIndex = 0
    
    enum class DetectionState {
        WAITING_FOR_PEAK,
        WAITING_FOR_VALLEY,
    }
    
    /**
     * Feed raw accelerometer values. Returns true if a step was detected.
     * 
     * Algorithm: Detect peak-valley pairs in acceleration magnitude.
     * A step is one full gait cycle: foot strike (peak) → midstance (valley).
     * 
     * The acceleration magnitude is: √(x² + y² + z²)
     * At rest, this equals ~9.81 m/s² (1g). During walking, it oscillates
     * between ~0.7g (midstance) and ~1.3g (heel strike).
     */
    fun onAccelerometerEvent(x: Float, y: Float, z: Float, timestampNanos: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z) / 9.81  // Normalize to g-force
        
        // Low-pass filter via moving average
        buffer[bufferIndex % buffer.size] = magnitude
        bufferIndex++
        if (bufferIndex < buffer.size) return false
        
        val filtered = buffer.average()
        
        return when (state) {
            DetectionState.WAITING_FOR_PEAK -> {
                if (filtered > peakThreshold) {
                    lastPeakValue = filtered
                    state = DetectionState.WAITING_FOR_VALLEY
                }
                false
            }
            DetectionState.WAITING_FOR_VALLEY -> {
                if (filtered < valleyThreshold) {
                    val elapsed = Duration.nanoseconds(timestampNanos - lastStepTime)
                    if (elapsed >= minStepInterval) {
                        lastStepTime = timestampNanos
                        state = DetectionState.WAITING_FOR_PEAK
                        true  // Step detected!
                    } else {
                        state = DetectionState.WAITING_FOR_PEAK
                        false
                    }
                } else {
                    false
                }
            }
        }
    }
}
```

### 12.3 Step Length Estimation

Step length varies by person and walking speed. We use the Weinberg model:

```kotlin
class StepLengthEstimator(
    private val calibrationFactor: Double = 0.415  // Tunable. Default empirically reasonable.
) {
    /**
     * Weinberg step length model:
     * L = k × (a_max - a_min)^(1/4)
     * 
     * Where:
     *   L = step length in meters
     *   k = calibration constant (person-specific)
     *   a_max = maximum acceleration in the step cycle
     *   a_min = minimum acceleration in the step cycle
     *
     * Typical adult step length: 0.6 - 0.8 meters
     * This model adapts to walking speed (faster = longer steps).
     */
    fun estimate(accelMax: Double, accelMin: Double): Double {
        val diff = (accelMax - accelMin).coerceAtLeast(0.0)
        return calibrationFactor * diff.pow(0.25)
    }
    
    /**
     * Alternative: fixed step length based on user height.
     * stepLength ≈ height × 0.415 (for men) or height × 0.413 (for women)
     * Configure in settings.
     */
    fun fromHeight(heightCm: Double): Double = heightCm / 100.0 * 0.415
}
```

### 12.4 Heading Estimation with Complementary Filter

The gyroscope provides accurate short-term heading changes (but drifts over time). The magnetometer provides absolute heading (but is noisy and affected by magnetic interference). A complementary filter combines both:

```kotlin
class HeadingEstimator(
    private val alpha: Double = 0.98  // Weight for gyroscope (0.95-0.99 typical)
) {
    private var heading: Double = 0.0       // Radians, 0 = North, clockwise
    private var lastGyroTimestamp: Long = 0
    private var initialized: Boolean = false
    
    // Rotation matrix from Android's sensor fusion (TYPE_ROTATION_VECTOR)
    // is actually better than raw gyro+mag when available
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    /**
     * Complementary filter:
     * heading = α × (heading + gyroΔ) + (1-α) × magHeading
     * 
     * The gyroscope integration gives smooth, responsive heading changes.
     * The magnetometer provides a drift-correcting anchor.
     * α close to 1.0 means we trust gyro more (less mag noise, more drift).
     */
    fun update(
        gyroZ: Float,           // Rotation rate around Z axis (rad/s)
        gyroTimestamp: Long,    // Nanoseconds
        magHeading: Double,     // Magnetic heading in radians
    ): Double {
        if (!initialized) {
            heading = magHeading
            lastGyroTimestamp = gyroTimestamp
            initialized = true
            return heading
        }
        
        val dt = (gyroTimestamp - lastGyroTimestamp) / 1_000_000_000.0  // Convert ns to seconds
        lastGyroTimestamp = gyroTimestamp
        
        // Gyroscope integration
        val gyroHeading = heading + gyroZ * dt
        
        // Complementary filter
        heading = alpha * gyroHeading + (1 - alpha) * magHeading
        
        // Normalize to [0, 2π)
        heading = ((heading % (2 * PI)) + (2 * PI)) % (2 * PI)
        
        return heading
    }
    
    /**
     * Alternative: use Android's TYPE_ROTATION_VECTOR sensor, which already
     * fuses accelerometer + gyroscope + magnetometer internally.
     * This is simpler and often more accurate on devices with good sensor fusion.
     */
    fun updateFromRotationVector(values: FloatArray): Double {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        heading = orientationAngles[0].toDouble()  // Azimuth in radians
        if (heading < 0) heading += 2 * PI
        return heading
    }
}
```

### 12.5 Sensor Fusion — Dead Reckoning Engine

Combines step detection, step length estimation, and heading into position updates:

```kotlin
class DeadReckoningEngine @Inject constructor(
    private val stepDetector: StepDetector,
    private val stepLengthEstimator: StepLengthEstimator,
    private val headingEstimator: HeadingEstimator,
) {
    // Position in meters relative to starting point
    private var x: Double = 0.0
    private var y: Double = 0.0
    private var totalDistance: Double = 0.0
    private var stepCount: Int = 0
    
    private var accelMax: Double = Double.MIN_VALUE
    private var accelMin: Double = Double.MAX_VALUE
    
    private val _position = MutableStateFlow(Position(0.0, 0.0, 0, 1.0))
    val position: StateFlow<Position> = _position.asStateFlow()
    
    fun onSensorEvent(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val mag = sqrt(
                    event.values[0].pow(2) + 
                    event.values[1].pow(2) + 
                    event.values[2].pow(2)
                ).toDouble()
                
                accelMax = max(accelMax, mag)
                accelMin = min(accelMin, mag)
                
                if (stepDetector.onAccelerometerEvent(
                    event.values[0], event.values[1], event.values[2], event.timestamp
                )) {
                    // Step detected!
                    val stepLength = stepLengthEstimator.estimate(accelMax, accelMin)
                    val heading = headingEstimator.getCurrentHeading()
                    
                    // Update position using polar → cartesian
                    x += stepLength * sin(heading)
                    y += stepLength * cos(heading)
                    totalDistance += stepLength
                    stepCount++
                    
                    // Reset peak/valley tracking for next step
                    accelMax = Double.MIN_VALUE
                    accelMin = Double.MAX_VALUE
                    
                    _position.value = Position(
                        x = x,
                        y = y,
                        floor = _position.value.floor,
                        confidence = calculateConfidence(),
                    )
                }
            }
            
            Sensor.TYPE_ROTATION_VECTOR -> {
                headingEstimator.updateFromRotationVector(event.values)
            }
        }
    }
    
    /**
     * Manual position correction by the user (tap on floor plan).
     * Resets dead reckoning anchor point to the tapped location.
     * This is the primary drift correction mechanism.
     */
    fun correctPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
        _position.value = Position(x, y, _position.value.floor, 1.0)
    }
    
    /**
     * Confidence degrades with distance from last correction.
     * After ~50 meters of dead reckoning without correction, 
     * positional error is typically 5-10% of distance traveled.
     */
    private fun calculateConfidence(): Double {
        val distanceSinceCorrection = totalDistance // Reset on correction
        return (1.0 - (distanceSinceCorrection * 0.002)).coerceIn(0.1, 1.0)
    }
    
    fun setFloor(floor: Int) {
        _position.value = _position.value.copy(floor = floor)
    }
    
    fun reset() {
        x = 0.0
        y = 0.0
        totalDistance = 0.0
        stepCount = 0
        _position.value = Position(0.0, 0.0, 0, 1.0)
    }
}
```

### 12.6 Floor Plan Coordinate System

We need to convert between:
1. **Real-world coordinates** (meters from origin) — dead reckoning output
2. **Floor plan coordinates** (pixels on the loaded image)
3. **Screen coordinates** (pixels on the display, after zoom/pan transforms)

```kotlin
data class CoordinateTransform(
    val scaleMetersPerPixel: Double,  // How many real meters per floor plan pixel
    val originX: Double,              // Origin offset in floor plan pixels
    val originY: Double,
    val rotation: Double,             // Rotation angle if floor plan isn't north-aligned
) {
    fun metersToFloorPlan(mx: Double, my: Double): Pair<Double, Double> {
        val cos = cos(rotation)
        val sin = sin(rotation)
        val rx = mx * cos - my * sin
        val ry = mx * sin + my * cos
        return Pair(
            originX + rx / scaleMetersPerPixel,
            originY + ry / scaleMetersPerPixel,
        )
    }
    
    fun floorPlanToMeters(px: Double, py: Double): Pair<Double, Double> {
        val dx = (px - originX) * scaleMetersPerPixel
        val dy = (py - originY) * scaleMetersPerPixel
        val cos = cos(-rotation)
        val sin = sin(-rotation)
        return Pair(
            dx * cos - dy * sin,
            dx * sin + dy * cos,
        )
    }
}
```

Calibration procedure:
1. User loads a floor plan image.
2. User taps two points on the floor plan and enters the real-world distance between them.
3. The app calculates `scaleMetersPerPixel = distance / pixelDistance`.
4. User optionally sets the north direction by rotating the floor plan.

---

## 13. Heatmap Generation Engine

### 13.1 Architecture

```kotlin
class HeatmapEngine @Inject constructor() {
    
    /**
     * Generate a heatmap from survey measurements.
     *
     * @param measurements List of (x, y, rssi) triplets in floor plan coordinates
     * @param width Output width in pixels
     * @param height Output height in pixels
     * @param config Rendering configuration
     * @return HeatmapResult containing tiled bitmaps and metadata
     */
    suspend fun generate(
        measurements: List<MeasurementSample>,
        width: Int,
        height: Int,
        config: HeatmapConfig,
    ): HeatmapResult = withContext(Dispatchers.Default) {
        
        // Step 1: Create the interpolation grid
        val grid = DoubleArray(width * height) { Double.NaN }
        
        // Step 2: Place known measurements on the grid
        for (m in measurements) {
            val px = m.x.roundToInt()
            val py = m.y.roundToInt()
            if (px in 0 until width && py in 0 until height) {
                grid[py * width + px] = m.rssi
            }
        }
        
        // Step 3: Interpolate unknown cells
        val interpolated = when (config.algorithm) {
            InterpolationAlgorithm.IDW -> idwInterpolate(measurements, width, height, config.idwPower)
            InterpolationAlgorithm.KRIGING -> krigingInterpolate(measurements, width, height)
            InterpolationAlgorithm.RBF -> rbfInterpolate(measurements, width, height, config.rbfEpsilon)
            InterpolationAlgorithm.NEAREST -> nearestNeighborInterpolate(measurements, width, height)
        }
        
        // Step 4: Apply Gaussian smoothing
        val smoothed = if (config.smoothingRadius > 0) {
            gaussianBlur(interpolated, width, height, config.smoothingRadius)
        } else {
            interpolated
        }
        
        // Step 5: Clip to floor plan mask (if provided)
        val masked = config.floorPlanMask?.let { mask ->
            applyMask(smoothed, mask, width, height)
        } ?: smoothed
        
        // Step 6: Map values to colors
        val pixels = IntArray(width * height)
        for (i in masked.indices) {
            pixels[i] = if (masked[i].isNaN()) {
                Color.TRANSPARENT
            } else {
                rssiToColor(masked[i], config.colorScheme, config.minRssi, config.maxRssi)
            }
        }
        
        // Step 7: Create tiled bitmaps
        val tiles = createTiles(pixels, width, height, config.tileSize)
        
        HeatmapResult(
            tiles = tiles,
            width = width,
            height = height,
            minRssi = masked.filter { !it.isNaN() }.minOrNull() ?: -100.0,
            maxRssi = masked.filter { !it.isNaN() }.maxOrNull() ?: -30.0,
            measurementCount = measurements.size,
            algorithm = config.algorithm,
        )
    }
}
```

### 13.2 Heatmap Configuration

```kotlin
data class HeatmapConfig(
    val algorithm: InterpolationAlgorithm = InterpolationAlgorithm.IDW,
    val idwPower: Double = 2.0,                          // IDW exponent (higher = more local)
    val rbfEpsilon: Double = 1.0,                        // RBF shape parameter
    val smoothingRadius: Int = 5,                        // Gaussian blur radius in pixels
    val tileSize: Int = 512,                             // Tile size for memory efficiency
    val colorScheme: ColorScheme = ColorScheme.THERMAL,  // Color gradient
    val minRssi: Double = -90.0,                         // dBm floor for color mapping
    val maxRssi: Double = -30.0,                         // dBm ceiling for color mapping
    val alpha: Int = 180,                                // Heatmap overlay alpha (0-255)
    val floorPlanMask: BooleanArray? = null,             // Mask: true = draw, false = skip
    val resolution: Float = 1.0f,                        // Downscale factor (0.5 = half resolution)
)

enum class InterpolationAlgorithm {
    IDW,       // Inverse Distance Weighting — fast, good default
    KRIGING,   // Kriging — statistically optimal, slower
    RBF,       // Radial Basis Functions — smooth, balanced
    NEAREST,   // Nearest neighbor — fastest, blocky
}

enum class ColorScheme {
    THERMAL,   // Red (strong) → Yellow → Green → Blue (weak)
    VIRIDIS,   // Perceptually uniform, colorblind-friendly
    MAGMA,     // Dark-to-bright thermal
    GRAYSCALE, // White (strong) → Black (weak)
}
```

### 13.3 Color Gradient Implementation

```kotlin
object HeatmapColors {
    
    // Thermal gradient: 256 pre-computed ARGB colors from strong → weak
    // Index 0 = strongest signal, Index 255 = weakest signal
    private val thermalGradient: IntArray by lazy {
        val colors = IntArray(256)
        val keyPoints = listOf(
            0.00f to Color(0xFF, 0x00, 0x00),  // Red (strongest)
            0.15f to Color(0xFF, 0x66, 0x00),  // Orange-red
            0.30f to Color(0xFF, 0xCC, 0x00),  // Orange-yellow
            0.45f to Color(0xFF, 0xFF, 0x00),  // Yellow
            0.60f to Color(0x66, 0xFF, 0x00),  // Yellow-green
            0.75f to Color(0x00, 0xCC, 0x66),  // Green-cyan
            0.85f to Color(0x00, 0x66, 0xFF),  // Cyan-blue
            1.00f to Color(0x00, 0x00, 0xCC),  // Blue (weakest)
        )
        
        for (i in 0 until 256) {
            val t = i / 255f
            // Find surrounding key points and interpolate
            val (lower, upper) = findSurroundingKeyPoints(t, keyPoints)
            val localT = (t - lower.first) / (upper.first - lower.first)
            colors[i] = lerpColor(lower.second, upper.second, localT)
        }
        colors
    }
    
    fun rssiToColor(rssi: Double, scheme: ColorScheme, minRssi: Double, maxRssi: Double, alpha: Int = 180): Int {
        val normalized = ((rssi - minRssi) / (maxRssi - minRssi)).coerceIn(0.0, 1.0)
        val index = ((1.0 - normalized) * 255).roundToInt()  // Invert: 0 = strong, 255 = weak
        
        val gradient = when (scheme) {
            ColorScheme.THERMAL -> thermalGradient
            ColorScheme.VIRIDIS -> viridisGradient
            ColorScheme.MAGMA -> magmaGradient
            ColorScheme.GRAYSCALE -> grayscaleGradient
        }
        
        val rgb = gradient[index]
        return (alpha shl 24) or (rgb and 0x00FFFFFF)
    }
}
```

---

## 14. Interpolation Algorithms

### 14.1 Inverse Distance Weighting (IDW)

IDW is the default algorithm. It's fast, intuitive, and produces acceptable results for most surveys.

```kotlin
/**
 * Inverse Distance Weighting interpolation.
 *
 * For each unknown point P, the estimated value is a weighted average of
 * all known measurements, where the weight of each measurement is inversely
 * proportional to its distance from P raised to the power p.
 *
 * Formula:
 *   z(P) = Σ(wᵢ × zᵢ) / Σ(wᵢ)
 *   where wᵢ = 1 / d(P, Pᵢ)^p
 *
 * Properties:
 * - Exact: passes through known data points (if grid point coincides with measurement)
 * - p=1: linear decay → smooth, gradual transitions (may over-smooth)
 * - p=2: quadratic decay → good balance (default)
 * - p=3+: sharper decay → more local influence, can create "bull's-eye" artifacts
 *
 * Complexity: O(W × H × N) where N = number of measurements
 * For 1080×2400 grid with 200 measurements: ~518M operations
 * Optimization: use search radius to limit N per pixel.
 */
fun idwInterpolate(
    measurements: List<MeasurementSample>,
    width: Int,
    height: Int,
    power: Double = 2.0,
    searchRadius: Double = Double.MAX_VALUE,  // Pixels. Use MAX_VALUE for global IDW.
): DoubleArray {
    val grid = DoubleArray(width * height) { Double.NaN }
    
    // Build KD-tree for efficient spatial queries if we have many measurements
    // For < 500 measurements, brute-force is fast enough
    
    for (y in 0 until height) {
        for (x in 0 until width) {
            var weightedSum = 0.0
            var weightSum = 0.0
            var exactMatch = false
            
            for (m in measurements) {
                val dx = x - m.x
                val dy = y - m.y
                val dist = sqrt(dx * dx + dy * dy)
                
                if (dist < 0.5) {
                    // Grid point coincides with measurement — use exact value
                    grid[y * width + x] = m.rssi
                    exactMatch = true
                    break
                }
                
                if (dist > searchRadius) continue
                
                val weight = 1.0 / dist.pow(power)
                weightedSum += weight * m.rssi
                weightSum += weight
            }
            
            if (!exactMatch && weightSum > 0) {
                grid[y * width + x] = weightedSum / weightSum
            }
        }
    }
    
    return grid
}
```

### 14.2 Ordinary Kriging

Kriging is the gold standard for spatial interpolation. It's statistically optimal (BLUE — Best Linear Unbiased Estimator) and provides confidence estimates.

```kotlin
/**
 * Ordinary Kriging interpolation.
 *
 * Unlike IDW, Kriging considers the spatial correlation structure of the data
 * (the semivariogram) and produces minimum-variance predictions.
 *
 * Steps:
 * 1. Estimate the semivariogram from measurement pairs
 * 2. Fit a variogram model (spherical, exponential, or Gaussian)
 * 3. For each unknown point, solve the Kriging system to get weights
 * 4. Predict as a weighted average using the Kriging weights
 *
 * Complexity: O(N³) for the matrix solve per prediction point
 * Practical: Use a moving neighborhood of the nearest ~20 measurements.
 */
class KrigingInterpolator(
    private val variogramModel: VariogramModel = VariogramModel.SPHERICAL,
) {
    // Variogram parameters (estimated from data)
    private var nugget: Double = 0.0    // Micro-scale variation / measurement noise
    private var sill: Double = 1.0      // Total variance
    private var range: Double = 100.0   // Distance at which spatial correlation vanishes
    
    /**
     * Experimental semivariogram estimation.
     * 
     * γ(h) = (1/2N(h)) × Σ|z(xᵢ) - z(xⱼ)|²
     * where N(h) = number of pairs with distance ~h
     *
     * We bin pairs into distance classes and compute the average semivariance per bin.
     */
    fun fitVariogram(measurements: List<MeasurementSample>) {
        val n = measurements.size
        val pairs = mutableListOf<Pair<Double, Double>>()  // (distance, squared_diff)
        
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = measurements[i].x - measurements[j].x
                val dy = measurements[i].y - measurements[j].y
                val dist = sqrt(dx * dx + dy * dy)
                val sqDiff = (measurements[i].rssi - measurements[j].rssi).pow(2)
                pairs.add(dist to sqDiff)
            }
        }
        
        // Bin into distance classes
        val maxDist = pairs.maxOfOrNull { it.first } ?: return
        val numBins = 15
        val binWidth = maxDist / numBins
        
        val bins = Array(numBins) { mutableListOf<Double>() }
        for ((dist, sqDiff) in pairs) {
            val binIdx = ((dist / binWidth).toInt()).coerceIn(0, numBins - 1)
            bins[binIdx].add(sqDiff)
        }
        
        // Compute experimental variogram values
        val expVariogram = bins.mapIndexed { i, values ->
            val h = (i + 0.5) * binWidth
            val gamma = if (values.isNotEmpty()) values.average() / 2.0 else Double.NaN
            h to gamma
        }.filter { !it.second.isNaN() }
        
        // Fit variogram model (least squares)
        fitVariogramModel(expVariogram)
    }
    
    /**
     * Spherical variogram model:
     * γ(h) = nugget + (sill - nugget) × [1.5(h/range) - 0.5(h/range)³]  if h ≤ range
     * γ(h) = sill                                                          if h > range
     */
    fun variogram(h: Double): Double = when (variogramModel) {
        VariogramModel.SPHERICAL -> {
            if (h <= 0) 0.0
            else if (h >= range) sill
            else {
                val ratio = h / range
                nugget + (sill - nugget) * (1.5 * ratio - 0.5 * ratio.pow(3))
            }
        }
        VariogramModel.EXPONENTIAL -> {
            nugget + (sill - nugget) * (1 - exp(-3 * h / range))
        }
        VariogramModel.GAUSSIAN -> {
            nugget + (sill - nugget) * (1 - exp(-3 * (h / range).pow(2)))
        }
    }
    
    /**
     * Solve the Kriging system for one prediction point.
     *
     * The Kriging system:
     * [Γ  1] [λ]   [γ₀]
     * [1' 0] [μ] = [1 ]
     *
     * Where:
     *   Γ = N×N matrix of semivariogram values between measurement pairs
     *   γ₀ = N×1 vector of semivariogram values between measurement points and prediction point
     *   λ = Kriging weights
     *   μ = Lagrange multiplier (for unbiasedness constraint)
     */
    fun predict(
        measurements: List<MeasurementSample>,
        predX: Double,
        predY: Double,
    ): KrigingPrediction {
        val n = measurements.size
        
        // Build Kriging matrix (N+1 × N+1)
        val matrix = Array(n + 1) { DoubleArray(n + 1) }
        val rhs = DoubleArray(n + 1)
        
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dx = measurements[i].x - measurements[j].x
                val dy = measurements[i].y - measurements[j].y
                matrix[i][j] = variogram(sqrt(dx * dx + dy * dy))
            }
            matrix[i][n] = 1.0
            matrix[n][i] = 1.0
            
            val dxp = measurements[i].x - predX
            val dyp = measurements[i].y - predY
            rhs[i] = variogram(sqrt(dxp * dxp + dyp * dyp))
        }
        matrix[n][n] = 0.0
        rhs[n] = 1.0
        
        // Solve the system using LU decomposition
        val solution = solveLU(matrix, rhs)
        
        // Prediction = weighted average
        var prediction = 0.0
        var variance = 0.0
        for (i in 0 until n) {
            prediction += solution[i] * measurements[i].rssi
            variance += solution[i] * rhs[i]
        }
        variance += solution[n]  // Add Lagrange multiplier contribution
        
        return KrigingPrediction(
            value = prediction,
            variance = variance.coerceAtLeast(0.0),
            standardError = sqrt(variance.coerceAtLeast(0.0)),
        )
    }
}

data class KrigingPrediction(
    val value: Double,
    val variance: Double,
    val standardError: Double,
)
```

### 14.3 Gaussian Smoothing

Applied after interpolation for visual appeal:

```kotlin
/**
 * 2D Gaussian blur using separable convolution.
 * 
 * Complexity: O(W × H × R) instead of O(W × H × R²) for 2D kernel.
 * For 1080×2400 with radius 5: ~25M operations.
 */
fun gaussianBlur(
    input: DoubleArray,
    width: Int,
    height: Int,
    radius: Int,
): DoubleArray {
    val sigma = radius / 3.0
    val kernelSize = radius * 2 + 1
    val kernel = DoubleArray(kernelSize) { i ->
        val x = i - radius
        exp(-x * x / (2 * sigma * sigma))
    }
    val kernelSum = kernel.sum()
    for (i in kernel.indices) kernel[i] /= kernelSum
    
    // Horizontal pass
    val horizontal = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0.0
            var wsum = 0.0
            for (k in -radius..radius) {
                val nx = (x + k).coerceIn(0, width - 1)
                val v = input[y * width + nx]
                if (!v.isNaN()) {
                    sum += v * kernel[k + radius]
                    wsum += kernel[k + radius]
                }
            }
            horizontal[y * width + x] = if (wsum > 0) sum / wsum else Double.NaN
        }
    }
    
    // Vertical pass
    val output = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0.0
            var wsum = 0.0
            for (k in -radius..radius) {
                val ny = (y + k).coerceIn(0, height - 1)
                val v = horizontal[ny * width + x]
                if (!v.isNaN()) {
                    sum += v * kernel[k + radius]
                    wsum += kernel[k + radius]
                }
            }
            output[y * width + x] = if (wsum > 0) sum / wsum else Double.NaN
        }
    }
    
    return output
}
```

---

## 15. Path Loss Modeling

### 15.1 Log-Distance Path Loss Model

Used to estimate AP distance from RSSI and to predict coverage in unmeasured areas:

```kotlin
/**
 * Log-Distance Path Loss Model:
 * 
 * PL(d) = PL(d₀) + 10 × n × log₁₀(d/d₀) + Xσ
 * 
 * Where:
 *   PL(d)  = Path loss at distance d (dB)
 *   PL(d₀) = Reference path loss at d₀ (typically 1 meter)
 *   n      = Path loss exponent (environment-dependent)
 *   d₀     = Reference distance (1 meter)
 *   Xσ     = Shadow fading (log-normal, zero mean, σ standard deviation)
 *
 * Equivalently, for RSSI:
 *   RSSI(d) = RSSI(d₀) - 10 × n × log₁₀(d/d₀)
 *
 * Typical path loss exponents:
 *   Free space:        n = 2.0
 *   Open office:       n = 2.2-2.8
 *   Residential:       n = 2.8-3.5
 *   Dense office:      n = 3.0-4.0
 *   Through 1 wall:    n = 3.0-4.0
 *   Through 2+ walls:  n = 4.0-6.0
 */
object PathLossModel {
    
    // Reference RSSI at 1 meter (free-space, typical AP transmit power)
    // 2.4 GHz: ~ -40 dBm at 1m
    // 5 GHz:   ~ -47 dBm at 1m (higher frequency = higher free-space loss)
    fun referenceRssi(frequencyMhz: Int): Double {
        // Free Space Path Loss at 1m: FSPL = 20×log₁₀(f) + 20×log₁₀(d) - 147.55
        // At 1m: FSPL = 20×log₁₀(f) - 147.55 + 20×log₁₀(0.001 km)
        val fspl1m = 20 * log10(frequencyMhz.toDouble()) + 20 * log10(0.001) - 147.55
        // Typical AP transmit power: 20 dBm (100 mW)
        // RSSI at 1m = TxPower - FSPL - cable/antenna losses
        return 20.0 - fspl1m - 3.0  // 3 dB for typical losses
    }
    
    /**
     * Estimate distance from RSSI.
     * d = d₀ × 10^((RSSI(d₀) - RSSI) / (10 × n))
     */
    fun estimateDistance(
        rssi: Double,
        frequencyMhz: Int,
        pathLossExponent: Double = 3.0,  // Default: indoor residential
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        val distance = 10.0.pow((refRssi - rssi) / (10 * pathLossExponent))
        return distance.coerceIn(0.1, 200.0)  // Clamp to reasonable range
    }
    
    /**
     * Predict RSSI at a given distance.
     * RSSI(d) = RSSI(d₀) - 10 × n × log₁₀(d/d₀)
     */
    fun predictRssi(
        distance: Double,
        frequencyMhz: Int,
        pathLossExponent: Double = 3.0,
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        return refRssi - 10 * pathLossExponent * log10(distance.coerceAtLeast(0.1))
    }
    
    /**
     * Calibrate the path loss exponent from survey data.
     * 
     * Given a set of (distance, RSSI) measurements for a single AP,
     * find the n that minimizes the sum of squared errors.
     *
     * n = Σ(RSSI(d₀) - RSSIᵢ) / (10 × Σlog₁₀(dᵢ))
     */
    fun calibrateExponent(
        measurements: List<Pair<Double, Double>>,  // (distance_meters, rssi)
        frequencyMhz: Int,
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        var numerator = 0.0
        var denominator = 0.0
        
        for ((dist, rssi) in measurements) {
            if (dist > 0.1) {
                numerator += (refRssi - rssi)
                denominator += 10 * log10(dist)
            }
        }
        
        return if (denominator > 0) (numerator / denominator).coerceIn(1.5, 6.0) else 3.0
    }
}
```

### 15.2 Wall Attenuation Model

For more accurate predictions, model wall attenuation:

```kotlin
/**
 * Wall attenuation adds a fixed dB loss per wall crossing.
 * 
 * Typical attenuation values:
 *   Drywall (interior):     3-5 dB
 *   Concrete/brick:         10-15 dB
 *   Glass (clear):          2-3 dB
 *   Glass (tinted/coated):  5-8 dB
 *   Metal/foil:             15-25 dB
 *   Floor/ceiling:          10-20 dB
 *   Wood door:              3-5 dB
 *   Metal door:             10-15 dB
 *
 * These values are at 2.4 GHz. At 5 GHz, multiply by ~1.3-1.5.
 */
data class WallSegment(
    val x1: Double, val y1: Double,  // Start point (floor plan coordinates)
    val x2: Double, val y2: Double,  // End point
    val material: WallMaterial,
    val attenuation: Double,         // dB loss per crossing
)

enum class WallMaterial(val defaultAttenuation24: Double, val defaultAttenuation5: Double) {
    DRYWALL(4.0, 5.5),
    CONCRETE(12.0, 16.0),
    BRICK(10.0, 14.0),
    GLASS(2.5, 3.5),
    METAL(20.0, 25.0),
    WOOD(3.5, 5.0),
}
```

---

# PART IV: DATA LAYER

---

## 16. Database Schema

### 16.1 Entity-Relationship Diagram (Textual)

```
surveys
  ├── 1:N → measurement_points
  │             ├── 1:N → ap_measurements
  │             └── position (x, y, floor)
  └── 1:1 → floor_plans
                └── calibration data

access_points (deduplicated by BSSID)
  └── referenced by ap_measurements.bssid

channel_snapshots (time-series channel data)
```

### 16.2 Room Entity Definitions

```kotlin
@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val floorPlanId: Long?,
    val createdAt: Long,       // epoch millis
    val completedAt: Long?,
    val status: String,        // ACTIVE, COMPLETED, ARCHIVED
    val totalPoints: Int,
    val totalAps: Int,
    val durationMs: Long,
    val notes: String?,
)

@Entity(
    tableName = "measurement_points",
    foreignKeys = [ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["surveyId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("surveyId")],
)
data class MeasurementPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val x: Double,             // Floor plan coordinate (pixels)
    val y: Double,
    val floor: Int,
    val positionConfidence: Double,
    val timestamp: Long,       // epoch millis
    val scanTimestamp: Long,   // ScanResult.timestamp (microseconds since boot)
)

@Entity(
    tableName = "ap_measurements",
    foreignKeys = [ForeignKey(
        entity = MeasurementPointEntity::class,
        parentColumns = ["id"],
        childColumns = ["measurementPointId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("measurementPointId"), Index("bssid")],
)
data class ApMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measurementPointId: Long,
    val bssid: String,
    val ssid: String,
    val rssi: Int,             // Raw RSSI in dBm
    val smoothedRssi: Double,  // Kalman-filtered
    val frequency: Int,        // MHz
    val channel: Int,
    val channelWidth: String,  // "20", "40", "80", "160"
    val band: String,          // "2.4" or "5"
    val security: String,      // "OPEN", "WEP", "WPA", "WPA2", "WPA3"
    val standard: String,      // "a", "b", "g", "n", "ac"
    val capabilities: String,  // Raw capabilities string
)

@Entity(tableName = "floor_plans")
data class FloorPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String,     // Relative path in app's files directory
    val imageWidth: Int,
    val imageHeight: Int,
    val scaleMetersPerPixel: Double,
    val originX: Double,
    val originY: Double,
    val rotation: Double,      // Radians
    val floors: Int,           // Number of floors this plan covers
    val createdAt: Long,
)

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey val bssid: String,
    val ssid: String,
    val vendor: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val bestRssi: Int,
    val primaryFrequency: Int,
    val primaryChannel: Int,
    val security: String,
    val notes: String?,
    val isHidden: Boolean,
    val isFavorite: Boolean,
)

@Entity(
    tableName = "wall_segments",
    foreignKeys = [ForeignKey(
        entity = FloorPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["floorPlanId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class WallSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val floorPlanId: Long,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val material: String,
    val attenuation: Double,
    val floor: Int,
)
```

### 16.3 Database Class

```kotlin
@Database(
    entities = [
        SurveyEntity::class,
        MeasurementPointEntity::class,
        ApMeasurementEntity::class,
        FloorPlanEntity::class,
        AccessPointEntity::class,
        WallSegmentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun floorPlanDao(): FloorPlanDao
    abstract fun accessPointDao(): AccessPointDao
    abstract fun wallSegmentDao(): WallSegmentDao
}
```

---

## 17. Data Models & Entities

### 17.1 Domain Models (core-model module, pure Kotlin)

```kotlin
// Survey
data class Survey(
    val id: Long,
    val name: String,
    val description: String?,
    val floorPlan: FloorPlan?,
    val status: SurveyStatus,
    val createdAt: Instant,
    val completedAt: Instant?,
    val totalPoints: Int,
    val totalAps: Int,
    val duration: Duration,
)

enum class SurveyStatus { ACTIVE, COMPLETED, ARCHIVED }

// Measurement
data class MeasurementPoint(
    val id: Long,
    val position: Position,
    val timestamp: Instant,
    val apMeasurements: List<ApMeasurement>,
)

data class Position(
    val x: Double,
    val y: Double,
    val floor: Int,
    val confidence: Double,
)

data class ApMeasurement(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val smoothedRssi: Double,
    val frequency: Int,
    val channel: Int,
    val channelWidth: ChannelWidth,
    val band: WifiBand,
    val security: SecurityType,
)

// Enums
enum class WifiBand { BAND_2_4_GHZ, BAND_5_GHZ }
enum class ChannelWidth { MHZ_20, MHZ_40, MHZ_80, MHZ_160 }
enum class SecurityType { OPEN, WEP, WPA_PSK, WPA2_PSK, WPA3_SAE, WPA_EAP, WPA2_EAP, UNKNOWN }
enum class WifiStandard { LEGACY, WIFI_4_N, WIFI_5_AC }

// Floor Plan
data class FloorPlan(
    val id: Long,
    val name: String,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val transform: CoordinateTransform,
    val floors: Int,
)

// Heatmap
data class MeasurementSample(
    val x: Double,
    val y: Double,
    val rssi: Double,
)

data class HeatmapResult(
    val tiles: List<BitmapTile>,
    val width: Int,
    val height: Int,
    val minRssi: Double,
    val maxRssi: Double,
    val measurementCount: Int,
    val algorithm: InterpolationAlgorithm,
)

data class BitmapTile(
    val bitmap: Bitmap,
    val x: Int,      // Tile position in tile grid
    val y: Int,
    val width: Int,   // Tile size in pixels
    val height: Int,
)
```

---

## 18. Repository Pattern & DAOs

### 18.1 DAO Interfaces

```kotlin
@Dao
interface SurveyDao {
    @Query("SELECT * FROM surveys ORDER BY createdAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>
    
    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getSurveyById(id: Long): SurveyEntity?
    
    @Query("SELECT * FROM surveys WHERE status = :status ORDER BY createdAt DESC")
    fun getSurveysByStatus(status: String): Flow<List<SurveyEntity>>
    
    @Insert
    suspend fun insertSurvey(survey: SurveyEntity): Long
    
    @Update
    suspend fun updateSurvey(survey: SurveyEntity)
    
    @Delete
    suspend fun deleteSurvey(survey: SurveyEntity)
    
    @Query("UPDATE surveys SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun completeSurvey(id: Long, status: String, completedAt: Long)
}

@Dao
interface MeasurementDao {
    @Query("""
        SELECT * FROM measurement_points 
        WHERE surveyId = :surveyId 
        ORDER BY timestamp ASC
    """)
    fun getMeasurementPoints(surveyId: Long): Flow<List<MeasurementPointEntity>>
    
    @Query("""
        SELECT * FROM ap_measurements 
        WHERE measurementPointId = :pointId
    """)
    suspend fun getApMeasurements(pointId: Long): List<ApMeasurementEntity>
    
    @Transaction
    @Query("""
        SELECT * FROM measurement_points 
        WHERE surveyId = :surveyId
    """)
    fun getMeasurementsWithAps(surveyId: Long): Flow<List<MeasurementWithAps>>
    
    @Insert
    suspend fun insertMeasurementPoint(point: MeasurementPointEntity): Long
    
    @Insert
    suspend fun insertApMeasurements(measurements: List<ApMeasurementEntity>)
    
    @Transaction
    suspend fun insertMeasurementWithAps(
        point: MeasurementPointEntity,
        apMeasurements: List<ApMeasurementEntity>,
    ) {
        val pointId = insertMeasurementPoint(point)
        insertApMeasurements(apMeasurements.map { it.copy(measurementPointId = pointId) })
    }
    
    @Query("DELETE FROM measurement_points WHERE id = :id")
    suspend fun deleteMeasurementPoint(id: Long)
    
    @Query("""
        SELECT DISTINCT bssid, ssid, 
               MIN(rssi) as minRssi, MAX(rssi) as maxRssi, AVG(rssi) as avgRssi,
               COUNT(*) as measurementCount
        FROM ap_measurements 
        WHERE measurementPointId IN (SELECT id FROM measurement_points WHERE surveyId = :surveyId)
        GROUP BY bssid
    """)
    suspend fun getApSummary(surveyId: Long): List<ApSummaryProjection>
    
    // For heatmap generation: get (x, y, rssi) for a specific BSSID
    @Query("""
        SELECT mp.x, mp.y, am.smoothedRssi as rssi
        FROM measurement_points mp
        INNER JOIN ap_measurements am ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId AND am.bssid = :bssid
    """)
    suspend fun getMeasurementSamples(surveyId: Long, bssid: String): List<MeasurementSampleProjection>
    
    // For band-level heatmap: get best RSSI per point for a band
    @Query("""
        SELECT mp.x, mp.y, MAX(am.smoothedRssi) as rssi
        FROM measurement_points mp
        INNER JOIN ap_measurements am ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId AND am.band = :band
        GROUP BY mp.id
    """)
    suspend fun getBandMeasurementSamples(surveyId: Long, band: String): List<MeasurementSampleProjection>
}

data class MeasurementSampleProjection(
    val x: Double,
    val y: Double,
    val rssi: Double,
)

data class ApSummaryProjection(
    val bssid: String,
    val ssid: String,
    val minRssi: Int,
    val maxRssi: Int,
    val avgRssi: Double,
    val measurementCount: Int,
)

data class MeasurementWithAps(
    @Embedded val point: MeasurementPointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "measurementPointId",
    )
    val apMeasurements: List<ApMeasurementEntity>,
)
```

### 18.2 Repository Interfaces (Domain Layer)

```kotlin
interface SurveyRepository {
    fun getAllSurveys(): Flow<List<Survey>>
    fun getSurveysByStatus(status: SurveyStatus): Flow<List<Survey>>
    suspend fun getSurveyById(id: Long): Survey?
    suspend fun createSurvey(name: String, description: String?, floorPlanId: Long?): Long
    suspend fun completeSurvey(id: Long)
    suspend fun deleteSurvey(id: Long)
}

interface MeasurementRepository {
    fun getMeasurements(surveyId: Long): Flow<List<MeasurementPoint>>
    suspend fun addMeasurement(surveyId: Long, point: MeasurementPoint)
    suspend fun deleteMeasurement(pointId: Long)
    suspend fun getMeasurementSamples(surveyId: Long, bssid: String): List<MeasurementSample>
    suspend fun getBandMeasurementSamples(surveyId: Long, band: WifiBand): List<MeasurementSample>
    suspend fun getApSummary(surveyId: Long): List<ApSummary>
}

interface FloorPlanRepository {
    fun getAllFloorPlans(): Flow<List<FloorPlan>>
    suspend fun getFloorPlan(id: Long): FloorPlan?
    suspend fun saveFloorPlan(name: String, imageUri: Uri, transform: CoordinateTransform): Long
    suspend fun deleteFloorPlan(id: Long)
    suspend fun updateTransform(id: Long, transform: CoordinateTransform)
}
```

---

## 19. Data Migration Strategy

Since this is v1, no migrations yet. But the schema is designed to be extensible:

```kotlin
// Future migrations:
// v1 → v2: Add "channel_snapshots" table for time-series data
// v1 → v2: Add "ap_positions" table for estimated AP locations
// v1 → v2: Add "survey_zones" table for named zones within a floor plan

// Room migration template:
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS channel_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                surveyId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                channel INTEGER NOT NULL,
                band TEXT NOT NULL,
                utilizationPercent REAL NOT NULL,
                noiseFloor INTEGER NOT NULL,
                apCount INTEGER NOT NULL,
                FOREIGN KEY(surveyId) REFERENCES surveys(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_channel_snapshots_surveyId ON channel_snapshots(surveyId)")
    }
}
```

---

# PART V: UI/UX SPECIFICATION

---

## 20. Screen Map & Navigation

### 20.1 Navigation Graph

```
                    ┌─────────────┐
                    │   Splash    │  (only on cold start, < 500ms)
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
              ┌─────│  Dashboard  │─────┐
              │     └──────┬──────┘     │
              │            │            │
     ┌────────▼──┐  ┌──────▼──────┐  ┌──▼────────┐
     │ Survey    │  │  Floor Plan │  │  Settings  │
     │ List      │  │  Manager    │  │            │
     └────┬──────┘  └──────┬──────┘  └────────────┘
          │                │
     ┌────▼──────┐  ┌──────▼──────┐
     │ Active    │  │ Floor Plan  │
     │ Survey    │  │ Calibration │
     └────┬──────┘  └─────────────┘
          │
     ┌────▼──────┐
     │ Survey    │──────┐
     │ Results   │      │
     └────┬──────┘      │
          │        ┌────▼──────┐
     ┌────▼──────┐ │ Channel   │
     │ Heatmap   │ │ Analysis  │
     │ Viewer    │ │           │
     └────┬──────┘ └───────────┘
          │
     ┌────▼──────┐
     │  Export   │
     │           │
     └───────────┘
```

### 20.2 Navigation Routes

```kotlin
sealed class Route(val route: String) {
    data object Dashboard : Route("dashboard")
    data object SurveyList : Route("surveys")
    data object FloorPlanManager : Route("floor-plans")
    data object Settings : Route("settings")
    
    data class ActiveSurvey(val surveyId: Long) : Route("survey/{surveyId}") {
        companion object { const val pattern = "survey/{surveyId}" }
    }
    data class SurveyResults(val surveyId: Long) : Route("survey/{surveyId}/results") {
        companion object { const val pattern = "survey/{surveyId}/results" }
    }
    data class HeatmapViewer(val surveyId: Long) : Route("survey/{surveyId}/heatmap") {
        companion object { const val pattern = "survey/{surveyId}/heatmap" }
    }
    data class ChannelAnalysis(val surveyId: Long) : Route("survey/{surveyId}/channels") {
        companion object { const val pattern = "survey/{surveyId}/channels" }
    }
    data class FloorPlanCalibration(val floorPlanId: Long) : Route("floor-plan/{floorPlanId}/calibrate") {
        companion object { const val pattern = "floor-plan/{floorPlanId}/calibrate" }
    }
    data class Export(val surveyId: Long) : Route("survey/{surveyId}/export") {
        companion object { const val pattern = "survey/{surveyId}/export" }
    }
}
```

---

## 21. Screen Specifications

### 21.1 Dashboard Screen

**Purpose**: Main entry point. Shows quick status and recent activity.

**Layout**:
```
┌───────────────────────────────────────┐
│  WiFi Thermal Scanner      [≡] [⚙]  │  ← Top bar with settings
├───────────────────────────────────────┤
│ ┌─────────────────────────────────┐   │
│ │  CURRENT SIGNAL                 │   │
│ │  Connected: HomeNetwork-5G      │   │
│ │  RSSI: -45 dBm  ████████░░     │   │
│ │  Channel: 36  Band: 5 GHz      │   │
│ │  Quality: EXCELLENT             │   │
│ └─────────────────────────────────┘   │
│                                       │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│ │ Visible │ │ 2.4 GHz │ │  5 GHz  │  │
│ │   23    │ │   15    │ │    8    │  │
│ │   APs   │ │   APs   │ │   APs   │  │
│ └─────────┘ └─────────┘ └─────────┘  │
│                                       │
│  RECENT SURVEYS                       │
│  ┌─────────────────────────────────┐  │
│  │ Office Floor 2  ·  Yesterday   │  │
│  │ 145 points · 32 APs · Good     │  │
│  ├─────────────────────────────────┤  │
│  │ Home Ground Floor · 3 days ago │  │
│  │ 78 points · 12 APs · Fair      │  │
│  └─────────────────────────────────┘  │
│                                       │
│  ┌─────────────────────────────────┐  │
│  │         START NEW SURVEY        │  │  ← Primary action
│  └─────────────────────────────────┘  │
│                                       │
│  [Dashboard] [Surveys] [APs] [More]  │  ← Bottom nav
└───────────────────────────────────────┘
```

### 21.2 Active Survey Screen

**Purpose**: The core workflow. User walks a space collecting measurements.

**Layout**:
```
┌───────────────────────────────────────┐
│  ← Back   Office Survey   [⋮] menu  │
├───────────────────────────────────────┤
│                                       │
│  ┌─────────────────────────────────┐  │
│  │                                 │  │
│  │      FLOOR PLAN IMAGE           │  │
│  │      (pinch to zoom, pan)       │  │
│  │                                 │  │
│  │      ● ← measurement points    │  │
│  │      ● ●                        │  │
│  │          ●                      │  │
│  │    ◎ ← current position cursor │  │
│  │                                 │  │
│  │                                 │  │
│  └─────────────────────────────────┘  │
│                                       │
│  ┌───────────────┬─────────────────┐  │
│  │ Points: 45    │  APs: 28       │  │
│  │ Time: 12:34   │  Last: -52 dBm │  │
│  └───────────────┴─────────────────┘  │
│                                       │
│  [Correct Pos]  [██ COLLECT ██]  [⏸] │
│                                       │
│  Mode: [Point-by-Point] [Continuous] │
└───────────────────────────────────────┘
```

**Interaction details**:
- **Tap on floor plan**: Set/correct current position (dead reckoning anchor)
- **COLLECT button**: Trigger WiFi scan at current position. Button shows scan progress.
- **Correct Pos**: Manual position correction mode — next tap on floor plan sets position
- **Continuous mode**: Auto-collect as you walk (every N steps or every M seconds)
- **Measurement point colors**: Green (≥-50), Yellow (-50 to -70), Red (< -70), representing best signal at that point

### 21.3 Heatmap Viewer Screen

**Purpose**: Visualize signal coverage as a thermal overlay on the floor plan.

**Layout**:
```
┌───────────────────────────────────────┐
│  ← Back   Heatmap           [Share] │
├───────────────────────────────────────┤
│  AP: [▼ HomeNetwork-5G (all APs)  ]  │  ← AP selector dropdown
│  Band: [All] [2.4] [5]               │  ← Band filter
├───────────────────────────────────────┤
│                                       │
│  ┌─────────────────────────────────┐  │
│  │                                 │  │
│  │   FLOOR PLAN + HEATMAP OVERLAY  │  │
│  │   (thermal colors with alpha)   │  │
│  │                                 │  │
│  │   Red ← strong signal          │  │
│  │   Yellow                        │  │
│  │   Green                         │  │
│  │   Blue ← weak signal           │  │
│  │                                 │  │
│  └─────────────────────────────────┘  │
│                                       │
│  ┌─────────────────────────────────┐  │
│  │  Legend: -30 ██████████ -90 dBm │  │
│  └─────────────────────────────────┘  │
│                                       │
│  Algorithm: [IDW ▼]                   │
│  Smoothing: [═══●════] 5             │
│  Opacity:   [════●═══] 70%           │
│                                       │
│  [Regenerate]  [Compare]  [Export]   │
└───────────────────────────────────────┘
```

### 21.4 Channel Analysis Screen

**Purpose**: Visualize channel congestion and help select optimal channels.

**Layout**:
```
┌───────────────────────────────────────┐
│  ← Back   Channel Analysis           │
├───────────────────────────────────────┤
│  Band: [2.4 GHz ●] [5 GHz ○]        │
├───────────────────────────────────────┤
│                                       │
│  CHANNEL GRAPH (2.4 GHz)             │
│  Signal                               │
│  Strength                             │
│   -30│    ╱╲                          │
│   -40│   ╱  ╲     ╱╲                 │
│   -50│  ╱    ╲   ╱  ╲               │
│   -60│ ╱      ╲ ╱    ╲    ╱╲        │
│   -70│╱        ╳      ╲  ╱  ╲       │
│   -80│─────────────────────────      │
│      └──────────────────────────     │
│        1  2  3  4  5  6  7  8  9 10 11│
│                Channels               │
│                                       │
│  CHANNEL UTILIZATION                  │
│  Ch 1:  ████████░░░░  4 APs  -45 avg │
│  Ch 6:  ██████░░░░░░  3 APs  -52 avg │
│  Ch 11: ████████████  6 APs  -38 avg │
│                                       │
│  RECOMMENDATION                       │
│  ┌─────────────────────────────────┐  │
│  │ Best 2.4 GHz channel: 6        │  │
│  │ Reason: Lowest utilization,     │  │
│  │ fewest overlapping networks     │  │
│  └─────────────────────────────────┘  │
└───────────────────────────────────────┘
```

---

## 22. Jetpack Compose Components

### 22.1 Core Composables

```kotlin
// Zoomable, pannable floor plan with overlay support
@Composable
fun ZoomableFloorPlan(
    floorPlan: ImageBitmap,
    heatmapTiles: List<BitmapTile>?,
    measurementPoints: List<MeasurementPointUi>,
    currentPosition: Position?,
    onTap: (Offset) -> Unit,         // Floor plan coordinates
    onLongPress: (Offset) -> Unit,
    modifier: Modifier = Modifier,
)

// Real-time signal strength indicator
@Composable
fun SignalStrengthBar(
    rssi: Int,
    quality: SignalQuality,
    animated: Boolean = true,
    modifier: Modifier = Modifier,
)

// Channel utilization bar chart
@Composable
fun ChannelChart(
    channels: List<ChannelInfo>,
    band: WifiBand,
    selectedChannel: Int?,
    onChannelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
)

// AP list item with signal info
@Composable
fun AccessPointListItem(
    ap: ProcessedScanResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
)

// Heatmap legend (color gradient bar with dBm labels)
@Composable
fun HeatmapLegend(
    minRssi: Double,
    maxRssi: Double,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
)

// Survey statistics card
@Composable
fun SurveyStatsCard(
    pointCount: Int,
    apCount: Int,
    duration: Duration,
    avgSignal: Double,
    modifier: Modifier = Modifier,
)

// Calibration overlay for floor plan scale setting
@Composable
fun CalibrationOverlay(
    point1: Offset?,
    point2: Offset?,
    distance: Double?,
    onSetPoint: (Offset) -> Unit,
    onSetDistance: (Double) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 22.2 ZoomableFloorPlan Implementation Sketch

This is the most complex Composable — handles multi-touch zoom, pan, rotation, and renders the floor plan with heatmap overlay:

```kotlin
@Composable
fun ZoomableFloorPlan(
    floorPlan: ImageBitmap,
    heatmapTiles: List<BitmapTile>?,
    measurementPoints: List<MeasurementPointUi>,
    currentPosition: Position?,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(0.5f, 10f)
        offset += panChange
        rotation += rotationChange
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .transformable(transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        // Convert screen coordinates → floor plan coordinates
                        val fpCoord = screenToFloorPlan(screenOffset, scale, offset, rotation)
                        onTap(fpCoord)
                    },
                    onLongPress = { screenOffset ->
                        val fpCoord = screenToFloorPlan(screenOffset, scale, offset, rotation)
                        onLongPress(fpCoord)
                    },
                )
            }
    ) {
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scale, scale, pivot = center)
            rotate(rotation, pivot = center)
        }) {
            // Layer 1: Floor plan image
            drawImage(floorPlan)
            
            // Layer 2: Heatmap overlay (alpha-blended tiles)
            heatmapTiles?.forEach { tile ->
                drawImage(
                    image = tile.bitmap.asImageBitmap(),
                    topLeft = Offset(tile.x.toFloat() * tile.width, tile.y.toFloat() * tile.height),
                    alpha = 0.7f,
                )
            }
            
            // Layer 3: Measurement point markers
            measurementPoints.forEach { point ->
                drawCircle(
                    color = point.quality.color,
                    radius = 8f,
                    center = Offset(point.x.toFloat(), point.y.toFloat()),
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(point.x.toFloat(), point.y.toFloat()),
                    style = Stroke(width = 2f),
                )
            }
            
            // Layer 4: Current position cursor (animated pulsing circle)
            currentPosition?.let { pos ->
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.3f),
                    radius = 20f,  // Animated pulse
                    center = Offset(pos.x.toFloat(), pos.y.toFloat()),
                )
                drawCircle(
                    color = Color.Blue,
                    radius = 6f,
                    center = Offset(pos.x.toFloat(), pos.y.toFloat()),
                )
            }
        }
    }
}
```

---

## 23. Theming & Design System

### 23.1 Color Palette

Dark theme by default (better for field work — less battery drain, less glare):

```kotlin
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),          // Light blue — primary actions
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFF81C784),         // Green — positive signal indicators
    tertiary = Color(0xFFFFB74D),          // Orange — warnings
    error = Color(0xFFEF5350),             // Red — errors, weak signal
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF444444),
)
```

### 23.2 Typography

Dense, monospace-friendly for data display:

```kotlin
val AppTypography = Typography(
    // Titles
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    
    // Body
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    
    // Data labels (monospace for alignment)
    labelLarge = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
    labelMedium = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
    labelSmall = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
)
```

---

## 24. Gesture & Interaction Design

| Gesture | Context | Action |
|---|---|---|
| Single tap | Floor plan (survey mode) | Set/correct current position |
| Single tap | Floor plan (heatmap mode) | Show dBm value at point |
| Single tap | Measurement point marker | Show measurement details |
| Long press | Floor plan (survey mode) | Collect measurement at tapped location (manual placement) |
| Pinch | Floor plan | Zoom in/out |
| Two-finger drag | Floor plan | Pan |
| Two-finger rotate | Floor plan | Rotate view |
| Double tap | Floor plan | Quick zoom to 2x / reset |
| Swipe left/right | Heatmap viewer | Switch between APs |
| Pull down | Survey list | Refresh |

---

# PART VI: ADVANCED FEATURES

---

## 25. Channel Congestion Analyzer

### 25.1 Channel Utilization Calculation

```kotlin
class ChannelAnalyzer @Inject constructor() {
    
    /**
     * Analyze channel congestion from scan results.
     * 
     * For 2.4 GHz:
     * - Channels 1-14, each 22 MHz wide
     * - Only channels 1, 6, 11 are non-overlapping
     * - Channel N overlaps with N-2 through N+2
     * 
     * For 5 GHz:
     * - Channels 36-165, each 20 MHz base width
     * - Non-overlapping by default (20 MHz spacing)
     * - 40/80/160 MHz channels overlap with adjacent
     */
    fun analyzeChannels(scanResults: List<ProcessedScanResult>): ChannelAnalysisResult {
        val results2g = scanResults.filter { it.band == WifiBand.BAND_2_4_GHZ }
        val results5g = scanResults.filter { it.band == WifiBand.BAND_5_GHZ }
        
        val channels2g = (1..14).map { ch ->
            val apsOnChannel = results2g.filter { it.channel == ch }
            val overlappingAps = results2g.filter { abs(it.channel - ch) <= 2 && it.channel != ch }
            
            ChannelInfo(
                channel = ch,
                band = WifiBand.BAND_2_4_GHZ,
                frequency = ChannelMapper.channelToFrequency(ch, WifiBand.BAND_2_4_GHZ),
                apCount = apsOnChannel.size,
                overlappingApCount = overlappingAps.size,
                strongestSignal = apsOnChannel.maxOfOrNull { it.rssi } ?: -100,
                averageSignal = apsOnChannel.map { it.rssi }.average().takeIf { !it.isNaN() } ?: -100.0,
                congestionScore = calculateCongestionScore(apsOnChannel, overlappingAps),
                aps = apsOnChannel,
            )
        }
        
        val channels5g = listOf(36,40,44,48,52,56,60,64,100,104,108,112,116,120,124,128,132,136,140,144,149,153,157,161,165).map { ch ->
            val apsOnChannel = results5g.filter { it.channel == ch }
            ChannelInfo(
                channel = ch,
                band = WifiBand.BAND_5_GHZ,
                frequency = ChannelMapper.channelToFrequency(ch, WifiBand.BAND_5_GHZ),
                apCount = apsOnChannel.size,
                overlappingApCount = 0,  // 5 GHz channels don't overlap at 20 MHz
                strongestSignal = apsOnChannel.maxOfOrNull { it.rssi } ?: -100,
                averageSignal = apsOnChannel.map { it.rssi }.average().takeIf { !it.isNaN() } ?: -100.0,
                congestionScore = calculateCongestionScore(apsOnChannel, emptyList()),
                aps = apsOnChannel,
            )
        }
        
        return ChannelAnalysisResult(
            channels2g = channels2g,
            channels5g = channels5g,
            recommended2g = findBestChannel(channels2g),
            recommended5g = findBestChannel(channels5g),
        )
    }
    
    /**
     * Congestion score: 0.0 (empty) to 1.0 (severely congested).
     * 
     * Factors:
     * - Number of APs on the exact channel (each AP adds contention)
     * - Number of overlapping APs (partial interference)
     * - Signal strength of interferers (strong ones are worse)
     */
    private fun calculateCongestionScore(
        apsOnChannel: List<ProcessedScanResult>,
        overlappingAps: List<ProcessedScanResult>,
    ): Double {
        if (apsOnChannel.isEmpty() && overlappingAps.isEmpty()) return 0.0
        
        // Weight by signal strength: a -40 dBm interferer is much worse than a -90 dBm one
        val coChannelWeight = apsOnChannel.sumOf { rssiToWeight(it.rssi) }
        val adjChannelWeight = overlappingAps.sumOf { rssiToWeight(it.rssi) * 0.5 }
        
        val totalWeight = coChannelWeight + adjChannelWeight
        return (totalWeight / 5.0).coerceIn(0.0, 1.0)  // Normalize: 5.0 = "completely congested"
    }
    
    private fun rssiToWeight(rssi: Int): Double {
        // Map -30 dBm → 1.0, -90 dBm → 0.1
        return ((rssi + 90.0) / 60.0).coerceIn(0.1, 1.0)
    }
    
    private fun findBestChannel(channels: List<ChannelInfo>): Int {
        // Among non-overlapping channels (1, 6, 11 for 2.4 GHz; any for 5 GHz),
        // pick the one with the lowest congestion score.
        val candidates = if (channels.first().band == WifiBand.BAND_2_4_GHZ) {
            channels.filter { it.channel in ChannelMapper.NON_OVERLAPPING_2_4 }
        } else {
            channels
        }
        return candidates.minByOrNull { it.congestionScore }?.channel ?: channels.first().channel
    }
}
```

---

## 26. Network Security Auditor

### 26.1 Security Analysis

```kotlin
class SecurityAuditor @Inject constructor() {
    
    fun audit(scanResults: List<ProcessedScanResult>): SecurityAuditResult {
        val findings = mutableListOf<SecurityFinding>()
        
        for (ap in scanResults) {
            // Check 1: Open networks (no encryption)
            if (ap.security == SecurityType.OPEN) {
                findings.add(SecurityFinding(
                    severity = Severity.HIGH,
                    bssid = ap.bssid,
                    ssid = ap.ssid,
                    title = "Open Network (No Encryption)",
                    description = "This network transmits all data in plaintext. " +
                        "Anyone within range can intercept traffic.",
                    recommendation = "Enable WPA2-PSK or WPA3-SAE encryption.",
                ))
            }
            
            // Check 2: WEP encryption (trivially breakable)
            if (ap.security == SecurityType.WEP) {
                findings.add(SecurityFinding(
                    severity = Severity.CRITICAL,
                    bssid = ap.bssid,
                    ssid = ap.ssid,
                    title = "WEP Encryption (Broken)",
                    description = "WEP can be cracked in minutes with freely available tools. " +
                        "It provides no meaningful security.",
                    recommendation = "Upgrade to WPA2-PSK or WPA3-SAE immediately.",
                ))
            }
            
            // Check 3: WPA (TKIP) — deprecated
            if (ap.security == SecurityType.WPA_PSK) {
                findings.add(SecurityFinding(
                    severity = Severity.MEDIUM,
                    bssid = ap.bssid,
                    ssid = ap.ssid,
                    title = "WPA-TKIP (Deprecated)",
                    description = "WPA with TKIP has known vulnerabilities. " +
                        "While not trivially breakable like WEP, it's below modern standards.",
                    recommendation = "Upgrade to WPA2-AES or WPA3-SAE.",
                ))
            }
            
            // Check 4: Hidden SSID (false sense of security)
            if (ap.ssid.isEmpty() || ap.isHidden) {
                findings.add(SecurityFinding(
                    severity = Severity.LOW,
                    bssid = ap.bssid,
                    ssid = "(hidden)",
                    title = "Hidden SSID",
                    description = "Hiding the SSID does not improve security. " +
                        "The SSID is transmitted in probe responses and association frames.",
                    recommendation = "Visible SSIDs are fine. Focus on strong encryption instead.",
                ))
            }
            
            // Check 5: Default SSID names (may indicate unconfigured router)
            val defaultPatterns = listOf(
                "linksys", "netgear", "dlink", "tp-link", "default", "setup",
                "HOME-", "Wireless", "FRITZ!Box", "Vodafone-", "TIM-",
            )
            if (defaultPatterns.any { ap.ssid.startsWith(it, ignoreCase = true) }) {
                findings.add(SecurityFinding(
                    severity = Severity.LOW,
                    bssid = ap.bssid,
                    ssid = ap.ssid,
                    title = "Default SSID Name",
                    description = "This network uses a manufacturer default SSID, " +
                        "suggesting the router may not have been fully configured.",
                    recommendation = "Verify that the admin password has been changed from default.",
                ))
            }
        }
        
        // Check 6: Rogue AP detection (same SSID, different BSSID)
        val ssidGroups = scanResults.groupBy { it.ssid }.filter { it.value.size > 1 }
        for ((ssid, aps) in ssidGroups) {
            if (ssid.isBlank()) continue
            val vendors = aps.map { it.vendor }.distinct()
            if (vendors.size > 1) {
                findings.add(SecurityFinding(
                    severity = Severity.HIGH,
                    bssid = aps.map { it.bssid }.joinToString(", "),
                    ssid = ssid,
                    title = "Potential Rogue AP / Evil Twin",
                    description = "Multiple APs share the SSID '$ssid' but come from " +
                        "different manufacturers (${vendors.joinToString(", ")}). " +
                        "This could indicate a rogue access point.",
                    recommendation = "Verify all APs are authorized. " +
                        "Check if unknown BSSIDs belong to your infrastructure.",
                ))
            }
        }
        
        return SecurityAuditResult(
            findings = findings.sortedBy { it.severity.ordinal },
            totalAps = scanResults.size,
            openCount = scanResults.count { it.security == SecurityType.OPEN },
            wepCount = scanResults.count { it.security == SecurityType.WEP },
            wpaCount = scanResults.count { it.security == SecurityType.WPA_PSK },
            wpa2Count = scanResults.count { it.security in listOf(SecurityType.WPA2_PSK, SecurityType.WPA2_EAP) },
            wpa3Count = scanResults.count { it.security == SecurityType.WPA3_SAE },
            overallScore = calculateOverallScore(findings),
        )
    }
    
    private fun calculateOverallScore(findings: List<SecurityFinding>): SecurityScore {
        val criticalCount = findings.count { it.severity == Severity.CRITICAL }
        val highCount = findings.count { it.severity == Severity.HIGH }
        
        return when {
            criticalCount > 0 -> SecurityScore.CRITICAL
            highCount > 2 -> SecurityScore.POOR
            highCount > 0 -> SecurityScore.FAIR
            findings.isEmpty() -> SecurityScore.EXCELLENT
            else -> SecurityScore.GOOD
        }
    }
}

data class SecurityFinding(
    val severity: Severity,
    val bssid: String,
    val ssid: String,
    val title: String,
    val description: String,
    val recommendation: String,
)

enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
enum class SecurityScore { EXCELLENT, GOOD, FAIR, POOR, CRITICAL }
```

---

## 27. Speed & Throughput Estimator

```kotlin
/**
 * Estimate expected throughput from signal quality.
 * 
 * This is an ESTIMATE, not a measurement. Actual throughput depends on
 * many factors (interference, AP load, backhaul, protocol overhead).
 * 
 * We use empirical models based on typical real-world performance
 * at various RSSI levels for each WiFi standard.
 */
object ThroughputEstimator {
    
    data class ThroughputEstimate(
        val maxTheoretical: Double,  // Mbps (link rate)
        val estimatedActual: Double, // Mbps (realistic throughput)
        val confidence: Double,      // 0.0-1.0
    )
    
    fun estimate(
        rssi: Double,
        standard: WifiStandard,
        channelWidth: ChannelWidth,
    ): ThroughputEstimate {
        // Theoretical max link rates
        val maxLinkRate = when (standard) {
            WifiStandard.LEGACY -> 54.0     // 802.11a/g
            WifiStandard.WIFI_4_N -> when (channelWidth) {
                ChannelWidth.MHZ_20 -> 72.0
                ChannelWidth.MHZ_40 -> 150.0
                else -> 150.0
            }
            WifiStandard.WIFI_5_AC -> when (channelWidth) {
                ChannelWidth.MHZ_20 -> 87.0
                ChannelWidth.MHZ_40 -> 200.0
                ChannelWidth.MHZ_80 -> 433.0
                ChannelWidth.MHZ_160 -> 867.0
            }
        }
        
        // RSSI → throughput percentage (empirical curve)
        // At -30 dBm: ~90% of max
        // At -50 dBm: ~70% of max
        // At -65 dBm: ~40% of max
        // At -75 dBm: ~15% of max
        // At -85 dBm: ~2% of max
        val percentage = when {
            rssi >= -30 -> 0.90
            rssi >= -50 -> 0.90 - (rssi + 30) / (-20) * 0.20   // Linear 90% → 70%
            rssi >= -65 -> 0.70 - (rssi + 50) / (-15) * 0.30   // Linear 70% → 40%
            rssi >= -75 -> 0.40 - (rssi + 65) / (-10) * 0.25   // Linear 40% → 15%
            rssi >= -85 -> 0.15 - (rssi + 75) / (-10) * 0.13   // Linear 15% → 2%
            else -> 0.01
        }
        
        // Real-world overhead: TCP/IP, contention, retransmits ≈ 50-60% of link rate
        val overheadFactor = 0.55
        
        return ThroughputEstimate(
            maxTheoretical = maxLinkRate,
            estimatedActual = maxLinkRate * percentage * overheadFactor,
            confidence = if (rssi >= -70) 0.7 else 0.4,  // Low confidence at weak signals
        )
    }
}
```

---

## 28. AR Signal Overlay

### 28.1 Concept

Point the phone's camera at the room and see a color overlay indicating signal strength in real-time. This uses the camera preview + live WiFi data + IMU for orientation.

```kotlin
/**
 * AR overlay that shows signal strength as a colored tint on the camera preview.
 * 
 * Simple approach (no ARCore needed):
 * 1. Show camera preview
 * 2. Overlay a semi-transparent color based on current RSSI
 * 3. Optionally show floating labels with AP names and signal levels
 * 
 * This is NOT a full AR experience — it's a utility overlay.
 * No spatial anchoring, no plane detection, no 3D models.
 * Just a live camera feed with signal information overlaid.
 */
@Composable
fun ArSignalOverlay(
    currentRssi: Int,
    connectedSsid: String?,
    visibleAps: List<ProcessedScanResult>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Camera preview
        CameraPreview(modifier = Modifier.fillMaxSize())
        
        // Layer 2: Color tint overlay based on signal strength
        val overlayColor = SignalQuality.fromRssi(currentRssi).color.copy(alpha = 0.2f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
        
        // Layer 3: Signal information HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Text(
                text = "${connectedSsid ?: "Not connected"}: $currentRssi dBm",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${visibleAps.size} APs visible",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
```

---

## 29. Multi-Floor 3D Mapping

### 29.1 Floor Management

```kotlin
data class FloorConfig(
    val floorNumber: Int,       // 0 = ground, 1 = first, -1 = basement
    val label: String,          // "Ground Floor", "Floor 1", "Basement"
    val floorPlanId: Long?,     // Reference to floor plan entity
    val heightMeters: Double,   // Floor-to-floor height (for signal propagation)
    val alignment: FloorAlignment, // How this floor aligns with the reference floor
)

data class FloorAlignment(
    val referenceFloor: Int,
    val offsetX: Double,  // Meters offset from reference floor origin
    val offsetY: Double,
    val rotation: Double, // Radians
)
```

### 29.2 Inter-Floor Signal Propagation

```kotlin
/**
 * Signal propagation through floors.
 * 
 * Typical floor attenuation:
 *   Concrete floor:  15-20 dB per floor
 *   Wood floor:      8-12 dB per floor
 *   Metal deck:      20-30 dB per floor
 * 
 * Signal from an AP on floor N reaching floor M:
 *   RSSI_M = RSSI_N - floorAttenuation × |M - N|
 * 
 * Note: attenuation is NOT linear with number of floors.
 * First floor crossing: full attenuation
 * Second floor: slightly less (signal finds paths around)
 * Use: totalAttenuation = firstFloor + additionalFloor × (numFloors - 1)
 */
fun estimateInterFloorRssi(
    rssiSameFloor: Double,
    floorDifference: Int,
    firstFloorAttenuation: Double = 18.0,  // dB
    additionalFloorAttenuation: Double = 12.0,  // dB per additional floor
): Double {
    if (floorDifference == 0) return rssiSameFloor
    val totalLoss = firstFloorAttenuation + 
        additionalFloorAttenuation * (abs(floorDifference) - 1).coerceAtLeast(0)
    return rssiSameFloor - totalLoss
}
```

---

## 30. Export & Reporting Engine

### 30.1 Export Formats

```kotlin
sealed interface ExportFormat {
    data object CsvRaw : ExportFormat          // Raw measurement data
    data object CsvSummary : ExportFormat      // Per-AP summary
    data object JsonFull : ExportFormat         // Complete survey data (importable)
    data object HeatmapPng : ExportFormat      // Heatmap as image
    data object PdfReport : ExportFormat       // Full report with charts
    data object KmlGeodata : ExportFormat      // For outdoor surveys with GPS
}

class ExportEngine @Inject constructor(
    private val context: Context,
) {
    suspend fun exportCsv(
        surveyId: Long, 
        measurements: List<MeasurementPoint>,
        format: ExportFormat,
    ): Uri {
        val fileName = "survey_${surveyId}_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir("exports"), fileName)
        
        file.bufferedWriter().use { writer ->
            // Header
            writer.appendLine("point_id,x,y,floor,timestamp,bssid,ssid,rssi,smoothed_rssi,frequency,channel,channel_width,band,security")
            
            // Data rows
            for (point in measurements) {
                for (ap in point.apMeasurements) {
                    writer.appendLine(buildString {
                        append(point.id).append(',')
                        append(point.position.x).append(',')
                        append(point.position.y).append(',')
                        append(point.position.floor).append(',')
                        append(point.timestamp).append(',')
                        append(ap.bssid).append(',')
                        append('"').append(ap.ssid.replace("\"", "\"\"")).append('"').append(',')
                        append(ap.rssi).append(',')
                        append(ap.smoothedRssi).append(',')
                        append(ap.frequency).append(',')
                        append(ap.channel).append(',')
                        append(ap.channelWidth).append(',')
                        append(ap.band).append(',')
                        append(ap.security)
                    })
                }
            }
        }
        
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    
    suspend fun exportHeatmapPng(
        heatmapResult: HeatmapResult,
        floorPlan: Bitmap?,
    ): Uri {
        val combined = Bitmap.createBitmap(
            heatmapResult.width,
            heatmapResult.height,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(combined)
        
        // Draw floor plan
        floorPlan?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        
        // Draw heatmap tiles with alpha
        val paint = Paint().apply { alpha = 180 }
        for (tile in heatmapResult.tiles) {
            canvas.drawBitmap(
                tile.bitmap,
                (tile.x * tile.width).toFloat(),
                (tile.y * tile.height).toFloat(),
                paint,
            )
        }
        
        val fileName = "heatmap_${System.currentTimeMillis()}.png"
        val file = File(context.getExternalFilesDir("exports"), fileName)
        file.outputStream().use { combined.compress(Bitmap.CompressFormat.PNG, 100, it) }
        
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
```

---

## 31. Historical Analysis & Diffing

### 31.1 Survey Comparison

```kotlin
class SurveyComparator @Inject constructor() {
    
    /**
     * Compare two surveys of the same space to show improvements or degradation.
     * 
     * Produces a diff heatmap: green = improved, red = degraded, gray = unchanged.
     */
    suspend fun compare(
        baseline: List<MeasurementSample>,
        current: List<MeasurementSample>,
        width: Int,
        height: Int,
        config: HeatmapConfig,
    ): ComparisonResult = withContext(Dispatchers.Default) {
        // Interpolate both surveys to the same grid
        val baselineGrid = idwInterpolate(baseline, width, height, config.idwPower)
        val currentGrid = idwInterpolate(current, width, height, config.idwPower)
        
        // Compute difference grid
        val diffGrid = DoubleArray(width * height)
        var improvedCount = 0
        var degradedCount = 0
        var unchangedCount = 0
        
        for (i in diffGrid.indices) {
            if (baselineGrid[i].isNaN() || currentGrid[i].isNaN()) {
                diffGrid[i] = Double.NaN
            } else {
                diffGrid[i] = currentGrid[i] - baselineGrid[i]
                when {
                    diffGrid[i] > 3.0 -> improvedCount++
                    diffGrid[i] < -3.0 -> degradedCount++
                    else -> unchangedCount++
                }
            }
        }
        
        // Color map: green (positive diff) ← gray (zero) → red (negative diff)
        val pixels = IntArray(width * height)
        for (i in diffGrid.indices) {
            pixels[i] = if (diffGrid[i].isNaN()) {
                Color.TRANSPARENT
            } else {
                diffToColor(diffGrid[i])
            }
        }
        
        ComparisonResult(
            diffBitmap = createBitmap(pixels, width, height),
            avgImprovement = diffGrid.filter { !it.isNaN() }.average(),
            improvedPercent = improvedCount.toDouble() / (improvedCount + degradedCount + unchangedCount),
            degradedPercent = degradedCount.toDouble() / (improvedCount + degradedCount + unchangedCount),
        )
    }
    
    private fun diffToColor(diff: Double): Int {
        val maxDiff = 20.0  // ±20 dBm is full saturation
        val normalized = (diff / maxDiff).coerceIn(-1.0, 1.0)
        
        return when {
            normalized > 0 -> {
                // Improved: gray → green
                val intensity = (normalized * 255).toInt()
                android.graphics.Color.argb(180, 0, intensity, 0)
            }
            normalized < 0 -> {
                // Degraded: gray → red
                val intensity = (-normalized * 255).toInt()
                android.graphics.Color.argb(180, intensity, 0, 0)
            }
            else -> android.graphics.Color.argb(60, 128, 128, 128)
        }
    }
}
```

---

# PART VII: IMPLEMENTATION

---

## 32. Complete File Structure

```
wifi-thermal-scanner/
│
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/alexcupsa/wifithermal/
│       │   ├── WifiThermalApp.kt                    # Application class (@HiltAndroidApp)
│       │   ├── MainActivity.kt                       # Single activity entry point
│       │   ├── MainNavHost.kt                        # Navigation graph setup
│       │   └── di/
│       │       └── AppModule.kt                      # Top-level Hilt module
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/
│           │   └── ic_launcher.xml
│           └── xml/
│               └── file_paths.xml                    # FileProvider paths
│
├── core/
│   ├── core-model/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/core/model/
│   │       ├── Survey.kt
│   │       ├── MeasurementPoint.kt
│   │       ├── ApMeasurement.kt
│   │       ├── Position.kt
│   │       ├── FloorPlan.kt
│   │       ├── CoordinateTransform.kt
│   │       ├── HeatmapResult.kt
│   │       ├── HeatmapConfig.kt
│   │       ├── ChannelInfo.kt
│   │       ├── SecurityFinding.kt
│   │       ├── ProcessedScanResult.kt
│   │       └── Enums.kt                             # WifiBand, ChannelWidth, SecurityType, etc.
│   │
│   ├── core-data/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/core/data/
│   │       ├── repository/
│   │       │   ├── SurveyRepository.kt               # Interface
│   │       │   ├── SurveyRepositoryImpl.kt
│   │       │   ├── MeasurementRepository.kt           # Interface
│   │       │   ├── MeasurementRepositoryImpl.kt
│   │       │   ├── FloorPlanRepository.kt             # Interface
│   │       │   ├── FloorPlanRepositoryImpl.kt
│   │       │   ├── WiFiRepository.kt                  # Interface
│   │       │   └── WiFiRepositoryImpl.kt
│   │       ├── datasource/
│   │       │   ├── WiFiScannerDataSource.kt
│   │       │   ├── SensorDataSource.kt
│   │       │   └── LocationDataSource.kt
│   │       ├── mapper/
│   │       │   ├── SurveyMapper.kt                    # Entity ↔ Domain model
│   │       │   ├── MeasurementMapper.kt
│   │       │   └── FloorPlanMapper.kt
│   │       └── di/
│   │           ├── DataModule.kt                      # Hilt bindings
│   │           └── RepositoryModule.kt
│   │
│   ├── core-database/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/core/database/
│   │       ├── AppDatabase.kt
│   │       ├── Converters.kt                          # Room type converters
│   │       ├── entity/
│   │       │   ├── SurveyEntity.kt
│   │       │   ├── MeasurementPointEntity.kt
│   │       │   ├── ApMeasurementEntity.kt
│   │       │   ├── FloorPlanEntity.kt
│   │       │   ├── AccessPointEntity.kt
│   │       │   └── WallSegmentEntity.kt
│   │       ├── dao/
│   │       │   ├── SurveyDao.kt
│   │       │   ├── MeasurementDao.kt
│   │       │   ├── FloorPlanDao.kt
│   │       │   ├── AccessPointDao.kt
│   │       │   └── WallSegmentDao.kt
│   │       ├── relation/
│   │       │   └── MeasurementWithAps.kt
│   │       └── di/
│   │           └── DatabaseModule.kt
│   │
│   ├── core-engine/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/core/engine/
│   │       ├── wifi/
│   │       │   ├── WiFiScannerEngine.kt
│   │       │   ├── ChannelMapper.kt
│   │       │   ├── OuiLookup.kt
│   │       │   ├── SecurityParser.kt
│   │       │   └── ChannelAnalyzer.kt
│   │       ├── signal/
│   │       │   ├── SignalProcessor.kt
│   │       │   ├── RssiKalmanFilter.kt
│   │       │   ├── OutlierRejector.kt
│   │       │   └── PathLossModel.kt
│   │       ├── position/
│   │       │   ├── DeadReckoningEngine.kt
│   │       │   ├── StepDetector.kt
│   │       │   ├── StepLengthEstimator.kt
│   │       │   ├── HeadingEstimator.kt
│   │       │   └── SensorFusion.kt
│   │       ├── heatmap/
│   │       │   ├── HeatmapEngine.kt
│   │       │   ├── IdwInterpolator.kt
│   │       │   ├── KrigingInterpolator.kt
│   │       │   ├── RbfInterpolator.kt
│   │       │   ├── GaussianBlur.kt
│   │       │   ├── HeatmapColors.kt
│   │       │   └── TileManager.kt
│   │       ├── security/
│   │       │   └── SecurityAuditor.kt
│   │       ├── analysis/
│   │       │   ├── ThroughputEstimator.kt
│   │       │   └── SurveyComparator.kt
│   │       └── math/
│   │           ├── LuDecomposition.kt                 # For Kriging matrix solve
│   │           ├── Statistics.kt                      # Mean, median, MAD, percentiles
│   │           └── Geometry.kt                        # Line intersection (wall crossing)
│   │
│   ├── core-ui/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/core/ui/
│   │       ├── theme/
│   │       │   ├── Color.kt
│   │       │   ├── Type.kt
│   │       │   └── Theme.kt
│   │       ├── components/
│   │       │   ├── ZoomableFloorPlan.kt
│   │       │   ├── SignalStrengthBar.kt
│   │       │   ├── ChannelChart.kt
│   │       │   ├── AccessPointListItem.kt
│   │       │   ├── HeatmapLegend.kt
│   │       │   ├── SurveyStatsCard.kt
│   │       │   ├── CalibrationOverlay.kt
│   │       │   └── LoadingIndicator.kt
│   │       └── util/
│   │           ├── CoordinateUtil.kt
│   │           └── FormatUtil.kt
│   │
│   └── core-common/
│       ├── build.gradle.kts
│       └── src/main/java/com/alexcupsa/wifithermal/core/common/
│           ├── DispatcherProvider.kt
│           ├── Result.kt                              # Sealed result wrapper
│           └── Extensions.kt
│
├── feature/
│   ├── feature-dashboard/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/dashboard/
│   │       ├── DashboardScreen.kt
│   │       ├── DashboardViewModel.kt
│   │       └── DashboardUiState.kt
│   │
│   ├── feature-survey/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/survey/
│   │       ├── list/
│   │       │   ├── SurveyListScreen.kt
│   │       │   ├── SurveyListViewModel.kt
│   │       │   └── SurveyListUiState.kt
│   │       ├── active/
│   │       │   ├── ActiveSurveyScreen.kt
│   │       │   ├── ActiveSurveyViewModel.kt
│   │       │   ├── ActiveSurveyUiState.kt
│   │       │   └── SurveyForegroundService.kt
│   │       └── results/
│   │           ├── SurveyResultsScreen.kt
│   │           ├── SurveyResultsViewModel.kt
│   │           └── SurveyResultsUiState.kt
│   │
│   ├── feature-heatmap/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/heatmap/
│   │       ├── HeatmapScreen.kt
│   │       ├── HeatmapViewModel.kt
│   │       └── HeatmapUiState.kt
│   │
│   ├── feature-analysis/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/analysis/
│   │       ├── ChannelAnalysisScreen.kt
│   │       ├── ChannelAnalysisViewModel.kt
│   │       ├── SecurityAuditScreen.kt
│   │       └── SecurityAuditViewModel.kt
│   │
│   ├── feature-floorplan/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/floorplan/
│   │       ├── FloorPlanManagerScreen.kt
│   │       ├── FloorPlanManagerViewModel.kt
│   │       ├── CalibrationScreen.kt
│   │       └── CalibrationViewModel.kt
│   │
│   ├── feature-export/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/alexcupsa/wifithermal/feature/export/
│   │       ├── ExportScreen.kt
│   │       └── ExportViewModel.kt
│   │
│   └── feature-settings/
│       ├── build.gradle.kts
│       └── src/main/java/com/alexcupsa/wifithermal/feature/settings/
│           ├── SettingsScreen.kt
│           └── SettingsViewModel.kt
│
├── gradle/
│   ├── libs.versions.toml                            # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                                  # Root build file
├── settings.gradle.kts                               # Module registration
├── gradle.properties                                 # Build configuration
├── .gitignore
├── PLAN.md                                           # This file
└── keystore/
    └── .gitkeep                                      # Keystore NOT committed
```

---

## 33. Implementation Phases

### Phase 1: Foundation (Week 1-2)

**Goal**: Build the skeleton — compile, run, see a screen.

| # | Task | Files | Estimated Hours |
|---|---|---|---|
| 1.1 | Set up development environment (JDK, Android SDK, Android Studio) | System setup | 2 |
| 1.2 | Initialize Gradle project with all modules | build.gradle.kts, settings.gradle.kts, libs.versions.toml | 3 |
| 1.3 | Configure Hilt dependency injection | AppModule.kt, WifiThermalApp.kt | 1 |
| 1.4 | Set up Room database with all entities and DAOs | core-database/ | 4 |
| 1.5 | Implement domain models | core-model/ | 2 |
| 1.6 | Implement repository interfaces and basic implementations | core-data/repository/ | 3 |
| 1.7 | Set up Compose theme and design system | core-ui/theme/ | 1 |
| 1.8 | Create navigation graph and empty screen shells | MainNavHost.kt, all Screen.kt files | 2 |
| 1.9 | Build Dashboard screen with real-time WiFi info | feature-dashboard/ | 3 |
| 1.10 | First APK build and install on device | Build pipeline | 1 |

**Deliverable**: App installs, shows dashboard with current WiFi connection info and AP count.

### Phase 2: Core Scanning (Week 3-4)

**Goal**: Reliable WiFi scanning and data collection.

| # | Task | Files | Hours |
|---|---|---|---|
| 2.1 | Implement WiFiScannerEngine with throttle management | WiFiScannerEngine.kt | 4 |
| 2.2 | Implement SignalProcessor (Kalman filter, outlier rejection) | SignalProcessor.kt, RssiKalmanFilter.kt, OutlierRejector.kt | 3 |
| 2.3 | Implement ChannelMapper and OUI lookup | ChannelMapper.kt, OuiLookup.kt | 2 |
| 2.4 | Build AP list screen (real-time scan results) | Integrated into Dashboard | 3 |
| 2.5 | Implement survey creation and measurement recording | SurveyRepositoryImpl.kt, MeasurementRepositoryImpl.kt | 3 |
| 2.6 | Build Survey List screen | feature-survey/list/ | 2 |
| 2.7 | Build Active Survey screen (basic — manual position tapping) | feature-survey/active/ | 4 |
| 2.8 | Implement foreground service for continuous scanning | SurveyForegroundService.kt | 3 |
| 2.9 | Android permissions handling (location, WiFi, notifications) | Permission utilities | 2 |

**Deliverable**: User can create a survey, tap positions on a blank canvas, collect WiFi measurements, and see them saved.

### Phase 3: Floor Plans & Positioning (Week 5-6)

**Goal**: Load floor plans, calibrate them, and track position.

| # | Task | Files | Hours |
|---|---|---|---|
| 3.1 | Implement FloorPlan import from gallery/camera | FloorPlanRepositoryImpl.kt | 2 |
| 3.2 | Build ZoomableFloorPlan Composable | ZoomableFloorPlan.kt | 5 |
| 3.3 | Build floor plan calibration screen (two-point distance) | CalibrationScreen.kt, CalibrationViewModel.kt | 3 |
| 3.4 | Implement StepDetector (accelerometer peak detection) | StepDetector.kt | 3 |
| 3.5 | Implement HeadingEstimator (complementary filter) | HeadingEstimator.kt | 3 |
| 3.6 | Implement DeadReckoningEngine (sensor fusion) | DeadReckoningEngine.kt | 3 |
| 3.7 | Integrate positioning into Active Survey screen | ActiveSurveyScreen.kt, ActiveSurveyViewModel.kt | 3 |
| 3.8 | Implement position correction (tap to set position) | Active survey screen | 1 |
| 3.9 | Add continuous collection mode (auto-collect on walk) | ActiveSurveyViewModel.kt | 2 |

**Deliverable**: User loads a floor plan, walks around with position tracking, collects measurements that appear on the floor plan.

### Phase 4: Heatmap Engine (Week 7-8)

**Goal**: Generate and display beautiful heatmaps.

| # | Task | Files | Hours |
|---|---|---|---|
| 4.1 | Implement IDW interpolation | IdwInterpolator.kt | 3 |
| 4.2 | Implement Gaussian smoothing | GaussianBlur.kt | 2 |
| 4.3 | Implement color gradient mapping | HeatmapColors.kt | 2 |
| 4.4 | Implement tile-based bitmap generation | TileManager.kt | 3 |
| 4.5 | Build HeatmapEngine (orchestrator) | HeatmapEngine.kt | 3 |
| 4.6 | Build Heatmap Viewer screen with overlay on floor plan | HeatmapScreen.kt, HeatmapViewModel.kt | 4 |
| 4.7 | Add AP selector and band filter to heatmap viewer | HeatmapScreen.kt | 2 |
| 4.8 | Add heatmap configuration controls (algorithm, smoothing, opacity) | HeatmapScreen.kt | 2 |
| 4.9 | Implement Kriging interpolation (optional, higher quality) | KrigingInterpolator.kt, LuDecomposition.kt | 5 |

**Deliverable**: Full heatmap visualization with configurable algorithms, overlaid on floor plan.

### Phase 5: Analysis & Features (Week 9-10)

**Goal**: Channel analysis, security audit, export.

| # | Task | Files | Hours |
|---|---|---|---|
| 5.1 | Implement ChannelAnalyzer | ChannelAnalyzer.kt | 3 |
| 5.2 | Build Channel Analysis screen with charts | ChannelAnalysisScreen.kt | 4 |
| 5.3 | Implement SecurityAuditor | SecurityAuditor.kt | 3 |
| 5.4 | Build Security Audit screen | SecurityAuditScreen.kt | 3 |
| 5.5 | Implement CSV/JSON export | ExportEngine.kt | 3 |
| 5.6 | Implement heatmap PNG export | ExportEngine.kt | 2 |
| 5.7 | Build Export screen | ExportScreen.kt | 2 |
| 5.8 | Implement ThroughputEstimator | ThroughputEstimator.kt | 1 |
| 5.9 | Implement PathLossModel distance estimation | PathLossModel.kt | 2 |

**Deliverable**: Full analysis suite — channels, security, export.

### Phase 6: Polish & Advanced (Week 11-12)

**Goal**: Survey comparison, AR overlay, settings, optimization.

| # | Task | Files | Hours |
|---|---|---|---|
| 6.1 | Implement SurveyComparator (diff heatmaps) | SurveyComparator.kt | 3 |
| 6.2 | Build comparison view in heatmap screen | HeatmapScreen.kt | 2 |
| 6.3 | Implement AR Signal Overlay (camera + color tint) | ArSignalOverlay.kt | 4 |
| 6.4 | Build Settings screen (step length cal, theme, defaults) | SettingsScreen.kt | 2 |
| 6.5 | Battery optimization (sensor polling rates, scan scheduling) | Throughout codebase | 3 |
| 6.6 | Performance profiling and optimization | Throughout codebase | 4 |
| 6.7 | Edge case handling and error resilience | Throughout codebase | 3 |
| 6.8 | Multi-floor support | FloorConfig integration | 3 |
| 6.9 | Wall drawing and attenuation modeling | Wall segment editor | 4 |
| 6.10 | Final testing on device | All screens | 4 |

**Deliverable**: Production-ready personal tool.

---

## 34. Class & Interface Catalog

### 34.1 Complete Interface Index

| Interface | Module | Purpose |
|---|---|---|
| `SurveyRepository` | core-data | Survey CRUD operations |
| `MeasurementRepository` | core-data | Measurement point CRUD and queries |
| `FloorPlanRepository` | core-data | Floor plan management |
| `WiFiRepository` | core-data | WiFi scan result access |
| `SurveyDao` | core-database | Room DAO for surveys |
| `MeasurementDao` | core-database | Room DAO for measurements |
| `FloorPlanDao` | core-database | Room DAO for floor plans |
| `AccessPointDao` | core-database | Room DAO for access points |
| `WallSegmentDao` | core-database | Room DAO for wall segments |

### 34.2 Complete Class Index

| Class | Module | Purpose |
|---|---|---|
| `WifiThermalApp` | app | Application class, Hilt entry point |
| `MainActivity` | app | Single activity, hosts Compose navigation |
| `WiFiScannerEngine` | core-engine | WiFi scanning orchestration |
| `SignalProcessor` | core-engine | RSSI smoothing and processing |
| `RssiKalmanFilter` | core-engine | Per-BSSID Kalman filter |
| `OutlierRejector` | core-engine | MAD-based outlier detection |
| `ChannelMapper` | core-engine | Frequency ↔ channel conversion |
| `ChannelAnalyzer` | core-engine | Channel congestion analysis |
| `OuiLookup` | core-engine | BSSID → vendor lookup |
| `SecurityParser` | core-engine | Capabilities string → SecurityType |
| `StepDetector` | core-engine | Accelerometer step detection |
| `StepLengthEstimator` | core-engine | Weinberg model step length |
| `HeadingEstimator` | core-engine | Complementary filter heading |
| `DeadReckoningEngine` | core-engine | Sensor fusion positioning |
| `HeatmapEngine` | core-engine | Heatmap generation orchestration |
| `HeatmapColors` | core-engine | RSSI → color mapping |
| `KrigingInterpolator` | core-engine | Kriging interpolation with variogram |
| `GaussianBlur` | core-engine | Separable Gaussian smoothing |
| `PathLossModel` | core-engine | Log-distance path loss |
| `SecurityAuditor` | core-engine | Network security analysis |
| `ThroughputEstimator` | core-engine | Signal → throughput estimation |
| `SurveyComparator` | core-engine | Before/after survey diffing |
| `LuDecomposition` | core-engine | Matrix solver for Kriging |
| `ExportEngine` | core-data | CSV/JSON/PNG export |
| `DashboardViewModel` | feature-dashboard | Dashboard state management |
| `ActiveSurveyViewModel` | feature-survey | Survey collection state management |
| `HeatmapViewModel` | feature-heatmap | Heatmap generation state management |
| `SurveyForegroundService` | feature-survey | Android foreground service for scanning |

---

## 35. Build Configuration (Gradle)

### 35.1 Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
compose-bom = "2024.09.02"
hilt = "2.52"
room = "2.6.1"
navigation = "2.8.1"
lifecycle = "2.8.5"
coroutines = "1.9.0"
coil = "3.0.0"
serialization = "1.7.2"
hilt-navigation = "1.2.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# Lifecycle
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Serialization
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# Image
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

# Core
core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.2" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 35.2 Root build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### 35.3 App module build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.alexcupsa.wifithermal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alexcupsa.wifithermal"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-engine"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-common"))
    implementation(project(":feature:feature-dashboard"))
    implementation(project(":feature:feature-survey"))
    implementation(project(":feature:feature-heatmap"))
    implementation(project(":feature:feature-analysis"))
    implementation(project(":feature:feature-floorplan"))
    implementation(project(":feature:feature-export"))
    implementation(project(":feature:feature-settings"))

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

### 35.4 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- WiFi scanning permissions -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
        android:usesPermissionFlags="neverForLocation" />

    <!-- Foreground service for continuous scanning -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Camera for AR overlay and floor plan photo capture -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- Storage for floor plan images and export -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-feature android:name="android.hardware.wifi" android:required="true" />
    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />
    <uses-feature android:name="android.hardware.sensor.gyroscope" android:required="true" />
    <uses-feature android:name="android.hardware.sensor.compass" android:required="true" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.location.gps" android:required="false" />

    <application
        android:name=".WifiThermalApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="WiFi Thermal Scanner"
        android:supportsRtl="true"
        android:theme="@style/Theme.WifiThermal">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.WifiThermal">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".feature.survey.active.SurveyForegroundService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false" />

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

---

## 36. ProGuard / R8 Rules

```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-dontwarn dagger.hilt.**

# Keep our data classes for serialization
-keep class com.alexcupsa.wifithermal.core.model.** { *; }
-keep class com.alexcupsa.wifithermal.core.database.entity.** { *; }
```

---

# PART VIII: QUALITY & OPERATIONS

---

## 37. Testing Strategy

### 37.1 Test Pyramid

| Level | Coverage Target | Framework | What to Test |
|---|---|---|---|
| **Unit Tests** | 90%+ for core-engine | JUnit 5 + Coroutines Test | All algorithms, filters, math, signal processing |
| **Integration Tests** | 80%+ for core-data | Room in-memory DB | DAO queries, repository operations, data flow |
| **UI Tests** | Key flows | Compose Testing | Survey creation, heatmap generation, navigation |

### 37.2 Critical Unit Tests

```kotlin
// RssiKalmanFilter tests
class RssiKalmanFilterTest {
    @Test fun `converges to stable value with constant input`()
    @Test fun `tracks changing signal with appropriate lag`()
    @Test fun `rejects noise while preserving signal trend`()
    @Test fun `handles initialization correctly`()
    @Test fun `reset clears all state`()
}

// StepDetector tests
class StepDetectorTest {
    @Test fun `detects steps from real walking accelerometer data`()
    @Test fun `rejects steps faster than minimum interval`()
    @Test fun `does not detect steps while standing still`()
    @Test fun `handles phone orientation changes`()
}

// IDW Interpolator tests
class IdwInterpolatorTest {
    @Test fun `exact match at measurement points`()
    @Test fun `reasonable interpolation between two points`()
    @Test fun `higher power gives more local influence`()
    @Test fun `handles single measurement point`()
    @Test fun `NaN for points outside search radius`()
}

// ChannelMapper tests
class ChannelMapperTest {
    @Test fun `2412 MHz maps to channel 1`()
    @Test fun `2437 MHz maps to channel 6`()
    @Test fun `2462 MHz maps to channel 11`()
    @Test fun `5180 MHz maps to channel 36`()
    @Test fun `5745 MHz maps to channel 149`()
    @Test fun `round-trip frequency-channel-frequency is identity`()
}

// SecurityAuditor tests
class SecurityAuditorTest {
    @Test fun `flags open networks as high severity`()
    @Test fun `flags WEP as critical`()
    @Test fun `detects potential rogue APs with same SSID different vendor`()
    @Test fun `clean scan produces excellent score`()
}

// PathLossModel tests
class PathLossModelTest {
    @Test fun `distance estimate increases as RSSI decreases`()
    @Test fun `1 meter estimate matches reference RSSI`()
    @Test fun `5 GHz has higher free-space loss than 2_4 GHz`()
    @Test fun `calibration produces reasonable exponent for indoor data`()
}
```

### 37.3 Integration Test Example

```kotlin
@RunWith(AndroidJUnit4::class)
class MeasurementDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var measurementDao: MeasurementDao
    private lateinit var surveyDao: SurveyDao
    
    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        measurementDao = db.measurementDao()
        surveyDao = db.surveyDao()
    }
    
    @After
    fun teardown() {
        db.close()
    }
    
    @Test
    fun insertAndRetrieveMeasurementWithAps() = runTest {
        // Create survey
        val surveyId = surveyDao.insertSurvey(/* ... */)
        
        // Insert measurement with AP data
        val pointId = measurementDao.insertMeasurementPoint(/* ... */)
        measurementDao.insertApMeasurements(listOf(/* ... */))
        
        // Verify retrieval
        val results = measurementDao.getMeasurementSamples(surveyId, "AA:BB:CC:DD:EE:FF")
        assertThat(results).hasSize(1)
        assertThat(results[0].rssi).isEqualTo(-45.0)
    }
}
```

---

## 38. Performance Budgets

| Metric | Budget | Measurement Method |
|---|---|---|
| Cold start to interactive | < 1.5 seconds | `adb shell am start-activity` timing |
| WiFi scan → UI update | < 200 ms | Systrace / custom timing |
| Heatmap generation (200 points, IDW) | < 3 seconds | `measureTimeMillis` |
| Heatmap generation (200 points, Kriging) | < 10 seconds | `measureTimeMillis` |
| Floor plan zoom/pan frame time | < 16.6 ms (60 fps) | Compose frame timing |
| Measurement point tap → saved | < 100 ms | End-to-end timing |
| Memory (steady state) | < 150 MB | Android Profiler |
| APK size (release, minified) | < 15 MB | `ls -la app-release.apk` |

### 38.1 Performance Optimization Techniques

1. **Heatmap rendering**: Use `Bitmap.createBitmap()` with `ARGB_8888` and recycle. Tile-based rendering (512×512) avoids single large allocation.
2. **Interpolation**: For IDW with >200 points on a 1080×2400 grid, downsample the grid (e.g., compute at 1/4 resolution, then upscale with bilinear interpolation). This gives 16× speedup.
3. **Sensor events**: Process on a dedicated thread. Don't allocate objects in the sensor callback — use pre-allocated arrays.
4. **Room queries**: Use `@Transaction` for read consistency. Return `Flow` for reactive updates — no polling.
5. **Bitmap loading**: Use `BitmapFactory.Options.inSampleSize` to downsample floor plans. A 4000×3000 photo loaded at 1/2 sample = 2000×1500, saving 75% memory.
6. **Compose recomposition**: Use `remember`, `derivedStateOf`, and stable data classes to minimize recomposition. Profile with Layout Inspector.

---

## 39. Battery Optimization

### 39.1 Power Budget Analysis

| Component | Power Draw | Duration | Mitigation |
|---|---|---|---|
| Screen (1080p, 70% brightness) | ~300 mW | Continuous | Allow auto-dim. Dark theme. |
| WiFi scanning | ~100 mW per scan | 4 per 2 min | Can't reduce — OS throttle is already the limit. |
| Sensor polling (100 Hz) | ~50 mW | Continuous during survey | Reduce to 50 Hz when standing still (step detector detects no motion). |
| GPS (if outdoor) | ~150 mW | Periodic | Use fused location provider. Request updates every 5s, not continuous. |
| CPU (signal processing) | ~100-500 mW | Burst | Process in batches, then sleep. Avoid sustained high CPU. |
| **Total (active survey)** | **~500-1000 mW** | — | 5000 mAh battery → **~2.5-5 hours** of continuous surveying. |

### 39.2 Power Saving Strategies

1. **Adaptive sensor rate**: Full 100 Hz during walking, drop to 10 Hz when stationary (no steps detected for 5 seconds).
2. **Screen timeout override**: Keep screen on during survey (`FLAG_KEEP_SCREEN_ON`) but allow dimming.
3. **Batch DB writes**: Buffer measurements in memory, flush to Room every 10 points or every 30 seconds.
4. **Lazy heatmap generation**: Only generate heatmaps when explicitly requested, not continuously.
5. **Foreground service notification**: Shows battery-friendly progress — "Surveying: 45 points collected".

---

## 40. Error Handling & Resilience

### 40.1 Error Categories

| Category | Example | Handling |
|---|---|---|
| **Permission denied** | Location permission not granted | Show clear permission rationale screen. Block survey start until granted. |
| **Location services off** | GPS/Location toggle disabled | Prompt to enable. WiFi scanning requires it on Android 10+. |
| **WiFi disabled** | WiFi adapter turned off | Prompt to enable. Show warning banner. |
| **Scan throttled** | Too many scans requested | Show countdown timer to next available scan. Auto-retry. |
| **No scan results** | Empty result from WifiManager | Retry once. If still empty, check WiFi state. Show "No APs found" — may be normal in shielded areas. |
| **Sensor unavailable** | Device lacks gyroscope | Fall back to magnetometer-only heading. Degrade step detection to accelerometer-only. Show accuracy warning. |
| **Out of memory** | Heatmap bitmap too large | Reduce resolution automatically. Show "Memory limit reached, rendering at reduced quality." |
| **Floor plan load failure** | Corrupted image file | Show error toast. Suggest re-importing. |
| **Database error** | SQLite FULL or corrupt | Extremely unlikely for personal use. Log and show generic error. |

### 40.2 Crash Recovery

The app uses a `ProcessLifecycleOwner` observer to detect unexpected termination:
- Active survey state is persisted to Room on every measurement collection.
- On restart, check for an active survey with status ACTIVE and offer to resume it.
- Sensor and scan state are stateless — they restart cleanly.

---

## 41. Build, Sign & Deploy

### 41.1 Debug Build (Daily Development)

```bash
# Build and install directly to connected device
./gradlew :app:installDebug

# Or build APK only
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 41.2 Release Build

```bash
# Build release APK (requires signing config in build.gradle.kts)
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Install release build
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 41.3 Direct ADB Install (No IDE Needed)

```bash
# One-liner: build + install + launch
./gradlew :app:installDebug && \
adb shell am start -n com.alexcupsa.wifithermal/.MainActivity
```

### 41.4 Quick Iteration Cycle

```bash
# Watch for changes and rebuild (use Gradle continuous build)
./gradlew :app:assembleDebug --continuous

# In another terminal, install when build completes
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

# APPENDIX A: Android WiFi Scanning — API Reference Summary

For the developer implementing this plan, here are the exact Android API calls used:

```kotlin
// Get WifiManager
val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

// Check WiFi state
val wifiEnabled = wifiManager.isWifiEnabled

// Get current connection info
val wifiInfo = wifiManager.connectionInfo  // Deprecated but functional
// wifiInfo.ssid, wifiInfo.bssid, wifiInfo.rssi, wifiInfo.frequency, wifiInfo.linkSpeed

// Start scan (deprecated since API 28 but still works)
val scanStarted = wifiManager.startScan()

// Get scan results (cached + fresh)
val results: List<ScanResult> = wifiManager.scanResults

// Register for scan completion
val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
context.registerReceiver(scanReceiver, intentFilter)

// ScanResult fields (API 34):
// .SSID         String      Network name
// .BSSID        String      MAC address
// .level        int         RSSI in dBm
// .frequency    int         Center frequency in MHz
// .channelWidth int         CHANNEL_WIDTH_20MHZ / 40 / 80 / 160 / 80MHZ_PLUS_MHZ
// .capabilities String      "[WPA2-PSK-CCMP][ESS]" format
// .timestamp    long        Microseconds since boot
// .standard     int         WIFI_STANDARD_LEGACY / 11N / 11AC / 11AX (API 30+)
```

---

# APPENDIX B: Sensor API Reference Summary

```kotlin
// Get SensorManager
val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

// Get sensors
val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

// Register listener
sensorManager.registerListener(
    listener,
    accelerometer,
    SensorManager.SENSOR_DELAY_GAME,  // ~20ms = 50 Hz
)

// SensorEvent fields:
// .sensor       Sensor        Which sensor
// .values       float[]       Sensor data (3 axes for accel/gyro/mag)
// .timestamp    long          Nanoseconds since boot
// .accuracy     int           SENSOR_STATUS_ACCURACY_*

// Accelerometer values: [x, y, z] in m/s² (includes gravity)
// Gyroscope values:     [x, y, z] in rad/s
// Magnetometer values:  [x, y, z] in μT (micro-Tesla)
// Rotation vector:      [x, y, z, cos(θ/2), accuracy]

// Get orientation from rotation matrix
val rotMatrix = FloatArray(9)
val orientation = FloatArray(3)
SensorManager.getRotationMatrixFromVector(rotMatrix, rotationVectorValues)
SensorManager.getOrientation(rotMatrix, orientation)
// orientation[0] = azimuth (radians, 0=North, clockwise)
// orientation[1] = pitch
// orientation[2] = roll
```

---

# APPENDIX C: Key Mathematical Formulas Reference

## Signal Processing

| Formula | Description |
|---|---|
| `RSSI_smooth = α × RSSI_prev + (1-α) × RSSI_raw` | Exponential Moving Average (simple alternative to Kalman) |
| `FSPL(dB) = 20·log₁₀(d) + 20·log₁₀(f) - 147.55` | Free Space Path Loss (d in meters, f in Hz) |
| `d = 10^((RSSI₀ - RSSI) / (10·n))` | Distance estimation from RSSI |
| `MAD = median(|xᵢ - median(x)|)` | Median Absolute Deviation (robust dispersion) |
| `Z_mod = 0.6745·(x - median) / MAD` | Modified Z-score for outlier detection |

## Positioning

| Formula | Description |
|---|---|
| `x += L·sin(θ)` | Dead reckoning X update (L=step length, θ=heading) |
| `y += L·cos(θ)` | Dead reckoning Y update |
| `L = k·(a_max - a_min)^0.25` | Weinberg step length model |
| `θ = α·(θ + ω·dt) + (1-α)·θ_mag` | Complementary filter heading |

## Interpolation

| Formula | Description |
|---|---|
| `z(P) = Σ(wᵢ·zᵢ) / Σ(wᵢ), wᵢ = 1/dᵢᵖ` | Inverse Distance Weighting |
| `γ(h) = C₀ + C·[1.5(h/a) - 0.5(h/a)³]` | Spherical variogram (Kriging) |
| `G(x) = (1/σ√2π)·exp(-x²/2σ²)` | Gaussian kernel (for smoothing) |

---

# APPENDIX D: Deployment Checklist

Before each release build:

- [ ] All unit tests pass: `./gradlew test`
- [ ] Room schema exported: check `schemas/` directory
- [ ] `versionCode` incremented in `build.gradle.kts`
- [ ] `versionName` updated
- [ ] Release keystore is accessible
- [ ] ProGuard rules don't strip needed classes
- [ ] Test on device: full survey workflow start-to-finish
- [ ] Test on device: heatmap generation for survey with 50+ points
- [ ] Test on device: export CSV and PNG
- [ ] Battery drain acceptable during 30-minute survey
- [ ] No ANR (Application Not Responding) during heavy operations
- [ ] Permissions are requested at the right time with rationale

---

*End of Engineering Specification — WiFi Thermal Scanner v1.0.0*
*Total estimated implementation: ~130 hours across 12 weeks*
*Lines of Kotlin (estimated): ~8,000-12,000*

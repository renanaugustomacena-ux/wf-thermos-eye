# WiFi Thermal Scanner — System Engineering Specification v2

**Version**: 2.0.0
**Date**: 2026-04-26
**Classification**: Personal advanced work tool — not a public product
**Methodology**: Platform Engineering Doctrine (Cupsa, 2026)

---

## Methodological Sequence

This document follows the mandatory operating sequence:

1. **System Purpose & Organizational Context** — Why this system exists
2. **Stakeholder & Operator Modeling** — Who uses it, how, under what conditions
3. **Constraint Identification** — Hard limits that shape every downstream decision
4. **Layer & Responsibility Decomposition** — What exists at each layer, what it owns
5. **Interface & Contract Definition** — Explicit contracts between every boundary
6. **Failure, Scaling & Evolution Modeling** — How the system breaks, recovers, and changes
7. **Validation Against Constraints** — Proof that the design satisfies the constraints
8. **Technical Specification** — Implementation details (referencing v1 appendix where appropriate)
9. **Execution Plan** — Phased build-out with verification gates

Skipping steps is not permitted.

---

# 1. SYSTEM PURPOSE & ORGANIZATIONAL CONTEXT

## 1.1 Problem Statement

WiFi coverage analysis in indoor environments currently requires one of:

| Approach | Cost | Problems |
|---|---|---|
| **Enterprise tools** (Ekahau, NetSpot Pro) | $500-$2000/year license | Locked to specific hardware adapters. Overkill for personal use. Vendor lock-in on data formats. No control over algorithms. |
| **Consumer apps** (WiFi Analyzer, NetSpot Free) | Free-$50 | No spatial mapping. No heatmap generation. No floor plan overlay. Limited to real-time signal display. |
| **Manual measurement** | Free | Walk around with any app noting RSSI at locations. Error-prone. No reproducibility. No visualization. |

None of these are acceptable for a platform engineer who needs:
- Full control over the measurement pipeline
- Raw data access at every stage
- Custom interpolation algorithms selectable per survey
- Deterministic, reproducible results
- A tool that can be extended and evolved without vendor permission

## 1.2 System Purpose

The WiFi Thermal Scanner exists to **transform raw RF signal measurements into actionable spatial intelligence about WiFi coverage quality**.

It answers these operational questions:
1. Where are the dead zones in this space?
2. Which access point should serve which area?
3. What channel configuration minimizes co-channel interference?
4. Did the changes I made (moving an AP, changing channels) actually improve coverage?
5. Is there a rogue or misconfigured AP in this environment?

## 1.3 What This System Optimizes For

| Priority | Optimized For | At the Cost Of |
|---|---|---|
| 1 | **Measurement accuracy** | Survey speed — the operator walks slowly, collects deliberately |
| 2 | **Data transparency** | Polish — raw data is always accessible, even when ugly |
| 3 | **Algorithmic correctness** | Simplicity — Kriging is harder to implement than simple averaging, but statistically optimal |
| 4 | **Offline autonomy** | Features — no cloud sync, no remote storage, no API dependencies |
| 5 | **Single-device reliability** | Portability — optimized for one phone, not abstracted for N devices |

## 1.4 What This System Is Not

- **Not a network monitor.** It surveys, it doesn't watch. Continuous monitoring is a different system with different constraints.
- **Not a network configurator.** Read-only. It never sends management frames, never changes AP settings.
- **Not a platform for others.** Single operator. No multi-user, no auth, no tenancy. The "platform thinking" applies to the engineering discipline, not the product.
- **Not a map application.** The floor plan is a reference image, not a GIS layer. We don't need projections, geocoding, or tile servers.

## 1.5 Organizational Context

Single-person operation:
- **Developer** = the operator = the user = the maintainer.
- No team coordination overhead.
- No release trains, no feature flags, no A/B testing.
- Evolution is driven by direct operational need, not product management.

This simplifies:
- No backward-compatibility obligations to other consumers
- No API versioning for external clients
- No CI/CD pipeline (local builds, ADB install)
- No telemetry or analytics

This does NOT simplify:
- Code quality (operator-developer feedback loop is instant — bugs hurt immediately)
- Reliability (a crash during a 45-minute survey means lost work)
- Data integrity (a corrupted survey database means re-surveying)
- Algorithmic correctness (wrong heatmap → wrong AP placement decision → wasted time)

---

# 2. STAKEHOLDER & OPERATOR MODELING

## 2.1 The Single Operator

| Attribute | Value | Design Implication |
|---|---|---|
| **Technical level** | Platform engineer / systems architect | Power-user UI. Dense information. No tutorials. No hand-holding. |
| **Domain knowledge** | RF fundamentals, networking, signal propagation | Can interpret dBm values, understands channel overlap, knows what path loss means |
| **Usage frequency** | Periodic — survey when environment changes or new space encountered | App must cold-start fast. State from weeks ago must be retrievable. |
| **Session duration** | 15 minutes (small room) to 2 hours (office floor) | Battery is a hard constraint. State must survive process death. |
| **Environment** | Indoor residential, indoor office, occasionally outdoor | Path loss exponent varies. Calibration must be per-environment. |
| **Physical context** | Walking, phone in hand, looking at screen periodically | One-handed operation. Large touch targets for collection button. Minimal text input during survey. |
| **Decision output** | AP placement, channel selection, coverage verification | Heatmap and channel analysis are the primary deliverables. Export for documentation. |

## 2.2 Operator Workflow Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    OPERATOR WORKFLOW                             │
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ PREPARE  │───►│ SURVEY   │───►│ ANALYZE  │───►│ ACT      │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│                                                                 │
│  Load floor plan  Walk space      Generate heatmap  Move AP     │
│  Calibrate scale  Collect points  Channel analysis  Change ch   │
│  Set origin       Mark position   Security audit    Re-survey   │
│                   Tap collect      Compare before/   Document    │
│                                   after                         │
└─────────────────────────────────────────────────────────────────┘
```

### Phase: PREPARE (1-5 minutes)
- Load or photograph a floor plan
- Calibrate: tap two known points, enter real-world distance between them
- Set starting position on the floor plan
- Select survey name and parameters

### Phase: SURVEY (15-120 minutes)
- Walk to a location
- Wait for position tracking to stabilize (1-2 seconds)
- Tap COLLECT — app triggers WiFi scan and records all visible APs with RSSI at current position
- Repeat every 2-5 meters throughout the space
- Optionally: use continuous mode (auto-collect every N steps)
- Periodically correct position drift by tapping known location on floor plan

### Phase: ANALYZE (2-10 minutes)
- Select an AP or band to visualize
- View heatmap overlaid on floor plan
- Inspect channel congestion chart
- Run security audit
- Compare with previous survey if available
- Export results (PNG heatmap, CSV data)

### Phase: ACT (external to this system)
- Make changes to the WiFi environment based on analysis
- Re-survey to verify improvement

## 2.3 Operator Pain Points (Design Constraints)

| Pain Point | Design Response |
|---|---|
| "I'm walking and can't type" | Collect is a single large button. No text input during survey. |
| "I lost my position on the floor plan" | Tap anywhere on floor plan to correct. Dead reckoning resumes from there. |
| "The scan didn't work" | Visual feedback: button color changes during scan. Countdown to next available scan. |
| "I don't know if I have enough data" | Real-time measurement density indicator on floor plan. |
| "The heatmap looks wrong" | Expose algorithm selection, smoothing, and interpolation parameters. Raw data always accessible. |
| "My phone died mid-survey" | State persisted after every collection point. Resume on restart. |
| "I need to show this to someone" | One-tap PNG export of heatmap. One-tap CSV export of raw data. |

---

# 3. CONSTRAINT IDENTIFICATION

Constraints are ordered by impact on design. Each constraint is hard (non-negotiable) or soft (can be traded against other concerns).

## 3.1 Hard Constraints

### HC-1: Android WiFi Scan Throttle
**Constraint**: Android 9+ limits foreground WiFi scans to 4 per 2-minute window. Background: 1 per 30 minutes.
**Source**: Android platform policy, enforced by WifiManager.
**Impact**: This is the single most important constraint in the entire system. It determines:
- Minimum time per collection point (~30 seconds for guaranteed fresh scan)
- Maximum spatial resolution in continuous mode
- The fundamental tradeoff between survey speed and data freshness
**Mitigation**: Each scan returns ALL visible APs simultaneously (typically 10-50). This means one scan gives a complete RF snapshot at that position. The throttle limits spatial sampling density, not per-AP data richness.

### HC-2: Location Permission Dependency
**Constraint**: WiFi scanning on Android 10+ requires `ACCESS_FINE_LOCATION` permission AND Location Services enabled system-wide.
**Source**: Android privacy policy.
**Impact**: The app cannot scan WiFi at all if location is disabled. This is a binary gate — not a degraded mode.
**Design Response**: Check on app start. If not granted, block survey functionality entirely with clear explanation. Do not attempt workarounds — they'll break on the next Android update.

### HC-3: Target Device — Motorola Moto G35
**Constraint**: Single target device. Android 14 (API 34). Unisoc T760. 4-8 GB RAM. 802.11ac (WiFi 5). 2.4+5 GHz dual-band. Full IMU (accel + gyro + mag).
**Impact**:
- No WiFi 6/6E scanning (no 6 GHz band visibility)
- 4 GB RAM variant limits app heap to ~150 MB
- Unisoc GPU (Mali-G57 MP4) limits rendering complexity
- WiFi 5 means we see ac-capable APs but can't detect ax-only features
**Design Response**: Target SDK 34. No backward compatibility. Optimize for this exact SoC. Test only on this device.

### HC-4: Offline-Only Operation
**Constraint**: Zero network dependencies. No cloud services, no API calls, no remote databases.
**Source**: Operator requirement (Section 1.3 — offline autonomy is priority 4).
**Impact**: All data stored locally in Room/SQLite. All processing on-device. No OTA updates — reinstall via ADB.
**Tradeoff**: No cross-device sync. No backup unless operator manually copies database files.

### HC-5: Battery Budget
**Constraint**: 5000 mAh battery. WiFi scanning + sensors + screen-on draws 500-1000 mW.
**Source**: Device hardware.
**Impact**: Continuous survey sessions limited to ~2.5-5 hours. This is adequate for the operator workflow (max 2-hour sessions per Section 2.2), but leaves no margin for waste.
**Design Response**: Adaptive sensor rates. Batch DB writes. Dark theme. No background processing when survey is paused.

## 3.2 Soft Constraints

### SC-1: Memory Budget
**Target**: App heap ≤ 150 MB steady-state.
**Flexibility**: Can spike to 250 MB during heatmap generation if the floor plan is large.
**Design Response**: Tiled bitmap rendering (512x512). Floor plan subsampling. Eager bitmap recycling.

### SC-2: APK Size
**Target**: < 15 MB.
**Flexibility**: Up to 25 MB acceptable if features justify it.
**Design Response**: Native Android (no cross-platform runtime). Minimal dependencies. OUI lookup table is the largest embedded data (~200 KB).

### SC-3: Heatmap Generation Time
**Target**: < 3 seconds for IDW with 200 measurement points.
**Flexibility**: Kriging may take 10+ seconds for dense surveys — acceptable since it's an explicit operator choice.
**Design Response**: Downsampled interpolation grid (1/4 resolution) with bilinear upscaling. Background coroutine with progress indicator.

### SC-4: Position Accuracy
**Target**: ≤ 2 meter error after 50 meters of dead reckoning.
**Flexibility**: IMU-only dead reckoning drifts ~5-10% of distance traveled. Periodic manual correction is expected and designed-for.
**Design Response**: Position correction on tap. Confidence indicator degrades with distance from last correction. Operator is trained to correct every 20-30 meters.

## 3.3 Constraint Dependency Map

```
HC-1 (Scan Throttle) ──────► Survey workflow design
                              Collection timing
                              Continuous mode feasibility
                              
HC-2 (Location Permission) ─► Permission flow design
                              App entry gate
                              
HC-3 (Device Hardware) ─────► Memory budget (SC-1)
                              Rendering approach
                              Sensor availability
                              Band coverage limits
                              
HC-4 (Offline-Only) ────────► Storage design
                              Export design
                              No remote dependencies
                              
HC-5 (Battery) ─────────────► Sensor polling strategy
                              Scan scheduling
                              Background processing limits

SC-4 (Position Accuracy) ───► Correction UX design
                              Confidence tracking
                              Measurement weighting
```

---

# 4. LAYER & RESPONSIBILITY DECOMPOSITION

## 4.1 System Layers

```
╔═══════════════════════════════════════════════════════════════════╗
║  LAYER 5: OPERATOR INTERFACE                                     ║
║  Responsibility: Present actionable information. Capture input.  ║
║  Components: Compose screens, ViewModels, navigation             ║
║  Depends on: Layer 4                                             ║
╠═══════════════════════════════════════════════════════════════════╣
║  LAYER 4: USE CASES (DOMAIN LOGIC)                               ║
║  Responsibility: Orchestrate business operations.                ║
║  Components: Use case classes (pure Kotlin, no Android imports)  ║
║  Depends on: Layer 3 interfaces                                  ║
╠═══════════════════════════════════════════════════════════════════╣
║  LAYER 3: DATA GATEWAY                                           ║
║  Responsibility: Abstract storage and hardware access.           ║
║  Components: Repository implementations, data sources, mappers  ║
║  Depends on: Layer 2, Layer 1                                    ║
╠═══════════════════════════════════════════════════════════════════╣
║  LAYER 2: PROCESSING ENGINE                                     ║
║  Responsibility: Signal processing, interpolation, analysis.    ║
║  Components: Pure computation (no Android, no I/O)              ║
║  Depends on: Domain models only                                  ║
╠═══════════════════════════════════════════════════════════════════╣
║  LAYER 1: PLATFORM SERVICES                                     ║
║  Responsibility: Hardware abstraction. OS API wrapping.          ║
║  Components: WiFi scanner, sensor manager, file system, Room DB ║
║  Depends on: Android SDK, device hardware                        ║
╚═══════════════════════════════════════════════════════════════════╝
```

## 4.2 Responsibility Assignment Matrix

| Responsibility | Owner Layer | Justification |
|---|---|---|
| Triggering WiFi scans | L1 (Platform Services) | Direct WifiManager interaction |
| Parsing ScanResult objects | L1 (Platform Services) | Android-specific data types |
| RSSI smoothing (Kalman filter) | L2 (Processing Engine) | Pure math, no platform dependency |
| Step detection | L2 (Processing Engine) | Pure signal processing on accel data |
| Dead reckoning position update | L2 (Processing Engine) | Pure trigonometry |
| Heatmap interpolation (IDW/Kriging) | L2 (Processing Engine) | Pure math |
| Color gradient mapping | L2 (Processing Engine) | Pure numeric → color transform |
| Channel congestion scoring | L2 (Processing Engine) | Pure analysis |
| Security classification | L2 (Processing Engine) | Pure rule evaluation |
| Persisting measurements to DB | L3 (Data Gateway) | Room/SQLite interaction |
| Loading floor plan images | L3 (Data Gateway) | File I/O + BitmapFactory |
| Mapping DB entities ↔ domain models | L3 (Data Gateway) | Translation layer |
| "Start Survey" orchestration | L4 (Use Cases) | Coordinates L1 scan + L2 processing + L3 storage |
| "Generate Heatmap" orchestration | L4 (Use Cases) | Coordinates L3 data retrieval + L2 interpolation |
| Displaying heatmap on floor plan | L5 (Operator Interface) | Canvas rendering, zoom/pan |
| Handling COLLECT button tap | L5 (Operator Interface) | User input → use case invocation |

## 4.3 Module Structure

```
wifi-thermal-scanner/
├── app/                           # Wiring only: Hilt, navigation, manifest
├── core/
│   ├── model/                     # Domain models. Pure Kotlin. ZERO dependencies.
│   ├── engine/                    # L2: All algorithms. Pure Kotlin. Testable in JVM.
│   ├── data/                      # L3: Repositories, data sources, mappers.
│   ├── database/                  # L1: Room DB, entities, DAOs.
│   ├── platform/                  # L1: WiFi scanner, sensor wrappers.
│   ├── ui/                        # L5: Shared composables, theme.
│   └── common/                    # Cross-cutting utilities.
├── feature/
│   ├── dashboard/                 # L5: Dashboard screen + ViewModel
│   ├── survey/                    # L5: Survey screens + ViewModels
│   ├── heatmap/                   # L5: Heatmap viewer + ViewModel
│   ├── analysis/                  # L5: Channel/security analysis
│   ├── floorplan/                 # L5: Floor plan management
│   ├── export/                    # L5: Export functionality
│   └── settings/                  # L5: Configuration
└── gradle/                        # Build infrastructure
```

**Module dependency rules** (enforced by Gradle):
- `model` → nothing
- `engine` → `model` only
- `database` → `model` only
- `platform` → `model`, `engine`
- `data` → `model`, `engine`, `database`, `platform`
- `ui` → `model`
- `feature-*` → `model`, `data`, `ui`, `common`
- `feature-*` → NEVER depends on another `feature-*`
- `app` → all modules (wiring only)

## 4.4 Why This Decomposition

**What it optimizes for**: Testability of the processing engine. Layer 2 is pure Kotlin with zero Android imports — it runs on JVM, tests in milliseconds, and can be reasoned about independently.

**What it sacrifices**: Simplicity. A flat structure with everything in `app/` would be simpler to navigate. The multi-module structure adds Gradle configuration overhead and increases build times.

**Why the tradeoff is justified**: The processing engine (Kalman filter, interpolation, path loss model, step detection) contains the hardest-to-debug code in the system. If a heatmap looks wrong, the first question is "is the algorithm correct?" — and that question must be answerable with a unit test, not by deploying to a phone and walking around.

**Risks that remain**: Module boundaries may need adjustment as features are built. The current decomposition is based on predicted responsibility assignment, not observed usage patterns. Plan for one boundary refactor around Phase 4 (heatmap integration).

---

# 5. INTERFACE & CONTRACT DEFINITION

Every boundary between components has an explicit contract. Implicit contracts are system fragility.

## 5.1 Contract: WiFi Scanner → Signal Processor

```kotlin
// INPUT CONTRACT: Raw scan results from Android
data class RawScanInput(
    val bssid: String,          // Non-null, non-empty, format: XX:XX:XX:XX:XX:XX
    val ssid: String,           // May be empty (hidden networks). Never null.
    val rssi: Int,              // Range: [-127, 0] dBm. Typical: [-100, -20].
    val frequency: Int,         // MHz. Valid: 2400-2500 or 5100-5900.
    val channelWidth: Int,      // Android constant. One of: 0 (20MHz), 1 (40), 2 (80), 3 (160), 4 (80+80).
    val capabilities: String,   // Format: "[WPA2-PSK-CCMP][ESS]". Parse defensively.
    val timestamp: Long,        // Microseconds since boot. Monotonic.
)

// OUTPUT CONTRACT: Processed results ready for storage and display
data class ProcessedScanOutput(
    val bssid: String,
    val ssid: String,
    val rawRssi: Int,
    val smoothedRssi: Double,        // Kalman-filtered. Same unit (dBm). Always ≥ rawRssi - 10.
    val frequency: Int,
    val channel: Int,                 // Derived. Range: 1-14 (2.4G) or 32-177 (5G). -1 if unknown.
    val channelWidth: ChannelWidth,   // Enum, not raw int.
    val band: WifiBand,               // Derived from frequency. Enum.
    val security: SecurityType,       // Parsed from capabilities. Enum.
    val standard: WifiStandard,       // Derived. Enum.
    val signalQuality: SignalQuality, // Derived from smoothedRssi. Enum.
    val estimatedDistance: Double,     // Meters. From path loss model. Range: [0.1, 200].
    val vendor: String?,              // OUI lookup. Null if unknown.
    val ageMs: Long,                  // Milliseconds since scan. 0 if fresh.
)

// PROCESSING CONTRACT:
// - Every input produces exactly one output (1:1 mapping)
// - smoothedRssi converges to true value within 5 samples
// - Outliers (|Modified Z-score| > 3.0) are rejected: smoothedRssi holds previous value
// - Processing time: < 1ms per scan result
// - Thread safety: SignalProcessor is thread-safe (concurrent map of per-BSSID filters)
```

## 5.2 Contract: Sensor Pipeline → Position Tracker

```kotlin
// INPUT CONTRACT: Raw sensor events (from SensorManager callbacks)
data class AccelerometerInput(
    val x: Float, val y: Float, val z: Float,  // m/s². Includes gravity.
    val timestamp: Long,                         // Nanoseconds since boot.
)
data class RotationVectorInput(
    val x: Float, val y: Float, val z: Float, val w: Float,
    val timestamp: Long,
)

// OUTPUT CONTRACT: Position update
data class PositionOutput(
    val x: Double,             // Meters from origin. Positive = east.
    val y: Double,             // Meters from origin. Positive = north.
    val floor: Int,            // Floor number. Operator-set.
    val heading: Double,       // Radians. 0 = north, clockwise. Range: [0, 2π).
    val confidence: Double,    // Range: [0, 1]. Degrades with distance from last correction.
    val stepCount: Int,        // Total steps since survey start.
    val totalDistance: Double,  // Total meters walked since survey start.
)

// PROCESSING CONTRACT:
// - Position updates emitted on every detected step (not every sensor event)
// - Step detection latency: < 100ms from heel strike to position emission
// - Heading accuracy: ±10° in magnetically clean environment, ±30° near metal
// - Confidence = 1.0 at correction point, degrades by 0.002 per meter walked
// - confidence < 0.3 triggers visual warning to operator
// - Manual correction resets x, y, and confidence to 1.0
```

## 5.3 Contract: Measurement Data → Heatmap Engine

```kotlin
// INPUT CONTRACT
data class HeatmapInput(
    val samples: List<MeasurementSample>,  // Minimum 3 samples required. No upper limit.
    val width: Int,                         // Output grid width in pixels. Range: [100, 4096].
    val height: Int,                        // Output grid height in pixels. Range: [100, 4096].
    val config: HeatmapConfig,              // Algorithm, smoothing, color scheme parameters.
)

data class MeasurementSample(
    val x: Double,    // Floor plan coordinate (pixels). Must be within [0, width).
    val y: Double,    // Floor plan coordinate (pixels). Must be within [0, height).
    val rssi: Double, // dBm value. Range: [-100, -10]. NaN is rejected.
)

// OUTPUT CONTRACT
data class HeatmapOutput(
    val grid: DoubleArray,       // Size: width × height. Values in dBm or NaN (unmeasured).
    val pixels: IntArray,        // Size: width × height. ARGB_8888 colors.
    val minRssi: Double,         // Observed minimum in grid (excluding NaN).
    val maxRssi: Double,         // Observed maximum in grid.
    val generationTimeMs: Long,  // Elapsed time. For operator feedback.
)

// PROCESSING CONTRACT:
// - < 3 samples: return error, not an empty heatmap
// - IDW: exact interpolation at measurement points (grid value = sample value ± 0.1 dBm)
// - Kriging: may smooth through measurement points (statistically optimal, not exact)
// - NaN cells: areas where no interpolation is possible (outside convex hull by > searchRadius)
// - Grid is computed at config.resolution scale, then upsampled if resolution < 1.0
// - Thread: runs on Dispatchers.Default. Caller must not be on Main thread.
// - Cancellation: respects coroutine cancellation. Checks isActive every row.
```

## 5.4 Contract: Repository → Database

```kotlin
// STORAGE CONTRACT:
// - All writes are transactional (survey + measurements + AP data in one transaction)
// - Cascade delete: deleting a survey deletes all its measurements and AP data
// - Reactive reads: all list queries return Flow<List<T>>, emit on every DB change
// - Single reads: return suspend T?, null if not found
// - Write operations: return Long (inserted ID) or Unit (update/delete)
// - Thread safety: Room handles connection pooling. No caller-side locking needed.
// - Schema versioning: exported schema in /schemas/ directory. Migrations tested.
```

## 5.5 Contract: ViewModel → UI

```kotlin
// UI STATE CONTRACT:
// - Every screen has exactly one StateFlow<UiState> representing the complete UI state
// - UiState is a sealed interface: Loading | Active | Error
// - State is immutable. Updates via copy() or new instance.
// - No mutable state in Composables. Composables are pure functions of UiState.
// - One-shot events (navigation, snackbar) use Channel<Event>, not StateFlow.
// - State is collected via collectAsStateWithLifecycle() — pauses below STARTED.

// EXAMPLE:
sealed interface SurveyUiState {
    data object Loading : SurveyUiState
    data class Active(
        val surveyName: String,
        val floorPlan: FloorPlanUi?,
        val measurements: List<MeasurementPointUi>,
        val currentPosition: PositionUi?,
        val scanStatus: ScanStatus,
        val stats: SurveyStats,
    ) : SurveyUiState
    data class Error(val message: String) : SurveyUiState
}

// INVARIANTS:
// - Active state is NEVER emitted with null floorPlan AND non-empty measurements
//   (you can't have measurement positions without a coordinate system)
// - measurements list is always sorted by timestamp ascending
// - scanStatus reflects the CURRENT scan state, not historical
```

---

# 6. FAILURE, SCALING & EVOLUTION MODELING

## 6.1 Failure Modes

### F1: Measurement Data Quality Failure
**Cause**: Kalman filter diverges, outlier rejection too aggressive, or stale cached scan results used.
**Detection**: Heatmap shows implausible patterns — sharp discontinuities, extreme values at adjacent points.
**Observable signal**: Standard deviation of RSSI across nearby points exceeds 15 dBm.
**Recovery**: Operator inspects raw data (CSV export). Identifies bad measurements. Deletes and re-collects. App shows per-point RSSI variance in survey results screen.
**Prevention**: Scan result freshness validation. Reject results older than 60 seconds. Display age in UI.

### F2: Position Drift Accumulation
**Cause**: IMU dead reckoning accumulates error. After 50 meters without correction, error is typically 2.5-5 meters.
**Detection**: Confidence indicator drops below 0.3. Operator notices cursor position doesn't match physical location.
**Observable signal**: Confidence value displayed in real-time. Color changes from green (>0.7) to yellow (0.3-0.7) to red (<0.3).
**Recovery**: Operator taps correct position on floor plan. Dead reckoning resets.
**Prevention**: Prompt operator to correct every 30 meters (soft notification, not blocking).

### F3: Survey Interrupted (App Kill / Battery Death)
**Cause**: Android kills the app for memory. Battery dies. Operator accidentally swipes away.
**Detection**: Active survey with status=ACTIVE found on next app launch.
**Recovery**: Prompt to resume. All measurements collected before interruption are preserved (written to Room after each collection).
**Prevention**: Foreground service with persistent notification. Measurement persistence after every collection, not batched.
**Data loss scope**: Maximum 1 measurement point (the one being collected at the moment of kill).

### F4: Floor Plan Calibration Error
**Cause**: Operator enters wrong distance between calibration points, or taps imprecisely.
**Detection**: Heatmap scale looks wrong. Estimated AP distances don't match reality.
**Observable signal**: Show estimated distance between two points on the floor plan. Operator verifies against physical measurement.
**Recovery**: Re-calibrate. Existing measurements retain pixel coordinates — they don't depend on the scale. Only derived values (estimated distance) change.
**Prevention**: Display the derived scale (e.g., "1 pixel = 0.05 meters") for operator verification.

### F5: Heatmap Generation OOM
**Cause**: Floor plan is very large. Full-resolution interpolation grid exceeds memory budget.
**Detection**: OutOfMemoryError caught in heatmap generation coroutine.
**Recovery**: Automatically retry at half resolution. Inform operator of downgrade.
**Prevention**: Calculate memory requirement before starting: `width * height * 8 bytes (double) * 2 (grid + output)`. If > 100 MB, auto-downscale.

### F6: WiFi Scanning Unavailable
**Cause**: Location permission revoked. Location services disabled. WiFi adapter off.
**Detection**: Check all three conditions before survey start AND before each scan.
**Recovery**: Clear message identifying which condition failed. Deep link to system settings.
**Prevention**: Permission check gate at survey start. Periodic re-check during survey.

## 6.2 Failure Response Classification

| Failure | Severity | Response | Operator Action Required |
|---|---|---|---|
| F1 (Data quality) | Medium | Log warning, display in survey stats | Manual review, selective re-collect |
| F2 (Position drift) | Low | Auto-prompt for correction | Tap to correct |
| F3 (Interrupted) | High | Auto-persist, resume prompt | Confirm resume |
| F4 (Calibration) | Medium | Display scale for verification | Re-calibrate if wrong |
| F5 (OOM) | High | Auto-downscale, notify | Accept or reduce floor plan size |
| F6 (Scan unavailable) | Critical | Block survey, show fix steps | Enable permission/location/WiFi |

## 6.3 Evolution Model

### Planned Evolution Path

| Version | Addition | Constraint Change | Migration |
|---|---|---|---|
| 1.0 | Core survey + heatmap + channel analysis | — | — |
| 1.1 | AR signal overlay | Camera permission added | No DB migration |
| 1.2 | Wall drawing + attenuation modeling | Wall segments table | DB migration v1→v2 |
| 1.3 | Multi-floor support | Floor alignment table | DB migration v2→v3 |
| 1.4 | WiFi 6/6E support (when device supports) | New band enum value | DB migration v3→v4 |
| 2.0 | BLE beacon integration for positioning | BLE permission, beacon table | DB migration v4→v5 |

### Evolution Safeguards
- Room schema export is mandatory. `exportSchema = true` in `@Database`.
- Every migration has a corresponding test that:
  1. Creates database at version N
  2. Inserts test data
  3. Runs migration to version N+1
  4. Verifies data integrity
- Enum evolution: all enums that touch the database use string storage (not ordinal). Adding a new value is backward-compatible. Removing a value requires migration.
- Adding a column: `ALTER TABLE ADD COLUMN` with default value. Non-breaking.
- Adding a table: `CREATE TABLE IF NOT EXISTS`. Non-breaking.
- Removing anything: requires explicit migration with data preservation plan.

## 6.4 Scaling Dimensions

This is a single-user, single-device tool. Traditional scaling concerns (traffic, teams, tenancy) don't apply. But these do:

| Dimension | Current | Growth Path | Limit |
|---|---|---|---|
| **Survey size** (measurement points) | 50-200 per survey | 500+ for large venues | Room handles millions of rows. IDW: O(W*H*N). At N=500, downscale grid. |
| **Survey count** | 5-20 | 100+ over months | No limit. List pagination in UI. |
| **Floor plan resolution** | 1-4 megapixels | 10+ MP from high-res photos | Subsample on load. Never hold full-res in memory. |
| **Feature count** | v1.0 features | v2.0+ additions | Module structure supports addition without modifying existing modules. |
| **Data export volume** | Individual surveys | Batch export, cross-survey analysis | Export engine supports iteration. Not a current priority. |

---

# 7. VALIDATION AGAINST CONSTRAINTS

## 7.1 Constraint Satisfaction Matrix

| Constraint | Design Element | Satisfied? | Evidence |
|---|---|---|---|
| **HC-1** Scan throttle | 30-second minimum scan interval. User-paced collection. Freshness validation. | Yes | One scan returns all APs. 4 scans/2min is 120+ AP observations per 2 minutes. Adequate for survey. |
| **HC-2** Location permission | Binary gate check before survey. Deep link to settings. | Yes | No degraded mode — correct behavior is to block, not limp. |
| **HC-3** Device hardware | Target SDK 34. Memory budget 150 MB. Tiled rendering. Dual-band only. | Yes | Memory analysis in Section 3.2. Rendering approach prevents large single allocations. |
| **HC-4** Offline-only | Room local DB. No network permissions (except WiFi state). No HTTP client dependency. | Yes | Dependency list has zero networking libraries. |
| **HC-5** Battery | Adaptive sensor rates. Dark theme. Batch DB writes. Foreground service (avoids background scheduling overhead). | Yes | Power analysis: 500-1000 mW → 2.5-5 hours on 5000 mAh. Operator sessions ≤ 2 hours. |
| **SC-1** Memory ≤ 150 MB | Tiled bitmaps. Floor plan subsampling. Per-scan data ~2 MB for 500 points × 50 APs. | Yes | Detailed budget: 10 MB heatmap tiles + 20 MB floor plan + 2 MB data + 30 MB UI + 80 MB headroom = 142 MB. |
| **SC-2** APK < 15 MB | Native Android. Minimal dependencies. No cross-platform runtime. | Yes | Compose + Room + Hilt + Coil ≈ 5-8 MB. OUI table ~200 KB. App code ~2 MB. Total: ~10 MB. |
| **SC-3** Heatmap < 3s (IDW) | Downsampled grid (1/4 res). Bilinear upscale. Dispatchers.Default (4 big cores). | Yes | 270×600 grid × 200 points = 32M operations. T760 big core at 2.2 GHz ≈ 0.5 seconds. |
| **SC-4** Position ≤ 2m error | Weinberg step length. Complementary filter heading. Manual correction every 20-30m. | Conditional | Depends on operator discipline. System provides prompts and confidence display. |

## 7.2 Risk Registry

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Scan throttle changes in future Android versions | Low | High | Abstract scanner behind interface. Mitigation strategies (cached results, background scan events) already in design. |
| Unisoc sensor quality is poor (noisy accel/gyro) | Medium | Medium | Kalman filter and complementary filter are designed for noisy inputs. Manual correction is the primary accuracy mechanism, not dead reckoning. |
| Room performance degrades with large surveys | Low | Medium | Indexed queries. Pagination. Tested with 1000+ points in integration tests. |
| Heatmap rendering janks during zoom/pan | Medium | Low | Tiled rendering. Pre-computed bitmap. Zoom/pan only transforms tiles, doesn't regenerate. |

---

# 8. TECHNICAL SPECIFICATION

The complete technical specification (algorithms, data models, API references, UI mockups) is in **PLAN.md (v1)**, which serves as the technical appendix to this systems document.

Key sections referenced:

| Topic | v1 Section | Key Content |
|---|---|---|
| WiFi Scanner Engine | 10 | ScanResult processing, frequency-to-channel mapping, signal quality classification |
| Signal Processing | 11 | Kalman filter implementation, outlier rejection (MAD), per-BSSID filter management |
| Indoor Positioning | 12 | Step detection, Weinberg step length, complementary filter heading, dead reckoning |
| Heatmap Generation | 13-14 | IDW, Kriging (with variogram fitting), Gaussian blur, color gradients, tiled rendering |
| Path Loss Model | 15 | Log-distance model, distance estimation, exponent calibration, wall attenuation |
| Database Schema | 16-19 | Room entities, DAOs, repository pattern, migration strategy |
| UI Specifications | 20-24 | Screen layouts, navigation graph, Compose components, gesture design |
| Advanced Features | 25-31 | Channel analysis, security audit, throughput estimation, AR overlay, export engine |
| Build Configuration | 35 | Gradle setup, version catalog, AndroidManifest, ProGuard rules |
| Testing Strategy | 37 | Unit tests for engine, integration tests for DB, UI tests for key flows |

All algorithm implementations in v1 are validated against the contracts defined in Section 5 of this document.

---

# 9. EXECUTION PLAN

## 9.1 Phase Structure

Each phase has:
- **Entry gate**: What must be true before starting
- **Deliverable**: What the phase produces
- **Verification**: How we know it's correct
- **Exit gate**: What must be true before proceeding

## 9.2 Phase 0: Environment Bootstrap

**Entry gate**: Ubuntu 24.04 with internet access. ADB installed.
**Deliverable**: JDK 21, Android SDK, Gradle wrapper, empty project that compiles.
**Verification**: `./gradlew assembleDebug` succeeds. APK installs on device. Empty activity launches.
**Exit gate**: Green build. Device shows blank app.

```bash
# 1. Install JDK 21
sudo apt install -y openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# 2. Install Android SDK
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "platforms;android-34" "build-tools;35.0.0"

# 3. Create project (Gradle init or manual structure)
# 4. ./gradlew assembleDebug
# 5. adb install app/build/outputs/apk/debug/app-debug.apk
```

## 9.3 Phase 1: Core Model + Engine

**Entry gate**: Phase 0 complete (green build).
**Deliverable**: All domain models. All pure-Kotlin engine algorithms. Unit tests passing.
**Verification**: `./gradlew :core:engine:test` — all tests green.
**Exit gate**: Kalman filter, IDW interpolation, step detection, channel mapping all verified by unit tests.

**Why this phase first**: The engine is the hardest code to get right and the easiest to test. Pure Kotlin, no Android dependencies. If the algorithms are wrong, everything built on top is wrong. Validate the foundation before building the house.

## 9.4 Phase 2: Platform Services + Database

**Entry gate**: Phase 1 complete (engine tests green).
**Deliverable**: WiFi scanner data source. Sensor data source. Room database with all entities/DAOs. Integration tests passing.
**Verification**: `./gradlew :core:database:connectedAndroidTest` on device.
**Exit gate**: Can scan WiFi, read sensors, and store/retrieve data on the real device.

## 9.5 Phase 3: Data Layer + Minimal UI

**Entry gate**: Phase 2 complete.
**Deliverable**: Repository implementations. Dashboard screen showing live WiFi data. Survey list screen.
**Verification**: Launch app on device. See connected network info and AP count updating in real-time.
**Exit gate**: Real WiFi data flowing from hardware → processing → storage → UI.

## 9.6 Phase 4: Survey Workflow

**Entry gate**: Phase 3 complete.
**Deliverable**: Floor plan loading. Calibration. Active survey screen with position tracking and measurement collection. Foreground service.
**Verification**: Complete a real survey of a room. 20+ measurement points collected with position data.
**Exit gate**: Survey data in database matches physical positions on floor plan.

## 9.7 Phase 5: Heatmap + Analysis

**Entry gate**: Phase 4 complete (real survey data available).
**Deliverable**: Heatmap generation and display. Channel analysis screen. Security audit.
**Verification**: Generate heatmap from Phase 4 survey. Verify coverage patterns match reality (strong near AP, weak far away).
**Exit gate**: Heatmap visually correct. Channel recommendations make sense. Security findings are accurate.

## 9.8 Phase 6: Export + Polish

**Entry gate**: Phase 5 complete.
**Deliverable**: CSV/JSON/PNG export. Survey comparison. Settings. Battery optimization. Error handling.
**Verification**: Full end-to-end workflow: prepare → survey → analyze → export → compare.
**Exit gate**: 30-minute survey session without crash. Export produces usable files. Battery usage acceptable.

---

*End of System Engineering Specification v2*
*This document governs the WHY and the WHAT.*
*PLAN.md (v1) governs the HOW.*

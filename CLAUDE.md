# WF-Thermos-Eye

WiFi password discovery tool for rooted Android devices.

## Target Platform

- Rooted Android device (Magisk/KernelSU)
- Termux for CLI tools (aircrack-ng, reaver, hashcat)
- Custom firmware for monitor mode (device-specific)

## Attack Vectors

1. **Saved Passwords** - Extract from `/data/misc/wifi/` with root
2. **Vendor Keygen** - Generate ISP default passwords from BSSID (Fastweb, TIM, Vodafone, WindTre)
3. **Router Admin Brute Force** - Detect router IP, brute force admin dashboard
4. **WPS PIN Attack** - Reaver/bully via Termux
5. **PMKID/EAPOL Capture** - Monitor mode or ESP32 companion
6. **Cracking** - Local (Termux hashcat) or remote (VPS script)

## Working Doctrine

Derived from macena-bug-hunter.

### Code Quality

- Readable at 3 AM
- Comments explain WHY, not WHAT
- Error messages tell the operator what to do
- No AI slop: ban "delve", "robust", "leverage", "seamless", "innovative"

### Architecture

- Plugin registry for extensible components
- Graceful degradation on I/O failure
- Everything runs from Android, no external servers required

### Testing

- Tier 0: Unit tests with mocks
- Tier 1: Integration with real hardware
- Tier 2: Adversarial (tests that should find nothing)

## Project Structure

- `app/` - Android presentation layer
- `core/` - Data, database, engine, model modules
- `companion/` - ESP32 sniffer firmware (optional, for devices without monitor mode)
- `backend/` - Remote cracking script (optional, for VPS cracking)

## Implementation Status

### Phase 1: Root Shell Integration (DONE)
- `core/common/root/RootShell.kt` - su -c command execution
- `core/data/root/SavedPasswordExtractor.kt` - WifiConfigStore.xml and wpa_supplicant.conf parsing

### Phase 2: Vendor Keygen Engine (DONE)
- `core/engine/keygen/VendorKeygen.kt` - Italian ISP keygen (Fastweb, TIM, Vodafone, WindTre)
- OUI-based vendor detection
- SHA256/MD5 password generation algorithms

### Phase 3: Router Admin Brute Force (DONE)
- `core/engine/router/RouterDetector.kt` - Gateway detection and port scanning
- `core/engine/router/AdminBruteForce.kt` - Basic auth and form POST attacks
- Rate limiting detection

### Phase 4: UI Integration (DONE)
- `app/attack/AttackVectorViewModel.kt` - ViewModel for attack flows
- `app/attack/AttackVectorScreen.kt` - UI for all attack vectors
- Navigation wired from dashboard

### Phase 5: Termux Integration (DONE)
- `core/data/termux/TermuxBridge.kt` - Termux package detection and tool management
- Tool availability check (aircrack-ng, reaver, bully, hashcat, hcxtools)
- RunCommandService integration for executing commands in Termux

### Phase 6: WPS PIN Attack (DONE)
- `core/engine/wps/WpsPinAttack.kt` - Reaver/Bully integration
- Pixie Dust attack (offline PIN recovery)
- Common PIN brute force with rate limiting detection
- WPS PIN checksum computation

### Phase 7: Monitor Mode (DONE)
- `core/engine/monitor/MonitorMode.kt` - airmon-ng integration
- Interface listing and mode switching
- PMKID capture with hcxdumptool
- Handshake capture with airodump-ng + deauth
- Hash conversion to hashcat 22000 format

### Phase 8: WPS/Monitor UI (DONE)
- `app/attack/WpsAttackViewModel.kt` - Attack flow orchestration
- `app/attack/WpsAttackScreen.kt` - Full UI for WPS and capture attacks
- Environment check (root, Termux, tools)
- Interface selection and monitor mode toggle

### Pending
- Custom firmware integration for devices without native monitor mode support
- Local cracking in Termux with hashcat

## Implementation Reference

Full specifications in `IMPLEMENTATION_PLAN.md`.

# AUTHORIZATION — wf-thermos-eye Layer B

This file is the human-readable mirror of the operator's offensive-scope
declaration. The runtime gate (`OffensiveScopeGuard`) reads its data from
the JSON manifest stored in the app's `filesDir/authorization_manifest.json`,
which is editable from the Authorization screen in the app. This file
exists as the in-repo audit trail.

## Operator

- Name: Renan Augusto Macena
- Role: primary maintainer and sole user of this codebase + device
- Device: single personal Android phone (Motorola G35), connected via Tailscale to operator-owned backend

## Layer A — passive observation

Geographically unbounded. Layer A passively observes 802.11 beacons and
probe responses, which is what every Wi-Fi-capable device does when its
radio is on. No authorization scope required for the discovery,
mapping, anomaly-detection, or reporting features.

## Layer B — offensive operations

Strictly enforced by `OffensiveScopeGuard` in `core/engine/audit/`:

- Default-deny — every Layer B operation refused unless the target BSSID
  appears verbatim in `OffensiveScope.authorizedBssids`.
- No prefix matching, no wildcards, no OUI ranges. Exact MAC only.
- Empty list → Layer B fully inert. The "Connect / Scan" button on the
  Layer B screen is disabled. The `BleSnifferClient` will reject any
  capture frame regardless of its BSSID. The `CapturedHandshakeRepository`
  will reject any insertion. The `CrackingBackendClient` will reject any
  submission and mark the row `REJECTED_OUT_OF_SCOPE`.
- Defense in depth: the same scope guard is consulted at four points in
  the pipeline (BLE frame ingest, repository insert, backend submit,
  backend poll).

## Authorized BSSID list

Currently empty. The operator will populate this with exact MACs of own
testbed routers as they are acquired. Each entry committed here is a
declaration that the corresponding hardware is physically owned and
controlled by the operator, and that the operator authorizes themselves
to capture handshakes / extract PMKIDs / run offline dictionary attacks
against it for security-research purposes.

```
authorizedBssids = [
    // intentionally empty until the operator owns physical testbed
    // routers; will be added via the Authorization screen, not by
    // hand-editing this file.
]
```

## Out of scope

- Any Wi-Fi network not physically owned and controlled by the operator
- Public Wi-Fi (cafés, hotels, airports, transit)
- Neighbor / building / shared infrastructure not owned by the operator
- Networks shared with parties who have not given explicit consent

If a BSSID encountered in the field looks interesting and the operator
does not own the underlying hardware, the correct action is **not**
to add it to `authorizedBssids`.

## Italian legal context

Operating outside this scope on networks not under the operator's
control would engage:

- **Art. 615-ter c.p.** — accesso abusivo a sistema informatico
- **Art. 617-quater c.p.** — intercettazione fraudolenta di comunicazioni
- **Art. 615-quinquies c.p.** — detenzione e diffusione di programmi
  diretti a danneggiare un sistema informatico

The technical gate in `OffensiveScopeGuard` exists explicitly to prevent
accidental scope drift and to provide an in-codebase audit trail.

## Layer separation summary

| Layer | What it does                       | Scope                  | Gate                    |
|-------|------------------------------------|------------------------|-------------------------|
| A     | Passive scan + analysis + report   | Geographically open    | ScopeGuard (SSID/OUI)   |
| B     | Active capture + cracking          | Exact BSSIDs only      | OffensiveScopeGuard     |

Last updated: 2026-04-27

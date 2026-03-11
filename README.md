# Network Monitor — Build Instructions

## Requirements

| Tool | Minimum Version |
|------|----------------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.2 (via wrapper) |

---

## Quick Start

### 1. Open in Android Studio
```
File → Open → Select the AndroidNetworkMonitor/ folder
```
Android Studio will auto-sync Gradle dependencies.

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Build Release Bundle (Play Store)
```bash
./gradlew bundleRelease
```
AAB output: `app/build/outputs/bundle/release/app-release.aab`

### 4. Install on Device / Emulator
```bash
./gradlew installDebug
```

---

## Project Structure

```
app/src/main/java/com/example/networkmonitor/
├── data/
│   ├── AppDatabase.kt          # Room database singleton
│   ├── ConnectionDao.kt        # Connection queries
│   ├── AlertDao.kt             # Alert queries
│   ├── ConnectionEntity.kt     # Connection log entry model
│   ├── AlertEntity.kt          # Security alert model
│   └── NetworkRepository.kt    # Data access layer
├── vpn/
│   ├── NetworkMonitorVpnService.kt  # Core VPN service (TUN interface)
│   ├── PacketParser.kt              # IPv4 / DNS / SNI / HTTP parser
│   ├── AppIdentifier.kt             # UID → package name resolver
│   ├── GeoIpResolver.kt             # ip-api.com geo lookup
│   └── RiskAnalyzer.kt              # Multi-layer risk scoring
├── viewmodel/
│   └── MainViewModel.kt        # MVVM ViewModel
├── ui/
│   ├── MainActivity.kt         # Entry point + navigation
│   ├── theme/Theme.kt          # Dark cybersecurity color scheme
│   └── screens/
│       ├── MonitorScreen.kt    # Real-time connection list
│       ├── AppDetailScreen.kt  # Per-app domain breakdown
│       ├── AlertsScreen.kt     # Security alert feed
│       └── HistoryScreen.kt    # Grouped connection history
└── util/
    └── ExportManager.kt        # JSON / CSV export
```

---

## First Run

1. Launch the app on a physical device or emulator (API 26+)
2. Tap **Start** — Android will prompt for VPN permission
3. Accept the VPN dialog
4. Use Chrome, Telegram, etc. and watch connections appear in real-time

---

## Key Features

- 🔒 **Zero-root** — uses Android VpnService (TUN interface)
- 📡 **Real-time monitoring** — live connection feed with app, domain, IP, country
- 🧠 **DNS + SNI parsing** — extracts hostnames without decrypting HTTPS payloads
- ⚠ **Risk analysis** — heuristic TLD/tracker detection + optional AbuseIPDB integration
- 🗄️ **Local storage** — Room DB retaining up to 7 days of traffic metadata
- 📤 **Export** — JSON and CSV via Android Storage Access Framework
- 🔔 **Notifications** — foreground VPN status + alert notifications for suspicious domains

---

## Optional: AbuseIPDB Integration

To enable live IP reputation checks:
1. Get a free API key from [abuseipdb.com](https://www.abuseipdb.com)
2. In `RiskAnalyzer.kt`, implement `getStoredApiKey()` to return your key from SharedPreferences

---

## Testing Checklist

- [ ] VPN connects without root
- [ ] Chrome connections appear in monitor
- [ ] Telegram domains (api.telegram.org) visible
- [ ] TikTok analytics flagged as Suspicious
- [ ] Alerts screen shows suspicious domains
- [ ] Export JSON produces valid file
- [ ] Export CSV opens correctly in spreadsheet
- [ ] History screen groups by date correctly
- [ ] App Detail shows domains per-app

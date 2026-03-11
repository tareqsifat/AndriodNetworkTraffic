# Network Traffic Monitor Android App

## Objective
Develop an Android application that monitors outbound network connections from the device and shows which applications connect to which servers, including basic risk analysis of those servers.

The app must work **without root access** using Android's VPN Service API.

---

# Core Concept
The application creates a **local VPN tunnel** using `VpnService`.

All device traffic flows through the app which allows it to:

- inspect packet metadata
- capture DNS queries
- associate traffic with apps
- detect suspicious domains

The app will NOT decrypt HTTPS payloads.

---

# Core Features

## Real‑Time Network Monitor
Display active network connections.

Each connection should include:

- App name
- Package name
- Domain
- IP address
- Country
- Risk level

Example table:

| App | Domain | IP | Country | Risk |
|-----|------|-----|------|------|
| Telegram | api.telegram.org | 149.154.x.x | Netherlands | Safe |
| Chrome | google.com | 142.250.x.x | USA | Safe |
| Unknown App | suspicious.xyz | 185.x.x.x | Russia | Suspicious |

---

# Per‑App Network Activity
Selecting an app should show:

- domains contacted
- timestamps
- number of connections

Example:

```
App: Telegram

Connections:
api.telegram.org
cdn.telegram.org
149.154.x.x

Last seen: 10 seconds ago
Total connections: 24
```

---

# Malicious Server Detection
Integrate threat intelligence sources.

Recommended sources:

- VirusTotal
- AbuseIPDB
- Malware domain lists

When detected:

```
⚠ Suspicious Server
Domain: malicious-example.com
Risk: High
Source: AbuseIPDB
```

---

# DNS Logging
Capture DNS queries made by apps.

Example:

```
Chrome → google.com
Instagram → graph.facebook.com
TikTok → analytics.tiktok.com
```

---

# Connection History
Store traffic metadata locally.

Minimum retention:

- 24 hours

Optional:

- 7 days

Stored fields:

```
timestamp
app
domain
ip
risk score
```

---

# Security Explanation Layer
Provide simple explanations.

Example:

```
analytics.tiktok.com

Purpose:
Analytics server used by TikTok to collect usage metrics.

Risk: Low
```

---

# Architecture

Traffic flow:

```
Installed Apps
      ↓
Android Network Stack
      ↓
VPN Interface
      ↓
Traffic Monitor App
      ↓
Internet
```

---

# Key Android Components

Use:

```
android.net.VpnService
```

Creates a TUN interface.

Traffic flow:

```
apps → tun interface → app → internet
```

---

# Packet Capture

The app must:

1. Read packets from VPN interface
2. Parse headers
3. Extract metadata
4. Forward packets

Packet types:

```
TCP
UDP
DNS
```

---

# Domain Extraction

Extract domain using:

### DNS packets

Parse UDP port:

```
53
```

### TLS Server Name Indication

Use SNI field when available.

---

# App Identification

Use Android APIs:

```
PackageManager
ConnectivityManager
```

Map:

```
UID → App Package
```

Example:

```
UID: 10234
Package: org.telegram.messenger
App: Telegram
```

---

# IP Intelligence

Resolve metadata:

```
IP
Country
Organization
ASN
Hosting Provider
```

Recommended dataset:

```
MaxMind GeoLite
```

---

# Risk Scoring

Example model:

```
Safe = 0
Suspicious = 1
Malicious = 2
```

Signals:

```
malware list hit
phishing domain
spam IP
new domain
```

---

# User Interface

## Main Screen

Columns:

```
App
Domain
Country
Risk
```

---

## App Detail Screen

```
Telegram

Servers Contacted
api.telegram.org
cdn.telegram.org
149.154.x.x
```

---

## Alerts Screen

```
⚠ Suspicious
malicious-site.xyz
Detected by VirusTotal
```

---

# Performance Requirements

VPN must avoid:

```
battery drain
network slowdown
```

Use:

- async IO
- efficient parsing

---

# Privacy Requirements

Rules:

- Do NOT collect personal data
- Do NOT store payload content
- Store metadata only
- Perform analysis locally

---

# Required Permissions

```
INTERNET
FOREGROUND_SERVICE
BIND_VPN_SERVICE
ACCESS_NETWORK_STATE
```

---

# Minimum Android Version

```
Android 8 (API 26)
```

---

# Recommended Tech Stack

Language:

```
Kotlin
```

Architecture:

```
MVVM
```

Libraries:

```
Room
Coroutines
Jetpack Compose
```

---

# Data Storage

SQLite / Room tables:

```
apps
connections
domains
alerts
```

---

# Logging Schema

```
timestamp
package_name
app_name
domain
ip
country
risk_level
```

---

# Export Feature

Allow exporting logs as:

```
JSON
CSV
```

---

# Future Enhancements

Possible upgrades:

```
AI anomaly detection
traffic visualization graphs
tracker detection
network firewall
```

---

# Deliverables

Developer must provide:

```
complete source code
build instructions
APK
Play Store ready AAB
```

---

# Testing

Test using:

```
Chrome
Telegram
Instagram
TikTok
WhatsApp
```

Verify:

- connections detected
- apps mapped correctly
- DNS visible

---

# Success Criteria

App is complete when:

- VPN routing works
- connections mapped to apps
- domain/IP metadata visible
- suspicious domains flagged


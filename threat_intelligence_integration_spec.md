# Threat Intelligence Integration Specification

## Objective
Add malicious domain and IP detection capabilities to the Network Traffic Monitor Android App.

The application will use **trusted threat‑intelligence databases** to determine whether a server contacted by any app is potentially malicious.

The system will rely primarily on a **local database** of threat indicators which will be updated weekly.

---

# Supported Threat Intelligence Sources

The following threat intelligence feeds must be supported.

## Google Safe Browsing
Detects:

- phishing websites
- malware distribution sites
- harmful URLs

Data type:

- domain
- URL

---

## AbuseIPDB
Detects:

- malicious IP addresses
- botnet infrastructure
- brute force attack sources
- spam servers

Data type:

- IP reputation

Example fields:

```
ip
abuse_confidence_score
report_count
categories
```

---

## VirusTotal
Aggregated reputation score from multiple antivirus engines.

Supported lookups:

- domain
- IP
- URL

Example fields:

```
domain
malicious_engines
suspicious_engines
harmless_engines
```

---

## PhishTank
Database of verified phishing URLs.

Data type:

```
phishing_urls
verified_status
target_brand
```

---

## URLHaus
Malware hosting URL database.

Tracks URLs hosting:

- trojans
- ransomware
- malware payloads

Example fields:

```
url
malware_family
status
```

---

## AlienVault OTX
Threat intelligence community feed containing:

- malicious domains
- IP addresses
- malware campaigns

---

# Detection Pipeline

When the monitoring engine detects a network connection, the following checks must occur.

```
connection detected
        ↓
extract domain
        ↓
extract IP
        ↓
check local threat database
        ↓
calculate risk score
        ↓
display result to user
```

---

# Local Threat Database

The application must maintain a **local offline database** containing known malicious indicators.

Types of stored indicators:

```
domain
ip
url
threat_type
source
confidence_score
last_updated
```

Database engine:

```
SQLite / Room
```

Tables:

```
threat_domains
threat_ips
threat_urls
threat_sources
```

---

# Weekly Database Update System

The app must update the threat database periodically.

## Update Strategy

1. App checks last update timestamp
2. If more than 7 days have passed
3. Prompt user to update database

---

## Weekly Update Popup

The user must see a popup notification.

Example:

```
Security Database Update Available

New threat intelligence data is available.
Updating improves malicious site detection.

[Update Now]
[Remind Me Later]
```

---

## Update Process

```
user clicks update
      ↓
download threat dataset
      ↓
validate dataset
      ↓
replace local database
      ↓
store update timestamp
```

---

# Risk Scoring System

Each domain or IP should receive a risk score.

Risk levels:

```
SAFE
LOW
MEDIUM
HIGH
CRITICAL
```

Example logic:

```
if domain in phishing_list → HIGH
if IP abuse_score > 80 → HIGH
if malware_url_detected → CRITICAL
```

---

# Risk Explanation Engine

The application must show human‑readable explanations.

Example:

```
Domain: malicious-example.com

Risk Level: HIGH

Reason:
This domain is known to host phishing pages and was reported by multiple security databases.
```

---

# User Actions

When a suspicious or malicious server is detected, the application must allow the user to take actions.

Available actions:

```
Block Connection
Ignore Warning
View Details
Report Threat
```

---

## Block Connection

The app blocks future connections to the detected domain or IP.

Implementation:

```
add rule to VPN firewall blocklist
```

---

## Ignore Warning

The user can ignore warnings for a specific domain.

Implementation:

```
add domain to trusted list
```

---

## View Details

Display detailed threat intelligence data.

Example:

```
Domain
IP
Country
Threat type
Source database
Detection timestamp
```

---

## Report Threat

Allow user to submit suspicious servers to community reporting systems.

Optional integration:

```
AbuseIPDB report API
```

---

# UI Components

## Alert Notification

Example alert:

```
⚠ Suspicious Server Detected

App: Unknown App
Domain: suspicious-domain.xyz
Risk Level: HIGH
```

Buttons:

```
Block
Ignore
Details
```

---

# Performance Considerations

Threat checks must be fast.

Requirements:

```
lookup time < 10 ms
```

Techniques:

- indexed database tables
- in-memory cache

---

# Privacy Requirements

The application must follow strict privacy rules.

Rules:

```
no personal data collection
no traffic payload logging
only domain and IP metadata stored
```

---

# Deliverables

Developer must implement:

```
local threat intelligence database
weekly update system
malicious domain detection
risk explanation system
user action controls
```

---

# Success Criteria

The feature is complete when:

```
malicious domains detected
risk levels displayed
weekly database updates working
user actions functional
```


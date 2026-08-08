# WiFi Profile Extractor for Windows

Part of the wf-thermos-eye WiFi security audit platform.

## Purpose

Extracts saved WiFi passwords from the local Windows system using the `netsh wlan` command. Works on Windows 10 and Windows 11.

## Requirements

- Windows 10 or Windows 11
- Python 3.8+ (for the Python script)
- Administrator privileges (required to view passwords)

## Usage

### Using the Batch File (Recommended)

Double-click `wifi_extractor.bat` - it will automatically request administrator privileges.

### Using Python Directly

Run as administrator:

```cmd
python wifi_extractor.py
```

### Options

```
--json      Output as JSON format
--output    Write to file instead of stdout
```

### Examples

```cmd
# Human-readable output
python wifi_extractor.py

# JSON output
python wifi_extractor.py --json

# Save to file
python wifi_extractor.py --json --output wifi_profiles.json
```

## Output Format

### Human-Readable

```
============================================================
SAVED WIFI PROFILES
============================================================

SSID: HomeNetwork
  Auth Type: WPA2-Personal
  Cipher: CCMP
  Password: mysecretpassword123
------------------------------------------------------------

Total profiles: 5
With passwords: 4
```

### JSON

```json
{
  "profiles": [
    {
      "ssid": "HomeNetwork",
      "auth_type": "WPA2-Personal",
      "cipher": "CCMP",
      "password": "mysecretpassword123",
      "connection_type": "WiFi"
    }
  ],
  "total": 5,
  "with_passwords": 4
}
```

## Security Notes

- This tool is for security auditing of networks you own or have authorization to test
- Passwords are displayed in plaintext - handle output securely
- Run only on systems you are authorized to access
- The tool does not transmit any data - all processing is local

## How It Works

1. Runs `netsh wlan show profiles` to list saved networks
2. For each profile, runs `netsh wlan show profile name="X" key=clear`
3. Parses the output to extract authentication type, cipher, and password
4. Formats and displays the results

## Troubleshooting

### "Passwords not visible"

Run as administrator. Without admin privileges, Windows hides the `key=clear` content.

### "Profile not found"

Some profiles may be corrupted or from a different user. The script skips these.

### "Python not found"

Install Python 3 from python.org and ensure it's added to your PATH.

# WF-Thermos-Eye Remote Cracker

Lightweight script for VPS-based WPA password cracking.

## Quick Start

```bash
# On VPS with GPU and hashcat installed
scp capture.22000 vps:/tmp/
ssh vps "python3 /path/to/remote_crack.py /tmp/capture.22000"
```

## Usage

```
python3 remote_crack.py <hash_file> [--wordlist PATH] [--rules PATH] [--json]
```

## Requirements

- Python 3.8+
- hashcat (GPU version recommended)
- Wordlist (rockyou.txt)

## Output

```bash
# Success
Password: mysecretpassword

# Not found
Password not found in wordlist

# JSON mode
{"status": "cracked", "password": "mysecretpassword"}
```

## Legacy Code

The `api/` and `worker/` directories contain FastAPI/Redis code from an earlier iteration. This overengineered approach was replaced with the standalone script above. The legacy code remains for reference if you need a more complex server setup.

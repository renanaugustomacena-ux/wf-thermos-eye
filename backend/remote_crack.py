#!/usr/bin/env python3
"""
WF-Thermos-Eye Remote Cracker

Standalone script for VPS-based GPU cracking. No FastAPI, no Redis, no Docker.
Upload hash file via SCP, run this script, get result.

Usage:
    python3 remote_crack.py <hash_file> [--wordlist PATH] [--rules PATH]

Example:
    scp capture.22000 vps:/tmp/
    ssh vps "python3 remote_crack.py /tmp/capture.22000"
"""

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Optional

WORDLISTS = [
    "/opt/wordlists/rockyou.txt",
    "/usr/share/wordlists/rockyou.txt",
    Path.home() / "wordlists" / "rockyou.txt",
]

RULES = [
    "/usr/share/hashcat/rules/best64.rule",
    "/opt/hashcat/rules/best64.rule",
]


def find_wordlist() -> Optional[str]:
    for wl in WORDLISTS:
        if Path(wl).exists():
            return str(wl)
    return None


def find_rules() -> Optional[str]:
    for rule in RULES:
        if Path(rule).exists():
            return str(rule)
    return None


def crack(hash_file: str, wordlist: Optional[str] = None, rules: Optional[str] = None) -> dict:
    if not Path(hash_file).exists():
        return {"status": "error", "message": f"Hash file not found: {hash_file}"}

    wl = wordlist or find_wordlist()
    if not wl:
        return {"status": "error", "message": "No wordlist found"}

    pot_file = hash_file + ".pot"

    cmd = [
        "hashcat",
        "-m", "22000",
        "-a", "0",
        "--potfile-path", pot_file,
        "-o", pot_file,
        hash_file,
        wl,
    ]

    rule_file = rules or find_rules()
    if rule_file:
        cmd.extend(["-r", rule_file])

    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=86400)
    except subprocess.TimeoutExpired:
        return {"status": "timeout"}
    except FileNotFoundError:
        return {"status": "error", "message": "hashcat not found"}

    if Path(pot_file).exists():
        content = Path(pot_file).read_text().strip()
        if ":" in content:
            password = content.split(":")[-1]
            Path(pot_file).unlink(missing_ok=True)
            return {"status": "cracked", "password": password}

    if result.returncode == 1:
        return {"status": "exhausted"}

    return {"status": "failed", "returncode": result.returncode, "stderr": result.stderr[:500]}


def main():
    parser = argparse.ArgumentParser(description="WF-Thermos-Eye Remote Cracker")
    parser.add_argument("hash_file", help="Path to hashcat 22000 format file")
    parser.add_argument("--wordlist", "-w", help="Path to wordlist")
    parser.add_argument("--rules", "-r", help="Path to rules file")
    parser.add_argument("--json", "-j", action="store_true", help="Output as JSON")
    args = parser.parse_args()

    result = crack(args.hash_file, args.wordlist, args.rules)

    if args.json:
        print(json.dumps(result))
    else:
        if result["status"] == "cracked":
            print(f"Password: {result['password']}")
        elif result["status"] == "exhausted":
            print("Password not found in wordlist")
        else:
            print(f"Error: {result.get('message', result.get('stderr', 'Unknown error'))}")

    sys.exit(0 if result["status"] == "cracked" else 1)


if __name__ == "__main__":
    main()

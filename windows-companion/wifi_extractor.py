#!/usr/bin/env python3
"""
WiFi Profile Extractor for Windows

Extracts saved WiFi passwords from the local system using netsh.
Part of the wf-thermos-eye WiFi security audit platform.

Usage:
    python wifi_extractor.py [--json] [--output FILE]

Options:
    --json    Output as JSON (default is human-readable)
    --output  Write to file instead of stdout

Requirements:
    - Windows 10/11
    - Administrator privileges (for key=clear)
"""

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass, asdict
from typing import List, Optional


@dataclass
class WifiProfile:
    """Represents a saved WiFi profile."""

    ssid: str
    auth_type: str
    cipher: str
    password: Optional[str]
    connection_type: str = "WiFi"


def run_netsh(args: List[str]) -> str:
    """Run netsh command and return output."""
    try:
        result = subprocess.run(
            ["netsh"] + args,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0,
        )
        return result.stdout
    except Exception as e:
        print(f"Error running netsh: {e}", file=sys.stderr)
        return ""


def get_profile_names() -> List[str]:
    """Get list of all saved WiFi profile names."""
    output = run_netsh(["wlan", "show", "profiles"])
    profiles = []

    for line in output.split("\n"):
        # Match profile lines in various languages
        # English: "All User Profile"
        # Italian: "Profilo tutti gli utenti"
        # German: "Profil für alle Benutzer"
        # French: "Profil de tous les utilisateurs"
        if ":" in line and ("profile" in line.lower() or "profil" in line.lower()):
            match = re.search(r":\s*(.+)$", line)
            if match:
                profile_name = match.group(1).strip()
                if profile_name and profile_name not in profiles:
                    profiles.append(profile_name)

    return profiles


def get_profile_details(profile_name: str) -> Optional[WifiProfile]:
    """Get detailed information about a WiFi profile including password."""
    output = run_netsh(["wlan", "show", "profile", f'name="{profile_name}"', "key=clear"])

    if not output or "not found" in output.lower():
        return None

    # Parse authentication type
    auth_patterns = [
        r"(?:Authentication|Autenticazione|Authentifizierung|Authentification)\s*:\s*(.+)",
    ]
    auth_type = "Unknown"
    for pattern in auth_patterns:
        match = re.search(pattern, output, re.IGNORECASE)
        if match:
            auth_type = match.group(1).strip()
            break

    # Parse cipher
    cipher_patterns = [
        r"(?:Cipher|Crittografia|Verschlüsselung|Chiffrement)\s*:\s*(.+)",
    ]
    cipher = "Unknown"
    for pattern in cipher_patterns:
        match = re.search(pattern, output, re.IGNORECASE)
        if match:
            cipher = match.group(1).strip()
            break

    # Parse password (key content)
    key_patterns = [
        r"(?:Key Content|Contenuto chiave|Schlüsselinhalt|Contenu de la cl[eé])\s*:\s*(.+)",
    ]
    password = None
    for pattern in key_patterns:
        match = re.search(pattern, output, re.IGNORECASE)
        if match:
            password = match.group(1).strip()
            break

    return WifiProfile(
        ssid=profile_name,
        auth_type=auth_type,
        cipher=cipher,
        password=password,
    )


def extract_all_profiles() -> List[WifiProfile]:
    """Extract all WiFi profiles with passwords."""
    profiles = []

    for name in get_profile_names():
        profile = get_profile_details(name)
        if profile:
            profiles.append(profile)

    return profiles


def format_human_readable(profiles: List[WifiProfile]) -> str:
    """Format profiles for human-readable output."""
    lines = [
        "=" * 60,
        "SAVED WIFI PROFILES",
        "=" * 60,
    ]

    for profile in profiles:
        lines.append("")
        lines.append(f"SSID: {profile.ssid}")
        lines.append(f"  Auth Type: {profile.auth_type}")
        lines.append(f"  Cipher: {profile.cipher}")
        if profile.password:
            lines.append(f"  Password: {profile.password}")
        else:
            lines.append("  Password: (not available or open network)")
        lines.append("-" * 60)

    lines.append("")
    lines.append(f"Total profiles: {len(profiles)}")
    lines.append(f"With passwords: {len([p for p in profiles if p.password])}")

    return "\n".join(lines)


def format_json(profiles: List[WifiProfile]) -> str:
    """Format profiles as JSON."""
    data = {
        "profiles": [asdict(p) for p in profiles],
        "total": len(profiles),
        "with_passwords": len([p for p in profiles if p.password]),
    }
    return json.dumps(data, indent=2, ensure_ascii=False)


def check_admin() -> bool:
    """Check if running with administrator privileges."""
    try:
        import ctypes
        return ctypes.windll.shell32.IsUserAnAdmin() != 0
    except Exception:
        return False


def main():
    parser = argparse.ArgumentParser(
        description="Extract saved WiFi passwords from Windows"
    )
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--output", "-o", help="Write to file instead of stdout")
    args = parser.parse_args()

    # Check for admin privileges
    if not check_admin():
        print(
            "WARNING: Not running as administrator. Passwords may not be visible.",
            file=sys.stderr,
        )
        print("Run with elevated privileges for full output.", file=sys.stderr)
        print("", file=sys.stderr)

    # Extract profiles
    profiles = extract_all_profiles()

    # Format output
    if args.json:
        output = format_json(profiles)
    else:
        output = format_human_readable(profiles)

    # Write output
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(output)
        print(f"Output written to {args.output}")
    else:
        print(output)


if __name__ == "__main__":
    main()

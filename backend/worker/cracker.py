"""
Hashcat Worker

Processes cracking jobs from the Redis queue.
"""

import json
import os
import re
import signal
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, Any

import redis

# Add parent directory to path for config import
sys.path.insert(0, str(Path(__file__).parent.parent))
from api.config import config


class HashcatWrapper:
    """Wrapper for hashcat GPU cracking."""

    def __init__(self, redis_client: redis.Redis):
        self.redis = redis_client
        self.hashcat_path = config.hashcat_path
        self.wordlist_dir = Path(config.wordlist_dir)
        self.rules_dir = Path(config.rules_dir)
        self.temp_dir = Path(config.temp_dir)
        self.temp_dir.mkdir(parents=True, exist_ok=True)

    def crack(self, job: Dict[str, Any]) -> Dict[str, Any]:
        """Run hashcat against a capture."""
        job_id = job["id"]

        # Write hashcat record to temp file
        hash_file = self.temp_dir / f"{job_id}.22000"
        payload = bytes.fromhex(job["payloadHex"])
        hash_file.write_bytes(payload)

        # Build wordlist paths
        wordlist_paths = []
        for wl in job.get("wordlists", ["rockyou"]):
            wl_path = self.wordlist_dir / f"{wl}.txt"
            if wl_path.exists():
                wordlist_paths.append(str(wl_path))

        if not wordlist_paths:
            return {
                "status": "failed",
                "error": "No valid wordlists found",
            }

        # Build rule arguments
        rule_args = []
        for rule in job.get("rules", []):
            rule_path = self.rules_dir / f"{rule}.rule"
            if rule_path.exists():
                rule_args.extend(["-r", str(rule_path)])

        # Output file for cracked password
        pot_file = self.temp_dir / f"{job_id}.pot"

        # Build hashcat command
        cmd = [
            self.hashcat_path,
            "-m", "22000",           # WPA-PBKDF2-PMKID+EAPOL
            "-a", "0",               # Dictionary attack
            "--status",              # Enable status output
            "--status-timer", "5",   # Status every 5 seconds
            "--machine-readable",    # Machine-readable status
            "--potfile-path", str(pot_file),
            "-o", str(pot_file),
            str(hash_file),
        ]
        cmd.extend(wordlist_paths)
        cmd.extend(rule_args)

        # Update job status
        self._update_job(job_id, {
            "status": "running",
            "startedAt": datetime.utcnow().isoformat(),
        })

        start_time = time.time()

        try:
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
            )

            while process.poll() is None:
                # Check for cancellation
                if self.redis.get(f"cancel:{job_id}"):
                    process.send_signal(signal.SIGTERM)
                    process.wait()
                    self._cleanup(hash_file, pot_file)
                    return {"status": "cancelled"}

                # Read and parse status output
                line = process.stdout.readline()
                if line:
                    status_update = self._parse_status(line)
                    if status_update:
                        self._update_job(job_id, status_update)

                time.sleep(0.5)

            # Process remaining output
            for line in process.stdout:
                status_update = self._parse_status(line)
                if status_update:
                    self._update_job(job_id, status_update)

            elapsed = time.time() - start_time

            # Check exit code
            if process.returncode == 0:
                # Success - password found
                password = self._read_potfile(pot_file)
                self._cleanup(hash_file, pot_file)
                return {
                    "status": "cracked",
                    "password": password,
                    "completedAt": datetime.utcnow().isoformat(),
                    "timeTakenSeconds": int(elapsed),
                    "method": f"wordlist:{','.join(job.get('wordlists', []))} + rule:{','.join(job.get('rules', []))}",
                }
            elif process.returncode == 1:
                # Exhausted - no password found
                self._cleanup(hash_file, pot_file)
                return {
                    "status": "exhausted",
                    "completedAt": datetime.utcnow().isoformat(),
                    "timeTakenSeconds": int(elapsed),
                }
            else:
                # Error
                self._cleanup(hash_file, pot_file)
                return {
                    "status": "failed",
                    "error": f"Hashcat exited with code {process.returncode}",
                    "completedAt": datetime.utcnow().isoformat(),
                }

        except Exception as e:
            self._cleanup(hash_file, pot_file)
            return {
                "status": "failed",
                "error": str(e),
                "completedAt": datetime.utcnow().isoformat(),
            }

    def _parse_status(self, line: str) -> Optional[Dict[str, Any]]:
        """Parse hashcat machine-readable status line."""
        # Machine-readable format: STATUS\tvalue\tvalue\t...
        if not line.startswith("STATUS"):
            return None

        parts = line.strip().split("\t")
        if len(parts) < 2:
            return None

        result = {}

        # Parse progress
        for i, part in enumerate(parts):
            if part == "PROGRESS" and i + 1 < len(parts):
                try:
                    progress = parts[i + 1].split("/")
                    if len(progress) == 2:
                        current = int(progress[0])
                        total = int(progress[1])
                        if total > 0:
                            result["progressPercent"] = round(current / total * 100, 2)
                except (ValueError, IndexError):
                    pass

            elif part == "SPEED" and i + 1 < len(parts):
                try:
                    # Speed format varies, try to extract number
                    speed_str = parts[i + 1]
                    match = re.search(r"(\d+)", speed_str)
                    if match:
                        result["speedHashesPerSec"] = int(match.group(1))
                except (ValueError, IndexError):
                    pass

            elif part == "CURKU" and i + 1 < len(parts):
                try:
                    result["candidatesTried"] = int(parts[i + 1])
                except (ValueError, IndexError):
                    pass

        return result if result else None

    def _read_potfile(self, pot_file: Path) -> Optional[str]:
        """Read cracked password from potfile."""
        try:
            if pot_file.exists():
                content = pot_file.read_text().strip()
                # Format: hash:password
                if ":" in content:
                    return content.split(":")[-1]
        except Exception:
            pass
        return None

    def _update_job(self, job_id: str, updates: Dict[str, Any]):
        """Update job data in Redis."""
        job_data = self.redis.get(f"job:{job_id}")
        if job_data:
            job = json.loads(job_data)
            job.update(updates)
            self.redis.set(f"job:{job_id}", json.dumps(job))

    def _cleanup(self, hash_file: Path, pot_file: Path):
        """Clean up temporary files."""
        for f in [hash_file, pot_file]:
            try:
                f.unlink(missing_ok=True)
            except Exception:
                pass


def worker_main():
    """Main worker loop."""
    print("Hashcat Worker starting...")

    redis_client = redis.Redis(
        host=config.redis_host,
        port=config.redis_port,
        db=config.redis_db,
        decode_responses=True,
    )

    cracker = HashcatWrapper(redis_client)

    print(f"Connected to Redis at {config.redis_host}:{config.redis_port}")
    print(f"Hashcat path: {config.hashcat_path}")
    print(f"Wordlist directory: {config.wordlist_dir}")
    print("Waiting for jobs...")

    while True:
        # Update heartbeat
        redis_client.set("worker:heartbeat", datetime.utcnow().isoformat(), ex=60)

        # Pop from queue (priority order: high, normal, low)
        job_id = None
        for priority in ["high", "normal", "low"]:
            result = redis_client.brpop(f"queue:{priority}", timeout=5)
            if result:
                _, job_id = result
                break

        if not job_id:
            continue

        # Get job data
        job_data = redis_client.get(f"job:{job_id}")
        if not job_data:
            print(f"Job {job_id} not found in Redis, skipping")
            continue

        job = json.loads(job_data)

        print(f"Processing job {job_id}: {job.get('ssid', 'unknown')} ({job.get('kind', 'unknown')})")

        # Run cracking
        result = cracker.crack(job)

        # Update final status
        job.update(result)
        redis_client.set(f"job:{job_id}", json.dumps(job))

        print(f"Job {job_id} completed: {result.get('status', 'unknown')}")
        if result.get("password"):
            print(f"  Password found: {result['password']}")


if __name__ == "__main__":
    worker_main()

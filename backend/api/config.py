"""
Backend Configuration

Loads configuration from environment variables with sensible defaults.
"""

import os
from dataclasses import dataclass


@dataclass
class Config:
    """Backend configuration."""

    # Redis connection
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0

    # API settings
    api_host: str = "0.0.0.0"
    api_port: int = 8443
    api_key: str = ""
    debug: bool = False

    # Hashcat settings
    hashcat_path: str = "/usr/bin/hashcat"
    wordlist_dir: str = "/data/wordlists"
    rules_dir: str = "/data/rules"
    temp_dir: str = "/tmp/hashcat"

    # Worker settings
    max_concurrent_jobs: int = 1
    default_timeout_hours: int = 24

    @classmethod
    def from_env(cls) -> "Config":
        """Load configuration from environment variables."""
        return cls(
            redis_host=os.getenv("REDIS_HOST", "localhost"),
            redis_port=int(os.getenv("REDIS_PORT", "6379")),
            redis_db=int(os.getenv("REDIS_DB", "0")),
            api_host=os.getenv("API_HOST", "0.0.0.0"),
            api_port=int(os.getenv("API_PORT", "8443")),
            api_key=os.getenv("API_KEY", ""),
            debug=os.getenv("DEBUG", "").lower() in ("1", "true", "yes"),
            hashcat_path=os.getenv("HASHCAT_PATH", "/usr/bin/hashcat"),
            wordlist_dir=os.getenv("WORDLIST_DIR", "/data/wordlists"),
            rules_dir=os.getenv("RULES_DIR", "/data/rules"),
            temp_dir=os.getenv("TEMP_DIR", "/tmp/hashcat"),
            max_concurrent_jobs=int(os.getenv("MAX_CONCURRENT_JOBS", "1")),
            default_timeout_hours=int(os.getenv("DEFAULT_TIMEOUT_HOURS", "24")),
        )


config = Config.from_env()

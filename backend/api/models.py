"""
Pydantic Models for API Request/Response
"""

from typing import Optional, List
from pydantic import BaseModel, Field


class SubmitRequest(BaseModel):
    """Request to submit a capture for cracking."""

    bssid: str = Field(..., description="AP MAC address (XX:XX:XX:XX:XX:XX)")
    ssid: str = Field(..., description="Network SSID")
    kind: str = Field(..., description="Capture kind: PMKID or EAPOL_4WAY")
    payloadHex: str = Field(..., description="Hashcat 22000 record in hex")
    priority: str = Field(default="normal", description="Job priority: low, normal, high")
    wordlists: Optional[List[str]] = Field(default=None, description="Wordlists to use")
    rules: Optional[List[str]] = Field(default=None, description="Rules to apply")
    timeoutHours: Optional[int] = Field(default=24, description="Timeout in hours")


class SubmitResponse(BaseModel):
    """Response after submitting a capture."""

    jobId: str = Field(..., description="Unique job identifier")
    status: str = Field(..., description="Job status: queued")
    position: Optional[int] = Field(default=None, description="Position in queue")
    estimatedStart: Optional[str] = Field(default=None, description="Estimated start time")


class JobStatus(BaseModel):
    """Job status response."""

    jobId: str
    status: str  # queued, running, cracked, exhausted, failed, cancelled
    password: Optional[str] = None
    progressPercent: Optional[float] = None
    speedHashesPerSec: Optional[int] = None
    candidatesTried: Optional[int] = None
    estimatedCompletion: Optional[str] = None


class JobResult(BaseModel):
    """Job result response."""

    jobId: str
    status: str
    password: Optional[str] = None
    foundAt: Optional[str] = None
    timeTakenSeconds: Optional[int] = None
    method: Optional[str] = None


class HealthResponse(BaseModel):
    """Health check response."""

    status: str
    redis: str
    workerActive: bool
    queuedJobs: int

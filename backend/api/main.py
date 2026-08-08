"""
WF-Thermos-Eye Hashcat Backend API

FastAPI server for receiving captures and managing cracking jobs.
"""

import json
import uuid
from datetime import datetime
from typing import Optional

import redis
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware

from .config import config
from .models import (
    SubmitRequest,
    SubmitResponse,
    JobStatus,
    JobResult,
    HealthResponse,
)
from .auth import verify_api_key


app = FastAPI(
    title="WF-Thermos-Eye Hashcat Backend",
    version="1.0.0",
    docs_url="/docs" if config.debug else None,
    redoc_url="/redoc" if config.debug else None,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict in production
    allow_methods=["*"],
    allow_headers=["*"],
)

# Redis client
redis_client: Optional[redis.Redis] = None


@app.on_event("startup")
async def startup_event():
    """Initialize Redis connection on startup."""
    global redis_client
    redis_client = redis.Redis(
        host=config.redis_host,
        port=config.redis_port,
        db=config.redis_db,
        decode_responses=True,
    )


@app.on_event("shutdown")
async def shutdown_event():
    """Close Redis connection on shutdown."""
    global redis_client
    if redis_client:
        redis_client.close()


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    try:
        redis_client.ping()
        redis_status = "connected"

        # Check worker heartbeat
        worker_heartbeat = redis_client.get("worker:heartbeat")
        worker_active = worker_heartbeat is not None

        # Count queued jobs
        queued = sum(
            redis_client.llen(f"queue:{p}")
            for p in ["high", "normal", "low"]
        )

        return HealthResponse(
            status="healthy",
            redis=redis_status,
            workerActive=worker_active,
            queuedJobs=queued,
        )
    except Exception as e:
        return HealthResponse(
            status="unhealthy",
            redis=str(e),
            workerActive=False,
            queuedJobs=0,
        )


@app.post("/jobs", response_model=SubmitResponse)
async def submit_job(
    request: SubmitRequest,
    _: str = Depends(verify_api_key),
):
    """Submit a capture for cracking."""
    job_id = str(uuid.uuid4())

    job_data = {
        "id": job_id,
        "bssid": request.bssid,
        "ssid": request.ssid,
        "kind": request.kind,
        "payloadHex": request.payloadHex,
        "priority": request.priority,
        "wordlists": request.wordlists or ["rockyou"],
        "rules": request.rules or [],
        "timeoutHours": request.timeoutHours or config.default_timeout_hours,
        "status": "queued",
        "createdAt": datetime.utcnow().isoformat(),
        "startedAt": None,
        "completedAt": None,
        "password": None,
        "error": None,
        "progressPercent": None,
        "speedHashesPerSec": None,
        "candidatesTried": None,
    }

    # Store job data
    redis_client.set(f"job:{job_id}", json.dumps(job_data))

    # Add to priority queue
    queue_key = f"queue:{request.priority}"
    redis_client.lpush(queue_key, job_id)

    # Get queue position
    position = redis_client.llen(queue_key)

    return SubmitResponse(
        jobId=job_id,
        status="queued",
        position=position,
        estimatedStart=None,
    )


@app.get("/jobs/{job_id}", response_model=JobStatus)
async def get_job_status(
    job_id: str,
    _: str = Depends(verify_api_key),
):
    """Get job status."""
    job_data = redis_client.get(f"job:{job_id}")
    if not job_data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found",
        )

    job = json.loads(job_data)

    return JobStatus(
        jobId=job_id,
        status=job["status"],
        password=job.get("password"),
        progressPercent=job.get("progressPercent"),
        speedHashesPerSec=job.get("speedHashesPerSec"),
        candidatesTried=job.get("candidatesTried"),
        estimatedCompletion=job.get("estimatedCompletion"),
    )


@app.get("/jobs/{job_id}/result", response_model=JobResult)
async def get_job_result(
    job_id: str,
    _: str = Depends(verify_api_key),
):
    """Get job result. Returns 202 if job not yet complete."""
    job_data = redis_client.get(f"job:{job_id}")
    if not job_data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found",
        )

    job = json.loads(job_data)

    if job["status"] not in ("cracked", "exhausted", "failed", "cancelled"):
        raise HTTPException(
            status_code=status.HTTP_202_ACCEPTED,
            detail="Job not yet complete",
            headers={"Retry-After": "30"},
        )

    # Calculate time taken
    time_taken = None
    if job.get("startedAt") and job.get("completedAt"):
        started = datetime.fromisoformat(job["startedAt"])
        completed = datetime.fromisoformat(job["completedAt"])
        time_taken = int((completed - started).total_seconds())

    return JobResult(
        jobId=job_id,
        status=job["status"],
        password=job.get("password"),
        foundAt=job.get("completedAt"),
        timeTakenSeconds=time_taken,
        method=job.get("method"),
    )


@app.delete("/jobs/{job_id}")
async def cancel_job(
    job_id: str,
    _: str = Depends(verify_api_key),
):
    """Cancel a job."""
    job_data = redis_client.get(f"job:{job_id}")
    if not job_data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found",
        )

    job = json.loads(job_data)

    if job["status"] in ("cracked", "exhausted", "failed", "cancelled"):
        return {"status": job["status"], "message": "Job already completed"}

    job["status"] = "cancelled"
    job["completedAt"] = datetime.utcnow().isoformat()
    redis_client.set(f"job:{job_id}", json.dumps(job))

    # Signal worker to stop if running
    redis_client.set(f"cancel:{job_id}", "1", ex=3600)

    return {"status": "cancelled"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host=config.api_host,
        port=config.api_port,
        reload=config.debug,
    )

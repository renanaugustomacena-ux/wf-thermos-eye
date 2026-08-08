"""
API Authentication

Simple API key authentication via header.
"""

from fastapi import Header, HTTPException, status

from .config import config


async def verify_api_key(authorization: str = Header(default="")):
    """
    Verify the API key from the Authorization header.

    Expected format: Bearer <api_key>
    """
    if not config.api_key:
        # No API key configured = auth disabled (dev mode only)
        return None

    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header",
        )

    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format. Expected: Bearer <token>",
        )

    token = parts[1]
    if token != config.api_key:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid API key",
        )

    return token

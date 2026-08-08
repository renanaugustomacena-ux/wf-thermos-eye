@echo off
REM WF-Thermos-Eye WiFi Extractor
REM Runs the Python script with administrator privileges

:: Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process '%~dpnx0' -Verb RunAs"
    exit /b
)

:: Change to script directory
cd /d "%~dp0"

:: Check if Python is available
where python >nul 2>&1
if %errorLevel% neq 0 (
    echo Python not found in PATH.
    echo Please install Python 3 and add it to PATH.
    pause
    exit /b 1
)

:: Run the extractor
python wifi_extractor.py %*

:: Keep window open
pause

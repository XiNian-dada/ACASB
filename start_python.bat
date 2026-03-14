@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%acasb-analysis"
set "CONFIG_FILE=%SCRIPT_DIR%config.properties"
set "PYTHON_HOST=127.0.0.1"
set "PYTHON_PORT=5000"

if exist "%CONFIG_FILE%" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%CONFIG_FILE%") do (
    if /I "%%A"=="python.host" set "PYTHON_HOST=%%B"
    if /I "%%A"=="python.port" set "PYTHON_PORT=%%B"
  )
)

set "ACASB_PYTHON_HOST=%PYTHON_HOST%"
set "ACASB_PYTHON_PORT=%PYTHON_PORT%"

echo Starting Python API Service...
echo.

echo Checking Python installation...
python --version
echo.

echo Python API target: %PYTHON_HOST%:%PYTHON_PORT%
echo.

echo Starting ACASB Python API on port %PYTHON_PORT%...
cd /d "%PROJECT_DIR%"
python api_server.py

pause

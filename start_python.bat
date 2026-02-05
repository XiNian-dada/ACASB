@echo off
echo Starting Python API Service...
echo.

echo Checking Python installation...
python --version
echo.

echo Starting ACASB Python API on port 5000...
cd acasb-analysis
python api_server.py

pause
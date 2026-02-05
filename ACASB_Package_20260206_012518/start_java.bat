@echo off
echo Starting Java Backend Service...
echo.

set JAVA_HOME=D:\Zulu17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Java from: %JAVA_HOME%
java -version
echo.

echo Starting ACASB Java Backend on port 8080...
java -jar ACASB-0.0.1-SNAPSHOT.jar

pause
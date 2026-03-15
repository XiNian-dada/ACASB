@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "CONFIG_FILE=%SCRIPT_DIR%config.properties"

set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=acasb"
set "DB_USERNAME=root"
set "DB_PASSWORD="
set "SERVER_PORT=8081"
set "PYTHON_HOST=localhost"
set "PYTHON_PORT=5000"
set "TEMP_FOLDER=./temp"
set "STORAGE_FOLDER=./uploads"
set "DATASET_STORAGE_FOLDER=./dataset-storage"
set "AI_ENABLED=false"
set "AI_BASE_URL=https://api.openai.com"
set "AI_INTERFACE=responses"
set "AI_RESPONSES_PATH=/v1/responses"
set "AI_CHAT_PATH=/v1/chat/completions"
set "AI_API_KEY="
set "AI_MODEL=gpt-4.1-mini"
set "AUTH_JWT_ENABLED=true"
set "AUTH_JWT_SECRET="
set "AUTH_JWT_EXPIRES_HOURS=720"

if exist "%CONFIG_FILE%" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%CONFIG_FILE%") do (
    if /I "%%A"=="db.host" set "DB_HOST=%%B"
    if /I "%%A"=="db.port" set "DB_PORT=%%B"
    if /I "%%A"=="db.name" set "DB_NAME=%%B"
    if /I "%%A"=="db.username" set "DB_USERNAME=%%B"
    if /I "%%A"=="db.password" set "DB_PASSWORD=%%B"
    if /I "%%A"=="server.port" set "SERVER_PORT=%%B"
    if /I "%%A"=="python.host" set "PYTHON_HOST=%%B"
    if /I "%%A"=="python.port" set "PYTHON_PORT=%%B"
    if /I "%%A"=="temp.folder" set "TEMP_FOLDER=%%B"
    if /I "%%A"=="storage.folder" set "STORAGE_FOLDER=%%B"
    if /I "%%A"=="dataset.storage.folder" set "DATASET_STORAGE_FOLDER=%%B"
    if /I "%%A"=="ai.analysis.enabled" set "AI_ENABLED=%%B"
    if /I "%%A"=="ai.analysis.base-url" set "AI_BASE_URL=%%B"
    if /I "%%A"=="ai.analysis.api-interface" set "AI_INTERFACE=%%B"
    if /I "%%A"=="ai.analysis.responses-path" set "AI_RESPONSES_PATH=%%B"
    if /I "%%A"=="ai.analysis.chat-completions-path" set "AI_CHAT_PATH=%%B"
    if /I "%%A"=="ai.analysis.api-key" set "AI_API_KEY=%%B"
    if /I "%%A"=="ai.analysis.model" set "AI_MODEL=%%B"
    if /I "%%A"=="auth.jwt.enabled" set "AUTH_JWT_ENABLED=%%B"
    if /I "%%A"=="auth.jwt.secret" set "AUTH_JWT_SECRET=%%B"
    if /I "%%A"=="auth.jwt.expires-hours" set "AUTH_JWT_EXPIRES_HOURS=%%B"
  )
)

echo Starting Java Backend Service...
echo.

if defined JAVA_HOME (
  set PATH=%JAVA_HOME%\bin;%PATH%
  echo Using Java from: %JAVA_HOME%
) else (
  echo Using Java from PATH
)
java -version
echo.

echo Database target: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo Java service port: %SERVER_PORT%
echo Python service: %PYTHON_HOST%:%PYTHON_PORT%
echo.

echo AI interface: %AI_INTERFACE%
echo.
echo JWT auth enabled: %AUTH_JWT_ENABLED%
echo.

set "SPRING_ARGS=--spring.datasource.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=Asia/Shanghai --spring.datasource.username=%DB_USERNAME% --spring.datasource.password=%DB_PASSWORD% --server.port=%SERVER_PORT% --python.service.host=%PYTHON_HOST% --python.service.port=%PYTHON_PORT% --app.temp-folder=%TEMP_FOLDER% --app.storage-folder=%STORAGE_FOLDER% --app.dataset-storage-folder=%DATASET_STORAGE_FOLDER% --ai.analysis.enabled=%AI_ENABLED% --ai.analysis.base-url=%AI_BASE_URL% --ai.analysis.api-interface=%AI_INTERFACE% --ai.analysis.responses-path=%AI_RESPONSES_PATH% --ai.analysis.chat-completions-path=%AI_CHAT_PATH% --ai.analysis.api-key=%AI_API_KEY% --ai.analysis.model=%AI_MODEL% --auth.jwt.enabled=%AUTH_JWT_ENABLED% --auth.jwt.secret=%AUTH_JWT_SECRET% --auth.jwt.expires-hours=%AUTH_JWT_EXPIRES_HOURS%"

echo Starting ACASB Java Backend on port %SERVER_PORT%...
if exist "%SCRIPT_DIR%ACASB.jar" (
  java -jar "%SCRIPT_DIR%ACASB.jar" %SPRING_ARGS%
) else (
  java -jar "%SCRIPT_DIR%ACASB-0.0.1-SNAPSHOT.jar" %SPRING_ARGS%
)

pause

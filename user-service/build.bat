@echo off
REM Build script for User Service

echo Building User Service...

REM Check if mvnw.cmd exists, if not copy from parent or use mvn command
if not exist "mvnw.cmd" (
    echo Maven wrapper not found, using system Maven...
    call mvn clean package -DskipTests
) else (
    call mvnw.cmd clean package -DskipTests
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo JAR file: target\user-service-1.0.0.jar
) else (
    echo.
    echo Build failed with error code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

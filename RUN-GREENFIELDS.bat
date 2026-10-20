@echo off
title GreenFields Crop Monitoring

set "USB=%~d0"
set "JAVA_HOME=%USB%\tools\windows\jdk-17"
set "PROJECT=%USB%\projects\GreenFieldsCropMonitoring"

cd /d "%PROJECT%" || (
    echo ERROR: Project folder not found.
    pause
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Portable Java 17 not found on USB.
    pause
    exit /b 1
)

if not exist "target\greenfields-crop-monitoring.jar" (
    echo ERROR: Project JAR not found.
    pause
    exit /b 1
)

echo Starting GreenFields Crop Monitoring...
echo Open http://localhost:8080 in your browser.
echo.

"%JAVA_HOME%\bin\java.exe" -jar "target\greenfields-crop-monitoring.jar"

echo.
echo GreenFields has stopped.
pause

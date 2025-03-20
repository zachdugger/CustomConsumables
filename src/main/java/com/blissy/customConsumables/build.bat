@echo off
echo Building Custom Consumables mod...

REM Check for Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java not found. Please install Java 8 or higher.
    exit /b 1
)

REM Build using Gradle wrapper
if exist gradlew.bat (
    call gradlew.bat build
) else (
    echo Gradle wrapper not found. Please run with a proper Gradle installation.
    exit /b 1
)

REM Check if build succeeded
if %ERRORLEVEL% equ 0 (
    echo Build successful!
    echo The mod JAR file is located at: build\libs\customconsumables-1.0.jar
    echo Copy this file to your Minecraft mods folder to use.
) else (
    echo Build failed. Please check the error messages above.
    exit /b 1
)

echo Done!
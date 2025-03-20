#!/bin/bash

# Build script for Custom Consumables mod

echo "Building Custom Consumables mod..."

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Java not found. Please install Java 8 or higher."
    exit 1
fi

# Check for Gradle
if command -v ./gradlew &> /dev/null; then
    # Use Gradle wrapper if available
    ./gradlew build
elif command -v gradle &> /dev/null; then
    # Use system Gradle if installed
    gradle build
else
    echo "Gradle not found. Please install Gradle or use the provided wrapper."
    exit 1
fi

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "The mod JAR file is located at: build/libs/customconsumables-1.0.jar"
    echo "Copy this file to your Minecraft mods folder to use."
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi

echo "Done!"
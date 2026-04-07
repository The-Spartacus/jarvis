#!/bin/bash
# Jarvis Terminal Launcher Script
# Run from any directory - automatically manages configuration

set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_PATH="$SCRIPT_DIR/target/JarvisTerminal.jar"
CONFIG_PATH="$SCRIPT_DIR/config"

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ Error: JarvisTerminal.jar not found at $JAR_PATH"
    echo "Please run: cd $SCRIPT_DIR && mvn clean package"
    exit 1
fi

# Check if config exists
if [ ! -f "$CONFIG_PATH/config.properties" ]; then
    echo "❌ Error: config.properties not found at $CONFIG_PATH/config.properties"
    echo "Please run: cd $SCRIPT_DIR && nano config/config.properties"
    exit 1
fi

# Check if API key is configured (OpenAI or Gemini)
if ! grep -qE "(OPENAI_API_KEY|GOOGLE_API_KEY)=(sk-|AIza)" "$CONFIG_PATH/config.properties"; then
    echo "⚠️  Warning: API key not configured!"
    echo "Please edit: $CONFIG_PATH/config.properties"
    echo "Configure one of the following:"
    echo "  - OpenAI: OPENAI_API_KEY=sk-your-key-here"
    echo "  - Gemini: GOOGLE_API_KEY=AIza..."
    exit 1
fi

# Run from the script directory (so config/config.properties is found)
echo "🚀 Starting Jarvis Terminal..."
cd "$SCRIPT_DIR"
exec java -jar "$JAR_PATH" "$@"

#!/usr/bin/env bash

# Script to restart LSP server by killing existing process
# IntelliJ will automatically restart it when needed

echo "Searching for LSP server process..."
PID=$(ps aux | grep "ch.turic.lsp.LSPServerMain" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "No LSP server process found"
else
    echo "Found LSP server process with PID: $PID"
    echo "Killing process..."
    kill -9 $PID

    # Verify it was killed
    if ps -p $PID > /dev/null 2>&1; then
        echo "Failed to kill process $PID"
        exit 1
    else
        echo "LSP server process $PID killed successfully"
        echo "IntelliJ should automatically restart it when needed"
    fi
fi
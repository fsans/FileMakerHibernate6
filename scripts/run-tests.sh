#!/bin/bash
# Run all tests and generate Allure results
echo "Running tests..."
cd "$(dirname "$0")/.."
mvn clean test
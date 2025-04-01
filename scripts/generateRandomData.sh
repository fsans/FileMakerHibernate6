#!/bin/bash

# Script to generate random test data for FileMaker Hibernate tests
# Usage: ./generateRandomData.sh

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Change to the project root directory (parent of scripts)
cd "$SCRIPT_DIR/.."

echo "Generating random test data..."
echo "This will create 100 random contacts in the database."
echo "Press Ctrl+C to cancel, or any other key to continue..."
read -n 1 -s

# Run the test data generation
mvn test -Dtest=GenerateRandomDataTest -DexcludeGroups=""

# Check if the command was successful
if [ $? -eq 0 ]; then
    echo "Test data generation completed successfully."
else
    echo "Error: Test data generation failed."
    exit 1
fi
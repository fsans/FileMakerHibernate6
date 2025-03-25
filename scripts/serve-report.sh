#!/bin/bash
# Generate and serve Allure report
echo "Serving Allure report..."
cd "$(dirname "$0")/.."
mvn allure:serve
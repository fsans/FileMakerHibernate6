#!/bin/bash
# Generate static Allure report
echo "Generating Allure report..."
cd "$(dirname "$0")/.."
mvn allure:report
echo "Report generated in target/site/allure-maven-plugin"
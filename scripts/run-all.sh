#!/bin/bash
# Run tests and generate report
echo "Running full test suite with report..."
cd "$(dirname "$0")/.."
./scripts/run-tests.sh && ./scripts/serve-report.sh
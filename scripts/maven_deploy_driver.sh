#!/bin/sh

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Extract default version from pom.xml
DEFAULT_VERSION=$(grep -A1 "<artifactId>FileMakerDialect</artifactId>" "${PROJECT_ROOT}/pom.xml" | grep version | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

# Use provided version or default version
VERSION=${1:-$DEFAULT_VERSION}

if [ -z "$VERSION" ]; then
    echo "Error: Could not determine version from pom.xml and no version parameter provided"
    echo "Usage: $0 [version]"
    exit 1
fi

echo "Using version: ${VERSION}"

# Change to project root directory
cd "${PROJECT_ROOT}"

mvn install:install-file \
-Dfile=drivers/fmjdbc.${VERSION}.jar \
-DgroupId=com.filemaker.jdbc.Driver \
-DartifactId=fmjdbc \
-Dversion=${VERSION} \
-Dpackaging=jar \
-DgeneratePom=true
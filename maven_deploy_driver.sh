#!/bin/sh

# Check if version parameter is provided
if [ $# -eq 0 ]; then
    echo "Error: Version parameter is required"
    echo "Usage: $0 <version>"
    exit 1
fi

VERSION=$1

mvn install:install-file \
-Dfile=./src/main/resources/fmjdbc.${VERSION}.jar \
-DgroupId=com.filemaker.jdbc.Driver \
-DartifactId=fmjdbc \
-Dversion=${VERSION} \
-Dpackaging=jar \
-DgeneratePom=true

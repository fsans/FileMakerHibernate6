#!/bin/sh

# Deploy FileMaker JDBC driver to local Maven repository
# The driver JAR must be placed in ./src/main/resources/fmjdbc.<version>.jar
# Driver is not included in repo - obtain from FileMaker Server installation
#
# Usage: ./maven_deploy_driver.sh 22.0.1
#
# Check if version parameter is provided
if [ $# -eq 0 ]; then
    echo "Error: Version parameter is required"
    echo "Usage: $0 <version>"
    exit 1
fi

VERSION=$1

mvn install:install-file \
-Dfile=./src/main/resources/fmjdbc_${VERSION}.jar \
-DgroupId=com.filemaker.jdbc.Driver \
-DartifactId=fmjdbc \
-Dversion=${VERSION} \
-Dpackaging=jar \
-DgeneratePom=true

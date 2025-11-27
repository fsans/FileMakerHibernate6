#!/bin/sh

# Deploy FileMaker Hibernate Dialect to local Maven repository
# Run 'mvn package' first to build the JAR
#
# Usage: ./maven_deploy_dialect.sh 21.0.2
#
# Check if version parameter is provided
if [ $# -eq 0 ]; then
    echo "Error: Version parameter is required"
    echo "Usage: $0 <version>"
    exit 1
fi

VERSION=$1

mvn install:install-file \
-Dfile=./target/FileMakerDialect-${VERSION}.jar \
-DgroupId=com.filemaker.hibernate.dialect \
-DartifactId=filemakerdialect \
-Dversion=${VERSION} \
-Dpackaging=jar \
-DgeneratePom=true
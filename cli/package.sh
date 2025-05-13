#!/bin/bash

cd "$(dirname "$(readlink -f "$0")")"

echo "Working directory: $(pwd)"

# Read VERSION from turicum_versions.turi
VERSION=$(grep -m1 '^let VERSION *= *"' ../turicum_versions.turi | sed -E 's/^let VERSION *= *"(.*)";/\1/')
TVERSION=$(echo "$VERSION" | sed 's/-SNAPSHOT$//')

export VERSION

echo "Version=$VERSION"
echo "Translated Version=$TVERSION"

mkdir -p target/JARS
rm -rf target/JARS/*
unzip "target/turicum-cli-${VERSION}-distribution.zip" -d target/JARS

# Function to create package based on the operating system
create_package() {
    local INSTALLER_TYPE=$1
    jpackage --input target/JARS \
        --name turicum \
        --app-version "${TVERSION}" \
        --main-jar "turicum-cli-${VERSION}.jar" \
        --main-class ch.turic.cli.Main \
        --type "$INSTALLER_TYPE" \
        --dest output \
        --java-options -Xmx2048m \
        --resource-dir src/packaging-resources
}

# Detect the operating system and create appropriate package
case "$(uname -s)" in
    Linux*)
        echo "Creating deb package"
        create_package deb
        ;;
    Darwin*)
        echo "Creating pkg package"
        create_package pkg
        ;;
    *)
        echo "Unsupported operating system"
        exit 1
        ;;
esac



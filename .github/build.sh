#!/bin/sh

# Download build script`
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/main/ci-build.sh

# Override mvn function to add -DskipTests
mvn() {
  command mvn -DskipTests "$@"
}
export -f mvn

# Run build
sh ci-build.sh

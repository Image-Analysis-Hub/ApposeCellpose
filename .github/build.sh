#!/bin/sh
export MAVEN_OPTS="-DskipTests"
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/main/ci-build.sh
sh ci-build.sh

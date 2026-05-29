#!/bin/bash

curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/main/ci-build.sh
BUILD_ARGS=-DskipTests sh ci-build.sh



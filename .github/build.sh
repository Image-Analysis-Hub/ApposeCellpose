#!/bin/bash

curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/main/ci-build.sh
TEST_THRESHOLD=10 sh ci-build.sh

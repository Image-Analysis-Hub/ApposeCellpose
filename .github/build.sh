#!/bin/bash

# Create a wrapper script that shadows the real mvn
mkdir -p /tmp/mvn-bin
cat > /tmp/mvn-bin/mvn << 'MVNWRAPPER'
#!/bin/bash
exec $(which -a mvn | tail -1) -DskipTests "$@"
MVNWRAPPER
chmod +x /tmp/mvn-bin/mvn

# Put our wrapper first in PATH
export PATH="/tmp/mvn-bin:$PATH"

curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/main/ci-build.sh
bash ci-build.sh


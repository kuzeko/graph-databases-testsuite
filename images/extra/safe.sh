#!/bin/bash
set -eu -o pipefail
IFS=$'\n\t'

export JAVA_OPTIONS='-Xms4G -Xmn128M -Xmx10G'
export HADOOP_HEAPSIZE='12288'

# Compute current path and set working dir so maven works
SCRIPTPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPTPATH"

#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : loadSampleData.sh
# -----------------------------------------------------------------------------
# Usage:
#   % sampleData/loadSampleData.sh
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    GTS_HOME=".";  # - default to current dir
    exit 99
fi
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------
GTS_DEBUG=0

# --- usage (and exit)
function usage() {
    echo "Usage: $0"
    exit 1
}

# -----------------------------------------------------------------------------

# ---
echo "Creating 'demo' Account ..."
${GTS_HOME}/bin/admin.pl Account -account=demo -nopass -create

# ---
echo "Creating 'demo/demo' Device ..."
${GTS_HOME}/bin/admin.pl Device  -account=demo -device=demo  -create
echo "Creating 'demo/demo2' Device ..."
${GTS_HOME}/bin/admin.pl Device  -account=demo -device=demo2 -create

# ---
echo "Loading sample data ..."
${GTS_HOME}/bin/dbAdmin.pl -load=EventData -dir=./sampleData -overwrite

# --- exit normally
exit 0

# ---

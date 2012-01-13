#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : parseFile.sh
# -----------------------------------------------------------------------------
# Description:
#   This command-line utility loads previously stored OpenDMTP event data files
#   into the 'gtsdmtp' server (and ultimately into the database).
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    exit 99
fi
# -----------------------------------------------------------------------------

QUIET='-quiet'
${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.servers.gtsdmtp.ParseFile $*

# ---

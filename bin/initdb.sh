#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : initdb.sh
# -----------------------------------------------------------------------------
# Valid Options:
#     -rootUser=<id>    MySQL user with permissions to create a database and tables
#     -rootPass=<pass>  Password for specified user
# This command is equivalent to running the following:
#     % bin/dbAdmin.pl -createdb -grant -tables -user=<root>
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh
# -----------------------------------------------------------------------------
GTS_DEBUG=0

# --- usage (and exit)
function usage() {
    echo "Usage: $0 -rootUser=<id> -rootPass=<pass>"
    exit 1
}

# -----------------------------------------------------------------------------

# --- Main entry point
JMAIN="org.opengts.db.DBConfig"
JMAIN_ARGS="'-conf=${GTS_CONF}' -log.file.enable=false"

# --- default account:device
ACCOUNT='opendmtp'
DEVICE='mobile'
NEWDEV=""; # "-newAccount=${ACCOUNT} -newDevice=${DEVICE}";

# ---
JMAIN="org.opengts.db.DBConfig"
CMD_ARGS="-initTables ${NEWDEV} $1 $2 $3 $4 $5"
COMMAND="${CMD_JAVA} -classpath ${CPATH} ${JMAIN} ${JMAIN_ARGS} ${CMD_ARGS}"
if [ $GTS_DEBUG -ne 0 ]; then
    echo "${COMMAND}"
fi
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
else
    eval "${COMMAND}"
fi

# ---

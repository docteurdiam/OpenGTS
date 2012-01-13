#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : urlGet.sh
# -----------------------------------------------------------------------------
# Description:
#   This command-line utility is similar to the command 'wget' in that it will
#   attempt to copy a file from the specified URL to the local disk.
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    exit 99
fi
# -----------------------------------------------------------------------------

# --- usage (and exit)
function usage() {
    echo "Usage:"
    echo "  Display this help:"
    echo "    $0 -h"
    echo "  Read a file from the specified URL:"
    echo "    $0 [-dir <dir>] [-out <file>] -url <URL>"
    exit 1
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- 
ARGS=""
DEBUG=0
QUIET="-quiet"
URL=""
TODIR=""
TOFILE=""
OVERWRITE=0

# --- argument
while [ $# -gt 0 ]; do
    case "$1" in 
        "-h" | "-help") 
            usage
            ;;
        "-dir" | "-todir" )
            TODIR=$2
            shift
            ;;
        "-to" | "-tofile" | "-out" | "-file" )
            TOFILE=$2
            shift
            ;;
        "-url")
            URL=$2
            shift
            ;;
        "-overwrite")  # - not currently supported
            OVERWRITE=1
            ;;
        "-debug") 
            DEBUG=1
            QUIET=""
            ;;
        "--")
            shift
            break
            ;;
        *)
            echo "Invalid argument! [$1]"
            usage
            ;;
    esac
    shift
done

# --- URL specified?
if [ "${URL}" = "" ]; then
    echo "Missing URL"
    usage
fi

# -----------------------------------------------------------------------------

# --- debug mode
if [ $DEBUG -eq 1 ]; then
    ARGS="${ARGS} -debugMode";
fi

# --- make sure file logging is disabled
ARGS="${ARGS} -log.file.enable=false"

# --- "to" directory/file
if [ "${TODIR}" != "" ]; then
    ARGS="${ARGS} -dir=${TODIR}"
fi
if [ "${TOFILE}" != "" ]; then
    ARGS="${ARGS} -to=${TOFILE}"
fi
ARGS="${ARGS} -url=${URL}"

# --- execute
${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.util.FileTools ${ARGS} $*

# ---

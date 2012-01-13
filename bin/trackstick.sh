#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : rgTest.sh
# -----------------------------------------------------------------------------
# Description:
#   This command-line utility allows testing of the ReverseGeocodeProvider for a
#   specific Account or PrivateLabel name.
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    exit 99
fi
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------

# --- options
QUIET="-quiet"
DEBUG=
CMD_ARGS=

# --- check arguments
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - quiet
        "-quiet" | "-q" ) 
            QUIET="-quiet"
            DEBUG=
            ;;

        # - verbose
        "-verbose" | "-v" | "-debug" | "-debugMode" ) 
            #echo "DebugMode"
            QUIET=
            DEBUG="-debugMode"
            GTS_DEBUG=1
            ;;

        # ------------------------------------------------------

        # - Estimate/Save odometer
        "-saveOdom" ) 
            CMD_ARGS="${CMD_ARGS} -saveOdom=true"
            ;;

        # - Do Not Estimate/Save odometer
        "-noOdom" ) 
            CMD_ARGS="${CMD_ARGS} -saveOdom=false"
            ;;

        # ------------------------------------------------------

        # - Add ignition state
        "-addIgn" | "-ign" ) 
            CMD_ARGS="${CMD_ARGS} -addIgn=true"
            ;;

        # ------------------------------------------------------

        # - Pre-Clear existing records
        "-preClear" | "-clear" ) 
            CMD_ARGS="${CMD_ARGS} -preClear=true"
            ;;

        # - Pre-Clear existing records (then exit)
        "-preClearOnly" ) 
            CMD_ARGS="${CMD_ARGS} -preClearOnly=true"
            ;;

        # ------------------------------------------------------

        # - Account
        "-a" | "-account" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -account=$2"
                shift
            else
                echo "Missing 'account' Account argument"
                exit 99
            fi
            ;;

        # - Device
        "-d" | "-device" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -device=$2"
                shift
            else
                echo "Missing 'device' Device argument"
                exit 99
            fi
            ;;

        # - TimeZone
        "-tz" | "-tmz" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -tmz=$2"
                shift
            else
                echo "Missing 'tmz' TimeZone argument"
                exit 99
            fi
            ;;

        # - TimeZone
        "-csv" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} '-csv=$2'"
                shift
            else
                echo "Missing 'csv' TrackStick CSV file argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - include regular argument
        * )
            CMD_ARGS="${CMD_ARGS} '$1'"
            ;;

        # - skip remaining args
        "--" )
            shift
            break
            ;;

    esac
    shift
done

# ---
JMAIN="org.opengts.db.TrackStick"
JMAIN_ARGS="${DEBUG} '-conf=${GTS_CONF}' -log.file.enable=false"
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
#${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.db.TrackStick ${DEBUG} ${ARGS} $*

# --- exit normally
exit 0

# ---


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

        # - PrivateLabel
        "-pl" | "-privLabel" ) 
            #echo "PrivateLabel $2"
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -pl=$2"
                shift
            else
                echo "Missing 'pl' PrivateLabel argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - GepPoint
        "-gp" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -gp=$2"
                shift
            else
                echo "Missing 'gp' GeoPoint argument"
                exit 99
            fi
            ;;

        # - Address
        "-addr" ) 
            #echo "Address $2"
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -addr='$2'"
                shift
            else
                echo "Missing 'addr' Address argument"
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
JMAIN="org.opengts.geocoder.ReverseGeocodeProviderAdapter"
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
#${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.geocoder.ReverseGeocodeProviderAdapter ${DEBUG} ${ARGS} $*

# --- exit normally
exit 0

# ---


#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : startServers.sh
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    #export GTS_HOME=`realpath .`;  # - default to current dir
    # - WARNING: 'realpath' may not exist on some Linux distributions (ie. CentOS, Fedora, ...)
    export GTS_HOME=`/bin/pwd`;  # - default to current dir
    exit 99; # - exit anyway
fi
. ${GTS_HOME}/bin/common.sh # - sets "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------

# --- file containing the list of servers to start
# - This file should contain startup commands in the following form:
# -   execServer "Server Description" "server"  "${option}"  "-option=someOptionHere" 
# -   execServer "GTS OpenDMTP"       "gtsdmtp" "${option}"  "-debugMode"
SERVER_LIST="${GTS_HOME}/bin/serverList"
SERVER_LIST_CUSTOM="${GTS_HOME}/bin/serverList.custom"

# --- options
DEBUG_MODE=0    # - debug mode

# -----------------------------------------------------------------------------

# --- start servers
function runServers() {
    # arg $1 = <Options>
    local option="$1"
    echo ""
    if [ -f "${SERVER_LIST}" ]; then
        . ${SERVER_LIST}
    else
        echo "File does not exist: ${SERVER_LIST}"
    fi
    echo ""
}

# -----------------------------------------------------------------------------

# --- exec server
function execServer() {
    # arg $1 = <ServerDescription>
    # arg $2 = <ServerName>
    # arg $3 = <Options>
    # arg $4 = <AltOptions>
    local descr="$1"
    local server="$2"
    local option="$3"
    local altOpt="$4"
    
    # - debug mode
    local debugArg="";
    if [ ${DEBUG_MODE} -ne 0 ]; then
        debugArg="-debug";
    fi
    
    # - stop/start
    if   [ "${option}" = "-list" ]; then
        echo "Server ${server} - '${descr}'"
    elif [ "${option}" = "-kill" ] || [ "${option}" = "-stop" ]; then
        echo ""
        echo "Stopping ${server} - '${descr}' ..."
        ${GTS_HOME}/bin/runserver.pl -s ${server} ${debugArg} -kill
    else
        echo ""
        echo "Starting ${server} - '${descr}': ${option} -- ${altOpt}"
        ${GTS_HOME}/bin/runserver.pl -s ${server} ${debugArg} ${option} -- ${altOpt}
    fi

}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# "startServers.sh" Entry Point:

# --- display usage (and exit)
function usage() {
    echo "Usage: "
    echo "   $0 [-debug] [-custom] -start"
    echo "   $0 [-debug] [-custom] -restart"
    echo "   $0 -stop"
    echo "   $0 -kill"
    echo "   $0 -list"
    echo "Options:"
    echo "   -debug     - enable debug logging level"
    echo "   -custom    - load custom serverList (if available)"
    echo "   -start     - start servers"
    echo "   -stop      - stop servers"
    echo "   -restart   - restart servers"
    echo "   -list      - list servers"
    exit 1
}

# --- are we root?
WHOAMI=`whoami`;
if [ "$WHOAMI" = "root" ]; then
    echo "--------------------------------------------------------------------"
    echo "WARNING: This server startup script should not be run as user 'root'"
    echo "--------------------------------------------------------------------"
fi

# --- make sure we have at least one argument
if [ $# -le 0 ]; then
    echo "Missing arguments ..."
    usage
fi

# --- chack argument
while (( "$#" )); do
    case "$1" in 

        # --------------------------------------------
        
        # - Debug
        "-debug" | "-debugMode" | "-verbose" ) 
            DEBUG_MODE=1
            shift
            ;;

        # - No Debug
        "-nodebug" ) 
            DEBUG_MODE=0
            shift
            ;;

        # --------------------------------------------

        # - List
        "-list" | "list" ) 
            runServers "-list"
            exit 0
            ;;

        # --------------------------------------------

        # - use "serverList.custom"
        "-custom" | "custom" ) 
            SERVER_LIST="${SERVER_LIST_CUSTOM}"
            shift
            ;;

        # - Start
        "-start" | "start" ) 
            runServers ""
            exit 0
            ;;

        # - Stop
        "-stop" | "stop" | "-kill" | "kill" ) 
            runServers "-kill"
            exit 0
            ;;

        # - Restart
        "-restart" | "restart" ) 
            runServers "-kill"
            runServers ""
            exit 0
            ;;

        # --------------------------------------------

        # - error
        * )
            echo "Invalid argument!"
            usage
            exit 99
            ;;
    
    esac
done

# ---

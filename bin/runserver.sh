#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : runserver.sh
# -----------------------------------------------------------------------------
# Device Parser Server Startup
#  Valid Options:
#    -s <server>    : server name
#    -p <port>      : [optional] listen port
#    -i             : [optional] interactive
#    -kill          : [optional] kill running server
#  Examples:
#     % runserver.sh -s <server> -p 31000 -i
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    GTS_HOME=".";  # - default to current dir
    exit 99;
fi
. ${GTS_HOME}/bin/common.sh
# -----------------------------------------------------------------------------

# --- usage (and exit)
function usage() {
    echo "Usage:"
    echo "  Display this help:"
    echo "    $0 -h"
    echo "  Start a server:"
    echo "    $0 -s <server> [-p <port>] [-i]"
    echo "  Stop a server:"
    echo "    $0 -s <server> -kill"
    exit 1
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- 
TEST_EXISTS=0
SERVER_NAME=""
PORT=0
CMDPORT=0
INTERACTIVE=0
KILL_PROC=0
DEBUG=0

# --- argument
while [ $# -gt 0 ]; do
    case "$1" in 
    
        # - display help and exit
        "-h" | "-help") 
            usage
            ;;

        # - test for server existance and exit
        "-e" | "-exists")
            if [ "$2" != "" ]; then
                SERVER_NAME=$2
                shift
            fi
            TEST_EXISTS=1
            ;;

        # - specify server-id
        "-s" | "-server") 
            SERVER_NAME=$2
            shift
            ;;
        
        # - set explicit port
        "-p" | "-port")
            PORT=$2
            shift
            ;;
        
        # - set explicit command port
        "-cp" | "-cmdport" | "-command" )
            CMDPORT=$2
            shift
            ;;

        # - run interactive (output to console)
        "-i" | "-interactive")
            INTERACTIVE=1
            ;;
            
        # - debug mode
        "-debug" | "-debugMode" | "-verbose" )
            DEBUG=1
            ;;

        # - kill server
        "-kill")
            KILL_PROC=1
            ;;
            
        # - skip remaining arguments
        "--" )
            shift
            break
            ;;

        # - unknown argument
        *)
            echo "Invalid argument! [$1]"
            usage
            ;;
            
    esac
    shift
done

# --- validate arguments
if [ "$SERVER_NAME" = "" ]; then
    echo "Invalid server specified"
    usage
fi
if [ $PORT -lt 0 ]; then
    echo "Invalid port specified"
    usage
fi
if [ $CMDPORT -lt 0 ]; then
    echo "Invalid command-port specified"
    usage
fi

# --- log file
LOG_DIR="${GTS_HOME}/logs"
LOG_FILE="${LOG_DIR}/${SERVER_NAME}.log"
LOG_FILE_OUT="${LOG_DIR}/${SERVER_NAME}.out"
PID_FILE="${LOG_DIR}/${SERVER_NAME}.pid"

# --- memory, initial args
#MEMORY="256m";
#ARGS="-Xmx${MEMORY}"
ARGS=""

# --- Java Main start-server command
SERVER_JAR="${GTS_HOME}/build/lib/${SERVER_NAME}.jar"
if [ -f "${SERVER_JAR}" ]; then
    # - The jar file knows how to start itself (via "Main-Class: ...")
    # - (this may still depend on external jars: gtsdb.jar)
    ARGS="${ARGS} -jar ${SERVER_JAR}";
else
    # - not found
    if [ $TEST_EXISTS -eq 1 ]; then
        exit 1
    else
        echo "Server not found: ${SERVER_JAR}"
        usage
    fi
fi

# --- test for server existance only
if [ $TEST_EXISTS -eq 1 ]; then
    exit 0
fi

# --- debug mode
if [ $DEBUG -eq 1 ]; then
    ARGS="${ARGS} -debugMode";
fi

# --- config file
ARGS="${ARGS} '-conf=${GTS_CONF}' -log.name=${SERVER_NAME}"

# --- stop process?
if [ $KILL_PROC -eq 1 ]; then
    if [ -f "$PID_FILE" ]; then
        serverPID=`${CMD_CAT} ${PID_FILE}`
        if [ $serverPID != "" ]; then
            echo "Killing '${SERVER_NAME}' PID: $serverPID";
            $CMD_KILL -9 $serverPID
            pidKillStat=$?
            if [ $pidKillStat -ne 0 ]; then
                echo "Error killing server: $pidKillStat"
            fi
            $CMD_RM $PID_FILE
            exit $pidKillStat
        fi
    else
        echo "PidFile not found: ${PID_FILE}"
    fi
    exit 99
fi

# --- port 
if [ $PORT -gt 0 ]; then
    ARGS="${ARGS} -${SERVER_NAME}.port=${PORT}"
fi

# --- command-port 
if [ $CMDPORT -gt 0 ]; then
    ARGS="${ARGS} -${SERVER_NAME}.commandPort=${CMDPORT}"
fi

# --- start
ARGS="${ARGS} -start"

# --- extra args
ARGS="${ARGS} $1 $2 $3"

# --- interactive
if [ $INTERACTIVE -eq 1 ]; then
    COMMAND="${CMD_JAVA} ${ARGS} -log.file.enable=false"
    echo "${COMMAND}"
    # --- run interactively
    echo "Server jar: ${SERVER_JAR}"
    eval "${COMMAND}"
    exit 0
fi

# --- already running
if [ -f "$PID_FILE" ]; then
    pid=`${CMD_CAT} ${PID_FILE}`
    echo "PID file already exists: $PID_FILE  [pid $pid]\n";
    if [ ${CMD_PS} == "" ]; then
        echo "The '${SERVER_NAME}' server may already be running.\n"
        echo "If server has stopped, delete the server pid file and rerun this command.\n"
        echo "Aborting ...\n"
        exit 99
    fi
    ${CMD_PS} -p ${pid} >/dev/null
    if [ $? -eq 0 ]; then
        echo "The '${SERVER_NAME}' server is likely already running using pid ${pid}.\n";
        echo "Make sure this server is stopped before attempting to restart.\n";
        echo "Aborting ...\n";
        exit 99
    else
        echo "(Service on pid ${pid} seems to stopped, continuing ...)\n";
    fi
    ${CMD_RM} -f ${PID_FILE}
fi

# --- execute (background)
${CMD_MKDIR} -p ${LOG_DIR}
LOG_ARGS="-log.file.enable=true -log.file=${LOG_FILE}"
COMMAND="${CMD_JAVA} ${ARGS} ${LOG_ARGS}"
echo "${COMMAND}"
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND} 1>> ${LOG_FILE_OUT} 2>&1 &           # pipe stdout/stderr to output file
else
    eval "${COMMAND} 1>> ${LOG_FILE_OUT} 2>&1 &"    # pipe stdout/stderr to output file
fi
javaPID=$!
sleep 1
echo "Started '${SERVER_JAR}' [background pid $javaPID]"
echo ${javaPID} > ${PID_FILE}

# ---

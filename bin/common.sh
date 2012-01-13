# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : common.sh
# -----------------------------------------------------------------------------
# Description:
#   This shell script is to be included in other '.sh' scripts and is not intended 
#   to be executed separately.
# -----------------------------------------------------------------------------
# Environment variables:
#   OS          [optional] The operating system indicator (ie. "Windows")
#   GTS_HOME    [required] OpenGTS installation directory (MUST be set)
#   GTS_CONF    [optional] Full path to OpenGTS runtime config file (ie. 'default.conf')
#   GTS_CHARSET [optional] The character set used when starting up the Java proces
#   GTS_DEBUG   [optional] '1' for debug mode (echoes java command), blank/0 otherwise
# -----------------------------------------------------------------------------
# Notes:
#   - to pipe stdout/stderr to output file:
#       $ command 1>> outputFile.log 2>&1 &
# -----------------------------------------------------------------------------

# --- debug mode
if [ "$GTS_DEBUG" = "" ]; then
    GTS_DEBUG=0
else
    GTS_DEBUG=1
fi

# --- "GTS_CONF" config file name
if [ "$GTS_CONF" = "" ]; then
    export GTS_CONF="${GTS_HOME}/default.conf"
fi

# --- character set
if [ "$GTS_CHARSET" != "" ]; then
    JAVA_CHARSET="-Dfile.encoding=${GTS_CHARSET}";
else
    JAVA_CHARSET="-Dfile.encoding=UTF-8";
    #JAVA_CHARSET="-Dfile.encoding=ISO-8859-1";
fi

# --- set location of Jar libraries
JARDIR="${GTS_HOME}/build/lib";

# --- set the classpath
if echo ${OS} | grep -q "Windows" ; then
    IS_WINDOWS=1
    PATHSEP=";"
else
    IS_WINDOWS=0
    PATHSEP=":"
fi
CPATH="${JARDIR}/gtsdb.jar"
for jarname in "gtsutils" "optdb" "ruledb" "bcrossdb" "custom" "dmtpserv" "gtsdmtp"
do
    if [ -f "${JARDIR}/${jarname}.jar" ]; then
        CPATH="${CPATH}${PATHSEP}${JARDIR}/${jarname}.jar"
    fi
done
#if [ "$CATALINA_HOME" != "" ]; then
    # - DBCP - DB Connection Pooling
    #CPATH="${CPATH}${PATHSEP}${CATALINA_HOME}/common/lib/naming-factory-dbcp.jar"
#fi

# --- memory
if [ "${JAVA_MEMORY}" = "" ]; then 
    JAVAMEM=-Xmx256m
else
    JAVAMEM=-Xmx${JAVA_MEMORY}
fi

# --- commands
if [ "$JAVA_HOME" != "" ]; then
    CMD_JAVA="${JAVA_HOME}/bin/java ${JAVAMEM} ${JAVA_CHARSET}"
else
    CMD_JAVA="java ${JAVAMEM} ${JAVA_CHARSET}"
fi
CMD_MKDIR="mkdir"
CMD_CAT="cat"
CMD_PS="ps"
CMD_RM="rm"
CMD_KILL="kill"
CMD_UNAME="uname"

# --- Mac OSX
UNAME=`$CMD_UNAME`
IS_MACOSX=0
if [ "$UNAME" = "Darwin" ]; then
    IS_MACOSX=1
fi

# ---

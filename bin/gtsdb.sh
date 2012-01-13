#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : gtsdb.sh
# -----------------------------------------------------------------------------
# Displays the build version of the 'gtsdb.jar' file.
# This should be executed from the same directory containing the 'gtsdb.jar' file
# -----------------------------------------------------------------------------
ARGS="-log.file.enable=false -info"
if   [ -f "./gtsdb.jar" ]; then 
    echo -n "Version(./gtsdb.jar): "
    java -cp "gtsdb.jar" org.opengts.Version $ARGS
elif [ -f "./build/lib/gtsdb.jar" ]; then
    echo -n "Version(build/lib/gtsdb.jar): "
    java -cp "build/lib/gtsdb.jar" org.opengts.Version $ARGS
elif [ -f "${GTS_HOME}/build/lib/gtsdb.jar" ]; then
    echo -n "Version(${GTS_HOME}/build/lib/gtsdb.jar): "
    java -cp "${GTS_HOME}/build/lib/gtsdb.jar" org.opengts.Version $ARGS
else
    echo "Unable to locate 'gtsdb.jar'"
fi
#

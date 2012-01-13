#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : admin.sh
# -----------------------------------------------------------------------------
# Description:
#   Database administrative tool.
# Usage:
#   % bin/admin.sh <TableName> [<Options>]
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------

# --- usage (and exit)
function usage() {
    echo "Usage: $0 <table> <option> [<option> [...]]"
    exit 1
}

# -----------------------------------------------------------------------------

# --- Get entry-point from table name
JMAIN=""
case "$1" in 
    "Account"           ) JMAIN="org.opengts.db.tables.Account"              ;;
    "AccountString"     ) JMAIN="org.opengts.db.tables.AccountString"        ;;
    "Device"            ) JMAIN="org.opengts.db.tables.Device"               ;;
    "DeviceGroup"       ) JMAIN="org.opengts.db.tables.DeviceGroup"          ;;
    "DeviceList"        ) JMAIN="org.opengts.db.tables.DeviceList"           ;;
    "Driver"            ) JMAIN="org.opengts.db.tables.Driver"               ;;
    "Geozone"           ) JMAIN="org.opengts.db.tables.Geozone"              ;;
    "Resource"          ) JMAIN="org.opengts.db.tables.Resource"             ;;
    "Role"              ) JMAIN="org.opengts.db.tables.Role"                 ;;
    "RoleAcl"           ) JMAIN="org.opengts.db.tables.RoleAcl"              ;;
    "StatusCode"        ) JMAIN="org.opengts.db.tables.StatusCode"           ;;
    "SystemProps"       ) JMAIN="org.opengts.db.tables.SystemProps"          ;;
    "Transport"         ) JMAIN="org.opengts.db.tables.Transport"            ;;
    "UniqueXID"         ) JMAIN="org.opengts.db.tables.UniqueXID"            ;;
    "User"              ) JMAIN="org.opengts.db.tables.User"                 ;;
    "UserAcl"           ) JMAIN="org.opengts.db.tables.UserAcl"              ;;
    # --
    "PendingPacket"     ) JMAIN="org.opengts.db.dmtp.PendingPacket"          ;;
    "Property"          ) JMAIN="org.opengts.db.dmtp.Property"               ;;
    # --
    "Entity"            ) JMAIN="org.opengts.extra.tables.Entity"            ;;
    "SessionStats"      ) JMAIN="org.opengts.extra.tables.SessionStats"      ;;
    "UnassignedDevices" ) JMAIN="org.opengts.extra.tables.UnassignedDevices" ;;
    # --
    "GeoCorridor"       ) JMAIN="org.opengts.rule.tables.GeoCorridor"        ;;
    "GeoCorridorList"   ) JMAIN="org.opengts.rule.tables.GeoCorridorList"    ;;
    "Rule"              ) JMAIN="org.opengts.rule.tables.Rule"               ;;
    "RuleList"          ) JMAIN="org.opengts.rule.tables.RuleList"           ;;
    # --
    "BorderCrossing"    ) JMAIN="org.opengts.bcross.tables.BorderCrossing"   ;;
esac
if [ "$JMAIN" = "" ]; then
    usage
fi
echo "Entry Point: $JMAIN"
JMAIN_ARGS="'-conf=${GTS_CONF}' -log.file.enable=false"

# --- execute
CMD_ARGS="$2 $3 $4 $5 $6"
COMMAND="${CMD_JAVA} -classpath ${CPATH} ${JMAIN} ${JMAIN_ARGS} ${CMD_ARGS}"
if [ $GTS_DEBUG -ne 0 ]; then
    echo "${COMMAND}"
fi
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
else
    eval "${COMMAND}"
fi

# --- exit normally
exit 0

# ---

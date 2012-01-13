#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : installUpdate.sh
# -----------------------------------------------------------------------------
# Description:
#   Installation update.  This module performs the following:
#      - Sets the execute bit on various script files in the 'bin' directory.
#      - Runs 'bin/dbAdmin.pl -tables=cak' to update the database.
#      - Runs CheckInstall
# Usage:
#   % bin/postInstall.sh
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
# -----------------------------------------------------------------------------

# ---- set execute bit on executable command script
function setExecutable()
{
    # arg $1 = command name
    local CMD="$1"
    if [ -f "${GTS_HOME}/bin/${CMD}" ]; then
        echo "  ${GTS_HOME}/bin/${CMD}"
        chmod a+x "${GTS_HOME}"/bin/${CMD}
    fi
}

# -----------------------------------------------------------------------------
# --- perform upgrade tasks

echo ""
echo "Upgrading GTS installation at '${GTS_HOME}'"

# --- update
if [ -d "${GTS_HOME}/bin" ] && [ -f "${GTS_HOME}/bin/psJava" ]; then

    # --- set execute bit on commands
    echo ""
    echo "Setting executable bit on GTS commands ..."
    echo "  ${GTS_HOME}/bin/*.sh"
    chmod a+x "${GTS_HOME}"/bin/*.sh
    echo "  ${GTS_HOME}/bin/*.pl"
    chmod a+x "${GTS_HOME}"/bin/*.pl
    setExecutable gtsAdmin.command
    setExecutable gtsConfig.command
    setExecutable psJava
    setExecutable exeJava
    setExecutable readTCP
    setExecutable readUDP
    setExecutable writeTCP
    setExecutable writeUDP
    echo "... done"

    # --- update tables
    echo ""
    echo "Updating GTS tables ..."
    ${GTS_HOME}/bin/dbAdmin.pl -tables=cak
    echo "... done"
    
    # --- run checkInstall
    echo ""
    echo "Running CheckInstall ..."
    ${GTS_HOME}/bin/checkInstall.sh
    echo ""

else

    echo ""
    echo "GTS installation directory not found."
    echo "[GTS_HOME = ${GTS_HOME}]"
    echo ""
    echo "Run this command as follows:"
    echo " - Set the GTS_HOME environment variable to the latest GTS Installation directory"
    echo " - 'cd' to the GTS Installation directory:"
    echo "      cd \$GTS_HOME"
    echo " - Run this command from within the GTS Installation directory:"
    echo "      bin/installUpdate.sh"
    echo "    or"
    echo "      .  bin/installUpdate.sh"
    echo ""

fi

# -----------------------------------------------------------------------------
# ---

#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : makeGtsVarsEnv.sh
# -----------------------------------------------------------------------------
# Description:
#   Create "/usr/local/gts_vars.env" file
# Usage:
#   % bin/makeGtsVarsEnv.sh -installDir /usr/local -user opengts -out /usr/local/gts_vars.env
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------

# --- command existance check
COMMAND_ERROR=0
function chkCmd() { if [ ! -f "$1" ]; then echo "ERROR: Command unavailable - $1"; COMMAND_ERROR=1; fi }

# --- commands
CMD_chmod="/bin/chmod"                          ; chkCmd "${CMD_chmod}"
CMD_whoami="/usr/bin/whoami"                    ; chkCmd "${CMD_whoami}"

# --- command errors?
if [ ${COMMAND_ERROR} -ne 0 ]; then
    echo ""
    echo "ERROR: Missing required commands"
    echo "Cannot continue ..."
    exit 99
fi

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# ---- create "/usr/local/gts_vars.env"
function create_gtsVarsEnv()
{
    # arg $1 = "/usr/local/gts_vars.env"
    # arg $2 = "/usr/local/"
    # arg $3 = user
    local GTSVARS_ENV="$1"
    local INSTALL_DIR="$2"
    local GTS_USER="$3"

    # --- external command requirements:
    # - CMD_whoami
    # - CMD_chmod

    # --- already exists?
    if [ -f "${GTSVARS_ENV}" ]; then
        echo ""
        echo "*** '${GTSVARS_ENV}' already exists"
        echo ""
        exit 1
    fi

    # --- Install dir not specified?
    if [ "${INSTALL_DIR}" = "" ]; then
        echo ""
        echo "*** Installation directory not specified"
        echo ""
        exit 1
    fi

    # --- Install dir does not exist?
    if [ ! -d "${INSTALL_DIR}" ]; then
        echo ""
        echo "*** Installation directory does not exist: ${INSTALL_DIR}"
        echo ""
        exit 1
    fi

    # --- user specified?
    if [ "${GTS_USER}" = "" ]; then
        echo ""
        echo "*** User not specified"
        echo ""
        exit 1
    fi
    
    # --- symbolic link existance
    # - ${INSTALL_DIR}/gts
    # - ${INSTALL_DIR}/java
    # - ${INSTALL_DIR}/tomcat
    local LINK_CHECK=1
    if [ ! -h "${INSTALL_DIR}/gts" ]; then
        echo ""
        echo "*** Symbolic link does not exist: ${INSTALL_DIR}/gts"
        echo "(Symbolic link '${INSTALL_DIR}/gts' should point to the latest GTS installation directory)"
        LINK_CHECK=0
    fi
    if [ ! -h "${INSTALL_DIR}/java" ]; then
        echo ""
        echo "*** Symbolic link does not exist: ${INSTALL_DIR}/java"
        echo "(Symbolic link '${INSTALL_DIR}/java' should point to the Java JDK directory)"
        LINK_CHECK=0
    fi
    if [ ! -h "${INSTALL_DIR}/tomcat" ]; then
        echo ""
        echo "*** Symbolic link does not exist: ${INSTALL_DIR}/tomcat"
        echo "(Symbolic link '${INSTALL_DIR}/tomcat' should point to the Tomcat installation directory)"
        LINK_CHECK=0
    fi
    if [ $LINK_CHECK -ne 1 ]; then
        echo ""
        echo "*** Missing symbolic links (change install directory, or create symbolic links)"
        echo ""
        exit 1
    fi

    # --- create file
    echo "# --------------------------------------------------------------"             >> ${GTSVARS_ENV}
    echo "# File : ${GTSVARS_ENV}"                                                      >> ${GTSVARS_ENV}
    echo "# Desc : Set up GTS environment variables"                                    >> ${GTSVARS_ENV}
    echo "# Note : This file should be read-only and owned by root"                     >> ${GTSVARS_ENV}
    echo "# Usage: Source within a Bash shell:"                                         >> ${GTSVARS_ENV}
    echo "#         .  ${GTSVARS_ENV}"                                                  >> ${GTSVARS_ENV}
    echo "# --------------------------------------------------------------"             >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- Users"                                                                  >> ${GTSVARS_ENV}
    echo "GTS_USER=${GTS_USER}"                                                         >> ${GTSVARS_ENV}
    echo "TOMCAT_USER=${GTS_USER}"                                                      >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- Directories"                                                            >> ${GTSVARS_ENV}
    echo "export GTS_HOME=\`(cd -P ${INSTALL_DIR}/gts; /bin/pwd)\`"                     >> ${GTSVARS_ENV}
    echo "export JAVA_HOME=\`(cd -P ${INSTALL_DIR}/java; /bin/pwd)\`"                   >> ${GTSVARS_ENV}
    echo "export JRE_HOME=\${JAVA_HOME}"                                                >> ${GTSVARS_ENV}
    echo "export CATALINA_HOME=\`(cd -P ${INSTALL_DIR}/tomcat; /bin/pwd)\`"             >> ${GTSVARS_ENV}
    echo "export CATALINA_BASE=\${CATALINA_HOME}"                                       >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- May be required for Java/Tomcat on systems with limited memory"         >> ${GTSVARS_ENV}
    echo "#export JAVA_MEMORY=100m"                                                     >> ${GTSVARS_ENV}
    echo "#export JAVA_OPTS=-Xmx256m"                                                   >> ${GTSVARS_ENV}
    echo "#export ANT_OPTS=-Xmx100m"                                                    >> ${GTSVARS_ENV}
    echo "#export CATALINA_OPTS=-Xmx512m"                                               >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- Path"                                                                   >> ${GTSVARS_ENV}
    echo "PATH=.:\${JAVA_HOME}/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"        >> ${GTSVARS_ENV}
    echo "export PATH"                                                                  >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- Aliases"                                                                >> ${GTSVARS_ENV}
    echo "alias setgts='export GTS_HOME=\`(cd -P .;/bin/pwd)\`; echo \${GTS_HOME}'"     >> ${GTSVARS_ENV}
    echo "alias ls='ls -aF'"                                                            >> ${GTSVARS_ENV}
    echo "alias wh='find . -name'"                                                      >> ${GTSVARS_ENV}
    echo "alias cd='cd -P'"                                                             >> ${GTSVARS_ENV}
    echo "alias psjava='\${GTS_HOME}/bin/psjava'"                                       >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}
    echo "# --- Prompt"                                                                 >> ${GTSVARS_ENV}
    echo "if [ -n \"\${PS1+x}\" ]; then"                                                >> ${GTSVARS_ENV}
    echo "   SAVE_PS1=\${PS1}"                                                          >> ${GTSVARS_ENV}
    echo "   if [ \"\`${CMD_whoami}\`\" = \"root\" ]; then"                             >> ${GTSVARS_ENV}
    echo "      PS1=\"\h:\w # \""                                                       >> ${GTSVARS_ENV}
    echo "   else"                                                                      >> ${GTSVARS_ENV}
    echo "      PS1=\"\h:\w \\\$ \""                                                    >> ${GTSVARS_ENV}
    echo "   fi"                                                                        >> ${GTSVARS_ENV}
    echo "else"                                                                         >> ${GTSVARS_ENV}
    echo "   SAVE_PS1=\"\""                                                             >> ${GTSVARS_ENV}
    echo "fi"                                                                           >> ${GTSVARS_ENV}
    echo ""                                                                             >> ${GTSVARS_ENV}

    # - set permissions
    ${CMD_chmod} 644 ${GTSVARS_ENV}
    echo ""
    echo "Environment setup file '${GTSVARS_ENV}' created."

}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# --- usage (and exit)
function usage() {
    # arg $1 = exit code
    local exitCode=$1
    if [ $exitCode -ne 0 ]; then
    echo ""
    echo "------------------------------------------------------------------------------------------"
    fi
    echo ""
    echo "Usage: $0 -dir <dir> -user <user> -out <gts_vars.env>"
    echo "  or"
    echo "Usage: $0 -help"
    echo ""
    echo "Options:"
    echo "   -dir <dir>         - Directory where symbolic links have been installed (ie. '/usr/local')"
    echo "   -user <user>       - User that owns GTS installation directory, and Tomcat directory"
    echo "   -out <filePath>    - Fully qualified file name where environment script will be written"
    echo "   -help              - This displayed help output"
    echo ""
    echo "Description:"
    echo "   This command creates this output file 'gts_vars.env' (or filename as specified on the '-out'"
    echo "   option on the command-line), which can be used to initialize the following environment variables:"
    echo "      GTS_USER        - The user owning the GTS installation directory "
    echo "      TOMCAT_USER     - Same as \$GTS_USER"
    echo "      GTS_HOME        - The GTS installation directory"
    echo "      JAVA_HOME       - The Java installation directory"
    echo "      JRE_HOME        - Same as \$JAVA_HOME"
    echo "      CATALINA_HOME   - The Tomcat installation directory"
    echo "      CATALINA_BASE   - Currently, the same as \$CATALINA_HOME"
    echo ""
    echo "   The created script expects the following symbolic links to already exist:"
    echo "      <dir>/gts       - Points to the latest GTS installation directory"
    echo "      <dir>/java      - Points to the Oracle/Sun Java installation directory"
    echo "      <dir>/tomcat    - Points to the Apache Tomcat installation directory"
    echo ""
    echo "   The resulting output script should be placed into the '/usr/local/' directory (root access"
    echo "   may be required).  The following BASH 'source' command may be executed to initialize the various"
    echo "   environment variables with the current session:"
    echo "      .  /usr/local/gts_vars.env"
    echo "   (Note the space after the first '.' character)"
    echo ""
    exit ${exitCode}
}

# --- check arguments
INSTALL_DIR=""
GTS_USER=""
GTS_VARS=""
ENVVARS_OUT=""
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - help
        "-help" | "-h" ) 
            usage 0
            ;;

        # ------------------------------------------------------

        # - install directory
        "-installDir" | "-dir" | "-d" )
            if [ $# -ge 2 ]; then
                INSTALL_DIR="$2"
                export INSTALL_DIR
                shift
            else
                echo ""
                echo "*** Missing '-installDir' argument"
                usage 99
            fi
            ;;

        # - user
        "-user" | "-u" )
            if [ $# -ge 2 ]; then
                GTS_USER="$2"
                export GTS_USER
                shift
            else
                echo ""
                echo "*** Missing '-user' argument"
                usage 99
            fi
            ;;

        # - output file name
        "-out" | "-o" | "-gtsVars" )
            if [ $# -ge 2 ]; then
                ENVVARS_OUT="$2"
                export ENVVARS_OUT
                shift
            else
                echo ""
                echo "*** Missing '-out' argument"
                usage 99
            fi
            ;;

        # ------------------------------------------------------

        # - error
        * )
            echo ""
            echo "*** Invalid/Unexpected argument! $1"
            usage 99
            ;;

    esac
    shift
done

# --- check installation directory
if [ "${INSTALL_DIR}" = "" ]; then
    echo ""
    echo "*** Missing '-dir' argument"
    usage 1
fi
if [ ! -d "${INSTALL_DIR}" ]; then
    echo ""
    echo "*** Installation directory does ot exist: ${INSTALL_DIR}"
    usage 1
fi
INSTALL_DIR=`(cd -P ${INSTALL_DIR}; /bin/pwd)`

# --- check user
if [ "${GTS_USER}" = "" ]; then
    echo ""
    echo "*** Missing '-user' argument"
    usage 1
fi

# --- check output
if [ "${ENVVARS_OUT}" = "" ]; then
    echo ""
    echo "*** Missing '-out' argument"
    usage 1
fi

# --- display args
echo ""
echo "Install Dir : ${INSTALL_DIR}"
echo "User        : ${GTS_USER}"
echo "Output      : ${ENVVARS_OUT}"

# -----------------------------------------------------------------------------

# --- create
create_gtsVarsEnv "${ENVVARS_OUT}" "${INSTALL_DIR}" "${GTS_USER}"
echo ""

# --- done
exit 0

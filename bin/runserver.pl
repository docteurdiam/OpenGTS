#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : runserver.pl
# Description: Command-line GTS server startup utility
# -----------------------------------------------------------------------------
# Device Parser Server Startup (MySQL datastore)
#  Valid Options:
#    -s <server>    : server name
#    -p <port>      : [optional] listen port
#    -i             : [optional] interactive
#    -kill          : [optional] kill running server
#  Examples:
#     % runserver.pl -s <server> -p 31000 -i
#     % runserver.pl -s <server> -kill
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "!!! ERROR: GTS_HOME not defined !!!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
    exit(99);
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- options
use Getopt::Long;
%argctl = (
    "exists:s"      => \$opt_exists,
    "server:s"      => \$opt_server,
    "memory:s"      => \$opt_memory,
    "mem:s"         => \$opt_memory,
    "logName:s"     => \$opt_logName,
    "bind:s"        => \$opt_bind,
    "bindAddress:s" => \$opt_bind,
    "port:s"        => \$opt_port,
    "cmdport:s"     => \$opt_cmdport,
    "command:s"     => \$opt_cmdport,
    "i"             => \$opt_interactive,
    "debugMode"     => \$opt_debug,
    "debug"         => \$opt_debug,
    "verbose"       => \$opt_debug,
    "log"           => \$opt_logLevel,
    "kill"          => \$opt_kill,
    "kill_"         => \$opt_ambiguous,
    "parseFile:s"   => \$opt_parseFile,
    "insert:s"      => \$opt_parseInsert,
    "insert_"       => \$opt_ambiguous,
    "lookup:s"      => \$opt_lookup,
    "help"          => \$opt_help,
);
$optok = &GetOptions(%argctl);
if (defined $opt_exists) { $opt_server = $opt_exists; }
if (!$optok || (defined $opt_help) || 
    (!(defined $opt_server) && !(defined $opt_lookup))) {
    print "Usage:\n";
    print "  Display this help:\n";
    print "    runserver.pl -h\n";
    print "  Start a server:\n";
    print "    runserver.pl -s <server> [-p <port>] [-i]\n";
    print "  Stop a server:\n";
    print "    runserver.pl -s <server> -kill\n";
    print "  Parse a file containing static data:\n";
    print "    runserver.pl -s <server> -parseFile=<file>\n";
    exit(99);
}

# -----------------------------------------------------------------------------

# --- lookup unique-id
if (defined $opt_lookup) {
    my $rtn = &sysCmd("$GTS_HOME/bin/exeJava org.opengts.db.DCServerFactory -lookup=$opt_lookup",$ECHO_CMD);
    exit($rtn);
}

# -----------------------------------------------------------------------------

# --- echo Java command-line prior to execution
$ECHO_CMD       = ("$GTS_DEBUG" eq "1")? $true : $false;

# --- server name 
$SERVER_NAME    = $opt_server;

# --- log file name
$LOG_DIR        = "$GTS_HOME/logs";
$LOG_NAME       = (defined $opt_logName)? $opt_logName : $SERVER_NAME;
$LOG_FILE       = "$LOG_DIR/${LOG_NAME}.log";
$LOG_FILE_OUT   = "$LOG_DIR/${LOG_NAME}.out";
$PID_FILE       = "$LOG_DIR/${LOG_NAME}.pid";

# --- lib dir: build/lib
$LIB_DIR        = "${GTS_HOME}/build/lib";

# --- memory, initial command
$Command        = "$cmd_java";
if (defined $opt_memory) {
    $MEMORY     = $opt_memory;
    $Command   .= " -Xmx$MEMORY";
}

# --- Java Main start-server command
$SERVER_JAR     = "${LIB_DIR}/${SERVER_NAME}.jar";
if (!(-f "${SERVER_JAR}")) {
    # - not found, check for self contained version
    my $SelfContained = "${LIB_DIR}/${SERVER_NAME}_SC.jar";
    if (-f "${SelfContained}") {
        $SERVER_JAR = "${SelfContained}";
    }
}
if (-f "$SERVER_JAR") {
    # - The jar file knows how to start itself (via "Main-Class: ...")
    # - (this may still depend on external jars: gtsdb.jar)
    $Command .= " -jar $SERVER_JAR";
} else {
    # - not found
    if (defined $opt_exists) {
        exit(1);
    } else {
        print "Server not found: $SERVER_JAR\n";
        exit(1);
    }
}

# --- test for server existance only
if (defined $opt_exists) {
    exit(0);
}

# --- debug mode
if (defined $opt_debug) {
    $Command .= " -debugMode";
    $ECHO_CMD = $true;
}

# --- log level
if (defined $opt_logLevel) {
    $Command .= " -log.level=$opt_logLevel";
}

# --- config file (should be first argument)
$Command .= " -conf=$GTS_CONF -log.name=$LOG_NAME";

# --- stop process?
if (defined $opt_kill) {
    if (-f "$PID_FILE") {
        my $pid = `$cmd_cat $PID_FILE`; chomp $pid;
        if ($pid ne "") {
            print "Killing '$LOG_NAME' PID: $pid\n";
            my $rtn = &sysCmd("$cmd_kill -9 $pid ; $cmd_rm $PID_FILE",$ECHO_CMD);
            if ($rtn != 0) {
                print "Error killing server: $rtn\n";
            }
            exit($rtn);
        } else {
            print "Invalid PID: $pid\n";
        }
    } else {
        print "PidFile not found: $PID_FILE\n";
    }
    exit(99);
}

# --- parse file?
if (defined $opt_parseFile) {
    print "Server jar: $SERVER_JAR\n";
    print "Parsing file: $opt_parseFile\n";
    my $parseCmd = $Command . " -parseFile=$opt_parseFile";
    if (defined  $opt_parseInsert) {
        $parseCmd .= " -insert=$opt_parseInsert";
    }
    &sysCmd($parseCmd, $ECHO_CMD);
    exit(99);
}

# --- assemble command
my $DCSCommand = $Command . " -start";
if (defined $opt_bind) {
    $DCSCommand .= " -bindAddress=$opt_bind";
}
if (defined $opt_port) {
    $DCSCommand .= " -${SERVER_NAME}.port=$opt_port";
    #$DCSCommand .= " -port=$opt_port";
}
if (defined $opt_cmdport) {
    $DCSCommand .= " -${SERVER_NAME}.commandPort=$opt_cmdport";
    #$DCSCommand .= " -commandPort=$opt_cmdport";
}
$DCSCommand .= " " . join(' ', @ARGV);

# --- start interactive
if (defined $opt_interactive) {
    # - ignore $PID_FILE for interactive 
    print "Server jar: $SERVER_JAR\n";
    $DCSCommand .= " -log.file.enable=false";
    &sysCmd($DCSCommand, $ECHO_CMD);
    # - actually, we wait above until the user hits Control-C
    exit(99); # <-- never gets here
}

# --- already running?
if (-f "$PID_FILE") {
    my $pid = `$cmd_cat $PID_FILE`; chomp $pid;
    print "PID file already exists: $PID_FILE  [pid $pid]\n";
    if ($cmd_ps eq "") {
        print "The '${LOG_NAME}' server may already be running.\n";
        print "If server has stopped, delete the server pid file and rerun this command.\n";
        print "Aborting ...\n";
        exit(99);
    }
    my $rtn = &sysCmd("$cmd_ps -p $pid >/dev/null");
    if ($rtn == 0) {
        print "The '${LOG_NAME}' server is likely already running using pid $pid.\n";
        print "Make sure this server is stopped before attempting to restart.\n";
        print "Aborting ...\n";
        exit(99);
    } else {
        print "(Service on pid $pid seems to stopped, continuing ...)\n";
    }
    &sysCmd("$cmd_rm -f $PID_FILE", $ECHO_CMD);
}

# --- create logging directory
if (!(-d "$LOG_DIR")) {
    &sysCmd("$cmd_mkdir -p $LOG_DIR", $ECHO_CMD);
}

# --- log messages to file
$DCSCommand .= " -log.file.enable=true -log.file=$LOG_FILE";

# --- background server (save the pid)
my $pid = &forkCmd($DCSCommand, $LOG_FILE_OUT, $ECHO_CMD);
sleep(1);
print "Started '$SERVER_JAR' [background pid $pid]\n";
&sysCmd("echo $pid > $PID_FILE", $ECHO_CMD);
exit(0);

# -----------------------------------------------------------------------------

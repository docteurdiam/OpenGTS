#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : initdb.pl
# Description: Command-line DB initialization utility.
# -----------------------------------------------------------------------------
# Device initialization utility
#  Valid Options:
#     -rootUser=<id>    MySQL user with permissions to create a database and tables
#     -rootPass=<pass>  Password for specified user
# This command is equivalent to running the following:
#     % bin/dbAdmin.pl -createdb -grant -tables -user=<root>
# -----------------------------------------------------------------------------
# This command uses the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "WARNING: GTS_HOME not defined!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- default account:device
$ACCOUNT = ""; # "opendmtp";
$DEVICE  = ""; # "mobile";

# --- Java Device command
$Entry_point = "$GTS_STD_PKG.db.DBConfig";
$CP          = "-classpath '$CLASSPATH'";
$Command     = "$cmd_java $CP $Entry_point -conf=$GTS_CONF -log.file.enable=false";
$CommandArg  = "";

# --- commands
$CommandArg .= " -initTables";

# --- new demo account/device
if ($ACCOUNT ne "") {
    $CommandArg .= " -newAccount=$ACCOUNT";
}
if ($DEVICE ne "") {
    $CommandArg .= " -newDevice=$DEVICE";
}

# --- execute
my $args = join(' ', @ARGV);
my $cmd  = $Command . "$CommandArg $args";
&sysCmd($cmd, $true);
exit(0);

# -----------------------------------------------------------------------------

#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : admin.pl
# Description: Command-line DB table administrative tool.
# -----------------------------------------------------------------------------
# Database administrative tool.
# Usage:
#   % bin/admin.pl <TableName> [<Options>]
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "WARNING: GTS_HOME not defined!\n";
    use Cwd 'realpath';
    use File::Basename;
    my $REAL_PATH = realpath($0);
    my $EXEC_BIN = dirname($REAL_PATH);
    require "$EXEC_BIN/common.pl";
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

$Standard_Tables = "$GTS_STD_PKG.db.tables";
$OpenDMTP_Tables = "$GTS_STD_PKG.db.dmtp";
$Extra_Tables    = "$GTS_STD_PKG.extra.tables";
$Rule_Tables     = "$GTS_STD_PKG.rule.tables";
$BCross_Tables   = "$GTS_STD_PKG.bcross.tables";

# --- Entry points
%Class_names = (
    "Account"           => $Standard_Tables . ".Account",
    "AccountString"     => $Standard_Tables . ".AccountString",
    "Device"            => $Standard_Tables . ".Device",
    "DeviceGroup"       => $Standard_Tables . ".DeviceGroup",
    "DeviceList"        => $Standard_Tables . ".DeviceList",
    "Driver"            => $Standard_Tables . ".Driver",
    "Geozone"           => $Standard_Tables . ".Geozone",
    "Resource"          => $Standard_Tables . ".Resource",
    "Role"              => $Standard_Tables . ".Role",
    "RoleAcl"           => $Standard_Tables . ".RoleAcl",
    "StatusCode"        => $Standard_Tables . ".StatusCode",
    "SystemProps"       => $Standard_Tables . ".SystemProps",
    "Transport"         => $Standard_Tables . ".Transport",
    "UniqueXID"         => $Standard_Tables . ".UniqueXID",
    "User"              => $Standard_Tables . ".User",
    "UserAcl"           => $Standard_Tables . ".UserAcl",
    # --
    "PendingPacket"     => $OpenDMTP_Tables . ".PendingPacket",
    "Property"          => $OpenDMTP_Tables . ".Property",
    # --
    "Entity"            => $Extra_Tables    . ".Entity",
    "SessionStats"      => $Extra_Tables    . ".SessionStats",
    "UnassignedDevices" => $Extra_Tables    . ".UnassignedDevices",
    "WorkOrder"         => $Extra_Tables    . ".WorkOrder",
    "WorkZone"          => $Extra_Tables    . ".WorkZone",
    # --
    "GeoCorridor"       => $Rule_Tables     . ".GeoCorridor",
    "GeoCorridorList"   => $Rule_Tables     . ".GeoCorridorList",
    "Rule"              => $Rule_Tables     . ".Rule",
    "RuleList"          => $Rule_Tables     . ".RuleList",
    # --
    "BorderCrossing"    => $BCross_Tables   . ".BorderCrossing",
);

# --- Named entry point
$TableName = $ARGV[0];
$Entry_point = $Class_names{$TableName};
if ("$Entry_point" eq "") {
    print "TableName not found: $TableName\n";
    exit(1);
}
shift;

# --- debug
$DebugMode = "";
$_DebugMode = $ARGV[0];
if (("$_DebugMode" eq "-debug") || ("$_DebugMode" eq "-debugMode")) {
    $GTS_DEBUG = 1;
    $DebugMode = "-debugMode=true";
}

# --- Java command
$CP          = "-classpath '$CLASSPATH'";
$Command     = "$cmd_java $CP $Entry_point -conf=$GTS_CONF $DebugMode -log.file.enable=false";

# --- execute
my $args = join(' ', @ARGV);
my $cmd  = $Command . " $args";
&sysCmd($cmd, $GTS_DEBUG);
exit(0);

# -----------------------------------------------------------------------------

#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : dbAdmin.pl
# Description: Command-line DB administration utility.
# -----------------------------------------------------------------------------
#  Usage: dbAdmin.pl [-user=<user>] [<options>]
#   -createdb       Create default database (need only be done once)
#   -grant          Grant privileges to default user (need only be done once)
#   -tables=<flags> Rebuilds all registered tables, and validates fields
#   -dump           Dump all tables to $DumpDir
#   -load=<table>   Reload previously dumped table from $DumpDir
#   -drop=<table>   Drop (delete) table from database [WARNING: Also deletes data!]
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
# Notes:
#  - When performing a 'mysqldump' (ie. "bin/dbAdmin.pl -dump"), you may recevie the following error:
#     mysqldump: Got error: 1: Can't create/write to file '.../Account.txt' (Errcode: 13) when executing 'SELECT INTO OUTFILE'
#    This is caused by a permissions issue with SELinux (the root cause of which I have not yet discovered)
#    SELinux enforcement can be temporarily suspended with the following command (must be performed as 'root'):
#       % echo 0 > /selinux/enforce          <-- disables SELinux enforcement
#     <perform dump as another user here>
#       % echo 1 > /selinux/enforce          <-- enables SELinux enforcement
#    Warning: Use with caution!  This has only been tested on Fedora.
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print STDERR "WARNING: GTS_HOME not defined!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- all tables
@ALL_TABLES = (
    "Account"           ,
    "AccountString"     ,
    "Antx"              ,
    "BorderCrossing"    ,
    "Device"            ,
    "DeviceGroup"       ,
    "DeviceList"        ,
    "Diagnostic"        ,
    "Driver"            ,
    "Entity"            ,
    "EventData"         ,
    "EventTemplate"     ,
    "FuelRegister"      ,   # <-- Rule
    "GeoCorridor"       ,   # <-- Rule
    "GeoCorridorList"   ,   # <-- Rule
    "Geozone"           ,
    "GroupList"         ,
    "NotifyQueue"       ,   # <-- Rule
    "PendingCommands"   ,
    "PendingPacket"     ,
    "Property"          ,
    "Resource"          ,
    "Role"              ,
    "RoleAcl"           ,
    "Rule"              ,
    "RuleList"          ,
    "SessionStats"      ,
    "StatusCode"        ,
    "SystemAudit"       ,
    "SystemProps"       ,   # <-- system table
    "Transport"         ,
    "UnassignedDevices" ,   # <-- system table
    "UniqueXID"         ,   # <-- note: accountID is not first column
    "User"              ,
    "UserAcl"           ,
    "UserDevice"        ,
    "WorkOrder"         ,
    "WorkZone"          ,
    "WorkZoneList"      ,
);

# -----------------------------------------------------------------------------

# --- Java DBAdmin/DBConfig command
$Entry_point = "$GTS_STD_PKG.db.DBConfig";
$CP          = "-classpath \"$CLASSPATH\"";
$Command     = "$cmd_java $CP $Entry_point -conf=$GTS_CONF -log.file.enable=false";

# --- options
use Getopt::Long;
%argctl = (
    "createdb"      => \$opt_createdb,
    "grant"	        => \$opt_grant,
    "rootUser:s"    => \$opt_rootUser,  # -- needed for 'createdb', 'grant', 'dump'
    "user:s"        => \$opt_rootUser,  # -- needed for 'createdb', 'grant', 'dump'
    "tables:s"      => \$opt_tables,
    "name:s"        => \$opt_tableName, # --- used by '-tables' only
    "tableName:s"   => \$opt_tableName, # --- used by '-tables' only
    "drop:s"        => \$opt_drop,
    "yes"           => \$opt_yes,       # --- used by '-drop' only
    "db:s"          => \$opt_db,        # --- optional for 'dump', 'load'
    "dump:s"        => \$opt_dump,
    "where:s"       => \$opt_where,     # --- opt for 'dump' (specify as '-where=(X="Y")')
    "account:s"     => \$opt_account,
    "load:s"        => \$opt_load,
    "overwrite"     => \$opt_overwrite,
    "dir:s"         => \$opt_dir,
    "bean:s"        => \$opt_bean,      # -- experimental
    "schema:s"      => \$opt_schema,
    "tree"          => \$opt_tree,
    "showsql"       => \$opt_showSQL,
    "debug"         => \$opt_debug,
    "debugMode"     => \$opt_debug,
    "help"          => \$opt_help,
  # "reload:s"      => \$opt_reload,    # --- shortcut for dump/drop/load
  # "hibxml:s"      => \$opt_hibernate, # -- experimental
);
$optok = &GetOptions(%argctl);
$optFound = $false;

# --- MySQL '-dump' options
$DumpDB  = (defined $opt_db)? $opt_db : "gts";
$DumpDir = (defined $opt_dir)? $opt_dir : "/tmp/$DumpDB";
$ALWAYS_USE_MYSQLDUMP = $false; # - always use 'mysqldump' to dump tables?

# --- help
if (!$optok || $opt_help) {
    usage:;
    print "Usage: $0 [-user=<user>] [<options>]\n";
    print "\n";
    print "  Validate table/columns:\n";
    print "    -tables[=<flags>]\n";
    print "        't' - create missing tables [default]\n";
    print "        'c' - add missing columns\n";
    print "        'a' - alter columns with changed types\n";
    print "        'k' - rebuild key indices\n";
    print "        'w' - display warnings\n";
    print "        'e' - display errors\n";
    print "        'u' - also check character encoding (must be used with 'a' or 'w')\n";
    print "        's' - show columns\n";
    print "        'n' - specified table name only (specified with '-tableName=TABLE')\n";
    print "\n";
    print "  Dump table(s) to $DumpDir:\n";
    print "    -dump[=<table>] [-db=<DB>] [-dir=<dir>] [-account=<acct>]\n";
    print "\n";
    print "  Reload previously dumped table from $DumpDir:\n";
    print "    -load=<table> [-db=<DB>] [-dir=<dir>] [-overwrite]\n";
    print "\n";
    print "  Drop (delete) table from database [WARNING: Also deletes data!]:\n";
    print "    -drop=<table> [-yes]\n";
    print "\n";
    exit(1);
}

# --- Debug mode?
if (defined $opt_debug) {
    $Command .= " -debugMode=true";
    $GTS_DEBUG = 1;
}

# --- show SQL?
if (defined $opt_showSQL) {
    $Command .= " -db.showSQL=true";
}

# --- extra args
$Command .= " " . join(' ', @ARGV);

# -----------------------------------------------------------------------------

# --- reload? [OBSOLETE: no longer implemented in DBConfig/DBAdmin]
if (defined $opt_reload) {
    # - validate
    if ("$opt_reload" eq "") {
        print "Missing '-reload' argument\n";
        exit(1);
    }
    # - confirm
    my $reload = &readStdin("Are you sure you want to reload table $opt_reload? [yes/no]");
    if ("$reload" ne "yes") {
        print "Cancelled, table was not reloaded. [$reload]\n";
        exit(1);
    }
    # - reload
    if ($true) {
        my $rtnErr = 0;
        # - make temp directory
        my $cmd_mkdir = &findCmd("mkdir");
        my $cmd_chmod = &findCmd("chmod");
        $rtnErr = &sysCmd("$cmd_mkdir -p $DumpDir; $cmd_chmod a+w $DumpDir;", $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "'mkdir' error: $rtnErr\n";
            exit(1);
        }
        # - reload table
        my $cmd    = $Command . " -reload=$opt_reload -db=$DumpDB -dir=$DumpDir";
        $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "Table reload error: $rtnErr\n";
            exit(1);
        }
        $optFound = $true;
    } else {
        # - set up other options
        $opt_dump = $opt_reload;
        $opt_drop = $opt_reload;
        $opt_yes  = $true;
        $opt_load = $opt_reload;
        if (!(defined $opt_dir)) {
            $opt_dir = "./tmp";
        }
        $opt_tables = $true;
    }
    # - continue
}

# --- create database
if (defined $opt_createdb) {
    print "\n";
    print "Create database ...\n";
    my $rtnErr = 0;
    my $user = (defined $opt_rootUser)? $opt_rootUser : "root";
    my $pass = &readStdin("Enter MySQL '$user' password (needed to create the database):");
    my $cmd  = $Command . " -createdb -rootUser=$user -rootPass=$pass";
    $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
    if ($rtnErr != 0) {
        print "CreateDB error: $rtnErr\n";
        exit(1);
    }
    $optFound = $true;
}

# --- grant privileges
if (defined $opt_grant) {
    print "\n";
    print "Grant privileges ...\n";
    my $rtnErr = 0;
    my $user = (defined $opt_rootUser)? $opt_rootUser : "root";
    my $pass = &readStdin("Enter MySQL '$user' password (needed to assign privileges):");
    my $cmd  = $Command . " -grant -rootUser=$user -rootPass=$pass";
    $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
    if ($rtnErr != 0) {
        print "Grant privileges error: $rtnErr\n";
        exit(1);
    }
    $optFound = $true;
}

# --- dump table/database
if (defined $opt_dump) {
    # bin/dbAdmin.pl -dir=/var/lib/mysql/gts_dump -dump
    print "\n";
    print "Dump table(s) ...\n";
    my $rtnErr = 0;
    # - make temp directory
    if (!(-d "$DumpDir")) {
        my $cmd_mkdir = &findCmd("mkdir");
        my $cmd_chmod = &findCmd("chmod");
        $rtnErr = &sysCmd("$cmd_mkdir -p $DumpDir; $cmd_chmod a+w $DumpDir;", $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "'mkdir' error: $rtnErr\n";
            exit(1);
        }
    }
    # - Use 'mysqldump' or custom dump
    if ((($opt_dump eq "all") && (!defined $opt_account)) || ($opt_dump eq "") || $ALWAYS_USE_MYSQLDUMP) {
        print "Dumping all '$DumpDB' tables to '$DumpDir' ...\n";
        # - MySQL 'dump' commands
        my $cmd_mysqldump = &findCmd("mysqldump");
        # - this uses 'mysqldump'
        my $usr = (defined $opt_rootUser)? $opt_rootUser : "root";
        my $pwd = &readStdin("Enter MySQL '$usr' password (needed to dump the database):");
        my $cmd = "$cmd_mysqldump --user=$usr --password=$pwd --fields-terminated-by=', ' --fields-optionally-enclosed-by='\"' --tab=$DumpDir $DumpDB";
        print "(starting dump via '$cmd_mysqldump' ...)\n";
        $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "MySQL dump error: $rtnErr\n";
            print "-----------------------------------------------------------------------\n";
            print "If present, SELinux may be preventing MySQL from writing to '$DumpDir'\n";
            print "Modify SELinux to allow writing to '$DumpDir', then retry this dump.\n";
            print "('setenforce 0' will temporarily disable SELinux checks)\n";
            print "-----------------------------------------------------------------------\n";
           #(by default, MySQL is allowed to write to '/usr/lib/mysql/x')
            exit(1);
        }
        print "... dump complete.\n";
    } else {
        print "Attempting to dump table '$DumpDB.$opt_dump' to '$DumpDir' ...\n";
        # - the following may fail if the table has changed
        my $cmd = $Command . " -dump=$opt_dump -db=$DumpDB -dir=$DumpDir";
        if (defined $opt_account) {
            $cmd .= " -account=$opt_account";
        }
        if (defined $opt_where) {
            $cmd .= " '-where=$opt_where'";
        }
        $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "Account dump error: $rtnErr\n";
            exit(1);
        }
    }
    # - success
    $optFound = $true;
}

# --- drop table
if (defined $opt_drop) {
    print "\n";
    print "Drop table ...\n";
    my $rtnErr = 0;
    if ("$opt_drop" ne "") {
        my $drop = "";
        if (defined $opt_yes) {
            print "Are you sure you want to drop table $opt_drop? [yes/no]\n";
            print "yes [automatic entry]\n";
            $drop = "yes";
        } else {
            $drop = &readStdin("Are you sure you want to drop table $opt_drop? [yes/no]");
        }
        if ("$drop" eq "yes") {
            my $cmd = $Command . " -drop=$opt_drop";
            $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
            if ($rtnErr != 0) {
                print "Table drop error: $rtnErr\n";
                exit(1);
            }
        } else {
            print "Cancelled, table was not dropped. [$drop]\n";
            exit(1);
        }
    } else {
        print "ERROR: Missing drop table name specification.\n";
        exit(1);
    }
    $optFound = $true; # - never reaches here
}

# --- load table
if (defined $opt_load) {
    my @tblList = ("$opt_load" eq "all")? @ALL_TABLES : split(',',$opt_load);
    foreach my $tableName (@tblList) {
        print "\n";
        print "--------------------------------------------------\n";
        print "Loading table: $tableName ...\n";
        my $rtnErr = 0;
        my $cmd = $Command . " -load=$tableName -dir=$DumpDir";
        if (defined $opt_overwrite) {
            $cmd .= " -overwrite";
        }
        $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
        if ($rtnErr != 0) {
            print "Table '$tableName' load error: $rtnErr\n";
            exit(1);
        }
    }
    print "\n";
    $optFound = $true;
}

# --- create/verify tables
if (defined $opt_tables) {
    #print "\n";
    #print "Create/Verify tables ...\n";
    my $rtnErr = 0;
    my $cmd = $Command . " -tables=$opt_tables";
    if (defined $opt_tableName) {
        $cmd = $cmd . " -tableName=$opt_tableName";
    }
    $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
    if ($rtnErr != 0) {
        print "Table create/verify error: $rtnErr\n";
        exit(1);
    }
    $optFound = $true;
}

# --- display table schema
if (defined $opt_schema) {
    #print "\n";
    #print "Table Schema ...\n";
    my $rtnErr = 0;
    my $cmd = $Command . " -schema=$opt_schema";
    $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
    $optFound = $true;
}

# --- display table dependency tree
if (defined $opt_tree) {
    print "\n";
    print "Table dependency tree...\n";
    my $rtnErr = 0;
    my $cmd = $Command . " -tree";
    $rtnErr = &sysCmd($cmd, $GTS_DEBUG);
    $optFound = $true;
}

# --- validate bean access methods for table (EXPERIMENTAL)
if (defined $opt_bean) {
    if ("$opt_bean" ne "") {
        my $cmd = $Command . " -bean=$opt_bean";
        &sysCmd($cmd, $GTS_DEBUG);
        exit(0);
    } else {
        print "ERROR: Missing bean table.\n";
        exit(1);
    }
    $optFound = $true; # - never reaches here
}

# --- print Hibernate XML for table (EXPERIMENTAL - not fully implemented)
if (defined $opt_hibernate) {
    if ("$opt_hibernate" ne "") {
        my $cmd = $Command . " -hibxml=$opt_hibernate";
        &sysCmd($cmd, $GTS_DEBUG);
        exit(0);
    } else {
        print "ERROR: Missing hibernate table.\n";
        exit(1);
    }
    $optFound = $true; # - never reaches here
}

# --- no option specified
if (!$optFound) {
    goto usage;
}
exit(0);

# -----------------------------------------------------------------------------

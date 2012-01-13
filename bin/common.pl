# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : common.pl
# -----------------------------------------------------------------------------
# Description:
#   This Perl script is to be included in other Perl scripts and is not intended 
#   to be executed separately.
# -----------------------------------------------------------------------------
# Environment variables:
#   OS          [optional] The operating system indicator (ie. "Windows")
#   GTS_HOME    [required] OpenGTS installation directory (MUST be set)
#   GTS_CONF    [optional] Full path to OpenGTS runtime config file (ie. 'default.conf')
#   GTS_CHARSET [optional] The character set used when starting up the Java proces
#   GTS_DEBUG   [optional] '1' for debug mode (echoes java command), blank/0 otherwise
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# General command startup initialization:

# --- external
use Cwd 'realpath';
use File::Basename;

# --- constants
$true               = 1;
$false              = 0;

# --- Java path separator
$OS                 = $ENV{'OS'};
$IS_WINDOWS         = ($OS =~ /^Windows/)? $true : $false;
$PATHSEP            = $IS_WINDOWS? ";" : ":";

# --- project directories/packaged
$GTS_STD_DIR        = "org/opengts";
$GTS_STD_PKG        = "org.opengts";
$GTS_OPT_DIR        = "org/opengts/opt";
$GTS_OPT_PKG        = "org.opengts.opt";

# --- debug mode
$GTS_DEBUG          = $ENV{'GTS_DEBUG'};
if ("$GTS_DEBUG" eq "") {
    $GTS_DEBUG      = 0;
}

# --- CATALINA_HOME
$CATALINA_HOME      = $ENV{'CATALINA_HOME'};
$CATALINA_BASE      = $ENV{'CATALINA_BASE'};
if ("$CATALINA_BASE" eq "") {
    $CATALINA_BASE  = $CATALINA_HOME;
}

# --- character set
$GTS_CHARSET        = $ENV{'GTS_CHARSET'};
if ("$GTS_CHARSET" ne "") {
    $JAVA_CHARSET   = "-Dfile.encoding=${GTS_CHARSET}";
} else {
    $JAVA_CHARSET   = "-Dfile.encoding=UTF-8";
    #$JAVA_CHARSET   = "-Dfile.encoding=ISO-8859-1";
}

# --- current directory
$PWD                = &deblank(`pwd`);
$PWD_               = $PWD . "/";

# --- JAVA_HOME
$JAVA_HOME          = $ENV{"JAVA_HOME"};
if ("$JAVA_HOME" eq "") {
    print "JAVA_HOME not defined!\n";
}
if (($JAVA_HOME =~ / /) && !($JAVA_HOME =~ /^\"/)) { # && !($JAVA_HOME =~ /\\/))
    # - contains embedded spaces, and is not already quoted/escaped
    $JAVA_HOME = "\"$JAVA_HOME\""; # - quote for Windows
    #$JAVA_HOME =~ s/ /\\ /g;       # - escape for Linux
}

# --- memory
$ENV_JAVA_MEMORY = $ENV{"JAVA_MEMORY"};
if ("$ENV_JAVA_MEMORY" eq "") {
    # --- JAVA_MEMORY not defined in environment variables
    if (!defined($JAVA_MEMORY) || ("$JAVA_MEMORY" eq "")) {
        # --- JAVA_MEMORY not already specified by parent script
        $JAVA_MEMORY = "300m";
        #print "Using default JAVA_MEMORY: ${JAVA_MEMORY}\n";
    } else {
        # --- JAVA_MEMORY already specified in parent script
        #print "Using pre-specified JAVA_MEMORY: ${JAVA_MEMORY}\n";
    }
} else {
    # --- use JAVA_MEMORY from environment variables
    $JAVA_MEMORY = "${ENV_JAVA_MEMORY}";
}
$JAVAMEM = "-Xmx${JAVA_MEMORY}";

# --- Java commands
$cmd_java     = (-e "$JAVA_HOME/bin/java")? "$JAVA_HOME/bin/java" : &findCmd("java");
$cmd_java     = "$cmd_java $JAVAMEM $JAVA_CHARSET";
#$cmd_jar     = (-e "$JAVA_HOME/bin/jar" )? "$JAVA_HOME/bin/jar"  : &findCmd("jar" );
$cmd_ant      = &findCmd("ant"  ,$false);

# --- other commands
$cmd_which    = &findCmd("which",$true);    # - used for determining a path of a command
$cmd_mkdir    = &findCmd("mkdir",$true);
$cmd_cp       = &findCmd("cp"   ,$true);
$cmd_kill     = &findCmd("kill" ,$true);
$cmd_cat      = &findCmd("cat"  ,$true);
$cmd_rm       = &findCmd("rm"   ,$true);
$cmd_ps       = &findCmd("ps"   ,$true);
$cmd_uname    = &findCmd("uname",$false);
$cmd_grep     = &findCmd("grep" ,$false);
$cmd_sed      = &findCmd("sed"  ,$false);
$cmd_diff     = &findCmd("diff" ,$false);

# --- Cygwin?
$UNAME        = ($cmd_uname ne "")? `$cmd_uname` : ""; chomp $UNAME;
$IS_CYGWIN    = ($UNAME =~ /^CYGWIN/)? $true : $false;
if ($IS_CYGWIN) { 
    $cmd_cygpath = &findCmd("cygpath",$true);
}

# --- Mac OSX?
$IS_MACOSX    = ($UNAME =~ /^Darwin/)? $true : $false;

# --- "GTS_HOME" installation directory
# - "GTS_HOME" should already be defined, this just makes sure
if ("$GTS_HOME" eq "") {
    $GTS_HOME = $ENV{"GTS_HOME"};
    if ("$GTS_HOME" eq "") {
        my $home = &getCommandPath($0) . "/..";
        $GTS_HOME = realpath($home);
    }
}
if ($IS_CYGWIN) { 
    $GTS_HOME = `$cmd_cygpath --mixed "$GTS_HOME"`; chomp $GTS_HOME; 
}
if (($GTS_HOME =~ / /) && !($GTS_HOME =~ /^\"/)) { # && !($GTS_HOME =~ /\\/))
    # - contains embedded spaces, and is not already quoted
    $GTS_HOME = "\"$GTS_HOME\""; # - quote for Windows
    #$GTS_HOME =~ s/ /\\ /g;      # - escape for Linux
}

# --- "GTS_CONF" config file name
$GTS_CONF = $ENV{"GTS_CONF"};
if ("$GTS_CONF" eq "") {
    my $conf = "$GTS_HOME/default.conf";
    $GTS_CONF = $conf;
}
if ($IS_CYGWIN) { 
    $GTS_CONF = `$cmd_cygpath --mixed "$GTS_CONF"`; chomp $GTS_CONF; 
}
if (($GTS_CONF =~ / /) && !($GTS_CONF =~ /^\"/)) { # && !($GTS_CONF =~ /\\/))
    # - contains embedded spaces, and is not already quoted
    $GTS_CONF = "\"$GTS_CONF\""; # - quote for Windows
    #$GTS_CONF =~ s/ /\\ /g;      # - escape for Linux
}

# --- JAR directory (modify for production environment)
$JARDIR = "$GTS_HOME/build/lib";
$SCAN_JARDIR_FOR_JARS = $false;

# --- Java classpath
$CLASSPATH = "";
if ($SCAN_JARDIR_FOR_JARS) {
    opendir(JARLIB_DIR, "$JARDIR");
    my @JARLIB_LIST = readdir(JARLIB_DIR);
    closedir(JARLIB_DIR);
    foreach my $JARLIB_FILE (@JARLIB_LIST) {
        if ($JARLIB_FILE =~ /.jar$/) {
            # print "Found JAR: $JARLIB_FILE\n";
            if (("$JARLIB_FILE" eq "dmtpserv.jar") || ("$JARLIB_FILE" eq "gtsdmtp.jar")) {
                if ("$CLASSPATH" ne "") { $CLASSPATH .= $PATHSEP; }
                $CLASSPATH .= "$JARDIR/$JARLIB_FILE";
            } 
            elsif (("$JARLIB_FILE" eq "icare.jar") || ("$JARLIB_FILE" eq "template.jar")) {
                # skip
            } else {
                if ("$CLASSPATH" ne "") { $CLASSPATH .= $PATHSEP; }
                $CLASSPATH .= "$JARDIR/$JARLIB_FILE";
            }
        }
    }
} else {
    my @CJARS = (
        "gtsdb.jar",                            # - GTS db tables & utilities
        "gtsutils.jar",                         # - GTS utilities
        "optdb.jar",                            # - optional GTS db tables & utilities
        "ruledb.jar",                           # - optional RuleFactory support
        "bcrossdb.jar",                         # - optional RuleFactory support
        "custom.jar",                           # - custom code
        "gtsdmtp.jar",                          # - GTS DMTP tables/support
        "dmtpserv.jar",                         # - DMTP server
       #"activation.jar",                       # - JavaMail [optional here]
       #"mail.jar",                             # - JavaMail [optional here]
       #"mysql-connector-java-3.1.7-bin.jar",   # - MySQL JDBC [optional here]
    );
    foreach ( @CJARS ) {
        if (-f "$JARDIR/$_") {
            if ("$CLASSPATH" ne "") { $CLASSPATH .= $PATHSEP; }
            $CLASSPATH .= "$JARDIR/$_";
        }
    }
}
if ("$CATALINA_HOME" ne "") {
    # - DBCP - DB Connection Pooling
    #if ("$CLASSPATH" ne "") { $CLASSPATH .= $PATHSEP; }
    #$CLASSPATH .= "$CATALINA_HOME/common/lib/naming-factory-dbcp.jar";
}
#print "CLASSPATH = $CLASSPATH\n";

# -----------------------------------------------------------------------------

# --- remove leading/trailing spaces
sub deblank(\$) {
    my ($x) = @_;
    $x =~ s/^[ \t\n\r]*//; # --- leading
    $x =~ s/[ \t\n\r]*$//; # --- trailing
    return $x;
}

# --- execute command
sub sysCmd(\$\$) {
    my ($cmd, $verbose) = @_;
    if ($verbose) { print "$cmd\n"; }
    my $rtn = system("$cmd") / 256;
    return $rtn;
}

# --- fork and execute command
sub forkCmd(\$\$\$) {
    my ($cmd, $outLog, $verbose) = @_;
    if ($verbose) { print "$cmd\n"; }
    my $pid = fork();
    if ($pid == 0) {
        if ($outLog ne "") {
            # - this is done this way to make sure that the 'exec' below
            # - doesn't perform another 'fork'.  Otherwise we would have
            # - just included the redirection in the command string.
            if (open(LOGOUT, ">>$outLog")) {
                open(STDOUT, ">&LOGOUT");   # redirect stdout
                open(STDERR, ">&LOGOUT");   # redirect stderr
            } else {
                print "ERROR: Unable to redirect STDOUT/STDERR!\n";
                exit(99);
            }
        }
        select(STDERR); $|=1;
        select(STDOUT); $|=1;
        exec("$cmd");   # -- does not return if command was started successfully
        exit(1);        # -- only reaches here if the above 'exec' failed
    }
    return $pid;
}

# --- read from stdin
sub readStdin(\$) {
    my ($msg) = @_;
    print "$msg\n";
    my $readIn = <>;
    chomp $readIn; # --- remove trailing '\r'
    return $readIn;
}

# --- find location of specified command
sub findCmd(\$\$) {
    my ($cmdLine, $mustFind) = @_;
    if ($cmdLine =~ /^\//) {
        return $cmdLine; # - already absolute path
    } else {
        my @CPATH = (
            "/sbin",
            "/bin",
            "/usr/bin",
            "/use/local/bin",
            "/mysql/bin",  # --- laptop
            # --- add directories as necessary
        );
        my @cmdArgs = split(' ', $cmdLine);
        my $cmd = $cmdArgs[0];
        foreach ( @CPATH ) {
            if (-x "$_/$cmd") {
                #print "Found: $_/$cmd\n";
                $cmdArgs[0] = "$_/$cmd";
                return join(' ', @cmdArgs);
            }
        }
        #print "Not found: $cmd\n";
        return $mustFind? "" : $cmdLine;
    }
}

# --- return the absolute path of the specified command
sub getCommandPath(\$) {
    my $cmd = $_[0];
    if ("$cmd_which" ne "") {
        my $c = `$cmd_which $cmd 2>/dev/null`;
        chomp $c;
        if ("$c" ne "") {
            $cmd = $c;
        }
    }
    return &getFilePath($cmd);
}

# --- return the path of the specified file
sub getFilePath(\$) {
    my $x = $_[0];
    my $abs = ($x =~ /^\//);
    if ($x =~ /^(.*)\/(.*)$/) {
        $x =~ s/^(.*)\/(.*)$/\1/;
        if ($x ne "") {
            return $x;
        } else {
            return "/";
        }
    }
    return ".";
}

# --- return filename portion of a full file path
sub getFileName(\$) {
    my ($x) = @_;
    $x =~ s/^(.*)\/(.*)$/$2/;
    return $x;
}

# --- return file extension portion of a full file path
sub getFileExtn(\$) {
    my $x = $_[0];
    my $ext = &getFileName($x);
    $ext =~ s/^(.*)\.//;
    return $ext;
}

# --- return property value
sub getPropertyString(\$) {
    my $k = $_[0];
    my $kv = `($cmd_grep '^$k' $GTS_CONF | $cmd_sed 's/^\\(.*\\)=//')`; chomp $kv;
    return $kv;
    #if ($kv ne "") {
    #    my $v = $kv;
    #    $v =~ s/^(.*)=//;
    #    return $v;
    #}
    #return "";
}

# General command startup initialization complete
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# --- return 'true'
$true;

:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : common.bat
:: -----------------------------------------------------------------------------
:: This command is to be included in other '.bat' commands and is not intended
:: to be executed separately.
:: -----------------------------------------------------------------------------
:: Environment variables:
::   OS          [optional] The operating system indicator (ie. "Windows")
::   GTS_HOME    [required] OpenGTS installation directory (MUST be set)
::   GTS_CONF    [optional] Full path to OpenGTS runtime config file (ie. 'default.conf')
::   GTS_CHARSET [optional] The character set used when starting up the Java proces
::   GTS_DEBUG   [optional] '1' for debug mode (echoes java command), blank/0 otherwise
:: -----------------------------------------------------------------------------

REM --- %GTS_DEBUG% debug mode
if "%GTS_DEBUG%"=="" goto gdmBlank
    set GTS_DEBUG=1
    goto gdmEnd
:gdmBlank
    set GTS_DEBUG=0
:gdmEnd

REM --- %GTS_CONF% config file name
if NOT "%GTS_CONF%"=="" goto gtsconfOK
set GTS_CONF=%GTS_HOME%\default.conf
:gtsconfOK

REM --- %GTS_CONF% exists?
if EXIST "%GTS_CONF%" goto confExists
echo "%GTS_CONF%" does not exist
:confExists

REM --- Character set
if "%GTS_CHARSET%"=="" goto csBlank
    set JAVA_CHARSET="-Dfile.encoding=%GTS_CHARSET%"
    goto csEnd
:csBlank
    set JAVA_CHARSET="-Dfile.encoding=UTF-8"
    REM set JAVA_CHARSET="-Dfile.encoding=ISO-8859-1"
:csEnd

REM --- Java command
if "%JAVA_HOME%" == "" echo Missing JAVA_HOME environment variable
set CMD_JAVA="%JAVA_HOME%\bin\java" %JAVA_CHARSET%
::if EXIST "%CMD_JAVA%" goto javaExists
::echo "%CMD_JAVA%" does not exist
::goto exit
::javaExists

REM --- set the classpath
set JARDIR="%GTS_HOME%\build\lib"
set CPATH=%JARDIR%\gtsdb.jar;%JARDIR%\gtsutils.jar;%JARDIR%\optdb.jar;%JARDIR%\ruledb.jar;%JARDIR%\bcrossdb.jar;%JARDIR%\custom.jar;%JARDIR%\dmtpserv.jar;%JARDIR%\gtsdmtp.jar;

REM --- add DBCP to classpath
REM if NOT EXISTS "%CATALINA_HOME%/common/lib/naming-factory-dbcp.jar" goto noTomcat
REM set CPATH=%CPATH%%CATALINA_HOME%/common/lib/naming-factory-dbcp.jar;
:noTomcat

REM ---

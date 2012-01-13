@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : dbconfig.bat
:: -----------------------------------------------------------------------------
:: This script provides a very rough approximation of the 'dbAdmin.pl' command.
:: This invokes org.opengts.db.DBConfig directly, passing any command-line args.
:: -----------------------------------------------------------------------------
if "%GTS_HOME%" == "" echo Missing GTS_HOME environment variable
call "%GTS_HOME%\bin\common.bat"

REM ---
set MAIN=org.opengts.db.DBConfig
set ARGS=-conf:"%GTS_CONF%" -log.file.enable:false %1 %2 %3 %4 %5
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %MAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %MAIN% %ARGS%

REM ---

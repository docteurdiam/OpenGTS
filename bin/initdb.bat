@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : initdb.bat
:: -----------------------------------------------------
::  Valid Options:  (options should be enclosed in quotes)
::     -rootUser:<id>    MySQL user with permissions to create a database and tables
::     -rootPass:<pass>  Password for specified user
::  This command is equivalent to running the following:
::     % bin/dbAdmin.pl -createdb -grant -tables -user=<root>
:: -----------------------------------------------------
if "%GTS_HOME%" == "" echo Missing GTS_HOME environment variable
call "%GTS_HOME%\bin\common.bat"
set GTS_DEBUG=1

REM ---
::set NEWDEV=-newAccount=opendmtp -newDevice=mobile
set NEWDEV=

REM ---
set MAIN=org.opengts.db.DBConfig
set ARGS=-conf:"%GTS_CONF%" -log.file.enable:false -initTables %NEWDEV% %1 %2 %3 %4
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %MAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %MAIN% %ARGS%

REM ---

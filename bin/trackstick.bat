@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : trackstick.bat
:: -----------------------------------------------------------------------------
:: Usage:
::   > bin\trackstick.bat -a:<AccountID> -d:<DeviceID> -tmz:<TimeZone> -csv:<TrackStickCSVFile> 
:: Example:
::   > bin\trackstick.bat -a:demo -d:demo -tmz:GMT -csv:2011-03-26_04.05.10.csv
:: -----------------------------------------------------------------------------
:: This assumes that GTS_HOME has already been set
if NOT "%GTS_HOME%" == "" goto gtsHomeFound
    echo ERROR Missing GTS_HOME environment variable
    goto exit
:gtsHomeFound
call "%GTS_HOME%\bin\common.bat"

REM --- main entry point
set JMAIN=org.opengts.db.TrackStick

REM ---
set ARGS=-conf:"%GTS_CONF%" %1 %2 %3 %4 %5 %6 %7
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%

REM ---
:exit

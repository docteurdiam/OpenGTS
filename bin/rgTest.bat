@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : rgTest.bat
:: -----------------------------------------------------------------------------
:: Usage:
::   > bin\rgTest.bat -pl:<PrivateLabelName> -gp:<Latitude>/<Longitude> 
:: Example:
::   > bin\rgTest.bat -pl:default -gp:39.12345/-142.12345
:: -----------------------------------------------------------------------------
:: This assumes that GTS_HOME has already been set
if NOT "%GTS_HOME%" == "" goto gtsHomeFound
    echo ERROR Missing GTS_HOME environment variable
    goto exit
:gtsHomeFound
call "%GTS_HOME%\bin\common.bat"

REM --- main entry point
set JMAIN=org.opengts.geocoder.ReverseGeocodeProviderAdapter

REM ---
set ARGS=-conf:"%GTS_CONF%" %2 %3 %4 %5 %6
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%

REM ---
:exit

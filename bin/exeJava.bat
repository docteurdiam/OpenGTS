@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : exeJava.bat
:: -----------------------------------------------------------------------------
if "%GTS_HOME%" == "" echo Missing GTS_HOME environment variable
:echo "%GTS_HOME%\bin\common.bat"
call "%GTS_HOME%\bin\common.bat"

REM --- main entry point
set JMAIN=%1

REM ---
set ARGS=-conf:"%GTS_CONF%" %2 %3 %4 %5 %6
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%

REM ---
:exit

@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : checkInstall.bat
:: -----------------------------------------------------------------------------
:: This script invokes CheckInstall, passing any command-line args to the 
:: Java org.opengts.tools.CheckInstall class
:: Usage:
::   > bin\checkInstall.bat
:: -----------------------------------------------------------------------------
if NOT "%GTS_HOME%" == "" goto homeOK
    echo Missing GTS_HOME environment variable
    goto exit
:homeOK

REM --- %GTS_HOME% exists?
if EXIST "%GTS_HOME%" goto homeExists
    echo GTS_HOME %GTS_HOME% does not exist
    goto exit
:homeExists

REM --- %GTS_CONF% config file name
if NOT "%GTS_CONF%" == "" goto gtsconfOK
    set GTS_CONF=%GTS_HOME%\default.conf
:gtsconfOK

REM --- %GTS_CONF% exists?
if EXIST "%GTS_CONF%" goto confExists
    echo GTS_CONF "%GTS_CONF%" does not exist
    goto exit
:confExists

REM -- GTS jar library directory
set JARDIR=build\lib
if EXIST "%JARDIR%\tools.jar" goto toolsExists
    echo ERROR: "%JARDIR%\tools.jar" not found!
    echo Possible reasons may include one or more of the following:
    echo  - This command is not being run from the OpenGTS installation directory
    echo  - The OpenGTS project has not been compiled properly
    echo  - The GTS_HOME environment variable has not been properly set
:toolsExists

REM -- 'track' classes directory
set TRACKCLASSES=build\track\WEB-INF\classes
if EXIST "%TRACKCLASSES%" goto trackExists
    echo ERROR: "%TRACKCLASSES%" not found!
    echo Possible reasons may include one or more of the following:
    echo  - This command is not being run from the OpenGTS installation directory
    echo  - The OpenGTS project has not been compiled properly
    echo  - The GTS_HOME environment variable has not been properly set
    echo This condition may cause false errors/warnings to be reported!
:trackExists

REM -- Tomcat classes directory
set TOMCATAPI="%CATALINA_HOME%"\common\lib\servlet-api.jar
if EXIST %TOMCATAPI% goto tomcatExists
set TOMCATAPI="%CATALINA_HOME%"\lib\servlet-api.jar
if EXIST %TOMCATAPI% goto tomcatExists
    echo ERROR: %TOMCATAPI% not found!
    echo Possible reasons may include one or more of the following:
    echo  - The Tomcat 'CATALINA_HOME' environment variable was not set properly
    echo  - Tomcat was not installed properly
:tomcatExists

REM --- Java command
set CMD_JAVA=java

REM --- headless
set HEADLESS=-Djava.awt.headless=true

REM --- set the classpath
set CPATH=%JARDIR%\tools.jar;%JARDIR%\gtsdb.jar;%JARDIR%\gtsutils.jar;%JARDIR%\dmtpserv.jar;%JARDIR%\gtsdmtp.jar
set CPATH=%CPATH%;%JARDIR%\ruledb.jar;%JARDIR%\ruletrack.jar
set CPATH=%CPATH%;%JARDIR%\bcrossdb.jar;%JARDIR%\bcrosstrack.jar
set CPATH=%CPATH%;%JARDIR%\optdb.jar;%JARDIR%\opttrack.jar
set CPATH=%CPATH%;%JARDIR%\wartools.jar;%JARDIR%\warmaps.jar
set CPATH=%CPATH%;%TRACKCLASSES%
set CPATH=%CPATH%;%TOMCATAPI%
REM set CPATH=%CPATH%;"%CATALINA_HOME%"\common\lib\servlet-api.jar
REM set CPATH=%CPATH%;"%CATALINA_HOME%"\common\lib\naming-factory-dbcp.jar
REM set CPATH=%CPATH%;"%CATALINA_HOME%"\lib\servlet-api.jar

REM ---
set MAIN=org.opengts.tools.CheckInstall
set ARGS="-conf:%GTS_CONF%" -log.file.enable:false %1 %2 %3 %4
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% %HEADLESS% -classpath %CPATH% %MAIN% %ARGS%
:noEcho
%CMD_JAVA% %HEADLESS% -classpath %CPATH% %MAIN% %ARGS%

REM ---
:exit

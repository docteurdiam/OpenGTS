@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : admin.bat
:: -----------------------------------------------------------------------------
:: Database administrative command.
:: Usage:
::   > bin\admin.bat <TableName> [<Options>]
:: Example:
::   > bin\admin.bat Account -list
:: -----------------------------------------------------------------------------
if "%GTS_HOME%" == "" echo Missing GTS_HOME environment variable
call "%GTS_HOME%\bin\common.bat"

REM --- find the table class entry point
set JTABLE=
if "%1"=="Account"           (set JTABLE=org.opengts.db.tables.Account)              & goto foundTable
if "%1"=="AccountString"     (set JTABLE=org.opengts.db.tables.AccountString)        & goto foundTable
if "%1"=="Device"            (set JTABLE=org.opengts.db.tables.Device)               & goto foundTable
if "%1"=="DeviceGroup"       (set JTABLE=org.opengts.db.tables.DeviceGroup)          & goto foundTable
if "%1"=="DeviceList"        (set JTABLE=org.opengts.db.tables.DeviceList)           & goto foundTable
if "%1"=="Driver"            (set JTABLE=org.opengts.db.tables.Driver)               & goto foundTable
if "%1"=="Geozone"           (set JTABLE=org.opengts.db.tables.Geozone)              & goto foundTable
if "%1"=="Resource"          (set JTABLE=org.opengts.db.tables.Resource)             & goto foundTable
if "%1"=="Role"              (set JTABLE=org.opengts.db.tables.Role)                 & goto foundTable
if "%1"=="RoleAcl"           (set JTABLE=org.opengts.db.tables.RoleAcl)              & goto foundTable
if "%1"=="StatusCode"        (set JTABLE=org.opengts.db.tables.StatusCode)           & goto foundTable
if "%1"=="SystemProps"       (set JTABLE=org.opengts.db.tables.SystemProps)          & goto foundTable
if "%1"=="Transport"         (set JTABLE=org.opengts.db.tables.Transport)            & goto foundTable
if "%1"=="UniqueXID"         (set JTABLE=org.opengts.db.tables.UniqueXID)            & goto foundTable
if "%1"=="User"              (set JTABLE=org.opengts.db.tables.User)                 & goto foundTable
if "%1"=="UserAcl"           (set JTABLE=org.opengts.db.tables.UserAcl)              & goto foundTable
if "%1"=="PendingPacket"     (set JTABLE=org.opengts.db.dmtp.PendingPacket)          & goto foundTable
if "%1"=="Property"          (set JTABLE=org.opengts.db.dmtp.Property)               & goto foundTable
if "%1"=="Entity"            (set JTABLE=org.opengts.extra.tables.Entity)            & goto foundTable
if "%1"=="SessionStats"      (set JTABLE=org.opengts.extra.tables.SessionStats)      & goto foundTable
if "%1"=="UnassignedDevices" (set JTABLE=org.opengts.extra.tables.UnassignedDevices) & goto foundTable
if "%1"=="GeoCorridor"       (set JTABLE=org.opengts.rule.tables.GeoCorridor)        & goto foundTable
if "%1"=="GeoCorridorList"   (set JTABLE=org.opengts.rule.tables.GeoCorridorList)    & goto foundTable
if "%1"=="Rule"              (set JTABLE=org.opengts.rule.tables.Rule)               & goto foundTable
if "%1"=="RuleList"          (set JTABLE=org.opengts.rule.tables.RuleList)           & goto foundTable
if "%1"=="BorderCrossing"    (set JTABLE=org.opengts.bcross.tables.BorderCrossing)   & goto foundTable
echo Table NOT found: %1
goto :EOF

REM --- found the table entry point
:foundTable
if "%2"=="test" (echo Table Found: %1) & goto :EOF
shift

REM ---
set ARGS=-conf:"%GTS_CONF%" -log.file.enable:false %1 %2 %3 %4 %5
if "%GTS_DEBUG%"=="1" echo %CMD_JAVA% -classpath %CPATH% %JTABLE% %ARGS%
%CMD_JAVA% -classpath %CPATH% %JTABLE% %ARGS%
goto :EOF

REM ---
:exit

echo off

set scriptpath=%~dp0
set packagepath=%scriptpath%
set modulespath=%packagepath%\modules

set "JAVA_CMD=%packagepath%jdk\bin\java"
set "BANALYTICS_HOME=%packagepath%"

echo Java command: %JAVA_CMD%
echo Banalytics Home: %BANALYICS_HOME%

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -XX:VMOptionsFile=config/banalytics.vmoptions -cp "%packagepath%banalytics-box.jar";"%modulespath%\*" com.banalytics.box.BanalyticsBoxApplication
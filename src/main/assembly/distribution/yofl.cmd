@echo off

rem ---------------------------------------------------------------------------
rem Yetamine OSGi Framework Launcher bootstrap script
rem
rem Environment variables can be used to set or override some parameters. Do
rem not set the variables in this script. For specific deployment or 
rem application the variables should be rather set by the specific wrapper.
rem
rem JAVA
rem   The command to launch JVM. Default assumes 'java' command available from
rem   PATH.
rem
rem JAVA_OPTS
rem   Additional Java runtime options passed to the JAVA command.
rem
rem YOFL_AUTOPATH
rem   The path to the directory which shall be scanned for the .jar files to be
rem   added to the boot class path.
rem
rem YOFL_BOOTPATH
rem   The class path to be appended to the boot class path.
rem
rem YOFL_LOGGING_FILE
rem   The desired output stream for the launcher logger. The value may be
rem   'stdout' or 'stderr', which applies as the default, or a file name.
rem
rem YOFL_LOGGING_LEVEL
rem   The logging level for the launcher logger.
rem
rem ---------------------------------------------------------------------------

setlocal

rem Guess YOFL_HOME if not defined
if not "%YOFL_HOME%" == "" goto gotHome
rem Use the pushd/cd/popd sequence to get the path without trailing backslash
pushd "%~dp0"
set YOFL_HOME=%cd%
popd
:gotHome
if exist "%YOFL_HOME%\yofl.jar" goto okHome
echo The YOFL_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
exit /B 1
:okHome

rem Ensure that YOFL_HOME does not contain a semi-colon as this is used as
rem the separator in the classpath and Java provides no mechanism for escaping
rem if the same character appears in the path. Check this by replacing all
rem occurrences of ';' with '' and checking that it did not change
if "%YOFL_HOME%" == "%YOFL_HOME:;=%" goto homeNoSemicolon
echo Unable to start as YOFL_HOME contains a semicolon (;) character
exit /B 1
:homeNoSemicolon

rem Set the Java executable
if "%JAVA%" == "" set JAVA=java

rem The main class, which must be given explicitly when using -classpath
set MAIN=net.yetamine.osgi.launcher.Main

rem Override any existing CLASSPATH
set CLASSPATH=%YOFL_HOME%\yofl.jar
rem Extend the class path with configured variables
if not "%YOFL_BOOTPATH%" == "" set CLASSPATH=%CLASSPATH%;%YOFL_BOOTPATH%

if "%YOFL_AUTOPATH%" == "" goto classpathDone
pushd "%YOFL_AUTOPATH%"
for %%G in (*.jar) do call:appendToClasspath %%G "%YOFL_AUTOPATH%"
popd
goto classpathDone

:appendToClasspath
set filename=%~1
set suffix=%filename:~-4%
if %suffix% equ .jar set CLASSPATH=%CLASSPATH%;%~2\%filename%
goto :EOF

:classpathDone

rem Execute Java to launch the launcher
"%JAVA%" %JAVA_OPTS% "-Dnet.yetamine.osgi.launcher.logging.level=%YOFL_LOGGING_LEVEL%" "-Dnet.yetamine.osgi.launcher.logging.file=%YOFL_LOGGING_FILE%" -classpath "%CLASSPATH%" "%MAIN%" %*

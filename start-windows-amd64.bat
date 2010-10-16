@echo off
if NOT EXIST "%SystemRoot%\SysWOW64" goto JAVA32

if "%ProgramFiles%" == "%ProgramFiles(x86)%" goto JAVA32SHELL

set key=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment
set JAVA_VERSION=
set JAVA_HOME=
for /f "tokens=3* skip=2" %%a in ('reg query "%key%" /v CurrentVersion') do set JAVA_VERSION=%%a
for /f "tokens=2* skip=2" %%a in ('reg query "%key%\%JAVA_VERSION%" /v JavaHome') do set JAVA_HOME=%%b

if not exist "%JAVA_HOME%\bin\java.exe" goto JAVAMISSING
echo Running Jpcsp 64bit...
"%JAVA_HOME%\bin\java" -Xmx1024m -Djava.library.path=lib/windows-amd64 -jar bin/jpcsp.jar
goto END

:JAVA32
echo Unable to run a 64bit build on a 32bit platform. Install a 64bit version of Windows first.
pause
goto END

:JAVA32SHELL
echo Unable to properly run a 64bit application from a 32bit context on a 64bit platform.
pause
goto END

:JAVAMISSING
echo The required version of Java has not been installed.
echo Go to http://java.sun.com to install the 64bit Java JRE.
pause

:END
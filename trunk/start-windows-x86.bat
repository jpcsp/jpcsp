@echo off
set PATH=%PATH%;lib\;lib\windows-x86\

if NOT EXIST "%SystemRoot%\SysWOW64" goto JAVA32
set key=HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment
goto JAVA

:JAVA32
set key=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment

:JAVA
set JAVA_VERSION=
set JAVA_HOME=
for /f "tokens=3* skip=2" %%a in ('reg query "%key%" /v CurrentVersion') do set JAVA_VERSION=%%a
for /f "tokens=2* skip=2" %%a in ('reg query "%key%\%JAVA_VERSION%" /v JavaHome') do set JAVA_HOME=%%b

if not exist "%JAVA_HOME%\bin\java.exe" goto JAVAMISSING
echo Running Jpcsp 32bit...
"%JAVA_HOME%\bin\java" -Xmx1024m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=64m -Djava.library.path=lib/windows-x86 -jar bin/jpcsp.jar
goto END

:JAVAMISSING
echo The required version of Java has not been installed or isn't recognized.
echo Go to http://java.sun.com to install the 32bit Java JRE.
pause

:END
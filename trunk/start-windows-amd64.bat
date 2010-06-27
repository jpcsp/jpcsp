@echo off
if "%programfiles(x86)%XXX"=="XXX" goto JAVA32
if not exist "%programfiles%\Java\jre6\bin" goto JAVAMISSING
echo Running Jpcsp 64bit...
"%programfiles%\Java\jre6\bin\java" -Xmx1024m -Djava.library.path=lib/windows-amd64 -jar bin/jpcsp.jar
goto END

:JAVA32
echo Unable to run a 64bit build on a 32bit platform. Install a 64bit version of Windows first.
pause
goto END

:JAVAMISSING
echo The required version of Java has not been installed.
echo Go to http://java.sun.com to install the 64bit Java JRE.
pause

:END
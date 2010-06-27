@echo off
set PATH=%PATH%;lib\;lib\windows-x86\
if "%programfiles(x86)%XXX"=="XXX" goto JAVA32
if not exist "%programfiles(x86)%\Java\jre6\bin" goto JAVAMISSING
echo Running Jpcsp 32bit...
"%programfiles(x86)%\Java\jre6\bin\java" -Xmx1024m -Djava.library.path=lib/windows-x86 -jar bin/jpcsp.jar
goto END

:JAVA32
if not exist "%programfiles%\Java\jre6\bin" goto JAVAMISSING
echo Running Jpcsp 32bit...
"%programfiles%\Java\jre6\bin\java" -Xmx1024m -Djava.library.path=lib/windows-x86 -jar bin/jpcsp.jar
goto END

:JAVAMISSING
echo The required version of Java has not been installed or isn't recognized.
echo Go to http://java.sun.com to install the 32bit Java JRE.
pause

:END
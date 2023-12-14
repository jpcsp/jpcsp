@echo off
rem change to the directory where the script itself; this is required when running as an administrator
cd /d "%~dp0"

rem check if 64-bit OS
if not exist "%SYSTEMROOT%\SysWOW64" ( goto OS_ARCH )

rem check if java is installed
java -help 2> nul
if not %errorlevel% equ 0 ( goto JAVA_MISSING )

rem check the path of java executable if exist and configured
set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
if not exist %JAVA_CMD% ( goto JAVA_MISSING )

rem check java if can run 64-bit application
java -d64 -version > nul 2>&1
if not %errorlevel% equ 0 ( goto JAVA_ARCH )

java -version
echo JAVA_HOME Path: %JAVA_HOME%

echo Running JPCSP 64-bit...
%JAVA_CMD%^
    -Xmx2048m -Xss2m -XX:ReservedCodeCacheSize=64m^
    -Djava.library.path=lib/windows-amd64;lib/jinput-2.0.9-natives-all^
    -Djinput.useDefaultPlugin=false^
    -Dorg.lwjgl.system.allocator=system^
    -classpath "bin/jpcsp.jar;lib/lwjgl-3.2.3/lwjgl.jar;lib/lwjgl-3.2.3/lwjgl-openal.jar;lib/lwjgl-3.2.3/lwjgl-opengl.jar;lib/lwjgl-3.2.3/lwjgl-jawt.jar;lib/lwjgl-3.2.3/lwjgl-natives-windows.jar;lib/lwjgl-3.2.3/lwjgl-openal-natives-windows.jar;lib/lwjgl-3.2.3/lwjgl-opengl-natives-windows.jar;lib/lwjgl-3.2.3/lwjgl-glfw.jar;lib/lwjgl-3.2.3/lwjgl-glfw-natives-windows.jar"^
    jpcsp.MainGUI %*
goto END

:OS_ARCH
echo Unable to run a 64-bit JPCSP on a 32-bit system. Install a 64-bit version of Windows first
echo or download the 32-bit JPCSP from https://jpcsp.org/index?p=downloads.
goto END

:JAVA_ARCH
echo Unable to properly run a 64-bit JPCSP from 32-bit JRE on a 64-bit system.
goto END

:JAVA_MISSING
echo The required version of Java Runtime Environment(JRE) has not been installed or isn't recognized.
echo Go to https://adoptium.net/temurin/releases/ to install the 64-bit(x64) JRE.
echo NOTE:
echo   If you already installed the JRE then you need to set up the JAVA_HOME variable
echo   and add to the PATH with \bin like this: %%JAVA_HOME%%\bin
goto END

:END
pause

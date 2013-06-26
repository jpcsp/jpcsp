GCC=x86_64-w64-mingw32-gcc.exe

$GCC -Wall -static-libgcc "-D__int64=long long" -I "$JAVA_HOME/include/" -I "$JAVA_HOME/include/win32/" -Wl,--kill-at -mdll -m64 -O3 memory.c -o memory.dll

cp memory.dll ../lib/windows-amd64/memory.dll

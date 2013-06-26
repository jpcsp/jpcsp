GCC=gcc

$GCC -mno-cygwin -Wall -static-libgcc "-D__int64=long long" -I "$JAVA_HOME/include/" -I "$JAVA_HOME/include/win32/" -Wl,--kill-at -mdll -m32 -O3 memory.c -o memory.dll

cp memory.dll ../lib/windows-x86/memory.dll

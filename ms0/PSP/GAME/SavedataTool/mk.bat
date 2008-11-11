ee-gcc -march=r4000 -g -mgp32 -mlong32 -c savedatatool.c
ee-gcc -march=r4000 -g -mgp32 -mlong32 -c pg.c
ee-gcc -march=r4000 -g -mgp32 -c -xassembler -O -o startup.o startup.s
ee-ld -O0 -G0 startup.o savedatatool.o pg.o -M -Ttext 8900000 -q -o out > savedatatool.map
ee-strip out
outpatch
copy /y outp UNKNOWN.PSP
pack-pbp EBOOT.PBP PARAM.SFO ICON0.PNG NULL NULL NULL NULL UNKNOWN.PSP NULL
copy /y EBOOT.PBP d:\PSP\GAME\SAVEDATATOOL\EBOOT.PBP

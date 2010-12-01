@echo off

rem TODO HIMDRender is producing some noise, how to avoid it?

lib\windows-x86\HIMDRender.exe -e -i %1 -o %2

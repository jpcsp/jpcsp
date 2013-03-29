@echo off
if exist lib\windows-x86\at3tool.exe goto at3tool
lib\windows-x86\HIMDRender.exe -e -i %1 -o %2
goto end
:at3tool
lib\windows-x86\at3tool.exe -d -repeat 0 %3 %2
:end

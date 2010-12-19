@echo off

rem Muting the system volume because HIMDRender is producing some noise
lib\windows-x86\nircmd\nircmdc.exe mutesysvolume 1

lib\windows-x86\HIMDRender.exe -e -i %1 -o %2


rem un-Muting the system volume
lib\windows-x86\nircmd\nircmdc.exe mutesysvolume 0

@echo off
if not exist lib\windows-amd64\atrac3plus2wav.exe goto at3tool
lib\windows-amd64\atrac3plus2wav.exe %1
if not exist "%1.wav" goto at3tool
rename %1.wav %2
goto end

:at3tool
if not exist lib\windows-amd64\at3tool.exe goto himdrender
lib\windows-amd64\at3tool.exe -d -repeat 1 %3 %2
if not exist "%2" goto himdrender
goto end

:himdrender
lib\windows-amd64\HIMDRender.exe -e -i %1 -o %2
goto end

:end

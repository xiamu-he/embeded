@echo off
set CLASSPATH=./out/production/embedded_2023;%CLASSPATH%
setlocal enabledelayedexpansion

set fileName=result.txt
set content= $(date +%Y%m%d)_$(date +%H%M%S)
javac -encoding UTF-8 -d ./out/production/embedded_2023 ./src/Main.java

echo ***************%date% %time%*************** >> %fileName%
echo *********2********* >> %fileName%
set start_time=!time!
java Main < ./data/2.in
echo *********4********* >> %fileName%
java Main < ./data/4.in
echo *********6********* >> %fileName%
java Main < ./data/6.in
echo *********8********* >> %fileName%
java Main < ./data/8.in
echo *********10********* >> %fileName%
java Main < ./data/10.in
echo *********12********* >> %fileName%
java Main < ./data/12.in
echo *********24********* >> %fileName%
java Main < ./data/24.in
echo *********total********* >> %fileName%



set end_time=!time!
for /f "tokens=1-3 delims=:.," %%a in ("%start_time%") do set /a "start_seconds=(((%%a*60)+1%%b)*60)+1%%c-100"
for /f "tokens=1-3 delims=:.," %%a in ("%end_time%") do set /a "end_seconds=(((%%a*60)+1%%b)*60)+1%%c-100"
set /a "total_seconds=end_seconds-start_seconds"
echo totalTime:%total_seconds% >> %fileName%


set count=0
set /a sum=0

for /f "tokens=2 delims=:" %%a in ('findstr /c:"totalAddSideCost:" result.txt') do (
	set /a count+=1
)
echo %count%
set /a start_line=%count%-7
set /a end_line=%count%
echo %start_line%

echo %end_line%
for /f "skip=%start_line% tokens=2 delims=:" %%a in ('findstr /c:"totalAddSideCost:" result.txt') do (
    set /a sum+=%%a
    set /a lines+=1
    if !lines! equ %end_line% goto :exit
)

:exit
echo total:%sum% >> %fileName%

pause
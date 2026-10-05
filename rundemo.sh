#!/bin/sh
cd C:/mod/work/b43_0807
powershell -NoProfile -ExecutionPolicy Bypass -Command "Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue; & C:/mod/work/b43_0807/kits/minecraft/play.ps1 -Mod C:/mod/work/b43_0807/mod -Demo"
i=0
while [ $i -lt 150 ]; do
  if grep -q SIGF_READY .mc/run/logs/latest.log 2>/dev/null; then break; fi
  if grep -q "SIGF_ERROR\|Crash Report" .mc/run/logs/latest.log 2>/dev/null; then echo ERROR; grep -m3 -A8 "SIGF_ERROR\|Crash Report" .mc/run/logs/latest.log; exit 1; fi
  sleep 2; i=$((i+1))
done
sleep 4
printf 1 > C:/mod/work/mc-rec.txt
echo started

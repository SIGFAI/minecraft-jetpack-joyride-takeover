#!/bin/sh
# usage: waitlog.sh pattern [timeoutsec]
i=0; t=${2:-120}
while [ $i -lt $t ]; do grep -q "$1" C:/mod/work/b43_0807/.mc/run/logs/latest.log && exit 0; sleep 1; i=$((i+1)); done; exit 1

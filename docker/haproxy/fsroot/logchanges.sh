#!/bin/bash
# log the contents of a file every time it changes
F=$1
echo "Will report changes in file $F"
while true
do
  inotifywait -e close_write $F
  echo "***** $F changed *****"
  sleep 2
  cat $F
  echo "************************"
done &

#!/bin/bash

x=`netstat -a -p tcp | grep :.*:4000\ *ESTABLISHED | wc -l`
x=$(( x + 2 ))

set -e -x

make nei_test.class
java nei_test L$x

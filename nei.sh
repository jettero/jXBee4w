#!/bin/bash

x=`ps auxfw | grep nei_test | wc -l`
x=$(( x + 1 ))

set -e -x

make nei_test.class
java nei_test L$x

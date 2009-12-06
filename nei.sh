#!/bin/bash

set -e -x

x=`ps auxfw | grep nei_test | wc -l`
x=$(( x + 1 ))

make nei_test.class
java nei_test L$x

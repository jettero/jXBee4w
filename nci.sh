#!/bin/bash

set -e -x

make nci_test.class
java nci_test

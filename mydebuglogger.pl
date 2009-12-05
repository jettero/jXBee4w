#!/usr/bin/perl

use strict;

$| = 1;

open my $out, ">", "debug.log" or die $!;

while(<>) {
    print $out $_;
    print $_ unless m/\[debug\]/;
}

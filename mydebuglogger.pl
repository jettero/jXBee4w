#!/usr/bin/perl

use strict;

$| = 1;

open my $out, ">", "run.log" or die $!;

while(<>) {
    my $time = localtime;

    s/[\r\n]$//g;

    print $out "$time: $_\n";
    print "$_\n" unless m/\[debug\]/;
}

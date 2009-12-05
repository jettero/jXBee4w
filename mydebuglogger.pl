#!/usr/bin/perl

use strict;
use POSIX;

$| = 1;

open my $out, ">", "run.log" or die $!;

while(<>) {
    my $time = strftime('%M:%S', localtime);

    s/00:13:a2:00:40/xbee/g; # all these MACs are the same

    s/[\r\n]$//g;
    my $debug = s/\[debug\] //;

    print $out "$time: $_\n";
    print "$_\n" unless $debug;
}

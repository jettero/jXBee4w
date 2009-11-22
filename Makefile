SHELL=/bin/bash

default_test=handle_test
lastfile=/tmp/.lastjxbee4w

#default: XBeeHandle.class

run_last_test:
	@-if [ -f $(lastfile) ]; then make `cat $(lastfile)`; else make $(default_test); fi

%_test:
	@echo $@ > $(lastfile)
	@make --no-print-directory $@.class
	@echo " >> RUNNING $@ << "
	@java $@; chmod 644 *.txt *.pkt

config_test: config_test.class
	@echo $@ > $(lastfile)
	@echo " >> RUNNING $@ << "
	@for i in `cat /tmp/p1`; do java $@ $$i; done

show show_packets: run_last_test
	(for i in packet-*.pkt; do echo -n $$i; xxd $$i; done) | less -ES

clean:
	git clean -dfx

packets:                    XBeeRxPacket.class XBeeTxPacket.class XBeeATResponsePacket.class XBeeATCommandPacket.class 
XBeeTxPacket.class:         XBeeRadio64Packet.class
XBeeRxPacket.class:         XBeeRadio64Packet.class
XBeeATCommandPacket.class:  XBeeATPacket.class
XBeeATResponsePacket.class: XBeeATPacket.class
XBeeATPacket.class:         XBeePacket.class
XBeeRadio64Packet.class:    XBeePacket.class

Address64.class:      Address64Exception.class
XBeePacketizer.class: packets
XBeePacket.class:     Address64.class PayloadException.class
XBeeConfig.class:     XBeeConfigException.class
XBeeHandle.class:     packets PacketRecvEvent.class

address_test.class:    Address64.class
packetizer_test.class: XBeePacketizer.class
checksum_test.class:   XBeePacket.class
config_test.class:     XBeeConfig.class
handle_test.class:     XBeeHandle.class XBeePacket.class
at_cmd_test.class:     XBeePacketizer.class

%.class: %.java RXTXcomm.jar
	javac -cp '.;RXTXcomm.jar' $<
	@chmod 644 *.class

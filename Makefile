SHELL=/bin/bash

default_test=handle_test
lastfile=/tmp/.lastjxbee4w

#default: XBeeHandle.class

run_last_test:
	@-if [ -f $(lastfile) ]; then make --no-print-directory `cat $(lastfile)`; else make --no-print-directory $(default_test); fi

%_test:
	@echo $@ > $(lastfile)
	@make --no-print-directory $@.class
	@-rm -f *.pkg *.dat
	@echo " >> RUNNING $@ << "
	@java $@; chmod 644 *.txt *.pkt *.dat &>/dev/null || /bin/true

config_test: config_test.class
	@echo $@ > $(lastfile)
	@-rm -f *.pkg *.dat
	@echo " >> RUNNING $@ << "
	@for i in `cat /tmp/p1`; do java $@ $$i; done
	@chmod 644 *.txt *.pkt *.dat &>/dev/null || /bin/true

show show_packets:
	(for i in `ls -rt1 *.pkt`; do echo -n $$i; xxd -c 32 "$$i"; done) | less -ES

clean:
	@ echo cleaning in 2 seconds; sleep 2
	git clean -dfx

XBeeTxPacket.class:         XBeePacket.class
XBeeRxPacket.class:         XBeePacket.class
XBeeATCommandPacket.class:  XBeeATPacket.class
XBeeATResponsePacket.class: XBeeATPacket.class
XBeeATPacket.class:         XBeePacket.class
XBeeTxStatusPacket.class:   XBeePacket.class

Address64.class:      Address64Exception.class
XBeePacketizer.class: Message.class XBeeRxPacket.class XBeeTxPacket.class XBeeATResponsePacket.class XBeeATCommandPacket.class XBeeTxStatusPacket.class
XBeePacket.class:     Address64.class PayloadException.class
XBeeConfig.class:     XBeeConfigException.class XBeePacketizer.class TestENV.class
XBeeHandle.class:     XBeePacketizer.class PacketRecvEvent.class TestENV.class

ACKQueue.class: XBeePacket.class
Message.class:  PayloadException.class

NetworkEndpointHandle.class:  XBeeHandle.class XBeeConfig.class MessageRecvEvent.class ACKQueue.class

address_test.class:     Address64.class
packetizer_test.class:  XBeePacketizer.class
checksum_test.class:    XBeePacket.class
config_test.class:      XBeeConfig.class
handle_test.class:      XBeeHandle.class XBeePacket.class
at_cmd_test.class:      XBeePacketizer.class
modem2modem_test.class: NetworkEndpointHandle.class
neh_test.class:         NetworkEndpointHandle.class
ackq_test.class:        ACKQueue.class
fragment_test.class:    Message.class
hashmap_test.class:     Address64.class

%.class: %.java RXTXcomm.jar
	javac -Xlint:unchecked -cp '.;RXTXcomm.jar' $<
	@chmod 644 *.class

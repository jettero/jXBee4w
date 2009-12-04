SHELL=/bin/bash

default_test=handle_test
lastfile=/tmp/.lastjxbee4w

ifeq ($(shell uname -s),Linux)
    CLASSPATH='.:RXTXcomm.jar'
else
    CLASSPATH='.;RXTXcomm.jar'
endif

run_last_test:
	@-if [ -f $(lastfile) ]; then make --no-print-directory `cat $(lastfile)`; else make --no-print-directory $(default_test); fi

%_test:
	@echo $@ > $(lastfile)
	@make --no-print-directory $@.class
	@-rm -f *.pkt *.dat
	@echo " >> RUNNING $@ << "
	@java $@; chmod 644 *.txt *.pkt *.dat &>/dev/null || /bin/true

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

XBeeDispatcher.class:  XBeeHandle.class XBeeConfig.class MessageRecvEvent.class RawRecvEvent.class ACKQueue.class \
    PacketQueueWriter.class PacketQueueWriterDestinationMap.class


address_test.class:     Address64.class
packetizer_test.class:  XBeePacketizer.class
checksum_test.class:    XBeePacket.class
config_test.class:      XBeeConfig.class
handle_test.class:      XBeeHandle.class XBeePacket.class
at_cmd_test.class:      XBeePacketizer.class
modem2modem_test.class: XBeeDispatcher.class
randm2m_test.class:     XBeeDispatcher.class
dispatch_test.class:    XBeeDispatcher.class
ackq_test.class:        ACKQueue.class
fragment_test.class:    Message.class
hashmap_test.class:     Address64.class

%.class: %.java RXTXcomm.jar
	javac -Xlint:unchecked -cp $(CLASSPATH) $<
	@chmod 644 *.class

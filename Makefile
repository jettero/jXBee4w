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
	@java $@; chmod 644 *.txt

config_test: config_test.class
	@echo $@ > $(lastfile)
	@echo " >> RUNNING $@ << "
	@for i in `cat /tmp/p1`; do java $@ $$i; done

clean:
	git clean -dfx

Address64.class:      Address64Exception.class
XBeePacketizer.class: XBeePacket.class
XBeePacket.class:     Address64.class PayloadException.class
XBeeConfig.class:     XBeeConfigException.class
XBeeHandle.class:     XBeePacket.class PacketRecvEvent.class

address_test.class:    Address64.class
packetizer_test.class: XBeePacketizer.class
checksum_test.class:   XBeePacket.class
config_test.class:     XBeeConfig.class
handle_test.class:     XBeeHandle.class XBeePacket.class
at_cmd_test.class:     XBeePacketizer.class

%.class: %.java RXTXcomm.jar
	javac -cp '.;RXTXcomm.jar' $<
	@chmod 644 *.class

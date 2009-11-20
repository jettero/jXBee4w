SHELL=/bin/bash

default_test=handle_test

#default: XBeeHandle.class

run_last_test:
	@-if [ -f .last ]; then make `cat .last`; else make $(default_test); fi

%_test:
	@echo $@ > .last
	@make --no-print-directory $@.class
	@echo " >> RUNNING $@ << "
	@java $@

config_test: config_test.class
	@echo $@ > .last
	@echo " >> RUNNING $@ << "
	@for i in `cat /tmp/p1`; do java $@ $$i; done

clean:
	git clean -dfx

Address64.class:      Address64Exception.class
XBeePacketizer.class: XBeePacket.class
XBeePacket.class:     Address64.class PayloadException.class FrameException.class
XBeeConfig.class:     XBeeConfigException.class
XBeeHandle.class:     XBeePacket.class PacketRecvEvent.class

address_test.class:    Address64.class
packetizer_test.class: XBeePacketizer.class
checksum_test.class:   XBeePacket.class
config_test.class:     XBeeConfig.class
handle_test.class:     XBeeHandle.class XBeePacket.class

%.class: %.java RXTXcomm.jar
	javac -cp '.;RXTXcomm.jar' $<
	@chmod 644 *.class

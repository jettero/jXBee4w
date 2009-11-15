SHELL=/bin/bash

default_test=packetizer_test

run_last_test:
	@-if [ -f .last ]; then make `cat .last`; else make $(default_test); fi

%_test: %_test.class
	@echo $@ > .last; echo " >> RUNNING $@ << "; java $@

clean:
	git clean -dfx

Address64.class:      Address64Exception.class
XBeePacketizer.class: XBeePacket.class
XBeePacket.class:     Address64.class PayloadException.class FrameException.class

address_test.class:    Address64.class
packetizer_test.class: XBeePacketizer.class

%.class: %.java
	javac $<
	@chmod 644 $@

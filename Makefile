SHELL=/bin/bash

default_test=packetizer_test

run_last_test:
	@-if [ -f .last ]; then make `cat .last`; else make $(default_test); fi

%_test: %_test.class
	@echo $@ > .last; echo " >> RUNNING $@ << "; java $@

clean:
	git clean -dfx

address_test.class: Address64.class Address64Exception.class

packetizer_test.class: Address64.class Address64Exception.class PayloadException.class XBeePacket.class XBeePacketizer.class

%.class: %.java
	javac $<
	@chmod 644 $@

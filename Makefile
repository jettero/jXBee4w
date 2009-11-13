
default: run_test

#quotes Quoter: Quoter.class
#	java Quoter SYK GOOG JPM

run_%: %.class
	x=`echo $@ | sed s/^run_//`; java $$x

clean:
	git clean -dfx

test.class: XBeePacketizer.class XBeePacket.class PayloadException.class

%.class: %.java
	javac $<
	@chmod 644 $@

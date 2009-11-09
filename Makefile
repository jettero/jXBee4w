
default: run_enumerate

#quotes Quoter: Quoter.class
#	java Quoter SYK GOOG JPM

run_%: %.class
	x=`echo $@ | sed s/^run_//`; java $$x

clean:
	git clean -dfx

%.class: %.java
	javac $<

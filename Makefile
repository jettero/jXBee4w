
default: run_test

#quotes Quoter: Quoter.class
#	java Quoter SYK GOOG JPM

run_%: %.class
	x=`echo $@ | sed s/^run_//`; java $$x

clean:
	git clean -dfx

%.class: %.java
	@rm *.class  # until I can figure out a better way to do the dependencies
	javac $<
	@chmod 644 $@

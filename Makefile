

#quotes Quoter: Quoter.class
#	java Quoter SYK GOOG JPM

clean:
	git clean -dfx

%.class: %.java
	javac $<

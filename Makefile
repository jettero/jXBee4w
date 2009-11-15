SHELL=/bin/bash

default_test=address

run_last_test:
	@-if [ -f .last ]; then make run_`cat .last`; else make run_$(default_test)_test; fi

run_%: %.class
	x=`echo $@ | sed s/^run_//`; echo $$x > .last; java $$x

clean:
	git clean -dfx

%.class: %.java
	@-rm -f *.class  # until I can figure out a better way to do the dependencies
	javac $<
	@chmod 644 $@

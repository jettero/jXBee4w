SHELL=/bin/bash

default_test=address

run_last_test:
	@-if [ -f .last ]; then make run_`cat .last`; else make run_$(default_test)_test; fi

run_%: %.class
	@x=`echo $@ | sed s/^run_//`; echo $$x > .last; echo " >> RUNNING $$x << "; java $$x

clean:
	git clean -dfx

address_test.class: Address64.class

%.class: %.java
	javac $<
	@chmod 644 $@

SHELL := /bin/bash

.PHONY: test wave clean

test:
	bash run_course_compat_tests.sh

wave:
	TRACE=1 bash run_course_compat_tests.sh

clean:
	rm -rf ../../build/course_compat

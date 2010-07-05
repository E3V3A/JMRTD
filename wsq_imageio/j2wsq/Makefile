# On Windows (in VC Command prompt) type nmake, on Unix type make...
# I'm assuming make exists iff nmake doesn't.

all:
	-nmake -f Makefile.win32
	-make -f Makefile.linux

install:
	-nmake -f Makefile.win32 install
	-make -f Makefile.linux install

clean:
	-nmake -f Makefile.win32 clean
	-make -f Makefile.linux clean

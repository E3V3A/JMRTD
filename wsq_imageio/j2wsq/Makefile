# On Windows (in VC Command prompt) type nmake, on Unix type make...
# I'm assuming make exists iff nmake doesn't.

all:
	-nmake -f Makefile.win32
	-make -f Makefile.linux

clean:
	-nmake -f Makefile.win32 clean
	-make -f Makefile.linux clean

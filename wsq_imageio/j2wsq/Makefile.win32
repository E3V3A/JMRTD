JAVA_INCLUDE="c:\Program Files\Java\jdk1.6.0_15\include"
TARGET=win32
CFILES=allocfet.c computil.c dataio.c delfet.c extrfet.c fatalerr.c freefet.c huff.c lkupfet.c nistcom.c strfet.c syserr.c updatfet.c wsq_decoder.c wsq_encoder.c wsq_globals.c wsq_huff.c wsq_ppi.c wsq_tableio.c wsq_tree.c wsq_util.c

j2wsq.dll: objects
	cl /O1 /LD /Zi j2wsq.c *.obj /Fej2wsq.dll /I. /I$(JAVA_INCLUDE)\win32 /I$(JAVA_INCLUDE) /link /nodefaultlib:libcmt msvcrt.lib
	

objects:
	cl /W3 /O1 /c /I. $(CFILES)


clean:
	del *.dll *.obj
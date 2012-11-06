apifiles = src/sos/*/*.java 
sample = src/sos/mrtd/sample/*.java
jdklibs= /usr/lib/jvm/jdk1.6.0_06/lib
eclipselibs= /usr/local/eclipse/plugins/com.ibm.bluez.jcop.eclipse_3.1.2/lib
jars = $(jdklibs)/bcprov-jdk16-139.jar:$(jdklibs)/apduio.jar:$(eclipselibs)/offcard.jar
sos= ../sosshared

main_memory_use =       -ms256M -mx256M
bin:	bin.stamp	
bin.stamp: $(apifiles) $(sample)
	mkdir -p bin
	javac -classpath src:$(sos)/src:$(jars) -d bin -source 1.6 $(apifiles) $(sample)
	touch bin.stamp

release:
	rm -rf /tmp/passporthostapi;\
	   mkdir -p /tmp/passporthostapi;\
	   svn export src /tmp/passporthostapi/src;\
	   cp LICENSE /tmp/passporthostapi;\
	   cp README /tmp/passporthostapi;\
	   cd bin; jar -mcf ../MANIFEST.MF /tmp/passporthostapi/jmrtd.jar *; cd ..;\
	   cd /tmp; zip -r passporthostapi.zip passporthostapi

run: bin
	java $(main_memory_use) -classpath bin:$(jars) sos.mrtd.sample.PassportGUI demo

doc:
	mkdir -p apidocs
	javadoc -d apidocs -author -version\
	   -sourcepath src:../sosshared/src\
	   -classpath .:$(jars)\
	   -windowtitle "JMRTD API"\
	   -doctitle "JMRTD API"\
	   -link http://java.sun.com/javase/6/docs/api/\
	   -link http://www.bouncycastle.org/docs/docs1.6\
	   -link http://java.sun.com/products/javacard/htmldoc\
	   sos.mrtd

# sos.util sos.gui sos.smartcards sos.mrtd sos.mrtd.sample

clean: 
	rm -rf bin bin.stamp apidocs

crun:	clean run
cbin:	clean bin


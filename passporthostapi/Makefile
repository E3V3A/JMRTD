apifiles = src/sos/*/*.java 
sample = src/sos/mrtd/sample/*.java
jars = src/lib/bcprov-jdk14-132.jar:src/lib/apduio.jar:src/lib/offcard.jar:src/lib/jpcsc.jar
main_memory_use =       -ms256M -mx256M
bin:	bin.stamp	
bin.stamp: $(apifiles) $(sample)
	mkdir -p bin
	javac -classpath bin:$(jars) -d bin -source 1.5 $(apifiles) $(sample)
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
	   -sourcepath src\
	   -classpath .:$(HOME)/java/java_card_kit-2_2_2/lib/apduio.jar:/usr/local/eclipse/plugins/com.ibm.bluez.jcop.eclipse_3.1.1.b/lib/offcard.jar\
	   -windowtitle "JMRTD API"\
	   -doctitle "JMRTD API"\
	   -link http://java.sun.com/j2se/1.5/docs/api\
	   -link http://www.bouncycastle.org/docs/docs1.5\
	   -link http://java.sun.com/products/javacard/htmldoc\
	   sos.smartcards sos.mrtd

# sos.util sos.gui sos.smartcards sos.mrtd sos.mrtd.sample

clean: 
	rm -rf bin bin.stamp apidocs

crun:	clean run
cbin:	clean bin


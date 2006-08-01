javafiles = sos/mrtd/*.java
jars = lib/bcprov-jdk14-132.jar:lib/apduio.jar:lib/offcard.jar
main_memory_use =       -ms256M -mx256M
build:	build.stamp	
build.stamp: $(javafiles)
	mkdir -p build
	javac -classpath build:$(jars) -d build -source 1.4 sos/mrtd/*.java
	touch build.stamp

run:	build
	java $(main_memory_use) -classpath build:$(jars) sos.mrtd.PassportGUI

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
	rm -rf build build.stamp apidocs

crun:	clean run
cbuild:	clean build


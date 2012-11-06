APPLET_PACKAGE=sos.passportapplet
APPLET_CLASS=PassportApplet
PACKAGE_AID=0xA0:0x00:0x00:0x02:0x47:0x10
APPLET_AID=0xA0:0x00:0x00:0x02:0x47:0x10:0x01
APPLET_AID_LENGTH=7

#JAVA_HOME=/usr/local/java/jdk
JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAVADOC=$(JAVA_HOME)/bin/javadoc

# java card kit must be version 2.2.1 for gpshell
#JCPATH=/usr/local/java/java_card_kit-2_2_2
JCPATH=/usr/local/java/java_card_kit-2_2_1
# JCPATH=$(HOME)/java/java_card_kit-2_1_2
# JCPATH=$(HOME)/java/java_card_kit-2_2_1

JCAPI=$(JCPATH)/lib/api.jar
# JCAPI=$(JCPATH)/lib/api.jar
JCCONV=$(JCPATH)/lib/converter.jar
JCVERIFIER=$(JCPATH)/lib/offcardverifier.jar
JCEXPORTPATH=$(JCPATH)/api_export_files
# EXPORTPATH=$(JCPATH)/api_export_files

# Global Platform API
GPAPI=$(JAVA_HOME)/lib/gp211.jar
GPEXPORTPATH=$(JAVA_HOME)/lib/api_export_files

SCRIPTGEN=$(JCPATH)/bin/scriptgen
APDUTOOL=$(JCPATH)/bin/apdutool
CREF=$(JCPATH)/bin/cref

# Apdutool...
APDUTOOL_POWERUP="powerup;"
APDUTOOL_SELECT_INSTALLER="0x00 0xA4 0x04 0x00 0x09 0xA0 0x00 0x00 0x00 0x62 0x03 0x01 0x08 0x01 0x7F;"
APDUTOOL_SCRIPT=`$(SCRIPTGEN) applet.cap | grep '0x'`
APDUTOOL_APPLET_AID=`echo "$(APPLET_AID)" | tr ':' ' '`
APDUTOOL_INSTALL="0x80 0xB8 0x00 0x00 0x0`expr $(APPLET_AID_LENGTH) + 2` 0x0$(APPLET_AID_LENGTH) $(APDUTOOL_APPLET_AID) 0x00 0x7F;"
APDUTOOL_POWERDOWN="powerdown;"

# GPShell...
GPSHELL= "gpshell"
GPSHELL_SCRIPT="applet.gpsh"

# Can't get this to work?!?! :(
# P1=`echo "$(APPLET_AID)" | tr ':' '\n' | wc -l | tr -d [:space:]`
# P2=`expr $(P1) + 2`

all: applet.cap

classes: build
	$(JAVAC)\
	   -g\
	   -classpath $(JCAPI):$(GPAPI):.\
	   -d build\
	   -sourcepath src \
	   -source 1.2 \
	   -target 1.2 \
	   src/sos/passportapplet/PassportApplet.java

doc:	api
	$(JAVADOC)\
	   -sourcepath .\
	   -classpath $(JCAPI):.\
	   -author\
	   -version\
	   -d doc/api\
	   $(APPLET_PACKAGE)

applet.cap: classes
	$(JAVA)\
	   -classpath $(JCCONV):$(JCVERIFIER)\
	   com.sun.javacard.converter.Converter\
	      -classdir build\
	      -exportpath $(JCEXPORTPATH):$(GPEXPORTPATH)\
	      -applet $(APPLET_AID) $(APPLET_CLASS)\
	      -d build\
	      -out CAP\
	      -v\
              -debug \
	      $(APPLET_PACKAGE) $(PACKAGE_AID) 1.0
	cp build/sos/passportapplet/javacard/passportapplet.cap applet.cap

applet.scr: applet.cap
	rm -f applet.scr
	echo $(APDUTOOL_POWERUP)\
	     $(APDUTOOL_SELECT_INSTALLER)\
	     $(APDUTOOL_SCRIPT)\
	     $(APDUTOOL_INSTALL)\
	     $(APDUTOOL_POWERDOWN)\
	     | sed -e 's/; */;\n/g' > applet.scr

applet.ee: applet.scr
	$(CREF) -o applet.ee &
	$(APDUTOOL) applet.scr

build:
	mkdir -p build

install:
	$(GPSHELL) $(GPSHELL_SCRIPT)

api:
	mkdir -p doc/api

clean:
	rm -f applet.cap applet.scr applet.ee
	rm -rf build


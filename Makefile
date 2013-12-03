
all: larray

include Makefile.common


SRC:=src/main/scala

LARRAY_OUT:=$(TARGET)/larray-$(os_arch)
LARRAY_SRC_DIR:=$(SRC)/xerial/larray/impl
LARRAY_SRC:=$(shell find $(LARRAY_SRC_DIR))
LARRAY_OBJ:=$(LARRAY_OUT)/LArrayNative.o

VERSION:=$(shell perl -npe "s/version in ThisBuild\s+:=\s+\"(.*)\"/\1/" version.sbt | sed -e "/^$$/d")



CFLAGS:=$(CFLAGS) -I$(LARRAY_SRC_DIR)

LARRAY_HEADER:=$(SRC)/xerial/larray/impl/LArrayNative.h

$(TARGET)/classes/xerial/larray/impl/%.class : $(LARRAY_SRC_DIR)/%.java
	$(JAVAC) -sourcepath $(SRC) -d $(TARGET)/classes $<

jni-header: $(LARRAY_HEADER)

$(LARRAY_HEADER): $(SRC)/xerial/larray/impl/LArrayNative.java  $(TARGET)/classes/xerial/larray/impl/LArrayNative.class
	@mkdir -p $(TARGET)/classes
	$(JAVAH) -classpath $(TARGET)/classes -o $@ xerial.larray.impl.LArrayNative

bytecode: src/main/resources/xerial/larray/LArrayNativeLoader.bytecode

src/main/resources/xerial/larray/LArrayNativeLoader.bytecode: src/main/resources/xerial/larray/LArrayNativeLoader.java
	@mkdir -p $(TARGET)/temp
	$(JAVAC) -source 1.5 -target 1.5 -d $(TARGET)/temp $<
	cp $(TARGET)/temp/xerial/larray/LArrayNativeLoader.class $@

VERSION_FILE:=src/main/resources/xerial/larray/VERSION

$(VERSION_FILE):
	echo "version=$(VERSION)" > $@


$(LARRAY_OUT)/%.o : $(LARRAY_SRC_DIR)/%.c 
	@mkdir -p $(@D)
	$(CC) $(CFLAGS) -c $< -o $@ 

$(LARRAY_OUT)/$(LIBNAME): $(LARRAY_OBJ)
	$(CC) $(CFLAGS) -o $@ $+ $(LINKFLAGS) 
	$(STRIP) $@

clean-native: 
	rm -rf $(LARRAY_OUT)

clean:
	rm -rf $(TARGET)

NATIVE_DIR:=src/main/resources/xerial/larray/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/xerial/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

native: osinfo $(NATIVE_DLL) 
larray: native $(VERSION_FILE) $(TARGET)/larray-$(VERSION).jar

SBT:=./sbt

$(NATIVE_DLL): $(LARRAY_OUT)/$(LIBNAME) 
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)

$(TARGET)/larray-$(VERSION).jar: native $(NATIVE_DLL)
	$(SBT) package

test: $(NATIVE_DLL)
	$(SBT) test

win32: 
	$(MAKE) native CROSS_PREFIX=i686-w64-mingw32- OS_NAME=Windows OS_ARCH=x86

# for cross-compilation on Ubuntu, install the g++-mingw-w64-x86-64 package
win64:
	$(MAKE) native CROSS_PREFIX=x86_64-w64-mingw32- OS_NAME=Windows OS_ARCH=amd64

mac32: 
	$(MAKE) native OS_NAME=Mac OS_ARCH=i386

linux32:
	$(MAKE) native OS_NAME=Linux OS_ARCH=i386

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabi package
linux-arm:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabi- OS_NAME=Linux OS_ARCH=arm

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabihf package
linux-armhf:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armhf

clean-native-linux32:
	$(MAKE) clean-native OS_NAME=Linux OS_ARCH=i386

clean-native-win32:
	$(MAKE) clean-native OS_NAME=Windows OS_ARCH=x86


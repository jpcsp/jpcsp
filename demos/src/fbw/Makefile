TARGET = fbw
OBJS = main.o \
    ../common/vram.o ../common/fps.o \
    texdata_jpcsp.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lm

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = FBW Test

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

texdata_jpcsp.o: jpcsp128x64-8888.raw
	bin2o -i jpcsp128x64-8888.raw texdata_jpcsp.o texdata_jpcsp

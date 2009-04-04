TARGET = tbw
OBJS = main.o \
    ../common/vram.o \
    texdata_24.o texdata_32.o texdata_48.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lm

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = TBW Test

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

texdata_24.o: stripes24x8-8888.raw
	bin2o -i stripes24x8-8888.raw texdata_24.o texdata_24

texdata_32.o: stripes32x8-8888.raw
	bin2o -i stripes32x8-8888.raw texdata_32.o texdata_32

texdata_48.o: stripes48x8-8888.raw
	bin2o -i stripes48x8-8888.raw texdata_48.o texdata_48

TARGET = audio_lle
OBJS = main.o DmacManForKernel.o sceDdr.o sceCodec.o sceClockgen.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Audio LLE Test

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


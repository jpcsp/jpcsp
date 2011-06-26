TARGET = JpcspConnector

OBJS = decoder.o reader.o video.o audio.o pmfplayer.o main.o debug.o JpcspConnector.o

INCDIR = ../libmpeg
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =
LIBS = -lpspgu ../libmpeg/libpspmpeg.a -lpspaudiolib -lpspaudio -lpsppower -lpspusb -lpspusbstor -lpspatrac3 -lpspaudio ../libpsputility/libpsputility.a
USE_PSPSDK_LIBC = 1

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Jpcsp Connector

BUILD_PRX = 1
PRX_EXPORTS = exports.exp

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build_prx.mak

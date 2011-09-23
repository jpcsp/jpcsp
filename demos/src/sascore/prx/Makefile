TARGET = sascore
OBJS = main.o sascore.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lpsppower

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = sceSasCore Test

BUILD_PRX = 1
PRX_EXPORTS = exports.exp

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build_prx.mak


TARGET = displayWait
OBJS = main.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lpsppower libdisplay/libpspdisplaymulti.a

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = sceDisplayWaitVblankStartMulti Test

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


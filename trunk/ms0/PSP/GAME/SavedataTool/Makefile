TARGET = savedatatool
OBJS = main.o savedatatool.o pg.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS =

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Savedatatool

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


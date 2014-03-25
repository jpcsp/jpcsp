TARGET = io
OBJS = main.o

INCDIR =
CFLAGS = -O2 -G0 -Wall -fno-tree-loop-optimize -fno-inline-small-functions
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lpsppower

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = IO Tests

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


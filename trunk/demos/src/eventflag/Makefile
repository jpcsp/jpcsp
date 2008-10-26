TARGET = eventflag
OBJS = main.o

INCDIR =
CFLAGS = -g -G0 -Wall -fno-strict-aliasing -fsingle-precision-constant -fomit-frame-pointer -ffast-math
CXXFLAGS = -fno-exceptions -fno-rtti
ASFLAGS =

LIBDIR =
LDFLAGS =
LIBS =

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Event Flag Sample

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

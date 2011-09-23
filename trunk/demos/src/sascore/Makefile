TARGET = sascore
OBJS = main.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspgum -lpspgu -lpsppower -lpsputility libpsputility/libpsputility.a

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = sceSasCore Test

BUILD_PRX = 1
PSP_FW_VERSION=371
PSP_LARGE_MEMORY=1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


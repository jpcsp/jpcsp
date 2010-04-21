TARGET = JpcspConnector

OBJS = main.o

INCDIR =
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =
LIBS = -lpsputility ../libpsputility/libpsputility.a ../prx/debug.o

#To build for custom firmware:
BUILD_PRX = 1
PSP_FW_VERSION=371
PSP_LARGE_MEMORY=1

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Jpcsp Connector 3xx

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

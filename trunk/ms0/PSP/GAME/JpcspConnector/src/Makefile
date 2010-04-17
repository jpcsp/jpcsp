TARGET = JpcspConnector

OBJS = main.o

INCDIR = 
CFLAGS = -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =
LIBS = 
USE_PSPSDK_LIBC = 1

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Jpcsp Connector

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

TARGET = net
OBJS = main.o

INCDIR =
CFLAGS = -O0 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = 

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Network Test

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


TARGET = wait
OBJS = main.o umd.o mutex.o

INCDIR =
CFLAGS = -g -O2 -G0 -Wall
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

LIBDIR =
LDFLAGS =

LIBS = -lpspumd

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = waitType test

BUILD_PRX = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak


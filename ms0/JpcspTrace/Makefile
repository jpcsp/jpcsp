TARGET = JpcspTrace
OBJS = main.o

INCDIR = ../../../procfw/include
LIBDIR = ../../../procfw/libs

LIBS = -lpspsystemctrl_kernel -lpsprtc

CFLAGS = -Os -G0 -Wall -fno-pic -fno-inline
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

BUILD_PRX = 1
PRX_EXPORTS = exports.exp

LDFLAGS = -mno-crt0 -nostartfiles

USE_KERNEL_LIBC = 1
USE_KERNEL_LIBS = 1

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

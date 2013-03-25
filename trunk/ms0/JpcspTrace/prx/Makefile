TARGET = JpcspTraceUser
OBJS = user.o exports.o ../common.o

all:
	cp JpcspTraceUser.prx ..

LIBS = -lpsprtc -lpsputility

CFLAGS = -Os -G0 -Wall -fno-pic -fno-inline
CXXFLAGS = $(CFLAGS) -fno-exceptions -fno-rtti
ASFLAGS = $(CFLAGS)

BUILD_PRX = 1
PRX_EXPORTS = exports.exp

LDFLAGS = -mno-crt0 -nostartfiles

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

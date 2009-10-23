TARGET = template
OBJS = main.o

INCDIR = 
CFLAGS = -O1 -G0 -Wall -mno-gpopt -fno-strict-aliasing -fsingle-precision-constant -fomit-frame-pointer -ffast-math
CXXFLAGS = -fno-exceptions -fno-rtti
ASFLAGS = 

LIBDIR =
LDFLAGS =

EXTRA_TARGETS = EBOOT.PBP
PSP_EBOOT_TITLE = Fpu Test

PSPSDK=$(shell psp-config --pspsdk-path)
include $(PSPSDK)/lib/build.mak

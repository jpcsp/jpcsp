libdir = @PSPSDK_LIBDIR@

CC = @PSP_CC@
CCAS = $(CC)
AR = @PSP_AR@
RANLIB = @PSP_RANLIB@

INCLUDES = -I$(top_srcdir)/src/base -I$(top_srcdir)/src/kernel
CFLAGS = @PSPSDK_CFLAGS@
CCASFLAGS = $(CFLAGS) -I$(top_srcdir)/src/base -I$(top_srcdir)/src/kernel

DISPLAYMULTI_OBJS = sceDisplayMulti_0000.o sceDisplayMulti_0001.o

libpspdisplaymultiincludedir = @PSPSDK_INCLUDEDIR@
libpspdisplaymultiinclude_HEADERS = pspdisplaymulti.h

lib_LIBRARIES = libpspdisplaymulti.a
libpspdisplaymulti_a_SOURCES = sceDisplayMulti.S
libpspdisplaymulti_a_LIBADD = $(DISPLAYMULTI_OBJS)

$(DISPLAYMULTI_OBJS): sceDisplayMulti.S
        $(COMPILE) -DF_$* $< -c -o $@

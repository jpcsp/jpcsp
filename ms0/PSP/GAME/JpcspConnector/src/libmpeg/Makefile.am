
libdir = @PSPSDK_LIBDIR@

CC = @PSP_CC@
CCAS = $(CC)
AR = @PSP_AR@
RANLIB = @PSP_RANLIB@

INCLUDES = -I$(top_srcdir)/src/base -I$(top_srcdir)/src/kernel
CFLAGS = @PSPSDK_CFLAGS@
CCASFLAGS = $(CFLAGS) -I$(top_srcdir)/src/base -I$(top_srcdir)/src/kernel

MPEG_OBJS = sceMpeg_0000.o sceMpeg_0001.o sceMpeg_0002.o sceMpeg_0003.o sceMpeg_0004.o sceMpeg_0005.o sceMpeg_0006.o sceMpeg_0007.o sceMpeg_0008.o sceMpeg_0009.o sceMpeg_0010.o sceMpeg_0011.o sceMpeg_0012.o sceMpeg_0013.o sceMpeg_0014.o sceMpeg_0015.o sceMpeg_0016.o sceMpeg_0017.o sceMpeg_0018.o sceMpeg_0019.o sceMpeg_0020.o sceMpeg_0021.o sceMpeg_0022.o sceMpeg_0023.o sceMpeg_0024.o sceMpeg_0025.o sceMpeg_0026.o sceMpeg_0027.o sceMpeg_0028.o sceMpeg_0029.o sceMpeg_0030.o sceMpeg_0031.o sceMpeg_0032.o sceMpeg_0033.o sceMpeg_0034.o sceMpeg_0035.o sceMpeg_0036.o sceMpeg_0037.o sceMpeg_0038.o

libpspmpegincludedir = @PSPSDK_INCLUDEDIR@
libpspmpeginclude_HEADERS = pspmpeg.h

lib_LIBRARIES = libpspmpeg.a
libpspmpeg_a_SOURCES = sceMpeg.S
libpspmpeg_a_LIBADD = $(MPEG_OBJS)

$(MPEG_OBJS): sceMpeg.S
	$(COMPILE) -DF_$* $< -c -o $@


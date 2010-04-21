PSPSDK=$(shell psp-config --pspsdk-path)
UTILITY_OBJS = sceUtility.o sceUtility_0000.o sceUtility_0001.o sceUtility_0002.o sceUtility_0003.o sceUtility_0004.o sceUtility_0005.o sceUtility_0006.o sceUtility_0007.o sceUtility_0008.o sceUtility_0009.o sceUtility_0010.o sceUtility_0011.o sceUtility_0012.o sceUtility_0013.o sceUtility_0014.o sceUtility_0015.o sceUtility_0016.o sceUtility_0017.o sceUtility_0018.o sceUtility_0019.o sceUtility_0020.o sceUtility_0021.o sceUtility_0022.o sceUtility_0023.o sceUtility_0024.o sceUtility_0025.o sceUtility_0026.o sceUtility_0027.o sceUtility_0028.o sceUtility_0029.o sceUtility_0030.o sceUtility_0031.o sceUtility_0032.o sceUtility_0033.o sceUtility_0034.o sceUtility_0035.o sceUtility_0036.o sceUtility_0037.o sceUtility_0038.o

%.o: sceUtility.S
	psp-gcc -g -O2 -G0 -Wall -I${PSPSDK}/include -DF_$* $< -c -o $@

all: $(UTILITY_OBJS)
	psp-ar cru libpsputility.a $(UTILITY_OBJS)
	psp-ranlib libpsputility.a

clean:
	rm -f *.o
	rm -f *.a

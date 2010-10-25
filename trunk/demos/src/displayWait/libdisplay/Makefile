PSPSDK=$(shell psp-config --pspsdk-path)
DISPLAYMULTI_OBJS = sceDisplayMulti.o sceDisplayMulti_0000.o sceDisplayMulti_0001.o

%.o: sceDisplayMulti.S
	psp-gcc -g -O2 -G0 -Wall -I${PSPSDK}/include -DF_$* $< -c -o $@

all: $(DISPLAYMULTI_OBJS)
	psp-ar cru libpspdisplaymulti.a $(DISPLAYMULTI_OBJS)
	psp-ranlib libpspdisplaymulti.a

clean:
	rm -f *.o
	rm -f *.a


#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

/* Define printf, just to make typing easier */
#define printf	pspDebugScreenPrintf

void printMem();

void fpl_test();
void vpl_test();

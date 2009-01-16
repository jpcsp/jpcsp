#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
//#include <psputility.h>
//#include <psppower.h>

#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

PSP_MODULE_INFO("SavedataTool", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

extern int xmain(int ra);

int main(int argc, char *argv[])
{
	return xmain(0);
}


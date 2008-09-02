#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspdebug.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#include <pspctrl.h>
#include <pspgu.h>
#include <psprtc.h>



int exit_callback(int arg1, int arg2, void *common) {
          sceKernelExitGame();
          return 0;
}
// Callback thread 
int CallbackThread(SceSize args, void *argp) {
          int cbid;
 
          cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
          sceKernelRegisterExitCallback(cbid);
 
          sceKernelSleepThreadCB();
 
          return 0;
}
// Sets up the callback thread and returns its thread id
int SetupCallbacks(void) {
          int thid = 0;
 
          thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, 0, 0);
          if(thid >= 0) {
                    sceKernelStartThread(thid, 0, 0);
          }
 
          return thid;
}

PSP_MODULE_INFO("rtctest", 0, 1, 1);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

void *fbp0;		// frame buffer
 
int fps = 0;		// for calculating the frames per second
char fpsDisplay[100];
u32 tickResolution;
u64 fpsTickNow;
u64 fpsTickLast;

int main(int argc, char **argv)
{
	pspDebugScreenInit();
	SetupCallbacks();
      sceRtcGetCurrentTick( &fpsTickLast );
	tickResolution = sceRtcGetTickResolution();
      fbp0  = 0;
      while( 1 )
	{
		FPS();
		//sceDisplayWaitVblankStart();
		fbp0 = sceGuSwapBuffers();
        }
	sceKernelExitGame();	// Quits Application
	return 0;
}

void FPS( void )
{
	fps++;
	sceRtcGetCurrentTick( &fpsTickNow );
	
	if( ((fpsTickNow - fpsTickLast)/((float)tickResolution)) >= 1.0f )
	{
		fpsTickLast = fpsTickNow;
		sprintf( fpsDisplay, "FPS: %d", fps );
		fps = 0;
	}
	pspDebugScreenSetOffset( (int)fbp0 );
	pspDebugScreenSetXY( 0, 0 );
	pspDebugScreenPrintf( fpsDisplay );
 
}

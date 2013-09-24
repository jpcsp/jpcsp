/*
 * PSP Controller test.
 *
 * Controller sensitivity,
 * holding,
 * tapping keys.
 *
 * PSP's controller latch status functions used.
 * vblanks used as delays.
 *
 * For greater precision of time delays
 * just rewrite the fdelay() function with
 * the timer routine preferred.
 *
 * The above may come in handy when if the frame
 * rate drops bellow 60 at which point the vblank
 * timer becomes inadequate (imprecise).
 *
 * ********************************************************* *
 * By Yuriy Y. Yermilov aka (binaryONE) cyclone.yyy@gmail.com
 *
 * binary001.blogspot.com
 * www.myspace.com/binaryone
 * www.devrc.org (private)
 *
 * THIS SOFTWARE IS PROVIDED BY THE OWNER ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OWNER
 * PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ********************************************************** */

#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <stdlib.h>
#include <string.h>

PSP_MODULE_INFO("PSP Controller test.", 0, 1, 1);

#define printf pspDebugScreenPrintf

/* Exit callback */
int exit_callback(int arg1, int arg2, void *common) {
          sceKernelExitGame();
          return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp) {
          int cbid;

          cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
          sceKernelRegisterExitCallback(cbid);

          sceKernelSleepThreadCB();

          return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void) {
          int thid = 0;

          thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, 0, 0);
          if(thid >= 0) {
                    sceKernelStartThread(thid, 0, 0);
          }

          return thid;
}

u32
input(void)
{
	static	SceCtrlData oldPad;
		SceCtrlData pad;

	if(sceCtrlPeekBufferPositive(&pad, 1)) {
		if(pad.Buttons != oldPad.Buttons) {
			oldPad = pad;
			return pad.Buttons;
		}
	}
	return 0;
}

static u32 held = 0,
	   iheld = 0,
	   key = 0,
	   dtime = 0,
	   ignore_held_keys = PSP_CTRL_UP|PSP_CTRL_SQUARE,
	   t_ignore = 0,	// set when a key that needs to be 'temporarily' ignored is pressed.
	   t_itime = 0;		// # of micro-seconds to ignore the t_ignore key.

void
fdelay(int _time_)
{
	int x;
	
	for (x = 0; x < _time_; x++)
		sceDisplayWaitVblankStart();
}

int uiMake, uiBreak, uiPress, uiRelease;

void
check_latch(void)
{
	SceCtrlLatch	pad_state;
	SceCtrlData	pad;
	int result;

	sceCtrlPeekBufferPositive(&pad, 1);
	result = sceCtrlReadLatch(&pad_state);
	if (result > 0) {
		if (pad_state.uiMake != uiMake || pad_state.uiBreak != uiBreak || pad_state.uiPress != uiPress || pad_state.uiRelease != uiRelease) {
			uiMake = pad_state.uiMake;
			uiBreak = pad_state.uiBreak;
			uiPress = pad_state.uiPress;
			uiRelease = pad_state.uiRelease;
			printf("Latch: 0x%06X 0x%06X 0x%06X 0x%06X\n", uiMake, uiBreak, uiPress, uiRelease);
		}
	}

	key = pad.Buttons;

	if(pad_state.uiPress) {
		if(held && held < 5) {
			t_ignore = 1;
			t_itime = 20;
		} else
			t_ignore = 0;
		if(key & (ignore_held_keys))
			iheld++;
		else
			iheld = 0;
		held++;
	} else if(pad_state.uiRelease) {
		key = 0;
		t_ignore = 0;
		held = 0;
		iheld = 0;
	}
}

char *
mapkey(u32 key)
{
	if(key >= 0) {
		if(key & PSP_CTRL_SQUARE)
			return "square";
		if(key & PSP_CTRL_UP)
			return "up";
		if(key & PSP_CTRL_CROSS)
			return "cross";
		if(key & PSP_CTRL_TRIANGLE)
			return "triangle";
		if(key & PSP_CTRL_CIRCLE)
			return "CIRCLE";
		if(key & PSP_CTRL_LEFT)
			return "left";
		if(key & PSP_CTRL_RIGHT)
			return "right";
		if(key & PSP_CTRL_DOWN)
			return "down";
		if(key & PSP_CTRL_LTRIGGER)
			return "L_trigger";
		if(key & PSP_CTRL_RTRIGGER)
			return "R_trigger";
	}

	if(t_ignore)
		return "TEMP_IGNORED_KEY";

	return "KEY_NOT_MAPPED_YET";
}

int n_delay = 60, n_delayed;

int
delay(int _time_)
{
	if(!_time_)
		_time_++;
	for(dtime = 0; dtime < _time_; dtime++) {
		check_latch();
		if(t_ignore) {
			t_itime--;
			if(t_itime <= 0) {
				t_ignore = 0;
				held = 0;
			}
		} else if(key)
			break;

		fdelay(1);
	}


	return dtime;
}

void
hold(void)
{
	if(n_delayed >= n_delay) {
		n_delayed = 0;
	}

	n_delayed += delay(n_delay - n_delayed);
}

int
proc_key(void)
{
	char *keyname;

	hold();
	keyname = mapkey(key);

	if(key & PSP_CTRL_TRIANGLE)
		return 0;
	else if(key) {
		if(held == 1) {
			fdelay(3);
			n_delayed += 3;
		} else if(held) {
			fdelay(1);
			n_delayed += 1;
		}
	}

	if(held > 5)
		if(iheld > 1 && (key & (ignore_held_keys)))
			key = 0;

	return 1;
}

int
main() {

	pspDebugScreenInit();
	SetupCallbacks();

	sceCtrlSetSamplingCycle(0);
	sceCtrlSetSamplingMode(0);

	printf("Testing key holding:\n");
	printf("Default IDLE DELAY time is 1 second:\n");
	printf("Default NON-IDLE DELAY time is 1/60th of a second:\n");
	printf("Use triangle key to exit.\n");
	printf("Press and hold/let go of any key.\n");

	while(proc_key())
		if(key)
			printf("[%s] key pressed. held = %d nd = %d dtime = %d\n", mapkey(key), held, n_delayed, dtime);

	sceKernelExitGame();
	return 0;
}

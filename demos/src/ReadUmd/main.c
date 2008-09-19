/*
 * ReadUMD Sample v0.03 by 0okm
 * Thanks moonlight, Humma Kavula, hitchhikr, Ditlew, Fanjita and PSPDEV
 */

#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspumd.h>

#include <string.h>

PSP_MODULE_INFO("ReadUMD", 0, 1, 1);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

int main(int argc, char* argv[])
{
    sceUmdActivate(1, "disc0:");
    sceUmdWaitDriveStat(UMD_WAITFORINIT);

    SceUID fd1;
    SceUID fd2;
    int readSize;
    char filebuf[0x8000];

    pspDebugScreenInit();
    pspDebugScreenPrintf("ReadUMD Sample  v0.03 by 0okm\n");
    pspDebugScreenPrintf("Thanks moonlight, Humma Kavula, hitchhikr, Ditlew, Fanjita and PSPDEV\n");
    pspDebugScreenPrintf("\n");
    pspDebugScreenPrintf(" press [TRIANGLE] - Copy disc0:/UMD_DATA.BIN to ms0:/UMD_DATA.BIN\n");
    pspDebugScreenPrintf(" press [SQUARE] - Copy disc0:/PSP_GAME/ICON0.PNG to ms0:/ICON0.PNG\n");
    pspDebugScreenPrintf(" press [CIRCLE] - Copy disc0:/PSP_GAME/PARAM.SFO to ms0:/PARAM.SFO\n");
    pspDebugScreenPrintf(" press [CROSS] to EXIT\n\n");

    SceCtrlData pad;
    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(0);
    while (1)
    {
        sceCtrlReadBufferPositive(&pad, 1);
        if (pad.Buttons & PSP_CTRL_TRIANGLE)
        {
            fd1 = sceIoOpen("disc0:/UMD_DATA.BIN", PSP_O_RDONLY, 0777);
            fd2 = sceIoOpen("ms0:/UMD_DATA.BIN", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
            while ((readSize = sceIoRead(fd1, filebuf, 0x08000)) > 0)
            {
                sceIoWrite(fd2, filebuf, readSize);
            }
            sceIoClose(fd2);
            sceIoClose(fd1);
        }
        if (pad.Buttons & PSP_CTRL_SQUARE)
        {
            fd1 = sceIoOpen("disc0:/PSP_GAME/ICON0.PNG", PSP_O_RDONLY, 0777);
            fd2 = sceIoOpen("ms0:/ICON0.PNG", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
            while ((readSize = sceIoRead(fd1, filebuf, 0x08000)) > 0)
            {
                sceIoWrite(fd2, filebuf, readSize);
            }
            sceIoClose(fd2);
            sceIoClose(fd1);
        }
        if (pad.Buttons & PSP_CTRL_CIRCLE)
        {
            fd1 = sceIoOpen("disc0:/PSP_GAME/PARAM.SFO", PSP_O_RDONLY, 0777);
            fd2 = sceIoOpen("ms0:/PARAM.SFO", PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
            while ((readSize = sceIoRead(fd1, filebuf, 0x08000)) > 0)
            {
                sceIoWrite(fd2, filebuf, readSize);
            }
            sceIoClose(fd2);
            sceIoClose(fd1);
        }
        if (pad.Buttons & PSP_CTRL_CROSS)
        {
            sceKernelExitGame();
        }
        sceKernelDelayThread(200*1000);
    }

    return 0;
}

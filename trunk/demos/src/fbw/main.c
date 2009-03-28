// basic fbw test

#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspctrl.h>
#include <pspgu.h>
#include <pspgum.h>

#include <string.h>

#include "../common/vram.h"
#include "../common/fps.h"

PSP_MODULE_INFO("FBW Test", 0, 1, 0);
PSP_HEAP_SIZE_KB(15360);

int SetupCallbacks(void);
void sendCommandi(int cmd, int argument);

static unsigned int __attribute__((aligned(16))) displaylist[0x40000];

extern unsigned char texdata_jpcsp_start[];
extern unsigned char texdata_bg_start[];

struct TPVertex
{
    float u, v;
    float x, y, z;
};

static const struct TPVertex __attribute__((aligned(16))) trianglestrip_f_vertices[1*4] =
{
       {    0,     0,    0,     0,   0 }, // Top Left
       { 1.0f,     0, 1.0f,     0,   0 }, // Top Right
       {    0,  1.0f,    0,  1.0f,   0 }, // Bottom Left
       { 1.0f,  1.0f, 1.0f,  1.0f,   0 }, // Bottom Right
};

static const struct TPVertex __attribute__((aligned(16))) sprite_f_vertices[1*2] =
{
       {    0,     0,    0,     0,   0 }, // Top Left
       { 1.0f,  1.0f, 1.0f,  1.0f,   0 }, // Bottom Right
};

int done = 0;

int draw_triangle_strip = 0;
int draw_sprite = 1;

void *fbp0;
void *fbp1;
void *zbp;

void *full_buffer[2]; // fixed
void *depth_buffer; // fixed
int displaybuffer_index; // 0/1 flipped

void *mini_drawbuffer; // fixed
void *full_drawbuffer; // flipped, displaybuffer_index ^ 1
void *displaybuffer; // flipped, displaybuffer_index
#define GET_DRAW_BUFFER() (full_buffer[displaybuffer_index ^ 1])
#define GET_DISPLAY_BUFFER() (full_buffer[displaybuffer_index])
#define SWAP_BUFFERS() do { displaybuffer_index ^= 1; } while(0)

void initDisplay()
{
    full_buffer[0] = getStaticVramBuffer(512, 272, GU_PSM_8888);
    full_buffer[1] = getStaticVramBuffer(512, 272, GU_PSM_8888);
    depth_buffer = getStaticVramBuffer(512, 272, GU_PSM_4444);
    mini_drawbuffer = getStaticVramBuffer(128, 64, GU_PSM_8888);
    displaybuffer_index = 1;

    sceGuInit();

    sceGuStart(GU_DIRECT, displaylist);
    sceGuDrawBuffer(GU_PSM_8888, GET_DRAW_BUFFER(), 512);
    sceGuDispBuffer(480, 272, GET_DISPLAY_BUFFER(), 512);
    sceGuDepthBuffer(depth_buffer, 512);
    sceGuOffset(2048 - (480/2), 2048 - (272/2));
    sceGuViewport(2048, 2048, 480, 272);
    sceGuScissor(0, 0, 480, 272);
    sceGuEnable(GU_SCISSOR_TEST);
    sceGuDepthFunc(GU_GEQUAL);

    sceGuEnable(GU_DEPTH_TEST);
    sceGuFrontFace(GU_CW);
    sceGuShadeModel(GU_SMOOTH);
	sceGuEnable(GU_CULL_FACE);
    sceGuDisable(GU_TEXTURE_2D);
	sceGuEnable(GU_CLIP_PLANES);
    sceGuFinish();
    sceGuSync(0, 0);

    sceDisplayWaitVblankStart();
    sceGuDisplay(GU_TRUE);
}

void renderInfo()
{
    pspDebugScreenSetOffset((int)GET_DRAW_BUFFER());
    pspDebugScreenSetXY(0, 29);

    pspDebugScreenPrintf("Triangle - Exit\n");
    pspDebugScreenPrintf("Square - toggle strips/sprites, sprites=%d\n", draw_sprite);
    pspDebugScreenPrintf("\n");

    pspDebugScreenPrintf("FPS : %02d %d\n", getFps(), getUpf());
}

#define DO_TRANSLATE(xx, yy, zz) \
	{ \
		ScePspFVector3 t = { (xx), (yy), (zz) }; \
		sceGumTranslate(&t); \
	}
#define DO_SCALE(xx, yy, zz) \
	{ \
		ScePspFVector3 s = { (xx), (yy), (zz) }; \
		sceGumScale(&s); \
	}

void draw_tex(int x, int y, int w, int h)
{
    // setup matrices
    sceGumLoadIdentity();
    DO_TRANSLATE(x, y, 0.0f);
    DO_SCALE(w, h, 0.0f);

    // draw texture as 2 triangles in a triangle strip
    if (draw_triangle_strip)
    {
        sceGumDrawArray(GU_TRIANGLE_STRIP,
            GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D,
            4, 0, trianglestrip_f_vertices);
    }

    // draw texture as a sprite
    if (draw_sprite)
    {
        sceGumDrawArray(GU_SPRITES,
            GU_TEXTURE_32BITF|GU_VERTEX_32BITF|GU_TRANSFORM_3D,
            2, 0, sprite_f_vertices);
    }
}

void set_tex(void *texdata)
{
    // setup texture
    sceGuEnable(GU_TEXTURE_2D);

    sceGuDisable(GU_BLEND);
    sceGuTexFunc(GU_TFX_ADD, GU_TCC_RGB);

    sceGuTexMode(GU_PSM_8888, 0, 0, 0);
    sceGuTexImage(0, 128, 64, 128, texdata);
    sceGuTexWrap(GU_CLAMP, GU_CLAMP);
    sceGuTexFilter(GU_NEAREST, GU_NEAREST);
    //sceGuTexFilter(GU_LINEAR, GU_LINEAR);
    //sceGuTexScale(1.0f / 128, 1.0f / 64); // not used in 2D mode(?)
    sceGuTexScale(1.0f, 1.0f); // needed for 3D mode
    sceGuTexOffset(0.0f, 0.0f);
}

int display()
{
    sceGuStart(GU_DIRECT, displaylist);

    // setup matrices
    sceGumMatrixMode(GU_PROJECTION);
    sceGumLoadIdentity();
    sceGumOrtho(0, 480, 272, 0, -1, 1);
    //sceGumOrtho(0, 480, 0, 272, -800.0f, 800.0f);

    sceGumMatrixMode(GU_VIEW);
    sceGumLoadIdentity();

    sceGumMatrixMode(GU_MODEL);
    sceGumLoadIdentity();

    // draw to mini draw buffer
    {
        sendCommandi(156, (  (unsigned int)mini_drawbuffer) & 0x00ffffff);
        sendCommandi(157, ((((unsigned int)mini_drawbuffer) & 0xff000000) >> 8) | 128);
        sceGuScissor(0, 0, 128, 64); // without this it wraps

        //sceGuClearDepth(0);
        //sceGuClear(GU_COLOR_BUFFER_BIT|GU_DEPTH_BUFFER_BIT);

        set_tex(texdata_jpcsp_start);
        draw_tex(0, 0, 128, 64);

#if 0
        // wrap test
        set_tex(texdata_jpcsp_start);
        draw_tex(64, 32, 128, 64);
#endif
    }

    // draw mini buffer to final draw buffer
    {
        sendCommandi(156, (  (unsigned int)GET_DRAW_BUFFER()) & 0x00ffffff);
        sendCommandi(157, ((((unsigned int)GET_DRAW_BUFFER()) & 0xff000000) >> 8) | 512);
        sceGuScissor(0, 0, 480, 272);

        // clear screen
        sceGuClearColor(0xff554433);
        sceGuClearDepth(0);
        sceGuClear(GU_COLOR_BUFFER_BIT|GU_DEPTH_BUFFER_BIT);

        set_tex(0x04000000 + mini_drawbuffer);
        draw_tex(0, 64, 256, 128);
    }

    sceGuFinish();
    sceGuSync(0, 0);

    updateFps();
    renderInfo();

    sceDisplayWaitVblankStartCB();

    // swap final draw buffer -> display buffer
    {
        SWAP_BUFFERS();
        sceDisplaySetFrameBuf(
            (void*)((unsigned int)0x04000000 + (unsigned int)GET_DISPLAY_BUFFER()),
            512,
            GU_PSM_8888,
            PSP_DISPLAY_SETBUF_IMMEDIATE);
    }

    return 0;
}

void eventHandler()
{
    SceCtrlData pad;
    static int oldButtons = 0;

    sceCtrlReadBufferPositive(&pad, 1); // context switch in here
    //sceCtrlPeekBufferPositive(&pad, 1); // no context switch version
    int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

    if (buttonDown & PSP_CTRL_TRIANGLE)
    {
        done = 1;
    }

    if (buttonDown & PSP_CTRL_SQUARE)
    {
        draw_triangle_strip ^= 1;
        draw_sprite ^= 1;
    }

    oldButtons = pad.Buttons;
}

int main(int argc, char* argv[])
{
    pspDebugScreenInit();
    SetupCallbacks();

    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

    initDisplay();
    initFps();

    while(!done)
    {
        eventHandler();
        display();
    }

    sceGuTerm();
    sceKernelExitGame();
    return 0;
}


/* Exit callback */
int exit_callback(int arg1, int arg2, void *common)
{
    done = 1;
    return 0;
}

/* Callback thread */
int CallbackThread(SceSize args, void *argp)
{
    int cbid;

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, 0);
    sceKernelRegisterExitCallback(cbid);

    sceKernelSleepThreadCB();

    return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void)
{
    int thid = 0;

    thid = sceKernelCreateThread("update_thread", CallbackThread,
                     0x11, 0xFA0, 0, 0);
    if(thid >= 0)
    {
        sceKernelStartThread(thid, 0, 0);
    }

    return thid;
}

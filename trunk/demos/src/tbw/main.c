// tbw/tw test

#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspctrl.h>
#include <pspgu.h>
#include <pspgum.h>

#include <string.h>
#include <malloc.h>

#include "../common/vram.h"

PSP_MODULE_INFO("TBW Test", 0, 1, 0);
PSP_HEAP_SIZE_KB(15360);

int SetupCallbacks(void);
void sendCommandi(int cmd, int argument);

static unsigned int __attribute__((aligned(16))) displaylist[0x40000];

extern unsigned char texdata_24_start[];
extern unsigned char texdata_32_start[];
extern unsigned char texdata_48_start[];

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

void *texdata_24;
void *texdata_32;
void *texdata_48;

void initDisplay()
{
    fbp0 = getStaticVramBuffer(512, 272, GU_PSM_8888);
    fbp1 = getStaticVramBuffer(512, 272, GU_PSM_8888);
    zbp = getStaticVramBuffer(512, 272, GU_PSM_4444);

    texdata_24 = memalign(16, (24 * 8 * 4) * 2);
    memset(texdata_24, 0x88, (24 * 8 * 4) * 2);
    memcpy(texdata_24, texdata_24_start, (24 * 8 * 4));

    texdata_32 = memalign(16, (32 * 8 * 4) * 2);
    memset(texdata_32, 0x88, (32 * 8 * 4) * 2);
    memcpy(texdata_32, texdata_32_start, (32 * 8 * 4));

    texdata_48 = memalign(16, (48 * 8 * 4) * 2);
    memset(texdata_48, 0x88, (48 * 8 * 4) * 2);
    memcpy(texdata_48, texdata_48_start, (48 * 8 * 4));

    sceKernelDcacheWritebackAll();

    sceGuInit();

    sceGuStart(GU_DIRECT, displaylist);
    sceGuDrawBuffer(GU_PSM_8888, fbp0, 512);
    sceGuDispBuffer(480, 272, fbp1, 512);
    sceGuDepthBuffer(zbp, 512);
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
    pspDebugScreenSetOffset((int)fbp0);
    pspDebugScreenSetXY(0, 29);

    pspDebugScreenPrintf("Triangle - Exit\n");
    pspDebugScreenPrintf("Square - toggle strips/sprites, sprites=%d\n", draw_sprite);
    pspDebugScreenPrintf("\n");
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

void set_tex(void *texdata, int tw, int th, int tbw)
{
    // setup texture
    sceGuEnable(GU_TEXTURE_2D);

    sceGuDisable(GU_BLEND);
    sceGuTexFunc(GU_TFX_ADD, GU_TCC_RGB);

    sceGuTexMode(GU_PSM_8888, 0, 0, 0);
    sceGuTexImage(0, tw, th, tbw, texdata);
    sceGuTexWrap(GU_CLAMP, GU_CLAMP);
    //sceGuTexFilter(GU_NEAREST, GU_NEAREST);
    sceGuTexFilter(GU_LINEAR, GU_LINEAR); // turn this on so we can see any distortion more easily
    //sceGuTexScale(1.0f / 128, 1.0f / 64); // not used in 2D mode(?)
    sceGuTexScale(1.0f, 1.0f); // needed for 3D mode
    sceGuTexOffset(0.0f, 0.0f);
}


/* notes:
24x8 = 192
32x6 = 192

tw = pow_2_round_down(tw)
for y = 0 to th
    for x = 0 to tw
        read and draw tbp + (y * tbw) + x

I try to calculate actual texture memory: [ tbp, tbp + (th - 1) * tbw + tw )
doesn't seem to work in jpcsp, need to use [ tbp, tbp + th * max(tw, tbw) )
*/
void test_24()
{
    // test as follows:
    // set texture with all combinations of tw/tbw set to 24/32, actual size of texture is 24x8
    // draw as 24x8
    // draw as 32x8

    // tw = tbw, actually tw < tbw
    set_tex(texdata_24, 24, 8, 24); // params are actually 16, 8, 24
    draw_tex(0, 0, 24, 8); // sharp
    draw_tex(32, 0, 32, 8); // sharp

    // tw < tbw, fake tbw
    // corruption starts on the 3rd line, bottom 2 lines missing
    // tw=24 actually converted to tw=16, this is why there is no corruption on the 2nd row
    set_tex(texdata_24, 24, 8, 32); // actually 16, 8, 32
    //sceGuTexScale(32.0f / 24.0f, 1.0f);
    //sceGuTexScale(24.0f / 32.0f, 1.0f);
    draw_tex(0, 16, 24, 8); // blended, because draw width should be 16 not 24
    draw_tex(32, 16, 32, 8); // blended

    // tw = tbw, both tw and tbw fake
    // corruption starts on the 1st line, bottom 2 lines missing
    set_tex(texdata_24, 32, 8, 32);
    draw_tex(0, 32, 24, 8); // sharp
    draw_tex(32, 32, 32, 8); // sharp

#if 1
    // tw > tbw, fake tw
    // corruption on all lines
    // jpcsp r984: exception, Required 1024 remaining bytes in buffer, only had 768
    set_tex(texdata_24, 32, 8, 24);
    draw_tex(0, 48, 24, 8); // blended (compressed)
    draw_tex(32, 48, 32, 8); // sharp

    // tw > tbw, fake tw
    // how to draw a non-pow2 texture with no distortion :)
    set_tex(texdata_24, 32, 8, 24); // width = 32 = pow_2_round_up(24)
    sceGuTexScale(24.0f / 32.0f, 1.0f); // use tex scaling
    draw_tex(0, 64, 24, 8); // sharp
    draw_tex(32, 64, 32, 8); // sharp
#endif
}


void test_32()
{
    // normal - ok
    set_tex(texdata_32, 32, 8, 32);
    draw_tex(128, 0, 32, 8);

    // subset
    // tw rounded down to nearest pow 2 (24 -> 16)
    set_tex(texdata_32, 24, 8, 32);
    draw_tex(96, 16, 24, 8);
    draw_tex(128, 16, 32, 8); // should just be stretched version - ok

    // just testing nearest pow 2
    {
        // 15 -> 8
        set_tex(texdata_32, 15, 8, 32);
        draw_tex(168, 8, 15, 8);
        draw_tex(168, 24, 8, 8);

        // 9 -> 8
        set_tex(texdata_32, 9, 8, 32);
        draw_tex(192, 8, 9, 8);
        draw_tex(192, 24, 8, 8);
    }

    // tw > tbw
    // draw 32 pixels, ok on first row
    // 2nd row starts at tbp+24 so contains some red
    // doesn't look useful to fake tbw
    set_tex(texdata_32, 32, 8, 24);
    draw_tex(96, 32, 24, 8); // should just be stretched version - ok
    draw_tex(128, 32, 32, 8);
}

void display()
{
    sceGuStart(GU_DIRECT, displaylist);

    // clear screen
    sceGuClearColor(0xff554433);
    sceGuClearDepth(0);
    sceGuClear(GU_COLOR_BUFFER_BIT|GU_DEPTH_BUFFER_BIT);

    // setup matrices
    sceGumMatrixMode(GU_PROJECTION);
    sceGumLoadIdentity();
    sceGumOrtho(0, 480, 272, 0, -1, 1);
    //sceGumOrtho(0, 480, 0, 272, -800.0f, 800.0f);

    sceGumMatrixMode(GU_VIEW);
    sceGumLoadIdentity();

    sceGumMatrixMode(GU_MODEL);
    sceGumLoadIdentity();

    test_24();
    test_32();
    //test_48();

    sceGuFinish();
    sceGuSync(0, 0);

    renderInfo();

    sceDisplayWaitVblankStartCB();
    fbp0 = sceGuSwapBuffers();
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

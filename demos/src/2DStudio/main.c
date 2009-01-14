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

PSP_MODULE_INFO("2D Studio", 0, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER);

#define BUF_WIDTH (512)
#define SCR_WIDTH (480)
#define SCR_HEIGHT (272)

void sendCommandi(int cmd, int argument);

int done = 0;
int selectedAttribute = 0;


static unsigned int __attribute__((aligned(16))) list[262144];

static unsigned int staticOffset = 0;

static unsigned int getMemorySize(unsigned int width, unsigned int height, unsigned int psm)
{
	switch (psm)
	{
		case GU_PSM_T4:
			return (width * height) >> 1;

		case GU_PSM_T8:
			return width * height;

		case GU_PSM_5650:
		case GU_PSM_5551:
		case GU_PSM_4444:
		case GU_PSM_T16:
			return 2 * width * height;

		case GU_PSM_8888:
		case GU_PSM_T32:
			return 4 * width * height;

		default:
			return 0;
	}
}

void* getStaticVramBuffer(unsigned int width, unsigned int height, unsigned int psm)
{
	unsigned int memSize = getMemorySize(width,height,psm);
	void* result = (void*)staticOffset;
	staticOffset += memSize;

	return result;
}


struct attribute
{
	char *label;
	int x;
	int y;
	int *pvalue;
	int min;
	int max;
	int step;
	char **names;
};

struct attribute attributes[100];
int nattributes = 0;
int textColor = 0xFFFFFFFF;
int selectedTextColor = 0xFF0000FF;
void* fbp0;
void* fbp1;
void* zbp;


void addAttribute(char *label, int *pvalue, int x, int y, int min, int max, int step)
{
	struct attribute *pattribute = &attributes[nattributes];
	pattribute->label  = label;
	pattribute->x      = x;
	pattribute->y      = y;
	pattribute->pvalue = pvalue;
	pattribute->min    = min;
	pattribute->max    = max;
	pattribute->step   = step;
	pattribute->names  = NULL;

	nattributes++;
}


void setAttributeValueNames(char **names)
{
	struct attribute *pattribute = &attributes[nattributes - 1];
	pattribute->names = names;
}


void drawAttribute(struct attribute *pattribute)
{
	char buffer[100];
	char *value;

	if (pattribute->names != NULL)
	{
		value = pattribute->names[*(pattribute->pvalue)];
	}
	else
	{
		sprintf(buffer, "%d", *(pattribute->pvalue));
		value = buffer;
	}

	pspDebugScreenSetXY(pattribute->x, pattribute->y);
	if (pattribute->label != NULL)
	{
		pspDebugScreenPrintf("%s: %s", pattribute->label, value);
	}
	else
	{
		pspDebugScreenPrintf("%s", value);
	}
}


void drawAttributes()
{
	int i;

	pspDebugScreenSetOffset((int)fbp0);
	for (i = 0; i < nattributes; i++)
	{
		pspDebugScreenSetTextColor(selectedAttribute == i ? selectedTextColor : textColor);
			
		drawAttribute(&attributes[i]);
	}
}


int states[] = {
	GU_ALPHA_TEST,
	GU_DEPTH_TEST,
	GU_TEXTURE_2D,
	GU_BLEND,
/*	GU_SCISSOR_TEST,
	GU_STENCIL_TEST,
	GU_CULL_FACE,
	GU_DITHER,
	GU_FOG,
	GU_CLIP_PLANES,
	GU_LIGHTING,
	GU_LIGHT0,
	GU_LIGHT1,
	GU_LIGHT2,
	GU_LIGHT3,
	GU_LINE_SMOOTH,
	GU_PATCH_CULL_FACE,
	GU_COLOR_TEST,
	GU_COLOR_LOGIC_OP,
	GU_FACE_NORMAL_REVERSE,
	GU_PATCH_FACE,
	GU_FRAGMENT_2X,
*/
	};

char *stateNames[] = {
	"GU_ALPHA_TEST  ",
	"GU_DEPTH_TEST  ",
	"GU_SCISSOR_TEST",
	"GU_STENCIL_TEST",
	"GU_BLEND       ",
	"GU_CULL_FACE   ",
	"GU_DITHER      ",
	"GU_FOG         ",
	"GU_CLIP_PLANES ",
	"GU_TEXTURE_2D  ",
	"GU_LIGHTING    ",
	"GU_LIGHT0      ",
	"GU_LIGHT1      ",
	"GU_LIGHT2      ",
	"GU_LIGHT3      ",
	"GU_LINE_SMOOTH ",
	"GU_PATCH_CULL_FACE",
	"GU_COLOR_TEST  ",
	"GU_COLOR_LOGIC_OP",
	"GU_FACE_NORMAL_REVERSE",
	"GU_PATCH_FACE  ",
	"GU_FRAGMENT_2X "
	};

int stateValues1[100];
int stateValues2[100];
char *stateValueNames[] = { "Off", "On", "Unchanged" };


struct Vertex
{
   float u, v;
   unsigned int color;
   float x, y, z;
};

struct Vertex __attribute__((aligned(16))) vertices1[4];
struct Vertex __attribute__((aligned(16))) vertices2[4];

struct Color
{
	int r;
	int g;
	int b;
	int a;
};

struct Point
{
	int x;
	int y;
	int z;
};

struct Color rectangle1color;
struct Color rectangle2color;
struct Point rectangle1point;
struct Point rectangle2point;

#define TEXTURE_WIDTH	8
#define TEXTURE_HEIGHT	8

unsigned int texture1[TEXTURE_WIDTH * TEXTURE_HEIGHT];
unsigned int texture2[TEXTURE_WIDTH * TEXTURE_HEIGHT];

int texFunc1 = 0;
int texFunc2 = 0;
char *texFuncNames[] = { "GU_TFX_MODULATE", "GU_TFX_DECAL", "GU_TFX_BLEND", "GU_TFX_REPLACE", "GU_TFX_ADD" };

int texFuncAlpha1 = 1;
int texFuncAlpha2 = 1;
char *texFuncAlphaNames[] = { "RGB", "ALPHA" };

int depthMask = 0;
char *depthMaskNames[] = { "enableWrites", "disableWrites" };

int alphaFunc = 7;
char *alphaFuncNames[] = { "GU_NEVER", "GU_ALWAYS", "GU_EQUAL", "GU_NOTEQUAL", "GU_LESS", "GU_LEQUAL", "GU_GREATER", "GU_GEQUAL" };
int alphaReference = 0;

int blendOp = 0;
char *blendOpNames[] = { "GU_ADD", "GU_SUBTRACT", "GU_REVERSE_SUBTRACT", "GU_MIN", "GU_MAX", "GU_ABS" };

int blendFuncSrc = 2;
int blendFuncDst = 3;
char *blendFuncNames[] = { "GU_SRC_COLOR", "GU_ONE_MINUS_SRC_COLOR", "GU_SRC_ALPHA", "GU_ONE_MINUS_SRC_ALPHA", "GU_DST_COLOR", "GU_ONE_MINUS_DST_COLOR", "GU_DST_ALPHA", "GU_ONE_MINUS_DST_ALPHA" };

int textureType1 = 0;
int textureType2 = 0;
char *textureTypeNames[] = { "Checkboard 1x1", "Checkboard 3x3", "Unicolor", "Vertical", "Horizontal", "Center" };

struct Color backgroundColor;
int clearMode = 0;
char *clearModeNames[] = { "Normal", "Draw in CLEAR command" };

int clearFlagColor = 1;
int clearFlagStencil = 0;
int clearFlagDepth = 1;
char *onOffNames[] = { "Off", "On" };


void drawStates(int stateValues[])
{
	int i;

	for (i = 0; i < sizeof(states) / sizeof(int); i++)
	{
		switch (stateValues[i])
		{
			case 0: sceGuDisable(states[i]); break;
			case 1: sceGuEnable(states[i]);  break;
			case 2: /* Unchanged */          break;
		}
	}
}


unsigned int getColor(struct Color *pcolor)
{
	unsigned int a = pcolor->a & 0xFF;
	unsigned int r = pcolor->r & 0xFF;
	unsigned int g = pcolor->g & 0xFF;
	unsigned int b = pcolor->b & 0xFF;

	return (a << 24) | (b << 16) | (g << 8) | (r << 0);
}


void setVertexColor(struct Color *pcolor, struct Vertex *pvertex)
{
	pvertex->color = getColor(pcolor);
}


void setVerticesColor(struct Color *pcolor, struct Vertex pvertices[], int sizeofVertices)
{
	int i;
	for (i = 0; i < sizeofVertices / sizeof(struct Vertex); i++)
	{
		setVertexColor(pcolor, &pvertices[i]);
	}
}


void setVertexPoint(struct Point *ppoint, struct Vertex *pvertex, int x, int y, int z, int u, int v)
{
	pvertex->u = u;
	pvertex->v = v;
	pvertex->x = x + ppoint->x;
	pvertex->y = y + ppoint->y;
	pvertex->z = z + ppoint->z;
}


void setRectanglePoint(struct Point *ppoint, struct Vertex pvertices[])
{
	setVertexPoint(ppoint, &pvertices[0], -50, -50, 0, 0,             0);
	setVertexPoint(ppoint, &pvertices[1],  50, -50, 0, TEXTURE_WIDTH, 0);
	setVertexPoint(ppoint, &pvertices[2], -50,  50, 0, 0,             TEXTURE_HEIGHT);
	setVertexPoint(ppoint, &pvertices[3],  50,  50, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
}


unsigned int getTextureColor(struct Color *pcolor, int textureType, int x, int y)
{
	unsigned int color1 = getColor(pcolor);
	unsigned int color2 = 0xFF000000;
	unsigned int color = color1;
	int factor = -1;
	int a, b;

	switch (textureType)
	{
		case 0: color = (((x + y / 1 * 1) / 1) & 1) == 0 ? color1 : color2; break;	/* Checkboard 1x1 */
		case 1: color = (((x + y / 3 * 3) / 3) & 1) == 0 ? color1 : color2; break;	/* Checkboard 3x3 */
		case 2: color = color1; break;							/* Unicolor */
		case 3: factor = x * 256 / TEXTURE_WIDTH; break;				/* Vertical */
		case 4: factor = y * 256 / TEXTURE_HEIGHT; break;				/* Horizontal */
		case 5: a = x - (TEXTURE_WIDTH  / 2);
			b = y - (TEXTURE_HEIGHT / 2);
			factor = 8 * (32 - ((a * a) + (b * b))); break;				/* Center */
	}

	if (factor != -1)
	{
		color = ((color1 * factor) + (color2 * (256 - factor))) / 256;
	}

	return color;
}


void createTexture(struct Color *pcolor, int textureType, unsigned int *texture, int width, int height)
{
	int x, y;

	for (y = 0; y < height; y++)
	{
		for (x = 0; x < width; x++)
		{
			int color = getTextureColor(pcolor, textureType, x, y);
			texture[y * width + x] = color;
		}
	}
}


void drawRectangles()
{
	createTexture(&rectangle1color, textureType1, texture1, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	createTexture(&rectangle2color, textureType2, texture2, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	setVerticesColor(&rectangle1color, vertices1, sizeof(vertices1));
	setVerticesColor(&rectangle2color, vertices2, sizeof(vertices2));
	setRectanglePoint(&rectangle1point, vertices1);
	setRectanglePoint(&rectangle2point, vertices2);

	int clearFlags = 0;
	if (clearFlagColor   != 0) clearFlags |= GU_COLOR_BUFFER_BIT;
	if (clearFlagDepth   != 0) clearFlags |= GU_DEPTH_BUFFER_BIT;
	if (clearFlagStencil != 0) clearFlags |= GU_STENCIL_BUFFER_BIT;

	if (clearMode == 0)
	{
		sceGuDisable(GU_TEXTURE_2D);
		sceGuClearColor(getColor(&backgroundColor));
		sceGuClearDepth(0);
		sceGuClearStencil(0);
		sceGuClear(clearFlags);
	}
	else
	{
		sendCommandi(211, (clearFlags << 8) | 0x01);
	}

	sceGuDepthMask(depthMask);
	sceGuAlphaFunc(alphaFunc, alphaReference, 0xFF);
	sceGuBlendFunc(blendOp, blendFuncSrc, blendFuncDst, 0, 0);

	sceGuColorMaterial(0);
	drawStates(stateValues1);
	sceGuTexMode(GU_PSM_8888,0,0,0);
	sceGuTexImage(0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, texture1);
	sceGuTexFunc(texFunc1, texFuncAlpha1);
	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	sceGuTexWrap(GU_CLAMP, GU_CLAMP);
	sceGuTexScale(1,1);
	sceGuTexOffset(0,0);
	sceGuAmbientColor(0xffffffff);
	sceGuDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF | GU_COLOR_8888 | GU_VERTEX_32BITF | GU_TRANSFORM_2D, sizeof(vertices1) / sizeof(struct Vertex), 0, vertices1);

	drawStates(stateValues2);
	sceGuTexMode(GU_PSM_8888,0,0,0);
	sceGuTexImage(0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, texture2);
	sceGuTexFunc(texFunc2, texFuncAlpha2);
	sceGuTexFilter(GU_NEAREST, GU_NEAREST);
	sceGuTexWrap(GU_CLAMP, GU_CLAMP);
	sceGuTexScale(1,1);
	sceGuTexOffset(0,0);
	sceGuAmbientColor(0xffffffff);
	sceGuDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF | GU_COLOR_8888 | GU_VERTEX_32BITF | GU_TRANSFORM_2D, sizeof(vertices2) / sizeof(struct Vertex), 0, vertices2);

	if (clearMode != 0)
	{
		sendCommandi(211,0);
	}
}


void draw()
{
	sceGuStart(GU_DIRECT, list);

	drawRectangles();

	sceGuFinish();
	sceGuSync(0, 0);

	pspDebugScreenSetBackColor(getColor(&backgroundColor));
	drawAttributes();
	sceDisplayWaitVblank();
	fbp0 = sceGuSwapBuffers();
}


void init()
{
	pspDebugScreenInit();

	fbp0 = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_8888);
	fbp1 = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_8888);
	zbp  = getStaticVramBuffer(BUF_WIDTH, SCR_HEIGHT, GU_PSM_4444);
 
	sceGuInit();
	sceGuStart(GU_DIRECT,list);
	sceGuDrawBuffer(GU_PSM_8888,fbp0,BUF_WIDTH);
	sceGuDispBuffer(SCR_WIDTH,SCR_HEIGHT,fbp1,BUF_WIDTH);
	sceGuDepthBuffer(zbp,BUF_WIDTH);
	sceGuOffset(2048 - (SCR_WIDTH/2),2048 - (SCR_HEIGHT/2));
	sceGuViewport(2048,2048,SCR_WIDTH,SCR_HEIGHT);
	sceGuDepthRange(65535,0);
	sceGuScissor(0,0,SCR_WIDTH,SCR_HEIGHT);
	sceGuEnable(GU_SCISSOR_TEST);
	sceGuFrontFace(GU_CW);
	sceGuShadeModel(GU_SMOOTH);
	sceGuDisable(GU_TEXTURE_2D);
	sceGuFinish();
	sceGuSync(0,0);
 
	sceDisplayWaitVblankStart();
	sceGuDisplay(1);

	sceCtrlSetSamplingCycle(0);
	sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);

	int x = 0;
	int y = 0;
	int i;
	for (i = 0; i < sizeof(states) / sizeof(int); i++)
	{
		stateValues1[i] = 0;
		stateValues2[i] = 0;
		addAttribute(stateNames[states[i]], &stateValues1[i], x, y, 0, 2, 1);
		setAttributeValueNames(&stateValueNames[0]);
		addAttribute(NULL, &stateValues2[i], x + 27, y, 0, 2, 1);
		setAttributeValueNames(&stateValueNames[0]);
		y++;
	}

	addAttribute("sceGuDepthMask", &depthMask, x, y, 0, 1, 1);
	setAttributeValueNames(&depthMaskNames[0]);
	y++;

	addAttribute("sceGuAlphaFunc", &alphaFunc, x, y, 0, 7, 1);
	setAttributeValueNames(&alphaFuncNames[0]);
	addAttribute(", Reference", &alphaReference, x + 27, y, 0, 255, 10);
	y++;

	addAttribute("sceGuBlendFunc op", &blendOp, x, y, 0, 5, 1);
	setAttributeValueNames(&blendOpNames[0]);
	y++;
	addAttribute("src", &blendFuncSrc, x + 15, y, 0, 7, 1);
	setAttributeValueNames(&blendFuncNames[0]);
	y++;
	addAttribute("dst", &blendFuncDst, x + 15, y, 0, 7, 1);
	setAttributeValueNames(&blendFuncNames[0]);
	y++;

	rectangle1color.r = 0x00;
	rectangle1color.g = 0xFF;
	rectangle1color.b = 0x00;
	rectangle1color.a = 0xFF;
	rectangle1point.x = 350;
	rectangle1point.y = 100;
	rectangle1point.z = 0;

	addAttribute("Rect1 X", &rectangle1point.x, x, y, 200, 400, 5);
	addAttribute(", Y", &rectangle1point.y, x + 12, y, 0, 250, 5);
	addAttribute(", Z", &rectangle1point.z, x + 20, y, -50, 50, 5);
	y++;
	addAttribute("R", &rectangle1color.r, x + 6, y, 0, 0xFF, 10);
	addAttribute(", G", &rectangle1color.g, x + 12, y, 0, 0xFF, 10);
	addAttribute(", B", &rectangle1color.b, x + 20, y, 0, 0xFF, 10);
	addAttribute(", A", &rectangle1color.a, x + 28, y, 0, 0xFF, 10);
	y++;

	addAttribute("sceGuTexFunc", &texFunc1, x + 6, y, 0, 4, 1);
	setAttributeValueNames(&texFuncNames[0]);
	y++;
	addAttribute("Alpha", &texFuncAlpha1, x + 20, y, 0, 1, 1);
	setAttributeValueNames(&texFuncAlphaNames[0]);
	y++;

	addAttribute("Texture Type", &textureType1, x + 6, y, 0, 5, 1);
	setAttributeValueNames(&textureTypeNames[0]);
	y++;

	rectangle2color.r = 0xFF;
	rectangle2color.g = 0x00;
	rectangle2color.b = 0x00;
	rectangle2color.a = 0xFF;
	rectangle2point.x = rectangle1point.x + 50;
	rectangle2point.y = rectangle1point.y + 50;
	rectangle2point.z = 0;

	addAttribute("Rect2 X", &rectangle2point.x, x, y, 200, 400, 5);
	addAttribute(", Y", &rectangle2point.y, x + 12, y, 0, 250, 5);
	addAttribute(", Z", &rectangle2point.z, x + 20, y, -50, 50, 5);
	y++;
	addAttribute("R", &rectangle2color.r, x + 6, y, 0, 0xFF, 10);
	addAttribute(", G", &rectangle2color.g, x + 12, y, 0, 0xFF, 10);
	addAttribute(", B", &rectangle2color.b, x + 20, y, 0, 0xFF, 10);
	addAttribute(", A", &rectangle2color.a, x + 28, y, 0, 0xFF, 10);
	y++;

	addAttribute("sceGuTexFunc", &texFunc2, x + 6, y, 0, 4, 1);
	setAttributeValueNames(&texFuncNames[0]);
	y++;
	addAttribute("Alpha", &texFuncAlpha2, x + 20, y, 0, 1, 1);
	setAttributeValueNames(&texFuncAlphaNames[0]);
	y++;

	addAttribute("Texture Type", &textureType2, x + 6, y, 0, 5, 1);
	setAttributeValueNames(&textureTypeNames[0]);
	y++;

	backgroundColor.r = 70;
	backgroundColor.g = 70;
	backgroundColor.b = 70;
	backgroundColor.a = 0xFF;

	addAttribute("Background R", &backgroundColor.r, x, y, 0, 0xFF, 10);
	addAttribute(", G", &backgroundColor.g, x + 17, y, 0, 0xFF, 10);
	addAttribute(", B", &backgroundColor.b, x + 25, y, 0, 0xFF, 10);
	addAttribute(", A", &backgroundColor.a, x + 33, y, 0, 0xFF, 10);
	y++;

	addAttribute("Clear Mode", &clearMode, x, y, 0, 1, 1);
	setAttributeValueNames(&clearModeNames[0]);
	y++;

	addAttribute("Clear Color", &clearFlagColor, x, y, 0, 1, 1);
	setAttributeValueNames(&onOffNames[0]);
	addAttribute(", Depth", &clearFlagDepth, x + 16, y, 0, 1, 1);
	setAttributeValueNames(&onOffNames[0]);
	addAttribute(", Stencil", &clearFlagStencil, x + 16 + 13, y, 0, 1, 1);
	setAttributeValueNames(&onOffNames[0]);
	y++;
}


int main(int argc, char *argv[])
{
    SceCtrlData pad;
    int oldButtons = 0;
#define SECOND       1000000
#define REPEAT_START (1 * SECOND)
#define REPEAT_DELAY (SECOND / 5)
    struct timeval repeatStart;
    struct timeval repeatDelay;

    repeatStart.tv_sec = 0;
    repeatStart.tv_usec = 0;
    repeatDelay.tv_sec = 0;
    repeatDelay.tv_usec = 0;

    init();

    while(!done)
    {
	draw();

        sceCtrlReadBufferPositive(&pad, 1);
        int buttonDown = (oldButtons ^ pad.Buttons) & pad.Buttons;

	if (pad.Buttons == oldButtons)
	{
		struct timeval now;
		gettimeofday(&now, NULL);
		if (repeatStart.tv_sec == 0)
		{
			repeatStart.tv_sec = now.tv_sec;
			repeatStart.tv_usec = now.tv_usec;
			repeatDelay.tv_sec = 0;
			repeatDelay.tv_usec = 0;
		}
		else
		{
			long usec = (now.tv_sec - repeatStart.tv_sec) * SECOND;
			usec += (now.tv_usec - repeatStart.tv_usec);
			if (usec >= REPEAT_START)
			{
				if (repeatDelay.tv_sec != 0)
				{
					usec = (now.tv_sec - repeatDelay.tv_sec) * SECOND;
					usec += (now.tv_usec - repeatDelay.tv_usec);
					if (usec >= REPEAT_DELAY)
					{
						repeatDelay.tv_sec = 0;
					}
				}

				if (repeatDelay.tv_sec == 0)
				{
					buttonDown = pad.Buttons;
					repeatDelay.tv_sec = now.tv_sec;
					repeatDelay.tv_usec = now.tv_usec;
				}
			}
		}
	}
	else
	{
		repeatStart.tv_sec = 0;
	}

	struct attribute *pattribute = &attributes[selectedAttribute];

        if (buttonDown & PSP_CTRL_CROSS)
        {
        }

        if (buttonDown & PSP_CTRL_LEFT)
        {
		*(pattribute->pvalue) -= pattribute->step;
		if (*(pattribute->pvalue) < pattribute->min)
		{
			*(pattribute->pvalue) = pattribute->min;
		}
        }

        if (buttonDown & PSP_CTRL_RIGHT)
        {
		*(pattribute->pvalue) += pattribute->step;
		if (*(pattribute->pvalue) > pattribute->max)
		{
			*(pattribute->pvalue) = pattribute->max;
		}
        }

        if (buttonDown & PSP_CTRL_UP)
        {
		selectedAttribute--;
		if (selectedAttribute < 0)
		{
			selectedAttribute = nattributes - 1;
		}
        }

        if (buttonDown & PSP_CTRL_DOWN)
        {
		selectedAttribute++;
		if (selectedAttribute >= nattributes)
		{
			selectedAttribute = 0;
		}
        }


        if (buttonDown & PSP_CTRL_TRIANGLE)
	{
        	done = 1;
	}

        oldButtons = pad.Buttons;
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

    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, (void*)0);
    sceKernelRegisterExitCallback(cbid);

    sceKernelSleepThreadCB();

    return 0;
}

/* Sets up the callback thread and returns its thread id */
int SetupCallbacks(void)
{
    int thid = 0;

    thid = sceKernelCreateThread("CallbackThread", CallbackThread, 0x11, 0xFA0, 0, 0);
    if(thid >= 0)
    {
        sceKernelStartThread(thid, 0, 0);
    }

    return thid;
}


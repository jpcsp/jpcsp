#define DMACPTR(ptr) ((void *) (((u32) (ptr)) & 0x1FFFFFFF))

typedef struct {
	u32 unknown0;
	u32 unknown4;
	u32 unknown8;
	u32 unknown12;
	u32 unknown16;
	u32 unknown20;
	u32 unknown24;
	u32 unknown28;
} DmaOperation;

typedef struct {
	void *src;
	void *dst;
	struct DmaOperationLink *next;
	u32 attributes;
} DmaOperationLink;

DmaOperation *sceKernelDmaOpAlloc();
int sceKernelDmaOpFree();
int sceKernelDmaOpAssign(DmaOperation *, int, int, int);
int sceKernelDmaOpEnQueue(DmaOperation *);
int sceKernelDmaOpQuit(DmaOperation *);
int sceKernelDmaOpDeQueue(DmaOperation *);
int sceKernelDmaOpSetupNormal();
int sceKernelDmaOpSetCallback(DmaOperation *, void *, int);
int sceKernelDmaOpSync(DmaOperation *, int, int);
int sceKernelDmaOpSetupLink(DmaOperation *, int, DmaOperationLink *);
int sceKernelDmaOpSetupMemcpy(DmaOperation *, void *, void *, int);

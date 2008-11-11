// outpatch
// patches elf header and section name

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define str_modulename "SavedataTool"
#define str_scemoduleinfo_pre "xodata.sceModuleInfo"
#define str_scemoduleinfo "rodata.sceModuleInfo"

int main(int argc, char* argv[])
{
	FILE *in,*out;
	unsigned long filesize;
	unsigned char *buf=NULL;
	unsigned long i,ofs_modulename,ofs_scemoduleinfo;

	in=fopen("out","rb");
	if (!in) {
		printf("file 'out' not found\n");
		exit(1);
	}
	fseek(in,0,SEEK_END);
	filesize=ftell(in);
	fseek(in,0,SEEK_SET);
	buf=(unsigned char *)malloc(filesize);
	fread(buf,1,filesize,in);
	fclose(in);

	ofs_modulename=0;
	unsigned long len_modulename=strlen(str_modulename);
	for (i=0; i<filesize; i++) {
		if (memcmp(buf+i,str_modulename,len_modulename)==0) {
			ofs_modulename=i;
			break;
		}
	}
	ofs_scemoduleinfo=0;
	unsigned long len_scemoduleinfo_pre=strlen(str_scemoduleinfo_pre);
	for (i=0; i<filesize; i++) {
		if (memcmp(buf+i,str_scemoduleinfo_pre,len_scemoduleinfo_pre)==0) {
			ofs_scemoduleinfo=i;
			break;
		}
	}
	if (ofs_modulename==0) {
		printf("modulename not found\n");
		if (buf) { free(buf); buf=NULL; }
		exit(1);
	}
	if (ofs_scemoduleinfo==0) {
		printf("scemoduleinfo not found\n");
		if (buf) { free(buf); buf=NULL; }
		exit(1);
	}

	*(unsigned long *)(buf+0x40)=ofs_modulename-4;
	memcpy(buf+ofs_scemoduleinfo,str_scemoduleinfo,strlen(str_scemoduleinfo));

	out=fopen("outp","wb");
	if (!out) {
		printf("file 'outp' can not open\n");
		exit(1);
	}
	fwrite(buf,1,filesize,out);
	fclose(out);

	printf("successed filesize:%08X modulename:%08X scemoduleinfo:%08X\n",filesize,ofs_modulename,ofs_scemoduleinfo);
	if (buf) { free(buf); buf=NULL; }
	return 0;
}


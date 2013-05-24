#include <pspkernel.h>
#include <pspdebug.h>
#include <pspctrl.h>
#include <pspkernel.h>
#include <pspdisplay.h>
#include <pspgu.h>

PSP_MODULE_INFO("vfpu test", 0, 1, 1);

PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU);

/* Define printf, just to make typing easier */
#define printf  pspDebugScreenPrintf

#define BUF_WIDTH	512
#define SCR_WIDTH	480
#define SCR_HEIGHT	272
#define FONT_HEIGHT	8

int done = 0;

void __attribute__((noinline)) vtfm4(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm4.q C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm4(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm4.q C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vtfm3_E(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm3.t C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vtfm3_M(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm3.t C000, M100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm3(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm3.t C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vtfm2(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vtfm2.p C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vhtfm2(ScePspFVector4 *v0, ScePspFMatrix4 *m0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, %2\n"

   "lv.q   C000, %0\n"

   "vhtfm2.p C000, E100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*m0) ,"m" (*v1) );
}

void __attribute__((noinline)) vmmulq(ScePspFMatrix4 *m0, ScePspFMatrix4 *m1, ScePspFMatrix4 *m2)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"

   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, 0x00+%2\n"
   "lv.q   C210, 0x10+%2\n"
   "lv.q   C220, 0x20+%2\n"
   "lv.q   C230, 0x30+%2\n"

   "vmmul.q E000, E100, E200\n"

   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0) : "m" (*m1) ,"m" (*m2) );
}

void __attribute__((noinline)) vmmult(ScePspFMatrix4 *m0, ScePspFMatrix4 *m1, ScePspFMatrix4 *m2)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"

   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, 0x00+%2\n"
   "lv.q   C210, 0x10+%2\n"
   "lv.q   C220, 0x20+%2\n"
   "lv.q   C230, 0x30+%2\n"

   "vmmul.t E000, E100, E200\n"

   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0) : "m" (*m1) ,"m" (*m2) );
}

void __attribute__((noinline)) vmmulp(ScePspFMatrix4 *m0, ScePspFMatrix4 *m1, ScePspFMatrix4 *m2)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"

   "lv.q   C100, 0x00+%1\n"
   "lv.q   C110, 0x10+%1\n"
   "lv.q   C120, 0x20+%1\n"
   "lv.q   C130, 0x30+%1\n"

   "lv.q   C200, 0x00+%2\n"
   "lv.q   C210, 0x10+%2\n"
   "lv.q   C220, 0x20+%2\n"
   "lv.q   C230, 0x30+%2\n"

   "vmmul.p E000, E100, E200\n"

   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0) : "m" (*m1) ,"m" (*m2) );
}

void __attribute__((noinline)) vsrt3s(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

// "vsrt3.s S000, S100\n"
   ".word 0xD0480400\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsrt3p(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

// "vsrt3.p S000, S100\n"
   ".word 0xD0480480\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsrt3t(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

// "vsrt3.t S000, S100\n"
   ".word 0xD0488400\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsrt3q(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vsrt3.q C000, C100\n"
// ".word 0xD0488480\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsgns(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vsgn.s S000, S100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsgnp(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vsgn.p C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsgnt(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vsgn.t C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsgnq(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vsgn.q C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vwbq(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vwb.q C100, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vdotq(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vdot.q S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vdott(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vdot.t S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vdotp(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vdot.p S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vrsqq(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vrsq.q C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vrsqt(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vrsq.t C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vrsqp(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vrsq.p C000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vrsqs(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vrsq.s S000, S100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vhdpq(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vhdp.q S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vhdpt(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vhdp.t S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vhdpp(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vhdp.p S000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vcrspt(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vcrsp.t C000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vcmovtq(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2, int imm3)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vcmp.q LT, C100, C200\n"

   : "+m" (*v0) : "m" (*v1), "m" (*v2));

   if (imm3 == 0) {
      asm volatile ("vcmovt.q C000, C100, 0\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 1) {
      asm volatile ("vcmovt.q C000, C100, 1\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 2) {
      asm volatile ("vcmovt.q C000, C100, 2\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 3) {
      asm volatile ("vcmovt.q C000, C100, 3\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 4) {
      asm volatile ("vcmovt.q C000, C100, 4\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 5) {
      asm volatile ("vcmovt.q C000, C100, 5\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 6) {
      asm volatile ("vcmovt.q C000, C100, 6\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 7) {
      // asm volatile ("vcmovt.q C000, C100, 7\n" : "+m" (*v0) : "m" (*v1));
      asm volatile (".word 0xD2A78480\n");
   }

	asm volatile (
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vcmovfq(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2, int imm3)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "lv.q   C200, %2\n"

   "vcmp.q LT, C100, C200\n"

   : "+m" (*v0) : "m" (*v1), "m" (*v2));

   if (imm3 == 0) {
      asm volatile ("vcmovf.q C000, C100, 0\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 1) {
      asm volatile ("vcmovf.q C000, C100, 1\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 2) {
      asm volatile ("vcmovf.q C000, C100, 2\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 3) {
      asm volatile ("vcmovf.q C000, C100, 3\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 4) {
      asm volatile ("vcmovf.q C000, C100, 4\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 5) {
      asm volatile ("vcmovf.q C000, C100, 5\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 6) {
      asm volatile ("vcmovf.q C000, C100, 6\n" : "+m" (*v0) : "m" (*v1));
   } else if (imm3 == 7) {
      // asm volatile ("vcmovf.q C000, C100, 7\n" : "+m" (*v0) : "m" (*v1));
      asm volatile (".word 0xD2AF8480\n");
   }

	asm volatile (
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vi2c(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vi2c.q S000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vi2uc(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"

   "lv.q   C100, %1\n"

   "vi2uc.q S000, C100\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) bvt(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
    ScePspFVector4 v;

	asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vcmp.s   NE, S000, S000\n"
   "bvt      0, skip1\n"
   "vmov.s   S000, S001\n"
   "skip1:\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v0->x = v.x;

	asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vcmp.s   EQ, S000, S000\n"
   "bvt      0, skip2\n"
   "vmov.s   S000, S001\n"
   "skip2:\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v0->y = v.x;

   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vcmp.s   EQ, S000, S000\n"
   "bvtl     0, skip3\n"
   "vmov.s   S000, S001\n"
   "skip3:\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v0->z = v.x;

   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vzero.s  S001\n"
   "vzero.s  S100\n"
   "vone.s   S101\n"
   "vcmp.p   EQ, C000, C100\n"
   "bvt      0, skip4\n"
   "bvt      1, skip5\n"
   "vadd.s   S000, S000, S101\n"
   "skip4: vadd.s   S000, S000, S101\n"
   "skip5: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v0->w = v.x;

   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vzero.s  S100\n"
   "vone.s   S101\n"
   "vcmp.p   EQ, C000, C100\n"
   "bvt      0, skip6\n"
   "bvt      1, skip7\n"
   "vadd.s   S000, S000, S101\n"
   "skip6: vadd.s   S000, S000, S101\n"
   "skip7: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v1->x = v.x;

   /*
    * S000 == S100, S001 == S101, S002 == S102, S003 == S103
    */
   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vzero.s  S002\n"
   "vzero.s  S003\n"
   "vzero.s  S100\n"
   "vone.s   S101\n"
   "vzero.s  S102\n"
   "vzero.s  S103\n"
   "vcmp.q   EQ, C000, C100\n"
   "bvt      0, skip8\n"
   "bvt      1, skip9\n"
   "bvt      2, skip10\n"
   "bvt      3, skip11\n"
   "vadd.s   S000, S000, S101\n"
   "skip8: vadd.s   S000, S000, S101\n"
   "skip9: vadd.s   S000, S000, S101\n"
   "skip10: vadd.s   S000, S000, S101\n"
   "skip11: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v1->y = v.x;

   /*
    * S000 != S100, S001 == S101, S002 == S102, S003 == S103
    */
   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vone.s   S001\n"
   "vzero.s  S002\n"
   "vzero.s  S003\n"
   "vone.s   S100\n"
   "vone.s   S101\n"
   "vzero.s  S102\n"
   "vzero.s  S103\n"
   "vcmp.q   EQ, C000, C100\n"
   "bvt      0, skip12\n"
   "bvt      1, skip13\n"
   "bvt      2, skip14\n"
   "bvt      3, skip15\n"
   "vadd.s   S000, S000, S101\n"
   "skip12: vadd.s   S000, S000, S101\n"
   "skip13: vadd.s   S000, S000, S101\n"
   "skip14: vadd.s   S000, S000, S101\n"
   "skip15: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v1->z = v.x;

   /*
    * S000 != S100, S001 != S101, S002 == S102, S003 == S103
    */
   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vzero.s  S001\n"
   "vzero.s  S002\n"
   "vzero.s  S003\n"
   "vone.s   S100\n"
   "vone.s   S101\n"
   "vzero.s  S102\n"
   "vzero.s  S103\n"
   "vcmp.q   EQ, C000, C100\n"
   "bvt      0, skip16\n"
   "bvt      1, skip17\n"
   "bvt      2, skip18\n"
   "bvt      3, skip19\n"
   "vadd.s   S000, S000, S101\n"
   "skip16: vadd.s   S000, S000, S101\n"
   "skip17: vadd.s   S000, S000, S101\n"
   "skip18: vadd.s   S000, S000, S101\n"
   "skip19: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v1->w = v.x;

   /*
    * S000 != S100, S001 != S101, S002 != S102, S003 == S103
    */
   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vzero.s  S001\n"
   "vzero.s  S002\n"
   "vzero.s  S003\n"
   "vone.s   S100\n"
   "vone.s   S101\n"
   "vone.s   S102\n"
   "vzero.s  S103\n"
   "vcmp.q   EQ, C000, C100\n"
   "bvt      0, skip20\n"
   "bvt      1, skip21\n"
   "bvt      2, skip22\n"
   "bvt      3, skip23\n"
   "vadd.s   S000, S000, S101\n"
   "skip20: vadd.s   S000, S000, S101\n"
   "skip21: vadd.s   S000, S000, S101\n"
   "skip22: vadd.s   S000, S000, S101\n"
   "skip23: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v2->x = v.x;

   /*
    * S000 != S100, S001 != S101, S002 != S102, S003 != S103
    */
   asm volatile (
   ".set push\n"
   ".set noreorder\n"
   "vzero.s  S000\n"
   "vzero.s  S001\n"
   "vzero.s  S002\n"
   "vzero.s  S003\n"
   "vone.s   S100\n"
   "vone.s   S101\n"
   "vone.s   S102\n"
   "vone.s   S103\n"
   "vcmp.q   EQ, C000, C100\n"
   "bvt      0, skip24\n"
   "bvt      1, skip25\n"
   "bvt      2, skip26\n"
   "bvt      3, skip27\n"
   "vadd.s   S000, S000, S101\n"
   "skip24: vadd.s   S000, S000, S101\n"
   "skip25: vadd.s   S000, S000, S101\n"
   "skip26: vadd.s   S000, S000, S101\n"
   "skip27: vadd.s   S000, S000, S101\n"
   "sv.s     S000, %0\n"
   ".set pop\n"
   : "+m" (v) : "m" (v));

   v2->y = v.x;
}

void __attribute__((noinline)) svlq(int address, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, %1\n"
   "svl.q  C100, 0($a0)\n"
   : "+m" (address) : "m" (*v1) );
}

void __attribute__((noinline)) svrq(int address, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, %1\n"
   "svr.q  C100, 0($a0)\n"
   : "+m" (address) : "m" (*v1) );
}

void __attribute__((noinline)) lvlq(ScePspFVector4 *v0, int address, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, %2\n"
   "lvl.q  C100, 0($a1)\n"
   "sv.q   C100, %0\n"
   : "+m" (*v0) : "m" (address), "m" (*v1) );
}

void __attribute__((noinline)) lvrq(ScePspFVector4 *v0, int address, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C100, %2\n"
   "lvr.q  C100, 0($a1)\n"
   "sv.q   C100, %0\n"
   : "+m" (*v0) : "m" (address), "m" (*v1) );
}

void __attribute__((noinline)) vsocps(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vsocp.s C000, S100\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vsocpp(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vsocp.p C000, C100\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vi2fq0(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vi2f.q C000, C100, 0\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vi2fq1(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vi2f.q C000, C100, 1\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vlog2q(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vlog2.q C000, C100\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vexp2q(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vexp2.q C000, C100\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vmidtq1(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.q M000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtq2(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.q E000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt1(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t M000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt2(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t E000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt3(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t M001\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt4(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t E001\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt5(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t M010\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtt6(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.t E010\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp1(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p M000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp2(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p E000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp3(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p M002\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp4(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p E002\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp5(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p M020\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vmidtp6(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vmidt.p E020\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp1(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p C000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp2(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p C002\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp3(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p R000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp4(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p R001\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp5(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p R020\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtp6(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.p R021\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtq1(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.q C000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtq2(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.q C010\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtq3(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.q R000\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vidtq4(ScePspFMatrix4 *m0)
{
	asm volatile (
   "lv.q   C000, 0x00+%0\n"
   "lv.q   C010, 0x10+%0\n"
   "lv.q   C020, 0x20+%0\n"
   "lv.q   C030, 0x30+%0\n"
   "vidt.q R001\n"
   "sv.q   C000, 0x00+%0\n"
   "sv.q   C010, 0x10+%0\n"
   "sv.q   C020, 0x20+%0\n"
   "sv.q   C030, 0x30+%0\n"
   : "+m" (*m0));
}

void __attribute__((noinline)) vaddq_cvs64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "lv.q   C200, %2\n"
//   "vadd.q C000, C100+64, C200\n"
   ".word 0x60088480+0x4000\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vaddq_cvt64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "lv.q   C200, %2\n"
//   "vadd.q C000, C100, C200+64\n"
   ".word 0x60088480+0x400000\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vaddq_cvd64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "lv.q   C200, %2\n"
//   "vadd.q C000+64, C100, C200\n"
   ".word 0x60088480+0x40\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vaddq_rvs64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   R000, %0\n"
   "lv.q   R100, %1\n"
   "lv.q   R200, %2\n"
//   "vadd.q R000, R100+64, R200\n"
   ".word 0x6028A4A0+0x4000\n"
   "sv.q   R000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vaddq_rvt64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   R000, %0\n"
   "lv.q   R100, %1\n"
   "lv.q   R200, %2\n"
//   "vadd.q R000, R100, R200+64\n"
   ".word 0x6028A4A0+0x400000\n"
   "sv.q   R000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vaddq_rvd64(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   R000, %0\n"
   "lv.q   R100, %1\n"
   "lv.q   R200, %2\n"
//   "vadd.q R000+64, R100, R200\n"
   ".word 0x6028A4A0+0x40\n"
   "sv.q   R000, %0\n"
   : "+m" (*v0) : "m" (*v1), "m" (*v2));
}

void __attribute__((noinline)) vc2i_vd64(ScePspFVector4 *v0, ScePspFVector4 *v1)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "vpfxd  [x, y, M, M]\n"
//   "vc2i.s C000+64, C100\n"
   ".word 0xD0390400+0x40\n"
   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1));
}

void __attribute__((noinline)) vdiv(ScePspFVector4 *v0, ScePspFVector4 *v1, ScePspFVector4 *v2)
{
	asm volatile (
   "lv.q   C000, %0\n"
   "lv.q   C100, %1\n"
   "lv.q   C200, %2\n"

   "vdiv.q C000, C100, C200\n"

   "sv.q   C000, %0\n"
   : "+m" (*v0) : "m" (*v1) ,"m" (*v2) );
}


ScePspFVector4 v0;
ScePspFVector4 v1;
ScePspFVector4 v2;
ScePspFMatrix4 m0;
ScePspFMatrix4 m1;
ScePspFMatrix4 m2;
ScePspFMatrix4 m3;

void initValues()
{
	// Reset output values
	v0.x = 123456;
	v0.y = 123456;
	v0.z = 123456;
	v0.w = 123456;

	m0.x.x = 0;
	m0.x.y = 0;
	m0.x.z = 0;
	m0.x.w = 0;

	m0.y.x = 0;
	m0.y.y = 0;
	m0.y.z = 0;
	m0.y.w = 0;

	m0.z.x = 0;
	m0.z.y = 0;
	m0.z.z = 0;
	m0.z.w = 0;

	m0.w.x = 0;
	m0.w.y = 0;
	m0.w.z = 0;
	m0.w.w = 0;

	// Some random input values...
	v1.x = 17;
	v1.y = 13;
	v1.z = -5;
	v1.w = 11;

	v2.x = 3;
	v2.y = -7;
	v2.z = -15;
	v2.w = 19;

	m1.x.x = -23;
	m1.x.y = -9;
	m1.x.z = 17;
	m1.x.w = -13;

	m1.y.x = 12;
	m1.y.y = 26;
	m1.y.z = -11;
	m1.y.w = -7;

	m1.z.x = 8;
	m1.z.y = -3;
	m1.z.z = 7;
	m1.z.w = 17;

	m1.w.x = 31;
	m1.w.y = -7;
	m1.w.z = 5;
	m1.w.w = 11;

	m2.x.x = 14;
	m2.x.y = -5;
	m2.x.z = -3;
	m2.x.w = 11;

	m2.y.x = 15;
	m2.y.y = -2;
	m2.y.z = -9;
	m2.y.w = 17;

	m2.z.x = 5;
	m2.z.y = -7;
	m2.z.z = 13;
	m2.z.w = -19;

	m2.w.x = -26;
	m2.w.y = 4;
	m2.w.z = -31;
	m2.w.w = 18;

	m3.x.x = 2;
	m3.x.y = 2;
	m3.x.z = 2;
	m3.x.w = 2;

	m3.y.x = 2;
	m3.y.y = 2;
	m3.y.z = 2;
	m3.y.w = 2;

	m3.z.x = 2;
	m3.z.y = 2;
	m3.z.z = 2;
	m3.z.w = 2;

	m3.w.x = 2;
	m3.w.y = 2;
	m3.w.z = 2;
	m3.w.w = 2;
}

void startNewScreen()
{
	pspDebugScreenInit();
	printf("Press Cross/Square/Circle/Left/Right to start test group 1, 2, 3, 4, 5\n");
	printf("Press Triangle to exit\n");
}

int divq(int a, int b) {
	return a / b;
}

int divr(int a, int b) {
	return a % b;
}

unsigned int divuq(unsigned int a, unsigned int b) {
	return a / b;
}

unsigned int divur(unsigned int a, unsigned int b) {
	return a % b;
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
	int i;
	int *pint;

    repeatStart.tv_sec = 0;
    repeatStart.tv_usec = 0;
    repeatDelay.tv_sec = 0;
    repeatDelay.tv_usec = 0;

	startNewScreen();

	printf("Signed   Divide by zero hi=0x%08X, lo=0x%08X\n", divr(0x12345678, 0), divq(0x12345678, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x12345678, 0), divuq(0x12345678, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0, 0), divuq(0, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x1, 0), divuq(0x1, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x10, 0), divuq(0x10, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x100, 0), divuq(0x100, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x1000, 0), divuq(0x1000, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0xFFFF, 0), divuq(0xFFFF, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x10000, 0), divuq(0x10000, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x100000, 0), divuq(0x100000, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x1000000, 0), divuq(0x1000000, 0));
	printf("Unsigned Divide by zero hi=0x%08X, lo=0x%08X\n", divur(0x10000000, 0), divuq(0x10000000, 0));

	while(!done)
	{
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

		if (buttonDown & PSP_CTRL_CROSS)
		{
			startNewScreen();

			initValues();
			vtfm4(&v0, &m1, &v1);
			printf("vtfm4  : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm4(&v0, &m1, &v1);
			printf("vhtfm4 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vtfm3_E(&v0, &m1, &v1);
			printf("vtfm3_E: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			initValues();
			vtfm3_M(&v0, &m1, &v1);
			printf("vtfm3_M: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm3(&v0, &m1, &v1);
			printf("vhtfm3 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vtfm2(&v0, &m1, &v1);
			printf("vtfm2  : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhtfm2(&v0, &m1, &v1);
			printf("vhtfm2 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vmmulq(&m0, &m1, &m2);
			printf("vmmul.q: %f %f %f %f\n", m0.x.x, m0.x.y, m0.x.z, m0.x.w);
			printf("         %f %f %f %f\n", m0.y.x, m0.y.y, m0.y.z, m0.y.w);
			printf("         %f %f %f %f\n", m0.z.x, m0.z.y, m0.z.z, m0.z.w);
			printf("         %f %f %f %f\n", m0.w.x, m0.w.y, m0.w.z, m0.w.w);

			initValues();
			vmmult(&m0, &m1, &m2);
			printf("vmmul.t: %f %f %f %f\n", m0.x.x, m0.x.y, m0.x.z, m0.x.w);
			printf("         %f %f %f %f\n", m0.y.x, m0.y.y, m0.y.z, m0.y.w);
			printf("         %f %f %f %f\n", m0.z.x, m0.z.y, m0.z.z, m0.z.w);
			printf("         %f %f %f %f\n", m0.w.x, m0.w.y, m0.w.z, m0.w.w);

			initValues();
			vmmulp(&m0, &m1, &m2);
			printf("vmmul.p: %f %f %f %f\n", m0.x.x, m0.x.y, m0.x.z, m0.x.w);
			printf("         %f %f %f %f\n", m0.y.x, m0.y.y, m0.y.z, m0.y.w);
			printf("         %f %f %f %f\n", m0.z.x, m0.z.y, m0.z.z, m0.z.w);
			printf("         %f %f %f %f\n", m0.w.x, m0.w.y, m0.w.z, m0.w.w);

			initValues();
			v1.x = -1;
			v1.y = 1;
			vsrt3s(&v0, &v1);
			printf("vsrt3.s: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = 0;
			v1.y = -1;
			vsrt3s(&v0, &v1);
			printf("vsrt3.s: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = 1;
			v1.y = -1;
			vsrt3s(&v0, &v1);
			printf("vsrt3.s: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = -100;
			v1.y = 1;
			vsrt3s(&v0, &v1);
			printf("vsrt3.s: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = -11.51;
			v1.y = 134.49;
			v1.z = 1.57;
			v1.w = 0.99;
			vsrt3s(&v0, &v1);
			printf("vsrt3.s: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 1;
			v1.y = 2;
			vsrt3p(&v0, &v1);
			printf("vsrt3.p: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 2;
			v1.y = 1;
			vsrt3p(&v0, &v1);
			printf("vsrt3.p: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 1;
			v1.y = 2;
			v1.z = 3;
			vsrt3t(&v0, &v1);
			printf("vsrt3.t: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 3;
			v1.y = 2;
			v1.z = 1;
			vsrt3t(&v0, &v1);
			printf("vsrt3.t: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 3;
			v1.y = 1;
			v1.z = 2;
			vsrt3t(&v0, &v1);
			printf("vsrt3.t: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = -3;
			v1.y = -1;
			v1.z = -2;
			vsrt3t(&v0, &v1);
			printf("vsrt3.t: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vsrt3q(&v0, &v1);
			printf("vsrt3.q: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			bvt(&v0, &v1, &v2);
			printf("bvt: %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f", v0.x, v0.y, v0.z, v0.w, v1.x, v1.y, v1.z, v1.w, v2.x, v2.y);
		}

		if (buttonDown & PSP_CTRL_SQUARE)
		{
			startNewScreen();

			initValues();
			v1.x = -100;
			vsgns(&v0, &v1);
			printf("vsgn.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = 100;
			vsgns(&v0, &v1);
			printf("vsgn.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = 0;
			vsgns(&v0, &v1);
			printf("vsgn.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			v1.x = 1/0.0f;
			vsgns(&v0, &v1);
			printf("vsgn.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = -100;
			v1.y = 100;
			vsgnp(&v0, &v1);
			printf("vsgn.p : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 100;
			v1.y = -100;
			v1.z = 100;
			vsgnt(&v0, &v1);
			printf("vsgn.t : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vsgnq(&v0, &v1);
			printf("vsgn.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vwbq(&v0, &v1);
			printf("vwb.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
			printf("        %f %f %f %f\n", v1.x, v1.y, v1.z, v1.w);

			initValues();
			vdotq(&v0, &v1, &v2);
			printf("vdot.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vdott(&v0, &v1, &v2);
			printf("vdot.t : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vdotp(&v0, &v1, &v2);
			printf("vdot.p : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vrsqq(&v0, &v1);
			printf("vrsq.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vrsqt(&v0, &v1);
			printf("vrsq.t : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vrsqp(&v0, &v1);
			printf("vrsq.p : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vrsqs(&v0, &v1);
			printf("vrsq.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhdpq(&v0, &v1, &v2);
			printf("vhdp.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhdpt(&v0, &v1, &v2);
			printf("vhdp.t : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vhdpp(&v0, &v1, &v2);
			printf("vhdp.p : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vcrspt(&v0, &v1, &v2);
			printf("vcrsp.t : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			pint = (int *) &v1;
			pint[0] = 0x11223344;
			pint[1] = 0x55667788;
			pint[2] = 0x99aabbcc;
			pint[3] = 0xddee00ff;
			vi2c(&v0, &v1);
			pint = (int *) &v0;
			printf("vi2c.q : %08x\n", *pint);

			initValues();
			pint = (int *) &v1;
			pint[0] = 0x11223344;
			pint[1] = 0x55667788;
			pint[2] = 0x99aabbcc;
			pint[3] = 0xddee00ff;
			vi2uc(&v0, &v1);
			pint = (int *) &v0;
			printf("vi2uc.q : %08x\n", *pint);

			initValues();
			v1.x = 0.4;
			vsocps(&v0, &v1);
			printf("vsocp.s : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			v1.x = 1.4;
			v1.y = -0.4;
			vsocpp(&v0, &v1);
			printf("vsocp.p : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			pint = (int *) &v1;
			pint[0] = 12345;
			pint[1] = -12345;
			pint[2] = 6789;
			pint[3] = -6789;
			vi2fq0(&v0, &v1);
			printf("vi2f.q 0 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			pint = (int *) &v1;
			pint[0] = 12345;
			pint[1] = -12345;
			pint[2] = 6789;
			pint[3] = -6789;
			vi2fq1(&v0, &v1);
			printf("vi2f.q 1 : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vlog2q(&v0, &v1);
			printf("vlog2.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vexp2q(&v0, &v1);
			printf("vexp2.q : %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			pint = (int *) &v1;
			pint[0] = 0x12345678;
			pint[1] = 0x55667788;
			pint[2] = 0x99aabbcc;
			pint[3] = 0xddee00ff;
			vc2i_vd64(&v0, &v1);
			pint = (int *) &v0;
			printf("vc2i.q : 0x%08X 0x%08X 0x%08X 0x%08X\n", pint[0], pint[1], pint[2], pint[3]);
		}

		if (buttonDown & PSP_CTRL_CIRCLE)
		{
			startNewScreen();

			for (i = 0; i < 8; i++)
			{
				initValues();
				v2.x = v1.x + 1;
				v2.y = v1.y;
				v2.z = v1.z - 1;
				v2.w = v1.w + 2;
				vcmovtq(&v0, &v1, &v2, i);
				printf("vcmovt.q %d: %f %f %f %f\n", i, v0.x, v0.y, v0.z, v0.w);
			}

			for (i = 0; i < 8; i++)
			{
				initValues();
				v2.x = v1.x + 1;
				v2.y = v1.y;
				v2.z = v1.z - 1;
				v2.w = v1.w + 2;
				vcmovfq(&v0, &v1, &v2, i);
				printf("vcmovf.q %d: %f %f %f %f\n", i, v0.x, v0.y, v0.z, v0.w);
			}

			initValues();
			v1.x = 0;
			v1.y = 1;
			v1.z = -1;
			v1.w = 1;
			v2.x = 0;
			v2.y = 0;
			v2.z = 0;
			v2.w = -0.0;
			vdiv(&v0, &v1, &v2);
			int *p = (int *) &v0;
			printf("vdiv.q: 0/0=%f(0x%X) 1/0=%f(0x%X) -1/0=%f(0x%X) 1/-0=%f(0x%X)\n", v0.x, p[0], v0.y, p[1], v0.z, p[2], v0.w, p[3]);

			initValues();
			v1.x = 0;
			v1.y = 0;
			v1.z = -0.0;
			v1.w = -0.0;
			v2.x = 0;
			v2.y = -0.0;
			v2.z = 0;
			v2.w = -0.0;
			vdiv(&v0, &v1, &v2);
			printf("vdiv.q: 0/0=%f(0x%X) 0/-0=%f(0x%X) -0/0=%f(0x%X) -0/-0=%f(0x%X)\n", v0.x, p[0], v0.y, p[1], v0.z, p[2], v0.w, p[3]);
		}

		if (buttonDown & PSP_CTRL_LEFT)
		{
			startNewScreen();

			for (i = 0; i < 4; i++)
			{
				initValues();
				svlq(((int) (&m0.y)) + (i << 2), &m1.y);
				printf("svlq.q %d: %.0f %.0f %.0f %.0f\n", i, m0.y.x, m0.y.y, m0.y.z, m0.y.w);
			}

			for (i = 0; i < 4; i++)
			{
				initValues();
				svrq(((int) (&m0.y)) + (i << 2), &m1.y);
				printf("svrq.q %d: %.0f %.0f %.0f %.0f\n", i, m0.y.x, m0.y.y, m0.y.z, m0.y.w);
			}

			for (i = 0; i < 4; i++)
			{
				initValues();
				lvlq(&v0, ((int) (&m1.y)) + (i << 2), &m0.x);
				printf("lvlq.q %d: %.0f %.0f %.0f %.0f\n", i, v0.x, v0.y, v0.z, v0.w);
			}

			for (i = 0; i < 4; i++)
			{
				initValues();
				lvrq(&v0, ((int) (&m1.y)) + (i << 2), &m0.x);
				printf("lvrq.q %d: %.0f %.0f %.0f %.0f\n", i, v0.x, v0.y, v0.z, v0.w);
			}
		}

		if (buttonDown & PSP_CTRL_RIGHT)
		{
			startNewScreen();

			initValues();
			vmidtq1(&m3);
			printf("vmidt.q M000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtq2(&m3);
			printf("vmidt.q E000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt1(&m3);
			printf("vmidt.t M000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt2(&m3);
			printf("vmidt.t E000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt3(&m3);
			printf("vmidt.t M001: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt4(&m3);
			printf("vmidt.t E001: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt5(&m3);
			printf("vmidt.t M010: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtt6(&m3);
			printf("vmidt.t E010: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp1(&m3);
			printf("vmidt.p M000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp2(&m3);
			printf("vmidt.p E000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp3(&m3);
			printf("vmidt.p M002: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp4(&m3);
			printf("vmidt.p E002: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp5(&m3);
			printf("vmidt.p M020: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vmidtp6(&m3);
			printf("vmidt.p E020: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp1(&m3);
			printf("vidt.p  C000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp2(&m3);
			printf("vidt.p  C002: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp3(&m3);
			printf("vidt.p  R000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp4(&m3);
			printf("vidt.p  R001: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp5(&m3);
			printf("vidt.p  R020: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtp6(&m3);
			printf("vidt.p  R021: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtq1(&m3);
			printf("vidt.q  C000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtq2(&m3);
			printf("vidt.q  C010: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtq3(&m3);
			printf("vidt.q  R000: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vidtq4(&m3);
			printf("vidt.q  R001: %.0f %.0f %.0f %.0f", m3.x.x, m3.x.y, m3.x.z, m3.x.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.y.x, m3.y.y, m3.y.z, m3.y.w);
			printf(" | %.0f %.0f %.0f %.0f", m3.z.x, m3.z.y, m3.z.z, m3.z.w);
			printf(" | %.0f %.0f %.0f %.0f\n", m3.w.x, m3.w.y, m3.w.z, m3.w.w);

			initValues();
			vaddq_cvs64(&v0, &v1, &v2);
			printf("vadd.q column vs64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vaddq_cvt64(&v0, &v1, &v2);
			printf("vadd.q column vt64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vaddq_cvd64(&v0, &v1, &v2);
			printf("vadd.q column vd64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vaddq_rvs64(&v0, &v1, &v2);
			printf("vadd.q row vs64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vaddq_rvt64(&v0, &v1, &v2);
			printf("vadd.q row vt64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);

			initValues();
			vaddq_rvd64(&v0, &v1, &v2);
			printf("vadd.q row vd64: %f %f %f %f\n", v0.x, v0.y, v0.z, v0.w);
		}

		if (buttonDown & PSP_CTRL_TRIANGLE)
		{
			done = 1;
		}

		oldButtons = pad.Buttons;
		sceDisplayWaitVblank();
	}

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

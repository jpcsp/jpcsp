/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.Allegrex.compiler;

import org.objectweb.asm.MethodVisitor;

/**
 * @author gid15
 *
 */
public interface ICompilerContext {
	public void compileInterpreterInstruction();
	public void compileRTRSIMM(String method, boolean signedImm);
	public void compileRDRT(String method);
	public void compileSyscall();
    public void loadRs();
    public void loadRt();
    public void loadRd();
    public void loadRegister(int reg);
    public void loadFs();
    public void loadFt();
    public void loadFd();
    public void loadFRegister(int reg);
    public void loadVs(int n);
    public void loadVs(int vsize, int n);
    public void loadVs(int vsize, int vs, int n);
    public void loadVsInt(int n);
    public void loadVsInt(int vsize, int n);
    public void loadVsInt(int vsize, int vs, int n);
    public void loadVt(int n);
    public void loadVt(int vsize, int n);
    public void loadVt(int vsize, int vt, int n);
    public void loadVtInt(int n);
    public void loadVtInt(int vsize, int n);
    public void loadVtInt(int vsize, int vt, int n);
    public void loadVd(int n);
    public void loadVd(int vize, int n);
    public void loadVd(int vize, int vd, int n);
    public void loadVdInt(int n);
    public void loadVdInt(int vsize, int n);
    public void loadVdInt(int vsize, int vd, int n);
    public void loadHilo();
    public void loadSaValue();
    public void loadFCr();
    public void loadFcr31c();
    public void loadVcrCc(int cc);
    public void storeRd();
    public void storeRt();
    public void storeRd(int constantValue);
    public void storeRt(int constantValue);
    public void storeFd();
    public void storeFt();
    public void storeVd(int n);
    public void storeVd(int vsize, int n);
    public void storeVd(int vsize, int vd, int n);
    public void storeVdInt(int n);
    public void storeVdInt(int vsize, int n);
    public void storeVdInt(int vsize, int vd, int n);
    public void storeVt(int n);
    public void storeVt(int vsize, int n);
    public void storeVt(int vsize, int vt, int n);
    public void storeVtInt(int n);
    public void storeVtInt(int vsize, int n);
    public void storeVtInt(int vsize, int vt, int n);
    public void storeHilo();
    public void storeFCr();
    public void storeFcr31c();
    public void storeVcrCc(int cc);
    public void prepareRdForStore();
    public void prepareRtForStore();
    public void prepareFdForStore();
    public void prepareFtForStore();
    public void prepareVdForStore(int n);
    public void prepareVdForStore(int vsize, int n);
    public void prepareVdForStore(int vsize, int vd, int n);
    public void prepareVdForStoreInt(int n);
    public void prepareVdForStoreInt(int vsize, int n);
    public void prepareVdForStoreInt(int vsize, int vd, int n);
    public void prepareVtForStore(int n);
    public void prepareVtForStore(int vsize, int n);
    public void prepareVtForStore(int vsize, int vt, int n);
    public void prepareVtForStoreInt(int n);
    public void prepareVtForStoreInt(int vsize, int n);
    public void prepareVtForStoreInt(int vsize, int vt, int n);
    public void prepareHiloForStore();
    public void prepareFCrForStore();
    public void prepareFcr31cForStore();
    public void prepareVcrCcForStore(int cc);
	public int getRsRegisterIndex();
    public int getRtRegisterIndex();
    public int getRdRegisterIndex();
	public int getFsRegisterIndex();
    public int getFtRegisterIndex();
    public int getFdRegisterIndex();
	public int getVsRegisterIndex();
    public int getVtRegisterIndex();
    public int getVdRegisterIndex();
    public int getVsize();
    public int getCrValue();
    public int getSaValue();
    public boolean isRdRegister0();
    public boolean isRtRegister0();
    public boolean isRsRegister0();
    public int getImm16(boolean signedImm);
    public int getImm14(boolean signedImm);
    public int getImm7();
    public int getImm5();
    public int getImm4();
    public int getImm3();
    public void loadImm(int imm);
    public void loadImm16(boolean signedImm);
    public MethodVisitor getMethodVisitor();
    public void memRead32(int registerIndex, int offset);
    public void memRead16(int registerIndex, int offset);
    public void memRead8(int registerIndex, int offset);
    public void memWrite32(int registerIndex, int offset);
    public void memWrite16(int registerIndex, int offset);
    public void memWrite8(int registerIndex, int offset);
    public void prepareMemWrite32(int registerIndex, int offset);
    public void prepareMemWrite16(int registerIndex, int offset);
    public void prepareMemWrite8(int registerIndex, int offset);
    public void memWriteZero8(int registerIndex, int offset);
    public void convertUnsignedIntToLong();
    public void startPfxCompiled();
    public void startPfxCompiled(boolean isFloat);
    public void endPfxCompiled();
    public void endPfxCompiled(boolean isFloat);
    public void endPfxCompiled(int vsize);
    public void endPfxCompiled(int vsize, boolean isFloat);
    public void endPfxCompiled(int vsize, boolean isFloat, boolean doFlush);
    public void flushPfxCompiled(int vsize, int vd, boolean isFloat);
    public boolean isPfxConsumed(int flag);
    public void loadTmp1();
    public void loadTmp2();
    public void loadLTmp1();
    public void loadFTmp1();
    public void loadFTmp2();
    public void loadFTmp3();
    public void loadFTmp4();
    public void storeTmp1();
    public void storeTmp2();
    public void storeLTmp1();
    public void storeFTmp1();
    public void storeFTmp2();
    public void storeFTmp3();
    public void storeFTmp4();
    public VfpuPfxSrcState getPfxsState();
    public VfpuPfxSrcState getPfxtState();
    public VfpuPfxDstState getPfxdState();
    public boolean isVsVdOverlap();
    public boolean isVtVdOverlap();
    public void compileVFPUInstr(Object cstBefore, int opcode, String mathFunction);
    public boolean compileVFPULoad(int registerIndex, int offset, int vt, int count);
    public boolean compileVFPUStore(int registerIndex, int offset, int vt, int count);
    public CodeInstruction getCodeInstruction();
    public CodeInstruction getCodeInstruction(int address);
    public void skipInstructions(int numberInstructionsToBeSkipped, boolean skipDelaySlot);
}

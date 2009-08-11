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
	public void compileRDRSRT(String method);
	public void compileRDRTRS(String method);
	public void compileRDRTSA(String method);
	public void compileRDRT(String method);
	public void compileSyscall();
    public void loadRs();
    public void loadRt();
    public void loadRd();
    public void loadHilo();
    public void loadSaValue();
    public void storeRd();
    public void storeRt();
    public void storeHilo();
    public void prepareRdForStore();
    public void prepareRtForStore();
    public void prepareHiloForStore();
	public int getRsRegisterIndex();
    public int getRtRegisterIndex();
    public int getRdRegisterIndex();
    public int getSaValue();
    public boolean isRdRegister0();
    public boolean isRtRegister0();
    public boolean isRsRegister0();
    public int getImm16(boolean signedImm);
    public void loadImm(int imm);
    public void loadImm16(boolean signedImm);
    public MethodVisitor getMethodVisitor();
    public void memRead32(int registerIndex, int offset);
    public void memRead16(int registerIndex, int offset);
    public void memRead8(int registerIndex, int offset);
    public void memWrite32(int registerIndex, int offset);
    public void prepareMemWrite32(int registerIndex, int offset);
    public void convertUnsignedIntToLong();
}

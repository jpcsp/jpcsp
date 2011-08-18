package jpcsp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import jpcsp.Allegrex.CpuState;

final public class ParameterReader {
	public CpuState cpu;
	public Memory memory;
	private int parameterIndex = 0;
	private int parameterIndexFloat = 0;
	Charset utf8;
	
	public ParameterReader(CpuState cpu, Memory memory) {
		this.cpu = cpu;
		this.memory = memory;
		this.utf8 = Charset.forName("UTF8");
	}
	
	public void resetReading() {
		parameterIndex = 0;
		parameterIndexFloat = 0;
	}
	
	private int getParameterIntAt(int index) {
		if (index >= 8) {
			return memory.read32(cpu.gpr[29] + (index - 8) * 4);
		} else {
			//System.err.println("getParameterIntAt(" + index + ") :: " + cpu.gpr[4 + index]);
			return cpu.gpr[4 + index];
		}
	}
	
	private float getParameterFloatAt(int index) {
		if (index >= 8) {
			throw(new NotImplementedException());
		} else {
			return cpu.fpr[12 + index];
		}
	}

	private long getParameterLongAt(int index) {
		if ((index % 2) != 0) throw(new RuntimeException("Parameter misalignment"));
		return (long)getParameterIntAt(index) + (long)getParameterIntAt(index + 0) << 32;
	}
	
	private String getParameterStringAt(int index, Charset charset) {
		return new String(getBytez(getParameterIntAt(index)), charset);
	}

	private int moveParameterIndex(int size) {
		while ((parameterIndex % size) != 0) parameterIndex++;
		int retParameterIndex = parameterIndex;
		parameterIndex += size;
		return retParameterIndex;
	}
	
	private int moveParameterIndexFloat(int size) {
		while ((parameterIndexFloat % size) != 0) parameterIndexFloat++;
		int retParameterIndexFloat = parameterIndexFloat;
		parameterIndexFloat += size;
		return retParameterIndexFloat;
	}
	
	protected byte[] getBytez(int addr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(memory.strlen(addr));
		memory.copyToMemory(addr, byteBuffer, byteBuffer.limit());
		return byteBuffer.array();
	}
	
	public int getNextInt() {
		return getParameterIntAt(moveParameterIndex(1));
	}

	public long getNextLong() {
		return getParameterLongAt(moveParameterIndex(2));
	}
	
	public float getNextFloat() {
		return getParameterFloatAt(moveParameterIndexFloat(1));
	}
	
	public String getNextStringUtf8() {
		return getNextString(this.utf8);
	}

	public String getNextString(Charset charset) {
		return getParameterStringAt(moveParameterIndex(1), charset);
	}
	
	public void setReturnValueInt(int value) {
		cpu.gpr[2] = value;
	}

	public void setReturnValueFloat(float value) {
		cpu.fpr[2] = value;
	}

	public void setReturnValueLong(long value) {
		cpu.gpr[2] = (int)((value >>  0) & 0xFFFFFFFF);
		cpu.gpr[3] = (int)((value >> 32) & 0xFFFFFFFF);
	}

	/*
	protected <T> T getNextObject() {
		return null;
	}
	*/
}

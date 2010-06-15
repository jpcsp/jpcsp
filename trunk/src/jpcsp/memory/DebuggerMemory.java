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
package jpcsp.memory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashSet;

import jpcsp.Emulator;
import jpcsp.Memory;

public class DebuggerMemory extends Memory {
	public static boolean traceMemoryRead = false;
	public static boolean traceMemoryWrite = false;
	public static boolean pauseEmulatorOnMemoryBreakpoint = true;
	// List of breakpoints for memory read
	public static int[] readBreakpoints  = { 0x1234567, 0x7654321 };
	// List of breakpoints for memory write
	public static int[] writeBreakpoints = { 0x1234567, 0x7654321 };
    // External breakpoints' list.
    public static String mBrkFilePath = "Memory.mbrk";

	private HashSet<Integer> memoryReadBreakpoint;
	private HashSet<Integer> memoryWriteBreakpoint;
	private Memory mem;

	public DebuggerMemory(Memory mem) {
		this.mem = mem;

		initBreakpoints();
	}

	private void initBreakpoints() {
		memoryReadBreakpoint = new HashSet<Integer>();
		memoryWriteBreakpoint = new HashSet<Integer>();

        try {
            File f = new File(mBrkFilePath);
            BufferedReader in = new BufferedReader(new FileReader(f));

            String nextBrk = in.readLine();
            if(nextBrk.equals("READ")) {
                traceMemoryRead = true;
                traceMemoryWrite = false;
                nextBrk = in.readLine();
            } else if(nextBrk.equals("WRITE")) {
                traceMemoryRead = false;
                traceMemoryWrite = true;
                nextBrk = in.readLine();
            } else if(nextBrk.equals("READ|WRITE")) {
                traceMemoryRead = true;
                traceMemoryWrite = true;
                nextBrk = in.readLine();
            } else {
                traceMemoryRead = false;
                traceMemoryWrite = false;
            }

            int[] memBrkR = new int[(int)f.length()];
            int r = 0;
            int[] memBrkW = new int[(int)f.length()];
            int w = 0;

            while (nextBrk != null) {
                if(nextBrk.charAt(0) == 'R') {
                    memBrkR[r] = Integer.parseInt(nextBrk.substring(3), 16);
                } else if (nextBrk.charAt(0) == 'W') {
                    memBrkW[w] = Integer.parseInt(nextBrk.substring(3), 16);
                }
                nextBrk = in.readLine();
            }

            readBreakpoints = memBrkR;
            writeBreakpoints = memBrkW;

        } catch (Exception e) {
            // Ignore.
        }

		for (int i = 0; readBreakpoints != null && i < readBreakpoints.length; i++) {
			memoryReadBreakpoint.add(readBreakpoints[i]);
		}
		for (int i = 0; writeBreakpoints != null && i < writeBreakpoints.length; i++) {
			memoryWriteBreakpoint.add(writeBreakpoints[i]);
		}
	}

	public static void install() {
		Memory mem = Memory.getInstance();
		if (!(mem instanceof DebuggerMemory)) {
			DebuggerMemory debuggerMemory = new DebuggerMemory(mem);
			Memory.setInstance(debuggerMemory);
		}
	}

	public static void deinstall() {
		Memory mem = Memory.getInstance();
		if (mem instanceof DebuggerMemory) {
			DebuggerMemory debuggerMemory = (DebuggerMemory) mem;
			Memory.setInstance(debuggerMemory.mem);
		}
	}

	protected String getMemoryReadMessage(int address, int width) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("0x%08X - ", Emulator.getProcessor().cpu.pc));
		if (width == 8 || width == 16 || width == 32) {
    		message.append(String.format("read%d(0x%08X)=0x", width, address));
    		if (width == 8) {
    			message.append(String.format("%02X", mem.read8(address)));
    		} else if (width == 16) {
    			message.append(String.format("%04X", mem.read16(address)));
    		} else if (width == 32) {
    			message.append(String.format("%08X", mem.read32(address)));
    		}
		} else {
			int length = width / 8;
			message.append(String.format("read 0x%08X-0x%08X (length=%d)", address, address + length, length));
		}

		return message.toString();
	}

	protected void memoryRead(int address, int width) {
		if (traceMemoryRead && log.isTraceEnabled()) {
    		log.trace(getMemoryReadMessage(address, width));
		}

		if (memoryReadBreakpoint.contains(address)) {
			log.info(getMemoryReadMessage(address, width));
			if (pauseEmulatorOnMemoryBreakpoint) {
				Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);
			}
		}
	}

	protected String getMemoryWriteMessage(int address, int value, int width) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("0x%08X - ", Emulator.getProcessor().cpu.pc));
		message.append(String.format("write%d(0x%08X, 0x", width, address));
		if (width == 8) {
			message.append(String.format("%02X", value & 0xFF));
		} else if (width == 16) {
			message.append(String.format("%04X", value & 0xFFFF));
		} else if (width == 32) {
			message.append(String.format("%08X", value));
		}
		message.append(")");

		return message.toString();
	}

	protected void memoryWrite(int address, int value, int width) {
		if (traceMemoryWrite && log.isTraceEnabled()) {
    		log.trace(getMemoryWriteMessage(address, value, width));
		}

		if (memoryWriteBreakpoint.contains(address)) {
			log.info(getMemoryWriteMessage(address, value, width));
			if (pauseEmulatorOnMemoryBreakpoint) {
				Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);
			}
		}
	}

	@Override
	public void Initialise() {
		mem.Initialise();
	}

	@Override
	public boolean allocate() {
		return mem.allocate();
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		// Perform copyToMemory using write8 to check memory access
		for (int i = 0; i < length && source.hasRemaining(); i++) {
			byte value = source.get();
			write8(address + i, value);
		}
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		memoryRead(address, length * 8);
		return mem.getBuffer(address, length);
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return mem.getMainMemoryByteBuffer();
	}

	@Override
	public boolean isAddressGood(int address) {
		return mem.isAddressGood(address);
	}

	@Override
	public boolean isRawAddressGood(int address) {
		return mem.isRawAddressGood(address);
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		// Perform memcpy using read8/write8 to check memory access
		for (int i = 0; i < length; i++) {
			write8(destination + i, (byte) mem.read8(source + i));
		}
	}

	@Override
	public void memset(int address, byte data, int length) {
		// Perform memset using write8 to check memory access
		for (int i = 0; i < length; i++) {
			write8(address + i, data);
		}
	}

	@Override
	public int read8(int address) {
		memoryRead(address, 8);
		return mem.read8(address);
	}

	@Override
	public int read16(int address) {
		memoryRead(address, 16);
		return mem.read16(address);
	}

	@Override
	public int read32(int address) {
		memoryRead(address, 32);
		return mem.read32(address);
	}

	@Override
	public long read64(int address) {
		// Perform read64 using read32 to check memory access
		long low = read32(address);
		long high = read32(address + 4);
		return low | high << 32;
	}

	@Override
	public void write8(int address, byte data) {
		memoryWrite(address, data, 8);
		mem.write8(address, data);
	}

	@Override
	public void write16(int address, short data) {
		memoryWrite(address, data, 16);
		mem.write16(address, data);
	}

	@Override
	public void write32(int address, int data) {
		memoryWrite(address, data, 32);
		mem.write32(address, data);
	}

	@Override
	public void write64(int address, long data) {
		// Perform write64 using write32 to check memory access
		mem.write32(address, (int) data);
		mem.write32(address + 4, (int) (data >> 32));
	}
}
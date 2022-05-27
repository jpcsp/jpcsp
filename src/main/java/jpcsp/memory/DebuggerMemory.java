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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.MemoryBreakpoints.MemoryBreakpoint;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

public class DebuggerMemory extends Memory {

    public boolean traceMemoryRead = false;
    public boolean traceMemoryWrite = false;
    public boolean traceMemoryRead8 = false;
    public boolean traceMemoryWrite8 = false;
    public boolean traceMemoryRead16 = false;
    public boolean traceMemoryWrite16 = false;
    public boolean traceMemoryRead32 = false;
    public boolean traceMemoryWrite32 = false;
    public boolean pauseEmulatorOnMemoryBreakpoint = false;
    private HashSet<Integer> memoryReadBreakpoint;
    private HashSet<Integer> memoryWriteBreakpoint;
    private List<MemoryBreakpoint> memoryBreakpoints;
    private int lowestReadBreakpointAddress;
    private int highestReadBreakpointAddress;
    private int lowestWriteBreakpointAddress;
    private int highestWriteBreakpointAddress;
    private Memory mem;
    // external breakpoint list
    public static String mBrkFilePath = "Memory.mbrk";

    public DebuggerMemory(Memory mem) {
        this.mem = mem;

        memoryReadBreakpoint = new HashSet<Integer>();
        memoryWriteBreakpoint = new HashSet<Integer>();
        memoryBreakpoints = new LinkedList<MemoryBreakpoint>();

        lowestReadBreakpointAddress = Integer.MAX_VALUE;
        highestReadBreakpointAddress = 0;
        lowestWriteBreakpointAddress = Integer.MAX_VALUE;
        highestWriteBreakpointAddress = 0;

        if (new File(mBrkFilePath).exists()) {
            importBreakpoints(mBrkFilePath);
        }
    }

	public List<MemoryBreakpoint> getMemoryBreakpoints() {
        return memoryBreakpoints;
    }

    private void setBreakpointToken(String token) {
        if (token.isEmpty()) {
            return;
        }
        if (token.equals("read")) {
            traceMemoryRead = true;
        } else if (token.equals("read8")) {
            traceMemoryRead8 = true;
        } else if (token.equals("read16")) {
            traceMemoryRead16 = true;
        } else if (token.equals("read32")) {
            traceMemoryRead32 = true;
        } else if (token.equals("write")) {
            traceMemoryWrite = true;
        } else if (token.equals("write8")) {
            traceMemoryWrite8 = true;
        } else if (token.equals("write16")) {
            traceMemoryWrite16 = true;
        } else if (token.equals("write32")) {
            traceMemoryWrite32 = true;
        } else if (token.equals("pause")) {
            pauseEmulatorOnMemoryBreakpoint = true;
        } else {
            log.error(String.format("Unknown token '%s'", token));
        }
    }

    final public void exportBreakpoints(String filename) {
        exportBreakpoints(new File(filename));
    }

    final public void exportBreakpoints(File f) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(f));

            Iterator<MemoryBreakpoint> it = memoryBreakpoints.iterator();
            while (it.hasNext()) {
                MemoryBreakpoint mbp = it.next();

                String line = "";
                switch (mbp.getAccess()) {
                    case READ:
                        line += "R ";
                        break;
                    case WRITE:
                        line += "W ";
                        break;
                    case READWRITE:
                        line += "RW ";
                        break;
                    default:
                        // ignore - but should never happen
                        continue;
                }

                line += String.format("0x%08X", mbp.getStartAddress());
                if (mbp.getStartAddress() != mbp.getEndAddress()) {
                    line += String.format(" - 0x%08X", mbp.getEndAddress());
                }
                line += System.getProperty("line.separator");

                out.write(line);
            }

            if (traceMemoryRead) {
                out.write("read|");
            }
            if (traceMemoryWrite) {
                out.write("write|");
            }
            if (traceMemoryRead8) {
                out.write("read8|");
            }
            if (traceMemoryWrite8) {
                out.write("write8|");
            }
            if (traceMemoryRead16) {
                out.write("read16|");
            }
            if (traceMemoryWrite16) {
                out.write("write16|");
            }
            if (traceMemoryRead32) {
                out.write("read32|");
            }
            if (traceMemoryWrite32) {
                out.write("write32|");
            }
            if (pauseEmulatorOnMemoryBreakpoint) {
                out.write("pause");
            }

            out.write(System.getProperty("line.separator"));

        } catch (IOException ioe) {
            // ignore
        } finally {
            Utilities.close(out);
        }
    }

    final public void importBreakpoints(String filename) {
        importBreakpoints(new File(filename));
    }

    final public void importBreakpoints(File f) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));

            // reset current configuration
            traceMemoryRead = false;
            traceMemoryWrite = false;
            traceMemoryRead8 = false;
            traceMemoryWrite8 = false;
            traceMemoryRead16 = false;
            traceMemoryWrite16 = false;
            traceMemoryRead32 = false;
            traceMemoryWrite32 = false;
            pauseEmulatorOnMemoryBreakpoint = false;

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                int rangeIndex = line.indexOf("-");
                if (rangeIndex >= 0) {
                    // Range parsing
                    if (line.startsWith("RW ")) {
                        int start = Utilities.parseAddress(line.substring(2, rangeIndex));
                        int end = Utilities.parseAddress(line.substring(rangeIndex + 1));
                        memoryBreakpoints.add(new MemoryBreakpoint(this, start, end, MemoryBreakpoint.AccessType.READWRITE));
                    } else if (line.startsWith("R ")) {
                        int start = Utilities.parseAddress(line.substring(1, rangeIndex));
                        int end = Utilities.parseAddress(line.substring(rangeIndex + 1));
                        memoryBreakpoints.add(new MemoryBreakpoint(this, start, end, MemoryBreakpoint.AccessType.READ));
                    } else if (line.startsWith("W ")) {
                        int start = Utilities.parseAddress(line.substring(1, rangeIndex));
                        int end = Utilities.parseAddress(line.substring(rangeIndex + 1));
                        memoryBreakpoints.add(new MemoryBreakpoint(this, start, end, MemoryBreakpoint.AccessType.WRITE));
                    }
                } else if (line.startsWith("RW ")) {
                    int address = Utilities.parseAddress(line.substring(2));
                    memoryBreakpoints.add(new MemoryBreakpoint(this, address, MemoryBreakpoint.AccessType.READWRITE));
                } else if (line.startsWith("R ")) {
                    int address = Utilities.parseAddress(line.substring(1));
                    memoryBreakpoints.add(new MemoryBreakpoint(this, address, MemoryBreakpoint.AccessType.READ));
                } else if (line.startsWith("W ")) {
                    int address = Utilities.parseAddress(line.substring(1));
                    memoryBreakpoints.add(new MemoryBreakpoint(this, address, MemoryBreakpoint.AccessType.WRITE));
                } else if (!line.startsWith("#")) {
                    String[] tokens = line.split("\\|");
                    for (int i = 0; tokens != null && i < tokens.length; i++) {
                        String token = tokens[i].trim().toLowerCase();
                        setBreakpointToken(token);
                    }
                }
            }
        } catch (IOException ioe) {
            // ignore
        } finally {
            Utilities.close(in);
        }

        log.info(String.format("%d memory breakpoint(s) imported", memoryBreakpoints.size()));
    }

    public static boolean isInstalled() {
    	return Memory.getInstance() instanceof DebuggerMemory;
    }

    public static void install() {
        if (!isInstalled()) {
            log.info("Using DebuggerMemory");
            DebuggerMemory debuggerMemory = new DebuggerMemory(Memory.getInstance());
            Memory.setInstance(debuggerMemory);
            RuntimeContext.updateMemory();
        }
    }

    public static void deinstall() {
        if (isInstalled()) {
            DebuggerMemory debuggerMemory = (DebuggerMemory) Memory.getInstance();
            Memory.setInstance(debuggerMemory.mem);
        }
    }

    public void addReadBreakpoint(int address) {
        address &= Memory.addressMask;
        if (address < lowestReadBreakpointAddress) {
        	lowestReadBreakpointAddress = address;
        }
        if (address > highestReadBreakpointAddress) {
        	highestReadBreakpointAddress = address;
        }
        memoryReadBreakpoint.add(address);
    }

    public void removeReadBreakpoint(int address) {
        address &= Memory.addressMask;
        memoryReadBreakpoint.remove(address);
    }

    public void addRangeReadBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            addReadBreakpoint(address);
        }
    }

    public void removeRangeReadBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            removeReadBreakpoint(address);
        }
    }

    public void addWriteBreakpoint(int address) {
        address &= Memory.addressMask;
        if (address < lowestWriteBreakpointAddress) {
        	lowestWriteBreakpointAddress = address;
        }
        if (address > highestWriteBreakpointAddress) {
        	highestWriteBreakpointAddress = address;
        }
        memoryWriteBreakpoint.add(address);
    }

    public void removeWriteBreakpoint(int address) {
        address &= Memory.addressMask;
        memoryWriteBreakpoint.remove(address);
    }

    public void addRangeWriteBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            addWriteBreakpoint(address);
        }
    }

    public void removeRangeWriteBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            removeWriteBreakpoint(address);
        }
    }

    public void addReadWriteBreakpoint(int address) {
        addReadBreakpoint(address);
        addWriteBreakpoint(address);
    }

    public void removeReadWriteBreakpoint(int address) {
    	removeReadBreakpoint(address);
    	removeWriteBreakpoint(address);
    }

    public void addRangeReadWriteBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            addReadWriteBreakpoint(address);
        }
    }

    public void removeRangeReadWriteBreakpoint(int start, int end) {
        for (int address = start; address <= end; address++) {
            removeReadWriteBreakpoint(address);
        }
    }

    protected String getMemoryReadMessage(int address, int value, int width) {
        StringBuilder message = new StringBuilder();

        Processor processor = getProcessor();
        if (processor != null) {
        	message.append(String.format("0x%08X - ", processor.cpu.pc));
        }

        if (width == 8 || width == 16 || width == 32) {
            message.append(String.format("read%d(0x%08X)=0x", width, address));
            if (width == 8) {
                message.append(String.format("%02X", value));
            } else if (width == 16) {
                message.append(String.format("%04X", value));
            } else if (width == 32) {
                //message.append(String.format("%08X (%f)", value, Float.intBitsToFloat(value)));
                message.append(String.format("%08X", value));
            }
        } else {
            int length = width / 8;
            message.append(String.format("read 0x%08X-0x%08X (length=%d)", address, address + length, length));
        }

        return message.toString();
    }

    protected void memoryRead(int address, int value, int width, boolean trace) {
        if ((traceMemoryRead || trace) && log.isTraceEnabled()) {
            log.trace(getMemoryReadMessage(address, value, width));
        }

        if ((pauseEmulatorOnMemoryBreakpoint || log.isInfoEnabled()) && memoryReadBreakpoint.contains(address & Memory.addressMask)) {
            log.info(getMemoryReadMessage(address, value, width));
            if (pauseEmulatorOnMemoryBreakpoint) {
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);
            }
        }
    }

    protected String getMemoryWriteMessage(int address, int value, int width) {
        StringBuilder message = new StringBuilder();

        Processor processor = getProcessor();
        if (processor != null) {
        	message.append(String.format("0x%08X - ", processor.cpu.pc));
        }

        message.append(String.format("write%d(0x%08X, 0x", width, address));
        if (width == 8) {
            message.append(String.format("%02X", value & 0xFF));
        } else if (width == 16) {
            message.append(String.format("%04X", value & 0xFFFF));
        } else if (width == 32) {
            //message.append(String.format("%08X (%f)", value, Float.intBitsToFloat(value)));
            message.append(String.format("%08X", value));
        }
        message.append(")");

        return message.toString();
    }

    protected void memoryWrite(int address, int value, int width, boolean trace) {
        if ((traceMemoryWrite || trace) && log.isTraceEnabled()) {
            log.trace(getMemoryWriteMessage(address, value, width));
        }

        if ((pauseEmulatorOnMemoryBreakpoint || log.isInfoEnabled()) && memoryWriteBreakpoint.contains(address & Memory.addressMask)) {
            log.info(getMemoryWriteMessage(address, value, width));
            if (pauseEmulatorOnMemoryBreakpoint) {
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_BREAKPOINT);
            }
        }
    }

    private boolean isCheckingMemoryWriteAccess(int address, int length, boolean trace) {
        if ((traceMemoryWrite || trace) && log.isTraceEnabled()) {
        	return true;
        }

        address &= Memory.addressMask;

        // Quick check using the lowest and highest values
        if (address > highestWriteBreakpointAddress || address + length < lowestWriteBreakpointAddress) {
        	return false;
        }

        // Check if we have a write breakpoint in the address range
        for (int i = 0; i < length; i++, address++) {
	        if (memoryWriteBreakpoint.contains(address)) {
	        	return true;
	        }
        }

        return false;
    }

    private boolean isCheckingMemoryReadAccess(int address, int length, boolean trace) {
        if ((traceMemoryRead || trace) && log.isTraceEnabled()) {
        	return true;
        }

        address &= Memory.addressMask;

        // Quick check using the lowest and highest values
        if (address > highestReadBreakpointAddress || address + length < lowestReadBreakpointAddress) {
        	return false;
        }

        // Check if we have a read breakpoint in the address range
        for (int i = 0; i < length; i++, address++) {
	        if (memoryReadBreakpoint.contains(address)) {
	        	return true;
	        }
        }

        return false;
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
    	if (isCheckingMemoryWriteAccess(address, length, traceMemoryWrite8)) {
	        // Perform copyToMemory using write8 to check memory access
	        for (int i = 0; i < length && source.hasRemaining(); i++) {
	            byte value = source.get();
	            write8(address + i, value);
	        }
    	} else {
    		mem.copyToMemory(address, source, length);
    	}
    }

    @Override
    public Buffer getBuffer(int address, int length) {
        memoryRead(address, 0, length * 8, false);
        return mem.getBuffer(address, length);
    }

    @Override
    public Buffer getMainMemoryByteBuffer() {
        return mem.getMainMemoryByteBuffer();
    }

    @Override
    protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
    	destination = normalize(destination);
		source = normalize(source);

		if (isCheckingMemoryWriteAccess(destination, length, traceMemoryWrite8) || isCheckingMemoryReadAccess(source, length, traceMemoryRead8)) {
			// Overlapping address ranges must be correctly handled:
			//   If source >= destination:
			//                 [---source---]
			//       [---destination---]
			//      => Copy from the head
			//   If source < destination:
			//       [---source---]
			//                 [---destination---]
			//      => Copy from the tail
			//
	    	if (!checkOverlap || source >= destination || !areOverlapping(destination, source, length)) {
	    		// Perform memcpy using read8/write8 to check memory access
	            for (int i = 0; i < length; i++) {
	                write8(destination + i, (byte) read8(source + i));
	            }
	    	} else {
	    		// Perform memcpy using read8/write8 to check memory access
				for (int i = length - 1; i >= 0; i--) {
					write8(destination + i, (byte) read8(source + i));
				}
	    	}
		} else if (checkOverlap) {
			mem.memmove(destination, source, length);
		} else {
			mem.memcpy(destination, source, length);
		}
    }

    @Override
    public void memset(int address, byte data, int length) {
    	if (isCheckingMemoryWriteAccess(address, length, traceMemoryWrite8)) {
	        // Perform memset using write8 to check memory access
	        for (int i = 0; i < length; i++) {
	            write8(address + i, data);
	        }
    	} else {
    		mem.memset(address, data, length);
    	}
    }

    @Override
    public int read8(int address) {
    	int value = mem.read8(address);
        memoryRead(address, value, 8, traceMemoryRead8);
        return value;
    }

    @Override
    public int read16(int address) {
    	int value = mem.read16(address);
        memoryRead(address, value, 16, traceMemoryRead16);
        return value;
    }

    @Override
    public int read32(int address) {
    	int value = mem.read32(address);
        memoryRead(address, value, 32, traceMemoryRead32);
        return value;
    }

    @Override
    public int internalRead8(int address) {
    	int value = mem.read8(address);
    	// No tracing
        return value;
    }

    @Override
    public int internalRead16(int address) {
    	int value = mem.read16(address);
    	// No tracing
        return value;
    }

    @Override
    public int internalRead32(int address) {
    	int value = mem.read32(address);
    	// No tracing
        return value;
    }

    @Override
    public void write8(int address, byte data) {
        memoryWrite(address, data, 8, traceMemoryWrite8);
        mem.write8(address, data);
    }

    @Override
    public void write16(int address, short data) {
        memoryWrite(address, data, 16, traceMemoryWrite16);
        mem.write16(address, data);
    }

    @Override
    public void write32(int address, int data) {
        memoryWrite(address, data, 32, traceMemoryWrite32);
        mem.write32(address, data);
    }

	@Override
	public void setIgnoreInvalidMemoryAccess(boolean ignoreInvalidMemoryAccess) {
		super.setIgnoreInvalidMemoryAccess(ignoreInvalidMemoryAccess);
		if (mem != null) {
			mem.setIgnoreInvalidMemoryAccess(ignoreInvalidMemoryAccess);
		}
	}

	@Override
	public void remapMemoryAtProcessorReset() {
		mem.remapMemoryAtProcessorReset();
	}

	@Override
	public void reset() {
		mem.reset();
	}

	@Override
	public int normalize(int address) {
		return mem.normalize(address);
	}

	public Memory getDebuggedMemory() {
		return mem;
	}

    @Override
	public void read(StateInputStream stream) throws IOException {
		mem.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		mem.write(stream);
	}
}

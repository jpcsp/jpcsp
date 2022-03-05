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

assumes:
- list contains fbw/fbp command
- list clears the screen
- sceDisplaySetFrameBuf is called after the list has executed

todo:
- need to save GE state
  - texture on/off
  - blend on/off + params.
  - save matrices, looks like something wrong with projection
  - more ...
- don't save the same piece of ram twice (multiple texture uploads of the same texture/clut)
- capture multiple lists per frame, ideally we want to capture everything between two calls to sceDisplaySetFrameBuf
*/

package jpcsp.graphics.capture;

import static jpcsp.graphics.GeCommands.CALL;
import static jpcsp.graphics.GeCommands.END;
import static jpcsp.graphics.GeCommands.JUMP;
import static jpcsp.graphics.GeCommands.RET;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.graphics.VideoEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class CaptureManager {
	public static Logger log = VideoEngine.log;

	private static final int MAGIC = 0x5245504C; // REPL
    private static final int CURRENT_VERSION = 1;
    public static boolean captureInProgress;
    protected static int version;
    private static DataOutputStream out;
    private static boolean listExecuted;
    private static CaptureFrameBufDetails replayFrameBufDetails;
    private static Level logLevel;
    private static HashSet<Integer> capturedImages;
    private static Map<Integer, Integer> capturedAddresses;

    public static void startReplay(Memory mem, String filename) {
        if (captureInProgress) {
            log.error("Ignoring startReplay, capture is in progress");
            return;
        }

        log.info(String.format("Starting replay '%s'", filename));

        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));

            // Read the version of the replay file format
            int magic = in.readInt();
            if (magic != MAGIC) {
            	log.error(String.format("Not a replay file"));
            	in.close();
            	return;
            }

            version = in.readInt();
            if (version > CURRENT_VERSION) {
            	log.error(String.format("Unsupported replay file version 0x%X", version));
            	in.close();
            	return;
            }

            while (in.available() > 0) {
                CaptureHeader header = CaptureHeader.read(in);
                int packetType = header.getPacketType();

                switch(packetType) {
                    case CaptureHeader.PACKET_TYPE_LIST:
                        CaptureList list = CaptureList.read(in);
                        list.commit(mem);
                        break;

                    case CaptureHeader.PACKET_TYPE_RAM:
                        CaptureRAM ramFragment = CaptureRAM.read(in);
                        ramFragment.commit(mem);
                        break;

                    case CaptureHeader.PACKET_TYPE_FRAMEBUF_DETAILS:
                        // don't replay this one immediately, wait until after the list has finished executing
                        replayFrameBufDetails = CaptureFrameBufDetails.read(in);
                        break;

                    default:
                        throw new IOException(String.format("Unknown packet type %d", packetType));
                }
            }

            in.close();
        } catch(IOException e) {
            log.error("Failed to start replay", e);
        }
    }

    public static void endReplay() {
        // replay final sceDisplaySetFrameBuf
        replayFrameBufDetails.commit();
        replayFrameBufDetails = null;

        log.info("Replay completed");
        Emulator.PauseEmu();
    }

    public static void startCapture(Memory mem, String filename, PspGeList list) {
        if (captureInProgress) {
            log.error("Ignoring startCapture, capture is already in progress");
            return;
        }

        // Set the VideoEngine log level to TRACE when capturing,
        // the information in the log file is also interesting
        logLevel = log.getLevel();
        VideoEngine.getInstance().setLogLevel(Level.TRACE);
        capturedImages = new HashSet<Integer>();
        capturedAddresses = new HashMap<Integer, Integer>();

        try {
            log.info(String.format("Starting capture... (list=0x%X)", list.id));
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));

            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);

            // write command buffer
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_LIST);
            header.write(out);
            CaptureList commandList = new CaptureList(mem, list);
            commandList.write(out);

            captureInProgress = true;
            listExecuted = false;
        } catch(IOException e) {
            log.error("Failed to start capture", e);
            Emulator.PauseEmu();
        }
    }

    public static void endCapture() {
        if (!captureInProgress) {
            log.warn("Ignoring endCapture, capture hasn't been started");
            Emulator.PauseEmu();
            return;
        }

        try {
            out.flush();
            out.close();
            out = null;
        } catch(IOException e) {
            log.error("Failed to end capture", e);
            Emulator.PauseEmu();
        }

        capturedAddresses = null;
        capturedImages = null;
        captureInProgress = false;

        log.info("Capture completed");
        log.setLevel(logLevel);
        Emulator.PauseEmu();
    }

    protected static int getListCmdsLength(int address, int stall) {
    	IMemoryReader memoryReader;
    	if (stall == 0) {
    		memoryReader = MemoryReader.getMemoryReader(address, 4);
    	} else {
    		memoryReader = MemoryReader.getMemoryReader(address, stall - address, 4);
    	}

    	while (memoryReader.getCurrentAddress() != stall) {
    		int instruction = memoryReader.readNext();
    		int command = VideoEngine.command(instruction);
    		if (command == END || command == JUMP || command == RET || command == CALL) {
    			break;
    		}
    	}

    	int length = memoryReader.getCurrentAddress() - address;

    	return length;
    }

    public static void captureList(Memory mem, PspGeList list) {
    	captureList(mem, list.getPc(), list.getStallAddr());
    }

    public static void captureList(Memory mem, int address, int stall) {
        if (!captureInProgress) {
            log.warn("Ignoring captureList, capture hasn't been started");
            return;
        }

        int length = getListCmdsLength(address, stall);
        if (log.isDebugEnabled()) {
        	log.debug(String.format("captureList pc=0x%08X, stall=0x%08X, length=0x%X", address, stall, length));
        }
    	captureRAM(mem, address, length);
    }

    private static boolean isAlreadyCaptured(int address, int length) {
        Integer capturedLength = capturedAddresses.get(address);
        return capturedLength != null && capturedLength.intValue() >= length;
    }

    public static void captureRAM(Memory mem, int address, int length) {
        if (!captureInProgress) {
            log.warn("Ignoring captureRAM, capture hasn't been started");
            return;
        }

        if (!Memory.isAddressGood(address) || length <= 0) {
        	return;
        }

        if (isAlreadyCaptured(address, length)) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("captureRAM already captured address=0x%08X, length=0x%X", address, length));
        	}
        	return;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("captureRAM address=0x%08X, length=0x%X", address, length));
        }

        try {
            // write ram fragment
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
            header.write(out);

            CaptureRAM captureRAM = new CaptureRAM(mem, address, length);
            captureRAM.write(out);

            capturedAddresses.put(address, length);
        } catch (IOException e) {
            log.error("Failed to capture RAM", e);
        }
    }

    public static void captureImage(int imageaddr, int level, Buffer buffer, int width, int height, int bufferWidth, int imageType, boolean compressedImage, int compressedImageSize, boolean invert, boolean overwriteFile) {
        try {
            // write image to the file system, not to the capture file itself
            CaptureImage captureImage = new CaptureImage(imageaddr, level, buffer, width, height, bufferWidth, imageType, compressedImage, compressedImageSize, invert, overwriteFile, null);
            captureImage.write();
            if (capturedImages != null) {
            	capturedImages.add(imageaddr);
            }
        } catch (IOException e) {
            log.error("Failed to capture Image", e);
            Emulator.PauseEmu();
        }
    }

    public static boolean isImageCaptured(int imageaddr) {
    	if (capturedImages == null) {
    		return false;
    	}

    	return capturedImages.contains(imageaddr);
    }

    public static void captureFrameBufDetails() {
        if (!captureInProgress) {
            log.warn("Ignoring captureFrameBufDetails, capture hasn't been started");
            return;
        }

        try {
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_FRAMEBUF_DETAILS);
            header.write(out);

            CaptureFrameBufDetails details = new CaptureFrameBufDetails();
            details.write(out);
        } catch(IOException e) {
            log.error("Failed to capture frame buf details", e);
            Emulator.PauseEmu();
        }
    }

    public static void markListExecuted() {
        listExecuted = true;
    }

    public static boolean hasListExecuted() {
        return listExecuted;
    }
}

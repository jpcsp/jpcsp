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
import static jpcsp.util.Utilities.MB;

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
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

import org.apache.log4j.Logger;

public class CaptureManager {
	public static Logger log = VideoEngine.log;

	public static final String replayFileName = "record.bin";
	private static final int MAGIC = 0x5245504C; // "REPL"
    private static final int CURRENT_VERSION = 1;
    private static final int BUFFER_SIZE = 1 * MB;
    public static final int PACKET_TYPE_RESERVED = 0;
    public static final int PACKET_TYPE_START_LIST = 1;
    public static final int PACKET_TYPE_RAM = 2;
    public static final int PACKET_TYPE_FRAME_BUFFER_DETAILS = 3;
    public static final int PACKET_TYPE_GE_DETAILS = 4;
    public static final int PACKET_TYPE_GE_CONTEXT = 5;
    public static boolean captureInProgress;
    protected static int version;
    private static DataOutputStream out;
    private static DataInputStream in;
    private static CaptureList list;
    private static boolean listExecuted;
    private static CaptureFrameBufDetails replayFrameBufDetails;
    private static CaptureGEDetails replayGEDetails;
    private static CaptureGeContext replayGeContext;
    private static HashSet<Integer> capturedImages;
    private static Map<Integer, Integer> capturedAddresses;

    private static boolean processReplayPacket(Memory mem, DataInputStream in) throws IOException {
    	if (in.available() <= 0) {
    		return true;
    	}

        boolean endOfList = false;
        int packetType = in.readInt();
        switch (packetType) {
            case PACKET_TYPE_START_LIST:
                list = CaptureList.read(in);
                break;

            case PACKET_TYPE_RAM:
                CaptureRAM ramFragment = CaptureRAM.read(in);
                ramFragment.commit(mem);
                break;

            case PACKET_TYPE_GE_DETAILS:
                // don't replay this one immediately, wait until after the list has finished executing
            	replayGEDetails = CaptureGEDetails.read(in);
            	endOfList = replayGEDetails.isEndOfList();
            	break;

            case PACKET_TYPE_FRAME_BUFFER_DETAILS:
                // don't replay this one immediately, wait until after the list has finished executing
                replayFrameBufDetails = CaptureFrameBufDetails.read(in);
                break;

            case PACKET_TYPE_GE_CONTEXT:
            	replayGeContext = CaptureGeContext.read(in);
            	break;

            default:
                throw new IOException(String.format("Unknown packet type 0x%08X", packetType));
        }

        return endOfList;
    }

    private static boolean processReplayHeader(Memory mem, DataInputStream in) throws IOException {
        // Read the version of the replay file format
        int magic = in.readInt();
        if (magic != MAGIC) {
        	log.error(String.format("Not a replay file"));
        	return false;
        }

        version = in.readInt();
        if (version > CURRENT_VERSION) {
        	log.error(String.format("Unsupported replay file version 0x%X", version));
        	return false;
        }

        return true;
    }

    public static synchronized boolean startRecordReplay(Memory mem, String filename) {
        if (captureInProgress) {
            log.error("Ignoring startRecordReplay, capture is in progress");
            return false;
        }

        log.info(String.format("Starting replay '%s'", filename));

    	boolean continueReplay = false;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename), BUFFER_SIZE));

            if (!processReplayHeader(mem, in)) {
            	in.close();
            	in = null;
            	return false;
            }

            list = null;
            continueReplay = continueRecordReplay(mem);
        } catch(IOException e) {
            log.error("Failed to start replay", e);
        }

        return continueReplay;
    }

    public static synchronized boolean continueRecordReplay(Memory mem) {
    	boolean continueReplay = false;

    	log.debug("continueRecordReplay");
        try {
            while (!processReplayPacket(mem, in)) {
            	// Loop until end of GE list
            }

            if (list != null) {
            	list.commit(mem);
            	list = null;
            }

            continueReplay = in != null && in.available() > 0;
        } catch(IOException e) {
            log.error("Failed to continue replay", e);
        }

        return continueReplay;
    }

    public static synchronized void startListReplay(IRenderingEngine re, GeContext context) {
    	if (replayGeContext != null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("startListReplay %s", replayGeContext));
    		}
    		replayGeContext.commit(re, context);
    		replayGeContext = null;
    	}
    }

    public static synchronized void endListReplay() {
    	if (replayGEDetails != null) {
    		replayGEDetails.commit();
    		replayGEDetails = null;
    	}

    	// replay final sceDisplaySetFrameBuf
    	if (replayFrameBufDetails != null) {
    		replayFrameBufDetails.commit();
    		replayFrameBufDetails = null;
    	}

        log.debug("Replay List completed");
        Emulator.PauseEmu();
    }

    private static void startGeContextCapture(GeContext context) {
    	try {
        	CaptureGeContext captureGeContext = new CaptureGeContext(context);
			captureGeContext.write(out);
		} catch (IOException e) {
            log.error("Failed to capture GE Context", e);
            Emulator.PauseEmu();
		}
    }

    public static synchronized void startCapture(Memory mem, String filename, PspGeList list) {
        if (captureInProgress) {
            log.error("Ignoring startCapture, capture is already in progress");
            return;
        }

        capturedImages = new HashSet<Integer>();
        capturedAddresses = new HashMap<Integer, Integer>();

        try {
            log.info(String.format("Starting capture to '%s'...", filename));
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename), BUFFER_SIZE));

            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);

            captureInProgress = true;

            startGeContextCapture(VideoEngine.getInstance().getContext());

            startListCapture(mem, list);
        } catch(IOException e) {
            log.error("Failed to start capture", e);
            Emulator.PauseEmu();
        }
    }

    public static synchronized void startListCapture(Memory mem, PspGeList list) {
        capturedImages = new HashSet<Integer>();
        capturedAddresses = new HashMap<Integer, Integer>();

        try {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Starting capture list id=0x%X", list.id));
        	}

            // write command buffer
            CaptureList commandList = new CaptureList(mem, list);
            commandList.write(out);

            captureList(mem, list.list_addr, list.getStallAddr());

            listExecuted = false;
        } catch(IOException e) {
            log.error("Failed to start list capture", e);
            Emulator.PauseEmu();
        }
    }

    public static synchronized void endCapture() {
        if (!captureInProgress) {
            log.error("Ignoring endCapture, capture hasn't been started");
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

    public static synchronized void captureList(Memory mem, PspGeList list) {
    	captureList(mem, list.getPc(), list.getStallAddr());
    }

    public static synchronized void captureList(Memory mem, int address, int stall) {
        if (!captureInProgress) {
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

    public static synchronized void captureRAM(Memory mem, int address, int length) {
        if (!captureInProgress) {
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
            CaptureRAM captureRAM = new CaptureRAM(mem, address, length);
            captureRAM.write(out);

            capturedAddresses.put(address, length);
        } catch (IOException e) {
            log.error("Failed to capture RAM", e);
        }
    }

    public static void dumpImage(int imageaddr, int level, Buffer buffer, int width, int height, int bufferWidth, int imageType, boolean compressedImage, int compressedImageSize, boolean invert, boolean overwriteFile) {
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

    public static synchronized void captureFrameBufDetails() {
        if (!captureInProgress) {
            return;
        }

        try {
            CaptureFrameBufDetails details = new CaptureFrameBufDetails();
            details.write(out);
        } catch (IOException e) {
            log.error("Failed to capture frame buf details", e);
            Emulator.PauseEmu();
        }
    }

    public static synchronized void captureGEDetails(boolean endOfList) {
    	if (!captureInProgress) {
    		return;
    	}

    	try {
    		CaptureGEDetails geDetails = new CaptureGEDetails(endOfList);
    		geDetails.write(out);
    	} catch (IOException e) {
            log.error("Failed to capture GE details", e);
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

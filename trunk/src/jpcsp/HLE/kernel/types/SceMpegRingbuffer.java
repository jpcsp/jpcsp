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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class SceMpegRingbuffer extends pspAbstractMemoryMappedStructure {
	public static final int ringbufferPacketSize = 2048;
    // PSP info
    private int packets;
    private int packetsRead;
    private int packetsWritten;
    private int packetsInRingbuffer;
    private int packetSize; // 2048
    private int data; // address, ring buffer
    private int callbackAddr; // see sceMpegRingbufferPut
    private int callbackArgs;
    private int dataUpperBound;
    private int semaID; // unused?
    private int mpeg; // pointer to mpeg struct, fixed up in sceMpegCreate
    // Internal info
    private pspFileBuffer videoBuffer;
    private pspFileBuffer audioBuffer;
    private boolean hasAudio = false;
    private boolean hasVideo = true;
    private int writePacketOffset;
    private int internalPacketsInRingbuffer;

    public SceMpegRingbuffer(int packets, int data, int size, int callbackAddr, int callbackArgs) {
        this.packets = packets;
        packetSize = ringbufferPacketSize;
        this.data = data;
        this.callbackAddr = callbackAddr;
        this.callbackArgs = callbackArgs;
        dataUpperBound = data + packets * ringbufferPacketSize;
        semaID = -1;
        mpeg = 0;
        initBuffer();

        if (dataUpperBound > data + size) {
            dataUpperBound = data + size;
            Modules.log.warn("SceMpegRingbuffer clamping dataUpperBound to " + dataUpperBound);
        }

        reset();
    }

    private SceMpegRingbuffer() {
    }

    private void initBuffer() {
        videoBuffer = new pspFileBuffer(data, dataUpperBound - data);
        audioBuffer = new pspFileBuffer(data, dataUpperBound - data);
        // No check on file size for MPEG.
        videoBuffer.setFileMaxSize(Integer.MAX_VALUE);
        audioBuffer.setFileMaxSize(Integer.MAX_VALUE);

        writePacketOffset = 0;
    }

    public static SceMpegRingbuffer fromMem(TPointer address) {
        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer();
        ringbuffer.read(address);
        ringbuffer.initBuffer();

        return ringbuffer;
    }

    public void reset() {
    	packetsRead = 0;
    	packetsWritten = 0;
    	packetsInRingbuffer = 0;
    	internalPacketsInRingbuffer = 0;
    	writePacketOffset = 0;
		videoBuffer.reset(0, 0);
		audioBuffer.reset(0, 0);
    }

	@Override
	protected void read() {
        packets             = read32();
        packetsRead         = read32();
        packetsWritten      = read32();
        packetsInRingbuffer = read32();
        packetSize          = read32();
        data                = read32();
        callbackAddr        = read32();
        callbackArgs        = read32();
        dataUpperBound      = read32();
        semaID              = read32();
        mpeg                = read32();
	}

	@Override
	protected void write() {
        write32(packets);
        write32(packetsRead);
        write32(packetsWritten);
        write32(packetsInRingbuffer);
        write32(packetSize);
        write32(data);
        write32(callbackAddr);
        write32(callbackArgs);
        write32(dataUpperBound);
        write32(semaID);
        write32(mpeg);
	}

	public int getFreePackets() {
		return packets - getPacketsInRingbuffer();
	}

	public void addPackets(int packetsAdded) {
		videoBuffer.notifyWrite(packetsAdded * packetSize);
		audioBuffer.notifyWrite(packetsAdded * packetSize);
		packetsRead += packetsAdded;
		packetsWritten += packetsAdded;
		packetsInRingbuffer += packetsAdded;
		internalPacketsInRingbuffer += packetsAdded;

		writePacketOffset += packetsAdded;
		if (writePacketOffset >= packets) {
			// Wrap around
			writePacketOffset -= packets;
		}
	}

	public void consumeAllPackets() {
		videoBuffer.notifyReadAll();
		audioBuffer.notifyReadAll();
		packetsInRingbuffer = 0;
		internalPacketsInRingbuffer = 0;
	}

	public int getPacketsInRingbuffer() {
		return packetsInRingbuffer;
	}

    public boolean isEmpty() {
    	return getPacketsInRingbuffer() == 0;
    }

	public int getReadPackets() {
		return packetsRead;
	}

	public boolean hasReadPackets() {
		return packetsRead != 0;
	}

	public void setReadPackets(int packetsRead) {
		this.packetsRead = packetsRead;
	}

	public int getTotalPackets() {
		return packets;
	}

	public int getPacketSize() {
		return packetSize;
	}

	public int getPutDataAddr() {
		return data + writePacketOffset * packetSize;
	}

	public int getPutSequentialPackets() {
		return Math.min(getFreePackets(), packets - writePacketOffset);
	}

	public int getTmpAddress(int length) {
		return dataUpperBound - length;
	}

	public void setMpeg(int mpeg) {
		this.mpeg = mpeg;
	}

	public int getUpperDataAddr() {
		return dataUpperBound;
	}

	public int getCallbackAddr() {
		return callbackAddr;
	}

	public int getCallbackArgs() {
		return callbackArgs;
	}

	public pspFileBuffer getAudioBuffer() {
		return audioBuffer;
	}

	public pspFileBuffer getVideoBuffer() {
		return videoBuffer;
	}

	public void notifyRead() {
		int remainingLength = 0;
		if (hasAudio()) {
			remainingLength = Math.max(remainingLength, audioBuffer.getCurrentSize());
		}
		if (hasVideo()) {
			remainingLength = Math.max(remainingLength, videoBuffer.getCurrentSize());
		}
		int remainingPackets = (remainingLength + packetSize - 1) / packetSize;

		if (internalPacketsInRingbuffer > remainingPackets) {
			internalPacketsInRingbuffer = remainingPackets;
		}
	}

	public void notifyConsumed() {
		packetsInRingbuffer = internalPacketsInRingbuffer;
	}

	public boolean hasAudio() {
		return hasAudio;
	}

	public void setHasAudio(boolean hasAudio) {
		this.hasAudio = hasAudio;
	}

	public boolean hasVideo() {
		return hasVideo;
	}

	public void setHasVideo(boolean hasVideo) {
		this.hasVideo = hasVideo;
	}

	@Override
	public int sizeof() {
		return 44;
	}

	@Override
	public String toString() {
		return String.format("SceMpegRingbuffer(packets=0x%X, packetsRead=0x%X, packetsWritten=0x%X, packetsFree=0x%X, packetSize=0x%X, hasVideo=%b, videoBuffer=%s, hasAudio=%b, audioBuffer=%s)", packets, packetsRead, packetsWritten, getFreePackets(), packetSize, hasVideo, videoBuffer, hasAudio, audioBuffer);
	}
}

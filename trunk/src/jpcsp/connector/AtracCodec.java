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
package jpcsp.connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;

import jpcsp.Memory;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.sceAtrac3plus;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Hash;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class AtracCodec {
	protected String id;
	protected static final String atracSuffix = ".at3";
	protected static final String decodedSuffix = ".decoded";
	protected static final String decodedAtracSuffix = atracSuffix + decodedSuffix;
	protected RandomAccessFile decodedStream;
	protected OutputStream atracStream;
	protected int atracEnd;
	protected int atracRemainFrames;
	protected int atracEndSample;
	protected byte[] atracDecodeBuffer;
	protected static boolean instructionsDisplayed = false;
	protected static boolean commandFileDirty = true;
	public static int waveFactChunkHeader = 0x74636166; // "fact"
	public static int waveDataChunkHeader = 0x61746164; // "data"

    // Media Engine based playback.
    protected static final int STREAM_SEEK_FLAG_FRAME = 8;
    protected MediaEngine me;
    protected PacketChannel atracChannel;
    protected int memBufOffset;
    protected int currentLoopCount;
    protected static boolean useMediaEngine = false;
    protected byte[] samplesBuffer;

    public boolean checkMediaEngineState() {
    	return useMediaEngine && me != null;
    }

    public static boolean useMediaEngine() {
    	return useMediaEngine;
    }

    public static void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

	public AtracCodec() {
        if (useMediaEngine()) {
            me = new MediaEngine();
            me.setAudioSamplesSize(sceAtrac3plus.maxSamples);
            atracChannel = new PacketChannel();
            memBufOffset = 0;
            currentLoopCount = 0;
        }

		atracDecodeBuffer = new byte[sceAtrac3plus.maxSamples * 4];
		samplesBuffer = new byte[sceAtrac3plus.maxSamples * 4];
		generateCommandFile();
	}

	protected String generateID(int address, int length, int fileSize) {
		int hashCode = Hash.getHashCodeFloatingMemory(0, address, length);

		return String.format("Atrac-%08X-%08X", fileSize, hashCode);
	}

	protected static String getBaseDirectory(String id) {
		return String.format("%s%s/Atrac/", Connector.baseDirectory, State.discId);
	}

	protected String getCompleteFileName(String suffix) {
		String completeFileName = String.format("%s%s%s", getBaseDirectory(id), id, suffix);

		return completeFileName;
	}

	protected void generateCommandFile() {
		if (!commandFileDirty) {
			return;
		}

		try {
			// Generate decode commands for all the non-decoded Atrac files
			String baseDirectory = getBaseDirectory(id);
			File directory = new File(baseDirectory);
			String[] files = directory.list();
			HashSet<String> atracFiles = new HashSet<String>();
			HashSet<String> decodedFiles = new HashSet<String>();
			for (int i = 0; files != null && i < files.length; i++) {
				String fileName = files[i];
				if (fileName.endsWith(atracSuffix)) {
					atracFiles.add(fileName);
				} else if (fileName.endsWith(decodedAtracSuffix)) {
					decodedFiles.add(fileName);
				}
			}

			PrintWriter command = new PrintWriter(String.format("%s%s", baseDirectory, Connector.commandFileName));
			for (String atracFileName : atracFiles) {
				if (!decodedFiles.contains(atracFileName + decodedSuffix)) {
					// File not yet decoded, add it to the command file
					command.println("DecodeAtrac3");
					command.println(Connector.basePSPDirectory + atracFileName);
				}
			}
			command.println("Exit");
			command.close();
			commandFileDirty = false;
		} catch (FileNotFoundException e) {
		}
	}

	protected void closeStreams() {
		if (decodedStream != null) {
			try {
				decodedStream.close();
			} catch (IOException e) {
			}
			decodedStream = null;
		}
		if (atracStream != null) {
			try {
				atracStream.close();
			} catch (IOException e) {
			}
			atracStream = null;
		}
	}

	public void atracSetData(int atracID, int codecType, int address, int length, int atracFileSize, boolean extract) {
		id = generateID(address, length, atracFileSize);
		closeStreams();
		atracEndSample = -1;
		atracRemainFrames = 1;

        if(codecType == 0x00001001) {
            Modules.log.info("Decodable AT3 data detected.");
            if(checkMediaEngineState()) {
                atracChannel.clear();
                me.finish();
                atracChannel.write(address, length);
                me.init(atracChannel, false, true);
                memBufOffset = 0;
                atracEndSample = 0;
                return;
            }
        } else if(codecType == 0x00001000) {
            Modules.log.info("Undecodable AT3+ data detected.");
        }
        me = null;

		File decodedFile = new File(getCompleteFileName(decodedAtracSuffix));

		if (!decodedFile.canRead()) {
			// Try to read the decoded file using an alternate file name,
			// without HashCode. These files can be generated by external tools
			// decoding the Atrac3+ files. These tools can't generate the HashCode.
			//
			// Use the following alternate file name scheme:
			//       Atrac-SSSSSSSS-NNNNNNNN-DDDDDDDD.at3.decoded
			// where SSSSSSSS is the file size in Hex
			//       NNNNNNNN is the number of samples in Hex found in the "fact" Chunk
			//       DDDDDDDD are the first 32-bit in Hex found in the "data" Chunk
			int numberOfSamples = 0;
			int data = 0;

			// Scan the Atrac data for NNNNNNNN and DDDDDDDD values
			Memory mem = Memory.getInstance();
			int scanAddress = address + 12;
			int endScanAddress = address + length;
			while (scanAddress < endScanAddress) {
				int chunkHeader = mem.read32(scanAddress);
				int chunkSize = mem.read32(scanAddress + 4);

				if (chunkHeader == waveFactChunkHeader) {
					numberOfSamples = mem.read32(scanAddress + 8);
				} else if (chunkHeader == waveDataChunkHeader) {
					data = mem.read32(scanAddress + 8);
					break;
				}

				// Go to the next Chunk
				scanAddress += chunkSize + 8;
			}

			File alternateDecodedFile = new File(String.format("%sAtrac-%08X-%08X-%08X%s", getBaseDirectory(id), atracFileSize, numberOfSamples, data, decodedAtracSuffix));
			if (alternateDecodedFile.canRead()) {
				decodedFile = alternateDecodedFile;
			}
		}

		File atracFile = new File(getCompleteFileName(atracSuffix));
		if (decodedFile.canRead()) {
			// Decoded file is already present
			try {
				decodedStream = new RandomAccessFile(decodedFile, "r");
				atracEndSample = (int) (decodedFile.length() / 4);
			} catch (FileNotFoundException e) {
			}
		} else if (atracFile.canRead() && atracFile.length() == atracFileSize) {
			// Atrac file is already written, no need to write it again
		} else if (extract) {
			commandFileDirty = true;
			displayInstructions();
			try {
				new File(getBaseDirectory(id)).mkdirs();
				atracStream = new FileOutputStream(getCompleteFileName(atracSuffix));
				byte[] buffer = new byte[length];
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
				for (int i = 0; i < length; i++) {
					buffer[i] = (byte) memoryReader.readNext();
				}
				atracStream.write(buffer);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
            generateCommandFile();
        }
	}

	public void atracAddStreamData(int address, int length) {
        if(checkMediaEngineState()) {
            atracChannel.write(address, length);
            return;
        }

		if (atracStream != null) {
			try {
				byte[] buffer = new byte[length];
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
				for (int i = 0; i < length; i++) {
					buffer[i] = (byte) memoryReader.readNext();
				}
				atracStream.write(buffer);
			} catch (IOException e) {
				Modules.log.error(e);
			}
		}
	}

	public int atracDecodeData(int atracID, int address) {
        if (checkMediaEngineState()) {
            me.stepAudio();
            return copySamplesToMem(address);
        }

        int samples = 0;
		if (decodedStream != null) {
			try {
				int length = decodedStream.read(atracDecodeBuffer);
				if (length > 0) {
					samples = length / 4;
					Memory.getInstance().copyToMemory(address, ByteBuffer.wrap(atracDecodeBuffer, 0, length), length);
				}
			} catch (IOException e) {
				// Ignore Exception
			}
		} else {
			samples = -1;
		}

		atracEnd = samples <= 0 ? 1 : 0;
		atracRemainFrames = -1;

		return samples;
	}

	public void atracResetPlayPosition(int sample) {
        if(checkMediaEngineState()) {
            me.getContainer().seekKeyFrame(sample * 4, 0, STREAM_SEEK_FLAG_FRAME);
        }

		if (decodedStream != null) {
			try {
				decodedStream.seek(sample * 4);
			} catch (IOException e) {
				Modules.log.error(e);
			}
		}
	}

	public int getAtracEnd() {
		return atracEnd;
	}

	public int getAtracRemainFrames() {
		return atracRemainFrames;
	}

	public int getAtracEndSample() {
		return atracEndSample;
	}

    public int getAtracNextDecodePosition() {
        if(checkMediaEngineState()) {
            return memBufOffset;
        }

        try {
            return (int)decodedStream.getFilePointer();
        } catch (Exception e) {
            return -1;
        }
	}

    public void setAtracLoopCount(int count) {
        currentLoopCount = count;
    }

    protected int copySamplesToMem(int address) {
        Memory mem = Memory.getInstance();

        int bytes = me.getCurrentAudioSamples(samplesBuffer);
        if (bytes > 0) {
            atracEndSample += bytes;
            mem.copyToMemory(address, ByteBuffer.wrap(samplesBuffer, 0, bytes), bytes);
        }

        return bytes / 4;
    }

    public void finish() {
		closeStreams();
	}

	protected void displayInstructions() {
		if (instructionsDisplayed) {
			return;
		}

		// Display decoding instructions into the log file, where else?
		Logger log = Modules.log;
		log.info("The ATRAC3 audio is currently being saved under");
		log.info("    " + getBaseDirectory(id));
		log.info("To decode the audio, copy the following file");
		log.info("    *" + atracSuffix);
		log.info("    " + Connector.commandFileName);
		log.info("to your PSP under");
		log.info("    " + Connector.basePSPDirectory);
		log.info("and run the '" + Connector.jpcspConnectorName + "' on your PSP.");
		log.info("After decoding on the PSP, move the following files");
		log.info("    " + Connector.basePSPDirectory + decodedAtracSuffix);
		log.info("back to Jpcsp under");
		log.info("    " + getBaseDirectory(id));
		log.info("Afterwards, you can delete the files on the PSP.");

		instructionsDisplayed = true;
	}
}
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

import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;

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
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.media.ExternalDecoder;
import jpcsp.media.FileProtocolHandler;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.media.VirtualFileProtocolHandler;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Hash;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class AtracCodec {
	private static Logger log = sceAtrac3plus.log;

	private class EnableMediaEngineSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableMediaEngine(value);
		}
	}

    protected String id;
    protected static final String atracSuffix = ".at3";
    protected static final String decodedSuffix = ".decoded";
    protected static final String decodedAtracSuffix = atracSuffix + decodedSuffix;
    protected RandomAccessFile decodedStream;
    protected OutputStream atracStream;
    protected boolean atracEnd;
    protected int atracEndSample;
    protected int atracMaxSamples;
    protected int atracFileSize;
    protected int atracBufferAddress;
    protected int atracHash;
    protected int bytesPerFrame;
    protected byte[] atracDecodeBuffer;
    protected static boolean instructionsDisplayed = false;
    protected static boolean commandFileDirty = true;
    public static int waveFactChunkHeader = 0x74636166; // "fact"
    public static int waveDataChunkHeader = 0x61746164; // "data"
    // Media Engine based playback.
    protected MediaEngine me;
    protected PacketChannel atracChannel;
	// The MediaEngine requires at least 3 * 0x8000 data bytes available
	// during initialization. Otherwise, it will assume to have reached
	// the end of the channel and will not further read.
	// The readRetryCount of the container does not help.
    protected int atracChannelStartLength = 3 * 0x8000;
    protected int currentLoopCount;
    protected boolean useMediaEngine = false;
    protected byte[] samplesBuffer;
    protected int samplesChannels = 2;
    protected int bytesPerSample = 4;
    protected ExternalDecoder externalDecoder;
    protected boolean requireAllAtracData;
    private static final String name = "AtracCodec";

    public AtracCodec() {
    	Settings.getInstance().registerSettingsListener(name, "emu.useMediaEngine", new EnableMediaEngineSettingsListerner());

    	if (useMediaEngine()) {
            me = new MediaEngine();
            atracChannel = new PacketChannel();
            currentLoopCount = 0;
        }

        externalDecoder = new ExternalDecoder();
        generateCommandFile();
    }

    protected boolean checkMediaEngineState() {
        return useMediaEngine && me != null;
    }

    protected boolean useMediaEngine() {
        return useMediaEngine;
    }

    private void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

    public void setAtracMaxSamples(int atracMaxSamples) {
    	if (this.atracMaxSamples != atracMaxSamples) {
	    	this.atracMaxSamples = atracMaxSamples;
	    	if (useMediaEngine() && me != null) {
	    		me.setAudioSamplesSize(atracMaxSamples);
	    	}
	    	createBuffers();
    	}
    }

    private void createBuffers() {
        atracDecodeBuffer = new byte[atracMaxSamples * bytesPerSample];
        samplesBuffer = new byte[atracMaxSamples * bytesPerSample];
    }

    protected void setChannels(int channels) {
    	if (channels != samplesChannels) {
    		samplesChannels = channels;
    		bytesPerSample = channels * 2;
    		createBuffers();
    	}
    }

    protected String generateID(int address, int length, int fileSize) {
        int hashCode = Hash.getHashCodeFloatingMemory(0, address, Math.min(length, fileSize));
        return String.format("Atrac-%08X-%08X", fileSize, hashCode);
    }

    public static String getBaseDirectory() {
        return String.format("%sAtrac%c", Settings.getInstance().getDiscTmpDirectory(), File.separatorChar);
    }

    protected String getCompleteFileName(String suffix) {
        String completeFileName = String.format("%s%s%s", getBaseDirectory(), id, suffix);
        return completeFileName;
    }

    protected void generateCommandFile() {
        if (!commandFileDirty) {
            return;
        }
        // Generate decode commands for all the non-decoded Atrac files
        String baseDirectory = getBaseDirectory();
        File directory = new File(baseDirectory);
        String[] files = directory.list();
        HashSet<String> atracFiles = new HashSet<String>();
        HashSet<String> decodedFiles = new HashSet<String>();

        if (files != null) {
            for (String fileName : files) {
                if (fileName.endsWith(atracSuffix)) {
                    atracFiles.add(fileName);
                } else if (fileName.endsWith(decodedAtracSuffix)) {
                    decodedFiles.add(fileName);
                }
            }
        }

        PrintWriter command = null;
        try {
            command = new PrintWriter(String.format("%s%s", baseDirectory, Connector.commandFileName));
            for (String atracFileName : atracFiles) {
                if (!decodedFiles.contains(atracFileName + decodedSuffix)) {
                    // File not yet decoded, add it to the command file
                    command.println("DecodeAtrac3");
                    command.println(Connector.basePSPDirectory + atracFileName);
                }
            }
            command.println("Exit");
            commandFileDirty = false;
        } catch (FileNotFoundException e) {
            // This exception can be safely ignored, since this file
            // is only used when using decoded data.
        } finally {
            Utilities.close(command);
        }
    }

    protected void closeStreams() {
        Utilities.close(decodedStream, atracStream);
        decodedStream = null;
        atracStream = null;
        requireAllAtracData = false;
    }

    public void setRequireAllAtracData() {
    	requireAllAtracData = true;
    }

    public void atracSetData(int atracID, int codecType, int address, int length, int atracFileSize, int atracHash) {
    	this.atracFileSize = atracFileSize;
    	this.atracBufferAddress = address;
    	this.atracHash = atracHash;
        id = generateID(address, length, atracFileSize);
        closeStreams();
        atracEndSample = -1;
        requireAllAtracData = false;

        int memoryCodecType = sceAtrac3plus.getCodecType(address);
        if (memoryCodecType != codecType && memoryCodecType != 0) {
        	log.info(String.format("Different CodecType received %d != %d, assuming %d", codecType, memoryCodecType, memoryCodecType));
        	codecType = memoryCodecType;
        }

        if (codecType == PSP_CODEC_AT3) {
            log.info("Decodable AT3 data detected.");
            if (checkMediaEngineState()) {
                me.finish();
                IVirtualFile extractedFile = externalDecoder.extractAtrac(address, length, atracFileSize, atracHash); 
                if (extractedFile == null) {
	                atracChannel = new PacketChannel();
	                atracChannel.setTotalStreamSize(atracFileSize);
	                atracChannel.setFarRewindAllowed(true);
	                atracChannel.write(address, length);
	                // Defer the initialization of the MediaEngine until atracDecodeData()
	                // to ensure we have enough data into the channel.
	                atracEndSample = 0;
                } else {
                	log.info(String.format("Playing AT3 file '%s'", extractedFile));
        			atracChannel = null;
        			me.init(new VirtualFileProtocolHandler(extractedFile), false, true, 0, 0);
                    atracEndSample = -1;
                }
                return;
            }
        } else if (codecType == PSP_CODEC_AT3PLUS) {
        	if (checkMediaEngineState() && ExternalDecoder.isEnabled()) {
        		IVirtualFile decodedFile = externalDecoder.decodeAtrac(address, length, atracFileSize, atracHash, this);
        		if (decodedFile != null) {
        			log.info(String.format("AT3+ data decoded by the external decoder, using '%s'.", decodedFile));
        			me.finish();
        			atracChannel = null;
        			me.init(new VirtualFileProtocolHandler(decodedFile), false, true, 0, 0);
                    atracEndSample = -1;
        			return;
        		} else if (requireAllAtracData) {
        			// The external decoder requires all the atrac data
        			// before it can try to decode the atrac.
        			me.finish();
        			atracChannel = new PacketChannel();
        			atracChannel.setTotalStreamSize(atracFileSize);
        			atracChannel.write(address, length);
        			return;
        		}
    			log.info("AT3+ data could not be decoded by the external decoder.");
        	} else {
        		log.info("Undecodable AT3+ data detected.");
        	}
        }
        me = null;

        File decodedFile = new File(getCompleteFileName(decodedAtracSuffix));

        Memory mem = Memory.getInstance();
        if (!decodedFile.canRead() && mem.read32(address) == sceAtrac3plus.RIFF_MAGIC) {
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

            File alternateDecodedFile = new File(String.format("%sAtrac-%08X-%08X-%08X%s", getBaseDirectory(), atracFileSize, numberOfSamples, data, decodedAtracSuffix));
            if (alternateDecodedFile.canRead()) {
                decodedFile = alternateDecodedFile;
            }
        }

        File atracFile = new File(getCompleteFileName(atracSuffix));
        if (decodedFile.canRead()) {
            try {
                decodedStream = new RandomAccessFile(decodedFile, "r");
                atracEndSample = (int) (decodedFile.length() / 4);
            } catch (FileNotFoundException e) {
                // Decoded file should already be present
                log.warn(e);
            }
        } else if (atracFile.canRead() && atracFile.length() == atracFileSize) {
            // Atrac file is already written, no need to write it again
        } else if (sceAtrac3plus.isEnableConnector()) {
            commandFileDirty = true;
            displayInstructions();
            new File(getBaseDirectory()).mkdirs();

            try {
                atracStream = new FileOutputStream(getCompleteFileName(atracSuffix));
                byte[] buffer = new byte[length];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
                for (int i = 0; i < length; i++) {
                    buffer[i] = (byte) memoryReader.readNext();
                }
                atracStream.write(buffer);
            } catch (IOException e) {
                log.warn(e);
            }
            generateCommandFile();
        }
    }

    public void atracAddStreamData(int address, int length) {
        if (checkMediaEngineState()) {
        	if (atracChannel != null) {
        		atracChannel.write(address, length);
        	}
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
                log.error(e);
            }
        }
    }

    public int atracDecodeData(int atracID, int address, int channels) {
        int samples = 0;
        atracEnd = false;

        if (checkMediaEngineState()) {
        	if (me.getContainer() == null && atracChannel != null) {
        		if (requireAllAtracData) {
        			if (atracChannel.length() >= atracFileSize) {
        				requireAllAtracData = false;
        	        	if (checkMediaEngineState() && ExternalDecoder.isEnabled()) {
        	        		String decodedFile = externalDecoder.decodeAtrac(atracChannel, atracBufferAddress, atracFileSize, atracHash);
        	        		if (decodedFile != null) {
        	        			log.info("AT3+ data decoded by the external decoder (all AT3+ data retrieved).");
        	        			me.finish();
        	        			atracChannel = null;
        	        			me.init(new FileProtocolHandler(decodedFile), false, true, 0, 0);
        	                    atracEndSample = -1;
        	        		} else {
        	        			log.info("AT3+ data could not be decoded by the external decoder, even after retrieving all AT3+ data.");
        	        			me = null;
        	        		}
        	        	} else {
    	        			log.info("AT3+ data could not be decoded by the external decoder, even after retrieving all AT3+ data.");
        	        		me = null;
        	        	}
        	        	if (me == null) {
        	        		return atracDecodeData(atracID, address, channels);
        	        	}
        			} else {
        				// Fake returning 1 sample with remainFrames == 0
        				// to force a call to sceAtracAddStreamData.
        				samples = 1;
        				if (address != 0) {
        					Memory.getInstance().memset(address, (byte) 0, samples * bytesPerSample);
        				}
        			}
        		} else if (atracChannel.length() >= getAtracChannelStartLength() || atracChannel.length() >= atracFileSize) {
    				me.init(atracChannel, false, true, 0, 0);
    			} else {
    				// Fake returning 1 sample with remainFrames == 0
    				// to force a call to sceAtracAddStreamData.
    				samples = 1;
    				if (address != 0) {
    					Memory.getInstance().memset(address, (byte) 0, samples * bytesPerSample);
    				}
    			}
        	}
        	setChannels(channels);
            if (me.stepAudio(atracMaxSamples * bytesPerSample, channels)) {
            	samples = copySamplesToMem(address);
            }
        	if (samples == 0) {
        		atracEnd = true;
        	}
        } else if (decodedStream != null) {
            try {
                int length = decodedStream.read(atracDecodeBuffer);
                if (length > 0) {
                    samples = length / 4;
                    if (address != 0) {
                    	Memory.getInstance().copyToMemory(address, ByteBuffer.wrap(atracDecodeBuffer, 0, length), length);
                    }
                    long restLength = decodedStream.length() - decodedStream.getFilePointer();
                    if (restLength <= 0) {
                    	atracEnd = true;
                    }
                } else {
                	atracEnd = true;
                }
            } catch (IOException e) {
                log.warn(e);
            }
        } else {
            samples = -1;
            atracEnd = true;
        }

        return samples;
    }

    public void atracResetPlayPosition(int sample) {
        if (checkMediaEngineState()) {
            me.audioResetPlayPosition(sample);
        }

        if (decodedStream != null) {
            try {
                decodedStream.seek(sample * bytesPerSample);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public int getChannelLength() {
    	if (atracChannel == null) {
    		// External audio
    		return atracFileSize;
    	}
    	return atracChannel.length();
    }

    public int getChannelPosition() {
    	if (atracChannel == null) {
    		return -1;
    	}
    	return (int) atracChannel.getPosition();
    }

    public void resetChannel() {
    	if (atracChannel == null) {
    		return;
    	}
    	atracChannel.reset();
    }

    public boolean getAtracEnd() {
        return atracEnd;
    }

    public int getAtracEndSample() {
        return atracEndSample;
    }

    public void setAtracLoopCount(int count) {
        currentLoopCount = count;
    }

    protected int copySamplesToMem(int address) {
        Memory mem = Memory.getInstance();

        int bytes = me.getCurrentAudioSamples(samplesBuffer);
        if (bytes > 0) {
            atracEndSample += bytes;
            if (address != 0) {
            	mem.copyToMemory(address, ByteBuffer.wrap(samplesBuffer, 0, bytes), bytes);
            }
        }

        return bytes / bytesPerSample;
    }

    public void finish() {
        closeStreams();
        Settings.getInstance().removeSettingsListener(name);
    }

    public boolean isExternalAudio() {
    	return atracChannel == null;
    }

    protected void displayInstructions() {
        if (instructionsDisplayed) {
            return;
        }

        // Display decoding instructions into the log file, where else?
        log.info("The ATRAC3 audio is currently being saved under");
        log.info("    " + getBaseDirectory());
        log.info("To decode the audio, copy the following file");
        log.info("    *" + atracSuffix);
        log.info("    " + Connector.commandFileName);
        log.info("to your PSP under");
        log.info("    " + Connector.basePSPDirectory);
        log.info("and run the '" + Connector.jpcspConnectorName + "' on your PSP.");
        log.info("After decoding on the PSP, move the following files");
        log.info("    " + Connector.basePSPDirectory + decodedAtracSuffix);
        log.info("back to Jpcsp under");
        log.info("    " + getBaseDirectory());
        log.info("Afterwards, you can delete the files on the PSP.");

        instructionsDisplayed = true;
    }

	public int getAtracChannelStartLength() {
		return atracChannelStartLength;
	}

	public void setAtracChannelStartLength(int atracChannelStartLength) {
		this.atracChannelStartLength = atracChannelStartLength;
	}
}
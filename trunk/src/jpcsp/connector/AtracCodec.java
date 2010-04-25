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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.HashSet;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Hash;

/**
 * @author gid15
 *
 */
public class AtracCodec {
	protected String id;
	protected static final String atracSuffix = ".at3";
	protected static final String decodedSuffix = ".decoded";
	protected static final String decodedAtracSuffix = atracSuffix + decodedSuffix;
	protected InputStream decodedStream;
	protected OutputStream atracStream;
	protected int atracEnd;
	protected int atracRemainFrames;
	protected int atracEndSample;
	protected byte[] atracDecodeBuffer;
	protected static boolean instructionsDisplayed = false;
	protected static boolean commandFileDirty = true;

	public AtracCodec() {
		atracDecodeBuffer = new byte[sceAtrac3plus.maxSamples * 4];
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

	public void atracSetData(int address, int length, int atracFileSize) {
		id = generateID(address, length, atracFileSize);

		closeStreams();

		atracEndSample = -1;

		File decodedFile = new File(getCompleteFileName(decodedAtracSuffix));
		File atracFile = new File(getCompleteFileName(atracSuffix));
		if (decodedFile.canRead()) {
			// Decoded file is already present
			try {
				decodedStream = new FileInputStream(decodedFile);
				atracEndSample = (int) (decodedFile.length() / 4);
			} catch (FileNotFoundException e) {
			}
		} else if (atracFile.canRead() && atracFile.length() == atracFileSize) {
			// Atrac file is already written, no need to write it again
		} else {
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
		}

		generateCommandFile();
	}

	public void atracAddStreamData(int address, int length) {
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

	public int atracDecodeData(int address) {
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
		atracRemainFrames = sceAtrac3plus.remainFrames;

		return samples;
	}

	public int getAtracEnd() {
		return atracEnd;
	}

	public int getAtracRemainFrames() {
		return atracRemainFrames;
	}

	public void finish() {
		closeStreams();
	}

	public int getAtracEndSample() {
		return atracEndSample;
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

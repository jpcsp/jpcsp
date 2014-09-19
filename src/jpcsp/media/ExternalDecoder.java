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
package jpcsp.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.psmf.PsmfAudioDemuxVirtualFile;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.FileLocator;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class ExternalDecoder {
	private static Logger log = Modules.log;
    private static File extAudioDecoder;
    private static boolean enabled = false;
    private static boolean dumpAudioStreamFile = false;
    private static boolean keepOmaFile = true;
    private static EnableExternalDecoderSettingsListerner enableExternalDecoderSettingsListerner;

	private static class EnableExternalDecoderSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnabled(value);
		}
	}

    public ExternalDecoder() {
    	init();
    }

    private static void setEnabled(boolean flag) {
    	enabled = flag;
    }

    private static void init() {
    	if (enableExternalDecoderSettingsListerner == null) {
    		enableExternalDecoderSettingsListerner = new EnableExternalDecoderSettingsListerner();
    		Settings.getInstance().registerSettingsListener("ExternalDecoder", "emu.useExternalDecoder", enableExternalDecoderSettingsListerner);
    	}

    	if (enabled && extAudioDecoder == null) {
    		String extAudioDecoderPath = System.getProperty("java.library.path");
    		if (extAudioDecoderPath == null) {
    			extAudioDecoderPath = "";
    		} else if (!extAudioDecoderPath.endsWith("/")) {
    			extAudioDecoderPath += "/";
    		}

    		String extAudioDecoders[] = { "DecodeAudio.bat", "DecodeAudio.exe", "DecodeAudio.sh" };
            for (int i = 0; i < extAudioDecoders.length; i++) {
                File f = new File(String.format("%s%s", extAudioDecoderPath, extAudioDecoders[i]));
                if (f.exists()) {
                    extAudioDecoder = f;
                    break;
                }
            }

            if (extAudioDecoder == null) {
            	enabled = false;
            } else {
            	log.info("Using the external audio decoder (SonicStage)");
            }
    	}

    	if (enabled) {
    		// Force the creation of a FileLocator so that it can register its IO listener.
    		FileLocator.getInstance();
    	}
    }

    public static boolean isEnabled() {
    	init();

    	return enabled;
    }

    private boolean executeExternalDecoder(String inputFileName, String outputFileName, String at3FileName, boolean keepInputFile) {
    	boolean decoded = executeExternalDecoder(inputFileName, outputFileName, at3FileName);

    	if (!keepInputFile) {
    		File inputFile = new File(inputFileName);
    		inputFile.delete();
    		if (at3FileName != null) {
	    		File at3File = new File(at3FileName);
	    		at3File.delete();
    		}
    	}

    	File outputFile = new File(outputFileName);
    	if (outputFile.canRead() && outputFile.length() == 0) {
			// Only an empty file has been generated, the file could not be converted
    		outputFile.delete();
    		decoded = false;
    	}

    	return decoded;
    }

    private boolean executeExternalDecoder(String inputFileName, String outputFileName, String at3FileName) {
		String[] cmd;

		if (at3FileName == null) {
			at3FileName = "";
		}

		if (extAudioDecoder.toString().endsWith(".bat")) {
			cmd = new String[] {
					"cmd",
					"/C",
					extAudioDecoder.toString(),
					inputFileName,
					outputFileName,
					at3FileName };
		} else {
			cmd = new String[] {
					extAudioDecoder.toString(),
					inputFileName,
					outputFileName,
					at3FileName };
		}
		try {
			Process extAudioDecodeProcess = Runtime.getRuntime().exec(cmd);
			StreamReader stdoutReader = new StreamReader(extAudioDecodeProcess.getInputStream());
			StreamReader stderrReader = new StreamReader(extAudioDecodeProcess.getErrorStream());
			stdoutReader.start();
			stderrReader.start();
			int exitValue = extAudioDecodeProcess.waitFor();
			if (log.isDebugEnabled()) {
				log.debug(String.format("External AudioDecode Process '%s' returned %d", extAudioDecoder.toString(), exitValue));
				log.debug("stdout: " + stdoutReader.getInput());
				log.debug("stderr: " + stderrReader.getInput());
			}
		} catch (InterruptedException e) {
			log.error(e);
			return false;
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		return true;
    }

    private boolean decodeExtAudio(byte[] audioStreamData, int mpegFileSize, int mpegOffset, int audioChannel, long lastTimestamp) {
    	if (audioStreamData == null) {
    		return false;
    	}

		if (dumpAudioStreamFile) {
			try {
				new File(MediaEngine.getExtAudioBasePath(mpegFileSize, lastTimestamp)).mkdirs();
				FileOutputStream pmfOut = new FileOutputStream(MediaEngine.getExtAudioPath(mpegFileSize, audioChannel, lastTimestamp, "audio"));
				pmfOut.write(audioStreamData);
				pmfOut.close();
			} catch (IOException e) {
				log.error(e);
			}
		}

		ByteBuffer audioStream = ByteBuffer.wrap(audioStreamData);
		ByteBuffer omaBuffer = OMAFormat.convertStreamToOMA(audioStream); 
		if (omaBuffer == null) {
			return false;
		}

		try {
			new File(MediaEngine.getExtAudioBasePath(mpegFileSize, lastTimestamp)).mkdirs();
			String encodedFileName = MediaEngine.getExtAudioPath(mpegFileSize, audioChannel, lastTimestamp, "oma");
			FileOutputStream os = new FileOutputStream(encodedFileName);
			os.getChannel().write(omaBuffer);
			os.close();

			String decodedFileName = MediaEngine.getExtAudioPath(mpegFileSize, audioChannel, lastTimestamp, "wav");

			if (!executeExternalDecoder(encodedFileName, decodedFileName, null, keepOmaFile)) {
				int channels = OMAFormat.getOMANumberAudioChannels(omaBuffer);
				if (channels == 1) {
					// It seems that SonicStage has problems decoding mono AT3+ data
					// or we might generate an incorrect OMA file for monaural audio.
					log.info("Mono AT3+ audio stream could not be decoded by the external decoder");
				} else if (channels == 2) {
					log.info("Stereo AT3+ audio stream could not be decoded by the external decoder");
				} else {
					log.info("AT3+ audio stream could not be decoded by the external decoder (channels=" + channels + ")");
				}
				return false;
			}
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		return true;
    }

    public void decodeExtAudio(IVirtualFile vFilePsmf, int mpegFileSize, int mpegOffset, int audioChannel, long lastTimestamp) {
    	if (!isEnabled()) {
    		return;
    	}

    	if (vFilePsmf == null) {
    		return;
    	}

    	IVirtualFile vFileDemuxedAudio = new PsmfAudioDemuxVirtualFile(vFilePsmf, mpegOffset, audioChannel);
    	byte[] audioStreamData = Utilities.readCompleteFile(vFileDemuxedAudio);
    	if (audioStreamData == null || audioStreamData.length == 0) {
    		return;
    	}

    	decodeExtAudio(audioStreamData, mpegFileSize, mpegOffset, audioChannel, lastTimestamp);
    }

    public void setStreamFile(SeekableDataInput dataInput, IVirtualFile vFile, int address, long startPosition, int length) {
    	FileLocator.getInstance().setFileData(dataInput, vFile, address, startPosition, length);
    }
}

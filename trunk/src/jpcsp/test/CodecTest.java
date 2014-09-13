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
package jpcsp.test;

import static jpcsp.HLE.modules150.sceAtrac3plus.AT3_MAGIC;
import static jpcsp.HLE.modules150.sceAtrac3plus.AT3_PLUS_MAGIC;
import static jpcsp.HLE.modules150.sceAtrac3plus.FMT_CHUNK_MAGIC;
import static jpcsp.HLE.modules150.sceAtrac3plus.RIFF_MAGIC;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AAC;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_MP3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceAudiocodec;
import jpcsp.HLE.modules.sceMp3;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder;
import jpcsp.media.codec.mp3.Mp3Decoder;
import jpcsp.media.codec.mp3.Mp3Header;
import jpcsp.util.Utilities;

public class CodecTest {
	private static Logger log = Atrac3plusDecoder.log;

	public static void main(String[] args) {
        DOMConfigurator.configure("LogSettings.xml");
		Memory mem = Memory.getInstance();

		try {
			File file = new File("sample.aac");
			log.info(String.format("Reading file %s", file));
			int length = (int) file.length();
			InputStream in = new FileInputStream(file);
			byte buffer[] = new byte[length];
			in.read(buffer);
			in.close();

			int samplesAddr = MemoryMap.START_USERSPACE;
			int inputAddr = MemoryMap.START_USERSPACE + 0x10000;
			for (int i = 0; i < length; i++) {
				mem.write8(inputAddr + i, buffer[i]);
			}

			int channels = 2;
			int codecType = -1;
			int bytesPerFrame = 0;
			int codingMode = 0;
			int dataOffset = 0;
			if (mem.read32(inputAddr) == RIFF_MAGIC) {
				int scanOffset = 12;
				while (dataOffset < 0) {
					int chunkMagic = mem.read32(inputAddr + scanOffset);
					int chunkLength = mem.read32(inputAddr + scanOffset + 4);
					scanOffset += 8;
					switch (chunkMagic) {
						case FMT_CHUNK_MAGIC:
							switch (mem.read16(inputAddr + scanOffset + 0)) {
								case AT3_PLUS_MAGIC: codecType = PSP_CODEC_AT3PLUS; break;
								case AT3_MAGIC     : codecType = PSP_CODEC_AT3;     break;
							}
							channels = mem.read16(inputAddr + scanOffset + 2);
							bytesPerFrame = mem.read16(inputAddr + scanOffset + 12);
							int extraDataSize = mem.read16(inputAddr + scanOffset + 16);
							if (extraDataSize == 14) {
								codingMode = mem.read16(inputAddr + scanOffset + 18 + 6);
							}
							break;
						case sceAtrac3plus.DATA_CHUNK_MAGIC:
							dataOffset = scanOffset;
							break;
					}
					scanOffset += chunkLength;
				}
			} else if (mem.read32(inputAddr) == 0x02334449) { // ID3v2
				int headerLength = 0;
				for (int i = 0; i < 4; i++) {
					headerLength = (headerLength << 7) + (mem.read8(inputAddr + 6 + i) & 0x7F);
				}
				if (sceMp3.isMp3Magic(Utilities.readUnaligned16(mem, inputAddr + 10 + headerLength))) {
					dataOffset = headerLength + 10;
					codecType = PSP_CODEC_MP3;
				}
			} else if (sceMp3.isMp3Magic(mem.read16(inputAddr))) {
				Mp3Header mp3Header = new Mp3Header();
				if (Mp3Decoder.decodeHeader(mp3Header, Integer.reverseBytes(mem.read32(inputAddr))) == 0) {
					dataOffset = mp3Header.frameSize;
				}
				codecType = PSP_CODEC_MP3;
			} else if ((Utilities.endianSwap16(mem.read16(inputAddr)) & 0xFFF0) == 0xFFF0 || file.getName().endsWith(".aac")) {
				codecType = PSP_CODEC_AAC;
			} else {
				log.error(String.format("File '%s' not in RIFF format", file));
				return;
			}

	        AudioFormat audioFormat = new AudioFormat(44100,
	                16,
	                channels,
	                true,
	                false);
	        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine mLine = (SourceDataLine) AudioSystem.getLine(info);
            mLine.open(audioFormat);
            mLine.start();

			ICodec codec = CodecFactory.getCodec(codecType);
			codec.init(bytesPerFrame, channels, channels, codingMode);

			inputAddr += dataOffset;
			length -= dataOffset;

			for (int frameNbr = 0; true; frameNbr++) {
				int result = codec.decode(inputAddr, length, samplesAddr);
				if (result < 0) {
					log.error(String.format("Frame #%d, result 0x%08X", frameNbr, result));
					break;
				}
				if (result == 0) {
					// End of data
					break;
				}
				int consumedBytes = bytesPerFrame;
				if (result != bytesPerFrame) {
					if (bytesPerFrame == 0) {
						consumedBytes = result;
					} else {
						log.warn(String.format("Frame #%d, result 0x%X, expected 0x%X", frameNbr, result, bytesPerFrame));
					}
				}

				inputAddr += consumedBytes;
				length -= consumedBytes;

				byte bytes[] = new byte[codec.getNumberOfSamples() * 2 * channels];
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = (byte) mem.read8(samplesAddr + i);
				}
				mLine.write(bytes, 0, bytes.length);
			}

            mLine.drain();
            mLine.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}

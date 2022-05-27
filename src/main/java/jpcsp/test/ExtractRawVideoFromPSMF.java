package jpcsp.test;

import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PACK_START_CODE;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PADDING_STREAM;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_1;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_2;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.SYSTEM_HEADER_START_CODE;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jpcsp.HLE.modules.sceMpeg;

public class ExtractRawVideoFromPSMF {
	private static byte buffer[] = new byte[sceMpeg.MPEG_AVC_ES_SIZE];

	private static int read8(InputStream in) throws IOException {
		return in.read() & 0xFF;
	}

	private static int read16(InputStream in) throws IOException {
		return (read8(in) << 8) | read8(in);
	}

	private static int read32(InputStream in) throws IOException {
		return (read8(in) << 24) | (read8(in) << 16) | (read8(in) << 8) | read8(in);
	}

	private static void skip(InputStream in, int n) throws IOException {
		in.skip(n);
	}

	private static int skipPesHeader(InputStream in, int startCode) throws IOException {
		int pesLength = 0;
		int c = read8(in);
		pesLength++;
		while (c == 0xFF) {
			c = read8(in);
			pesLength++;
		}

		if ((c & 0xC0) == 0x40) {
			skip(in, 1);
			c = read8(in);
			pesLength += 2;
		}

		if ((c & 0xE0) == 0x20) {
			skip(in, 4);
			pesLength += 4;
			if ((c & 0x10) != 0) {
				skip(in, 5);
				pesLength += 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			skip(in, 1);
			int headerLength = read8(in);
			pesLength += 2;
			skip(in, headerLength);
			pesLength += headerLength;
		}

		if (startCode == 0x1BD) { // PRIVATE_STREAM_1
			int channel = read8(in);
			pesLength++;
			if (channel >= 0x80 && channel <= 0xCF) {
				skip(in, 3);
				pesLength += 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(in, 1);
					pesLength++;
				}
			} else {
				skip(in, 3);
				pesLength += 3;
			}
		}

		return pesLength;
	}

	private static void write(OutputStream out, InputStream in, int n) throws IOException {
		if (n > 0) {
			in.read(buffer, 0, n);
			out.write(buffer, 0, n);
		}
	}

	public static void main(String[] args) {
		try {
			FileInputStream in = new FileInputStream(args[0]);
			FileOutputStream out = new FileOutputStream(args[0] + ".raw");
			while (true) {
				int startCode = read32(in);
				if (startCode == -1) {
					break;
				}
				int codeLength;
				switch (startCode) {
					case PACK_START_CODE:
						skip(in, 10);
						break;
					case SYSTEM_HEADER_START_CODE:
						skip(in, 14);
						break;
					case PADDING_STREAM:
					case PRIVATE_STREAM_2:
					case PRIVATE_STREAM_1: // Audio stream
						codeLength = read16(in);
						skip(in, codeLength);
						break;
					case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
					case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
					case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
					case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
						codeLength = read16(in);
						codeLength -= skipPesHeader(in, startCode);
						write(out, in, codeLength);
						break;
				}
			}
			out.close();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

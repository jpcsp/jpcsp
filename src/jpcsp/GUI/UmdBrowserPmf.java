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
package jpcsp.GUI;

import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jpcsp.Emulator;
import jpcsp.HLE.modules600.sceMpeg;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.IVideoCodec;
import jpcsp.media.codec.h264.H264Utils;

import com.twilight.h264.decoder.H264Context;

import jpcsp.util.Utilities;

public class UmdBrowserPmf {
	private static org.apache.log4j.Logger log = Emulator.log;
    private UmdIsoReader iso;
    private String fileName;
    private long startTime;
    private Image image;
    private boolean done;
    private boolean endOfVideo;
    private boolean threadExit;
    private JLabel display;
    private PmfDisplayThread displayThread;
    private IVideoCodec videoCodec;
	private int[] videoData = new int[0x10000];
	private int videoDataOffset;
	private InputStream is;
	private int videoChannel = 0;
	private int frame;
	private int videoWidth;
	private int videoHeight;

    public UmdBrowserPmf(UmdIsoReader iso, String fileName, JLabel display) {
        this.iso = iso;
        this.fileName = fileName;
        this.display = display;

        init();
        initVideo();
    }

	private int read8(InputStream is) {
		try {
			return is.read();
		} catch (IOException e) {
			// Ignore exception
		}

		return -1;
	}

	private int read16(InputStream is) {
		return (read8(is) << 8) | read8(is);
	}

	private int read32(InputStream is) {
		return (read8(is) << 24) | (read8(is) << 16) | (read8(is) << 8) | read8(is);
	}

	private void skip(InputStream is, int n) {
		if (n > 0) {
			try {
				is.skip(n);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int skipPesHeader(InputStream is, int startCode) {
		int pesLength = 0;
		int c = read8(is);
		pesLength++;
		while (c == 0xFF) {
			c = read8(is);
			pesLength++;
		}

		if ((c & 0xC0) == 0x40) {
			skip(is, 1);
			c = read8(is);
			pesLength += 2;
		}

		if ((c & 0xE0) == 0x20) {
			skip(is, 4);
			pesLength += 4;
			if ((c & 0x10) != 0) {
				skip(is, 5);
				pesLength += 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			skip(is, 1);
			int headerLength = read8(is);
			pesLength += 2;
			skip(is, headerLength);
			pesLength += headerLength;
		}

		if (startCode == 0x1BD) { // PRIVATE_STREAM_1
			int channel = read8(is);
			pesLength++;
			if (channel >= 0x80 && channel <= 0xCF) {
				skip(is, 3);
				pesLength += 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(is, 1);
					pesLength++;
				}
			} else {
				skip(is, 3);
				pesLength += 3;
			}
		}

		return pesLength;
	}

	private void addVideoData(InputStream is, int length) {
		if (videoDataOffset + length > videoData.length) {
			// Extend the inputBuffer
			int[] newVideoData = new int[videoDataOffset + length];
			System.arraycopy(videoData, 0, newVideoData, 0, videoDataOffset);
			videoData = newVideoData;
		}

		for (int i = 0; i < length; i++) {
			videoData[videoDataOffset++] = read8(is);
		}
	}

	private boolean readPsmfHeader() {
		try {
            is = iso.getFile(fileName);
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			log.error("readPsmfHeader", e);
			return false;
		}

		if (read32(is) != 0x50534D46) { // PSMF
			return false;
		}

		skip(is, 4);
		int mpegOffset = read32(is);
		skip(is, sceMpeg.PSMF_FRAME_WIDTH_OFFSET - sceMpeg.PSMF_STREAM_SIZE_OFFSET);
		videoWidth = read8(is) << 4;
		videoHeight = read8(is) << 4;
		skip(is, mpegOffset - sceMpeg.PSMF_FRAME_HEIGHT_OFFSET - 1);

		return true;
	}

	private boolean readPsmfPacket(int videoChannel) {
		while (true) {
			int startCode = read32(is);
			if (startCode == -1) {
				// End of file
				return false;
			}
			int codeLength;
			switch (startCode) {
				case 0x1BA: // PACK_START_CODE
					skip(is, 10);
					break;
				case 0x1BB: // SYSTEM_HEADER_START_CODE
					skip(is, 14);
					break;
				case 0x1BE: // PADDING_STREAM
				case 0x1BF: // PRIVATE_STREAM_2
				case 0x1BD: // PRIVATE_STREAM_1, Audio stream
					codeLength = read16(is);
					skip(is, codeLength);
					break;
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16(is);
					if (videoChannel < 0 || startCode - 0x1E0 == videoChannel) {
						int pesLength = skipPesHeader(is, startCode);
						codeLength -= pesLength;
						addVideoData(is, codeLength);
						return true;
					}
					skip(is, codeLength);
					break;
			}
		}
	}

	private void consumeVideoData(int length) {
		if (length >= videoDataOffset) {
			videoDataOffset = 0;
		} else {
			System.arraycopy(videoData, length, videoData, 0, videoDataOffset - length);
			videoDataOffset -= length;
		}
	}

	private int findFrameEnd() {
		for (int i = 5; i < videoDataOffset; i++) {
			if (videoData[i - 4] == 0x00 &&
			    videoData[i - 3] == 0x00 &&
			    videoData[i - 2] == 0x00 &&
			    videoData[i - 1] == 0x01) {
				int naluType = videoData[i] & 0x1F;
				if (naluType == H264Context.NAL_AUD) {
					return i - 4;
				}
			}
		}

		return -1;
	}

	private void init() {
        image = null;
        done = false;
        threadExit = false;
    }

    private Image getImage() {
        return image;
    }

    final public boolean initVideo() {
        if (!startVideo()) {
            return false;
        }

        displayThread = new PmfDisplayThread();
        displayThread.setDaemon(true);
        displayThread.setName("UMD Browser - PMF Display Thread");
        displayThread.start();

        return true;
    }

    private boolean startVideo() {
        endOfVideo = false;

        if (!readPsmfHeader()) {
        	return false;
        }

        videoCodec = CodecFactory.getVideoCodec();
        videoCodec.init();

        startTime = System.currentTimeMillis();
        frame = 0;

        return true;
    }

    private void closeVideo() {
    	videoCodec = null;

        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore Exception
            }
            is = null;
        }
    }

    private void loopVideo() {
        closeVideo();
        startVideo();
    }

    private void stopDisplayThread() {
        while (displayThread != null && !threadExit) {
            done = true;
            Utilities.sleep(1, 0);
        }
        displayThread = null;
    }

    public void stopVideo() {
        stopDisplayThread();
        closeVideo();
    }

    public void stepVideo() {
    	image = null;

    	int frameSize = -1;
	    do {
	    	if (!readPsmfPacket(videoChannel)) {
	    		if (videoDataOffset <= 0) {
	    			// Enf of file reached
	    			break;
	    		}
	    		frameSize = findFrameEnd();
	    		if (frameSize < 0) {
		    		// Process pending last frame
	    			frameSize = videoDataOffset;
	    		}
	    	} else {
	    		frameSize = findFrameEnd();
	    	}
	    } while (frameSize <= 0);

	    if (frameSize <= 0) {
	    	endOfVideo = true;
	    	return;
	    }

	    int consumedLength = videoCodec.decode(videoData, 0, frameSize);
	    if (consumedLength < 0) {
	    	endOfVideo = true;
	    	return;
	    }
	    consumeVideoData(consumedLength);

	    if (videoCodec.hasImage()) {
	    	int width = videoCodec.getImageWidth();
	    	int height = videoCodec.getImageHeight();
	    	int size = width * height;
	    	int size2 = size >> 2;
	    	int luma[] = new int[size];
	    	int cr[] = new int[size2];
	    	int cb[] = new int[size2];
	    	if (videoCodec.getImage(luma, cb, cr) == 0) {
	    		int abgr[] = new int[size];
	    		H264Utils.YUV2ARGB(width, height, luma, cb, cr, abgr);
	    		image = display.createImage(new MemoryImageSource(videoWidth, videoHeight, abgr, 0, width));

	    		frame++;

	    		long now = System.currentTimeMillis();
			    long currentDuration = now - startTime;
			    long videoDuration = frame * 100000L / 3003L;
			    if (currentDuration < videoDuration) {
			    	Utilities.sleep((int) (videoDuration - currentDuration), 0);
			    }
	    	}
	    }
    }

    private class PmfDisplayThread extends Thread {
        @Override
        public void run() {
            while (!done) {
                while (!endOfVideo && !done) {
                    stepVideo();

                    if (display != null && getImage() != null) {
                        display.setIcon(new ImageIcon(getImage()));
                    }
                }

                if (!done) {
                    loopVideo();
                }
            }

            threadExit = true;
        }
    }
}

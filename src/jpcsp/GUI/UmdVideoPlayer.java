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

import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.HLE.modules150.sceMpeg.mpegTimestampPerSecond;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PACK_START_CODE;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PADDING_STREAM;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_1;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_2;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.SYSTEM_HEADER_START_CODE;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.sleep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.hardware.Screen;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.IVideoCodec;
import jpcsp.media.codec.h264.H264Utils;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceMpeg;

import com.twilight.h264.decoder.H264Context;

public class UmdVideoPlayer implements KeyListener {
	private static Logger log = Logger.getLogger("videoplayer");

    // ISO file
    private String fileName;
    private UmdIsoReader iso;
    private UmdIsoFile isoFile;

    // Stream storage
    private List<MpsStreamInfo> mpsStreams;
    private int currentStreamIndex;

    // Display
    private JLabel display;
    private int screenWidth;
    private int screenHeigth;
    private Image image;

    // Video
    private IVideoCodec videoCodec;
	private int[] videoData = new int[0x10000];
	private int videoDataOffset;
	private int videoChannel = 0;
	private int frame;
	private int videoWidth;
	private int videoHeight;
	private int[] luma;
	private int[] cr;
	private int[] cb;
	private int[] abgr;
	private boolean foundFrameStart;
	// Audio
    private ICodec audioCodec;
    private boolean audioCodecInitialized;
	private int[] audioData = new int[0x10000];
	private int audioDataOffset;
    private int audioFrameLength;
    private int audioChannels;
    private final int frameHeader[] = new int[8];
    private int frameHeaderLength;
	private int audioChannel = 0;
	private int samplesAddr = MemoryMap.START_USERSPACE;
	private int audioBufferAddr = MemoryMap.START_USERSPACE + 0x10000;
	private byte[] audioBytes;
	// Time synchronization
	private int pesHeaderChannel;
	private long startTime;
    private int fastForwardSpeed;
    private int fastRewindSpeed;
    private static final int fastForwardSpeeds[] = new int[] { 1, 50, 100, 200, 400 };
    private static final int fastRewindSpeeds[] = new int[] { 1, 50, 100, 200, 400 };

    // State (for sync thread).
    private volatile boolean videoPaused;
    private volatile boolean done;
    private volatile boolean endOfVideo;
    private volatile boolean threadExit;

    // Internal data
    private MpsDisplayThread displayThread;
    private SourceDataLine mLine;

    // MPS stream class.
    protected class MpsStreamInfo {
        private String streamName;
        private int streamWidth;
        private int streamHeigth;
        private int streamFirstTimestamp;
        private int streamLastTimestamp;
        private MpsStreamMarkerInfo[] streamMarkers;

        public MpsStreamInfo(String name, int width, int heigth, int firstTimestamp, int lastTimestamp, MpsStreamMarkerInfo[] markers) {
            streamName = name;
            streamWidth = width;
            streamHeigth = heigth;
            streamFirstTimestamp = firstTimestamp;
            streamLastTimestamp = lastTimestamp;
            streamMarkers = markers;
        }

        public String getName() {
            return streamName;
        }

        public int getWidth() {
            return streamWidth;
        }

        public int getHeigth() {
            return streamHeigth;
        }

        public int getFirstTimestamp() {
            return streamFirstTimestamp;
        }

        public int getLastTimestamp() {
            return streamLastTimestamp;
        }

        public MpsStreamMarkerInfo[] getMarkers() {
            return streamMarkers;
        }

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("name='%s', %dx%d, %s(%d to %d), markers=[", getName(), getWidth(), getHeigth(), getTimestampString(getLastTimestamp() - getFirstTimestamp()), getFirstTimestamp(), getLastTimestamp()));
			for (int i = 0; i < streamMarkers.length; i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(streamMarkers[i]);
			}
			s.append("]");

			return s.toString();
		}
    }

    // MPS stream's marker class.
    protected class MpsStreamMarkerInfo {
        private String streamMarkerName;
        private int streamMarkerTimestamp;

        public MpsStreamMarkerInfo(String name, int timestamp) {
            streamMarkerName = name;
            streamMarkerTimestamp = timestamp;
        }

        public String getName() {
            return streamMarkerName;
        }

        public int getTimestamp() {
            return streamMarkerTimestamp;
        }

		@Override
		public String toString() {
			return String.format("'%s' %s(timeStamp=%d)", getName(), getTimestampString(getTimestamp()), getTimestamp());
		}
    }

    private static String getTimestampString(int timestamp) {
    	int seconds = timestamp / mpegTimestampPerSecond;
    	int minutes = seconds / 60;
    	seconds -= minutes * 60;
    	int hours = minutes / 60;
    	minutes -= hours * 60;

    	if (hours == 0) {
    		return String.format("%02d:%02d", minutes, seconds);
    	}

    	return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public UmdVideoPlayer(MainGUI gui, UmdIsoReader iso) {
        this.iso = iso;

        display = new JLabel();
        gui.remove(Modules.sceDisplayModule.getCanvas());
        gui.getContentPane().add(display, BorderLayout.CENTER);
        gui.addKeyListener(this);
        setVideoPlayerResizeScaleFactor(gui, 1);

        init();
    }

    @Override
    public void keyPressed(KeyEvent keyCode) {
        if (keyCode.getKeyCode() == KeyEvent.VK_RIGHT) {
            stopDisplayThread();
            goToNextMpsStream();
        } else if (keyCode.getKeyCode() == KeyEvent.VK_LEFT && currentStreamIndex > 0) {
            stopDisplayThread();
            goToPreviousMpsStream();
        } else if (keyCode.getKeyCode() == KeyEvent.VK_W && !videoPaused) {
            pauseVideo();
        } else if (keyCode.getKeyCode() == KeyEvent.VK_S) {
            resumeVideo();
        } else if (keyCode.getKeyCode() == KeyEvent.VK_A) {
            rewind();
        } else if (keyCode.getKeyCode() == KeyEvent.VK_D) {
            fastForward();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyCode) {
    }

    @Override
    public void keyTyped(KeyEvent keyCode) {
    }

    private void init() {
        done = false;
        threadExit = false;
        isoFile = null;
        mpsStreams = new LinkedList<UmdVideoPlayer.MpsStreamInfo>();
        currentStreamIndex = 0;
        parsePlaylistFile();
        log.info("Setting aspect ratio to 16:9");
        if (currentStreamIndex < mpsStreams.size()) {
            MpsStreamInfo info = mpsStreams.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            log.info("Loading stream: " + fileName);
            try {
                isoFile = iso.getFile(fileName);
                // Look for valid CLIPINF files (contain the ripped off PSMF header from the
                // MPS streams).
                String cpiFileName = "UMD_VIDEO/CLIPINF/" + info.getName() + ".CLP";
                UmdIsoFile cpiFile = iso.getFile(cpiFileName);
                if (cpiFile != null) {
                    log.info("Found CLIPINF data for this stream: " + cpiFileName);
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                Emulator.log.error(e);
            }
        }
        if (isoFile != null) {
            startVideo();
        }
    }

    public void setVideoPlayerResizeScaleFactor(MainGUI gui, int factor) {
        screenWidth = Screen.width * factor;
        screenHeigth = Screen.height * factor;
        Insets insets = gui.getInsets();
        Dimension minSize = new Dimension(
                screenWidth + insets.left + insets.right,
                screenHeigth + insets.top + insets.bottom);
        gui.setMinimumSize(minSize);
    }

    @SuppressWarnings("unused")
	private void parsePlaylistFile() {
        try {
            UmdIsoFile file = iso.getFile("UMD_VIDEO/PLAYLIST.UMD");
            int umdvMagic = file.readInt();
            int umdvVersion = file.readInt();
            int globalDataOffset = endianSwap32(file.readInt());
            file.seek(globalDataOffset);
            int playListSize = endianSwap32(file.readInt());
            int playListTracksNum = endianSwap16(file.readShort());
            file.skipBytes(2); // NULL.
            if (umdvMagic != 0x56444D55) { // UMDV
                log.warn("Accessing invalid PLAYLIST.UMD file!");
            } else {
                log.info("Accessing valid PLAYLIST.UMD file: playListSize=" + playListSize + ", playListTracksNum=" + playListTracksNum);
            }
            for (int i = 0; i < playListTracksNum; i++) {
                file.skipBytes(2);   // 0x035C.
                file.skipBytes(2);   // 0x0310.
                file.skipBytes(2);   // 0x0332.
                file.skipBytes(30);  // NULL.
                file.skipBytes(2);   // 0x02E8.
                file.skipBytes(2);   // 0x0000/0x1000.
                int releaseDateYear = endianSwap16(file.readShort());
                int releaseDateDay = file.readByte();
                int releaseDateMonth = file.readByte();
                file.skipBytes(4);   // NULL.
                file.skipBytes(4);   // Unknown (found 0x00000900).
                file.skipBytes(1);   // Unknown size.
                file.skipBytes(732); // Unknown NULL area with size 0x2DC.
                int streamHeight = (int) (file.readByte() * 0x10); // Stream's original height.
                file.skipBytes(2);   // NULL.
                file.skipBytes(4);   // 0x00010000.
                file.skipBytes(1);   // NULL.
                int streamWidth = (int) (file.readByte() * 0x10); // Stream's original width.
                file.skipBytes(1);   // NULL.
                int streamNameCharsNum = (int) file.readByte();   // Stream's name non null characters count.
                byte[] stringBuf = new byte[5];
                file.read(stringBuf, 0, 5);
                String streamName = new String(stringBuf);
                file.skipBytes(3); // NULL chars.
                file.skipBytes(2); // NULL.
                int streamFirstTimestamp = endianSwap32(file.readInt());
                file.skipBytes(2); // NULL.
                int streamLastTimestamp = endianSwap32(file.readInt());
                file.skipBytes(2); // NULL.
                int streamMarkerDataLength = endianSwap16(file.readShort());  // Stream's markers' data length.
                int streamMarkersNum = endianSwap16(file.readShort());  // Stream's number of markers.
                MpsStreamMarkerInfo[] streamMarkers = new MpsStreamMarkerInfo[streamMarkersNum];
                for (int j = 0; j < streamMarkersNum; j++) {
                    file.skipBytes(1); // 0x05.
                    int streamMarkerCharsNum = (int) file.readByte(); // Marker name length.
                    file.skipBytes(4); // NULL.
                    int streamMarkerTimestamp = endianSwap32(file.readInt());
                    file.skipBytes(2); // NULL.
                    file.skipBytes(4); // NULL.
                    byte[] markerBuf = new byte[24];
                    file.read(markerBuf, 0, 24);
                    String markerName = new String(markerBuf, 0, streamMarkerCharsNum);
                    if ((j + 1) == streamMarkersNum) {
                        file.skip(2); // Skip terminator (NULL).
                    }
                    streamMarkers[j] = new MpsStreamMarkerInfo(markerName, streamMarkerTimestamp);
                }
                // Map this stream.
                MpsStreamInfo info = new MpsStreamInfo(streamName, streamWidth, streamHeight, streamFirstTimestamp, streamLastTimestamp, streamMarkers);
                if (log.isDebugEnabled()) {
                	log.debug(String.format("StreamInfo #%d: %s", i, info));
                }
                mpsStreams.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean goToNextMpsStream() {
    	if (currentStreamIndex + 1 >= mpsStreams.size()) {
    		return false;
    	}

    	currentStreamIndex++;
        MpsStreamInfo info = mpsStreams.get(currentStreamIndex);
        fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
        log.info("Loading stream: " + fileName);
        try {
            isoFile = iso.getFile(fileName);
            String cpiFileName = "UMD_VIDEO/CLIPINF/" + info.getName() + ".CLP";
            UmdIsoFile cpiFile = iso.getFile(cpiFileName);
            if (cpiFile != null) {
                log.info("Found CLIPINF data for this stream: " + cpiFileName);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            Emulator.log.error(e);
        }

        if (isoFile != null) {
            startVideo();
            initVideo();
        }

        return true;
    }

    private boolean goToPreviousMpsStream() {
    	if (currentStreamIndex <= 0) {
    		return false;
    	}

    	currentStreamIndex--;
        MpsStreamInfo info = mpsStreams.get(currentStreamIndex);
        fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
        log.info("Loading stream: " + fileName);
        try {
            isoFile = iso.getFile(fileName);
            String cpiFileName = "UMD_VIDEO/CLIPINF/" + info.getName() + ".CLP";
            UmdIsoFile cpiFile = iso.getFile(cpiFileName);
            if (cpiFile != null) {
                log.info("Found CLIPINF data for this stream: " + cpiFileName);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            Emulator.log.error(e);
        }

        if (isoFile != null) {
            startVideo();
            initVideo();
        }

        return true;
    }

    public void initVideo() {
    	done = false;
        videoPaused = false;
        if (displayThread == null) {
            displayThread = new MpsDisplayThread();
            displayThread.setDaemon(true);
            displayThread.setName("UMD Video Player Thread");
            displayThread.start();
        }
    }

    public void pauseVideo() {
        videoPaused = true;
    }

    public void resumeVideo() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Resume video"));
    	}
        videoPaused = false;
        fastForwardSpeed = 0;
        fastRewindSpeed = 0;
	}

    public void fastForward() {
    	if (fastRewindSpeed > 0) {
    		fastRewindSpeed--;
    	} else {
    		fastForwardSpeed = Math.min(fastForwardSpeeds.length - 1, fastForwardSpeed + 1);
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Fast forward %d, fast rewind %d", fastForwardSpeed, fastRewindSpeed));
    	}
    }

    public void rewind() {
    	if (fastForwardSpeed > 0) {
    		fastForwardSpeed--;
    	} else {
    		fastRewindSpeed = Math.min(fastRewindSpeeds.length - 1, fastRewindSpeed + 1);
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Fast forward %d, fast rewind %d", fastForwardSpeed, fastRewindSpeed));
    	}
    }

	private int read8() {
		try {
			return isoFile.read();
		} catch (IOException e) {
			// Ignore exception
		}

		return -1;
	}

	private int read16() {
		return (read8() << 8) | read8();
	}

	private int read32() {
		return (read8() << 24) | (read8() << 16) | (read8() << 8) | read8();
	}

	private void skip(int n) {
		if (n > 0) {
			try {
				isoFile.skip(n);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int skipPesHeader(int startCode) {
		int pesLength = 0;
		int c = read8();
		pesLength++;
		while (c == 0xFF) {
			c = read8();
			pesLength++;
		}

		if ((c & 0xC0) == 0x40) {
			skip(1);
			c = read8();
			pesLength += 2;
		}

		if ((c & 0xE0) == 0x20) {
			skip(4);
			pesLength += 4;
			if ((c & 0x10) != 0) {
				skip(5);
				pesLength += 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			skip(1);
			int headerLength = read8();
			pesLength += 2;
			skip(headerLength);
			pesLength += headerLength;
		}

		if (startCode == 0x1BD) { // PRIVATE_STREAM_1
			int channel = read8();
			pesHeaderChannel = channel;
			pesLength++;
			if (channel >= 0x80 && channel <= 0xCF) {
				skip(3);
				pesLength += 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(1);
					pesLength++;
				}
			} else {
				skip(3);
				pesLength += 3;
			}
		}

		return pesLength;
	}

	private byte[] resize(byte[] array, int size) {
		if (array == null) {
			return new byte[size];
		}

		if (size <= array.length) {
			return array;
		}

		byte[] newArray = new byte[size];
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	private int[] resize(int[] array, int size) {
		if (array == null) {
			return new int[size];
		}

		if (size <= array.length) {
			return array;
		}

		int[] newArray = new int[size];
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	private void addVideoData(int length, long position) {
		videoData = resize(videoData, videoDataOffset + length);

		for (int i = 0; i < length; i++) {
			videoData[videoDataOffset++] = read8();
		}
	}

	private void addAudioData(int length) {
		audioData = resize(audioData, audioDataOffset + length);

		while (length > 0) {
    		int currentFrameLength = audioFrameLength == 0 ? 0 : audioDataOffset % audioFrameLength;
    		if (currentFrameLength == 0) {
    			// 8 bytes header:
    			// - byte 0: 0x0F
    			// - byte 1: 0xD0
    			// - byte 2: 0x28
    			// - byte 3: (frameLength - 8) / 8
    			// - bytes 4-7: 0x00
    			while (frameHeaderLength < frameHeader.length && length > 0) {
    				frameHeader[frameHeaderLength++] = read8();
    				length--;
    			}
    			if (frameHeaderLength < frameHeader.length) {
    				// Frame header not yet complete
    				break;
    			}
    			if (length == 0) {
    				// Frame header is complete but no data is following the header.
    				// Retry when some data is available
    				break;
    			}

    			int frameHeader23 = (frameHeader[2] << 8) | frameHeader[3];
    			audioFrameLength = ((frameHeader23 & 0x3FF) << 3) + 8;
    			if (frameHeader[0] != 0x0F || frameHeader[1] != 0xD0) {
    				if (log.isInfoEnabled()) {
    					log.warn(String.format("Audio frame length 0x%X with incorrect header (header: %02X %02X %02X %02X %02X %02X %02X %02X)", audioFrameLength, frameHeader[0], frameHeader[1], frameHeader[2], frameHeader[3], frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7]));
    				}
    			} else if (log.isTraceEnabled()) {
    				log.trace(String.format("Audio frame length 0x%X (header: %02X %02X %02X %02X %02X %02X %02X %02X)", audioFrameLength, frameHeader[0], frameHeader[1], frameHeader[2], frameHeader[3], frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7]));
    			}

    			frameHeaderLength = 0;
    		}
    		int lengthToNextFrame = audioFrameLength - currentFrameLength;
    		int readLength = Utilities.min(length, lengthToNextFrame);
    		for (int i = 0; i < readLength; i++) {
    			audioData[audioDataOffset++] = read8();
    		}
    		length -= readLength;
		}
	}

	private long getCurrentFilePosition() {
		try {
			return isoFile.getFilePointer();
		} catch (IOException e) {
		}

		return -1L;
	}

	private boolean readPsmfPacket(int videoChannel, int audioChannel) {
		while (true) {
			int startCode = read32();
			if (startCode == -1) {
				// End of file
				return false;
			}
			int codeLength, pesLength;
			switch (startCode) {
				case PACK_START_CODE:
					skip(10);
					break;
				case SYSTEM_HEADER_START_CODE:
					skip(14);
					break;
				case PADDING_STREAM:
				case PRIVATE_STREAM_2:
					codeLength = read16();
					skip(codeLength);
					break;
				case PRIVATE_STREAM_1: // Audio stream
					codeLength = read16();
					pesHeaderChannel = audioChannel;
					pesLength = skipPesHeader(startCode);
					codeLength -= pesLength;
					if (pesHeaderChannel == audioChannel || audioChannel < 0) {
						addAudioData(codeLength);
						return true;
					}
					skip(codeLength);
					break;
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16();
					if (videoChannel < 0 || startCode - 0x1E0 == videoChannel) {
						pesLength = skipPesHeader(startCode);
						codeLength -= pesLength;
						addVideoData(codeLength, getCurrentFilePosition());
						return true;
					}
					skip(codeLength);
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

	private void consumeAudioData(int length) {
		if (length >= audioDataOffset) {
			audioDataOffset = 0;
		} else {
			System.arraycopy(audioData, length, audioData, 0, audioDataOffset - length);
			audioDataOffset -= length;
		}
	}

	private int findVideoFrameEnd() {
		for (int i = 5; i < videoDataOffset; i++) {
			if (videoData[i - 4] == 0x00 &&
			    videoData[i - 3] == 0x00 &&
			    videoData[i - 2] == 0x00 &&
			    videoData[i - 1] == 0x01) {
				int naluType = videoData[i] & 0x1F;
				if (naluType == H264Context.NAL_AUD) {
					foundFrameStart = false;
					return i - 4;
				}
				if (naluType == H264Context.NAL_SLICE || naluType == H264Context.NAL_IDR_SLICE) {
					if (foundFrameStart) {
						return i - 4;
					}
					foundFrameStart = true;
				} else {
					foundFrameStart = false;
				}
			}
		}

		return -1;
	}

	public boolean startVideo() {
        endOfVideo = false;
        videoPaused = false;

        videoCodec = CodecFactory.getVideoCodec();
        videoCodec.init(null);
        videoDataOffset = 0;
        videoWidth = 0;
        videoHeight = 0;

        audioCodec = CodecFactory.getCodec(PSP_CODEC_AT3PLUS);
        audioCodecInitialized = false;
        audioChannels = 2;
        audioDataOffset = 0;
        audioFrameLength = 0;
        frameHeaderLength = 0;
        foundFrameStart = false;

        startTime = System.currentTimeMillis();
        frame = 0;

        return true;
    }

    private void closeVideo() {
    	videoCodec = null;
    	if (isoFile != null) {
    		try {
				isoFile.seek(0);
			} catch (IOException e) {
				// Ignore exception
			}
    	}
    }

    private void stopDisplayThread() {
    	done = true;
        while (displayThread != null && !threadExit) {
            sleep(1, 0);
        }
        displayThread = null;
    }

    public void stopVideo() {
        stopDisplayThread();
        closeVideo();
        closeAudio();
        if (isoFile != null) {
            try {
                isoFile.close();
            } catch (IOException e) {
                // Ignore Exception
            }
        }
    }

    public void stepVideo() {
    	image = null;

    	int frameSize = -1;
	    do {
	    	if (!readPsmfPacket(videoChannel, audioChannel)) {
	    		if (videoDataOffset <= 0) {
	    			// Enf of file reached
	    			break;
	    		}
	    		frameSize = findVideoFrameEnd();
	    		if (frameSize < 0) {
		    		// Process pending last frame
	    			frameSize = videoDataOffset;
	    		}
	    	} else {
	    		frameSize = findVideoFrameEnd();
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

	    if (videoCodec.hasImage()) {
	    	frame++;
	    }

	    consumeVideoData(consumedLength);

	    boolean skipFrame = false;
		if ((frame % fastForwardSpeeds[fastForwardSpeed]) != 0) {
			skipFrame = true;
			startTime -= sceMpeg.videoTimestampStep;
		}

	    if (videoCodec.hasImage() && !skipFrame) {
	    	int width = videoCodec.getImageWidth();
	    	int height = videoCodec.getImageHeight();
	    	if (videoWidth <= 0) {
	    		videoWidth = width;
	    	}
	    	if (videoHeight <= 0) {
	    		videoHeight = height;
	    	}

	    	int size = width * height;
	    	int size2 = size >> 2;
	    	luma = resize(luma, size);
	    	cr = resize(cr, size2);
	    	cb = resize(cb, size2);

	    	if (videoCodec.getImage(luma, cb, cr) == 0) {
	    		abgr = resize(abgr, size);
	    		H264Utils.YUV2ARGB(width, height, luma, cb, cr, abgr);
	    		image = display.createImage(new MemoryImageSource(videoWidth, videoHeight, abgr, 0, width));

	    		long now = System.currentTimeMillis();
			    long currentDuration = now - startTime;
			    long videoDuration = frame * 100000L / sceMpeg.videoTimestampStep;
			    if (currentDuration < videoDuration) {
			    	Utilities.sleep((int) (videoDuration - currentDuration), 0);
			    }
	    	}
	    }

	    if (audioFrameLength > 0 && audioDataOffset >= audioFrameLength) {
	    	if (!audioCodecInitialized) {
	    		audioCodec.init(audioFrameLength, audioChannels, audioChannels, 0);

	    		AudioFormat audioFormat = new AudioFormat(44100, 16, audioChannels, true, false);
	            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
	    		try {
	    			mLine = (SourceDataLine) AudioSystem.getLine(info);
	    	        mLine.open(audioFormat);
	    		} catch (LineUnavailableException e) {
	    			// Ignore error
	    		}
	            mLine.start();

	    		audioCodecInitialized = true;
	    	}

	    	int result = -1;
	    	if (fastForwardSpeed == 0) {
		    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(audioBufferAddr, audioFrameLength, 1);
		    	for (int i = 0; i < audioFrameLength; i++) {
		    		memoryWriter.writeNext(audioData[i]);
		    	}
		    	memoryWriter.flush();
		    	result = audioCodec.decode(audioBufferAddr, audioFrameLength, samplesAddr);
	    	}
    		consumeAudioData(audioFrameLength);

	    	if (result > 0) {
	    		int audioBytesLength = audioCodec.getNumberOfSamples() * 2 * audioChannels;
	    		audioBytes = resize(audioBytes, audioBytesLength);
		    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(samplesAddr, audioBytesLength, 1);
				for (int i = 0; i < audioBytesLength; i++) {
					audioBytes[i] = (byte) memoryReader.readNext();
				}
				mLine.write(audioBytes, 0, audioBytesLength);
	    	}
	    }
    }

    private void closeAudio() {
        if (mLine != null) {
            mLine.drain();
            mLine.close();
            mLine = null;
        }
    }

    public void takeScreenshot() {
        int tag = 0;
        String screenshotName = State.title + "-" + "Shot" + "-" + tag + ".png";
        File screenshot = new File(screenshotName);
        File directory = new File(System.getProperty("user.dir"));
        for(File file : directory.listFiles()) {
            if (file.getName().contains(State.title + "-" + "Shot")) {
                screenshotName = State.title + "-" + "Shot" + "-" + ++tag + ".png";
                screenshot = new File(screenshotName);
            }
        }
        try {
            BufferedImage img = (BufferedImage)getImage();
            ImageIO.write(img, "png", screenshot);
            img.flush();
        } catch (Exception e) {
            return;
        }
    }

    private Image getImage() {
        return image;
    }

    private class MpsDisplayThread extends Thread {

        @Override
        public void run() {
            if (log.isTraceEnabled()) {
            	log.trace(String.format("Starting Mps Display thread"));
            }

            threadExit = false;

            while (!done) {
                while (!endOfVideo && !done) {
                    if (!videoPaused) {
                        stepVideo();
                        if (display != null && image != null) {
                            display.setIcon(new ImageIcon(getImage()));
                        }
                    } else {
                    	Utilities.sleep(10, 0);
                    }
                }
                if (!done) {
                    if (log.isTraceEnabled()) {
                    	log.trace(String.format("Switching to next stream"));
                    }
                	if (!goToNextMpsStream()) {
                		done = true;
                	}
                }
            }

            threadExit = true;

            if (log.isTraceEnabled()) {
            	log.trace(String.format("Exiting Mps Display thread"));
            }
        }
    }
}
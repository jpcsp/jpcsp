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

import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_CIRCLE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_CROSS;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_DOWN;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_LEFT;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_LTRIGGER;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_RIGHT;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_RTRIGGER;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_TRIANGLE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_UP;
import static jpcsp.HLE.modules.sceMpeg.UNKNOWN_TIMESTAMP;
import static jpcsp.HLE.modules.sceMpeg.mpegTimestampPerSecond;
import static jpcsp.HLE.modules.scePsmfPlayer.videoTimestampStep;
import static jpcsp.HLE.modules.sceUtility.PSP_SYSTEMPARAM_BUTTON_CROSS;
import static jpcsp.HLE.modules.sceUtility.getSystemParamButtonPreference;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PACK_START_CODE;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PADDING_STREAM;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_1;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_2;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.SYSTEM_HEADER_START_CODE;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.sleep;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.RCO;
import jpcsp.format.psmf.PesHeader;
import jpcsp.format.rco.Display;
import jpcsp.format.rco.RCOState;
import jpcsp.format.rco.vsmx.objects.MoviePlayer;
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
import jpcsp.HLE.modules.sceUtility;

import com.twilight.h264.decoder.GetBitContext;
import com.twilight.h264.decoder.H264Context;

public class UmdVideoPlayer implements KeyListener {
	private static Logger log = Logger.getLogger("videoplayer");
	private static final boolean dumpFrames = false;

    // ISO file
    private String fileName;
    private UmdIsoReader iso;
    private UmdIsoFile isoFile;

    // Stream storage
    private List<MpsStreamInfo> mpsStreams;
    private int currentStreamIndex;

    // Display
    private JLabel display;
    private Display rcoDisplay;
    private int screenWidth;
    private int screenHeigth;
    private Image image;
    private int resizeScaleFactor;
    private MainGUI gui;

    // Video
    private IVideoCodec videoCodec;
    private boolean videoCodecInit;
	private int[] videoData = new int[0x10000];
	private int videoDataOffset;
	private int videoChannel = 0;
	public static int frame;
	private int videoWidth;
	private int videoHeight;
	private int videoAspectRatioNum;
	private int videoAspectRatioDen;
	private int[] luma;
	private int[] cr;
	private int[] cb;
	private int[] abgr;
	private boolean foundFrameStart;
	private int parseState;
	private int[] parseHistory = new int[6];
	private int parseHistoryCount;
	private int parseLastMb;
	private int nalLengthSize = 0;
	private boolean isAvc = false;
	private int lastParsePosition;
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
	private PesHeader pesHeaderAudio;
	private PesHeader pesHeaderVideo;
	private long currentVideoTimestamp;
	private int currentChapterNumber;
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
    private Memory mem;

    // RCO MoviePlayer
    private MoviePlayer moviePlayer;
    private RCOState rcoState;
    private DisplayControllerThread displayControllerThread;

    // MPS stream class.
    protected class MpsStreamInfo {
        private String streamName;
        private int streamWidth;
        private int streamHeigth;
        private int streamFirstTimestamp;
        private int streamLastTimestamp;
        private MpsStreamMarkerInfo[] streamMarkers;
        private int playListNumber;

        public MpsStreamInfo(String name, int width, int heigth, int firstTimestamp, int lastTimestamp, MpsStreamMarkerInfo[] markers, int playListNumber) {
            streamName = name;
            streamWidth = width;
            streamHeigth = heigth;
            streamFirstTimestamp = firstTimestamp;
            streamLastTimestamp = lastTimestamp;
            streamMarkers = markers;
            this.playListNumber = playListNumber;
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

        public int getPlayListNumber() {
        	return playListNumber;
        }

        public int getChapterNumber(long timestamp) {
        	int marker = -1;
        	if (streamMarkers != null) {
        		for (int i = 0; i < streamMarkers.length; i++) {
	        		if (streamMarkers[i].getTimestamp() <= timestamp) {
	        			marker = i;
	        		} else {
	        			break;
	        		}
	        	}
        	}

        	return marker;
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
        private long streamMarkerTimestamp;

        public MpsStreamMarkerInfo(String name, long timestamp) {
            streamMarkerName = name;
            streamMarkerTimestamp = timestamp;
        }

        public String getName() {
            return streamMarkerName;
        }

        public long getTimestamp() {
            return streamMarkerTimestamp;
        }

		@Override
		public String toString() {
			return String.format("'%s' %s(timeStamp=%d)", getName(), getTimestampString(getTimestamp()), getTimestamp());
		}
    }

    private static String getTimestampString(long timestamp) {
    	int seconds = (int) (timestamp / mpegTimestampPerSecond);
    	int hundredth = (int) (timestamp - ((long) seconds) * mpegTimestampPerSecond);
    	hundredth = 100 * hundredth / mpegTimestampPerSecond;
    	int minutes = seconds / 60;
    	seconds -= minutes * 60;
    	int hours = minutes / 60;
    	minutes -= hours * 60;

    	return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredth);
    }

    public UmdVideoPlayer(MainGUI gui, UmdIsoReader iso) {
        this.iso = iso;

        display = new JLabel();
        rcoDisplay = new Display();
        JPanel panel = new JPanel();
        panel.setLayout(new OverlayLayout(panel));
        panel.add(rcoDisplay);
        panel.add(display);
        gui.remove(Modules.sceDisplayModule.getCanvas());
        gui.getContentPane().add(panel, BorderLayout.CENTER);
        gui.addKeyListener(this);
        setVideoPlayerResizeScaleFactor(gui, 1);

        init();
    }

    public void exit() {
    	stopDisplayThread();
    }

    @Override
    public void keyPressed(KeyEvent event) {
    	State.controller.keyPressed(event);

    	if (moviePlayer != null) {
	    	if ((State.controller.getButtons() & PSP_CTRL_UP) != 0) {
    			moviePlayer.onUp();
	    	}
	    	if ((State.controller.getButtons() & PSP_CTRL_DOWN) != 0) {
    			moviePlayer.onDown();
	    	}
	    	if ((State.controller.getButtons() & PSP_CTRL_LEFT) != 0) {
    			moviePlayer.onLeft();
	    	}
	    	if ((State.controller.getButtons() & PSP_CTRL_RIGHT) != 0) {
    			moviePlayer.onRight();
	    	}
	    	int pushButton = getSystemParamButtonPreference() == PSP_SYSTEMPARAM_BUTTON_CROSS ? PSP_CTRL_CROSS : PSP_CTRL_CIRCLE;
	    	if ((State.controller.getButtons() & pushButton) != 0) {
    			moviePlayer.onPush();
	    	}

	    	// TODO Non-standard key mappings...
	    	if ((State.controller.getButtons() & PSP_CTRL_RTRIGGER) != 0) {
	            fastForward();
	    	}
	    	if ((State.controller.getButtons() & PSP_CTRL_LTRIGGER) != 0) {
	            rewind();
	    	}
	    	if ((State.controller.getButtons() & PSP_CTRL_TRIANGLE) != 0) {
	            resumeVideo();
	    	}
    	} else {
	    	if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
	            stopDisplayThread();
	            goToNextMpsStream();
	        } else if (event.getKeyCode() == KeyEvent.VK_LEFT && currentStreamIndex > 0) {
	            stopDisplayThread();
	            goToPreviousMpsStream();
	        } else if (event.getKeyCode() == KeyEvent.VK_W && !videoPaused) {
	            pauseVideo();
	        } else if (event.getKeyCode() == KeyEvent.VK_S) {
	            resumeVideo();
	        } else if (event.getKeyCode() == KeyEvent.VK_A) {
	            rewind();
	        } else if (event.getKeyCode() == KeyEvent.VK_D) {
	            fastForward();
	        } else if (event.getKeyCode() == KeyEvent.VK_UP) {
	        	if (moviePlayer != null) {
	        		moviePlayer.onUp();
	        	}
	        } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
	        	if (moviePlayer != null) {
	        		moviePlayer.onDown();
	        	}
	        }
    	}
    }

    @Override
    public void keyReleased(KeyEvent event) {
        State.controller.keyReleased(event);
    }

    @Override
    public void keyTyped(KeyEvent keyCode) {
    }

    private void init() {
    	Emulator.getScheduler().reset();
    	Emulator.getClock().resume();
    	mem = Emulator.getMemory();

    	displayControllerThread = new DisplayControllerThread();
    	displayControllerThread.setName("Display Controller Thread");
    	displayControllerThread.setDaemon(true);
    	displayControllerThread.start();

    	done = false;
        threadExit = false;
        isoFile = null;
        mpsStreams = new LinkedList<UmdVideoPlayer.MpsStreamInfo>();
        pauseVideo();
        currentStreamIndex = 0;
        parsePlaylistFile();
        parseRCO();

        if (videoPaused) {
        	goToMpsStream(currentStreamIndex);
        }
    }

    public void setVideoPlayerResizeScaleFactor(MainGUI gui, int resizeScaleFactor) {
    	this.resizeScaleFactor = resizeScaleFactor;
    	this.gui = gui;

    	resizeVideoPlayer();
    }

    private void resizeVideoPlayer() {
    	if (videoWidth <= 0 || videoHeight <= 0) {
	        screenWidth = Screen.width * resizeScaleFactor;
	        screenHeigth = Screen.height * resizeScaleFactor;
    	} else {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("video size %dx%d resizeScaleFactor=%d", videoWidth, videoHeight, resizeScaleFactor));
    		}
	        screenWidth = videoWidth * videoAspectRatioNum / videoAspectRatioDen * resizeScaleFactor;
	        screenHeigth = videoHeight * resizeScaleFactor;
    	}

        gui.setDisplayMinimumSize(screenWidth, screenHeigth);
    }

    private int readByteHexTo10(UmdIsoFile file) throws IOException {
    	int hex = file.readByte() & 0xFF;
    	return (hex >> 4) * 10 + (hex & 0x0F);
    }

    private void parsePlaylistFile() {
        try {
            UmdIsoFile file = iso.getFile("UMD_VIDEO/PLAYLIST.UMD");
            int umdvMagic = file.readInt();
            int umdvVersion = file.readInt();
            if (log.isDebugEnabled()) {
            	log.debug(String.format("Magic 0x%08X,  version 0x%08X", umdvMagic, umdvVersion));
            }
            int globalDataOffset = endianSwap32(file.readInt());
            file.seek(globalDataOffset);
            int playListSize = endianSwap32(file.readInt());
            int playListTracksNum = endianSwap16(file.readShort());
            file.skipBytes(2); // NULL.
            if (umdvMagic != 0x56444D55) { // UMDV
                log.warn("Accessing invalid PLAYLIST.UMD file!");
            } else {
                log.info(String.format("Accessing valid PLAYLIST.UMD file: playListSize=%d, playListTracksNum=%d", playListSize, playListTracksNum));
            }
            for (int i = 0; i < playListTracksNum; i++) {
                file.skipBytes(2);   // 0x035C.
                file.skipBytes(2);   // 0x0310.
                file.skipBytes(2);   // 0x0332.
                file.skipBytes(30);  // NULL.
                file.skipBytes(2);   // 0x02E8.
                int unknown = endianSwap16(file.readShort());
                int releaseDateYear = readByteHexTo10(file) * 100 + readByteHexTo10(file);
                int releaseDateDay = file.readByte();
                int releaseDateMonth = file.readByte();
                file.skipBytes(4);   // NULL.
                file.skipBytes(4);   // Unknown (found 0x00000900).
                int nameLength = file.readByte() & 0xFF;
                byte[] nameBuffer = new byte[nameLength];
                file.read(nameBuffer);
                String name = new String(nameBuffer);
                file.skipBytes(732 - nameLength); // Unknown NULL area with size 0x2DC.
                int streamHeight = (int) (file.readByte() * 0x10); // Stream's original height.
                file.skipBytes(2);   // NULL.
                file.skipBytes(4);   // 0x00010000.
                file.skipBytes(1);   // NULL.
                int streamWidth = (int) (file.readByte() * 0x10); // Stream's original width.
                file.skipBytes(1);   // NULL.
                int streamNameCharsNum = (int) file.readByte();   // Stream's name non null characters count.
                byte[] stringBuf = new byte[streamNameCharsNum];
                file.read(stringBuf);
                String streamName = new String(stringBuf);
                file.skipBytes(8 - streamNameCharsNum); // NULL chars.
                file.skipBytes(2); // NULL.
                int streamFirstTimestamp = endianSwap32(file.readInt());
                file.skipBytes(2); // NULL.
                int streamLastTimestamp = endianSwap32(file.readInt());
                file.skipBytes(2); // NULL.
                int streamMarkerDataLength = endianSwap16(file.readShort());  // Stream's markers' data length.
                MpsStreamMarkerInfo[] streamMarkers;
                if (streamMarkerDataLength > 0) {
	                int streamMarkersNum = endianSwap16(file.readShort());  // Stream's number of markers.
	                streamMarkers = new MpsStreamMarkerInfo[streamMarkersNum];
	                for (int j = 0; j < streamMarkersNum; j++) {
	                    file.skipBytes(1); // 0x05.
	                    int streamMarkerCharsNum = (int) file.readByte(); // Marker name length.
	                    file.skipBytes(4); // NULL.
	                    long streamMarkerTimestamp = endianSwap32(file.readInt()) & 0xFFFFFFFFL;
	                    file.skipBytes(2); // NULL.
	                    file.skipBytes(4); // NULL.
	                    byte[] markerBuf = new byte[streamMarkerCharsNum];
	                    file.read(markerBuf);
	                    String markerName = new String(markerBuf);
	                    file.skipBytes(24 - streamMarkerCharsNum);
	                    streamMarkers[j] = new MpsStreamMarkerInfo(markerName, streamMarkerTimestamp);
	                }
	                file.skip(2); // NULL
                } else {
                	streamMarkers = new MpsStreamMarkerInfo[0];
                }
                // Map this stream.
                MpsStreamInfo info = new MpsStreamInfo(streamName, streamWidth, streamHeight, streamFirstTimestamp, streamLastTimestamp, streamMarkers, i + 1);
                if (log.isDebugEnabled()) {
                	log.debug(String.format("Release date %d-%d-%d, name '%s', unknown=0x%04X", releaseDateYear, releaseDateMonth, releaseDateDay, name, unknown));
                	log.debug(String.format("StreamInfo #%d: %s", i, info));
                }
                mpsStreams.add(info);
            }
        } catch (Exception e) {
        	log.error("parsePlaylistFile", e);
        }
    }

    private void parseRCO() {
        try {
        	String[] resources = iso.listDirectory("UMD_VIDEO/RESOURCE");
        	if (resources == null || resources.length <= 0) {
        		return;
        	}

        	int preferredLanguage = sceUtility.getSystemParamLanguage();
    		String languagePrefix = "EN";
    		switch (preferredLanguage) {
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_JAPANESE: languagePrefix = "JA"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_ENGLISH: languagePrefix = "EN"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_FRENCH: languagePrefix = "FR"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_SPANISH: languagePrefix = "ES"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_GERMAN: languagePrefix = "DE"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_ITALIAN: languagePrefix = "IT"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_DUTCH: languagePrefix = "NL"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_PORTUGUESE: languagePrefix = "PO"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_RUSSIAN: languagePrefix = "RU"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_KOREAN: languagePrefix = "KO"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_CHINESE_TRADITIONAL: languagePrefix = "CN"; break;
    			case sceUtility.PSP_SYSTEMPARAM_LANGUAGE_CHINESE_SIMPLIFIED: languagePrefix = "CN"; break;
    		}

    		// The resource names are tried in this order:
    		final String resourceNames[] = new String[] {
    				"100000",
    				"000000",
    				"110000",
    				"010000"
    		};
    		String resourceFileName = null;
    		for (String resourceName : resourceNames) {
    			String fileName = languagePrefix + resourceName + ".RCO";
    			if (iso.hasFile("UMD_VIDEO/RESOURCE/" + fileName)) {
    				resourceFileName = fileName;
    				break;
    			}
    		}

    		if (resourceFileName != null) {
	        	if (log.isDebugEnabled()) {
	        		log.debug(String.format("Reading RCO file '%s'", resourceFileName));
	        	}
				UmdIsoFile file = iso.getFile("UMD_VIDEO/RESOURCE/" + resourceFileName);
				byte[] buffer = new byte[(int) file.length()];
				file.read(buffer);
				RCO rco = new RCO(buffer);
				if (log.isDebugEnabled()) {
					log.debug(String.format("RCO: %s", rco));
				}

				rcoState = rco.execute(this, resourceFileName.replace(".RCO", ""));
    		}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			log.error("parse RCO", e);
		}
        
    }

    public void changeResource(String resourceName) {
    	try {
			UmdIsoFile file = iso.getFile(String.format("UMD_VIDEO/RESOURCE/%s.RCO", resourceName));
	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("Reading RCO file '%s.RCO'", resourceName));
	    	}
			byte[] buffer = new byte[(int) file.length()];
			file.read(buffer);
			RCO rco = new RCO(buffer);
			if (log.isDebugEnabled()) {
				log.debug(String.format("RCO: %s", rco));
			}

			getRCODisplay().changeResource();
			rcoState = rco.execute(rcoState, this, resourceName);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			log.error("changeResource", e);
		}
    }

    private int getStreamIndexFromPlayListNumber(int playListNumber) {
    	for (int i = 0; i < mpsStreams.size(); i++) {
    		MpsStreamInfo info = mpsStreams.get(i);
    		if (info.getPlayListNumber() == playListNumber) {
    			return i;
    		}
    	}

    	return playListNumber;
    }

    public void setMoviePlayer(MoviePlayer moviePlayer) {
    	this.moviePlayer = moviePlayer;
    }

    public void play(int playListNumber, int chapterNumber, int videoNumber, int audioNumber, int audioFlag, int subtitleNumber, int subtitleFlag) {
    	done = false;
    	int streamIndex = getStreamIndexFromPlayListNumber(playListNumber);
    	goToMpsStream(streamIndex);
    }

    private boolean goToMpsStream(int streamIndex) {
    	if (streamIndex < 0 || streamIndex >= mpsStreams.size()) {
    		return false;
    	}

    	currentStreamIndex = streamIndex;
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

    private boolean goToNextMpsStream() {
    	return goToMpsStream(currentStreamIndex + 1);
    }

    private boolean goToPreviousMpsStream() {
    	return goToMpsStream(currentStreamIndex - 1);
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

	private long readPts(int c) {
		return (((long) (c & 0x0E)) << 29) | ((read16() >> 1) << 15) | (read16() >> 1);
	}

	private long readPts() {
		return readPts(read8());
	}

	private int readPesHeader(int startCode, PesHeader pesHeader) {
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

		pesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
		if ((c & 0xE0) == 0x20) {
			pesHeader.setDtsPts(readPts(c));
			pesLength += 4;
			if ((c & 0x10) != 0) {
				pesHeader.setPts(readPts());
				pesLength += 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			int flags = read8();
			int headerLength = read8();
			pesLength += 2;
			pesLength += headerLength;
			if ((flags & 0x80) != 0) {
				pesHeader.setDtsPts(readPts());
				headerLength -= 5;
				if ((flags & 0x40) != 0) {
					pesHeader.setDts(readPts());
					headerLength -= 5;
				}
			}
			if ((flags & 0x3F) != 0 && headerLength == 0) {
				flags &= 0xC0;
			}
			if ((flags & 0x01) != 0) {
				int pesExt = read8();
				headerLength--;
				int skip = (pesExt >> 4) & 0x0B;
				skip += skip & 0x09;
				if ((pesExt & 0x40) != 0 || skip > headerLength) {
					pesExt = skip = 0;
				}
				skip(skip);
				headerLength -= skip;
				if ((pesExt & 0x01) != 0) {
					int ext2Length = read8();
					headerLength--;
					 if ((ext2Length & 0x7F) != 0) {
						 int idExt = read8();
						 headerLength--;
						 if ((idExt & 0x80) == 0) {
							 startCode = ((startCode & 0xFF) << 8) | idExt;
						 }
					 }
				}
			}
			skip(headerLength);
		}

		if (startCode == 0x1BD) { // PRIVATE_STREAM_1
			int channel = read8();
			pesHeader.setChannel(channel);
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
		while (!done) {
			int startCode = read32();
			if (startCode == -1) {
				// End of file
				break;
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
					pesLength = readPesHeader(startCode, pesHeaderAudio);
					codeLength -= pesLength;
					if (pesHeaderAudio.getChannel() == audioChannel || audioChannel < 0) {
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
						pesLength = readPesHeader(startCode, pesHeaderVideo);
						codeLength -= pesLength;
						addVideoData(codeLength, getCurrentFilePosition());
						return true;
					}
					skip(codeLength);
					break;
			}
		}

		return false;
	}

	private void consumeVideoData(int length) {
		if (length >= videoDataOffset) {
			videoDataOffset = 0;
			lastParsePosition = 0;
		} else {
			System.arraycopy(videoData, length, videoData, 0, videoDataOffset - length);
			videoDataOffset -= length;
			lastParsePosition -= length;
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

	private int startCodeFindCandidate(int offset, int size) {
		for (int i = 0; i < size; i++) {
			if (videoData[offset + i] == 0x00) {
				return i;
			}
		}

		return size;
	}

	private int findVideoFrameEnd() {
		if (parseState > 13) {
			parseState = 7;
		}

		if (lastParsePosition < 0) {
			lastParsePosition = 0;
		}

		int nextAvc = isAvc ? 0 : videoDataOffset;
		int found = -1;
		for (int i = lastParsePosition; i < videoDataOffset; i++) {
			if (i >= nextAvc) {
				int nalSize = 0;
				i = nextAvc;
				for (int j = 0; j < nalLengthSize; j++) {
					nalSize = (nalSize << 8) | videoData[i++];
				}
				if (nalSize <= 0 || nalSize > videoDataOffset - 1) {
					return videoDataOffset;
				}
				nextAvc = i + nalLengthSize;
				parseState = 5;
			}

			if (parseState == 7) {
				i += startCodeFindCandidate(i, nextAvc - i);
				if (i < nextAvc) {
					parseState = 2;
				}
			} else if (parseState <= 2) {
				if (videoData[i] == 1) {
					parseState ^= 5; // 2->7, 1->4, 0->5
				} else if (videoData[i] != 0) {
					parseState = 7;
				} else {
					parseState >>= 1; // 2->1, 1->0, 0->0
				}
			} else if (parseState <= 5) {
				int naluType = videoData[i] & 0x1F;
				if (naluType == H264Context.NAL_SEI || naluType == H264Context.NAL_SPS ||
				    naluType == H264Context.NAL_PPS || naluType == H264Context.NAL_AUD) {
					if (foundFrameStart) {
						found = i + 1;
						break;
					}
				} else if (naluType == H264Context.NAL_SLICE || naluType == H264Context.NAL_DPA ||
				           naluType == H264Context.NAL_IDR_SLICE) {
					parseState += 8;
					continue;
				}
				parseState = 7;
			} else {
				parseHistory[parseHistoryCount++] = videoData[i];
				if (parseHistoryCount > 5) {
					int lastMb = parseLastMb;
					GetBitContext gb = new GetBitContext();
					gb.init_get_bits(parseHistory, 0, 8 * parseHistoryCount);
					parseHistoryCount = 0;
					int mb = gb.get_ue_golomb("UmdVideoPlayer.findVideoFrameEnd");
					parseLastMb = mb;
					if (foundFrameStart) {
						if (mb <= lastMb) {
							found = i;
							break;
						}
					} else {
						foundFrameStart = true;
					}
					parseState = 7;
				}
			}
		}

		if (found >= 0) {
			foundFrameStart = false;
			found -= (parseState & 5);
			if (parseState > 7) {
				found -= 5;
			}
			parseState = 7;
			lastParsePosition = found;
		} else {
			lastParsePosition = videoDataOffset;
		}

		return found;
	}

	public boolean startVideo() {
        endOfVideo = false;
        videoPaused = false;

        videoCodec = CodecFactory.getVideoCodec();
        videoCodecInit = false;
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

        pesHeaderAudio = new PesHeader(audioChannel);
        pesHeaderVideo = new PesHeader(videoChannel);

        startTime = System.currentTimeMillis();
        frame = 0;
        currentChapterNumber = -1;

        return true;
    }

    private void stopDisplayThread() {
    	done = true;
        while (displayThread != null && !threadExit) {
            sleep(1, 0);
        }
    }

    private void writeFile(int[] values, int size, String name) {
    	try {
			OutputStream os = new FileOutputStream(name);
			byte[] bytes = new byte[size];
			for (int i = 0; i < size; i++) {
				bytes[i] = (byte) values[i];
			}
			os.write(bytes);
			os.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
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
	    } while (frameSize <= 0 && !done);

	    if (frameSize <= 0) {
	    	endOfVideo = true;
	    	return;
	    }

	    if (!videoCodecInit) {
	    	int[] extraData = null;
	    	int extraDataLength = H264Utils.findExtradata(videoData, 0, frameSize);
	    	if (extraDataLength > 0) {
	    		extraData = new int[extraDataLength];
	    		System.arraycopy(videoData, 0, extraData, 0, extraDataLength);
	    	}

	    	if (videoCodec.init(extraData) == 0) {
	    		videoCodecInit = true;
	    	} else {
	    		endOfVideo = true;
	    		return;
	    	}
	    }

	    int consumedLength = videoCodec.decode(videoData, 0, frameSize);
	    if (consumedLength < 0) {
	    	endOfVideo = true;
	    	return;
	    }

	    if (videoCodec.hasImage()) {
    		int[] aspectRatio = new int[2];
    		videoCodec.getAspectRatio(aspectRatio);
    		videoAspectRatioNum = aspectRatio[0];
    		videoAspectRatioDen = aspectRatio[1];

    		frame++;
	    }

	    consumeVideoData(consumedLength);

	    boolean skipFrame = false;
		if ((frame % fastForwardSpeeds[fastForwardSpeed]) != 0) {
			skipFrame = true;
			startTime -= videoTimestampStep;
		}

	    if (videoCodec.hasImage() && !skipFrame) {
	    	int width = videoCodec.getImageWidth();
	    	int height = videoCodec.getImageHeight();
	    	boolean resized = false;
	    	if (videoWidth <= 0) {
	    		videoWidth = width;
	    		resized = true;
	    	}
	    	if (videoHeight <= 0) {
	    		videoHeight = height;
	    		resized = true;
	    	}
	    	if (log.isTraceEnabled()) {
	    		log.trace(String.format("Decoded video frame %dx%d (video %dx%d), pes=%s, SAR %d:%d", width, height, videoWidth, videoHeight, pesHeaderVideo, videoAspectRatioNum, videoAspectRatioDen));
	    	}
	    	if (resized) {
	    		resizeVideoPlayer();
	    	}

	    	int size = width * height;
	    	int size2 = size >> 2;
	    	luma = resize(luma, size);
	    	cr = resize(cr, size2);
	    	cb = resize(cb, size2);

	    	if (videoCodec.getImage(luma, cb, cr) == 0) {
	    		if (dumpFrames) {
		    		writeFile(luma, size, String.format("Frame%d.y", frame));
		    		writeFile(cb, size2, String.format("Frame%d.cb", frame));
		    		writeFile(cr, size2, String.format("Frame%d.cr", frame));
	    		}

	    		abgr = resize(abgr, size);
	    		// TODO How to find out if we have a YUVJ image?
	    		// H264Utils.YUVJ2YUV(luma, luma, size);
	    		H264Utils.YUV2ARGB(width, height, luma, cb, cr, abgr);
	    		image = display.createImage(new MemoryImageSource(videoWidth, videoHeight, abgr, 0, width));

	    		long now = System.currentTimeMillis();
			    long currentDuration = now - startTime;
			    long videoDuration = frame * 100000L / videoTimestampStep;
			    if (currentDuration < videoDuration) {
			    	Utilities.sleep((int) (videoDuration - currentDuration), 0);
			    }
	    	}
	    }

	    if (videoCodec.hasImage()) {
	    	if (pesHeaderVideo.getPts() != UNKNOWN_TIMESTAMP) {
	    		currentVideoTimestamp = pesHeaderVideo.getPts();
	    	} else {
	    		currentVideoTimestamp += videoTimestampStep;
	    	}
	    	if (log.isTraceEnabled()) {
	    		MpsStreamInfo streamInfo = mpsStreams.get(currentStreamIndex);
	    		log.trace(String.format("Playing stream %d: %s / %s", currentStreamIndex, getTimestampString(currentVideoTimestamp - streamInfo.streamFirstTimestamp), getTimestampString(streamInfo.streamLastTimestamp - streamInfo.streamFirstTimestamp)));
	    	}
	    }

    	if (pesHeaderVideo.getPts() != UNKNOWN_TIMESTAMP) {
		    int chapterNumber = mpsStreams.get(currentStreamIndex).getChapterNumber(pesHeaderVideo.getPts());
		    if (chapterNumber != currentChapterNumber) {
		    	if (moviePlayer != null) {
		    		// For the MoviePlayer, chapters are numbered starting from 1
		    		moviePlayer.onChapter(chapterNumber + 1);
		    	}
		    	currentChapterNumber = chapterNumber;
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
		    	result = audioCodec.decode(mem, audioBufferAddr, audioFrameLength, mem, samplesAddr);
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

    public Display getRCODisplay() {
    	return rcoDisplay;
    }

    private class DisplayControllerThread extends Thread {
    	private volatile boolean done = false;

        @Override
        public void run() {
        	while (!done) {
        		Emulator.getScheduler().step();
        		jpcsp.State.controller.hleControllerPoll();
        		Utilities.sleep(10, 0);
        	}
        }
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
                        	Image scaledImage = getImage();
                        	if (videoWidth != screenWidth || videoHeight != screenHeigth) {
                        		if (log.isTraceEnabled()) {
                        			log.trace(String.format("Scaling video image from %dx%d to %dx%d", videoWidth, videoHeight, screenWidth, screenHeigth));
                        		}
                        		scaledImage = scaledImage.getScaledInstance(screenWidth, screenHeigth, Image.SCALE_SMOOTH);
                        	}
                            display.setIcon(new ImageIcon(scaledImage));
                        }
                    } else {
                    	Utilities.sleep(10, 0);
                    }
                }
                if (!done) {
                	if (moviePlayer != null) {
                		done = true;
                		moviePlayer.onPlayListEnd(mpsStreams.get(currentStreamIndex).getPlayListNumber());
                	} else {
	                    if (log.isTraceEnabled()) {
	                    	log.trace(String.format("Switching to next stream"));
	                    }
	                	if (!goToNextMpsStream()) {
	                		done = true;
	                	}
                	}
                }
            }

            threadExit = true;
            displayThread = null;

            if (log.isTraceEnabled()) {
            	log.trace(String.format("Exiting Mps Display thread"));
            }
        }
    }
}
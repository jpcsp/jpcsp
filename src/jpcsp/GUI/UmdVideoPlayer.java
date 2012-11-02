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

import static java.lang.Math.max;
import static jpcsp.HLE.modules150.sceMpeg.mpegTimestampPerSecond;
import static jpcsp.util.Utilities.sleep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import jpcsp.State;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;
import jpcsp.HLE.Modules;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class UmdVideoPlayer implements KeyListener {

	private static Logger log = Logger.getLogger("videoplayer");
    private static final int BASE_VIDEO_WIDTH = 480;
    private static final int BASE_VIDEO_HEIGTH = 272;

    // ISO file vars.
    private String fileName;
    private UmdIsoReader iso;
    private UmdIsoFile isoFile;

    // Stream storage vars.
    private List<MpsStreamInfo> mpsStreams;
    private int currentStreamIndex;

    // Display related vars.
    private JLabel display;
    private int screenWidth;
    private int screenHeigth;

    // MediaEngine vars.
    private IContainer container;
    private IVideoResampler resampler;
    private int videoStreamId;
    private IStreamCoder videoCoder;
    private int audioStreamId;
    private IStreamCoder audioCoder;
    private IPacket packet;
    private long firstTimestampInStream;
    private long systemClockStartTime;
    private IConverter converter;
    private BufferedImage image;
    private volatile boolean seekFrameFastForward;
    private volatile boolean seekFrameRewind;
    private volatile FrameSeekState frameSeekState;
    private volatile boolean startNewPicture;

    // State vars (for sync thread).
    private volatile boolean videoPaused;
    private volatile boolean done;
    private volatile boolean endOfVideo;
    private volatile boolean threadExit;

    // Internal data related vars.
    private MpsDisplayThread displayThread;
    private MpsInput mpsInput;
    private SourceDataLine mLine;

    private enum FrameSeekState {
    	noSeek,
    	waitForKeyFrameStart,
    	waitForKeyFrameEnd
    }

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
        image = null;
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
        screenWidth = BASE_VIDEO_WIDTH * factor;
        screenHeigth = BASE_VIDEO_HEIGTH * factor;
        Insets insets = gui.getInsets();
        Dimension minSize = new Dimension(
                screenWidth + insets.left + insets.right,
                screenHeigth + insets.top + insets.bottom);
        gui.setMinimumSize(minSize);
    }

    private int endianSwap32(int x) {
        return Integer.reverseBytes(x);
    }

    private short endianSwap16(short x) {
        return Short.reverseBytes(x);
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
        firstTimestampInStream = Global.NO_PTS;
        videoPaused = false;
        seekFrameFastForward = false;
        seekFrameRewind = false;
        frameSeekState = FrameSeekState.noSeek;
	}

    public void fastForward() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Fast forward"));
    	}
        seekFrameFastForward = true;
        seekFrameRewind = false;
    }

    public void rewind() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Rewind"));
    	}
    	seekFrameFastForward = false;
        seekFrameRewind = true;
    }

    public boolean startVideo() {
        endOfVideo = false;
        videoPaused = false;

        closeVideo();
        closeAudio();

        try {
            container = IContainer.make();
        } catch (Throwable e) {
            Emulator.log.error(e);
            return false;
        }

        try {
            isoFile.seek(0);
        } catch (IOException e) {
            Emulator.log.error(e);
            return false;
        }
        mpsInput = new MpsInput(isoFile);

        if (container.open(mpsInput, IContainer.Type.READ, null) < 0) {
            Emulator.log.error("could not open file: " + fileName);
            return false;
        }

        int numStreams = container.getNumStreams();
        videoStreamId = -1;
        videoCoder = null;
        audioStreamId = -1;
        audioCoder = null;
        boolean audioMuted = Settings.getInstance().readBool("emu.mutesound");
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO && !audioMuted) {
                audioStreamId = i;
                audioCoder = coder;
            }
        }

        if (videoCoder != null && videoCoder.open(null, null) < 0) {
            Emulator.log.error("could not open video decoder for container: " + fileName);
            return false;
        }
        if (audioCoder != null && audioCoder.open(null, null) < 0) {
            Emulator.log.info("AT3+ audio format is not yet supported by Jpcsp (file=" + fileName + ")");
            return false;
        }

        resampler = null;
        if (videoCoder != null) {
            converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight());
            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                resampler = IVideoResampler.make(screenWidth,
                        screenHeigth, IPixelFormat.Type.BGR24,
                        videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
                if (resampler == null) {
                    Emulator.log.error("could not create color space resampler for: " + fileName);
                    return false;
                }
            }
        }
        if (audioCoder != null) {
            openAudio(audioCoder);
        }
        packet = IPacket.make();
        firstTimestampInStream = Global.NO_PTS;
        systemClockStartTime = 0;

        return true;
    }

    private void closeVideo() {
        if (container != null) {
            container.close();
            container = null;
        }
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (resampler != null) {
            resampler.delete();
            resampler = null;
        }
        if (converter != null) {
            converter.delete();
            converter = null;
        }
        if (packet != null) {
            packet.delete();
            packet = null;
        }
        frameSeekState = FrameSeekState.noSeek;
        startNewPicture = false;
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
        if (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == videoStreamId && videoCoder != null) {
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());
                int offset = 0;
                while (offset < packet.getSize()) {
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                    if (bytesDecoded < 0) {
                        return;
                    }
                    offset += bytesDecoded;

                    if (startNewPicture && frameSeekState == FrameSeekState.waitForKeyFrameStart && picture.isKeyFrame()) {
                    	if (log.isDebugEnabled()) {
                    		log.debug(String.format("Start of key frame detected"));
                    	}
                    	frameSeekState = FrameSeekState.waitForKeyFrameEnd;
                    }

                    if (log.isTraceEnabled()) {
                    	log.trace(String.format("startNewPicture=%b, isKeyFrame=%b, isComplete=%b, frameSeekState=%s, timestamp=%d", startNewPicture, picture.isKey(), picture.isComplete(), frameSeekState, picture.getTimeStamp() > 0 ? picture.getTimeStamp() : 0));
                    }

                    if (picture.isComplete()) {
                    	startNewPicture = true;
                    	if (frameSeekState == FrameSeekState.waitForKeyFrameEnd) {
                        	if (log.isDebugEnabled()) {
                        		log.debug(String.format("End of key frame detected"));
                        	}
                    		frameSeekState = FrameSeekState.noSeek;
                    	}

                    	if (frameSeekState == FrameSeekState.noSeek) {
	                        IVideoPicture newPic = picture;
	                        if (resampler != null) {
	                            newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
	                                    screenWidth, screenHeigth);
	                            if (resampler.resample(newPic, picture) < 0) {
	                                return;
	                            }
	                        }
	                        if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
	                            return;
	                        }
	                        if (firstTimestampInStream == Global.NO_PTS) {
	                            firstTimestampInStream = picture.getTimeStamp();
	                            systemClockStartTime = System.currentTimeMillis();
	                        } else {
	                            long systemClockCurrentTime = System.currentTimeMillis();
	                            long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - systemClockStartTime;
	                            long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - firstTimestampInStream) / 1000;
	                            final long millisecondsTolerance = 50;
	                            final long millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
	                            // Don't sleep when fast-forwarding, rewinding or pausing.
	                            if (!seekFrameFastForward && !seekFrameRewind && !videoPaused && millisecondsToSleep > 0) {
	                                sleep((int) millisecondsToSleep, 0);
	                            }
	                        }
	                        if ((converter != null) && (newPic != null)) {
	                        	if (log.isTraceEnabled()) {
	                        		log.trace(String.format("Displaying picture"));
	                        	}
	                            image = converter.toImage(newPic);
	                        }
                    	}
                    } else {
                    	startNewPicture = false;
                    }
                }
            } else if (packet.getStreamIndex() == audioStreamId && audioCoder != null) {
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
                int offset = 0;
                while (offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0) {
                        return;
                    }
                    offset += bytesDecoded;
                    if (samples.isComplete()) {
                        playAudio(samples);
                    }
                }
            }

            if (frameSeekState == FrameSeekState.noSeek) {
	            if (seekFrameFastForward) {
	            	int fastForwardMillis = 50;
	            	IRational timestamp = IRational.make(fastForwardMillis, 1000);
	            	IStream stream = container.getStream(videoStreamId);
	            	long currentDts = stream.getCurrentDts();
	            	IRational timeBase = stream.getTimeBase();
	            	long seekTimestamp = (long) timestamp.divide(timeBase).getValue();
	            	long seekDts = currentDts + seekTimestamp;
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("FastForward %d ms from %d to %d", fastForwardMillis, currentDts, seekDts));
	            	}
	            	int result = container.seekKeyFrame(videoStreamId, seekDts, 0);
	            	if (result < 0) {
	            		log.debug(String.format("seekKeyFrame returned %s", IError.make(result)));
	            	}
	            	frameSeekState = FrameSeekState.waitForKeyFrameStart;
	            	startNewPicture = false;
	            } else if (seekFrameRewind) {
	            	int rewindMillis = 250;
	            	IRational timestamp = IRational.make(rewindMillis, 1000);
	            	IStream stream = container.getStream(videoStreamId);
	            	long currentDts = stream.getCurrentDts();
	            	IRational timeBase = stream.getTimeBase();
	            	long seekTimestamp = (long) timestamp.divide(timeBase).getValue();
	            	long seekDts = max(currentDts - seekTimestamp, 0);
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("Rewind %d ms from %d to %d", rewindMillis, currentDts, seekDts));
	            	}
	            	container.seekKeyFrame(videoStreamId, 0, IContainer.SEEK_FLAG_BACKWARDS);
        			int result = container.seekKeyFrame(videoStreamId, seekDts, IContainer.SEEK_FLAG_BACKWARDS);
	            	if (result < 0) {
	            		log.debug(String.format("seekKeyFrame returned %s", IError.make(result)));
	            	}
	            	frameSeekState = FrameSeekState.waitForKeyFrameStart;
	            	startNewPicture = false;
	            }
            }
        } else {
            endOfVideo = true;
        }
    }

    private void openAudio(IStreamCoder aAudioCoder) {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
                (int) IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()),
                aAudioCoder.getChannels(),
                true,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            mLine = (SourceDataLine) AudioSystem.getLine(info);
            mLine.open(audioFormat);
            mLine.start();
        } catch (IllegalArgumentException iae) {
            // Some streams use a NULL PCM format.
            // Ignore the audio handling in these cases, for now.
            audioCoder = null;
        } catch (LineUnavailableException e) {
            return;
        }
    }

    private void playAudio(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        mLine.write(rawBytes, 0, aSamples.getSize());
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

    private static class MpsInput implements IURLProtocolHandler {
        private UmdIsoFile file;

        public MpsInput(UmdIsoFile file) {
            this.file = file;
        }

		@Override
		public boolean isStreamed(String url, int flags) {
			return false;
		}

		@Override
		public int open(String url, int flags) {
			return 0;
		}

		@Override
		public int read(byte[] buf, int size) {
			int length = -1;
			try {
				length = file.read(buf, 0, size);
				if (length == 0 && size > 0) {
					// EOF
					length = -1;
				}
			} catch (IOException e) {
				log.error("read", e);
			}

            if (log.isTraceEnabled()) {
            	log.trace(String.format("MpsInput read %d bytes, %d requested", length, size));
            }

            return length;
		}

		@Override
		public long seek(long offset, int whence) {
            long seek = -1;
    		try {
    			switch (whence) {
    				case SEEK_SET:
    					seek = offset;
    					break;
    				case SEEK_CUR:
    					seek = file.getFilePointer() + offset;
    					break;
    				case SEEK_END:
    					seek = file.length() + offset;
    					break;
    				case SEEK_SIZE:
    		            if (log.isTraceEnabled()) {
    		            	log.trace(String.format("MpsInput seek SEEK_SIZE returning %d", file.length()));
    		            }
    					return file.length();
    				default:
    					log.error(String.format("Unknown seek whence %d", whence));
    					return -1;
    			}
    			file.seek(seek);
    		} catch (IOException e) {
    			log.error(e);
    		}

            if (log.isTraceEnabled()) {
            	log.trace(String.format("MpsInput seek offset=%d, whence=%d, returning %d", offset, whence, seek));
            }

            return seek;
		}

		@Override
		public int write(byte[] buf, int size) {
			return -1;
		}

		@Override
		public int close() {
			try {
				file.close();
			} catch (IOException e) {
				log.error("close", e);
			}

			file = null;

			return 0;
		}
    }
}
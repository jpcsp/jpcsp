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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.endianSwap32;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.Settings;
import jpcsp.util.Debug;
import jpcsp.util.FIFOByteBuffer;
import jpcsp.util.FileLocator;
import jpcsp.util.Utilities;

import com.xuggle.ferry.Logger;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class MediaEngine {
	public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("me");
    protected static final int AVSEEK_FLAG_BACKWARD = 1; // seek backward
    protected static final int AVSEEK_FLAG_BYTE     = 2; // seeking based on position in bytes
    protected static final int AVSEEK_FLAG_ANY      = 4; // seek to any frame, even non-keyframes
    protected static final int AVSEEK_FLAG_FRAME    = 8; // seeking based on frame number
	private static boolean initialized = false;
    private IContainer container;
    private int numStreams;
    private IStreamCoder videoCoder;
    private IStreamCoder audioCoder;
    private int videoStreamID;
    private int audioStreamID;
    private BufferedImage currentImg;
    private FIFOByteBuffer decodedAudioSamples;
    private int currentSamplesSize = 1024;  // Default size.
    private IVideoPicture videoPicture;
    private IAudioSamples audioSamples;
    private IConverter videoConverter;
    private IVideoResampler videoResampler;
    private int[] videoImagePixels;
    private int bufferAddress;
    private int bufferSize;
    private int bufferMpegOffset;
    private byte[] bufferData;
    private StreamState videoStreamState;
    private StreamState audioStreamState;
    private List<IPacket> freePackets = new LinkedList<IPacket>();
    private ExternalDecoder externalDecoder = new ExternalDecoder();
    private byte[] tempBuffer;
    private int firstTimestamp;

    // External audio loading vars.
    private IContainer extContainer;

    public MediaEngine() {
    	initXuggler();
    }

    public static void initXuggler() {
    	if (!initialized) {
    		try {
		        // Disable Xuggler's logging, since we do our own.
		        Logger.setGlobalIsLogging(Logger.Level.LEVEL_DEBUG, false);
		        Logger.setGlobalIsLogging(Logger.Level.LEVEL_ERROR, false);
		        Logger.setGlobalIsLogging(Logger.Level.LEVEL_INFO, false);
		        Logger.setGlobalIsLogging(Logger.Level.LEVEL_TRACE, false);
		        Logger.setGlobalIsLogging(Logger.Level.LEVEL_WARN, false);
    		} catch (NoClassDefFoundError e) {
    			log.warn("Xuggler is not available on your platform");
    		}
            initialized = true;
    	}
    }

    @SuppressWarnings("deprecation")
	public static int streamCoderOpen(IStreamCoder streamCoder) {
    	try {
        	if (streamCoder.isOpen()) {
        		return 0;
        	}
    		// This method is not available in Xuggle 3.4
    		return streamCoder.open(null, null);
    	} catch (NoSuchMethodError e) {
    		// We are using Xuggle 3.4, try the old (deprecated) method.
    		return streamCoder.open();
    	}
    }

    public IContainer getContainer() {
        return container;
    }

    public IContainer getAudioContainer() {
    	if (audioStreamState == null) {
    		return null;
    	}
        return audioStreamState.getContainer();
    }

    public IContainer getExtContainer() {
        return extContainer;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public IStreamCoder getVideoCoder() {
        return videoCoder;
    }

    public IStreamCoder getAudioCoder() {
        return audioCoder;
    }

    public int getVideoStreamID() {
        return videoStreamID;
    }

    public int getAudioStreamID() {
        return audioStreamID;
    }

    public BufferedImage getCurrentImg() {
        return currentImg;
    }

    public int getCurrentAudioSamples(byte[] buffer) {
    	if (decodedAudioSamples == null) {
    		return 0;
    	}

    	int length = Math.min(buffer.length, decodedAudioSamples.length());
    	if (length > 0) {
    		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, length);
    		length = decodedAudioSamples.readByteBuffer(byteBuffer);
    	}

    	return length;
    }

    public int getAudioSamplesSize() {
        return currentSamplesSize;
    }

    public void setAudioSamplesSize(int newSize) {
        currentSamplesSize = newSize;
    }

    public void release(IPacket packet) {
    	if (packet != null) {
    		freePackets.add(packet);
    	}
    }

    public IPacket getPacket() {
    	if (!freePackets.isEmpty()) {
    		return freePackets.remove(0);
    	}

    	return IPacket.make();
    }

    private boolean readAu(StreamState state, SceMpegAu au, int requiredAudioChannels) {
    	boolean successful = true;
    	if (state == null) {
    		au.dts = 0;
    		au.pts = 0;
    	} else {
    		while (true) {
    			if (!getNextPacket(state)) {
    				if (state == videoStreamState) {
    					state.incrementTimestamps(sceMpeg.videoTimestampStep);
    				} else if (state == audioStreamState) {
    					state.incrementTimestamps(sceMpeg.audioTimestampStep);
    				}
    				successful = false;
    				break;
    			}

    			state.updateTimestamps();
    			if (state.getPts() >= firstTimestamp) {
    				break;
    			}

    			decodePacket(state, 0, requiredAudioChannels);
    		}
    		state.getTimestamps(au);
    	}

    	return successful;
    }

    public boolean readVideoAu(SceMpegAu au, int requiredAudioChannels) {
    	boolean successful = readAu(videoStreamState, au, requiredAudioChannels);

    	// On PSP, video DTS is always 1 frame behind PTS
    	if (au.pts >= sceMpeg.videoTimestampStep) {
    		au.dts = au.pts - sceMpeg.videoTimestampStep;
    	}

    	return successful;
    }

    public boolean readAudioAu(SceMpegAu au, int requiredAudioChannels) {
    	boolean successful = readAu(audioStreamState, au, requiredAudioChannels);

    	// On PSP, audio DTS is always set to -1
    	au.dts = sceMpeg.UNKNOWN_TIMESTAMP;

    	return successful;
    }

    public void getCurrentAudioAu(SceMpegAu au) {
    	if (audioStreamState != null) {
    		audioStreamState.getTimestamps(au);
    	} else {
    		au.pts += sceMpeg.audioTimestampStep;
    	}

    	// On PSP, audio DTS is always set to -1
    	au.dts = sceMpeg.UNKNOWN_TIMESTAMP;
    }

    public void getCurrentVideoAu(SceMpegAu au) {
    	if (videoStreamState != null) {
    		videoStreamState.getTimestamps(au);
    	} else {
    		au.pts += sceMpeg.videoTimestampStep;
    	}

    	// On PSP, video DTS is always 1 frame behind PTS
    	if (au.pts >= sceMpeg.videoTimestampStep) {
    		au.dts = au.pts - sceMpeg.videoTimestampStep;
    	}
    }

    private int read32(byte[] data, int offset) {
    	int n1 = data[offset] & 0xFF;
    	int n2 = data[offset + 1] & 0xFF;
    	int n3 = data[offset + 2] & 0xFF;
    	int n4 = data[offset + 3] & 0xFF;

    	return (n4 << 24) | (n3 << 16) | (n2 << 8) | n1;
    }

    public void setStreamFile(SeekableDataInput dataInput, IVirtualFile vFile, int address, long startPosition, int length) {
    	if (ExternalDecoder.isEnabled()) {
    		externalDecoder.setStreamFile(dataInput, vFile, address, startPosition, length);
    	}
    }

    public void init(byte[] bufferData) {
    	this.bufferData = bufferData;
    	this.bufferAddress = 0;
    	this.bufferSize = endianSwap32(read32(bufferData, sceMpeg.PSMF_STREAM_SIZE_OFFSET));
    	this.bufferMpegOffset = endianSwap32(read32(bufferData, sceMpeg.PSMF_STREAM_OFFSET_OFFSET));
    	init();
    }

    public void init(int bufferAddress, int bufferSize, int bufferMpegOffset) {
    	this.bufferAddress = bufferAddress;
    	this.bufferSize = bufferSize;
    	this.bufferMpegOffset = bufferMpegOffset;

    	// Save the content of the MPEG header as it might be already overwritten
    	// when we need it (at sceMpegGetAtracAu or sceMpegGetAvcAu)
    	bufferData = new byte[sceMpeg.MPEG_HEADER_BUFFER_MINIMUM_SIZE];
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(bufferAddress, bufferData.length, 1);
    	for (int i = 0; i < bufferData.length; i++) {
    		bufferData[i] = (byte) memoryReader.readNext();
    	}

    	init();
    }

    public void init() {
    	finish();
        videoStreamID = -1;
        audioStreamID = -1;
    }

    /*
     * Split version of decodeAndPlay.
     *
     * This method is to be used when the video and audio frames
     * are decoded and played step by step (sceMpeg case).
     * The sceMpeg functions must call init() first for each MPEG stream and then
     * keep calling step() until the video is finished and finish() is called.
     */
    public void init(IURLProtocolHandler channel, boolean decodeVideo, boolean decodeAudio, int videoChannel, int audioChannel) {
    	init();

    	container = IContainer.make();

        // Keep trying to read
        container.setReadRetryCount(-1);

        // This defines the size of the buffer used by ffmpeg to read from our channel.
        // 32Kb seems to work best to avoid ffmpeg reading past the end of the available buffer.
        // This is the same buffer size as used by Xuggle 3.4.
        container.setInputBufferLength(32 * 1024);

        // query stream meta data only for video, not for audio (is requiring the entire
        // stream to be available for the audio, which is often not the case with Atrac3 audio)
        if (container.open(channel, IContainer.Type.READ, null, false, decodeVideo) < 0) {
            log.error("MediaEngine: Invalid container format!");
        }

        numStreams = container.getNumStreams();

        int audioChannelIndex = 0;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Found video stream %s", stream));
            	}
            	if (videoStreamID < 0) {
            		// stream.getId() returns a value 0x1En, where n is the videoChannel (e.g. 0x1E0, 0x1E1...)
            		if (videoChannel < 0 || videoChannel == (stream.getId() & 0xF)) {
            			videoStreamID = i;
            			videoCoder = coder;
            		}
            	}
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Found audio stream %s", stream));
            	}
            	if (audioStreamID < 0) {
            		if (audioChannel < 0 || audioChannel == audioChannelIndex) {
                        audioStreamID = i;
                        audioCoder = coder;
            		}
            	}
            	audioChannelIndex++;
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Found unknown stream %s", stream));
            	}
            }
        }

        if (decodeVideo) {
            if (videoStreamID < 0) {
                log.error("MediaEngine: No video streams found!");
            } else if (streamCoderOpen(videoCoder) < 0) {
            	videoCoder.delete();
            	videoCoder = null;
                log.error("MediaEngine: Can't open video decoder!");
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Using video stream #%d", videoStreamID));
            	}
            	videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                videoPicture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
            	if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
	                videoResampler = IVideoResampler.make(videoCoder.getWidth(),
	                        videoCoder.getHeight(), IPixelFormat.Type.BGR24,
	                        videoCoder.getWidth(), videoCoder.getHeight(),
	                        videoCoder.getPixelType());
                	videoPicture = IVideoPicture.make(videoResampler.getOutputPixelFormat(), videoPicture.getWidth(), videoPicture.getHeight());
            	}
                videoStreamState = new StreamState(this, videoStreamID, container, 0);
            }
        }

        if (decodeAudio) {
            if (audioStreamID < 0) {
            	// Try to use an external audio file instead
            	if (!initExtAudio(audioChannel)) {
            		log.error("MediaEngine: No audio streams found!");
            		audioStreamState = new StreamState(this, -1, null, sceMpeg.audioFirstTimestamp);
            	}
            } else if (streamCoderOpen(audioCoder) < 0) {
            	audioCoder.delete();
            	audioCoder = null;
                log.error("MediaEngine: Can't open audio decoder!");
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Using audio stream #%d from %d", audioStreamID, audioChannelIndex));
            	}
        		// The creation of the audioSamples might fail
            	// if the audioCoder returns 0 channels. The audioSamples will then be
            	// created later, when trying to decode audio samples.
            	// This is the case when decoding an MP3 stream: it seems that the number
            	// of channels is only set after reading one packet from the stream.
        		audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
        		decodedAudioSamples = new FIFOByteBuffer();
                audioStreamState = new StreamState(this, audioStreamID, container, 0);
            }
        }
    }

    public void changeAudioChannel(int audioChannel) {
    	if (container == null) {
    		return;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Changing audio channel to %d", audioChannel));
    	}

    	boolean channelChanged = false;
        int audioChannelIndex = 0;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Found audio stream %s", stream));
            	}
        		if (audioChannel < 0 || audioChannel == audioChannelIndex) {
        			if (audioStreamID != i) {
        				audioStreamID = i;
        				audioCoder = coder;
        				channelChanged = true;
        			}
        			break;
        		}
            	audioChannelIndex++;
        	}
        }

        if (!channelChanged) {
			// Audio channel unchanged
        	return;
        }

        if (audioStreamID < 0) {
        	// Try to use an external audio file instead
        	if (!initExtAudio(audioChannel)) {
        		log.error("MediaEngine: No audio streams found!");
        		audioStreamState = new StreamState(this, -1, null, sceMpeg.audioFirstTimestamp);
        	}
        } else if (streamCoderOpen(audioCoder) < 0) {
        	audioCoder.delete();
        	audioCoder = null;
            log.error("MediaEngine: Can't open audio decoder!");
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Using audio stream #%d", audioStreamID));
        	}
            audioStreamState.setStreamID(audioStreamID);
        }
    }

    public void changeVideoChannel(int videoChannel) {
    	if (container == null) {
    		return;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Changing video channel to %d", videoChannel));
    	}

    	boolean channelChanged = false;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Found video stream %s", stream));
            	}
        		// stream.getId() returns a value 0x1En, where n is the videoChannel (e.g. 0x1E0, 0x1E1...)
        		if (videoChannel < 0 || videoChannel == (stream.getId() & 0xF)) {
        			if (videoStreamID != i) {
        				videoStreamID = i;
        				videoCoder = coder;
        				channelChanged = true;
        			}
        			break;
        		}
            }
        }

        if (!channelChanged) {
			// Video channel unchanged
        	return;
        }

        if (streamCoderOpen(videoCoder) < 0) {
        	videoCoder.delete();
        	videoCoder = null;
            log.error("MediaEngine: Can't open video decoder!");
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Using video stream #%d", videoStreamID));
        	}
        	videoStreamState.setStreamID(videoStreamID);
        }
    }

    private boolean getNextPacket(StreamState state) {
    	if (state.isPacketEmpty()) {
    		// Retrieve the next packet for the stream.
    		// First try if there is a pending packet for this stream.
    		state.releasePacket();
    		IPacket packet = state.getNextPacket();
    		if (packet != null) {
    			// use the pending packet
    			state.setPacket(packet);
    		} else {
    			// There is no pending packet, read packets from the container
    			// until a packet for this stream is found.
    			IContainer container = state.getContainer();
    			if (container == null) {
    				return false;
    			}
    			while (state.isPacketEmpty()) {
    				packet = getPacket();
			        if (container.readNextPacket(packet) < 0) {
			        	// No more packets available in the container...
			        	release(packet);
			        	return false;
			        }

			        // Process the packet
			        int streamIndex = packet.getStreamIndex();
			        if (packet.getSize() <= 0) {
			        	// Empty packet, drop it
			        	release(packet);
			        } else if (state.isStream(container, streamIndex)) {
			        	// This is the kind of packet we are looking for
			        	state.setPacket(packet);
			        } else if (videoCoder != null && videoStreamState != null && videoStreamState.isStream(container, streamIndex)) {
			        	// We are currently not interested in video packets,
			        	// add this packet to the video pending packets
			        	videoStreamState.addPacket(packet);
			        } else if (audioCoder != null && audioStreamState != null && audioStreamState.isStream(container, streamIndex)) {
			        	// We are currently not interested in audio packets,
			        	// add this packet to the audio pending packets
			        	audioStreamState.addPacket(packet);
			        } else {
			        	// Packet with unknown stream index, ignore it
			        	release(packet);
			        }
    			}
    		}
    	}

    	return true;
    }

    private boolean decodePacket(StreamState state, int requiredAudioBytes, int requiredAudioChannels) {
    	boolean complete = false;
        if (state == videoStreamState) {
        	if (videoCoder == null) {
        		// No video coder, skip all the video packets
        		state.releasePacket();
        		complete = true;
        	} else {
        		// Decode the current video packet
        		// and check if we have a complete video sample
        		complete = decodeVideoPacket(state);
            }
        } else if (state == audioStreamState) {
        	if (audioCoder == null) {
        		// No audio coder, skip all the audio packets
        		state.releasePacket();
        		complete = true;
        	} else {
        		// Decode the current audio packet
        		// and check if we have a complete audio sample,
        		// with the minimum required sample bytes
        		if (decodeAudioPacket(state, requiredAudioChannels)) {
        			if (decodedAudioSamples.length() >= requiredAudioBytes) {
    					complete = true;
        			}
        		}
            }
        }

        return complete;
    }

    private boolean decodeVideoPacket(StreamState state) {
    	boolean complete = false;
        while (!state.isPacketEmpty()) {
        	int decodedBytes = videoCoder.decodeVideo(videoPicture, state.getPacket(), state.getOffset());

            if (decodedBytes < 0) {
            	// An error occured with this packet, skip it
            	state.releasePacket();
            	break;
            }

            state.updateTimestamps();
            state.consume(decodedBytes);

            // Xuggle 5.4 is not setting the first few frames of the video as complete.
            // The PSP however can decode them as normal frame. Simulate the completion
            // of the first frames of the video (until the first frame is really completed).
            boolean simulateVideoPictureCompletion = getCurrentImg() == null;

            if (videoPicture.isComplete() || simulateVideoPictureCompletion) {
            	if (videoConverter != null && videoPicture.isComplete()) {
            		currentImg = videoConverter.toImage(videoPicture);
            	}
            	complete = true;
            	break;
            }
        }

        return complete;
    }

    private boolean decodeAudioPacket(StreamState state, int requiredAudioChannels) {
    	boolean complete = false;
        while (!state.isPacketEmpty()) {
        	if (audioSamples == null) {
        		// Create the audioSamples if required.
        		// Their creation sometimes fails at init if the audioCoder still returns 0 channels.
        		audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
        	}
        	int decodedBytes = audioCoder.decodeAudio(audioSamples, state.getPacket(), state.getOffset());

            if (decodedBytes < 0) {
            	// An error occured with this packet, skip it
            	state.releasePacket();
            	break;
            }

            state.updateTimestamps();
            state.consume(decodedBytes);

        	if (audioSamples.isComplete()) {
                updateSoundSamples(audioSamples, requiredAudioChannels);
                complete = true;
                break;
            }
        }

        return complete;
    }

    public static String getExtAudioBasePath(int mpegStreamSize) {
    	return String.format("%sMpeg-%d%c", Settings.getInstance().getDiscTmpDirectory(), mpegStreamSize, File.separatorChar);
    }

    public static String getExtAudioPath(int mpegStreamSize, String suffix) {
        return String.format("%sExtAudio.%s", getExtAudioBasePath(mpegStreamSize), suffix);
    }

    public static String getExtAudioPath(int mpegStreamSize, int audioChannel, String suffix) {
    	if (audioChannel < 0) {
    		return getExtAudioPath(mpegStreamSize, suffix);
    	}
        return String.format("%sExtAudio-%d.%s", getExtAudioBasePath(mpegStreamSize), audioChannel, suffix);
    }

    public boolean stepVideo(int requiredAudioChannels) {
    	return step(videoStreamState, 0, requiredAudioChannels);
    }

    public boolean stepAudio(int requiredAudioBytes, int requiredAudioChannels) {
    	boolean success = step(audioStreamState, requiredAudioBytes, requiredAudioChannels);
    	if (decodedAudioSamples != null && decodedAudioSamples.length() > 0) {
    		success = true;
    	}

    	return success;
    }

    private boolean step(StreamState state, int requiredAudioBytes, int requiredAudioChannels) {
    	boolean complete = false;

    	if (state != null) {
	    	while (!complete) {
	    		if (!getNextPacket(state)) {
		        	break;
	    		}

	    		complete = decodePacket(state, requiredAudioBytes, requiredAudioChannels);
	    	}
    	}

        return complete;
    }

    private File getExtAudioFile(int audioChannel) {
        String supportedFormats[] = {"wav", "mp3", "at3", "raw", "wma", "flac", "m4a"};
        for (int i = 0; i < supportedFormats.length; i++) {
            File f = new File(getExtAudioPath(bufferSize, audioChannel, supportedFormats[i]));
            if (f.canRead() && f.length() > 0) {
            	return f;
            }
        }

        return null;
    }

    private boolean initExtAudio(int audioChannel) {
    	boolean useExtAudio = false;

    	File extAudioFile = getExtAudioFile(audioChannel);
    	if (extAudioFile == null && ExternalDecoder.isEnabled()) {
    		// Try to decode the audio using the external decoder
    		if (bufferAddress == 0) {
    			if (bufferData != null) {
    				externalDecoder.decodeExtAudio(new ByteArrayVirtualFile(bufferData), bufferSize, bufferMpegOffset, audioChannel);
    			}
    		} else {
    			externalDecoder.decodeExtAudio(FileLocator.getInstance().getVirtualFile(bufferAddress, sceMpeg.MPEG_HEADER_BUFFER_MINIMUM_SIZE, bufferSize, bufferData), bufferSize, bufferMpegOffset, audioChannel);
    		}
			extAudioFile = getExtAudioFile(audioChannel);
    	}

    	if (extAudioFile != null) {
    		useExtAudio = initExtAudio(extAudioFile.toString());
    	}

        return useExtAudio;
    }

    // Override audio data line with one from an external file.
    private boolean initExtAudio(String file) {
        extContainer = IContainer.make();

        if (log.isDebugEnabled()) {
        	log.debug(String.format("initExtAudio %s", file));
        }

        IURLProtocolHandler fileProtocolHandler = new FileProtocolHandler(file);
        if (extContainer.open(fileProtocolHandler, IContainer.Type.READ, null) < 0) {
            log.error("MediaEngine: Invalid file or container format: " + file);
            extContainer.close();
            extContainer = null;
            return false;
        }

        int extNumStreams = extContainer.getNumStreams();

        audioStreamID = -1;
        audioCoder = null;

        for (int i = 0; i < extNumStreams; i++) {
            IStream stream = extContainer.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (audioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamID = i;
                audioCoder = coder;
            }
        }

        if (audioStreamID == -1) {
            log.error("MediaEngine: No audio streams found in external audio!");
            extContainer.close();
            extContainer = null;
            return false;
        } else if (streamCoderOpen(audioCoder) < 0) {
            log.error("MediaEngine: Can't open audio decoder!");
            extContainer.close();
            extContainer = null;
            return false;
        }

		audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
		decodedAudioSamples = new FIFOByteBuffer();

		// External audio is starting at timestamp 0,
		// but the PSP audio is starting at timestamp 89249:
		// offset the external audio timestamp by this value.
		audioStreamState = new StreamState(this, audioStreamID, extContainer, sceMpeg.audioFirstTimestamp);
        audioStreamState.setTimestamps(sceMpeg.mpegTimestampPerSecond);

        log.info(String.format("Using external audio '%s'", file));

        return true;
    }

    // Cleanup function.
    public void finish() {
    	if (container != null) {
    		container.close();
        	container = null;
    	}
    	if (videoStreamState != null) {
    		videoStreamState.finish();
    		videoStreamState = null;
    	}
    	if (audioStreamState != null) {
        	audioStreamState.finish();
    		audioStreamState = null;
    	}
    	while (!freePackets.isEmpty()) {
    		IPacket packet = getPacket();
    		packet.delete();
    	}
    	if (videoCoder != null) {
    		videoCoder.close();
    		videoCoder = null;
    	}
    	if (audioCoder != null) {
    		audioCoder.close();
    		audioCoder = null;
    	}
    	if (videoConverter != null) {
    		videoConverter.delete();
    		videoConverter = null;
    	}
    	if (videoPicture != null) {
    		videoPicture.delete();
    		videoPicture = null;
    	}
    	if (audioSamples != null) {
    		audioSamples.delete();
    		audioSamples = null;
    	}
    	if (videoResampler != null) {
    		videoResampler.delete();
    		videoResampler = null;
    	}
    	if (extContainer != null) {
    		extContainer.close();
    		extContainer = null;
    	}
    	if (decodedAudioSamples != null) {
    		decodedAudioSamples.delete();
            decodedAudioSamples = null;
    	}
    	tempBuffer = null;
    	currentImg = null;
    }

    /**
     * Convert the audio samples to a stereo format with signed 16-bit samples.
     * The converted audio samples are always stored in tempBuffer.
     *
     * @param samples the audio sample container
     * @param buffer  the audio sample bytes
     * @param length  the number of bytes in buffer
     * @return        the new number of bytes in tempBuffer
     */
    private int convertSamples(IAudioSamples samples, byte[] buffer, int length, int requiredAudioChannels) {
    	if (samples.getFormat() != IAudioSamples.Format.FMT_S16) {
    		log.error("Unsupported audio samples format: " + samples.getFormat());
    		return length;
    	}

    	if (samples.getChannels() == requiredAudioChannels) {
    		// Samples already in the correct format
    		return length;
    	}

    	if (samples.getChannels() == 1 && requiredAudioChannels == 2) {
    		return convertSamplesMonoToStereo(buffer, length);
    	} else if (samples.getChannels() == 2 && requiredAudioChannels == 1) {
    		return convertSamplesStereoToMono(buffer, length);
    	} else {
    		log.error(String.format("Cannot convert %d audio channels to %d channels", samples.getChannels(), requiredAudioChannels));
    		return length;
    	}

    }

    private int convertSamplesMonoToStereo(byte[] buffer, int length) {
		if (log.isDebugEnabled()) {
			log.debug("Converting mono samples to stereo");
		}

		// Convert mono audio samples (1 channel) to stereo (2 channels)
    	int samplesSize = length << 1;
    	if (tempBuffer == null || samplesSize > tempBuffer.length) {
    		tempBuffer = new byte[samplesSize];
    	}

    	// Copy backwards in case the source buffer is also the tempBuffer
    	for (int i = samplesSize - 4, j = length - 2; i >= 0; i -= 4, j -= 2) {
    		byte byte1 = buffer[j + 0];
    		byte byte2 = buffer[j + 1];
    		tempBuffer[i + 0] = byte1;
    		tempBuffer[i + 1] = byte2;
    		tempBuffer[i + 2] = byte1;
    		tempBuffer[i + 3] = byte2;
    	}

    	return samplesSize;
    }

    private int convertSamplesStereoToMono(byte[] buffer, int length) {
		if (log.isDebugEnabled()) {
			log.debug("Converting stereo samples to mono");
		}

    	// Convert stereo audio samples (2 channels) to mono (1 channel)
    	int samplesSize = length >> 1;
    	if (tempBuffer == null || samplesSize > tempBuffer.length) {
    		tempBuffer = new byte[samplesSize];
    	}

    	for (int i = 0, j = 0; i < samplesSize; i += 2, j += 4) {
    		int left = Utilities.readUnaligned16(buffer, j);
    		int right = Utilities.readUnaligned16(buffer, j + 2);
    		int mono = (left + right) >> 1;
    		tempBuffer[i + 0] = (byte) mono;
    		tempBuffer[i + 1] = (byte) (mono >> 8);
    	}

    	return samplesSize;
    }

    /**
     * Add the audio samples to the decoded audio samples buffer.
     * 
     * @param samples          the samples to be added
     */
    private void updateSoundSamples(IAudioSamples samples, int requiredAudioChannels) {
    	int samplesSize = samples.getSize();
    	if (tempBuffer == null || samplesSize > tempBuffer.length) {
    		tempBuffer = new byte[samplesSize];
    	}
    	samples.get(0, tempBuffer, 0, samplesSize);

    	samplesSize = convertSamples(samples, tempBuffer, samplesSize, requiredAudioChannels);

        decodedAudioSamples.write(tempBuffer, 0, samplesSize);
    }

    // This function is time critical and has to execute under
    // sceMpeg.avcDecodeDelay to match the PSP and allow a fluid video rendering
    public void writeVideoImage(int dest_addr, int frameWidth, int videoPixelMode) {
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);

        // Tell the display that we are updating the given address
        Modules.sceDisplayModule.write(dest_addr);

        // Get the current generated image, convert it to pixels and write it
        // to memory.
        if (getCurrentImg() != null) {
            // Override the base dimensions with the image's real dimensions.
            int width = getCurrentImg().getWidth();
            int height = getCurrentImg().getHeight();
            int imageSize = height * width;
            BufferedImage image = getCurrentImg();
            if (image.getColorModel() instanceof ComponentColorModel && image.getRaster().getDataBuffer() instanceof DataBufferByte) {
            	// Optimized version for most common case: 1 pixel stored in 3 bytes in BGR format
            	byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            	if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
            		// Fastest version for pixel format 8888
	                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
	            	for (int y = 0, i = 0; y < height; y++) {
		                for (int x = 0; x < width; x++) {
		                    int b = imageData[i++] & 0xFF;
		                    int g = imageData[i++] & 0xFF;
		                    int r = imageData[i++] & 0xFF;
		                    int colorABGR = 0xFF000000 | b << 16 | g << 8 | r;
		                    memoryWriter.writeNext(colorABGR);
		                }
    	                memoryWriter.skip(frameWidth - width);
	            	}
	                memoryWriter.flush();
            	} else {
            		// Slower version for pixel format other than 8888
	                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
	            	for (int y = 0, i = 0; y < height; y++) {
		                for (int x = 0; x < width; x++) {
		                    int b = imageData[i++] & 0xFF;
		                    int g = imageData[i++] & 0xFF;
		                    int r = imageData[i++] & 0xFF;
		                    int colorABGR = 0xFF000000 | b << 16 | g << 8 | r;
		                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
		                    memoryWriter.writeNext(pixelColor);
		                }
    	                memoryWriter.skip(frameWidth - width);
	            	}
	                memoryWriter.flush();
            	}
            } else {
            	// Non-optimized version supporting any image format,
            	// but very slow (due to BufferImage.getRGB() call)
	            if (videoImagePixels == null || videoImagePixels.length < imageSize) {
	            	videoImagePixels = new int[imageSize];
	            }
	            // getRGB is very slow...
				videoImagePixels = image.getRGB(0, 0, width, height, videoImagePixels, 0, width);
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
	            for (int y = 0; y < height; y++) {
	                for (int x = 0; x < width; x++) {
	                    int colorARGB = videoImagePixels[y * width + x];
	                    // Convert from ARGB to ABGR.
	                    int a = (colorARGB >>> 24) & 0xFF;
	                    int r = (colorARGB >>> 16) & 0xFF;
	                    int g = (colorARGB >>> 8) & 0xFF;
	                    int b = colorARGB & 0xFF;
	                    int colorABGR = a << 24 | b << 16 | g << 8 | r;
	                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
	                    memoryWriter.writeNext(pixelColor);
	                }
	                memoryWriter.skip(frameWidth - width);
	            }
                memoryWriter.flush();
            }
        } else {
        	Memory.getInstance().memset(dest_addr, (byte) 0, videoPicture.getHeight() * frameWidth * bytesPerPixel);
        }
    }

    public void writeVideoImageWithRange(int dest_addr, int frameWidth, int videoPixelMode, int x, int y, int w, int h) {
        // Get the current generated image, convert it to pixels and write it
        // to memory.
        if (getCurrentImg() != null) {
        	// If we have a range covering the whole image, call the method
        	// without range, its execution is faster.
        	if (x == 0 && y == 0 && getCurrentImg().getWidth() == w && getCurrentImg().getHeight() == h) {
        		writeVideoImage(dest_addr, frameWidth, videoPixelMode);
        		return;
        	}

        	int imageSize = h * w;
            if (videoImagePixels == null || videoImagePixels.length < imageSize) {
                videoImagePixels = new int[imageSize];
            }
            videoImagePixels = getCurrentImg().getRGB(x, y, w, h, videoImagePixels, 0, w);
            int pixelIndex = 0;
            final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
            for (int i = 0; i < h; i++) {
                int address = dest_addr + i * frameWidth * bytesPerPixel;
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, bytesPerPixel);
                for (int j = 0; j < w; j++, pixelIndex++) {
                    int colorARGB = videoImagePixels[pixelIndex];
                    // Convert from ARGB to ABGR.
                    int a = (colorARGB >>> 24) & 0xFF;
                    int r = (colorARGB >>> 16) & 0xFF;
                    int g = (colorARGB >>> 8) & 0xFF;
                    int b = colorARGB & 0xFF;
                    int colorABGR = a << 24 | b << 16 | g << 8 | r;
                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                    memoryWriter.writeNext(pixelColor);
                }
                memoryWriter.flush();
            }
        }
    }

    public void audioResetPlayPosition(int sample) {
    	if (container != null && audioStreamID != -1) {
    		if (container.seekKeyFrame(audioStreamID, sample, AVSEEK_FLAG_ANY | AVSEEK_FLAG_FRAME) < 0) {
    			log.warn(String.format("Could not reset audio play position to %d", sample));
    		}
    	}
    }

    public void setFirstTimestamp(int firstTimestamp) {
    	this.firstTimestamp = firstTimestamp;
    }

    private void setStartPts(long startPts, IContainer container, int streamID) {
    	if (container.seekKeyFrame(streamID, startPts, 0) < 0) {
    		log.warn(String.format("Could not set the starting PTS to %d", startPts));
    	}
    }

    public void setStartPts(long startPts) {
    	if (container != null) {
    		setStartPts(startPts, container, videoStreamID);
    	}
    	if (extContainer != null) {
    		setStartPts(startPts / 2, extContainer, audioStreamID);
    	}
    }
}
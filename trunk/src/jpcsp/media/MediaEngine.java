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

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.sound.SoundChannel;
import jpcsp.util.Debug;
import jpcsp.util.FIFOByteBuffer;

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
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class MediaEngine {

	private static boolean initialized = false;
    private IContainer container;
    private int numStreams;
    private IStreamCoder videoCoder;
    private IStreamCoder audioCoder;
    private int videoStreamID;
    private int audioStreamID;
    private SoundChannel extSoundChannel;
    private BufferedImage currentImg;
    private FIFOByteBuffer currentSamples;
    private int currentSamplesSize = 1024;  // Default size.
    private IVideoPicture videoPicture;
    private IAudioSamples audioSamples;
    private IConverter videoConverter;
    private IVideoResampler videoResampler;
    private boolean extAudioChecked;
    private int[] videoImagePixels;
    private StreamState videoStreamState;
    private StreamState audioStreamState;
    private List<IPacket> freePackets = new LinkedList<IPacket>();

    // External audio loading vars.
    private IContainer extContainer;
    private IPacket extPacket;
    private IStreamCoder extAudioCoder;
    private int extAudioStreamID;
    private IAudioSamples extAudioSamples;

    public MediaEngine() {
    	if (!initialized) {
	        // Disable Xuggler's logging, since we do our own.
	        Logger.setGlobalIsLogging(Logger.Level.LEVEL_DEBUG, false);
	        Logger.setGlobalIsLogging(Logger.Level.LEVEL_ERROR, false);
	        Logger.setGlobalIsLogging(Logger.Level.LEVEL_INFO, false);
	        Logger.setGlobalIsLogging(Logger.Level.LEVEL_TRACE, false);
	        Logger.setGlobalIsLogging(Logger.Level.LEVEL_WARN, false);
	        initialized = true;
    	}
    }

    public IContainer getContainer() {
        return container;
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
    	int length = Math.min(buffer.length, currentSamples.length());
    	if (length > 0) {
    		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, length);
    		length = currentSamples.readByteBuffer(byteBuffer);
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

    private void getTimestamps(StreamState state, SceMpegAu au) {
    	if (state == null) {
    		au.dts = 0;
    		au.pts = 0;
    	} else {
    		state.getTimestamps(au);
    	}
    }

    public void getVideoTimestamp(SceMpegAu au) {
    	getTimestamps(videoStreamState, au);
    }

    public void getAudioTimestamp(SceMpegAu au) {
    	getTimestamps(audioStreamState, au);
    }

    public void init() {
    	finish();
        videoStreamID = -1;
        audioStreamID = -1;
        extAudioChecked = false;
    }

    /*
     * Split version of decodeAndPlay.
     *
     * This method is to be used when the video and audio frames
     * are decoded and played step by step (sceMpeg case).
     * The sceMpeg functions must call init() first for each MPEG stream and then
     * keep calling step() until the video is finished and finish() is called.
     */
    public void init(ReadableByteChannel channel, boolean decodeVideo, boolean decodeAudio) {
    	init();

    	container = IContainer.make();

        // Keep trying to read
        container.setReadRetryCount(-1);

        if (container.open(channel, null) < 0) {
            Modules.log.error("MediaEngine: Invalid container format!");
        }

        numStreams = container.getNumStreams();

        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (videoStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamID = i;
                videoCoder = coder;
            } else if (audioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamID = i;
                audioCoder = coder;
            }
        }

        if (decodeVideo) {
            if (videoStreamID == -1) {
                Modules.log.error("MediaEngine: No video streams found!");
            } else if (videoCoder.open() < 0) {
                Modules.log.error("MediaEngine: Can't open video decoder!");
            } else {
            	videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                videoPicture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
            	if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
	                videoResampler = IVideoResampler.make(videoCoder.getWidth(),
	                        videoCoder.getHeight(), IPixelFormat.Type.BGR24,
	                        videoCoder.getWidth(), videoCoder.getHeight(),
	                        videoCoder.getPixelType());
                	videoPicture = IVideoPicture.make(videoResampler.getOutputPixelFormat(), videoPicture.getWidth(), videoPicture.getHeight());
            	}
            }
        }

        if (decodeAudio) {
            if (audioStreamID == -1) {
                Modules.log.error("MediaEngine: No audio streams found!");
            } else if (audioCoder.open() < 0) {
                Modules.log.error("MediaEngine: Can't open audio decoder!");
            } else {
        		audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
        		currentSamples = new FIFOByteBuffer();
            }
        }

        videoStreamState = new StreamState(this, videoStreamID);
        audioStreamState = new StreamState(this, audioStreamID);
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
    			while (state.isPacketEmpty()) {
    				packet = getPacket();
			        if (container.readNextPacket(packet) < 0) {
			        	// No more packets available in the container...
			        	release(packet);
			        	return false;
			        }

			        // Process the packet
			        if (packet.getSize() <= 0) {
			        	// Empty packet, drop it
			        	release(packet);
			        } else if (state.isStream(packet.getStreamIndex())) {
			        	// This is the kind of packet we are looking for
			        	state.setPacket(packet);
			        } else if (packet.getStreamIndex() == videoStreamID && videoCoder != null) {
			        	// We are currently not interested in video packets,
			        	// add this packet to the video pending packets
			        	videoStreamState.addPacket(packet);
			        } else if (packet.getStreamIndex() == audioStreamID && audioCoder != null) {
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

    private boolean decodeVideoPacket(StreamState state) {
    	boolean complete = false;
        while (!state.isPacketEmpty()) {
        	int decodedBytes = videoCoder.decodeVideo(videoPicture, state.getPacket(), state.getOffset());

            if (decodedBytes < 0) {
            	// An error occured with this packet, skip it
            	state.releasePacket();
            	break;
            }

            state.consume(decodedBytes);
            state.updateTimestamps();

        	if (videoPicture.isComplete()) {
            	if (videoConverter != null) {
            		currentImg = videoConverter.toImage(videoPicture);
            	}
            	complete = true;
            	break;
            }
        }

        return complete;
    }

    private boolean decodeAudioPacket(StreamState state) {
    	boolean complete = false;
        while (!state.isPacketEmpty()) {
        	int decodedBytes = audioCoder.decodeAudio(audioSamples, state.getPacket(), state.getOffset());

            if (decodedBytes < 0) {
            	// An error occured with this packet, skip it
            	state.releasePacket();
            	break;
            }

            state.consume(decodedBytes);
            state.updateTimestamps();

        	if (audioSamples.isComplete()) {
                updateSoundSamples(audioSamples);
                complete = true;
                break;
            }
        }

        return complete;
    }

    public boolean stepVideo() {
    	return step(videoStreamState);
    }

    public boolean stepAudio() {
    	return step(audioStreamState);
    }

    private boolean step(StreamState state) {
    	boolean complete = false;

    	while (!complete) {
    		if (!getNextPacket(state)) {
	        	videoStreamState.incrementTimestamps(sceMpeg.videoTimestampStep);
	        	audioStreamState.incrementTimestamps(sceMpeg.audioTimestampStep);
	        	break;
    		}

	        if (state.isStream(videoStreamID)) {
	        	if (videoCoder == null) {
	        		// No video coder, skip all the video packets
	        		state.releasePacket();
	        		complete = true;
	        	} else {
	        		// Decode the current video packet
	        		// and check if we have a complete video sample
	        		complete = decodeVideoPacket(state);
	            }
	        } else if (state.isStream(audioStreamID)) {
	        	if (audioCoder == null) {
	        		// No audio coder, skip all the audio packets
	        		state.releasePacket();
	        		complete = true;
	        	} else {
	        		// Decode the current audio packet
	        		// and check if we have a complete audio sample
	        		complete = decodeAudioPacket(state);
	            }
	        }
    	}

        return complete;
    }

    public void stepExtAudio(int mpegStreamSize) {
    	if (extContainer != null) {
    		stepExtAudio();
    	} else if (!extAudioChecked) {
            extAudioChecked = true;
            audioStreamState = new StreamState(this, -1);
            audioStreamState.setTimestamps(sceMpeg.mpegTimestampPerSecond);
            String pmfExtAudioPath = "tmp/" + jpcsp.State.discId + "/Mpeg-" + mpegStreamSize + "/ExtAudio.";
            String supportedFormats[] = {"wav", "mp3", "at3", "raw", "wma", "flac"};
            for (int i = 0; i < supportedFormats.length; i++) {
                File f = new File(pmfExtAudioPath + supportedFormats[i]);
                if (f.exists()) {
                    pmfExtAudioPath += supportedFormats[i];
                    initExtAudio(pmfExtAudioPath);
                    break;
                }
            }
    	} else {
    		audioStreamState.incrementTimestamps(sceMpeg.audioTimestampStep);
    	}
    }

    // Override audio data line with one from an external file.
    private void initExtAudio(String file) {
        extContainer = IContainer.make();

        if (extContainer.open(file, IContainer.Type.READ, null) < 0) {
            Modules.log.error("MediaEngine: Invalid file or container format: " + file);
            extContainer.close();
            extContainer = null;
            return;
        }

        int extNumStreams = extContainer.getNumStreams();

        extAudioStreamID = -1;
        extAudioCoder = null;

        for (int i = 0; i < extNumStreams; i++) {
            IStream stream = extContainer.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (extAudioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                extAudioStreamID = i;
                extAudioCoder = coder;
            }
        }

        if (extAudioStreamID == -1) {
            Modules.log.error("MediaEngine: No audio streams found!");
            extContainer.close();
            extContainer = null;
            return;
        } else if (extAudioCoder.open() < 0) {
            Modules.log.error("MediaEngine: Can't open audio decoder!");
            extContainer.close();
            extContainer = null;
            return;
        }

		extAudioSamples = IAudioSamples.make(2048, extAudioCoder.getChannels());

        startSound(extAudioCoder);

        extPacket = IPacket.make();
    }

    private void stepExtAudio() {
    	audioStreamState.incrementTimestamps(sceMpeg.audioTimestampStep);

		// Keep the SoundChannel full to avoid sound pauses
		while (!extSoundChannel.isOutputBlocking()) {
	    	if (extContainer.readNextPacket(extPacket) < 0) {
	    		return;
	    	}

        	int decodedBytes;
            for (int offset = 0; offset < extPacket.getSize(); offset += decodedBytes) {
            	decodedBytes = extAudioCoder.decodeAudio(extAudioSamples, extPacket, offset);

                if (decodedBytes < 0) {
	            	// An error occured with this packet, skip it
                	break;
                }

                if (extAudioSamples.isComplete()) {
                    playSound(extAudioSamples);
                }
            }
        }
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
    	if (extAudioCoder != null) {
    		extAudioCoder.close();
    		extAudioCoder = null;
    	}
    	if (extPacket != null) {
    		extPacket.delete();
    		extPacket = null;
    	}
    	if (extSoundChannel != null) {
    		extSoundChannel.release();
    		extSoundChannel = null;
    	}
    	if (extAudioSamples != null) {
    		extAudioSamples.delete();
    		extAudioSamples = null;
    	}
    	if (currentSamples != null) {
    		currentSamples.delete();
            currentSamples = null;
    	}
    }

    private void startSound(IStreamCoder aAudioCoder) {
    	extSoundChannel = new SoundChannel(0);
    	extSoundChannel.setSampleRate(aAudioCoder.getSampleRate());
    	extSoundChannel.setFormat(aAudioCoder.getChannels() >= 2 ? SoundChannel.FORMAT_STEREO : SoundChannel.FORMAT_MONO);
    }

    private void playSound(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        extSoundChannel.play(rawBytes);
    }

    // Continuous sample update function for internal audio decoding.
    private void updateSoundSamples(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());

        currentSamples.write(rawBytes);
    }

    public void writeVideoImage(int dest_addr, int frameWidth, int videoPixelMode) {
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        // Get the current generated image, convert it to pixels and write it
        // to memory.
        if (getCurrentImg() != null) {
            // Override the base dimensions with the image's real dimensions.
            int width = getCurrentImg().getWidth();
            int height = getCurrentImg().getHeight();
            int imageSize = height * width;
            if (videoImagePixels == null || videoImagePixels.length < imageSize) {
            	videoImagePixels = new int[imageSize];
            }
            videoImagePixels = getCurrentImg().getRGB(0, 0, width, height, videoImagePixels, 0, width);
            for (int y = 0; y < height; y++) {
                int address = dest_addr + y * frameWidth * bytesPerPixel;
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, bytesPerPixel);
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
            }
        }
    }
}
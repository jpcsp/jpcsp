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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import jpcsp.HLE.Modules;

import com.xuggle.ferry.Logger;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;

public class MediaEngine {

    private static MediaEngine instance;
    private static IContainer container;
    private static IPacket packet;
    private static int numStreams;
    private static IStreamCoder videoCoder;
    private static IStreamCoder audioCoder;
    private static int videoStreamID;
    private static int audioStreamID;
    private static SourceDataLine audioLine;
    private static long clockStartTime;
    private static long firstTimestamp;
    private static JFrame movieFrame;
    private static BufferedImage currentImg;
    private static byte[] currentSamples;
    private static int decodedAudioBytes;
    private static int decodedVideoBytes;

    // External audio loading vars.
    private static IContainer extContainer;
    private static IPacket extPacket;
    private static IStreamCoder extAudioCoder;
    private static int extAudioStreamID;

    public MediaEngine() {
        // Disable Xuggler's logging, since we do our own.
        Logger.setGlobalIsLogging(Logger.Level.LEVEL_DEBUG, false);
        Logger.setGlobalIsLogging(Logger.Level.LEVEL_ERROR, false);
        Logger.setGlobalIsLogging(Logger.Level.LEVEL_INFO, false);
        Logger.setGlobalIsLogging(Logger.Level.LEVEL_TRACE, false);
        Logger.setGlobalIsLogging(Logger.Level.LEVEL_WARN, false);

        instance = this;
    }

    public static MediaEngine getInstance() {
        return instance;
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

    public byte[] getCurrentAudioSamples() {
        return currentSamples;
    }

    public int getDecodedVideoBytes() {
        return decodedVideoBytes;
    }

    public int getDecodedAudioBytes() {
        return decodedAudioBytes;
    }

    /**
     * Method getPacketTimestamp() - analyzes the current packet
     * and returns it's timestamp.
     * Note: Only call this after a sucessful Media Engine init().
     *
     * @param stream the stream to check for (video or audio).
     * @param type the timestamp format (PTS or DTS).
     * @return the timestamp of the current packet or 0 if none.
     */
    public long getPacketTimestamp(String stream, String type) {
        if (stream.equals("Video")) {
            if (packet.getStreamIndex() == getVideoStreamID()) {
                if (type.equals("DTS")) {
                    return packet.getDts();
                } else if (type.equals("PTS")) {
                    return packet.getPts();
                }
            }
        } else if (stream.equals("Audio")) {
            if (packet.getStreamIndex() == getAudioStreamID()) {
                if (type.equals("DTS")) {
                    return packet.getDts();
                } else if (type.equals("PTS")) {
                    return packet.getPts();
                }
            }
        }
        return 0;
    }

    /*
     * Split version of decodeAndPlay.
     *
     * This method is to be used when the video and audio frames
     * are decoded and played step by step (sceMpeg case).
     * The sceMpeg functions must call init() first for each MPEG stream and then
     * keep calling step() until the video is finished and finish() is called.
     */
    public void init(String file) {
        container = IContainer.make();

        if (container.open(file, IContainer.Type.READ, null) < 0) {
            Modules.log.error("MediaEngine: Invalid file or container format!");
        }

        numStreams = container.getNumStreams();

        videoStreamID = -1;
        videoCoder = null;
        audioStreamID = -1;
        audioCoder = null;

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

        if (videoStreamID == -1) {
            Modules.log.info("MediaEngine: No video streams found!");
        } else if (videoCoder.open() < 0) {
            Modules.log.error("MediaEngine: Can't open video decoder!");
        }

        if (audioStreamID == -1) {
            Modules.log.info("MediaEngine: No audio streams found!");
        } else if (audioCoder.open() < 0) {
            Modules.log.error("MediaEngine: Can't open audio decoder!");
        } else {
            try {
                startSound(audioCoder);
            } catch (LineUnavailableException ex) {
                Modules.log.error("MediaEngine: Can't start audio line!");
            }
        }

        packet = IPacket.make();
        firstTimestamp = Global.NO_PTS;
        clockStartTime = 0;
    }

    @SuppressWarnings("deprecation")
    public void step() {
        container.readNextPacket(packet);

        if (packet.getStreamIndex() == videoStreamID && videoCoder != null) {

            IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                    videoCoder.getWidth(), videoCoder.getHeight());

            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                picture = resample(picture, IPixelFormat.Type.BGR24);
            }

            decodedVideoBytes = videoCoder.decodeVideo(picture, packet, 0);

            if (decodedVideoBytes < 0) {
                Modules.log.error("MediaEngine: No video bytes decoded!");
                return;
            }

            if (picture.isComplete()) {
                currentImg = Utils.videoPictureToImage(picture);
            }
        } else if (packet.getStreamIndex() == audioStreamID && audioCoder != null) {
            IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

            int offset = 0;
            while (offset < packet.getSize()) {
                decodedAudioBytes = audioCoder.decodeAudio(samples, packet, offset);

                if (decodedAudioBytes < 0) {
                    Modules.log.error("MediaEngine: No audio bytes decoded!");
                    return;
                }

                offset += decodedAudioBytes;

                if (samples.isComplete()) {
                    updateSoundSamples(samples);
                }
            }
        }
    }

    // Override audio data line with one from an external file.
    public void initExtAudio(String file) {
        extContainer = IContainer.make();

        if (extContainer.open(file, IContainer.Type.READ, null) < 0) {
            Modules.log.error("MediaEngine: Invalid file or container format!");
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
        } else if (extAudioCoder.open() < 0) {
            Modules.log.error("MediaEngine: Can't open audio decoder!");
        }

        try {
            startSound(extAudioCoder);
        } catch (LineUnavailableException ex) {
            Modules.log.error("MediaEngine: Can't start audio line!");

        }
        extPacket = IPacket.make();
    }

    public void stepExtAudio() {
        extContainer.readNextPacket(extPacket);

        if (extPacket.getStreamIndex() == extAudioStreamID && extAudioCoder != null) {
            IAudioSamples samples = IAudioSamples.make(1024, extAudioCoder.getChannels());

            int offset = 0;
            while (offset < extPacket.getSize()) {
                int bytesDecoded = extAudioCoder.decodeAudio(samples, extPacket, offset);

                if (bytesDecoded < 0) {
                    Modules.log.error("MediaEngine: No audio bytes decoded!");
                    return;
                }

                offset += bytesDecoded;

                if (samples.isComplete()) {
                    playSound(samples);
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
        if (extContainer != null) {
            extContainer.close();
            extContainer = null;
        }
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (extAudioCoder != null) {
            extAudioCoder.close();
            extAudioCoder = null;
        }
        if (audioLine != null) {
            audioLine.drain();
            audioLine.close();
            audioLine = null;
        }
        if (movieFrame != null) {
            movieFrame.dispose();
            movieFrame = null;
        }
        if(currentSamples != null) {
            currentSamples = null;
        }
    }

    private static long calculateDelay(IVideoPicture picture) {
        long millisecondsToSleep = 0;
        if (firstTimestamp == Global.NO_PTS) {
            firstTimestamp = picture.getTimeStamp();
            clockStartTime = System.currentTimeMillis();
            millisecondsToSleep = 0;
        } else {
            long systemClockCurrentTime = System.currentTimeMillis();
            long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - clockStartTime;
            long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - firstTimestamp) / 1000;
            final long millisecondsTolerance = 50;
            millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo -
                    (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
        }
        return millisecondsToSleep;
    }

    // This function attempts to resample an IVideoPicture to any given
    // pixel format.
    private IVideoPicture resample(IVideoPicture picture, IPixelFormat.Type pixel) {
        IVideoResampler resampler = null;

        resampler = IVideoResampler.make(videoCoder.getWidth(),
                videoCoder.getHeight(), pixel,
                videoCoder.getWidth(), videoCoder.getHeight(),
                videoCoder.getPixelType());

        if (resampler != null) {
            picture = IVideoPicture.make(resampler.getOutputPixelFormat(),
                    picture.getWidth(), picture.getHeight());
        }

        return picture;
    }

    // Sound sampling functions also based on Xuggler's demos.
    private static void startSound(IStreamCoder aAudioCoder) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
                (int) IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()),
                aAudioCoder.getChannels(),
                true,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(audioFormat);
        audioLine.start();
    }

    private static void playSound(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        audioLine.write(rawBytes, 0, aSamples.getSize());
    }

    private static void updateSoundSamples(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());

        if(currentSamples == null) {
            currentSamples = rawBytes;
        } else {
            byte[] temp = new byte[rawBytes.length + currentSamples.length];
            System.arraycopy(currentSamples, 0, temp, 0, currentSamples.length);
            System.arraycopy(rawBytes, 0, temp, currentSamples.length, rawBytes.length);

            currentSamples = temp;
        }
    }
}
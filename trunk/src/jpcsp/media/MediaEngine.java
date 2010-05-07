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

import jpcsp.Controller;
import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.Settings;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import com.xuggle.ferry.Logger;

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

        if (container.open(file, IContainer.Type.READ, null) < 0)
            Modules.log.error("MediaEngine: Invalid file or container format!");

        numStreams = container.getNumStreams();

        videoStreamID = -1;
        videoCoder = null;
        audioStreamID = -1;
        audioCoder = null;

        for(int i = 0; i < numStreams; i++) {
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

        if (videoStreamID == -1)
            Modules.log.error("MediaEngine: No video streams found!");
        else if (videoCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open video decoder!");

        if (audioStreamID == -1)
            Modules.log.error("MediaEngine: No audio streams found!");
        else if (audioCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open audio decoder!");
        else {
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

    @SuppressWarnings("deprecated")
    public void step() {
        container.readNextPacket(packet);

        if (packet.getStreamIndex() == videoStreamID && videoCoder != null) {

            IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                    videoCoder.getWidth(), videoCoder.getHeight());

            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                picture = resample(picture, IPixelFormat.Type.BGR24);
            }

            int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);

            if (bytesDecoded < 0)
                Modules.log.error("MediaEngine: No video bytes decoded!");

            if (picture.isComplete())
                currentImg = Utils.videoPictureToImage(picture);
        } else if (packet.getStreamIndex() == audioStreamID && audioCoder != null) {
            IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

            int offset = 0;
            while(offset < packet.getSize()) {
                int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);

                if (bytesDecoded < 0)
                    Modules.log.error("MediaEngine: No audio bytes decoded!");

                offset += bytesDecoded;

                if (samples.isComplete())
                    playSound(samples);
            }
        }
    }

    // Override audio data line with one from an external file.
    public void initExtAudio(String file) {
        extContainer = IContainer.make();

        if (extContainer.open(file, IContainer.Type.READ, null) < 0)
            Modules.log.error("MediaEngine: Invalid file or container format!");

        int extNumStreams = extContainer.getNumStreams();

        extAudioStreamID = -1;
        extAudioCoder = null;

        for(int i = 0; i < extNumStreams; i++) {
            IStream stream = extContainer.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (extAudioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                extAudioStreamID = i;
                extAudioCoder = coder;
            }
        }

        if (extAudioStreamID == -1)
            Modules.log.error("MediaEngine: No audio streams found!");
        else if (extAudioCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open audio decoder!");

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
            while(offset < extPacket.getSize()) {
                int bytesDecoded = extAudioCoder.decodeAudio(samples, extPacket, offset);

                if (bytesDecoded < 0)
                    Modules.log.error("MediaEngine: No audio bytes decoded!");

                offset += bytesDecoded;

                if (samples.isComplete())
                    playSound(samples);
            }
        }
    }

    /*
     * Function based on Xuggler's demos.
     * Given a certain file, it should parse it, look for the video stream,
     * and generate images from each packet.
     * Time control is based on timestamps, but it needs a small delay to
     * avoid speedups.
     *
     * This method is used when the video is supposed to be played
     * all at once (scePsmfPlayer case).
     */
    @SuppressWarnings("deprecated")
    public void decodeAndPlay(String file) {
        container = IContainer.make();

        if (container.open(file, IContainer.Type.READ, null) < 0)
            Modules.log.error("MediaEngine: Invalid file or container format!");

        numStreams = container.getNumStreams();

        videoStreamID = -1;
        videoCoder = null;
        audioStreamID = -1;
        audioCoder = null;

        for(int i = 0; i < numStreams; i++) {
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

        if (videoStreamID == -1)
            Modules.log.error("MediaEngine: No video streams found!");
        else if (videoCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open video decoder!");

        if (audioStreamID == -1)
            Modules.log.error("MediaEngine: No audio streams found!");
        else if (audioCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open audio decoder!");
        else {
            try {
                startSound(audioCoder);
            } catch (LineUnavailableException ex) {
                Modules.log.error("MediaEngine: Can't start audio line!");
            }
        }

        packet = IPacket.make();
        firstTimestamp = Global.NO_PTS;
        clockStartTime = 0;

        while(container.readNextPacket(packet) >= 0) {
            // Break the loop if the "START" key was pressed.
            if(checkSkip())
                break;

            // If the emulator is paused, waste time and let the
            // thread sleep to compensate possible video speedups.
            do{
                try {
                    Thread.sleep(10);
                } catch(Exception e) {}
            } while(checkPause());

            if (packet.getStreamIndex() == videoStreamID && videoCoder != null) {

                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());

                if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                    picture = resample(picture, IPixelFormat.Type.BGR24);
                }

                int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);

                if (bytesDecoded < 0)
                    Modules.log.error("MediaEngine: No video bytes decoded!");

           if (picture.isComplete()) {
               long delay = calculateDelay(picture);

                   if (delay > 0) {
                       try {
                           Thread.sleep(delay);
                       } catch (InterruptedException e) {
                           return;
                       }
                   }
               }
               BufferedImage img = Utils.videoPictureToImage(picture);
               displayImage(img);

            } else if (packet.getStreamIndex() == audioStreamID && audioCoder != null) {
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

                int offset = 0;
                while(offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);

                    if (bytesDecoded < 0)
                        Modules.log.error("MediaEngine: No audio bytes decoded!");

                    offset += bytesDecoded;

                    if (samples.isComplete())
                        playSound(samples);
                }
            } else {
                do {} while(false);
            }
        }
        finish();
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
        if(movieFrame != null) {
            movieFrame.dispose();
            movieFrame = null;
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
            long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - firstTimestamp)/1000;
            final long millisecondsTolerance = 50;
            millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo -
                    (millisecondsClockTimeSinceStartofVideo+millisecondsTolerance));
        }
        return millisecondsToSleep;
    }

    // This function attempts to resample an IVideoPicture to any given
    // pixel format.
    private IVideoPicture resample(IVideoPicture picture, IPixelFormat.Type pixel){
        IVideoResampler resampler = null;

            resampler = IVideoResampler.make(videoCoder.getWidth(),
                    videoCoder.getHeight(), pixel,
                    videoCoder.getWidth(), videoCoder.getHeight(),
                    videoCoder.getPixelType());

            if(resampler != null) {
                picture = IVideoPicture.make(resampler.getOutputPixelFormat(),
                        picture.getWidth(), picture.getHeight());
            }

        return picture;
     }

    /*
     * Main interaction functions.
     */
    private void displayImage(BufferedImage img) {
        if(movieFrame == null) {
            movieFrame = new JFrame("JPCSP - Movie Playback");
            movieFrame.setUndecorated(true);
            movieFrame.setSize(img.getWidth(), img.getHeight());
            int pos[] = Settings.getInstance().readWindowPos("mainwindow");
            movieFrame.setLocation(pos[0] + 4, pos[1] + 76);
            movieFrame.setResizable(false);
            movieFrame.setAlwaysOnTop(true);
            movieFrame.setVisible(true);
        }

        Graphics g = movieFrame.getGraphics();
        g.drawImage(img, 0, 0, null);
    }

    // Sound sampling functions also based on Xuggler's demos.
    private static void startSound(IStreamCoder aAudioCoder) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
                (int)IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()),
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

    private boolean checkSkip() {
        Controller control = Controller.getInstance();

        if(control.isKeyPressed(jpcsp.Controller.keyCode.START))
            return true;

        return false;
    }

    private boolean checkPause() {
        Emulator emu = Emulator.getInstance();

        if(emu.pause)
            return true;

        return false;
    }
}
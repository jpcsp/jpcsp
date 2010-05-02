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
import jpcsp.HLE.pspdisplay;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

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
    private static int numStreams;
    private static IStreamCoder videoCoder;
    private static IStreamCoder audioCoder;
    private static int videoStreamID;
    private static int audioStreamID;
    private static SourceDataLine audioLine;
    private static long clockStartTime;
    private static long firstTimestamp;

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

    public IContainer getContainer() {
        return container;
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

    // Function based on Xuggler's demos.
    // Given a certain file, it should parse it, look for the video stream,
    // and generate images from each packet.
    // Time control is based on timestamps, but it needs a small delay to
    // avoid speedups.
    @SuppressWarnings("deprecated")
    public void decode(String file) {
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

        IPacket packet = IPacket.make();
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

    // Cleanup function.
    private void finish() {
        if (container !=null) {
            container.close();
            container = null;
        }
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (audioLine != null) {
            audioLine.drain();
            audioLine.close();
            audioLine=null;
        }
    }

    // Hook pspdisplay and show each image.
    // Currently we're just overlaying the canvas
    // so the video plays independently and with it's
    // own time.
    // TODO: Integrate this better with our decoding method.
    private void displayImage(BufferedImage img) {
        pspdisplay display = pspdisplay.getInstance();
        Graphics g = display.getGraphics();
        g.drawImage(img, 0, 0, null);
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
}
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

import static jpcsp.media.MediaEngine.streamCoderOpen;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jpcsp.Emulator;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.media.MediaEngine;
import jpcsp.settings.Settings;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioResampler;
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
import jpcsp.util.Constants;
import jpcsp.util.Utilities;

public class UmdBrowserPmf {

    private UmdIsoReader iso;
    private UmdIsoFile isoFile;
    private String fileName;
    private IContainer container;
    private IAudioResampler audioResampler;
    private IVideoResampler videoResampler;
    private int videoStreamId;
    private IStreamCoder videoCoder;
    private int audioStreamId;
    private IStreamCoder audioCoder;
    private IPacket packet;
    private long firstTimestampInStream;
    private long systemClockStartTime;
    private IConverter converter;
    private BufferedImage image;
    private boolean done;
    private boolean endOfVideo;
    private boolean threadExit;
    private JLabel display;
    private PmfDisplayThread displayThread;
    private PmfByteChannel byteChannel;
    private SourceDataLine mLine;

    public UmdBrowserPmf(UmdIsoReader iso, String fileName, JLabel display) {
        this.iso = iso;
        this.fileName = fileName;
        this.display = display;

        init();
        initVideo();
    }

    private void init() {
        image = null;
        done = false;
        threadExit = false;

        MediaEngine.initXuggler();
        isoFile = null;
        try {
            isoFile = iso.getFile(fileName);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            Emulator.log.error(e);
        }
    }

    private Image getImage() {
        return image;
    }

    final public boolean initVideo() {
        if (isoFile == null) {
            return false;
        }

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

        try {
            container = IContainer.make();
        } catch (Throwable e) {
            // The xuggler libraries are probably not available
            Emulator.log.error(e);
            return false;
        }

        try {
            isoFile.seek(0);
        } catch (IOException e) {
            Emulator.log.error(e);
            return false;
        }
        byteChannel = new PmfByteChannel(isoFile);

        if (container.open(byteChannel, null) < 0) {
            Emulator.log.error("could not open file: " + fileName);
            return false;
        }

        // query how many streams the call to open found
        int numStreams = container.getNumStreams();

        // and iterate through the streams to find the first video and audio stream
        videoStreamId = -1;
        videoCoder = null;
        audioStreamId = -1;
        audioCoder = null;
        boolean audioMuted = Settings.getInstance().readBool("emu.mutesound");
        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO && !audioMuted) {
                audioStreamId = i;
                audioCoder = coder;
            }
        }

        /*
         * Now we have found the audio and video streams in this file.
         * Let's open up our decoder so it can do work.
         */
        if (videoCoder != null && streamCoderOpen(videoCoder) < 0) {
            Emulator.log.error("could not open video decoder for container: " + fileName);
            return false;
        }
        if (audioCoder != null && streamCoderOpen(audioCoder) < 0) {
            Emulator.log.info("could not open audio decoder for container: " + fileName);
            return false;
        }
        if (!Settings.getInstance().readBool("emu.useAtrac3plus")) {
            Emulator.log.info("Unsupported Atrac3+ data!");
            return false;
        }

        videoResampler = null;
        audioResampler = null;
        if (videoCoder != null) {
            converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight());

            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24 || videoCoder.getWidth() != Constants.ICON0_WIDTH || videoCoder.getHeight() != Constants.ICON0_HEIGHT) {
                // if this stream is not in BGR24, we're going to need to
                // convert it.  The VideoResampler does that for us.
                videoResampler = IVideoResampler.make(Constants.ICON0_WIDTH, Constants.ICON0_HEIGHT, IPixelFormat.Type.BGR24,
                        videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());

                if (videoResampler == null) {
                    Emulator.log.error("could not create color space resampler for: " + fileName);
                    return false;
                }
            }
        }

        if (audioCoder != null) {
            // If the audio is Atrac3+, we must resample it first.
            if (audioCoder.getSampleFormat() == IAudioSamples.Format.FMT_FLTP) {
                audioResampler = IAudioResampler.make(audioCoder.getChannels(), audioCoder.getChannels(),
                audioCoder.getSampleRate(), audioCoder.getSampleRate(),
                IAudioSamples.Format.FMT_S16, audioCoder.getSampleFormat());
                
                if (audioResampler == null) {
                    Emulator.log.error("could not create audio resampler for: " + fileName);
                    return false;
                }
            }
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
        
        if (audioResampler != null) {
            audioResampler.delete();
            audioResampler = null;
        }

        if (videoResampler != null) {
            videoResampler.delete();
            videoResampler = null;
        }

        if (converter != null) {
            converter.delete();
            converter = null;
        }

        if (packet != null) {
            packet.delete();
            packet = null;
        }
    }

    private void loopVideo() {
        closeVideo();
        closeAudio();
        startVideo();
    }

    private void stopDisplayThread() {
        while (displayThread != null && !threadExit) {
            done = true;
            sleep(1);
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
            /*
             * Now we have a packet, let's see if it belongs to our video stream
             */
            if (packet.getStreamIndex() == videoStreamId && videoCoder != null) {
                /*
                 * We allocate a new picture to get the data out of Xuggler
                 */
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());

                int offset = 0;
                while (offset < packet.getSize()) {
                    /*
                     * Now, we decode the video, checking for any errors.
                     *
                     */
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                    if (bytesDecoded < 0) {
                        throw new RuntimeException("got error decoding video in: " + fileName);
                    }
                    offset += bytesDecoded;

                    /*
                     * Some decoders will consume data in a packet, but will not be able to construct
                     * a full video picture yet.  Therefore you should always check if you
                     * got a complete picture from the decoder
                     */
                    if (picture.isComplete()) {
                        IVideoPicture newPic = picture;
                        /*
                         * If the resampler is not null, that means we didn't get the
                         * video in BGR24 format and
                         * need to convert it into BGR24 format.
                         */
                        if (videoResampler != null) {
                            // we must resample
                            newPic = IVideoPicture.make(videoResampler.getOutputPixelFormat(), videoResampler.getOutputWidth(), videoResampler.getOutputHeight());
                            if (videoResampler.resample(newPic, picture) < 0) {
                                throw new RuntimeException("could not resample video from: " + fileName);
                            }
                        }
                        if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
                            throw new RuntimeException("could not decode video BGR 24 bit data in: " + fileName);
                        }

                        /**
                         * We could just display the images as quickly as we
                         * decode them, but it turns out we can decode a lot
                         * faster than you think.
                         *
                         * So instead, the following code does a poor-man's
                         * version of trying to match up the frame-rate
                         * requested for each IVideoPicture with the system
                         * clock time on your computer.
                         *
                         * Remember that all Xuggler IAudioSamples and
                         * IVideoPicture objects always give timestamps in
                         * Microseconds, relative to the first decoded item. If
                         * instead you used the packet timestamps, they can be
                         * in different units depending on your IContainer, and
                         * IStream and things can get hairy quickly.
                         */
                        if (firstTimestampInStream == Global.NO_PTS) {
                            // This is our first time through
                            firstTimestampInStream = picture.getTimeStamp();
                            // get the starting clock time so we can hold up frames
                            // until the right time.
                            systemClockStartTime = System.currentTimeMillis();
                        } else {
                            long systemClockCurrentTime = System.currentTimeMillis();
                            long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - systemClockStartTime;
                            // compute how long for this frame since the first frame in the
                            // stream.
                            // remember that IVideoPicture and IAudioSamples timestamps are
                            // always in MICROSECONDS,
                            // so we divide by 1000 to get milliseconds.
                            long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - firstTimestampInStream) / 1000;
                            final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
                            final long millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
                            sleep(millisecondsToSleep);
                        }

                        // And finally, convert the BGR24 to an Java buffered image
                        image = converter.toImage(newPic);
                    }
                }
            } else if (packet.getStreamIndex() == audioStreamId && audioCoder != null) {
                /*
                 * We allocate a set of samples with the same number of channels as the
                 * coder tells us is in this buffer.
                 *
                 * We also pass in a buffer size (1024 in our example), although Xuggler
                 * will probably allocate more space than just the 1024 (it's not important why).
                 */
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

                /*
                 * A packet can actually contain multiple sets of samples (or frames of samples
                 * in audio-decoding speak).  So, we may need to call decode audio multiple
                 * times at different offsets in the packet's data.  We capture that here.
                 */
                int offset = 0;

                /*
                 * Keep going until we've processed all data
                 */
                while (offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0) {
                        throw new RuntimeException("got error decoding audio in: " + fileName);
                    }
                    offset += bytesDecoded;
                    /*
                     * Some decoder will consume data in a packet, but will not be able to construct
                     * a full set of samples yet.  Therefore you should always check if you
                     * got a complete set of samples from the decoder
                     */
                    if (samples.isComplete()) { 
                        if (audioResampler != null) {
                            IAudioSamples newSamples = samples;
                            int samplesSize = samples.getSize();
                            newSamples = IAudioSamples.make(samplesSize, samples.getChannels());
                            if (audioResampler.resample(newSamples, samples, samplesSize) < 0) {
                                throw new RuntimeException("could not resample audio from: " + fileName);
                            }
                            playAtrac3plusAudio(newSamples);
                        } else {
                            playAudio(samples);
                        }
                    }
                }
            }
        } else {
            endOfVideo = true;
        }
    }

    private void openAudio(IStreamCoder aAudioCoder) {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
                16,
                aAudioCoder.getChannels(),
                true, /* xuggler defaults to signed 16 bit samples */
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            mLine = (SourceDataLine) AudioSystem.getLine(info);
            /**
             * if that succeeded, try opening the line.
             */
            mLine.open(audioFormat);
            /**
             * And if that succeed, start the line.
             */
            mLine.start();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("could not open audio line");
        }
    }

    private void playAudio(IAudioSamples aSamples) {
        /**
         * We're just going to dump all the samples into the line.
         */
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        mLine.write(rawBytes, 0, aSamples.getSize());
    }
    
    private void playAtrac3plusAudio(IAudioSamples aSamples) {
        int samplesSize = aSamples.getSize();
        byte[] rawBytes = aSamples.getData().getByteArray(0, samplesSize);
        
         // Fix the audio panning (Xuggler bug).
    	for (int i = 0, j = 0; i < samplesSize - 2; i++, j++) {
    		int src1 = Utilities.read8(rawBytes, j);
    		int src2 = Utilities.read8(rawBytes, j + 2);
    		int src = (src1 + src2);
    		rawBytes[i] = (byte) (src & 0xFF);
    	}
        
        mLine.write(rawBytes, 0, aSamples.getSize());
    }

    private void closeAudio() {
        if (mLine != null) {
            /*
             * Wait for the line to finish playing
             */
            mLine.drain();
            /*
             * Close the line.
             */
            mLine.close();
            mLine = null;
        }
    }

    private void sleep(long millis) {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // Ignore exception
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

    private static class PmfByteChannel implements ReadableByteChannel {

        private UmdIsoFile file;
        private byte[] buffer;

        public PmfByteChannel(UmdIsoFile file) {
            this.file = file;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int available = dst.remaining();
            if (buffer == null || buffer.length < available) {
                buffer = new byte[available];
            }

            int length = file.read(buffer, 0, available);
            if (length > 0) {
                dst.put(buffer, 0, length);
            }

            return length;
        }

        @Override
        public void close() throws IOException {
            file.close();
            file = null;
        }

        @Override
        public boolean isOpen() {
            return file != null;
        }
    }
}

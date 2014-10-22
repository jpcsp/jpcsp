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
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jpcsp.Emulator;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;

import com.xuggle.ferry.Logger;
import com.xuggle.xuggler.Global;
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
	private static org.apache.log4j.Logger log = Emulator.log;
    private UmdIsoReader iso;
    private UmdIsoFile isoFile;
    private String fileName;
    private IContainer container;
    private IVideoResampler videoResampler;
    private int videoStreamId;
    private IStreamCoder videoCoder;
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
    private static boolean initialized = false;

    public UmdBrowserPmf(UmdIsoReader iso, String fileName, JLabel display) {
        this.iso = iso;
        this.fileName = fileName;
        this.display = display;

        init();
        initVideo();
    }

    private static void initXuggler() {
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
    private static int streamCoderOpen(IStreamCoder streamCoder) {
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

    private void init() {
        image = null;
        done = false;
        threadExit = false;

        initXuggler();
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

        // and iterate through the streams to find the first video stream
        videoStreamId = -1;
        videoCoder = null;
        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            }
        }

        /*
         * Now we have found the video stream in this file.
         * Let's open up our decoder so it can do work.
         */
        if (videoCoder != null && streamCoderOpen(videoCoder) < 0) {
            Emulator.log.error("could not open video decoder for container: " + fileName);
            return false;
        }

        videoResampler = null;
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
                            Utilities.sleep((int) millisecondsToSleep, 0);
                        }

                        // And finally, convert the BGR24 to an Java buffered image
                        image = converter.toImage(newPic);
                    }
                }
            }
        } else {
            endOfVideo = true;
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

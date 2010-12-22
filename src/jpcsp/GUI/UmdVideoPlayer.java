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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.HLE.Modules;

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
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class UmdVideoPlayer implements KeyListener {

    private UmdIsoReader iso;
    private UmdIsoFile isoFile;
    private HashMap<Integer, MpsStreamInfo> mpsStreamMap;
    private int currentStreamIndex;
    private int screenWidth;
    private int screenHeigth;
    private boolean videoPaused;
    private String fileName;
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
    private boolean done;
    private boolean endOfVideo;
    private boolean threadExit;
    private JLabel display;
    private MpsDisplayThread displayThread;
    private MpsByteChannel byteChannel;
    private SourceDataLine mLine;

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
    }

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
    }

    public UmdVideoPlayer(MainGUI gui, UmdIsoReader iso) {
        this.iso = iso;
        screenWidth = 480;
        screenHeigth = 272;
        display = new JLabel();
        gui.remove(Modules.sceDisplayModule);
        gui.getContentPane().add(display, BorderLayout.CENTER);
        gui.addKeyListener(this);
        Insets insets = gui.getInsets();
        Dimension minSize = new Dimension(
                screenWidth + insets.left + insets.right,
                screenHeigth + insets.top + insets.bottom);
        gui.setMinimumSize(minSize);
        init();
    }

    @Override
    public void keyPressed(KeyEvent keyCode) {
        if (keyCode.getKeyCode() == KeyEvent.VK_RIGHT) {
            goToNextMpsStream();
        } else if ((keyCode.getKeyCode() == KeyEvent.VK_LEFT) && (currentStreamIndex > 0)) {
            goToPreviousMpsStream();
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
        mpsStreamMap = new HashMap<Integer, MpsStreamInfo>();
        currentStreamIndex = 0;
        parsePlaylistFile();
        Modules.log.info("Setting aspect ratio to 16:9");
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                Emulator.log.error(e);
            }
        }
        if (isoFile != null) {
            startVideo();
        }
    }

    private int endianSwap32(int x) {
        return Integer.reverseBytes(x);
    }

    private short endianSwap16(short x) {
        return Short.reverseBytes(x);
    }

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
                Modules.log.warn("Accessing invalid PLAYLIST.UMD file!");
            } else {
                Modules.log.info("Accessing valid PLAYLIST.UMD file: playListSize=" + playListSize + ", playListTracksNum=" + playListTracksNum);
            }
            for (int i = 0; i < playListTracksNum; i++) {
                int trackLength = endianSwap32(file.readInt());
                file.skipBytes(4);   // 0x03100332.
                file.skipBytes(29);  // NULL.
                file.skipBytes(2);   // 0x02E8.
                file.skipBytes(2);   // 0x0000/0x1000.
                file.skipBytes(4);   // Date? (found 0x20100509 - 2010/05/09 on a 2010 release).
                file.skipBytes(4);   // NULL.
                file.skipBytes(4);   // Unknown (found 0x00000900).
                file.skipBytes(1);   // Unknown size.
                file.skipBytes(731); // Unknown NULL area with size 0x2DB.
                int streamHeigth = (int) (file.readByte() * 0x10); // Stream's original heigth.
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
                int streamMarkerDataLength = endianSwap32(file.readInt());  // Stream's markers' data length.
                int streamMarkersNum = endianSwap16(file.readShort());  // Stream's number of markers.
                MpsStreamMarkerInfo[] streamMarkers = new MpsStreamMarkerInfo[streamMarkersNum];
                for (int j = 0; j < streamMarkersNum; j++) {
                    file.skipBytes(1); // 0x05.
                    int streamMarkerCharsNum = (int) file.readByte(); // Marker name length.
                    file.skipBytes(4); // NULL.
                    int streamMarkerTimestamp = endianSwap32(file.readInt());
                    file.skipBytes(6); // NULL.
                    byte[] markerBuf = new byte[24];
                    file.read(markerBuf, 0, 24);
                    String markerName = new String(markerBuf);
                    if ((j + 1) == streamMarkersNum) {
                        file.skip(2); // Skip terminator (NULL).
                    }
                    streamMarkers[j] = new MpsStreamMarkerInfo(markerName, streamMarkerTimestamp);
                }
                // Map this stream.
                MpsStreamInfo info = new MpsStreamInfo(streamName, streamWidth, streamHeigth, streamFirstTimestamp, streamLastTimestamp, streamMarkers);
                mpsStreamMap.put(i, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToNextMpsStream() {
        currentStreamIndex++;
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                Emulator.log.error(e);
            }
        }
        if (isoFile != null) {
            startVideo();
        }
    }

    private void goToPreviousMpsStream() {
        closeVideo();
        closeAudio();
        currentStreamIndex--;
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                Emulator.log.error(e);
            }
        }
        if (isoFile != null) {
            startVideo();
        }
    }

    private Image getImage() {
        return image;
    }

    public void takeScreenshot() {
        int tag = 0;
        File screenshot = new File(State.title + "-" + "Shot" + "-" + tag + ".png");
        File directory = new File(System.getProperty("user.dir"));
        for (File file : directory.listFiles()) {
            if (file.getName().equals(screenshot.getName())) {
               tag++;
               screenshot = new File(State.title + "-" + "Shot" + "-" + tag + ".png");
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

    public void initVideo() {
        if (displayThread == null) {
            displayThread = new MpsDisplayThread();
            displayThread.setDaemon(true);
            displayThread.setName("UMD Video Player Thread");
            displayThread.start();
        }
        videoPaused = false;
    }

    public void pauseVideo() {
        videoPaused = true;
    }

    private boolean startVideo() {
        endOfVideo = false;
        videoPaused = false;

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
        byteChannel = new MpsByteChannel(isoFile);

        if (container.open(byteChannel, null) < 0) {
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

        if (videoCoder != null && videoCoder.open() < 0) {
            Emulator.log.error("could not open video decoder for container: " + fileName);
            return false;
        }
        if (audioCoder != null && audioCoder.open() < 0) {
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
                    if (picture.isComplete()) {
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
                            sleep(millisecondsToSleep);
                        }
                        if ((converter != null) && (newPic != null)) {
                            image = converter.toImage(newPic);
                        }
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

    private void sleep(long millis) {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // Ignore exception
            }
        }
    }

    private class MpsDisplayThread extends Thread {

        @Override
        public void run() {
            while (!done) {
                while (!endOfVideo && !done) {
                    if (!videoPaused) {
                        stepVideo();
                        if (display != null && image != null) {
                            display.setIcon(new ImageIcon(getImage()));
                        }
                    }
                }
                goToNextMpsStream();
            }
            threadExit = true;
        }
    }

    private static class MpsByteChannel implements ReadableByteChannel {

        private UmdIsoFile file;
        private byte[] buffer;
        private int bufOffset;

        public MpsByteChannel(UmdIsoFile file) {
            this.file = file;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int available = dst.remaining();
            if (buffer == null || buffer.length < available) {
                buffer = new byte[available];
            }

            int length = file.read(buffer, bufOffset, available);
            if (length > 0) {
                dst.put(buffer, bufOffset, length);
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
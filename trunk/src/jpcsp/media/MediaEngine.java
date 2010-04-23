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

import jpcsp.HLE.Modules;
import jpcsp.HLE.pspdisplay;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.xuggle.xuggler.Global;
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
    private static int streamID;

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

    public int getNumStreams() {
        return numStreams;
    }

    public IStreamCoder getVideoCoder() {
         return videoCoder;
    }

    public int getVideoStreamID() {
        return streamID;
    }

    // Function based on Xuggler's DecodeAndPlayVideo demo.
    // Given a certain file, it should parse it, look for the video stream,
    // and generate images from each packet.
    // Time control is based on timestamps, but it needs a small delay to
    // avoid speedups.
    @SuppressWarnings("deprecated")
    public void decodeVideo(String file) {
        container = IContainer.make();

        if (container.open(file, IContainer.Type.READ, null) < 0)
            Modules.log.error("MediaEngine: Invalid file or container format!");

        numStreams = container.getNumStreams();

        streamID = -1;
        videoCoder = null;

        for(int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                streamID = i;
                videoCoder = coder;
                break;
            }
        }

        if (streamID == -1)
            Modules.log.error("MediaEngine: No video streams found!");

        if (videoCoder.open() < 0)
            Modules.log.error("MediaEngine: Can't open video decoder!");

        IPacket packet = IPacket.make();
        long firstTimestampInStream = Global.NO_PTS;
        long systemClockStartTime = 0;

        while(container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == streamID) {

                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());

                if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                    picture = resample(picture, IPixelFormat.Type.BGR24);
                }

                int offset = 0;
                while(offset < packet.getSize()) {
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);

                    if (bytesDecoded < 0)
                        Modules.log.error("MediaEngine: No bytes decoded!");

                    offset += bytesDecoded;

           if (picture.isComplete()) {
               if (firstTimestampInStream == Global.NO_PTS) {
                   firstTimestampInStream = picture.getTimeStamp();
                   systemClockStartTime = System.currentTimeMillis();
               } else {
                   long systemClockCurrentTime = System.currentTimeMillis();
                   long millisecondsClockTimeSinceStartofVideo =
                           systemClockCurrentTime - systemClockStartTime;
                   long millisecondsStreamTimeSinceStartOfVideo =
                           (picture.getTimeStamp() - firstTimestampInStream)/1000;
                   final long millisecondsTolerance = 50;
                   final long millisecondsToSleep =
                           (millisecondsStreamTimeSinceStartOfVideo -
                           (millisecondsClockTimeSinceStartofVideo +
                           millisecondsTolerance));

                   if (millisecondsToSleep > 0) {
                       try {
                           Thread.sleep(millisecondsToSleep);
                       } catch (InterruptedException e) {
                           return;
                       }
                   }
               }
               BufferedImage img = Utils.videoPictureToImage(picture);
               displayImage(img);
           }
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
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }

        if (container !=null) {
            container.close();
            container = null;
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
}

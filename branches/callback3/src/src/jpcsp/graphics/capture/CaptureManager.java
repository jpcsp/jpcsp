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

assumes:
- list contains fbw/fbp command
- list clears the screen
- sceDisplaySetFrameBuf is called after the list has executed

todo:
- need to save GE state
  - texture on/off
  - blend on/off + params.
  - save matrices, looks like something wrong with projection
  - more ...
- don't save the same piece of ram twice (multiple texture uploads of the same texture/clut)
- capture multiple lists per frame, ideally we want to capture everything between two calls to sceDisplaySetFrameBuf
*/

package jpcsp.graphics.capture;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.nio.Buffer;

import org.apache.log4j.Level;


import jpcsp.Emulator;
import jpcsp.graphics.VideoEngine;
import jpcsp.HLE.kernel.types.PspGeList;

public class CaptureManager {

    private static OutputStream out;
    private static boolean captureInProgress;
    private static boolean listExecuted;
    private static CaptureFrameBufDetails replayFrameBufDetails;
    private static Level logLevel;

    public static void startReplay(String filename) {
        if (captureInProgress) {
            VideoEngine.log.error("Ignoring startReplay, capture is in progress");
            return;
        }

        VideoEngine.log.info("Starting replay: " + filename);

        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filename));

            while (in.available() > 0) {
                CaptureHeader header = CaptureHeader.read(in);
                int packetType = header.getPacketType();

                switch(packetType) {
                    case CaptureHeader.PACKET_TYPE_LIST:
                        CaptureList list = CaptureList.read(in);
                        list.commit();
                        break;

                    case CaptureHeader.PACKET_TYPE_RAM:
                        CaptureRAM ramFragment = CaptureRAM.read(in);
                        ramFragment.commit();
                        break;

                    // deprecated
                    case CaptureHeader.PACKET_TYPE_DISPLAY_DETAILS:
                        CaptureDisplayDetails displayDetails = CaptureDisplayDetails.read(in);
                        displayDetails.commit();
                        break;

                    case CaptureHeader.PACKET_TYPE_FRAMEBUF_DETAILS:
                        // don't replay this one immediately, wait until after the list has finished executing
                        replayFrameBufDetails = CaptureFrameBufDetails.read(in);
                        break;

                    default:
                        throw new Exception("Unknown packet type " + packetType);
                }
            }

            in.close();
        } catch(Exception e) {
            VideoEngine.log.error("Failed to start replay: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void endReplay() {
        // replay final sceDisplaySetFrameBuf
        replayFrameBufDetails.commit();
        replayFrameBufDetails = null;

        VideoEngine.log.info("Replay completed");
        Emulator.PauseEmu();
    }

    public static void startCapture(String filename, PspGeList list) {
    //public static void startCapture(int displayBufferAddress, int displayBufferWidth, int displayBufferPsm,
    //    int drawBufferAddress, int drawBufferWidth, int drawBufferPsm,
    //    int depthBufferAddress, int depthBufferWidth) {
        if (captureInProgress) {
            VideoEngine.log.error("Ignoring startCapture, capture is already in progress");
            return;
        }

        // Set the VideoEngine log level to TRACE when capturing,
        // the information in the log file is also interesting
        logLevel = VideoEngine.log.getLevel();
        VideoEngine.log.setLevel(Level.TRACE);

        try {
            VideoEngine.log.info("Starting capture... (list=" + list.id + ")");
            out = new BufferedOutputStream(new FileOutputStream(filename));

            CaptureHeader header;

            /*
            // write render target details
            header = new CaptureHeader(CaptureHeader.PACKET_TYPE_DISPLAY_DETAILS);
            header.write(out);
            CaptureDisplayDetails displayDetails = new CaptureDisplayDetails();
            displayDetails.write(out);
            */

            // write command buffer
            header = new CaptureHeader(CaptureHeader.PACKET_TYPE_LIST);
            header.write(out);
            CaptureList commandList = new CaptureList(list);
            commandList.write(out);

            captureInProgress = true;
            listExecuted = false;
        } catch(Exception e) {
            VideoEngine.log.error("Failed to start capture: " + e.getMessage());
            e.printStackTrace();
            Emulator.PauseEmu();
        }
    }

    public static void endCapture() {
        if (!captureInProgress) {
            VideoEngine.log.warn("Ignoring endCapture, capture hasn't been started");
            Emulator.PauseEmu();
            return;
        }

        try {
            out.flush();
            out.close();
            out = null;
        } catch(Exception e) {
            VideoEngine.log.error("Failed to end capture: " + e.getMessage());
            e.printStackTrace();
            Emulator.PauseEmu();
        }

        captureInProgress = false;

        VideoEngine.log.info("Capture completed");
        VideoEngine.log.setLevel(logLevel);
        Emulator.PauseEmu();
    }

    public static void captureRAM(int address, int length) {
        if (!captureInProgress) {
            VideoEngine.log.warn("Ignoring captureRAM, capture hasn't been started");
            return;
        }

        try {
            // write ram fragment
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
            header.write(out);

            CaptureRAM captureRAM = new CaptureRAM(address, length);
            captureRAM.write(out);
        } catch(Exception e) {
            VideoEngine.log.error("Failed to capture RAM: " + e.getMessage());
            e.printStackTrace();
            Emulator.PauseEmu();
        }
    }

    public static void captureImage(int imageaddr, int level, Buffer buffer, int width, int height, int bufferWidth, int imageType, boolean compressedImage, int compressedImageSize) {
        if (!captureInProgress) {
            VideoEngine.log.warn("Ignoring captureImage, capture hasn't been started");
            return;
        }

        try {
            // write image to the file system, not to the capture file itself
            CaptureImage captureImage = new CaptureImage(imageaddr, level, buffer, width, height, bufferWidth, imageType, compressedImage, compressedImageSize);
            captureImage.write();
        } catch(Exception e) {
            VideoEngine.log.error("Failed to capture Image: " + e.getMessage());
            e.printStackTrace();
            Emulator.PauseEmu();
        }
    }

    public static void captureFrameBufDetails() {
        if (!captureInProgress) {
            VideoEngine.log.warn("Ignoring captureRAM, capture hasn't been started");
            return;
        }

        try {
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_FRAMEBUF_DETAILS);
            header.write(out);

            CaptureFrameBufDetails details = new CaptureFrameBufDetails();
            details.write(out);
        } catch(Exception e) {
            VideoEngine.log.error("Failed to capture frame buf details: " + e.getMessage());
            e.printStackTrace();
            Emulator.PauseEmu();
        }
    }

    public static void markListExecuted() {
        listExecuted = true;
    }

    public static boolean hasListExecuted() {
        return listExecuted;
    }
}

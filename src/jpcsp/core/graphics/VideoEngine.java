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
package jpcsp.core.graphics;

import javax.media.opengl.GL;
import static jpcsp.core.graphics.GeCommands.*;

public class VideoEngine {

    private boolean fullscreen;
    private int mode;
    private int linesize;
    private int pixelsize;
    private int width;
    private int height;
    private boolean ha;
    private static VideoEngine instance;
    private GL drawable;
    private static final boolean isDebugMode = true;
    private static GeCommands helper;

    private static void log(String msg) {
        System.out.println("VIDEO DEBUG > " + msg);
    }

    public static VideoEngine getEngine(GL draw, boolean fullScreen, boolean hardwareAccelerate) {
        VideoEngine engine = getEngine();
        engine.setFullScreenShoot(fullScreen);
        engine.setHardwareAcc(hardwareAccelerate);
        engine.drawable = draw;
        return engine;
    }

    private static VideoEngine getEngine() {
        if (instance == null) {
            instance = new VideoEngine();
            helper = new GeCommands();
        }
        return instance;
    }

    private VideoEngine() {
    }
    private boolean listIsOver = true;
    private DisplayList actualList;

    public void executeList(DisplayList list) {
        actualList = list;
        while (!listIsOver) {
            executeCommand(list.pointer);
        }
    }

    private byte command(int word) {
        return (byte)(word >>> 24);
    }
    
    private int intArgument(int word) {
        return (word & 0x00FFFFFF);
    }
    
    private float floatArgument(int word) {
        return Float.intBitsToFloat(word << 8);
    }
    
    public void executeCommand(int word) {
        // the conversion to float argument by psp, lose 8bits
        int clearFlags;

        int normalArgument = intArgument(word);
        
        switch (command(word)) {
            case BASE:
                actualList.base = normalArgument;
                if (isDebugMode)log(helper.getCommandString(BASE) + " " + normalArgument);
                
                break;
            case IADDR:
                actualList.stackIndex = actualList.base | normalArgument;
                break;
            case PRIM:
            case SHADE:
                int SHADE_MODEL_SELECTED = normalArgument << 23; //bit 0
                SHADE_MODEL_SELECTED = (SHADE_MODEL_SELECTED == 0)?SHADE_TYPE_SMOOTH:SHADE_TYPE_FLAT ;
                drawable.glShadeModel(SHADE_MODEL_SELECTED);
                if (isDebugMode)log(helper.getCommandString(SHADE) + " " + SHADE_MODEL_SELECTED );
                
            case NOP:
                if (isDebugMode)log(helper.getCommandString(NOP));
                break;
            default:
                log("Unknow/unimplemented video command [ " + command(word) + " ]");
        }

    }

    public void setFullScreenShoot(boolean b) {
    }

    public void setLineSize(int linesize) {
    }

    public void setMode(int mode) {
    }

    public void setPixelSize(int pixelsize) {
    }

    public void setup(int mode, int xres, int yres) {
    }

    public void show() {
    }

    public void waitVBlank() {
    }

    private void setHardwareAcc(boolean hardwareAccelerate) {
    }
}

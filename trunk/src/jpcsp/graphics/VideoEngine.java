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
package jpcsp.graphics;

import javax.media.opengl.GL;
import jpcsp.Emulator;
import static jpcsp.graphics.GeCommands.*;

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
    private Vertex vertex = new Vertex();
    private static final char SPACE = ' ';

    private static void log(String msg) {
        if (isDebugMode) {
            System.out.println("sceGe DEBUG > " + msg);
        }
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
    private boolean listIsOver = false;
    private DisplayList actualList;

    public void executeList(DisplayList list) {
        actualList = list;
        
        while (!listIsOver) {
            try {
                executeCommand(list.pointer);
            } catch (Exception e) {
                log(e.toString());
            }

             if (actualList.start == actualList.stallAddress) {
                listIsOver = true;
                continue;
            }
            actualList.start++; //is it correct?
            actualList.pointer = Emulator.getMemory().read32(actualList.start);
        }
        listIsOver = false;
    }

    //I guess here we use ubyte
    private int command(int word) {
        return (word >>> 24);
    }

    private int intArgument(int word) {
        return (word & 0x00FFFFFF);
    }

    private float floatArgument(int word) {
        return Float.intBitsToFloat(word << 8);
    }

    public void executeCommand(int word) {
        int normalArgument = intArgument(word);
        float floatArgument = floatArgument(word);

        switch (command(word)) {
            case END:
                listIsOver = true;
                log(helper.getCommandString(END));
                break;
            case FINISH:
                listIsOver = true;
                log(helper.getCommandString(FINISH));
                break;
            case BASE:
                actualList.base = normalArgument;
                log(helper.getCommandString(BASE),normalArgument);
                break;
            case IADDR:
                vertex.index = actualList.base | normalArgument;
                log(helper.getCommandString(IADDR),vertex.index);
                break;
            case VADDR:
                vertex.pointer = actualList.base | normalArgument;
                log(helper.getCommandString(VADDR), vertex.pointer);
                break;
            case XPOS:
                log(helper.getCommandString(XPOS), floatArgument);
                break;
            case YPOS:
                log(helper.getCommandString(YPOS), floatArgument);
                break;
            case ZPOS:
                log(helper.getCommandString(ZPOS), floatArgument);
                break;
            case FBP:
                log(helper.getCommandString(FBP), normalArgument);
                break;
            case PRIM:
                int numberOfVertex = normalArgument & 0xFFFF;
                int draw = ((normalArgument >> 16) & 0x7);                
                switch (draw){
                    case PRIM_POINT:
                        log(helper.getCommandString(PRIM) + " point");
                        break;
                    case PRIM_LINES_STRIPS:
                        log(helper.getCommandString(PRIM) + " lines_strips");
                        break;
                    case PRIM_LINE:
                        log(helper.getCommandString(PRIM) + " line");
                        break;
                    case PRIM_SPRITES:
                        log(helper.getCommandString(PRIM) + " sprites");
                        break;
                    case PRIM_TRIANGLE:
                        log(helper.getCommandString(PRIM) + " triangle");
                        break;
                    case PRIM_TRIANGLE_FANS:
                        log(helper.getCommandString(PRIM) + " triangle_fans");
                        break;
                    case PRIM_TRIANGLE_STRIPS:
                        log(helper.getCommandString(PRIM) + " triangle_strips");
                        break;
                }
                break;
            case SHADE:
                int SETTED_MODEL = normalArgument | 0x01; //bit 0
                SETTED_MODEL = (SETTED_MODEL == 0x01) ? SHADE_TYPE_SMOOTH : SHADE_TYPE_FLAT;
                //drawable.glShadeModel(SETTED_MODEL);
                log(helper.getCommandString(SHADE) + " " + ((SETTED_MODEL==0x01) ? "smooth" : "flat"));
                break;
            case JUMP:
                actualList.pointer =  Emulator.getMemory().read32((normalArgument | actualList.base) & 0xFFFFFFFC);
                //I guess it must be unsign as psp player emulator
                log(helper.getCommandString(JUMP),actualList.pointer);
                break;
            case CALL:
                actualList.stack[actualList.stackIndex++] = actualList.pointer;
                actualList.pointer = Emulator.getMemory().read32((normalArgument | actualList.base) & 0xFFFFFFFC);
                log(helper.getCommandString(CALL),actualList.pointer);
                break;
            case RET:
                actualList.pointer = actualList.stack[--actualList.stackIndex];
                log(helper.getCommandString(RET),actualList.pointer);
                break;
            case NOP:
                log(helper.getCommandString(NOP));
                break;
            default:
                log("Unknow/unimplemented video command [ " + helper.getCommandString(command(word)) + " ]");
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

    private void log(String commandString, float floatArgument) {
        log(commandString+SPACE+floatArgument);
    }

    private void log(String commandString, int pointer) {
        log(commandString+SPACE+pointer);
    }

    private void setHardwareAcc(boolean hardwareAccelerate) {
    }
}

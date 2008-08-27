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

    // TODO these currently here for testing only
    private int fbp, fbw; // frame buffer pointer and width
    private int zbp, zbw; // depth buffer pointer and width
    private int psm; // pixel format

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
        listIsOver = false;

        while (!listIsOver &&
            actualList.pc != actualList.stallAddress &&
            // TODO maybe remove this line when all is done, because if we exit the loop early we either need to completely stop emulation or have some way of restarting the list
            !Emulator.pause) {
            int ins = Emulator.getMemory().read32(actualList.pc);
            actualList.pc += 4;
            executeCommand(ins);
        }

        if (actualList.pc == actualList.stallAddress) {
            actualList.status = DisplayList.STALL_REACHED;
            log("list " + actualList.id + " stalled at " + String.format("%08x", actualList.stallAddress));
        }

        if (listIsOver) {
            // TODO remove list cleanly :P
            actualList.status = DisplayList.DRAWING_DONE; // DONE, DRAWING_DONE, CANCEL_DONE ?
            jpcsp.HLE.pspge.get_instance().sceGeListDeQueue(actualList.id);
        }
    }

    //I guess here we use ubyte
    private int command(int instruction) {
        return (instruction >>> 24);
    }

    private int intArgument(int instruction) {
        return (instruction & 0x00FFFFFF);
    }

    private float floatArgument(int instruction) {
        return Float.intBitsToFloat(instruction << 8);
    }

    public void executeCommand(int instruction) {
        int normalArgument = intArgument(instruction);
        float floatArgument = floatArgument(instruction);

        switch (command(instruction)) {
            case END:
                listIsOver = true;
                log(helper.getCommandString(END));
                break;
            case FINISH:
                listIsOver = true;
                log(helper.getCommandString(FINISH));
                break;
            case BASE:
                actualList.base = normalArgument << 8;
                log(helper.getCommandString(BASE) + " " + String.format("%08x", actualList.base));
                break;
            case IADDR:
                vertex.index = actualList.base | normalArgument;
                log(helper.getCommandString(IADDR),vertex.index);
                break;
            case VADDR:
                vertex.pointer = actualList.base | normalArgument;
                log(helper.getCommandString(VADDR) + " " + String.format("%08x", vertex.pointer));
                break;

            case TME:
                if (normalArgument != 0)
                    log("sceGuEnable(GU_TEXTURE_2D)");
                else
                    log("sceGuDisable(GU_TEXTURE_2D)");
                break;

            case XSCALE:
                log("sceGuViewport width = " + (floatArgument * 2));
                break;
            case YSCALE:
                log("sceGuViewport height = " + (- floatArgument * 2));
                break;

            // sceGuViewport cx/cy, can we discard these settings? it's only for clipping?
            case XPOS:
                log("sceGuViewport cx = " + floatArgument);
                break;
            case YPOS:
                log("sceGuViewport cy = " + floatArgument);
                break;

            case ZPOS:
                log(helper.getCommandString(ZPOS), floatArgument);
                break;

            // sceGuOffset, can we discard these settings? it's only for clipping?
            case OFFSETX:
                log("sceGuOffset x = " + (normalArgument >> 4));
                break;
            case OFFSETY:
                log("sceGuOffset y = " + (normalArgument >> 4));
                break;

            case FBP:
                // assign or OR lower 24-bits?
                fbp = normalArgument;
                break;
            case FBW:
                fbp &= 0xffffff;
                fbp |= (normalArgument << 8) & 0xff000000;
                fbw = (normalArgument) & 0xffff;
                log("fbp=" + Integer.toHexString(fbp) + ", fbw=" + fbw);
                break;

            case ZBP:
                // assign or OR lower 24-bits?
                zbp = normalArgument;
                break;
            case ZBW:
                zbp &= 0xffffff;
                zbp |= (normalArgument << 8) & 0xff000000;
                zbw = (normalArgument) & 0xffff;
                log("zbp=" + Integer.toHexString(zbp) + ", zbw=" + zbw);
                break;

            case PSM:
                psm = normalArgument;
                log("psm=" + normalArgument);
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
                int npc = (normalArgument | actualList.base) & 0xFFFFFFFC;
                //I guess it must be unsign as psp player emulator
                log(helper.getCommandString(JUMP) + " old PC:" + String.format("%08x", actualList.pc)
                    + " new PC:" + String.format("%08x", npc));
                actualList.pc = npc;
                break;
            case CALL:
                actualList.stack[actualList.stackIndex++] = actualList.pc + 4;
                actualList.pc = (normalArgument | actualList.base) & 0xFFFFFFFC;
                log(helper.getCommandString(CALL), actualList.pc);
                break;
            case RET:
                actualList.pc = actualList.stack[--actualList.stackIndex];
                log(helper.getCommandString(RET), actualList.pc);
                break;

            case NOP:
                log(helper.getCommandString(NOP));
                break;

            default:
                log("Unknown/unimplemented video command [ " + helper.getCommandString(command(instruction)) + " ]");
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

    private void log(String commandString, int value) {
        log(commandString+SPACE+value);
    }

    private void setHardwareAcc(boolean hardwareAccelerate) {
    }
}

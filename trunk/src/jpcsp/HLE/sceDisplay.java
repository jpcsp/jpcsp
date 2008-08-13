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
//based on Firmware 1.50
package jpcsp.HLE;


import jpcsp.Emulator;

/**
 *
 * @author shadow
 */
public class sceDisplay {
    private static sceDisplay instance;
    public static sceDisplay get_instance() {
        if (instance == null) {
            instance = new sceDisplay();
        }
        return instance;
    }
    public void sceDisplaySetMode(int mode, int xres, int  yres)/*0x213a*/
    {
        System.out.println("sceDisplaySetMode: mode= " +mode + " xres= "+ xres + " yres= "+yres);
        /* pseudo opengl code */
        /*glClearColor(0,0,0,0); //clear screen
	glClear(GL_DEPTH_BUFFER_BIT); //clear depthbuffer
        glColor4f(1,1,1,1);//set color to black
        glEnable(GL_TEXTURE_2D);//enable 2D */
        Emulator.getProcessor().gpr[2] = 0;
    }
    public void sceDisplaySetFrameBuf(int topaddr, int linesize, int pixelsize, int mode)/*0x213f*/
    {
        //all the trick should be done here about rendering the framebuffer.
        System.out.println("sceDisplaySetFrameBuf: topaddr = 0x" + Integer.toHexString(topaddr) + " linesize = " + linesize + " pixelsize = " + pixelsize + " mode= " + mode);
        //read memory based on topaddress that is the framebuffer and then display it
        //seems simple err maybe not :P
    }
    public void sceDisplayWaitVblankStart()/*0x2147*/
    {
        System.out.println("sceDisplayWaitVblankStart");
    }
    
}

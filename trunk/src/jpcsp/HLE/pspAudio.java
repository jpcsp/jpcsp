/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__Audio.html


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
package jpcsp.HLE;

import jpcsp.Emulator;

/**
 *
 * @author shadow
 */
public class pspAudio {
    public int[] pspchannels; // psp channels
    private static pspAudio instance;
    public static pspAudio get_instance() {
        if (instance == null) {
            instance = new pspAudio();
        }
        return instance;
    }
    public pspAudio()
    {
      pspchannels = new int[8];
      
    }
    public void sceAudioChReserve(int a0 , int a1 ,int a2)
    {
        System.out.println("(Unimplement)sceAudioChReserve channel= " + a0 + " samplecount = " + a1 + " format = " + a2);
        if(a0 == -1) //PSP_AUDIO_NEXT_CHANNEL 
        {
             //get next available channel
        }
        Emulator.getProcessor().gpr[2] = 0; //just return the first channel
    }
    
}

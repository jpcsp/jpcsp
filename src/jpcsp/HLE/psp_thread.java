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
package jpcsp.HLE;

import jpcsp.Processor;

public class psp_thread {

	/* This is just the very first idea of a psp_thread class
	 * The idea is to spawn a thread for main(), set its PC
	 * Then cycle through and spawn further threads as required
	 * The first thing to do here is make sure to handle CPU/regs
	 * correctly and then sort out the cpu_tick() - at the moment
	 * we have a single PC stepping linearly through the program
	 * Also, must handle the stack correctly.
	 */
	
    static psp_thread _current; //current thread
    static psp_thread[] list; //list of threads
    
    public Processor cpu;
    public int regs[] = new int[32];//32 base registers
    public int pc; //program counter
    public int hi, lo;
    public int callback;
    Boolean sleeping = true;
    
    public psp_thread(int threadID,int cback,int stacksize,Processor c,int r[],int progC) { //FIXME - these should all be uints (?) I'll figure out how to handle properly later
    	this.cpu = c; //set the cpu
    	for (int i=0; i<32; i++)
    		regs[i]=r[i]; //set up the registers
    	this.pc=progC; //this is the program counter
    	this.callback=cback;
    }
    
    public void start() //creation is separate to starting
    {
    }
    
    public void close()
    {
    }
    
    public void next() //skip on to following thread
    {
    }
}
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
package jpcsp;

import java.io.IOException;
import jpcsp.format.Elf32;
import jpcsp.format.PBP;

public class Emulator {

    private static Processor cpu;
    private FileManager romManager;

    public Emulator() {
        cpu = new Processor();
    }

    public void load(String rom) throws IOException {
        // TODO: here will load rom, iso or etc...        
        getProcessor().reset(); //
        ElfHeader.readHeader(rom, getProcessor());
        // load after implemented : move the content from futureLoad() to load()

    }

    

    private void futureLoad() throws IOException{
        String rom ="path";

        initNewPsp();
        romManager = new FileManager(rom, getProcessor()); //here cpu already reset

        switch (romManager.getType()) {
            case FileManager.FORMAT_ELF:
                init(romManager.getElf32()); // init RAM, CPU, GPU... the only one working by now!?!
                break;
            case FileManager.FORMAT_ISO:
                break;
            case FileManager.FORMAT_PBP:
                init(romManager.getPBP());
                break;
            case FileManager.FORMAT_UMD:
                break;
            default:
                throw new IOException("Is not an acceptable format, please choose the rigth file.");
        }
    }

    //elf32 init
    private void init(Elf32 elf32) {
        initRamByElf32();
        initCpuByElf32(); 
    }

    private void init(PBP pBP) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    
    private void initRamByElf32() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    private void initCpuByElf32() {
        //set the default values for registers not sure if they are correct and UNTESTED!!
        // from soywiz/pspemulator
        cpu.pc = (int) romManager.getBaseoffset() + (int) romManager.getElf32().getHeader().getE_entry(); //set the pc register.
        cpu.cpuregisters[31] = 0x08000004; //ra, should this be 0?
        cpu.cpuregisters[5] = (int) romManager.getBaseoffset() + (int) romManager.getElf32().getHeader().getE_entry(); // argumentsPointer a1 reg
        cpu.cpuregisters[28] = (int) romManager.getBaseoffset() + (int) romManager.getPSPModuleInfo().getM_gp(); //gp reg    gp register should get the GlobalPointer!!!
        cpu.cpuregisters[29] = 0x09F00000; //sp
        cpu.cpuregisters[26] = 0x09F00000; //k0
    }

    private void initNewPsp() {
        getProcessor().reset();
        Memory.get_instance().NullMemory();
    }

    public void run() throws GeneralJpcspException {
        // for while nothing...
    }

    public void pause() {
    }

    public void resume() {
    }

    public void stop() {
    }

    public static Processor getProcessor() {
        return cpu;
    }

    public static Memory getMemory(){
        return Memory.get_instance();
    }


}

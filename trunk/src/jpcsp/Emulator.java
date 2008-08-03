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

public class Emulator {

    private Processor cpu;
    private FileManager romManager;

    public Emulator() {
        cpu = new Processor();
    }

    public void load(String rom) throws IOException {
        // TODO: here will load rom, iso or etc...

        getProcessor().reset(); //
        ElfHeader.readHeader(rom, getProcessor());
        // load after implemented futureLoad()
    }

    private void futureLoad() throws IOException{
        String rom ="path";

        initNewPsp();
        romManager = new FileManager(rom, getProcessor()); //here cpu already reset

        switch (romManager.getType()) {
            case FileManager.FORMAT_ELF:
                break;
            case FileManager.FORMAT_ISO:
                break;
            case FileManager.FORMAT_PBP:
                break;
            case FileManager.FORMAT_UMD:
                break;
            default:
                throw new IOException("Is not an acceptable format, please choose the rigth file.");
        }
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

    public Processor getProcessor() {
        return cpu;
    }

    public Memory getMemory(){
        return Memory.get_instance();
    }
}

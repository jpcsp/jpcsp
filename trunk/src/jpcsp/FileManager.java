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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import jpcsp.format.PBP;

public class FileManager {
    private Processor cpu;
    private String filePath;
    
    
    public final static int ELF = 0;
    public final static int PBP = 10;
    public final static int UMD = 20;
    public final static int ISO = 30;
    
    public FileManager(String filePath,Processor cpu){
        this.filePath = filePath;
        this.cpu = cpu;
        this.cpu.reset();
        //loadAndDefine(filePath);
    }

    public int getType() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    
    private boolean isELF(){
        return false;
    }
    
    private boolean isPBP(){
        return false;
    }

    private void loadAndDefine(String filePath) throws FileNotFoundException, IOException {
        RandomAccessFile f = new RandomAccessFile (filePath, "r");
        long elfoffset = 0;
        long baseoffset = 0;
        boolean relocate = false;
        PBP pbp = new PBP(f);
        
    /** Read pbp **/
   /* PBP_Header pbp = new PBP_Header();
    pbp.read(f);
    if(Long.toHexString(pbp.p_magic & 0xFFFFFFFFL).equals("50425000"))//file is pbp 50425000 == 0x3016CA8??? the comparison is made by hexa
        // TODO : check the documentation ...
    {
        elfoffset = pbp.offset_psp_data;
        f.seek(pbp.offset_psp_data); //seek the new offset!
        PbpInfo = pbp.toString();
    }
    else
    {
        elfoffset = 0;
        f.seek(0); // start read from start file is not pbp check if it an elf;
        PbpInfo = "-----NOT A PBP FILE---------\n";
    }*/
    }
}

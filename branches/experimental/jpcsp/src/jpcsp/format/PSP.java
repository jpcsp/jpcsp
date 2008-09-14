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

package jpcsp.format;

import java.io.IOException;
import java.nio.ByteBuffer;
import jpcsp.filesystems.*;
import static jpcsp.util.Utilities.*;
/**
 *
 * @author shadow
 */
public class PSP {  /* format ~PSP */
    private long e_magic; 
    
    private void read(ByteBuffer f) throws IOException {
        e_magic = readUWord(f);
    }
    public PSP(ByteBuffer f) throws IOException {
        read(f);
    }

     public boolean isValid(){
        return (Long.toHexString( e_magic & 0xFFFFFFFFL).toUpperCase().equals("5053507E"));//~PSP
     }
}

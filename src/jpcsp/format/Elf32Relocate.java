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

import static jpcsp.util.Utilities.readUWord;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Elf32Relocate {
    private int r_offset;
    private int r_info;

    public static int sizeof() {
        return 8;
    }

    public void read(ByteBuffer f) throws IOException {
        setR_offset(readUWord(f));
        setR_info(readUWord(f));
    }

    @Override
	public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(String.format("r_offset \t 0x%08X\n", getR_offset()));
        str.append(String.format("r_info \t\t 0x%08X\n", getR_info()));
        return str.toString();
    }

    public int getR_offset() {
        return r_offset;
    }

    public void setR_offset(int r_offset) {
        this.r_offset = r_offset;
    }

    public int getR_info() {
        return r_info;
    }

    public void setR_info(int r_info) {
        this.r_info = r_info;
    }
}

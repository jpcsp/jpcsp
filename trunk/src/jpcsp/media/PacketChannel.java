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

package jpcsp.media;

import java.io.File;
import java.io.RandomAccessFile;

import jpcsp.Memory;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.IMemoryReader;

/*
 * Common interface for PSMF/MPEG -> Media Engine communication.
 *
 * Currently, it's working with temporary RandomAccessFiles.
 * TODO: Make this class implement ByteChannel and operate
 * only internally.
 *
 */
public class PacketChannel {
    private RandomAccessFile pcRaf;
    private String pcRafPath;

    public PacketChannel() {
        pcRaf = null;
        pcRafPath = "tmp/TMP.PMF";
    }

    public void writePacket(int address, int length) {
        if (length > 0 && Memory.getInstance().isAddressGood(address)) {

            try {
                if(pcRaf == null)
                    pcRaf = new RandomAccessFile(pcRafPath, "rw");

                byte[] buffer = new byte[length];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
                for (int i = 0; i < length; i++) {
                    buffer[i] = (byte)memoryReader.readNext();
                }

                pcRaf.write(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void writeFile(byte[] buffer) {
        try {
            if(pcRaf == null)
                pcRaf = new RandomAccessFile(pcRafPath, "rw");

            pcRaf.write(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            pcRaf.close();
            new File(pcRafPath).delete();
            pcRaf = null;
        } catch (Exception e) {
            // Ignore.
        }
    }

    public String getFilePath() {
        return pcRafPath;
    }
}
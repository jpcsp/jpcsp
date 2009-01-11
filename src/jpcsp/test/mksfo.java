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
package jpcsp.test;

import jpcsp.format.PSF;

import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.IOException;


import java.io.RandomAccessFile;
//import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class mksfo {
    public static void write(String filename) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(0x10000);
        PSF psf = new PSF(0);

        psf.put("DISC_VERSION", "1.00");
        psf.put("DISC_ID", "UCJS10041");
        psf.put("CATEGORY", "UG");
        psf.put("BOOTABLE", 1);
        psf.put("DISC_TOTAL", 1);
        psf.put("DISC_NUMBER", 1);
        psf.put("REGION", 32768);
        psf.put("PSP_SYSTEM_VER", "1.00");
        psf.put("PARENTAL_LEVEL", 0);
        psf.put("TITLE", "SFO Test");

        psf.write(buffer);
        byte[] data = new byte[(int)psf.size()];
        buffer.position(0);
        buffer.get(data);

        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.close();
    }

    public static void read(String filename) throws IOException {
        PSF psf = new PSF(0);

        RandomAccessFile raf = new RandomAccessFile(filename, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
        psf.read(buffer);
        channel.close();
        raf.close();

        System.out.println(psf);
    }

    public static void main(String[] args) throws IOException {
        write("PARAM.SFO");
        read("PARAM.SFO");
    }
}

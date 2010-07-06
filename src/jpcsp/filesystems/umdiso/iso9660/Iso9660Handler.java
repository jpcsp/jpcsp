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
package jpcsp.filesystems.umdiso.iso9660;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import jpcsp.filesystems.umdiso.UmdIsoReader;

/**
 *
 * @author gigaherz
 */
public class Iso9660Handler extends Iso9660Directory {

    private Iso9660Directory internalDir;

    public Iso9660Handler(UmdIsoReader r) throws IOException
    {
        super(r, 0, 0);

        byte[] sector = r.readSector(16);
        ByteArrayInputStream byteStream = new ByteArrayInputStream(sector);

        byteStream.skip(157); // reach rootDirTocHeader

        byte[] b = new byte[38];

        byteStream.read(b);
        Iso9660File rootDirEntry = new Iso9660File(b,b.length);

        int rootLBA = rootDirEntry.getLBA();
        int rootSize = rootDirEntry.getSize();

        internalDir = new Iso9660Directory(r, rootLBA, rootSize);
    }

    @Override
    public Iso9660File getEntryByIndex(int index) throws ArrayIndexOutOfBoundsException
    {
        return internalDir.getEntryByIndex(index);
    }

    @Override
    public int getFileIndex(String fileName) throws FileNotFoundException
    {
        return internalDir.getFileIndex(fileName);
    }

    @Override
    public String[] getFileList() throws FileNotFoundException
    {
        return internalDir.getFileList();
    }
}
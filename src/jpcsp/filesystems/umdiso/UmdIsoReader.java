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
package jpcsp.filesystems.umdiso;

import static jpcsp.filesystems.umdiso.UmdIsoFile.sectorLength;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.filesystems.umdiso.iso9660.Iso9660Directory;
import jpcsp.filesystems.umdiso.iso9660.Iso9660File;
import jpcsp.filesystems.umdiso.iso9660.Iso9660Handler;
import jpcsp.util.Utilities;

/**
 *
 * @author gigaherz, gid15
 */
public class UmdIsoReader {

    public static final int startSector = 16;
    private static final int headerLength = 24;
    private final ISectorDevice sectorDevice;
    private final HashMap<String, Iso9660File> fileCache = new HashMap<String, Iso9660File>();
    private final HashMap<String, Iso9660Directory> dirCache = new HashMap<String, Iso9660Directory>();
    private final int numSectors;

    public UmdIsoReader(String umdFilename) throws IOException, FileNotFoundException {
        RandomAccessFile fileReader = new RandomAccessFile(umdFilename, "r");

        byte[] header = new byte[headerLength];
        fileReader.seek(0);
        fileReader.read(header);

        if (header[0] == 'C' && header[1] == 'I' && header[2] == 'S' && header[3] == 'O') {
            sectorDevice = new CSOFileSectorDevice(fileReader, header);
        } else if (header[0] == 0 && header[1] == 'P' && header[2] == 'B' && header[3] == 'P') {
        	sectorDevice = new PBPFileSectorDevice(fileReader);
        } else {
            sectorDevice = new ISOFileSectorDevice(fileReader);
        }
        numSectors = sectorDevice.getNumSectors();

        if (!hasIsoHeader()) {
            throw new IOException(String.format("Unsupported file format or corrupted file '%s'.", umdFilename));
        }
    }

    public UmdIsoReader(ISectorDevice sectorDevice) throws IOException {
        this.sectorDevice = sectorDevice;
        numSectors = sectorDevice.getNumSectors();
    }

    public void close() throws IOException {
        sectorDevice.close();
    }

    private boolean hasIsoHeader() throws IOException {
        if (numSectors <= 0) {
            return false;
        }

        UmdIsoFile f = new UmdIsoFile(this, startSector, sectorLength, null, null);
        byte[] header = new byte[6];
        int length = f.read(header);
        f.close();
        if (length < header.length) {
            return false;
        }

        if (header[1] != 'C' || header[2] != 'D' || header[3] != '0' || header[4] != '0' || header[5] != '1') {
            return false;
        }

        return true;
    }

    public int getNumSectors() {
        return numSectors;
    }

    /**
     * Read sequential sectors into a byte array
     *
     * @param sectorNumber - the first sector to be read
     * @param numberSectors - the number of sectors to be read
     * @param buffer - the byte array where to write the sectors
     * @param offset - offset into the byte array where to start writing
     * @return the number of sectors read
     * @throws IOException
     */
    public int readSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException {
        if (sectorNumber < 0 || (sectorNumber + numberSectors) > numSectors) {
            Arrays.fill(buffer, offset, offset + numberSectors * sectorLength, (byte) 0);
            Emulator.log.warn(String.format("Sectors start=%d, end=%d out of ISO (numSectors=%d)", sectorNumber, sectorNumber + numberSectors, numSectors));
            return numberSectors;
        }

        return sectorDevice.readSectors(sectorNumber, numberSectors, buffer, offset);
    }

    /**
     * Read one sector into a byte array
     *
     * @param sectorNumber - the sector number to be read
     * @param buffer - the byte array where to write
     * @param offset - offset into the byte array where to start writing
     * @throws IOException
     */
    public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
        if (sectorNumber < 0 || sectorNumber >= numSectors) {
            Arrays.fill(buffer, offset, offset + sectorLength, (byte) 0);
            Emulator.log.warn(String.format("Sector number %d out of ISO (numSectors=%d)", sectorNumber, numSectors));
            return;
        }

        sectorDevice.readSector(sectorNumber, buffer, offset);
    }

    /**
     * Read one sector
     *
     * @param sectorNumber - the sector number to be read
     * @return a new byte array of size sectorLength containing the sector
     * @throws IOException
     */
    public byte[] readSector(int sectorNumber) throws IOException {
        return readSector(sectorNumber, null);
    }

    /**
     * Read one sector
     *
     * @param sectorNumber - the sector number to be read
     * @param buffer - try to reuse this buffer if possible
     * @return a new byte array of size sectorLength containing the sector or
     * the buffer if it could be reused.
     * @throws IOException
     */
    public byte[] readSector(int sectorNumber, byte[] buffer) throws IOException {
        if (buffer == null || buffer.length != sectorLength) {
            buffer = new byte[sectorLength];
        }
        readSector(sectorNumber, buffer, 0);

        return buffer;
    }

    private int removePath(String[] path, int index, int length) {
        if (index < 0 || index >= length) {
            return length;
        }

        for (int i = index + 1; i < length; i++) {
            path[i - 1] = path[i];
        }

        return length - 1;
    }

    private Iso9660File getFileEntry(String filePath) throws IOException, FileNotFoundException {
        Iso9660File info;

        info = fileCache.get(filePath);
        if (info != null) {
            return info;
        }

        int parentDirectoryIndex = filePath.lastIndexOf('/');
        if (parentDirectoryIndex >= 0) {
            String parentDirectory = filePath.substring(0, parentDirectoryIndex);
            Iso9660Directory dir = dirCache.get(parentDirectory);
            if (dir != null) {
                int index = dir.getFileIndex(filePath.substring(parentDirectoryIndex + 1));
                info = dir.getEntryByIndex(index);
                if (info != null) {
                    fileCache.put(filePath, info);
                    return info;
                }
            }
        }

        Iso9660Directory dir = new Iso9660Handler(this);

        String[] path = filePath.split("[\\/]");

        // First convert the path to a canonical path by removing all the
        // occurrences of "." and "..".
        int pathLength = path.length;
        for (int i = 0; i < pathLength;) {
            if (path[i].equals(".")) {
                // Remove "."
                pathLength = removePath(path, i, pathLength);
            } else if (path[i].equals("..")) {
                // Remove ".." and its parent
                pathLength = removePath(path, i, pathLength);
                pathLength = removePath(path, i - 1, pathLength);
            } else {
                i++;
            }
        }

        // walk through the canonical path
        for (int i = 0; i < pathLength;) {
            int index = dir.getFileIndex(path[i]);

            info = dir.getEntryByIndex(index);

            if (isDirectory(info)) {
                dir = new Iso9660Directory(this, info.getLBA(), info.getSize());
                StringBuilder dirPath = new StringBuilder(path[0]);
                for (int j = 1; j <= i; j++) {
                    dirPath.append("/").append(path[j]);
                }
                dirCache.put(dirPath.toString(), dir);
            }
            i++;
        }

        if (info != null) {
            fileCache.put(filePath, info);
        }

        return info;
    }

    public UmdIsoFile getFile(String filePath) throws IOException, FileNotFoundException {
        int fileStart;
        long fileLength;
        Date timestamp;
        String fileName = null;

        if (filePath != null && filePath.startsWith("sce_lbn")) {
            //
            // Direct sector access on UMD is using the following file name syntax:
            //     sce_lbnSSSS_sizeLLLL
            // where SSSS is the index of the first sector (in base 16)
            //       LLLL is the length in bytes (in base 16)
            // E.g.
            //       disc0:/sce_lbn0x5fa0_size0x1428
            //       disc0:/sce_lbn7050_sizeee850
            //
            filePath = filePath.substring(7);
            int sep = filePath.indexOf("_size");
            fileStart = (int) Utilities.parseHexLong(filePath.substring(0, sep));
            fileLength = Utilities.parseHexLong(filePath.substring(sep + 5));
            timestamp = new Date();
            fileName = null;
            if (fileStart < 0 || fileStart >= numSectors) {
                throw new IOException("File '" + filePath + "': Invalid Start Sector");
            }
        } else if (filePath != null && filePath.length() == 0) {
            fileStart = 0;
            fileLength = numSectors * sectorLength;
            timestamp = new Date();
        } else {
            Iso9660File info = getFileEntry(filePath);
            if (info != null && isDirectory(info)) {
                info = null;
            }

            if (info == null) {
                throw new FileNotFoundException("File '" + filePath + "' not found or not a file.");
            }

            fileStart = info.getLBA();
            fileLength = info.getSize();
            timestamp = info.getTimestamp();
            fileName = info.getFileName();
        }

        return new UmdIsoFile(this, fileStart, fileLength, timestamp, fileName);
    }

    public String resolveSectorPath(int start, long length) {
        String fileName = null;
        // Scroll back through the sectors until the file's start sector is reached
        // and it's name can be obtained.
        while ((fileName == null) || (start <= startSector)) {
            fileName = getFileName(start);
            start--;
        }
        return fileName;
    }

    public String[] listDirectory(String filePath) throws IOException, FileNotFoundException {
        Iso9660Directory dir = null;

        if (filePath.length() == 0) {
            dir = new Iso9660Handler(this);
        } else {
            Iso9660File info = getFileEntry(filePath);
            if (info != null && isDirectory(info)) {
                dir = new Iso9660Directory(this, info.getLBA(), info.getSize());
            }
        }

        if (dir == null) {
            throw new FileNotFoundException("File '" + filePath + "' not found or not a directory.");
        }

        return dir.getFileList();
    }

    public int getFileProperties(String filePath) throws IOException, FileNotFoundException {
        if (filePath.length() == 0) {
            return 2;
        }

        Iso9660File info = getFileEntry(filePath);

        if (info == null) {
            throw new FileNotFoundException("File '" + filePath + "' not found.");
        }

        return info.getProperties();
    }

    public boolean isDirectory(String filePath) throws IOException, FileNotFoundException {
        return ((getFileProperties(filePath) & 2) == 2);
    }

    public boolean isDirectory(Iso9660File file) {
        return (file.getProperties() & 2) == 2;
    }

    private String getFileNameRecursive(int fileStartSector, String path, String[] files) throws FileNotFoundException, IOException {
        for (String file : files) {
            String filePath = path + "/" + file;
            Iso9660File info = null;
            if (path.length() == 0) {
                filePath = file;
            } else {
                info = getFileEntry(filePath);
                if (info != null) {
                    if (info.getLBA() == fileStartSector) {
                        return info.getFileName();
                    }
                }
            }

            if ((info == null || isDirectory(info)) && !file.equals(".") && !file.equals("\01")) {
                try {
                    String[] childFiles = listDirectory(filePath);
                    String fileName = getFileNameRecursive(fileStartSector, filePath, childFiles);
                    if (fileName != null) {
                        return fileName;
                    }
                } catch (FileNotFoundException e) {
                    // Continue
                }
            }
        }

        return null;
    }

    public String getFileName(int fileStartSector) {
        try {
            String[] files = listDirectory("");
            return getFileNameRecursive(fileStartSector, "", files);
        } catch (FileNotFoundException e) {
            // Ignore Exception
        } catch (IOException e) {
            // Ignore Exception
        }

        return null;
    }

    public long dumpIndexRecursive(PrintWriter out, String path, String[] files) throws IOException {
        long size = 0;
        for (String file : files) {
            String filePath = path + "/" + file;
            Iso9660File info;
            int fileStart = 0;
            long fileLength = 0;

            if (path.length() == 0) {
                filePath = file;
            }

            info = getFileEntry(filePath);
            if (info != null) {
                fileStart = info.getLBA();
                fileLength = info.getSize();
                size += (fileLength + 0x7FF) & ~0x7FF;
            }

            // "." isn't a directory (throws an exception)
            // "\01" claims to be a directory but ends up in an infinite loop
            // ignore them here as they do not contribute much to the listing
            if (file.equals(".") || file.equals("\01")) {
                continue;
            }

            if (info == null || isDirectory(info)) {
                out.println(String.format("D %08X %10d %s", fileStart, fileLength, filePath));
                String[] childFiles = listDirectory(filePath);
                size += dumpIndexRecursive(out, filePath, childFiles);
            } else {
                out.println(String.format("  %08X %10d %s", fileStart, fileLength, filePath));
            }
        }
        return size;
    }

    public void dumpIndexFile(String filename) throws IOException, FileNotFoundException {
        PrintWriter out = new PrintWriter(new FileOutputStream(filename));
        out.println("  Start    Size       Name");
        String[] files = listDirectory("");
        long size = dumpIndexRecursive(out, "", files);
        out.println(String.format("Total Size %10d", size));
        out.println(String.format("Image Size %10d", numSectors * sectorLength));
        out.println(String.format("Missing    %10d (%d sectors)", (numSectors * sectorLength) - size, numSectors - (size / sectorLength)));
        out.close();
    }
}

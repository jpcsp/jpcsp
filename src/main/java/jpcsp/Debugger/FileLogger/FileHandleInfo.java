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
package jpcsp.Debugger.FileLogger;

/**
 *
 * @author fiveofhearts
 */
public class FileHandleInfo implements Comparable<FileHandleInfo> {
    public int fd;
    public final String filename;
    public int bytesRead;
    public int bytesWritten;

    private boolean isOpen;
    private int sortId;
    private static int nextSortId = 0;

    public FileHandleInfo(int fd, String filename) {
        this.fd = fd;
        this.filename = filename;
        bytesRead = 0;
        bytesWritten = 0;

        isOpen = true;
        sortId = nextSortId++;
    }

    public void isOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public boolean isOpen() {
        return isOpen;
    }

    /** For sort by time opened */
    @Override
    public int compareTo(FileHandleInfo obj) {
        return (sortId - obj.sortId);
    }
}

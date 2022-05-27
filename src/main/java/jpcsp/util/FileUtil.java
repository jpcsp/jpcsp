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
package jpcsp.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class FileUtil {
    static public String getExtension(File file) {
        String base = file.getName();
        int index = base.lastIndexOf('.');
        if (index < 0) {
        	return "";
        }
        return base.substring(index + 1).toLowerCase();
    }

    static public void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] temp = new byte[0x10000];
        while (true) {
            int count = is.read(temp);
            if (count < 0) break;
            os.write(temp, 0, count);
        }
    }

    static public byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyStream(is, os);
        return os.toByteArray();
    }

    static public byte[] readURL(URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            return readInputStream(inputStream);
        }
    }

    static public String getURLBaseName(URL url) {
        if (url == null) return null;
        String path = url.getPath();
        int i = path.lastIndexOf('/');
        if (i < 0) {
        	return path;
        }
        return path.substring(i + 1);
    }

    static public void writeBytes(File file, byte[] data) throws FileNotFoundException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            os.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readAll(InputStream is, byte[] buffer, int offset, int len) throws IOException {
        int aoffset = offset;
        int remaining = len;
        while (remaining > 0) {
            int read = is.read(buffer, aoffset, remaining);
            if (read > 0) {
                remaining -= read;
                aoffset += read;
            }
        }
    }

    public static File findFolderNameInAncestors(File base, String name) {
        File current = base;
        while (current != null) {
            File file = new File(current, name);
            if (file.exists()) return file.getAbsoluteFile();
            current = current.getParentFile();
        }
        return null;
    }

    public static File[] listFilesRecursively(File directory, FileFilter fileFilter) {
    	File[] result = new File[0];
    	File[] paths = directory.listFiles();
    	for (File path : paths) {
    		if (fileFilter == null || fileFilter.accept(path)) {
				result = Utilities.add(result, path);
			}

    		if (path.isDirectory()) {
    			File[] subPaths = listFilesRecursively(path, fileFilter);
    			result = Utilities.add(result, subPaths);
    		}
    	}

    	return result;
    }
}

package jpcsp.util;

import java.io.File;

public class FileUtil {
    static public String getExtension(File file) {
        String base = file.getName();
        int index = base.lastIndexOf('.');
        if (index >= 0) {
            return base.substring(index + 1).toLowerCase();
        } else {
            return "";
        }
    }
}

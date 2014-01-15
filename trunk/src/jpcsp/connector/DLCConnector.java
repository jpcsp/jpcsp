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
package jpcsp.connector;

import java.io.File;
import java.io.FileNotFoundException;

import jpcsp.HLE.Modules;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.settings.Settings;
import jpcsp.State;

public class DLCConnector {
    
    public DLCConnector() {
    }

    public String getBaseDLCDirectory() {
        return String.format("%sDLC", Settings.getInstance().getDiscTmpDirectory());
    }
    
    protected String getFileNameFromPath(String path) {
        String pcfilename = Modules.IoFileMgrForUserModule.getDeviceFilePath(path);

        String[] name = pcfilename.split("/");
        String fName = "";
        for (int i = 0; i < name.length; i++) {
            if (name[i].toUpperCase().contains("EDAT")) {
                fName = name[i];
            }
        }

        return fName;
    }

    protected String getDLCPathFromFilePath(String path) {
        String pcfilename = Modules.IoFileMgrForUserModule.getDeviceFilePath(path);

        String[] name = pcfilename.split("/");
        String fName = "";
        for (int i = 0; i < name.length; i++) {
            String uname = name[i].toUpperCase();
            if (!name[i].contains("ms0") && uname.contains("PSP")
                    && uname.contains("GAME") && uname.contains(State.discId)
                    && (uname.contains("EDAT") || uname.contains("SPRX"))) {
                fName += File.separatorChar + name[i];
            }
        }

        if (fName.length() == 0) {
            return fName;
        }

        return fName.substring(1);
    }
    
    public String getDLCPath(String filepath) {
        return getBaseDLCDirectory() + getDLCPathFromFilePath(filepath);
    }
    
    public String getDecryptedDLCPath(String filepath) {
        return (getDLCPath(filepath) + File.separatorChar + getFileNameFromPath(filepath));
    }

    public SeekableDataInput loadDecryptedDLCFile(String fileName) {
        SeekableDataInput fileInput = null;
        File decryptedFile = new File(fileName);
        if (decryptedFile.canRead() && decryptedFile.length() > 0) {
            try {
                fileInput = new SeekableRandomFile(decryptedFile, "r");
                Modules.log.info("Using decrypted file " + fileName);
            } catch (FileNotFoundException e) {
            }
        }

        return fileInput;
    }
}

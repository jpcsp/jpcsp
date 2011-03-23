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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.swing.JOptionPane;

import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class PGDFileConnector {

    public static final String encryptedFileName = "PGDfile.raw";
    public static final String decryptedFileName = encryptedFileName + ".decrypted";
    public String id;

    public PGDFileConnector() {
    }

    public String getBaseDirectory(String id) {
        return String.format("%s%s\\PGD\\%s\\", Connector.baseDirectory, State.discId, id);
    }

    public String generateID(int startSector) {
        return String.format("File-%d", startSector);
    }

    public String getCompleteFileName(String fileName) {
        String completeFileName = String.format("%s%s", getBaseDirectory(id), fileName);

        return completeFileName;
    }

    protected void generateCommandFile(String keyHex) {
        try {
            PrintWriter command = new PrintWriter(String.format("%s%s", getBaseDirectory(id), Connector.commandFileName));
            command.println("DecryptPGD");
            command.println(Connector.basePSPDirectory + encryptedFileName + " " + keyHex);
            command.println("Exit");
            command.close();
        } catch (FileNotFoundException e) {
            // Ignore Exception
        }
    }

    public SeekableDataInput loadDecryptedPGDFile(String fileName) {
        SeekableDataInput fileInput = null;
        SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(fileName);
        if (stat != null) {
            int startSector = stat.getReserved(0);
            id = generateID(startSector);
            String decryptedCompleteFileName = getCompleteFileName(decryptedFileName);
            File decryptedFile = new File(decryptedCompleteFileName);
            if (decryptedFile.canRead() && decryptedFile.length() > 0) {
	            try {
	                fileInput = new SeekableRandomFile(decryptedFile, "r");
	                Modules.log.info("Using decrypted file " + decryptedCompleteFileName);
	            } catch (FileNotFoundException e) {
	            }
            }
        }
        return fileInput;
    }

    public void extractPGDFile(String fileName, SeekableDataInput fileInput, String keyHex) {
        Modules.log.info(String.format("decryptPGDFile(fileName='%s', key=%s)", fileName, keyHex));
        SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(fileName);
        if (stat != null) {
            int startSector = stat.getReserved(0);
            id = generateID(startSector);

            new File(getBaseDirectory(id)).mkdirs();
            generateCommandFile(keyHex);
            OutputStream outputFile = null;
            try {
                outputFile = new FileOutputStream(getCompleteFileName(encryptedFileName));
                byte[] buffer = new byte[100 * 1024];
                long fileLength = fileInput.length();
                for (long readLength = 0; readLength < fileLength;) {
                    long restLength = fileLength - readLength;
                    int length = buffer.length;
                    if (restLength < length) {
                        length = (int) restLength;
                    }
                    fileInput.readFully(buffer, 0, length);
                    outputFile.write(buffer, 0, length);
                    readLength += length;
                }

                // Inform the user how to decrypt the file using JpcspConnector
                String msg = "";
                msg += "This application contains a PGD file.\n";
                msg += "Copy the following 2 files\n";
                msg += "    " + getCompleteFileName(encryptedFileName) + "\n";
                msg += "    " + getBaseDirectory(id) + Connector.commandFileName + "\n";
                msg += "to your PSP under\n";
                msg += "    " + Connector.basePSPDirectory + "\n";
                msg += "and run the '" + Connector.jpcspConnectorName + "' on your PSP.\n";
                msg += "Decrypting might take a while on your PSP.\n";
                msg += "When the JpcspConnector is done, copy back the PSP file\n";
                msg += "    " + Connector.basePSPDirectory + decryptedFileName + "\n";
                msg += "to Jpcsp\n";
                msg += "    " + getCompleteFileName(decryptedFileName) + "\n";
                msg += "Afterwards, you can delete the files on your PSP.\n";
                msg += "Now run the application again in Jpcsp.\n";
                JOptionPane.showMessageDialog(null, msg);
            } catch (IOException e) {
                Modules.log.error(e);
            } finally {
                Utilities.close(outputFile);
            }
        }
    }
}

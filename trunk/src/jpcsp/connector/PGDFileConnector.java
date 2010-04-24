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
import jpcsp.HLE.pspiofilemgr;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;

/**
 * @author gid15
 *
 */
public class PGDFileConnector {
	protected static final String encryptedFileName = "PGDfile.raw";
	protected static final String decryptedFileName = encryptedFileName + ".decrypted";
	protected String id;

	public PGDFileConnector() {
	}

	protected static String getBaseDirectory(String id) {
		return String.format("%s%s/umd/%s/", Connector.baseDirectory, State.discId, id);
	}

	protected String generateID(int startSector) {
		return String.format("File-%d", startSector);
	}

	protected String getCompleteFileName(String fileName) {
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

	public SeekableDataInput decryptPGDFile(String fileName, SeekableDataInput fileInput, String keyHex) {
		Modules.log.info(String.format("decryptPGDFile(fileName='%s', key=%s)", fileName, keyHex));
		SceIoStat stat = pspiofilemgr.getInstance().statFile(fileName);
		if (stat != null) {
			int startSector = stat.getReserved(0);
			id = generateID(startSector);

			String decryptedCompleteFileName = getCompleteFileName(decryptedFileName);
			File decryptedFile = new File(decryptedCompleteFileName);
			if (decryptedFile.canRead()) {
				// The file has already been decrypted, use the decrypted file
				try {
					fileInput = new SeekableRandomFile(decryptedFile, "r");
					Modules.log.info("Using decrypted file " + decryptedCompleteFileName);
				} catch (FileNotFoundException e) {
				}
			} else {
				// The file has not yet been decrypted, prepare for decryption with JpcspConnector
				new File(getBaseDirectory(id)).mkdirs();
				generateCommandFile(keyHex);
				try {
					OutputStream outputFile = new FileOutputStream(getCompleteFileName(encryptedFileName));
					byte[] buffer = new byte[100 * 1024];
					long fileLength = fileInput.length();
					for (long readLength = 0; readLength < fileLength; ) {
						long restLength = fileLength - readLength;
						int length = buffer.length;
						if (restLength < length) {
							length = (int) restLength;
						}
						fileInput.readFully(buffer, 0, length);
						outputFile.write(buffer, 0, length);
						readLength += length;
					}
					outputFile.close();

					// Inform the user how to decrypt the file using JpcspConnector
					String msg = "";
					msg += "This application contains a crypted file which needs to be decrypted on your PSP.\n";
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
				}
			}
		}

		return fileInput;
	}
}

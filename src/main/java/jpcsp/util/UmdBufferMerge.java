package jpcsp.util;

import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class UmdBufferMerge {
	public static void main(String[] args) {
		File fileToc1 = new File("tmp/umdbuffer1.toc");
		File fileIso1 = new File("tmp/umdbuffer1.iso");
		File fileToc2 = new File("tmp/umdbuffer2.toc");
		File fileIso2 = new File("tmp/umdbuffer2.iso");
		File fileToc = new File("tmp/umdbuffer.toc");
		File fileIso = new File("tmp/umdbuffer.iso");

		try {
			FileOutputStream fosToc = new FileOutputStream(fileToc);
			FileOutputStream fosIso = new FileOutputStream(fileIso);
			FileInputStream fisToc1 = new FileInputStream(fileToc1);
			FileInputStream fisIso1 = new FileInputStream(fileIso1);
			FileInputStream fisToc2 = new FileInputStream(fileToc2);
			DataOutput toc = new DataOutputStream(fosToc);
			DataOutput iso = new DataOutputStream(fosIso);
			DataInput toc1 = new DataInputStream(fisToc1);
			DataInput iso1 = new DataInputStream(fisIso1);
			DataInput toc2 = new DataInputStream(fisToc2);
			RandomAccessFile iso2 = new RandomAccessFile(fileIso2, "r");

			int numSectors = toc1.readInt();
			int numSectorsMerge = toc2.readInt();
			toc.writeInt(Math.max(numSectors, numSectorsMerge));

			HashMap<Integer, Integer> tocHashMap = new HashMap<Integer, Integer>();
			for (int i = 4; i < fileToc1.length(); i += 8) {
				int sectorNumber = toc1.readInt();
				int bufferedSectorNumber = toc1.readInt();
				tocHashMap.put(sectorNumber, bufferedSectorNumber);
				toc.writeInt(sectorNumber);
				toc.writeInt(bufferedSectorNumber);
			}

			byte[] buffer = new byte[sectorLength];
			for (int i = 0; i < fileIso1.length(); i += buffer.length) {
				iso1.readFully(buffer);
				iso.write(buffer);
			}

			int nextFreeBufferedSectorNumber = (int) (fileIso1.length() / sectorLength);
			for (int i = 4; i < fileToc2.length(); i += 8) {
				int sectorNumber = toc2.readInt();
				int bufferedSectorNumber = toc2.readInt();
				if (!tocHashMap.containsKey(sectorNumber)) {
					iso2.seek(bufferedSectorNumber * (long) sectorLength);
					iso2.readFully(buffer);
					iso.write(buffer);

					toc.writeInt(sectorNumber);
					toc.writeInt(nextFreeBufferedSectorNumber);
					tocHashMap.put(sectorNumber, nextFreeBufferedSectorNumber);
					nextFreeBufferedSectorNumber++;
				}
			}

			fosToc.close();
			fosIso.close();
			fisToc1.close();
			fisIso1.close();
			fisToc2.close();
			iso2.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

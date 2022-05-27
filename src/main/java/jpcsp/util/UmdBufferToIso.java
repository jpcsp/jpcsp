package jpcsp.util;

import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class UmdBufferToIso {
	public static void main(String[] args) {
		try {
			RandomAccessFile inToc = new RandomAccessFile("tmp/umdbuffer.toc", "r");
			RandomAccessFile inIso = new RandomAccessFile("tmp/umdbuffer.iso", "r");
			RandomAccessFile outIso = new RandomAccessFile("tmp/umd.iso", "rw");

			int numSectors = inToc.readInt();
			System.out.println(String.format("numSectors=%d", numSectors));
			byte[] buffer = new byte[sectorLength];
			for (int i = 4; i < inToc.length(); i += 8) {
				int sectorNumber = inToc.readInt();
				int bufferedSectorNumber = inToc.readInt();
				inIso.seek(bufferedSectorNumber * (long) sectorLength);
				inIso.readFully(buffer);

				outIso.seek(sectorNumber * (long) sectorLength);
				outIso.write(buffer);
			}
			inToc.close();
			inIso.close();
			outIso.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

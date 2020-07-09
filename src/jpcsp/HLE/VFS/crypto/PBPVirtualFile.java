package jpcsp.HLE.VFS.crypto;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;

public class PBPVirtualFile extends AbstractProxyVirtualFile {
	private byte[] key;
	private int pgdOffset;

	public PBPVirtualFile(byte[] key, IVirtualFile vFile) {
		super(vFile);
		this.key = key;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;
		switch (command) {
			case 0x04100001:
				if (key != null) {
					// The PBP file is encrypted
					IVirtualFile pgdFile = new PGDVirtualFile(key, vFile, pgdOffset);
					setProxyVirtualFile(pgdFile);
				} else {
					// The PBP file is not encrypted
					ioLseek(pgdOffset);
				}
				result = 0;
				break;
			case 0x04100002:
				pgdOffset = inputPointer.getValue32();
				result = 0;
				break;
			default:
				result = super.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
				break;
		}

		return result;
	}

	@Override
	public IVirtualFile duplicate() {
		PBPVirtualFile vFileDuplicate = new PBPVirtualFile(key, super.duplicate());
		vFileDuplicate.pgdOffset = pgdOffset;
		vFileDuplicate.ioLseek(getPosition());

		return vFileDuplicate;
	}
}

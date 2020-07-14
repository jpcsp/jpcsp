package jpcsp.HLE.VFS.crypto;

import static jpcsp.format.Elf32Header.ELF_MAGIC;
import static jpcsp.format.PSP.PSP_MAGIC;

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

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int result = super.ioRead(outputPointer, outputLength);

		// If the PBP file is not encrypted, patch the "~ELF" header with "~PSP"
		// to let popsman.prx think it is still encrypted.
		if (key == null && outputLength == 4 && result == outputLength) {
			if (outputPointer.getUnalignedValue32() == ELF_MAGIC) {
				outputPointer.setUnalignedValue32(0, PSP_MAGIC);
			}
		}

		return result;
	}
}

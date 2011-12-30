package jpcsp.HLE.VFS.iso;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.FileArg;
import jpcsp.HLE.VFS.IoDirent;
import jpcsp.HLE.VFS.IoStat;
import jpcsp.HLE.VFS.VirtualFileSystem;

public class UmdIsoVirtualFileSystem extends VirtualFileSystem {

	@Override
	public void ioInit() {
	}

	@Override
	public void ioExit() {
	}

	@Override
	public int ioOpen(FileArg fileArg, String fileName, int flags, int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioClose(FileArg fileArg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioRead(FileArg fileArg, TPointer outputPointer, int outputLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioWrite(FileArg fileArg, TPointer inputPointer, int inputLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long ioLseek(FileArg fileArg, long offset, int whence) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioIoctl(FileArg fileArg, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioRemove(FileArg fileArg, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioMkdir(FileArg fileArg, String name, int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioRmdir(FileArg fileArg, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioDopen(FileArg fileArg, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioDclose(FileArg fileArg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioDread(FileArg fileArg, IoDirent dir) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioGetstat(FileArg fileArg, String fileName, IoStat stat) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioChstat(FileArg fileArg, String fileName, IoStat stat, int bits) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioRename(FileArg fileArg, String oldFileName, String newFileName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioChdir(FileArg fileArg, String directoryName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioMount(FileArg fileArg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioUmount(FileArg fileArg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioDevctl(FileArg fileArg, String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ioUnk21(FileArg fileArg) {
		// TODO Auto-generated method stub
		return 0;
	}

}

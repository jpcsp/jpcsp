package jpcsp.HLE.VFS;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import jpcsp.HLE.TPointer;

abstract public class VirtualFileSystem extends AbstractVirtualFileSystem {
	@Override
	public void ioInit() {
		throw(new NotImplementedException());
	}

	@Override
	public void ioExit() {
		throw(new NotImplementedException());
	}

	@Override
	public int ioOpen(FileArg fileArg, String fileName, int flags, int mode) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioClose(FileArg fileArg) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioRead(FileArg fileArg, TPointer outputPointer, int outputLength) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioWrite(FileArg fileArg, TPointer inputPointer, int inputLength) {
		throw(new NotImplementedException());
	}

	@Override
	public long ioLseek(FileArg fileArg, long offset, int whence) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioIoctl(FileArg fileArg, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioRemove(FileArg fileArg, String name) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioMkdir(FileArg fileArg, String name, int mode) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioRmdir(FileArg fileArg, String name) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioDopen(FileArg fileArg, String name) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioDclose(FileArg fileArg) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioDread(FileArg fileArg, IoDirent dir) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioGetstat(FileArg fileArg, String fileName, IoStat stat) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioChstat(FileArg fileArg, String fileName, IoStat stat, int bits) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioRename(FileArg fileArg, String oldFileName, String newFileName) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioChdir(FileArg fileArg, String directoryName) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioMount(FileArg fileArg) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioUmount(FileArg fileArg) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioDevctl(FileArg fileArg, String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		throw(new NotImplementedException());
	}

	@Override
	public int ioUnk21(FileArg fileArg) {
		throw(new NotImplementedException());
	}
}

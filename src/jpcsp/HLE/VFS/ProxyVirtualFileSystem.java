package jpcsp.HLE.VFS;

import jpcsp.HLE.TPointer;

public class ProxyVirtualFileSystem extends VirtualFileSystem {
	protected VirtualFileSystem parent;
	
	public ProxyVirtualFileSystem(VirtualFileSystem parent) {
		this.parent = parent;
	}
	
	protected FileArg hookFileArg(FileArg fileArg) {
		return fileArg;
	}
	
	protected String hookFileName(String fileName) {
		return fileName;
	}
	
	protected String hookDirectoryName(String directoryName) {
		return hookFileName(directoryName);
	}
	
	protected String hookDeviceName(String deviceName) {
		return deviceName;
	}

	@Override
	public void ioInit() {
		parent.ioInit();
	}

	@Override
	public void ioExit() {
		parent.ioExit();
	}

	@Override
	public int ioOpen(FileArg fileArg, String fileName, int flags, int mode) {
		return parent.ioOpen(hookFileArg(fileArg), hookFileName(fileName), flags, mode);
	}

	@Override
	public int ioClose(FileArg fileArg) {
		return parent.ioClose(hookFileArg(fileArg));
	}

	@Override
	public int ioRead(FileArg fileArg, TPointer outputPointer, int outputLength) {
		return parent.ioRead(hookFileArg(fileArg), outputPointer, outputLength);
	}

	@Override
	public int ioWrite(FileArg fileArg, TPointer inputPointer, int inputLength) {
		return parent.ioWrite(hookFileArg(fileArg), inputPointer, inputLength);
	}

	@Override
	public long ioLseek(FileArg fileArg, long offset, int whence) {
		return parent.ioLseek(hookFileArg(fileArg), offset, whence);
	}

	@Override
	public int ioIoctl(FileArg fileArg, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return parent.ioIoctl(hookFileArg(fileArg), command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public int ioRemove(FileArg fileArg, String name) {
		return parent.ioRemove(hookFileArg(fileArg), hookFileName(name));
	}

	@Override
	public int ioMkdir(FileArg fileArg, String name, int mode) {
		return parent.ioMkdir(hookFileArg(fileArg), hookDirectoryName(name), mode);
	}

	@Override
	public int ioRmdir(FileArg fileArg, String name) {
		return parent.ioRmdir(hookFileArg(fileArg), hookDirectoryName(name));
	}

	@Override
	public int ioDopen(FileArg fileArg, String name) {
		return parent.ioDopen(hookFileArg(fileArg), hookDirectoryName(name));
	}

	@Override
	public int ioDclose(FileArg fileArg) {
		return parent.ioDclose(hookFileArg(fileArg));
	}

	@Override
	public int ioDread(FileArg fileArg, IoDirent dir) {
		return parent.ioDread(hookFileArg(fileArg), dir);
	}

	@Override
	public int ioGetstat(FileArg fileArg, String fileName, IoStat stat) {
		return parent.ioGetstat(hookFileArg(fileArg), hookFileName(fileName), stat);
	}

	@Override
	public int ioChstat(FileArg fileArg, String fileName, IoStat stat, int bits) {
		return parent.ioChstat(hookFileArg(fileArg), hookFileName(fileName), stat, bits);
	}

	@Override
	public int ioRename(FileArg fileArg, String oldFileName, String newFileName) {
		return parent.ioRename(hookFileArg(fileArg), hookFileName(oldFileName), hookFileName(newFileName));
	}

	@Override
	public int ioChdir(FileArg fileArg, String directoryName) {
		return parent.ioChdir(hookFileArg(fileArg), hookDirectoryName(directoryName));
	}

	@Override
	public int ioMount(FileArg fileArg) {
		return parent.ioMount(hookFileArg(fileArg));
	}

	@Override
	public int ioUmount(FileArg fileArg) {
		return parent.ioUmount(hookFileArg(fileArg));
	}

	@Override
	public int ioDevctl(FileArg fileArg, String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return parent.ioDevctl(hookFileArg(fileArg), hookDeviceName(deviceName), command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public int ioUnk21(FileArg fileArg) {
		return parent.ioUnk21(hookFileArg(fileArg));
	}

}

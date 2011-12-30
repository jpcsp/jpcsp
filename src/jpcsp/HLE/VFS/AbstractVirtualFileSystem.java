package jpcsp.HLE.VFS;

import jpcsp.HLE.TPointer;

abstract public class AbstractVirtualFileSystem {
	abstract public void ioInit();
	abstract public void ioExit(); 
	abstract public int ioOpen(FileArg fileArg, String fileName, int flags, int mode);
	abstract public int ioClose(FileArg fileArg);
	abstract public int ioRead(FileArg fileArg, TPointer outputPointer, int outputLength);
	abstract public int ioWrite(FileArg fileArg, TPointer inputPointer, int inputLength);
	abstract public long ioLseek(FileArg fileArg, long offset, int whence);
	abstract public int ioIoctl(FileArg fileArg, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength);
	abstract public int ioRemove(FileArg fileArg, String name);
	abstract public int ioMkdir(FileArg fileArg, String name, int mode);
	abstract public int ioRmdir(FileArg fileArg, String name);
	abstract public int ioDopen(FileArg fileArg, String name);
	abstract public int ioDclose(FileArg fileArg);
	abstract public int ioDread(FileArg fileArg, IoDirent dir);
	abstract public int ioGetstat(FileArg fileArg, String fileName, IoStat stat);
	abstract public int ioChstat(FileArg fileArg, String fileName, IoStat stat, int bits);
	abstract public int ioRename(FileArg fileArg, String oldFileName, String newFileName);
	abstract public int ioChdir(FileArg fileArg, String directoryName);
	abstract public int ioMount(FileArg fileArg);
	abstract public int ioUmount(FileArg fileArg);
	abstract public int ioDevctl(FileArg fileArg, String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength);
	abstract public int ioUnk21(FileArg fileArg);
}

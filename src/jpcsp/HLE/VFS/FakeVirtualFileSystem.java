package jpcsp.HLE.VFS;

import java.util.HashMap;
import java.util.Map;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceIoStat;

public class FakeVirtualFileSystem extends AbstractVirtualFileSystem {
	private static FakeVirtualFileSystem instance;
	private Map<String, IVirtualFile> registeredFakeVirtualFiles = new HashMap<String, IVirtualFile>();

	public static FakeVirtualFileSystem getInstance() {
		if (instance == null) {
			instance = new FakeVirtualFileSystem();
		}

		return instance;
	}

	public void registerAllFakeVirtualFiles(VirtualFileSystemManager vfsManager) {
		for (String fileName : registeredFakeVirtualFiles.keySet()) {
			vfsManager.register(fileName, this);
		}
	}

	public void registerFakeVirtualFile(String fileName, IVirtualFile vFile) {
		registeredFakeVirtualFiles.put(fileName, vFile);
		Modules.IoFileMgrForUserModule.getVirtualFileSystemManager().register(fileName, this);
	}

	public void unregisterFakeVirtualFile(String fileName) {
		registeredFakeVirtualFiles.remove(fileName);
		Modules.IoFileMgrForUserModule.getVirtualFileSystemManager().unregister(fileName);
	}

	private IVirtualFile getRegisteredFakeVirtualFile(String fileName) {
		return registeredFakeVirtualFiles.get(fileName);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		IVirtualFile vFile = getRegisteredFakeVirtualFile(fileName);
		if (vFile != null) {
			return vFile.duplicate();
		}

		return super.ioOpen(fileName, flags, mode);
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		IVirtualFile vFile = getRegisteredFakeVirtualFile(fileName);
		if (vFile != null) {
			stat.size = vFile.length();
			return 0;
		}

		return super.ioGetstat(fileName, stat);
	}
}

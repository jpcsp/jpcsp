package jpcsp.HLE.VFS;

public class MountableVirtualFileSystem extends ProxyVirtualFileSystem {
	public MountableVirtualFileSystem(VirtualFileSystem parent) {
		super(parent);
	}
}

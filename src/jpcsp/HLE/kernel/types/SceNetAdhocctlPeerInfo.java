package jpcsp.HLE.kernel.types;

import jpcsp.HLE.modules.sceNetAdhocctl;

/** Peer info structure */
public class SceNetAdhocctlPeerInfo extends pspAbstractMemoryMappedStructure {
	public int nextAddr;
	/** Nickname */
	public String nickName;
	/** Mac address */
	public pspNetMacAddress macAddress;
	/** Time stamp */
	public long timestamp;

	@Override
	protected void read() {
		nextAddr = read32();
		nickName = readStringNZ(sceNetAdhocctl.NICK_NAME_LENGTH);
		macAddress = new pspNetMacAddress();
		read(macAddress);
		readUnknown(6);
		timestamp = read64();
	}

	@Override
	protected void write() {
		write32(nextAddr);
		writeStringNZ(sceNetAdhocctl.NICK_NAME_LENGTH, nickName);
		write(macAddress);
		writeUnknown(6);
		write64(timestamp);
	}

	@Override
	public int sizeof() {
		return 152;
	}
}

/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.network.jpcsp;

import static jpcsp.HLE.Modules.sceNetAdhocctlModule;
import static jpcsp.HLE.modules150.sceNetAdhocctl.GROUP_NAME_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.IBSS_NAME_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.MAX_GAME_MODE_MACS;
import static jpcsp.HLE.modules150.sceNetAdhocctl.NICK_NAME_LENGTH;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;

import java.util.List;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules150.sceNet;

/**
 * @author gid15
 *
 */
public class JpcspAdhocctlMessage {
	protected String nickName;
	protected byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
	protected String groupName;
	protected String ibss;
	protected int mode;
	protected int channel;
	protected boolean gameModeComplete;
	protected byte[][] gameModeMacs;

	public JpcspAdhocctlMessage(String nickName, byte[] macAddress, String groupName) {
		this.nickName = nickName;
		System.arraycopy(macAddress, 0, this.macAddress, 0, this.macAddress.length);
		this.groupName = groupName;
		ibss = sceNetAdhocctlModule.hleNetAdhocctlGetIBSS();
		mode = sceNetAdhocctlModule.hleNetAdhocctlGetMode();
		channel = sceNetAdhocctlModule.hleNetAdhocctlGetChannel();
		gameModeComplete = false;
		gameModeMacs = null;
	}

	public JpcspAdhocctlMessage(byte[] message, int length) {
		int offset = 0;
		nickName = copyFromMessage(message, offset, NICK_NAME_LENGTH);
		offset += NICK_NAME_LENGTH;
		copyFromMessage(message, offset, macAddress);
		offset += macAddress.length;
		groupName = copyFromMessage(message, offset, GROUP_NAME_LENGTH);
		offset += GROUP_NAME_LENGTH;
		ibss = copyFromMessage(message, offset, IBSS_NAME_LENGTH);
		offset += IBSS_NAME_LENGTH;
		mode = copyInt32FromMessage(message, offset);
		offset += 4;
		channel = copyInt32FromMessage(message, offset);
		offset += 4;
		gameModeComplete = copyBoolFromMessage(message, offset);
		offset++;
		int numberGameModeMacs = copyInt32FromMessage(message, offset);
		offset += 4;
		if (numberGameModeMacs > 0) {
			gameModeMacs = copyMacsFromMessage(message, offset, numberGameModeMacs);
			offset += MAC_ADDRESS_LENGTH * numberGameModeMacs;
		}
	}

	public void setGameModeComplete(boolean gameModeComplete, List<pspNetMacAddress> requiredGameModeMacs) {
		this.gameModeComplete = gameModeComplete;
		int numberGameModeMacs = requiredGameModeMacs.size();
		gameModeMacs = new byte[numberGameModeMacs][MAC_ADDRESS_LENGTH];
		int i = 0;
		for (pspNetMacAddress macAddress : requiredGameModeMacs) {
			gameModeMacs[i] = macAddress.macAddress;
			i++;
		}
	}

	private String copyFromMessage(byte[] message, int offset, int length) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < length; i++) {
			byte b = message[offset + i];
			if (b == 0) {
				break;
			}
			s.append((char) b);
		}

		return s.toString();
	}

	private int copyInt32FromMessage(byte[] message, int offset) {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			n |= (message[offset + i] & 0xFF) << (i * 8);
		}

		return n;
	}

	private boolean copyBoolFromMessage(byte[] message, int offset) {
		return message[offset] != 0;
	}

	private void copyFromMessage(byte[] message, int offset, byte[] bytes) {
		System.arraycopy(message, offset, bytes, 0, bytes.length);
	}

	private byte[][] copyMacsFromMessage(byte[] message, int offset, int numberMacs) {
		byte[][] macs = new byte[numberMacs][MAC_ADDRESS_LENGTH];
		for (int i = 0; i < numberMacs; i++) {
			copyFromMessage(message, offset, macs[i]);
			offset += macs[i].length;
		}

		return macs;
	}

	private void copyToMessage(byte[] message, int offset, String s) {
		if (s != null) {
    		int length = s.length();
    		for (int i = 0; i < length; i++) {
    			message[offset + i] = (byte) s.charAt(i);
    		}
		}
	}

	private void copyToMessage(byte[] message, int offset, byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			message[offset + i] = bytes[i];
		}
	}

	private void copyInt32ToMessage(byte[] message, int offset, int value) {
		for (int i = 0; i < 4; i++) {
			message[offset + i] = (byte) (value >> (i * 8));
		}
	}

	private void copyBoolToMessage(byte[] message, int offset, boolean value) {
		message[offset] = (byte) (value ? 1 : 0);
	}

	private void copyMacsToMessage(byte[] message, int offset, byte[][] macs) {
		for (int i = 0; i < macs.length; i++) {
			copyToMessage(message, offset, macs[i]);
			offset += macs[i].length;
		}
	}

	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];

		int offset = 0;
		copyToMessage(message, offset, nickName);
		offset += NICK_NAME_LENGTH;
		copyToMessage(message, offset, macAddress);
		offset += macAddress.length;
		copyToMessage(message, offset, groupName);
		offset += GROUP_NAME_LENGTH;
		copyToMessage(message, offset, ibss);
		offset += IBSS_NAME_LENGTH;
		copyInt32ToMessage(message, offset, mode);
		offset += 4;
		copyInt32ToMessage(message, offset, channel);
		offset += 4;
		copyBoolToMessage(message, offset, gameModeComplete);
		offset++;
		if (gameModeMacs == null) {
			copyInt32ToMessage(message, offset, 0);
			offset += 4;
		} else {
			copyInt32ToMessage(message, offset, gameModeMacs.length);
			offset += 4;
			copyMacsToMessage(message, offset, gameModeMacs);
			offset += gameModeMacs.length * MAC_ADDRESS_LENGTH;
		}

		return message;
	}

	public static int getMessageLength() {
		return NICK_NAME_LENGTH + MAC_ADDRESS_LENGTH + GROUP_NAME_LENGTH + IBSS_NAME_LENGTH + 4 + 4 + 1 + 4 + MAX_GAME_MODE_MACS * MAC_ADDRESS_LENGTH;
	}

	@Override
	public String toString() {
		StringBuilder macs = new StringBuilder();
		if (gameModeMacs != null) {
			macs.append(", gameModeMacs=[");
			for (int i = 0; i < gameModeMacs.length; i++) {
				if (i > 0) {
					macs.append(", ");
				}
				macs.append(sceNet.convertMacAddressToString(gameModeMacs[i]));
			}
			macs.append("]");
		}

		return String.format("JpcspAdhocctlMessage[nickName='%s', macAddress=%s, groupName='%s', IBSS='%s', mode=%d, channel=%d, gameModeComplete=%b%s]", nickName, sceNet.convertMacAddressToString(macAddress), groupName, ibss, mode, channel, gameModeComplete, macs.toString());
	}
}

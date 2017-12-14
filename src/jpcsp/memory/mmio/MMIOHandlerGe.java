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
package jpcsp.memory.mmio;

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_GE_INTR;
import static jpcsp.graphics.RE.externalge.NativeUtils.CTRL_ACTIVE;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_END;

import org.apache.log4j.Logger;

import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.RE.externalge.CoreThreadMMIO;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.graphics.RE.externalge.NativeUtils;

public class MMIOHandlerGe extends MMIOHandlerBase {
	public static Logger log = sceGe_user.log;
	public static final int BASE_ADDRESS = 0xBD400000;
	private static MMIOHandlerGe instance;
	private int ctrl;
	private int status;
	private int list;
	private int stall;
	private int raddr1;
	private int raddr2;
	private int vaddr;
	private int iaddr;
	private int oaddr;
	private int oaddr1;
	private int oaddr2;
	private int cmdStatus;
	private int interrupt;

	public static MMIOHandlerGe getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerGe(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerGe(int baseAddress) {
		super(baseAddress);
	}

	public void triggerGeInterrupt() {
		setInterrupt(getInterrupt() | INTR_STAT_END);
	}

	private int getStatus() {
		if (ExternalGE.isActive()) {
			status = NativeUtils.getCoreStat();
		}
		return status;
	}

	private void setStatus(int status) {
		this.status = status;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreStat(status);
		}
	}

	private int getList() {
		if (ExternalGE.isActive()) {
			list = NativeUtils.getCoreMadr();
		}
		return list;
	}

	private void setList(int list) {
		this.list = list;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreMadr(list);
		}
	}

	private int getStall() {
		if (ExternalGE.isActive()) {
			stall = NativeUtils.getCoreSadr();
		}
		return stall;
	}

	private void setStall(int stall) {
		this.stall = stall;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreSadr(stall);
			CoreThreadMMIO.getInstance().sync();
		}
	}

	private int getRaddr1() {
		return raddr1;
	}

	private void setRaddr1(int raddr1) {
		this.raddr1 = raddr1;
	}

	private int getRaddr2() {
		return raddr2;
	}

	private void setRaddr2(int raddr2) {
		this.raddr2 = raddr2;
	}

	private int getVaddr() {
		return vaddr;
	}

	private void setVaddr(int vaddr) {
		this.vaddr = vaddr;
	}

	private int getIaddr() {
		return iaddr;
	}

	private void setIaddr(int iaddr) {
		this.iaddr = iaddr;
	}

	private int getOaddr() {
		return oaddr;
	}

	private void setOaddr(int oaddr) {
		this.oaddr = oaddr;
	}

	private int getOaddr1() {
		return oaddr1;
	}

	private void setOaddr1(int oaddr1) {
		this.oaddr1 = oaddr1;
	}

	private int getOaddr2() {
		return oaddr2;
	}

	private void setOaddr2(int oaddr2) {
		this.oaddr2 = oaddr2;
	}

	private int getCmdStatus() {
		if (ExternalGE.isActive()) {
			cmdStatus = NativeUtils.getCoreIntrStat();
		}
		return cmdStatus;
	}

	private void setCmdStatus(int cmdStatus) {
		this.cmdStatus = cmdStatus;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreIntrStat(cmdStatus);
		}
	}

	private void changeCmdStatus(int mask) {
		setCmdStatus(cmdStatus ^ mask);
	}

	private int getInterrupt() {
		return interrupt;
	}

	private void setInterrupt(int interrupt) {
		this.interrupt = interrupt;

		if ((interrupt & INTR_STAT_END) == 0) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_GE_INTR);
		} else {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_GE_INTR);
		}
	}

	private void changeInterrupt(int mask) {
		setInterrupt(interrupt ^ mask);
	}

	private void clearInterrupt(int mask) {
		setInterrupt(interrupt & ~mask);
	}

	private void startGeList() {
		if (ExternalGE.isActive()) {
			NativeUtils.setLogLevel();
			// Update the screen scale only at the start of a new list
			NativeUtils.setScreenScale(ExternalGE.getScreenScale());
			CoreThreadMMIO.getInstance().sync();
		}
	}

	private void stopGeList() {
		// TODO
	}

	private int getCtrl() {
		if (ExternalGE.isActive()) {
			ctrl = NativeUtils.getCoreCtrl();
		}
		return ctrl;
	}

	private void setCtrl(int ctrl) {
		int oldCtrl = this.ctrl;
		this.ctrl = ctrl;

		if (ExternalGE.isActive()) {
			NativeUtils.setCoreCtrl(ctrl);
		}

		if ((oldCtrl & CTRL_ACTIVE) != (ctrl & CTRL_ACTIVE)) {
			if ((ctrl & CTRL_ACTIVE) != 0) {
				startGeList();
			} else {
				stopGeList();
			}
		}
	}

	private int readGeCmd(int cmd) {
		return 0;
	}

	private int readGeBone(int bone) {
		return 0;
	}

	private int readGeWorld(int world) {
		return 0;
	}

	private int readGeView(int view) {
		return 0;
	}

	private int readGeProjection(int projection) {
		return 0;
	}

	private int readGeTexture(int texture) {
		return 0;
	}

	@Override
	public int read32(int address) {
		int value;
		int localAddress = address - baseAddress;
		switch (localAddress) {
			case 0x004: value = 0; break; // Unknown
			case 0x008: value = MemoryMap.SIZE_VRAM >> 10; break;
			case 0x100: value = getCtrl(); break;
			case 0x104: value = getStatus(); break;
			case 0x108: value = getList(); break;
			case 0x10C: value = getStall(); break;
			case 0x110: value = getRaddr1(); break;
			case 0x114: value = getRaddr2(); break;
			case 0x118: value = getVaddr(); break;
			case 0x11C: value = getIaddr(); break;
			case 0x120: value = getOaddr(); break;
			case 0x124: value = getOaddr1(); break;
			case 0x128: value = getOaddr2(); break;
			case 0x304: value = getCmdStatus(); break;
			case 0x308: value = getInterrupt(); break;
			case 0x400: value = 0; break; // Unknown
			default:
				if (localAddress >= 0x800 && localAddress < 0xC00) {
					value = readGeCmd((localAddress - 0x800) >> 2);
				} else if (localAddress >= 0xC00 && localAddress < 0xD80) {
					value = readGeBone((localAddress - 0xC00) >> 2);
				} else if (localAddress >= 0xD80 && localAddress < 0xDB0) {
					value = readGeWorld((localAddress - 0xD80) >> 2);
				} else if (localAddress >= 0xDB0 && localAddress < 0xDE0) {
					value = readGeView((localAddress - 0xDB0) >> 2);
				} else if (localAddress >= 0xDE0 && localAddress < 0xE20) {
					value = readGeProjection((localAddress - 0xDE0) >> 2);
				} else if (localAddress >= 0xE20 && localAddress < 0xE50) {
					value = readGeTexture((localAddress - 0xE20) >> 2);
				} else {
					value = super.read32(address);
				}
				break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x100: setCtrl(value); break;
			case 0x104: setStatus(value); break;
			case 0x108: setList(value); break;
			case 0x10C: setStall(value); break;
			case 0x110: setRaddr1(value); break;
			case 0x114: setRaddr2(value); break;
			case 0x118: setVaddr(value); break;
			case 0x11C: setIaddr(value); break;
			case 0x120: setOaddr(value); break;
			case 0x124: setOaddr1(value); break;
			case 0x128: setOaddr2(value); break;
			case 0x304: setCmdStatus(value); break;
			case 0x308: clearInterrupt(value); break;
			case 0x30C: changeInterrupt(value); break;
			case 0x310: changeCmdStatus(value); break;
			case 0x400: break; // Unknown
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}

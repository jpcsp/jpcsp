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

import static jpcsp.HLE.Modules.sceDisplayModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_GE_INTR;
import static jpcsp.graphics.RE.externalge.NativeUtils.CTRL_ACTIVE;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_END;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.externalge.CoreThreadMMIO;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.graphics.RE.externalge.NativeUtils;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerGe extends MMIOHandlerBase {
	public static Logger log = sceGe_user.log;
	private static final int STATE_VERSION = 0;
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
	private int unknown200;
	private int unknown400;

	public static MMIOHandlerGe getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerGe(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerGe(int baseAddress) {
		super(baseAddress);

		ExternalGE.init();
		if (!ExternalGE.isActive() && !VideoEngine.getInstance().lleIsActive()) {
			log.error(String.format("MMIOHandlerGe is only working with the External Software Renderer or the OpenGL Renderer"));
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		setStatus(stream.readInt());
		setList(stream.readInt());
		setStall(stream.readInt());
		setRaddr1(stream.readInt());
		setRaddr2(stream.readInt());
		setVaddr(stream.readInt());
		setIaddr(stream.readInt());
		setOaddr(stream.readInt());
		setOaddr1(stream.readInt());
		setOaddr2(stream.readInt());
		setCmdStatus(stream.readInt());
		setInterrupt(stream.readInt());
		unknown200 = stream.readInt();
		unknown400 = stream.readInt();
		for (int cmd = 0x00; cmd <= 0xFF; cmd++) {
			int value = stream.readInt();
			writeGeCmd(cmd, value);
		}
		for (int i = 0; i < 8 * 12; i++) {
			writeGeBone(i, stream.readInt());
		}
		for (int i = 0; i < 12; i++) {
			writeGeWorld(i, stream.readInt());
		}
		for (int i = 0; i < 12; i++) {
			writeGeView(i, stream.readInt());
		}
		for (int i = 0; i < 16; i++) {
			writeGeProjection(i, stream.readInt());
		}
		for (int i = 0; i < 12; i++) {
			writeGeTexture(i, stream.readInt());
		}
		// Setting the ctrl must be the last action as it might trigger a GE list execution
		setCtrl(stream.readInt());
		super.read(stream);

		sceDisplayModule.setGeDirty(true);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(getStatus());
		stream.writeInt(getList());
		stream.writeInt(getStall());
		stream.writeInt(getRaddr1());
		stream.writeInt(getRaddr2());
		stream.writeInt(getVaddr());
		stream.writeInt(getIaddr());
		stream.writeInt(getOaddr());
		stream.writeInt(getOaddr1());
		stream.writeInt(getOaddr2());
		stream.writeInt(getCmdStatus());
		stream.writeInt(getInterrupt());
		stream.writeInt(unknown200);
		stream.writeInt(unknown400);
		for (int cmd = 0x00; cmd <= 0xFF; cmd++) {
			stream.writeInt(readGeCmd(cmd));
		}
		for (int i = 0; i < 8 * 12; i++) {
			stream.writeInt(readGeBone(i));
		}
		for (int i = 0; i < 12; i++) {
			stream.writeInt(readGeWorld(i));
		}
		for (int i = 0; i < 12; i++) {
			stream.writeInt(readGeView(i));
		}
		for (int i = 0; i < 16; i++) {
			stream.writeInt(readGeProjection(i));
		}
		for (int i = 0; i < 12; i++) {
			stream.writeInt(readGeTexture(i));
		}
		stream.writeInt(getCtrl());
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleReset();
		}

		setStatus(0);
		setList(0);
		setStall(0);
		setRaddr1(0);
		setRaddr2(0);
		setVaddr(0);
		setIaddr(0);
		setOaddr(0);
		setOaddr1(0);
		setOaddr2(0);
		setCmdStatus(0);
		setInterrupt(0);
		unknown200 = 0;
		unknown400 = 0;
		for (int cmd = 0x00; cmd <= 0xFF; cmd++) {
			writeGeCmd(cmd, 0);
		}
		for (int i = 0; i < 8 * 12; i++) {
			writeGeBone(i, 0);
		}
		for (int i = 0; i < 12; i++) {
			writeGeWorld(i, 0);
		}
		for (int i = 0; i < 12; i++) {
			writeGeView(i, 0);
		}
		for (int i = 0; i < 16; i++) {
			writeGeProjection(i, 0);
		}
		for (int i = 0; i < 12; i++) {
			writeGeTexture(i, 0);
		}
		// Setting the ctrl must be the last action as it might trigger a GE list execution
		setCtrl(0);
	}

	public void onGeInterrupt() {
		checkInterrupt();
		sceDisplayModule.setGeDirty(true);
	}

	private int getStatus() {
		if (ExternalGE.isActive()) {
			status = NativeUtils.getCoreStat();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			status = VideoEngine.getInstance().lleGetCoreStat();
		}
		return status;
	}

	private void setStatus(int status) {
		this.status = status;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreStat(status);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreStat(status);
		}
	}

	private int getList() {
		if (ExternalGE.isActive()) {
			list = NativeUtils.getCoreMadr();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			list = VideoEngine.getInstance().lleGetCoreMadr();
		}
		return list;
	}

	private void setList(int list) {
		this.list = list;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreMadr(list);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreMadr(list);
		}
	}

	private int getStall() {
		if (ExternalGE.isActive()) {
			stall = NativeUtils.getCoreSadr();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			stall = VideoEngine.getInstance().lleGetCoreSadr();
		}
		return stall;
	}

	private void setStall(int stall) {
		this.stall = stall;
		Modules.sceGe_userModule.onMMIOGeStallAddressChanged(stall);
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreSadr(stall);
			CoreThreadMMIO.getInstance().sync();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreSadr(stall);
		}
	}

	private int getRaddr1() {
		if (ExternalGE.isActive()) {
			raddr1 = NativeUtils.getCoreRadr1();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			raddr1 = VideoEngine.getInstance().lleGetCoreRadr1();
		}
		return raddr1;
	}

	private void setRaddr1(int raddr1) {
		this.raddr1 = raddr1;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreRadr1(raddr1);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreRadr1(raddr1);
		}
	}

	private int getRaddr2() {
		if (ExternalGE.isActive()) {
			raddr2 = NativeUtils.getCoreRadr2();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			raddr2 = VideoEngine.getInstance().lleGetCoreRadr2();
		}
		return raddr2;
	}

	private void setRaddr2(int raddr2) {
		this.raddr2 = raddr2;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreRadr2(raddr2);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreRadr2(raddr2);
		}
	}

	private int getVaddr() {
		if (ExternalGE.isActive()) {
			vaddr = NativeUtils.getCoreVadr();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			vaddr = VideoEngine.getInstance().lleGetCoreVadr();
		}
		return vaddr;
	}

	private void setVaddr(int vaddr) {
		this.vaddr = vaddr;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreVadr(vaddr);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreVadr(vaddr);
		}
	}

	private int getIaddr() {
		if (ExternalGE.isActive()) {
			iaddr = NativeUtils.getCoreIadr();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			iaddr = VideoEngine.getInstance().lleGetCoreIadr();
		}
		return iaddr;
	}

	private void setIaddr(int iaddr) {
		this.iaddr = iaddr;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreIadr(iaddr);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreIadr(iaddr);
		}
	}

	private int getOaddr() {
		if (ExternalGE.isActive()) {
			oaddr = NativeUtils.getCoreOadr();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			oaddr = VideoEngine.getInstance().lleGetCoreOadr();
		}
		return oaddr;
	}

	private void setOaddr(int oaddr) {
		this.oaddr = oaddr;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreOadr(oaddr);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreOadr(oaddr);
		}
	}

	private int getOaddr1() {
		if (ExternalGE.isActive()) {
			oaddr1 = NativeUtils.getCoreOadr1();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			oaddr1 = VideoEngine.getInstance().lleGetCoreOadr1();
		}
		return oaddr1;
	}

	private void setOaddr1(int oaddr1) {
		this.oaddr1 = oaddr1;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreOadr1(oaddr1);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreOadr1(oaddr1);
		}
	}

	private int getOaddr2() {
		if (ExternalGE.isActive()) {
			oaddr2 = NativeUtils.getCoreOadr2();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			oaddr2 = VideoEngine.getInstance().lleGetCoreOadr2();
		}
		return oaddr2;
	}

	private void setOaddr2(int oaddr2) {
		this.oaddr2 = oaddr2;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreOadr2(oaddr2);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreOadr2(oaddr2);
		}
	}

	private int getCmdStatus() {
		if (ExternalGE.isActive()) {
			cmdStatus = NativeUtils.getCoreIntrStat();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			cmdStatus = VideoEngine.getInstance().lleGetCoreIntrStat();
		}
		return cmdStatus;
	}

	private void setCmdStatus(int cmdStatus) {
		this.cmdStatus = cmdStatus;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreIntrStat(cmdStatus);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreIntrStat(cmdStatus);
		}

		// Clearing some flags from cmdStatus is also clearing the related interrupt flags
		setInterrupt(getInterrupt() & cmdStatus);
	}

	private void changeCmdStatus(int mask) {
		setCmdStatus(cmdStatus ^ mask);
	}

	// All methods reading/writing the interrupt field are synchronized as they can be called from multiple threads
	private synchronized int getInterrupt() {
		if (ExternalGE.isActive()) {
			interrupt = NativeUtils.getCoreInterrupt();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			interrupt = VideoEngine.getInstance().lleGetCoreInterrupt();
		}
		return interrupt;
	}

	// All methods reading/writing the interrupt field are synchronized as they can be called from multiple threads
	private synchronized void checkInterrupt() {
		if ((getInterrupt() & INTR_STAT_END) == 0) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_GE_INTR);
		} else {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_GE_INTR);
		}
	}

	// All methods reading/writing the interrupt field are synchronized as they can be called from multiple threads
	private synchronized void setInterrupt(int interrupt) {
		this.interrupt = interrupt;
		if (ExternalGE.isActive()) {
			NativeUtils.setCoreInterrupt(interrupt);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreInterrupt(interrupt);
		}

		checkInterrupt();
	}

	// All methods reading/writing the interrupt field are synchronized as they can be called from multiple threads
	private synchronized void changeInterrupt(int mask) {
		setInterrupt(getInterrupt() ^ mask);
	}

	// All methods reading/writing the interrupt field are synchronized as they can be called from multiple threads
	private synchronized void clearInterrupt(int mask) {
		setInterrupt(getInterrupt() & ~mask);
	}

	private void startGeList() {
		if (ExternalGE.isActive()) {
			NativeUtils.setLogLevel();
			// Update the screen scale only at the start of a new list
			NativeUtils.setScreenScale(ExternalGE.getScreenScale());
			CoreThreadMMIO.getInstance().sync();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleStartGeList();
		}
	}

	private void stopGeList() {
		if (ExternalGE.isActive()) {
			// TODO Implement LLE stopGeList() for the external software renderer
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleStopGeList();
		}
	}

	private int getCtrl() {
		if (ExternalGE.isActive()) {
			ctrl = NativeUtils.getCoreCtrl();
		} else if (VideoEngine.getInstance().lleIsActive()) {
			ctrl = VideoEngine.getInstance().lleGetCoreCtrl();
		}
		return ctrl;
	}

	private void setCtrl(int ctrl) {
		int oldCtrl = this.ctrl;
		this.ctrl = ctrl;

		if (ExternalGE.isActive()) {
			NativeUtils.setCoreCtrl(ctrl);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCoreCtrl(ctrl);
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
		int value = 0;
		if (ExternalGE.isActive()) {
			value = ExternalGE.getCmd(cmd);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			value = VideoEngine.getInstance().lleGetCmd(cmd);
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("readGeCmd(%s)=0x%08X", GeCommands.getInstance().getCommandString(cmd), value));
		}

		return value;
	}

	private void writeGeCmd(int cmd, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setCmd(cmd, value);
			if (GeCommands.pureStateCommands[cmd]) {
				ExternalGE.interpretCmd(cmd, value);
			}
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetCmd(cmd, value);
			if (GeCommands.pureStateCommands[cmd]) {
				VideoEngine.getInstance().lleInterpretCmd(cmd, value);
			}
		}
	}

	private int readMatrix(int matrixType, int matrixIndex) {
		float[] matrix = null;
		if (ExternalGE.isActive()) {
			 matrix = ExternalGE.getMatrix(matrixType);
		} else if (VideoEngine.getInstance().lleIsActive()) {
			matrix = VideoEngine.getInstance().lleGetMatrix(matrixType);
		}

		if (matrix == null || matrixIndex < 0 || matrixIndex >= matrix.length) {
			return 0;
		}

		int value = Float.floatToRawIntBits(matrix[matrixIndex]) >>> 8;

		if (log.isDebugEnabled()) {
			log.debug(String.format("readMatrix(matrixType=%d, matrixIndex=%d)=0x%X(%f)", matrixType, matrixIndex, value, matrix[matrixIndex]));
		}

		return value;
	}

	private int readGeBone(int bone) {
		return readMatrix(sceGe_user.PSP_GE_MATRIX_BONE0 + (bone / 12), bone % 12);
	}

	private void writeGeBone(int bone, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setMatrix(sceGe_user.PSP_GE_MATRIX_BONE0 + (bone / 12), bone %12, Float.intBitsToFloat(value << 8));
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetMatrix(sceGe_user.PSP_GE_MATRIX_BONE0 + (bone / 12), bone %12, Float.intBitsToFloat(value << 8));
		}
	}

	private int readGeWorld(int world) {
		return readMatrix(sceGe_user.PSP_GE_MATRIX_WORLD, world);
	}

	private void writeGeWorld(int world, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setMatrix(sceGe_user.PSP_GE_MATRIX_WORLD, world, Float.intBitsToFloat(value << 8));
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetMatrix(sceGe_user.PSP_GE_MATRIX_WORLD, world, Float.intBitsToFloat(value << 8));
		}
	}

	private int readGeView(int view) {
		return readMatrix(sceGe_user.PSP_GE_MATRIX_VIEW, view);
	}

	private void writeGeView(int view, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setMatrix(sceGe_user.PSP_GE_MATRIX_VIEW, view, Float.intBitsToFloat(value << 8));
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetMatrix(sceGe_user.PSP_GE_MATRIX_VIEW, view, Float.intBitsToFloat(value << 8));
		}
	}

	private int readGeProjection(int projection) {
		return readMatrix(sceGe_user.PSP_GE_MATRIX_PROJECTION, projection);
	}

	private void writeGeProjection(int projection, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setMatrix(sceGe_user.PSP_GE_MATRIX_PROJECTION, projection, Float.intBitsToFloat(value << 8));
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetMatrix(sceGe_user.PSP_GE_MATRIX_PROJECTION, projection, Float.intBitsToFloat(value << 8));
		}
	}

	private int readGeTexture(int texture) {
		return readMatrix(sceGe_user.PSP_GE_MATRIX_TEXGEN, texture);
	}

	private void writeGeTexture(int texture, int value) {
		if (ExternalGE.isActive()) {
			ExternalGE.setMatrix(sceGe_user.PSP_GE_MATRIX_TEXGEN, texture, Float.intBitsToFloat(value << 8));
		} else if (VideoEngine.getInstance().lleIsActive()) {
			VideoEngine.getInstance().lleSetMatrix(sceGe_user.PSP_GE_MATRIX_TEXGEN, texture, Float.intBitsToFloat(value << 8));
		}
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
			case 0x200: value = unknown200; break;
			case 0x304: value = getCmdStatus(); break;
			case 0x308: value = getInterrupt(); break;
			case 0x400: value = unknown400; break;
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
			case 0x200: unknown200 = value; break;
			case 0x304: setCmdStatus(value); break;
			case 0x308: clearInterrupt(value); break;
			case 0x30C: changeInterrupt(value); break;
			case 0x310: changeCmdStatus(value); break;
			case 0x400: unknown400 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerGe ctrl=0x%X, status=0x%X, list=0x%08X, interrupt=0x%X, cmdStatus=0x%X", ctrl, status, list, interrupt, cmdStatus);
	}
}

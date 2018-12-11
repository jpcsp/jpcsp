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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MECODEC_INTR;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.ExceptionManager;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.hardware.MemoryStick;
import jpcsp.hardware.Usb;
import jpcsp.mediaengine.MEProcessor;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerSystemControl extends MMIOHandlerBase {
	public static Logger log = Logger.getLogger("systemcontrol");
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBC100000;
	public static final int SYSREG_RESET_TOP       = 0;
	public static final int SYSREG_RESET_SC        = 1;
	public static final int SYSREG_RESET_ME        = 2;
	public static final int SYSREG_RESET_AW        = 3;
	public static final int SYSREG_RESET_VME       = 4;
	public static final int SYSREG_RESET_AVC       = 5;
	public static final int SYSREG_RESET_USB       = 6;
	public static final int SYSREG_RESET_ATA       = 7;
	public static final int SYSREG_RESET_MSIF0     = 8;
	public static final int SYSREG_RESET_MSIF1     = 9;
	public static final int SYSREG_RESET_KIRK      = 10;
	public static final int SYSREG_RESET_ATA_HDD   = 12;
	public static final int SYSREG_RESET_USB_HOST  = 13;
	public static final int SYSREG_RESET_UNKNOWN0  = 14;
	public static final int SYSREG_RESET_UNKNOWN1  = 15;
	public static final int SYSREG_BUSCLK_ME       = 0;
	public static final int SYSREG_BUSCLK_AWA      = 1;
	public static final int SYSREG_BUSCLK_AWB      = 2;
	public static final int SYSREG_BUSCLK_EDRAM    = 3;
	public static final int SYSREG_BUSCLK_DMACPLUS = 4;
	public static final int SYSREG_BUSCLK_DMAC0    = 5;
	public static final int SYSREG_BUSCLK_DMAC1    = 6;
	public static final int SYSREG_BUSCLK_KIRK     = 7;
	public static final int SYSREG_BUSCLK_ATA      = 8;
	public static final int SYSREG_BUSCLK_USB      = 9;
	public static final int SYSREG_BUSCLK_MSIF0    = 10;
	public static final int SYSREG_BUSCLK_MSIF1    = 11;
	public static final int SYSREG_BUSCLK_EMCDDR   = 12;
	public static final int SYSREG_BUSCLK_EMCSM    = 13;
	public static final int SYSREG_BUSCLK_APB      = 14;
	public static final int SYSREG_BUSCLK_AUDIO0   = 15;
	public static final int SYSREG_BUSCLK_AUDIO1   = 16;
	public static final int SYSREG_CLK1_ATA        = 0;
	public static final int SYSREG_CLK1_USB        = 4;
	public static final int SYSREG_CLK1_MSIF0      = 8;
	public static final int SYSREG_CLK1_MSIF1      = 9;
	public static final int SYSREG_CLK_SPI0        = 0;
	public static final int SYSREG_CLK_SPI1        = 1;
	public static final int SYSREG_CLK_SPI2        = 2;
	public static final int SYSREG_CLK_SPI3        = 3;
	public static final int SYSREG_CLK_SPI4        = 4;
	public static final int SYSREG_CLK_SPI5        = 5;
	public static final int SYSREG_CLK_UART0       = 6;
	public static final int SYSREG_CLK_UART1       = 7;
	public static final int SYSREG_CLK_UART2       = 8;
	public static final int SYSREG_CLK_UART3       = 9;
	public static final int SYSREG_CLK_UART4       = 10;
	public static final int SYSREG_CLK_UART5       = 11;
	public static final int SYSREG_CLK_APB_TIMER0  = 12;
	public static final int SYSREG_CLK_APB_TIMER1  = 13;
	public static final int SYSREG_CLK_APB_TIMER2  = 14;
	public static final int SYSREG_CLK_APB_TIMER3  = 15;
	public static final int SYSREG_CLK_AUDIO0      = 16;
	public static final int SYSREG_CLK_AUDIO1      = 17;
	public static final int SYSREG_CLK_UNKNOWN0    = 18;
	public static final int SYSREG_CLK_UNKNOWN1    = 19;
	public static final int SYSREG_CLK_UNKNOWN2    = 20;
	public static final int SYSREG_CLK_UNKNOWN3    = 21;
	public static final int SYSREG_CLK_SIRCS       = 22;
	public static final int SYSREG_CLK_GPIO        = 23;
	public static final int SYSREG_CLK_AUDIO_CLKOUT= 24;
	public static final int SYSREG_CLK_UNKNOWN4    = 25;
	public static final int SYSREG_IO_EMCSM        = 1;
	public static final int SYSREG_IO_USB          = 2;
	public static final int SYSREG_IO_ATA          = 3;
	public static final int SYSREG_IO_MSIF0        = 4;
	public static final int SYSREG_IO_MSIF1        = 5;
	public static final int SYSREG_IO_LCDC         = 6;
	public static final int SYSREG_IO_AUDIO0       = 7;
	public static final int SYSREG_IO_AUDIO1       = 8;
	public static final int SYSREG_IO_IIC          = 9;
	public static final int SYSREG_IO_SIRCS        = 10;
	public static final int SYSREG_IO_UNK          = 11;
	public static final int SYSREG_IO_KEY          = 12;
	public static final int SYSREG_IO_PWM          = 13;
	public static final int SYSREG_IO_UART0        = 16;
	public static final int SYSREG_IO_UART1        = 17;
	public static final int SYSREG_IO_UART2        = 18;
	public static final int SYSREG_IO_UART3        = 19;
	public static final int SYSREG_IO_UART4        = 20;
	public static final int SYSREG_IO_UART5        = 21;
	public static final int SYSREG_IO_SPI0         = 24;
	public static final int SYSREG_IO_SPI1         = 25;
	public static final int SYSREG_IO_SPI2         = 26;
	public static final int SYSREG_IO_SPI3         = 27;
	public static final int SYSREG_IO_SPI4         = 28;
	public static final int SYSREG_IO_SPI5         = 29;
	public static final int SYSREG_AVC_POWER       = 2;
	public static final int SYSREG_USBMS_USB_CONNECTED      = 0x000001;
	public static final int SYSREG_USBMS_USB_INTERRUPT1     = 1;
	public static final int SYSREG_USBMS_USB_INTERRUPT2     = 2;
	public static final int SYSREG_USBMS_USB_INTERRUPT3     = 3;
	public static final int SYSREG_USBMS_USB_INTERRUPT4     = 4;
	public static final int SYSREG_USBMS_USB_INTERRUPT_MASK = 0xF << SYSREG_USBMS_USB_INTERRUPT1;
	public static final int SYSREG_USBMS_MS0_CONNECTED      = 0x000100;
	public static final int SYSREG_USBMS_MS0_INTERRUPT_MASK = 0x001E00;
	public static final int SYSREG_USBMS_MS1_CONNECTED      = 0x010000;
	public static final int SYSREG_USBMS_MS1_INTERRUPT_MASK = 0x1E0000;
	public static final int RAM_SIZE_16MB = 0;
	public static final int RAM_SIZE_32MB = 1;
	public static final int RAM_SIZE_64MB = 2;
	public static final int RAM_SIZE_128MB = 3;
	private static MMIOHandlerSystemControl instance;
	private int resetDevices;
	private int busClockDevices;
	private int clock1Devices;
	private int clockDevices;
	private int ioDevices;
	private int ramSize;
	private int tachyonVersion;
	private int usbAndMemoryStick;
	private int avcPower;
	private int interrupts;
	private int pllFrequency;
	private int spiClkSelect;
	private int gpioEnable;
	private int ataClkSelect;
	private int unknown00;
	private int unknown3C;
	private int unknown60;
	private int unknown6C;
	private int unknown74;

	public static MMIOHandlerSystemControl getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerSystemControl(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerSystemControl(int baseAddress) {
		super(baseAddress);

		ramSize = RAM_SIZE_16MB;
		tachyonVersion = Modules.sceSysregModule.sceSysregGetTachyonVersion();

		if (MemoryStick.isInserted()) {
			usbAndMemoryStick |= SYSREG_USBMS_MS0_CONNECTED;
		}

		if (Usb.isCableConnected()) {
			usbAndMemoryStick |= SYSREG_USBMS_USB_CONNECTED;
			resetDevices |= 1 << SYSREG_RESET_USB;
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		resetDevices = stream.readInt();
		busClockDevices = stream.readInt();
		clock1Devices = stream.readInt();
		clockDevices = stream.readInt();
		ioDevices = stream.readInt();
		ramSize = stream.readInt();
		tachyonVersion = stream.readInt();
		usbAndMemoryStick = stream.readInt();
		avcPower = stream.readInt();
		interrupts = stream.readInt();
		pllFrequency = stream.readInt();
		spiClkSelect = stream.readInt();
		gpioEnable = stream.readInt();
		ataClkSelect = stream.readInt();
		unknown00 = stream.readInt();
		unknown3C = stream.readInt();
		unknown60 = stream.readInt();
		unknown6C = stream.readInt();
		unknown74 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(resetDevices);
		stream.writeInt(busClockDevices);
		stream.writeInt(clock1Devices);
		stream.writeInt(clockDevices);
		stream.writeInt(ioDevices);
		stream.writeInt(ramSize);
		stream.writeInt(tachyonVersion);
		stream.writeInt(usbAndMemoryStick);
		stream.writeInt(avcPower);
		stream.writeInt(interrupts);
		stream.writeInt(pllFrequency);
		stream.writeInt(spiClkSelect);
		stream.writeInt(gpioEnable);
		stream.writeInt(ataClkSelect);
		stream.writeInt(unknown00);
		stream.writeInt(unknown3C);
		stream.writeInt(unknown60);
		stream.writeInt(unknown6C);
		stream.writeInt(unknown74);
		super.write(stream);
	}

	private static String getResetDeviceName(int bit) {
		switch (bit) {
			case SYSREG_RESET_TOP     : return "TOP";
			case SYSREG_RESET_SC      : return "SC";
			case SYSREG_RESET_ME      : return "ME";
			case SYSREG_RESET_AW      : return "AW";
			case SYSREG_RESET_VME     : return "VME";
			case SYSREG_RESET_AVC     : return "AVC";
			case SYSREG_RESET_USB     : return "USB";
			case SYSREG_RESET_ATA     : return "ATA";
			case SYSREG_RESET_MSIF0   : return "MSIF0";
			case SYSREG_RESET_MSIF1   : return "MSIF1";
			case SYSREG_RESET_KIRK    : return "KIRK";
			case SYSREG_RESET_ATA_HDD : return "ATA_HDD";
			case SYSREG_RESET_USB_HOST: return "USB_HOST";
			case SYSREG_RESET_UNKNOWN0: return "UNKNOWN0";
			case SYSREG_RESET_UNKNOWN1: return "UNKNOWN1";
		}
		return String.format("SYSREG_RESET_%02X", bit);
	}

	private static String getBusClockDeviceName(int bit) {
		switch (bit) {
			case SYSREG_BUSCLK_ME      : return "ME";
			case SYSREG_BUSCLK_AWA     : return "AWA";
			case SYSREG_BUSCLK_AWB     : return "AWB";
			case SYSREG_BUSCLK_EDRAM   : return "EDRAM";
			case SYSREG_BUSCLK_DMACPLUS: return "DMACPLUS";
			case SYSREG_BUSCLK_DMAC0   : return "DMAC0";
			case SYSREG_BUSCLK_DMAC1   : return "DMAC1";
			case SYSREG_BUSCLK_KIRK    : return "KIRK";
			case SYSREG_BUSCLK_ATA     : return "ATA";
			case SYSREG_BUSCLK_USB     : return "USB";
			case SYSREG_BUSCLK_MSIF0   : return "MSIF0";
			case SYSREG_BUSCLK_MSIF1   : return "MSIF1";
			case SYSREG_BUSCLK_EMCDDR  : return "EMCDDR";
			case SYSREG_BUSCLK_EMCSM   : return "EMCSM";
			case SYSREG_BUSCLK_APB     : return "APB";
			case SYSREG_BUSCLK_AUDIO0  : return "AUDIO0";
			case SYSREG_BUSCLK_AUDIO1  : return "AUDIO1";
		}
		return String.format("SYSREG_BUSCLK_%02X", bit);
	}

	private static String getClock1DeviceName(int bit) {
		switch (bit) {
			case SYSREG_CLK1_ATA  : return "ATA";
			case SYSREG_CLK1_USB  : return "USB";
			case SYSREG_CLK1_MSIF0: return "MSIF0";
			case SYSREG_CLK1_MSIF1: return "MSIF1";
		}
		return String.format("SYSREG_CLK1_%02X", bit);
	}

	private static String getClockDeviceName(int bit) {
		switch (bit) {
			case SYSREG_CLK_SPI0        : return "SPI0";
			case SYSREG_CLK_SPI1        : return "SPI1";
			case SYSREG_CLK_SPI2        : return "SPI2";
			case SYSREG_CLK_SPI3        : return "SPI3";
			case SYSREG_CLK_SPI4        : return "SPI4";
			case SYSREG_CLK_SPI5        : return "SPI5";
			case SYSREG_CLK_UART0       : return "UART0";
			case SYSREG_CLK_UART1       : return "UART1";
			case SYSREG_CLK_UART2       : return "UART2";
			case SYSREG_CLK_UART3       : return "UART3";
			case SYSREG_CLK_UART4       : return "UART4";
			case SYSREG_CLK_UART5       : return "UART5";
			case SYSREG_CLK_APB_TIMER0  : return "APB_TIMIER0";
			case SYSREG_CLK_APB_TIMER1  : return "APB_TIMIER1";
			case SYSREG_CLK_APB_TIMER2  : return "APB_TIMIER2";
			case SYSREG_CLK_APB_TIMER3  : return "APB_TIMIER2";
			case SYSREG_CLK_AUDIO0      : return "AUDIO0";
			case SYSREG_CLK_AUDIO1      : return "AUDIO1";
			case SYSREG_CLK_UNKNOWN0    : return "UNKNOWN0";
			case SYSREG_CLK_UNKNOWN1    : return "UNKNOWN1";
			case SYSREG_CLK_UNKNOWN2    : return "UNKNOWN2";
			case SYSREG_CLK_UNKNOWN3    : return "UNKNOWN3";
			case SYSREG_CLK_SIRCS       : return "SIRCS";
			case SYSREG_CLK_GPIO        : return "GPIO";
			case SYSREG_CLK_AUDIO_CLKOUT: return "AUDIO_CLKOUT";
			case SYSREG_CLK_UNKNOWN4    : return "UNKNOWN4";
		}
		return String.format("SYSREG_CLK_%02X", bit);
	}

	private static String getIoDeviceName(int bit) {
		switch (bit) {
			case SYSREG_IO_EMCSM : return "EMCSM";
			case SYSREG_IO_USB   : return "USB";
			case SYSREG_IO_ATA   : return "ATA";
			case SYSREG_IO_MSIF0 : return "MSIF0";
			case SYSREG_IO_MSIF1 : return "MSIF1";
			case SYSREG_IO_LCDC  : return "LCDC";
			case SYSREG_IO_AUDIO0: return "AUDIO0";
			case SYSREG_IO_AUDIO1: return "AUDIO1";
			case SYSREG_IO_IIC   : return "IIC";
			case SYSREG_IO_SIRCS : return "SIRCS";
			case SYSREG_IO_UNK   : return "UNK";
			case SYSREG_IO_KEY   : return "KEY";
			case SYSREG_IO_PWM   : return "PWM";
			case SYSREG_IO_UART0 : return "UART0";
			case SYSREG_IO_UART1 : return "UART1";
			case SYSREG_IO_UART2 : return "UART2";
			case SYSREG_IO_UART3 : return "UART3";
			case SYSREG_IO_UART4 : return "UART4";
			case SYSREG_IO_UART5 : return "UART5";
			case SYSREG_IO_SPI0  : return "SPI0";
			case SYSREG_IO_SPI1  : return "SPI1";
			case SYSREG_IO_SPI2  : return "SPI2";
			case SYSREG_IO_SPI3  : return "SPI3";
			case SYSREG_IO_SPI4  : return "SPI4";
			case SYSREG_IO_SPI5  : return "SPI5";
		}
		return String.format("SYSREG_IO_%02X", bit);
	}

	private void sysregInterruptToOther(int value) {
		if (value != 0) {
			if (RuntimeContextLLE.isMainCpu()) {
				// Interrupt from the main cpu to the Media Engine cpu
				if (log.isDebugEnabled()) {
					log.debug(String.format("sysregInterruptToOther to ME on %s", MMIOHandlerMeCore.getInstance().toString()));
				}
				RuntimeContextLLE.triggerInterrupt(RuntimeContextLLE.getMediaEngineProcessor(), PSP_MECODEC_INTR);
				RuntimeContextLLE.getMediaEngineProcessor().triggerException(ExceptionManager.IP2);
			} else {
				// Interrupt from the Media Engine cpu to the main cpu
				if (log.isDebugEnabled()) {
					log.debug(String.format("sysregInterruptToOther from ME on %s", MMIOHandlerMeCore.getInstance().toString()));
				}
				MMIOHandlerMeCore.getInstance().hleCompleteMeCommand();
				RuntimeContextLLE.triggerInterrupt(RuntimeContextLLE.getMainProcessor(), PSP_MECODEC_INTR);
			}
		}
	}

	private boolean isFalling(int oldFlag, int newFlag, int bit) {
		int mask = 1 << bit;
		return (oldFlag & mask) > (newFlag & mask);
	}

	private boolean hasBit(int value, int bit) {
		return (value & (1 << bit)) != 0;
	}

	private void setResetDevices(int value) {
		int oldResetDevices = resetDevices;
		resetDevices = value;

		if (isFalling(oldResetDevices, resetDevices, SYSREG_RESET_ME)) {
			MEProcessor.getInstance().triggerReset();
		}
		if (isFalling(oldResetDevices, resetDevices, SYSREG_RESET_USB)) {
			MMIOHandlerUsb.getInstance().triggerReset();
		}
	}

	private void setBusClockDevices(int value) {
		busClockDevices = value;
	}

	private void setClock1Devices(int value) {
		clock1Devices = value;
	}

	private void setClockDevices(int value) {
		clockDevices = value;
	}

	private void setIoDevices(int value) {
		ioDevices = value;
	}

	private void setRamSize(int value) {
		ramSize = value & 0x3;
	}

	public void triggerUsbMemoryStickInterrupt(int bit) {
		usbAndMemoryStick |= 1 << bit;
	}

	private void clearUsbMemoryStick(int mask) {
		int oldUsbAndMemoryStick = usbAndMemoryStick;
		mask &= SYSREG_USBMS_USB_INTERRUPT_MASK | SYSREG_USBMS_MS0_INTERRUPT_MASK | SYSREG_USBMS_MS1_INTERRUPT_MASK;
		usbAndMemoryStick &= ~mask;

		if (isFalling(oldUsbAndMemoryStick, usbAndMemoryStick, SYSREG_USBMS_USB_INTERRUPT1)) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), IntrManager.PSP_USB_58);
		}
		if (isFalling(oldUsbAndMemoryStick, usbAndMemoryStick, SYSREG_USBMS_USB_INTERRUPT2)) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), IntrManager.PSP_USB_59);
		}
		if (isFalling(oldUsbAndMemoryStick, usbAndMemoryStick, SYSREG_USBMS_USB_INTERRUPT3)) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), IntrManager.PSP_USB_57);
		}
		if (isFalling(oldUsbAndMemoryStick, usbAndMemoryStick, SYSREG_USBMS_USB_INTERRUPT4)) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), IntrManager.PSP_USB_56);
		}
	}

	private void setAvcPower(int value) {
		avcPower = value;

		if (hasBit(avcPower, SYSREG_AVC_POWER)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("MMIOHandlerSystemControl.setAvcPower enabling Avc power"));
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("MMIOHandlerSystemControl.setAvcPower disabling Avc power"));
			}
		}

		// Only bit SYSREG_AVC_POWER is known
		if ((value & ~(1 << SYSREG_AVC_POWER)) != 0) {
			log.error(String.format("MMIOHandlerSystemControl.setAvcPower unknown value 0x%X", value));
		}
	}

	private void clearInterrupts(int mask) {
		interrupts &= ~mask;
	}

	private void setPllFrequency(int value) {
		if ((value & 0x80) != 0) {
			pllFrequency = value & 0xF;
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = unknown00; break;
			case 0x3C: value = unknown3C; break;
			case 0x40: value = (tachyonVersion << 8) | ramSize; break;
			case 0x4C: value = resetDevices; break;
			case 0x50: value = busClockDevices; break;
			case 0x54: value = clock1Devices; break;
			case 0x58: value = clockDevices; break;
			case 0x5C: value = ataClkSelect; break;
			case 0x60: value = unknown60; break;
			case 0x64: value = spiClkSelect; break;
			case 0x68: value = pllFrequency; break;
			case 0x6C: value = unknown6C; break;
			case 0x70: value = avcPower; break;
			case 0x74: value = unknown74; break;
			case 0x78: value = ioDevices; break;
			case 0x7C: value = gpioEnable; break;
			case 0x80: value = usbAndMemoryStick; break;
			case 0x90: value = (int) Modules.sceSysregModule.sceSysregGetFuseId(); break;
			case 0x94: value = (int) (Modules.sceSysregModule.sceSysregGetFuseId() >> 32); break;
			case 0x98: value = Modules.sceSysregModule.sceSysregGetFuseConfig(); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: unknown00 = value; break;
			case 0x04: clearInterrupts(value); break;
			case 0x3C: unknown3C = value; break;
			case 0x40: setRamSize(value); break;
			case 0x44: sysregInterruptToOther(value); break;
			case 0x4C: setResetDevices(value); break;
			case 0x50: setBusClockDevices(value); break;
			case 0x5C: ataClkSelect = value; break;
			case 0x54: setClock1Devices(value); break;
			case 0x58: setClockDevices(value); break;
			case 0x60: unknown60 = value; break;
			case 0x64: spiClkSelect = value; break;
			case 0x68: setPllFrequency(value); break;
			case 0x6C: unknown6C = value; break;
			case 0x70: setAvcPower(value); break;
			case 0x74: unknown74 = value; break;
			case 0x78: setIoDevices(value); break;
			case 0x7C: gpioEnable = value; break;
			case 0x80: clearUsbMemoryStick(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	private void toString(StringBuilder sb, int bits, int type, String prefix) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(prefix);
		sb.append("[");
		boolean first = true;
		for (int bit = 0; bit < 32; bit++) {
			if (hasBit(bits, bit)) {
				if (first) {
					first = false;
				} else {
					sb.append("|");
				}
				switch (type) {
					case 0: sb.append(getResetDeviceName(bit)); break;
					case 1: sb.append(getBusClockDeviceName(bit)); break;
					case 2: sb.append(getClock1DeviceName(bit)); break;
					case 3: sb.append(getClockDeviceName(bit)); break;
					case 4: sb.append(getIoDeviceName(bit)); break;
				}
			}
		}
		sb.append("]");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, resetDevices, 0, "resetDevices");
		toString(sb, busClockDevices, 1, "busClockDevices");
		toString(sb, clock1Devices, 2, "clock1Devices");
		toString(sb, clockDevices, 3, "clockDevices");
		toString(sb, ioDevices, 4, "ioDevices");

		sb.append(String.format(", USB[connected=%b, interrupt=0x%01X]", (usbAndMemoryStick & SYSREG_USBMS_USB_CONNECTED) != 0, (usbAndMemoryStick & SYSREG_USBMS_USB_INTERRUPT_MASK) >> 1));
		sb.append(String.format(", MemoryStick0[connected=%b, interrupt=0x%01X]", (usbAndMemoryStick & SYSREG_USBMS_MS0_CONNECTED) != 0, (usbAndMemoryStick & SYSREG_USBMS_USB_INTERRUPT_MASK) >> 9));
		sb.append(String.format(", MemoryStick1[connected=%b, interrupt=0x%01X]", (usbAndMemoryStick & SYSREG_USBMS_MS1_CONNECTED) != 0, (usbAndMemoryStick & SYSREG_USBMS_USB_INTERRUPT_MASK) >> 17));
		sb.append(String.format(", interrupts=0x%X", interrupts));
		sb.append(String.format(", pllFrequency=0x%X", pllFrequency));

		return sb.toString();
	}
}

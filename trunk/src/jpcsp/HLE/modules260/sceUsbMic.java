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
package jpcsp.HLE.modules260;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspUsbMicInputInitExParam;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceUsbMic extends HLEModule {
	public static Logger log = Modules.getLogger("sceUsbMic");

	@Override
	public String getName() {
		return "sceUsbMic";
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x06128E42, version = 260)
	public int sceUsbMicPollInputEnd() {
		return 0;
	}

	@HLEFunction(nid = 0x2E6DCDCD, version = 260)
	public int sceUsbMicInputBlocking(int maxSamples, int frequency, TPointer buffer) {
		if (maxSamples <= 0 || (maxSamples & 0x3F) != 0) {
			return SceKernelErrors.ERROR_USBMIC_INVALID_MAX_SAMPLES;
		}

		if (frequency != 44100 && frequency != 22050 && frequency != 11025) {
			return SceKernelErrors.ERROR_USBMIC_INVALID_FREQUENCY;
		}

		return Modules.sceAudioModule.hleAudioInputBlocking(maxSamples, frequency, buffer);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x45310F07, version = 260)
	public int sceUsbMicInputInitEx(@CanBeNull pspUsbMicInputInitExParam param) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5F7F368D, version = 260)
	public int sceUsbMicInput() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x63400E20, version = 260)
	public int sceUsbMicGetInputLength() {
		return Modules.sceAudioModule.hleAudioGetInputLength();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB8E536EB, version = 260)
	public int sceUsbMicInputInit(int unknown1, int inputVolume, int unknown2) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF899001C, version = 260)
	public int sceUsbMicWaitInputEnd() {
		return 0;
	}
}

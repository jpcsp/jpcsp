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
package jpcsp.HLE.modules150;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.util.Utilities;

@HLELogging
public class sceAudiocodec extends HLEModule {
	public static Logger log = Modules.getLogger("sceAudiocodec");

	public static final int PSP_CODEC_AT3PLUS = 0x00001000;
	public static final int PSP_CODEC_AT3 = 0x00001001;
	public static final int PSP_CODEC_MP3 = 0x00001002;
	public static final int PSP_CODEC_AAC = 0x00001003;

	@Override
	public String getName() {
		return "sceAudiocodec";
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D3F790C, version = 150)
	public int sceAudiocodecCheckNeedMem(TPointer workArea, int codecType) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5B37EB1D, version = 150)
	public int sceAudiocodecInit(TPointer workArea, int codecType) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x70A703F8, version = 150)
	public int sceAudiocodecDecode(TPointer workArea, int codecType) {
		int inputBuffer = workArea.getValue32(24);
		int outputBuffer = workArea.getValue32(32);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAudiocodecDecode inputBuffer=0x%08X, outputBuffer=0x%08X", inputBuffer, outputBuffer));
			if (log.isTraceEnabled()) {
				log.trace(String.format("sceAudiocodecDecode inputBuffer: %s", Utilities.getMemoryDump(inputBuffer, 0x180)));
			}
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8ACA11D5, version = 150)
	public int sceAudiocodecGetInfo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x59176A0F, version = 150)
	public int sceAudiocodecAlcExtendParameter() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x3A20A200, version = 150)
	public int sceAudiocodecGetEDRAM(TPointer workArea, int codecType) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x29681260, version = 150)
	public int sceAudiocodecReleaseEDRAM(TPointer workArea) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6CD2A861, version = 150)
	public int sceAudiocodec_6CD2A861() {
		return 0;
	}
}

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
package jpcsp.memory.mmio.syscon;

import static jpcsp.nec78k0.Nec78k0Processor.SFR_ADDRESS;

import org.apache.log4j.Logger;

import jpcsp.nec78k0.Nec78k0Memory;

/**
 * NEC 78k0 Memory map used by the Syscon firmware:
 *   - [0x0000..0xFEDF]: RAM
 *   - [0xFEE0..0xFEE7]: Register address banks 3
 *   - [0xFEE8..0xFEEF]: Register address banks 2
 *   - [0xFEF0..0xFEF7]: Register address banks 1
 *   - [0xFEF8..0xFEFF]: Register address banks 0
 *   - [0xFF00..0xFFFF]: Special Function Registers (SFR)
 *   - [0xFF1C..0xFF1D]: Register SP
 *   - [0xFF1E..0xFF1E]: Register PSW
 *
 * @author gid15
 *
 */
public class SysconMemory extends Nec78k0Memory {
	public SysconMemory(Logger log) {
		super(log, new MMIOHandlerSysconFirmwareSfr(SFR_ADDRESS), BASE_RAM0, SIZE_RAM0);
	}
}

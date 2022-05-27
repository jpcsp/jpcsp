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
package jpcsp.format.rco.type;

import jpcsp.format.rco.RCOContext;
import jpcsp.format.rco.vsmx.VSMX;

import org.apache.log4j.Logger;

public class BaseType {
	protected static final Logger log = VSMX.log;
	protected int value;

	public int size() {
		return 4;
	}

	protected int read8(RCOContext context) {
		return context.buffer[context.offset++] & 0xFF;
	}

	protected int read16(RCOContext context) {
		return read8(context) | (read8(context) << 8);
	}

	protected int read32(RCOContext context) {
		return read16(context) | (read16(context) << 16);
	}

	public void read(RCOContext context) {
		value = read32(context);
	}

	public void init(RCOContext context) {
	}

	public int getIntValue() {
		return value;
	}

	public void setIntValue(int value) {
		this.value = value;
	}

	public float getFloatValue() {
		return (float) value;
	}

	public void setFloatValue(float value) {
		this.value = (int) value;
	}

	@Override
	public String toString() {
		return String.format("value=0x%X", getIntValue());
	}
}

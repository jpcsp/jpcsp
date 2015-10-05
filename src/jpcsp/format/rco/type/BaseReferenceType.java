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

public class BaseReferenceType extends BaseType {
	protected static final int REFERENCE_TYPE_NONE = 0xFFFF;
	protected static final int REFERENCE_TYPE_EVENT = 0x400;
	protected static final int REFERENCE_TYPE_IMAGE = 0x402;
	protected static final int REFERENCE_TYPE_OBJECT = 0x407;
	public int referenceType;
	public int unknownShort;

	@Override
	public int size() {
		return 8;
	}

	@Override
	public void read(RCOContext context) {
		referenceType = read16(context);
		unknownShort = read16(context);

		super.read(context);
	}

	private static String getReferenceTypeString(int referenceType) {
		switch (referenceType) {
			case REFERENCE_TYPE_NONE: return "NONE";
			case REFERENCE_TYPE_EVENT: return "EVENT";
			case REFERENCE_TYPE_IMAGE: return "IMAGE";
			case REFERENCE_TYPE_OBJECT: return "OBJECT";
		}

		return "UNKNOWN";
	}

	@Override
	public String toString() {
		return String.format("referenceType=0x%X(%s), short1=0x%X, value=0x%X", referenceType, getReferenceTypeString(referenceType), unknownShort, value);
	}
}

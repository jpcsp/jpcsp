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

import java.awt.image.BufferedImage;

import jpcsp.format.rco.RCOContext;
import jpcsp.format.rco.object.BaseObject;
import jpcsp.format.rco.object.BasePositionObject;

public class BaseReferenceType extends BaseType {
	protected static final int REFERENCE_TYPE_NONE = 0xFFFF;
	protected static final int REFERENCE_TYPE_EVENT = 0x400;
	protected static final int REFERENCE_TYPE_TEXT = 0x401;
	protected static final int REFERENCE_TYPE_IMAGE = 0x402;
	protected static final int REFERENCE_TYPE_MODEL = 0x403;
	protected static final int REFERENCE_TYPE_FONT = 0x405;
	protected static final int REFERENCE_TYPE_OBJECT = 0x407;
	protected static final int REFERENCE_TYPE_ANIM = 0x408;
	protected static final int REFERENCE_TYPE_POSITION_OBJECT = 0x409;
	public int referenceType;
	public int unknownShort;
	protected String event;
	protected BaseObject object;
	protected BufferedImage image;

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

	@Override
	public void init(RCOContext context) {
		switch (referenceType) {
			case REFERENCE_TYPE_NONE:
				break;
			case REFERENCE_TYPE_EVENT:
				event = context.events.get(value);
				break;
			case REFERENCE_TYPE_OBJECT:
			case REFERENCE_TYPE_POSITION_OBJECT:
			case REFERENCE_TYPE_ANIM:
				object = context.objects.get(value);
				break;
			case REFERENCE_TYPE_IMAGE:
				image = context.images.get(value);
				break;
			default:
				log.warn(String.format("BaseReferenceType: unknown referenceType 0x%X(%s)", referenceType, getReferenceTypeString(referenceType)));
				break;
		}
		super.init(context);
	}

	private static String getReferenceTypeString(int referenceType) {
		switch (referenceType) {
			case REFERENCE_TYPE_NONE: return "NONE";
			case REFERENCE_TYPE_EVENT: return "EVENT";
			case REFERENCE_TYPE_TEXT: return "TEXT";
			case REFERENCE_TYPE_IMAGE: return "IMAGE";
			case REFERENCE_TYPE_MODEL: return "MODEL";
			case REFERENCE_TYPE_FONT: return "FONT";
			case REFERENCE_TYPE_OBJECT: return "OBJECT";
			case REFERENCE_TYPE_ANIM: return "ANIM";
			case REFERENCE_TYPE_POSITION_OBJECT: return "POSITION_OBJECT";
		}

		return "UNKNOWN";
	}

	public String getEvent() {
		return event;
	}

	public BaseObject getObject() {
		return object;
	}

	public BasePositionObject getPositionObject() {
		if (object instanceof BasePositionObject) {
			return (BasePositionObject) object;
		}

		return null;
	}

	public BufferedImage getImage() {
		return image;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("referenceType=0x%X(%s), short1=0x%X, value=0x%X", referenceType, getReferenceTypeString(referenceType), unknownShort, value));
		if (event != null) {
			s.append(String.format(", event='%s'", event));
		}
		if (object != null) {
			s.append(String.format(", object='%s'", object.getName()));
		}
		if (image != null) {
			s.append(String.format(", image=%dx%d", image.getWidth(), image.getHeight()));
		}

		return s.toString();
	}
}

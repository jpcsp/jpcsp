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
package jpcsp.format.rco.object;

import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.type.EventType;
import jpcsp.format.rco.type.FloatType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;

public class BasePositionObject extends BaseObject {
	@ObjectField(order = 101)
	public FloatType posX;
	@ObjectField(order = 102)
	public FloatType posY;
	@ObjectField(order = 103)
	public FloatType posZ;
	@ObjectField(order = 104)
	public FloatType redScale;
	@ObjectField(order = 105)
	public FloatType greenScale;
	@ObjectField(order = 106)
	public FloatType blueScale;
	@ObjectField(order = 107)
	public FloatType alphaScale;
	@ObjectField(order = 108)
	public FloatType width;
	@ObjectField(order = 109)
	public FloatType height;
	@ObjectField(order = 110)
	public FloatType depth;
	@ObjectField(order = 111)
	public FloatType scaleWidth;
	@ObjectField(order = 112)
	public FloatType scaleHeight;
	@ObjectField(order = 113)
	public FloatType scaleDepth;
	@ObjectField(order = 114)
	public IntType iconOffset;
	@ObjectField(order = 115)
	public EventType onInit;

	public void setPos(VSMXBaseObject object, VSMXBaseObject posX, VSMXBaseObject posY) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPos(%s, %s)", posX, posY));
		}
		this.posX.setFloatValue(posX.getFloatValue());
		this.posY.setFloatValue(posY.getFloatValue());
	}

	public void setPos(VSMXBaseObject object, VSMXBaseObject posX, VSMXBaseObject posY, VSMXBaseObject posZ) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPos(%s, %s, %s)", posX, posY, posZ));
		}
		this.posX.setFloatValue(posX.getFloatValue());
		this.posY.setFloatValue(posY.getFloatValue());
		this.posZ.setFloatValue(posZ.getFloatValue());
	}

	public void setRotate(VSMXBaseObject object, VSMXBaseObject x, VSMXBaseObject y, VSMXBaseObject rotationRads) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setRotate(%s, %s, %s)", x, y, rotationRads));
		}
	}

	public VSMXBaseObject getColor(VSMXBaseObject object) {
		VSMXInterpreter interpreter = object.getInterpreter();
		VSMXArray color = new VSMXArray(interpreter, 4);
		color.setPropertyValue(0, new VSMXNumber(interpreter, redScale.getFloatValue()));
		color.setPropertyValue(1, new VSMXNumber(interpreter, greenScale.getFloatValue()));
		color.setPropertyValue(2, new VSMXNumber(interpreter, blueScale.getFloatValue()));
		color.setPropertyValue(3, new VSMXNumber(interpreter, alphaScale.getFloatValue()));

		if (log.isDebugEnabled()) {
			log.debug(String.format("getColor() returning %s", color));
		}

		return color;
	}

	public void setColor(VSMXBaseObject object, VSMXBaseObject red, VSMXBaseObject green, VSMXBaseObject blue, VSMXBaseObject alpha) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColor(%s, %s, %s, %s)", red, green, blue, alpha));
		}

		redScale.setFloatValue(red.getFloatValue());
		greenScale.setFloatValue(green.getFloatValue());
		blueScale.setFloatValue(blue.getFloatValue());
		alphaScale.setFloatValue(alpha.getFloatValue());
	}

	public void animColor(VSMXBaseObject object, VSMXBaseObject red, VSMXBaseObject green, VSMXBaseObject blue, VSMXBaseObject alpha, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animColor(%s, %s, %s, %s, %s)", red, green, blue, alpha, duration));
		}
	}

	public void setScale(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height, VSMXBaseObject depth) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setScale(%s, %s, %s)", width, height, depth));
		}
	}

	public void animScale(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height, VSMXBaseObject depth, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animScale(%s, %s, %s, %s)", width, height, depth, duration));
		}
	}
}

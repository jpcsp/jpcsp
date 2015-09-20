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
}

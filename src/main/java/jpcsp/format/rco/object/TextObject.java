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
import jpcsp.format.rco.type.FloatType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.type.RefType;
import jpcsp.format.rco.type.TextType;
import jpcsp.format.rco.type.UnknownType;

public class TextObject extends BasePositionObject {
	@ObjectField(order = 201)
	public TextType text;
	@ObjectField(order = 202)
	public RefType unknownRef18;
	@ObjectField(order = 203)
	public IntType unknownInt20;
	@ObjectField(order = 204)
	public IntType unknownInt21;
	@ObjectField(order = 205)
	public FloatType size;
	@ObjectField(order = 206)
	public FloatType topRed;
	@ObjectField(order = 207)
	public FloatType topGreen;
	@ObjectField(order = 208)
	public FloatType topBlue;
	@ObjectField(order = 209)
	public FloatType bottomRed;
	@ObjectField(order = 210)
	public FloatType bottomGreen;
	@ObjectField(order = 211)
	public FloatType bottomBlue;
	@ObjectField(order = 212)
	public FloatType spacingHorizontal;
	@ObjectField(order = 213)
	public UnknownType unknown30;
	@ObjectField(order = 214)
	public UnknownType unknown31;
	@ObjectField(order = 215)
	public UnknownType unknown32;
	@ObjectField(order = 216)
	public FloatType spacingVertical;
	@ObjectField(order = 217)
	public FloatType shadowX;
	@ObjectField(order = 218)
	public FloatType shadowY;
	@ObjectField(order = 219)
	public FloatType shadowPerspective;
	@ObjectField(order = 220)
	public FloatType shadowRed;
	@ObjectField(order = 221)
	public FloatType shadowGreen;
	@ObjectField(order = 222)
	public FloatType shadowBlue;
	@ObjectField(order = 223)
	public FloatType shadowAlpha;
	@ObjectField(order = 224)
	public UnknownType unknown41;
	@ObjectField(order = 225)
	public UnknownType unknown42;
	@ObjectField(order = 226)
	public UnknownType unknown43;
	@ObjectField(order = 227)
	public FloatType unknownFloat44;
	@ObjectField(order = 228)
	public FloatType unknownFloat45;
	@ObjectField(order = 229)
	public FloatType unknownFloat46;
	@ObjectField(order = 230)
	public FloatType unknownFloat47;
	@ObjectField(order = 231)
	public UnknownType unknown48;
}

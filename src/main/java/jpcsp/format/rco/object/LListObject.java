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
import jpcsp.format.rco.type.RefType;
import jpcsp.format.rco.type.UnknownType;

public class LListObject extends BasePositionObject {
	@ObjectField(order = 201)
	public IntType unknownInt16;
	@ObjectField(order = 202)
	public UnknownType unknown17;
	@ObjectField(order = 203)
	public FloatType unknownFloat18;
	@ObjectField(order = 204)
	public RefType unknownRef19;
	@ObjectField(order = 205)
	public RefType unknownRef21;
	@ObjectField(order = 206)
	public RefType unknownRef23;
	@ObjectField(order = 207)
	public RefType unknownRef25;
	@ObjectField(order = 208)
	public RefType unknownRef27;
	@ObjectField(order = 209)
	public RefType unknownRef29;
	@ObjectField(order = 210)
	public EventType unknownEvent31;
	@ObjectField(order = 211)
	public RefType unknownRef33;
}

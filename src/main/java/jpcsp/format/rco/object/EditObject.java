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
import jpcsp.format.rco.type.ObjectType;
import jpcsp.format.rco.type.RefType;
import jpcsp.format.rco.type.UnknownType;

public class EditObject extends BasePositionObject {
	@ObjectField(order = 201)
	public UnknownType unknown16;
	@ObjectField(order = 202)
	public UnknownType unknown17;
	@ObjectField(order = 203)
	public UnknownType unknown18;
	@ObjectField(order = 204)
	public UnknownType unknown19;
	@ObjectField(order = 205)
	public RefType unknownRef20;
	@ObjectField(order = 206)
	public RefType unknownRef22;
	@ObjectField(order = 207)
	public EventType unknownEvent24;
	@ObjectField(order = 208)
	public RefType unknownRef26;
	@ObjectField(order = 209)
	public RefType unknownRef28;
	@ObjectField(order = 210)
	public RefType unknownRef30;
	@ObjectField(order = 211)
	public EventType unknownEvent32;
	@ObjectField(order = 212)
	public EventType unknownEvent34;
	@ObjectField(order = 213)
	public ObjectType unknownObject36;
	@ObjectField(order = 214)
	public ObjectType unknownObject38;
	@ObjectField(order = 215)
	public RefType unknownRef40;
}

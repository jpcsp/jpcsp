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
import jpcsp.format.rco.type.IntType;

public class XListObject extends BasePositionObject {
	@ObjectField(order = 201)
	public IntType unknownInt16;
	@ObjectField(order = 202)
	public EventType onCursorMove;
	@ObjectField(order = 203)
	public EventType onScrollIn;
	@ObjectField(order = 204)
	public EventType onScrollOut;
	@ObjectField(order = 205)
	public EventType onPush;
	@ObjectField(order = 206)
	public EventType onContextMenu;
	@ObjectField(order = 207)
	public EventType onFocusLeft;
	@ObjectField(order = 208)
	public EventType onFocusRight;
}

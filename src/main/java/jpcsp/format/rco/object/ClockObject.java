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
import jpcsp.format.rco.type.TextType;

public class ClockObject extends BasePositionObject {
	@ObjectField(order = 201)
	public IntType unknownInt16;
	@ObjectField(order = 202)
	public FloatType unknownFloat17;
	@ObjectField(order = 203)
	public TextType unknownText18;
	@ObjectField(order = 204)
	public TextType unknownText20;
	@ObjectField(order = 205)
	public RefType unknownRef22;
	@ObjectField(order = 206)
	public RefType unknownRef24;
	@ObjectField(order = 207)
	public EventType onDecide;
	@ObjectField(order = 208)
	public EventType onCancel;
	@ObjectField(order = 209)
	public EventType onFocusIn;
	@ObjectField(order = 210)
	public EventType onFocusOut;
	@ObjectField(order = 211)
	public EventType onFocusLeft;
	@ObjectField(order = 212)
	public EventType onFocusRight;
	@ObjectField(order = 213)
	public EventType onFocusUp;
	@ObjectField(order = 214)
	public EventType onFocusDown;
	@ObjectField(order = 215)
	public EventType onDigitalSwitch;
}

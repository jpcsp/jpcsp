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
import jpcsp.format.rco.type.RefType;
import jpcsp.format.rco.type.UnknownType;

public class ProgressObject extends BasePositionObject {
	@ObjectField(order = 201)
	public FloatType unknownFloat16;
	@ObjectField(order = 202)
	public UnknownType unknown17;
	@ObjectField(order = 203)
	public RefType unknownRef18;
	@ObjectField(order = 204)
	public RefType unknownRef20;
	@ObjectField(order = 205)
	public RefType unknownRef22;
}

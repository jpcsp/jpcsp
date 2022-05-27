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
package jpcsp.format.rco.anim;

import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.type.FloatType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.type.ObjectType;

public class SlideOutAnim extends BaseAnim {
	@ObjectField(order = 1)
	public ObjectType ref;
	@ObjectField(order = 2)
	public FloatType duration;
	@ObjectField(order = 3)
	public IntType accelMode;
	@ObjectField(order = 4)
	public FloatType xspeed;
	@ObjectField(order = 5)
	public FloatType yspeed;
	@ObjectField(order = 6)
	public FloatType xcompress;
	@ObjectField(order = 7)
	public FloatType ycompress;
}

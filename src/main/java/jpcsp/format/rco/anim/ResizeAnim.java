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
import jpcsp.format.rco.object.BasePositionObject;
import jpcsp.format.rco.type.FloatType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.type.ObjectType;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;

public class ResizeAnim extends BaseAnim {
	@ObjectField(order = 1)
	public ObjectType ref;
	@ObjectField(order = 2)
	public FloatType duration;
	@ObjectField(order = 3)
	public IntType accelMode;
	@ObjectField(order = 4)
	public FloatType width;
	@ObjectField(order = 5)
	public FloatType height;
	@ObjectField(order = 6)
	public FloatType depth;

	private class ResizeAnimAction extends AbstractAnimAction {
		private BasePositionObject positionObject;
		private float startWidth;
		private float startHeight;
		private float startDepth;

		public ResizeAnimAction(int duration, BasePositionObject positionObject) {
			super(duration);
			this.positionObject = positionObject;

			startWidth = positionObject.scaleWidth.getFloatValue();
			startHeight = positionObject.scaleHeight.getFloatValue();
			startDepth = positionObject.scaleDepth.getFloatValue();
		}

		@Override
		protected void anim(float step) {
			positionObject.scaleWidth.setFloatValue(startWidth + width.getFloatValue() * step);
			positionObject.scaleHeight.setFloatValue(startHeight + height.getFloatValue() * step);
			positionObject.scaleDepth.setFloatValue(startDepth + depth.getFloatValue() * step);

			positionObject.onDisplayUpdated();

			if (log.isDebugEnabled()) {
				log.debug(String.format("ResizeAnim '%s' from (%f,%f,%f) to (%f,%f,%f)", positionObject.getName(), startWidth, startHeight, startDepth, positionObject.scaleWidth.getFloatValue(), positionObject.scaleHeight.getFloatValue(), positionObject.scaleDepth.getFloatValue()));
			}
		}
	}

	@Override
	protected long doPlayReference(BasePositionObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("ResizeAnim play %s on %s", toString(), object));
		}

		getScheduler().addAction(new ResizeAnimAction(duration.getIntValue(), object));

		return 0;
	}

	@Override
	protected long doPlay(VSMXBaseObject object) {
		return doPlayReference(ref);
	}
}

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
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.objects.BaseNativeObject;

public class RecolourAnim extends BaseAnim {
	@ObjectField(order = 1)
	public ObjectType ref;
	@ObjectField(order = 2)
	public FloatType duration;
	@ObjectField(order = 3)
	public IntType accelMode;
	@ObjectField(order = 4)
	public FloatType red;
	@ObjectField(order = 5)
	public FloatType green;
	@ObjectField(order = 6)
	public FloatType blue;
	@ObjectField(order = 7)
	public FloatType alpha;

	private class RecolourAnimAction extends AbstractAnimAction {
		private BasePositionObject positionObject;
		private float startRed;
		private float startGreen;
		private float startBlue;
		private float startAlpha;

		public RecolourAnimAction(int duration, BasePositionObject positionObject) {
			super(duration);
			this.positionObject = positionObject;
			startRed   = positionObject.animRed;
			startGreen = positionObject.animGreen;
			startBlue  = positionObject.animBlue;
			startAlpha = positionObject.animAlpha;
		}

		@Override
		protected void anim(float step) {
			positionObject.animRed   = interpolate(startRed  , red.getFloatValue()  , step);
			positionObject.animGreen = interpolate(startGreen, green.getFloatValue(), step);
			positionObject.animBlue  = interpolate(startBlue , blue.getFloatValue() , step);
			positionObject.animAlpha = interpolate(startAlpha, alpha.getFloatValue(), step);

			if (log.isDebugEnabled()) {
				log.debug(String.format("RecolourAnimn '%s' from (%f,%f,%f,%f) to (%f,%f,%f,%f)", positionObject.getName(), startRed, startGreen, startBlue, startAlpha, positionObject.animRed, positionObject.animGreen, positionObject.animBlue, positionObject.animAlpha));
			}
		}
	}

	@Override
	protected long doPlayReference(BasePositionObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("RecolourAnim play %s on %s", toString(), object));
		}

		getScheduler().addAction(new RecolourAnimAction(duration.getIntValue(), object));

		return 0;
	}

	@Override
	protected long doPlay(VSMXBaseObject object) {
		if (object instanceof VSMXNativeObject) {
			VSMXNativeObject nativeObject = (VSMXNativeObject) object;
			BaseNativeObject baseNativeObject = nativeObject.getObject();
			if (baseNativeObject instanceof BasePositionObject) {
				return doPlayReference((BasePositionObject) baseNativeObject);
			}
		}
		return super.doPlay(object);
	}
}

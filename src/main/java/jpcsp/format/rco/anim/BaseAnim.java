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

import java.util.List;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.format.rco.object.BaseObject;
import jpcsp.format.rco.object.BasePositionObject;
import jpcsp.format.rco.type.ObjectType;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.objects.BaseNativeObject;
import jpcsp.format.rco.vsmx.objects.Resource;
import jpcsp.scheduler.Scheduler;

public class BaseAnim extends BaseObject {
	private static class PlayAnimAction implements IAction {
		private BaseAnim[] children;
		private int index;
		private int length;
		private VSMXBaseObject object;

		public PlayAnimAction(BaseAnim[] children, int index, int length, VSMXBaseObject object) {
			this.children = children;
			this.index = index;
			this.length = length;
			this.object = object;
		}

		@Override
		public void execute() {
			while (index < length) {
				long delay = children[index++].doPlay(object);
				if (delay > 0) {
					getScheduler().addAction(Scheduler.getNow() + delay * 1000, this);
					return;
				}
			}
		}
	}

	protected long doPlayReference(BasePositionObject object) {
		return 0;
	}

	protected long doPlayReference(BaseNativeObject object) {
		if (object instanceof BasePositionObject) {
			return doPlayReference((BasePositionObject) object);
		}

		return 0;
	}

	private long doPlayReference(VSMXBaseObject object) {
		if (object instanceof VSMXNativeObject) {
			return doPlayReference(((VSMXNativeObject) object).getObject());
		}

		return 0;
	}

	protected long doPlayReference(ObjectType ref) {
		BasePositionObject positionObject = ref.getPositionObject();
		if (positionObject == null) {
			return 0;
		}

		VSMXBaseObject object = positionObject.getObject();
		long delay = doPlayReference(object);
		if (object.hasPropertyValue(Resource.childrenName)) {
			VSMXBaseObject children = object.getPropertyValue(Resource.childrenName);
			List<String> names = children.getPropertyNames();
			if (names != null) {
				for (String name : names) {
					VSMXBaseObject child = children.getPropertyValue(name);
					delay = Math.max(delay, doPlayReference(child));
				}
			}
		}

		return delay;
	}

	protected long doPlay(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("BaseAnim play on %s", object));
		}

		return 0;
	}

	public void play(VSMXBaseObject thisObject, VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("BaseAnim.play %s, %s", thisObject, object));
		}

		if (thisObject.hasPropertyValue(Resource.childrenName)) {
			VSMXBaseObject children = thisObject.getPropertyValue(Resource.childrenName);
			List<String> names = children.getPropertyNames();
			BaseAnim baseAnims[] = new BaseAnim[names.size() + 1];
			int numberBaseAnims = 0;
			baseAnims[numberBaseAnims++] = this;
			for (String name : names) {
				VSMXBaseObject child = children.getPropertyValue(name);
				if (child instanceof VSMXNativeObject) {
					BaseNativeObject baseNativeObject = ((VSMXNativeObject) child).getObject();
					if (baseNativeObject instanceof BaseAnim) {
						baseAnims[numberBaseAnims++] = (BaseAnim) baseNativeObject;
					}
				}
			}

			if (numberBaseAnims > 0) {
				getScheduler().addAction(new PlayAnimAction(baseAnims, 0, numberBaseAnims, object));
			}
		}
	}
}

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
package jpcsp.format.rco;

import jpcsp.format.RCO;
import jpcsp.format.rco.anim.Anim;
import jpcsp.format.rco.anim.BaseAnim;
import jpcsp.format.rco.anim.DelayAnim;
import jpcsp.format.rco.anim.FadeAnim;
import jpcsp.format.rco.anim.FireEventAnim;
import jpcsp.format.rco.anim.LockAnim;
import jpcsp.format.rco.anim.MoveToAnim;
import jpcsp.format.rco.anim.RecolourAnim;
import jpcsp.format.rco.anim.ResizeAnim;
import jpcsp.format.rco.anim.RotateAnim;
import jpcsp.format.rco.anim.SlideOutAnim;
import jpcsp.format.rco.anim.UnlockAnim;

public class AnimFactory {
	public static BaseAnim newAnim(int type) {
		switch (type) {
			case  1: return new Anim();
			case  2: return new MoveToAnim();
			case  3: return new RecolourAnim();
			case  4: return new RotateAnim();
			case  5: return new ResizeAnim();
			case  6: return new FadeAnim();
			case  7: return new DelayAnim();
			case  8: return new FireEventAnim();
			case  9: return new LockAnim();
			case 10: return new UnlockAnim();
			case 11: return new SlideOutAnim();
		}

		RCO.log.warn(String.format("AnimFactory.newAnim unknown type 0x%X", type));

		return null;
	}
}

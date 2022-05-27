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
import jpcsp.format.rco.object.ActionObject;
import jpcsp.format.rco.object.BaseObject;
import jpcsp.format.rco.object.ButtonObject;
import jpcsp.format.rco.object.ClockObject;
import jpcsp.format.rco.object.EditObject;
import jpcsp.format.rco.object.GroupObject;
import jpcsp.format.rco.object.IItemObject;
import jpcsp.format.rco.object.IListObject;
import jpcsp.format.rco.object.IconObject;
import jpcsp.format.rco.object.ItemSpinObject;
import jpcsp.format.rco.object.LItemObject;
import jpcsp.format.rco.object.LListObject;
import jpcsp.format.rco.object.MItemObject;
import jpcsp.format.rco.object.MListObject;
import jpcsp.format.rco.object.ModelObject;
import jpcsp.format.rco.object.PageObject;
import jpcsp.format.rco.object.PlaneObject;
import jpcsp.format.rco.object.ProgressObject;
import jpcsp.format.rco.object.ScrollObject;
import jpcsp.format.rco.object.SpinObject;
import jpcsp.format.rco.object.TextObject;
import jpcsp.format.rco.object.UButtonObject;
import jpcsp.format.rco.object.XItemObject;
import jpcsp.format.rco.object.XListObject;
import jpcsp.format.rco.object.XMListObject;
import jpcsp.format.rco.object.XMenuObject;

public class ObjectFactory {
	public static BaseObject newObject(int type) {
		switch (type) {
			case  1: return new PageObject();
			case  2: return new PlaneObject();
			case  3: return new ButtonObject();
			case  4: return new XMenuObject();
			case  5: return new XMListObject();
			case  6: return new XListObject();
			case  7: return new ProgressObject();
			case  8: return new ScrollObject();
			case  9: return new MListObject();
			case 10: return new MItemObject();
			case 11: break; // Unknown object 11
			case 12: return new XItemObject();
			case 13: return new TextObject();
			case 14: return new ModelObject();
			case 15: return new SpinObject();
			case 16: return new ActionObject();
			case 17: return new ItemSpinObject();
			case 18: return new GroupObject();
			case 19: return new LListObject();
			case 20: return new LItemObject();
			case 21: return new EditObject();
			case 22: return new ClockObject();
			case 23: return new IListObject();
			case 24: return new IItemObject();
			case 25: return new IconObject();
			case 26: return new UButtonObject();
		}

		RCO.log.warn(String.format("ObjectFactory.newObject unknown type 0x%X", type));

		return null;
	}
}

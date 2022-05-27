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
package jpcsp.network;

import jpcsp.network.jpcsp.JpcspWlanAdapter;
import jpcsp.network.xlinkkai.XLinkKaiWlanAdapter;

/**
 * @author gid15
 *
 */
public class WlanAdapterFactory {
	public static IWlanAdapter createWlanAdapter() {
		IWlanAdapter wlanAdapter;

		if (XLinkKaiWlanAdapter.isEnabled()) {
			wlanAdapter = new XLinkKaiWlanAdapter();
		} else {
			wlanAdapter = new JpcspWlanAdapter();
		}

		// Make sure that all the methods are asynchronous and are executed without delay
		wlanAdapter = new AsyncWlanAdapter(wlanAdapter);

		return wlanAdapter;
	}
}

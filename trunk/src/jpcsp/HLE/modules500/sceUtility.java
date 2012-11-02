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
package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;

@HLELogging
public class sceUtility extends jpcsp.HLE.modules303.sceUtility {
	public static Logger log = jpcsp.HLE.modules150.sceUtility.log;

	protected UtilityDialogState storeCheckoutState;

	@Override
	public void start() {
		storeCheckoutState = new NotImplementedUtilityDialogState("sceUtilityStoreCheckout");

		super.start();
	}

	@HLEFunction(nid = 0xDA97F1AA, version = 500)
	public void sceUtilityStoreCheckoutInitStart(Processor processor) {
		storeCheckoutState.executeInitStart(processor);
	}

	@HLEFunction(nid = 0x54A5C62F, version = 500)
	public void sceUtilityStoreCheckoutShutdownStart(Processor processor) {
		storeCheckoutState.executeShutdownStart(processor);
	}

	@HLEFunction(nid = 0xB8592D5F, version = 500)
	public void sceUtilityStoreCheckoutUpdate(Processor processor) {
		storeCheckoutState.executeUpdate(processor);
	}

	@HLEFunction(nid = 0x3AAD51DC, version = 500)
	public void sceUtilityStoreCheckoutGetStatus(Processor processor) {
		storeCheckoutState.executeGetStatus(processor);
	}
}

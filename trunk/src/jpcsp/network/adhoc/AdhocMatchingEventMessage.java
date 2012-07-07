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
package jpcsp.network.adhoc;

import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING;

/**
 * @author gid15
 *
 */
public abstract class AdhocMatchingEventMessage extends AdhocMessage {
	// One of sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_xxx
	private int event;
	private MatchingObject matchingObject;

	public AdhocMatchingEventMessage(MatchingObject matchingObject, int event) {
		super();
		this.event = event;
		this.matchingObject = matchingObject;
	}

	public AdhocMatchingEventMessage(MatchingObject matchingObject, int event, int address, int length, byte[] toMacAddress) {
		super(address, length, toMacAddress);
		this.event = event;
		this.matchingObject = matchingObject;
	}

	public AdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		super(message, length);
		this.matchingObject = matchingObject;
	}

	public int getEvent() {
		return event;
	}

	protected void setEvent(int event) {
		this.event = event;
	}

	protected MatchingObject getMatchingObject() {
		return matchingObject;
	}

	public void processOnReceive(int macAddr, int optData, int optLen) {
		if (event != PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING) {
			matchingObject.notifyCallbackEvent(getEvent(), macAddr, optLen, optData);
		}
	}

	public void processOnSend(int macAddr, int optData, int optLen) {
		// Nothing to do
	}
}

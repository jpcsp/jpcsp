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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

public class RawChannel extends SelectableChannel {
	private SelectableRawSocket socket;
	private boolean blocking;
	Selector selector;
	RawSelectionKey selectionKey;

	public RawChannel() {
		socket = new SelectableRawSocket();
	}

	public SelectableRawSocket socket() {
		return socket;
	}

	@Override
	public Object blockingLock() {
		return socket;
	}

	@Override
	public SelectableChannel configureBlocking(boolean block) throws IOException {
		blocking = block;
		return this;
	}

	@Override
	public boolean isBlocking() {
		return blocking;
	}

	@Override
	public boolean isRegistered() {
		return selector != null;
	}

	@Override
	public SelectionKey keyFor(Selector sel) {
		return selectionKey;
	}

	@Override
	public SelectorProvider provider() {
		return SelectorProvider.provider();
	}

	@Override
	public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
		if (sel instanceof RawSelector) {
			RawSelector rawSelector = (RawSelector) sel;

			selector = sel;
			selectionKey = new RawSelectionKey(this, rawSelector);
			selectionKey.attach(att);
			selectionKey.interestOps(ops);

			rawSelector.register(selectionKey);
		}

		return selectionKey;
	}

	@Override
	public int validOps() {
		return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}

	@Override
	protected void implCloseChannel() throws IOException {
		socket.close();
	}

	public void onSelectorClosed() {
		selector = null;
		selectionKey = null;
	}
}

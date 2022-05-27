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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Set;

public class RawSelector extends Selector {
	Set<RawSelectionKey> rawSelectionKeys;
	Set<SelectionKey> rawSelectedKeys;

	public static RawSelector open() throws IOException {
		return new RawSelector();
	}

	protected RawSelector() throws IOException {
		rawSelectionKeys = new HashSet<RawSelectionKey>();
		rawSelectedKeys = new HashSet<SelectionKey>();
	}

	@Override
	public void close() throws IOException {
		for (SelectionKey rawSelectionKey: rawSelectionKeys) {
			((RawSelectionKey) rawSelectionKey).getRawChannel().onSelectorClosed();
		}
		rawSelectionKeys.clear();
		rawSelectedKeys.clear();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public Set<SelectionKey> keys() {
		Set<SelectionKey> setKeys = new HashSet<SelectionKey>();
		setKeys.addAll(rawSelectionKeys);
		return setKeys;
	}

	@Override
	public SelectorProvider provider() {
		return null;
	}

	@Override
	public int select() throws IOException {
		return selectNow();
	}

	@Override
	public int select(long timeout) throws IOException {
		return selectNow();
	}

	protected void selectNow(RawSelectionKey rawSelectionKey) {
		SelectableRawSocket rawSocket = rawSelectionKey.getRawChannel().socket();
		rawSelectionKey.clearReadyOps();

		if ((rawSelectionKey.interestOps() & SelectionKey.OP_READ) != 0) {
			if (rawSocket.isSelectedForRead()) {
				rawSelectionKey.addReadyOp(SelectionKey.OP_READ);
			}
		}

		if ((rawSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0) {
			if (rawSocket.isSelectedForWrite()) {
				rawSelectionKey.addReadyOp(SelectionKey.OP_WRITE);
			}
		}

		if (rawSelectionKey.readyOps() != 0) {
			rawSelectedKeys.add(rawSelectionKey);
		}
	}

	@Override
	public int selectNow() throws IOException {
		rawSelectedKeys.clear();

		for (RawSelectionKey rawSelectionKey: rawSelectionKeys) {
			selectNow(rawSelectionKey);
		}

		return rawSelectedKeys.size();
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return rawSelectedKeys;
	}

	@Override
	public Selector wakeup() {
		return this;
	}

	public void register(RawSelectionKey rawSelectionKey) {
		rawSelectionKeys.add(rawSelectionKey);
	}
}

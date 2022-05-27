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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class RawSelectionKey extends SelectionKey {
	RawChannel channel;
	RawSelector selector;
	int interestOps;
	int readyOps;

	public RawSelectionKey(RawChannel channel, RawSelector selector) {
		this.channel = channel;
		this.selector = selector;
	}

	@Override
	public void cancel() {
	}

	@Override
	public SelectableChannel channel() {
		return channel;
	}

	public RawChannel getRawChannel() {
		return channel;
	}

	@Override
	public int interestOps() {
		return interestOps;
	}

	@Override
	public SelectionKey interestOps(int ops) {
		interestOps = ops;
		return this;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public int readyOps() {
		return readyOps;
	}

	@Override
	public Selector selector() {
		return selector;
	}

	public void addReadyOp(int op) {
		readyOps |= op;
	}

	public void clearReadyOps() {
		readyOps = 0;
	}
}

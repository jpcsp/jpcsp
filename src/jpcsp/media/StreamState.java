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
package jpcsp.media;

import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules.sceMpeg;

import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;

public class StreamState {
    private static final long timestampMask = 0x7FFFFFFFFFFFFFFFL;
    private MediaEngine me;
	private int streamID;
	private List<IPacket> pendingPackets;
	private IPacket packet;
	private int offset;
	private long dts;
	private long pts;

	public StreamState(MediaEngine me, int streamID) {
		this.me = me;
		this.streamID = streamID;
		pendingPackets = new LinkedList<IPacket>();
	}

	public IPacket getPacket() {
		return packet;
	}

	public void setPacket(IPacket packet) {
		this.packet = packet;
		offset = 0;
	}

	public void releasePacket() {
		if (packet != null) {
			me.release(packet);
			packet = null;
			offset = 0;
		}
	}

	public int getStreamID() {
		return streamID;
	}

	public int getOffset() {
		return offset;
	}

	public void consume(int bytes) {
		offset += bytes;
	}

	public IPacket getNextPacket() {
		if (pendingPackets.isEmpty()) {
			return null;
		}

		return pendingPackets.remove(0);
	}

	public void addPacket(IPacket packet) {
		pendingPackets.add(packet);
	}

    private long convertTimestamp(long ts, IRational timeBase, int timestampsPerSecond) {
		// Timestamps have sometimes high bit set. Clear it
    	return Math.round((ts & timestampMask) * timeBase.getDouble() * timestampsPerSecond);
    }

	public void updateTimestamps() {
    	dts = convertTimestamp(packet.getDts(), packet.getTimeBase(), sceMpeg.mpegTimestampPerSecond);
		pts = convertTimestamp(packet.getPts(), packet.getTimeBase(), sceMpeg.mpegTimestampPerSecond);
	}

	public void getTimestamps(SceMpegAu au) {
		au.pts = pts;
		au.dts = dts;
	}

	public void finish() {
		setTimestamps(0);
		releasePacket();

		while (!pendingPackets.isEmpty()) {
			me.release(getNextPacket());
		}
	}

	public boolean isPacketEmpty() {
		return packet == null || offset >= packet.getSize();
	}

	public void incrementTimestamps(int step) {
		pts += step;
		dts += step;
	}

	public void setTimestamps(long ts) {
		pts = ts;
		dts = ts;
	}

	public boolean isStream(int streamID) {
		return getStreamID() == streamID;
	}
}

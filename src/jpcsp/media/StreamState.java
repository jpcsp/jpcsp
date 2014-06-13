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

import com.xuggle.xuggler.IContainer;
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
	private IContainer container;
	private int timestampOffset;
	private int timestampStep;
	private boolean firstTimestamp;

	public StreamState(MediaEngine me, int streamID, IContainer container, int timestampOffset, int timestampStep) {
		this.me = me;
		this.streamID = streamID;
		this.container = container;
		this.timestampOffset = timestampOffset;
		this.timestampStep = timestampStep;
		pts = timestampOffset;
		dts = timestampOffset;
		pendingPackets = new LinkedList<IPacket>();
		firstTimestamp = true;
	}

	public void setStreamID(int streamID) {
		this.streamID = streamID;
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

	public void updateTimestamps(boolean increment) {
		// If multiple picture / audio samples are stored into one packet,
		// the timestamp of the packet is only valid for the first picture / sample.
		// Further pictures /samples in the same packet have an unknown timestamp.
		if (getOffset() == 0) {
	    	// When both the pts and dts are equal to (1 << 63), assume they are unknown
			// and automatically increment the timestamp.
			if (packet.getDts() == packet.getPts() && packet.getPts() == (timestampMask + 1)) {
				if (increment) {
					if (firstTimestamp) {
						firstTimestamp = false;
					} else {
						pts += timestampStep;
						dts += timestampStep;
					}
				}
			} else {
				dts = timestampOffset + convertTimestamp(packet.getDts(), packet.getTimeBase(), sceMpeg.mpegTimestampPerSecond);
				pts = timestampOffset + convertTimestamp(packet.getPts(), packet.getTimeBase(), sceMpeg.mpegTimestampPerSecond);
			}
		} else {
			dts = sceMpeg.UNKNOWN_TIMESTAMP;
			pts = sceMpeg.UNKNOWN_TIMESTAMP;
		}
	}

	public void getTimestamps(SceMpegAu au) {
		au.pts = pts;
		au.dts = dts;
	}

	private void releasePackets() {
		releasePacket();

		while (!pendingPackets.isEmpty()) {
			me.release(getNextPacket());
		}
	}

	public void finish() {
		setTimestamps(0);
		releasePackets();
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

	public boolean isStream(IContainer container, int streamID) {
		return getContainer() == container && getStreamID() == streamID;
	}

	public IContainer getContainer() {
		return container;
	}

	public long getPts() {
		return pts;
	}

	@Override
	public String toString() {
		return String.format("StreamID=%d, Pts=%d", getStreamID(), getPts());
	}
}

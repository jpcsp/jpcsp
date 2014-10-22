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
package jpcsp.format.psmf;

/**
 * PES Header in a PSMF/MPEG file.
 * 
 * @author gid15
 *
 */
public class PesHeader {
	private long pts;
	private long dts;
	private int channel;

	public PesHeader(int channel) {
		setPts(0);
		setDts(0);
		setChannel(channel);
	}

	public PesHeader(PesHeader pesHeader) {
		pts = pesHeader.pts;
		dts = pesHeader.dts;
		channel = pesHeader.channel;
	}

	public long getPts() {
		return pts;
	}

	public void setPts(long pts) {
		this.pts = pts;
	}

	public long getDts() {
		return dts;
	}

	public void setDts(long dts) {
		this.dts = dts;
	}

	public void setDtsPts(long ts) {
		this.dts = ts;
		this.pts = ts;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	@Override
	public String toString() {
		return String.format("PesHeader(channel=%d, pts=%d, dts=%d)", getChannel(), getPts(), getDts());
	}
}

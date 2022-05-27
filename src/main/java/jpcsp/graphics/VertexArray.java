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
package jpcsp.graphics;

import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class VertexArray {
	private int id = -1;
	private int vtype;
	private VertexBuffer vertexBuffer;
	private int stride;
	private boolean pendingReload = false;

	public VertexArray(int vtype, VertexBuffer vertexBuffer, int stride) {
		this.vtype = vtype & VertexInfo.vtypeMask;
		this.vertexBuffer = vertexBuffer;
		this.stride = stride;
	}

	public boolean bind(IRenderingEngine re) {
		boolean needSetDataPointers = pendingReload;
		pendingReload = false;
		if (id == -1) {
			id = re.genVertexArray();
			needSetDataPointers = true;
		}
		re.bindVertexArray(id);

		return needSetDataPointers;
	}

	public void delete(IRenderingEngine re) {
		re.deleteVertexArray(id);
		id = -1;
	}

	public boolean isMatching(int vtype, VertexBuffer vertexBuffer, int address, int stride) {
		if (this.vertexBuffer != vertexBuffer || this.stride != stride) {
			return false;
		}

		if (this.vtype != (vtype & VertexInfo.vtypeMask)) {
			return false;
		}

		if ((vertexBuffer.getBufferOffset(address) % stride) != 0) {
			return false;
		}

		return true;
	}

	public int getVertexOffset(int address) {
		return vertexBuffer.getBufferOffset(address) / stride;
	}

	public VertexBuffer getVertexBuffer() {
		return vertexBuffer;
	}

	public void forceReload() {
		pendingReload = true;
	}

	@Override
	public String toString() {
		VertexInfo vinfo = new VertexInfo();
		vinfo.processType(vtype);
		return String.format("VertexArray[%s, stride %d, id %d, %s]", vinfo, stride, id, vertexBuffer);
	}
}

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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class VertexBufferManager {
	private static VertexBufferManager instance;
	private static final int maxSize = 1000;
	private List<VertexBuffer> vertexBuffers = new LinkedList<VertexBuffer>();
	private static final int allowedVertexGapSize = 4 * 1024;
	private CpuDurationStatistics statistics = new CpuDurationStatistics("VertexBufferManager");
	private HashMap<Integer, VertexBuffer> fastLookup = new HashMap<Integer, VertexBuffer>();

	public static VertexBufferManager getInstance() {
		if (instance == null) {
			instance = new VertexBufferManager();
		}

		return instance;
	}

	public static void exit() {
		if (instance != null) {
			if (DurationStatistics.collectStatistics) {
				VideoEngine.log.info(instance.statistics);
			}
		}
	}

	private static int getFastLookupKey(int address, int length) {
		return address;
	}

	private static int getFastLookupKey(int address, int length, int stride) {
		return address + (stride << 24);
	}

	public synchronized VertexBuffer getVertexBuffer(IRenderingEngine re, int address, int length, int stride, boolean strideAligned) {
		statistics.start();
		if (strideAligned) {
			int fastLookupKey = getFastLookupKey(address, length, stride);
			VertexBuffer vertexBuffer = fastLookup.get(fastLookupKey);
			if (vertexBuffer != null) {
				if (stride == vertexBuffer.getStride() && (vertexBuffer.getBufferOffset(address) % stride) == 0) {
					if (vertexBuffer.isAddressInside(address, length, allowedVertexGapSize)) {
						statistics.end();
						return vertexBuffer;
					}
				}
			}

			boolean first = true;
			for (ListIterator<VertexBuffer> lit = vertexBuffers.listIterator(); lit.hasNext(); ) {
				vertexBuffer = lit.next();
				if (stride == vertexBuffer.getStride() && (vertexBuffer.getBufferOffset(address) % stride) == 0) {
					if (vertexBuffer.isAddressInside(address, length, allowedVertexGapSize)) {
						if (!first) {
							// Move the VertexBuffer to the head of the list
							lit.remove();
							vertexBuffers.add(0, vertexBuffer);
						}
						fastLookup.put(fastLookupKey, vertexBuffer);
						statistics.end();

						return vertexBuffer;
					}
				}
				first = false;
			}
		} else {
			int fastLookupKey = getFastLookupKey(address, length);
			VertexBuffer vertexBuffer = fastLookup.get(fastLookupKey);
			if (vertexBuffer != null) {
				if (vertexBuffer.isAddressInside(address, length, allowedVertexGapSize)) {
					statistics.end();
					return vertexBuffer;
				}
			}

			boolean first = true;
			for (ListIterator<VertexBuffer> lit = vertexBuffers.listIterator(); lit.hasNext(); ) {
				vertexBuffer = lit.next();
				if (vertexBuffer.isAddressInside(address, length, allowedVertexGapSize)) {
					if (!first) {
						// Move the VertexBuffer to the head of the list
						lit.remove();
						vertexBuffers.add(0, vertexBuffer);
					}
					fastLookup.put(fastLookupKey, vertexBuffer);
					statistics.end();

					return vertexBuffer;
				}
				first = false;
			}
		}

		VertexBuffer vertexBuffer = new VertexBuffer(address, stride);
		vertexBuffers.add(0, vertexBuffer);

		if (vertexBuffers.size() > maxSize && re != null) {
			VertexBuffer toBeDeleted = vertexBuffers.remove(vertexBuffers.size() - 1);
			if (re.isVertexArrayAvailable()) {
				VertexArrayManager.getInstance().onVertexBufferDeleted(re, toBeDeleted);
			}
			toBeDeleted.delete(re);
		}

		statistics.end();

		return vertexBuffer;
	}

	public synchronized void forceReloadAllVertexBuffers() {
		for (VertexBuffer vertexBuffer : vertexBuffers) {
			vertexBuffer.forceReload();
		}
	}

	public synchronized void resetAddressAlreadyChecked() {
		for (VertexBuffer vertexBuffer : vertexBuffers) {
			vertexBuffer.resetAddressAlreadyChecked();
		}
	}

	protected void displayStatistics() {
		int length = 0;
		for (VertexBuffer vertexBuffer : vertexBuffers) {
			VideoEngine.log.info(vertexBuffer);
			length += vertexBuffer.getLength();
		}

		VideoEngine.log.info(String.format("VertexBufferManager: %d buffers, total length %d", vertexBuffers.size(), length));
	}

	public synchronized void reset(IRenderingEngine re) {
		for (ListIterator<VertexBuffer> lit = vertexBuffers.listIterator(); lit.hasNext(); ) {
			VertexBuffer vertexBuffer = lit.next();
			vertexBuffer.delete(re);
			lit.remove();
		}
	}
}

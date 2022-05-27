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
public class VertexArrayManager {
	private static VertexArrayManager instance;
	private static final int maxSize = 1000;
	private List<VertexArray> vertexArrays = new LinkedList<VertexArray>();
	private CpuDurationStatistics statistics = new CpuDurationStatistics("VertexArrayManager");
	private HashMap<Integer, VertexArray> fastLookup = new HashMap<Integer, VertexArray>();

	public static VertexArrayManager getInstance() {
		if (instance == null) {
			instance = new VertexArrayManager();
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

	private static int getFastLookupKey(int vtype, VertexBuffer vertexBuffer, int address, int stride) {
		return vertexBuffer.getId() + (vtype << 8);
	}

	public VertexArray getVertexArray(IRenderingEngine re, int vtype, VertexBuffer vertexBuffer, int address, int stride) {
		statistics.start();
		int fastLookupKey = getFastLookupKey(vtype, vertexBuffer, address, stride);
		VertexArray vertexArray = fastLookup.get(fastLookupKey);
		if (vertexArray != null) {
			if (vertexArray.isMatching(vtype, vertexBuffer, address, stride)) {
				statistics.end();
				return vertexArray;
			}
		}

		boolean first = true;
		for (ListIterator<VertexArray> lit = vertexArrays.listIterator(); lit.hasNext(); ) {
			vertexArray = lit.next();
			if (vertexArray.isMatching(vtype, vertexBuffer, address, stride)) {
				if (!first) {
					// Move the VertexArray to the head of the list
					lit.remove();
					vertexArrays.add(0, vertexArray);
				}
				fastLookup.put(fastLookupKey, vertexArray);
				statistics.end();

				return vertexArray;
			}
			first = false;
		}

		vertexArray = new VertexArray(vtype, vertexBuffer, stride);
		vertexArrays.add(0, vertexArray);

		if (vertexArrays.size() > maxSize) {
			VertexArray toBeDeleted = vertexArrays.remove(vertexArrays.size() - 1);
			if (toBeDeleted != null) {
				toBeDeleted.delete(re);
			}
		}

		statistics.end();

		return vertexArray;
	}

	public void onVertexBufferDeleted(IRenderingEngine re, VertexBuffer vertexBuffer) {
		// Delete all the VertexArray using the deleted VertexBuffer
		for (ListIterator<VertexArray> lit = vertexArrays.listIterator(); lit.hasNext(); ) {
			VertexArray vertexArray = lit.next();
			if (vertexArray.getVertexBuffer() == vertexBuffer) {
				lit.remove();
				vertexArray.delete(re);
			}
		}
	}

	public void forceReloadAllVertexArrays() {
		for (VertexArray vertexArray : vertexArrays) {
			vertexArray.forceReload();
		}
	}

	protected void displayStatistics() {
		for (VertexArray vertexArray : vertexArrays) {
			VideoEngine.log.info(vertexArray);
		}

		VideoEngine.log.info(String.format("VertexArrayManager: %d VAOs", vertexArrays.size()));
	}
}

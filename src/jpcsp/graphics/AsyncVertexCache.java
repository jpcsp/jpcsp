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

import java.nio.Buffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jpcsp.Memory;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class AsyncVertexCache extends VertexCache {
	AsyncVertexCacheThread asyncVertexCacheThread;
	private boolean useVertexArray = false;
	private VertexInfo vinfo = new VertexInfo();

	public static AsyncVertexCache getInstance() {
		if (instance == null) {
			instance = new AsyncVertexCache();
		}

		return (AsyncVertexCache) instance;
	}

	protected AsyncVertexCache() {
		asyncVertexCacheThread = new AsyncVertexCacheThread(this);
		asyncVertexCacheThread.setName("Async Vertex Cache Thread");
		asyncVertexCacheThread.setDaemon(true);
		asyncVertexCacheThread.start();
	}

	private void asyncLoadVertex(VertexInfo vertexInfo, AsyncEntry asyncEntry) {
		// TODO
	}

	private void asyncCheckVertex(VertexInfo vertex, VertexInfo vertexInfo, AsyncEntry asyncEntry) {
		if (!vertex.equals(vertexInfo, asyncEntry.count)) {
			setVertexAlreadyChecked(vertexInfo);
			vertex.setDirty();
			asyncLoadVertex(vertexInfo, asyncEntry);
		}
	}

	public void asyncCheck(AsyncEntry asyncEntry) {
		vinfo.processType(asyncEntry.vtype);
		vinfo.ptr_vertex = asyncEntry.vertices;
		vinfo.ptr_index = asyncEntry.indices;

		if (useVertexArray) {
			int address = vinfo.ptr_vertex;
			int length = vinfo.vertexSize * asyncEntry.count;
			VertexBuffer vertexBuffer = VertexBufferManager.getInstance().getVertexBuffer(null, address, length, vinfo.vertexSize, useVertexArray);
			if (vertexBuffer != null) {
				Buffer buffer = Memory.getInstance().getBuffer(address, length);
				vertexBuffer.preLoad(buffer, address, length);
			}
		} else {
			VertexInfo vertex = getVertex(vinfo);

			if (vertex == null) {
				asyncLoadVertex(vinfo, asyncEntry);
			} else {
				asyncCheckVertex(vertex, vinfo, asyncEntry);
			}
		}
	}

	public void addAsyncCheck(int prim, int vtype, int count, int indices, int vertices) {
		AsyncEntry asyncEntry = new AsyncEntry(prim, vtype, count, indices, vertices);
		asyncVertexCacheThread.addAsyncEntry(asyncEntry);
	}

	@Override
	public void exit() {
		super.exit();
		asyncVertexCacheThread.exit();
	}

	public void setUseVertexArray(boolean useVertexArray) {
		this.useVertexArray = useVertexArray;
	}

	private static class AsyncVertexCacheThread extends Thread {
		private AsyncVertexCache asyncVertexCache;
		private BlockingQueue<AsyncEntry> asyncEntries = new LinkedBlockingQueue<AsyncEntry>();
		private volatile boolean done = false;
		public CpuDurationStatistics statistics = new CpuDurationStatistics("Async Vertex Cache Thread");

		public AsyncVertexCacheThread(AsyncVertexCache asyncVertexCache) {
			this.asyncVertexCache = asyncVertexCache;
		}

		public void exit() {
			done = true;
			// Add a dummy entry to allow the thread to exit
			asyncEntries.add(new AsyncEntry());
			if (DurationStatistics.collectStatistics) {
				VideoEngine.log.info(statistics);
			}
		}

		@Override
		public void run() {
			while (!done) {
				try {
					AsyncEntry asyncEntry = asyncEntries.take();
					if (asyncEntry != null && !done) {
						statistics.start();
						asyncVertexCache.asyncCheck(asyncEntry);
						statistics.end();
					}
				} catch (InterruptedException e) {
					// Ignore Exception
				}
			}
		}

		public void addAsyncEntry(AsyncEntry asyncEntry) {
			asyncEntries.add(asyncEntry);
		}
	}

	private static class AsyncEntry {
		public int prim;
		public int vtype;
		public int count;
		public int indices;
		public int vertices;

		public AsyncEntry() {
		}

		public AsyncEntry(int prim, int vtype, int count, int indices, int vertices) {
			this.prim = prim;
			this.vtype = vtype;
			this.count = count;
			this.indices = indices;
			this.vertices = vertices;
		}

		@Override
		public String toString() {
			return String.format("AsyncEntry(prim=%d, vtype=0x%X, count=%d, indices=0x%08X, vertices=0x%08X", prim, vtype, count, indices, vertices);
		}
	}
}

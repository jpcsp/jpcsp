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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.util.CacheStatistics;
import jpcsp.util.DurationStatistics;

public class VertexCache {
	public static final int cacheMaxSize = 30000;
	public static final float cacheLoadFactor = 0.75f;
	protected static VertexCache instance = null;
	private LinkedHashMap<Integer, VertexInfo> cache;
	protected CacheStatistics statistics = new CacheStatistics("Vertex", cacheMaxSize);
	// Remember which vertex have already been checked during one display
	// (for applications reusing the same vertex multiple times in one display)
	private Set<Integer> vertexAlreadyChecked;

	public static VertexCache getInstance() {
		if (instance == null) {
			instance = new VertexCache();
		}

		return instance;
	}

	protected VertexCache() {
		//
		// Create a cache having
		// - initial size large enough so that no rehash will occur
		// - the LinkedList is based on access-order for LRU
		//
		cache = new LinkedHashMap<Integer, VertexInfo>((int) (cacheMaxSize / cacheLoadFactor) + 1, cacheLoadFactor, true);
		vertexAlreadyChecked = new HashSet<Integer>();
	}

	public void exit() {
		if (DurationStatistics.collectStatistics) {
			VideoEngine.log.info(statistics);
		}
	}

	private static Integer getKey(VertexInfo vertexInfo) {
		return new Integer(vertexInfo.ptr_vertex + vertexInfo.ptr_index);
	}

	public boolean hasVertex(VertexInfo vertexInfo) {
		return cache.containsKey(getKey(vertexInfo));
	}

	protected synchronized VertexInfo getVertex(VertexInfo vertexInfo) {
		return cache.get(getKey(vertexInfo));
	}

	public synchronized void addVertex(IRenderingEngine re, VertexInfo vertexInfo, int numberOfVertex, float[][] boneMatrix, int numberOfWeightsForShader) {
		Integer key = getKey(vertexInfo);
		VertexInfo previousVertex = cache.get(key);
		if (previousVertex != null) {
		    vertexInfo.reuseCachedBuffer(previousVertex);
		    previousVertex.deleteVertex(re);
		} else {
			// Check if the cache is not growing too large
			if (cache.size() >= cacheMaxSize) {
				// Remove the LRU cache entry
				Iterator<Map.Entry<Integer, VertexInfo>> it = cache.entrySet().iterator();
				if (it.hasNext()) {
					Map.Entry<Integer, VertexInfo> entry = it.next();
					entry.getValue().deleteVertex(re);
					it.remove();

					statistics.entriesRemoved++;
				}
			}
		}

		vertexInfo.prepareForCache(this, numberOfVertex, boneMatrix, numberOfWeightsForShader);
        cache.put(key, vertexInfo);

        if (cache.size() > statistics.maxSizeUsed) {
            statistics.maxSizeUsed = cache.size();
        }
	}

	public VertexInfo getVertex(VertexInfo vertexInfo, int numberOfVertex, float[][] boneMatrix, int numberOfWeightsForShader) {
		statistics.totalHits++;
		VertexInfo vertex = getVertex(vertexInfo);

		if (vertex == null) {
			statistics.notPresentHits++;
			return vertex;
		}

		if (vertex.equals(vertexInfo, numberOfVertex, boneMatrix, numberOfWeightsForShader)) {
			statistics.successfulHits++;
			return vertex;
		}

		statistics.changedHits++;
		return null;
	}

	public void resetVertexAlreadyChecked() {
		vertexAlreadyChecked.clear();
	}

	public boolean vertexAlreadyChecked(VertexInfo vertexInfo) {
		return vertexAlreadyChecked.contains(getKey(vertexInfo));
	}

	public void setVertexAlreadyChecked(VertexInfo vertexInfo) {
		vertexAlreadyChecked.add(getKey(vertexInfo));
	}
}

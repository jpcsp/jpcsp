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

import javax.media.opengl.GL;

import jpcsp.util.CacheStatistics;

public class VertexCache {
	public static final int cacheMaxSize = 30000;
	public static final float cacheLoadFactor = 0.75f;
	private static VertexCache instance = null;
	private LinkedHashMap<Integer, VertexInfo> cache;
	public CacheStatistics statistics = new CacheStatistics("Vertex", cacheMaxSize);
	// Remember which vertex have already been hashed during one display
	// (for applications reusing the same vertex multiple times in one display)
	private Set<Integer> vertexAlreadyHashed;

	public static VertexCache getInstance() {
		if (instance == null) {
			instance = new VertexCache();
		}

		return instance;
	}

	private VertexCache() {
		//
		// Create a cache having
		// - initial size large enough so that no rehash will occur
		// - the LinkedList is based on access-order for LRU
		//
		cache = new LinkedHashMap<Integer, VertexInfo>((int) (cacheMaxSize / cacheLoadFactor) + 1, cacheLoadFactor, true);
		vertexAlreadyHashed = new HashSet<Integer>();
	}

	private Integer getKey(VertexInfo vertexInfo) {
		return new Integer(vertexInfo.ptr_vertex);
	}

	public boolean hasVertex(VertexInfo vertexInfo) {
		return cache.containsKey(getKey(vertexInfo));
	}

	private VertexInfo getVertex(VertexInfo vertexInfo) {
		return cache.get(getKey(vertexInfo));
	}

	public void addVertex(GL gl, VertexInfo vertexInfo, int numberOfVertex, float[][] boneMatrix, int numberOfWeightsForShader) {
		Integer key = getKey(vertexInfo);
		VertexInfo previousVertex = cache.get(key);
		if (previousVertex != null) {
		    previousVertex.deleteVertex(gl);
		} else {
			// Check if the cache is not growing too large
			if (cache.size() >= cacheMaxSize) {
				// Remove the LRU cache entry
				Iterator<Map.Entry<Integer, VertexInfo>> it = cache.entrySet().iterator();
				if (it.hasNext()) {
					Map.Entry<Integer, VertexInfo> entry = it.next();
					entry.getValue().deleteVertex(gl);
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

	public void resetVertexAlreadyHashed() {
		vertexAlreadyHashed.clear();
	}

	public boolean vertexAlreadyHashed(VertexInfo vertexInfo) {
		return vertexAlreadyHashed.contains(getKey(vertexInfo));
	}

	public void setVertexAlreadyHashed(VertexInfo vertexInfo) {
		vertexAlreadyHashed.add(getKey(vertexInfo));
	}
}

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
package jpcsp.graphics.textures;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.util.CacheStatistics;

public class TextureCache {
	public static final int cacheMaxSize = 1000;
	public static final float cacheLoadFactor = 0.75f;
	private static Logger log = VideoEngine.log;
	private static TextureCache instance = null;
	private LinkedHashMap<Integer, Texture> cache;
	public CacheStatistics statistics = new CacheStatistics("Texture", cacheMaxSize);
	// Remember which textures have already been hashed during one display
	// (for applications reusing the same texture multiple times in one display)
	private Set<Integer> textureAlreadyHashed;
	// Remember which textures are located in VRAM. Only these textures have to be
	// scanned when checking for textures updated while rendering to GE.
	private LinkedList<Texture> vramTextures = new LinkedList<Texture>();

	public static TextureCache getInstance() {
		if (instance == null) {
			instance = new TextureCache();
		}

		return instance;
	}

	private TextureCache() {
		//
		// Create a cache having
		// - initial size large enough so that no rehash will occur
		// - the LinkedList is based on access-order for LRU
		//
		cache = new LinkedHashMap<Integer, Texture>((int) (cacheMaxSize / cacheLoadFactor) + 1, cacheLoadFactor, true);
		textureAlreadyHashed = new HashSet<Integer>();
	}

	private Integer getKey(int addr, int clutAddr) {
		// Some games use the same texture address with different cluts.
		// Keep a combination of both texture address and clut address in the cache
		return new Integer(addr + clutAddr);
	}

	public boolean hasTexture(int addr, int clutAddr) {
		return cache.containsKey(getKey(addr, clutAddr));
	}

	private Texture getTexture(int addr, int clutAddr) {
		return cache.get(getKey(addr, clutAddr));
	}

	public void addTexture(IRenderingEngine re, Texture texture) {
		Integer key = getKey(texture.getAddr(), texture.getClutAddr());
		Texture previousTexture = cache.get(key);
		if (previousTexture != null) {
		    previousTexture.deleteTexture(re);
		    vramTextures.remove(previousTexture);
		} else {
			// Check if the cache is not growing too large
			if (cache.size() >= cacheMaxSize) {
				// Remove the LRU cache entry
				Iterator<Map.Entry<Integer, Texture>> it = cache.entrySet().iterator();
				if (it.hasNext()) {
					Map.Entry<Integer, Texture> entry = it.next();
					Texture lruTexture = entry.getValue();
					lruTexture.deleteTexture(re);
					vramTextures.remove(lruTexture);
					it.remove();

					statistics.entriesRemoved++;
				}
			}
		}

        cache.put(key, texture);
        if (isVramTexture(texture)) {
        	vramTextures.add(texture);
        }

        if (cache.size() > statistics.maxSizeUsed) {
            statistics.maxSizeUsed = cache.size();
        }
	}

	public Texture getTexture(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels, boolean mipmapShareClut, short[] values16, int[] values32) {
		statistics.totalHits++;
		Texture texture = getTexture(addr, clutAddr);

		if (texture == null) {
			statistics.notPresentHits++;
			return texture;
		}

		if (texture.equals(addr, lineWidth, width, height, pixelStorage, clutAddr, clutMode, clutStart, clutShift, clutMask, clutNumBlocks, mipmapLevels, mipmapShareClut, values16, values32)) {
			statistics.successfulHits++;
			return texture;
		}

		statistics.changedHits++;
		return null;
	}

	public void resetTextureAlreadyHashed() {
		textureAlreadyHashed.clear();
	}

	public boolean textureAlreadyHashed(int addr, int clutAddr) {
		return textureAlreadyHashed.contains(getKey(addr, clutAddr));
	}

	public void setTextureAlreadyHashed(int addr, int clutAddr) {
		textureAlreadyHashed.add(getKey(addr, clutAddr));
	}

	public void resetTextureAlreadyHashed(int addr, int clutAddr) {
		textureAlreadyHashed.remove(getKey(addr, clutAddr));
	}

	public void reset(IRenderingEngine re) {
		for (Texture texture : cache.values()) {
			texture.deleteTexture(re);
		}
		cache.clear();
		resetTextureAlreadyHashed();
	}

	private boolean isVramTexture(Texture texture) {
		return Memory.isVRAM(texture.getAddr());
	}

	public void deleteVramTextures(IRenderingEngine re, int addr, int length) {
		for (ListIterator<Texture> lit = vramTextures.listIterator(); lit.hasNext(); ) {
			Texture texture = lit.next();
			if (texture.isInsideMemory(addr, addr + length)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Delete VRAM texture inside GE %s", texture.toString()));
				}
				texture.deleteTexture(re);
				lit.remove();
				Integer key = getKey(texture.getAddr(), texture.getClutAddr());
				cache.remove(key);
				statistics.entriesRemoved++;
			}
		}
	}
}

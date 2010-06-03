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
package jpcsp.util;

public class CacheStatistics {
	private String name;
	private int cacheMaxSize;
	public long totalHits = 0;			// Number of times a vertex was searched
	public long successfulHits = 0;		// Number of times a vertex was successfully found
	public long notPresentHits = 0;		// Number of times a vertex was not present
	public long changedHits = 0;		// Number of times a vertex was present but had to be discarded because it was changed
	public long entriesRemoved = 0;		// Number of times a vertex had to be removed from the cache due to the size limit
	public long maxSizeUsed = 0;		// Maximum size of the cache

	public CacheStatistics(String name, int cacheMaxSize) {
		this.name = name;
		this.cacheMaxSize = cacheMaxSize;
	}

	private String percentage(long n, long max) {
		return String.format("%.2f%%", (n / (double) max) * 100);
	}

	private String percentage(long hits) {
		return percentage(hits, totalHits);
	}

	public void reset() {
		totalHits = 0;
		successfulHits = 0;
		notPresentHits = 0;
		changedHits = 0;
		entriesRemoved = 0;
		maxSizeUsed = 0;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (name != null) {
			result.append(name);
			result.append(" ");
		}
		result.append("Cache Statistics: ");
		if (totalHits == 0) {
			result.append("Cache deactivated");
		} else {
		    result.append("TotalHits=" + totalHits + ", ");
		    result.append("SuccessfulHits=" + successfulHits + " (" + percentage(successfulHits) + "), ");
		    result.append("NotPresentHits=" + notPresentHits + " (" + percentage(notPresentHits) + "), ");
		    result.append("ChangedHits=" + changedHits + " (" + percentage(changedHits) + "), ");
		    result.append("EntriesRemoved=" + entriesRemoved + ", ");
		    result.append("MaxSizeUsed=" + maxSizeUsed + " (" + percentage(maxSizeUsed, cacheMaxSize) + ")");
		}
		return result.toString();
	}

}

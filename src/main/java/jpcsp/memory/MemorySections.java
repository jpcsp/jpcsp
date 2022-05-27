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
package jpcsp.memory;

import java.util.LinkedList;
import java.util.List;

public class MemorySections {
	private static MemorySections instance;
	private List<MemorySection> allMemorySections;
	private List<MemorySection> readMemorySections;
	private List<MemorySection> writeMemorySections;
	private List<MemorySection> executeMemorySections;

	public static MemorySections getInstance() {
		if (instance == null) {
			instance = new MemorySections();
		}
		return instance;
	}

	private MemorySections() {
		allMemorySections = new LinkedList<MemorySection>();
		readMemorySections = new LinkedList<MemorySection>();
		writeMemorySections = new LinkedList<MemorySection>();
		executeMemorySections = new LinkedList<MemorySection>();
	}

	public void reset() {
		allMemorySections.clear();
		readMemorySections.clear();
		writeMemorySections.clear();
		executeMemorySections.clear();
	}

	public void addMemorySection(MemorySection memorySection) {
		allMemorySections.add(memorySection);
		if (memorySection.canRead()) {
			readMemorySections.add(memorySection);
		}
		if (memorySection.canWrite()) {
			writeMemorySections.add(memorySection);
		}
		if (memorySection.canExecute()) {
			executeMemorySections.add(memorySection);
		}
	}

	public MemorySection getMemorySection(int address) {
		for (MemorySection memorySection: allMemorySections) {
			if (memorySection.contains(address)) {
				return memorySection;
			}
		}

		return null;
	}

	private boolean contains(List<MemorySection> memorySections, int address, boolean defaultValue) {
		for (MemorySection memorySection : memorySections) {
			if (memorySection.contains(address)) {
				return true;
			}
		}

		return defaultValue;
	}

	public boolean canRead(int address, boolean defaultValue) {
		return contains(readMemorySections, address, defaultValue);
	}

	public boolean canWrite(int address, boolean defaultValue) {
		return contains(writeMemorySections, address, defaultValue);
	}

	public boolean canExecute(int address, boolean defaultValue) {
		return contains(executeMemorySections, address, defaultValue);
	}
}

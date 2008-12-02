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
package jpcsp;

public class Clock {
	private long baseNanos;
	private long pauseNanos;
	private boolean isPaused;

	public Clock() {
		reset();
	}

	public synchronized void pause() {
		if (!isPaused) {
			pauseNanos = System.nanoTime();
			isPaused = true;
		}
	}

	public synchronized void resume() {
		if (isPaused) {
			// Do not take into account the elapsed time between pause() & resume()
			baseNanos += System.nanoTime() - pauseNanos;
			isPaused = false;
		}
	}

	public synchronized void reset() {
		baseNanos = System.nanoTime();

		// Start with a paused Clock
		pauseNanos = baseNanos;
		isPaused = true;
	}

	public synchronized long nanoTime() {
		long now;

		if (isPaused) {
			now = pauseNanos;
		} else {
			now = System.nanoTime();
		}

		return now - baseNanos;
	}

	public long milliTime() {
		return nanoTime() / 1000000;
	}

	public long microTime() {
		return nanoTime() / 1000;
	}
}

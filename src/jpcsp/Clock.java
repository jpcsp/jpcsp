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
	private long baseTimeMillis;
	private boolean isPaused;

	public Clock() {
		reset();
	}

	public Clock(Clock clock) {
		if (clock != null) {
			this.baseNanos = clock.baseNanos;
			this.pauseNanos = clock.pauseNanos;
			this.baseTimeMillis = clock.baseTimeMillis;
			this.isPaused = clock.isPaused;
		} else {
			reset();
		}
	}

	public synchronized void pause() {
		if (!isPaused) {
			pauseNanos = getSystemNanoTime();
			isPaused = true;
		}
	}

	public synchronized void resume() {
		if (isPaused) {
			// Do not take into account the elapsed time between pause() & resume()
			baseNanos += getSystemNanoTime() - pauseNanos;
			isPaused = false;
		}
	}

	public synchronized void reset() {
		baseNanos = getSystemNanoTime();
		baseTimeMillis = getSystemMilliTime();

		// Start with a paused Clock
		pauseNanos = baseNanos;
		isPaused = true;
	}

	public long nanoTime() {
		long now;

		if (isPaused) {
			now = pauseNanos;
		} else {
			now = getSystemNanoTime();
		}

		return now - baseNanos;
	}

	public long milliTime() {
		return nanoTime() / 1000000;
	}

	public long microTime() {
		return nanoTime() / 1000;
	}

	public long currentTimeMillis() {
		return baseTimeMillis + milliTime();
	}

	public TimeNanos currentTimeNanos() {
		long nanoTime = nanoTime();
		long currentTimeMillis = baseTimeMillis + (nanoTime / 1000000);

		// Be careful that subsequent calls always return ascending values
		TimeNanos timeNano = new TimeNanos();
		timeNano.nanos = (int) (nanoTime % 1000);
		timeNano.micros = (int) ((nanoTime / 1000) % 1000);
		timeNano.millis = (int) (currentTimeMillis % 1000);
		timeNano.seconds = (int) (currentTimeMillis / 1000);

		return timeNano;
	}

	protected long getSystemNanoTime() {
		return System.nanoTime();
	}

	protected long getSystemMilliTime() {
		return System.currentTimeMillis();
	}

	public static class TimeNanos {
		public int seconds;
		public int millis;
		public int micros;
		public int nanos;
	}
}

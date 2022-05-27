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

/**
 * A PSP Clock that can run slower or faster than the normal clock.
 * The speed can be changed dynamically to let an application run slower/faster.
 * 
 * @author gid15
 *
 */
public class VariableSpeedClock extends Clock {
	private int numerator;
	private int denominator;
	private long baseSystemNanoTime;
	private long baseSystemMilliTime;
	private long baseStartSystemNanoTime;
	private long baseStartSystemMilliTime;

	public VariableSpeedClock(Clock clock, int numerator, int denominator) {
		super(clock);
		setSpeed(numerator, denominator);
	}

	public void setSpeed(int numerator, int denominator) {
		if (this.denominator == 0) {
			baseStartSystemNanoTime = super.getSystemNanoTime();
			baseStartSystemMilliTime = super.getSystemMilliTime();
		} else {
			baseStartSystemNanoTime = getSystemNanoTime();
			baseStartSystemMilliTime = getSystemMilliTime();
		}

		this.numerator = numerator;
		this.denominator = denominator;

		baseSystemNanoTime = super.getSystemNanoTime();
		baseSystemMilliTime = super.getSystemMilliTime();
	}

	private long adaptToSpeed(long value) {
		return (value * numerator) / denominator;
	}

	@Override
	protected long getSystemNanoTime() {
		long systemNanoTime = super.getSystemNanoTime();
		long deltaNanoTime = systemNanoTime - baseSystemNanoTime;

		return baseStartSystemNanoTime + adaptToSpeed(deltaNanoTime);
	}

	@Override
	protected long getSystemMilliTime() {
		long systemMilliTime = super.getSystemMilliTime();
		long deltaMilliTime = systemMilliTime - baseSystemMilliTime;

		return baseStartSystemMilliTime + adaptToSpeed(deltaMilliTime);
	}
}

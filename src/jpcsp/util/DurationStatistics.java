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

public class DurationStatistics implements Comparable<DurationStatistics> {
    public String name;
    public long cumulatedTimeMillis;
    public long numberCalls;
    private long startTimeMillis;

    public DurationStatistics() {
    	reset();
    }

    public DurationStatistics(String name) {
        this.name = name;
        reset();
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
    }

    public long end() {
        long duration = System.currentTimeMillis() - startTimeMillis;
        cumulatedTimeMillis += duration;
        numberCalls++;

        return duration;
    }

    public void reset() {
    	cumulatedTimeMillis = 0;
    	numberCalls = 0;
    	startTimeMillis = 0;
    }

    @Override
	public String toString() {
        StringBuilder result = new StringBuilder();

        if (name != null) {
            result.append(name);
            result.append(": ");
        }
        result.append(numberCalls);
        result.append(" calls");
        if (numberCalls > 0) {
            result.append(" in ");
            result.append(String.format("%.3fs", cumulatedTimeMillis / 1000.0));
            result.append(" (avg=");
            double average = cumulatedTimeMillis / (1000.0 * numberCalls);
            if (average < 0.000001) {
            	result.append(String.format("%.3fus", average * 1000000));
            } else if (average < 0.001) {
            	result.append(String.format("%.3fms", average * 1000));
            } else {
            	result.append(String.format("%.3fs", average));
            }
            result.append(")");
        }

        return result.toString();
    }

    @Override
    public int compareTo(DurationStatistics o)
    {
        if (cumulatedTimeMillis < o.cumulatedTimeMillis) {
            return 1;
        } else if (cumulatedTimeMillis > o.cumulatedTimeMillis) {
            return -1;
        } else if (numberCalls < o.numberCalls) {
            return 1;
        } else if (numberCalls > o.numberCalls) {
            return -1;
        }

        return 0;
    }
}

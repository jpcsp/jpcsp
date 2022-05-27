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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class CpuDurationStatistics extends DurationStatistics {
	public long cumulatedCpuTimeNanos;
	private long startCpuTimeNanos;
	private ThreadMXBean threadMXBean;

	public CpuDurationStatistics() {
		super();
		threadMXBean = ManagementFactory.getThreadMXBean();
	}

	public CpuDurationStatistics(String name) {
		super(name);
		threadMXBean = ManagementFactory.getThreadMXBean();
	}

	public long getCpuDurationMillis() {
		return cumulatedCpuTimeNanos / 1000000L;
	}

	@Override
	public void start() {
		if (!collectStatistics) {
			return;
		}

		if (threadMXBean.isThreadCpuTimeEnabled()) {
			startCpuTimeNanos = threadMXBean.getCurrentThreadCpuTime();
		}
		super.start();
	}

	@Override
	public void end() {
		if (!collectStatistics) {
			return;
		}

		if (threadMXBean.isThreadCpuTimeEnabled()) {
			long duration = threadMXBean.getCurrentThreadCpuTime() - startCpuTimeNanos;
			cumulatedCpuTimeNanos += duration;
		}
		super.end();
	}

	@Override
	public void reset() {
		cumulatedCpuTimeNanos = 0;
		startCpuTimeNanos = 0;
		super.reset();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(super.toString());
		if (collectStatistics && numberCalls > 0 && threadMXBean.isThreadCpuTimeEnabled()) {
			result.append(String.format(" CPU %.3fs", cumulatedCpuTimeNanos / 1000000000.0));
		}

		return result.toString();
	}

    @Override
    public int compareTo(DurationStatistics o)
    {
    	if (o instanceof CpuDurationStatistics) {
        	CpuDurationStatistics cpuDurationStatistics = (CpuDurationStatistics) o;
            if (cumulatedCpuTimeNanos < cpuDurationStatistics.cumulatedCpuTimeNanos) {
                return 1;
            } else if (cumulatedCpuTimeNanos > cpuDurationStatistics.cumulatedCpuTimeNanos) {
                return -1;
            }
    	}

        return super.compareTo(o);
    }
}

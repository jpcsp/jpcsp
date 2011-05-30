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
package jpcsp.graphics.RE;

import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class RenderingEngineFactory {
	private static final boolean enableDebugProxy = false;
	private static final boolean enableStatisticsProxy = false;

	public static IRenderingEngine createRenderingEngine() {
		// Build the rendering pipeline, from the last entry to the first one.

		// The RenderingEngine actually performing the OpenGL calls
		IRenderingEngine re = RenderingEngineLwjgl.newInstance();

		if (enableStatisticsProxy && DurationStatistics.collectStatistics) {
			re = new StatisticsProxy(re);
		}

		if (enableDebugProxy) {
			re = new DebugProxy(re);
		}

		if (REShader.useShaders(re)) {
			// RenderingEngine using shaders
			re = new REShader(re);
		} else {
			// RenderingEngine using the OpenGL fixed-function pipeline (i.e. without shaders)
			re = new REFixedFunction(re);
		}

        // Proxy implementing a viewport resizing filter
		re = new ViewportFilter(re);

		// Proxy implementing a texture anisotropic filter
		re = new AnisotropicFilter(re);

		// Proxy removing redundant calls.
		// E.g. calls setting multiple times the same value,
		// or calls with an invalid parameter (e.g. for unused shader uniforms).
		re = new StateProxy(re);

		// Return the first entry in the pipeline
		return re;
	}

	/**
	 * Create a rendering engine to be used when the HLE modules have not yet
	 * been started.
	 * 
	 * @param gl
	 * @return the initial rendering engine
	 */
	public static IRenderingEngine createInitialRenderingEngine() {
		IRenderingEngine re = RenderingEngineLwjgl.newInstance();

		return re;
	}
}

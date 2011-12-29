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

import jpcsp.graphics.RE.software.RESoftware;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class RenderingEngineFactory {
	private static final boolean enableDebugProxy = false;
	private static final boolean enableStatisticsProxy = false;
	public static final boolean enableSoftwareRendering = false;

	private static IRenderingEngine createRenderingEngine(boolean enableSoftwareRendering) {
		// Build the rendering pipeline, from the last entry to the first one.
		IRenderingEngine re;

		if (enableSoftwareRendering) {
			// RenderingEngine using a complete software implementation, i.e. not using the GPU
			re = new RESoftware();
		} else {
			// RenderingEngine performing the OpenGL calls by using the LWJGL library
			re = RenderingEngineLwjgl.newInstance();
		}

		if (enableStatisticsProxy && DurationStatistics.collectStatistics) {
			// Proxy collecting statistics for all the calls (number of calls and execution time)
			re = new StatisticsProxy(re);
		}

		if (enableDebugProxy) {
			// Proxy logging the calls at the DEBUG level
			re = new DebugProxy(re);
		}

		if (!enableSoftwareRendering) {
			if (REShader.useShaders(re)) {
				// RenderingEngine using shaders
				re = new REShader(re);
			} else {
				// RenderingEngine using the OpenGL fixed-function pipeline (i.e. without shaders)
				re = new REFixedFunction(re);
			}
		}

		// Proxy removing redundant calls.
		// E.g. calls setting multiple times the same value,
		// or calls with an invalid parameter (e.g. for unused shader uniforms).
		// In the rendering pipeline, the State Proxy has to be called after
		// the Anisotropic/Viewport filters. These are modifying some parameters
		// and the State Proxy has to use the final parameter values.
		re = new StateProxy(re);

		// Proxy implementing a texture anisotropic filter
		re = new AnisotropicFilter(re);

        // Proxy implementing a viewport resizing filter
		re = new ViewportFilter(re);

		// Return the first entry in the pipeline
		return re;
	}

	/**
	 * Create a rendering engine to be used for processing the GE lists.
	 * 
	 * @return the rendering engine to be used
	 */
	public static IRenderingEngine createRenderingEngine() {
		return createRenderingEngine(enableSoftwareRendering);
	}

	/**
	 * Create a rendering engine to be used for display.
	 * This rendering engine forces the use of OpenGL and is not using the software rendering.
	 * 
	 * @return the rendering engine to be used for display
	 */
	public static IRenderingEngine createRenderingEngineForDisplay() {
		return createRenderingEngine(false);
	}

	/**
	 * Create a rendering engine to be used when the HLE modules have not yet
	 * been started.
	 * 
	 * @return the initial rendering engine
	 */
	public static IRenderingEngine createInitialRenderingEngine() {
		IRenderingEngine re = RenderingEngineLwjgl.newInstance();

		if (enableDebugProxy) {
			re = new DebugProxy(re);
		}

		return re;
	}
}

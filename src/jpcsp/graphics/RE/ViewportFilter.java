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

import jpcsp.graphics.VideoEngine;
import jpcsp.HLE.Modules;

public class ViewportFilter extends BaseRenderingEngineProxy {
    private int viewportScaleFactor = 1;
    private boolean useViewportResizeFilter;

	public ViewportFilter(IRenderingEngine proxy) {
		super(proxy);
	}

	@Override
	public void startDisplay() {
        boolean useViewportFilter = VideoEngine.getInstance().isUseViewportResizeFilter();
        int resizeFactor = VideoEngine.getInstance().getViewportResizeFilterResolution();
		useViewportResizeFilter = useViewportFilter;
        viewportScaleFactor = resizeFactor;
        if (useViewportResizeFilter) {
          Modules.sceDisplayModule.setResizeFactor(viewportScaleFactor);
        }
		super.startDisplay();
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		super.setRenderingEngine(re);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		if (useViewportResizeFilter) {
			super.setViewport(x * viewportScaleFactor, y * viewportScaleFactor,
                    width * viewportScaleFactor, height * viewportScaleFactor);
		} else {
			super.setViewport(x, y, width, height);
		}
	}

    @Override
	public void setScissor(int x, int y, int width, int height) {
		if (useViewportResizeFilter) {
			super.setScissor(x * viewportScaleFactor, y * viewportScaleFactor,
                    width * viewportScaleFactor, height * viewportScaleFactor);
		} else {
			super.setScissor(x, y, width, height);
		}
	}
}

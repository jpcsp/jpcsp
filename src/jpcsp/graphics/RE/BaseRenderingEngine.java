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

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.buffer.BufferManagerFactory;
import jpcsp.graphics.RE.buffer.IREBufferManager;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 * Base class for a RenderingEngine.
 * This class offers convenience functions:
 * - the subclass is free to implement
 *      setMatrix()
 *   or setXXXMatrix()
 *   This class performs the mapping between the 2 equivalent sets of methods.
 */
public abstract class BaseRenderingEngine implements IRenderingEngine {
	protected final static Logger log = VideoEngine.log;
	protected IRenderingEngine re = this;
	protected GeContext context;
    protected IREBufferManager bufferManager;

    public BaseRenderingEngine() {
		bufferManager = BufferManagerFactory.createBufferManager(this);
		bufferManager.setRenderingEngine(this);
    }

    @Override
	public void setRenderingEngine(IRenderingEngine re) {
		this.re = re;
		bufferManager.setRenderingEngine(re);
	}

	@Override
	public void setGeContext(GeContext context) {
		this.context = context;
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		re.setMatrixMode(GU_PROJECTION);
		re.setMatrix(values);
	}

	@Override
	public void setViewMatrix(float[] values) {
		// The View matrix has always to be set BEFORE the Model matrix
		re.setMatrixMode(RE_MODELVIEW);
		setMatrix(values);
	}

	@Override
	public void setModelMatrix(float[] values) {
		// The Model matrix has always to be set AFTER the View matrix
		re.setMatrixMode(RE_MODELVIEW);
		re.multMatrix(values);
	}

	@Override
	public void setTextureMatrix(float[] values) {
		re.setMatrixMode(GU_TEXTURE);
		re.setMatrix(values);
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		re.setMatrixMode(RE_MODELVIEW);
		setMatrix(values);
	}

	@Override
	public IREBufferManager getBufferManager() {
		return bufferManager;
	}

	@Override
	public void setVertexColor(float[] color) {
		for (int i = 0; i < context.vertexColor.length; i++) {
			context.vertexColor[i] = color[i];
		}
	}
}

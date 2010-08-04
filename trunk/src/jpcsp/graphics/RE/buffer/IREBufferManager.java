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
package jpcsp.graphics.RE.buffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 * The interface for a RenderingEngine buffer manager.
 */
public interface IREBufferManager {
	public void setRenderingEngine(IRenderingEngine re);
	public boolean useVBO();
	public int genBuffer(int type, int size, int usage);
	public void bindBuffer(int buffer);
	public void deleteBuffer(int buffer);
	public ByteBuffer getBuffer(int buffer);
	public void setTexCoordPointer(int buffer, int size, int type, int stride, int offset);
	public void setColorPointer(int buffer, int size, int type, int stride, int offset);
	public void setVertexPointer(int buffer, int size, int type, int stride, int offset);
	public void setNormalPointer(int buffer, int type, int stride, int offset);
	public void setWeightPointer(int buffer, int size, int type, int stride, int offset);
	public void setVertexAttribPointer(int buffer, int id, int size, int type, boolean normalized, int stride, int offset);
	public void setBufferData(int buffer, int size, Buffer data, int usage);
}

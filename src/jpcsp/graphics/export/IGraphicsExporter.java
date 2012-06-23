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
package jpcsp.graphics.export;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;

/**
 * @author gid15
 *
 */
public interface IGraphicsExporter {
	public void startExport(GeContext context);
	public void endExport();
	public void startPrimitive(int numberOfVertex, int primitiveType);
	public void exportVertex(VertexState originalV, VertexState transformedV);
	public void endVertex(int numberOfVertex, int primitiveType);
	public void endPrimitive(int numberOfVertex, int primitiveType);
	public void exportTexture(String fileName);
}

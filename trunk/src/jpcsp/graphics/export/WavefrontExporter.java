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

import static jpcsp.graphics.GeCommands.PRIM_TRIANGLE;
import static jpcsp.graphics.GeCommands.PRIM_TRIANGLE_STRIPS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class WavefrontExporter implements IGraphicsExporter {
	private static Logger log = VideoEngine.log;
	// Use "." as decimal separator for floating point values
	private static Locale l = Locale.ENGLISH;
	private GeContext context;
    private BufferedWriter exportObj;
    private BufferedWriter exportMtl;
    private int exportVertexCount;
    private int exportTextureCount;
    private int exportNormalCount;
    private int exportModelCount;
    private int exportMaterialCount;
    // Blender does not import normals (as of blender 2.63)
    private static final boolean exportNormal = false;

    protected void exportObjLine(String line) {
    	if (exportObj != null) {
	    	try {
				exportObj.write(line);
		    	exportObj.newLine();
			} catch (IOException e) {
				log.error("Error writing export.obj file", e);
			}
    	}
    }

    protected void exportMtlLine(String line) {
    	if (exportMtl != null) {
	    	try {
				exportMtl.write(line);
		    	exportMtl.newLine();
			} catch (IOException e) {
				log.error("Error writing export.mtl file", e);
			}
    	}
    }

    public static String getExportDirectory() {
    	for (int i = 1; true; i++) {
    		String directory = String.format("%sExport-%d%c", IGraphicsExporter.exportDirectory, i, File.separatorChar);
    		if (!new File(directory).exists()) {
    			return directory;
    		}
    	}
    }

    @Override
	public void startExport(GeContext context, String directory) {
		this.context = context;

		try {
			// Prepare the export writers
			exportObj = new BufferedWriter(new FileWriter(String.format("%sexport.obj", directory)));
			exportMtl = new BufferedWriter(new FileWriter(String.format("%sexport.mtl", directory)));
		} catch (IOException e) {
			log.error("Error creating the export files", e);
		}
		exportVertexCount = 1;
		exportModelCount = 1;
		exportTextureCount = 1;
		exportMaterialCount = 1;

		exportObjLine(String.format("mtllib export.mtl"));
	}

	@Override
	public void endExport() {
		if (exportObj != null) {
			try {
				exportObj.close();
			} catch (IOException e) {
				// Ignore error
			}
			exportObj = null;
		}

		if (exportMtl != null) {
			try {
				exportMtl.close();
			} catch (IOException e) {
				// Ignore error
			}
			exportMtl = null;
		}
	}

	@Override
	public void startPrimitive(int numberOfVertex, int primitiveType) {
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("Exporting Object model%d", exportModelCount));
    	}

    	exportObjLine(String.format("# modelCount=%d, vertexCount=%d, textureCount=%d, normalCount=%d", exportModelCount, exportVertexCount, exportTextureCount, exportNormalCount));
	}

	@Override
	public void exportVertex(VertexState originalV, VertexState transformedV) {
		exportObjLine(String.format(l, "v %f %f %f", transformedV.p[0], transformedV.p[1], transformedV.p[2]));
        if (context.vinfo.texture != 0) {
        	exportObjLine(String.format(l, "vt %f %f", transformedV.t[0], transformedV.t[1]));
        }
        if (exportNormal && context.vinfo.normal != 0) {
        	exportObjLine(String.format(l, "vn %f %f %f", transformedV.n[0], transformedV.n[1], transformedV.n[2]));
        }
	}

	@Override
	public void endVertex(int numberOfVertex, int primitiveType) {
		// Export object material
		exportObjLine(String.format("g model%d", exportModelCount));
		exportObjLine(String.format("usemtl material%d", exportMaterialCount));

		// Export faces
		exportObjLine("");
		switch (primitiveType) {
    		case PRIM_TRIANGLE: {
				boolean clockwise = context.frontFaceCw;

				for (int i = 0; i < numberOfVertex; i += 3) {
					if (clockwise) {
						exportFace(i + 1, i, i + 2);
					} else {
						exportFace(i, i + 1, i + 2);
					}
				}
				break;
    		}
			case PRIM_TRIANGLE_STRIPS: {
				for (int i = 0; i < numberOfVertex - 2; i++) {
					// Front face is alternating every 2 triangle strips
					boolean clockwise = (i % 2) == 0;

					if (!context.frontFaceCw) {
						clockwise = !clockwise;
					}

					if (clockwise) {
						exportFace(i + 1, i, i + 2);
					} else {
						exportFace(i, i + 1, i + 2);
					}
				}
				break;
			}
    	}
	}

	@Override
	public void endPrimitive(int numberOfVertex, int primitiveType) {
    	exportVertexCount += numberOfVertex;
    	if (context.vinfo.texture != 0) {
    		exportTextureCount += numberOfVertex;
    	}
    	if (exportNormal && context.vinfo.normal != 0) {
    		exportNormalCount += numberOfVertex;
    	}
    	exportModelCount++;
    	exportMaterialCount++;
	}

	@Override
	public void exportTexture(String fileName) {
    	// Export material definition
    	int illum = 1;
		exportMtlLine(String.format("newmtl material%d", exportMaterialCount));
		exportMtlLine(String.format("illum %d", illum));

    	exportColor(l, "Ka", context.mat_ambient);
    	exportColor(l, "Kd", context.mat_diffuse);
    	exportColor(l, "Ks", context.mat_specular);

    	if (fileName != null) {
    		exportMtlLine(String.format("map_Kd %s", fileName));
    	}
	}

    private void exportFace(int i1, int i2, int i3) {
    	int p1 = i1 + exportVertexCount;
    	int p2 = i2 + exportVertexCount;
    	int p3 = i3 + exportVertexCount;
    	if (exportNormal && context.vinfo.normal != 0) {
        	int n1 = i1 + exportNormalCount;
        	int n2 = i2 + exportNormalCount;
        	int n3 = i3 + exportNormalCount;
    		if (context.vinfo.texture != 0) {
	        	int t1 = i1 + exportTextureCount;
	        	int t2 = i2 + exportTextureCount;
	        	int t3 = i3 + exportTextureCount;
	        	exportObjLine(String.format("f %d/%d/%d %d/%d/%d %d/%d/%d", p1, t1, n1, p2, t2, n2, p3, t3, n3));
	    	} else {
	    		exportObjLine(String.format("f %d//%d %d//%d %d//%d", p1, n1, p2, n2, p3, n3));
	    	}
    	} else {
    		if (context.vinfo.texture != 0) {
	        	int t1 = i1 + exportTextureCount;
	        	int t2 = i2 + exportTextureCount;
	        	int t3 = i3 + exportTextureCount;
	        	exportObjLine(String.format("f %d/%d %d/%d %d/%d", p1, t1, p2, t2, p3, t3));
	    	} else {
	    		exportObjLine(String.format("f %d %d %d", p1, p2, p3));
	    	}
    	}
    }

    private void exportColor(Locale l, String name, float[] color) {
    	exportMtlLine(String.format(l, "%s %f %f %f", name, color[0], color[1], color[2]));
    }
}

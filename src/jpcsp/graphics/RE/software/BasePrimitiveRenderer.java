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
package jpcsp.graphics.RE.software;

import static jpcsp.graphics.RE.software.PixelColor.doubleColor;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.util.Utilities.invertMatrix3x3;
import static jpcsp.util.Utilities.matrixMult;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.maxInt;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.minInt;
import static jpcsp.util.Utilities.round;
import static jpcsp.util.Utilities.transposeMatrix3x3;
import static jpcsp.util.Utilities.vectorMult44;

import java.util.Arrays;
import java.util.HashMap;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.util.DurationStatistics;
import jpcsp.util.LongLongKey;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 * This class extends the BaseRenderer class to include
 * vertex specific information.
 * The methods from this class can be used to set the vertex
 * information specific for the rendering of one primitive (e.g. one triangle)
 */
public abstract class BasePrimitiveRenderer extends BaseRenderer {
	private static HashMap<Integer, DurationStatistics> pixelsStatistics = new HashMap<Integer, DurationStatistics>();
	private DurationStatistics pixelStatistics = new DurationStatistics();
	public final PixelState pixel = new PixelState();
	public PrimitiveState prim = new PrimitiveState();
	protected boolean needScissoringX;
	protected boolean needScissoringY;
	protected boolean needSourceDepthRead;
	protected boolean needDestinationDepthRead;
	protected boolean needDepthWrite;
	protected boolean needTextureUV;
	protected boolean swapTextureUV;
	protected boolean simpleTextureUV;
	protected boolean needTextureWrapU;
	protected boolean needTextureWrapV;
	protected boolean sameVertexColor;
	protected boolean needSourceDepthClamp;
	public int fbAddress;
	public int depthAddress;
    public IRendererWriter rendererWriter;

	protected void copy(BasePrimitiveRenderer from) {
		super.copy(from);
		pixel.copy(from.pixel);
		prim.copy(from.prim);
		needScissoringX = from.needScissoringX;
		needScissoringY = from.needScissoringY;
		needSourceDepthRead = from.needSourceDepthRead;
		needDestinationDepthRead = from.needDestinationDepthRead;
		needDepthWrite = from.needDepthWrite;
		needTextureUV = from.needTextureUV;
		simpleTextureUV = from.simpleTextureUV;
		needTextureWrapU = from.needTextureWrapU;
		needTextureWrapV = from.needTextureWrapV;
		sameVertexColor = from.sameVertexColor;
		fbAddress = from.fbAddress;
		depthAddress = from.depthAddress;
		rendererWriter = from.rendererWriter;
		needSourceDepthClamp = from.needSourceDepthClamp;
	}

	@Override
	protected void init(GeContext context, CachedTextureResampled texture, boolean useVertexTexture, boolean isTriangle) {
		super.init(context, texture, useVertexTexture, isTriangle);

		prim.pxMax = Integer.MIN_VALUE;
		prim.pxMin = Integer.MAX_VALUE;
		prim.pyMax = Integer.MIN_VALUE;
		prim.pyMin = Integer.MAX_VALUE;
		prim.pzMax = Integer.MIN_VALUE;
		prim.pzMin = Integer.MAX_VALUE;

		if (!transform2D) {
			// Pre-compute the Model-View matrix
			matrixMult(pixel.modelViewMatrix, context.view_uploaded_matrix, context.model_uploaded_matrix);

			// Pre-compute the Model-View-Projection matrix
			matrixMult(pixel.modelViewProjectionMatrix, context.proj_uploaded_matrix, pixel.modelViewMatrix);
		}
	}

	@Override
	protected void initRendering(GeContext context) {
		if (renderingInitialized) {
			return;
		}

		super.initRendering(context);

		pixel.materialAmbient = getColor(context.mat_ambient);
		pixel.materialDiffuse = getColor(context.mat_diffuse);
		pixel.materialSpecular = getColor(context.mat_specular);

		if (!transform2D) {
			if (context.tex_map_mode == GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX) {
				// Copy the Texture matrix
				System.arraycopy(context.texture_uploaded_matrix, 0, pixel.textureMatrix, 0, pixel.textureMatrix.length);
			}

			pixel.hasNormal = context.vinfo.normal != 0;

			if (pixel.hasNormal) {
				// Pre-compute the matrix to transform a normal to the eye coordinates
				// See http://www.lighthouse3d.com/tutorials/glsl-tutorial/the-normal-matrix/
				float[] invertedModelViewMatrix = new float[16];
				if (invertMatrix3x3(invertedModelViewMatrix, pixel.modelViewMatrix)) {
					transposeMatrix3x3(pixel.normalMatrix, invertedModelViewMatrix);
				} else {
					// What is using the PSP in this case? Assume it just takes the Model-View matrix
					System.arraycopy(pixel.modelViewMatrix, 0, pixel.normalMatrix, 0, pixel.normalMatrix.length);
					if (isLogDebugEnabled) {
						log.debug(String.format("ModelView matrix cannot be inverted, taking the Model-View matrix itself!"));
					}
				}
			}
		}
	}

	protected void addPosition(float[] p) {
		float[] screenCoordinates = new float[4];
		getScreenCoordinates(screenCoordinates, p);
		prim.pxMax = maxInt(prim.pxMax, screenCoordinates[0]);
		prim.pxMin = minInt(prim.pxMin, screenCoordinates[0]);
		prim.pyMax = maxInt(prim.pyMax, screenCoordinates[1]);
		prim.pyMin = minInt(prim.pyMin, screenCoordinates[1]);
		prim.pzMax = maxInt(prim.pzMax, screenCoordinates[2]);
		prim.pzMin = minInt(prim.pzMin, screenCoordinates[2]);
	}

	protected void setVertexPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2, v3);
	}

	protected void setVertexPositions(VertexState v1, VertexState v2) {
		setPositions(v1, v2);
	}

	protected void setVertexTextures(GeContext context, VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2, v3);
		setVertexTextures(context, v1.c, v2.c, v3.c);
	}

	protected void setVertexTextures(GeContext context, VertexState v1, VertexState v2) {
		setTextures(v1, v2);
		setVertexTextures(context, v1.c, v2.c, null);
	}

	private void setVertexTextures(GeContext context, float[] c1, float[] c2, float[] c3) {
		textureWidth = context.texture_width[mipmapLevel];
		textureHeight = context.texture_height[mipmapLevel];

		// The rendering will be performed into the following ranges:
		// 3D:
		//   - x: [pxMin..pxMax] (min and max included)
		//   - y: [pxMin..pxMax] (min and max included)
		// 2D:
		//   - x: [pxMin..pxMax-1] (min included but max excluded)
		//   - y: [pxMin..pxMax-1] (min included but max excluded)
        if (transform2D) {
        	prim.pxMax--;
        	prim.pyMax--;
        } else {
        	// Restrict the drawn area to the scissor area.
        	// We can just update the min/max values, the TextureMapping filter
        	// will take are of the correct texture mapping.
        	// We do no longer need a scissoring filter.
        	if (needScissoringX) {
        		prim.pxMin = max(prim.pxMin, scissorX1);
        		prim.pxMax = min(prim.pxMax, scissorX2);
        		needScissoringX = false;
        	}
        	if (needScissoringY) {
        		prim.pyMin = max(prim.pyMin, scissorY1);
        		prim.pyMax = min(prim.pyMax, scissorY2);
        		needScissoringY = false;
        	}
        }
        prim.destinationWidth = prim.pxMax - prim.pxMin + 1;
        prim.destinationHeight = prim.pyMax - prim.pyMin + 1;

        if (isUsingTexture(context)) {
			simpleTextureUV = !isTriangle;

			if (!simpleTextureUV && isTriangle && transform2D) {
				// Check if the 2D triangle can be rendered using a simple texture UV mapping:
				// this is only possible when the triangle has a square angle.
				//
				// 1---2     1---2     1             1
				// |  /       \  |     | \         / |
				// | /         \ |     |  \       /  |
				// 3             3     3---2     3---2
				//
				// 1---3     1---3     1             1
				// |  /       \  |     | \         / |
				// | /         \ |     |  \       /  |
				// 2             2     2---3     2---3
				//
				if (prim.p1x == prim.p2x && prim.t1u == prim.t2u) {
					if (prim.p1y == prim.p3y && prim.t1v == prim.t3v) {
						simpleTextureUV = true;
					} else if (prim.p2y == prim.p3y && prim.t2v == prim.t3v){
						simpleTextureUV = true;
					}
				} else if (prim.p1x == prim.p3x && prim.t1u == prim.t3u) {
					if (prim.p1y == prim.p2y && prim.t1v == prim.t2v) {
						simpleTextureUV = true;
					} else if (prim.p2y == prim.p3y && prim.t2v == prim.t3v){
						simpleTextureUV = true;
					}
				} else if (prim.p2x == prim.p3x && prim.t2u == prim.t3u) {
					if (prim.p1y == prim.p2y && prim.t1v == prim.t2v) {
						simpleTextureUV = true;
					} else if (prim.p1y == prim.p1y && prim.t2v == prim.t3v){
						simpleTextureUV = true;
					}
				}
			}

			if (simpleTextureUV) {
		        if (transform2D) {
			    	boolean flipX = false;
			    	boolean flipY = false;
			    	if (isTriangle) {
			    		// Compute texture flips for a triangle
			    		if (prim.t1u != prim.t2u) {
			    			flipX = (prim.t1u > prim.t2u) ^ (prim.p1x > prim.p2x);
			    		}
			    		if (prim.t1v != prim.t2v) {
			    			flipY = (prim.t1v > prim.t2v) ^ (prim.p1y > prim.p2y);
			    		}
			    		if (!flipX && prim.t2u != prim.t3u) {
				    		flipX = (prim.t2u > prim.t3u) ^ (prim.p2x > prim.p3x);
			    		}
			    		if (!flipY && prim.t2v != prim.t3v) {
				    		flipY = (prim.t2v > prim.t3v) ^ (prim.p2y > prim.p3y);
			    		}
			    	} else {
			    		// Compute texture flips for a sprite
			    		flipX = (prim.t1u > prim.t2u) ^ (prim.p1x > prim.p2x);
			    		flipY = (prim.t1v > prim.t2v) ^ (prim.p1y > prim.p2y);
			    		if (flipX && flipY) {
			    			swapTextureUV = true;
			    			flipX = false;
			    			flipY = false;
			    		}
			    	}
			    	if (isLogTraceEnabled) {
			    		log.trace(String.format("2D texture flipX=%b, flipY=%b, swapUV=%b, point (%d,%d)-(%d,%d), texture (%d,%d)-(%d,%d)", flipX, flipY, swapTextureUV, prim.pxMin, prim.pyMin, prim.pxMax, prim.pyMax, prim.tuMin, prim.tvMin, prim.tuMax, prim.tvMax));
			    	}
			    	prim.uStart = flipX ? prim.tuMax : prim.tuMin;
			    	float uEnd = flipX ? prim.tuMin : prim.tuMax;
			    	prim.vStart = flipY ? prim.tvMax : prim.tvMin;
			    	float vEnd = flipY ? prim.tvMin : prim.tvMax;
			    	prim.uStep = (uEnd - prim.uStart) / (swapTextureUV ? prim.destinationHeight : prim.destinationWidth);
			    	prim.vStep = (vEnd - prim.vStart) / (swapTextureUV ? prim.destinationWidth : prim.destinationHeight);
		        } else {
		        	// 3D sprite
		        	prim.uStart = prim.t1u;
		        	float uEnd = prim.t2u;
		        	prim.vStart = prim.t1v;
		        	float vEnd = prim.t2v;
			    	prim.uStep = (uEnd - prim.uStart) / (prim.destinationWidth - 1);
			    	prim.vStep = (vEnd - prim.vStart) / (prim.destinationHeight - 1);
		        }

		        // Perform scissoring and update uStart/uStep and vStart/vStep
	        	if (needScissoringX) {
					int deltaX = scissorX1 - prim.pxMin;
					if (deltaX > 0) {
						prim.uStart += prim.uStep * deltaX;
						prim.pxMin += deltaX;
						if (transform2D) {
							prim.tuMin += round(prim.uStep * deltaX);
						}
					}
					deltaX = prim.pxMax - scissorX2;
					if (deltaX > 0) {
						prim.pxMax -= deltaX;
						if (transform2D) {
							prim.tuMax -= round(prim.uStep * deltaX);
						}
					}
			        prim.destinationWidth = prim.pxMax - prim.pxMin + 1;
					needScissoringX = false;
	        	}
	        	if (needScissoringY) {
					int deltaY = scissorY1 - prim.pyMin;
					if (deltaY > 0) {
						prim.vStart += prim.vStep * deltaY;
						prim.pyMin += deltaY;
						if (transform2D) {
							prim.tvMin += round(prim.vStep * deltaY);
						}
					}
					deltaY = prim.pyMax - scissorY2;
					if (deltaY > 0) {
						prim.pyMax -= deltaY;
						if (transform2D) {
							prim.tvMax -= round(prim.vStep * deltaY);
						}
					}
			        prim.destinationHeight = prim.pyMax - prim.pyMin + 1;
					needScissoringY = false;
	        	}
			}
        }

        if (setVertexPrimaryColor) {
    		if (c3 != null) {
	    		pixel.c1a = getColor(c1[3]);
	    		pixel.c1b = getColor(c1[2]);
	    		pixel.c1g = getColor(c1[1]);
	    		pixel.c1r = getColor(c1[0]);
	    		pixel.c2a = getColor(c2[3]);
	    		pixel.c2b = getColor(c2[2]);
	    		pixel.c2g = getColor(c2[1]);
	    		pixel.c2r = getColor(c2[0]);
	    		pixel.c3a = getColor(c3[3]);
	    		pixel.c3b = getColor(c3[2]);
	    		pixel.c3g = getColor(c3[1]);
	    		pixel.c3r = getColor(c3[0]);
    			pixel.c3 = getColor(c3);
    		}
        	if (isTriangle) {
        		if (context.shadeModel == GeCommands.SHADE_TYPE_FLAT) {
        			// Flat shade model: always use the color of the 3rd triangle vertex
        			sameVertexColor = true;
        		} else {
        			sameVertexColor = Utilities.sameColor(c1, c2, c3);
        		}
    			// For triangles, take the weighted color from the 3 vertices.
        	} else {
    			// For sprites, take only the color from the 2nd vertex
    			primaryColor = getColor(c2);
    			if (context.textureColorDoubled) {
    				primaryColor = doubleColor(primaryColor);
    			}
        	}
        }

		// Try to avoid to compute expensive values
		needDepthWrite = getNeedDepthWrite(context);
		needSourceDepthRead = needDepthWrite || getNeedSourceDepthRead(context);
		needDestinationDepthRead = getNeedDestinationDepthRead(context);
		if (zbw <= 0) {
			needDepthWrite = false;
			needSourceDepthRead = false;
			needDestinationDepthRead = false;
		}
		needTextureUV = getNeedTextureUV(context);
		if (transform2D) {
			needTextureWrapU = prim.tuMin < 0 || prim.tuMax >= context.texture_width[mipmapLevel];
			needTextureWrapV = prim.tvMin < 0 || prim.tvMax >= context.texture_height[mipmapLevel];
		} else {
			if (context.tex_map_mode != GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV) {
				needTextureWrapU = true;
				needTextureWrapV = true;
			} else {
				float tuMin, tuMax;
				float tvMin, tvMax;
				if (isTriangle) {
					tuMin = Utilities.min(prim.t1u, Utilities.min(prim.t2u, prim.t3u));
					tuMax = Utilities.max(prim.t1u, Utilities.max(prim.t2u, prim.t3u));
					tvMin = Utilities.min(prim.t1v, Utilities.min(prim.t2v, prim.t3v));
					tvMax = Utilities.max(prim.t1v, Utilities.max(prim.t2v, prim.t3v));
				} else {
					tuMin = Utilities.min(prim.t1u, prim.t2u);
					tuMax = Utilities.max(prim.t1u, prim.t2u);
					tvMin = Utilities.min(prim.t1v, prim.t2v);
					tvMax = Utilities.max(prim.t1v, prim.t2v);
				}
				tuMin = tuMin * texScaleX + texTranslateX;
				tuMax = tuMax * texScaleX + texTranslateX;
				tvMin = tvMin * texScaleY + texTranslateY;
				tvMax = tvMax * texScaleY + texTranslateY;
				needTextureWrapU = tuMin < 0f || tuMax >= 0.99999f;
				needTextureWrapV = tvMin < 0f || tvMax >= 0.99999f;
			}
		}
		needSourceDepthClamp = false;
		if (needDepthWrite && needSourceDepthRead && isTriangle) {
			if (prim.p1z < 0f || prim.p2z < 0f || prim.p3z < 0f) {
				needSourceDepthClamp = true;
			} else if (prim.p1z > 65535f || prim.p2z > 65535f || prim.p3z > 65535f) {
				needSourceDepthClamp = true;
			}
		}

		prepareWriters();

		LongLongKey rendererKey = getRendererKey();
    	if (compiledRenderer == null || !rendererKey.equals(compiledRendererKey)) {
			compiledRendererKey = rendererKey;
			compiledRenderer = FilterCompiler.getInstance().getCompiledRenderer(this, rendererKey, context);
			if (isLogTraceEnabled) {
				log.trace(String.format("Rendering using compiled renderer %s", compiledRenderer.getClass().getName()));
			}
    	}

    	if (c3 != null) {
        	prim.preComputeTriangleWeights();
        }
	}

	private boolean getNeedSourceDepthRead(GeContext context) {
		if (!clearMode && context.depthTestFlag.isEnabled()) {
			if (context.depthFunc != GeCommands.ZTST_FUNCTION_NEVER_PASS_PIXEL && context.depthFunc != GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL) {
				return true;
			}
		}

		if (!clearMode && !transform2D) {
			if (nearZ != 0x0000 || farZ != 0xFFFF) {
				return true;
			}
		}

		return false;
	}

	private boolean getNeedDestinationDepthRead(GeContext context) {
		if (!clearMode && context.depthTestFlag.isEnabled()) {
			if (context.depthFunc != GeCommands.ZTST_FUNCTION_NEVER_PASS_PIXEL && context.depthFunc != GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL) {
				return true;
			}
		}

		if (clearMode) {
			// Depth writes disabled
			if (!context.clearModeDepth) {
				return true;
			}
		} else if (!context.depthTestFlag.isEnabled()) {
			// Depth writes are disabled when the depth test is not enabled.
			return true;
		} else if (!context.depthMask) {
			// Depth writes disabled
			return true;
		}

		return false;
	}

	private boolean getNeedDepthWrite(GeContext context) {
		if (clearMode) {
			// Depth writes disabled
			if (!context.clearModeDepth) {
				return false;
			}
		} else if (!context.depthTestFlag.isEnabled()) {
			// Depth writes are disabled when the depth test is not enabled.
			return false;
		} else if (!context.depthMask) {
			// Depth writes disabled
			return false;
		}

		return true;
	}

	private boolean getNeedTextureUV(GeContext context) {
    	if (context.textureFlag.isEnabled() && (!transform2D || useVertexTexture) && !clearMode) {
    		// UV always required by the texture reader
    		return true;
    	}

		return false;
	}

	private void setPositions(VertexState v1, VertexState v2) {
        pixel.v1x = v1.p[0];
        pixel.v1y = v1.p[1];
        pixel.v1z = v1.p[2];
        pixel.n1x = v1.n[0];
        pixel.n1y = v1.n[1];
        pixel.n1z = v1.n[2];

        pixel.v2x = v2.p[0];
        pixel.v2y = v2.p[1];
        pixel.v2z = v2.p[2];
        pixel.n2x = v2.n[0];
        pixel.n2y = v2.n[1];
        pixel.n2z = v2.n[2];

        if (transform2D) {
        	prim.p1x = pixel.v1x;
        	prim.p1y = pixel.v1y;
        	prim.p1z = pixel.v1z;
        	prim.p2x = pixel.v2x;
        	prim.p2y = pixel.v2y;
        	prim.p2z = pixel.v2z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v1x, pixel.v1y, pixel.v1z);
			prim.p1x = screenCoordinates[0];
			prim.p1y = screenCoordinates[1];
			prim.p1z = screenCoordinates[2];
			prim.p1w = screenCoordinates[3];
			prim.p1wInverted = 1.f / prim.p1w;
			getScreenCoordinates(screenCoordinates, pixel.v2x, pixel.v2y, pixel.v2z);
			prim.p2x = screenCoordinates[0];
			prim.p2y = screenCoordinates[1];
			prim.p2z = screenCoordinates[2];
			prim.p2w = screenCoordinates[3];
			prim.p2wInverted = 1.f / prim.p2w;
		}

        prim.pxMax = maxInt(prim.p1x, prim.p2x);
        prim.pxMin = minInt(prim.p1x, prim.p2x);
        prim.pyMax = maxInt(prim.p1y, prim.p2y);
        prim.pyMin = minInt(prim.p1y, prim.p2y);
        prim.pzMax = maxInt(prim.p1z, prim.p2z);
        prim.pzMin = minInt(prim.p1z, prim.p2z);
	}

	private void setPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2);

		pixel.v3x = v3.p[0];
		pixel.v3y = v3.p[1];
		pixel.v3z = v3.p[2];
		pixel.n3x = v3.n[0];
		pixel.n3y = v3.n[1];
		pixel.n3z = v3.n[2];

        if (transform2D) {
        	prim.p3x = pixel.v3x;
        	prim.p3y = pixel.v3y;
        	prim.p3z = pixel.v3z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v3x, pixel.v3y, pixel.v3z);
			prim.p3x = screenCoordinates[0];
			prim.p3y = screenCoordinates[1];
			prim.p3z = screenCoordinates[2];
			prim.p3w = screenCoordinates[3];
			prim.p3wInverted = 1.f / prim.p3w;
		}

        prim.pxMax = maxInt(prim.pxMax, prim.p3x);
        prim.pxMin = minInt(prim.pxMin, prim.p3x);
        prim.pyMax = maxInt(prim.pyMax, prim.p3y);
        prim.pyMin = minInt(prim.pyMin, prim.p3y);
        prim.pzMax = maxInt(prim.pzMax, prim.p3z);
        prim.pzMin = minInt(prim.pzMin, prim.p3z);
	}

	private void setTextures(VertexState v1, VertexState v2) {
		prim.t1u = v1.t[0];
		prim.t1v = v1.t[1];
		prim.t2u = v2.t[0];
		prim.t2v = v2.t[1];

        if (transform2D) {
        	prim.tuMax = max(round(prim.t1u), round(prim.t2u));
        	prim.tuMin = min(round(prim.t1u), round(prim.t2u));
        	prim.tvMax = max(round(prim.t1v), round(prim.t2v));
        	prim.tvMin = min(round(prim.t1v), round(prim.t2v));
        }
	}

	private void setTextures(VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2);

		prim.t3u = v3.t[0];
		prim.t3v = v3.t[1];

        if (transform2D) {
        	prim.tuMax = max(prim.tuMax, round(prim.t3u));
        	prim.tuMin = min(prim.tuMin, round(prim.t3u));
        	prim.tvMax = max(prim.tvMax, round(prim.t3v));
        	prim.tvMin = min(prim.tvMin, round(prim.t3v));
        }
	}

	private void prepareWriters() {
        fbAddress = getTextureAddress(fbp, prim.pxMin, prim.pyMin, fbw, psm);
    	depthAddress = getTextureAddress(zbp, prim.pxMin, prim.pyMin, zbw, depthBufferPixelFormat);
    	if (!RendererTemplate.isRendererWriterNative(memInt, psm)) {
    		rendererWriter = RendererWriter.getRendererWriter(fbAddress, fbw, psm, depthAddress, zbw, depthBufferPixelFormat, needDestinationDepthRead, needDepthWrite);
    	}
        imageWriterSkipEOL = fbw - prim.destinationWidth;
        depthWriterSkipEOL = zbw - prim.destinationWidth;
	}

	protected boolean isVisible() {
    	if (!transform2D) {
    		// Each vertex screen coordinates (without offset) has to be in the range:
    		// - x: [0..4095]
    		// - y: [0..4095]
    		// - z: [..65535]
    		// If one of the vertex coordinate is not in the valid range, the whole
    		// primitive is discarded.
        	if ((prim.pxMin + screenOffsetX) < 0 ||
        	    (prim.pxMax + screenOffsetX) >= 4096 ||
        	    (prim.pyMin + screenOffsetY) < 0 ||
        	    (prim.pyMax + screenOffsetY) >= 4096 ||
        	    prim.pzMax >= 65536) {
        		if (isLogTraceEnabled) {
        			log.trace(String.format("Screen coordinates outside valid range %d-%d, %d-%d, %d", prim.pxMin + screenOffsetX, prim.pxMax + screenOffsetX, prim.pyMin + screenOffsetY, prim.pyMax + screenOffsetY, prim.pzMax));
        		}
        		return false;
        	}

        	// This is probably a rounding error when one triangle
        	// extends from back to front over a very large distance
        	// (more than the allowed range for Z values).
        	if (prim.pzMin < 0 && prim.pzMax > 0 && prim.pzMax - prim.pzMin > 65536) {
        		if (isLogTraceEnabled) {
        			log.trace(String.format("Z range too large: %d, %d", prim.pzMin, prim.pzMax));
        		}
        		return false;
        	}

        	if (!clipPlanesEnabled) {
        		// The primitive is discarded when one of the vertex is behind the viewpoint
        		// (only the the ClipPlanes flag is not enabled).
        		if (prim.pzMin < 0) {
        			if (isLogTraceEnabled) {
        				log.trace(String.format("Z behind clip plane %d", prim.pzMin));
        			}
        			return false;
        		}
        	} else {
            	// TODO Implement proper triangle clipping against the near plane
            	if (prim.p1w < 0f || prim.p2w < 0f || prim.p3w < 0f) {
        			if (isLogTraceEnabled) {
        				log.trace(String.format("W negative %f, %f, %f", prim.p1w, prim.p2w, prim.p3w));
        			}
            		return false;
            	}
        	}
    	}

		if (!useVertexTexture) {
			prim.pxMin = Math.max(prim.pxMin, scissorX1);
			prim.pxMax = Math.min(prim.pxMax, Math.min(scissorX2 + 1, fbw));
			prim.pyMin = Math.max(prim.pyMin, scissorY1);
			prim.pyMax = Math.min(prim.pyMax, scissorY2 + 1);
		}

		if (prim.pxMin == prim.pxMax || prim.pyMin == prim.pyMax) {
			// Empty area to be displayed
			if (isLogTraceEnabled) {
				log.trace("Empty area");
			}
			return false;
		}

		if (isTriangle) {
			if ((pixel.v1x == pixel.v2x && pixel.v1y == pixel.v2y && pixel.v1z == pixel.v2z) ||
			    (pixel.v1x == pixel.v3x && pixel.v1y == pixel.v3y && pixel.v1z == pixel.v3z) ||
			    (pixel.v2x == pixel.v3x && pixel.v2y == pixel.v3y && pixel.v2z == pixel.v3z)) {
				// 2 vertices are equal in the triangle, nothing has to be displayed
				if (isLogTraceEnabled) {
					log.trace("2 vertices equal in triangle");
				}
				return false;
			}
		}

		if (!insideScissor()) {
			if (isLogTraceEnabled) {
				log.trace("Not inside scissor");
			}
			return false;
		}

        return true;
	}

	protected boolean insideScissor() {
        needScissoringX = false;
        needScissoringY = false;

        // Scissoring (also applied in clear mode)
    	if (prim.pxMax < scissorX1 || prim.pxMin > scissorX2) {
    		// Completely outside the scissor area, skip
			if (isLogTraceEnabled) {
				log.trace(String.format("X outside scissor area %d-%d, %d-%d", prim.pxMin, prim.pxMax, scissorX1, scissorX2));
			}
    		return false;
    	}
    	if (prim.pyMax < scissorY1 || prim.pyMin > scissorY2) {
    		// Completely outside the scissor area, skip
			if (isLogTraceEnabled) {
				log.trace(String.format("Y outside scissor area %d-%d, %d-%d", prim.pyMin, prim.pyMax, scissorY1, scissorY2));
			}
    		return false;
    	}
    	if (!transform2D) {
        	if ((nearZ > 0x0000 && prim.pzMax < nearZ) || (farZ < 0xFFFF && prim.pzMin > farZ)) {
        		// Completely outside the view area, skip
    			if (isLogTraceEnabled) {
    				log.trace(String.format("Z outside view area %d-%d, %d-%d", prim.pzMin, prim.pzMax, nearZ, farZ));
    			}
        		return false;
        	}
    	}

    	if (prim.pxMin < scissorX1 || prim.pxMax > scissorX2) {
    		// partially outside the scissor area, use the scissoring filter
    		needScissoringX = true;
    	}
    	if (prim.pyMin < scissorY1 || prim.pyMax > scissorY2) {
    		// partially outside the scissor area, use the scissoring filter
    		needScissoringY = true;
    	}

        return true;
	}

	private void getScreenCoordinates(float[] screenCoordinates, float[] position) {
		getScreenCoordinates(screenCoordinates, position[0], position[1], position[2]);
	}

	private void getScreenCoordinates(float[] screenCoordinates, float x, float y, float z) {
		float[] position4 = new float[4];
		position4[0] = x;
		position4[1] = y;
		position4[2] = z;
		position4[3] = 1.f;
		float[] projectedCoordinates = new float[4];
		vectorMult44(projectedCoordinates, pixel.modelViewProjectionMatrix, position4);
		float w = projectedCoordinates[3];
		float wInverted = 1.f / w;
		screenCoordinates[0] = projectedCoordinates[0] * wInverted * viewportWidth + viewportX - screenOffsetX;
		screenCoordinates[1] = projectedCoordinates[1] * wInverted * viewportHeight + viewportY - screenOffsetY;
		screenCoordinates[2] = projectedCoordinates[2] * wInverted * zscale + zpos;
		screenCoordinates[3] = w;

		if (isLogTraceEnabled) {
			log.trace(String.format("X,Y,Z = %f, %f, %f, projected X,Y,Z,W = %f, %f, %f, %f -> Screen %.3f, %.3f, %.3f", x, y, z, projectedCoordinates[0] / w, projectedCoordinates[1] / w, projectedCoordinates[2] / w, w, screenCoordinates[0], screenCoordinates[1], screenCoordinates[2]));
		}
	}

	@Override
	public void postRender() {
		if (DurationStatistics.collectStatistics && isLogInfoEnabled) {
			pixelStatistics.end();
			final int pixelsGrouping = 1000;
			int n = pixel.getNumberPixels() / pixelsGrouping;
			if (!pixelsStatistics.containsKey(n)) {
				pixelsStatistics.put(n, new DurationStatistics(String.format("Pixels count=%d", n * pixelsGrouping)));
			}
			if (isLogTraceEnabled) {
				log.trace(String.format("Pixels statistics count=%d, real count=%d", n * pixelsGrouping, pixel.getNumberPixels()));
			}
			pixelsStatistics.get(n).add(pixelStatistics);
		}

		if (rendererWriter != null) {
			rendererWriter.flush();
		}

		super.postRender();

		statisticsFilters(pixel.getNumberPixels());
	}

	@Override
	public void preRender() {
		pixel.reset();

		super.preRender();

		if (DurationStatistics.collectStatistics && isLogInfoEnabled) {
			pixelStatistics.reset();
			pixelStatistics.start();
		}
	}

	protected void writerSkip(int count) {
		rendererWriter.skip(count, count);
	}

	protected void writerSkipEOL(int count) {
		rendererWriter.skip(count + imageWriterSkipEOL, count + depthWriterSkipEOL);
	}

	protected void writerSkipEOL() {
		rendererWriter.skip(imageWriterSkipEOL, depthWriterSkipEOL);
	}

	@Override
	public void render() {
		compiledRenderer.render(this);
	}

	public static void exit() {
		if (!log.isInfoEnabled() || pixelsStatistics.isEmpty()) {
			return;
		}

		DurationStatistics[] sortedPixelsStatistics = pixelsStatistics.values().toArray(new DurationStatistics[pixelsStatistics.size()]);
		Arrays.sort(sortedPixelsStatistics);
		for (DurationStatistics durationStatistics : sortedPixelsStatistics) {
			log.info(durationStatistics);
		}
	}

	protected LongLongKey getRendererKey() {
		LongLongKey key = new LongLongKey(baseRendererKey);

		key.addKeyComponent(needSourceDepthRead);
		key.addKeyComponent(needDestinationDepthRead);
		key.addKeyComponent(needDepthWrite);
		key.addKeyComponent(needTextureUV);
		key.addKeyComponent(simpleTextureUV);
		key.addKeyComponent(swapTextureUV);
		key.addKeyComponent(needScissoringX);
		key.addKeyComponent(needScissoringY);
		key.addKeyComponent(needTextureWrapU);
		key.addKeyComponent(needTextureWrapV);
		key.addKeyComponent(sameVertexColor);
		key.addKeyComponent(needSourceDepthClamp);

		return key;
	}
}

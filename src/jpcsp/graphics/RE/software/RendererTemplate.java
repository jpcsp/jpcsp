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

import static jpcsp.graphics.GeCommands.LMODE_SEPARATE_SPECULAR_COLOR;
import static jpcsp.graphics.GeCommands.SOP_REPLACE_STENCIL_VALUE;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.RE.software.PixelColor.doubleColor;
import static jpcsp.graphics.RE.software.PixelColor.ONE;
import static jpcsp.graphics.RE.software.PixelColor.ZERO;
import static jpcsp.graphics.RE.software.PixelColor.absBGR;
import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;
import static jpcsp.graphics.RE.software.PixelColor.combineComponent;
import static jpcsp.graphics.RE.software.PixelColor.doubleComponent;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getBlue;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.getColorBGR;
import static jpcsp.graphics.RE.software.PixelColor.getGreen;
import static jpcsp.graphics.RE.software.PixelColor.getRed;
import static jpcsp.graphics.RE.software.PixelColor.maxBGR;
import static jpcsp.graphics.RE.software.PixelColor.minBGR;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.graphics.RE.software.PixelColor.multiplyBGR;
import static jpcsp.graphics.RE.software.PixelColor.multiplyComponent;
import static jpcsp.graphics.RE.software.PixelColor.setAlpha;
import static jpcsp.graphics.RE.software.PixelColor.setBGR;
import static jpcsp.graphics.RE.software.PixelColor.substractBGR;
import static jpcsp.util.Utilities.pixelToTexel;
import static jpcsp.util.Utilities.wrap;
import static jpcsp.util.Utilities.clamp;
import static jpcsp.util.Utilities.dot3;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.normalize3;
import static jpcsp.util.Utilities.round;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.software.Rasterizer.Range;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class RendererTemplate {
	public static boolean hasMemInt;
	public static boolean needSourceDepthRead;
	public static boolean needDestinationDepthRead;
	public static boolean needDepthWrite;
	public static boolean needTextureUV;
	public static boolean simpleTextureUV;
	public static boolean swapTextureUV;
	public static boolean needScissoringX;
	public static boolean needScissoringY;
	public static boolean transform2D;
	public static boolean clearMode;
	public static boolean clearModeColor;
	public static boolean clearModeStencil;
	public static boolean clearModeDepth;
	public static int nearZ;
	public static int farZ;
	public static boolean colorTestFlagEnabled;
	public static int colorTestFunc;
	public static boolean alphaTestFlagEnabled;
	public static int alphaFunc;
	public static int alphaRef;
	public static boolean stencilTestFlagEnabled;
	public static int stencilFunc;
	public static int stencilRef;
	public static int stencilOpFail;
	public static int stencilOpZFail;
	public static int stencilOpZPass;
	public static boolean depthTestFlagEnabled;
	public static int depthFunc;
	public static boolean blendFlagEnabled;
	public static int blendEquation;
	public static int blendSrc;
	public static int blendDst;
	public static int sfix;
	public static int dfix;
	public static boolean colorLogicOpFlagEnabled;
	public static int logicOp;
	public static int colorMask;
	public static boolean depthMask;
	public static boolean textureFlagEnabled;
	public static boolean useVertexTexture;
	public static boolean lightingFlagEnabled;
	public static boolean sameVertexColor;
	public static boolean setVertexPrimaryColor;
	public static boolean primaryColorSetGlobally;
	public static boolean isTriangle;
	public static boolean matFlagAmbient;
	public static boolean matFlagDiffuse;
	public static boolean matFlagSpecular;
	public static boolean useVertexColor;
	public static boolean textureColorDoubled;
	public static int lightMode;
	public static int texMapMode;
	public static int texProjMapMode;
	public static float texTranslateX;
	public static float texTranslateY;
	public static float texScaleX;
	public static float texScaleY;
	public static int texWrapS;
	public static int texWrapT;
	public static int textureFunc;
	public static boolean textureAlphaUsed;
	public static int psm;
	public static int texMagFilter;
	public static boolean needTextureWrapU;
	public static boolean needTextureWrapV;
	public static boolean needSourceDepthClamp;
	public static boolean isLogTraceEnabled;
	public static boolean collectStatistics;

	private static final boolean resampleTextureForMag = true;
	private static DurationStatistics statistics;

	public RendererTemplate() {
		if (collectStatistics) {
			if (statistics == null) {
				statistics = new DurationStatistics(String.format("Duration %s", getClass().getName()));
			}
		}
	}

	public static boolean isRendererWriterNative(int[] memInt, int psm) {
		return memInt != null && psm == GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
	}

	public DurationStatistics getStatistics() {
		return statistics;
	}

	public void render(final BasePrimitiveRenderer renderer) {
		doRender(renderer);
	}

	private static void doRenderStart(final BasePrimitiveRenderer renderer) {
		if (isLogTraceEnabled) {
			final PrimitiveState prim = renderer.prim;
			String comment;
			if (isTriangle) {
				if (transform2D) {
					comment = "Triangle doRender 2D";
				} else {
					comment = "Triangle doRender 3D";
				}
			} else {
				if (transform2D) {
					comment = "Sprite doRender 2D";
				} else {
					comment = "Sprite doRender 3D";
				}
			}
			VideoEngine.log.trace(String.format("%s (%d,%d)-(%d,%d) skip=%d", comment, prim.pxMin, prim.pyMin, prim.pxMax, prim.pyMax, renderer.imageWriterSkipEOL));
		}

		renderer.preRender();

		if (collectStatistics) {
			if (isTriangle) {
				if (transform2D) {
					RESoftware.triangleRender2DStatistics.start();
				} else {
					RESoftware.triangleRender3DStatistics.start();
				}
			} else {
				RESoftware.spriteRenderStatistics.start();
			}
			statistics.start();
		}
	}

	private static void doRenderEnd(final BasePrimitiveRenderer renderer) {
		if (collectStatistics) {
			statistics.end();
			if (isTriangle) {
				if (transform2D) {
					RESoftware.triangleRender2DStatistics.end();
				} else {
					RESoftware.triangleRender3DStatistics.end();
				}
			} else {
				RESoftware.spriteRenderStatistics.end();
			}
		}

		renderer.postRender();
	}

	private static IRandomTextureAccess resampleTexture(final BasePrimitiveRenderer renderer) {
		IRandomTextureAccess textureAccess = renderer.textureAccess;
		renderer.prim.needResample = false;

		if (textureFlagEnabled && (!transform2D || useVertexTexture) && !clearMode) {
			if (texMagFilter == GeCommands.TFLT_LINEAR) {
				final PrimitiveState prim = renderer.prim;
				if (needTextureUV && simpleTextureUV && transform2D) {
					if (renderer.cachedTexture != null) {
						if (Math.abs(prim.uStep) != 1f || Math.abs(prim.vStep) != 1f) {
							prim.resampleFactorWidth = 1f / Math.abs(prim.uStep);
							prim.resampleFactorHeight = 1f / Math.abs(prim.vStep);
							if (renderer.cachedTexture.canResample(prim.resampleFactorWidth, prim.resampleFactorHeight)) {
								prim.needResample = true;
								prim.uStart *= prim.resampleFactorWidth;
								prim.vStart *= prim.resampleFactorHeight;
								prim.uStep = prim.uStep < 0f ? -1f : 1f;
								prim.vStep = prim.vStep < 0f ? -1f : 1f;
							} else if (isLogTraceEnabled) {
								VideoEngine.log.trace(String.format("Cannot resample with factors %f, %f", prim.resampleFactorWidth, prim.resampleFactorHeight));
							}
						}
					}
				} else {
					if (resampleTextureForMag && !transform2D && renderer.cachedTexture != null) {
						prim.resampleFactorWidth = 2f;
						prim.resampleFactorHeight = 2f;
						prim.needResample = renderer.cachedTexture.canResample(prim.resampleFactorWidth, prim.resampleFactorHeight);
					}
				}
			}
		}

		return textureAccess;
	}

	private static void doRender(final BasePrimitiveRenderer renderer) {
		final PixelState pixel = renderer.pixel;
		final PrimitiveState prim = renderer.prim;
		final IRendererWriter rendererWriter = renderer.rendererWriter;
		final Lighting lighting = renderer.lighting;
		IRandomTextureAccess textureAccess = resampleTexture(renderer);

		doRenderStart(renderer);

		int stencilRefAlpha = 0;
		if (stencilTestFlagEnabled && !clearMode && stencilRef != 0) {
			if (stencilOpFail == SOP_REPLACE_STENCIL_VALUE || stencilOpZFail == SOP_REPLACE_STENCIL_VALUE || stencilOpZPass == SOP_REPLACE_STENCIL_VALUE) {
				// Prepare stencilRef as a ready-to-use alpha value
				stencilRefAlpha = renderer.stencilRef << 24;
			}
		}
		int stencilRefMasked = renderer.stencilRef & renderer.stencilMask;

		int notColorMask = 0xFFFFFFFF;
		if (!clearMode && colorMask != 0x00000000) {
			notColorMask = ~renderer.colorMask;
		}

		int alpha, a, b, g, r;
		int textureWidthMask = renderer.textureWidth - 1;
		int textureHeightMask = renderer.textureHeight - 1;
		float textureWidthFloat = renderer.textureWidth;
		float textureHeightFloat = renderer.textureHeight;
		final int alphaRef = renderer.alphaRef;
		final int primSourceDepth = (int) prim.p2z;
		float u = prim.uStart;
		float v = prim.vStart;
		ColorDepth colorDepth = new ColorDepth();
		PrimarySecondaryColors colors = new PrimarySecondaryColors();

		Range range = null;
		Rasterizer rasterizer = null;
		float t1uw = 0f;
		float t1vw = 0f;
		float t2uw = 0f;
		float t2vw = 0f;
		float t3uw = 0f;
		float t3vw = 0f;
		if (isTriangle) {
			if (transform2D) {
				t1uw = prim.t1u;
				t1vw = prim.t1v;
				t2uw = prim.t2u;
				t2vw = prim.t2v;
				t3uw = prim.t3u;
				t3vw = prim.t3v;
			} else {
				t1uw = prim.t1u * prim.p1wInverted;
				t1vw = prim.t1v * prim.p1wInverted;
				t2uw = prim.t2u * prim.p2wInverted;
				t2vw = prim.t2v * prim.p2wInverted;
				t3uw = prim.t3u * prim.p3wInverted;
				t3vw = prim.t3v * prim.p3wInverted;
			}
			range = new Range();
			// No need to use a Rasterizer when rendering very small area.
			// The overhead of the Rasterizer would lead to slower rendering.
			if (prim.destinationWidth >= Rasterizer.MINIMUM_WIDTH && prim.destinationHeight >= Rasterizer.MINIMUM_HEIGHT) {
				rasterizer = new Rasterizer(prim.p1x, prim.p1y, prim.p2x, prim.p2y, prim.p3x, prim.p3y, prim.pyMin, prim.pyMax);
				rasterizer.setY(prim.pyMin);
			}
		}

		int fbIndex = 0;
		int depthIndex = 0;
		int depthOffset = 0;
		final int[] memInt = renderer.memInt;
		if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
			fbIndex = renderer.fbAddress >> 2;
			depthIndex = renderer.depthAddress >> 2;
			depthOffset = (renderer.depthAddress >> 1) & 1;
		}

		// Use local variables instead of "pixel" members.
		// The Java JIT is then producing a slightly faster code.
		int sourceColor = 0;
		int sourceDepth = 0;
		int destinationColor = 0;
		int destinationDepth = 0;
		int primaryColor = renderer.primaryColor;
		int secondaryColor = 0;
		float pixelU = 0f;
		float pixelV = 0f;
		boolean needResample = prim.needResample;

        for (int y = prim.pyMin; y <= prim.pyMax; y++) {
    		int startX = prim.pxMin;
    		int endX = prim.pxMax;
    		if (isTriangle && rasterizer != null) {
    			rasterizer.getNextRange(range);
    			startX = max(range.xMin, startX);
    			endX = min(range.xMax, endX);
        		if (isLogTraceEnabled) {
        			VideoEngine.log.trace(String.format("Rasterizer line (%d-%d,%d)", startX, endX, y));
    			}
    		}
    		if (isTriangle && startX > endX) {
    			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
    				fbIndex += prim.destinationWidth + renderer.imageWriterSkipEOL;
    				if (needDepthWrite || needDestinationDepthRead) {
	    				depthOffset += prim.destinationWidth + renderer.depthWriterSkipEOL;
	    				depthIndex += depthOffset >> 1;
	    				depthOffset &= 1;
    				}
    			} else {
    				rendererWriter.skip(prim.destinationWidth + renderer.imageWriterSkipEOL, prim.destinationWidth + renderer.depthWriterSkipEOL);
    			}
    		} else {
    			if (needTextureUV && simpleTextureUV) {
    				if (swapTextureUV) {
    					v = prim.vStart;
    				} else {
    					u = prim.uStart;
    				}
    			}
	    		if (isTriangle) {
	    			int startSkip = startX - prim.pxMin;
	    			if (startSkip > 0) {
	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
	        				fbIndex += startSkip;
	        				if (needDepthWrite || needDestinationDepthRead) {
	    	    				depthOffset += startSkip;
	    	    				depthIndex += depthOffset >> 1;
	    	    				depthOffset &= 1;
	        				}
	        			} else {
		    				rendererWriter.skip(startSkip, startSkip);
	        			}
	    				if (simpleTextureUV) {
	    					if (swapTextureUV) {
	    						v += startSkip * prim.vStep;
	    					} else {
	    						u += startSkip * prim.uStep;
	    					}
	    				}
	    			}
	    			prim.computeTriangleWeights(pixel, startX, y);
	    		}
	        	for (int x = startX; x <= endX; x++) {
	            	// Use a dummy "do { } while (false);" loop to allow to exit
	            	// quickly from the pixel rendering when a filter does not pass.
	            	// When a filter does not pass, the following is executed:
	            	//      rendererWriter.skip(1, 1); // Skip the pixel
	            	//      continue;
	            	do {
	            		//
	            		// Test if the pixel is inside the triangle
	            		//
		        		if (isTriangle && !pixel.isInsideTriangle()) {
		        			// Pixel not inside triangle, skip the pixel
		            		if (isLogTraceEnabled) {
		            			VideoEngine.log.trace(String.format("Pixel (%d,%d) outside triangle (%f, %f, %f)", x, y, pixel.triangleWeight1, pixel.triangleWeight2, pixel.triangleWeight3));
	            			}
		        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        				fbIndex++;
		        				if (needDepthWrite || needDestinationDepthRead) {
		    	    				depthOffset++;
		    	    				depthIndex += depthOffset >> 1;
		    	    				depthOffset &= 1;
		        				}
		        			} else {
			        			rendererWriter.skip(1, 1);
		        			}
		        			continue;
		        		}

		        		//
		        		// Start rendering the pixel
		        		//
		        		if (transform2D) {
	        				pixel.newPixel2D();
	        			} else {
	        				pixel.newPixel3D();
	        			}

	            		//
	            		// ScissorTest (performed as soon as the pixel screen coordinates are available)
	            		//
	            		if (transform2D) {
			            	if (needScissoringX && needScissoringY) {
			        			if (!(x >= renderer.scissorX1 && x <= renderer.scissorX2 && y >= renderer.scissorY1 && y <= renderer.scissorY2)) {
				        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
				        				fbIndex++;
				        				if (needDepthWrite || needDestinationDepthRead) {
				    	    				depthOffset++;
				    	    				depthIndex += depthOffset >> 1;
				    	    				depthOffset &= 1;
				        				}
				        			} else {
					        			rendererWriter.skip(1, 1);
				        			}
			        				continue;
			        			}
			            	} else if (needScissoringX) {
			        			if (!(x >= renderer.scissorX1 && x <= renderer.scissorX2)) {
				        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
				        				fbIndex++;
				        				if (needDepthWrite || needDestinationDepthRead) {
				    	    				depthOffset++;
				    	    				depthIndex += depthOffset >> 1;
				    	    				depthOffset &= 1;
				        				}
				        			} else {
					        			rendererWriter.skip(1, 1);
				        			}
			        				continue;
			        			}
			            	} else if (needScissoringY) {
			        			if (!(y >= renderer.scissorY1 && y <= renderer.scissorY2)) {
				        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
				        				fbIndex++;
				        				if (needDepthWrite || needDestinationDepthRead) {
				    	    				depthOffset++;
				    	    				depthIndex += depthOffset >> 1;
				    	    				depthOffset &= 1;
				        				}
				        			} else {
					        			rendererWriter.skip(1, 1);
				        			}
			        				continue;
			        			}
			            	}
	            		}

		            	//
		            	// Pixel source depth
		            	//
		            	if (needSourceDepthRead) {
		        			if (isTriangle) {
		        				sourceDepth = round(pixel.getTriangleWeightedValue(prim.p1z, prim.p2z, prim.p3z));
		        			} else {
		        				sourceDepth = primSourceDepth;
		        			}
		        		}

	            		//
	            		// ScissorDepthTest (performed as soon as the pixel source depth is available)
	            		//
	            		if (!transform2D && !clearMode && needSourceDepthRead) {
		            		if (nearZ != 0x0000 || farZ != 0xFFFF) {
		            			if (sourceDepth < renderer.nearZ || sourceDepth > renderer.farZ) {
		    	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		    	        				fbIndex++;
		    	        				if (needDepthWrite || needDestinationDepthRead) {
		    	    	    				depthOffset++;
		    	    	    				depthIndex += depthOffset >> 1;
		    	    	    				depthOffset &= 1;
		    	        				}
		    	        			} else {
		    		        			rendererWriter.skip(1, 1);
		    	        			}
			        				continue;
		            			}
		            		}
	            		}

		            	//
		            	// Pixel destination color and depth
		            	//
		        		if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
	        				destinationColor = memInt[fbIndex];
	        				if (needDestinationDepthRead) {
	        					if (depthOffset == 0) {
	        						destinationDepth = memInt[depthIndex] & 0x0000FFFF;
	        					} else {
	        						destinationDepth = memInt[depthIndex] >>> 16;
	        					}
	        				}
	        			} else {
	        				rendererWriter.readCurrent(colorDepth);
			            	destinationColor = colorDepth.color;
			            	if (needDestinationDepthRead) {
			            		destinationDepth = colorDepth.depth;
			            	}
	        			}

	            		//
	            		// StencilTest (performed as soon as destination color is known)
	            		//
	            		if (stencilTestFlagEnabled && !clearMode) {
	            			switch (stencilFunc) {
	            				case GeCommands.STST_FUNCTION_NEVER_PASS_STENCIL_TEST:
	            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
	            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
	            					}
	        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
	        	        				fbIndex++;
	        	        				if (needDepthWrite || needDestinationDepthRead) {
	        	    	    				depthOffset++;
	        	    	    				depthIndex += depthOffset >> 1;
	        	    	    				depthOffset &= 1;
	        	        				}
	        	        			} else {
	        		        			rendererWriter.skip(1, 1);
	        	        			}
			        				continue;
	            				case GeCommands.STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST:
	            					// Nothing to do
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_MATCHES:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) != stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_DIFFERS:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) == stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) >= stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) > stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) <= stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	            				case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL:
	            					if ((getAlpha(destinationColor) & renderer.stencilMask) < stencilRefMasked) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), stencil test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		            					if (stencilOpFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpFail(destinationColor, stencilRefAlpha);
		            					}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
	            					}
	            					break;
	                		}
	            		}

	            		//
	            		// DepthTest (performed as soon as depths are known, but after the stencil test)
	            		//
	            		if (depthTestFlagEnabled && !clearMode) {
	            			switch (depthFunc) {
		        				case GeCommands.ZTST_FUNCTION_NEVER_PASS_PIXEL:
	            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
	            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
	            					}
	        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
	        	        				fbIndex++;
	        	        				if (needDepthWrite || needDestinationDepthRead) {
	        	    	    				depthOffset++;
	        	    	    				depthIndex += depthOffset >> 1;
	        	    	    				depthOffset &= 1;
	        	        				}
	        	        			} else {
	        		        			rendererWriter.skip(1, 1);
	        	        			}
			        				continue;
		        				case GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL:
		        					// No filter required
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL:
		        					if (sourceDepth != destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL:
		        					if (sourceDepth == destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS:
		        					if (sourceDepth >= destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL:
		        					if (sourceDepth > destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER:
		        					if (sourceDepth <= destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
		        				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL:
		        					if (sourceDepth < destinationDepth) {
		            					if (stencilTestFlagEnabled && stencilOpZFail != GeCommands.SOP_KEEP_STENCIL_VALUE) {
		            						destinationColor = stencilOpZFail(destinationColor, stencilRefAlpha);
		            					}
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), depth test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		        					}
		        					break;
	            			}
	            		}

		        		//
		        		// Primary color
		        		//
	            		if (setVertexPrimaryColor) {
	            			if (isTriangle) {
	            				if (sameVertexColor) {
	            					primaryColor = pixel.c3;
	            				} else {
	            					primaryColor = pixel.getTriangleColorWeightedValue();
	            				}
	            			}
	            		}

	            		//
	            		// Material Flags
	            		//
	            		if (lightingFlagEnabled && !transform2D && useVertexColor && isTriangle) {
	            			if (matFlagAmbient) {
	            				pixel.materialAmbient = primaryColor;
	            			}
	            			if (matFlagDiffuse) {
	            				pixel.materialDiffuse = primaryColor;
	            			}
	            			if (matFlagSpecular) {
	            				pixel.materialSpecular = primaryColor;
	            			}
	            		}

	            		//
	            		// Lighting
	            		//
	            		if (lightingFlagEnabled && !transform2D) {
	            			lighting.applyLighting(colors, pixel);
	            			primaryColor = colors.primaryColor;
	            			secondaryColor = colors.secondaryColor;
	            		}

	        			//
	        			// Pixel texture U,V
	        			//
	        			if (needTextureUV) {
		            		if (simpleTextureUV) {
			            		pixelU = u;
			            		pixelV = v;
		            		} else {
		            			// Compute the mapped texture u,v coordinates
		            			// based on the Barycentric coordinates.
		            			pixelU = pixel.getTriangleWeightedValue(t1uw, t2uw, t3uw);
		            			pixelV = pixel.getTriangleWeightedValue(t1vw, t2vw, t3vw);
		            			if (!transform2D) {
			            			// In 3D, apply a perspective correction by weighting
		            				// the coordinates by their "w" value. See
			            			// http://en.wikipedia.org/wiki/Texture_mapping#Perspective_correctness
		            				float weightInverted = 1.f / pixel.getTriangleWeightedValue(prim.p1wInverted, prim.p2wInverted, prim.p3wInverted);
		            				pixelU *= weightInverted;
		            				pixelV *= weightInverted;
		            			}
		            		}
		            	}

	            		//
	            		// Texture
	            		//
	            		if (textureFlagEnabled && (!transform2D || useVertexTexture) && !clearMode) {
		            		//
	            			// TextureMapping
		            		//
	            			if (!transform2D) {
	            				switch (texMapMode) {
	            					case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
	            						if (texScaleX != 1f) {
	            							pixelU *= renderer.texScaleX;
	            						}
	            						if (texTranslateX != 0f) {
	            							pixelU += renderer.texTranslateX;
	            						}
	            						if (texScaleY != 1f) {
	            							pixelV *= renderer.texScaleY;
	            						}
	            						if (texTranslateY != 0f) {
	            							pixelV += renderer.texTranslateY;
	            						}
	            						break;
	            					case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX:
	            						switch (texProjMapMode) {
	            							case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_POSITION:
	            								final float[] V = pixel.getV();
	            								pixelU = V[0] * pixel.textureMatrix[0] + V[1] * pixel.textureMatrix[4] + V[2] * pixel.textureMatrix[8] + pixel.textureMatrix[12];
	            								pixelV = V[0] * pixel.textureMatrix[1] + V[1] * pixel.textureMatrix[5] + V[2] * pixel.textureMatrix[9] + pixel.textureMatrix[13];
	            								//pixelQ = V[0] * pixel.textureMatrix[2] + V[1] * pixel.textureMatrix[6] + V[2] * pixel.textureMatrix[10] + pixel.textureMatrix[14];
	            								break;
	            							case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES:
	            								float tu = pixelU;
	            								float tv = pixelV;
	            								pixelU = tu * pixel.textureMatrix[0] + tv * pixel.textureMatrix[4] + pixel.textureMatrix[12];
	            								pixelV = tu * pixel.textureMatrix[1] + tv * pixel.textureMatrix[5] + pixel.textureMatrix[13];
	            								//pixelQ = tu * pixel.textureMatrix[2] + tv * pixel.textureMatrix[6] + pixel.textureMatrix[14];
	            								break;
	            							case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
	            								final float[] normalizedN = pixel.getNormalizedN();
	            								pixelU = normalizedN[0] * pixel.textureMatrix[0] + normalizedN[1] * pixel.textureMatrix[4] + normalizedN[2] * pixel.textureMatrix[8] + pixel.textureMatrix[12];
	            								pixelV = normalizedN[0] * pixel.textureMatrix[1] + normalizedN[1] * pixel.textureMatrix[5] + normalizedN[2] * pixel.textureMatrix[9] + pixel.textureMatrix[13];
	            								//pixelQ = normalizedN[0] * pixel.textureMatrix[2] + normalizedN[1] * pixel.textureMatrix[6] + normalizedN[2] * pixel.textureMatrix[10] + pixel.textureMatrix[14];
	            								break;
	            							case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
	            								final float[] N = pixel.getN();
	            								pixelU = N[0] * pixel.textureMatrix[0] + N[1] * pixel.textureMatrix[4] + N[2] * pixel.textureMatrix[8] + pixel.textureMatrix[12];
	            								pixelV = N[0] * pixel.textureMatrix[1] + N[1] * pixel.textureMatrix[5] + N[2] * pixel.textureMatrix[9] + pixel.textureMatrix[13];
	            								//pixelQ = N[0] * pixel.textureMatrix[2] + N[1] * pixel.textureMatrix[6] + N[2] * pixel.textureMatrix[10] + pixel.textureMatrix[14];
	            								break;
	            						}
	            						break;
	            					case GeCommands.TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
	            						// Implementation based on shader.vert/ApplyTexture:
	            						//
	            						//   vec3  Nn = normalize(N);
	            			            //   vec3  Ve = vec3(gl_ModelViewMatrix * V);
	            			            //   float k  = gl_FrontMaterial.shininess;
	            			            //   vec3  Lu = gl_LightSource[texShade.x].position.xyz - Ve.xyz * gl_LightSource[texShade.x].position.w;
	            			            //   vec3  Lv = gl_LightSource[texShade.y].position.xyz - Ve.xyz * gl_LightSource[texShade.y].position.w;
	            			            //   float Pu = psp_lightKind[texShade.x] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
	            			            //   float Pv = psp_lightKind[texShade.y] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
	            			            //   T.xyz = vec3(0.5*vec2(1.0 + Pu, 1.0 + Pv), 1.0);
	            						//
	            						final float[] Ve = new float[3];
	            						final float[] Ne = new float[3];
	            						final float[] Lu = new float[3];
	            						final float[] Lv = new float[3];
	            						pixel.getVe(Ve);
	            						pixel.getNormalizedNe(Ne);
	            						for (int i = 0; i < 3; i++) {
	            							Lu[i] = renderer.envMapLightPosU[i] - Ve[i] * renderer.envMapLightPosU[3];
	            							Lv[i] = renderer.envMapLightPosV[i] - Ve[i] * renderer.envMapLightPosV[3];
	            						}

	            						float Pu;
	            						if (renderer.envMapDiffuseLightU) {
	            							normalize3(Lu, Lu);
	            							Pu = dot3(Ne, Lu);
	            						} else {
	            							Lu[2] += 1f;
	            							normalize3(Lu, Lu);
	            							Pu = (float) Math.pow(dot3(Ne, Lu), renderer.envMapShininess);
	            						}

	            						float Pv;
	            						if (renderer.envMapDiffuseLightV) {
	            							normalize3(Lv, Lv);
	            							Pv = dot3(Ne, Lv);
	            						} else {
	            							Lv[2] += 1f;
	            							normalize3(Lv, Lv);
	            							Pv = (float) Math.pow(dot3(Ne, Lv), renderer.envMapShininess);
	            						}

	            						pixelU = (Pu + 1f) * 0.5f;
	            						pixelV = (Pv + 1f) * 0.5f;
	            						//pixelQ = 1f;
	            						break;
	            				}
	            			}

	            			//
	            			// Texture resampling (as late as possible)
	            			//
            				if (texMagFilter == GeCommands.TFLT_LINEAR) {
		            			if (needResample) {
		            				// Perform the resampling as late as possible.
		            				// We might be lucky that all the pixel are eliminated
		            				// by the depth or stencil tests. In which case,
		            				// we don't need to resample.
									textureAccess = renderer.cachedTexture.resample(prim.resampleFactorWidth, prim.resampleFactorHeight);
									textureWidthMask = textureAccess.getWidth() - 1;
									textureHeightMask = textureAccess.getHeight() - 1;
									textureWidthFloat = textureAccess.getWidth();
									textureHeightFloat = textureAccess.getHeight();
									needResample = false;
		            			}
            				}

            				//
	            			// TextureWrap
		            		//
	            			if (needTextureWrapU) {
		            			switch (texWrapS) {
		            				case GeCommands.TWRAP_WRAP_MODE_REPEAT:
			        					if (transform2D) {
			        						pixelU = wrap(pixelU, textureWidthMask);
			        					} else {
			        						pixelU = wrap(pixelU);
			        					}
			        					break;
		            				case GeCommands.TWRAP_WRAP_MODE_CLAMP:
			        					if (transform2D) {
			        						pixelU = clamp(pixelU, 0f, textureWidthMask);
			        					} else {
			        						// Clamp to [0..1[ (1 is excluded)
			        						pixelU = clamp(pixelU, 0f, 0.99999f);
			        					}
			        					break;
		            			}
	            			}
	            			if (needTextureWrapV) {
		            			switch (texWrapT) {
		            				case GeCommands.TWRAP_WRAP_MODE_REPEAT:
			        					if (transform2D) {
			        						pixelV = wrap(pixelV, textureHeightMask);
			        					} else {
			        						pixelV = wrap(pixelV);
			        					}
			        					break;
		            				case GeCommands.TWRAP_WRAP_MODE_CLAMP:
			        					if (transform2D) {
			        						pixelV = clamp(pixelV, 0f, textureHeightMask);
			        					} else {
			        						// Clamp to [0..1[ (1 is excluded)
			        						pixelV = clamp(pixelV, 0f, 0.99999f);
			        					}
			        					break;
		            			}
	            			}

            				//
	    					// TextureReader
		            		//
	    					if (transform2D) {
    							sourceColor = textureAccess.readPixel(pixelToTexel(pixelU), pixelToTexel(pixelV));
	    					} else {
    							sourceColor = textureAccess.readPixel(pixelToTexel(pixelU * textureWidthFloat), pixelToTexel(pixelV * textureHeightFloat));
	    					}

		            		//
	    					// TextureFunction
		            		//
	    					switch (textureFunc) {
		    					case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE:
		    						if (textureAlphaUsed) {
		    							sourceColor = multiply(sourceColor, primaryColor);
		    						} else {
		    							sourceColor = multiply(sourceColor | 0xFF000000, primaryColor);
		    						}
		    						break;
		    					case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_DECAL:
		    						if (textureAlphaUsed) {
		    							alpha = getAlpha(sourceColor);
		    							a = getAlpha(primaryColor);
		    							b = combineComponent(getBlue(primaryColor), getBlue(sourceColor), alpha);
		    							g = combineComponent(getGreen(primaryColor), getGreen(sourceColor), alpha);
		    							r = combineComponent(getRed(primaryColor), getRed(sourceColor), alpha);
		    							sourceColor = getColor(a, b, g, r);
		    						} else {
		    							sourceColor = (sourceColor & 0x00FFFFFF) | (primaryColor & 0xFF000000);
		    						}
		    						break;
		    					case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_BLEND:
		    						if (textureAlphaUsed) {
		    							a = multiplyComponent(getAlpha(sourceColor), getAlpha(primaryColor));
		    							b = combineComponent(getBlue(primaryColor), renderer.texEnvColorB, getBlue(sourceColor));
		    							g = combineComponent(getGreen(primaryColor), renderer.texEnvColorG, getGreen(sourceColor));
		    							r = combineComponent(getRed(primaryColor), renderer.texEnvColorR, getRed(sourceColor));
		    							sourceColor = getColor(a, b, g, r);
		    						} else {
		    							a = getAlpha(primaryColor);
		    							b = combineComponent(getBlue(primaryColor), renderer.texEnvColorB, getBlue(sourceColor));
		    							g = combineComponent(getGreen(primaryColor), renderer.texEnvColorG, getGreen(sourceColor));
		    							r = combineComponent(getRed(primaryColor), renderer.texEnvColorR, getRed(sourceColor));
		    							sourceColor = getColor(a, b, g, r);
		    						}
		    						break;
		    					case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE:
		    						if (!textureAlphaUsed) {
		    							sourceColor = (sourceColor & 0x00FFFFFF) | (primaryColor & 0xFF000000);
		    						}
		    						break;
		    					case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_ADD:
		    						if (textureAlphaUsed) {
		    							a = multiplyComponent(getAlpha(sourceColor), getAlpha(primaryColor));
		    							sourceColor = setAlpha(addBGR(sourceColor, primaryColor), a);
		    						} else {
		    							sourceColor = add(sourceColor & 0x00FFFFFF, primaryColor);
		    						}
		    						break;
	    					}

		            		//
	    					// ColorDoubling
		            		//
	            			if (textureColorDoubled) {
	            				if (!transform2D && lightingFlagEnabled && lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
	            					sourceColor = doubleColor(sourceColor);
	            					secondaryColor = doubleColor(secondaryColor);
	            				} else {
	            					sourceColor = doubleColor(sourceColor);
	            				}
	            			}

		            		//
	            			// SourceColor
		            		//
	            			if (!transform2D && lightingFlagEnabled && lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
	            				sourceColor = add(sourceColor, secondaryColor);
	            			}
	            		} else {
		            		//
	            			// ColorDoubling
		            		//
	            			if (textureColorDoubled) {
	            				if (!transform2D && lightingFlagEnabled && lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
	            					primaryColor = doubleColor(primaryColor);
	            					secondaryColor = doubleColor(secondaryColor);
	            				} else if (!primaryColorSetGlobally) {
	        						primaryColor = doubleColor(primaryColor);
	            				}
	            			}

		            		//
	            			// SourceColor
		            		//
	            			if (!transform2D && lightingFlagEnabled && lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
	            				sourceColor = add(primaryColor, secondaryColor);
	            			} else {
	            				sourceColor = primaryColor;
	            			}
	            		}

	            		//
	            		// ColorTest
	            		//
	            		if (colorTestFlagEnabled && !clearMode) {
	            			switch (colorTestFunc) {
		            			case GeCommands.CTST_COLOR_FUNCTION_ALWAYS_PASS_PIXEL:
		            				// Nothing to do
		            				break;
		            			case GeCommands.CTST_COLOR_FUNCTION_NEVER_PASS_PIXEL:
		    	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		    	        				fbIndex++;
		    	        				if (needDepthWrite || needDestinationDepthRead) {
		    	    	    				depthOffset++;
		    	    	    				depthIndex += depthOffset >> 1;
		    	    	    				depthOffset &= 1;
		    	        				}
		    	        			} else {
		    		        			rendererWriter.skip(1, 1);
		    	        			}
			        				continue;
		            			case GeCommands.CTST_COLOR_FUNCTION_PASS_PIXEL_IF_COLOR_MATCHES:
		            				if ((sourceColor & renderer.colorTestMsk) != renderer.colorTestRef) {
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
		            			case GeCommands.CTST_COLOR_FUNCTION_PASS_PIXEL_IF_COLOR_DIFFERS:
		            				if ((sourceColor & renderer.colorTestMsk) == renderer.colorTestRef) {
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
	            			}
	            		}

	            		//
	            		// AlphaTest
	            		//
	            		if (alphaTestFlagEnabled && !clearMode) {
	            			switch (alphaFunc) {
		            			case GeCommands.ATST_ALWAYS_PASS_PIXEL:
		            				// Nothing to do
		            				break;
		            			case GeCommands.ATST_NEVER_PASS_PIXEL:
		    	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		    	        				fbIndex++;
		    	        				if (needDepthWrite || needDestinationDepthRead) {
		    	    	    				depthOffset++;
		    	    	    				depthIndex += depthOffset >> 1;
		    	    	    				depthOffset &= 1;
		    	        				}
		    	        			} else {
		    		        			rendererWriter.skip(1, 1);
		    	        			}
			        				continue;
		            			case GeCommands.ATST_PASS_PIXEL_IF_MATCHES:
		            				if (getAlpha(sourceColor) != alphaRef) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
		            			case GeCommands.ATST_PASS_PIXEL_IF_DIFFERS:
		            				if (getAlpha(sourceColor) == alphaRef) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
		            			case GeCommands.ATST_PASS_PIXEL_IF_LESS:
		            				if (getAlpha(sourceColor) >= alphaRef) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
		            			case GeCommands.ATST_PASS_PIXEL_IF_LESS_OR_EQUAL:
		            				// No test if alphaRef==0xFF
		            				if (RendererTemplate.alphaRef < 0xFF) {
			            				if (getAlpha(sourceColor) > alphaRef) {
			    		            		if (isLogTraceEnabled) {
			    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
			    	            			}
			        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
			        	        				fbIndex++;
			        	        				if (needDepthWrite || needDestinationDepthRead) {
			        	    	    				depthOffset++;
			        	    	    				depthIndex += depthOffset >> 1;
			        	    	    				depthOffset &= 1;
			        	        				}
			        	        			} else {
			        		        			rendererWriter.skip(1, 1);
			        	        			}
					        				continue;
			            				}
		            				}
		            				break;
		            			case GeCommands.ATST_PASS_PIXEL_IF_GREATER:
		            				if (getAlpha(sourceColor) <= alphaRef) {
		    		            		if (isLogTraceEnabled) {
		    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
		    	            			}
		        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
		        	        				fbIndex++;
		        	        				if (needDepthWrite || needDestinationDepthRead) {
		        	    	    				depthOffset++;
		        	    	    				depthIndex += depthOffset >> 1;
		        	    	    				depthOffset &= 1;
		        	        				}
		        	        			} else {
		        		        			rendererWriter.skip(1, 1);
		        	        			}
				        				continue;
		            				}
		            				break;
		            			case GeCommands.ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL:
		            				// No test if alphaRef==0x00
		            				if (RendererTemplate.alphaRef > 0x00) {
			            				if (getAlpha(sourceColor) < alphaRef) {
			    		            		if (isLogTraceEnabled) {
			    		            			VideoEngine.log.trace(String.format("Pixel (%d,%d), alpha test failed, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
			    	            			}
			        	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
			        	        				fbIndex++;
			        	        				if (needDepthWrite || needDestinationDepthRead) {
			        	    	    				depthOffset++;
			        	    	    				depthIndex += depthOffset >> 1;
			        	    	    				depthOffset &= 1;
			        	        				}
			        	        			} else {
			        		        			rendererWriter.skip(1, 1);
			        	        			}
					        				continue;
			            				}
		            				}
		            				break;
		            		}
	            		}

	            		//
	            		// AlphaBlend
	            		//
	            		if (blendFlagEnabled && !clearMode) {
	            			int filteredSrc;
	            			int filteredDst;
	            			switch (blendEquation) {
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ADD:
		            				if (blendSrc == GeCommands.ALPHA_FIX && sfix == 0xFFFFFF &&
	            				        blendDst == GeCommands.ALPHA_FIX && dfix == 0x000000) {
		            					// Nothing to do, this is a NOP
		            				} else if (blendSrc == GeCommands.ALPHA_FIX && sfix == 0xFFFFFF &&
		            				           blendDst == GeCommands.ALPHA_FIX && dfix == 0xFFFFFF) {
		            					sourceColor = PixelColor.add(sourceColor, destinationColor & 0x00FFFFFF);
		            				} else if (blendSrc == GeCommands.ALPHA_SOURCE_ALPHA && blendDst == GeCommands.ALPHA_ONE_MINUS_SOURCE_ALPHA) {
		            					// This is the most common case and can be optimized
		            					int srcAlpha = sourceColor >>> 24;
		            					if (srcAlpha == ZERO) {
		            						// Set color of destination
		            						sourceColor = (sourceColor & 0xFF000000) | (destinationColor & 0x00FFFFFF);
		            					} else if (srcAlpha == ONE) {
		            						// Nothing to change
		            					} else {
		            						int oneMinusSrcAlpha = ONE - srcAlpha;
		            						filteredSrc = multiplyBGR(sourceColor, srcAlpha, srcAlpha, srcAlpha);
		            						filteredDst = multiplyBGR(destinationColor, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
		            						sourceColor = setBGR(sourceColor, addBGR(filteredSrc, filteredDst));
		            					}
		            				} else {
			            				filteredSrc = multiplyBGR(sourceColor, blendSrc(sourceColor, destinationColor, renderer.sfix));
			            				filteredDst = multiplyBGR(destinationColor, blendDst(sourceColor, destinationColor, renderer.dfix));
			            				sourceColor = setBGR(sourceColor, addBGR(filteredSrc, filteredDst));
		            				}
		            				break;
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT:
		            				filteredSrc = multiplyBGR(sourceColor, blendSrc(sourceColor, destinationColor, renderer.sfix));
		            				filteredDst = multiplyBGR(destinationColor, blendDst(sourceColor, destinationColor, renderer.dfix));
		            				sourceColor = setBGR(sourceColor, substractBGR(filteredSrc, filteredDst));
		            				break;
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT:
		            				filteredSrc = multiplyBGR(sourceColor, blendSrc(sourceColor, destinationColor, renderer.sfix));
		            				filteredDst = multiplyBGR(destinationColor, blendDst(sourceColor, destinationColor, renderer.dfix));
		            				sourceColor = setBGR(sourceColor, substractBGR(filteredDst, filteredSrc));
		            				break;
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE:
		            				// Source and destination factors are not applied
		            				sourceColor = setBGR(sourceColor, minBGR(sourceColor, destinationColor));
		            				break;
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE:
		            				// Source and destination factors are not applied
		            				sourceColor = setBGR(sourceColor, maxBGR(sourceColor, destinationColor));
		            				break;
		            			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE:
		            				// Source and destination factors are not applied
		            				sourceColor = setBGR(sourceColor, absBGR(sourceColor, destinationColor));
		            				break;
	            			}
	            		}

	            		//
	            		// StencilOpZPass
	            		//
	            		if (stencilTestFlagEnabled && !clearMode) {
	            			switch (stencilOpZPass) {
	            				case GeCommands.SOP_KEEP_STENCIL_VALUE:
	            					sourceColor = (sourceColor & 0x00FFFFFF) | (destinationColor & 0xFF000000);
	            					break;
	            				case GeCommands.SOP_ZERO_STENCIL_VALUE:
	            					sourceColor &= 0x00FFFFFF;
	            					break;
	            				case GeCommands.SOP_REPLACE_STENCIL_VALUE:
	            					if (stencilRef == 0) {
	            						// SOP_REPLACE_STENCIL_VALUE with a 0 value is equivalent
	            						// to SOP_ZERO_STENCIL_VALUE
	                					sourceColor &= 0x00FFFFFF;
	            					} else {
	            						sourceColor = (sourceColor & 0x00FFFFFF) | stencilRefAlpha;
	            					}
	            					break;
	            				case GeCommands.SOP_INVERT_STENCIL_VALUE:
	            					sourceColor = (sourceColor & 0x00FFFFFF) | ((~destinationColor) & 0xFF000000);
	            					break;
	            				case GeCommands.SOP_INCREMENT_STENCIL_VALUE:
	            					alpha = destinationColor & 0xFF000000;
	            					if (alpha != 0xFF000000) {
	            						alpha += 0x01000000;
	            					}
	            					sourceColor = (sourceColor & 0x00FFFFFF) | alpha;
	            					break;
	            				case GeCommands.SOP_DECREMENT_STENCIL_VALUE:
	            					alpha = destinationColor & 0xFF000000;
	            					if (alpha != 0x00000000) {
	            						alpha -= 0x01000000;
	            					}
	            					sourceColor = (sourceColor & 0x00FFFFFF) | alpha;
	            					break;
	            			}
	            		} else if (!clearMode) {
	            			// Write the alpha/stencil value to the frame buffer
	            			// only when the stencil test is enabled
	            			sourceColor = (sourceColor & 0x00FFFFFF) | (destinationColor & 0xFF000000);
	            		}

	            		//
	            		// ColorLogicalOperation
	            		//
	            		if (colorLogicOpFlagEnabled && !clearMode) {
	            			switch (logicOp) {
		            			case GeCommands.LOP_CLEAR:
		            				sourceColor = ZERO;
		            				break;
		            			case GeCommands.LOP_AND:
		            				sourceColor &= destinationColor;
		            				break;
		            			case GeCommands.LOP_REVERSE_AND:
		            				sourceColor &= (~destinationColor);
		            				break;
		            			case GeCommands.LOP_COPY:
		            				// This is a NOP
		            				break;
		            			case GeCommands.LOP_INVERTED_AND:
		            				sourceColor = (~sourceColor) & destinationColor;
		            				break;
		            			case GeCommands.LOP_NO_OPERATION:
		            				sourceColor = destinationColor;
		            				break;
		            			case GeCommands.LOP_EXLUSIVE_OR:
		            				sourceColor ^= destinationColor;
		            				break;
		            			case GeCommands.LOP_OR:
		            				sourceColor |= destinationColor;
		            				break;
		            			case GeCommands.LOP_NEGATED_OR:
		            				sourceColor = ~(sourceColor | destinationColor);
		            				break;
		            			case GeCommands.LOP_EQUIVALENCE:
		            				sourceColor = ~(sourceColor ^ destinationColor);
		            				break;
		            			case GeCommands.LOP_INVERTED:
		            				sourceColor = ~destinationColor;
		            				break;
		            			case GeCommands.LOP_REVERSE_OR:
		            				sourceColor |= (~destinationColor);
		            				break;
		            			case GeCommands.LOP_INVERTED_COPY:
		            				sourceColor = ~sourceColor;
		            				break;
		            			case GeCommands.LOP_INVERTED_OR:
		            				sourceColor = (~sourceColor) | destinationColor;
		            				break;
		            			case GeCommands.LOP_NEGATED_AND:
		            				sourceColor = ~(sourceColor & destinationColor);
		            				break;
		            			case GeCommands.LOP_SET:
		            				sourceColor = 0xFFFFFFFF;
		            				break;
		            		}
	            		}

	            		//
	            		// ColorMask
	            		//
	            		if (clearMode) {
	            			if (clearModeColor) {
	            				if (!clearModeStencil) {
	            					sourceColor = (sourceColor & 0x00FFFFFF) | (destinationColor & 0xFF000000);
	            				}
	            			} else {
	            				if (clearModeStencil) {
	            					sourceColor = (sourceColor & 0xFF000000) | (destinationColor & 0x00FFFFFF);
	            				} else {
	            					sourceColor = destinationColor;
	            				}
	            			}
	            		} else {
	            			if (colorMask != 0x00000000) {
	            				sourceColor = (sourceColor & notColorMask) | (destinationColor & colorMask);
	            			}
	            		}

	            		//
	            		// DepthMask
	            		//
	            		if (needDepthWrite) {
		            		if (clearMode) {
		            			if (!clearModeDepth) {
		            				sourceDepth = destinationDepth;
		            			}
		            		} else if (!depthTestFlagEnabled) {
		            			// Depth writes are disabled when the depth test is not enabled.
		            			sourceDepth = destinationDepth;
		            		} else if (!depthMask) {
		            			sourceDepth = destinationDepth;
		            		}
	            		}

	            		//
	            		// Filter passed
	            		//
	            		if (isLogTraceEnabled) {
	            			VideoEngine.log.trace(String.format("Pixel (%d,%d), passed=true, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d", x, y, pixelU, pixelV, sourceColor, destinationColor, primaryColor, secondaryColor, sourceDepth, destinationDepth));
            			}
	            		if (needDepthWrite && needSourceDepthClamp) {
	            			// Clamp between 0 and 65535
	            			sourceDepth = Math.max(0, Math.min(sourceDepth, 65535));
	            		}
	        			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
	        				memInt[fbIndex] = sourceColor;
	        				fbIndex++;
	        				if (needDepthWrite) {
		        				if (depthOffset == 0) {
		        					memInt[depthIndex] = (memInt[depthIndex] & 0xFFFF0000) | (sourceDepth & 0x0000FFFF);
		        					depthOffset = 1;
		        				} else {
		        					memInt[depthIndex] = (memInt[depthIndex] & 0x0000FFFF) | (sourceDepth << 16);
		        					depthIndex++;
		        					depthOffset = 0;
		        				}
	        				} else if (needDestinationDepthRead) {
	    	    				depthOffset++;
	    	    				depthIndex += depthOffset >> 1;
	    	    				depthOffset &= 1;
	        				}
	        			} else {
		            		if (needDepthWrite) {
		            			colorDepth.color = sourceColor;
		            			colorDepth.depth = sourceDepth;
			            		rendererWriter.writeNext(colorDepth);
		            		} else {
			            		rendererWriter.writeNextColor(sourceColor);
		            		}
	        			}
	            	} while (false);

	            	if (needTextureUV && simpleTextureUV) {
	            		if (swapTextureUV) {
	            			v += prim.vStep;
	            		} else {
	            			u += prim.uStep;
	            		}
	            	}
	            	if (isTriangle) {
	            		prim.deltaXTriangleWeigths(pixel);
	            	}
	        	}

	        	int skip = prim.pxMax - endX;
    			if (hasMemInt && psm == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
    				fbIndex += skip + renderer.imageWriterSkipEOL;
    				if (needDepthWrite || needDestinationDepthRead) {
	    				depthOffset += skip + renderer.depthWriterSkipEOL;
	    				depthIndex += depthOffset >> 1;
	    				depthOffset &= 1;
    				}
    			} else {
    	        	rendererWriter.skip(skip + renderer.imageWriterSkipEOL, skip + renderer.depthWriterSkipEOL);
    			}
    		}

    		if (needTextureUV && simpleTextureUV) {
    			if (swapTextureUV) {
    				u += prim.uStep;
    			} else {
    				v += prim.vStep;
    			}
        	}
        }

        doRenderEnd(renderer);
	}

	protected static int stencilOpFail(int destination, int stencilRefAlpha) {
		int alpha;
		switch (stencilOpFail) {
			case GeCommands.SOP_KEEP_STENCIL_VALUE:
				return destination;
			case GeCommands.SOP_ZERO_STENCIL_VALUE:
				return destination & 0x00FFFFFF;
			case GeCommands.SOP_REPLACE_STENCIL_VALUE:
				if (stencilRef == 0) {
					// SOP_REPLACE_STENCIL_VALUE with a 0 value is equivalent
					// to SOP_ZERO_STENCIL_VALUE
					return destination & 0x00FFFFFF;
				}
				return (destination & 0x00FFFFFF) | stencilRefAlpha;
			case GeCommands.SOP_INVERT_STENCIL_VALUE:
				return destination ^ 0xFF000000;
			case GeCommands.SOP_INCREMENT_STENCIL_VALUE:
				alpha = destination & 0xFF000000;
				if (alpha != 0xFF000000) {
					alpha += 0x01000000;
				}
				return (destination & 0x00FFFFFF) | alpha;
			case GeCommands.SOP_DECREMENT_STENCIL_VALUE:
				alpha = destination & 0xFF000000;
				if (alpha != 0x00000000) {
					alpha -= 0x01000000;
				}
				return (destination & 0x00FFFFFF) | alpha;
		}

		return destination;
	}

	protected static int stencilOpZFail(int destination, int stencilRefAlpha) {
		int alpha;
		switch (stencilOpZFail) {
			case GeCommands.SOP_KEEP_STENCIL_VALUE:
				return destination;
			case GeCommands.SOP_ZERO_STENCIL_VALUE:
				return destination & 0x00FFFFFF;
			case GeCommands.SOP_REPLACE_STENCIL_VALUE:
				if (stencilRef == 0) {
					// SOP_REPLACE_STENCIL_VALUE with a 0 value is equivalent
					// to SOP_ZERO_STENCIL_VALUE
					return destination & 0x00FFFFFF;
				}
				return (destination & 0x00FFFFFF) | stencilRefAlpha;
			case GeCommands.SOP_INVERT_STENCIL_VALUE:
				return destination ^ 0xFF000000;
			case GeCommands.SOP_INCREMENT_STENCIL_VALUE:
				alpha = destination & 0xFF000000;
				if (alpha != 0xFF000000) {
					alpha += 0x01000000;
				}
				return (destination & 0x00FFFFFF) | alpha;
			case GeCommands.SOP_DECREMENT_STENCIL_VALUE:
				alpha = destination & 0xFF000000;
				if (alpha != 0x00000000) {
					alpha -= 0x01000000;
				}
				return (destination & 0x00FFFFFF) | alpha;
		}

		return destination;
	}

	protected static int blendSrc(int source, int destination, int fix) {
		int alpha;
		switch (blendSrc) {
			case GeCommands.ALPHA_SOURCE_COLOR:
				return source;
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_COLOR:
				return 0xFFFFFFFF - source;
			case GeCommands.ALPHA_SOURCE_ALPHA:
				alpha = getAlpha(source);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_ALPHA:
				alpha = ONE - getAlpha(source);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DESTINATION_ALPHA:
				alpha = getAlpha(destination);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DESTINATION_ALPHA:
				alpha = ONE - getAlpha(destination);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DOUBLE_SOURCE_ALPHA:
				alpha = doubleComponent(getAlpha(source));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_SOURCE_ALPHA:
				alpha = ONE - doubleComponent(getAlpha(source));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DOUBLE_DESTINATION_ALPHA:
				alpha = doubleComponent(getAlpha(destination));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_DESTINATION_ALPHA:
				alpha = ONE - doubleComponent(getAlpha(destination));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_FIX:
				return fix;
		}

		return source;
	}

	protected static int blendDst(int source, int destination, int fix) {
		int alpha;
		switch (blendDst) {
			case GeCommands.ALPHA_DESTINATION_COLOR:
				return destination;
			case GeCommands.ALPHA_ONE_MINUS_DESTINATION_COLOR:
				return 0xFFFFFFFF - destination;
			case GeCommands.ALPHA_SOURCE_ALPHA:
				alpha = getAlpha(source);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_ALPHA:
				alpha = ONE - getAlpha(source);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DESTINATION_ALPHA:
				alpha = getAlpha(destination);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DESTINATION_ALPHA:
				alpha = ONE - getAlpha(destination);
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DOUBLE_SOURCE_ALPHA:
				alpha = doubleComponent(getAlpha(source));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_SOURCE_ALPHA:
				alpha = ONE - doubleComponent(getAlpha(source));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_DOUBLE_DESTINATION_ALPHA:
				alpha = doubleComponent(getAlpha(destination));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_DESTINATION_ALPHA:
				alpha = ONE - doubleComponent(getAlpha(destination));
				return getColorBGR(alpha, alpha, alpha);
			case GeCommands.ALPHA_FIX:
				return fix;
		}

		return destination;
	}
}

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

import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.util.Utilities.round;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.ImageReader;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 * This class is the base for all renderers.
 * It can be re-used for multiple primitives (e.g. multiple triangles)
 * belonging to the same IRenderingEngine.drawArrays call.
 * This class contains all the information based
 * on the GeContext but has no vertex-specific information.
 */
public abstract class BaseRenderer implements IRenderer {
	protected static final Logger log = VideoEngine.log;
    protected final boolean isLogTraceEnabled;
    protected final boolean isLogDebugEnabled;
    protected final boolean isLogInfoEnabled;

    protected static final boolean captureEachPrimitive = false;
	protected static final boolean captureZbuffer = false;
    public static final int depthBufferPixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
    protected static final int MAX_NUMBER_FILTERS = 13;
	protected IMemoryReaderWriter imageWriter;
	protected IMemoryReaderWriter depthWriter;
	protected int imageWriterSkipEOL;
	protected int depthWriterSkipEOL;
	protected final IPixelFilter[] filters = new IPixelFilter[MAX_NUMBER_FILTERS];
	protected int numberFilters;
	protected IPixelFilter compiledFilter;
	protected int compiledFilterId;
	protected boolean transform2D;
	protected int viewportWidth;
	protected int viewportHeight;
	protected int viewportX;
	protected int viewportY;
	protected int screenOffsetX;
	protected int screenOffsetY;
	protected float zscale;
	protected float zpos;
	protected int nearZ;
	protected int farZ;
	protected int scissorX1, scissorY1;
	protected int scissorX2, scissorY2;
	protected boolean setVertexPrimaryColor;
	protected int primaryColorFilter = -1;
	protected int fbp, fbw, psm;
	protected int zbp, zbw;
	protected boolean clearMode;
	protected boolean cullFaceEnabled;
	protected boolean frontFaceCw;
	protected boolean clipPlanesEnabled;
	protected boolean useVertexTexture;
	protected int scissorFilter = -1;
	protected IRandomTextureAccess textureAccess;
	protected int mipmapLevel = 0;
	protected IPixelFilter lightingFilter;
	private static HashMap<Integer, Integer> filtersStatistics = new HashMap<Integer, Integer>();
	private static HashMap<Integer, String> filterNames = new HashMap<Integer, String>();

	protected void copy(BaseRenderer from) {
		numberFilters = from.numberFilters;
		System.arraycopy(from.filters, 0, filters, 0, numberFilters);
		imageWriter = from.imageWriter;
		depthWriter = from.depthWriter;
		imageWriterSkipEOL = from.imageWriterSkipEOL;
		depthWriterSkipEOL = from.depthWriterSkipEOL;
		compiledFilter = from.compiledFilter;
		transform2D = from.transform2D;
	}

	protected BaseRenderer() {
		isLogTraceEnabled = log.isTraceEnabled();
		isLogDebugEnabled = log.isDebugEnabled();
		isLogInfoEnabled = log.isInfoEnabled();
	}

	protected int getTextureAddress(int address, int x, int y, int textureWidth, int pixelFormat) {
		int bytesPerPixel = IRenderingEngine.sizeOfTextureType[pixelFormat];
		int numberOfPixels = y * textureWidth + x;
		// bytesPerPixel == 0 means 2 pixels per byte (4bit indexed)
		return address + (bytesPerPixel == 0 ? numberOfPixels >> 1 : numberOfPixels * bytesPerPixel);
	}

	private static int getFrameBufferAddress(int addr) {
		addr &= Memory.addressMask;
		if (addr < MemoryMap.START_VRAM) {
			addr += MemoryMap.START_VRAM;
		}

		return addr;
	}

	protected void init(GeContext context, CachedTexture texture, boolean useVertexTexture, boolean isTriangle) {
		fbp = getFrameBufferAddress(context.fbp);
		fbw = context.fbw;
		psm = context.psm;
		zbp = getFrameBufferAddress(context.zbp);
		zbw = context.zbw;
		nearZ = round(context.nearZ * 0xFFFF);
		farZ = round(context.farZ * 0xFFFF);
		numberFilters = 0;
		scissorX1 = context.scissor_x1;
		scissorY1 = context.scissor_y1;
		scissorX2 = context.scissor_x2;
		scissorY2 = context.scissor_y2;
		clearMode = context.clearMode;
		cullFaceEnabled = context.cullFaceFlag.isEnabled();
		frontFaceCw = context.frontFaceCw;
		clipPlanesEnabled = context.clipPlanesFlag.isEnabled();
		this.useVertexTexture = useVertexTexture;

		transform2D = context.vinfo.transform2D;
		if (!transform2D) {
			viewportWidth = context.viewport_width;
			viewportHeight = context.viewport_height;
			viewportX = context.viewport_cx;
			viewportY = context.viewport_cy;
			screenOffsetX = context.offset_x;
			screenOffsetY = context.offset_y;
			zscale = context.zscale * 65535.f;
			zpos = context.zpos * 65535.f;
		}

		prepareTextureReader(context, texture, isTriangle);
        prepareFilters(context);
	}

	private static final String getLightState(GeContext context, int l) {
		return context.lightFlags[l].isEnabled() ? "ON" : "OFF";
	}

	private void prepareTextureReader(GeContext context, CachedTexture texture, boolean isTriangle) {
        lightingFilter = Lighting.getLighting(context, context.view_uploaded_matrix);

        // Is the lighting model using the material color from the vertex color?
        if (!isNopFilter(lightingFilter) && context.mat_flags != 0 && context.useVertexColor && context.vinfo.color != 0) {
			// Reserve an empty filter slot. The filter will be set by the
			// BasePrimitiveRenderer when the vertices are known.
			if (isLogTraceEnabled) {
				log.trace(String.format("Using VertexColorFilter"));
			}
        	primaryColorFilter = allocateFilterSlot();
        	filters[primaryColorFilter] = null;
        	setVertexPrimaryColor = true;

			if (isLogTraceEnabled) {
				log.trace(String.format("Using MaterialColorFilter material flags=%d", context.mat_flags));
			}
        	addFilter(MaterialColorFilter.getMaterialColorFilter(context));
        }

        boolean primaryColorSetGlobally = false;
        boolean added = addFilter(lightingFilter);
        if (added) {
        	if (isLogTraceEnabled) {
        		log.trace(String.format("Using Lighting Filter %s, %s, %s, %s", getLightState(context, 0), getLightState(context, 1), getLightState(context, 2), getLightState(context, 3)));
        	}
        } else {
        	// No lighting, take the primary color from the vertex.
        	// This will be done by the BasePrimitiveRenderer when the vertices are known.
    		if (context.useVertexColor && context.vinfo.color != 0) {
    			setVertexPrimaryColor = true;
    			if (isTriangle) {
    				// Reserve an empty filter slot. The filter will be set by the
    				// BasePrimitiveRenderer when the vertices are known.
    				primaryColorFilter = allocateFilterSlot();
    			} else {
    				// Use the color of the 2nd sprite vertex
    				primaryColorSetGlobally = true;
    			}
    		} else {
    			// Use context.vertexColor as the primary color
    			primaryColorSetGlobally = true;
    		}
        }

        IPixelFilter textureFilter;
        textureAccess = null;
    	if (context.textureFlag.isEnabled() && (!transform2D || useVertexTexture)) {
    		int textureBufferWidth = VideoEngine.alignBufferWidth(context.texture_buffer_width[mipmapLevel], context.texture_storage);
    		int textureHeight = context.texture_height[mipmapLevel];
            int textureAddress = context.texture_base_pointer[mipmapLevel];
        	textureAccess = texture;
        	if (textureAccess == null) {
            	int[] clut32 = VideoEngine.getInstance().readClut32(mipmapLevel);
            	short[] clut16 = VideoEngine.getInstance().readClut16(mipmapLevel);
	        	// Always request the whole buffer width, the texture will be cropped
	        	// as required in the next step
	            IMemoryReader imageReader = ImageReader.getImageReader(textureAddress, textureBufferWidth, textureHeight, textureBufferWidth, context.texture_storage, context.texture_swizzle, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_num_blocks, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, clut32, clut16);
	            textureAccess = new RandomTextureAccessReader(imageReader, textureBufferWidth, textureHeight);
        	}
        	// Avoid an access outside the texture area
        	textureAccess = new TextureClip(textureAccess, textureBufferWidth, textureHeight);

            if (!transform2D) {
            	// Perform the texture mapping (UV / texture matrix / environment map)
            	textureFilter = TextureMapping.getTextureMapping(context);
            	addFilter(textureFilter);
            }

            // Apply the texture wrap mode (clamp or repeat)
            textureFilter = TextureWrap.getTextureWrap(context, mipmapLevel);
            addFilter(textureFilter);

            // Read the corresponding texture texel
            textureFilter = TextureReader.getTextureReader(textureAccess, context, mipmapLevel);
            addFilter(textureFilter);

            // Apply the texture function (modulate, decal, blend, replace, add)
            textureFilter = TextureFunction.getTextureFunction(context);
            addFilter(textureFilter);

            addFilter(ColorDoubling.getColorDoubling(context, false, false));

            addFilter(SourceColorFilter.getSourceColorFilter(context, false));
		} else {
			if (primaryColorSetGlobally && isLogTraceEnabled) {
				log.trace(String.format("Using ColorTextureFilter vertexColor=0x%08X", getColor(context.vertexColor)));
			}
            addFilter(ColorDoubling.getColorDoubling(context, true, primaryColorSetGlobally));

            addFilter(SourceColorFilter.getSourceColorFilter(context, true));
		}
	}

	private int allocateFilterSlot() {
		return numberFilters++;
	}

	private boolean isNopFilter(IPixelFilter filter) {
		return filter == null || filter == NopFilter.NOP;
	}

	private boolean addFilter(IPixelFilter filter) {
		if (isNopFilter(filter)) {
			return false;
		}

		int filterSlot = allocateFilterSlot();
		filters[filterSlot] = filter;

		return true;
	}

	private void prepareFilters(GeContext context) {
		boolean added;

        //
        // Add all the enabled tests and filters, in the correct processing order
        //
        if (transform2D) {
        	// Reserve a filter slot for the scissor
        	scissorFilter = allocateFilterSlot();
        	filters[scissorFilter] = null;
        }
        if (!transform2D && !context.clearMode) {
        	added = addFilter(ScissorDepthFilter.getScissorDepthFilter(context, nearZ, farZ));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using ScissorDepthFilter near=%f, far=%f", context.nearZ, context.farZ));
        	}
        }
        if (context.colorTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(ColorTestFilter.getColorTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using ColorTestFilter func=%d, ref=0x%02X, mask=0x%02X", context.colorTestFunc, context.colorTestRef, context.colorTestMsk));
        	}
        }
        if (context.alphaTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaTestFilter.getAlphaTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using AlphaTestFilter func=%d, ref=0x%02X", context.alphaFunc, context.alphaRef));
        	}
        }
        if (context.stencilTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(StencilTestFilter.getStencilTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using StencilTestFilter func=%d, ref=0x%02X, mask=0x%02X, stencilOpFail=%d, stencilOpZFail=%d, stencilOpZPass=%d", context.stencilFunc, context.stencilRef, context.stencilMask, context.stencilOpFail, context.stencilOpZFail, context.stencilOpZPass));
        	}
        }
        if (context.depthTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(DepthTestFilter.getDepthTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using DepthTestFilter func=%d", context.depthFunc));
        	}
        }
        if (context.blendFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaBlendFilter.getAlphaBlendFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using AlphaBlendFilter func=%d, src=%d, dst=%d, sfix=0x%06X, dfix=0x%06X", context.blendEquation, context.blend_src, context.blend_dst, context.sfix, context.dfix));
        	}
        }
        if (context.stencilTestFlag.isEnabled() && !context.clearMode) {
        	// Execute the stencilOpZPass operation when both the stencil and the depth tests
        	// were successful. This has to be executed only after the AlphaBlendFilter because
        	// it still requires the unmodified destination alpha.
        	added = addFilter(StencilTestFilter.getStencilOp(context.stencilOpZPass, context.stencilRef, false));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using StencilOpFilter stencilOpZPass=%d, ref=0x%02X", context.stencilOpZPass, context.stencilRef));
        	}
        }
        if (context.colorLogicOpFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(LogicalOperationFilter.getLogicalOperationFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using LogicalOperationFilter func=%d", context.logicOp));
        	}
        }
        {
        	added = addFilter(MaskFilter.getMaskFilter(context, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using MaskFilter colorMask=0x%08X, depthMask=%b, clearMode=%b, clearModeColor=%b, clearModeStencil=%b, clearModeDepth=%b", getColor(context.colorMask), context.depthMask, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	}
        }
	}

	protected void preRender() {
	}

	protected void postRender() {
        imageWriter.flush();
        depthWriter.flush();

        if (captureEachPrimitive && State.captureGeNextFrame) {
        	// Capture the GE screen after each primitive
        	Modules.sceDisplayModule.captureGeImage();
        }
        if (captureZbuffer && State.captureGeNextFrame) {
        	captureZbufferImage();
        }
	}

	protected boolean hasFlag(int flags, int flag) {
		return (flags & flags) != 0;
	}

	protected int getFiltersFlags() {
		int flags = 0;

		for (int i = 0; i < numberFilters; i++) {
			flags |= filters[i].getFlags();
		}

		return flags;
	}

	protected void captureZbufferImage() {
		GeContext context = VideoEngine.getInstance().getContext();
		int width = context.zbw;
		int height = Modules.sceDisplayModule.getHeightFb();
		int address = getTextureAddress(zbp, 0, 0, zbw, depthBufferPixelFormat);
		Buffer buffer = Memory.getInstance().getBuffer(address, width * height * IRenderingEngine.sizeOfTextureType[depthBufferPixelFormat]);
		CaptureManager.captureImage(address, 0, buffer, width, height, width, depthBufferPixelFormat, false, 0, false, false);
	}

	protected void writerSkip(int count) {
		imageWriter.skip(count);
		depthWriter.skip(count);
	}

	protected void writerSkipEOL(int count) {
		imageWriter.skip(count + imageWriterSkipEOL);
		depthWriter.skip(count + depthWriterSkipEOL);
	}

	protected void writerSkipEOL() {
		imageWriter.skip(imageWriterSkipEOL);
		depthWriter.skip(depthWriterSkipEOL);
	}

	static protected int mixIds(int id1, int id2, int id3) {
		return mixIds(mixIds(id1, id2), id3);
	}

	static protected int mixIds(int id1, int id2) {
		return id1 + id2;
	}

	protected int getFiltersId() {
		int filtersId = 0;
		for (int i = 0; i < numberFilters; i++) {
			filtersId = mixIds(filtersId, filters[i].getCompilationId());
		}

		return filtersId;
	}

	protected String getFilterName(IPixelFilter filter) {
		String s = filter.toString();
		if (s.indexOf('@') >= 0) {
			return filter.getClass().getName().replace("jpcsp.graphics.RE.software.", "");
		}
		return s;
	}

	protected String getFiltersName() {
		if (compiledFilter != null) {
			return getFilterName(compiledFilter);
		}

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < numberFilters; i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(getFilterName(filters[i]));
		}

		return s.toString();
	}

	protected void statisticsFilters(int numberPixels) {
		if (!DurationStatistics.collectStatistics || !isLogInfoEnabled) {
			return;
		}

		int filtersId = compiledFilter != null ? compiledFilterId : getFiltersId();

		Integer count = filtersStatistics.get(filtersId);
		if (count == null) {
			count = 0;
		}
		filtersStatistics.put(filtersId, count + numberPixels);

		if (!filterNames.containsKey(filtersId)) {
			filterNames.put(filtersId, getFiltersName());
		}
	}

	public static void exit() {
		if (!log.isInfoEnabled() || filtersStatistics.isEmpty()) {
			return;
		}

		Integer[] filterIds = filtersStatistics.keySet().toArray(new Integer[filtersStatistics.size()]);
		Arrays.sort(filterIds, new FilterComparator());
		for (Integer filterId : filterIds) {
			Integer count = filtersStatistics.get(filterId);
			log.info(String.format("Filter: count=%d, id=%d, %s", count, filterId, filterNames.get(filterId)));
		}
	}

	private static class FilterComparator implements Comparator<Integer> {
		@Override
		public int compare(Integer o1, Integer o2) {
			return filtersStatistics.get(o1).compareTo(filtersStatistics.get(o2));
		}
	}
}

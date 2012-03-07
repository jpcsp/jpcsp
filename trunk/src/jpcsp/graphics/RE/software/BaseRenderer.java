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
import static jpcsp.graphics.RE.software.PixelColor.getColorBGR;
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
import jpcsp.memory.FastMemory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.ImageReader;
import jpcsp.util.DurationStatistics;
import jpcsp.util.LongLongKey;
import jpcsp.util.Utilities;

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
    protected static final int MAX_NUMBER_FILTERS = 15;
	public int[] memInt;
	public int imageWriterSkipEOL;
	public int depthWriterSkipEOL;
	protected RendererTemplate compiledRenderer;
	protected LongLongKey compiledRendererKey;
	protected LongLongKey baseRendererKey;
	protected boolean transform2D;
	protected int viewportWidth;
	protected int viewportHeight;
	protected int viewportX;
	protected int viewportY;
	protected int screenOffsetX;
	protected int screenOffsetY;
	protected float zscale;
	protected float zpos;
	public int nearZ;
	public int farZ;
	public int scissorX1, scissorY1;
	public int scissorX2, scissorY2;
	protected boolean setVertexPrimaryColor;
	protected boolean sameVertexColor;
	protected int fbp, fbw, psm;
	protected int zbp, zbw;
	protected boolean clearMode;
	protected boolean cullFaceEnabled;
	protected boolean frontFaceCw;
	protected boolean clipPlanesEnabled;
	protected boolean useVertexTexture;
	public IRandomTextureAccess textureAccess;
	protected int mipmapLevel = 0;
	public Lighting lighting;
	private static HashMap<LongLongKey, Integer> filtersStatistics = new HashMap<LongLongKey, Integer>();
	private static HashMap<LongLongKey, String> filterNames = new HashMap<LongLongKey, String>();
	protected boolean renderingInitialized;
	public CachedTextureResampled cachedTexture;
	protected boolean isTriangle;
	public int colorTestRef;
	public int colorTestMsk;
	public int alphaRef;
	public int stencilRef;
	public int stencilMask;
	public int sfix;
	public int dfix;
	public int colorMask;
	public boolean primaryColorSetGlobally;
	public float texTranslateX;
	public float texTranslateY;
	public float texScaleX = 1f;
	public float texScaleY = 1f;
	public int textureWidth;
	public int textureHeight;
	public int texEnvColorB;
	public int texEnvColorG;
	public int texEnvColorR;
	public float[] envMapLightPosU;
	public float[] envMapLightPosV;
	public boolean envMapDiffuseLightU;
	public boolean envMapDiffuseLightV;
	public float envMapShininess;
	public int texMinFilter;
	public int texMagFilter;
	public int primaryColor;

	protected void copy(BaseRenderer from) {
		imageWriterSkipEOL = from.imageWriterSkipEOL;
		depthWriterSkipEOL = from.depthWriterSkipEOL;
		compiledRendererKey = from.compiledRendererKey;
		compiledRenderer = from.compiledRenderer;
		lighting = from.lighting;
		textureAccess = from.textureAccess;
		memInt = from.memInt;
		transform2D = from.transform2D;
		nearZ = from.nearZ;
		farZ = from.farZ;
		scissorX1 = from.scissorX1;
		scissorY1 = from.scissorY1;
		scissorX2 = from.scissorX2;
		scissorY2 = from.scissorY2;
		isTriangle = from.isTriangle;
		colorTestRef = from.colorTestRef;
		colorTestMsk = from.colorTestMsk;
		alphaRef = from.alphaRef;
		stencilRef = from.stencilRef;
		stencilMask = from.stencilMask;
		sfix = from.sfix;
		dfix = from.dfix;
		colorMask = from.colorMask;
		primaryColorSetGlobally = from.primaryColorSetGlobally;
		texTranslateX = from.texTranslateX;
		texTranslateY = from.texTranslateY;
		texScaleX = from.texScaleX;
		texScaleY = from.texScaleY;
		textureWidth = from.textureWidth;
		textureHeight = from.textureHeight;
		texEnvColorB = from.texEnvColorB;
		texEnvColorG = from.texEnvColorG;
		texEnvColorR = from.texEnvColorR;
		envMapDiffuseLightU = from.envMapDiffuseLightU;
		envMapDiffuseLightV = from.envMapDiffuseLightV;
		envMapShininess = from.envMapShininess;
		texMinFilter = from.texMinFilter;
		texMagFilter = from.texMagFilter;
		primaryColor = from.primaryColor;
	}

	protected BaseRenderer() {
		isLogTraceEnabled = log.isTraceEnabled();
		isLogDebugEnabled = log.isDebugEnabled();
		isLogInfoEnabled = log.isInfoEnabled();
	}

	protected int getTextureAddress(int address, int x, int y, int textureWidth, int pixelFormat) {
		int bytesPerPixel = IRenderingEngine.sizeOfTextureType[pixelFormat];
		int numberOfPixels = y * textureWidth + x;
		address &= Memory.addressMask;
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

	protected void init(GeContext context, CachedTextureResampled texture, boolean useVertexTexture, boolean isTriangle) {
		this.cachedTexture = texture;
		this.useVertexTexture = useVertexTexture;
		this.isTriangle = isTriangle;
		nearZ = round(context.nearZ * 0xFFFF);
		farZ = round(context.farZ * 0xFFFF);
		scissorX1 = context.scissor_x1;
		scissorY1 = context.scissor_y1;
		scissorX2 = context.scissor_x2;
		scissorY2 = context.scissor_y2;
		clearMode = context.clearMode;
		cullFaceEnabled = context.cullFaceFlag.isEnabled();
		frontFaceCw = context.frontFaceCw;
		clipPlanesEnabled = context.clipPlanesFlag.isEnabled();
		fbw = context.fbw;
		zbw = context.zbw;

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
			if (context.tex_map_mode == GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV) {
				texTranslateX = context.tex_translate_x;
				texTranslateY = context.tex_translate_y;
				texScaleX = context.tex_scale_x;
				texScaleY = context.tex_scale_y;
			} else if (context.tex_map_mode == GeCommands.TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP) {
				envMapLightPosU = new float[4];
				envMapLightPosV = new float[4];
				Utilities.copy(envMapLightPosU, context.light_pos[context.tex_shade_u]);
				Utilities.copy(envMapLightPosV, context.light_pos[context.tex_shade_v]);
				envMapDiffuseLightU = context.light_type[context.tex_shade_u] == GeCommands.LIGHT_AMBIENT_DIFFUSE;
				envMapDiffuseLightV = context.light_type[context.tex_shade_v] == GeCommands.LIGHT_AMBIENT_DIFFUSE;
				envMapShininess = context.materialShininess;
			}
		}
	}

	protected void initRendering(GeContext context) {
		if (renderingInitialized) {
			return;
		}

		fbp = getFrameBufferAddress(context.fbp);
		psm = context.psm;
		zbp = getFrameBufferAddress(context.zbp);
		colorTestRef = getColorBGR(context.colorTestRef);
		colorTestMsk = getColorBGR(context.colorTestMsk);
		alphaRef = context.alphaRef;
		stencilRef = context.stencilRef & context.stencilMask;
		stencilMask = context.stencilMask;
		sfix = context.sfix;
		dfix = context.dfix;
		colorMask = getColor(context.colorMask);
		textureWidth = context.texture_width[mipmapLevel];
		textureHeight = context.texture_height[mipmapLevel];
		texEnvColorB = getColor(context.tex_env_color[2]);
		texEnvColorG = getColor(context.tex_env_color[1]);
		texEnvColorR = getColor(context.tex_env_color[0]);
		texMinFilter = context.tex_min_filter;
		texMagFilter = context.tex_mag_filter;
		primaryColor = getColor(context.vertexColor);

		Memory mem = Memory.getInstance();
		if (mem instanceof FastMemory) {
			memInt = ((FastMemory) mem).getAll();
		}

		baseRendererKey = getBaseRendererKey(context);

		if (!transform2D && context.lightingFlag.isEnabled()) {
			lighting = new Lighting(context.view_uploaded_matrix,
			               context.mat_emissive,
			               context.ambient_light,
			               context.lightFlags,
			               context.light_pos,
			               context.light_kind,
			               context.light_type,
			               context.lightAmbientColor,
			               context.lightDiffuseColor,
			               context.lightSpecularColor,
			               context.lightConstantAttenuation,
			               context.lightLinearAttenuation,
			               context.lightQuadraticAttenuation,
			               context.spotLightCutoff,
			               context.spotLightCosCutoff,
			               context.light_dir,
			               context.spotLightExponent,
			               context.materialShininess,
			               context.lightMode,
			               context.vinfo.normal != 0);
		}

        // Is the lighting model using the material color from the vertex color?
        if (!transform2D && context.lightingFlag.isEnabled() && context.mat_flags != 0 && context.useVertexColor && context.vinfo.color != 0 && isTriangle) {
			// Reserve an empty filter slot. The filter will be set by the
			// BasePrimitiveRenderer when the vertices are known.
			if (isLogTraceEnabled) {
				log.trace(String.format("Using VertexColorFilter"));
			}
        	setVertexPrimaryColor = true;
        }

        primaryColorSetGlobally = false;
        if (transform2D || !context.lightingFlag.isEnabled()) {
        	// No lighting, take the primary color from the vertex.
        	// This will be done by the BasePrimitiveRenderer when the vertices are known.
    		if (context.useVertexColor && context.vinfo.color != 0) {
    			setVertexPrimaryColor = true;
    			if (!isTriangle) {
    				// Use the color of the 2nd sprite vertex
    				primaryColorSetGlobally = true;
    			}
    		} else {
    			// Use context.vertexColor as the primary color
    			primaryColorSetGlobally = true;
    		}
        }

        textureAccess = null;
    	if (context.textureFlag.isEnabled() && (!transform2D || useVertexTexture) && !clearMode) {
    		int textureBufferWidth = VideoEngine.alignBufferWidth(context.texture_buffer_width[mipmapLevel], context.texture_storage);
    		int textureHeight = context.texture_height[mipmapLevel];
            int textureAddress = context.texture_base_pointer[mipmapLevel];
        	if (cachedTexture == null) {
            	int[] clut32 = VideoEngine.getInstance().readClut32(mipmapLevel);
            	short[] clut16 = VideoEngine.getInstance().readClut16(mipmapLevel);
	        	// Always request the whole buffer width
	            IMemoryReader imageReader = ImageReader.getImageReader(textureAddress, textureBufferWidth, textureHeight, textureBufferWidth, context.texture_storage, context.texture_swizzle, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_num_blocks, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, clut32, clut16);
	            textureAccess = new RandomTextureAccessReader(imageReader, textureBufferWidth, textureHeight);
        	} else {
        		textureAccess = cachedTexture.getOriginalTexture();
        	}

        	// Avoid an access outside the texture area
        	textureAccess = TextureClip.getTextureClip(context, mipmapLevel, textureAccess, textureBufferWidth, textureHeight);
		}

        renderingInitialized = true;
	}

	private LongLongKey getBaseRendererKey(GeContext context) {
		LongLongKey key = new LongLongKey();

		key.addKeyComponent(memInt != null);
		key.addKeyComponent(transform2D);
		key.addKeyComponent(clearMode);
		if (clearMode) {
			key.addKeyComponent(context.clearModeColor);
			key.addKeyComponent(context.clearModeStencil);
			key.addKeyComponent(context.clearModeDepth);
		} else {
			key.addKeyComponent(false);
			key.addKeyComponent(false);
			key.addKeyComponent(false);
		}
		key.addKeyComponent(nearZ == 0x0000);
		key.addKeyComponent(farZ == 0xFFFF);

		key.addKeyComponent(context.colorTestFlag.isEnabled() ? context.colorTestFunc : GeCommands.CTST_COLOR_FUNCTION_ALWAYS_PASS_PIXEL, 2);

		if (context.alphaTestFlag.isEnabled()) {
			key.addKeyComponent(context.alphaFunc, 3);
			key.addKeyComponent(context.alphaRef == 0x00);
			key.addKeyComponent(context.alphaRef == 0xFF);
		} else {
			key.addKeyComponent(GeCommands.ATST_ALWAYS_PASS_PIXEL, 3);
			key.addKeyComponent(false);
			key.addKeyComponent(false);
		}

		if (context.stencilTestFlag.isEnabled()) {
			key.addKeyComponent(context.stencilFunc, 3);
			key.addKeyComponent(context.stencilOpFail, 3);
			key.addKeyComponent(context.stencilOpZFail, 3);
			key.addKeyComponent(context.stencilOpZPass, 3);
		} else {
			key.addKeyComponent(GeCommands.STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST, 3);
			key.addKeyComponent(GeCommands.SOP_REPLACE_STENCIL_VALUE, 3);
			key.addKeyComponent(GeCommands.SOP_REPLACE_STENCIL_VALUE, 3);
			key.addKeyComponent(GeCommands.SOP_REPLACE_STENCIL_VALUE, 3);
		}

		key.addKeyComponent(context.depthTestFlag.isEnabled() ? context.depthFunc : GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL, 3);

		if (context.blendFlag.isEnabled()) {
			key.addKeyComponent(context.blendEquation, 3);
			key.addKeyComponent(context.blend_src, 4);
			key.addKeyComponent(context.blend_dst, 4);
		} else {
			// Use an invalid blend equation value
			key.addKeyComponent(7, 3);
			key.addKeyComponent(15, 4);
			key.addKeyComponent(15, 4);
		}

		key.addKeyComponent(context.colorLogicOpFlag.isEnabled() ? context.logicOp : GeCommands.LOP_COPY, 4);

		key.addKeyComponent(PixelColor.getColor(context.colorMask) == 0x00000000);
		key.addKeyComponent(context.depthMask);
		key.addKeyComponent(context.textureFlag.isEnabled());
		key.addKeyComponent(useVertexTexture);
		key.addKeyComponent(context.lightingFlag.isEnabled());
		key.addKeyComponent(sameVertexColor);
		key.addKeyComponent(setVertexPrimaryColor);
		key.addKeyComponent(primaryColorSetGlobally);
		key.addKeyComponent(isTriangle);
		key.addKeyComponent(context.mat_flags, 3);
		key.addKeyComponent(context.useVertexColor);
		key.addKeyComponent(context.textureColorDoubled);
		key.addKeyComponent(context.lightMode, 1);
		key.addKeyComponent(context.tex_map_mode, 2);
		if (context.tex_map_mode == GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX) {
			key.addKeyComponent(context.tex_proj_map_mode, 2);
		} else {
			key.addKeyComponent(0, 2);
		}
		key.addKeyComponent(context.tex_translate_x == 0f);
		key.addKeyComponent(context.tex_translate_y == 0f);
		key.addKeyComponent(context.tex_scale_x == 1f);
		key.addKeyComponent(context.tex_scale_y == 1f);
		key.addKeyComponent(context.tex_wrap_s, 1);
		key.addKeyComponent(context.tex_wrap_t, 1);
		key.addKeyComponent(context.textureFunc, 3);
		key.addKeyComponent(context.textureAlphaUsed);
		key.addKeyComponent(context.psm, 2);
		key.addKeyComponent(context.tex_min_filter, 3);
		key.addKeyComponent(context.tex_mag_filter, 1);
		key.addKeyComponent(isLogTraceEnabled);
		key.addKeyComponent(DurationStatistics.collectStatistics);

		return key;
	}

	protected void preRender() {
	}

	protected void postRender() {
        if (captureEachPrimitive && State.captureGeNextFrame) {
        	// Capture the GE screen after each primitive
        	Modules.sceDisplayModule.captureGeImage();
        }
        if (captureZbuffer && State.captureGeNextFrame) {
        	captureZbufferImage();
        }
	}

	protected void captureZbufferImage() {
		GeContext context = VideoEngine.getInstance().getContext();
		int width = context.zbw;
		int height = Modules.sceDisplayModule.getHeightFb();
		int address = getTextureAddress(zbp, 0, 0, zbw, depthBufferPixelFormat);
		Buffer buffer = Memory.getInstance().getBuffer(address, width * height * IRenderingEngine.sizeOfTextureType[depthBufferPixelFormat]);
		CaptureManager.captureImage(address, 0, buffer, width, height, width, depthBufferPixelFormat, false, 0, false, false);
	}

	protected void statisticsFilters(int numberPixels) {
		if (!DurationStatistics.collectStatistics || !isLogInfoEnabled) {
			return;
		}

		Integer count = filtersStatistics.get(compiledRendererKey);
		if (count == null) {
			count = 0;
		}
		filtersStatistics.put(compiledRendererKey, count + numberPixels);

		if (!filterNames.containsKey(compiledRendererKey)) {
			filterNames.put(compiledRendererKey, compiledRenderer.getClass().getName());
		}
	}

	public static void exit() {
		if (log.isInfoEnabled() && DurationStatistics.collectStatistics) {
			LongLongKey[] filterKeys = filtersStatistics.keySet().toArray(new LongLongKey[filtersStatistics.size()]);
			Arrays.sort(filterKeys, new FilterComparator());
			for (LongLongKey filterKey : filterKeys) {
				Integer count = filtersStatistics.get(filterKey);
				log.info(String.format("Filter: count=%d, id=%s, %s", count, filterKey, filterNames.get(filterKey)));
			}
		}

		FilterCompiler.exit();
	}

	private static class FilterComparator implements Comparator<LongLongKey> {
		@Override
		public int compare(LongLongKey o1, LongLongKey o2) {
			return filtersStatistics.get(o1).compareTo(filtersStatistics.get(o2));
		}
	}
}

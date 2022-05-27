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

import static jpcsp.graphics.GeCommands.CMAT_FLAG_AMBIENT;
import static jpcsp.graphics.GeCommands.CMAT_FLAG_DIFFUSE;
import static jpcsp.graphics.GeCommands.CMAT_FLAG_SPECULAR;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.util.ClassSpecializer;
import jpcsp.util.DurationStatistics;
import jpcsp.util.LongLongKey;

/**
 * @author gid15
 *
 * Implementation of a filter compilation.
 * The class RendererTemplate is specialized using fixed GE values/flags.
 */
public class FilterCompiler {
	private static Logger log = VideoEngine.log;
	private static FilterCompiler instance;
	private HashMap<LongLongKey, RendererTemplate> compiledRenderers = new HashMap<LongLongKey, RendererTemplate>();
	private static int classNameId = 0;

	public static FilterCompiler getInstance() {
		if (instance == null) {
			instance = new FilterCompiler();
		}
		return instance;
	}

	private FilterCompiler() {
	}

	public RendererTemplate getCompiledRenderer(BasePrimitiveRenderer renderer, LongLongKey id, GeContext context) {
		RendererTemplate compiledRenderer = compiledRenderers.get(id);
		if (compiledRenderer == null) {
			compiledRenderer = compileRenderer(renderer, id, context);
			if (compiledRenderer != null) {
				compiledRenderers.put(id, compiledRenderer);
			}
		}

		return compiledRenderer;
	}

	private static String getNewCompiledRendererClassName() {
		return String.format("Renderer%d", classNameId++);
	}

	private RendererTemplate compileRenderer(BasePrimitiveRenderer renderer, LongLongKey id, GeContext context) {
		if (log.isInfoEnabled()) {
			log.info(String.format("Compiling Renderer %s", id));
		}

		HashMap<String, Object> variables = new HashMap<String, Object>();
		// All these variables have to be defined as static members in the class RendererTemplate.
		variables.put("hasMemInt", Boolean.valueOf(RuntimeContext.hasMemoryInt()));
		variables.put("transform2D", Boolean.valueOf(renderer.transform2D));
		variables.put("clearMode", Boolean.valueOf(renderer.clearMode));
		variables.put("clearModeColor", Boolean.valueOf(context.clearModeColor));
		variables.put("clearModeStencil", Boolean.valueOf(context.clearModeStencil));
		variables.put("clearModeDepth", Boolean.valueOf(context.clearModeDepth));
		variables.put("needSourceDepthRead", Boolean.valueOf(renderer.needSourceDepthRead));
		variables.put("needDestinationDepthRead", Boolean.valueOf(renderer.needDestinationDepthRead));
		variables.put("needDepthWrite", Boolean.valueOf(renderer.needDepthWrite));
		variables.put("needTextureUV", Boolean.valueOf(renderer.needTextureUV));
		variables.put("simpleTextureUV", Boolean.valueOf(renderer.simpleTextureUV));
		variables.put("swapTextureUV", Boolean.valueOf(renderer.swapTextureUV));
		variables.put("needScissoringX", Boolean.valueOf(renderer.needScissoringX));
		variables.put("needScissoringY", Boolean.valueOf(renderer.needScissoringY));
		variables.put("nearZ", Integer.valueOf(renderer.nearZ));
		variables.put("farZ", Integer.valueOf(renderer.farZ));
		variables.put("colorTestFlagEnabled", Boolean.valueOf(context.colorTestFlag.isEnabled()));
		variables.put("colorTestFunc", Integer.valueOf(context.colorTestFunc));
		variables.put("alphaTestFlagEnabled", Boolean.valueOf(context.alphaTestFlag.isEnabled()));
		variables.put("alphaFunc", Integer.valueOf(context.alphaFunc));
		variables.put("alphaRef", Integer.valueOf(context.alphaRef));
		variables.put("alphaMask", Integer.valueOf(context.alphaMask));
		variables.put("stencilTestFlagEnabled", Boolean.valueOf(context.stencilTestFlag.isEnabled()));
		variables.put("stencilFunc", Integer.valueOf(context.stencilFunc));
		variables.put("stencilOpFail", Integer.valueOf(context.stencilOpFail));
		variables.put("stencilOpZFail", Integer.valueOf(context.stencilOpZFail));
		variables.put("stencilOpZPass", Integer.valueOf(context.stencilOpZPass));
		variables.put("stencilRef", Integer.valueOf(context.stencilRef));
		variables.put("depthTestFlagEnabled", Boolean.valueOf(context.depthTestFlag.isEnabled()));
		variables.put("depthFunc", Integer.valueOf(context.depthFunc));
		variables.put("blendFlagEnabled", Boolean.valueOf(context.blendFlag.isEnabled()));
		variables.put("blendEquation", Integer.valueOf(context.blendEquation));
		variables.put("blendSrc", Integer.valueOf(context.blend_src));
		variables.put("blendDst", Integer.valueOf(context.blend_dst));
		variables.put("sfix", Integer.valueOf(context.sfix));
		variables.put("dfix", Integer.valueOf(context.dfix));
		variables.put("colorLogicOpFlagEnabled", Boolean.valueOf(context.colorLogicOpFlag.isEnabled()));
		variables.put("logicOp", Integer.valueOf(context.logicOp));
		variables.put("colorMask", Integer.valueOf(PixelColor.getColor(context.colorMask)));
		variables.put("depthMask", Boolean.valueOf(context.depthMask));
		variables.put("textureFlagEnabled", Boolean.valueOf(context.textureFlag.isEnabled()));
		variables.put("useVertexTexture", Boolean.valueOf(renderer.useVertexTexture));
		variables.put("lightingFlagEnabled", Boolean.valueOf(context.lightingFlag.isEnabled()));
		variables.put("sameVertexColor", Boolean.valueOf(renderer.sameVertexColor));
		variables.put("setVertexPrimaryColor", Boolean.valueOf(renderer.setVertexPrimaryColor));
		variables.put("primaryColorSetGlobally", Boolean.valueOf(renderer.primaryColorSetGlobally));
		variables.put("isTriangle", Boolean.valueOf(renderer.isTriangle));
		variables.put("matFlagAmbient", Boolean.valueOf((context.mat_flags & CMAT_FLAG_AMBIENT) != 0));
		variables.put("matFlagDiffuse", Boolean.valueOf((context.mat_flags & CMAT_FLAG_DIFFUSE) != 0));
		variables.put("matFlagSpecular", Boolean.valueOf((context.mat_flags & CMAT_FLAG_SPECULAR) != 0));
		variables.put("useVertexColor", Boolean.valueOf(context.useVertexColor));
		variables.put("textureColorDoubled", Boolean.valueOf(context.textureColorDoubled));
		variables.put("lightMode", Integer.valueOf(context.lightMode));
		variables.put("texMapMode", Integer.valueOf(context.tex_map_mode));
		variables.put("texProjMapMode", Integer.valueOf(context.tex_proj_map_mode));
		variables.put("texTranslateX", Float.valueOf(context.tex_translate_x));
		variables.put("texTranslateY", Float.valueOf(context.tex_translate_y));
		variables.put("texScaleX", Float.valueOf(context.tex_scale_x));
		variables.put("texScaleY", Float.valueOf(context.tex_scale_y));
		variables.put("texWrapS", Integer.valueOf(context.tex_wrap_s));
		variables.put("texWrapT", Integer.valueOf(context.tex_wrap_t));
		variables.put("textureFunc", Integer.valueOf(context.textureFunc));
		variables.put("textureAlphaUsed", Boolean.valueOf(context.textureAlphaUsed));
		variables.put("psm", Integer.valueOf(context.psm));
		variables.put("texMagFilter", Integer.valueOf(context.tex_mag_filter));
		variables.put("needTextureWrapU", Boolean.valueOf(renderer.needTextureWrapU));
		variables.put("needTextureWrapV", Boolean.valueOf(renderer.needTextureWrapV));
		variables.put("needSourceDepthClamp", Boolean.valueOf(renderer.needSourceDepthClamp));
		variables.put("isLogTraceEnabled", Boolean.valueOf(renderer.isLogTraceEnabled));
		variables.put("collectStatistics", Boolean.valueOf(DurationStatistics.collectStatistics));
		variables.put("ditherFlagEnabled", Boolean.valueOf(context.ditherFlag.isEnabled()));

		String specializedClassName = getNewCompiledRendererClassName();
		ClassSpecializer cs = new ClassSpecializer();
		Class<?> specializedClass = cs.specialize(specializedClassName, RendererTemplate.class, variables);
		RendererTemplate compiledRenderer = null;
		if (specializedClass != null) {
			try {
				compiledRenderer = (RendererTemplate) specializedClass.newInstance();
			} catch (InstantiationException e) {
				log.error("Error while instanciating compiled renderer", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instanciating compiled renderer", e);
			}
		}

		return compiledRenderer;
	}

	public static void exit() {
		if (instance == null) {
			return;
		}

		if (log.isInfoEnabled() && DurationStatistics.collectStatistics) {
			DurationStatistics[] statistics = new DurationStatistics[instance.compiledRenderers.size()];
			int n = 0;
			for (RendererTemplate renderer : instance.compiledRenderers.values()) {
				statistics[n++] = renderer.getStatistics();
			}
			Arrays.sort(statistics, 0, n);
			for (int i = 0; i < n; i++) {
				log.info(statistics[i]);
			}
		}
	}
}

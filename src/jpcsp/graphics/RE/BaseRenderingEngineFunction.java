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

import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;
import static jpcsp.graphics.GeCommands.TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Level;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.GeContext.EnableDisableFlag;
import jpcsp.graphics.RE.buffer.BufferManagerFactory;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 * Base class the RenderingEngine providing basic functionalities:
 * - generic clear mode handling: implementation matching the PSP behavior,
 *   but probably not the most efficient implementation.
 * - merge of View and Model matrix for OpenGL supporting
 *   only combined model-view matrix
 * - direct rendering mode
 * - implementation of the bounding box processing
 *   (using OpenGL Query with a partial software implementation)
 * - mapping of setColorMask(int, int, int, int) to setColorMask(bool, bool, bool, bool)
 * - bug fix for glMultiDrawArrays
 *
 * The partial software implementation for Bounding Boxes tries to find out
 * if a bounding box is visible or not without using an OpenGL Query.
 * The OpenGL queries have quite a large overhead to be setup and the software
 * implementation solves the following bounding box cases:
 * - if at least one bounding box vertex is visible,
 *   the complete bounding box is visible
 * - if all the bounding box vertices are not visible and are all placed on the
 *   same side of a frustum plane, then the complete bounding box is not visible.
 *   E.g.: all the vertices are hidden on the left side of the frustum.
 *
 *   If some vertices are hidden on different sides of the frustum (e.g. one on
 *   the left side and one on the right side), the implementation cannot determine
 *   if some pixels in between are visible or not. A complete intersection test is
 *   necessary in that case. Remark: this could be implemented in software too.
 *
 * In all other cases, the OpenGL query has to be used to determine if the bounding
 * box is visible or not.
 */
public class BaseRenderingEngineFunction extends BaseRenderingEngineProxy {

    protected static final boolean usePartialSoftwareTestForBoundingBox = true;
    protected static final boolean useQueryForBoundingBox = true;
    protected static final boolean useWorkaroundForMultiDrawArrays = true;
    protected boolean useVertexArray;
    protected IREBufferManager bufferManager;
    protected boolean clearMode;
    ClearModeContext clearModeContext = new ClearModeContext();
    protected boolean directMode;
    protected boolean directModeSetOrthoMatrix;
    protected static final boolean[] flagsValidInClearMode = new boolean[]{
        false, // GU_ALPHA_TEST
        false, // GU_DEPTH_TEST
        true,  // GU_SCISSOR_TEST
        false, // GU_STENCIL_TEST
        false, // GU_BLEND
        false, // GU_CULL_FACE
        true,  // GU_DITHER
        false, // GU_FOG
        true,  // GU_CLIP_PLANES
        false, // GU_TEXTURE_2D
        true,  // GU_LIGHTING
        true,  // GU_LIGHT0
        true,  // GU_LIGHT1
        true,  // GU_LIGHT2
        true,  // GU_LIGHT3
        true,  // GU_LINE_SMOOTH
        true,  // GU_PATCH_CULL_FACE
        false, // GU_COLOR_TEST
        false, // GU_COLOR_LOGIC_OP
        true,  // GU_FACE_NORMAL_REVERSE
        true,  // GU_PATCH_FACE
        true,  // GU_FRAGMENT_2X
        true,  // RE_COLOR_MATERIAL
        true,  // RE_TEXTURE_GEN_S
        true,  // RE_TEXTURE_GEN_T
    };

    protected static class ClearModeContext {

        public boolean color;
        public boolean stencil;
        public boolean depth;
        public int alphaFunc;
        public int depthFunc;
        public int textureFunc;
        public boolean textureAlphaUsed;
        public boolean textureColorDoubled;
        public int stencilFunc;
        public int stencilRef;
        public int stencilMask;
        public int stencilOpFail;
        public int stencilOpZFail;
        public int stencilOpZPass;
    };
    protected boolean viewMatrixLoaded = false;
    protected boolean modelMatrixLoaded = false;
    protected boolean queryAvailable;
    protected int bboxQueryId;
    protected boolean bboxQueryInitialized;
    protected int bboxBuffer;
    protected int numberOfVertexBoundingBox;
    protected int bboxNumberVertex;
    protected VisibilityTestResult bboxVisible;
    protected float[] bboxCenter = new float[3];

    protected enum VisibilityTestResult {

        undefined,
        visible,
        notVisibleLeft,
        notVisibleRight,
        notVisibleTop,
        notVisibleBottom,
        mustUseQuery
    };
    protected int activeTextureUnit = 0;
    private boolean[] colorMask = new boolean[4];

    public BaseRenderingEngineFunction(IRenderingEngine proxy) {
        super(proxy);
        init();
    }

    protected void init() {
        bufferManager = BufferManagerFactory.createBufferManager(re);
        bufferManager.setRenderingEngine(re);

        queryAvailable = re.isQueryAvailable();
        if (queryAvailable) {
            bboxQueryId = re.genQuery();

            // We need 24 * vertex (having 3 floats each) for one BBOX.
            // We reserve space for 10 bboxes.
            bboxBuffer = bufferManager.genBuffer(RE_FLOAT, 10 * 24 * 3, RE_DYNAMIC_DRAW);
        }

        useVertexArray = Settings.getInstance().readBool("emu.enablevao") && super.isVertexArrayAvailable();
        if (useVertexArray) {
            log.info("Using VAO (Vertex Array Object)");
        }
    }

    @Override
    public void setRenderingEngine(IRenderingEngine re) {
        bufferManager.setRenderingEngine(re);
        super.setRenderingEngine(re);
    }

    protected void setFlag(EnableDisableFlag flag) {
        if (flag.isEnabled()) {
            re.enableFlag(flag.getReFlag());
        } else {
            re.disableFlag(flag.getReFlag());
        }
    }

    protected void setClearModeSettings(boolean color, boolean stencil, boolean depth) {
        // Disable all the flags invalid in clear mode
        for (int i = 0; i < flagsValidInClearMode.length; i++) {
            if (!flagsValidInClearMode[i]) {
                re.disableFlag(i);
            }
        }

        if (depth) {
        	re.enableFlag(GU_DEPTH_TEST);
        	context.depthFunc = GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL;
        	re.setDepthFunc(context.depthFunc);
        }

        // Update color, stencil and depth masks.
        re.setDepthMask(depth);
        re.setColorMask(color, color, color, stencil);
        re.setTextureFunc(GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE, true, false);
        re.setBones(0, null);
    }

    @Override
    public void startClearMode(boolean color, boolean stencil, boolean depth) {
        // Clear mode flags.
        clearModeContext.color = color;
        clearModeContext.stencil = stencil;
        clearModeContext.depth = depth;
        
        // Save depth.
        clearModeContext.depthFunc = context.depthFunc;
        
        // Save texture.     
        clearModeContext.textureFunc = context.textureFunc;
        clearModeContext.textureAlphaUsed = context.textureAlphaUsed;
        clearModeContext.textureColorDoubled = context.textureColorDoubled;
        
        // Save stencil.
        clearModeContext.stencilFunc = context.stencilFunc;
        clearModeContext.stencilRef = context.stencilRef;
        clearModeContext.stencilMask = context.stencilMask;
        clearModeContext.stencilOpFail = context.stencilOpFail;
        clearModeContext.stencilOpZFail = context.stencilOpZFail;
        clearModeContext.stencilOpZPass = context.stencilOpZPass;

        setClearModeSettings(color, stencil, depth);
        super.startClearMode(color, stencil, depth);
        
        clearMode = true;
    }

    @Override
    public void endClearMode() {
        if (clearMode) {
            clearMode = false;

            // Reset all the flags disabled in CLEAR mode
            for (EnableDisableFlag flag : context.flags) {
                if (!flagsValidInClearMode[flag.getReFlag()]) {
                    setFlag(flag);
                }
            }

            // Restore depth.
            context.depthFunc = clearModeContext.depthFunc;
            re.setDepthFunc(context.depthFunc);

            // Restore texture.
            context.textureFunc = clearModeContext.textureFunc;
            context.textureAlphaUsed = clearModeContext.textureAlphaUsed;
            context.textureColorDoubled = clearModeContext.textureColorDoubled;
            re.setTextureFunc(context.textureFunc, context.textureAlphaUsed, context.textureColorDoubled);

            // Restore stencil.
            context.stencilFunc = clearModeContext.stencilFunc;
            context.stencilRef = clearModeContext.stencilRef;
            context.stencilMask = clearModeContext.stencilMask;
            re.setStencilFunc(context.stencilFunc, context.stencilRef, context.stencilRef);
            
            context.stencilOpFail = clearModeContext.stencilOpFail;
            context.stencilOpZFail = clearModeContext.stencilOpZFail;
            context.stencilOpZPass = clearModeContext.stencilOpZPass;
            re.setStencilOp(context.stencilOpFail, context.stencilOpZFail, context.stencilOpZPass);

            re.setDepthMask(context.depthMask);
            re.setColorMask(true, true, true, context.stencilTestFlag.isEnabled());
            re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

            super.endClearMode();
        }
    }

    protected boolean isClearMode() {
        return clearMode;
    }

    protected boolean canUpdateFlag(int flag) {
        return !clearMode || directMode || flagsValidInClearMode[flag];
    }

    protected boolean canUpdate() {
        return !clearMode || directMode;
    }

    protected static boolean getBooleanColorMask(String name, int bitMask) {
        if (bitMask == 0xFF) {
            return false;
        } else if (bitMask != 0x00) {
            log.warn(String.format("Unimplemented %s 0x%02X", name, bitMask));
        }

        return true;
    }

    @Override
    public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
        colorMask[0] = redWriteEnabled;
        colorMask[1] = greenWriteEnabled;
        colorMask[2] = blueWriteEnabled;
        colorMask[3] = alphaWriteEnabled;
        super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
    }

    @Override
    public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
        boolean redWriteEnabled = getBooleanColorMask("Red color mask", redMask);
        boolean greenWriteEnabled = getBooleanColorMask("Green color mask", greenMask);
        boolean blueWriteEnabled = getBooleanColorMask("Blue color mask", blueMask);
        // boolean alphaWriteEnabled = getBooleanColorMask("Alpha mask", alphaMask);
        re.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, colorMask[3]);
        super.setColorMask(redMask, greenMask, blueMask, alphaMask);
    }

    @Override
    public void setDepthMask(boolean depthWriteEnabled) {
        if (canUpdate()) {
            super.setDepthMask(depthWriteEnabled);
        }
    }

    @Override
    public void setViewMatrix(float[] values) {
        super.setViewMatrix(values);
        // Reload the Model matrix if it was loaded before the View matrix (wrong order)
        if (modelMatrixLoaded) {
            re.setModelMatrix(context.model_uploaded_matrix);
        }
        viewMatrixLoaded = true;
    }

    @Override
    public void setModelMatrix(float[] values) {
        if (!viewMatrixLoaded) {
            re.setViewMatrix(context.view_uploaded_matrix);
        }
        super.setModelMatrix(values);
        modelMatrixLoaded = true;
    }

    @Override
    public void endModelViewMatrixUpdate() {
        if (!viewMatrixLoaded) {
            re.setViewMatrix(context.view_uploaded_matrix);
        }
        if (!modelMatrixLoaded) {
            re.setModelMatrix(context.model_uploaded_matrix);
        }
        super.endModelViewMatrixUpdate();
        viewMatrixLoaded = false;
        modelMatrixLoaded = false;
    }

    @Override
    public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
        directMode = true;

        re.disableFlag(GU_DEPTH_TEST);
        re.disableFlag(GU_BLEND);
        re.disableFlag(GU_ALPHA_TEST);
        re.disableFlag(GU_FOG);
        re.disableFlag(GU_LIGHTING);
        re.disableFlag(GU_COLOR_LOGIC_OP);
        re.disableFlag(GU_STENCIL_TEST);
        re.disableFlag(GU_CULL_FACE);
        re.disableFlag(GU_SCISSOR_TEST);
        if (textureEnabled) {
            re.enableFlag(GU_TEXTURE_2D);
        } else {
            re.disableFlag(GU_TEXTURE_2D);
        }
        re.setTextureMipmapMinFilter(TFLT_NEAREST);
        re.setTextureMipmapMagFilter(TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
        int colorMask = colorWriteEnabled ? 0x00 : 0xFF;
        re.setColorMask(colorMask, colorMask, colorMask, colorMask);
        re.setColorMask(colorWriteEnabled, colorWriteEnabled, colorWriteEnabled, colorWriteEnabled);
        re.setDepthMask(depthWriteEnabled);
        re.setTextureFunc(RE_TEXENV_REPLACE, true, false);
        re.setTextureMapMode(TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV, TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES);
        re.setFrontFace(true);
        re.setBones(0, null);

        directModeSetOrthoMatrix = setOrthoMatrix;
        if (setOrthoMatrix) {
            float[] orthoMatrix;
            if (orthoInverted) {
                orthoMatrix = VideoEngine.getOrthoMatrix(0, width, 0, height, -1, 1);
            } else {
                orthoMatrix = VideoEngine.getOrthoMatrix(0, width, height, 0, -1, 1);
            }
            re.setProjectionMatrix(orthoMatrix);
            re.setModelViewMatrix(null);
            re.setTextureMatrix(null);
        }

        super.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
    }

    @Override
    public void endDirectRendering() {
        // Restore all the values according to the context or the clearMode
        re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);
        if (clearMode) {
            setClearModeSettings(clearModeContext.color, clearModeContext.stencil, clearModeContext.depth);
        } else {
            context.depthTestFlag.updateEnabled();
            context.blendFlag.updateEnabled();
            context.alphaTestFlag.updateEnabled();
            context.fogFlag.updateEnabled();
            context.colorLogicOpFlag.updateEnabled();
            context.stencilTestFlag.updateEnabled();
            context.cullFaceFlag.updateEnabled();
            context.textureFlag.update();
            re.setDepthMask(context.depthMask);
            re.setTextureFunc(context.textureFunc, context.textureAlphaUsed, context.textureColorDoubled);
        }
        re.setTextureMapMode(context.tex_map_mode, context.tex_proj_map_mode);
        context.scissorTestFlag.updateEnabled();
        context.lightingFlag.updateEnabled();
        re.setTextureMipmapMagFilter(context.tex_mag_filter);
        re.setTextureMipmapMinFilter(context.tex_min_filter);
        re.setTextureWrapMode(context.tex_wrap_s, context.tex_wrap_t);
        re.setFrontFace(context.frontFaceCw);

        if (directModeSetOrthoMatrix) {
            VideoEngine videoEngine = VideoEngine.getInstance();
            videoEngine.projectionMatrixUpload.setChanged(true);
            videoEngine.viewMatrixUpload.setChanged(true);
            videoEngine.modelMatrixUpload.setChanged(true);
            videoEngine.textureMatrixUpload.setChanged(true);
        }

        super.endDirectRendering();

        directMode = false;
    }

    @Override
    public boolean hasBoundingBox() {
        return (usePartialSoftwareTestForBoundingBox
                || (useQueryForBoundingBox && queryAvailable))
                && super.hasBoundingBox();
    }

    @Override
    public void beginBoundingBox(int numberOfVertexBoundingBox) {
        bboxQueryInitialized = false;
        bboxVisible = VisibilityTestResult.undefined;
        this.numberOfVertexBoundingBox = numberOfVertexBoundingBox;

        super.beginBoundingBox(numberOfVertexBoundingBox);
    }

    protected void initializeBoundingBoxQuery() {
        if (!bboxQueryInitialized) {
            // Bounding box should not be displayed, disable all drawings
            re.startDirectRendering(false, false, false, false, false, 0, 0);

            re.beginQuery(bboxQueryId);

            bufferManager.getBuffer(bboxBuffer).clear();
            bboxNumberVertex = 0;

            bboxQueryInitialized = true;
        }
    }

    @Override
    public void drawBoundingBox(float[][] values) {
        boolean needQuery = queryAvailable;

        if (usePartialSoftwareTestForBoundingBox) {
            // The Bounding Box is visible if at least one vertex is visible.
            //
            // The Bounding Box is not visible if all the vertices are not visible
            // under the same condition (e.g. all vertices not visible on the left).
            //
            // We must use the full query for complex cases, i.e. when none of the
            // vertices are visible but under different conditions
            // (e.g. one vertex on the left side and one right side: the middle
            // could be visible)
            if (bboxVisible != VisibilityTestResult.visible) {
                for (int i = 0; i < values.length; i++) {
                    VisibilityTestResult vertexVisible = isVertexVisible(values[i]);
                    if (log.isDebugEnabled()) {
                        log.debug("BBOX Vertex #" + i + ": visible=" + vertexVisible);
                    }
                    if (vertexVisible == VisibilityTestResult.visible) {
                        bboxVisible = vertexVisible;
                        break;
                    } else if (bboxVisible == VisibilityTestResult.undefined) {
                        bboxVisible = vertexVisible;
                    } else if (vertexVisible != bboxVisible) {
                        bboxVisible = VisibilityTestResult.mustUseQuery;
                    }
                }
            }

            // If we could not take any decision based on the vertices,
            // check additionally if the bounding box center is visible...
            if (bboxVisible == VisibilityTestResult.mustUseQuery) {
                bboxCenter[0] = 0;
                bboxCenter[1] = 0;
                bboxCenter[2] = 0;
                for (int i = 0; i < values.length; i++) {
                    bboxCenter[0] += values[i][0];
                    bboxCenter[1] += values[i][1];
                    bboxCenter[2] += values[i][2];
                }
                bboxCenter[0] /= values.length;
                bboxCenter[1] /= values.length;
                bboxCenter[2] /= values.length;

                VisibilityTestResult centerVisible = isVertexVisible(bboxCenter);
                if (log.isDebugEnabled()) {
                    log.debug("BBOX Center: visible=" + centerVisible);
                }
                if (centerVisible == VisibilityTestResult.visible) {
                    // If the center is visible, the bounding box is visible!
                    bboxVisible = centerVisible;
                }
            }

            if (bboxVisible == VisibilityTestResult.visible) {
                needQuery = false;
            } else if (bboxVisible != VisibilityTestResult.mustUseQuery && numberOfVertexBoundingBox == values.length) {
                needQuery = false;
            }
        }

        if (useQueryForBoundingBox && needQuery) {
            initializeBoundingBoxQuery();
            //
            // The bounding box is a cube defined by 8 vertices.
            // It is not clear if the vertices have to be listed in a pre-defined order.
            // Which primitive should be used?
            // - GL_TRIANGLE_STRIP: we only draw 3 faces of the cube
            // - GL_QUADS: how are organized the 8 vertices to draw all the cube faces?
            //

            //
            // Cube from BBOX:
            //
            // BBOX Front face:
            //  2---3
            //  |   |
            //  |   |
            //  0---1
            //
            // BBOX Back face:
            //  6---7
            //  |   |
            //  |   |
            //  4---5
            //
            // OpenGL QUAD:
            //  3---2
            //  |   |
            //  |   |
            //  0---1
            //

            ByteBuffer byteBuffer = bufferManager.getBuffer(bboxBuffer);
            byteBuffer.clear();
            FloatBuffer bboxVertexBuffer = byteBuffer.asFloatBuffer();

            // Front face
            bboxVertexBuffer.put(values[0]);
            bboxVertexBuffer.put(values[1]);
            bboxVertexBuffer.put(values[3]);
            bboxVertexBuffer.put(values[2]);

            // Back face
            bboxVertexBuffer.put(values[4]);
            bboxVertexBuffer.put(values[5]);
            bboxVertexBuffer.put(values[7]);
            bboxVertexBuffer.put(values[6]);

            // Right face
            bboxVertexBuffer.put(values[1]);
            bboxVertexBuffer.put(values[5]);
            bboxVertexBuffer.put(values[7]);
            bboxVertexBuffer.put(values[3]);

            // Left face
            bboxVertexBuffer.put(values[0]);
            bboxVertexBuffer.put(values[4]);
            bboxVertexBuffer.put(values[6]);
            bboxVertexBuffer.put(values[2]);

            // Top face
            bboxVertexBuffer.put(values[2]);
            bboxVertexBuffer.put(values[3]);
            bboxVertexBuffer.put(values[7]);
            bboxVertexBuffer.put(values[6]);

            // Bottom face
            bboxVertexBuffer.put(values[0]);
            bboxVertexBuffer.put(values[1]);
            bboxVertexBuffer.put(values[5]);
            bboxVertexBuffer.put(values[4]);

            bboxNumberVertex += 6 * 4;
        }

        super.drawBoundingBox(values);
    }

    @Override
    public void endBoundingBox(VertexInfo vinfo) {
        if (bboxQueryInitialized) {
            if (re.isVertexArrayAvailable()) {
                re.bindVertexArray(0);
            }
            re.setVertexInfo(vinfo, false, false, false, -1);
            re.bindBuffer(RE_ARRAY_BUFFER, 0);
            re.disableClientState(IRenderingEngine.RE_TEXTURE);
            re.disableClientState(IRenderingEngine.RE_COLOR);
            re.disableClientState(IRenderingEngine.RE_NORMAL);
            re.enableClientState(IRenderingEngine.RE_VERTEX);
            bufferManager.setVertexPointer(bboxBuffer, 3, IRenderingEngine.RE_FLOAT, 3 * SIZEOF_FLOAT, 0);
            bufferManager.setBufferData(bboxBuffer, bboxNumberVertex * 3 * SIZEOF_FLOAT, bufferManager.getBuffer(bboxBuffer).rewind(), RE_DYNAMIC_DRAW);
            re.drawArrays(RE_QUADS, 0, bboxNumberVertex);

            re.endQuery();
            re.endDirectRendering();
        }

        super.endBoundingBox(vinfo);
    }

    @Override
    public boolean isBoundingBoxVisible() {
        boolean isVisible = true;

        if (usePartialSoftwareTestForBoundingBox && bboxVisible != VisibilityTestResult.mustUseQuery) {
            isVisible = (bboxVisible == VisibilityTestResult.visible);
            if (log.isDebugEnabled()) {
                log.debug("Used software test for BBOX, visible=" + isVisible);
            }
        } else if (bboxQueryInitialized) {
            if (usePartialSoftwareTestForBoundingBox && log.isDebugEnabled()) {
                log.debug("Failed to use software test for BBOX");
            }

            boolean resultAvailable = false;

            // Wait for query result available
            for (int i = 0; i < 20000; i++) {
                resultAvailable = re.getQueryResultAvailable(bboxQueryId);
                if (log.isTraceEnabled()) {
                    log.trace("glGetQueryObjectiv result available " + resultAvailable);
                }

                if (resultAvailable) {
                    // Retrieve query result (number of visible samples)
                    int result = re.getQueryResult(bboxQueryId);
                    if (log.isTraceEnabled()) {
                        log.trace("glGetQueryObjectiv result " + result);
                    }

                    // 0 samples visible means the bounding box was occluded (not visible)
                    if (result == 0) {
                        isVisible = false;
                    }
                    break;
                }
            }

            if (!resultAvailable) {
                if (log.isEnabledFor(Level.WARN)) {
                    log.warn("BoundingBox Query result not available in due time");
                }
            }
        }

        return isVisible && super.isBoundingBoxVisible();
    }

    @Override
    public IREBufferManager getBufferManager() {
        return bufferManager;
    }

    @Override
    public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
        re.setTexEnv(RE_TEXENV_RGB_SCALE, colorDoubled ? 2.0f : 1.0f);
        re.setTexEnv(RE_TEXENV_ENV_MODE, func);
        super.setTextureFunc(func, alphaUsed, colorDoubled);
    }

    protected static void multMatrix44(float[] result4, float[] matrix44, float[] vector4) {
        float x = vector4[0];
        float y = vector4[1];
        float z = vector4[2];
        float w = vector4.length < 4 ? 1.f : vector4[3];
        result4[0] = matrix44[0] * x + matrix44[4] * y + matrix44[ 8] * z + matrix44[12] * w;
        result4[1] = matrix44[1] * x + matrix44[5] * y + matrix44[ 9] * z + matrix44[13] * w;
        result4[2] = matrix44[2] * x + matrix44[6] * y + matrix44[10] * z + matrix44[14] * w;
        result4[3] = matrix44[3] * x + matrix44[7] * y + matrix44[11] * z + matrix44[15] * w;
    }

    protected VisibilityTestResult isVertexVisible(float[] vertex) {
        float[] mVertex = new float[4];
        float[] mvVertex = new float[4];
        float[] mvpVertex = new float[4];

        multMatrix44(mVertex, context.model_uploaded_matrix, vertex);
        multMatrix44(mvVertex, context.view_uploaded_matrix, mVertex);
        multMatrix44(mvpVertex, context.proj_uploaded_matrix, mvVertex);

        float w = mvpVertex[3];
        if (w != 0.f) {
            float viewportX = context.viewport_cx - context.offset_x;
            float viewportWidth = context.viewport_width;
            float windowX = (mvpVertex[0] / w * 0.5f + 0.5f) * viewportWidth + viewportX;
            if (log.isTraceEnabled()) {
                log.trace("isVertexVisible windows X=" + windowX);
            }
            if (windowX < context.region_x1) {
                return VisibilityTestResult.notVisibleLeft;
            }
            if (windowX > context.region_x2) {
                return VisibilityTestResult.notVisibleRight;
            }

            float viewportY = context.viewport_cy - context.offset_y;
            float viewportHeight = context.viewport_height;
            float windowY = (mvpVertex[1] / w * 0.5f + 0.5f) * viewportHeight + viewportY;
            if (log.isTraceEnabled()) {
                log.trace("isVertexVisible windows Y=" + windowY);
            }
            if (windowY < context.region_y1) {
                return VisibilityTestResult.notVisibleBottom;
            }
            if (windowY > context.region_y2) {
                return VisibilityTestResult.notVisibleTop;
            }
        }

        return VisibilityTestResult.visible;
    }

    @Override
    public void startDisplay() {
        for (int light = 0; light < context.lightFlags.length; light++) {
            context.lightFlags[light].update();
        }
        super.startDisplay();
    }

    @Override
    public boolean isVertexArrayAvailable() {
        return useVertexArray;
    }

    @Override
    public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count) {
        // (gid15) I don't know why, but glMultiDrawArrays doesn't seem to work
        // as expected... is it a bug in LWJGL or did I misunderstood the effect
        // of the function?
        // Workaround using glDrawArrays provided.
        if (useWorkaroundForMultiDrawArrays) {
            int primitiveCount = first.remaining();
            int positionFirst = first.position();
            int positionCount = count.position();
            if (primitive == GeCommands.PRIM_POINT || primitive == GeCommands.PRIM_LINE || primitive == GeCommands.PRIM_TRIANGLE || primitive == IRenderingEngine.RE_QUADS) {
                // Independant elements can be rendered in one drawArrays call
                // if all the elements are sequentially defined
                boolean sequential = true;
                int firstIndex = first.get(positionFirst);
                int currentIndex = firstIndex;
                for (int i = 1; i < primitiveCount; i++) {
                    currentIndex += count.get(positionCount + i - 1);
                    if (currentIndex != first.get(positionFirst + i)) {
                        sequential = false;
                        break;
                    }
                }

                if (sequential) {
                    re.drawArrays(primitive, firstIndex, currentIndex - firstIndex + count.get(positionCount + primitiveCount - 1));
                    return;
                }
            }

            // Implement multiDrawArrays using multiple drawArrays.
            // The first call is using drawArrays and the subsequent calls,
            // drawArraysBurstMode (allowing a faster implementation).
            re.drawArrays(primitive, first.get(positionFirst), count.get(positionCount));
            for (int i = 1; i < primitiveCount; i++) {
                re.drawArraysBurstMode(primitive, first.get(positionFirst + i), count.get(positionCount + i));
            }
        } else {
            super.multiDrawArrays(primitive, first, count);
        }
    }

    @Override
    public void bindActiveTexture(int index, int texture) {
        int previousActiveTextureUnit = activeTextureUnit;
        re.setActiveTexture(index);
        re.bindTexture(texture);
        re.setActiveTexture(previousActiveTextureUnit);
    }

    @Override
    public void setActiveTexture(int index) {
        activeTextureUnit = index;
        super.setActiveTexture(index);
    }

    protected void setAlphaMask(boolean alphaWriteEnabled) {
        if (colorMask[3] != alphaWriteEnabled) {
            colorMask[3] = alphaWriteEnabled;
            re.setColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        }
    }

    @Override
    public void disableFlag(int flag) {
        if (flag == IRenderingEngine.GU_STENCIL_TEST) {
            setAlphaMask(false);
        }
        super.disableFlag(flag);
    }

    @Override
    public void enableFlag(int flag) {
        if (flag == IRenderingEngine.GU_STENCIL_TEST) {
            setAlphaMask(true);
        }
        super.enableFlag(flag);
    }

    private int getBlendFix(float[] fix_color) {
        if (fix_color[0] == 0 && fix_color[1] == 0 && fix_color[2] == 0) {
            return IRenderingEngine.GU_FIX_BLACK;
        } else if (fix_color[0] == 1 && fix_color[1] == 1 && fix_color[2] == 1) {
            return IRenderingEngine.GU_FIX_WHITE;
        } else {
            return IRenderingEngine.GU_FIX_BLEND_COLOR;
        }
    }

    private int getColorInt(float[] color) {
        return (((int) (color[0] * 255)))
                | (((int) (color[1] * 255)) << 8)
                | (((int) (color[2] * 255)) << 16)
                | (((int) (color[3] * 255)) << 24);
    }

    private float[] getBlendColor(int gl_blend_src, int gl_blend_dst) {
        float[] blend_color = null;
        if (gl_blend_src == IRenderingEngine.GU_FIX_BLEND_COLOR) {
            blend_color = context.sfix_color;
            if (gl_blend_dst == IRenderingEngine.GU_FIX_BLEND_COLOR) {
                if (context.sfix_color[0] != context.dfix_color[0]
                        || context.sfix_color[1] != context.dfix_color[1]
                        || context.sfix_color[2] != context.dfix_color[2]
                        || context.sfix_color[3] != context.dfix_color[3]) {
                    log.warn(String.format("UNSUPPORTED: Both different SFIX (%08X) and DFIX (%08X) are not supported (blend equation=%d)", getColorInt(context.sfix_color), getColorInt(context.dfix_color), context.blendEquation));
                }
            }
        } else if (gl_blend_dst == IRenderingEngine.GU_FIX_BLEND_COLOR) {
            blend_color = context.dfix_color;
        }

        return blend_color;
    }

    @Override
    public void setBlendFunc(int src, int dst) {
        if (src == 10) { // GU_FIX
            src = getBlendFix(context.sfix_color);
        }

        if (dst == 10) { // GU_FIX
            if (src == IRenderingEngine.GU_FIX_BLEND_COLOR
                    && context.sfix_color[0] + context.dfix_color[0] == 1
                    && context.sfix_color[1] + context.dfix_color[1] == 1
                    && context.sfix_color[2] + context.dfix_color[2] == 1) {
                dst = IRenderingEngine.GU_FIX_BLEND_ONE_MINUS_COLOR;
            } else {
                dst = getBlendFix(context.dfix_color);
            }
        }

        float[] blend_color = getBlendColor(src, dst);
        if (blend_color != null) {
            re.setBlendColor(blend_color);
        }

        super.setBlendFunc(src, dst);
    }

    @Override
    public void setBlendDFix(float[] color) {
        // Update the blend color and functions when the DFIX is changing
        setBlendFunc(context.blend_src, context.blend_dst);

        super.setBlendDFix(color);
    }

    @Override
    public void setBlendSFix(float[] color) {
        // Update the blend color and functions when the SFIX is changing
        setBlendFunc(context.blend_src, context.blend_dst);

        super.setBlendSFix(color);
    }
}
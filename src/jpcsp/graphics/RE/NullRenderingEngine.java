package jpcsp.graphics.RE;

import java.nio.Buffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.buffer.BufferManagerFactory;
import jpcsp.graphics.RE.buffer.IREBufferManager;

public class NullRenderingEngine implements IRenderingEngine {
	protected final static Logger log = VideoEngine.log;
	protected IRenderingEngine re = this;
	protected GeContext context;
    protected IREBufferManager bufferManager;

    public NullRenderingEngine() {
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
	public IREBufferManager getBufferManager() {
		return bufferManager;
	}

	@Override
	public void exit() {
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
	}

	@Override
	public void endDirectRendering() {
	}

	@Override
	public void startDisplay() {
	}

	@Override
	public void endDisplay() {
	}

	@Override
	public void enableFlag(int flag) {
	}

	@Override
	public void disableFlag(int flag) {
	}

	@Override
	public void setMorphWeight(int index, float value) {
	}

	@Override
	public void setPatchDiv(int s, int t) {
	}

	@Override
	public void setPatchPrim(int prim) {
	}

	@Override
	public void setMatrixMode(int type) {
	}

	@Override
	public void setMatrix(float[] values) {
	}

	@Override
	public void multMatrix(float[] values) {
	}

	@Override
	public void setProjectionMatrix(float[] values) {
	}

	@Override
	public void setViewMatrix(float[] values) {
	}

	@Override
	public void setModelMatrix(float[] values) {
	}

	@Override
	public void setModelViewMatrix(float[] values) {
	}

	@Override
	public void setTextureMatrix(float[] values) {
	}

	@Override
	public void endModelViewMatrixUpdate() {
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
	}

	@Override
	public void setDepthRange(float zpos, float zscale, int near, int far) {
	}

	@Override
	public void setDepthFunc(int func) {
	}

	@Override
	public void setShadeModel(int model) {
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
	}

	@Override
	public void setMaterialShininess(float shininess) {
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
	}

	@Override
	public void setLightMode(int mode) {
	}

	@Override
	public void setLightPosition(int light, float[] position) {
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
	}

	@Override
	public void setLightType(int light, int type, int kind) {
	}

	@Override
	public void setBlendFunc(int src, int dst) {
	}

	@Override
	public void setBlendColor(float[] color) {
	}

	@Override
	public void setLogicOp(int logicOp) {
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
	}

	@Override
	public void setVertexColor(float[] color) {
	}

	@Override
	public void setUniform(int id, int value) {
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
	}

	@Override
	public void setUniform(int id, float value) {
	}

	@Override
	public void setUniform2(int id, int[] values) {
	}

	@Override
	public void setUniform3(int id, int[] values) {
	}

	@Override
	public void setUniform3(int id, float[] values) {
	}

	@Override
	public void setUniform4(int id, int[] values) {
	}

	@Override
	public void setUniform4(int id, float[] values) {
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
	}

	@Override
	public void setColorTestFunc(int func) {
	}

	@Override
	public void setColorTestReference(int[] values) {
	}

	@Override
	public void setColorTestMask(int[] values) {
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
	}

	@Override
	public int setBones(int count, float[] values) {
		// Bones are not supported
		return 0;
	}

	@Override
	public void setTexEnv(int name, int param) {
	}

	@Override
	public void setTexEnv(int name, float param) {
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
	}

	@Override
	public void endClearMode() {
	}

	@Override
	public int createShader(int type) {
		return 0;
	}

	@Override
	public boolean compilerShader(int shader, String source) {
		return false;
	}

	@Override
	public int createProgram() {
		return 0;
	}

	@Override
	public void useProgram(int program) {
	}

	@Override
	public void attachShader(int program, int shader) {
	}

	@Override
	public boolean linkProgram(int program) {
		return false;
	}

	@Override
	public boolean validateProgram(int program) {
		return false;
	}

	@Override
	public int getUniformLocation(int program, String name) {
		return -1;
	}

	@Override
	public int getAttribLocation(int program, String name) {
		return -1;
	}

	@Override
	public void bindAttribLocation(int program, int index, String name) {
	}

	@Override
	public String getShaderInfoLog(int shader) {
		return null;
	}

	@Override
	public String getProgramInfoLog(int program) {
		return null;
	}

	@Override
	public boolean isExtensionAvailable(String name) {
		return false;
	}

	@Override
	public void drawArrays(int primitive, int first, int count) {
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, Buffer indices, int indicesOffset) {
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, long indicesOffset) {
	}

	@Override
	public int genBuffer() {
		return 0;
	}

	@Override
	public void deleteBuffer(int buffer) {
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
	}

	@Override
	public void setBufferSubData(int target, int offset, int size, Buffer buffer) {
	}

	@Override
	public void bindBuffer(int target, int buffer) {
	}

	@Override
	public void enableClientState(int type) {
	}

	@Override
	public void disableClientState(int type) {
	}

	@Override
	public void enableVertexAttribArray(int id) {
	}

	@Override
	public void disableVertexAttribArray(int id) {
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer) {
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
	}

	@Override
	public int genTexture() {
		return 0;
	}

	@Override
	public void bindTexture(int texture) {
	}

	@Override
	public void deleteTexture(int texture) {
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
	}
        
        @Override
	public void setTexImagexBRZ(int level, int internalFormat, int width, int height, int bufwidth, int format, int type, int textureSize, Buffer buffer) {
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, int textureSize, Buffer buffer) {
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
	}

	@Override
	public void setAlphaFunc(int func, int ref, int mask) {
	}

	@Override
	public void setFogHint() {
	}

	@Override
	public void setFogColor(float[] color) {
	}

	@Override
	public void setFogDist(float start, float end) {
	}

	@Override
	public void setTextureEnvColor(float[] color) {
	}

	@Override
	public void setFrontFace(boolean cw) {
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
	}

	@Override
	public void setBlendEquation(int mode) {
	}

	@Override
	public void setLineSmoothHint() {
	}

	@Override
	public void beginBoundingBox(int numberOfVertexBoundingBox) {
	}

	@Override
	public void drawBoundingBox(float[][] values) {
	}

	@Override
	public void endBoundingBox(VertexInfo vinfo) {
	}

	@Override
	public boolean isBoundingBoxVisible() {
		return false;
	}

	@Override
	public int genQuery() {
		return 0;
	}

	@Override
	public void beginQuery(int id) {
	}

	@Override
	public void endQuery() {
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
		return false;
	}

	@Override
	public int getQueryResult(int id) {
		return 0;
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		return false;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		return false;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, int type) {
	}

	@Override
	public void setProgramParameter(int program, int parameter, int value) {
	}

	@Override
	public boolean isQueryAvailable() {
		return false;
	}

	@Override
	public boolean isShaderAvailable() {
		return false;
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		return -1;
	}

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint) {
	}

	@Override
	public int getUniformIndex(int program, String name) {
		return -1;
	}

	@Override
	public int[] getUniformIndices(int program, String[] names) {
		return null;
	}

	@Override
	public int getActiveUniformOffset(int program, int uniformIndex) {
		return 0;
	}

	@Override
	public boolean isFramebufferObjectAvailable() {
		return false;
	}

	@Override
	public int genFramebuffer() {
		return 0;
	}

	@Override
	public int genRenderbuffer() {
		return 0;
	}

	@Override
	public void deleteFramebuffer(int framebuffer) {
	}

	@Override
	public void deleteRenderbuffer(int renderbuffer) {
	}

	@Override
	public void bindFramebuffer(int target, int framebuffer) {
	}

	@Override
	public void bindRenderbuffer(int renderbuffer) {
	}

	@Override
	public void setRenderbufferStorage(int internalFormat, int width, int height) {
	}

	@Override
	public void setFramebufferRenderbuffer(int target, int attachment, int renderbuffer) {
	}

	@Override
	public void setFramebufferTexture(int target, int attachment, int texture, int level) {
	}

	@Override
	public int genVertexArray() {
		return 0;
	}

	@Override
	public void bindVertexArray(int id) {
	}

	@Override
	public void deleteVertexArray(int id) {
	}

	@Override
	public boolean isVertexArrayAvailable() {
		return false;
	}

	@Override
	public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count) {
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
	}

	@Override
	public void multiDrawElements(int primitive, IntBuffer first, IntBuffer count, int indexType, long indicesOffset) {
	}

	@Override
	public void drawElementsBurstMode(int primitive, int count, int indexType, long indicesOffset) {
	}

	@Override
	public void setPixelTransfer(int parameter, int value) {
	}

	@Override
	public void setPixelTransfer(int parameter, float value) {
	}

	@Override
	public void setPixelTransfer(int parameter, boolean value) {
	}

	@Override
	public void setPixelMap(int map, int mapSize, Buffer buffer) {
	}

	@Override
	public boolean canNativeClut(int textureAddress, boolean textureSwizzle) {
		return false;
	}

	@Override
	public void setActiveTexture(int index) {
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
	}

	@Override
	public void bindActiveTexture(int index, int texture) {
	}

	@Override
	public void setTextureAnisotropy(float value) {
	}

	@Override
	public float getMaxTextureAnisotropy() {
		return 0;
	}

	@Override
	public String getShadingLanguageVersion() {
		return null;
	}

	@Override
	public void setBlendSFix(int sfix, float[] color) {
	}

	@Override
	public void setBlendDFix(int dfix, float[] color) {
	}

	@Override
	public void waitForRenderingCompletion() {
	}

	@Override
	public boolean canReadAllVertexInfo() {
		return false;
	}

	@Override
	public void readStencil(int x, int y, int width, int height, int bufferSize, Buffer buffer) {
	}

	@Override
	public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
	}

	@Override
	public boolean checkAndLogErrors(String logComment) {
		// No error
		return false;
	}

	@Override
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		return false;
	}
}

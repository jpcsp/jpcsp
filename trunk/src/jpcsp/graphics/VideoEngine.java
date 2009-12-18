/*
Parts based on soywiz's pspemulator.

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
package jpcsp.graphics;

import static jpcsp.graphics.GeCommands.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.pspge;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.pspGeContext;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;
import static jpcsp.HLE.pspge.*;

import org.apache.log4j.Logger;

import com.sun.opengl.util.BufferUtil;

public class VideoEngine {
	private static final boolean useViewport = false;
    public static final int NUM_LIGHTS = 4;
    private final int[] prim_mapping = new int[] { GL.GL_POINTS, GL.GL_LINES, GL.GL_LINE_STRIP, GL.GL_TRIANGLES, GL.GL_TRIANGLE_STRIP, GL.GL_TRIANGLE_FAN, GL.GL_QUADS };

    public final static String[] psm_names = new String[] {
        "PSM_5650",
        "PSM_5551",
        "PSM_4444",
        "PSM_8888",
        "PSM_4BIT_INDEXED",
        "PSM_8BIT_INDEXED",
        "PSM_16BIT_INDEXED",
        "PSM_32BIT_INDEXED",
        "PSM_DXT1",
        "PSM_DXT3",
        "PSM_DXT5"
    };
    public final static String[] logical_ops_names = new String[] {
        "LOP_CLEAR",
        "LOP_AND",
        "LOP_REVERSE_AND",
        "LOP_COPY",
        "LOP_INVERTED_AND",
        "LOP_NO_OPERATION",
        "LOP_EXLUSIVE_OR",
        "LOP_OR",
        "LOP_NEGATED_OR",
        "LOP_EQUIVALENCE",
        "LOP_INVERTED",
        "LOP_REVERSE_OR",
        "LOP_INVERTED_COPY",
        "LOP_INVERTED_OR",
        "LOP_NEGATED_AND",
        "LOP_SET"
    };
    private static final int[] texturetype_mapping = {
            GL.GL_UNSIGNED_SHORT_5_6_5_REV,
            GL.GL_UNSIGNED_SHORT_1_5_5_5_REV,
            GL.GL_UNSIGNED_SHORT_4_4_4_4_REV,
            GL.GL_UNSIGNED_BYTE,
        };
    private static final int[] textureByteAlignmentMapping = { 2, 2, 2, 4 };

    private static VideoEngine instance;
    private GL gl;
    private GLU glu;
    public static Logger log = Logger.getLogger("ge");
    public static final boolean useTextureCache = true;
    private static GeCommands helper;
    private VertexInfo vinfo = new VertexInfo();
    private VertexInfoReader vertexInfoReader = new VertexInfoReader();
    private static final char SPACE = ' ';
    private DurationStatistics statistics;
    private DurationStatistics vertexStatistics = new DurationStatistics("Vertex");
    private DurationStatistics[] commandStatistics;
    private boolean openGL1_2;
    private boolean openGL1_5;
    private int errorCount;
    private static final int maxErrorCount = 5; // Abort list processing when detecting more errors

    private int fbp, fbw; // frame buffer pointer and width
    private int zbp, zbw; // depth buffer pointer and width
    private int psm; // pixel format

    private int region_x1, region_y1, region_x2, region_y2;
    private int region_width, region_height; // derived
    private int scissor_x1, scissor_y1, scissor_x2, scissor_y2;
    private int scissor_width, scissor_height; // derived
    private int offset_x, offset_y;
    private int viewport_width, viewport_height; // derived from xyscale
    private int viewport_cx, viewport_cy;

    private float[] proj_uploaded_matrix = new float[4 * 4];
    private MatrixUpload projectionMatrixUpload;

    private float[] texture_uploaded_matrix = new float[4 * 4];
    private MatrixUpload textureMatrixUpload;

    private float[] model_uploaded_matrix = new float[4 * 4];
    private MatrixUpload modelMatrixUpload;

    private float[] view_uploaded_matrix = new float[4 * 4];
    private MatrixUpload viewMatrixUpload;

    private int boneMatrixIndex;
    private float[][] bone_uploaded_matrix = new float[8][4 * 3];

    private float[] morph_weight = new float[8];

    private float[] tex_envmap_matrix = new float[4*4];

    private float[][] light_pos = new float[NUM_LIGHTS][4];
    private float[][] light_dir = new float[NUM_LIGHTS][3];

    private int[] light_enabled = new int[NUM_LIGHTS];
    private int[] light_type = new int[NUM_LIGHTS];
    private int[] light_kind = new int[NUM_LIGHTS];
    private boolean lighting = false;
    private float[][] lightAmbientColor = new float[NUM_LIGHTS][4];
    private float[][] lightDiffuseColor = new float[NUM_LIGHTS][4];
    private float[][] lightSpecularColor = new float[NUM_LIGHTS][4];
    private static final float[] blackColor = new float[] { 0, 0, 0, 0 };
    private float[] spotLightExponent = new float[NUM_LIGHTS];
    private float[] spotLightCutoff = new float[NUM_LIGHTS];

    private float[] fog_color = new float[4];
    private float fog_far = 0.0f,fog_dist = 0.0f;

    private float nearZ = 0.0f, farZ = 0.0f, zscale, zpos;

    private int mat_flags = 0;
    private float[] mat_ambient = new float[4];
    private float[] mat_diffuse = new float[4];
    private float[] mat_specular = new float[4];
    private float[] mat_emissive = new float[4];

    private float[] ambient_light = new float[4];

    private int texture_storage, texture_num_mip_maps;
    private boolean texture_swizzle;
    private int[] texture_base_pointer = new int[8];
    private int[] texture_width = new int[8];
    private int[] texture_height = new int[8];
    private int[] texture_buffer_width = new int[8];
    private int tex_min_filter = GL.GL_NEAREST;
    private int tex_mag_filter = GL.GL_NEAREST;
    private int tex_mipmap_mode;
    private float tex_mipmap_bias;
    private int tex_mipmap_bias_int;
    private boolean mipmapShareClut;

    private float tex_translate_x = 0.f, tex_translate_y = 0.f;
    private float tex_scale_x = 1.f, tex_scale_y = 1.f;
    private float[] tex_env_color = new float[4];
    private int tex_enable;

    private int tex_clut_addr;
    private int tex_clut_num_blocks;
    private int tex_clut_mode, tex_clut_shift, tex_clut_mask, tex_clut_start;
    private int tex_wrap_s = GL.GL_REPEAT, tex_wrap_t = GL.GL_REPEAT;
    private int patch_div_s;
    private int patch_div_t;
    private boolean clutIsDirty;

    private int transform_mode;

    private int textureTx_sourceAddress;
    private int textureTx_sourceLineWidth;
    private int textureTx_destinationAddress;
    private int textureTx_destinationLineWidth;
    private int textureTx_width;
    private int textureTx_height;
    private int textureTx_sx;
    private int textureTx_sy;
    private int textureTx_dx;
    private int textureTx_dy;
    private int textureTx_pixelSize;

    private float[] dfix_color = new float[4];
    private float[] sfix_color = new float[4];
    private int blend_src;
    private int blend_dst;

    private boolean clearMode;
    private int clearModeDepthFunc;
    private float[] clearModeRgbScale = new float[1];
    private int[] clearModeTextureEnvMode = new int[1];

    private int depthFunc2D;
    private int depthFunc3D;

    private int[] dither_matrix = new int[16];

    private boolean takeConditionalJump;

    private boolean glColorMask[];

	// opengl needed information/buffers
    private int[] gl_texture_id = new int[1];
    private int[] tmp_texture_buffer32 = new int[1024*1024];
    private short[] tmp_texture_buffer16 = new short[1024*1024];
    private int[] clut_buffer32 = new int[4096];
    private short[] clut_buffer16 = new short[4096];
    private int tex_map_mode = TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;
    private int tex_proj_map_mode = TMAP_TEXTURE_PROJECTION_MODE_POSITION;


    private boolean listHasEnded;
    private PspGeList currentList; // The currently executing list

    private boolean useVBO = true;
    private int[] vboBufferId = new int[1];
    private static final int vboBufferSize = 1024 * 1024 * BufferUtil.SIZEOF_FLOAT;
    private ByteBuffer vboBuffer = ByteBuffer.allocateDirect(vboBufferSize).order(ByteOrder.nativeOrder());
    private FloatBuffer vboFloatBuffer = vboBuffer.asFloatBuffer();
    private static final int nativeBufferSize = vboBufferSize;
    private ByteBuffer nativeBuffer = ByteBuffer.allocateDirect(nativeBufferSize).order(ByteOrder.LITTLE_ENDIAN);

    private boolean useShaders = true;
    private int shaderProgram;

    private ConcurrentLinkedQueue<PspGeList> drawListQueue;
    private boolean somethingDisplayed;
    private boolean geBufChanged;

    private class MatrixUpload {
    	float[] matrix;
    	int matrixWidth;
    	int matrixHeight;
    	int currentX;
    	int currentY;

    	public MatrixUpload(float[] matrix, int matrixWidth, int matrixHeight) {
    		this.matrix = matrix;
    		this.matrixWidth = matrixWidth;
    		this.matrixHeight = matrixHeight;

    		for (int y = 0; y < 4; y++) {
    			for (int x = 0; x < 4; x++) {
    				matrix[y * 4 + x] = (x == y ? 1 : 0);
    			}
    		}
    	}

    	public void startUpload(int startIndex) {
    		currentX = startIndex % matrixWidth;
    		currentY = startIndex / matrixWidth;
    	}

    	public boolean uploadValue(float value) {
    		boolean done = false;

    		if (currentY >= matrixHeight) {
    			error("Ignored Matrix upload value");
    			return true;
    		}

    		matrix[currentY * 4 + currentX] = value;
    		currentX++;
    		if (currentX >= matrixWidth) {
    			currentX = 0;
    			currentY++;
    			if (currentY >= matrixHeight) {
    				done = true;
    			}
    		}

    		return done;
    	}
    }

    private static void log(String msg) {
        log.debug(msg);
        /*if (isDebugMode) {
            System.out.println("sceGe DEBUG > " + msg);
        }*/
    }

    public static VideoEngine getInstance() {
        if (instance == null) {
            helper = new GeCommands();
            instance = new VideoEngine();
        }
        return instance;
    }

    private VideoEngine() {
    	modelMatrixUpload = new MatrixUpload(model_uploaded_matrix, 3, 4);
    	viewMatrixUpload = new MatrixUpload(view_uploaded_matrix, 3, 4);
    	textureMatrixUpload = new MatrixUpload(texture_uploaded_matrix, 3, 4);
    	projectionMatrixUpload = new MatrixUpload(proj_uploaded_matrix, 4, 4);
        tex_envmap_matrix[0] = tex_envmap_matrix[5] = tex_envmap_matrix[10] = tex_envmap_matrix[15] = 1.f;
        light_pos[0][3] = light_pos[1][3] = light_pos[2][3] = light_pos[3][3] = 1.f;
        morph_weight[0] = 1.f;
        tex_mipmap_mode = TBIAS_MODE_AUTO;
        tex_mipmap_bias = 0.f;
        tex_mipmap_bias_int = 0;
        takeConditionalJump = false;
        glColorMask = new boolean[] { true, true, true, true };
        mipmapShareClut = true;

        statistics = new DurationStatistics("VideoEngine Statistics");
        commandStatistics = new DurationStatistics[256];
        for (int i = 0; i < commandStatistics.length; i++) {
            commandStatistics[i] = new DurationStatistics(String.format("%-11s", helper.getCommandString(i)));
        }

        drawListQueue = new ConcurrentLinkedQueue<PspGeList>();
    }

    /** Called from pspge module */
    public void pushDrawList(PspGeList list) {
        drawListQueue.add(list);
    }

    public boolean hasDrawLists() {
        return !drawListQueue.isEmpty();
    }

    public void setGL(GL gl) {
    	this.gl = gl;
    	this.glu = new GLU();

    	String openGLVersion = getOpenGLVersion(gl);
        openGL1_2 = openGLVersion.compareTo("1.2") >= 0;
        openGL1_5 = openGLVersion.compareTo("1.5") >= 0;

        useVBO = !Settings.getInstance().readBool("emu.disablevbo") &&
            gl.isFunctionAvailable("glGenBuffersARB") &&
            gl.isFunctionAvailable("glBindBufferARB") &&
            gl.isFunctionAvailable("glBufferDataARB") &&
            gl.isFunctionAvailable("glDeleteBuffersARB") &&
            gl.isFunctionAvailable("glGenBuffers");

        useShaders = Settings.getInstance().readBool("emu.useshaders") &&
			gl.isFunctionAvailable("glCreateShader") &&
			gl.isFunctionAvailable("glShaderSource") &&
			gl.isFunctionAvailable("glCompileShader") &&
			gl.isFunctionAvailable("glCreateProgram") &&
			gl.isFunctionAvailable("glAttachShader") &&
			gl.isFunctionAvailable("glLinkProgram") &&
			gl.isFunctionAvailable("glValidateProgram") &&
			gl.isFunctionAvailable("glUseProgram") && true;

        VideoEngine.log.info("OpenGL version: " + openGLVersion);

        if(useShaders) {
        	VideoEngine.log.info("Using shaders");
        	loadShaders(gl);
        }

        if(useVBO) {
            VideoEngine.log.info("Using VBO");
            buildVBO(gl);
        }
    }

    private void buildVBO(GL gl) {
        if (openGL1_5) {
            gl.glGenBuffers(1, vboBufferId, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboBufferId[0]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBufferSize, vboFloatBuffer, GL.GL_STREAM_DRAW);
        } else {
            gl.glGenBuffersARB(1, vboBufferId, 0);
            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER, vboBufferId[0]);
            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vboBufferSize, vboFloatBuffer, GL.GL_STREAM_DRAW);
        }
    }

    private void loadShaders(GL gl) {
    	int v = gl.glCreateShader(GL.GL_VERTEX_SHADER);
    	int f = gl.glCreateShader(GL.GL_FRAGMENT_SHADER);

    	BufferedInputStream biv = new BufferedInputStream(getClass().getResourceAsStream("/jpcsp/graphics/shader.vert"));

    	String[] srcArray = new String[1];
    	StringBuilder sb = new StringBuilder();
    	int c;
    	try {
			while((c = (int) biv.read()) != -1)
				sb.append((char)c);
			biv.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	srcArray[0] = sb.toString();
    	gl.glShaderSource(v, 1, srcArray, null, 0);
    	gl.glCompileShader(v);
    	printShaderInfoLog(gl, v);

    	BufferedInputStream bif = new BufferedInputStream(getClass().getResourceAsStream("/jpcsp/graphics/shader.frag"));
    	sb = new StringBuilder();
    	try {
			while((c = bif.read()) != -1)
				sb.append((char)c);
			bif.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	srcArray[0] = sb.toString();
    	gl.glShaderSource(f, 1, srcArray, null, 0);
    	gl.glCompileShader(f);
    	printShaderInfoLog(gl, f);

    	shaderProgram = gl.glCreateProgram();
    	gl.glAttachShader(shaderProgram, v);
    	//gl.glAttachShader(shaderProgram, f);
    	gl.glLinkProgram(shaderProgram);
    	printProgramInfoLog(gl, shaderProgram);
    	gl.glValidateProgram(shaderProgram);
    	printProgramInfoLog(gl, shaderProgram);

    	for(Uniforms uniform : Uniforms.values())
    		uniform.allocateId(gl, shaderProgram);
        }

    void printShaderInfoLog(GL gl, int obj)
	{
	    int[] infologLength = new int[1];
	    int[] charsWritten = new int[1];
	    byte[] infoLog;

		gl.glGetShaderiv(obj, GL.GL_INFO_LOG_LENGTH, infologLength, 0);

	    if (infologLength[0] > 1)
	    {
	        infoLog = new byte[infologLength[0]];
	        gl.glGetShaderInfoLog(obj, infologLength[0], charsWritten, 0, infoLog, 0);
			log.error("Shader info log : " + new String(infoLog));
	    }
	}

	void printProgramInfoLog(GL gl, int obj)
	{
		int[] infologLength = new int[1];
	    int[] charsWritten = new int[1];
	    byte[] infoLog;

		gl.glGetProgramiv(obj, GL.GL_INFO_LOG_LENGTH, infologLength, 0);

	    if (infologLength[0] > 1)
	    {
	        infoLog = new byte[infologLength[0]];
	        gl.glGetProgramInfoLog(obj, infologLength[0], charsWritten, 0, infoLog, 0);
			log.error("Program info log : " + new String(infoLog));
	    }
	}


    public static void exit() {
        if (instance != null) {
            log.info(instance.statistics.toString());
            Arrays.sort(instance.commandStatistics);
            int numberCommands = 20;
            log.info(numberCommands + " most time intensive Video commands:");
            for (int i = 0; i < numberCommands; i++) {
                VideoEngine.log.info("    " + instance.commandStatistics[i].toString());
            }
            log.info(instance.vertexStatistics);
        }
    }

    public static DurationStatistics getStatistics() {
        if (instance == null) {
            return null;
        }

        return instance.statistics;
    }

    /** call from GL thread
     * @return true if an update was made
     */
    public boolean update() {
        PspGeList list = drawListQueue.poll();
        if (list == null)
            return false;

        if (useShaders)
        	gl.glUseProgram(shaderProgram);

        statistics.start();
        TextureCache.getInstance().resetTextureAlreadyHashed();
        somethingDisplayed = false;
        errorCount = 0;

        if (State.captureGeNextFrame) {
            CaptureManager.startCapture("capture.bin", list);
        }

        if (State.replayGeNextFrame) {
            // Load the replay list into drawListQueue
            CaptureManager.startReplay("capture.bin");

            // Hijack the current list with the replay list
            // TODO this is assuming there is only 1 list in drawListQueue at this point, only the last list is the replay list
            PspGeList replayList = drawListQueue.poll();
            replayList.id = list.id;
            replayList.syncStatus = list.syncStatus;
            replayList.thid = list.thid;
            list = replayList;
        }

        do {
            executeList(list);
            list = drawListQueue.poll();
        } while(list != null);

        if (useShaders)
        	gl.glUseProgram(0);

        if (State.captureGeNextFrame) {
            // Can't end capture until we get a sceDisplaySetFrameBuf after the list has executed
            CaptureManager.markListExecuted();
        }

        if (State.replayGeNextFrame) {
            CaptureManager.endReplay();
            State.replayGeNextFrame = false;
        }

        statistics.end();

        return true;
    }

    public void error(String message) {
    	errorCount++;
    	log.error(message);
    	if (errorCount >= maxErrorCount) {
    		if (tryToFallback()) {
    			log.error("Aborting current list processing due to too many errors");
    		}
    	}
    }

    private boolean tryToFallback() {
    	boolean abort = false;

    	if (!currentList.isStackEmpty()) {
    		// When have some CALLs on the stack, try to return from the last CALL
    		int oldPc = currentList.pc;
    		currentList.ret();
    		int newPc = currentList.pc;
            if (log.isDebugEnabled()) {
                log(String.format("tryToFallback old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
            }
    	} else {
    		// Finish this list
    		currentList.listHasFinished = true;
    		listHasEnded = true;
    		abort = true;
    	}

    	return abort;
    }

    private void checkCurrentListPc() {
    	Memory mem = Memory.getInstance();
        while (!mem.isAddressGood(currentList.pc)) {
        	if (!mem.isIgnoreInvalidMemoryAccess()) {
        		error("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.pc));
        		break;
        	} else {
        		// Ignoring memory read errors.
        		// Try to fall back and continue the list processing.
        		log.warn("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.pc));
        		if (tryToFallback()) {
        			break;
        		}
        	}
        }
    }

    // call from GL thread
    // There is an issue here with Emulator.pause
    // - We want to stop on errors
    // - But user may also press pause button
    //   - Either continue drawing to the end of the list (bad if the list contains an infinite loop)
    //   - Or we want to be able to restart drawing when the user presses the run button
    private void executeList(PspGeList list) {
        currentList = list;
        listHasEnded = false;

        if (log.isDebugEnabled()) {
            log("executeList id=" + list.id);
        }

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(currentList.pc, 4);
        int memoryReaderPc = currentList.pc;
        while (!listHasEnded &&
                currentList.pc != currentList.stall_addr
                && (!Emulator.pause || State.captureGeNextFrame)) {
        	if (currentList.pc != memoryReaderPc) {
        		// The currentList.pc is no longer reading in sequence
        		// and has jumped to a next location, get a new memory reader.
        		checkCurrentListPc();
        		if (listHasEnded || Emulator.pause) {
        			break;
        		}
    			memoryReader = MemoryReader.getMemoryReader(currentList.pc, 4);
        	}
            int ins = memoryReader.readNext();
            currentList.pc += 4;
            memoryReaderPc = currentList.pc;

        	executeCommand(ins);
        }

        if (currentList.pc == currentList.stall_addr) {
            currentList.currentStatus = PSP_GE_LIST_STALL_REACHED;
            if (log.isDebugEnabled()) {
                log("list id=" + currentList.id + " stalled at " + String.format("%08x", currentList.stall_addr) + " listHasEnded=" + listHasEnded);
            }
        }

        if (Emulator.pause && !listHasEnded) {
            VideoEngine.log.info("Emulator paused - cancelling current list id=" + currentList.id);
            currentList.currentStatus = PSP_GE_LIST_CANCEL_DONE;
        }

        // let DONE take priority over STALL_REACHED
        if (listHasEnded) {

            // for now testing, but maybe list is not DONE until we get a FINISH and an END?
            // this could explain games trying to sync lists that have been discarded (fiveofhearts)
            // No FINISH:
            // - Virtua Tennis: World Tour (1 instruction: signal)
            if (!currentList.listHasFinished) {
            	currentList.currentStatus = PSP_GE_LIST_END_REACHED;
            } else {
            	currentList.currentStatus = PSP_GE_LIST_DONE;
            }
        }

        if (list.currentStatus == list.syncStatus ||
            list.currentStatus == PSP_GE_LIST_DONE ||
            list.currentStatus == PSP_GE_LIST_STALL_REACHED ||
            list.currentStatus == PSP_GE_LIST_CANCEL_DONE ||
            list.currentStatus == PSP_GE_LIST_END_REACHED) {
            pspge.getInstance().hleGeListSyncDone(list);
        }
    }

    public static int command(int instruction) {
        return (instruction >>> 24);
    }

    private static int intArgument(int instruction) {
        return (instruction & 0x00FFFFFF);
    }

    private static float floatArgument(int instruction) {
        return Float.intBitsToFloat(instruction << 8);
    }

    private int getStencilOp (int pspOP) {
    	switch (pspOP) {
	    	case SOP_KEEP_STENCIL_VALUE:
	    		return GL.GL_KEEP;

	        case SOP_ZERO_STENCIL_VALUE:
	        	return GL.GL_ZERO;

	        case SOP_REPLACE_STENCIL_VALUE:
	        	return GL.GL_REPLACE;

	        case SOP_INVERT_STENCIL_VALUE:
	        	return GL.GL_INVERT;

	        case SOP_INCREMENT_STENCIL_VALUE:
	        	return GL.GL_INCR;

	        case SOP_DECREMENT_STENCIL_VALUE:
	        	return GL.GL_DECR;
    	}

    	log ("UNKNOWN stencil op "+ pspOP);
    	return GL.GL_KEEP;
    }

    private int getLogicalOp (int pspLOP) {
        switch(pspLOP){
            case LOP_CLEAR:
                return GL.GL_CLEAR;
            case LOP_AND:
                return GL.GL_AND;
            case LOP_REVERSE_AND:
                return GL.GL_AND_REVERSE;
            case LOP_COPY:
                return GL.GL_COPY;
            case LOP_INVERTED_AND:
                return GL.GL_AND_INVERTED;
            case LOP_NO_OPERATION:
                return GL.GL_NOOP;
            case LOP_EXLUSIVE_OR:
                return GL.GL_XOR;
            case LOP_OR:
                return GL.GL_OR;
            case LOP_NEGATED_OR:
                return GL.GL_NOR;
            case LOP_EQUIVALENCE:
                return GL.GL_EQUIV;
            case LOP_INVERTED:
                return GL.GL_INVERT;
            case LOP_REVERSE_OR:
                return GL.GL_OR_REVERSE;
            case LOP_INVERTED_COPY:
                return GL.GL_COPY_INVERTED;
            case LOP_INVERTED_OR:
                return GL.GL_OR_INVERTED;
            case LOP_NEGATED_AND:
                return GL.GL_NAND;
            case LOP_SET:
                return GL.GL_SET;
        }
        return GL.GL_COPY;
    }

    private int getBlendFix(float[] fix_color) {
        if (fix_color[0] == 0 && fix_color[1] == 0 && fix_color[2] == 0) {
            return GL.GL_ZERO;
        } else if (fix_color[0] == 1 && fix_color[1] == 1 && fix_color[2] == 1) {
            return GL.GL_ONE;
        } else {
            return GL.GL_CONSTANT_COLOR;
        }
    }

    private float[] getBlendColor(int gl_blend_src, int gl_blend_dst) {
        float[] blend_color = null;
        if (gl_blend_src == GL.GL_CONSTANT_COLOR) {
            blend_color = sfix_color;
            if (gl_blend_dst == GL.GL_CONSTANT_COLOR) {
                if (sfix_color[0] != dfix_color[0] ||
                    sfix_color[1] != dfix_color[1] ||
                    sfix_color[2] != dfix_color[2] ||
                    sfix_color[3] != dfix_color[3]
                    ) {
                    log.warn("UNSUPPORTED: Both different SFIX and DFIX are not supported");
                }
            }
        } else if (gl_blend_dst == GL.GL_CONSTANT_COLOR) {
            blend_color = dfix_color;
        }

        return blend_color;
    }

    // hack partially based on pspplayer
    private void setBlendFunc() {
        int gl_blend_src = GL.GL_SRC_COLOR;
        switch(blend_src) {
        case  0: gl_blend_src = GL.GL_DST_COLOR;           break;
        case  1: gl_blend_src = GL.GL_ONE_MINUS_DST_COLOR; break;
        case  2: gl_blend_src = GL.GL_SRC_ALPHA;           break;
        case  3: gl_blend_src = GL.GL_ONE_MINUS_SRC_ALPHA; break;
        case  4: gl_blend_src = GL.GL_DST_ALPHA;           break;
        case  5: gl_blend_src = GL.GL_ONE_MINUS_DST_ALPHA; break;
        case  6: gl_blend_src = GL.GL_SRC_ALPHA;           break;
        case  7: gl_blend_src = GL.GL_ONE_MINUS_SRC_ALPHA; break;
        case  8: gl_blend_src = GL.GL_DST_ALPHA;           break;
        case  9: gl_blend_src = GL.GL_ONE_MINUS_DST_ALPHA; break;
        case 10: gl_blend_src = getBlendFix(sfix_color);   break;
        default:
            error("Unhandled alpha blend src used " + blend_src);
        }

        int gl_blend_dst = GL.GL_DST_COLOR;
        switch(blend_dst) {
        case  0: gl_blend_dst = GL.GL_SRC_COLOR;           break;
        case  1: gl_blend_dst = GL.GL_ONE_MINUS_SRC_COLOR; break;
        case  2: gl_blend_dst = GL.GL_SRC_ALPHA;           break;
        case  3: gl_blend_dst = GL.GL_ONE_MINUS_SRC_ALPHA; break;
        case  4: gl_blend_dst = GL.GL_DST_ALPHA;           break;
        case  5: gl_blend_dst = GL.GL_ONE_MINUS_DST_ALPHA; break;
        case  6: gl_blend_dst = GL.GL_SRC_ALPHA;           break;
        case  7: gl_blend_dst = GL.GL_ONE_MINUS_SRC_ALPHA; break;
        case  8: gl_blend_dst = GL.GL_DST_ALPHA;           break;
        case  9: gl_blend_dst = GL.GL_ONE_MINUS_DST_ALPHA; break;
        case 10: gl_blend_dst = getBlendFix(dfix_color);   break;
        default:
            error("Unhandled alpha blend dst used " + blend_dst);
        }

        try {
            float[] blend_color = getBlendColor(gl_blend_src, gl_blend_dst);
            if (blend_color != null) {
                gl.glBlendColor(blend_color[0], blend_color[1], blend_color[2], blend_color[3]);
            }

            gl.glBlendFunc(gl_blend_src, gl_blend_dst);
        } catch (GLException e) {
            log.warn("VideoEngine: " + e.getMessage());
        }
    }

    private int getClutAddr(int level, int clutNumEntries, int clutEntrySize) {
    	return tex_clut_addr + tex_clut_start * clutEntrySize;
    }

    private void readClut() {
    	if (!clutIsDirty) {
    		return;
    	}

    	// Texture using clut?
    	if (texture_storage >= TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED && texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED) {
    		if (tex_clut_mode == CMODE_FORMAT_32BIT_ABGR8888) {
    			readClut32(0);
    		} else {
    			readClut16(0);
    		}
    	}
    }

    private short[] readClut16(int level) {
		int clutNumEntries = tex_clut_num_blocks * 16;

		// Update the clut_buffer only if some clut parameters have been changed
		// since last update.
		if (clutIsDirty) {
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 2), (clutNumEntries - tex_clut_start) * 2, 2);
			for (int i = tex_clut_start; i < clutNumEntries; i++) {
				clut_buffer16[i] = (short) memoryReader.readNext();
			}
			clutIsDirty = false;
    	}

        if (State.captureGeNextFrame) {
            log.info("Capture readClut16");
            CaptureManager.captureRAM(tex_clut_addr, clutNumEntries * 2);
        }

    	return clut_buffer16;
    }

    private int[] readClut32(int level) {
		int clutNumEntries = tex_clut_num_blocks * 8;

		// Update the clut_buffer only if some clut parameters have been changed
		// since last update.
    	if (clutIsDirty) {
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 4), (clutNumEntries - tex_clut_start) * 4, 4);
			for (int i = tex_clut_start; i < clutNumEntries; i++) {
				clut_buffer32[i] = memoryReader.readNext();
			}
			clutIsDirty = false;
    	}

        if (State.captureGeNextFrame) {
            log.info("Capture readClut32");
            CaptureManager.captureRAM(tex_clut_addr, clutNumEntries * 4);
        }

    	return clut_buffer32;
    }

    private int getClutIndex(int index) {
        return ((tex_clut_start + index) >> tex_clut_shift) & tex_clut_mask;
    }

    // UnSwizzling based on pspplayer
    private Buffer unswizzleTextureFromMemory(int texaddr, int bytesPerPixel, int level) {
        int rowWidth = (bytesPerPixel > 0) ? (texture_buffer_width[level] * bytesPerPixel) : (texture_buffer_width[level] / 2);
        int pitch = ( rowWidth - 16 ) / 4;
        int bxc = rowWidth / 16;
        int byc = (texture_height[level] + 7) / 8;

        int ydest = 0;

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, 4);
        for( int by = 0; by < byc; by++ )
        {
            int xdest = ydest;
            for( int bx = 0; bx < bxc; bx++ )
            {
                int dest = xdest;
                for( int n = 0; n < 8; n++ )
                {
                    tmp_texture_buffer32[dest  ] = memoryReader.readNext();
                    tmp_texture_buffer32[dest+1] = memoryReader.readNext();
                    tmp_texture_buffer32[dest+2] = memoryReader.readNext();
                    tmp_texture_buffer32[dest+3] = memoryReader.readNext();

                    dest += pitch+4;
                }
                xdest += (16/4);
            }
            ydest += (rowWidth * 8)/4;
        }

        if (State.captureGeNextFrame) {
            log.info("Capture unswizzleTextureFromMemory");
            CaptureManager.captureRAM(texaddr, rowWidth * texture_height[level]);
        }

        return IntBuffer.wrap(tmp_texture_buffer32);
    }

    private boolean getGLMask(String name, int bitMask) {
    	if (bitMask == 0x00) {
    		return true;
    	} else if (bitMask == 0xFF) {
    		return false;
    	} else {
    		log.warn(String.format("Unimplemented %s 0x%02X", name, bitMask));
    		return true;
    	}
    }

    private void setGLColorMask() {
    	gl.glColorMask(glColorMask[0], glColorMask[1], glColorMask[2], glColorMask[3]);
    }

    public void executeCommand(int instruction) {
        int normalArgument = intArgument(instruction);
        float floatArgument = floatArgument(instruction);

        int command = command(instruction);
        commandStatistics[command].start();
        switch (command) {
            case END:
        		listHasEnded = true;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(END) + " pc=0x" + Integer.toHexString(currentList.pc));
                }
                updateGeBuf();
                break;

            case FINISH:
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(FINISH) + " (hex="+Integer.toHexString(normalArgument)+",int="+normalArgument+",float="+floatArgument+")");
                }
                currentList.listHasFinished = true;
                currentList.pushFinishCallback(normalArgument);
                break;

            case SIGNAL:
            	int behavior = (normalArgument >> 16) & 0xFF;
            	int signal = normalArgument & 0xFFFF;
            	if (log.isDebugEnabled()) {
                    log(helper.getCommandString(SIGNAL) + " (behavior=" + behavior + ",signal=0x" + Integer.toHexString(signal) + ")");
                }
                currentList.pushSignalCallback(currentList.id, behavior, signal);
                break;

            case BASE:
        		currentList.base = (normalArgument << 8) & 0xff000000;
        		// Bits of (normalArgument & 0x0000FFFF) are ignored
        		// (tested: "Ape Escape On the Loose")
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(BASE) + " " + String.format("%08x", currentList.base));
                }
                break;

            case ORIGIN_ADDR:
            	currentList.baseOffset = currentList.pc - 4;
            	if (normalArgument != 0) {
                    log.warn(String.format("%s unknown argument 0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument));
            	} else if (log.isDebugEnabled()) {
                    log(String.format("%s 0x%08X originAddr=0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument, currentList.baseOffset));
                }
            	break;

            case OFFSET_ADDR:
            	currentList.baseOffset = normalArgument << 8;
            	if (log.isDebugEnabled()) {
                    log(String.format("%s 0x%08X", helper.getCommandString(OFFSET_ADDR), currentList.baseOffset));
                }
            	break;

            case IADDR:
                vinfo.ptr_index = currentList.getAddress(normalArgument);
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(IADDR) + " " + String.format("%08x", vinfo.ptr_index));
                }
                break;

            case VADDR:
                vinfo.ptr_vertex = currentList.getAddress(normalArgument);
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(VADDR) + " " + String.format("%08x", vinfo.ptr_vertex));
                }
                break;

            case VTYPE:
                vinfo.processType(normalArgument);
                transform_mode = (normalArgument >> 23) & 0x1;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(VTYPE) + " " + vinfo.toString());
                }
                break;

            case REGION1:
                region_x1 = normalArgument & 0x3ff;
                region_y1 = normalArgument >> 10;
                break;

            case REGION2:
                region_x2 = normalArgument & 0x3ff;
                region_y2 = normalArgument >> 10;
                region_width = (region_x2 + 1) - region_x1;
                region_height = (region_y2 + 1) - region_y1;
                if (log.isDebugEnabled()) {
                    log("drawRegion(" + region_x1 + "," + region_y1 + "," + region_width + "," + region_height + ")");
                }
                break;

            case TME:
                if (!clearMode) {
                    if (normalArgument != 0) {
                    	tex_enable = 1;
                        gl.glEnable(GL.GL_TEXTURE_2D);
                        if(useShaders) gl.glUniform1i(Uniforms.texEnable.getId(), 1);
                        log("sceGuEnable(GU_TEXTURE_2D)");
                    } else {
                    	tex_enable = 0;
                        gl.glDisable(GL.GL_TEXTURE_2D);
                        if(useShaders) gl.glUniform1i(Uniforms.texEnable.getId(), 0);
                        log("sceGuDisable(GU_TEXTURE_2D)");
                    }
                }
                break;

            case VMS:
            	viewMatrixUpload.startUpload(normalArgument);
            	if (log.isDebugEnabled()) {
            		log("sceGumMatrixMode GU_VIEW " + normalArgument);
            	}
                break;

            case VIEW:
            	if (viewMatrixUpload.uploadValue(floatArgument)) {
                    log("glLoadMatrixf", view_uploaded_matrix);
            	}
                break;

            case MMS:
            	modelMatrixUpload.startUpload(normalArgument);
            	if (log.isDebugEnabled()) {
            		log("sceGumMatrixMode GU_MODEL " + normalArgument);
            	}
                break;

            case MODEL:
                if (modelMatrixUpload.uploadValue(floatArgument)) {
                	log("glLoadMatrixf", model_uploaded_matrix);
                }
                break;

            /*
             *  Light attributes
             */

            // Position

            case LXP0:
            case LXP1:
            case LXP2:
            case LXP3: {
                int lnum = (command - LXP0) / 3;
            	light_pos[lnum][0] = floatArgument;
            	break;
            }

            case LYP0:
            case LYP1:
            case LYP2:
            case LYP3: {
                int lnum = (command - LYP0) / 3;
            	light_pos[lnum][1] = floatArgument;
            	break;
            }

            case LZP0:
            case LZP1:
            case LZP2:
            case LZP3: {
                int lnum = (command - LZP0) / 3;
            	light_pos[lnum][2] = floatArgument;
            	break;
            }

            // Color

            // Ambient
            case ALC0:
            case ALC1:
            case ALC2:
            case ALC3: {
                int lnum = (command - ALC0) / 3;
            	lightAmbientColor[lnum][0] = ((normalArgument      ) & 255) / 255.f;
            	lightAmbientColor[lnum][1] = ((normalArgument >>  8) & 255) / 255.f;
            	lightAmbientColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
            	lightAmbientColor[lnum][3] = 1.f;             
            	gl.glLightfv(GL.GL_LIGHT0 + lnum, GL.GL_AMBIENT, lightAmbientColor[lnum], 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_AMBIENT)");
            	break;
            }
            // Diffuse
            case DLC0:
            case DLC1:
            case DLC2:
            case DLC3: {
                int lnum = (command - DLC0) / 3;
            	lightDiffuseColor[lnum][0] = ((normalArgument      ) & 255) / 255.f;
            	lightDiffuseColor[lnum][1] = ((normalArgument >>  8) & 255) / 255.f;
            	lightDiffuseColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
            	lightDiffuseColor[lnum][3] = 1.f;
            	gl.glLightfv(GL.GL_LIGHT0 + lnum, GL.GL_DIFFUSE, lightDiffuseColor[lnum], 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_DIFFUSE)");
            	break;
            }

            // Specular
            case SLC0:
            case SLC1:
            case SLC2:
            case SLC3: {
                int lnum = (command - SLC0) / 3;
            	lightSpecularColor[lnum][0] = ((normalArgument      ) & 255) / 255.f;
            	lightSpecularColor[lnum][1] = ((normalArgument >>  8) & 255) / 255.f;
            	lightSpecularColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
            	lightSpecularColor[lnum][3] = 1.f;
            	gl.glLightfv(GL.GL_LIGHT0 + lnum, GL.GL_SPECULAR, lightSpecularColor[lnum], 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_SPECULAR)");
            	break;
            }

            // Light Attenuation

            // Constant
            case LCA0:
            case LCA1:
            case LCA2:
            case LCA3: {
                int lnum = (command - LCA0) / 3;
            	gl.glLightf(GL.GL_LIGHT0 + lnum, GL.GL_CONSTANT_ATTENUATION, floatArgument);
            	break;
            }

            // Linear
            case LLA0:
            case LLA1:
            case LLA2:
            case LLA3: {
                int lnum = (command - LLA0) / 3;
            	gl.glLightf(GL.GL_LIGHT0 + lnum, GL.GL_LINEAR_ATTENUATION, floatArgument);
            	break;
            }

            // Quadratic
            case LQA0:
            case LQA1:
            case LQA2:
            case LQA3: {
                int lnum = (command - LQA0) / 3;
            	gl.glLightf(GL.GL_LIGHT0 + lnum, GL.GL_QUADRATIC_ATTENUATION, floatArgument);
            	break;
            }

            case LMODE: {
                int lightmode = (normalArgument != 0) ? GL.GL_SEPARATE_SPECULAR_COLOR : GL.GL_SINGLE_COLOR;
                gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, lightmode);
                if (log.isDebugEnabled()) {
                    VideoEngine.log.info("sceGuLightMode(" + ((normalArgument != 0) ? "GU_SEPARATE_SPECULAR_COLOR" : "GU_SINGLE_COLOR") + ")");
                }
                // Check if other values than 0 and 1 are set
                if ((normalArgument & ~1) != 0) {
                    VideoEngine.log.warn(String.format("Unknown light mode sceGuLightMode(%06X)", normalArgument));
                }
                break;
            }

            case LXD0: case LXD1: case LXD2: case LXD3:
            case LYD0: case LYD1: case LYD2: case LYD3:
            case LZD0: case LZD1: case LZD2: case LZD3: {
                int lnum = (command - LXD0) / 3;
                int dircomponent = (command - LXD0) % 3;
                // OpenGL requires a normal in the opposite direction as the PSP
                light_dir[lnum][dircomponent] = -floatArgument;
                // OpenGL parameter for light direction is set in initRendering
                // because it depends on the model/view matrix
                break;
            }

            /*
             * Light types
             */

            case LT0:
            case LT1:
            case LT2:
            case LT3: {
            	int lnum = command - LT0;
            	light_type[lnum] = (normalArgument >> 8) & 3;
            	light_kind[lnum] = normalArgument & 3;
            	switch(light_type[lnum]) {
            	case LIGHT_DIRECTIONAL:
            		light_pos[lnum][3] = 0.f;
            		break;
            	case LIGHT_POINT:
            		gl.glLightf(GL.GL_LIGHT0 + lnum, GL.GL_SPOT_CUTOFF, 180);
            		light_pos[lnum][3] = 1.f;
            		break;
            	case LIGHT_SPOT:
            		light_pos[lnum][3] = 1.f;
            		break;
            	default:
            		error("Unknown light type : " + normalArgument);
            	}
            	if(useShaders) {
            		gl.glUniform4iv(Uniforms.lightType.getId(), 1, light_type, 0);
            		gl.glUniform4iv(Uniforms.lightKind.getId(), 1, light_kind, 0);
            	}
            	log.debug("Light " + lnum + " type " + (normalArgument >> 8) + " kind " + (normalArgument & 3));
            	break;
            }
            /*
             * Individual lights enable/disable
             */
            case LTE0:
            case LTE1:
            case LTE2:
            case LTE3: {
            	int lnum = command - LTE0;
            	light_enabled[lnum] = normalArgument & 1;
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHT0 + lnum);
                    log("sceGuEnable(GL_LIGHT"+lnum+")");
                } else {
                    gl.glDisable(GL.GL_LIGHT0 + lnum);
                    log("sceGuDisable(GL_LIGHT"+lnum+")");
                }
            	if(useShaders) {
            		gl.glUniform4iv(Uniforms.lightEnabled.getId(), 1, light_enabled, 0);
            	}
                break;
            }

            /*
             * Spot light exponent
             */
            case SLE0:
            case SLE1:
            case SLE2:
            case SLE3: {
            	int lnum = command - SLE0;
            	spotLightExponent[lnum] = floatArgument;
                if (log.isDebugEnabled()) {
                    VideoEngine.log.debug("sceGuLightSpot(" + lnum + ",X," + floatArgument + ",X)");
                }
            	break;
            }

            /*
             * Spot light cutoff angle
             */
            case SLF0:
            case SLF1:
            case SLF2:
            case SLF3: {
            	int lnum = command - SLF0;
            	// PSP Cutoff is cosine of angle, OpenGL expects degrees
            	float degreeCutoff = (float) Math.toDegrees(Math.acos(floatArgument));
            	if ((degreeCutoff >= 0 && degreeCutoff <= 90) || degreeCutoff == 180) {
	                spotLightCutoff[lnum] = degreeCutoff;
	                if (log.isDebugEnabled()) {
	                    log.debug("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + "=" + degreeCutoff + ")");
	                }
            	} else {
                    log.warn("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + ") invalid argument value");
            	}
            	break;
            }

            /*
             * Lighting enable/disable
             */
            case LTE:
            	if (normalArgument != 0) {
            		lighting = true;
            		if(useShaders) gl.glUniform1i(Uniforms.lightingEnable.getId(), 1);
                    gl.glEnable(GL.GL_LIGHTING);
                    log("sceGuEnable(GL_LIGHTING)");
                } else {
                	lighting = false;
                	if(useShaders) gl.glUniform1i(Uniforms.lightingEnable.getId(), 0);
                    gl.glDisable(GL.GL_LIGHTING);
                    log("sceGuDisable(GL_LIGHTING)");
                }
                break;

            /*
             * Material setup
             */
            case CMAT:
            	mat_flags = normalArgument & 7;
                if (log.isDebugEnabled()) {
                    log("sceGuColorMaterial " + mat_flags);
                }
            	break;

            case AMA:
            	mat_ambient[3] = ((normalArgument      ) & 255) / 255.f;
                if (log.isDebugEnabled()) {
                    log(String.format("material ambient a=%.1f (%02X)",
                            mat_ambient[3], normalArgument & 255));
                }
            	break;

            case AMC:
            	mat_ambient[0] = ((normalArgument	   ) & 255) / 255.f;
            	mat_ambient[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_ambient[2] = ((normalArgument >> 16) & 255) / 255.f;
                if (log.isDebugEnabled()) {
                    log(String.format("material ambient r=%.1f g=%.1f b=%.1f (%08X)",
                            mat_ambient[0], mat_ambient[1], mat_ambient[2], normalArgument));
                }
            	break;

            case DMC:
            	mat_diffuse[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_diffuse[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_diffuse[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_diffuse[3] = 1.f;
                if (log.isDebugEnabled()) {
                    log("material diffuse " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            mat_diffuse[0], mat_diffuse[1], mat_diffuse[2], normalArgument));
                }
            	break;

            case EMC:
            	mat_emissive[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_emissive[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_emissive[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_emissive[3] = 1.f;
            	gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, mat_emissive, 0);
                if (log.isDebugEnabled()) {
                    log("material emission " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            mat_emissive[0], mat_emissive[1], mat_emissive[2], normalArgument));
                }
            	break;

            case SMC:
            	mat_specular[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_specular[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_specular[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_specular[3] = 1.f;
                if (log.isDebugEnabled()) {
                    log("material specular " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            mat_specular[0], mat_specular[1], mat_specular[2], normalArgument));
                }
            	break;

            case ALC:
            	ambient_light[0] = ((normalArgument      ) & 255) / 255.f;
            	ambient_light[1] = ((normalArgument >>  8) & 255) / 255.f;
            	ambient_light[2] = ((normalArgument >> 16) & 255) / 255.f;
            	gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, ambient_light, 0);
                if (log.isDebugEnabled()) {
                    log("ambient light " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            ambient_light[0], ambient_light[1], ambient_light[2], normalArgument));
                }
            	break;

            case ALA:
            	ambient_light[3] = ((normalArgument      ) & 255) / 255.f;
            	gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, ambient_light, 0);
            	break;

            case SPOW:
            	gl.glMaterialf(GL.GL_FRONT, GL.GL_SHININESS, floatArgument);
                if (log.isDebugEnabled()) {
                    log("material shininess " + floatArgument);
                }
            	break;

            case TMS:
            	textureMatrixUpload.startUpload(normalArgument);
            	if (log.isDebugEnabled()) {
            		log("sceGumMatrixMode GU_TEXTURE " + normalArgument);
            	}
                break;

            case TMATRIX:
            	if (textureMatrixUpload.uploadValue(floatArgument)) {
                    log("glLoadMatrixf", texture_uploaded_matrix);
            	}
                break;

            case PMS:
            	projectionMatrixUpload.startUpload(normalArgument);
            	if (log.isDebugEnabled()) {
            		log("sceGumMatrixMode GU_PROJECTION " + normalArgument);
            	}
                break;

            case PROJ:
            	if (projectionMatrixUpload.uploadValue(floatArgument)) {
                    log("glLoadMatrixf", proj_uploaded_matrix);
            	}
                break;

            /*
             *
             */
            case TBW0:
            case TBW1:
            case TBW2:
            case TBW3:
            case TBW4:
            case TBW5:
            case TBW6:
            case TBW7: {
            	int level = command - TBW0;
                texture_base_pointer[level] = (texture_base_pointer[level] & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                texture_buffer_width[level] = normalArgument & 0xffff;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexImage(level=" + level + ", X, X, texBufferWidth=" + texture_buffer_width[level] + ", hi(pointer=0x" + Integer.toHexString(texture_base_pointer[level]) + "))");
                }
                break;
            }

            case TBP0:
            case TBP1:
            case TBP2:
            case TBP3:
            case TBP4:
            case TBP5:
            case TBP6:
            case TBP7: {
            	int level = command - TBP0;
                texture_base_pointer[level] = (texture_base_pointer[level] & 0xff000000) | normalArgument;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexImage(level=" + level + ", X, X, X, lo(pointer=0x" + Integer.toHexString(texture_base_pointer[level]) + "))");
                }
                break;
            }

            case TSIZE0:
            case TSIZE1:
            case TSIZE2:
            case TSIZE3:
            case TSIZE4:
            case TSIZE5:
            case TSIZE6:
            case TSIZE7: {
            	int level = command - TSIZE0;
            	// Astonishia Story is using normalArgument = 0x1804
            	// -> use texture_height = 1 << 0x08 (and not 1 << 0x18)
            	//        texture_width  = 1 << 0x04
            	texture_height[level] = 1 << ((normalArgument>>8) & 0x0F);
            	texture_width[level]  = 1 << ((normalArgument   ) & 0xFF);
                if (log.isDebugEnabled()) {
                    log ("sceGuTexImage(level=" + level + ", width=" + texture_width[level] + ", height=" + texture_height[level] + ", X, X)");
                }
            	break;
            }

            case TMODE: {
            	texture_num_mip_maps = ( normalArgument >> 16) & 0xFF;
            	// This parameter has only a meaning when
            	//  texture_storage == GU_PSM_T4 and texture_num_mip_maps > 0
            	// when parameter==0: all the mipmaps share the same clut entries (normal behavior)
            	// when parameter==1: each mipmap has its own clut table, 16 entries each, stored sequentially
            	mipmapShareClut      = ((normalArgument >>  8) & 0xFF) == 0;
            	texture_swizzle 	 = ((normalArgument      ) & 0xFF) != 0;
            	if (log.isDebugEnabled()) {
            	    log("sceGuTexMode(X, mipmaps=" + texture_num_mip_maps + ", mipmapShareClut=" + mipmapShareClut + ", swizzle=" + texture_swizzle + ")");
            	}
            	break;
            }

            case TPSM:
                // TODO find correct mask
                // - unknown game 0x105 (261)
                // - hot wheels 0x40 (64)
            	texture_storage = normalArgument & 0xFF;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexMode(tpsm=" + texture_storage + "(" + getPsmName(texture_storage) + "), X, X, X)");
                }
            	break;

            case CBP: {
                tex_clut_addr = (tex_clut_addr & 0xff000000) | normalArgument;
                clutIsDirty = true;
                if (log.isDebugEnabled()) {
                    log ("sceGuClutLoad(X, lo(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                }
                break;
            }

            case CBPH: {
                tex_clut_addr = (tex_clut_addr & 0x00ffffff) | ((normalArgument << 8) & 0x0f000000);
                clutIsDirty = true;
                if (log.isDebugEnabled()) {
                    log ("sceGuClutLoad(X, hi(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                }
                break;
            }

            case CLOAD: {
            	tex_clut_num_blocks = normalArgument;
                clutIsDirty = true;

                // Some games use the following sequence:
            	// - sceGuClutLoad(num_blocks=32, X)
            	// - sceGuClutLoad(num_blocks=1, X)
            	// - tflush
            	// - prim ... (texture data is referencing the clut entries from 32 blocks)
            	//
            	readClut();

            	if (log.isDebugEnabled()) {
                    log ("sceGuClutLoad(num_blocks=" + tex_clut_num_blocks + ", X)");
                }
            	break;
            }

            case CMODE: {
                tex_clut_mode   =  normalArgument       & 0x03;
                tex_clut_shift  = (normalArgument >> 2) & 0x3F;
                tex_clut_mask   = (normalArgument >> 8) & 0xFF;
                tex_clut_start  = (normalArgument >> 16) & 0xFF;
                clutIsDirty = true;
                if (log.isDebugEnabled()) {
                    log ("sceGuClutMode(cpsm=" + tex_clut_mode + "(" + getPsmName(tex_clut_mode) + "), shift=" + tex_clut_shift + ", mask=0x" + Integer.toHexString(tex_clut_mask) + ", start=" + tex_clut_start + ")");
                }
                break;
            }

            case TFLUSH: {
                // Do not load the texture right now, clut parameters can still be
                // defined after the TFLUSH and before the PRIM command.
                // Delay the texture loading until the PRIM command.
                if (log.isDebugEnabled()) {
                    log("tflush (deferring to prim)");
                }
                break;
            }

            case TFLT: {
            	log ("sceGuTexFilter(min=" + (normalArgument & 0xFF) + ", mag=" + ((normalArgument >> 8) & 0xFF) + ") (mm#" + texture_num_mip_maps + ")");

            	switch ((normalArgument>>8) & 0xFF)
            	{
	            	case TFLT_NEAREST: {
	            		tex_mag_filter = GL.GL_NEAREST;
	            		break;
	            	}
	            	case TFLT_LINEAR: {
	            		tex_mag_filter = GL.GL_LINEAR;
	            		break;
	            	}

	            	default: {
	            		log.warn("Unknown magnifiying filter " + ((normalArgument>>8) & 0xFF));
	            		break;
	            	}
            	}

            	switch (normalArgument & 0xFF)
            	{
	            	case TFLT_NEAREST: {
	            		tex_min_filter = GL.GL_NEAREST;
	            		break;
	            	}
	            	case TFLT_LINEAR: {
	            		tex_min_filter = GL.GL_LINEAR;
	            		break;
	            	}
	            	case TFLT_NEAREST_MIPMAP_NEAREST: {
	            		tex_min_filter = GL.GL_NEAREST_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_NEAREST_MIPMAP_LINEAR: {
	            		tex_min_filter = GL.GL_NEAREST_MIPMAP_LINEAR;
	            		break;
	            	}
	            	case TFLT_LINEAR_MIPMAP_NEAREST: {
	            		tex_min_filter = GL.GL_LINEAR_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_LINEAR_MIPMAP_LINEAR: {
	            		tex_min_filter = GL.GL_LINEAR_MIPMAP_LINEAR;
	            		break;
	            	}

	            	default: {
	            		log.warn("Unknown minimizing filter " + (normalArgument & 0xFF));
	            		break;
	            	}
            	}

            	break;
            }



            /*
             * Texture transformations
             */
            case UOFFSET: {
            	tex_translate_x = floatArgument;
                // only log in VOFFSET, assume the commands are always paired
                //if (log.isDebugEnabled()) {
                //    log ("sceGuTexOffset(u=" + tex_translate_x + ", X)");
                //}
            	break;
            }
            case VOFFSET: {
            	tex_translate_y = floatArgument;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexOffset(u=" + tex_translate_x + ", v=" + tex_translate_y + ")");
                }
            	break;
            }

            case USCALE: {
            	tex_scale_x = floatArgument;
                // only log in VSCALE, assume the commands are always paired
                //if (log.isDebugEnabled()) {
                //    log ("sceGuTexScale(u=" + tex_scale_x + ", X)");
                //}
            	break;
            }
            case VSCALE: {
            	tex_scale_y = floatArgument;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexScale(u=" + tex_scale_x + ", v=" + tex_scale_y + ")");
                }
            	break;
            }

            case TMAP:
            	tex_map_mode = normalArgument & 3;
            	tex_proj_map_mode = (normalArgument >> 8) & 3;
                if (log.isDebugEnabled()) {
                    log ("sceGuTexMapMode(mode=" + tex_map_mode + ", X, X)");
                    log ("sceGuTexProjMapMode(mode=" + tex_proj_map_mode + ")");
                }
            	break;

            case TEXTURE_ENV_MAP_MATRIX: {
            	if (normalArgument != 0) {
            		// Some games give column0=0x1B (Hot Wheels Ultimate Racing)
            		// TODO Check if our interpretation is correct. Masking with 0x03 for now.
            		//int column0 =  normalArgument     & 0xFF,
            		//	column1 = (normalArgument>>8) & 0xFF;
            		int column0 =  normalArgument     & 0x03,
            			column1 = (normalArgument>>8) & 0x03;

            		for (int i = 0; i < 3; i++) {
            			tex_envmap_matrix [i+0] = light_pos[column0][i];
            			tex_envmap_matrix [i+4] = light_pos[column1][i];
            		}

            		if (log.isDebugEnabled()) {
            			log("sceGuTexMapMode(X, " + column0 + ", " + column1 + ")");
            		}
            	}
            	break;
            }

            case TBIAS: {
                tex_mipmap_mode = normalArgument & 0xFFFF;
                tex_mipmap_bias_int = (int)(byte) (normalArgument >> 16);
                tex_mipmap_bias = tex_mipmap_bias_int / 16.0f;
                log.warn("Unimplemented sceGuTexLevelMode(mode=" + tex_mipmap_mode + ", bias=" + tex_mipmap_bias + ")");
                break;
            }

            case TFUNC:
           		int env_mode = GL.GL_MODULATE;
           		switch(normalArgument & 7) {
	           		case 0: env_mode = GL.GL_MODULATE; break;
	           		case 1: env_mode = GL.GL_DECAL; break;
	           		case 2: env_mode = GL.GL_BLEND; break;
	           		case 3: env_mode = GL.GL_REPLACE; break;
	           		case 4: env_mode = GL.GL_ADD; break;
           			default: VideoEngine.log.warn("Unimplemented tfunc mode " + (normalArgument & 7));
           		}
           		if(useShaders) gl.glUniform1i(Uniforms.texEnvMode.getId(), normalArgument & 7);

           		int rgbScaleParam = (normalArgument >> 16) & 0xFF;
           		float rgbScale = 1;
           		if (rgbScaleParam == TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_DOUBLED) {
           			rgbScale = 2;
           		} else if (rgbScaleParam != TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_UNTOUCHED) {
           			log.warn(String.format("sceGuTexFunc unknown RGB scale parameter %06X", normalArgument));
           		}

           		int alphaParam = (normalArgument >> 8) & 0xFF;
           		boolean alphaIsOne = false;
           		if (alphaParam == TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_IGNORED) {
       				// DECAL mode with ignored Alpha is always using
       				// the equivalent of Alpha = 1.0 on PSP.
           			alphaIsOne = true;
           		} else if (alphaParam != TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_READ) {
           			log.warn(String.format("sceGuTexFunc unknown alpha parameter %06X", normalArgument));
           		}

           		if (rgbScale != 1 || alphaIsOne) {
               		// GL_RGB_SCALE is only used in OpenGL when GL_TEXTURE_ENV_MODE is GL_COMBINE
           			// See http://www.opengl.org/sdk/docs/man/xhtml/glTexEnv.xml
           			switch (env_mode) {
	       				case GL.GL_MODULATE:
	       					// Cv = Cp * Cs
	       					// Av = Ap * As
	       					env_mode = GL.GL_COMBINE;
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_MODULATE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_RGB, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);

			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, GL.GL_MODULATE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, GL.GL_SRC_ALPHA);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_ALPHA, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, GL.GL_SRC_ALPHA);
	       					break;
           				case GL.GL_DECAL:
           					env_mode = GL.GL_COMBINE;
           					// Cv = Cs * As + Cp * (1 - As)
           					// Av = Ap
           					if (alphaIsOne) {
           						// Simplified version when As == 1:
           						// Cv = Cs
        		           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_REPLACE);
        		           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_TEXTURE);
        		           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);
           					} else {
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_INTERPOLATE);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_TEXTURE);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_RGB, GL.GL_PREVIOUS);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC2_RGB, GL.GL_TEXTURE);
				           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, GL.GL_SRC_ALPHA);
           					}

			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, GL.GL_REPLACE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, GL.GL_SRC_ALPHA);
           					break;
           				case GL.GL_BLEND:
           					// Cv = Cc * Cs + Cp * (1 - Cs)
           					// Av = As * Ap
           					env_mode = GL.GL_COMBINE;
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_INTERPOLATE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_CONSTANT);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_RGB, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC2_RGB, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, GL.GL_SRC_COLOR);

			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, GL.GL_MODULATE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, GL.GL_SRC_ALPHA);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_ALPHA, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, GL.GL_SRC_ALPHA);
           					break;
           				case GL.GL_REPLACE:
           					// Cv = Cs
           					// Av = As
	           				env_mode = GL.GL_COMBINE;
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_REPLACE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);

			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, GL.GL_REPLACE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, GL.GL_SRC_ALPHA);
			           		break;
	       				case GL.GL_ADD:
	       					// Cv = Cp + Cs
	       					// Av = Ap * As
	       					env_mode = GL.GL_COMBINE;
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_ADD);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_RGB, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);

			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, GL.GL_MODULATE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, GL.GL_SRC_ALPHA);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC1_ALPHA, GL.GL_PREVIOUS);
			           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, GL.GL_SRC_ALPHA);
	       					break;
       					default:
       	           			log.warn(String.format("Unimplemented sceGuTexFunc RGB doubled for env_mode=" + env_mode));
       						break;
           			}
           		}
           		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, rgbScale);
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, env_mode);

           		if (log.isDebugEnabled()) {
           		    log(String.format("sceGuTexFunc mode %06X", normalArgument)
           		            + (((normalArgument & 0x10000) != 0) ? " SCALE" : "")
           		            + (((normalArgument & 0x100) != 0) ? " ALPHA" : ""));
           		}
            	break;

            case TEC:
            	tex_env_color[0] = ((normalArgument      ) & 255) / 255.f;
            	tex_env_color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	tex_env_color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	tex_env_color[3] = 1.f;
            	gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, tex_env_color, 0);

                if (log.isDebugEnabled()) {
                    log(String.format("sceGuTexEnvColor %08X (no alpha)", normalArgument));
                }
            	break;

            case XSCALE:
                viewport_width = (int)(floatArgument * 2);
                break;
            case YSCALE:
                viewport_height = (int)(-floatArgument * 2);

                if (viewport_width != 480 || viewport_height != 272) {
                    log.warn("sceGuViewport(X, X, w=" + viewport_width + ", h=" + viewport_height + ") non-standard dimensions");
                } else if (log.isDebugEnabled()) {
                    log.debug("sceGuViewport(X, X, w=" + viewport_width + ", h=" + viewport_height + ")");
                }

                if (!useViewport) {
                	pspdisplay.getInstance().hleDisplaySetGeMode(viewport_width, viewport_height);
                }
                break;

            case ZSCALE:
            	zscale = floatArgument / 65535.f;
            	if(useShaders) gl.glUniform1f(Uniforms.zScale.getId(), zscale);
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(ZSCALE) + " " + floatArgument);
                }
                break;

            // sceGuViewport cx/cy, can we discard these settings? it's only for clipping?
            case XPOS:
                viewport_cx = (int)floatArgument;
                break;
            case YPOS:
                viewport_cy = (int)floatArgument;

                if (viewport_cx != 2048 || viewport_cy != 2048) {
                    log.warn("Unimplemented sceGuViewport(cx=" + viewport_cx + ", cy=" + viewport_cy + ", X, X) non-standard dimensions");
                } else {
                    log.warn("Unimplemented sceGuViewport(cx=" + viewport_cx + ", cy=" + viewport_cy + ", X, X)");
                }
                break;

            case ZPOS:
            	zpos = floatArgument / 65535.f;
            	if(useShaders) gl.glUniform1f(Uniforms.zPos.getId(), zpos);
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(ZPOS), floatArgument);
                }
                break;

            // sceGuOffset, can we discard these settings? it's only for clipping? (fiveofhearts)
            case OFFSETX:
                offset_x = normalArgument >> 4;
                break;
            case OFFSETY:
                offset_y = normalArgument >> 4;
                log.warn("Unimplemented sceGuOffset(x=" + offset_x + ",y=" + offset_y + ")");
                break;

            case FBP:
            	// FBP can be called before or after FBW
                fbp = (fbp & 0xff000000) | normalArgument;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(FBP) + " fbp=" + Integer.toHexString(fbp) + ", fbw=" + fbw);
                }
                geBufChanged = true;
                break;
            case FBW:
                fbp = (fbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                fbw = normalArgument & 0xffff;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(FBW) + " fbp=" + Integer.toHexString(fbp) + ", fbw=" + fbw);
                }
                geBufChanged = true;
                break;

            case ZBP:
                zbp = (zbp & 0xff000000) | normalArgument;
                if (log.isDebugEnabled()) {
                    log("zbp=" + Integer.toHexString(zbp) + ", zbw=" + zbw);
                }
                break;
            case ZBW:
                zbp = (zbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                zbw = normalArgument & 0xffff;
                if (log.isDebugEnabled()) {
                    log("zbp=" + Integer.toHexString(zbp) + ", zbw=" + zbw);
                }
                break;

            case PSM:
                psm = normalArgument;
                if (log.isDebugEnabled()) {
                    log("psm=" + normalArgument + "(" + getPsmName(normalArgument) + ")");
                }
                geBufChanged = true;
                break;

            case PRIM:
            {
                int numberOfVertex = normalArgument & 0xFFFF;
                int type = ((normalArgument >> 16) & 0x7);

                Memory mem = Memory.getInstance();
                if (!mem.isAddressGood(vinfo.ptr_vertex)) {
                	// Abort here to avoid a lot of useless memory read errors...
                	error(helper.getCommandString(PRIM) + " Invalid vertex address 0x" + Integer.toHexString(vinfo.ptr_vertex));
                	break;
                }

                updateGeBuf();
            	somethingDisplayed = true;

                loadTexture();

                // Logging
                if (log.isDebugEnabled()) {
                    switch (type) {
                        case PRIM_POINT:
                            log("prim point " + numberOfVertex + "x");
                            break;
                        case PRIM_LINE:
                            log("prim line " + (numberOfVertex / 2) + "x");
                            break;
                        case PRIM_LINES_STRIPS:
                            log("prim lines_strips " + (numberOfVertex - 1) + "x");
                            break;
                        case PRIM_TRIANGLE:
                            log("prim triangle " + (numberOfVertex / 3) + "x");
                            break;
                        case PRIM_TRIANGLE_STRIPS:
                            log("prim triangle_strips " + (numberOfVertex - 2) + "x");
                            break;
                        case PRIM_TRIANGLE_FANS:
                            log("prim triangle_fans " + (numberOfVertex - 2) + "x");
                            break;
                        case PRIM_SPRITES:
                            log("prim sprites " + (numberOfVertex / 2) + "x");
                            break;
                        default:
                            VideoEngine.log.warn("prim unhandled " + type);
                            break;
                    }
                }

                boolean useVertexColor = initRendering();

                boolean useTexture = false;
                boolean useTextureFromNormal = false;
                boolean useTextureFromPosition = false;
                if (vinfo.texture != 0) {
                	useTexture = true;
                } else if (tex_enable == 1 && transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
                	switch (tex_proj_map_mode) {
                		// What is the difference between MODE_NORMAL and MODE_NORMALIZED_NORMAL?
                		case TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
                		case TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
                			if (tex_proj_map_mode == TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL) {
                				log.warn("Texture mode TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL not tested");
                			}
                			if (vinfo.normal != 0) {
                				useTexture = true;
                				useTextureFromNormal = true;
                			}
                			break;
                		case TMAP_TEXTURE_PROJECTION_MODE_POSITION:
                			if (vinfo.position != 0) {
                				useTexture = true;
                				useTextureFromPosition = true;
                			}
                			break;
                	}
                }

                vertexStatistics.start();

                // Do not use optimized VertexInfo reading when tracing is enabled,
                // it doesn't produce any trace information
                if (vinfo.index == 0 && type != PRIM_SPRITES && mem.isAddressGood(vinfo.ptr_vertex) && !log.isTraceEnabled()) {
                	// Optimized VertexInfo reading:
                	// - do not copy the info already available in the OpenGL format
                	//   (native format), load it into nativeBuffer (a direct buffer
                	//   is required by OpenGL).
                	// - try to keep the info in "int" format when possible, convert
                	//   to "float" only when necessary
                	// The best case is no reading and no conversion at all when all the
                	// vertex info are available in a format usable by OpenGL.
                	//
                	// The optimized reading cannot currently handle
                	// indexed vertex info (vinfo.index != 0) and PRIM_SPRITES.
                	//
                    Buffer buffer = vertexInfoReader.read(vinfo, vinfo.ptr_vertex, numberOfVertex);

                    enableClientState(useVertexColor, useTexture);

                    int stride = vertexInfoReader.getStride();
					glBindBuffer();
					if (buffer != null) {
						if (useVBO) {
	                        if (openGL1_5) {
	                        	gl.glBufferData(GL.GL_ARRAY_BUFFER, stride * numberOfVertex, buffer, GL.GL_STREAM_DRAW);
	                        } else {
	                        	gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, stride * numberOfVertex, buffer, GL.GL_STREAM_DRAW);
	                        }
						} else {
							vboBuffer.clear();
							Utilities.putBuffer(vboBuffer, buffer, ByteOrder.nativeOrder());
						}
                    }

                	if (vertexInfoReader.hasNative()) {
                		// Copy the VertexInfo from Memory to the nativeBuffer
                		// (a direct buffer is required by glXXXPointer())
                		nativeBuffer.clear();
                    	Buffer memBuffer = mem.getBuffer(vinfo.ptr_vertex, vinfo.vertexSize * numberOfVertex);
                    	Utilities.putBuffer(nativeBuffer, memBuffer, ByteOrder.LITTLE_ENDIAN);
                	}

                	if (vinfo.texture != 0 || useTexture) {
                		boolean textureNative;
                		int textureOffset;
                		int textureType;
                		if (useTextureFromNormal) {
                			textureNative = vertexInfoReader.isNormalNative();
                			textureOffset = vertexInfoReader.getNormalOffset();
                			textureType = vertexInfoReader.getNormalType();
                		} else if (useTextureFromPosition) {
                			textureNative = vertexInfoReader.isPositionNative();
                			textureOffset = vertexInfoReader.getPositionOffset();
                			textureType = vertexInfoReader.getPositionType();
                		} else {
                			textureNative = vertexInfoReader.isTextureNative();
	                		textureOffset = vertexInfoReader.getTextureOffset();
	                		textureType = vertexInfoReader.getTextureType();
                		}
                		glTexCoordPointer(useTexture, textureType, stride, textureOffset, textureNative, false);
                    }

                	glColorPointer(useVertexColor, vertexInfoReader.getColorType(), stride, vertexInfoReader.getColorOffset(), vertexInfoReader.isColorNative(), false);
                	glNormalPointer(vertexInfoReader.getNormalType(), stride, vertexInfoReader.getNormalOffset(), vertexInfoReader.isNormalNative(), false);
                	glVertexPointer(vertexInfoReader.getPositionType(), stride, vertexInfoReader.getPositionOffset(), vertexInfoReader.isPositionNative(), false);

                    gl.glDrawArrays(prim_mapping[type], 0, numberOfVertex);

                } else {
                	// Non-optimized VertexInfo reading

                	bindBuffers(useVertexColor, useTexture);
                	vboFloatBuffer.clear();

	                switch (type) {
	                    case PRIM_POINT:
	                    case PRIM_LINE:
	                    case PRIM_LINES_STRIPS:
	                    case PRIM_TRIANGLE:
	                    case PRIM_TRIANGLE_STRIPS:
	                    case PRIM_TRIANGLE_FANS:
	                        for (int i = 0; i < numberOfVertex; i++) {
	                            int addr = vinfo.getAddress(mem, i);
	                            
	                            vinfo.setMorphWeights(morph_weight);
	                            VertexState v = vinfo.readVertex(mem, addr);

	                            // Do skinning first as it modifies v.p and v.n
	                            if (vinfo.position != 0 && vinfo.weight != 0) {
	                                doSkinning(vinfo, v);
	                            }

	                            if (vinfo.texture  != 0) vboFloatBuffer.put(v.t);
	                            else if (useTextureFromNormal) vboFloatBuffer.put(v.n, 0, 2);
	                            else if (useTextureFromPosition) vboFloatBuffer.put(v.p, 0, 2);
	                            if (useVertexColor) vboFloatBuffer.put(v.c);
	                            if (vinfo.normal   != 0) vboFloatBuffer.put(v.n);
	                            if (vinfo.position != 0) vboFloatBuffer.put(v.p);

	                            if (log.isTraceEnabled()) {
	                            	if (vinfo.texture != 0 && vinfo.position != 0) {
	                            		log.trace("  vertex#" + i + " (" + ((int) v.t[0]) + "," + ((int) v.t[1]) + ") at (" + ((int) v.p[0]) + "," + ((int) v.p[1]) + "," + ((int) v.p[2]) + ")");
	                            	}
	                            }
	                        }

	                        if(useVBO) {
	                            if (openGL1_5)
	                                gl.glBufferData(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
	                            else
	                                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
	                        }
	                        gl.glDrawArrays(prim_mapping[type], 0, numberOfVertex);
	                        break;

	                    case PRIM_SPRITES:
	                        gl.glPushAttrib(GL.GL_ENABLE_BIT);
	                        gl.glDisable(GL.GL_CULL_FACE);
	                        for (int i = 0; i < numberOfVertex; i += 2) {
	                            int addr1 = vinfo.getAddress(mem, i);
	                            int addr2 = vinfo.getAddress(mem, i + 1);
	                            vinfo.setMorphWeights(morph_weight);
	                            VertexState v1 = vinfo.readVertex(mem, addr1);
	                            VertexState v2 = vinfo.readVertex(mem, addr2);

	                            v1.p[2] = v2.p[2];

	                            if (log.isDebugEnabled() && transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
	                                log("  sprite (" + ((int) v1.t[0]) + "," + ((int) v1.t[1]) + ")-(" + ((int) v2.t[0]) + "," + ((int) v2.t[1]) + ") at (" + ((int) v1.p[0]) + "," + ((int) v1.p[1]) + "," + ((int) v1.p[2]) + ")-(" + + ((int) v2.p[0]) + "," + ((int) v2.p[1]) + "," + ((int) v2.p[2]) + ")");
	                            }

	                            // V1
	                            if (vinfo.texture  != 0) vboFloatBuffer.put(v1.t);
	                            if (useVertexColor) vboFloatBuffer.put(v2.c);
	                            if (vinfo.normal   != 0) vboFloatBuffer.put(v2.n);
	                            if (vinfo.position != 0) vboFloatBuffer.put(v1.p);

	                            if (vinfo.texture  != 0) vboFloatBuffer.put(v2.t[0]).put(v1.t[1]);
	                            if (useVertexColor) vboFloatBuffer.put(v2.c);
	                            if (vinfo.normal   != 0) vboFloatBuffer.put(v2.n);
	                            if (vinfo.position != 0) vboFloatBuffer.put(v2.p[0]).put(v1.p[1]).put(v2.p[2]);

	                            // V2
	                            if (vinfo.texture  != 0) vboFloatBuffer.put(v2.t);
	                            if (useVertexColor) vboFloatBuffer.put(v2.c);
	                            if (vinfo.normal   != 0) vboFloatBuffer.put(v2.n);
	                            if (vinfo.position != 0) vboFloatBuffer.put(v2.p);

	                            if (vinfo.texture  != 0) vboFloatBuffer.put(v1.t[0]).put(v2.t[1]);
	                            if (useVertexColor) vboFloatBuffer.put(v2.c);
	                            if (vinfo.normal   != 0) vboFloatBuffer.put(v2.n);
	                            if (vinfo.position != 0) vboFloatBuffer.put(v1.p[0]).put(v2.p[1]).put(v2.p[2]);
	                        }
	                        if(useVBO) {
	                            if (openGL1_5)
	                                gl.glBufferData(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
	                            else
	                                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
	                        }
	                        gl.glDrawArrays(GL.GL_QUADS, 0, numberOfVertex * 2);
	                        gl.glPopAttrib();
	                        break;
	                }
                }

                vertexStatistics.end();

                // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
                // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
                if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
                    log.info("Capture PRIM");
                    CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * numberOfVertex);
                    pspdisplay.getInstance().captureGeImage(gl);
                }

                // VADDR/IADDR are updated after vertex rendering
                // (IADDR when indexed and VADDR when not).
                // Some games rely on this and don't reload VADDR/IADDR between 2 PRIM calls.
                if (vinfo.index == 0) {
                	vinfo.ptr_vertex = vinfo.getAddress(mem, numberOfVertex);
                } else {
                	vinfo.ptr_index += numberOfVertex * vinfo.index;
                }

                endRendering(useVertexColor, useTexture);
                break;
            }

            case ALPHA: {
                int blend_mode = GL.GL_FUNC_ADD;
                blend_src =  normalArgument        & 0xF;
                blend_dst = (normalArgument >> 4 ) & 0xF;
                int op    = (normalArgument >> 8 ) & 0xF;

            	switch (op) {
	            	case ALPHA_SOURCE_BLEND_OPERATION_ADD:
	            		blend_mode = GL.GL_FUNC_ADD;
	            		break;

	                case ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT:
	                	blend_mode = GL.GL_FUNC_SUBTRACT;
	            		break;

	                case ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT:
	                	blend_mode = GL.GL_FUNC_REVERSE_SUBTRACT;
	            		break;

	                case ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE:
	                	blend_mode = GL.GL_MIN;
	            		break;

	                case ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE:
	                	blend_mode = GL.GL_MAX;
	            		break;

	                case ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE:
	                	blend_mode = GL.GL_FUNC_ADD;
	                	break;

                    default:
	                	error("Unhandled blend mode " + op);
	                	break;
            	}

            	try {
            		gl.glBlendEquation(blend_mode);
            	} catch (GLException e) {
            		log.warn("VideoEngine: " + e.getMessage());
            	}

            	if (log.isDebugEnabled()) {
            	    log("sceGuBlendFunc(op=" + op + ", src=" + blend_src + ", dst=" + blend_dst + ")");
            	}
            	break;
            }

            case SHADE: {
                int SETTED_MODEL = (normalArgument != 0) ? GL.GL_SMOOTH : GL.GL_FLAT;
                gl.glShadeModel(SETTED_MODEL);
                if (log.isDebugEnabled()) {
                    log("sceGuShadeModel(" + ((normalArgument != 0) ? "smooth" : "flat") + ")");
                }
                break;
            }

            case FFACE: {
                int frontFace = (normalArgument != 0) ? GL.GL_CW : GL.GL_CCW;
                gl.glFrontFace(frontFace);
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(FFACE) + " " + ((normalArgument != 0) ? "clockwise" : "counter-clockwise"));
                }
                break;
            }
            case DTE:
	        	if(normalArgument != 0)
	        	{
	        		gl.glEnable(GL.GL_DITHER);
	                log("sceGuEnable(GL_DITHER)");
	        	}
	        	else
	        	{
	                gl.glDisable(GL.GL_DITHER);
	                log("sceGuDisable(GL_DITHER)");
	        	}
	        	break;
            case BCE:
            	if (!clearMode) {
	                if(normalArgument != 0)
	                {
	                    gl.glEnable(GL.GL_CULL_FACE);
	                    log("sceGuEnable(GU_CULL_FACE)");
	                }
	                else
	                {
	                    gl.glDisable(GL.GL_CULL_FACE);
	                    log("sceGuDisable(GU_CULL_FACE)");
	                }
            	}
                break;
            case FGE:
            	if (!clearMode) {
	                if(normalArgument != 0)
	                {
	                    gl.glEnable(GL.GL_FOG);
	                    gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
					    gl.glHint(GL.GL_FOG_HINT, GL.GL_DONT_CARE);
	                    log("sceGuEnable(GL_FOG)");
	                }
	                else
	                {
	                    gl.glDisable(GL.GL_FOG);
	                    log("sceGuDisable(GL_FOG)");
	                }
            	}
                break;
            case FCOL:
	            	fog_color[0] = ((normalArgument      ) & 255) / 255.f;
	            	fog_color[1] = ((normalArgument >>  8) & 255) / 255.f;
	            	fog_color[2] = ((normalArgument >> 16) & 255) / 255.f;
	            	fog_color[3] = 1.f;
	            	gl.glFogfv(GL.GL_FOG_COLOR, fog_color, 0);

                    if (log.isDebugEnabled()) {
                        log(String.format("sceGuFog(X, X, color=%08X) (no alpha)", normalArgument));
                    }
	            break;
            case FFAR:
            	fog_far = floatArgument;
            	break;
            case FDIST:
            	fog_dist = floatArgument;
            	if((fog_far != 0.0f) && (fog_dist != 0.0f))
            	{
            		float end = fog_far;
            		float start = end - (1/floatArgument);
            		gl.glFogf( GL.GL_FOG_START, start );
            		gl.glFogf( GL.GL_FOG_END, end );
            	}
            	break;
            case ABE:
                if (!clearMode) {
                    if(normalArgument != 0) {
                        gl.glEnable(GL.GL_BLEND);
                        log("sceGuEnable(GU_BLEND)");
                    }
                    else {
                        gl.glDisable(GL.GL_BLEND);
                        log("sceGuDisable(GU_BLEND)");
                    }
                }
                break;
             case ATE:
                if (!clearMode) {
	            	if(normalArgument != 0) {
	            		gl.glEnable(GL.GL_ALPHA_TEST);
	            		log("sceGuEnable(GL_ALPHA_TEST)");
	            	}
	            	else {
	            		 gl.glDisable(GL.GL_ALPHA_TEST);
	                     log("sceGuDisable(GL_ALPHA_TEST)");
	            	}
                }
	            break;
            case ZTE:
                if (!clearMode) {
                    if(normalArgument != 0) {
                        gl.glEnable(GL.GL_DEPTH_TEST);
                        log("sceGuEnable(GU_DEPTH_TEST)");
                    }
                    else {
                        gl.glDisable(GL.GL_DEPTH_TEST);
                        log("sceGuDisable(GU_DEPTH_TEST)");
                    }
                }
                break;
            case STE:
            	if (!clearMode) {
	                if(normalArgument != 0) {
	                    gl.glEnable(GL.GL_STENCIL_TEST);
	                    log("sceGuEnable(GU_STENCIL_TEST)");
	                }
	                else {
	                    gl.glDisable(GL.GL_STENCIL_TEST);
	                    log("sceGuDisable(GU_STENCIL_TEST)");
	                }
            	}
                break;
            case AAE:
	            	if(normalArgument != 0)
	            	{
	            		gl.glEnable(GL.GL_LINE_SMOOTH);
	            		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
	            		log("sceGuEnable(GL_LINE_SMOOTH)");
	            	}else
	            	{
	            		gl.glDisable(GL.GL_LINE_SMOOTH);
	            		log("sceGuDisable(GL_LINE_SMOOTH)");
	            	}
	            	break;
            case LOE:
            	if (!clearMode) {
	                if(normalArgument != 0)
	                {
	                    gl.glEnable(GL.GL_COLOR_LOGIC_OP);
	                    log("sceGuEnable(GU_COLOR_LOGIC_OP)");
	                }
	                else
	                {
	                    gl.glDisable(GL.GL_COLOR_LOGIC_OP);
	                    log("sceGuDisable(GU_COLOR_LOGIC_OP)");
	                }
            	}
                break;
            case JUMP: {
            	int oldPc = currentList.pc;
            	currentList.jump(normalArgument);
            	int newPc = currentList.pc;
                if (log.isDebugEnabled()) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(JUMP), oldPc, newPc));
                }
                break;
            }
            case CALL: {
            	int oldPc = currentList.pc;
            	currentList.call(normalArgument);
            	int newPc = currentList.pc;
                if (log.isDebugEnabled()) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(CALL), oldPc, newPc));
                }
                break;
            }
            case RET: {
            	int oldPc = currentList.pc;
            	currentList.ret();
            	int newPc = currentList.pc;
                if (log.isDebugEnabled()) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(RET), oldPc, newPc));
                }
                break;
            }

            case ZMSK: {
            	if (!clearMode) {
	            	// NOTE: PSP depth mask as 1 is meant to avoid depth writes,
	            	//		on pc it's the opposite
	            	gl.glDepthMask(normalArgument == 1 ? false : true);

	                if (log.isDebugEnabled()) {
	                    log("sceGuDepthMask(" + (normalArgument == 1 ? "disableWrites" : "enableWrites") + ")");
	                }
            	}
            	break;
            }

	        case ATST: {

                    int guFunc = normalArgument & 0xFF;
	            	int guReferenceAlphaValue = (normalArgument >> 8) & 0xFF;
	            	int glFunc = GL.GL_ALWAYS;
                    float glReferenceAlphaValue = guReferenceAlphaValue / 255.0f;

	            	log("sceGuAlphaFunc(" + guFunc + "," + guReferenceAlphaValue + ")");

	            	switch(guFunc) {
	            	case ATST_NEVER_PASS_PIXEL:
	            		glFunc = GL.GL_NEVER;
	            		break;

	            	case ATST_ALWAYS_PASS_PIXEL:
	            		glFunc = GL.GL_ALWAYS;
	            		break;

	            	case ATST_PASS_PIXEL_IF_MATCHES:
	            		glFunc = GL.GL_EQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_DIFFERS:
	            		glFunc = GL.GL_NOTEQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_LESS:
	            		glFunc = GL.GL_LESS;
	            		break;

	            	case ATST_PASS_PIXEL_IF_LESS_OR_EQUAL:
	            		glFunc = GL.GL_LEQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_GREATER:
	            		glFunc = GL.GL_GREATER;
	            		break;

	            	case ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL:
	            		glFunc = GL.GL_GEQUAL;
	            		break;

                    default:
                        log.warn("sceGuAlphaFunc unhandled func " + guFunc);
                        break;
	            	}

            		gl.glAlphaFunc(glFunc, glReferenceAlphaValue);

	            	break;
	            }

            case STST: {

            	int func = GL.GL_ALWAYS;

            	switch (normalArgument & 0xFF) {
            		case STST_FUNCTION_NEVER_PASS_STENCIL_TEST:
            			func = GL.GL_NEVER;
            			break;

                	case STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST:
                		func = GL.GL_ALWAYS;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_MATCHES:
                		func = GL.GL_EQUAL;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_DIFFERS:
                		func = GL.GL_NOTEQUAL;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_LESS:
                		func = GL.GL_LESS;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL:
                		func = GL.GL_LEQUAL;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_GREATER:
                		func = GL.GL_GREATER;
            			break;

                	case STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL:
                		func = GL.GL_GEQUAL;
            			break;
            	}

                int ref  = (normalArgument >>  8) & 0xff;
                int mask = (normalArgument >> 16) & 0xff;
            	gl.glStencilFunc (func, ref, mask);

            	log ("sceGuStencilFunc(func=" + (normalArgument & 0xFF) + ", ref=" + ref + ", mask=" + mask + ")");
            	break;
            }

            case ZTST: {

                depthFunc2D = GL.GL_LESS;
                depthFunc3D = depthFunc2D;

            	switch (normalArgument & 0xFF) {
                    case ZTST_FUNCTION_NEVER_PASS_PIXEL:
                        depthFunc2D = GL.GL_NEVER;
                        depthFunc3D = GL.GL_NEVER;
                        break;
                    case ZTST_FUNCTION_ALWAYS_PASS_PIXEL:
                        depthFunc2D = GL.GL_ALWAYS;
                        depthFunc3D = GL.GL_ALWAYS;
                        break;
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL:
                        depthFunc2D = GL.GL_EQUAL;
                        depthFunc3D = GL.GL_EQUAL;
                        break;
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL:
                        depthFunc2D = GL.GL_NOTEQUAL;
                        depthFunc3D = GL.GL_NOTEQUAL;
                        break;
                    // TODO Remove this hack of depth test inversion for 3D and properly translate the GE commands
                    // But I guess we need to implement zscale first... which is about very difficult to do
                    // The depth is correctly handled for 2D drawing for not for 3D.
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS:
                        depthFunc2D = GL.GL_LESS;
                        depthFunc3D = GL.GL_GREATER;
                        break;
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL:
                        depthFunc2D = GL.GL_LEQUAL;
                        depthFunc3D = GL.GL_GEQUAL;
                        break;
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER:
                        depthFunc2D = GL.GL_GREATER;
                        depthFunc3D = GL.GL_LESS;
                        break;
                    case ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL:
                        depthFunc2D = GL.GL_GEQUAL;
                        depthFunc3D = GL.GL_LEQUAL;
                        break;
            	}

            	if (useShaders) {
            	    // Shaders handle correctly the depth in 3D
                    depthFunc3D = depthFunc2D;
                }

                log ("sceGuDepthFunc(" + normalArgument + ")");
                break;
            }

            case SCISSOR1:
                scissor_x1 = normalArgument & 0x3ff;
                scissor_y1 = normalArgument >> 10; // & 0x3ff?
                break;

            case SCISSOR2:
                scissor_x2 = normalArgument & 0x3ff;
                scissor_y2 = normalArgument >> 10; // & 0x3ff?
                scissor_width = 1 + scissor_x2 - scissor_x1;
                scissor_height = 1 + scissor_y2 - scissor_y1;

                // scissor enable/disable is determined by the scissor area matching the region area,
                // there's a problem if the region coords change while the scissor coords stay constant.
                // TODO
                // - as a workaround can we keep gl scissor on all the time at no performance loss?
                // - or we can just put extra checks in the region commands
                if (log.isDebugEnabled()) {
                    log("sceGuScissor(" + scissor_x1 + "," + scissor_y1 + "," + scissor_width + "," + scissor_height + ")");
                }

                if (scissor_x1 != 0 || scissor_y1 != 0 || scissor_width != region_width || scissor_height != region_height) {
                    gl.glEnable(GL.GL_SCISSOR_TEST);
                    // old: gl.glScissor(scissor_x, scissor_y, scissor_width, scissor_height);
                    // invert y coord (for open gl?)
                    // TODO replace 272 with viewport_height?
                    gl.glScissor(scissor_x1, 272 - scissor_y1 - scissor_height, scissor_width, scissor_height);

                    if (log.isDebugEnabled()) {
                        log("sceGuEnable(GU_SCISSOR_TEST) actual y-coord " + (272 - scissor_y1 - scissor_height) + " (inverted)");
                    }
                } else {
                    gl.glDisable(GL.GL_SCISSOR_TEST);
                    log("sceGuDisable(GU_SCISSOR_TEST)");
                }
                break;

            case NEARZ:
                nearZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
                break;

            case FARZ:
                farZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
                /* I really think we don't need this...*/
                /*if (nearZ > farZ) {
                    // swap nearZ and farZ
                    float temp = nearZ;
                    nearZ = farZ;
                    farZ = temp;
                }*/

                gl.glDepthRange(nearZ, farZ);
                if (log.isDebugEnabled()) {
                    log.debug("sceGuDepthRange("+ nearZ + ", " + farZ + ")");
                }
                break;

            case SOP: {
                int fail  = getStencilOp( normalArgument        & 0xFF);
                int zfail = getStencilOp((normalArgument >>  8) & 0xFF);
                int zpass = getStencilOp((normalArgument >> 16) & 0xFF);

                gl.glStencilOp(fail, zfail, zpass);
                break;
            }

            case CLEAR:
            	if(clearMode && (normalArgument & 1) == 0) {
            		clearMode = false;
            		depthFunc2D = clearModeDepthFunc;
            		gl.glPopAttrib();
            		// These attributes were not restored by glPopAttrib,
            		// restore saved copy.
                    gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, clearModeRgbScale[0]);
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, clearModeTextureEnvMode[0]);

                    // TODO Remove this glClear
            		// We should not use it at all but demos won't work at all without it and our current implementation
            		// We need to tweak the Z values written to the depth buffer, but I think this is impossible to do properly
            		// without a fragment shader
            		if(!useShaders) {
            			// gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            		} else {
            			gl.glUniform1f(Uniforms.zPos.getId(), zpos);
            			gl.glUniform1f(Uniforms.zScale.getId(), zscale);
            			gl.glUniform1i(Uniforms.texEnable.getId(), tex_enable);
            			gl.glUniform1i(Uniforms.lightingEnable.getId(), lighting ? 1 : 0);
            		}
            		log("clear mode end");
            	} else if((normalArgument & 1) != 0) {
            		clearMode = true;
            		// Save these attributes manually, they are not saved by glPushAttrib
                    gl.glGetTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, clearModeRgbScale, 0);
                    gl.glGetTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, clearModeTextureEnvMode, 0);
                    gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, 1.0f);
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

                    gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);
            		gl.glDisable(GL.GL_BLEND);
            		gl.glDisable(GL.GL_STENCIL_TEST);
            		gl.glDisable(GL.GL_LIGHTING);
            		gl.glDisable(GL.GL_TEXTURE_2D);
            		gl.glDisable(GL.GL_ALPHA_TEST);
            		gl.glDisable(GL.GL_FOG);
            		gl.glDisable(GL.GL_DEPTH_TEST);
            		gl.glDisable(GL.GL_LOGIC_OP);
            		gl.glDisable(GL.GL_CULL_FACE);
                    // TODO disable: scissor?

            		if(useShaders) {
            			gl.glUniform1f(Uniforms.zPos.getId(), 0);
            			gl.glUniform1f(Uniforms.zScale.getId(), 0);
            			gl.glUniform1i(Uniforms.texEnable.getId(), 0);
            			gl.glUniform1i(Uniforms.lightingEnable.getId(), 0);
            		}

            		// TODO Add more disabling in clear mode, we also need to reflect the change to the internal GE registers
            		boolean color = false;
            		boolean alpha = false;
            		if((normalArgument & 0x100) != 0) {
            			color = true;
            		}
            		if((normalArgument & 0x200) != 0) {
            			alpha = true;
            			// TODO Stencil not perfect, pspsdk clear code is doing more things
                		gl.glEnable(GL.GL_STENCIL_TEST);
            			gl.glStencilFunc(GL.GL_ALWAYS, 0, 0);
            			gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_ZERO);
            		}
            		if ((normalArgument & 0x400) != 0) {
	            		gl.glEnable(GL.GL_DEPTH_TEST);
	            		gl.glDepthMask(true);
            		} else {
	            		gl.glDepthMask(false);
            		}
            		clearModeDepthFunc = depthFunc2D;
            		depthFunc2D = GL.GL_ALWAYS;
            		gl.glColorMask(color, color, color, alpha);
                    if (log.isDebugEnabled()) {
                        log("clear mode : " + (normalArgument >> 8));
                    }
            	}
                break;
            case NOP:
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(NOP));
                }

                // Check if we are not reading from an invalid memory region.
                // Abort the list if this is the case.
                // This is only done in the NOP command to not impact performance.
                checkCurrentListPc();
                break;

            /*
             * Skinning
             */
            case BOFS: {
            	boneMatrixIndex = normalArgument;
            	if (log.isDebugEnabled()) {
            		log("bone matrix offset", normalArgument);
            	}
            	break;
            }
            case BONE: {
            	// Multiple BONE matrix can be loaded in sequence
            	// without having to issue a BOFS for each matrix.
            	int matrixIndex  = boneMatrixIndex / 12;
            	int elementIndex = boneMatrixIndex % 12;
            	if (matrixIndex >= 8) {
            		error("Ignoring BONE matrix element: boneMatrixIndex=" + boneMatrixIndex);
            	} else {
	            	bone_uploaded_matrix[matrixIndex][elementIndex] = floatArgument;
	            	boneMatrixIndex++;

	            	if (log.isDebugEnabled() && (boneMatrixIndex % 12) == 0) {
	                    for (int x = 0; x < 3; x++) {
	                        log.debug(String.format("bone matrix %d %.2f %.2f %.2f %.2f",
	                        							matrixIndex,
	                        							bone_uploaded_matrix[matrixIndex][x + 0],
						                        		bone_uploaded_matrix[matrixIndex][x + 3],
						                        		bone_uploaded_matrix[matrixIndex][x + 6],
						                        		bone_uploaded_matrix[matrixIndex][x + 9]));
	                    }
	            	}
            	}
                break;
            }

            /*
             * Morphing
             */
            case MW0:
            case MW1:
            case MW2:
            case MW3:
            case MW4:
            case MW5:
            case MW6:
            case MW7:
            	log("morph weight " + (command(instruction) - MW0), floatArgument);
            	morph_weight[command(instruction) - MW0] = floatArgument;
            	break;

            case TRXSBP:
            	textureTx_sourceAddress = (textureTx_sourceAddress & 0xFF000000) | normalArgument;
            	break;

            case TRXSBW:
            	textureTx_sourceAddress = (textureTx_sourceAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
            	textureTx_sourceLineWidth = normalArgument & 0x0000FFFF;
            	break;

            case TRXDBP:
            	textureTx_destinationAddress = (textureTx_destinationAddress & 0xFF000000) | normalArgument;
            	break;

            case TRXDBW:
            	textureTx_destinationAddress = (textureTx_destinationAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
            	textureTx_destinationLineWidth = normalArgument & 0x0000FFFF;
            	break;

            case TRXSIZE:
            	textureTx_width = (normalArgument & 0x3FF) + 1;
            	textureTx_height = ((normalArgument >> 10) & 0x1FF) + 1;
            	break;

            case TRXPOS:
            	textureTx_sx = normalArgument & 0x1FF;
            	textureTx_sy = (normalArgument >> 10) & 0x1FF;
            	break;

            case TRXDPOS:
            	textureTx_dx = normalArgument & 0x1FF;
            	textureTx_dy = (normalArgument >> 10) & 0x1FF;
            	break;

            case TRXKICK:
            	textureTx_pixelSize = normalArgument & 0x1;

                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(TRXKICK) + " from 0x" + Integer.toHexString(textureTx_sourceAddress) + "(" + textureTx_sx + "," + textureTx_sy + ") to 0x" + Integer.toHexString(textureTx_destinationAddress) + "(" + textureTx_dx + "," + textureTx_dy + "), width=" + textureTx_width + ", height=" + textureTx_height + ", pixelSize=" + textureTx_pixelSize);
                }

                updateGeBuf();

                pspdisplay display = pspdisplay.getInstance();
            	if (!display.isGeAddress(textureTx_destinationAddress)) {
            		if (log.isDebugEnabled()) {
            			log(helper.getCommandString(TRXKICK) + " not in Ge Address space");
            		}
                	int width = textureTx_width;
                	int height = textureTx_height;
                	int bpp = ( textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE ) ? 2 : 4;

                	int srcAddress = textureTx_sourceAddress      + (textureTx_sy * textureTx_sourceLineWidth      + textureTx_sx) * bpp;
            		int dstAddress = textureTx_destinationAddress + (textureTx_dy * textureTx_destinationLineWidth + textureTx_dx) * bpp;
            		Memory memory = Memory.getInstance();
            		if (textureTx_sourceLineWidth == width && textureTx_destinationLineWidth == width) {
            			// All the lines are adjacent in memory,
            			// copy them all in a single memcpy operation.
        				int copyLength = height * width * bpp;
            			if (log.isDebugEnabled()) {
            				log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
            			}
            			memory.memcpy(dstAddress, srcAddress, copyLength);
            		} else {
            			// The lines are not adjacent in memory: copy line by line.
        				int copyLength = width * bpp;
        				int srcLineLength = textureTx_sourceLineWidth * bpp;
        				int dstLineLength = textureTx_destinationLineWidth * bpp;
	            		for (int y = 0; y < height; y++) {
	            			if (log.isDebugEnabled()) {
	            				log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
	            			}
	            			memory.memcpy(dstAddress, srcAddress, copyLength);
	            			srcAddress += srcLineLength;
	            			dstAddress += dstLineLength;
	            		}
            		}

                    if (State.captureGeNextFrame) {
                        log.warn("TRXKICK outside of Ge Address space not supported in capture yet");
                    }
            	} else {
	            	if (textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE) {
	                    log.warn("Unsupported 16bit for video command [ " + helper.getCommandString(command(instruction)) + " ]");
	            		break;
	            	}

	            	int width = textureTx_width;
	            	int height = textureTx_height;
	            	int dx = textureTx_dx;
	            	int dy = textureTx_dy;
	            	int lineWidth = textureTx_sourceLineWidth;
	            	int bpp = (textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE) ? 2 : 4;

	            	int geAddr = display.getTopAddrGe();
	            	dy +=  (textureTx_destinationAddress - geAddr) / (display.getBufferWidthGe() * bpp);
	            	dx += ((textureTx_destinationAddress - geAddr) % (display.getBufferWidthGe() * bpp)) / bpp;

            		if (log.isDebugEnabled()) {
            			log(helper.getCommandString(TRXKICK) + " in Ge Address space: dx=" + dx + ", dy=" + dy + ", width=" + width + ", height=" + height);
            		}

	            	int[] textures = new int[1];
                	gl.glGenTextures(1, textures, 0);
                	int texture = textures[0];
	            	gl.glBindTexture(GL.GL_TEXTURE_2D, texture);

                    gl.glPushAttrib(GL.GL_ENABLE_BIT);
	            	gl.glDisable(GL.GL_DEPTH_TEST);
	            	gl.glDisable(GL.GL_BLEND);
                    gl.glDisable(GL.GL_ALPHA_TEST);
                    gl.glDisable(GL.GL_FOG);
                    gl.glDisable(GL.GL_LIGHTING);
                    gl.glDisable(GL.GL_LOGIC_OP);
                    gl.glDisable(GL.GL_STENCIL_TEST);
                    gl.glDisable(GL.GL_SCISSOR_TEST);

	            	gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, bpp);
	            	gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, lineWidth);

	            	gl.glMatrixMode(GL.GL_PROJECTION);
	            	gl.glPushMatrix();
	            	gl.glLoadIdentity();
	            	gl.glOrtho(0, 480, 272, 0, -1, 1);
	                gl.glMatrixMode(GL.GL_MODELVIEW);
	                gl.glPushMatrix ();
	                gl.glLoadIdentity();

                	Buffer buffer = Memory.getInstance().getBuffer(textureTx_sourceAddress, lineWidth * height * bpp);

                    if (State.captureGeNextFrame) {
                        log.info("Capture TRXKICK");
                        CaptureManager.captureRAM(textureTx_sourceAddress, lineWidth * height * bpp);
                    }

	        		//
	        		// glTexImage2D only supports
	        		//		width = (1 << n)	for some integer n
	        		//		height = (1 << m)	for some integer m
	            	//
	        		// This the reason why we are also using glTexSubImage2D.
	            	//
                	int bufferHeight = Utilities.makePow2(height);
                    gl.glTexImage2D(
                            GL.GL_TEXTURE_2D, 0,
                            GL.GL_RGBA,
                            lineWidth, bufferHeight, 0,
                            GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);

                	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

    	            gl.glTexSubImage2D(
    		                GL.GL_TEXTURE_2D, 0,
    		                textureTx_sx, textureTx_sy, width, height,
    		                GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);

    	            gl.glEnable(GL.GL_TEXTURE_2D);

                    gl.glBegin(GL.GL_QUADS);
    	            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    	            float texCoordX = width / (float) lineWidth;
    	            float texCoordY = height / (float) bufferHeight;

    	            gl.glTexCoord2f(0.0f, 0.0f);
    	            gl.glVertex2i(dx, dy);

    	            gl.glTexCoord2f(texCoordX, 0.0f);
    	            gl.glVertex2i(dx + width, dy);

    	            gl.glTexCoord2f(texCoordX, texCoordY);
    	            gl.glVertex2i(dx + width, dy + height);

    	            gl.glTexCoord2f(0.0f, texCoordY);
    	            gl.glVertex2i(dx, dy + height);

    	            gl.glEnd();

    	            gl.glMatrixMode(GL.GL_MODELVIEW);
	                gl.glPopMatrix();
	                gl.glMatrixMode(GL.GL_PROJECTION);
	                gl.glPopMatrix();

	                gl.glPopAttrib();

	                gl.glDeleteTextures(1, textures, 0);
            	}
            	break;

            case TWRAP:
            	int wrapModeS =  normalArgument       & 0xFF;
            	int wrapModeT = (normalArgument >> 8) & 0xFF;
            	switch (wrapModeS) {
            		case TWRAP_WRAP_MODE_REPEAT: {
            			tex_wrap_s = GL.GL_REPEAT;
            			break;
            		}
            		case TWRAP_WRAP_MODE_CLAMP: {
            			tex_wrap_s = GL.GL_CLAMP_TO_EDGE;
            			break;
            		}
            		default: {
                        log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + wrapModeS);
            		}
            	}

            	switch (wrapModeT) {
	        		case TWRAP_WRAP_MODE_REPEAT: {
	        			tex_wrap_t = GL.GL_REPEAT;
	        			break;
	        		}
	        		case TWRAP_WRAP_MODE_CLAMP: {
            			tex_wrap_t = GL.GL_CLAMP_TO_EDGE;
	        			break;
	        		}
	        		default: {
	                    log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + wrapModeT);
	        		}
            	}
            	break;

            case PSUB:
                patch_div_s =  normalArgument       & 0xFF;
                patch_div_t = (normalArgument >> 8) & 0xFF;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(PSUB) + " patch_div_s=" + patch_div_s + ", patch_div_t=" + patch_div_t);
                }
                break;

            case BEZIER:
                int ucount =  normalArgument       & 0xFF;
                int vcount = (normalArgument >> 8) & 0xFF;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(BEZIER) + " ucount=" + ucount + ", vcount=" + vcount);
                }

                updateGeBuf();
                loadTexture();

                drawBezier(ucount, vcount);
                break;
            case CPE: //Clip Plane Enable
                //uggly trying ... don't break any demo (that I've tested)
                if(normalArgument != 0) {
                    gl.glEnable(GL.GL_CLIP_PLANE0);
                    gl.glEnable(GL.GL_CLIP_PLANE1);
                    gl.glEnable(GL.GL_CLIP_PLANE2);
                    gl.glEnable(GL.GL_CLIP_PLANE3);
                    gl.glEnable(GL.GL_CLIP_PLANE4);
                    gl.glEnable(GL.GL_CLIP_PLANE5);
                    log("Clip Plane Enable (int="+normalArgument+",float="+floatArgument+")");
                }
                else {
                    gl.glDisable(GL.GL_CLIP_PLANE0);
                    gl.glDisable(GL.GL_CLIP_PLANE1);
                    gl.glDisable(GL.GL_CLIP_PLANE2);
                    gl.glDisable(GL.GL_CLIP_PLANE3);
                    gl.glDisable(GL.GL_CLIP_PLANE4);
                    gl.glDisable(GL.GL_CLIP_PLANE5);
                    log("Clip Plane Disable (int="+normalArgument+",float="+floatArgument+")");
                }
                break;

            case DFIX:
                dfix_color[0] = ((normalArgument      ) & 255) / 255.f;
                dfix_color[1] = ((normalArgument >>  8) & 255) / 255.f;
                dfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
                dfix_color[3] = 1.f;
                if (log.isDebugEnabled()) {
                    log(String.format("%s : 0x%08X", helper.getCommandString(command), normalArgument));
                }
                break;

            case SFIX:
                sfix_color[0] = ((normalArgument      ) & 255) / 255.f;
                sfix_color[1] = ((normalArgument >>  8) & 255) / 255.f;
                sfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
                sfix_color[3] = 1.f;
                if (log.isDebugEnabled()) {
                    log(String.format("%s : 0x%08X", helper.getCommandString(command), normalArgument));
                }
                break;
            case LOP:
                int LogicOp = getLogicalOp(normalArgument & 0x0F);
                gl.glLogicOp(LogicOp);
                log.debug("sceGuLogicalOp( LogicOp = " + normalArgument + "(" +getLOpName(normalArgument) + ")" );
                break;

            case DTH0:
                dither_matrix[0] = (normalArgument      ) & 0x0F;
                dither_matrix[1] = (normalArgument >> 4 ) & 0x0F;
                dither_matrix[2] = (normalArgument >> 8 ) & 0x0F;
                dither_matrix[3] = (normalArgument >> 12) & 0x0F;

                //The dither matrix's values can vary between -4 and 4.
                //Check for values superior to 4 and inferior to -4 and
                //translate them properly.

                for(int i = 0; i < 16; i++){
                    if(dither_matrix[i] > 4){
                        dither_matrix[i] -= 16;
                    }

                    else if(dither_matrix[i] < -4){
                        dither_matrix[i] += 16;
                    }
                }

                if (log.isDebugEnabled()) {
                    log("DTH0:" + "  " + dither_matrix[0] + "  " + dither_matrix[1] + "  "
                        + dither_matrix[2] + "  " + dither_matrix[3]);
                }

                break;

            case DTH1:
                dither_matrix[4] = (normalArgument      ) & 0x0F;
                dither_matrix[5] = (normalArgument >> 4 ) & 0x0F;
                dither_matrix[6] = (normalArgument >> 8 ) & 0x0F;
                dither_matrix[7] = (normalArgument >> 12) & 0x0F;

                for(int i = 0; i < 16; i++){
                    if(dither_matrix[i] > 4){
                        dither_matrix[i] -= 16;
                    }

                    else if(dither_matrix[i] < -4){
                        dither_matrix[i] += 16;
                    }
                }

                if (log.isDebugEnabled()) {
                log("DTH1:" + "  " + dither_matrix[4] + "  " + dither_matrix[5] + "  "
                        + dither_matrix[6] + "  " + dither_matrix[7]);
                }

                break;

            case DTH2:
                dither_matrix[8] = (normalArgument      ) & 0x0F;
                dither_matrix[9] = (normalArgument >> 4 ) & 0x0F;
                dither_matrix[10] = (normalArgument >> 8 ) & 0x0F;
                dither_matrix[11] = (normalArgument >> 12) & 0x0F;

                for(int i = 0; i < 16; i++){
                    if(dither_matrix[i] > 4){
                        dither_matrix[i] -= 16;
                    }

                    else if(dither_matrix[i] < -4){
                        dither_matrix[i] += 16;
                    }
                }

                if (log.isDebugEnabled()) {
                log("DTH2:" + "  " + dither_matrix[8] + "  " + dither_matrix[9] + "  "
                        + dither_matrix[10] + "  " + dither_matrix[11]);
                }

                break;

            case DTH3:
                dither_matrix[12] = (normalArgument      ) & 0x0F;
                dither_matrix[13] = (normalArgument >> 4 ) & 0x0F;
                dither_matrix[14] = (normalArgument >> 8 ) & 0x0F;
                dither_matrix[15] = (normalArgument >> 12) & 0x0F;

                for(int i = 0; i < 16; i++){
                    if(dither_matrix[i] > 4){
                        dither_matrix[i] -= 16;
                    }

                    else if(dither_matrix[i] < -4){
                        dither_matrix[i] += 16;
                    }
                }

                if (log.isDebugEnabled()) {
                log("DTH3:" + "  " + dither_matrix[12] + "  " + dither_matrix[13] + "  "
                        + dither_matrix[14] + "  " + dither_matrix[15]);
                }

                break;

            case BBOX: {
                int numberOfVertexBoundingBox = normalArgument;
                // TODO Check if the bounding box is visible
                if (log.isInfoEnabled()) {
                    log.info("Not implemented (but can be ignored): " + helper.getCommandString(BBOX) + " numberOfVertex=" + numberOfVertexBoundingBox);
                }
                // If not visible, set takeConditionalJump to true.
            	takeConditionalJump = false;
            	break;
            }
            case BJUMP: {
            	if (takeConditionalJump) {
                	int oldPc = currentList.pc;
                	currentList.jump(normalArgument);
                	int newPc = currentList.pc;
                    if (log.isDebugEnabled()) {
                        log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(BJUMP), oldPc, newPc));
                    }
            	} else {
	                if (log.isDebugEnabled()) {
	                    log(helper.getCommandString(BJUMP) + " not taking Conditional Jump");
	                }
            	}
                break;
            }

            case PMSKC: {
                if (log.isDebugEnabled()) {
                    log(String.format("%s color mask=0x%06X", helper.getCommandString(PMSKC), normalArgument));
                }
            	glColorMask[0] = getGLMask("Red color mask"  , (normalArgument      ) & 0xFF);
            	glColorMask[1] = getGLMask("Green color mask", (normalArgument >>  8) & 0xFF);
            	glColorMask[2] = getGLMask("Blue color mask" , (normalArgument >> 16) & 0xFF);
            	if (!clearMode) {
            		setGLColorMask();
            	}
                break;
            }
            case PMSKA: {
                if (log.isDebugEnabled()) {
                    log(String.format("%s alpha mask=0x%02X", helper.getCommandString(PMSKA), normalArgument));
                }
            	glColorMask[3] = getGLMask("Alpha color mask", normalArgument & 0xFF);
            	if (!clearMode) {
                	setGLColorMask();
            	}
                break;
            }

            case UNKNOWNCOMMAND_0xFF: {
            	// This command always appears before a BOFS command and seems to have
            	// no special meaning.
            	// The command also appears sometimes after a PRIM command.
            	// Ignore the command in these cases.
            	if (log.isInfoEnabled()) {
	            	Memory mem = Memory.getInstance();
	            	int nextCommand     = mem.read8(currentList.pc + 3);
	            	int previousCommand = mem.read8(currentList.pc - 5);
	            	if (normalArgument != 0) {
	            		// normalArgument != 0 means that we are executing some random
	            		// command list. Display this as an error, which will abort
	            		// the list processing when too many errors are displayed.
	                    error("Unknown/unimplemented video command [" + helper.getCommandString(command(instruction)) + "](int="+normalArgument+",float="+floatArgument+")");
	            	} else if (nextCommand != BOFS && previousCommand != PRIM && previousCommand != UNKNOWNCOMMAND_0xFF) {
	                    log.warn("Unknown/unimplemented video command [" + helper.getCommandString(command(instruction)) + "](int="+normalArgument+",float="+floatArgument+")");
	            	} else if (log.isDebugEnabled()) {
	                    log.debug("Ignored video command [" + helper.getCommandString(command(instruction)) + "](int="+normalArgument+")");
	            	}
            	}
                break;
            }

            default:
                log.warn("Unknown/unimplemented video command [" + helper.getCommandString(command(instruction)) + "](int="+normalArgument+",float="+floatArgument+")");
        }
        commandStatistics[command].end();
    }

    private void enableClientState(boolean useVertexColor, boolean useTexture) {
        if (vinfo.texture != 0 || useTexture) {
        	gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        }
        if (useVertexColor) {
        	gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        }
        if (vinfo.normal != 0) {
        	gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        }
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
    }

    private void glTexCoordPointer(boolean useTexture, int type, int stride, int offset, boolean isNative, boolean useVboFloatBuffer) {
	    if (vinfo.texture != 0 || useTexture) {
			if (isNative) {
				glBindBuffer(0);
				gl.glTexCoordPointer(2, type, vinfo.vertexSize, nativeBuffer.position(offset));
			} else {
		        glBindBuffer();
		        if (useVBO) {
		        	gl.glTexCoordPointer(2, type, stride, offset);
		        } else if (useVboFloatBuffer) {
		        	gl.glTexCoordPointer(2, type, stride, vboFloatBuffer.position(offset / BufferUtil.SIZEOF_FLOAT));
		        } else {
		        	gl.glTexCoordPointer(2, type, stride, vboBuffer.position(offset));
		        }
			}
	    }
    }

    private void glColorPointer(boolean useVertexColor, int type, int stride, int offset, boolean isNative, boolean useVboFloatBuffer) {
    	if (useVertexColor) {
        	if (isNative) {
            	glBindBuffer(0);
        		gl.glColorPointer(4, type, vinfo.vertexSize, nativeBuffer.position(offset));
        	} else {
                glBindBuffer();
                if (useVBO) {
                	gl.glColorPointer(4, type, stride, offset);
                } else if (useVboFloatBuffer) {
                	gl.glColorPointer(4, type, stride, vboFloatBuffer.position(offset / BufferUtil.SIZEOF_FLOAT));
                } else {
                	gl.glColorPointer(4, type, stride, vboBuffer.position(offset));
                }
            }
    	}
    }

    private void glVertexPointer(int type, int stride, int offset, boolean isNative, boolean useVboFloatBuffer) {
    	if (isNative) {
        	glBindBuffer(0);
        	gl.glVertexPointer(3, type, vinfo.vertexSize, nativeBuffer.position(offset));
        } else {
            glBindBuffer();
            if (useVBO) {
            	gl.glVertexPointer(3, type, stride, offset);
            } else if (useVboFloatBuffer) {
            	gl.glVertexPointer(3, type, stride, vboFloatBuffer.position(offset / BufferUtil.SIZEOF_FLOAT));
            } else {
            	gl.glVertexPointer(3, type, stride, vboBuffer.position(offset));
            }
        }
    	
    }

    private void glNormalPointer(int type, int stride, int offset, boolean isNative, boolean useVboFloatBuffer) {
    	if (vinfo.normal != 0) {
			if (isNative) {
	        	glBindBuffer(0);
	    		gl.glNormalPointer(type, vinfo.vertexSize, nativeBuffer.position(offset));
			} else {
	            glBindBuffer();
	            if (useVBO) {
	            	gl.glNormalPointer(type, stride, offset);
	            } else if (useVboFloatBuffer) {
	            	gl.glNormalPointer(type, stride, vboFloatBuffer.position(offset / BufferUtil.SIZEOF_FLOAT));
	            } else {
	            	gl.glNormalPointer(type, stride, vboBuffer.position(offset));
	            }
			}
    	}
    }

    private void bindBuffers(boolean useVertexColor, boolean useTexture) {
    	int stride = 0, cpos = 0, npos = 0, vpos = 0;

    	if (vinfo.texture != 0 || useTexture) {
        	stride += BufferUtil.SIZEOF_FLOAT * 2;
        	cpos = npos = vpos = stride;
        }
        if (useVertexColor) {
        	stride += BufferUtil.SIZEOF_FLOAT * 4;
        	npos = vpos = stride;
        }
        if (vinfo.normal != 0) {
        	stride += BufferUtil.SIZEOF_FLOAT * 3;
        	vpos = stride;
        }
        stride += BufferUtil.SIZEOF_FLOAT * 3;

    	enableClientState(useVertexColor, useTexture);
        glTexCoordPointer(useTexture, GL.GL_FLOAT, stride, 0, false, true);
        glColorPointer(useVertexColor, GL.GL_FLOAT, stride, cpos, false, true);
        glNormalPointer(GL.GL_FLOAT, stride, npos, false, true);
    	glVertexPointer(GL.GL_FLOAT, stride, vpos, false, true);
	}

    public void doPositionSkinning(VertexInfo vinfo, float[] boneWeights, float[] position) {
    	float x = 0, y = 0, z = 0;
    	for (int i = 0; i < vinfo.skinningWeightCount; i++) {
    		if (boneWeights[i] != 0) {
				x += (	position[0] * bone_uploaded_matrix[i][0]
  				     + 	position[1] * bone_uploaded_matrix[i][3]
  				     + 	position[2] * bone_uploaded_matrix[i][6]
  				     +           bone_uploaded_matrix[i][9]) * boneWeights[i];

  				y += (	position[0] * bone_uploaded_matrix[i][1]
  				     + 	position[1] * bone_uploaded_matrix[i][4]
  				     + 	position[2] * bone_uploaded_matrix[i][7]
  				     +           bone_uploaded_matrix[i][10]) * boneWeights[i];

  				z += (	position[0] * bone_uploaded_matrix[i][2]
  				     + 	position[1] * bone_uploaded_matrix[i][5]
  				     + 	position[2] * bone_uploaded_matrix[i][8]
  				     +           bone_uploaded_matrix[i][11]) * boneWeights[i];
    		}
    	}

    	position[0] = x;
    	position[1] = y;
    	position[2] = z;
    }

    public void doNormalSkinning(VertexInfo vinfo, float[] boneWeights, float[] normal) {
    	float nx = 0, ny = 0, nz = 0;
    	for (int i = 0; i < vinfo.skinningWeightCount; i++) {
    		if (boneWeights[i] != 0) {
				// Normals shouldn't be translated :)
				nx += (	normal[0] * bone_uploaded_matrix[i][0]
				   + 	normal[1] * bone_uploaded_matrix[i][3]
				   +	normal[2] * bone_uploaded_matrix[i][6]) * boneWeights[i];

				ny += (	normal[0] * bone_uploaded_matrix[i][1]
				   + 	normal[1] * bone_uploaded_matrix[i][4]
				   + 	normal[2] * bone_uploaded_matrix[i][7]) * boneWeights[i];

				nz += (	normal[0] * bone_uploaded_matrix[i][2]
				   + 	normal[1] * bone_uploaded_matrix[i][5]
				   + 	normal[2] * bone_uploaded_matrix[i][8]) * boneWeights[i];
    		}
    	}

		/*
		// TODO: I doubt psp hardware normalizes normals after skinning,
		// but if it does, this should be uncommented :)
		float length = nx*nx + ny*ny + nz*nz;

		if (length > 0.f) {
			length = 1.f / (float)Math.sqrt(length);

			nx *= length;
			ny *= length;
			nz *= length;
		}
		*/

    	normal[0] = nx;
    	normal[1] = ny;
    	normal[2] = nz;
    }

    private void doSkinning(VertexInfo vinfo, VertexState v) {
    	float x = 0, y = 0, z = 0;
    	float nx = 0, ny = 0, nz = 0;
		for(int i = 0; i < vinfo.skinningWeightCount; ++i) {
			if(v.boneWeights[i] != 0.f) {

				x += (	v.p[0] * bone_uploaded_matrix[i][0]
				     + 	v.p[1] * bone_uploaded_matrix[i][3]
				     + 	v.p[2] * bone_uploaded_matrix[i][6]
				     +           bone_uploaded_matrix[i][9]) * v.boneWeights[i];

				y += (	v.p[0] * bone_uploaded_matrix[i][1]
				     + 	v.p[1] * bone_uploaded_matrix[i][4]
				     + 	v.p[2] * bone_uploaded_matrix[i][7]
				     +           bone_uploaded_matrix[i][10]) * v.boneWeights[i];

				z += (	v.p[0] * bone_uploaded_matrix[i][2]
				     + 	v.p[1] * bone_uploaded_matrix[i][5]
				     + 	v.p[2] * bone_uploaded_matrix[i][8]
				     +           bone_uploaded_matrix[i][11]) * v.boneWeights[i];

				// Normals shouldn't be translated :)
				nx += (	v.n[0] * bone_uploaded_matrix[i][0]
				   + 	v.n[1] * bone_uploaded_matrix[i][3]
				   +	v.n[2] * bone_uploaded_matrix[i][6]) * v.boneWeights[i];

				ny += (	v.n[0] * bone_uploaded_matrix[i][1]
				   + 	v.n[1] * bone_uploaded_matrix[i][4]
				   + 	v.n[2] * bone_uploaded_matrix[i][7]) * v.boneWeights[i];

				nz += (	v.n[0] * bone_uploaded_matrix[i][2]
				   + 	v.n[1] * bone_uploaded_matrix[i][5]
				   + 	v.n[2] * bone_uploaded_matrix[i][8]) * v.boneWeights[i];
			}
		}

		v.p[0] = x;	v.p[1] = y;	v.p[2] = z;

		/*
		// TODO: I doubt psp hardware normalizes normals after skinning,
		// but if it does, this should be uncommented :)
		float length = nx*nx + ny*ny + nz*nz;

		if (length > 0.f) {
			length = 1.f / (float)Math.sqrt(length);

			nx *= length;
			ny *= length;
			nz *= length;
		}
		*/

		v.n[0] = nx;	v.n[1] = ny;	v.n[2] = nz;
	}

    private void log(String commandString, float floatArgument) {
        if (log.isDebugEnabled()) {
            log(commandString+SPACE+floatArgument);
        }
    }

    private void log(String commandString, int value) {
        if (log.isDebugEnabled()) {
            log(commandString+SPACE+value);
        }
    }

    private void log(String commandString, float[] matrix) {
        if (log.isDebugEnabled()) {
            for (int y = 0; y < 4; y++) {
                log(commandString+SPACE+String.format("%.1f %.1f %.1f %.1f", matrix[0 + y * 4], matrix[1 + y * 4], matrix[2 + y * 4], matrix[3 + y * 4]));
            }
        }
    }

    private String getOpenGLVersion(GL gl) {
    	return gl.glGetString(GL.GL_VERSION);
    }

    private void convertPixelType(short[] source, int[] destination,
    		                      int aMask, int aShift,
    		                      int rMask, int rShift,
    		                      int gMask, int gShift,
    		                      int bMask, int bShift,
    		                      int level) {
    	for (int i = 0; i < texture_buffer_width[level]*texture_height[level]; i++) {
    		int pixel = source[i];
    		int color = ((pixel & aMask) << aShift) |
    		            ((pixel & rMask) << rShift) |
    		            ((pixel & gMask) << gShift) |
    		            ((pixel & bMask) << bShift);
    		destination[i] = color;
    	}
    }

    private void loadTexture() {
        // HACK: avoid texture uploads of null pointers
        // This can come from Sony's GE init code (pspsdk GE init is ok)
        if (texture_base_pointer[0] == 0)
            return;

        // Texture not used in clear mode or when disabled.
        if (clearMode || tex_enable == 0) {
        	return;
        }

        Texture texture;
        int tex_addr = texture_base_pointer[0] & Memory.addressMask;
        // Some games are storing compressed textures in VRAM (e.g. Skate Park City).
        // Force only a reload of textures that can be generated by the GE buffer,
        // i.e. when texture_storage is one of
        // BGR5650=0, ABGR5551=1, ABGR4444=2 or ABGR8888=3.
        if (!useTextureCache || (isVRAM(tex_addr) && texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888)) {
            texture = null;

            // Generate a texture id if we don't have one
            if (gl_texture_id[0] == 0) {
                gl.glGenTextures(1, gl_texture_id, 0);
            }

            gl.glBindTexture(GL.GL_TEXTURE_2D, gl_texture_id[0]);
        } else {
            // Check if the texture is in the cache
            texture = TextureCache.getInstance().getTexture( texture_base_pointer[0]
                                                           , texture_buffer_width[0]
                                                           , texture_width[0]
                                                           , texture_height[0]
                                                           , texture_storage
                                                           , tex_clut_addr
                                                           , tex_clut_mode
                                                           , tex_clut_start
                                                           , tex_clut_shift
                                                           , tex_clut_mask
                                                           , tex_clut_num_blocks
                                                           , texture_num_mip_maps
                                                           , mipmapShareClut
                                                           );

            // Create the texture if not yet in the cache
            if (texture == null) {
            	TextureCache textureCache = TextureCache.getInstance();
                texture = new Texture( textureCache
                		             , texture_base_pointer[0]
                                     , texture_buffer_width[0]
                                     , texture_width[0]
                                     , texture_height[0]
                                     , texture_storage
                                     , tex_clut_addr
                                     , tex_clut_mode
                                     , tex_clut_start
                                     , tex_clut_shift
                                     , tex_clut_mask
                                     , tex_clut_num_blocks
                                     , texture_num_mip_maps
                                     , mipmapShareClut
                                     );
                textureCache.addTexture(gl, texture);
            }

            texture.bindTexture(gl);
        }

        // Load the texture if not yet loaded
        if (texture == null || !texture.isLoaded() || State.captureGeNextFrame || State.replayGeNextFrame) {
            if (log.isDebugEnabled()) {
                log(helper.getCommandString(TFLUSH)
                    + " " + String.format("0x%08X", texture_base_pointer[0])
                    + ", buffer_width=" + texture_buffer_width[0]
                    + " (" + texture_width[0] + "," + texture_height[0] + ")");

                log(helper.getCommandString(TFLUSH)
                    + " texture_storage=0x" + Integer.toHexString(texture_storage)
                    + "(" + getPsmName(texture_storage)
                    + "), tex_clut_mode=0x" + Integer.toHexString(tex_clut_mode)
                    + ", tex_clut_addr=" + String.format("0x%08X", tex_clut_addr)
                    + ", texture_swizzle=" + texture_swizzle);
            }

            Buffer  final_buffer = null;
            int     texture_type = 0;
            int     texclut = tex_clut_addr;
            int     texaddr;

            int textureByteAlignment = 4;   // 32 bits
            int texture_format = GL.GL_RGBA;
            boolean compressedTexture = false;

            int numberMipmaps = texture_num_mip_maps;
            // I'm not sure about the exact meaning of TBIAS_MODE_CONST.
            // I'm interpreting it here as forcing a specific mipmap (from bias parameter).
            // This seems to work with TBIAS_MODE_CONST and bias=0.
            if (tex_mipmap_mode == TBIAS_MODE_CONST) {
            	numberMipmaps = Math.min(tex_mipmap_bias_int, texture_base_pointer.length - 1);
            	log.debug("TBIAS_MODE_CONST " + tex_mipmap_bias_int);
            }

            for(int level = 0; level <= numberMipmaps; ++level) {
	            // Extract texture information with the minor conversion possible
	            // TODO: Get rid of information copying, and implement all the available formats
	            texaddr = texture_base_pointer[level];
	            texaddr &= Memory.addressMask;
	            texture_format = GL.GL_RGBA;
	            compressedTexture = false;
	            int compressedTextureSize = 0;

	            switch (texture_storage) {
	                case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED: {
	                    switch (tex_clut_mode) {
	                        case CMODE_FORMAT_16BIT_BGR5650:
	                        case CMODE_FORMAT_16BIT_ABGR5551:
	                        case CMODE_FORMAT_16BIT_ABGR4444: {
	                            if (texclut == 0)
	                                return;

	                            texture_type = texturetype_mapping[tex_clut_mode];
	                            textureByteAlignment = 2;  // 16 bits
	                            short[] clut = readClut16(level);
	                            int clutSharingOffset = mipmapShareClut ? 0 : level * 16;

	                            if (!texture_swizzle) {
	                            	int length = texture_buffer_width[level]*texture_height[level];
	                            	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
	                                for (int i = 0; i < length; i += 2) {

	                                    int index = memoryReader.readNext();

	                                    tmp_texture_buffer16[i]     = clut[getClutIndex( index       & 0xF) + clutSharingOffset];
	                                    tmp_texture_buffer16[i+1]   = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
	                                }
	                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/16 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
	                            } else {
	                                unswizzleTextureFromMemory(texaddr, 0, level);
	                                int pixels = texture_buffer_width[level] * texture_height[level];
	                                for (int i = 0, j = 0; i < pixels; i += 8, j++) {
	                                    int n = tmp_texture_buffer32[j];
	                                    int index = n & 0xF;
	                                    tmp_texture_buffer16[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 4) & 0xF;
	                                    tmp_texture_buffer16[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 8) & 0xF;
	                                    tmp_texture_buffer16[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 12) & 0xF;
	                                    tmp_texture_buffer16[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 16) & 0xF;
	                                    tmp_texture_buffer16[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 20) & 0xF;
	                                    tmp_texture_buffer16[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 24) & 0xF;
	                                    tmp_texture_buffer16[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 28) & 0xF;
	                                    tmp_texture_buffer16[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
	                                }
	                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                                break;
	                            }

	                            break;
	                        }

	                        case CMODE_FORMAT_32BIT_ABGR8888: {
	                            if (texclut == 0)
	                                return;

	                            texture_type = GL.GL_UNSIGNED_BYTE;
	                            int[] clut = readClut32(level);
	                            int clutSharingOffset = mipmapShareClut ? 0 : level * 16;

	                            if (!texture_swizzle) {
	                            	int length = texture_buffer_width[level]*texture_height[level];
	                            	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
	                                for (int i = 0; i < length; i += 2) {

	                                    int index = memoryReader.readNext();

	                                    tmp_texture_buffer32[i+1] = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
	                                    tmp_texture_buffer32[i]   = clut[getClutIndex( index       & 0xF) + clutSharingOffset];
	                                }
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/32 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
	                            } else {
	                                unswizzleTextureFromMemory(texaddr, 0, level);
	                                int pixels = texture_buffer_width[level] * texture_height[level];
	                                for (int i = pixels - 8, j = (pixels / 8) - 1; i >= 0; i -= 8, j--) {
	                                    int n = tmp_texture_buffer32[j];
	                                    int index = n & 0xF;
	                                    tmp_texture_buffer32[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 4) & 0xF;
	                                    tmp_texture_buffer32[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 8) & 0xF;
	                                    tmp_texture_buffer32[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 12) & 0xF;
	                                    tmp_texture_buffer32[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 16) & 0xF;
	                                    tmp_texture_buffer32[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 20) & 0xF;
	                                    tmp_texture_buffer32[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 24) & 0xF;
	                                    tmp_texture_buffer32[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
	                                    index = (n >> 28) & 0xF;
	                                    tmp_texture_buffer32[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
	                                }
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                            }

	                            break;
	                        }

	                        default: {
	                            error("Unhandled clut4 texture mode " + tex_clut_mode);
	                            return;
	                        }
	                    }

	                    break;
	                }
	                case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED: {
	                	final_buffer = readIndexedTexture(level, texaddr, texclut, 1);
	                    texture_type = texturetype_mapping[tex_clut_mode];
	                    textureByteAlignment = textureByteAlignmentMapping[tex_clut_mode];
	                    break;
	                }
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED: {
	                	final_buffer = readIndexedTexture(level, texaddr, texclut, 2);
	                    texture_type = texturetype_mapping[tex_clut_mode];
	                    textureByteAlignment = textureByteAlignmentMapping[tex_clut_mode];
	                	break;
	                }
	                case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED: {
	                	final_buffer = readIndexedTexture(level, texaddr, texclut, 4);
	                    texture_type = texturetype_mapping[tex_clut_mode];
	                    textureByteAlignment = textureByteAlignmentMapping[tex_clut_mode];
	                	break;
	                }
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: {
	                    texture_type = texturetype_mapping[texture_storage];
	                    textureByteAlignment = 2;  // 16 bits

	                    if (!texture_swizzle) {
                        	int length = Math.max(texture_buffer_width[level], texture_width[level]) * texture_height[level];
	                    	final_buffer = Memory.getInstance().getBuffer(texaddr, length * 2);
	                    	if (final_buffer == null) {
		                    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * 2, 2);
		                        for (int i = 0; i < length; i++) {
		                            int pixel = memoryReader.readNext();
		                            tmp_texture_buffer16[i] = (short)pixel;
		                        }

		                        final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                    	}

                            if (State.captureGeNextFrame) {
                                log.info("Capture loadTexture 16 unswizzled");
                                CaptureManager.captureRAM(texaddr, length * 2);
                            }
	                    } else {
	                        final_buffer = unswizzleTextureFromMemory(texaddr, 2, level);
	                    }

	                    break;
	                }

	                case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: {
	                    if (openGL1_2) {
	                        texture_type = GL.GL_UNSIGNED_INT_8_8_8_8_REV;  // Only available from V1.2
	                    } else {
	                        texture_type = GL.GL_UNSIGNED_BYTE;
	                    }

	                    final_buffer = getTexture32BitBuffer(texaddr, level);
	                    break;
	                }

                    case TPSM_PIXEL_STORAGE_MODE_DXT1: {
                    	if (log.isDebugEnabled()) {
                    		log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT1 " + Integer.toHexString(texaddr));
                    	}
                        texture_type = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                        compressedTexture = true;
	                    compressedTextureSize = getCompressedTextureSize(level, 8);
                    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                    	// PSP DXT1 hardware format reverses the colors and the per-pixel
                    	// bits, and encodes the color in RGB 565 format
                    	int n = compressedTextureSize / 4;
                        for (int i = 0; i < n; i += 2) {
                            tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                            tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT3: {
                    	if (log.isDebugEnabled()) {
                    		log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT3 " + Integer.toHexString(texaddr));
                    	}
                        texture_type = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                        compressedTexture = true;
	                    compressedTextureSize = getCompressedTextureSize(level, 4);
                    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                    	// PSP DXT3 format reverses the alpha and color parts of each block,
                    	// and reverses the color and per-pixel terms in the color part.
                    	int n = compressedTextureSize / 4;
                        for (int i = 0; i < n; i += 4) {
                        	// Color
                            tmp_texture_buffer32[i + 3] = memoryReader.readNext();
                            tmp_texture_buffer32[i + 2] = memoryReader.readNext();
                            // Alpha
                            tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                            tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT5: {
                    	log.warn("texture TPSM_PIXEL_STORAGE_MODE_DXT5 untested");
                    	if (log.isDebugEnabled()) {
                    		log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT5 " + Integer.toHexString(texaddr));
                    	}
                        texture_type = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                        compressedTexture = true;
	                    compressedTextureSize = getCompressedTextureSize(level, 4);
                    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 2);
                    	// PSP DXT5 format reverses the alpha and color parts of each block,
                    	// and reverses the color and per-pixel terms in the color part. In
                    	// the alpha part, the 2 reference alpha values are swapped with the
                    	// alpha interpolation values.
                    	int n = compressedTextureSize / 2;
                        for (int i = 0; i < n; i += 8) {
                        	// Color
                            tmp_texture_buffer16[i + 6] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 7] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 4] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 5] = (short) memoryReader.readNext();
                            // Alpha
                            tmp_texture_buffer16[i + 1] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 2] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 3] = (short) memoryReader.readNext();
                            tmp_texture_buffer16[i + 0] = (short) memoryReader.readNext();
                        }
                        final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                        break;
                    }

                    default: {
	                    error("Unhandled texture storage " + texture_storage);
	                    return;
	                }
	            }

	            // Some textureTypes are only supported from OpenGL v1.2.
	            // Try to convert to type supported in v1.
	            if (!openGL1_2) {
	                if (texture_type == GL.GL_UNSIGNED_SHORT_4_4_4_4_REV) {
	                    convertPixelType(tmp_texture_buffer16, tmp_texture_buffer32, 0xF000, 16, 0x0F00, 12, 0x00F0, 8, 0x000F, 4, level);
	                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                    texture_type = GL.GL_UNSIGNED_BYTE;
	                    textureByteAlignment = 4;
	                } else if (texture_type == GL.GL_UNSIGNED_SHORT_1_5_5_5_REV) {
	                    convertPixelType(tmp_texture_buffer16, tmp_texture_buffer32, 0x8000, 16, 0x7C00, 9, 0x03E0, 6, 0x001F, 3, level);
	                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                    texture_type = GL.GL_UNSIGNED_BYTE;
	                    textureByteAlignment = 4;
	                } else if (texture_type == GL.GL_UNSIGNED_SHORT_5_6_5_REV) {
	                    convertPixelType(tmp_texture_buffer16, tmp_texture_buffer32, 0x0000, 0, 0xF800, 8, 0x07E0, 5, 0x001F, 3, level);
	                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                    texture_type = GL.GL_UNSIGNED_BYTE;
	                    textureByteAlignment = 4;
	                    texture_format = GL.GL_RGB;
	                }
	            }

	            if (texture_type == GL.GL_UNSIGNED_SHORT_5_6_5_REV) {
	                texture_format = GL.GL_RGB;
	            }

	            // Upload texture to openGL
	            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, tex_min_filter);
	            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, tex_mag_filter);
	            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, textureByteAlignment);
	            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, texture_buffer_width[level]);

                // apparently w > tbw still works, but I think we should log it just incase (fiveofhearts)
                // update: seems some games are using tbw greater AND less than w, now I haven't got a clue what the meaning of the 2 variables are
                /*
                if (texture_width[level] > texture_buffer_width[level]) {
                    log.warn(helper.getCommandString(TFLUSH) + " w > tbw : w=" + texture_width[level] + " tbw=" + texture_buffer_width[level]);
                } else if (texture_width[level] < texture_buffer_width[level]) {
                    log.warn(helper.getCommandString(TFLUSH) + " w < tbw : w=" + texture_width[level] + " tbw=" + texture_buffer_width[level]);
                }
                */

	            if (compressedTexture) {
		            gl.glCompressedTexImage2D  (  GL.GL_TEXTURE_2D,
				                        level,
				                        texture_type,
				                        texture_width[level], texture_height[level],
				                        0,
				                        compressedTextureSize,
				                        final_buffer);
	            } else {
		            gl.glTexImage2D  (  GL.GL_TEXTURE_2D,
		                                level,
		                                texture_format,
		                                texture_width[level], texture_height[level],
		                                0,
		                                texture_format,
		                                texture_type,
		                                final_buffer);
	            }

	            if (State.captureGeNextFrame) {
	            	if (isVRAM(tex_addr)) {
	            		CaptureManager.captureImage(texaddr, level, final_buffer, texture_width[level], texture_height[level], texture_buffer_width[level], texture_type, compressedTexture, compressedTextureSize, false);
	                } else if (!CaptureManager.isImageCaptured(texaddr)) {
	            		CaptureManager.captureImage(texaddr, level, final_buffer, texture_width[level], texture_height[level], texture_buffer_width[level], texture_type, compressedTexture, compressedTextureSize, true);
	            	}
	            }

	            if (texture != null) {
	                texture.setIsLoaded();
	                if (log.isDebugEnabled()) {
	                    log(helper.getCommandString(TFLUSH) + " Loaded texture " + texture.getGlId());
	                }
	            }
            }

            checkTextureMinFilter(compressedTexture, numberMipmaps);

            // OpenGL cannot build mipmaps on compressed textures
            if (numberMipmaps != 0 && final_buffer != null && !compressedTexture) {
				if (log.isDebugEnabled()) {
	            	for(int level = 0; level <= numberMipmaps; ++level)
	            		log(String.format("Mipmap PSP Texture level %d size %dx%d", level, texture_width[level], texture_height[level]));
				}
	            int maxLevel = (int) (Math.log(Math.max(texture_width[numberMipmaps], texture_height[numberMipmaps]) * (1 << numberMipmaps))/Math.log(2));

	            if(maxLevel != numberMipmaps) {
	            	if (log.isDebugEnabled()) {
	            		log(String.format("Generating mipmaps from level %d Size %dx%d to maxLevel %d", numberMipmaps, texture_width[0], texture_height[0], maxLevel));
	            	}
		            // Build the other mipmaps level
		            glu.gluBuild2DMipmapLevels(GL.GL_TEXTURE_2D,
		            		texture_format,
		            		texture_width[numberMipmaps], texture_height[numberMipmaps],
		            		texture_format,
		            		texture_type,
		            		numberMipmaps, numberMipmaps + 1, maxLevel, final_buffer);
		            if (log.isDebugEnabled()) {
			            for(int i = 0; i <= maxLevel; ++i) {
			            	float[] size = new float[2];
			            	gl.glGetTexLevelParameterfv(GL.GL_TEXTURE_2D, i, GL.GL_TEXTURE_WIDTH, size, 0);
			            	gl.glGetTexLevelParameterfv(GL.GL_TEXTURE_2D, i, GL.GL_TEXTURE_HEIGHT, size, 1);
			            	log(String.format("OGL Texture level %d size %dx%d", i, (int)size[0], (int)size[1]));
			            }
		            }
	            }
            }
        } else {
        	boolean compressedTexture = (texture_storage >= TPSM_PIXEL_STORAGE_MODE_DXT1 && texture_storage <= TPSM_PIXEL_STORAGE_MODE_DXT5);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, tex_min_filter);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, tex_mag_filter);
            checkTextureMinFilter(compressedTexture, texture_num_mip_maps);

            if (log.isDebugEnabled()) {
                log(helper.getCommandString(TFLUSH) + " Reusing cached texture " + texture.getGlId());
            }
        }
    }

	private void checkTextureMinFilter(boolean compressedTexture, int numberMipmaps) {
		// OpenGL/Hardware cannot interpolate between compressed textures;
		// this restriction has been checked on NVIDIA GeForce 8500 GT and 9800 GT
		if (compressedTexture ||
		    (numberMipmaps == 0 && !(tex_min_filter == GL.GL_LINEAR || tex_min_filter == GL.GL_NEAREST))) {
			int nex_tex_min_filter;
			if(tex_min_filter == GL.GL_NEAREST_MIPMAP_LINEAR || tex_min_filter == GL.GL_NEAREST_MIPMAP_NEAREST)
				nex_tex_min_filter = GL.GL_NEAREST;
			else
				nex_tex_min_filter = GL.GL_LINEAR;
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, nex_tex_min_filter);
			log("Overwriting texture min filter, no mipmap was generated but filter was set to use mipmap");
		}
	}

	private Buffer readIndexedTexture(int level, int texaddr, int texclut, int bytesPerIndex) {
		Buffer buffer = null;

    	int length = texture_buffer_width[level]*texture_height[level];
        switch (tex_clut_mode) {
            case CMODE_FORMAT_16BIT_BGR5650:
            case CMODE_FORMAT_16BIT_ABGR5551:
            case CMODE_FORMAT_16BIT_ABGR4444: {
                if (texclut == 0)
                    return null;

                short[] clut = readClut16(level);

                if (!texture_swizzle) {
                	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer16[i] = clut[getClutIndex(index)];
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/16 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
                        case 1: {
	                        for (int i = 0, j = 0; i < length; i += 4, j++) {
	                            int n = tmp_texture_buffer32[j];
	                            int index = n & 0xFF;
	                            tmp_texture_buffer16[i + 0] = clut[getClutIndex(index)];
	                            index = (n >> 8) & 0xFF;
	                            tmp_texture_buffer16[i + 1] = clut[getClutIndex(index)];
	                            index = (n >> 16) & 0xFF;
	                            tmp_texture_buffer16[i + 2] = clut[getClutIndex(index)];
	                            index = (n >> 24) & 0xFF;
	                            tmp_texture_buffer16[i + 3] = clut[getClutIndex(index)];
	                        }
	                    	break;
                        }
                        case 2: {
	                        for (int i = 0, j = 0; i < length; i += 2, j++) {
	                            int n = tmp_texture_buffer32[j];
	                            tmp_texture_buffer16[i + 0] = clut[getClutIndex(n & 0xFFFF)];
	                            tmp_texture_buffer16[i + 1] = clut[getClutIndex(n >>> 16  )];
	                        }
                        	break;
                        }
                        case 4: {
	                        for (int i = 0; i < length; i++) {
	                            int n = tmp_texture_buffer32[i];
	                        	tmp_texture_buffer16[i] = clut[getClutIndex(n)];
	                        }
                        	break;
                        }
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                }

                break;
            }

            case CMODE_FORMAT_32BIT_ABGR8888: {
                if (texclut == 0)
                    return null;

                int[] clut = readClut32(level);

                if (!texture_swizzle) {
                	IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer32[i] = clut[getClutIndex(index)];
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/32 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
	                    case 1: {
	                        for (int i = length - 4, j = (length / 4) - 1; i >= 0; i -= 4, j--) {
	                            int n = tmp_texture_buffer32[j];
	                            int index = n & 0xFF;
	                            tmp_texture_buffer32[i + 0] = clut[getClutIndex(index)];
	                            index = (n >> 8) & 0xFF;
	                            tmp_texture_buffer32[i + 1] = clut[getClutIndex(index)];
	                            index = (n >> 16) & 0xFF;
	                            tmp_texture_buffer32[i + 2] = clut[getClutIndex(index)];
	                            index = (n >> 24) & 0xFF;
	                            tmp_texture_buffer32[i + 3] = clut[getClutIndex(index)];
	                        }
	                    	break;
	                    }
	                    case 2: {
	                        for (int i = length - 2, j = (length / 2) - 1; i >= 0; i -= 2, j--) {
	                            int n = tmp_texture_buffer32[j];
	                            tmp_texture_buffer32[i + 0] = clut[getClutIndex(n & 0xFFFF)];
	                            tmp_texture_buffer32[i + 1] = clut[getClutIndex(n >>> 16  )];
	                        }
	                    	break;
	                    }
	                    case 4: {
	                        for (int i = 0; i < length; i++) {
	                            int n = tmp_texture_buffer32[i];
	                        	tmp_texture_buffer32[i] = clut[getClutIndex(n)];
	                        }
	                    	break;
	                    }
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);
                }

                break;
            }

            default: {
                error("Unhandled clut8 texture mode " + tex_clut_mode);
                break;
            }
        }

        return buffer;
	}

	private boolean initRendering() {
        /*
         * Apply Blending
         */
        setBlendFunc();

        /*
         * Defer transformations until primitive rendering
         */
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix ();
        gl.glLoadIdentity();

        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
        	if (useViewport) {
        		gl.glViewport(viewport_cx - offset_x - viewport_width / 2, 272 - (viewport_cy - offset_y) - viewport_height / 2, viewport_width, viewport_height);
        	}
        	// Use non-inverted depthFunc (which is depthFunc2D, see ZTST)
        	// TODO Clean-up depthFunc3D when the depth-handling works correctly...
            gl.glDepthFunc(depthFunc2D);
            if (true) {
            	gl.glDepthRange(zpos - zscale, zpos + zscale);
            } else {
            	gl.glDepthRange(nearZ, farZ);
            }
            gl.glLoadMatrixf(proj_uploaded_matrix, 0);
        } else {
        	if (useViewport) {
        		gl.glViewport(0, 0, 480, 272);
        	}
            gl.glDepthFunc(depthFunc2D);
            gl.glDepthRange(0, 1);
            gl.glOrtho(0, 480, 272, 0, 0, -0xFFFF);

        	// 2D mode shouldn't be affected by the lighting and fog
            gl.glPushAttrib(GL.GL_LIGHTING_BIT | GL.GL_FOG_BIT);
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_FOG);
            if(useShaders) {
            	gl.glUniform1i(Uniforms.lightingEnable.getId(), 0);
                gl.glUniform1f(Uniforms.zPos.getId(), zpos);
                gl.glUniform1f(Uniforms.zScale.getId(), zscale);
            }
        }

        /*
         * Apply texture transforms
         */
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix ();
        gl.glLoadIdentity();
        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            gl.glTranslatef(tex_translate_x, tex_translate_y, 0.f);
            gl.glScalef(tex_scale_x, tex_scale_y, 1.f);
        } else {
            gl.glScalef(1.f / texture_width[0], 1.f / texture_height[0], 1.f);
        }

        switch (tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
                break;

            case TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX:
                gl.glMultMatrixf (texture_uploaded_matrix, 0);
                break;

            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {

                // First, setup texture uv generation
                gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
                gl.glEnable (GL.GL_TEXTURE_GEN_S);

                gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
                gl.glEnable (GL.GL_TEXTURE_GEN_T);

                // Setup also texture matrix
                gl.glMultMatrixf (tex_envmap_matrix, 0);
                break;
            }

            default:
                log ("Unhandled texture matrix mode " + tex_map_mode);
        }

        /*
         * Apply view matrix
         */
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix ();
        gl.glLoadIdentity();

        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
            gl.glLoadMatrixf(view_uploaded_matrix, 0);

        /*
         *  Setup lights on when view transformation is set up
         */
        if (lighting && transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
	        for (int i = 0; i < NUM_LIGHTS; i++) {
	            if (light_enabled[i] != 0) {
	            	gl.glLightfv(GL.GL_LIGHT0 + i, GL.GL_POSITION, light_pos[i], 0);

	            	if (light_type[i] == LIGHT_SPOT) {
	                   gl.glLightfv(GL.GL_LIGHT0 + i, GL.GL_SPOT_DIRECTION, light_dir[i], 0);
	                   gl.glLightf(GL.GL_LIGHT0 + i, GL.GL_SPOT_EXPONENT, spotLightExponent[i]);
	                   gl.glLightf(GL.GL_LIGHT0 + i, GL.GL_SPOT_CUTOFF, spotLightCutoff[i]);
	                } else {
	                	// uniform light distribution
	                   gl.glLightf(GL.GL_LIGHT0 + i, GL.GL_SPOT_EXPONENT, 0);
	                   gl.glLightf(GL.GL_LIGHT0 + i, GL.GL_SPOT_CUTOFF, 180);
	                }

	            	// Light kind:
	            	//  LIGHT_DIFFUSE_SPECULAR: use ambient, diffuse and specular colors
	            	//  all other light kinds: use ambient and diffuse colors (not specular)
	                if (light_kind[i] == LIGHT_DIFFUSE_SPECULAR) {
	                	gl.glLightfv(GL.GL_LIGHT0 + i, GL.GL_SPECULAR, lightSpecularColor[i], 0);
	                } else {
	                	gl.glLightfv(GL.GL_LIGHT0 + i, GL.GL_SPECULAR, blackColor, 0);
	                }
	            }
	        }
        }

        // Apply model matrix
        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
            gl.glMultMatrixf(model_uploaded_matrix, 0);

        boolean useVertexColor = false;
        if (!lighting || transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
            gl.glDisable(GL.GL_COLOR_MATERIAL);
            if(vinfo.color != 0) {
                useVertexColor = true;
            } else {
                gl.glColor4fv(mat_ambient, 0);
            }
        } else if (vinfo.color != 0 && mat_flags != 0) {
            useVertexColor = true;
            if(useShaders) {
        		int[] bvec = new int[4];
        		if((mat_flags & 1) != 0) bvec[1] = 1; // GL.GL_AMBIENT;
            	if((mat_flags & 2) != 0) bvec[2] = 1; // GL.GL_DIFFUSE;
            	if((mat_flags & 4) != 0) bvec[3] = 1; // GL.GL_SPECULAR;
        		bvec[0] = 1;
        		gl.glUniform4iv(Uniforms.matFlags.getId(), 1, bvec, 0);
        	} else {
            	int flags = 0;
            	// TODO : Can't emulate this properly right now since we can't mix the properties like we want
            	if((mat_flags & 1) != 0 && (mat_flags & 2) != 0)
            		flags = GL.GL_AMBIENT_AND_DIFFUSE;
            	else if((mat_flags & 1) != 0) flags = GL.GL_AMBIENT;
            	else if((mat_flags & 2) != 0) flags = GL.GL_DIFFUSE;
            	else if((mat_flags & 4) != 0) flags = GL.GL_SPECULAR;
            	gl.glColorMaterial(GL.GL_FRONT_AND_BACK, flags);
            	gl.glEnable(GL.GL_COLOR_MATERIAL);
        	}
        } else {
            gl.glDisable(GL.GL_COLOR_MATERIAL);
            gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
            gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
            gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
            if(useShaders) {
            	int[] bvec = new int[4];
            	bvec[0] = 0;
        		gl.glUniform4iv(Uniforms.matFlags.getId(), 1, bvec, 0);
            }
        }

        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, tex_wrap_s);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, tex_wrap_t);

        return useVertexColor;
    }

    private void endRendering(boolean useVertexColor, boolean useTexture) {
        switch (tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
                gl.glDisable (GL.GL_TEXTURE_GEN_S);
                gl.glDisable (GL.GL_TEXTURE_GEN_T);
                break;
            }
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        if(vinfo.texture != 0 || useTexture) gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        if(useVertexColor) gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        if(vinfo.normal != 0) gl.glDisableClientState(GL.GL_NORMAL_ARRAY);

        gl.glPopMatrix  ();
        gl.glMatrixMode (GL.GL_TEXTURE);
        gl.glPopMatrix  ();
        gl.glMatrixMode (GL.GL_PROJECTION);
        gl.glPopMatrix  ();
        gl.glMatrixMode (GL.GL_MODELVIEW);

        if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
            gl.glPopAttrib();
            if(useShaders) {
            	gl.glUniform1i(Uniforms.lightingEnable.getId(), lighting ? 1 : 0);
            }
        }
    }

    private void drawBezier(int ucount, int vcount) {
        if (ucount != 4 && vcount != 4) {
            log.warn("Unsupported Bezier parameters");
            return;
        }

        boolean useVertexColor = initRendering();

        VertexState[][] anchors = new VertexState[ucount][vcount];
        Memory mem = Memory.getInstance();
        for (int u = 0; u < ucount; u++) {
            for (int v = 0; v < vcount; v++) {
                int addr = vinfo.getAddress(mem, u * vcount + v);
                VertexState vs = vinfo.readVertex(mem, addr);
                if (log.isDebugEnabled()) {
                    log("drawBezier  vertex#" + u + "," + v + " (" + ((float) vs.t[0]) + "," + ((float) vs.t[1]) + ") at (" + ((float) vs.p[0]) + "," + ((float) vs.p[1]) + "," + ((int) vs.p[2]) + ")");
                }
                anchors[u][v] = vs;
            }
        }

        // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
        // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
        if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
            log.info("Capture drawBezier");
            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * ucount * vcount);
        }

        int udivs = patch_div_s;
        int vdivs = patch_div_t;

        //
        // Based on
        //    http://nehe.gamedev.net/data/lessons/lesson.asp?lesson=28
        //
        // TODO Check this behavior:
        //  - X & Y coordinates seems to be swapped. Currently only if vinfo.texture != 0, is this correct?
        //  - (Y,X) has to be mapped to (1-Y,1-X) so that the texture is displayed like on PSP
        //
        VertexState[] temp = new VertexState[ucount];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = new VertexState();
        }
        VertexState[] last = new VertexState[vdivs + 1];
        for (int i = 0; i < last.length; i++) {
            last[i] = new VertexState();
        }

        // The First Derived Curve (Along X-Axis)
        for (int u = 0; u < ucount; u++) {
            pointCopy(temp[u], anchors[u][vcount - 1]);
        }

        // Create The First Line Of Points
        for (int v = 0; v <= vdivs; v++) {
            // Percent Along Y-Axis
            float px = ((float) v) / ((float) vdivs);
            // Use The 4 Points From The Derived Curve To Calculate The Points Along That Curve
            Bernstein(last[v], px, temp);
        }

        boolean useTexture = true;
        bindBuffers(useVertexColor, useTexture);

        float pyold = 0;
        for (int u = 1; u <= udivs; u++) {
            // Percent Along Y-Axis
            float py = ((float) u) / ((float) udivs);

            // Calculate New Bezier Points
            for (int i = 0; i < ucount; i++) {
                 Bernstein(temp[i], py, anchors[i]);
            }

            vboFloatBuffer.clear();

            for (int v = 0; v <= vdivs; v++) {
                // Percent Along The X-Axis
                float px = ((float) v) / ((float) vdivs);

                // Apply The Old Texture Coords
                if (vinfo.texture != 0) {
                    vboFloatBuffer.put(last[v].t);
                } else {
                    vboFloatBuffer.put(1 - pyold);
                    vboFloatBuffer.put(1 - px);
                }
                // Old Point
                vboFloatBuffer.put(last[v].p);

                // Generate New Point
                Bernstein(last[v], px, temp);

                // Apply The New Texture Coords
                if (vinfo.texture != 0) {
                    vboFloatBuffer.put(last[v].t);
                } else {
                    vboFloatBuffer.put(1 - py);
                    vboFloatBuffer.put(1 - px);
                }
                // New Point
                vboFloatBuffer.put(last[v].p);
            }

            if(useVBO) {
                if (openGL1_5)
                    gl.glBufferData(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
                else
                    gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vboFloatBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboFloatBuffer.rewind(), GL.GL_STREAM_DRAW);
            }
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, (vdivs + 1) * 2);

            pyold = py;
        }

        endRendering(useVertexColor, useTexture);
    }

    private void pointAdd(VertexState result, VertexState p, VertexState q) {
        result.p[0] = p.p[0] + q.p[0];
        result.p[1] = p.p[1] + q.p[1];
        result.p[2] = p.p[2] + q.p[2];
        if (vinfo.texture != 0) {
            result.t[0] = p.t[0] + q.t[0];
            result.t[1] = p.t[1] + q.t[1];
        }
    }

    private void pointTimes(VertexState result, float c, VertexState p) {
        result.p[0] = p.p[0] * c;
        result.p[1] = p.p[1] * c;
        result.p[2] = p.p[2] * c;
        if (vinfo.texture != 0) {
            result.t[0] = p.t[0] * c;
            result.t[1] = p.t[1] * c;
        }
    }

    private void pointCopy(VertexState result, VertexState p) {
        result.p[0] = p.p[0];
        result.p[1] = p.p[1];
        result.p[2] = p.p[2];
        if (vinfo.texture != 0) {
            result.t[0] = p.t[0];
            result.t[1] = p.t[1];
        }
    }

    // Temporary variables for Bernstein()
    private VertexState bernsteinA = new VertexState();
    private VertexState bernsteinB = new VertexState();
    private VertexState bernsteinC = new VertexState();
    private VertexState bernsteinD = new VertexState();

    private void Bernstein(VertexState result, float u, VertexState[] p) {
        float uPow2 = u * u;
        float uPow3 = uPow2 * u;
        float u1 = 1 - u;
        float u1Pow2 = u1 * u1;
        float u1Pow3 = u1Pow2 * u1;
        pointTimes(bernsteinA, uPow3,      p[0]);
        pointTimes(bernsteinB, 3*uPow2*u1, p[1]);
        pointTimes(bernsteinC, 3*u*u1Pow2, p[2]);
        pointTimes(bernsteinD, u1Pow3,     p[3]);

        pointAdd(bernsteinA, bernsteinA, bernsteinB);
        pointAdd(bernsteinC, bernsteinC, bernsteinD);
        pointAdd(result, bernsteinA, bernsteinC);
    }

    private Buffer getTexture32BitBuffer(int texaddr, int level) {
        Buffer final_buffer = null;

        if (!texture_swizzle) {
        	// texture_width might be larger than texture_buffer_width 
            int bufferlen = Math.max(texture_buffer_width[level], texture_width[level]) * texture_height[level] * 4;
            final_buffer = Memory.getInstance().getBuffer(texaddr, bufferlen);
            if (final_buffer == null) {
            	int length = texture_buffer_width[level]*texture_height[level];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * 4, 4);
                for (int i = 0; i < length; i++) {
                    tmp_texture_buffer32[i] = memoryReader.readNext();
                }
                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
            }

            if (State.captureGeNextFrame) {
                log.info("Capture getTexture32BitBuffer unswizzled");
                CaptureManager.captureRAM(texaddr, bufferlen);
            }
        } else {
            final_buffer = unswizzleTextureFromMemory(texaddr, 4, level);
        }

        return final_buffer;
    }

    public final static String getPsmName(final int psm) {
        return (psm >= 0 && psm < psm_names.length)
            ? psm_names[psm % psm_names.length]
            : "PSM_UNKNOWN" + psm;
    }
    public final static String getLOpName(final int ops) {
        return (ops >= 0 && ops < logical_ops_names.length)
            ? logical_ops_names[ops % logical_ops_names.length]
            : "UNKNOWN_LOP" + ops;
    }

    private int getCompressedTextureSize(int level, int compressionRatio) {
    	return getCompressedTextureSize(texture_buffer_width[level], texture_height[level], compressionRatio);
    }

    public static int getCompressedTextureSize(int width, int height, int compressionRatio) {
    	int compressedTextureWidth = ((width + 3) / 4) * 4;
    	int compressedTextureHeight = ((height + 3) / 4) * 4;
        int compressedTextureSize = compressedTextureWidth * compressedTextureHeight * 4 / compressionRatio;

        return compressedTextureSize;
    }


    private void glBindBuffer() {
    	glBindBuffer(vboBufferId[0]);
    }

    private void glBindBuffer(int bufferId) {
        if (useVBO) {
        	if (openGL1_5) {
        		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        	} else {
        		gl.glBindBufferARB(GL.GL_ARRAY_BUFFER, bufferId);
        	}
        }
    }

    private void updateGeBuf() {
    	if (geBufChanged) {
    		pspdisplay.getInstance().hleDisplaySetGeBuf(gl, fbp, fbw, psm, somethingDisplayed);
    		geBufChanged = false;
    	}
    }
    // For capture/replay

    public int getFBP() { return fbp; }
    public int getFBW() { return fbw; }
    public int getZBP() { return zbp; }
    public int getZBW() { return zbw; }
    public int getPSM() { return psm; }

    private boolean isVertexBufferEmbedded() {
        // stall_addr may be 0
        return (vinfo.ptr_vertex >= currentList.list_addr && vinfo.ptr_vertex < currentList.stall_addr);
    }

    private boolean isVRAM(int addr) {
    	addr &= Memory.addressMask;

    	return addr >= MemoryMap.START_VRAM && addr <= MemoryMap.END_VRAM;    	
    }

    public void saveContext(pspGeContext context) {
    	context.fbp = fbp;
    	context.fbw = fbw;
    	context.zbp = zbp;
    	context.zbw = zbw;
    	context.psm = psm;

    	context.region_x1 = region_x1;
    	context.region_y1 = region_y1;
    	context.region_x2 = region_x2;
    	context.region_y2 = region_y2;
    	context.region_width = region_width;
    	context.region_height = region_height;
    	context.scissor_x1 = scissor_x1;
    	context.scissor_y1 = scissor_y1;
    	context.scissor_x2 = scissor_x2;
    	context.scissor_y2 = scissor_y2;
    	context.scissor_width = scissor_width;
    	context.scissor_height = scissor_height;
    	context.offset_x = offset_x;
    	context.offset_y = offset_y;
    	context.viewport_width = viewport_width;
    	context.viewport_height = viewport_height;
    	context.viewport_cx = viewport_cx;
    	context.viewport_cy = viewport_cy;

    	System.arraycopy(proj_uploaded_matrix, 0, context.proj_uploaded_matrix, 0, proj_uploaded_matrix.length);
    	System.arraycopy(texture_uploaded_matrix, 0, context.texture_uploaded_matrix, 0, texture_uploaded_matrix.length);
    	System.arraycopy(model_uploaded_matrix, 0, context.model_uploaded_matrix, 0, model_uploaded_matrix.length);
    	System.arraycopy(view_uploaded_matrix, 0, context.view_uploaded_matrix, 0, view_uploaded_matrix.length);
    	System.arraycopy(morph_weight, 0, context.morph_weight, 0, morph_weight.length);
    	System.arraycopy(tex_envmap_matrix, 0, context.tex_envmap_matrix, 0, tex_envmap_matrix.length);
    	if (pspGeContext.fullVersion) {
	    	for (int i = 0; i < bone_uploaded_matrix.length; i++) {
	    		System.arraycopy(bone_uploaded_matrix[i], 0, context.bone_uploaded_matrix[i], 0, bone_uploaded_matrix[i].length);
	    	}
    	}
    	for (int i = 0; i < light_pos.length; i++) {
    		System.arraycopy(light_pos[i], 0, context.light_pos[i], 0, light_pos[i].length);
    		System.arraycopy(light_dir[i], 0, context.light_dir[i], 0, light_dir[i].length);
    	}

    	System.arraycopy(light_enabled, 0, context.light_enabled, 0, light_enabled.length);
    	System.arraycopy(light_type, 0, context.light_type, 0, light_type.length);
    	System.arraycopy(light_kind, 0, context.light_kind, 0, light_kind.length);
    	context.lighting = lighting;
    	System.arraycopy(spotLightExponent, 0, context.spotLightExponent, 0, spotLightExponent.length);
    	System.arraycopy(spotLightCutoff, 0, context.spotLightCutoff, 0, spotLightCutoff.length);

    	System.arraycopy(fog_color, 0, context.fog_color, 0, fog_color.length);
    	context.fog_far = fog_far;
    	context.fog_dist = fog_dist;

    	context.nearZ = nearZ;
    	context.farZ = farZ;
    	context.zscale = zscale;
    	context.zpos = zpos;

    	context.mat_flags = mat_flags;
    	System.arraycopy(mat_ambient, 0, context.mat_ambient, 0, mat_ambient.length);
    	System.arraycopy(mat_diffuse, 0, context.mat_diffuse, 0, mat_diffuse.length);
    	System.arraycopy(mat_specular, 0, context.mat_specular, 0, mat_specular.length);
    	System.arraycopy(mat_emissive, 0, context.mat_emissive, 0, mat_emissive.length);

    	System.arraycopy(ambient_light, 0, context.ambient_light, 0, ambient_light.length);

    	context.texture_storage = texture_storage;
    	context.texture_num_mip_maps = texture_num_mip_maps;
    	context.texture_swizzle = texture_swizzle;

    	System.arraycopy(texture_base_pointer, 0, context.texture_base_pointer, 0, texture_base_pointer.length);
    	System.arraycopy(texture_width, 0, context.texture_width, 0, texture_width.length);
    	System.arraycopy(texture_height, 0, context.texture_height, 0, texture_height.length);
    	System.arraycopy(texture_buffer_width, 0, context.texture_buffer_width, 0, texture_buffer_width.length);
    	context.tex_min_filter = tex_min_filter;
    	context.tex_mag_filter = tex_mag_filter;

    	context.tex_translate_x = tex_translate_x;
    	context.tex_translate_y = tex_translate_y;
    	context.tex_scale_x = tex_scale_x;
    	context.tex_scale_y = tex_scale_y;
    	System.arraycopy(tex_env_color, 0, context.tex_env_color, 0, tex_env_color.length);
    	context.tex_enable = tex_enable;

        context.tex_clut_addr = tex_clut_addr;
        context.tex_clut_num_blocks = tex_clut_num_blocks;
        context.tex_clut_mode = tex_clut_mode;
		context.tex_clut_shift = tex_clut_shift;
		context.tex_clut_mask = tex_clut_mask;
		context.tex_clut_start = tex_clut_start;
        context.tex_wrap_s = tex_wrap_s;
		context.tex_wrap_t = tex_wrap_t;
        context.patch_div_s = patch_div_s;
        context.patch_div_t = patch_div_t;

        context.transform_mode = transform_mode;

        context.textureTx_sourceAddress = textureTx_sourceAddress;
        context.textureTx_sourceLineWidth = textureTx_sourceLineWidth;
        context.textureTx_destinationAddress = textureTx_destinationAddress;
        context.textureTx_destinationLineWidth = textureTx_destinationLineWidth;
        context.textureTx_width = textureTx_width;
        context.textureTx_height = textureTx_height;
        context.textureTx_sx = textureTx_sx;
        context.textureTx_sy = textureTx_sy;
        context.textureTx_dx = textureTx_dx;
        context.textureTx_dy = textureTx_dy;
        context.textureTx_pixelSize = textureTx_pixelSize;

    	System.arraycopy(dfix_color, 0, context.dfix_color, 0, dfix_color.length);
    	System.arraycopy(sfix_color, 0, context.sfix_color, 0, sfix_color.length);
        context.blend_src = blend_src;
        context.blend_dst = blend_dst;

        context.clearMode = clearMode;
        context.depthFuncClearMode = clearModeDepthFunc;

        context.depthFunc2D = depthFunc2D;
        context.depthFunc3D = depthFunc3D;

        context.tex_map_mode = tex_map_mode;
        context.tex_proj_map_mode = tex_proj_map_mode;

        System.arraycopy(glColorMask, 0, context.glColorMask, 0, glColorMask.length);

        context.copyGLToContext(gl);
    }

    public void restoreContext(pspGeContext context) {
    	fbp = context.fbp;
    	fbw = context.fbw;
    	zbp = context.zbp;
    	zbw = context.zbw;
    	psm = context.psm;

    	region_x1 = context.region_x1;
    	region_y1 = context.region_y1;
    	region_x2 = context.region_x2;
    	region_y2 = context.region_y2;
    	region_width = context.region_width;
    	region_height = context.region_height;
    	scissor_x1 = context.scissor_x1;
    	scissor_y1 = context.scissor_y1;
    	scissor_x2 = context.scissor_x2;
    	scissor_y2 = context.scissor_y2;
    	scissor_width = context.scissor_width;
    	scissor_height = context.scissor_height;
    	offset_x = context.offset_x;
    	offset_y = context.offset_y;
    	viewport_width = context.viewport_width;
    	viewport_height = context.viewport_height;
    	viewport_cx = context.viewport_cx;
    	viewport_cy = context.viewport_cy;

    	System.arraycopy(context.proj_uploaded_matrix, 0, proj_uploaded_matrix, 0, proj_uploaded_matrix.length);
    	System.arraycopy(context.texture_uploaded_matrix, 0, texture_uploaded_matrix, 0, texture_uploaded_matrix.length);
    	System.arraycopy(context.model_uploaded_matrix, 0, model_uploaded_matrix, 0, model_uploaded_matrix.length);
    	System.arraycopy(context.view_uploaded_matrix, 0, view_uploaded_matrix, 0, view_uploaded_matrix.length);
    	System.arraycopy(context.morph_weight, 0, morph_weight, 0, morph_weight.length);
    	System.arraycopy(context.tex_envmap_matrix, 0, tex_envmap_matrix, 0, tex_envmap_matrix.length);
    	if (pspGeContext.fullVersion) {
	    	for (int i = 0; i < bone_uploaded_matrix.length; i++) {
	    		System.arraycopy(context.bone_uploaded_matrix[i], 0, bone_uploaded_matrix[i], 0, bone_uploaded_matrix[i].length);
	    	}
    	}
    	for (int i = 0; i < light_pos.length; i++) {
    		System.arraycopy(context.light_pos[i], 0, light_pos[i], 0, light_pos[i].length);
    		System.arraycopy(context.light_dir[i], 0, light_dir[i], 0, light_dir[i].length);
    	}

    	System.arraycopy(context.light_enabled, 0, light_enabled, 0, light_enabled.length);
    	System.arraycopy(context.light_type, 0, light_type, 0, light_type.length);
    	System.arraycopy(context.light_kind, 0, light_kind, 0, light_kind.length);
    	lighting = context.lighting;
    	System.arraycopy(context.spotLightExponent, 0, spotLightExponent, 0, spotLightExponent.length);
    	System.arraycopy(context.spotLightCutoff, 0, spotLightCutoff, 0, spotLightCutoff.length);

    	System.arraycopy(context.fog_color, 0, fog_color, 0, fog_color.length);
    	fog_far = context.fog_far;
    	fog_dist = context.fog_dist;

    	nearZ = context.nearZ;
    	farZ = context.farZ;
    	zscale = context.zscale;
    	zpos = context.zpos;

    	mat_flags = context.mat_flags;
    	System.arraycopy(context.mat_ambient, 0, mat_ambient, 0, mat_ambient.length);
    	System.arraycopy(context.mat_diffuse, 0, mat_diffuse, 0, mat_diffuse.length);
    	System.arraycopy(context.mat_specular, 0, mat_specular, 0, mat_specular.length);
    	System.arraycopy(context.mat_emissive, 0, mat_emissive, 0, mat_emissive.length);

    	System.arraycopy(context.ambient_light, 0, ambient_light, 0, ambient_light.length);

    	texture_storage = context.texture_storage;
    	texture_num_mip_maps = context.texture_num_mip_maps;
    	texture_swizzle = context.texture_swizzle;

    	System.arraycopy(context.texture_base_pointer, 0, texture_base_pointer, 0, texture_base_pointer.length);
    	System.arraycopy(context.texture_width, 0, texture_width, 0, texture_width.length);
    	System.arraycopy(context.texture_height, 0, texture_height, 0, texture_height.length);
    	System.arraycopy(context.texture_buffer_width, 0, texture_buffer_width, 0, texture_buffer_width.length);
    	tex_min_filter = context.tex_min_filter;
    	tex_mag_filter = context.tex_mag_filter;

    	tex_translate_x = context.tex_translate_x;
    	tex_translate_y = context.tex_translate_y;
    	tex_scale_x = context.tex_scale_x;
    	tex_scale_y = context.tex_scale_y;
    	System.arraycopy(context.tex_env_color, 0, tex_env_color, 0, tex_env_color.length);
    	tex_enable = context.tex_enable;

        tex_clut_addr = context.tex_clut_addr;
        tex_clut_num_blocks = context.tex_clut_num_blocks;
        tex_clut_mode = context.tex_clut_mode;
		tex_clut_shift = context.tex_clut_shift;
		tex_clut_mask = context.tex_clut_mask;
		tex_clut_start = context.tex_clut_start;
        tex_wrap_s = context.tex_wrap_s;
		tex_wrap_t = context.tex_wrap_t;
        patch_div_s = context.patch_div_s;
        patch_div_t = context.patch_div_t;

        transform_mode = context.transform_mode;

        textureTx_sourceAddress = context.textureTx_sourceAddress;
        textureTx_sourceLineWidth = context.textureTx_sourceLineWidth;
        textureTx_destinationAddress = context.textureTx_destinationAddress;
        textureTx_destinationLineWidth = context.textureTx_destinationLineWidth;
        textureTx_width = context.textureTx_width;
        textureTx_height = context.textureTx_height;
        textureTx_sx = context.textureTx_sx;
        textureTx_sy = context.textureTx_sy;
        textureTx_dx = context.textureTx_dx;
        textureTx_dy = context.textureTx_dy;
        textureTx_pixelSize = context.textureTx_pixelSize;

    	System.arraycopy(context.dfix_color, 0, dfix_color, 0, dfix_color.length);
    	System.arraycopy(context.sfix_color, 0, sfix_color, 0, sfix_color.length);
        blend_src = context.blend_src;
        blend_dst = context.blend_dst;

        clearMode = context.clearMode;
        clearModeDepthFunc = context.depthFuncClearMode;

        depthFunc2D = context.depthFunc2D;
        depthFunc3D = context.depthFunc3D;

        tex_map_mode = context.tex_map_mode;
        tex_proj_map_mode = context.tex_proj_map_mode;

        System.arraycopy(context.glColorMask, 0, glColorMask, 0, glColorMask.length);

        context.copyContextToGL(gl);
    }
}

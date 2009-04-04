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
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.pspge;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;
import static jpcsp.HLE.pspge.*;

import org.apache.log4j.Logger;

import com.sun.opengl.util.BufferUtil;

public class VideoEngine {
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

    private static VideoEngine instance;
    private GL gl;
    private GLU glu;
    public static Logger log = Logger.getLogger("ge");
    public static final boolean useTextureCache = true;
    private static GeCommands helper;
    private VertexInfo vinfo = new VertexInfo();
    private static final char SPACE = ' ';
    private DurationStatistics statistics;
    private DurationStatistics[] commandStatistics;

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

    private boolean proj_upload_start;
    private int proj_upload_x;
    private int proj_upload_y;
    private float[] proj_matrix = new float[4 * 4];
    private float[] proj_uploaded_matrix = new float[4 * 4];

    private boolean texture_upload_start;
    private int texture_upload_x;
    private int texture_upload_y;
    private float[] texture_matrix = new float[4 * 4];
    private float[] texture_uploaded_matrix = new float[4 * 4];

    private boolean model_upload_start;
    private int     model_upload_x;
    private int     model_upload_y;
    private float[] model_matrix = new float[4 * 4];
    private float[] model_uploaded_matrix = new float[4 * 4];

    private boolean view_upload_start;
    private int view_upload_x;
    private int view_upload_y;
    private float[] view_matrix = new float[4 * 4];
    private float[] view_uploaded_matrix = new float[4 * 4];

    private boolean bone_upload_start;
    private int bone_upload_x;
    private int bone_upload_y;
    private int bone_matrix_offset;
    private float[] bone_matrix = new float[4 * 3];
    private float[][] bone_uploaded_matrix = new float[8][4 * 3];

    private float[] morph_weight = new float[8];

    private float[] tex_envmap_matrix = new float[4*4];

    private float[][] light_pos = new float[4][4];
    private float[][] light_dir = new float[4][3];

    private int[] light_enabled = new int[4];
    private int[] light_type = new int[4];
    private int[] light_kind = new int[4];
    private boolean lighting = false;

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
    private int depthFuncClearMode;

    private int depthFunc2D;
    private int depthFunc3D;

    // opengl needed information/buffers
    private int[] gl_texture_id = new int[1];
    private int[] tmp_texture_buffer32 = new int[1024*1024];
    private short[] tmp_texture_buffer16 = new short[1024*1024];
    private int[] tmp_clut_buffer32 = new int[4096];
    private short[] tmp_clut_buffer16 = new short[4096];
    private int tex_map_mode = TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;

    private boolean listHasEnded;
    //private DisplayList currentList; // The currently executing list
    private PspGeList currentList; // The currently executing list
    private boolean useVBO = true;
    private int[] vboBufferId = new int[1];
    private static final int vboBufferSize = 1024 * 1024;
    private FloatBuffer vboBuffer = BufferUtil.newFloatBuffer(vboBufferSize);
    private boolean useShaders = true;
    private int shaderProgram;

    private ConcurrentLinkedQueue<PspGeList> drawListQueue;
    private boolean somethingDisplayed;

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
        model_matrix[0] = model_matrix[5] = model_matrix[10] = model_matrix[15] = 1.f;
        view_matrix[0] = view_matrix[5] = view_matrix[10] = view_matrix[15] = 1.f;
        tex_envmap_matrix[0] = tex_envmap_matrix[5] = tex_envmap_matrix[10] = tex_envmap_matrix[15] = 1.f;
        light_pos[0][3] = light_pos[1][3] = light_pos[2][3] = light_pos[3][3] = 1.f;

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

        useVBO = !Settings.getInstance().readBool("emu.disablevbo") && gl.isFunctionAvailable("glGenBuffersARB") &&
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
        gl.glGenBuffers(1, vboBufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboBufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBufferSize *
                BufferUtil.SIZEOF_FLOAT, vboBuffer, GL.GL_STREAM_DRAW);
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
        boolean updated = false;

        PspGeList list = drawListQueue.poll();
        if (list == null)
            return false;

        if (useShaders)
        	gl.glUseProgram(shaderProgram);

        statistics.start();
        TextureCache.getInstance().resetTextureAlreadyHashed();
        somethingDisplayed = false;

        do {
            executeList(list);
            list = drawListQueue.poll();
        } while(list != null);
        updated = true;

        /* old
        DisplayList.Lock();
        Iterator<DisplayList> it = DisplayList.iterator();
        while(it.hasNext() && !Emulator.pause) {
            DisplayList list = it.next();
            if (list.status == DisplayList.QUEUED && list.HasFinish()) {
                executeList(list);

                if (list.status == DisplayList.DRAWING_DONE) {
                    updated = true;
                } else if (list.status == DisplayList.DONE) {
                    it.remove();
                    updated = true;
                }
            }
        }
        DisplayList.Unlock();
        */

        if (useShaders)
        	gl.glUseProgram(0);

        /* old
        if (updated)
            jpcsp.HLE.pspge.getInstance().syncDone = true;
        */

        statistics.end();

        return updated;
    }

    // call from GL thread
    // There is an issue here with Emulator.pause
    // - We want to stop on errors
    // - But user may also press pause button
    //   - Either continue drawing to the end of the list (bad if the list contains an infinite loop)
    //   - Or we want to be able to restart drawing when the user presses the run button
    //private void executeList(DisplayList list) {
    private void executeList(PspGeList list) {
        currentList = list;
        listHasEnded = false;

        if (log.isDebugEnabled()) {
            log("executeList id=" + list.id);
        }

        Memory mem = Memory.getInstance();
        while (!listHasEnded &&
                currentList.pc != currentList.stall_addr
                && !Emulator.pause) {
            int ins = mem.read32(currentList.pc);
            currentList.pc += 4;
            executeCommand(ins);
        }

        if (currentList.pc == currentList.stall_addr) {
            //currentList.status = DisplayList.STALL_REACHED;
            currentList.currentStatus = PSP_GE_LIST_STALL_REACHED;
            if (log.isDebugEnabled()) {
                log("list id=" + currentList.id + " stalled at " + String.format("%08x", currentList.stall_addr) + " listHasEnded=" + listHasEnded);
            }
        }

        if (Emulator.pause && !listHasEnded) {
            VideoEngine.log.info("Emulator paused - cancelling current list id=" + currentList.id);
            currentList.currentStatus = PSP_GE_LIST_CANCEL_DONE;
        }

        // takes priority over PSP_GE_LIST_STALL_REACHED
        if (listHasEnded) {
            currentList.currentStatus = PSP_GE_LIST_DONE;
        }

        if (list.currentStatus == list.syncStatus ||
            list.currentStatus == PSP_GE_LIST_DONE ||
            list.currentStatus == PSP_GE_LIST_STALL_REACHED ||
            list.currentStatus == PSP_GE_LIST_CANCEL_DONE) {
            pspge.getInstance().hleGeListSyncDone(list);
        }
    }

    private static int command(int instruction) {
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
            VideoEngine.log.error("Unhandled alpha blend src used " + blend_src);
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
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
            VideoEngine.log.error("Unhandled alpha blend dst used " + blend_dst);
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
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

    private short[] readClut16() {
    	Memory mem = Memory.getInstance();
		int clutNumEntries = tex_clut_num_blocks * 16;
		for (int i = tex_clut_start; i < clutNumEntries; i += 2) {
			int n = mem.read32(tex_clut_addr + i * 2);
			tmp_clut_buffer16[i    ] = (short)  n;
			tmp_clut_buffer16[i + 1] = (short) (n >> 16);
		}

    	return tmp_clut_buffer16;
    }

    private int[] readClut32() {
    	Memory mem = Memory.getInstance();
		int clutNumEntries = tex_clut_num_blocks * 8;
		for (int i = tex_clut_start; i < clutNumEntries; i++) {
			tmp_clut_buffer32[i] = mem.read32(tex_clut_addr + i * 4);
		}

    	return tmp_clut_buffer32;
    }

    private int getClutIndex(int index) {
        return ((tex_clut_start + index) >> tex_clut_shift) & tex_clut_mask;
    }

    // UnSwizzling based on pspplayer
    private Buffer unswizzleTextureFromMemory(int texaddr, int bytesPerPixel, int level) {
        Memory mem = Memory.getInstance();
        int rowWidth = (bytesPerPixel > 0) ? (texture_buffer_width[level] * bytesPerPixel) : (texture_buffer_width[level] / 2);
        int pitch = ( rowWidth - 16 ) / 4;
        int bxc = rowWidth / 16;
        int byc = texture_height[level] / 8;

        int src = texaddr, ydest = 0;

        for( int by = 0; by < byc; by++ )
        {
            int xdest = ydest;
            for( int bx = 0; bx < bxc; bx++ )
            {
                int dest = xdest;
                for( int n = 0; n < 8; n++ )
                {
                    tmp_texture_buffer32[dest] = mem.read32(src);
                    tmp_texture_buffer32[dest+1] = mem.read32(src + 4);
                    tmp_texture_buffer32[dest+2] = mem.read32(src + 8);
                    tmp_texture_buffer32[dest+3] = mem.read32(src + 12);

                    src     += 4*4;
                    dest    += pitch+4;
                }
                xdest += (16/4);
            }
            ydest += (rowWidth * 8)/4;
        }

        return IntBuffer.wrap(tmp_texture_buffer32);
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
                    log(helper.getCommandString(END));
                }
                break;

            case FINISH:
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(FINISH) + " (hex="+Integer.toHexString(normalArgument)+",int="+normalArgument+",float="+floatArgument+")");
                }
                currentList.pushFinishCallback(normalArgument);
                break;

            case SIGNAL:
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(SIGNAL) + " (hex="+Integer.toHexString(normalArgument)+",int="+normalArgument+",float="+floatArgument+")");
                }
                currentList.pushSignalCallback(normalArgument);
                break;

            case BASE:
                currentList.base = normalArgument << 8;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(BASE) + " " + String.format("%08x", currentList.base));
                }
                if ((currentList.base & 0x00FFFFFF) != 0) {
                    log.warn(helper.getCommandString(BASE) + " has lower bits set " + String.format("%08x", currentList.base));
                }
                break;

            case IADDR:
                vinfo.ptr_index = currentList.base | normalArgument;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(IADDR) + " " + String.format("%08x", vinfo.ptr_index));
                }
                break;

            case VADDR:
                vinfo.ptr_vertex = currentList.base | normalArgument;
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
                view_upload_start = true;
                log("sceGumMatrixMode GU_VIEW");
                break;

            case VIEW:
                if (view_upload_start) {
                    view_upload_x = 0;
                    view_upload_y = 0;
                    view_upload_start = false;
                }

                if (view_upload_y < 4) {
                    if (view_upload_x < 3) {
                    	view_matrix[view_upload_x + view_upload_y * 4] = floatArgument;

                    	view_upload_x++;
                        if (view_upload_x == 3) {
                        	view_matrix[view_upload_x + view_upload_y * 4] = (view_upload_y == 3) ? 1.0f : 0.0f;
                        	view_upload_x = 0;
                        	view_upload_y++;
                            if (view_upload_y == 4) {
                                log("glLoadMatrixf", view_matrix);

                                for (int i = 0; i < 4*4; i++)
                                	view_uploaded_matrix[i] = view_matrix[i];
                            }
                        }
                    }
                }
                break;

            case MMS:
                model_upload_start = true;
                log("sceGumMatrixMode GU_MODEL");
                break;

            case MODEL:
                if (model_upload_start) {
                    model_upload_x = 0;
                    model_upload_y = 0;
                    model_upload_start = false;
                }

                if (model_upload_y < 4) {
                    if (model_upload_x < 3) {
                        model_matrix[model_upload_x + model_upload_y * 4] = floatArgument;

                        model_upload_x++;
                        if (model_upload_x == 3) {
                            model_matrix[model_upload_x + model_upload_y * 4] = (model_upload_y == 3) ? 1.0f : 0.0f;
                            model_upload_x = 0;
                            model_upload_y++;
                            if (model_upload_y == 4) {
                                log("glLoadMatrixf", model_matrix);

                                for (int i = 0; i < 4*4; i++)
                                	model_uploaded_matrix[i] = model_matrix[i];
                            }
                        }
                    }
                }
                break;

            /*
             *  Light 0 attributes
             */

            // Position
            case LXP0:
            	light_pos[0][0] = floatArgument;
            	break;
            case LYP0:
            	light_pos[0][1] = floatArgument;
            	break;
            case LZP0:
            	light_pos[0][2] = floatArgument;
            	break;

            // Color
            case ALC0: {
            	float [] color = new float[4];


            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, color, 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_AMBIENT)");
            	break;
            }

            case DLC0: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, color, 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_DIFFUSE)");
            	break;
            }

            case SLC0: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, color, 0);
            	log("sceGuLightColor (GU_LIGHT0, GU_SPECULAR)");
            	break;
            }

            // Attenuation
            case LCA0:
            	gl.glLightf(GL.GL_LIGHT0, GL.GL_CONSTANT_ATTENUATION, floatArgument);
            	break;

            case LLA0:
            	gl.glLightf(GL.GL_LIGHT0, GL.GL_LINEAR_ATTENUATION, floatArgument);
            	break;

            case LQA0:
            	gl.glLightf(GL.GL_LIGHT0, GL.GL_QUADRATIC_ATTENUATION, floatArgument);
            	break;

        	/*
             *  Light 1 attributes
             */

            // Position
            case LXP1:
            	light_pos[1][0] = floatArgument;
            	break;
            case LYP1:
            	light_pos[1][1] = floatArgument;
            	break;
            case LZP1:
            	light_pos[1][2] = floatArgument;
            	break;

            // Color
            case ALC1: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, color, 0);
            	log("sceGuLightColor (GU_LIGHT1, GU_AMBIENT)");
            	break;
            }

            case DLC1: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, color, 0);
            	log("sceGuLightColor (GU_LIGHT1, GU_DIFFUSE)");
            	break;
            }

            case SLC1: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, color, 0);
            	log("sceGuLightColor (GU_LIGHT1, GU_SPECULAR)");
            	break;
            }

            // Attenuation
            case LCA1:
            	gl.glLightf(GL.GL_LIGHT1, GL.GL_CONSTANT_ATTENUATION, floatArgument);
            	break;

            case LLA1:
            	gl.glLightf(GL.GL_LIGHT1, GL.GL_LINEAR_ATTENUATION, floatArgument);
            	break;

            case LQA1:
            	gl.glLightf(GL.GL_LIGHT1, GL.GL_QUADRATIC_ATTENUATION, floatArgument);
            	break;

        	/*
             *  Light 2 attributes
             */

            // Position
            case LXP2:
            	light_pos[2][0] = floatArgument;
            	break;
            case LYP2:
            	light_pos[2][1] = floatArgument;
            	break;
            case LZP2:
            	light_pos[2][2] = floatArgument;
            	break;

            // Color
            case ALC2: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT2, GL.GL_AMBIENT, color, 0);
            	log("sceGuLightColor (GU_LIGHT2, GU_AMBIENT)");
            	break;
            }

            case DLC2: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT2, GL.GL_DIFFUSE, color, 0);
            	log("sceGuLightColor (GU_LIGHT2, GU_DIFFUSE)");
            	break;
            }

            case SLC2: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT2, GL.GL_SPECULAR, color, 0);
            	log("sceGuLightColor (GU_LIGHT2, GU_SPECULAR)");
            	break;
            }

            // Attenuation
            case LCA2:
            	gl.glLightf(GL.GL_LIGHT2, GL.GL_CONSTANT_ATTENUATION, floatArgument);
            	break;

            case LLA2:
            	gl.glLightf(GL.GL_LIGHT2, GL.GL_LINEAR_ATTENUATION, floatArgument);
            	break;

            case LQA2:
            	gl.glLightf(GL.GL_LIGHT2, GL.GL_QUADRATIC_ATTENUATION, floatArgument);
            	break;

        	/*
             *  Light 3 attributes
             */

            // Position
            case LXP3:
            	light_pos[3][0] = floatArgument;
            	break;
            case LYP3:
            	light_pos[3][1] = floatArgument;
            	break;
            case LZP3:
            	light_pos[3][2] = floatArgument;
            	break;

            // Color
            case ALC3: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT3, GL.GL_AMBIENT, color, 0);
            	log("sceGuLightColor (GU_LIGHT3, GU_AMBIENT)");
            	break;
            }

            case DLC3: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT3, GL.GL_DIFFUSE, color, 0);
            	log("sceGuLightColor (GU_LIGHT3, GU_DIFFUSE)");
            	break;
            }

            case SLC3: {
            	float [] color = new float[4];

            	color[0] = ((normalArgument      ) & 255) / 255.f;
            	color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	color[3] = 1.f;

            	gl.glLightfv(GL.GL_LIGHT3, GL.GL_SPECULAR, color, 0);
            	log("sceGuLightColor (GU_LIGHT3, GU_SPECULAR)");
            	break;
            }

            // Attenuation
            case LCA3:
            	gl.glLightf(GL.GL_LIGHT3, GL.GL_CONSTANT_ATTENUATION, floatArgument);
            	break;

            case LLA3:
            	gl.glLightf(GL.GL_LIGHT3, GL.GL_LINEAR_ATTENUATION, floatArgument);
            	break;

            case LQA3:
            	gl.glLightf(GL.GL_LIGHT3, GL.GL_QUADRATIC_ATTENUATION, floatArgument);
            	break;

            case LMODE: {
                int lightmode = (normalArgument != 0) ? GL.GL_SEPARATE_SPECULAR_COLOR : GL.GL_SINGLE_COLOR;
                gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, lightmode);
                if (log.isDebugEnabled()) {
                    VideoEngine.log.info("sceGuLightMode(" + ((normalArgument != 0) ? "GU_SEPARATE_SPECULAR_COLOR" : "GU_SINGLE_COLOR") + ")");
                }
                break;
            }

            case LXD0: case LXD1: case LXD2: case LXD3:
            case LYD0: case LYD1: case LYD2: case LYD3:
            case LZD0: case LZD1: case LZD2: case LZD3: {
                int lnum = (command - LXD0) / 3;
                int dircomponent = (command - LXD0) % 3;
                light_dir[lnum][dircomponent] = floatArgument;

                if ((command == LZD0 || command == LZD1 ||
                     command == LZD2 || command == LZD3) &&
                    light_type[lnum] == LIGHT_DIRECTIONAL) {
                    // TODO any other gl command required to set light type to spot light?
                    // TODO move to initRendering()?
                    gl.glLightfv(GL.GL_LIGHT0 + lnum, GL.GL_SPOT_DIRECTION, light_dir[lnum], 0);

                    if (log.isDebugEnabled()) {
                        log("sceGuLightSpot(" + lnum
                            + ", direction = {" + light_dir[lnum][0]
                            + ", " + light_dir[lnum][1]
                            + ", " + light_dir[lnum][2]
                            + "}, X, X)");
                    }
                }
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
            	light_kind[lnum] = normalArgument & 3; // TODO Use this somewhere...
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
            		log.error("Unknown light type : " + normalArgument);
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
            	texture_upload_start = true;
            	log("sceGumMatrixMode GU_TEXTURE");
                break;

            case TMATRIX:
                if (texture_upload_start) {
                	texture_upload_x = 0;
                	texture_upload_y = 0;
                	texture_upload_start = false;
                }

                if (texture_upload_y < 4) {
                    if (texture_upload_x < 3) {
                        texture_matrix[texture_upload_x + texture_upload_y * 4] = floatArgument;

                        texture_upload_x++;
                        if (texture_upload_x == 3) {
                            texture_matrix[texture_upload_x + texture_upload_y * 4] = (texture_upload_y == 3) ? 1.0f : 0.0f;
                            texture_upload_x = 0;
                            texture_upload_y++;
                            if (texture_upload_y == 4) {
                                log("glLoadMatrixf", texture_matrix);
                                for (int i = 0; i < 4*4; i++)
                                	texture_uploaded_matrix[i] = texture_matrix[i];
                            }
                        }
                    }
                }
                break;

            case PMS:
                proj_upload_start = true;
                log("sceGumMatrixMode GU_PROJECTION");
                break;

            case PROJ:
                if (proj_upload_start) {
                    proj_upload_x = 0;
                    proj_upload_y = 0;
                    proj_upload_start = false;
                }

                if (proj_upload_y < 4) {
                    if (proj_upload_x < 4) {
                        proj_matrix[proj_upload_x + proj_upload_y * 4] = floatArgument;

                        proj_upload_x++;
                        if (proj_upload_x == 4) {
                            proj_upload_x = 0;
                            proj_upload_y++;
                            if (proj_upload_y == 4) {
                                log("glLoadMatrixf", proj_matrix);
                                for (int i = 0; i < 4*4; i++)
                                	proj_uploaded_matrix[i] = proj_matrix[i];
                            }
                        }
                    }
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
                //texture_base_pointer[level] = (currentList.base & 0xff000000) | normalArgument;
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
            	texture_num_mip_maps = (normalArgument>>16) & 0xFF;
                int a2 = (normalArgument>>8) & 0xFF;
            	texture_swizzle 	 = ((normalArgument    ) & 0xFF) != 0;
            	if (log.isDebugEnabled()) {
            	    log ("sceGuTexMode(X, mipmaps=" + texture_num_mip_maps + ", a2=" + a2 + ", swizzle=" + texture_swizzle + ")");
            	}
            	break;
            }

            case TPSM:
            	texture_storage = normalArgument & 0xFF; // saw a game send 0x105
                if (log.isDebugEnabled()) {
                    log ("sceGuTexMode(tpsm=" + texture_storage + "(" + getPsmName(texture_storage) + "), X, X, X)");
                }
            	break;

            case CBP: {
                tex_clut_addr = (tex_clut_addr & 0xff000000) | normalArgument;
                if (log.isDebugEnabled()) {
                    log ("sceGuClutLoad(X, lo(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                }
                break;
            }

            case CBPH: {
                tex_clut_addr = (tex_clut_addr & 0x00ffffff) | ((normalArgument << 8) & 0x0f000000);
                if (log.isDebugEnabled()) {
                    log ("sceGuClutLoad(X, hi(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                }
                break;
            }

            case CLOAD: {
            	tex_clut_num_blocks = normalArgument;
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
            	log ("sceGuTexFilter(min, mag) (mm#" + texture_num_mip_maps + ")");

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
                if (log.isDebugEnabled()) {
                    log ("sceGuTexMapMode(mode=" + tex_map_mode + ", X, X)");
                }
            	break;

            case TEXTURE_ENV_MAP_MATRIX: {
            	log ("sceGuTexMapMode(X, column1, column2)");

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
            	}
            	break;
            }

            case TBIAS: {
                int mode = normalArgument & 0xFFFF;
                float bias = (normalArgument >> 16) / 16.0f;
                log.warn("Unimplemented sceGuTexLevelMode(mode=" + mode + ", bias=" + bias + ")");
                break;
            }

            case TFUNC:
           		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, (normalArgument & 0x10000) != 0 ? 1.0f : 2.0f);
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
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, env_mode);
           		// TODO : check this
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, (normalArgument & 0x100) == 0 ? GL.GL_PREVIOUS : GL.GL_TEXTURE);
           		if (log.isDebugEnabled()) {
           		    log(String.format("sceGuTexFunc mode %08X", normalArgument)
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

            /*
             *
             */
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

                pspdisplay.getInstance().hleDisplaySetGeMode(viewport_width, viewport_height);
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
                // assign or OR lower 24-bits? depends if it's always followed by fbw
                //acording with the psp documentation
                //24 least significant bits of pointer (see FBW)
                //http://hitmen.c02.at/files/yapspd/psp_doc/frames.html
                fbp = normalArgument;
                break;
            case FBW:
                fbp &= 0xffffff;
                fbp |= (normalArgument << 8) & 0xff000000;
                fbw = (normalArgument) & 0xffff;
                if (log.isDebugEnabled()) {
                    log("fbp=" + Integer.toHexString(fbp) + ", fbw=" + fbw);
                }
                pspdisplay.getInstance().hleDisplaySetGeBuf(gl, fbp, fbw, psm, somethingDisplayed);
                break;

            case ZBP:
                // assign or OR lower 24-bits?
                zbp = normalArgument;
                break;
            case ZBW:
                zbp &= 0xffffff;
                zbp |= (normalArgument << 8) & 0xff000000;
                zbw = (normalArgument) & 0xffff;
                if (log.isDebugEnabled()) {
                    log("zbp=" + Integer.toHexString(zbp) + ", zbw=" + zbw);
                }
                break;

            case PSM:
                psm = normalArgument;
                if (log.isDebugEnabled()) {
                    log("psm=" + normalArgument + "(" + getPsmName(normalArgument) + ")");
                }
                break;

            case PRIM:
            {
                int numberOfVertex = normalArgument & 0xFFFF;
                int type = ((normalArgument >> 16) & 0x7);

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

                Memory mem = Memory.getInstance();
                bindBuffers(useVertexColor, false);
                vboBuffer.clear();

                switch (type) {
                    case PRIM_POINT:
                    case PRIM_LINE:
                    case PRIM_LINES_STRIPS:
                    case PRIM_TRIANGLE:
                    case PRIM_TRIANGLE_STRIPS:
                    case PRIM_TRIANGLE_FANS:
                        for (int i = 0; i < numberOfVertex; i++) {
                            int addr = vinfo.getAddress(mem, i);
                            VertexState v = vinfo.readVertex(mem, addr);
                            if (vinfo.texture  != 0) vboBuffer.put(v.t);
                            if (useVertexColor) vboBuffer.put(v.c);
                            if (vinfo.normal   != 0) vboBuffer.put(v.n);
                            if (vinfo.position != 0) {
                            	if(vinfo.weight != 0)
                            		doSkinning(vinfo, v);
                                vboBuffer.put(v.p);
                            }
                            if (log.isTraceEnabled()) {
                            	if (vinfo.texture != 0 && vinfo.position != 0) {
                            		log.trace("  vertex#" + i + " (" + ((int) v.t[0]) + "," + ((int) v.t[1]) + ") at (" + ((int) v.p[0]) + "," + ((int) v.p[1]) + "," + ((int) v.p[2]) + ")");
                            	}
                            }
                        }

                        if(useVBO)
                        	gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboBuffer.rewind(), GL.GL_STREAM_DRAW);
                        gl.glDrawArrays(prim_mapping[type], 0, numberOfVertex);
                        break;


                    case PRIM_SPRITES:
                        gl.glPushAttrib(GL.GL_ENABLE_BIT);
                        gl.glDisable(GL.GL_CULL_FACE);
                        for (int i = 0; i < numberOfVertex; i += 2) {
                            int addr1 = vinfo.getAddress(mem, i);
                            int addr2 = vinfo.getAddress(mem, i + 1);
                            VertexState v1 = vinfo.readVertex(mem, addr1);
                            VertexState v2 = vinfo.readVertex(mem, addr2);

                            v1.p[2] = v2.p[2];

                            if (log.isDebugEnabled() && transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
                                log("  sprite (" + ((int) v1.t[0]) + "," + ((int) v1.t[1]) + ")-(" + ((int) v2.t[0]) + "," + ((int) v2.t[1]) + ") at (" + ((int) v1.p[0]) + "," + ((int) v1.p[1]) + "," + ((int) v1.p[2]) + ")-(" + + ((int) v2.p[0]) + "," + ((int) v2.p[1]) + "," + ((int) v2.p[2]) + ")");
                            }

                            // V1
                            if (vinfo.texture  != 0) vboBuffer.put(v1.t);
                            if (useVertexColor) vboBuffer.put(v2.c);
                            if (vinfo.normal   != 0) vboBuffer.put(v2.n);
                            if (vinfo.position != 0) vboBuffer.put(v1.p);

                            if (vinfo.texture  != 0) vboBuffer.put(v2.t[0]).put(v1.t[1]);
                            if (useVertexColor) vboBuffer.put(v2.c);
                            if (vinfo.normal   != 0) vboBuffer.put(v2.n);
                            if (vinfo.position != 0) vboBuffer.put(v2.p[0]).put(v1.p[1]).put(v2.p[2]);

                            // V2
                            if (vinfo.texture  != 0) vboBuffer.put(v2.t);
                            if (useVertexColor) vboBuffer.put(v2.c);
                            if (vinfo.normal   != 0) vboBuffer.put(v2.n);
                            if (vinfo.position != 0) vboBuffer.put(v2.p);

                            if (vinfo.texture  != 0) vboBuffer.put(v1.t[0]).put(v2.t[1]);
                            if (useVertexColor) vboBuffer.put(v2.c);
                            if (vinfo.normal   != 0) vboBuffer.put(v2.n);
                            if (vinfo.position != 0) vboBuffer.put(v1.p[0]).put(v2.p[1]).put(v2.p[2]);
                        }
                        if(useVBO)
                        	gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboBuffer.rewind(), GL.GL_STREAM_DRAW);
                        gl.glDrawArrays(GL.GL_QUADS, 0, numberOfVertex * 2);
                        gl.glPopAttrib();
                        break;
                }

                endRendering(useVertexColor);
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
	                	VideoEngine.log.error("Unhandled blend mode " + op);
                        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
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
                break;
            case FGE:
                if(normalArgument != 0)
                {
                    gl.glEnable(GL.GL_FOG);
                    gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
				    gl.glFogf(GL.GL_FOG_DENSITY, 0.1f);
				    gl.glHint(GL.GL_FOG_HINT, GL.GL_DONT_CARE);
                    log("sceGuEnable(GL_FOG)");
                }
                else
                {
                    gl.glDisable(GL.GL_FOG);
                    log("sceGuDisable(GL_FOG)");
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
                if(normalArgument != 0) {
                    gl.glEnable(GL.GL_STENCIL_TEST);
                    log("sceGuEnable(GU_STENCIL_TEST)");
                }
                else {
                    gl.glDisable(GL.GL_STENCIL_TEST);
                    log("sceGuDisable(GU_STENCIL_TEST)");
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
                break;
            case JUMP:
            {
                int npc = (normalArgument | currentList.base) & 0xFFFFFFFC;
                //I guess it must be unsign as psp player emulator
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(JUMP) + " old PC:" + String.format("%08x", currentList.pc)
                            + " new PC:" + String.format("%08x", npc));
                }
                currentList.pc = npc;
                break;
            }
            case CALL:
            {
                currentList.stack[currentList.stackIndex++] = currentList.pc;
                int npc = (normalArgument | currentList.base) & 0xFFFFFFFC;
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(CALL) + " old PC:" + String.format("%08x", currentList.pc)
                            + " new PC:" + String.format("%08x", npc));
                }
                currentList.pc = npc;
                break;
            }
            case RET:
            {
                int npc = currentList.stack[--currentList.stackIndex];
                if (log.isDebugEnabled()) {
                    log(helper.getCommandString(RET) + " old PC:" + String.format("%08x", currentList.pc)
                            + " new PC:" + String.format("%08x", npc));
                }
                currentList.pc = npc;
                break;
            }

            case ZMSK: {
            	// NOTE: PSP depth mask as 1 is meant to avoid depth writes,
            	//		on pc it's the opposite
            	gl.glDepthMask(normalArgument == 1 ? false : true);

                if (log.isDebugEnabled()) {
                    log("sceGuDepthMask(" + (normalArgument == 1 ? "disableWrites" : "enableWrites") + ")");
                }
            	break;
            }

	        case ATST: {

	            	int func = GL.GL_ALWAYS;

	            	switch(normalArgument & 0xFF) {
	            	case ATST_NEVER_PASS_PIXEL:
	            		func = GL.GL_NEVER;
	            		break;

	            	case ATST_ALWAYS_PASS_PIXEL:
	            		func = GL.GL_ALWAYS;
	            		break;

	            	case ATST_PASS_PIXEL_IF_MATCHES:
	            		func = GL.GL_EQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_DIFFERS:
	            		func = GL.GL_NOTEQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_LESS:
	            		func = GL.GL_LESS;
	            		break;

	            	case ATST_PASS_PIXEL_IF_LESS_OR_EQUAL:
	            		func = GL.GL_LEQUAL;
	            		break;

	            	case ATST_PASS_PIXEL_IF_GREATER:
	            		func = GL.GL_GREATER;
	            		break;

	            	case ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL:
	            		func = GL.GL_GEQUAL;
	            		break;
	            	}

	            	int referenceAlphaValue = (normalArgument >> 8) & 0xff;
            		gl.glAlphaFunc(func, referenceAlphaValue / 255.0f);
	            	log ("sceGuAlphaFunc(" + func + "," + referenceAlphaValue + ")");

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
            		depthFunc2D = depthFuncClearMode;
            		gl.glPopAttrib();
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
            		gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);
            		gl.glDisable(GL.GL_BLEND);
            		gl.glDisable(GL.GL_STENCIL_TEST);
            		gl.glDisable(GL.GL_LIGHTING);
            		gl.glDisable(GL.GL_TEXTURE_2D);
            		gl.glDisable(GL.GL_ALPHA_TEST);
                    // TODO disable: fog, logic op, scissor?

            		if(useShaders) {
            			gl.glUniform1f(Uniforms.zPos.getId(), 0);
            			gl.glUniform1f(Uniforms.zScale.getId(), 0);
            			gl.glUniform1i(Uniforms.texEnable.getId(), 0);
            			gl.glUniform1i(Uniforms.lightingEnable.getId(), 0);
            		}

            		// TODO Add more disabling in clear mode, we also need to reflect the change to the internal GE registers
            		boolean color = false, alpha = false;
            		if((normalArgument & 0x100) != 0) color = true;
            		if((normalArgument & 0x200) != 0) {
            			alpha = true;
            			// TODO Stencil not perfect, pspsdk clear code is doing more things
                		gl.glEnable(GL.GL_STENCIL_TEST);
            			gl.glStencilFunc(GL.GL_ALWAYS, 0, 0);
            			gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_ZERO);
            		}
            		gl.glDepthMask((normalArgument & 0x400) != 0);
            		depthFuncClearMode = depthFunc2D;
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
                break;

            /*
             * Skinning
             */
            case BOFS: {
            	log("bone matrix offset", normalArgument);

            	if(normalArgument % 12 != 0)
            		VideoEngine.log.warn("bone matrix offset " + normalArgument + " isn't a multiple of 12");

            	bone_matrix_offset = normalArgument / (4*3);
            	bone_upload_start = true;
            	break;
            }

            case BONE: {
            	if (bone_upload_start) {
            		bone_upload_x = 0;
            		bone_upload_y = 0;
            		bone_upload_start = false;
                }

                if (bone_upload_x < 4) {
                	if (bone_upload_y < 3) {
                        bone_matrix[bone_upload_x + bone_upload_y * 4] = floatArgument;

                        bone_upload_x++;
                        if (bone_upload_x == 4) {
                            bone_upload_x = 0;
                            bone_upload_y++;
                            if (bone_upload_y == 3) {
                                log("bone matrix " + bone_matrix_offset, model_matrix);

                                for (int i = 0; i < 4*3; i++)
                                	bone_uploaded_matrix[bone_matrix_offset][i] = bone_matrix[i];
                            }
                        }
                    }
                }
                break;
            }
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
                // use base?
            	textureTx_sourceAddress = normalArgument;
            	break;

            case TRXSBW:
                // remove upper bits first?
            	textureTx_sourceAddress |= (normalArgument << 8) & 0xFF000000;
            	textureTx_sourceLineWidth = normalArgument & 0x0000FFFF;
            	break;

            case TRXDBP:
                // use base?
            	textureTx_destinationAddress = normalArgument;
            	break;

            case TRXDBW:
                // remove upper bits first?
            	textureTx_destinationAddress |= (normalArgument << 8) & 0xFF000000;
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
                    log(helper.getCommandString(TRXKICK) + " from 0x" + Integer.toHexString(textureTx_sourceAddress) + "(" + textureTx_sx + "," + textureTx_sy + ") to 0x" + Integer.toHexString(textureTx_destinationAddress) + "(" + textureTx_dx + "," + textureTx_dy + "), width=" + textureTx_width + ", height=" + textureTx_height);
                }
            	if (!pspdisplay.getInstance().isGeAddress(textureTx_destinationAddress)) {
                    log(helper.getCommandString(TRXKICK) + " not in Ge Address space");
                	int width = textureTx_width;
                	int height = textureTx_height;
                	int bpp = ( textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE ) ? 2 : 4;

                	int srcAddress = textureTx_sourceAddress      + (textureTx_sy * textureTx_sourceLineWidth      + textureTx_sx) * bpp;
            		int dstAddress = textureTx_destinationAddress + (textureTx_dy * textureTx_destinationLineWidth + textureTx_dx) * bpp;
            		Memory memory = Memory.getInstance();
            		for (int y = 0; y < height; y++) {
            			for (int x = 0; x < width; x++) {
            				memory.write32(dstAddress, memory.read32(srcAddress));
            				srcAddress += bpp;
            				dstAddress += bpp;
            			}
            			srcAddress += (textureTx_sourceLineWidth - width) * bpp;
            			dstAddress += (textureTx_destinationLineWidth - width) * bpp;
            		}
            	} else {
                    log(helper.getCommandString(TRXKICK) + " in Ge Address space");

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

            default:
                log.warn("Unknown/unimplemented video command [" + helper.getCommandString(command(instruction)) + "](int="+normalArgument+",float="+floatArgument+")");
        }
        commandStatistics[command].end();
    }

    private void bindBuffers(boolean useVertexColor, boolean useTexture) {
    	int stride = 0, cpos = 0, npos = 0, vpos = 0;

    	if(vinfo.texture != 0 || useTexture) {
        	gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        	stride += BufferUtil.SIZEOF_FLOAT * 2;
        	cpos = npos = vpos = stride;
        }
        if(useVertexColor) {
        	gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        	stride += BufferUtil.SIZEOF_FLOAT * 4;
        	npos = vpos = stride;
        }
        if(vinfo.normal != 0) {
        	gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        	stride += BufferUtil.SIZEOF_FLOAT * 3;
        	vpos = stride;
        }
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        stride += BufferUtil.SIZEOF_FLOAT * 3;

    	if(useVBO) {
        	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboBufferId[0]);

        	if(vinfo.texture != 0) {
            	gl.glTexCoordPointer(2, GL.GL_FLOAT, stride, 0);
            }
            if(useVertexColor) {
            	gl.glColorPointer(4, GL.GL_FLOAT, stride, cpos);
            }
            if(vinfo.normal != 0) {
            	gl.glNormalPointer(GL.GL_FLOAT, stride, npos);
            }
            gl.glVertexPointer(3, GL.GL_FLOAT, stride, vpos);
        } else {
		    if(vinfo.texture != 0) {
		    	gl.glTexCoordPointer(2, GL.GL_FLOAT, stride, vboBuffer.position(0));
		    }
		    if(useVertexColor) {
		    	gl.glColorPointer(4, GL.GL_FLOAT, stride, vboBuffer.position(cpos / BufferUtil.SIZEOF_FLOAT));
		    }
		    if(vinfo.normal != 0) {
		    	gl.glNormalPointer(GL.GL_FLOAT, stride, vboBuffer.position(npos / BufferUtil.SIZEOF_FLOAT));
		    }
		    gl.glVertexPointer(3, GL.GL_FLOAT, stride, vboBuffer.position(vpos / BufferUtil.SIZEOF_FLOAT));
        }
	}

	private void doSkinning(VertexInfo vinfo, VertexState v) {
    	float x = 0, y = 0, z = 0;
    	float nx = 0, ny = 0, nz = 0;
		for(int i = 0; i < vinfo.skinningWeightCount; ++i) {
			if(v.boneWeights[i] != 0.f) {

				x += (	v.p[0] * 	bone_uploaded_matrix[i][0]
				     + 	v.p[1] * 	bone_uploaded_matrix[i][3]
				     + 	v.p[2] * 	bone_uploaded_matrix[i][6]
				     + 			bone_uploaded_matrix[i][9]) * v.boneWeights[i];

				y += (	v.p[0] * 	bone_uploaded_matrix[i][1]
				     + 	v.p[1] * 	bone_uploaded_matrix[i][4]
				     + 	v.p[2] * 	bone_uploaded_matrix[i][7]
				     + 			bone_uploaded_matrix[i][10]) * v.boneWeights[i];

				z += (	v.p[0] * 	bone_uploaded_matrix[i][2]
				     + 	v.p[1] * 	bone_uploaded_matrix[i][5]
				     + 	v.p[2] * 	bone_uploaded_matrix[i][8]
				     + 			bone_uploaded_matrix[i][11]) * v.boneWeights[i];

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

        Texture texture;
        int tex_addr = texture_base_pointer[0] & Memory.addressMask;
        if (!useTextureCache || (tex_addr >= MemoryMap.START_VRAM && tex_addr <= MemoryMap.END_VRAM)) {
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
                                     );
                textureCache.addTexture(gl, texture);
            }

            texture.bindTexture(gl);
            checkTextureMinFilter();
        }

        // Load the texture if not yet loaded
        if (texture == null || !texture.isLoaded()) {
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

            Memory  mem = Memory.getInstance();
            Buffer  final_buffer = null;
            int     texture_type = 0;
            int     texclut = tex_clut_addr;
            int     texaddr;

            final int[] texturetype_mapping = {
                GL.GL_UNSIGNED_SHORT_5_6_5_REV,
                GL.GL_UNSIGNED_SHORT_1_5_5_5_REV,
                GL.GL_UNSIGNED_SHORT_4_4_4_4_REV,
                GL.GL_UNSIGNED_BYTE,
            };

            int textureByteAlignment = 4;   // 32 bits
            int texture_format = GL.GL_RGBA;

            for(int level = 0; level <= texture_num_mip_maps; ++level) {
	            // Extract texture information with the minor conversion possible
	            // TODO: Get rid of information copying, and implement all the available formats
	            texaddr = texture_base_pointer[level];
	            texaddr &= 0xFFFFFFF;
	            texture_format = GL.GL_RGBA;

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
	                            short[] clut = readClut16();

	                            if (!texture_swizzle) {
	                                for (int i = 0, j = 0; i < texture_buffer_width[level]*texture_height[level]; i += 2, j++) {

	                                    int index = mem.read8(texaddr+j);

	                                    tmp_texture_buffer16[i+1]   = clut[getClutIndex((index >> 4) & 0xF)];
	                                    tmp_texture_buffer16[i]     = clut[getClutIndex( index       & 0xF)];
	                                }
	                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                            } else {
	                                VideoEngine.log.error("Unhandled swizzling on clut4/16 textures");
	                                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
	                                break;
	                            }

	                            break;
	                        }

	                        case CMODE_FORMAT_32BIT_ABGR8888: {
	                            if (texclut == 0)
	                                return;

	                            texture_type = GL.GL_UNSIGNED_BYTE;
	                            int[] clut = readClut32();

	                            if (!texture_swizzle) {
	                                for (int i = 0, j = 0; i < texture_buffer_width[level]*texture_height[level]; i += 2, j++) {

	                                    int index = mem.read8(texaddr+j);

	                                    tmp_texture_buffer32[i+1] = clut[getClutIndex((index >> 4) & 0xF)];
	                                    tmp_texture_buffer32[i]   = clut[getClutIndex( index       & 0xF)];
	                                }
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                            } else {
	                                unswizzleTextureFromMemory(texaddr, 0, level);
	                                int pixels = texture_buffer_width[level] * texture_height[level];
	                                for (int i = pixels - 8, j = (pixels / 8) - 1; i >= 0; i -= 8, j--) {
	                                    int n = tmp_texture_buffer32[j];
	                                    int index = n & 0xF;
	                                    tmp_texture_buffer32[i + 0] = clut[getClutIndex(index)];
	                                    index = (n >> 4) & 0xF;
	                                    tmp_texture_buffer32[i + 1] = clut[getClutIndex(index)];
	                                    index = (n >> 8) & 0xF;
	                                    tmp_texture_buffer32[i + 2] = clut[getClutIndex(index)];
	                                    index = (n >> 12) & 0xF;
	                                    tmp_texture_buffer32[i + 3] = clut[getClutIndex(index)];
	                                    index = (n >> 16) & 0xF;
	                                    tmp_texture_buffer32[i + 4] = clut[getClutIndex(index)];
	                                    index = (n >> 20) & 0xF;
	                                    tmp_texture_buffer32[i + 5] = clut[getClutIndex(index)];
	                                    index = (n >> 24) & 0xF;
	                                    tmp_texture_buffer32[i + 6] = clut[getClutIndex(index)];
	                                    index = (n >> 28) & 0xF;
	                                    tmp_texture_buffer32[i + 7] = clut[getClutIndex(index)];
	                                }
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                            }

	                            break;
	                        }

	                        default: {
	                            VideoEngine.log.error("Unhandled clut4 texture mode " + tex_clut_mode);
	                            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
	                            break;
	                        }
	                    }

	                    break;
	                }
	                case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED: {

	                    switch (tex_clut_mode) {
	                        case CMODE_FORMAT_16BIT_BGR5650:
	                        case CMODE_FORMAT_16BIT_ABGR5551:
	                        case CMODE_FORMAT_16BIT_ABGR4444: {
	                            if (texclut == 0)
	                                return;

	                            texture_type = texturetype_mapping[tex_clut_mode];
	                            textureByteAlignment = 2;  // 16 bits
	                            short[] clut = readClut16();

	                            if (!texture_swizzle) {
	                                for (int i = 0; i < texture_buffer_width[level]*texture_height[level]; i++) {
	                                    int index = mem.read8(texaddr+i);
	                                    tmp_texture_buffer16[i]     = clut[getClutIndex(index)];
	                                }
	                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                            } else {
	                                unswizzleTextureFromMemory(texaddr, 1, level);
	                                for (int i = 0, j = 0; i < texture_buffer_width[level]*texture_height[level]; i += 4, j++) {
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
	                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                            }

	                            break;
	                        }

	                        case CMODE_FORMAT_32BIT_ABGR8888: {
	                            if (texclut == 0)
	                                return;

	                            texture_type = GL.GL_UNSIGNED_BYTE;
	                            int[] clut = readClut32();

	                            if (!texture_swizzle) {
	                                for (int i = 0; i < texture_buffer_width[level]*texture_height[level]; i++) {
	                                    int index = mem.read8(texaddr+i);
	                                    tmp_texture_buffer32[i] = clut[getClutIndex(index)];
	                                }
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                            } else {
	                                unswizzleTextureFromMemory(texaddr, 1, level);
	                                int pixels = texture_buffer_width[level] * texture_height[level];
	                                for (int i = pixels - 4, j = (pixels / 4) - 1; i >= 0; i -= 4, j--) {
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
	                                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	                            }

	                            break;
	                        }

	                        default: {
	                            VideoEngine.log.error("Unhandled clut8 texture mode " + tex_clut_mode);
	                            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
	                            break;
	                        }
	                    }

	                    break;
	                }

	                case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
	                case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: {
	                    texture_type = texturetype_mapping[texture_storage];
	                    textureByteAlignment = 2;  // 16 bits

	                    if (!texture_swizzle) {
	                        /* TODO replace the loop with 1 line to ShortBuffer.wrap
	                         * but be careful of vram/mainram addresses
	                        final_buffer = ShortBuffer.wrap(
	                            memory.videoram.array(),
	                            texaddr - MemoryMap.START_VRAM + memory.videoram.arrayOffset(),
	                            texture_width0 * texture_height0).slice();
	                        final_buffer = ShortBuffer.wrap(
	                            memory.mainmemory.array(),
	                            texaddr - MemoryMap.START_RAM + memory.mainmemory.arrayOffset(),
	                            texture_width0 * texture_height0).slice();
	                        */

	                        for (int i = 0; i < texture_buffer_width[level]*texture_height[level]; i++) {
	                            int pixel = mem.read16(texaddr+i*2);
	                            tmp_texture_buffer16[i] = (short)pixel;
	                        }

	                        final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
	                    } else {
	                        final_buffer = unswizzleTextureFromMemory(texaddr, 2, level);
	                    }

	                    break;
	                }

	                case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: {
	                    if (getOpenGLVersion(gl).compareTo("1.2") >= 0) {
	                        texture_type = GL.GL_UNSIGNED_INT_8_8_8_8_REV;  // Only available from V1.2
	                    } else {
	                        texture_type = GL.GL_UNSIGNED_BYTE;
	                    }

	                    final_buffer = getTexture32BitBuffer(texaddr, level);
	                    break;
	                }

                    case TPSM_PIXEL_STORAGE_MODE_DXT1: {
                        texture_type = GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                        final_buffer = getTexture32BitBuffer(texaddr, level);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT3: {
                        texture_type = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                        final_buffer = getTexture32BitBuffer(texaddr, level);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT5: {
                        texture_type = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                        final_buffer = getTexture32BitBuffer(texaddr, level);
                        break;
                    }

                    default: {
	                    VideoEngine.log.warn("Unhandled texture storage " + texture_storage);
	                    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
	                    break;
	                }
	            }

	            // Some textureTypes are only supported from OpenGL v1.2.
	            // Try to convert to type supported in v1.
	            if (getOpenGLVersion(gl).compareTo("1.2") < 0) {
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
	            checkTextureMinFilter();

                // apparently w > tbw still works, but I think we should log it just incase (fiveofhearts)
                // update: seems some games are using tbw greater AND less than w, now I haven't got a clue what the meaning of the 2 variables are
                /*
                if (texture_width[level] > texture_buffer_width[level]) {
                    log.warn(helper.getCommandString(TFLUSH) + " w > tbw : w=" + texture_width[level] + " tbw=" + texture_buffer_width[level]);
                } else if (texture_width[level] < texture_buffer_width[level]) {
                    log.warn(helper.getCommandString(TFLUSH) + " w < tbw : w=" + texture_width[level] + " tbw=" + texture_buffer_width[level]);
                }
                */

	            gl.glTexImage2D  (  GL.GL_TEXTURE_2D,
	                                level,
	                                texture_format,
	                                texture_width[level], texture_height[level],
	                                0,
	                                texture_format,
	                                texture_type,
	                                final_buffer);

	            if (texture != null) {
	                texture.setIsLoaded();
	                if (log.isDebugEnabled()) {
	                    log(helper.getCommandString(TFLUSH) + " Loaded texture " + texture.getGlId());
	                }
	            }
            }
            if(texture_num_mip_maps != 0) {
            	for(int level = 0; level <= texture_num_mip_maps; ++level)
            		log(String.format("Mipmap PSP Texture level %d size %dx%d", level, texture_width[level], texture_height[level]));
	            int maxLevel = (int) (Math.log(Math.max(texture_width[texture_num_mip_maps], texture_height[texture_num_mip_maps]) * (1 << texture_num_mip_maps))/Math.log(2));

	            if(maxLevel != texture_num_mip_maps) {
		            log(String.format("Generating mipmaps from level %d Size %dx%d to maxLevel %d", texture_num_mip_maps, texture_width[0], texture_height[0], maxLevel));
		            // Build the other mipmaps level
		            glu.gluBuild2DMipmapLevels(GL.GL_TEXTURE_2D,
		            		texture_format,
		            		texture_width[texture_num_mip_maps], texture_height[texture_num_mip_maps],
		            		texture_format,
		            		texture_type,
		            		texture_num_mip_maps, texture_num_mip_maps + 1, maxLevel, final_buffer);
		            for(int i = 0; i <= maxLevel; ++i) {
		            	float[] size = new float[2];
		            	gl.glGetTexLevelParameterfv(GL.GL_TEXTURE_2D, i, GL.GL_TEXTURE_WIDTH, size, 0);
		            	gl.glGetTexLevelParameterfv(GL.GL_TEXTURE_2D, i, GL.GL_TEXTURE_HEIGHT, size, 1);
		            	log(String.format("OGL Texture level %d size %dx%d", i, (int)size[0], (int)size[1]));
		            }
	            }
            }
        } else {
            if (log.isDebugEnabled()) {
                log(helper.getCommandString(TFLUSH) + " Reusing cached texture " + texture.getGlId());
            }
        }
    }

	private void checkTextureMinFilter() {
		if(texture_num_mip_maps == 0 && !(tex_min_filter == GL.GL_LINEAR || tex_min_filter == GL.GL_NEAREST)) {
			int nex_tex_min_filter;
			if(tex_min_filter == GL.GL_NEAREST_MIPMAP_LINEAR || tex_min_filter == GL.GL_NEAREST_MIPMAP_NEAREST)
				nex_tex_min_filter = GL.GL_NEAREST;
			else
				nex_tex_min_filter = GL.GL_LINEAR;
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, nex_tex_min_filter);
			log("Overwriting texture min filter, no mipmap was generated but filter was set to use mipmap");
		}
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
            gl.glDepthFunc(depthFunc3D);
            gl.glLoadMatrixf(proj_uploaded_matrix, 0);
        } else {
            gl.glDepthFunc(depthFunc2D);
            // 2D mode shouldn't be affected by the lighting
        	gl.glOrtho(0.0, 480, 272, 0, Double.MAX_VALUE, Double.MIN_VALUE);
            gl.glPushAttrib(GL.GL_LIGHTING_BIT);
            gl.glDisable(GL.GL_LIGHTING);
            if(useShaders) {
            	gl.glUniform1i(Uniforms.lightingEnable.getId(), 0);
            }
        }

        /*
         * Apply texture transforms
         */
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix ();
        gl.glLoadIdentity();
        gl.glTranslatef(tex_translate_x, tex_translate_y, 0.f);
        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
            gl.glScalef(tex_scale_x, tex_scale_y, 1.f);
        else
            gl.glScalef(1.f / texture_width[0], 1.f / texture_height[0], 1.f);

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
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light_pos[0], 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light_pos[1], 0);
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_POSITION, light_pos[2], 0);
        gl.glLightfv(GL.GL_LIGHT3, GL.GL_POSITION, light_pos[3], 0);

        // Apply model matrix
        if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
            gl.glMultMatrixf(model_uploaded_matrix, 0);

        boolean useVertexColor = false;
        if(!lighting) {
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

    private void endRendering(boolean useVertexColor) {
        switch (tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
                gl.glDisable (GL.GL_TEXTURE_GEN_S);
                gl.glDisable (GL.GL_TEXTURE_GEN_T);
                break;
            }
        }

        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        if(vinfo.texture != 0) gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
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

        bindBuffers(useVertexColor, true);

        float pyold = 0;
        for (int u = 1; u <= udivs; u++) {
            // Percent Along Y-Axis
            float py = ((float) u) / ((float) udivs);

            // Calculate New Bezier Points
            for (int i = 0; i < ucount; i++) {
                 Bernstein(temp[i], py, anchors[i]);
            }

            vboBuffer.clear();

            for (int v = 0; v <= vdivs; v++) {
                // Percent Along The X-Axis
                float px = ((float) v) / ((float) vdivs);

                // Apply The Old Texture Coords
                if (vinfo.texture != 0) {
                    vboBuffer.put(last[v].t);
                } else {
                    vboBuffer.put(1 - pyold);
                    vboBuffer.put(1 - px);
                }
                // Old Point
                vboBuffer.put(last[v].p);

                // Generate New Point
                Bernstein(last[v], px, temp);

                // Apply The New Texture Coords
                if (vinfo.texture != 0) {
                    vboBuffer.put(last[v].t);
                } else {
                    vboBuffer.put(1 - py);
                    vboBuffer.put(1 - px);
                }
                // New Point
                vboBuffer.put(last[v].p);
            }

            if(useVBO) {
                gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBuffer.position() * BufferUtil.SIZEOF_FLOAT, vboBuffer.rewind(), GL.GL_STREAM_DRAW);
            }
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, (vdivs + 1) * 2);

            pyold = py;
        }

        endRendering(useVertexColor);
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
            // try and use ByteBuffer.wrap on the memory, taking note of vram/main ram
            // speed difference is unnoticeable :(
            Memory mem = Memory.getInstance();
            int bufferlen = Math.max(texture_width[level], texture_buffer_width[level]) * texture_height[level] * 4;
            Buffer pixels = mem.getBuffer(texaddr, bufferlen);
            if (pixels != null) {
                final_buffer = pixels;
            } else {
                VideoEngine.log.warn("tpsm 3 slow");
                for (int i = 0; i < texture_buffer_width[level]*texture_height[level]; i++) {
                    tmp_texture_buffer32[i] = mem.read32(texaddr+i*4);
                }
                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
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
}

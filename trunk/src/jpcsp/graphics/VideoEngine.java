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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.pspdisplay;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class VideoEngine {

    private static VideoEngine instance;
    private GL gl;
    public static Logger log = Logger.getLogger("ge");
    public static final boolean isDebugMode = true;
    private static GeCommands helper;
    private VertexInfo vinfo = new VertexInfo();
    private static final char SPACE = ' ';

    // TODO these currently here for testing only
    private int fbp, fbw; // frame buffer pointer and width
    private int zbp, zbw; // depth buffer pointer and width
    private int psm; // pixel format

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

    int[] light_type = new int[4];
    
    float[] fog_color = new float[4];
    float fog_far = 0.0f,fog_dist = 0.0f;
    
    private float nearZ = 0.0f,farZ = 0.0f;


	int mat_flags = 0;
    float[] mat_ambient = new float[4];
    float[] mat_diffuse = new float[4];
    float[] mat_specular = new float[4];
	float[] mat_emissive = new float[4];

    int texture_storage, texture_num_mip_maps;
    boolean texture_swizzle;
    int texture_base_pointer0, texture_width0, texture_height0;
    int texture_buffer_width0;
    int tex_min_filter = GL.GL_NEAREST;
    int tex_mag_filter = GL.GL_NEAREST;

    float tex_translate_x = 0.f, tex_translate_y = 0.f;
    float tex_scale_x = 1.f, tex_scale_y = 1.f;
    float[] tex_env_color = new float[4];

    private int tex_clut_addr;
    private int tex_clut_num_blocks;
    private int tex_clut_mode, tex_clut_shift, tex_clut_mask, tex_clut_start;

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

    // opengl needed information/buffers
    int[] gl_texture_id = new int[1];
    int[] tmp_texture_buffer32 = new int[1024*1024];
    short[] tmp_texture_buffer16 = new short[1024*1024];
    int tex_map_mode = TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;

    private boolean listHasEnded;
    private boolean listHasFinished;
    private DisplayList actualList; // The currently executing list
    private int clearFlags;
    private static void log(String msg) {
    	log.debug(msg);
        /*if (isDebugMode) {
            System.out.println("sceGe DEBUG > " + msg);
        }*/
    }

    public static VideoEngine getEngine(GL gl, boolean fullScreen, boolean hardwareAccelerate) {
        VideoEngine engine = getEngine();
        engine.setFullScreenShoot(fullScreen);
        engine.setHardwareAcc(hardwareAccelerate);
        engine.gl = gl;
        return engine;
    }

    private static VideoEngine getEngine() {
        if (instance == null) {
            instance = new VideoEngine();
            helper = new GeCommands();
        }
        return instance;
    }

    private VideoEngine() {
    	model_matrix[0] = model_matrix[5] = model_matrix[10] = model_matrix[15] = 1.f;
    	view_matrix[0] = view_matrix[5] = view_matrix[10] = view_matrix[15] = 1.f;
    	tex_envmap_matrix[0] = tex_envmap_matrix[5] = tex_envmap_matrix[10] = tex_envmap_matrix[15] = 1.f;
    	light_pos[0][3] = light_pos[1][3] = light_pos[2][3] = light_pos[3][3] = 1.f;
    }

    /** call from GL thread
     * @return true if an update was made
     */
    public boolean update() {
        //System.err.println("update start");

        // Don't draw anything until we get sync signal
        if (!jpcsp.HLE.pspge.getInstance().waitingForSync)
            return false;

        boolean updated = false;
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

        if (updated)
            jpcsp.HLE.pspge.getInstance().syncDone = true;

        //System.err.println("update done");
        return updated;
    }

    // call from GL thread
    private void executeList(DisplayList list) {
        actualList = list;
        listHasEnded = false;
        listHasFinished = false;

        log("executeList id " + list.id);

        Memory mem = Memory.getInstance();
        while (!listHasEnded && !listHasFinished &&
            actualList.pc != actualList.stallAddress && !Emulator.pause) {
            int ins = mem.read32(actualList.pc);
            actualList.pc += 4;
            executeCommand(ins);
        }

        if (actualList.pc == actualList.stallAddress) {
            actualList.status = DisplayList.STALL_REACHED;
            log("list " + actualList.id + " stalled at " + String.format("%08x", actualList.stallAddress));
        }

        if (listHasFinished) {
            // List can still be updated
            // TODO we should probably recycle lists if they never reach the end state
            actualList.status = DisplayList.DRAWING_DONE;
        }
        if (listHasEnded) {
            // Now we can remove the list context
            actualList.status = DisplayList.DONE;
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
    
    //hack based on pspplayer
    private int getBlendSrc (int pspSrc)
    {
    	switch(pspSrc)
    	{
	    	case 0x0:
	    		return GL.GL_DST_COLOR;
    		
	    	case 0x1:
	    		return GL.GL_ONE_MINUS_DST_COLOR;
	    		
	    	case 0x2:
	    		return GL.GL_SRC_ALPHA;
	    		
	    	case 0x3:
	    		return GL.GL_ONE_MINUS_SRC_ALPHA;
	    		
	    	case 0x4:
	    		return GL.GL_DST_ALPHA;
	    		
	    	case 0x5:
	    		return GL.GL_ONE_MINUS_DST_ALPHA;
	    		
	    	case 0x6:
	    		return GL.GL_SRC_ALPHA;
	    		
	    	case 0x7:
	    		return GL.GL_ONE_MINUS_SRC_ALPHA;
	    		
	    	case 0x8:
	    		return GL.GL_DST_ALPHA;
	    		
	    	case 0x9:
	    		return GL.GL_ONE_MINUS_DST_ALPHA;
	    		
	    	case 0xa: 
	    		return GL.GL_SRC_ALPHA;
    	
    	}
    	
    	VideoEngine.log.error("Unhandled alpha blend src used " + pspSrc);
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
    	return GL.GL_DST_COLOR;
    }

    private int getBlendOp (int pspOP) {
    	switch (pspOP) {
		    case ALPHA_SOURCE_COLOR:
		    	return GL.GL_SRC_COLOR;

		    case ALPHA_ONE_MINUS_SOURCE_COLOR:
		    	return GL.GL_ONE_MINUS_SRC_COLOR;

		    case ALPHA_SOURCE_ALPHA:
		    	return GL.GL_SRC_ALPHA;

		    case ALPHA_ONE_MINUS_SOURCE_ALPHA:
	    		return GL.GL_ONE_MINUS_SRC_ALPHA;

			// hacks based on pspplayer
		    case ALPHA_DESTINATION_COLOR:
		    	return GL.GL_DST_ALPHA;

		    case ALPHA_ONE_MINUS_DESTINATION_COLOR:
		    	return GL.GL_ONE_MINUS_DST_ALPHA;

		    case ALPHA_DESTINATION_ALPHA:
		    	return GL.GL_SRC_ALPHA;

		    case ALPHA_ONE_MINUS_DESTINATION_ALPHA:
		    	return GL.GL_ONE_MINUS_SRC_ALPHA;
		    	
		    case 0x8:
		    	return GL.GL_DST_ALPHA;
		    	
		    case 0x9:
 		    	return GL.GL_ONE_MINUS_DST_ALPHA;
		    	
		    case 0xa:
		    	return GL.GL_ONE_MINUS_SRC_ALPHA;
    	}

    	VideoEngine.log.error("Unhandled alpha blend op used " + pspOP);
        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
    	return GL.GL_ONE;
    }

    private int getClutIndex(int index) {
        return ((tex_clut_start + index) >> tex_clut_shift) & tex_clut_mask;
    }

    // UnSwizzling based on pspplayer
    // This won't work if you can't unswizzle inplace!
    private Buffer unswizzleTexture32() {
        int rowWidth = texture_width0 * 4;
        int pitch = ( rowWidth - 16 ) / 4;
        int bxc = rowWidth / 16;
        int byc = texture_height0 / 8;

        int src = 0, ydest = 0;

        for( int by = 0; by < byc; by++ )
        {
            int xdest = ydest;
            for( int bx = 0; bx < bxc; bx++ )
            {
                int dest = xdest;
                for( int n = 0; n < 8; n++ )
                {
                    tmp_texture_buffer32[dest] = tmp_texture_buffer32[src];
                    tmp_texture_buffer32[dest+1] = tmp_texture_buffer32[src + 1];
                    tmp_texture_buffer32[dest+2] = tmp_texture_buffer32[src + 2];
                    tmp_texture_buffer32[dest+3] = tmp_texture_buffer32[src + 3];

                    src     += 4;
                    dest    += pitch+4;
                }
                xdest += (16/4);
            }
            ydest += (rowWidth * 8)/4;
        }

        return IntBuffer.wrap(tmp_texture_buffer32);
    }

    // UnSwizzling based on pspplayer
    private Buffer unswizzleTextureFromMemory(int texaddr, int bytesPerPixel) {
        Memory mem = Memory.getInstance();
        int rowWidth = texture_width0 * bytesPerPixel;
        int pitch = ( rowWidth - 16 ) / 4;
        int bxc = rowWidth / 16;
        int byc = texture_height0 / 8;

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

        switch (command(instruction)) {
            case END:
                listHasEnded = true;
                log(helper.getCommandString(END));
                break;
            case FINISH:
                listHasFinished = true;
                log(helper.getCommandString(FINISH));
                break;
            case BASE:
                actualList.base = normalArgument << 8;
                log(helper.getCommandString(BASE) + " " + String.format("%08x", actualList.base));
                break;
            case IADDR:
                vinfo.ptr_index = actualList.base | normalArgument;
                log(helper.getCommandString(IADDR) + " " + String.format("%08x", vinfo.ptr_index));
                break;
            case VADDR:
                vinfo.ptr_vertex = actualList.base | normalArgument;
                log(helper.getCommandString(VADDR) + " " + String.format("%08x", vinfo.ptr_vertex));
                break;
            case VTYPE:
                vinfo.processType(normalArgument);
                transform_mode = (normalArgument >> 23) & 0x1;
                log(helper.getCommandString(VTYPE) + " " + vinfo.toString());
                break;

            case TME:
                if (normalArgument != 0) {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                    log("sceGuEnable(GU_TEXTURE_2D)");
                } else {
                    gl.glDisable(GL.GL_TEXTURE_2D);
                    log("sceGuDisable(GU_TEXTURE_2D)");
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


            /*
             * Light types
             */

            case LT0: {
            	light_type[0] = normalArgument;

            	if (light_type[0] == LIGTH_DIRECTIONAL)
            		light_pos[0][3] = 0.f;
            	else
            		light_pos[0][3] = 1.f;
            	break;
        	}
            case LT1: {
            	light_type[1] = normalArgument;

            	if (light_type[1] == LIGTH_DIRECTIONAL)
            		light_pos[1][3] = 0.f;
            	else
            		light_pos[1][3] = 1.f;
            	break;
        	}
            case LT2: {
            	light_type[2] = normalArgument;

            	if (light_type[2] == LIGTH_DIRECTIONAL)
            		light_pos[2][3] = 0.f;
            	else
            		light_pos[2][3] = 1.f;
            	break;
        	}
            case LT3: {
            	light_type[3] = normalArgument;

            	if (light_type[3] == LIGTH_DIRECTIONAL)
            		light_pos[3][3] = 0.f;
            	else
            		light_pos[3][3] = 1.f;
            	break;
        	}


            /*
             * Individual lights enable/disable
             */
            case LTE0:
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHT0);
                    log("sceGuEnable(GL_LIGHT0)");
                } else {
                    gl.glDisable(GL.GL_LIGHT0);
                    log("sceGuDisable(GL_LIGHT0)");
                }
                break;

            case LTE1:
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHT1);
                    log("sceGuEnable(GL_LIGHT1)");
                } else {
                    gl.glDisable(GL.GL_LIGHT1);
                    log("sceGuDisable(GL_LIGHT1)");
                }
                break;

            case LTE2:
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHT2);
                    log("sceGuEnable(GL_LIGHT2)");
                } else {
                    gl.glDisable(GL.GL_LIGHT2);
                    log("sceGuDisable(GL_LIGHT2)");
                }
                break;

            case LTE3:
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHT3);
                    log("sceGuEnable(GL_LIGHT3)");
                } else {
                    gl.glDisable(GL.GL_LIGHT3);
                    log("sceGuDisable(GL_LIGHT3)");
                }
                break;


            /*
             * Lighting enable/disable
             */
            case LTE:
            	if (normalArgument != 0) {
                    gl.glEnable(GL.GL_LIGHTING);
                    log("sceGuEnable(GL_LIGHTING)");
                } else {
                    gl.glDisable(GL.GL_LIGHTING);
                    log("sceGuDisable(GL_LIGHTING)");
                }
                break;

            /*
             * Material setup
             */
            case CMAT:
            	mat_flags = normalArgument & 7;
            	log("cmat");
            	break;

            case AMA:
            	mat_ambient[3] = ((normalArgument      ) & 255) / 255.f;
            	if((mat_flags & 1) != 0)
            		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
            	break;

            case AMC:
            	mat_ambient[0] = ((normalArgument	   ) & 255) / 255.f;
            	mat_ambient[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_ambient[2] = ((normalArgument >> 16) & 255) / 255.f;
            	if((mat_flags & 1) != 0)
            		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
            	log("sceGuAmbient " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                        mat_ambient[0], mat_ambient[1], mat_ambient[2], normalArgument));
            	break;

            case DMC:
            	mat_diffuse[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_diffuse[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_diffuse[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_diffuse[3] = 1.f;
            	if((mat_flags & 2) != 0)
            		gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
            	log("sceGuColor " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                        mat_diffuse[0], mat_diffuse[1], mat_diffuse[2], normalArgument));
            	break;

            case EMC:
            	mat_emissive[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_emissive[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_emissive[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_emissive[3] = 1.f;
            	gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, mat_emissive, 0);
            	log("material emission " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                        mat_emissive[0], mat_emissive[1], mat_emissive[2], normalArgument));
            	break;

            case SMC:
            	mat_specular[0] = ((normalArgument      ) & 255) / 255.f;
            	mat_specular[1] = ((normalArgument >>  8) & 255) / 255.f;
            	mat_specular[2] = ((normalArgument >> 16) & 255) / 255.f;
            	mat_specular[3] = 1.f;
            	if((mat_flags & 4) != 0)
            		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
            	log("material specular " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                        mat_specular[0], mat_specular[1], mat_specular[2], normalArgument));
            	break;

            case SPOW:
            	gl.glMaterialf(GL.GL_FRONT, GL.GL_SHININESS, floatArgument);
            	log("material shininess");
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
                    if (texture_upload_x < 4) {
                        texture_matrix[texture_upload_x + texture_upload_y * 4] = floatArgument;

                        texture_upload_x++;
                        if (texture_upload_x == 4) {
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
                texture_base_pointer0 = (texture_base_pointer0 & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                texture_buffer_width0 = normalArgument & 0xffff;
                log ("sceGuTexImage(X,X,X,texWidth=" + texture_buffer_width0 + ",hi(pointer=0x" + Integer.toHexString(texture_base_pointer0) + "))");
                break;

            case TBP0:
                texture_base_pointer0 = normalArgument;
                log ("sceGuTexImage(X,X,X,X,lo(pointer=0x" + Integer.toHexString(texture_base_pointer0) + "))");
                break;

            case TSIZE0:
            	texture_height0 = 1 << ((normalArgument>>8) & 0xFF);
            	texture_width0  = 1 << ((normalArgument   ) & 0xFF);
            	log ("sceGuTexImage(X,width=" + texture_width0 + ",height=" + texture_height0 + ",X,0)");
            	break;

            case TMODE:
            	texture_num_mip_maps = (normalArgument>>16) & 0xFF;
            	texture_swizzle 	 = ((normalArgument    ) & 0xFF) != 0;
            	break;

            case TPSM:
            	texture_storage = normalArgument;
            	break;

            case CBP: {
                tex_clut_addr = (tex_clut_addr & 0xff000000) | normalArgument;
                log ("sceGuClutLoad(X, lo(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                break;
            }

            case CBPH: {
                tex_clut_addr = (tex_clut_addr & 0x00ffffff) | ((normalArgument << 8) & 0x0f000000);
                log ("sceGuClutLoad(X, hi(cbp=0x" + Integer.toHexString(tex_clut_addr) + "))");
                break;
            }

            case CLOAD: {
            	tex_clut_num_blocks = normalArgument;
            	log ("sceGuClutLoad(num_blocks=" + tex_clut_num_blocks + ", X)");
            	break;
            }

            case CMODE: {
                tex_clut_mode   =  normalArgument       & 0x03;
                tex_clut_shift  = (normalArgument >> 2) & 0x3F;
                tex_clut_mask   = (normalArgument >> 8) & 0xFF;
                tex_clut_start  = (normalArgument >> 16) & 0xFF;
                log ("sceGuClutMode(cpsm=" + tex_clut_mode + ", shift=" + tex_clut_shift + ", mask=0x" + Integer.toHexString(tex_clut_mask) + ", start=" + tex_clut_start + ")");
                break;
            }

            case TFLUSH:
            {
            	// HACK: avoid texture uploads of null pointers
            	if (texture_base_pointer0 == 0)
            		break;

            	// Generate a texture id if we don't have one
            	if (gl_texture_id[0] == 0)
                	gl.glGenTextures(1, gl_texture_id, 0);


                log(helper.getCommandString(TFLUSH) + " 0x" + Integer.toHexString(texture_base_pointer0) + "(" + texture_width0 + "," + texture_height0 + ")");
            	// Extract texture information with the minor conversion possible
            	// TODO: Get rid of information copying, and implement all the available formats
            	Memory 	mem = Memory.getInstance();
            	Buffer 	final_buffer = null;
            	int 	texture_type = 0;
            	int		texclut = tex_clut_addr;
            	int 	texaddr = texture_base_pointer0;
            	texaddr &= 0xFFFFFFF;

                final int[] texturetype_mapping = {
                    GL.GL_UNSIGNED_SHORT_5_6_5_REV,
                    GL.GL_UNSIGNED_SHORT_1_5_5_5_REV,
                    GL.GL_UNSIGNED_SHORT_4_4_4_4_REV,
                    GL.GL_UNSIGNED_BYTE,
                };

                int textureByteAlignment = 4;   // 32 bits
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

            					if (!texture_swizzle) {
		            				for (int i = 0, j = 0; i < texture_width0*texture_height0; i += 2, j++) {

		            					int index = mem.read8(texaddr+j);

		            					// TODO:  I don't know if it's correct, or should read the 4bits in
		            					//       reverse order
		            					tmp_texture_buffer16[i] 	= (short)mem.read16(texclut + getClutIndex((index >> 4) & 0xF) * 2);
		            					tmp_texture_buffer16[i+1] 	= (short)mem.read16(texclut + getClutIndex( index       & 0xF) * 2);
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

	            				if (!texture_swizzle) {
		            				for (int i = 0, j = 0; i < texture_width0*texture_height0; i += 2, j++) {

		            					int index = mem.read8(texaddr+j);

		            					// TODO:  I don't know if it's correct, or should read the 4bits in
		            					//       reverse order
		            					tmp_texture_buffer32[i] 	= mem.read32(texclut + getClutIndex((index >> 4) & 0xF) * 4);
		            					tmp_texture_buffer32[i+1] 	= mem.read32(texclut + getClutIndex( index       & 0xF) * 4);
		            				}
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	            				} else {
            						//VideoEngine.log.error("Unhandled swizzling on clut4/32 textures");
		                            //Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
                                    final_buffer = unswizzleTexture32();
		                            break;
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

            					if (!texture_swizzle) {
		            				for (int i = 0; i < texture_width0*texture_height0; i++) {
		            					int index = mem.read8(texaddr+i);
		            					tmp_texture_buffer16[i] 	= (short)mem.read16(texclut + getClutIndex(index) * 2);
		            				}
                                    final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
            					} else {
            						VideoEngine.log.error("Unhandled swizzling on clut8/16 textures");
    	                            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
    	                            break;
            					}

	            				break;
	            			}

	            			case CMODE_FORMAT_32BIT_ABGR8888: {
	            				if (texclut == 0)
            						return;

	            				texture_type = GL.GL_UNSIGNED_BYTE;

	            				if (!texture_swizzle) {
		            				for (int i = 0; i < texture_width0*texture_height0; i++) {
		            					int index = mem.read8(texaddr+i);
		            					tmp_texture_buffer32[i] = mem.read32(texclut + getClutIndex(index) * 4);
		            				}
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
	            				} else {
	            					//VideoEngine.log.error("Unhandled swizzling on clut8/32 textures");
	    	                        //Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
                                    final_buffer = unswizzleTexture32();
	    	                        break;
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

	                    	for (int i = 0; i < texture_width0*texture_height0; i++) {
	                    		int pixel = mem.read16(texaddr+i*2);
	                    		tmp_texture_buffer16[i] = (short)pixel;
	                    	}

	                    	final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                        } else {
                            final_buffer = unswizzleTextureFromMemory(texaddr, 2);
             			}

            			break;
            		}

            		case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: {
            			texture_type = GL.GL_UNSIGNED_INT_8_8_8_8_REV;

            			if (!texture_swizzle) {
                            /* TODO replace the loop with 1 line to IntBuffer.wrap
                             * but be careful of vram/mainram addresses
                            final_buffer = IntBuffer.wrap(
                                memory.videoram.array(),
                                texaddr - MemoryMap.START_VRAM + memory.videoram.arrayOffset(),
                                texture_width0 * texture_height0).slice();
                            final_buffer = IntBuffer.wrap(
                                memory.mainmemory.array(),
                                texaddr - MemoryMap.START_RAM + memory.mainmemory.arrayOffset(),
                                texture_width0 * texture_height0).slice();
                            */

	                    	for (int i = 0; i < texture_width0*texture_height0; i++) {
	                    		tmp_texture_buffer32[i] = mem.read32(texaddr+i*4);
	                    	}

                            final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
            			} else {
                            final_buffer = unswizzleTextureFromMemory(texaddr, 4);
            			}
            			break;
            		}

            		default: {
            			System.out.println("Unhandled texture storage " + texture_storage);
                        Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
                        break;
            		}
            	}

            	// Upload texture to openGL
            	// TODO: Write a texture cache :)
            	gl.glBindTexture  (GL.GL_TEXTURE_2D, gl_texture_id[0]);
            	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, tex_min_filter);
            	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, tex_mag_filter);
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, textureByteAlignment);
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);   // ROW_LENGTH = width

            	int texture_format = texture_type == GL.GL_UNSIGNED_SHORT_5_6_5_REV ? GL.GL_RGB : GL.GL_RGBA;

            	gl.glTexImage2D  (	GL.GL_TEXTURE_2D,
            						0,
            						texture_format,
            						texture_width0, texture_height0,
            						0,
            						texture_format,
            						texture_type,
            						final_buffer);
            	break;
            }

            case TFLT: {
            	log ("sceGuTexFilter(min, mag)");

            	switch ((normalArgument>>8) & 0xFF)
            	{
	            	case TFLT_MAGNIFYING_FILTER_NEAREST: {
	            		tex_mag_filter = GL.GL_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_LINEAR: {
	            		tex_mag_filter = GL.GL_LINEAR;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_NEAREST_NEAREST: {
	            		tex_mag_filter = GL.GL_NEAREST_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_NEAREST_LINEAR: {
	            		tex_mag_filter = GL.GL_NEAREST_MIPMAP_LINEAR;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_LINEAR_NEAREST: {
	            		tex_mag_filter = GL.GL_LINEAR_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_LINEAR_LINEAR: {
	            		tex_mag_filter = GL.GL_LINEAR_MIPMAP_LINEAR;
	            		break;
	            	}

	            	default: {
	            		log ("Unknown magnifiying filter " + ((normalArgument>>8) & 0xFF));
	            		break;
	            	}
            	}

            	switch (normalArgument & 0xFF)
            	{
	            	case TFLT_MAGNIFYING_FILTER_NEAREST: {
	            		tex_min_filter = GL.GL_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_LINEAR: {
	            		tex_min_filter = GL.GL_LINEAR;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_NEAREST_NEAREST: {
	            		tex_min_filter = GL.GL_NEAREST_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_NEAREST_LINEAR: {
	            		tex_min_filter = GL.GL_NEAREST_MIPMAP_LINEAR;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_LINEAR_NEAREST: {
	            		tex_min_filter = GL.GL_LINEAR_MIPMAP_NEAREST;
	            		break;
	            	}
	            	case TFLT_MAGNIFYING_FILTER_MIPMAP_LINEAR_LINEAR: {
	            		tex_min_filter = GL.GL_LINEAR_MIPMAP_LINEAR;
	            		break;
	            	}

	            	default: {
	            		log ("Unknown minimizing filter " + (normalArgument & 0xFF));
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
            	log ("sceGuTexOffset(float u, X)");
            	break;
            }

            case VOFFSET: {
            	tex_translate_y = floatArgument;
            	log ("sceGuTexOffset(X, float v)");
            	break;
            }

            case USCALE: {
            	tex_scale_x = floatArgument;
            	log (String.format("sceGuTexScale(u=%.2f, X)", tex_scale_x));
            	break;
            }
            case VSCALE: {
            	tex_scale_y = floatArgument;
                log (String.format("sceGuTexScale(X, v=%.2f)", tex_scale_y));
            	break;
            }

            case TMAP: {
            	log ("sceGuTexMapMode(mode, X, X)");
            	tex_map_mode = normalArgument & 3;
            	break;
            }

            case TEXTURE_ENV_MAP_MATRIX: {
            	log ("sceGuTexMapMode(X, column1, column2)");

            	if (normalArgument != 0) {
            		int column0 =  normalArgument     & 0xFF,
            			column1 = (normalArgument>>8) & 0xFF;

            		for (int i = 0; i < 3; i++) {
            			tex_envmap_matrix [i+0] = light_pos[column0][i];
            			tex_envmap_matrix [i+4] = light_pos[column1][i];
            		}
            	}
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
           			default: VideoEngine.log.warn("Unimplemented tfunc mode");
           		}
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, env_mode);
           		// TODO : check this
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, (normalArgument & 0x100) == 0 ? GL.GL_PREVIOUS : GL.GL_TEXTURE);
           		log("sceGuTexFunc");
           		/*log(String.format("sceGuTexFunc mode %08X", normalArgument)
           				+ (((normalArgument & 0x10000) != 0) ? " SCALE" : "")
           				+ (((normalArgument & 0x100) != 0) ? " ALPHA" : ""));*/
            	break;

            case TEC:
            	tex_env_color[0] = ((normalArgument      ) & 255) / 255.f;
            	tex_env_color[1] = ((normalArgument >>  8) & 255) / 255.f;
            	tex_env_color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	tex_env_color[3] = 1.f;
            	gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, tex_env_color, 0);
            	log("tec");
            	break;

            /*
             *
             */
            case XSCALE:
                log("sceGuViewport width = " + (floatArgument * 2));
                break;
            case YSCALE:
                log("sceGuViewport height = " + (- floatArgument * 2));
                break;

            // sceGuViewport cx/cy, can we discard these settings? it's only for clipping?
            case XPOS:
                log("sceGuViewport cx = " + floatArgument);
                break;
            case YPOS:
                log("sceGuViewport cy = " + floatArgument);
                break;

            case ZPOS:
                log(helper.getCommandString(ZPOS), floatArgument);
                break;
            // sceGuOffset, can we discard these settings? it's only for clipping?
            case OFFSETX:
                log("sceGuOffset x = " + (normalArgument >> 4));
                break;
            case OFFSETY:
                log("sceGuOffset y = " + (normalArgument >> 4));
                break;

            case FBP:
                // assign or OR lower 24-bits?
                fbp = normalArgument;
                break;
            case FBW:
                fbp &= 0xffffff;
                fbp |= (normalArgument << 8) & 0xff000000;
                fbw = (normalArgument) & 0xffff;
                log("fbp=" + Integer.toHexString(fbp) + ", fbw=" + fbw);
                jpcsp.HLE.pspdisplay.getInstance().hleDisplaySetGeBuf(fbp, fbw, psm);
                break;

            case ZBP:
                // assign or OR lower 24-bits?
                zbp = normalArgument;
                break;
            case ZBW:
                zbp &= 0xffffff;
                zbp |= (normalArgument << 8) & 0xff000000;
                zbw = (normalArgument) & 0xffff;
                log("zbp=" + Integer.toHexString(zbp) + ", zbw=" + zbw);
                break;

            case PSM:
                psm = normalArgument;
                log("psm=" + normalArgument);
                break;

            case PRIM:
            {
                int[] mapping = new int[] { GL.GL_POINTS, GL.GL_LINES, GL.GL_LINE_STRIP, GL.GL_TRIANGLES, GL.GL_TRIANGLE_STRIP, GL.GL_TRIANGLE_FAN };
                int numberOfVertex = normalArgument & 0xFFFF;
                int type = ((normalArgument >> 16) & 0x7);

                // Logging
                switch (type) {
                    case PRIM_POINT:
                        log(helper.getCommandString(PRIM) + " point " + numberOfVertex + "x");
                        break;
                    case PRIM_LINE:
                        log(helper.getCommandString(PRIM) + " line " + (numberOfVertex / 2) + "x");
                        break;
                    case PRIM_LINES_STRIPS:
                        log(helper.getCommandString(PRIM) + " lines_strips " + (numberOfVertex - 1) + "x");
                        break;
                    case PRIM_TRIANGLE:
                        log(helper.getCommandString(PRIM) + " triangle " + (numberOfVertex / 3) + "x");
                        break;
                    case PRIM_TRIANGLE_STRIPS:
                        log(helper.getCommandString(PRIM) + " triangle_strips " + (numberOfVertex - 2) + "x");
                        break;
                    case PRIM_TRIANGLE_FANS:
                        log(helper.getCommandString(PRIM) + " triangle_fans " + (numberOfVertex - 2) + "x");
                        break;
                    case PRIM_SPRITES:
                        log(helper.getCommandString(PRIM) + " sprites " + (numberOfVertex / 2) + "x");
                        break;
                }

                /*
                 * Defer transformations until primitive rendering
                 */
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPushMatrix ();
                gl.glLoadIdentity();

                if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
                	gl.glLoadMatrixf(proj_uploaded_matrix, 0);
                else
                	gl.glOrtho(0.0, 480, 272, 0, -1.0, 1.0);

                /*
                 * Apply texture transforms
                 */
                gl.glMatrixMode(GL.GL_TEXTURE);
                gl.glPushMatrix ();
                gl.glLoadIdentity();
                gl.glTranslatef(tex_translate_x, tex_translate_y, 0.f);
                if (transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD)
                    gl.glScalef(tex_scale_x, tex_scale_y, 1.f);

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

                // HACK: If we don't have a material set up, and have colors per vertex, this will
                // override materials, so at least we'll see something, otherwise it would be black
                if (vinfo.color != 0)
                	gl.glEnable(GL.GL_COLOR_MATERIAL);

                Memory mem = Memory.getInstance();
                switch (type) {
                    case PRIM_POINT:
                    case PRIM_LINE:
                    case PRIM_LINES_STRIPS:
                    case PRIM_TRIANGLE:
                    case PRIM_TRIANGLE_STRIPS:
                    case PRIM_TRIANGLE_FANS:
                        gl.glBegin(mapping[type]);
                            for (int i = 0; i < numberOfVertex; i++) {
                                int addr = vinfo.getAddress(mem, i);
                                VertexState v = vinfo.readVertex(mem, addr);
                                if (vinfo.texture  != 0)
                                	if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD)
                                		gl.glTexCoord2f(v.u / texture_width0, v.v / texture_height0);
                                	else
                                		gl.glTexCoord2f(v.u, v.v);
                                if (vinfo.color    != 0) gl.glColor4f(v.r, v.g, v.b, v.a);
                                if (vinfo.normal   != 0) gl.glNormal3f(v.nx, v.ny, v.nz);
                                if (vinfo.position != 0) {
                                	if(vinfo.weight != 0)
                                		doSkinning(vinfo, v);
                                    gl.glVertex3f(v.px, v.py, v.pz);
                                }
                            }
                        gl.glEnd();
                        break;

                    case PRIM_SPRITES:
                        gl.glPushAttrib(GL.GL_ENABLE_BIT);
                        gl.glDisable(GL.GL_CULL_FACE);
                        gl.glBegin(GL.GL_QUADS);
                            for (int i = 0; i < numberOfVertex; i += 2) {
                                int addr1 = vinfo.getAddress(mem, i);
                                int addr2 = vinfo.getAddress(mem, i + 1);
                                VertexState v1 = vinfo.readVertex(mem, addr1);
                                VertexState v2 = vinfo.readVertex(mem, addr2);

                                // V1
                                if (vinfo.normal   != 0) gl.glNormal3f(v1.nx, v1.ny, v1.nz);
                                if (vinfo.color    != 0) gl.glColor4f(v2.r, v2.g, v2.b, v2.a); // color from v2 not v1

                                if (vinfo.texture  != 0)
                                	if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD)
                                		gl.glTexCoord2f(v1.u / texture_width0, v1.v / texture_height0);
                                	else
                                		gl.glTexCoord2f(v1.u, v1.v);
                                if (vinfo.position != 0) gl.glVertex3f(v1.px, v1.py, v1.pz);

                                if (vinfo.texture  != 0)
                                	if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD)
                                		gl.glTexCoord2f(v2.u / texture_width0, v1.v / texture_height0);
                                	else
                                		gl.glTexCoord2f(v2.u, v1.v);
                                if (vinfo.position != 0) gl.glVertex3f(v2.px, v1.py, v1.pz);

                                // V2
                                if (vinfo.normal   != 0) gl.glNormal3f(v2.nx, v2.ny, v2.nz);
                                if (vinfo.color    != 0) gl.glColor4f(v2.r, v2.g, v2.b, v2.a);

                                if (vinfo.texture  != 0)
                                	if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD)
                                		gl.glTexCoord2f(v2.u / texture_width0, v2.v / texture_height0);
                                	else
                                		gl.glTexCoord2f(v2.u, v2.v);
                                if (vinfo.position != 0) gl.glVertex3f(v2.px, v2.py, v1.pz);

                                if (vinfo.texture  != 0)
                                	if(transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD)
                                		gl.glTexCoord2f(v1.u / texture_width0, v2.v / texture_height0);
                                	else
                                		gl.glTexCoord2f(v1.u, v2.v);
                                if (vinfo.position != 0) gl.glVertex3f(v1.px, v2.py, v1.pz);
                            }
                        gl.glEnd();
                        gl.glPopAttrib();
                        break;
                }

                switch (tex_map_mode) {
                	case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
		            	gl.glDisable (GL.GL_TEXTURE_GEN_S);
		            	gl.glDisable (GL.GL_TEXTURE_GEN_T);
		            	break;
		            }
		        }

                gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, 1.0f);
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
           		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);

                if (vinfo.color != 0)
                	gl.glDisable	(GL.GL_COLOR_MATERIAL);

                gl.glPopMatrix 	();
                gl.glMatrixMode	(GL.GL_TEXTURE);
                gl.glPopMatrix 	();
                gl.glMatrixMode	(GL.GL_PROJECTION);
                gl.glPopMatrix 	();
                gl.glMatrixMode	(GL.GL_MODELVIEW);

                break;
            }

            case ALPHA: {

                int blend_mode = GL.GL_FUNC_ADD;
                int src = getBlendSrc( normalArgument        & 0xF);
                int dst = getBlendOp((normalArgument >> 4 ) & 0xF);
                int op  =            (normalArgument >> 8 ) & 0xF;

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
            	gl.glBlendFunc(src, dst);

            	log ("sceGuBlendFunc(int op, int src, int dest, X, X)");
            	break;
            }

            case SHADE: {
                int SETTED_MODEL = (normalArgument != 0) ? GL.GL_SMOOTH : GL.GL_FLAT;
                gl.glShadeModel(SETTED_MODEL);
                log(helper.getCommandString(SHADE) + " " + ((normalArgument != 0) ? "smooth" : "flat"));
                break;
            }

            case FFACE: {
                int frontFace = (normalArgument != 0) ? GL.GL_CW : GL.GL_CCW;
                gl.glFrontFace(frontFace);
                log(helper.getCommandString(FFACE) + " " + ((normalArgument != 0) ? "clockwise" : "counter-clockwise"));
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
	            	log("FCOL");
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
                if(normalArgument != 0) {
                    gl.glEnable(GL.GL_BLEND);
                    log("sceGuEnable(GU_BLEND)");
                }
                else {
                    gl.glDisable(GL.GL_BLEND);
                    log("sceGuDisable(GU_BLEND)");
                }
                break;
             case ATE:
	            	if(normalArgument != 0) {
	            		gl.glEnable(GL.GL_ALPHA_TEST);
	            		log("sceGuEnable(GL_ALPHA_TEST)");
	            	}
	            	else {
	            		 gl.glDisable(GL.GL_ALPHA_TEST);
	                     log("sceGuDisable(GL_ALPHA_TEST)");
	            	}
	            	break;
            case ZTE:
                if(normalArgument != 0) {
                    gl.glEnable(GL.GL_DEPTH_TEST);
                    log("sceGuEnable(GU_DEPTH_TEST)");
                }
                else {
                    gl.glDisable(GL.GL_DEPTH_TEST);
                    log("sceGuDisable(GU_DEPTH_TEST)");
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
                int npc = (normalArgument | actualList.base) & 0xFFFFFFFC;
                //I guess it must be unsign as psp player emulator
                log(helper.getCommandString(JUMP) + " old PC:" + String.format("%08x", actualList.pc)
                    + " new PC:" + String.format("%08x", npc));
                actualList.pc = npc;
                break;
            }
            case CALL:
            {
                actualList.stack[actualList.stackIndex++] = actualList.pc;
                int npc = (normalArgument | actualList.base) & 0xFFFFFFFC;
                log(helper.getCommandString(CALL) + " old PC:" + String.format("%08x", actualList.pc)
                    + " new PC:" + String.format("%08x", npc));
                actualList.pc = npc;
                break;
            }
            case RET:
            {
                int npc = actualList.stack[--actualList.stackIndex];
                log(helper.getCommandString(RET) + " old PC:" + String.format("%08x", actualList.pc)
                    + " new PC:" + String.format("%08x", npc));
                actualList.pc = npc;
                break;
            }

            case ZMSK: {
            	// NOTE: PSP depth mask as 1 is meant to avoid depth writes,
            	//		on pc it's the opposite
            	gl.glDepthMask(normalArgument == 1 ? false : true);

            	log ("sceGuDepthMask(disableWrites)");
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
	            	
	            	gl.glAlphaFunc(func,floatArgument);
	            	log ("sceGuAlphaFunc(" + func + "," + floatArgument + ")");
	            	
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

            	gl.glStencilFunc (func, ((normalArgument>>8) & 0xff), (normalArgument>>16) & 0xff);

            	log ("sceGuStencilFunc(func, ref, mask)");
            	break;
            }

            case NEARZ : {
	            	nearZ = ( float )( int )(  short )normalArgument;
	            }
	            break;
	            
	        case FARZ : {
	            	if(nearZ > ( float )( int )(  short )normalArgument)
	            	{
	            		farZ = nearZ;
	            		nearZ = ( float )( int )(  short )normalArgument;
	            	}
	            	else
	            		farZ = ( float )( int )(  short )normalArgument;
	            	
	            	gl.glDepthRange(nearZ, farZ);
	            	log ("sceGuDepthRange("+ nearZ + " ," + farZ + ")");
	            }
	            break;

            case SOP: {

            	int fail  = getStencilOp  (normalArgument & 0xFF);
            	int zfail = getStencilOp ((normalArgument>> 8) & 0xFF);
            	int zpass = getStencilOp ((normalArgument>>16) & 0xFF);

            	gl.glStencilOp(fail, zfail, zpass);

            	break;
            }

            case CLEAR:
                if ((normalArgument & 0x1)==0) {
                    // set clear color, actarus/sam
                    gl.glClearColor(vinfo.lastVertex.r, vinfo.lastVertex.g, vinfo.lastVertex.b, vinfo.lastVertex.a);
                	gl.glClear(clearFlags);
		            log(String.format("guclear r=%.1f g=%.1f b=%.1f a=%.1f", vinfo.lastVertex.r, vinfo.lastVertex.g, vinfo.lastVertex.b, vinfo.lastVertex.a));
				} else {
				     clearFlags = 0;
				     if ((normalArgument & 0x100)!=0) clearFlags |= GL.GL_COLOR_BUFFER_BIT; // target
				     if ((normalArgument & 0x200)!=0) clearFlags |= GL.GL_STENCIL_BUFFER_BIT; // stencil/alpha
				     if ((normalArgument & 0x400)!=0) clearFlags |= GL.GL_DEPTH_BUFFER_BIT; // zbuffer
				     log("setting clear flags");
                }
                break;
            case NOP:
                log(helper.getCommandString(NOP));
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
            	textureTx_sourceAddress = normalArgument;
            	break;

            case TRXSBW:
            	textureTx_sourceAddress |= (normalArgument << 8) & 0xFF000000;
            	textureTx_sourceLineWidth = normalArgument & 0x0000FFFF;
            	break;

            case TRXDBP:
            	textureTx_destinationAddress = normalArgument;
            	break;

            case TRXDBW:
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

                log(helper.getCommandString(TRXKICK) + " from 0x" + Integer.toHexString(textureTx_sourceAddress) + "(" + textureTx_sx + "," + textureTx_sy + ") to 0x" + Integer.toHexString(textureTx_destinationAddress) + "(" + textureTx_dx + "," + textureTx_dy + "), width=" + textureTx_width + ", height=" + textureTx_height);
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
	                    log("Unsupported 16bit for video command [ " + helper.getCommandString(command(instruction)) + " ]");
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

	            	gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, bpp);
	            	gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, lineWidth);

	            	gl.glMatrixMode(GL.GL_PROJECTION);
	            	gl.glPushMatrix();
	            	gl.glLoadIdentity();
	            	gl.glOrtho(0, 480, 272, 0, -1, 1);
	                gl.glMatrixMode(GL.GL_MODELVIEW);
	                gl.glPushMatrix ();
	                gl.glLoadIdentity();

                	ByteBuffer buffer = ByteBuffer.wrap(
                            Memory.getInstance().mainmemory.array(),
                            Memory.getInstance().mainmemory.arrayOffset() + textureTx_sourceAddress - MemoryMap.START_RAM,
                            lineWidth * height * bpp).slice();

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

            default:
                log("Unknown/unimplemented video command [ " + helper.getCommandString(command(instruction)) + " ]");
        }

    }

    private void doSkinning(VertexInfo vinfo, VertexState v) {
    	float x = 0, y = 0, z = 0;
    	float nx = 0, ny = 0, nz = 0;
		for(int i = 0; i < vinfo.skinningWeightCount; ++i) {
			if(v.boneWeights[i] != 0.f) {

				x += (	v.px * 	bone_uploaded_matrix[i][0]
				     + 	v.py * 	bone_uploaded_matrix[i][3]
				     + 	v.pz * 	bone_uploaded_matrix[i][6]
				     + 			bone_uploaded_matrix[i][9]) * v.boneWeights[i];

				y += (	v.px * 	bone_uploaded_matrix[i][1]
				     + 	v.py * 	bone_uploaded_matrix[i][4]
				     + 	v.pz * 	bone_uploaded_matrix[i][7]
				     + 			bone_uploaded_matrix[i][10]) * v.boneWeights[i];

				z += (	v.px * 	bone_uploaded_matrix[i][2]
				     + 	v.py * 	bone_uploaded_matrix[i][5]
				     + 	v.pz * 	bone_uploaded_matrix[i][8]
				     + 			bone_uploaded_matrix[i][11]) * v.boneWeights[i];

				// Normals shouldn't be translated :)
				nx += (	v.nx * bone_uploaded_matrix[i][0]
				   + 	v.ny * bone_uploaded_matrix[i][3]
				   +	v.nz * bone_uploaded_matrix[i][6]) * v.boneWeights[i];

				ny += (	v.nx * bone_uploaded_matrix[i][1]
				   + 	v.ny * bone_uploaded_matrix[i][4]
				   + 	v.nz * bone_uploaded_matrix[i][7]) * v.boneWeights[i];

				nz += (	v.nx * bone_uploaded_matrix[i][2]
				   + 	v.ny * bone_uploaded_matrix[i][5]
				   + 	v.nz * bone_uploaded_matrix[i][8]) * v.boneWeights[i];
			}
		}

		v.px = x;	v.py = y;	v.pz = z;

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

		v.nx = nx;	v.ny = ny;	v.nz = nz;
	}

	public void setFullScreenShoot(boolean b) {
    }

    public void setLineSize(int linesize) {
    }

    public void setMode(int mode) {
    }

    public void setPixelSize(int pixelsize) {
    }

    public void setup(int mode, int xres, int yres) {
    }

    public void show() {
    }

    public void waitVBlank() {
    }

    private void log(String commandString, float floatArgument) {
        log(commandString+SPACE+floatArgument);
    }

    private void log(String commandString, int value) {
        log(commandString+SPACE+value);
    }

    private void log(String commandString, float[] matrix) {
        for (int y = 0; y < 4; y++) {
            log(commandString+SPACE+String.format("%.1f %.1f %.1f %.1f", matrix[0 + y * 4], matrix[1 + y * 4], matrix[2 + y * 4], matrix[3 + y * 4]));
        }
    }

    private void setHardwareAcc(boolean hardwareAccelerate) {
    }
}

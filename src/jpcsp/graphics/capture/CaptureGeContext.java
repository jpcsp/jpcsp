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
package jpcsp.graphics.capture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;

public class CaptureGeContext {
	public static Logger log = CaptureManager.log;
	private GeContext context;
    private int region_x1, region_y1, region_x2, region_y2;
    private int region_width, region_height;
    private int scissor_x1, scissor_y1, scissor_x2, scissor_y2;
    private int scissor_width, scissor_height;
    private int offset_x, offset_y;
    public int viewport_width, viewport_height;
    public int viewport_cx, viewport_cy;
	private boolean alphaTestFlag;
	private boolean depthTestFlag;
	private boolean scissorTestFlag;
	private boolean stencilTestFlag;
	private boolean blendFlag;
	private boolean cullFaceFlag;
	private boolean ditherFlag;
	private boolean fogFlag;
	private boolean clipPlanesFlag;
	private boolean textureFlag;
	private boolean lightingFlag;
	private boolean lightFlags[] = new boolean[VideoEngine.NUM_LIGHTS];
	private boolean lineSmoothFlag;
	private boolean patchCullFaceFlag;
	private boolean colorTestFlag;
	private boolean colorLogicOpFlag;
	private boolean faceNormalReverseFlag;
	private boolean patchFaceFlag;
	private boolean fragment2xFlag;
	private boolean reColorMaterial;
	private boolean reTextureGenS;
	private boolean reTextureGenT;
	private final float morph_weigth[] = new float[8];

	private CaptureGeContext() {
	}

	public CaptureGeContext(GeContext context) {
		this.context = context;
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(CaptureManager.PACKET_TYPE_GE_CONTEXT);
		out.writeInt(context.region_x1);
		out.writeInt(context.region_y1);
		out.writeInt(context.region_x2);
		out.writeInt(context.region_y2);
		out.writeInt(context.region_width);
		out.writeInt(context.region_height);
		out.writeInt(context.scissor_x1);
		out.writeInt(context.scissor_y1);
		out.writeInt(context.scissor_x2);
		out.writeInt(context.scissor_y2);
		out.writeInt(context.scissor_width);
		out.writeInt(context.scissor_height);
		out.writeInt(context.offset_x);
		out.writeInt(context.offset_y);
		out.writeInt(context.viewport_width);
		out.writeInt(context.viewport_height);
		out.writeInt(context.viewport_cx);
		out.writeInt(context.viewport_cy);
		out.writeBoolean(context.alphaTestFlag.isEnabled());
		out.writeBoolean(context.depthTestFlag.isEnabled());
		out.writeBoolean(context.scissorTestFlag.isEnabled());
		out.writeBoolean(context.stencilTestFlag.isEnabled());
		out.writeBoolean(context.blendFlag.isEnabled());
		out.writeBoolean(context.cullFaceFlag.isEnabled());
		out.writeBoolean(context.ditherFlag.isEnabled());
		out.writeBoolean(context.fogFlag.isEnabled());
		out.writeBoolean(context.clipPlanesFlag.isEnabled());
		out.writeBoolean(context.textureFlag.isEnabled());
		out.writeBoolean(context.lightingFlag.isEnabled());
		for (int i = 0; i < lightFlags.length; i++) {
			out.writeBoolean(context.lightFlags[i].isEnabled());
		}
		out.writeBoolean(context.lineSmoothFlag.isEnabled());
		out.writeBoolean(context.patchCullFaceFlag.isEnabled());
		out.writeBoolean(context.colorTestFlag.isEnabled());
		out.writeBoolean(context.colorLogicOpFlag.isEnabled());
		out.writeBoolean(context.faceNormalReverseFlag.isEnabled());
		out.writeBoolean(context.patchFaceFlag.isEnabled());
		out.writeBoolean(context.fragment2xFlag.isEnabled());
		out.writeBoolean(context.reColorMaterial.isEnabled());
		out.writeBoolean(context.reTextureGenS.isEnabled());
		out.writeBoolean(context.reTextureGenT.isEnabled());
		for (int i = 0; i < morph_weigth.length; i++) {
			out.writeFloat(morph_weigth[i]);
		}
	}

	public static CaptureGeContext read(DataInputStream in) throws IOException {
		CaptureGeContext captureGeContext = new CaptureGeContext();
		captureGeContext.region_x1 = in.readInt();
		captureGeContext.region_y1 = in.readInt();
		captureGeContext.region_x2 = in.readInt();
		captureGeContext.region_y2 = in.readInt();
		captureGeContext.region_width = in.readInt();
		captureGeContext.region_height = in.readInt();
		captureGeContext.scissor_x1 = in.readInt();
		captureGeContext.scissor_y1 = in.readInt();
		captureGeContext.scissor_x2 = in.readInt();
		captureGeContext.scissor_y2 = in.readInt();
		captureGeContext.scissor_width = in.readInt();
		captureGeContext.scissor_height = in.readInt();
		captureGeContext.offset_x = in.readInt();
		captureGeContext.offset_y = in.readInt();
		captureGeContext.viewport_width = in.readInt();
		captureGeContext.viewport_height = in.readInt();
		captureGeContext.viewport_cx = in.readInt();
		captureGeContext.viewport_cy = in.readInt();
		captureGeContext.alphaTestFlag = in.readBoolean();
		captureGeContext.depthTestFlag = in.readBoolean();
		captureGeContext.scissorTestFlag = in.readBoolean();
		captureGeContext.stencilTestFlag = in.readBoolean();
		captureGeContext.blendFlag = in.readBoolean();
		captureGeContext.cullFaceFlag = in.readBoolean();
		captureGeContext.ditherFlag = in.readBoolean();
		captureGeContext.fogFlag = in.readBoolean();
		captureGeContext.clipPlanesFlag = in.readBoolean();
		captureGeContext.textureFlag = in.readBoolean();
		captureGeContext.lightingFlag = in.readBoolean();
		for (int i = 0; i < captureGeContext.lightFlags.length; i++) {
			captureGeContext.lightFlags[i] = in.readBoolean();
		}
		captureGeContext.lineSmoothFlag = in.readBoolean();
		captureGeContext.patchCullFaceFlag = in.readBoolean();
		captureGeContext.colorTestFlag = in.readBoolean();
		captureGeContext.colorLogicOpFlag = in.readBoolean();
		captureGeContext.faceNormalReverseFlag = in.readBoolean();
		captureGeContext.patchFaceFlag = in.readBoolean();
		captureGeContext.fragment2xFlag = in.readBoolean();
		captureGeContext.reColorMaterial = in.readBoolean();
		captureGeContext.reTextureGenS = in.readBoolean();
		captureGeContext.reTextureGenT = in.readBoolean();
		for (int i = 0; i < captureGeContext.morph_weigth.length; i++) {
			captureGeContext.morph_weigth[i] = in.readFloat();
		}

        if (log.isDebugEnabled()) {
        	log.debug(String.format("CaptureGeContext viewport_cx=%d, viewport_cy=%d, viewport_width=%d, viewport_height=%d", captureGeContext.viewport_cx, captureGeContext.viewport_cy, captureGeContext.viewport_width, captureGeContext.viewport_height));
        }

        return captureGeContext;
	}

	public void commit(IRenderingEngine re, GeContext context) {
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
		context.alphaTestFlag.setEnabled(re, alphaTestFlag);
		context.depthTestFlag.setEnabled(re, depthTestFlag);
		context.scissorTestFlag.setEnabled(re, scissorTestFlag);
		context.stencilTestFlag.setEnabled(re, stencilTestFlag);
		context.blendFlag.setEnabled(re, blendFlag);
		context.cullFaceFlag.setEnabled(re, cullFaceFlag);
		context.ditherFlag.setEnabled(re, ditherFlag);
		context.fogFlag.setEnabled(re, fogFlag);
		context.clipPlanesFlag.setEnabled(re, clipPlanesFlag);
		context.textureFlag.setEnabled(re, textureFlag);
		context.lightingFlag.setEnabled(re, lightingFlag);
		for (int i = 0; i < lightFlags.length; i++) {
			context.lightFlags[i].setEnabled(re, lightFlags[i]);
		}
		context.lineSmoothFlag.setEnabled(re, lineSmoothFlag);
		context.patchCullFaceFlag.setEnabled(re, patchCullFaceFlag);
		context.colorTestFlag.setEnabled(re, colorTestFlag);
		context.colorLogicOpFlag.setEnabled(re, colorLogicOpFlag);
		context.faceNormalReverseFlag.setEnabled(re, faceNormalReverseFlag);
		context.patchFaceFlag.setEnabled(re, patchFaceFlag);
		context.fragment2xFlag.setEnabled(re, fragment2xFlag);
		context.reColorMaterial.setEnabled(re, reColorMaterial);
		context.reTextureGenS.setEnabled(re, reTextureGenS);
		context.reTextureGenT.setEnabled(re, reTextureGenT);
		System.arraycopy(morph_weigth, 0, context.morph_weight, 0, morph_weigth.length);
	}
}

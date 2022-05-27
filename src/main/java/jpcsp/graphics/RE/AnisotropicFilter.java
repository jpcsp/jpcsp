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

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

/**
 * @author Aredo, gid15
 *
 * Implements a texture anisotropic filter.
 */
public class AnisotropicFilter extends BaseRenderingEngineProxy {
	// When the anisotropic filter is active, map the magnification filter
	// to TFLT_LINEAR/TFLT_LINEAR_MIPMAP_LINEAR
	protected static final int[] anisotropicMipmapMagFilter = {
		GeCommands.TFLT_LINEAR,                // TFLT_NEAREST
		GeCommands.TFLT_LINEAR,                // TFLT_LINEAR
		GeCommands.TFLT_UNKNOW1,               // TFLT_UNKNOW1
		GeCommands.TFLT_UNKNOW2,               // TFLT_UNKNOW2
		GeCommands.TFLT_LINEAR_MIPMAP_LINEAR,  // TFLT_NEAREST_MIPMAP_NEAREST
		GeCommands.TFLT_LINEAR_MIPMAP_LINEAR,  // TFLT_LINEAR_MIPMAP_NEAREST
		GeCommands.TFLT_LINEAR_MIPMAP_LINEAR,  // TFLT_NEAREST_MIPMAP_LINEAR
		GeCommands.TFLT_LINEAR_MIPMAP_LINEAR   // TFLT_LINEAR_MIPMAP_LINEAR
	};
	private float maxTextureAnisotropy;
	private float textureAnisotropy;
	private boolean useTextureAnisotropicFilter;

	public AnisotropicFilter(IRenderingEngine proxy) {
		super(proxy);
	}

	public void setDefaultTextureAnisotropy(float value) {
		textureAnisotropy = value;
	}

	@Override
	public void startDisplay() {
		useTextureAnisotropicFilter = VideoEngine.getInstance().isUseTextureAnisotropicFilter();
		super.startDisplay();
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		maxTextureAnisotropy = re.getMaxTextureAnisotropy();
		textureAnisotropy = maxTextureAnisotropy;
		super.setRenderingEngine(re);
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		if (useTextureAnisotropicFilter) {
			re.setTextureAnisotropy(textureAnisotropy);
			super.setTextureMipmapMagFilter(anisotropicMipmapMagFilter[filter]);
		} else {
			super.setTextureMipmapMagFilter(filter);
		}
	}
}

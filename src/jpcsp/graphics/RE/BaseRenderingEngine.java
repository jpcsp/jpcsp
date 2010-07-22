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

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 * Base class for a RenderingEngine.
 * This class offers convenience functions:
 * - the subclass is free to implement
 *      setMatrixElements()
 *   or setXXXMatrixElements()
 *   This class performs the mapping between the 2 equivalent sets of methods.
 * 
 * - the subclass is free to implement
 *      setMaterialColor()
 *   or setMaterialXXXColor()
 *   This class performs the mapping between the 2 equivalent sets of methods.
 *
 * - the subclass is free to implement
 *      setLightColor()
 *   or setLightXXXColor()
 *   This class performs the mapping between the 2 equivalent sets of methods.
 */
public abstract class BaseRenderingEngine implements IRenderingEngine {
	protected final static Logger log = VideoEngine.log;
	protected IRenderingEngine re = this;
	protected GeContext context;

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		this.re = re;
	}

	@Override
	public void setGeContext(GeContext context) {
		this.context = context;
	}

	//
	// Equivalence between setMatrixElements() and setXXXMatrixElements()
	//
	@Override
	public void setMatrixElements(int type, float[] values) {
		switch (type) {
			case GU_PROJECTION:
				setProjectionMatrixElements(values);
				break;
			case GU_MODEL:
				setModelMatrixElements(values);
				break;
			case GU_VIEW:
				setViewMatrixElements(values);
				break;
			case GU_TEXTURE:
				setTextureMatrixElements(values);
				break;
		}
	}

	@Override
	public void setProjectionMatrixElements(float[] values) {
		setMatrixElements(GU_PROJECTION, values);
	}

	@Override
	public void setViewMatrixElements(float[] values) {
		setMatrixElements(GU_VIEW, values);
	}

	@Override
	public void setModelMatrixElements(float[] values) {
		setMatrixElements(GU_MODEL, values);
	}

	@Override
	public void setTextureMatrixElements(float[] values) {
		setMatrixElements(GU_TEXTURE, values);
	}

	//
	// Equivalence between setMaterialColor() and setMaterialXXXColor()
	//
	@Override
	public void setMaterialColor(int type, float[] color) {
		switch (type) {
			case RE_AMBIENT:
				setMaterialAmbientColor(color);
				break;
			case RE_EMISSIVE:
				setMaterialEmissiveColor(color);
				break;
			case RE_DIFFUSE:
				setMaterialDiffuseColor(color);
				break;
			case RE_SPECULAR:
				setMaterialSpecularColor(color);
				break;
		}
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		setMaterialColor(RE_AMBIENT, color);
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		setMaterialColor(RE_DIFFUSE, color);
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		setMaterialColor(RE_EMISSIVE, color);
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		setMaterialColor(RE_SPECULAR, color);
	}

	//
	// Equivalence between setLightColor() and setLightXXXColor()
	//
	@Override
	public void setLightColor(int type, int light, float[] color) {
		switch (type) {
			case RE_AMBIENT:
				setLightAmbientColor(light, color);
				break;
			case RE_DIFFUSE:
				setLightDiffuseColor(light, color);
				break;
			case RE_SPECULAR:
				setLightSpecularColor(light, color);
				break;
		}
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		setLightColor(RE_AMBIENT, light, color);
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		setLightColor(RE_DIFFUSE, light, color);
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		setLightColor(RE_SPECULAR, light, color);
	}
}

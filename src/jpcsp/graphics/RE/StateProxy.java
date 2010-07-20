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

import jpcsp.graphics.Uniforms;

/**
 * @author gid15
 * 
 * RenderingEngine Proxy class removing redundant calls.
 * E.g. calls setting multiple times the same value,
 * or calls with an invalid parameter (e.g. for unused shader uniforms).
 * This class implements no rendering logic, it just skips unnecessary calls.
 */
public class StateProxy extends BaseRenderingEngineProxy {
	protected boolean[] flags = new boolean[IRenderingEngine.RE_NUMBER_FLAGS];
	protected int maxUniformId;
	protected int[] uniformInt;
	protected int[][] uniformIntArray;
	protected float[] uniformFloat;
	protected float[][] uniformFloatArray;

	public StateProxy(IRenderingEngine proxy) {
		super(proxy);
		init();
	}

	protected void init() {
		maxUniformId = 0;
		for (Uniforms uniform : Uniforms.values()) {
			int id = uniform.getId();
			if (id > maxUniformId) {
				maxUniformId = id;
			}
		}

		uniformInt = new int[maxUniformId + 1];
		uniformFloat = new float[maxUniformId + 1];
		uniformIntArray = new int[maxUniformId + 1][];
		uniformFloatArray = new float[maxUniformId + 1][];
	}

	@Override
	public void disableFlag(int flag) {
		if (flags[flag]) {
			super.disableFlag(flag);
			flags[flag] = false;
		}
	}

	@Override
	public void enableFlag(int flag) {
		if (!flags[flag]) {
			super.enableFlag(flag);
			flags[flag] = true;
		}
	}

	@Override
	public void setUniform(int id, int value) {
		// An unused uniform as an id == -1
		if (id >= 0 && id <= maxUniformId) {
			if (uniformInt[id] != value) {
				super.setUniform(id, value);
				uniformInt[id] = value;
			}
		}
	}

	@Override
	public void setUniform(int id, float value) {
		if (id >= 0 && id <= maxUniformId) {
			if (uniformFloat[id] != value) {
				super.setUniform(id, value);
				uniformFloat[id] = value;
			}
		}
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[id];
			if (oldValues == null || oldValues.length != 2) {
				super.setUniform(id, value1, value2);
				uniformIntArray[id] = new int[] { value1, value2 };
			} else {
				if (oldValues[0] != value1 || oldValues[1] != value2) {
					super.setUniform(id, value1, value2);
					oldValues[0] = value1;
					oldValues[1] = value2;
				}
			}
		}
	}

	@Override
	public void setUniform3(int id, int[] values) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[id];
			if (oldValues == null || oldValues.length != 3) {
				super.setUniform3(id, values);
				oldValues = new int[3];
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				uniformIntArray[id] = oldValues;
			} else if (oldValues[0] != values[0] || oldValues[1] != values[1] || oldValues[2] != values[2]) {
				super.setUniform3(id, values);
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
			}
		}
	}

	@Override
	public void setUniform4(int id, int[] values) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[id];
			if (oldValues == null || oldValues.length != 4) {
				super.setUniform4(id, values);
				oldValues = new int[4];
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				oldValues[3] = values[3];
				uniformIntArray[id] = oldValues;
			} else if (oldValues[0] != values[0] || oldValues[1] != values[1] || oldValues[2] != values[2] || oldValues[3] != values[3]) {
				super.setUniform4(id, values);
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				oldValues[3] = values[3];
			}
		}
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		if (id >= 0 && id <= maxUniformId && count > 0) {
			float[] oldValues = uniformFloatArray[id];
			int length = count * 16;
			if (oldValues == null || oldValues.length < length) {
				super.setUniformMatrix4(id, count, values);
				oldValues = new float[length];
				System.arraycopy(values, 0, oldValues, 0, length);
				uniformFloatArray[id] = oldValues;
			} else {
				boolean differ = false;
				for (int i = 0; i < length; i++) {
					if (oldValues[i] != values[i]) {
						differ = true;
						break;
					}
				}

				if (differ) {
					super.setUniformMatrix4(id, count, values);
					System.arraycopy(values, 0, oldValues, 0, length);
				}
			}
		}
	}
}

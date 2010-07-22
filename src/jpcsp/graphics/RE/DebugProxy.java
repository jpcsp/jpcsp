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

/**
 * @author gid15
 *
 */
public class DebugProxy extends BaseRenderingEngineProxy {
	public DebugProxy(IRenderingEngine proxy) {
		super(proxy);
	}

	@Override
	public void enableFlag(int flag) {
		log.debug(String.format("enableFlag %s", context.flags.get(flag).toString()));
		super.enableFlag(flag);
	}

	@Override
	public void disableFlag(int flag) {
		log.debug(String.format("disableFlag %s", context.flags.get(flag).toString()));
		super.disableFlag(flag);
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		log.debug(String.format("setAlphaFunc func=%d, ref=0x%02X", func, ref));
		super.setAlphaFunc(func, ref);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		log.debug(String.format("setTextureFunc func=%d%s%s", func, alphaUsed ? " ALPHA" : "", colorDoubled ? " COLORx2" : ""));
		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		log.debug(String.format("setBlendFunc src=%d, dst=%d", src, dst));
		super.setBlendFunc(src, dst);
	}

	@Override
	public void setBlendEquation(int mode) {
		log.debug(String.format("setBlendEquation mode=%d", mode));
		super.setBlendEquation(mode);
	}
}

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
package libchdr.codec;

import static libchdr.ChdHeader.ChdError.CHDERR_NONE;

import libchdr.Chd.CodecData;
import libchdr.ChdHeader.ChdError;

public abstract class BaseCodec implements ICodecInterface {
	@Override
	public ChdError init(CodecData codec, int hunkbytes) {
		return CHDERR_NONE;
	}

	@Override
	public void free(CodecData codec) {
	}

	@Override
	public ChdError decompress(CodecData codec, byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen) {
		return decompress(src, srcOffset, complen, dest, destOffset, destlen);
	}

	@Override
	public ChdError config(CodecData codec, int param, Object config) {
		return CHDERR_NONE;
	}

	protected abstract ChdError decompress(byte[] src, int srcOffset, int complen, byte[] dest, int destOffset, int destlen);
}

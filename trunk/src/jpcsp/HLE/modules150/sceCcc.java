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
package jpcsp.HLE.modules150;

import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

@HLELogging
public class sceCcc extends HLEModule {
    public static Logger log = Modules.getLogger("sceCcc");

	@Override
	public String getName() {
		return "sceCcc";
	}

	protected static final Charset charsetUTF8 = Charset.forName("UTF-8");
	protected static final Charset charsetUTF16 = Charset.forName("UTF-16LE");
	protected static final Charset charsetSJIS = Charset.forName("Shift_JIS");

	protected static byte[] addByteToArray(byte[] array, byte b) {
		byte[] newArray = new byte[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = b;

		return newArray;
	}

	protected static byte[] getBytesUTF16(int addr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, 2);
		byte[] bytes = new byte[0];
		while (true) {
			int utf16 = memoryReader.readNext();
			if (utf16 == 0) {
				break;
			}
			bytes = addByteToArray(bytes, (byte) utf16);
			bytes = addByteToArray(bytes, (byte) (utf16 >> 8));
		}

		return bytes;
	}

	protected static String getStringUTF16(int addr) {
		return new String(getBytesUTF16(addr), charsetUTF16);
	}

	protected static byte[] getBytesUTF8(int addr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, 1);
		byte[] bytes = new byte[0];
		while (true) {
			int utf8 = memoryReader.readNext();
			if (utf8 == 0) {
				break;
			}
			bytes = addByteToArray(bytes, (byte) utf8);
		}

		return bytes;
	}

	protected static String getStringUTF8(int addr) {
		return new String(getBytesUTF8(addr), charsetUTF8);
	}

	protected static String getStringSJIS(int addr) {
		return new String(getBytesUTF8(addr), charsetSJIS);
	}

	protected void writeStringBytes(byte[] bytes, int addr, int maxSize) {
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, 1);
		if (bytes != null) {
			int length = Math.min(bytes.length, maxSize - 1);
			for (int i = 0; i < length; i++) {
				memoryWriter.writeNext(bytes[i] & 0xFF);
			}
		}

		// write trailing '\0'
		memoryWriter.writeNext(0);
		memoryWriter.flush();
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x00D1378F, version = 150)
	public int sceCccUTF8toUTF16() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x068C4320, version = 150)
	public int sceCccEncodeSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0A00ECF9, version = 150)
	public int sceCccSwprintfSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x17E1D813, version = 150)
	public int sceCccSetErrorCharUTF8() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x3AEC5274, version = 150)
	public int sceCccSwprintfUTF8() {
		return 0;
	}

	@HLEFunction(nid = 0x41B724A5, version = 150)
	public int sceCccUTF16toUTF8(TPointer dstAddr, int dstSize, TPointer srcAddr) {
		String dstString = getStringUTF16(srcAddr.getAddress());
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccUTF16toUTF8 string='%s'", dstString));
		}
		byte[] dstBytes = dstString.getBytes(charsetUTF8);
		writeStringBytes(dstBytes, dstAddr.getAddress(), dstSize);

		return dstBytes.length;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x4BDEB2A8, version = 150)
	public int sceCccStrlenUTF16(TPointer strUTF16) {
    	String str = getStringUTF16(strUTF16.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceCccStrlenUTF16 str='%s'", str));
    	}

    	return str.length();
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x67BF0D19, version = 150)
	public int sceCccIsValidSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x6CBB36A0, version = 150)
	public int sceCccVswprintfUTF8() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x6F82EE03, version = 150)
	public int sceCccUTF8toSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x70ECAA10, version = 150)
	public int sceCccUCStoJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x76E33E9C, version = 150)
	public int sceCccIsValidUCS2() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x8406F469, version = 150)
	public int sceCccEncodeUTF16() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x90521AC5, version = 150)
	public int sceCccIsValidUTF8() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x92C05851, version = 150)
	public int sceCccEncodeUTF8() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x953E6C10, version = 150)
	public int sceCccDecodeSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA2D5D209, version = 150)
	public int sceCccIsValidJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA62E6E80, version = 150)
	public int sceCccSJIStoUTF8() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB4D1CBBF, version = 150)
	public int sceCccSetTable(TPointer unknown1, TPointer unknown2) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB7D3C112, version = 150)
	public int sceCccStrlenUTF8(TPointer strUTF8) {
    	String str = getStringUTF16(strUTF8.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceCccStrlenUTF8 str='%s'", str));
    	}

    	return str.length();
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB8476CF4, version = 150)
	public int sceCccSetErrorCharUTF16() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xBD11EEF3, version = 150)
	public int sceCccIsValidUnicode() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xBDC4D699, version = 150)
	public int sceCccVswprintfSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xBEB47224, version = 150)
	public int sceCccSJIStoUTF16(TPointer dstUTF16, int dstSize, TPointer srcSJIS) {
    	String str = getStringSJIS(srcSJIS.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceCccSJIStoUTF16 str='%s'", str));
    	}
    	byte[] bytesUTF16 = str.getBytes(charsetUTF16);
    	writeStringBytes(bytesUTF16, dstUTF16.getAddress(), dstSize);

    	return bytesUTF16.length;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xC56949AD, version = 150)
	public int sceCccSetErrorCharSJIS() {
		return 0;
	}

	@HLEFunction(nid = 0xC6A8BEE2, version = 150)
	public int sceCccDecodeUTF8(TPointer32 srcAddrUTF8) {
		String srcString = getStringUTF8(srcAddrUTF8.getValue());
		int codePoint = srcString.codePointAt(0);
		int codePointSize = Character.charCount(codePoint);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccDecodeUTF8 string='%s'(0x%08X), codePoint=0x%X(size=%d)", srcString, srcAddrUTF8.getValue(), codePoint, codePointSize));
		}

		srcAddrUTF8.setValue(srcAddrUTF8.getValue() + codePointSize);

		return codePoint;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCC0A8BDA, version = 150)
	public int sceCccIsValidUTF16() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD2B18485, version = 150)
	public int sceCccIsValidUCS4() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD9392CCB, version = 150)
	public int sceCccStrlenSJIS(TPointer strSJIS) {
    	String str = getStringUTF16(strSJIS.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceCccStrlenSJIS str='%s'", str));
    	}

    	return str.length();
	}

	@HLEFunction(nid = 0xE0CF8091, version = 150)
	public int sceCccDecodeUTF16(TPointer32 srcAddrUTF16) {
    	String srcString = getStringUTF16(srcAddrUTF16.getValue());
    	int codePoint = srcString.codePointAt(0);
    	int codePointSize = Character.charCount(codePoint);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceCccDecodeUTF16 string='%s'(0x%08X), codePoint=0x%X(size=%d)", srcString, srcAddrUTF16.getValue(), codePoint, codePointSize));
		}

		srcAddrUTF16.setValue(srcAddrUTF16.getValue() + (codePointSize << 1));

		return codePoint;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xF1B73D12, version = 150)
	public int sceCccUTF16toSJIS() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xFB7846E2, version = 150)
	public int sceCccJIStoUCS() {
		return 0;
	}
}
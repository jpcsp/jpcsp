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

package jpcsp.format;

import static jpcsp.util.Utilities.readStringZ;
import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readUWord;
import static jpcsp.util.Utilities.writeWord;
import static jpcsp.util.Utilities.writeHalf;
import static jpcsp.util.Utilities.writeByte;
import static jpcsp.util.Utilities.writeStringZ;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import jpcsp.Emulator;

/**
 *
 * @author George
 */
public class PSF {
    private long p_offset_param_sfo; //offset of param.sfo in pbp
    private final long psfident = 0x46535000;

    private long fileidentify;
    private long psfversion;
    private long offsetkeytable;
    private long offsetvaluetable;
    private long numberofkeypairs;

     //index table
     /*
0 	1 	2 	ul16 Offset of the key name into the key table (in bytes)
2 	2 	1 	4 Unknown, always 4. Maybe alignment requirement for the data?
3 	3 	1 	ul8 Datatype of the value, see below.
4 	7 	4 	ul32 Size of value data, in bytes
8 	11 	4 	ul32 Size of value data plus padding, in bytes
12 	15 	4 	ul32 Offset of the data value into the value table (in bytes) */
    private int[] offset_keyname;
    private byte[] alignment;
    private byte[] datatype;
    private long[] value_size;
    private long[] value_size_padding;
    private long[] offset_data_value;
    private String[] keys;

    private HashMap<String, Object> map = new HashMap<String, Object>();

    public PSF(long p_offset_param_sfo)
    {
    	fileidentify = psfident;
    	psfversion = 0x00000101;
    	offsetkeytable = 5 * 4;
    	offsetvaluetable = offsetkeytable;
        this.p_offset_param_sfo = p_offset_param_sfo;
    }

    public void read(ByteBuffer f) throws IOException {
        fileidentify = readUWord(f);
        if(psfident != fileidentify)
        {
            Emulator.log.error("not current psf file!");
            return;
        }
        psfversion = readUWord(f);
        offsetkeytable = readUWord(f);
        offsetvaluetable = readUWord(f);
        numberofkeypairs = readUWord(f);

        offset_keyname = new int[(int)numberofkeypairs];
        alignment = new byte[(int)numberofkeypairs];
        datatype = new byte[(int)numberofkeypairs];
        value_size = new long[(int)numberofkeypairs];
        value_size_padding = new long[(int)numberofkeypairs];
        offset_data_value = new long[(int)numberofkeypairs];
        keys = new String[(int)numberofkeypairs];

        /*System.out.println(psfversion);
        System.out.println(offsetkeytable);
        System.out.println(offsetvaluetable);
        System.out.println(numberofkeypairs);*/
        for(int i=0; i<numberofkeypairs; i++)
        {
            offset_keyname[i] = readUHalf(f);
            alignment[i] = (byte)readUByte(f);
            datatype[i]= (byte)readUByte(f);
            value_size[i] = readUWord(f);
            value_size_padding[i] =readUWord(f);
            offset_data_value[i] = readUWord(f);
            /* System.out.println(offset_keyname[i]);
            System.out.println(alignment[i]);
            System.out.println(datatype[i]);
            System.out.println(value_size[i]);
            System.out.println(value_size_padding[i]);
            System.out.println(offset_data_value[i]);*/
        }
        String Key;
        for(int i=0; i<numberofkeypairs; i++)
        {
            f.position((int)(p_offset_param_sfo + offsetkeytable+offset_keyname[i]));
            Key = readStringZ(f);
            keys[i] = Key;
            if(datatype[i]==2)
            {
                // String may not be in english!
                f.position((int)(p_offset_param_sfo + offsetvaluetable+offset_data_value[i]));
                byte[] s = new byte[(int) value_size[i]];
                f.get(s);
                String value = new String(s, 0, s[s.length - 1] == '\0' ? s.length - 1 : s.length, "UTF-8");
                map.put(Key, value);
                //System.out.println(Key + " string = " + value);
            }
            else if(datatype[i]==4)
            {
                f.position((int)(p_offset_param_sfo + offsetvaluetable+offset_data_value[i]));
                long value = readUWord(f);
                map.put(Key, value);
                //System.out.println(Key + " int = "  + value);
            }
            else if(datatype[i] == 0)
            {
                f.position((int)(p_offset_param_sfo + offsetvaluetable+offset_data_value[i]));
                byte[] value = new byte[(int) value_size[i]];
                f.get(value);
                map.put(Key, value);
                //System.out.println(Key + " = <binary data>");
            }
            else
            {
                Emulator.log.warn(Key + " UNIMPLEMENT DATATYPE " + datatype[i]);
            }
        }
    }

    public void write(ByteBuffer f) {
    	writeWord(f, fileidentify);
    	writeWord(f, psfversion);
    	writeWord(f, offsetkeytable);
    	writeWord(f, offsetvaluetable);
    	writeWord(f, numberofkeypairs);

    	for (int i = 0; i < numberofkeypairs; i++) {
    		writeHalf(f, offset_keyname[i]);
    		writeByte(f, alignment[i]);
    		writeByte(f, datatype[i]);
    		writeWord(f, (int) value_size[i]);
    		writeWord(f, (int) value_size_padding[i]);
    		writeWord(f, (int) offset_data_value[i]);
    	}

    	for (int i = 0; i < numberofkeypairs; i++) {
    		f.position((int)(p_offset_param_sfo + offsetkeytable + offset_keyname[i]));
    		writeStringZ(f, keys[i]);
    		switch (datatype[i]) {
	    		case 2:
	                f.position((int)(p_offset_param_sfo + offsetvaluetable + offset_data_value[i]));
	                writeStringZ(f, (String) map.get(keys[i]));
	                break;
	    		case 4:
	                f.position((int)(p_offset_param_sfo + offsetvaluetable + offset_data_value[i]));
	                writeWord(f, (Long) map.get(keys[i]));
	                break;
	    		case 0:
	                f.position((int)(p_offset_param_sfo + offsetvaluetable + offset_data_value[i]));
	    			f.put((byte[]) map.get(keys[i]));
	    			break;
				default:
	                Emulator.log.warn(keys[i] + " UNIMPLEMENT DATATYPE " + datatype[i]);
					break;
    		}
    	}
    }

    private long[] extendArray(long[] array, int extent) {
    	long[] newArray;

    	if (array == null) {
    		newArray = new long[extent];
    	} else {
    		newArray = new long[array.length + extent];
    		System.arraycopy(array, 0, newArray, 0, array.length);
    	}

    	return newArray;
    }

    private int[] extendArray(int[] array, int extent) {
    	int[] newArray;

    	if (array == null) {
    		newArray = new int[extent];
    	} else {
    		newArray = new int[array.length + extent];
    		System.arraycopy(array, 0, newArray, 0, array.length);
    	}

    	return newArray;
    }

    private byte[] extendArray(byte[] array, int extent) {
    	byte[] newArray;

    	if (array == null) {
    		newArray = new byte[extent];
    	} else {
    		newArray = new byte[array.length + extent];
    		System.arraycopy(array, 0, newArray, 0, array.length);
    	}

    	return newArray;
    }

    private String[] extendArray(String[] array, int extent) {
    	String[] newArray;

    	if (array == null) {
    		newArray = new String[extent];
    	} else {
    		newArray = new String[array.length + extent];
    		System.arraycopy(array, 0, newArray, 0, array.length);
    	}

    	return newArray;
    }

    private int nextKeyTableOffset() {
    	int i = (int) numberofkeypairs;

    	if (i == 0) {
    		return 0;
    	}

    	return offset_keyname[i - 1] + keys[i - 1].length() + 1;
    }

    private long nextValueTableOffset() {
    	int i = (int) numberofkeypairs;

    	if (i == 0) {
    		return 0;
    	}

    	return offset_data_value[i - 1] + value_size[i - 1] + value_size_padding[i - 1];
    }

    private void newKey(String key, int valueType, long valueLength) {
    	final int extent = 1;
    	offset_keyname     = extendArray(offset_keyname,     extent);
    	alignment          = extendArray(alignment,          extent);
    	datatype           = extendArray(datatype,           extent);
    	value_size         = extendArray(value_size,         extent);
    	value_size_padding = extendArray(value_size_padding, extent);
    	offset_data_value  = extendArray(offset_data_value,  extent);
    	keys               = extendArray(keys,               extent);
    	offsetkeytable    += 16 * extent;
    	offsetvaluetable  += 16 * extent;

    	int i = (int) numberofkeypairs;
    	offset_keyname[i] = nextKeyTableOffset();
    	alignment[i] = 0;
    	keys[i] = key;
    	if (offsetvaluetable >= offsetkeytable) {
    		offsetvaluetable += key.length() + 1;
    	}

    	datatype[i] = (byte) valueType;
    	value_size[i] = valueLength;
    	value_size_padding[i] = ((valueLength & 1) == 0 ? 0 : 1);
    	offset_data_value[i] = nextValueTableOffset();

    	if (offsetkeytable >= offsetvaluetable) {
    		offsetkeytable += value_size[i] + value_size_padding[i];
    	}

    	numberofkeypairs += extent;
    }

    public void put(String key, String value) {
    	newKey(key, 2, value.length() + 1);
    	map.put(key, value);
    }

    public void put(String key, long value) {
    	newKey(key, 4, 2);
    	map.put(key, value);
    }

    public void put(String key, byte[] value) {
    	newKey(key, 0, value.length);
    	map.put(key, value);
    }

    public long size() {
    	if (offsetkeytable >= offsetvaluetable) {
    		return offsetkeytable + nextKeyTableOffset();
    	} else {
    		return offsetvaluetable + nextValueTableOffset();
    	}
    }

    private boolean safeEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public boolean isLikelyHomebrew() {
        boolean homebrew = false;

        String disc_version = getString("DISC_VERSION");
        String disc_id = getString("DISC_ID");
        String category = getString("CATEGORY");
        Long bootable = (Long)get("BOOTABLE");
        Long region = (Long)get("REGION");
        String psp_system_ver = getString("PSP_SYSTEM_VER");
        Long parental_level = (Long)get("PARENTAL_LEVEL");

        Long ref_one = new Long(1);
        Long ref_region = new Long(32768);

        if (safeEquals(disc_version, "1.00") &&
            safeEquals(disc_id, "UCJS10041") && // loco roco demo, should not false positive since that demo has sys ver 3.40
            safeEquals(category, "MG") &&
            safeEquals(bootable, ref_one) &&
            safeEquals(region, ref_region) &&
            safeEquals(psp_system_ver, "1.00") &&
            safeEquals(parental_level, ref_one)) {

            if (map.size() == 8) {
                homebrew = true;
            } else if (map.size() == 9 &&
                safeEquals(get("MEMSIZE"), ref_one)) {
                // lua player hm 8
                homebrew = true;
            }
        } else if (map.size() == 4 &&
            safeEquals(category, "MG") &&
            safeEquals(bootable, ref_one) &&
            safeEquals(region, ref_region)) {
            homebrew = true;
        }

        return homebrew;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(" = ");
            if(entry.getValue() instanceof Array)
                sb.append("<binary data>");
            else
                sb.append(entry.getValue());
            sb.append('\n');
        }
        sb.append("probably homebrew? " + isLikelyHomebrew());
        return sb.toString();
    }

    public Object get(String key) {
        return map.get(key);
    }

    public String getString(String key) {
        return (String)map.get(key);
    }

    // kxploit patcher tool adds "\nKXPloit Boot by PSP-DEV Team"
    public String getPrintableString(String key) {
        String rawString = (String)map.get(key);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rawString.length(); i++) {
            char c = rawString.charAt(i);
            if (c == '\0' || c == '\n')
                break;
            sb.append(rawString.charAt(i));
        }

        return sb.toString();
    }

    public long getNumeric(String key) {
        return (Long)map.get(key);
    }
}

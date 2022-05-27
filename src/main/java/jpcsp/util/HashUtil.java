package jpcsp.util;

import java.util.zip.CRC32;

public class HashUtil {
    static public int crc32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }
}

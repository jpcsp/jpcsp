package jpcsp.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtil {
    static public ByteBuffer toByteBuffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    static public byte[] toByteArray(ByteBuffer data) {
        byte[] out = new byte[data.limit()];
        for (int n = 0; n < out.length; n++) out[n] = data.get(n);
        return out;
    }

    static public byte[] toByteArray(int... in) {
        byte[] out = new byte[in.length];
        for (int n = 0; n < in.length; n++) out[n] = (byte) in[n];
        return out;
    }

    static public byte[] readBytes(ByteBuffer buffer, int offset, int len) {
        byte[] out = new byte[len];
        int oldPos = buffer.position();
        try {
            buffer.position(offset);
            buffer.get(out);
        } finally {
            buffer.position(oldPos);
        }
        return out;
    }
}

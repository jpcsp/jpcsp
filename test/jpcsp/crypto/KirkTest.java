package jpcsp.crypto;

import jpcsp.util.ByteUtil;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class KirkTest {
    private CryptoEngine engine = new CryptoEngine(); // @TODO: HACK, KIRK doesn't have dependencies on cryptoengine but the check?
    private KIRK kirk = new KIRK();

    @Test
    public void testSha1() throws Exception {
        // @TODO: HACK, KIRK doesn't have dependencies on cryptoengine but the check?
        while (!CryptoEngine.getCryptoEngineStatus()) Thread.sleep(1L);

        ByteBuffer inp = ByteUtil.toByteBuffer(new byte[]{
                // Size
                0x20, 0x00, 0x00, 0x00,
                // Data
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        });
        ByteBuffer out = ByteBuffer.allocate(0x14);
        int result = kirk.hleUtilsBufferCopyWithRange(out, out.limit(), inp, inp.limit(), KIRK.PSP_KIRK_CMD_SHA1_HASH);

        Assert.assertEquals(0, result);

        Assert.assertArrayEquals(ByteUtil.toByteArray(
                0xDE, 0x8A, 0x84, 0x7B, 0xFF, 0x8C, 0x34, 0x3D, 0x69, 0xB8, 0x53, 0xA2,
                0x15, 0xE6, 0xEE, 0x77, 0x5E, 0xF2, 0xEF, 0x96
        ), ByteUtil.toByteArray(out));
    }
}

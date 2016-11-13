package jpcsp.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class FileUtilTest {
    @Test
    public void testGetExtension() throws Exception {
        Assert.assertEquals("txt", FileUtil.getExtension(new File("test/test.txt")));
        Assert.assertEquals("demo", FileUtil.getExtension(new File("test/test.DEMO")));
        Assert.assertEquals("", FileUtil.getExtension(new File("test/test.")));
        Assert.assertEquals("", FileUtil.getExtension(new File("test/test")));
    }
}

package com.twilight.h264.util;

public class FrameUtils {

	public static void YUV2RGB(Frame f, int[] rgb) {
		int[] luma = f.getY();
		int[] cb = f.getCb();
		int[] cr = f.getCr();
		int stride = f.getWidth();
		int strideChroma = f.getWidth() >> 1;

		for (int y = 0; y < f.getHeight(); y++) {
			int lineOffLuma = y * stride;
			int lineOffChroma = (y >> 1) * strideChroma;

			for (int x = 0; x < f.getWidth(); x++) {
				int c = luma[lineOffLuma + x] - 16;
				int d = cb[lineOffChroma + (x >> 1)] - 128;
				int e = cr[lineOffChroma + (x >> 1)] - 128;

				int red = (298 * c + 409 * e + 128) >> 8;
				red = red < 0 ? 0 : (red > 255 ? 255 : red);
				int green = (298 * c - 100 * d - 208 * e + 128) >> 8;
				green = green < 0 ? 0 : (green > 255 ? 255 : green);
				int blue = (298 * c + 516 * d + 128) >> 8;
				blue = blue < 0 ? 0 : (blue > 255 ? 255 : blue);
				int alpha = 255;

				rgb[lineOffLuma + x] = (alpha << 24) | ((red & 0xff) << 16)
						| ((green & 0xff) << 8) | (blue & 0xff);
			}
		}
	}
}

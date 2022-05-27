package com.twilight.h264.util;

import com.twilight.h264.decoder.AVFrame;

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

	public static void YUV2RGB_WOEdge(AVFrame f, int[] rgb) {
		int[] luma = f.data_base[0];
		int[] cb = f.data_base[1];
		int[] cr = f.data_base[2];

		for (int y = 0; y < f.imageHeightWOEdge; y++) {
			int lineOffLuma =  y       * f.linesize[0] + f.data_offset[0];
			int lineOffCb   = (y >> 1) * f.linesize[1] + f.data_offset[1];
			int lineOffCr   = (y >> 1) * f.linesize[2] + f.data_offset[2];
			int rgbOff = y * f.imageWidthWOEdge;

			for (int x = 0; x < f.imageWidthWOEdge; x++) {
				int c = luma[lineOffLuma + x] - 16;
				int d = cb[lineOffCb + (x >> 1)] - 128;
				int e = cr[lineOffCr + (x >> 1)] - 128;

				int red = (298 * c + 409 * e + 128) >> 8;
				red = red < 0 ? 0 : (red > 255 ? 255 : red);
				int green = (298 * c - 100 * d - 208 * e + 128) >> 8;
				green = green < 0 ? 0 : (green > 255 ? 255 : green);
				int blue = (298 * c + 516 * d + 128) >> 8;
				blue = blue < 0 ? 0 : (blue > 255 ? 255 : blue);
				int alpha = 255;

				rgb[rgbOff + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
			}
		}
	}
}

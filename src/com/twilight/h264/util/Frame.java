package com.twilight.h264.util;

public class Frame {
	private int width;
	private int height;
	private int[] y;
	private int[] cb;
	private int[] cr;

	public Frame(int width, int height, int[] y, int[] cb, int[] cr) {
		this.width = width;
		this.height = height;
		this.y = y;
		this.cb = cb;
		this.cr = cr;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int[] getY() {
		return y;
	}

	public int[] getCb() {
		return cb;
	}

	public int[] getCr() {
		return cr;
	}
}

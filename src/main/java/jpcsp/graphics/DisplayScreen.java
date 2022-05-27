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
package jpcsp.graphics;

import jpcsp.hardware.Screen;

/**
 * Simple class containing the attributes related to a display screen,
 * abstracting the possible screen rotation.
 * 
 * @author gid15
 *
 */
public class DisplayScreen {
	private float[] values;

	public DisplayScreen() {
		update(1f, 1f);
	}

	public void update(float texS, float texT) {
		int[] indices = getIndices();
		if (indices == null) {
			return;
		}
		float[] baseValues = new float[] { 0f, 0f, texS, 0f, 0f, texT, texS, texT };
		values = new float[baseValues.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = baseValues[indices[i]];
		}
	}

	protected int[] getIndices() {
		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
	}

	public boolean isSwappedXY() {
		return false;
	}

	protected int getWidth(int width, int height) {
		return isSwappedXY() ? height : width;
	}

	protected int getHeight(int width, int height) {
		return isSwappedXY() ? width : height;
	}

	public int getWidth() {
		return getWidth(Screen.width, Screen.height);
	}

	public int getHeight() {
		return getHeight(Screen.width, Screen.height);
	}

	public int getWidth(FrameBufferSettings fb) {
		return getWidth(fb.getWidth(), fb.getHeight());
	}

	public int getHeight(FrameBufferSettings fb) {
		return getHeight(fb.getWidth(), fb.getHeight());
	}

	public float getTextureUpperLeftS() {
		return values[0];
	}

	public float getTextureUpperLeftT() {
		return values[1];
	}

	public float getTextureUpperRightS() {
		return values[2];
	}

	public float getTextureUpperRightT() {
		return values[3];
	}

	public float getTextureLowerLeftS() {
		return values[4];
	}

	public float getTextureLowerLeftT() {
		return values[5];
	}

	public float getTextureLowerRightS() {
		return values[6];
	}

	public float getTextureLowerRightT() {
		return values[7];
	}

	@Override
	public String toString() {
		return String.format("DisplayScreen [%f, %f, %f, %f, %f, %f, %f, %f, %b]", values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], isSwappedXY());
	}

    public static class DisplayScreenRotation90 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 4, 5, 0, 1, 6, 7, 2, 3 };
		}

		@Override
		public boolean isSwappedXY() {
			return true;
		}
    }

    public static class DisplayScreenRotation180 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 6, 7, 4, 5, 2, 3, 0, 1 };
		}
    }

    public static class DisplayScreenRotation270 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 2, 3, 6, 7, 0, 1, 4, 5 };
		}

		@Override
		public boolean isSwappedXY() {
			return true;
		}
    }

    public static class DisplayScreenMirrorX extends DisplayScreen {
    	private DisplayScreen displayScreen;

    	public DisplayScreenMirrorX(DisplayScreen displayScreen) {
    		this.displayScreen = displayScreen;
    		update(1f, 1f);
    	}

    	@Override
		protected int[] getIndices() {
    		if (displayScreen == null) {
    			return null;
    		}
    		int[] i = displayScreen.getIndices();
			return new int[] { i[2], i[3], i[0], i[1], i[6], i[7], i[4], i[5] };
		}

		@Override
		public boolean isSwappedXY() {
			return displayScreen.isSwappedXY();
		}
    }

    public static class DisplayScreenMirrorY extends DisplayScreen {
    	private DisplayScreen displayScreen;

    	public DisplayScreenMirrorY(DisplayScreen displayScreen) {
    		this.displayScreen = displayScreen;
    	}

		@Override
		protected int[] getIndices() {
    		int[] i = displayScreen.getIndices();
			return new int[] { i[4], i[5], i[6], i[7], i[0], i[1], i[2], i[3] };
		}

		@Override
		public boolean isSwappedXY() {
			return displayScreen.isSwappedXY();
		}
    }
}

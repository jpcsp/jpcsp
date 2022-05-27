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
package jpcsp.graphics.RE.software;

import static jpcsp.util.Utilities.round;

/**
 * @author gid15
 *
 */
public class PixelColor {
	public static final int ONE = 0xFF;
	public static final int ZERO = 0x00;
	private static final int[][] preMultiply = new int[ONE + 1][ONE + 1];
	private static final int[] preClamp = new int[ONE + ONE + 1];

	static {
		// Pre-computed multiplication of color components. Might be faster?
		for (int c1 = 0; c1 <= ONE; c1++) {
			for (int c2 = 0; c2 <= ONE; c2++) {
				preMultiply[c1][c2] = (c1 * c2) / ONE;
			}
		}

		// Pre-computed clamping of color component. Might be faster?
		for (int c1 = 0; c1 < preClamp.length; c1++) {
			preClamp[c1] = Math.min(c1, ONE);
		}
	}

	public final static int getAlpha(int color) {
		return color >>> 24;
	}

	public final static int getRed(int color) {
		return color & 0xFF;
	}

	public final static int getGreen(int color) {
		return (color >> 8) & 0xFF;
	}

	public final static int getBlue(int color) {
		return (color >> 16) & 0xFF;
	}

	public final static int getColor(float[] color) {
		return getColor(getColor(color[3]), getColor(color[2]), getColor(color[1]), getColor(color[0]));
	}

	public final static int getColor(int[] color) {
		return getColor(color[3], color[2], color[1], color[0]);
	}

	public final static int getColorBGR(int[] color) {
		return getColorBGR(color[2], color[1], color[0]);
	}

	public final static int getColor(float color){
		return (int) (color * ONE);
	}

	public final static int getColor(int a, int b, int g, int r) {
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

	public final static int getColorBGR(int b, int g, int r) {
		return (b << 16) | (g << 8) | r;
	}

	public final static int multiplyComponent(int component1, int component2) {
		return preMultiply[component1][component2];
	}

	public final static int multiply(int color1, int a2, int b2, int g2, int r2) {
		int a = multiplyComponent(getAlpha(color1), a2);
		int b = multiplyComponent(getBlue(color1), b2);
		int g = multiplyComponent(getGreen(color1), g2);
		int r = multiplyComponent(getRed(color1), r2);
		return getColor(a, b, g, r);
	}

	public final static int multiplyBGR(int color1, int b2, int g2, int r2) {
		int b = multiplyComponent(getBlue(color1), b2);
		int g = multiplyComponent(getGreen(color1), g2);
		int r = multiplyComponent(getRed(color1), r2);
		return getColorBGR(b, g, r);
	}

	public final static int multiply(int color1, int color2) {
		// Handle common and simple cases first
		switch (color2) {
			case 0x00000000: return 0;
			case 0x00FFFFFF: return color1 & color2;
			case 0xFFFFFFFF: return color1;
		}
		switch (color1) {
			case 0x00000000: return 0;
			case 0x00FFFFFF: return color1 & color2;
			case 0xFFFFFFFF: return color2;
		}

		return multiply(color1, getAlpha(color2), getBlue(color2), getGreen(color2), getRed(color2));
	}

	public final static int multiplyBGR(int color1, int color2) {
		color1 &= 0x00FFFFFF;
		color2 &= 0x00FFFFFF;
		// Handle common and simple cases first
		if (color1 == ZERO || color2 == ZERO) {
			return ZERO;
		}
		if (color1 == 0x00FFFFFF) {
			return color2;
		}
		if (color2 == 0x00FFFFFF) {
			return color1;
		}

		return multiplyBGR(color1, getBlue(color2), getGreen(color2), getRed(color2));
	}

	public final static int multiplyBGR(int color, float factor) {
		color &= 0x00FFFFFF;
		// Handle common and simple cases first
		if (color == ZERO || factor <= 0f) {
			return ZERO;
		}
		if (factor == 1f) {
			return color;
		}

		int b = multiplyComponent(getBlue(color), factor);
		int g = multiplyComponent(getGreen(color), factor);
		int r = multiplyComponent(getRed(color), factor);
		return getColorBGR(b, g, r);
	}

	public final static int multiply(int color, float factor) {
		// Handle common and simple cases first
		if (color == ZERO || factor <= 0f) {
			return ZERO;
		}
		if (factor == 1f) {
			return color;
		}
		if (factor == .5f) {
			return divideBy2(color);
		}
		if (factor == .25f) {
			return divideBy4(color);
		}

		int a = multiplyComponent(getAlpha(color), factor);
		int b = multiplyComponent(getBlue(color), factor);
		int g = multiplyComponent(getGreen(color), factor);
		int r = multiplyComponent(getRed(color), factor);
		return getColor(a, b, g, r);
	}

	public final static int multiplyComponent(int component, float factor) {
		return Math.max(ZERO, Math.min(ONE, round(component * factor)));
	}

	public final static int addComponent(int component1, int component2) {
		// Add with clamp to ONE
		return preClamp[component1 + component2];
	}

	public final static int add(int color1, int color2) {
		// Handle common and simple cases first
		if (color1 == ZERO) {
			return color2;
		}
		if (color2 == ZERO) {
			return color1;
		}
		if (color1 == 0xFFFFFFFF || color2 == 0xFFFFFFFF) {
			return 0xFFFFFFFF;
		}

		int a = addComponent(getAlpha(color1), getAlpha(color2));
		int b = addComponent(getBlue(color1), getBlue(color2));
		int g = addComponent(getGreen(color1), getGreen(color2));
		int r = addComponent(getRed(color1), getRed(color2));
		return getColor(a, b, g, r);
	}

	public final static int addBGR(int color1, int color2) {
		color1 &= 0x00FFFFFF;
		color2 &= 0x00FFFFFF;
		// Handle common and simple cases first
		if (color1 == ZERO) {
			return color2;
		}
		if (color2 == ZERO) {
			return color1;
		}
		if (color1 == 0x00FFFFFF || color2 == 0x00FFFFFF) {
			return 0x00FFFFFF;
		}

		int b = addComponent(getBlue(color1), getBlue(color2));
		int g = addComponent(getGreen(color1), getGreen(color2));
		int r = addComponent(getRed(color1), getRed(color2));
		return getColorBGR(b, g, r);
	}

	public final static int substractComponent(int component1, int component2) {
		// Substract with clamp to ZERO
		return Math.max(component1 - component2, ZERO);
	}

	public final static int substract(int color1, int color2) {
		int a = substractComponent(getAlpha(color1), getAlpha(color2));
		int b = substractComponent(getBlue(color1), getBlue(color2));
		int g = substractComponent(getGreen(color1), getGreen(color2));
		int r = substractComponent(getRed(color1), getRed(color2));
		return getColor(a, b, g, r);
	}

	public final static int substractBGR(int color1, int color2) {
		int b = substractComponent(getBlue(color1), getBlue(color2));
		int g = substractComponent(getGreen(color1), getGreen(color2));
		int r = substractComponent(getRed(color1), getRed(color2));
		return getColorBGR(b, g, r);
	}

	public final static int min(int color1, int color2) {
		int a = Math.min(getAlpha(color1), getAlpha(color2));
		int b = Math.min(getBlue(color1), getBlue(color2));
		int g = Math.min(getGreen(color1), getGreen(color2));
		int r = Math.min(getRed(color1), getRed(color2));
		return getColor(a, b, g, r);
	}

	public final static int minBGR(int color1, int color2) {
		int b = Math.min(getBlue(color1), getBlue(color2));
		int g = Math.min(getGreen(color1), getGreen(color2));
		int r = Math.min(getRed(color1), getRed(color2));
		return getColorBGR(b, g, r);
	}

	public final static int max(int color1, int color2) {
		int a = Math.max(getAlpha(color1), getAlpha(color2));
		int b = Math.max(getBlue(color1), getBlue(color2));
		int g = Math.max(getGreen(color1), getGreen(color2));
		int r = Math.max(getRed(color1), getRed(color2));
		return getColor(a, b, g, r);
	}

	public final static int maxBGR(int color1, int color2) {
		int b = Math.max(getBlue(color1), getBlue(color2));
		int g = Math.max(getGreen(color1), getGreen(color2));
		int r = Math.max(getRed(color1), getRed(color2));
		return getColorBGR(b, g, r);
	}

	private final static int absComponent(int component1, int component2) {
		// Be careful to return a result value between 0x00 and 0xFF
		if (component1 >= component2) {
			return component1 - component2;
		}
		return component2 - component1;
	}

	public final static int abs(int color1, int color2) {
		int a = absComponent(getAlpha(color1), getAlpha(color2));
		int b = absComponent(getBlue(color1), getBlue(color2));
		int g = absComponent(getGreen(color1), getGreen(color2));
		int r = absComponent(getRed(color1), getRed(color2));
		return getColor(a, b, g, r);
	}

	public final static int absBGR(int color1, int color2) {
		int b = absComponent(getBlue(color1), getBlue(color2));
		int g = absComponent(getGreen(color1), getGreen(color2));
		int r = absComponent(getRed(color1), getRed(color2));
		return getColorBGR(b, g, r);
	}

	public final static int combineComponent(int component1, int component2, int factor) {
		return ((ONE - factor) * component1 + factor * component2) / ONE;
	}

	public final static int setAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (alpha << 24);
	}

	public final static int setBGR(int color, int bgr) {
		return (color & 0xFF000000) | bgr;
	}

	public final static int doubleColor(int color) {
		return addBGR(color, color) | (color & 0xFF000000);
	}

	public final static int doubleComponent(int component) {
		return addComponent(component, component);
	}

	public final static int divideBy2(int color) {
		return (color >>> 1) & 0x7F7F7F7F;
	}

	public final static int divideBy4(int color) {
		return (color >>> 2) & 0x3F3F3F3F;
	}
}

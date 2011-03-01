package jpcsp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.JFrame;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class OpenGL extends JFrame {
	private static final long serialVersionUID = -2382484285518105610L;
	private static Window window;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OpenGL instance = new OpenGL();
		try {
			instance.setTitle("OpenGL Test");
			instance.setSize(480, 272);
			instance.setDefaultCloseOperation(EXIT_ON_CLOSE);
			window = new Window();
			instance.add(window);
			instance.setVisible(true);
			new Thread() {
				@Override
				public void run() {
					while (true) {
						window.repaint();
						try {
							sleep(20);
						} catch (InterruptedException e) {
						}
					}
				}
			}.start();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	private static class Window extends AWTGLCanvas {
		private static final long serialVersionUID = -2905423386357820220L;
		private int texture1Id = -1;
		private int texture2Id = -1;

		public Window() throws LWJGLException {
			super();
		}

		@Override
		protected void paintGL() {
			try {
				makeCurrent();
				GL11.glClearColor(1, 1, 1, 1);
		        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		        GL11.glOrtho(-1, 1, -1, 1, -1, 1);
		        if (texture1Id == -1) {
			        IntBuffer texture1 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
			        texture1.put(0xFFFF0000);
			        texture1.put(0xFFCC0000);
			        texture1.put(0xFF990000);
			        texture1.put(0xFF660000);
			        texture1.put(0xFF006600);
			        texture1.put(0xFF009900);
			        texture1.put(0xFF00CC00);
			        texture1.put(0xFF00FF00);
			        texture1.put(0xFF0000FF);
			        texture1.put(0xFF0000CC);
			        texture1.put(0xFF000099);
			        texture1.put(0xFF000066);
			        texture1.put(0xFFFF0066);
			        texture1.put(0xFFCC0099);
			        texture1.put(0xFF9900CC);
			        texture1.put(0xFF6600FF);
			        texture1.rewind();
			        texture1Id = GL11.glGenTextures();
			        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture1Id);
			        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 4);
			        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
			        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 4);
			        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
			        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 4, 4, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texture1);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		        }
		        if (texture2Id == -1) {
			        IntBuffer texture2 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
			        texture2.put(0x00000000);
			        texture2.put(0x11111111);
			        texture2.put(0x22222222);
			        texture2.put(0x33333333);
			        texture2.put(0x44444444);
			        texture2.put(0x55555555);
			        texture2.put(0x66666666);
			        texture2.put(0x77777777);
			        texture2.put(0x88888888);
			        texture2.put(0x99999999);
			        texture2.put(0xAAAAAAAA);
			        texture2.put(0xBBBBBBBB);
			        texture2.put(0xCCCCCCCC);
			        texture2.put(0xDDDDDDDD);
			        texture2.put(0xEEEEEEEE);
			        texture2.put(0xFFFFFFFF);
			        texture2.rewind();
			        ByteBuffer texture3 = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
			        texture3.put((byte) 1);
			        texture3.put((byte) 2);
			        texture3.put((byte) 3);
			        texture3.put((byte) 4);
			        texture3.put((byte) 5);
			        texture3.put((byte) 6);
			        texture3.put((byte) 7);
			        texture3.put((byte) 8);
			        texture3.put((byte) 9);
			        texture3.put((byte) 10);
			        texture3.put((byte) 11);
			        texture3.put((byte) 12);
			        texture3.put((byte) 13);
			        texture3.put((byte) 14);
			        texture3.put((byte) 15);
			        texture3.put((byte) 16);
			        texture3.rewind();
			        texture2Id = GL11.glGenTextures();
			        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2Id);
			        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 4);
			        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
			        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 4);
			        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
//			        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 4, 4, 0, GL11.GL_COLOR_INDEX, GL11.GL_UNSIGNED_INT, texture2);
			        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 4, 4, 0, GL11.GL_COLOR_INDEX, GL11.GL_UNSIGNED_BYTE, texture3);
			        System.out.println("Error: " + GL11.glGetError());
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
			        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

			        IntBuffer mapR = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
			        mapR.put(0x00000000);
			        mapR.put(0x11111111);
			        mapR.put(0x22222222);
			        mapR.put(0x33333333);
			        mapR.put(0x44444444);
			        mapR.put(0x55555555);
			        mapR.put(0x66666666);
			        mapR.put(0x77777777);
			        mapR.put(0x88888888);
			        mapR.put(0x99999999);
			        mapR.put(0xAAAAAAAA);
			        mapR.put(0xBBBBBBBB);
			        mapR.put(0xCCCCCCCC);
			        mapR.put(0xDDDDDDDD);
			        mapR.put(0xEEEEEEEE);
			        mapR.put(0xFFFFFFFF);
			        mapR.rewind();
			        FloatBuffer mapF = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			        mapF.put(0f);
			        mapF.put(0.1f);
			        mapF.put(0.2f);
			        mapF.put(0.3f);
			        mapF.put(0.4f);
			        mapF.put(0.5f);
			        mapF.put(0.6f);
			        mapF.put(0.7f);
			        mapF.put(0.8f);
			        mapF.put(0.9f);
			        mapF.put(1f);
			        mapF.put(0.1f);
			        mapF.put(0.2f);
			        mapF.put(0.3f);
			        mapF.put(0.4f);
			        mapF.put(0.5f);
			        mapF.rewind();
//			        GL11.glPixelMapu(GL11.GL_PIXEL_MAP_I_TO_R, mapR);
//			        System.out.println("Error: " + GL11.glGetError());
//			        GL11.glPixelMapu(GL11.GL_PIXEL_MAP_I_TO_G, mapR);
//			        GL11.glPixelMapu(GL11.GL_PIXEL_MAP_I_TO_B, mapR);
//			        GL11.glPixelMapu(GL11.GL_PIXEL_MAP_I_TO_A, mapR);
			        GL11.glPixelMap(GL11.GL_PIXEL_MAP_I_TO_R, mapF);
			        System.out.println("Error: " + GL11.glGetError());
			        GL11.glPixelMap(GL11.GL_PIXEL_MAP_I_TO_G, mapF);
			        GL11.glPixelMap(GL11.GL_PIXEL_MAP_I_TO_B, mapF);
			        GL11.glPixelMap(GL11.GL_PIXEL_MAP_I_TO_A, mapF);
		        }

		        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture1Id);
		        GL11.glEnable(GL11.GL_TEXTURE_2D);
		        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		        GL11.glBegin(GL11.GL_QUADS);
		        	GL11.glColor3f(1, 0, 0);

			        GL11.glTexCoord2f(0, 1);
			        GL11.glVertex2f(0, .5f);

			        GL11.glTexCoord2f(0, 0);
			        GL11.glVertex2f(0, 0);

			        GL11.glTexCoord2f(1, 0);
			        GL11.glVertex2f(.5f, 0);

			        GL11.glTexCoord2f(1, 1);
			        GL11.glVertex2f(.5f, .5f);
		        GL11.glEnd();
		        GL11.glDisable(GL11.GL_TEXTURE_2D);

		        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2Id);
		        GL11.glEnable(GL11.GL_TEXTURE_2D);
		        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		        GL11.glBegin(GL11.GL_QUADS);
			        GL11.glColor3f(0, 1, 0);

			        GL11.glTexCoord2f(0, 1);
			        GL11.glVertex2f(0, .5f);

			        GL11.glTexCoord2f(0, 0);
			        GL11.glVertex2f(0, 0);

			        GL11.glTexCoord2f(1, 0);
			        GL11.glVertex2f(-.5f, 0);

			        GL11.glTexCoord2f(1, 1);
			        GL11.glVertex2f(-.5f, .5f);
		        GL11.glEnd();
		        GL11.glDisable(GL11.GL_TEXTURE_2D);

		        swapBuffers();
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
		}
	}
}

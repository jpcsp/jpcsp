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

package jpcsp.HLE;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;
import java.nio.Buffer;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.Threading;


/**
 *
 * @author shadow
 */
public class pspdisplay_glcanvas extends GLCanvas implements GLEventListener{
   private TextureData inputData; // TextureData can be flushed into Texture
    private Texture tex; // Texture can be Open GL rendered

    private boolean doupdate;
    private Buffer b;

    private static pspdisplay_glcanvas instance;
    
    public static GL getDrawable(){
        return get_instance().getGL();
    }
    public static pspdisplay_glcanvas get_instance() {
        if (instance == null) {
            instance = new pspdisplay_glcanvas();
        }
        return instance;
    }
    public pspdisplay_glcanvas()
    {
        setSize(480, 272);
        addGLEventListener(this);
        //final Animator animator = new Animator(this);
        //animator.start();

        b = null;
        doupdate = false;
    }

    public void createImage(Buffer b) {
        System.out.println("create tex (deferred)");
        this.b = b;
    }

    public void updateImage() {
        //System.out.println("update tex (deferred)");
        doupdate = true;

        // Why all the checks? see http://download.java.net/media/jogl/builds/nightly/javadoc_public/javax/media/opengl/Threading.html
        if (!Threading.isOpenGLThread() && Threading.isSingleThreaded()) {
            Threading.invokeOnOpenGLThread(new Runnable() {
                public void run() {
                    //System.err.println("thread display");
                    display();
                } });
        } else {
            //System.err.println("non-thread display");
            display();
        }
    }

    // ----------------------- GLEventListener -----------------------

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.setSwapInterval(1);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
       /* GL gl = drawable.getGL();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, 480, 272, 0, -1.0, 1.0);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();*/

      GL gl = drawable.getGL();
      gl.glViewport(0, 0, width, height);
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glOrtho(0, 480, 272, 0, -1.0, 1.0);
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        /*
        // Special handling for the case where the GLJPanel is translucent
        // and wants to be composited with other Java 2D content
        if ((drawable instanceof GLJPanel) &&
            !((GLJPanel) drawable).isOpaque() &&
            ((GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        } else {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
        */
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Deferred create texture (needs to be in GL thread)
        if (b != null) {
            System.out.println("create tex");
            inputData = new TextureData(GL.GL_RGBA,
                512, 512, 0,
                GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
                false, false, false,
                b, null);
            tex = TextureIO.newTexture(inputData);
            //tex.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            //tex.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            tex.enable(); // gl.glEnable(GL.GL_TEXTURE_2D);
            tex.bind();
            b = null;
        }

        // Deferred update (needs to be in GL thread)
        if (doupdate && tex != null) {
            //System.out.println("update tex");
            tex.updateImage(inputData);
            doupdate = false;
        }

        if (tex != null) {
            gl.glBegin(GL.GL_QUADS);
                //gl.glNormal3f(0.0f, 0.0f, 1.0f);

                // 512x512 texture, so tex coords go for 480x272
                gl.glTexCoord2f(0.0f, 0.0f);
                gl.glVertex3f(0.0f, 0.0f, 0.0f);

                gl.glTexCoord2f(480.0f / 512, 0.0f);
                gl.glVertex3f(480.0f, 0.0f, 0.0f);

                gl.glTexCoord2f(480.0f / 512, 272.0f / 512);
                gl.glVertex3f(480.0f, 272.0f, 0.0f);

                gl.glTexCoord2f(0.0f, 272.0f / 512);
                gl.glVertex3f(0.0f, 272.0f, 0.0f);
            gl.glEnd();
        }
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
}

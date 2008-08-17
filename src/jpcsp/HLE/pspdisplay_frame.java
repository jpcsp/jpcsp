package jpcsp.HLE;

import java.awt.*;
import java.awt.event.*;

import java.nio.Buffer;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.texture.*;

public class pspdisplay_frame implements GLEventListener {
    private Frame frame;

    private TextureData inputData; // TextureData can be flushed into Texture
    private Texture tex; // Texture can be Open GL rendered

    private boolean doupdate;
    private Buffer b;

    public pspdisplay_frame() {
        frame = new Frame("Display Buffer");
        GLCanvas canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        frame.add(canvas);
        frame.setSize(480, 272);
        //frame.setLocation(50, 760);

        // Ideally we would be like to control how frequently display() is called.
        // The Animator object will call it as fast as it likes, without it the window
        // is only updated once each time it takes the focus (from one of the JOGL demos).
        final Animator animator = new Animator(canvas);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                        //System.exit(0);
                    }
                }).start();
            }
          });

        frame.setVisible(true);
        animator.start();

        b = null;
        doupdate = false;
    }

    public void cleanup() {
        frame.setVisible(false);
    }

    public void createImage(Buffer b) {
        System.out.println("create tex (deferred)");
        this.b = b;
    }

    public void updateImage() {
        //System.out.println("update tex (deferred)");
        doupdate = true;
    }

    // ----------------------- GLEventListener -----------------------

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.setSwapInterval(1);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL gl = drawable.getGL();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, 480, 272, 0, -1.0, 1.0);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
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

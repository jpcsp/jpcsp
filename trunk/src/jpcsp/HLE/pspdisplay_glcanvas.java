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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.nio.Buffer;
//import com.sun.opengl.util.Animator;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.Threading;
import jpcsp.graphics.VideoEngine;

/**
 *
 * @author shadow
 */
public class pspdisplay_glcanvas extends GLCanvas implements GLEventListener{
    private static pspdisplay_glcanvas instance;

    // In theory we can protect shared access to videoram if only 1 thread can run at a time,
    // so this option lets us block the emu thread while the GL thread updates.
    private final static boolean UPDATE_BLOCKS = true;
    private Object callingThread;
    private Object waitObject;

    private BufferInfo currentBufferInfo;
    private BufferInfo lastBufferInfo;
    private List<BufferInfo> deferredDeleteList;

    private int reshape_width, reshape_height;

    private boolean doupdatetexture; // Call currentBufferInfo.updateTexture()
    private boolean docreatetexture; // Call currentBufferInfo.createTexture()

    public static GL getDrawable(){
        return get_instance().getGL();
    }
    public static pspdisplay_glcanvas get_instance() {
        if (instance == null) {
            instance = new pspdisplay_glcanvas();
        }
        return instance;
    }

    private pspdisplay_glcanvas()
    {
        setSize(480, 272);
        addGLEventListener(this);

        // The Animator will update however fast JOGL designers wanted it to
        // We don't need it anymore since pspdisplay will call updateImage at 60 fps
        //final Animator animator = new Animator(this);
        //animator.start();

        //waitObject = new Object(); // broken

        currentBufferInfo = null;
        lastBufferInfo = null;
        deferredDeleteList = Collections.synchronizedList(new LinkedList<BufferInfo>());

        doupdatetexture = false;
        docreatetexture = false;
    }

    /** call this when loading new media, otherwise the previous app's buffers
     * may get mixed up with the new app's so won't refresh properly. */
    public void invalidateBufferCache() {
        if (lastBufferInfo != null) {
            deferredDeleteList.add(lastBufferInfo);
            lastBufferInfo = null;
        }

        if (currentBufferInfo != null) {
            deferredDeleteList.add(currentBufferInfo);
            currentBufferInfo = null;
        }

        doupdatetexture = false;
        docreatetexture = false;
    }

    // Here pspdisplay shares its GL pixel data with pspdisplay_glcanvas
    // We anticipate PSP to flip between 2 buffers (2 addresses in vram), so we
    // implement a cache system so we don't call GL create/destroy texture lots.
    // addr should be associated with buffer, such that if buffer is backed by a different piece of memory then addr will also have a different value
    public void updateDispSettings(Buffer buffer, int width, int height, int bufferwidth, int psppixelformat, int addr) {
        BufferInfo newBufferInfo = new BufferInfo(buffer, width, height, bufferwidth, psppixelformat, addr);

        if (currentBufferInfo != null && newBufferInfo.equals(currentBufferInfo)) {
            //System.out.println("buffer didn't change");
            // Update the texture from the shared Buffer
            doupdatetexture = true;
        } else if (lastBufferInfo != null && newBufferInfo.equals(lastBufferInfo)) {
            //System.out.println("new buffer = last buffer");
            // Swap current/last
            BufferInfo tmp = lastBufferInfo;
            lastBufferInfo = currentBufferInfo;
            currentBufferInfo = tmp;

            // Update the texture from the shared Buffer
            doupdatetexture = true;
        } else {
            //System.out.println("new buffer");
            if (lastBufferInfo != null) {
                //System.out.println("deleting old buffer");
                deferredDeleteList.add(lastBufferInfo);
            }

            lastBufferInfo = currentBufferInfo;
            currentBufferInfo = newBufferInfo;

            // Generate the texture from the shared Buffer
            docreatetexture = true;
        }
    }

    // If UPDATE_BLOCKS = true, then this function blocks, so don't hold the DisplayList lock when you call it
    public void updateImage() {
        //System.out.println("updateImage (deferred)");

        // Update the texture from the shared Buffer
        doupdatetexture = true;

        if (UPDATE_BLOCKS) {
            /* broken
            callingThread = Thread.currentThread();
            try {
                while(callingThread != null) waitObject.wait();
            } catch(InterruptedException e) {
            }
            */
        }

        //System.err.println("updateImage " + Thread.currentThread());
        display();
    }

    // ----------------------- GLEventListener -----------------------

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.setSwapInterval(1);
    }

    // This should get called once when the canvas is created, then each time it gets resized
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

        reshape_width = width;
        reshape_height = height;
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Apparently we only need to use a synchronized when we use an iterator, not for adding new elements
        // http://java.sun.com/javase/6/docs/api/java/util/Collections.html#synchronizedList(java.util.List)
        synchronized(deferredDeleteList) {
            Iterator<BufferInfo> it = deferredDeleteList.iterator();
            while(it.hasNext()) {
                BufferInfo bufferInfo = it.next();
                bufferInfo.dispose();
                it.remove();
            }
        }

        if (currentBufferInfo != null) {
            //System.err.println("display " + Thread.currentThread());

            if (docreatetexture) {
                currentBufferInfo.createTexture();
                docreatetexture = false;
            }

            if (doupdatetexture) {
                currentBufferInfo.updateTexture();
                doupdatetexture = false;
            }

/* disabled for micket
            // Execute queued display lists
            VideoEngine ve = VideoEngine.getEngine(gl, true, true);
            ve.update();

            // Save VideoEngine's GL_TEXTURE_2D, current matrix mode, viewport settings
            gl.glPushAttrib(GL.GL_ENABLE_BIT|GL.GL_TRANSFORM_BIT|GL.GL_VIEWPORT_BIT);

            // Setup these here, since VideoEngine may have changed them
            gl.glViewport(0, 0, reshape_width, reshape_height);
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix(); // Save VideoEngine's GL_PROJECTION matrix
            gl.glLoadIdentity();
            gl.glOrtho(0, 480, 272, 0, -1.0, 1.0);
*/
            gl.glEnable(GL.GL_TEXTURE_2D);
            currentBufferInfo.tex.bind();

            gl.glBegin(GL.GL_QUADS);
                //gl.glNormal3f(0.0f, 0.0f, 1.0f);

                gl.glTexCoord2f(0.0f, 0.0f);
                gl.glVertex3f(0.0f, 0.0f, 0.0f);

                gl.glTexCoord2f(currentBufferInfo.u, 0.0f);
                gl.glVertex3f(480.0f, 0.0f, 0.0f);

                gl.glTexCoord2f(currentBufferInfo.u, currentBufferInfo.v);
                gl.glVertex3f((float)currentBufferInfo.width, (float)currentBufferInfo.height, 0.0f);

                gl.glTexCoord2f(0.0f, currentBufferInfo.v);
                gl.glVertex3f(0.0f, 272.0f, 0.0f);
            gl.glEnd();

/* disabled for micket
            // Restore VideoEngine's GL_PROJECTION matrix
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();

            // Restore VideoEngine's GL_TEXTURE_2D, current matrix mode, viewport settings
            gl.glPopAttrib();
*/
        }

        if (UPDATE_BLOCKS && callingThread != null) {
            /* broken
            waitObject.notify();
            callingThread = null;
            */
        }
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }


    public class BufferInfo {
        TextureData inputData; // TextureData can be flushed into Texture
        Texture tex; // Texture can be Open GL rendered
        float u, v; // Precalculated uv coords for bottom/right of texture

        private final Buffer buffer;
        private final int width, height, bufferwidth, pixelType, addr;

        public BufferInfo(Buffer buffer, int width, int height, int bufferwidth, int psppixelformat, int addr) {
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            this.bufferwidth = bufferwidth;
            this.addr = addr;

            switch(psppixelformat) {
                case 0: pixelType = GL.GL_UNSIGNED_SHORT_5_6_5_REV; break;
                case 1: pixelType = GL.GL_UNSIGNED_SHORT_1_5_5_5_REV; break;
                case 2: pixelType = GL.GL_UNSIGNED_SHORT_4_4_4_4_REV; break;
                default:
                // Both of these formats should be doing the same thing
                //case 3: pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV; break;
                case 3: pixelType = GL.GL_UNSIGNED_BYTE; break;
            }
        }

        // Call from GL thread
        public void createTexture() {
            inputData = new TextureData(GL.GL_RGBA,
                bufferwidth, height, 0,
                GL.GL_RGBA, pixelType,
                false, false, false,
                buffer, null);

            if (tex != null)
                tex.dispose();

            tex = TextureIO.newTexture(inputData);
            tex.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            tex.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            tex.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            tex.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

            u = (float)width / tex.getWidth();
            v = (float)height / tex.getHeight();
        }

        // Call from GL thread
        public void updateTexture() {
            tex.updateImage(inputData);
        }

        // Call from GL thread
        public void dispose() {
            if (tex != null) {
                //System.err.println("dispose " + Thread.currentThread());
                tex.dispose();
                tex = null;
            }
        }

        public boolean equals(BufferInfo b) {
            return (addr == b.addr &&
                pixelType == b.pixelType &&
                width == b.width &&
                height == b.height &&
                bufferwidth == b.bufferwidth);
        }
    }
}

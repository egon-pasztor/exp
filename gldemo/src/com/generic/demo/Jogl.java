package com.generic.demo;

import com.google.common.base.Objects;

import com.generic.base.Color;
import com.generic.base.Graphics3D;
import com.generic.base.Image;
import com.generic.base.Image.Size;
import com.generic.base.Platform;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

public class Jogl {
   
   // ======================================================================
   // Window
   // ======================================================================
   
   private static class Window3D implements GLEventListener,                                         /* jogamp   */
                                            MouseListener, MouseMotionListener, MouseWheelListener,  /* awt      */
                                            Platform.Widget.Renderer3D, Graphics3D.Listener {        /* platform */

      // Jogl.Window is a wrapper around this com.jogamp.opengl.awt.GLCanvas,
      // a window that will display the contents of a Graphics3D..

      private final GLCanvas glCanvas;
      private final Thread renderThread;
      
      public Window3D () {
         GLProfile glProfile = GLProfile.get(GLProfile.GL3);
         GLCapabilities glCapabilities = new GLCapabilities(glProfile);
         
         System.out.println("System Capabilities:" + glCapabilities.toString());
         System.out.println("Profile Details: " + glProfile.toString());
         System.out.println("Is GL3 Supported?: " + glProfile.isGL3());
         
         glCanvas = new GLCanvas(glCapabilities);
         glCanvas.setPreferredSize(new Dimension(400,300));
         glCanvas.addGLEventListener(this);
         glCanvas.addMouseListener(this);
         glCanvas.addMouseMotionListener(this);
         glCanvas.addMouseWheelListener(this);

         // Currently this object constructs its own top-level frame,
         // but eventually this Widget will have to support...
         // ... being "added" to a Platform.Widget.Container.
         //
         // I guess we'll have ... a package com.generic.platform.desktop,
         //   where desktop.Window will be a class that wraps .. a java.awt.Container?
         //   (currently, this is the code in GLSample.GUI)
         //
         //   when someone calls "addChild" on desktop.Window, it will
         //   have to examine the Platform.Widget it's given...
         //   if it's a Jogl.Window instance, it'll need to access "glCanvas"
         //   to call ... java.awt.Container.add 
         //
         //
         // For Android, we'll have .. a package com.generic.platform.droid,
         //   where droid.Window will wrap ... android.view.View?
         //   (see code in EngineAtivity.java)
         //         
         //   when someone calls "addChild" on droid.Window, it will
         //   have to examine the Platform.Widget it's given...
         //   if it's the android-equivalent of this class, it'll be wrapping
         //   an android.opengl.GLSurfaceView, which will be added to the View,,
         //
         // ...
         // 
         // 
         
         final Frame frame = new Frame() {
            private static final long serialVersionUID = 1L;
            public void update(Graphics g) { paint(g); }
         };
         frame.pack();
         frame.setBackground(java.awt.Color.CYAN);
         frame.setTitle("Window-Name");
         frame.addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent evt) {
              System.exit(0); 
            }
         });
         frame.setVisible(true);
         frame.setSize(new Dimension(400,300));
         frame.add(glCanvas); 
         
         renderThread = new Thread(new Runnable(){
            final static int TargetFPS = 1;
            
            @Override
            public void run() {
               while(true) {
                  try {
                     Thread.sleep(1000 / TargetFPS);
                  } catch (InterruptedException e) {}
                  
                  glCanvas.display();
               }
            }
         });
         renderThread.start();
      }
      
      // ==========================================================
      // Translating a Graphics3D object into GL3 calls
      // ==========================================================
      
      // This Jogl.Window object is displaying the contents of one Graphics3D.
      private Graphics3D graphics3D;

      public void setGraphics3D(Graphics3D graphics3D) {
         if (this.graphics3D != graphics3D) {
            if (this.graphics3D != null) {
               disconnectGraphics3D();
            }
            
            this.graphics3D = graphics3D;
            
            if (this.graphics3D != null) {
               connectGraphics3D();
            }
         }
      }
      
      public void vertexBufferAdded(int vertexBuffer) {
      }
      public void vertexBufferChanged(int vertexBuffer) {
      }
      public void vertexBufferRemoved(int vertexBuffer) {
      }
      public void samplerAdded(int sampler) {
      }
      public void samplerChanged(int sampler) {
      }
      public void samplerRemoved(int sampler) {
      }
      public void shaderAdded(int shader) {
      }
      public void shaderRemoved(int shader) {
      }
      public void commandsChanged() {
      }

      // --------------
      private void connectGraphics3D() {         
         // Connect all vertex-buffers.
         // Connect all samplers.
         // Connect all shaders.
         graphics3D.listeners.add(this);
      }
      private void disconnectGraphics3D() {         
         graphics3D.listeners.remove(this);
         // Mark all vertex-buffers dead.
         // Mark all samplers dead.
         // Mark all shaders dead.       
      }
      
      // ------------------------------------------------------------
      // Now then.  THIS WINDOW class will keep track of all the 
      // vertex-buffers being used in the scene
      
      private HashMap<Integer, GLBuffer> vertexBuffers = new HashMap<Integer, GLBuffer>();

      private static class GLBuffer {
         public final int key;
         public GLBuffer(int key) { 
            this.key = key;
            this.changed = true;
         }
         
         public boolean changed;
         public boolean destroyed;
         
         // The "Data.Array" in Graphics3D has to be FIRST converted
         // into a native Buffer, either IntBuffer or FloatBuffer
         public Buffer nativeBuffer;
         
         
         public Integer glBufferID;
         
         
         public void update(GL3 gl) {
            
         }
      }

      
      
      
      
      // ========================================================
      // Implementing Platform.Widget
      // ========================================================
      
      public Platform.Widget parent() {
         return null;
      }
      public boolean isConnected() {
         return true;
      }
      
      private Image.Size size = null;
      public Image.Size size() {
         return size;
      }
      
      private Platform.Widget.ResizeListener resizeListener = null;
      public void setResizeListener(Platform.Widget.ResizeListener resizeListener) {
         this.resizeListener = resizeListener;
      }
      
      // -----------------------------------------------------------
      // Implementing MouseListener & MouseMotionListener
      // -----------------------------------------------------------
      public void mouseWheelMoved(MouseWheelEvent e) {
      }
      public void mouseDragged(MouseEvent e) {
      }
      public void mouseMoved(MouseEvent e) {
      }
      public void mouseClicked(MouseEvent e) {
      }
      public void mousePressed(MouseEvent e) {
      }
      public void mouseReleased(MouseEvent e) {
      }
      public void mouseEntered(MouseEvent e) {
      }
      public void mouseExited(MouseEvent e) {
      }
      
      // -----------------------------------------------------------
      // Implementing GLEventListener
      // -----------------------------------------------------------
      public void init(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.init called\n");
         // Not sure if we have to do anything here
      }
      public void display(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.display() called\n");
      }
      public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {      
         System.out.format("JOGLWin.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
         Image.Size newSize = Image.Size.of(width, height);
         if (!Objects.equal(newSize, size)) {
            size = newSize;
            if (resizeListener != null) {
               resizeListener.resized();
            }
         }
      }
      public void dispose(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.dispose() called\n");
      }
   }
   
   // ======================================================================
   // GL Interface
   // ======================================================================
   /* 
      
     We had an idea for a "GLManager" that would own all the vertexbuffers
     and would support "sharing" between different 3D-windows in the same "Platform".
     It was abandoned when we made no progress after several weeks, but maybe
     that was just because we were doing other things?
    
   private static class GL3Manager {
      
            
      public void addGraphics3D(Graphics3D graphics3D) {
         this.graphics3D = graphics3D;
      }
      public void removeGraphics3D(Graphics3D graphics3D) {
         this.graphics3D = null;
      }
      
   }
   */
   
   // ======================================================================
   // Platform
   // ======================================================================
   
   private static class JoglPlatform implements Platform {
      
      // Currently Jogl.Window creates a "top-level" window that is a Jogl.Window3D.
      //
      // We believe in the final design we'll still create a "top-level" window,
      // but it will implement Platform.Widget.Container instead.
      // The owner will have to call widgetFactory().newRenderer3D()
      // to construct a Platform.Widget.Renderer3D instance, and he'll call 
      // "addChild" to ADD it to the "top-level" Platform.Widget.Container
      //
      private final Jogl.Window3D window;
      
      public JoglPlatform() {
         window = new Jogl.Window3D();
      }

      public Widget.Renderer3D root3D() {
         return window;  
      }
      
      public Widget.Container rootWidget() { return null; }
      public Widget.Factory widgetFactory() { return null; }
      
      
      public void log(String s, Object... args) {
         System.out.println(String.format(s, args));
      }
      public String loadResource(String name) {
         
         InputStream stream = this.getClass().getResourceAsStream(name);
         BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
         StringBuilder strBuilder = new StringBuilder();
         try {
            String line = reader.readLine();
            // get text from file, line per line
            while(line != null) {
               strBuilder.append(line + "\n");
               line = reader.readLine();  
            }
            // close resources
            reader.close();
            stream.close();
         } catch (IOException e) {
            e.printStackTrace();
         }

         return strBuilder.toString();
      }      
   }   
   
   // ======================================================================
   
   public static Platform platform = null;
   public static Platform platform() {
      if (platform == null) {
         platform = new JoglPlatform(); 
      }
      return platform;
   }
}

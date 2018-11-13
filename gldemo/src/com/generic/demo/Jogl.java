package com.generic.demo;

import com.generic.base.Color;
import com.generic.base.Graphics3D;
import com.generic.base.Image;
import com.generic.base.Image.Size;
import com.generic.base.Platform;
import com.generic.base.Algebra;
import com.generic.base.Algebra.*;
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
   
   private static class Renderer3D implements GLEventListener,                                         /* jogamp   */
                                            MouseListener, MouseMotionListener, MouseWheelListener,  /* awt      */
                                            Platform.Widget.Renderer3D, Graphics3D.Listener {        /* platform */

      // Jogl.Window is a wrapper around this com.jogamp.opengl.awt.GLCanvas,
      // a window that will display the contents of a Graphics3D..

      private final GLCanvas glCanvas;
      private final Thread renderThread;
      
      private final Object lock = new Object();
      private boolean gotInitialSize = false;
      
      public Renderer3D () {
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
         // I guess we'll have ... a package com.generic.platform.linux,
         //   where linux.Window will be a class that wraps .. a java.awt.Container?
         //   (currently, this is the code in GLSample.GUI)
         //
         //   when someone calls "addChild" on linux.Window, it will
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
         
         // We've created the root window, pause until we get a size.
         // (Not sure how we should be doing this... I guess at any given moment
         // a window mihgt not have a size, and owners just need to check for that?)
         System.out.println("RootWindow Started ... waiting for first size");
         synchronized(lock) {
            while (!gotInitialSize) {
               try {
                  lock.wait();
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
            System.out.format(
               "RootWindow ... my size is (%d x %d)", size.width, size.height);
         }
         
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

      

      private void renderGL (GL3 gl) {         
         if (size == null) {
            System.out.format("In renderGL .. but size == null\n");
            return;
         }
         int width = size.width;
         int height = size.height;
         gl.glViewport(0,0,width, height);
         gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
         gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
         
         // -------------------------------------------
         // 
         
         Matrix4x4 modelToView = null;
         Matrix4x4 viewToClip = null;
         Vector3 faceColor = null;
         Vector3 borderColor = null;
         int positionsBuffer = -1;
         int normalsBuffer = -1;
         int baryBuffer = -1;
                  
         for (Graphics3D.Shader.Command command : graphics3D.commands) {
            
            if (command instanceof Graphics3D.Shader.Variable.Binding) {
               if (command instanceof Graphics3D.Shader.Variable.Matrix4x4.Binding) {
                  Graphics3D.Shader.Variable.Matrix4x4.Binding b = (Graphics3D.Shader.Variable.Matrix4x4.Binding) command;
                  if (b.variable == Graphics3D.Shader.MODEL_TO_VIEW) {
                     modelToView = b.value;
                  }
                  if (b.variable == Graphics3D.Shader.VIEW_TO_CLIP) {
                     viewToClip = b.value;
                  }
               }
            
                  
               
            }
            // ... hmmm
            // 
            
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
         // utimately, this is the function that does the OpenGL work...
       
         GL3 gl = drawable.getGL().getGL3();
         renderGL(gl);

      }
      public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {      
         System.out.format("JOGLWin.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
         Image.Size newSize = Image.Size.of(width, height);
         if ((size == null) || !size.equals(newSize)) {
            size = newSize;
            if (resizeListener != null) {
               resizeListener.resized();
            }
         }
         synchronized(lock) {
            if (!gotInitialSize) {
               gotInitialSize = true;
               lock.notify();
            }
         }
      }
      public void dispose(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.dispose() called\n");
      }
   }
   
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
      private Jogl.Renderer3D window = null;
      
      public JoglPlatform() {
      }

      public Widget.Renderer3D root3D() {
         if (window == null) {
            window = new Jogl.Renderer3D();
         }
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

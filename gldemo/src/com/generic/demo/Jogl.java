package com.generic.demo;

import com.generic.base.Color;
import com.generic.base.Graphics3D;
import com.generic.base.Image;
import com.generic.base.Image.Size;
import com.generic.base.Platform;
import com.generic.base.Platform.Widget;
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

public class Jogl implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
                                   Platform.Widget.Renderer3D {
   
   // ======================================================================
   // Window
   // ======================================================================
   
   private static class Window3D implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
                                            com.generic.base.Platform.Widget.Renderer3D {

      // Jogl.Window is a wrapper around this com.jogamp.opengl.awt.GLCanvas,
      // a window that will display the contents of a Graphics3D..

      private final GLCanvas glCanvas;
      private final Jogl.GL3Manager glManager;
      private final Thread renderThread;
      
      public Window3D (Jogl.GL3Manager glManager) {
         this.glManager = glManager;
         
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
         frame.add(glCanvas); 
         frame.pack();
         frame.setBackground(java.awt.Color.CYAN);
         frame.setSize(new Dimension(400,300));
         frame.setTitle("Window-Name");
         frame.addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent evt) {
              System.exit(0); 
            }
         });
         frame.setVisible(true);
         
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
      
      // This Jogl.Window object is displaying the contents of this Graphics3D:
      private Graphics3D graphics3D;
      
      public void setGraphics3D(Graphics3D graphics3D) {
         if (this.graphics3D != graphics3D) {
            if (this.graphics3D != null) {
               glManager.removeGraphics3D(this.graphics3D);
            }
            
            this.graphics3D = graphics3D;
            
            if (this.graphics3D != null) {
               glManager.addGraphics3D(this.graphics3D);
            }
         }
      }
      
      // ----------------------------------------------------------
      // Implementing Platform.Widget
      // ----------------------------------------------------------
      
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
         glManager.display(drawable, graphics3D);
      }
      public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {      
         System.out.format("JOGLWin.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
         Image.Size newSize = Image.Size.of(width, height);
         if (!newSize.equals(size)) {
            size = newSize;
            if (resizeListener != null) {
               resizeListener.resized();
            }
         }
      }
      public void dispose(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.dispose() called\n");
         glManager.dispose(drawable, graphics3D);
      }
   }
   
   // ======================================================================
   // GL Interface
   // ======================================================================
   
   private static class GL3Manager {
      
      private class Graphics3DReference implements Graphics3D.Listener {
         private final Graphics3D graphics3D;
         
         public Graphics3DListener (Graphics3D graphics3D) {
            this.graphics3D = graphics3D;
            this.graphics3D.listeners.add(this);
         }
         public void dispose() {
            this.graphics3D.listeners.remove(this);
         }
         
         public void vertexBufferAdded(int vertexBuffer) {
            GLBuffer newBuffer = new GLBuffer(vertexBuffer);
            vertexBuffers.put(vertexBuffer, null)
         }
         public void vertexBufferRemoved(int vertexBuffer) {
         }
         public void vertexBufferChanged(int vertexBuffer) {
         }
         public void samplerAdded(int sampler) {
         }
         public void samplerRemoved(int sampler) {
         }
         public void samplerChanged(int sampler) {
         }
         public void shaderAdded(int shader) {
         }
         public void shaderRemoved(int shader) {
         }
         public void commandsChanged() {
         }
      }
      
      // TODO soon we'll want to handle multiple Graphics3D objects..
      // but let's get ONE working.
      private Graphics3D graphics3D;
      
      public void addGraphics3D(Graphics3D graphics3D) {
         this.graphics3D = graphics3D;
      }
      public void removeGraphics3D(Graphics3D graphics3D) {
         this.graphics3D = null;
      }
      
      // ----------------------------------------------------------------------------
      // The GL3Manager is reponsible for keeping track of the "vertexBuffers"
      // that have been registered with GL3 contexts..
      // ----------------------------------------------------------------------------
      
      private HashMap<Integer, GLBuffer> vertexBuffers = new HashMap<Integer, GLBuffer>();

      private static class GLBuffer {
         public final int key;
         public GLBuffer(int key) { 
            this.key = key;
            this.changed = true;
         }
         
         public boolean changed;
         public boolean destroyed;
         public Integer glBufferID;
         
         // The "Data.Array" in Graphics3D has to be FIRST converted
         // into a native Buffer, either IntBuffer or FloatBuffer
         public Buffer nativeBuffer;
         
         
         public void update(GL3 gl) {
            
         }
      }
      
      private void setupBuffers(GL3 gl) {
         
      }
      
      
      private Graphics3D graphics3D = null;
      
      
      
      
      
      // -------------------------------------------------------------
      // Now then, actually rendering the Graphics3D content involves
      // iterating over the Graphics3D "commands"..
      // -------------------------------------------------------------
      public void display(GLAutoDrawable drawable, Graphics3D graphics3D) {
         
         
      }
      public void dispose(GLAutoDrawable drawable, Graphics3D graphics3D) {
         // Not sure if we have to do anything here...
      }
   }
   
   // ======================================================================
   // Platform
   // ======================================================================
   
   private static class Platform implements com.generic.base.Platform {

      // The desktop platform will contain a single GL3Manager
      private final GL3Manager glManager;
      
      // Currently Jogl.Window creates a "top-level" window that is a Jogl.Window3D.
      //
      // We believe in the final design we'll still create a "top-level" window,
      // but it will implement Platform.Widget.Container instead.
      // The owner will have to call widgetFactory().newRenderer3D()
      // to construct a Platform.Widget.Renderer3D instance, and he'll call 
      // "addChild" to ADD it to the "top-level" Platform.Widget.Container
      //
      private final Jogl.Window3D window;
      
      public Platform() {
         glManager = new GL3Manager();
         window = new Jogl.Window3D(glManager);
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
   
   public static com.generic.base.Platform platform = null;
   public static com.generic.base.Platform platform() {
      if (platform == null) {
         platform = new Jogl.Platform(); 
      }
      return platform;
   }

   // -------------------------------------------------------------------
   // Interface to Jogl GL
   // -------------------------------------------------------------------
   

   // -------------------------------------------------------------------
   // This JoglWindow class manages "buffers"
   // so let's start with that.
   // -------------------------------------------------------------------
   
   
   public void setGraphics3D(Graphics3D graphics3D) {
      if (this.graphics3D != null) {
        this.graphics3D.listeners.remove(listener);
      }
      
      this.graphics3D = graphics3D;
      
      if (this.graphics3D != null) {
         this.graphics3D.listeners.add(listener);
      }
   }
   
   // ----------------------------------------------------------
   // Implementing Platform.Widget
   // ----------------------------------------------------------
   
   public Platform.Widget parent() {
      return null;
   }
   public boolean isConnected() {
      return true;
   }
   public Image.Size size() {
      return null;
   }
   public void setResizeListener(Platform.Widget.ResizeListener listener) {
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
   }
   public void dispose(GLAutoDrawable drawable) {
      System.out.format("JOGLWin.dispose() called\n");
   }
   public void display(GLAutoDrawable drawable) {
      System.out.format("JOGLWin.display() called\n");
   }
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {      
      System.out.format("JOGLWin.reshape(%d,%d,%d,%d) called\n",x,y,width,height);      
   }
}

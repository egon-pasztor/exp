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
   
   private static class Window implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
                                          com.generic.base.Platform.Widget.Renderer3D {

      // Jogl.Window is a wrapper around this com.jogamp.opengl.awt.GLCanvas,
      // a window that will display the contents of a Graphics3D..

      private final GLCanvas glCanvas;      
      
      public Window() {
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
         //   if it's a Jogl.Window instance, it'll need to access "glCanvas"
         //   to call ... java.awt.Container.add 
         
         // 
         // For web, we can have a ... package com.generic.platform.interactive
         //   which inherits ideas from the Google days..
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
         
         Thread animationThread = new Thread(new Runnable(){
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
         animationThread.start();

      }
      
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
   
   // ======================================================================
   // Platform
   // ======================================================================
   
   private static class Platform implements com.generic.base.Platform {
      private JoglWindow owner;
      public JoglWindowPlatform(JoglWindow owner) {
         this.owner = owner;
      }
      
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
      public Widget.Container rootWidget() { return null; }
      public Widget.Factory widgetFactory() { return null; }
      
      public Widget.Renderer3D root3D() {
        return owner;  
      }
   }
   private final JoglWindowPlatform platform;
   public Platform platform() { return platform; }

   // -------------------------------------------------------------------
   // Interface to Jogl GL
   // -------------------------------------------------------------------
   
   
   

   // -------------------------------------------------------------------
   // Interface to Jogl GL
   // -------------------------------------------------------------------
   
   private final GLCanvas glCanvas;   
   
   public Jogl() {
      platform = new JoglWindowPlatform(this);
      
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
      
      Thread animationThread = new Thread(new Runnable(){
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
      animationThread.start();
      
   }

   // -------------------------------------------------------------------
   // This JoglWindow class manages "buffers"
   // so let's start with that.
   // -------------------------------------------------------------------
   
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
   
   
   private class Graphics3DListener implements Graphics3D.Listener {
      public void vertexBufferAdded(int vertexBuffer) {
         GLBuffer newBuffer = new GLBuffer(vertexBuffer);
         vertexBuffers.put(vertexBuffer, value)
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
   private final Graphics3DListener listener = new Graphics3DListener ();
   private Graphics3D graphics3D = null;
   
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

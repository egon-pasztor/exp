package com.generic.demo;

import com.generic.base.Platform;
import com.generic.base.Platform.Root3DWidget;
import com.generic.base.Platform.Widget;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.Color;
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

public class JoglWindow implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener {
   
   // -------------------------------------------------------------------
   
   private class JoglWindowPlatform implements Platform {
      
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
      
      public Root3DWidget root3D() {
        return null;  
      }
   }
   // -------------------------------------------------------------------
   
   private final GLCanvas glCanvas;   
   private final JoglWindowPlatform platform;   

   public Platform platform() { return platform; }
   
   public JoglWindow() {
      
      platform = new JoglWindowPlatform();
      
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
      frame.setBackground(Color.CYAN);
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
   
   // upon startup:
   //   JOGLWin.init called
   //   JOGLWin.reshape(0,0,400,300) called
   //   JOGLWin.display() called
   //   JOGLWin.display() called
   //   ....
   // upon dragging from 1 window to another
   //   ...
   //   JOGLWin.display() called
   //   JOGLWin.display() called
   //   JOGLWin.dispose() called
   //   JOGLWin.init called
   //   JOGLWin.reshape(0,0,631,397) called
   //   JOGLWin.display() called
   //   JOGLWin.display() called
   //   ...

   

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

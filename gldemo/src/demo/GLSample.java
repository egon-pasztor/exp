package demo;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.JFrame;

import demo.VectorAlgebra.*;
import demo.GLMath.*;

public class GLSample implements GLEventListener, MouseListener, MouseMotionListener {

   private final GLCanvas glCanvas;
   private final GLWorld  world;
   
   private final long startTimeMillis;
   
   // -----------------------------------------------------------
   // Constructor
   // -----------------------------------------------------------

   public GLSample() {
      System.out.println("GLSample constructor BEGIN\n");
      
      world = new GLWorld(/* cube */ false, /* ico */ true, /* ball */ false, /* subdivide */ 1);
      //world = new GLWorld(/* cube */ true, /* ico */ false, /* ball */ false, /* subdivide */ 0);
      
      GLProfile glProfile = GLProfile.get(GLProfile.GL3);
      GLCapabilities glCapabilities = new GLCapabilities(glProfile);
      
      System.out.println("System Capabilities:" + glCapabilities.toString());
      System.out.println("Profile Details: " + glProfile.toString());
      System.out.println("Is GL3 Supported?: " + glProfile.isGL3());
      
      glCanvas = new GLCanvas(glCapabilities);
      glCanvas.setPreferredSize(new Dimension(400,400));
      glCanvas.addGLEventListener(this);
      glCanvas.addMouseListener(this);
      glCanvas.addMouseMotionListener(this);
      
      // Start animation thread
      startTimeMillis = System.currentTimeMillis();
      Thread animationThread = new Thread(new Runnable(){
         final static int TargetFPS = 30;
         
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
      System.out.println("GLSample constructor END\n");
      
      
   }
   
   // -----------------------------------------------------------
   // Implementing GLEventListener
   // -----------------------------------------------------------

   /** INIT */
   @Override
   public void init(GLAutoDrawable drawable) {
      System.out.format("GLSample.init called\n");
      
      GL3 gl = drawable.getGL().getGL3();
      gl.glEnable(GL.GL_DEPTH_TEST);
      //gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

      world.setupProgram(gl);
      world.setupBuffers(gl);
      world.setupTexture(gl);
   }

   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.format("GLSample.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
      
      world.cameraBall = new CameraBall(width, height,
                                  new Vector3f(0.0f, 0.0f, 0.0f),   // look-at
                                  new Vector3f(0.0f, 0.0f, 10.0f),   // camera-pos
                                  new Vector3f(0.0f, 1.0f, 0.0f),   // camera-up
                                  53.13f);
      
      GL3 gl = drawable.getGL().getGL3();
      //gl.glViewport(0, 0,  width,  height);
   }

   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      //System.out.format("GLSample.display() called\n");
      
      final int MillisPerCycle = 2000;
      long currentTime = System.currentTimeMillis();
      float phase = (float) (((float)(currentTime - startTimeMillis) / MillisPerCycle) * (Math.PI * 2.0));
            
      world.updateTranslations(phase);
      
      GL3 gl = drawable.getGL().getGL3();
      world.render(gl);
   }

   /** GL Complete */
   @Override
   public void dispose(GLAutoDrawable drawable) {
      System.out.format("GLSample.dispose() called\n");
   }

   // -----------------------------------------------------------
   // Implementing MouseListener & MouseMotionListener
   // -----------------------------------------------------------

   @Override
   public void mouseClicked(MouseEvent e) {
      //System.out.format("GLSample.mouseClicked() called\n");
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      //System.out.format("GLSample.mouseEntered() called\n");
   }

   @Override
   public void mouseExited(MouseEvent e) {
      //System.out.format("GLSample.mouseExited() called\n");
   }

   @Override
   public void mousePressed(MouseEvent e) {
      //System.out.format("GLSample.mousePressed() called\n");
      world.cameraBall.grab(e.getX(), e.getY(),
            e.isShiftDown()   ? (e.isControlDown() ? CameraBall.GrabType.FOV : CameraBall.GrabType.Zoom)
                              : (e.isControlDown() ? CameraBall.GrabType.Pan : CameraBall.GrabType.Rotate));
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      //System.out.format("GLSample.mouseReleased() called\n");
      world.cameraBall.release();
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      //System.out.format("GLSample.mouseMoved(%d,%d) called\n", e.getX(), e.getY());
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      //System.out.format("GLSample.mouseDragged(%d,%d) called\n", e.getX(), e.getY());
      world.cameraBall.moveTo(e.getX(), e.getY());
      glCanvas.display();
   }
   
   // -----------------------------------------------------------
   // MAIN
   // -----------------------------------------------------------

   public static void main(String[] args) {
      System.out.format("Hello world..\n");
      
      VectorAlgebra.testInverse3();
      VectorAlgebra.testInverse2();
      VectorAlgebra.testRotation();

      GLSample sample = new GLSample();
      
      JFrame frame = new JFrame("GL Sample");
      frame.setBounds(50, 50, 400, 400);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(sample.glCanvas);
      frame.setVisible(true);
   }
}

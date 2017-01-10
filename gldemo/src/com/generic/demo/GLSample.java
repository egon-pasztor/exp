package com.generic.demo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

import com.generic.base.Camera;
import com.generic.base.Shader;
import com.generic.base.Geometry;
import com.generic.base.VectorAlgebra;
import com.generic.base.World;

import com.generic.base.Geometry.*;
import com.generic.base.Raster.Image;
import com.generic.base.VectorAlgebra.*;
import com.generic.base.World.*;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.imageio.ImageIO;

public class GLSample implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener {

   // -----------------------------------------------------------
   // MAIN
   // -----------------------------------------------------------

   public static void main(String[] args) {
      System.out.format("Hello world..\n");
      new GLSample();
   }

   // -----------------------------------------------------------
   // Constructor
   // -----------------------------------------------------------

   private final DemoWorld demoWorld;
   public Camera.Controller cameraController;
   
   private final GLCanvas glCanvas;   
   private final GUI gui;
   
   private boolean intersectionIn3d;
   private final long startTimeMillis;

   
   public GLSample() {
      System.out.println("GLSample constructor BEGIN\n");

      // Create "Demo World" object...
      
      Image leaImage = imageFromResource("lea.png");
      Image teapotImage = imageFromResource("teapot.png");
      demoWorld = new DemoWorld(leaImage, teapotImage);
      intersectionIn3d = false;
      
      // Create "GLCanvas" (extends Canvas):
      
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
      glCanvas.addMouseWheelListener(this);

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
               gui.viewController.repaint();
            }
         }
      });
      
      animationThread.start();
      
      
      // Create GUI and frame window
      gui = new GUI();
      
      final Frame frame = new Frame() {
         private static final long serialVersionUID = 1L;
         public void update(Graphics g) { paint(g); }
      };
      frame.add(gui); 
      frame.pack();
      frame.setBackground(backgroundColorScheme.bgColor);
      frame.setSize(new Dimension(450,450));
      frame.setTitle("Grid Mapping");
      frame.addWindowListener(new WindowAdapter() { 
         public void windowClosing(WindowEvent evt) 
           { System.exit(0); }
      });
      frame.setVisible(true);
      
      System.out.println("GLSample constructor END\n");
   }

   
   private void updateUVPointer(float x, float y) {
      Vector2f newSelectedUVPoint = new Vector2f(x,y);
      if ((demoWorld.selectedUVPointIfAny == null) || !newSelectedUVPoint.equals(demoWorld.selectedUVPointIfAny)) {
         demoWorld.selectedUVPointIfAny = newSelectedUVPoint;
         gui.viewController.repaint();
      }
   }
   private void clearUVPointer() {
      if (demoWorld.selectedUVPointIfAny != null) {
         demoWorld.selectedUVPointIfAny = null;
         gui.viewController.repaint();
      }
   }


   // -----------------------------------------------------------
   // Color
   // -----------------------------------------------------------

   public static class ColorScheme {

      public final Color hiColor;
      public final Color bgColor;
      public final Color loColor;

      public final Color darkTextColor;
      public final Color darkLineColor;
      public final Color mediumLineColor;
      public final Color lightLineColor;
      
      private final float h;
      private final float s;
      private final float b;

      public ColorScheme(Color baseColor) {
         float[] baseHSV = new float[3];
         Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), baseHSV);
         h = baseHSV[0];
         s = baseHSV[1];
         b = baseHSV[2];

         hiColor = shiftB(20);
         bgColor = shiftB(0);
         loColor = shiftB(-20);
         
         darkTextColor = shiftB(-36);
         
         darkLineColor = shiftB(-28);
         mediumLineColor = shiftB(-14);
         lightLineColor = shiftB(-7);
      }
      
      public Color shiftB (int shift) {
         float newB = Math.max(Math.min(b + shift/100.0f, 1.0f), 0.0f);
         return Color.getHSBColor(h,s,newB);
      }
   }
   
   public static final ColorScheme backgroundColorScheme = new ColorScheme(
      Color.getHSBColor((181.0f/360.0f), (56.0f/100.0f), (77.0f/100.0f)));

   public static void drawBorder (Graphics g, int x, int y, int w, int h, 
                                  int t, Color tlColor, Color brColor) {
      for(int i=0; i<t; i++) {
          g.setColor(tlColor);
          g.drawLine(x,y,x,y+h-1); 
          g.drawLine(x+1,y,x+w-2,y); 
          g.setColor(brColor);
          g.drawLine(x+1,y+h-1,x+w-1,y+h-1); 
          g.drawLine(x+w-1,y+h-2,x+w-1,y);
          x++; y++; w-=2; h-=2;
      }
   }

   // -----------------------------------------------------------------------
   // Top level Container with a Label and a ViewController
   // -----------------------------------------------------------------------

   // many of our programs from Engine to Advisor have this pattern
   //   where we define PanelClass extends Container,
   //      with "doLayout" and "paint" methods...
   //   we enjoy a style of "beveled border" that we've stuck with for 20 years...
   //   along with a "ColorSchema" class..
   
   private class GUI extends Container {
      private static final long serialVersionUID = 1L;        
      public ViewController viewController;
      public Label label;
        
      final int outerBorderSize = 2;
      final int innerBorderSize = 3;
      
      final int topMargin = 10;
      final int bottomMargin = 20;
      final int middleMargin = 10;
      final int leftMargin = 10;
      final int rightMargin = 10;
      
      public GUI() {
         label = new Label();
         label.setFont(new Font(null, Font.PLAIN, 12));
         label.setBackground(backgroundColorScheme.bgColor);
         add(label);
         
         add(glCanvas);
         
         viewController = new ViewController(label);
         add(viewController);
      }
      
      public void update(Graphics g) { paint(g); }
      public void paint(Graphics g)  { 
         super.paint(g);
         
         Dimension dim = getSize();
         int w = dim.width, h = dim.height;
         if ((w>0) && (h>0)) {

            int verticalSpace = h - outerBorderSize*2 - topMargin - bottomMargin - middleMargin - innerBorderSize*4;
            
            int panel1Height = verticalSpace/2;
            int panel2Height = verticalSpace-panel1Height;
            
            int panel1Top  = outerBorderSize+topMargin+innerBorderSize;
            int panel2Top  = panel1Top+panel1Height+innerBorderSize*2+middleMargin;
            
            
            drawBorder(g,
                  0,0,w,h,outerBorderSize, 
                  backgroundColorScheme.hiColor,
                  backgroundColorScheme.loColor);
            
            g.setColor(backgroundColorScheme.bgColor);
            g.fillRect(outerBorderSize, outerBorderSize, 
                  (w-outerBorderSize*2), topMargin);
            
            g.fillRect(outerBorderSize, outerBorderSize+topMargin, 
                  leftMargin,  (h - outerBorderSize*2 -topMargin));
            g.fillRect(w-outerBorderSize-rightMargin, outerBorderSize+topMargin, 
                  rightMargin, (h - outerBorderSize*2 -topMargin));
            
            // ----------- 
            drawBorder(g,
                  outerBorderSize+leftMargin,
                  panel1Top - innerBorderSize,
                  (w - outerBorderSize*2 -leftMargin-rightMargin),
                  panel1Height + 2 * innerBorderSize, 
                  innerBorderSize-1, 
                  backgroundColorScheme.loColor,
                  backgroundColorScheme.hiColor);

            g.setColor(Color.BLACK);
            g.drawRect(
                  outerBorderSize+leftMargin+innerBorderSize-1,
                  panel1Top-1,
                  (w - outerBorderSize*2 - leftMargin-rightMargin - innerBorderSize*2)+1,
                  panel1Height+1);
            // ----------- 
            
            g.setColor(backgroundColorScheme.bgColor);
            g.fillRect(outerBorderSize, panel2Top-innerBorderSize-middleMargin, 
                  (w-outerBorderSize*2), middleMargin);
            
            // ----------- 
            drawBorder(g,
                  outerBorderSize+leftMargin,
                  panel2Top - innerBorderSize,
                  (w - outerBorderSize*2 -leftMargin-rightMargin),
                  panel2Height + 2 * innerBorderSize, 
                  innerBorderSize-1, 
                  backgroundColorScheme.loColor,
                  backgroundColorScheme.hiColor);

            g.setColor(Color.BLACK);
            g.drawRect(
                  outerBorderSize+leftMargin+innerBorderSize-1,
                  panel2Top-1,
                  (w - outerBorderSize*2 - leftMargin-rightMargin - innerBorderSize*2)+1,
                  panel2Height+1);
            // ----------- 
         }
      }
      
      public void doLayout() {
         Dimension size = getSize();
         int w  = size.width, h = size.height;

         int verticalSpace = h - outerBorderSize*2 - topMargin - bottomMargin - middleMargin - innerBorderSize*4;
         
         int panel1Height = verticalSpace/2;
         int panel2Height = verticalSpace-panel1Height;
         
         int panel1Top  = outerBorderSize+topMargin+innerBorderSize;
         int panel2Top  = panel1Top+panel1Height+innerBorderSize*2+middleMargin;
         
         glCanvas.setBounds(
               outerBorderSize+leftMargin+innerBorderSize,
               panel1Top,
               (w - outerBorderSize*2 - leftMargin-rightMargin - innerBorderSize*2),
               panel1Height);
         
         viewController.setBounds(
               outerBorderSize+leftMargin+innerBorderSize,
               panel2Top,
               (w - outerBorderSize*2 - leftMargin-rightMargin - innerBorderSize*2),
               panel2Height);

         label.setBounds(outerBorderSize+leftMargin, h-outerBorderSize-bottomMargin, 
               (w - outerBorderSize*2 - leftMargin-rightMargin), 
               bottomMargin);
      }
   }

   // -----------------------------------------------------------------------
   // ViewController
   // -----------------------------------------------------------------------

   // So.  ViewController/Viewport is used here and there's a copy used in Advisor,
   //   it is a mouse/motion/wheel-listener that maintains its own "dragInProgress" bool,
   //   (so, is it the 2d analog to "Camera" or to the code that calls "CameraController" methods?
   //    is CameraController's "dragInProgress" bool in the right place?)
   //
   // 
   
   private class ViewController extends Container 
              implements MouseListener, MouseMotionListener, MouseWheelListener {
      private static final long serialVersionUID = 1L;
       
      // -----------------------------------------------------
      
      private final Viewport view;
      private final Label statusLabel;
      private java.awt.Image image;
      private int renderTimeMillis;
       
      // This should be broken out and called "Controller" or something.
      public boolean dragInProgress;
      public int dragX;
      public int dragY;
      public Point2D.Double dragCenterAtStart;

      // -----------------------------------------------------
      
      public ViewController(Label statusLabel) {
         this.view = new Viewport();
         this.statusLabel = statusLabel;
          
         addMouseListener(this);
         addMouseMotionListener(this);
         addMouseWheelListener(this);
       }
     
      
      // RESIZING the window
      //
      public void doLayout() {
         int w = getSize().width, h = getSize().height;
         // System.out.printf("ViewPanel.doLayout at (%d x %d)\n", w,h);
         if ((w > 0) && (h > 0)) {
            image = createImage(w, h);
            view.resize(w,h);
         }
      }

      // PAINTING the window
      //
      public void paint(Graphics g)  {
         super.paint(g);

         if (image != null) {
            if (view.changed()) {
               long timeBefore = System.currentTimeMillis();
               view.paint(image);
               long timeAfter = System.currentTimeMillis();
               renderTimeMillis = (int)(timeAfter - timeBefore);
            }
            g.drawImage(image, 0, 0, null); 
            
            if (demoWorld.selectedUVPointIfAny != null) {
               int px = view.xToHPixel(demoWorld.selectedUVPointIfAny.x);
               int py = view.yToVPixel(demoWorld.selectedUVPointIfAny.y);
               
               g.setColor(Color.RED);
               int w = image.getWidth(null);
               int h = image.getHeight(null);
               g.drawLine(0,py,w,py);
               g.drawLine(px,0,px,h);
               
               g.setColor(Color.BLACK);
               g.fillRect(px-3, py-3,  7, 7);
               
               
               g.setColor(Color.YELLOW);
               g.fillRect(px-2, py-2,  5, 5);
               
               
            }
            
            statusLabel.setText(
                  String.format("Copy %d ms: ", renderTimeMillis) +
                  view.status() + 
                  ((demoWorld.selectedUVPointIfAny != null) ? String.format("Pointer: <%g,%g>", 
                        demoWorld.selectedUVPointIfAny.x, demoWorld.selectedUVPointIfAny.y) : ""));
          }
      }
      public void update(Graphics g) { paint(g); }

      // MOUSE events
      //
      @Override
      public void mouseClicked(MouseEvent e) {
         //System.out.printf("Mouse clicked (%d,%d)\n", 
         //      e.getX(), e.getY());
      }
      @Override
      public void mousePressed(MouseEvent e) {
         //System.out.printf("Mouse pressed (%d,%d)\n", 
         //      e.getX(), e.getY());

         if (!dragInProgress) {
            dragInProgress = true;
            dragX = e.getX();
            dragY = e.getY();
            dragCenterAtStart = view.getCenter();
         }
      }
      @Override
      public void mouseReleased(MouseEvent e) {
         //System.out.printf("Mouse released (%d,%d)\n", 
         //      e.getX(), e.getY());
            
         dragInProgress = false;
         dragX = 0;
         dragY = 0;
         dragCenterAtStart = null;
      }
      @Override
      public void mouseEntered(MouseEvent e) {
         //System.out.printf("Mouse entered (%d,%d)\n", 
         //      e.getX(), e.getY());
            
         updateUVPointer(e.getX(), e.getY());
      }
      @Override
      public void mouseExited(MouseEvent e) {
         //System.out.printf("Mouse exited (%d,%d)\n", 
         //      e.getX(), e.getY());
          
         GLSample.this.clearUVPointer();
      }
      @Override
      public void mouseDragged(MouseEvent e) {
         //System.out.printf("Mouse dragged (%d,%d)\n", 
         //      e.getX(), e.getY());
            
         if (dragInProgress) {
            int dX = e.getX() - dragX;
            int dY = e.getY() - dragY;
               
            Point2D.Double deviation = view.pan(dragCenterAtStart, dX, dY);
            dragCenterAtStart.x += deviation.x;
            dragCenterAtStart.y += deviation.y;
         }
         updateUVPointer(e.getX(), e.getY());
      }
      @Override
      public void mouseMoved(MouseEvent e) {
         //System.out.printf("Mouse moved (%d,%d)\n", 
         //      e.getX(), e.getY());
            
         updateUVPointer(e.getX(), e.getY());
      }
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
         final double step = (e.isAltDown() ? 1.02 : 1.2);
         double factor = ((e.getPreciseWheelRotation() > 0) ? step : (1.0/step));
         
         if (image != null) {
            int h = image.getHeight(null);
            view.zoom(e.getX(), h-e.getY(), factor, e.isControlDown(), !e.isShiftDown());
         }
         repaint();
      }
      
      private void updateUVPointer(int x, int y) {
         GLSample.this.updateUVPointer(view.hPixelToX(x), view.vPixelToY(y));
      }
   }
   
   
   // Viewport is apparently trying to be the 2D version of Camera?
   // Actually no, it's got a "paint" method.  That sucks.
   // But we would like a "paint" method, you know...
   
   

   private class Viewport {
      private boolean needsUpdate;
      private int w;
      private int h;
      
      private float xMin,xMax,yMin,yMax;
      
      public int xToHPixel(float x) {
         return (int) (w * (x-xMin)/(xMax-xMin));
      }
      public int yToVPixel(float y) {
         return h - (int) (h * (y-yMin)/(yMax-yMin));
      }
      public float hPixelToX(float x) {
         return xMin + ((float)x)/((float)w) * (xMax-xMin);
      }
      public float vPixelToY(int y) {
         return yMin + ((float)(h-y))/((float)h) * (yMax-yMin);
      }
      
      public Viewport() {
         needsUpdate = true;      
      }
      public boolean changed() {
         return needsUpdate;
      }
      public void setChanged() {
         needsUpdate = true;
      }
      
      public void resize (int w, int h) {
         System.out.format("Viewport.resize(%d,%d)\n", w,h);
         this.w = w;
         this.h = h;
         
         xMin = -4;
         xMax = +4;
         yMin = ((float)h/(float)w) * xMin;
         yMax = ((float)h/(float)w) * xMax;
         
         needsUpdate = true;
      }
      public void zoom (int pX, int pY, double factor, boolean ctrl, boolean shift) {
         System.out.format("Viewport.zoom(%d,%d,%g,%s,%s)\n", pX,pY,factor,ctrl?"ctrl":"",shift?"shift":"");

         float oldDxPerPixel = ((xMax-xMin)/((float)w));
         float oldDyPerPixel = ((yMax-yMin)/((float)h));
         
         Point2D.Double oldCenter = getCenter();
         
         float zoomCenterX = (float)oldCenter.x + (pX-(w/2)) * oldDxPerPixel;
         float zoomCenterY = (float)oldCenter.y + (pY-(h/2)) * oldDyPerPixel;

         float newDxPerPixel = oldDxPerPixel * (float)factor;
         float newDyPerPixel = oldDyPerPixel * (float)factor;

         float newCenterX = zoomCenterX + ((float)oldCenter.x - zoomCenterX) * (float)factor;
         float newCenterY = zoomCenterY + ((float)oldCenter.y - zoomCenterY) * (float)factor;
         
         xMin = newCenterX - (w/2.0f)*newDxPerPixel;
         xMax = newCenterX + (w/2.0f)*newDxPerPixel;
         yMin = newCenterY - (h/2.0f)*newDyPerPixel;
         yMax = newCenterY + (h/2.0f)*newDyPerPixel;
         
         needsUpdate = true;
      }
      public Point2D.Double pan (Point2D.Double dragCenter, int dX, int dY) {     
         System.out.format("Viewport.pan(%g,%g,%d,%d)\n", dragCenter.x, dragCenter.y, dX,dY);
         
         float dxPerPixel = ((xMax-xMin)/((float)w));
         float dyPerPixel = ((yMax-yMin)/((float)h));
         
         xMin = (float)dragCenter.x - (w/2.0f)*dxPerPixel;
         xMax = (float)dragCenter.x + (w/2.0f)*dxPerPixel;
         yMin = (float)dragCenter.y - (h/2.0f)*dyPerPixel;
         yMax = (float)dragCenter.y + (h/2.0f)*dyPerPixel;

         float dRX = -dX * dxPerPixel;
         float dRY = +dY * dyPerPixel;
         
         xMin += dRX; xMax += dRX;
         yMin += dRY; yMax += dRY;
         
         needsUpdate = true;
         
         // NOTE:  pan is returning a position, which indicates the amount that
         // the actual movement deviated from the requested movement.
         return new Point2D.Double(0,0);
      }
      public Point2D.Double getCenter() {
         return new Point2D.Double((xMin+xMax)/2,(yMin+yMax)/2);
      }
      

      
      // --------------------------------------------
      // okay for a given dx, what's the pixel difference?
      public float getDPixelPerDx() {
         return w / (xMax-xMin);
      }
      public float getDPixelPerDy() {
         return h / (yMax-yMin);
      }
      
      public class AxisScaling {
         public final float dCoord;
         public final boolean isMultipleOf10;
         public AxisScaling (float dCoord, boolean isMultpleOf10) {
            this.dCoord = dCoord;
            this.isMultipleOf10 = isMultpleOf10;
         }
         public AxisScaling nextSmaller() {
            return new AxisScaling(dCoord * (isMultipleOf10 ? 0.5f : 0.2f), !isMultipleOf10);
         }
         public AxisScaling nextLarger() {
            return new AxisScaling(dCoord * (isMultipleOf10 ? 5.0f : 2.0f), !isMultipleOf10);
         }
         public float pixelDifference() {
            return (w / (xMax-xMin)) * dCoord;
         }
      }
      public void drawLine(Graphics2D g, int x0, int y0, int x1, int y1, int colorType) {
         if (colorType == 0) {
            g.setColor(backgroundColorScheme.lightLineColor);
            g.drawLine(x0, y0, x1, y1);
         }
         if (colorType == 1) {
            g.setColor(backgroundColorScheme.mediumLineColor);
            g.drawLine(x0, y0, x1, y1);
         }
         if (colorType == 2) {
            g.setColor(backgroundColorScheme.darkLineColor);
            g.drawLine(x0, y0, x1, y1);
         }
         if (colorType == 3) {
            g.setColor(backgroundColorScheme.darkLineColor);
            g.drawLine(x0+1, y0+1, x1+1, y1+1);
         }
         if (colorType == 4) {
            g.setColor(Color.BLACK);
            g.drawLine(x0, y0, x1, y1);
            g.drawLine(x0+1, y0+1, x1+1, y1+1);
            g.drawLine(x0-1, y0-1, x1-1, y1-1);
         }
      }
      public void paintGrid(Graphics2D g, AxisScaling scaling, int colorType) {
         float sxMin = xMin / scaling.dCoord;
         float sxMax = xMax / scaling.dCoord;
         float syMin = yMin / scaling.dCoord;
         float syMax = yMax / scaling.dCoord;
         
         int xLineL = (int) sxMin; while (xLineL > sxMin) xLineL--;
         int xLineR = (int) sxMax; while (xLineR < sxMax) xLineR++;         
         int yLineL = (int) syMin; while (yLineL > syMin) yLineL--;
         int yLineR = (int) syMax; while (yLineR < syMax) yLineR++;
         
         g.setColor(backgroundColorScheme.darkLineColor);
         for (int xLine = xLineL; xLine <= xLineR; xLine++) {
            int x = xToHPixel(xLine * scaling.dCoord);
            drawLine(g, x, 0, x, h, colorType);
         }
         for (int yLine = yLineL; yLine <= yLineR; yLine++) {
            int y = yToVPixel(yLine * scaling.dCoord);
            drawLine(g, 0, y, w, y, colorType);
         }         
      }
      public AxisScaling UNIT_SCALING = new AxisScaling(1.0f, true);
      
      
      
      public void paintGrid(Graphics2D g) {
         float dPixelPerDx = getDPixelPerDx();
         float dPixelPerDy = getDPixelPerDy();
         
         AxisScaling scaling = UNIT_SCALING;
         while (scaling.pixelDifference() < 10) {
            scaling = scaling.nextLarger();
         }
         while (scaling.pixelDifference() >= 10) {
            scaling = scaling.nextSmaller();
         }
         scaling = scaling.nextLarger();
         
         // now scaling is the smallest/finest division that has a
         // pixel difference greater than 10.   these lines will be "fine"
         //
         paintGrid(g, scaling, 0);
         scaling = scaling.nextLarger();
         
         paintGrid(g, scaling, 1);
         scaling = scaling.nextLarger();

         paintGrid(g, scaling, 2);
         scaling = scaling.nextLarger();
         
         paintGrid(g, scaling, 3);
         scaling = scaling.nextLarger();
         
         paintGrid(g, scaling, 4);
         scaling = scaling.nextLarger();
      }
      
      public void paint(java.awt.Image image) {
         needsUpdate = false;
         
         Graphics2D g = (Graphics2D) image.getGraphics();
         g.setColor(new Color(120,180,230));
         g.fillRect(0,0,w,h);
         
         paintGrid(g);
         
         /* --------------------------------------------- */
         // Below is attempt #1 to draw grid lines...
         /* --------------------------------------------- */
         /*
         float xWidth = (xMax-xMin);
         float exp = 1.0f;
         while (xWidth >= 10.0f) {
            xWidth /= 10.0;
            exp /= 10;
         }
         while (xWidth < 1.0f) {
            xWidth *= 10.0;
            exp *= 10;
         }
         
         float sxMin = exp*xMin;
         float sxMax = exp*xMax;
         float syMin = exp*yMin;
         float syMax = exp*yMax;
         
         int xLineL = (int) sxMin; while (xLineL > sxMin) xLineL--;
         int xLineR = (int) sxMax; while (xLineR < sxMax) xLineR++;         
         int yLineL = (int) syMin; while (yLineL > syMin) yLineL--;
         int yLineR = (int) syMax; while (yLineR < syMax) yLineR++;
         
         g.setColor(backgroundColorScheme.darkLineColor);
         for (int xLine = xLineL; xLine <= xLineR; xLine++) {
            int x = xToHPixel(xLine/exp);
            g.drawLine(x, 0, x, h);
            if ((xLine%10)==0) {
               g.drawLine(x+1, 0, x+1, h);
            }
            if (xLine==0) {
               g.drawLine(x-1, 0, x-1, h);
            }
         }
         for (int yLine = yLineL; yLine <= yLineR; yLine++) {
            int y = yToVPixel(yLine/exp);
            g.drawLine(0, y, w, y);
            if ((yLine%10)==0) {
               g.drawLine(0, y+1, w, y+1);
            }
            if (yLine==0) {
               g.drawLine(0, y-1, w, y-1);
            }
         }
           */
         /* --------------------------------------------- */
         // find the smallest DX that maps to greater than 30 pixels ...
         //   
         
         /* --------------------------------------------- */

         
         
         // -------------------------------------------------------
         // Can we draw each "triangle" in demoWorld.mappingModel?
         // -------------------------------------------------------
         
         g.setColor(Color.BLACK);
         Composite c0 = g.getComposite();
         Composite c1 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f);
         for (Mesh.Triangle t : demoWorld.mappingModel.mesh.interiorTriangles) {
            Geometry.FlatFaceInfo fi = (Geometry.FlatFaceInfo) t.getData();
            
            Vector2f p0 = fi.tex0;
            int p0x = xToHPixel(p0.x);
            int p0y = yToVPixel(p0.y);
            
            Vector2f p1 = fi.tex1;
            int p1x = xToHPixel(p1.x);
            int p1y = yToVPixel(p1.y);
            
            Vector2f p2 = fi.tex2;
            int p2x = xToHPixel(p2.x);
            int p2y = yToVPixel(p2.y);
            
            g.setComposite(c1);
            g.setColor(Color.GREEN);
            g.fillPolygon(new int[] {p0x, p1x, p2x}, new int[] {p0y, p1y, p2y}, 3);
            
            g.setComposite(c0);
            g.setColor(Color.BLACK);
            g.drawLine(p0x, p0y, p1x, p1y);
            g.drawLine(p1x, p1y, p2x, p2y);
            g.drawLine(p2x, p2y, p0x, p0y);
         }
      }
      
      public String status() {
         String result = String.format("Viewport [%d x %d], (%g to %g) by (%g to %g)", 
               w,h, xMin,xMax, yMin,yMax);
         return result;
      }
   }

   // -------------------------------------------------------------------
   // Loading from RESOURCEs:
   // -------------------------------------------------------------------

   public static Image imageFromResource(String name) {
      System.out.format("Trying to load image named [%s]\n", name);
      BufferedImage im;
      try {
          im = ImageIO.read(Image.class.getResource(name));
      } catch (IOException e) {
          System.out.format("FAILED - Trying to load image named [%s]\n", name);
          e.printStackTrace();
          return null;
      }

      Image res = new Image(name, im.getWidth(), im.getHeight());
      for (int row = 0; row < res.height; row++) {
         for (int col = 0; col < res.width; col++) {
            int val = im.getRGB(col, row);
            res.pixels[col+row*res.width] = val;
            if ((row == 0) && (col == 0)) {
               System.out.format("At (0,0) we got 0x%8x\n", val);
            }
         }
      }
      return res;
   }
   private String loadStringFileFromCurrentPackage(String fileName){
      InputStream stream = this.getClass().getResourceAsStream(fileName);

      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      // allocate a string builder to add line per line 
      StringBuilder strBuilder = new StringBuilder();

      try {
         String line = reader.readLine();
         // get text from file, line per line
         while(line != null){
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
   
   
   // -----------------------------------------------------------
   // Implementing GLEventListener
   // -----------------------------------------------------------

   /** INIT */
   @Override
   public void init(GLAutoDrawable drawable) {
      System.out.format("GLSample.init called\n");
      System.out.format("----------------------\n");
      
      GL3 gl = drawable.getGL().getGL3();
      
      int[] result = new int[1];
      gl.glGetIntegerv(GL3.GL_MAX_VERTEX_ATTRIBS, result,0);
      System.out.format("GL_MAX_VERTEX_ATTRIBS:  I found a %d in result\n", result[0]);
      
      initGL(gl);
      System.out.format("----------------------\n");
   }

   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.format("GLSample.reshape(%d,%d,%d,%d) called\n",x,y,width,height);

      Camera initialCamera = new Camera(width, height,
         new Vector3f(0.0f, 0.0f, 0.0f),   // look-at
         new Vector3f(0.0f, 0.0f, 18.0f),   // camera-pos
         new Vector3f(0.0f, 1.0f, 0.0f),   // camera-up
         53.13f/2.0f);

      cameraController = new Camera.Controller(initialCamera);
   }

   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      demoWorld.updateDemoWorld(System.currentTimeMillis() - startTimeMillis);      
      GL3 gl = drawable.getGL().getGL3();
      renderGL(gl);
   }

   /** GL Complete */
   @Override
   public void dispose(GLAutoDrawable drawable) {
      System.out.format("GLSample.dispose() called\n");
   }

   // -----------------------------------------------------------
   // Implementing MouseListener & MouseMotionListener
   // -----------------------------------------------------------

   Integer hoverX, hoverY;

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
      if (intersectionIn3d) clearUVPointer();
      intersectionIn3d = false;
      
      hoverX = null;
      hoverY = null;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      //System.out.format("GLSample.mousePressed() called\n");
      cameraController.grab(e.getX(), e.getY(),
            e.isShiftDown()   ? (e.isControlDown() ? Camera.Controller.GrabState.FOV : Camera.Controller.GrabState.Zoom)
                              : (e.isControlDown() ? Camera.Controller.GrabState.Pan : Camera.Controller.GrabState.Rotate));
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      hoverX = e.getX();
      hoverY = e.getY();

      //System.out.format("GLSample.mouseDragged(%d,%d) called\n", e.getX(), e.getY());
      cameraController.moveTo(e.getX(), e.getY());
      glCanvas.display();
   }
   
   @Override
   public void mouseReleased(MouseEvent e) {
      //System.out.format("GLSample.mouseReleased() called\n");
      cameraController.release();
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      hoverX = e.getX();
      hoverY = e.getY();

      //System.out.format("GLSample.mouseMoved(%d,%d) called\n", e.getX(), e.getY());
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      cameraController.grab(e.getX(), e.getY(), Camera.Controller.GrabState.Zoom);
      cameraController.moveTo(e.getX(), e.getY() + 20 * ((e.getPreciseWheelRotation() > 0)?1:-1));
      cameraController.release();
      
      glCanvas.display();
   }
   
   // -----------------------------------------------------------
   // GL-RENDERING
   // -----------------------------------------------------------
   
   //    "initGL" (GL3)   -- bind all the RenderingStrategy / Texture / TexturedMeshs 
   //                        instances to GL-vertex-array-objects
   //                        save a map from Textures and TexturedMeshs
   //                        to the GL-vertex-array-object IDs
   
   public void initGL(GL3 gl) {
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
      setupProgram(gl);
      setupTextures(gl);
      setupBuffers(gl);
   }
   
   //    "renderGL" (GL3)  -- set camera-ball perspective matrix
   //                         for each TexturedMeshInstance
   //                              set the appropriate GL-vertex-array-object IDs
   //                              set the appropriate view matrix
   //                              render
   
   public void renderGL(GL3 gl) {
      updateBuffers(gl);

      Camera camera = cameraController.getCamera();
      int width = camera.width;
      int height = camera.height;
      
      demoWorld.bindPositions(camera.cameraToClipSpace, camera.worldToCameraSpace, width, height);
      
      gl.glViewport(0,0,width, height);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      
      // gl.glViewport(0,0,width/2, height/2);
      // gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
      // gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            
      // gl.glViewport(width/2,height/2,width/2, height/2);
      // gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
      // gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      // Transparancy:
      // If these are set, then the shader will honor alpha values set in the "outColor"
      // and blend them with what's already there, but that only works if triangles are
      // drawn from furthest-to-closest!:
      //
      // gl.glEnable(GL.GL_BLEND);
      // gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
      
      
      // RENDERING -------
      
      // Save Camera-To-Clip-Space Matrix
      //sendMatrixToGL(gl, camera.cameraToClipSpace, projMatrixLoc);
      
      renderShaderInstances(gl, demoWorld.getRootModel(), camera.worldToCameraSpace);

      // Check out error
      checkError(gl, "render");
   }
   
   public void renderShaderInstances(GL3 gl, Model m, Matrix4f viewMatrix) {
      viewMatrix = Matrix4f.product(viewMatrix, m.getModelToWorld());
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel)m).children) {
            renderShaderInstances(gl, child, viewMatrix);
         }
      }
      if (m instanceof ShaderInstanceModel) {
         Shader.Instance shaderInstance = ((ShaderInstanceModel) m).instance;
         MeshModel model = ((ShaderInstanceModel) m).model;

         boolean intersected = ((hoverX != null) && (hoverY != null)) 
                                  ? intersects(model, viewMatrix, cameraController.getCamera(), hoverX, hoverY)
                                  : false;
         if (intersected) {
            shaderInstance.bind(Shader.HIGHLIGHT_BOOL, new Shader.Variable.Uniform.IntBinding(1));
         } else {
            shaderInstance.bind(Shader.HIGHLIGHT_BOOL, new Shader.Variable.Uniform.IntBinding(0));
         }
         if (demoWorld.selectedUVPointIfAny != null) {
            shaderInstance.bind(Shader.UV_POINTER, new Shader.Variable.Uniform.Vec2Binding(demoWorld.selectedUVPointIfAny));
         } else {
            shaderInstance.bind(Shader.UV_POINTER, new Shader.Variable.Uniform.Vec2Binding(new Vector2f(-1.0f,-1.0f)));
         }
         
         // Tell GL to use the shader program for this instance...
         gl.glUseProgram(shaderInstance.program.getGLProgramID());  

         for (Shader.Variable variable : shaderInstance.program.variables) {
            Shader.Variable.Binding binding = shaderInstance.boundVariables.get(variable);
            if (binding instanceof Shader.Variable.Uniform.IntBinding) {
               Integer value = ((Shader.Variable.Uniform.IntBinding) binding).value;
               gl.glUniform1i(variable.getGLPProgramLocation(), value);
            }
            if (binding instanceof Shader.Variable.Uniform.Vec3Binding) {
               Vector3f value = ((Shader.Variable.Uniform.Vec3Binding) binding).value;
               gl.glUniform3f(variable.getGLPProgramLocation(), value.x, value.y, value.z);
            }
            if (binding instanceof Shader.Variable.Uniform.Vec2Binding) {
               Vector2f value = ((Shader.Variable.Uniform.Vec2Binding) binding).value;
               gl.glUniform2f(variable.getGLPProgramLocation(), value.x, value.y);
            }
            if (binding instanceof Shader.Variable.Uniform.Mat4Binding) {
               Matrix4f value = ((Shader.Variable.Uniform.Mat4Binding) binding).value;
               float arr[] = new float[16];
               value.copyToFloatArray(arr);
               gl.glUniformMatrix4fv(variable.getGLPProgramLocation(), 1, false, arr, 0);     
            }
            if (binding instanceof Shader.Variable.Uniform.TextureBinding) {
               Shader.ManagedTexture texture = ((Shader.Variable.Uniform.TextureBinding) binding).texture;
               gl.glBindTexture(GL.GL_TEXTURE_2D, texture.glTextureID); 
            }
         }
         
         // Binding the "vertex array object" ID takes care of all the per-vertex buffer bindings
         gl.glBindVertexArray(shaderInstance.getGLVertexArraySetupID());

         // Draw the bound arrays...
         gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3 * model.getNumTriangles());
      }
   }
   
   public void sendMatrixToGL(GL3 gl, Matrix4f m, int glLoc) {
      float arr[] = new float[16];
      m.copyToFloatArray(arr);
      gl.glUniformMatrix4fv(glLoc, 1, false, arr, 0);     
   }

   // --------------------------------------------------------------
   // PICKING VERSION 0 -- let's just answer the question here...

   private boolean intersects(MeshModel geometry,  // does this geometry...
                               Matrix4f modelToCamera,   // transformed by this matrix into camera space...
                               Camera camera,            // where this is the camera,
                               int x, int y) {           // intersect this point in the view?

      int width = camera.width;
      int height = camera.height;

      float aspect = ((float)width) / height;
      float fHeight = (float) Math.tan(camera.verticalFovInDegrees * (Math.PI / 180.0) * 0.5);
      float fWidth  = aspect * fHeight;

      // In camera space...
      //   camera is at (0,0,0)
      //   looking at (0,0,1),
      // 
      // in the z=1 plane,
      //   the window top-left  is (-fWidth,fHeight,1)
      //   the window top-right is (+fWidth,fHeight,1)
     
      float pixelWidth  = (float) ((2*fWidth) / width);
      float pixelHeight = (float) ((2*fHeight) / height);

      float xPos = -fWidth + pixelWidth*x    + pixelWidth  * 0.5f;
      float yPos = fHeight - pixelHeight*y   + pixelHeight * 0.5f;

      Vector3f camPos = new Vector3f(xPos,yPos,-1.0f);
      Segment3d s = new Segment3d(Vector3f.ORIGIN, camPos);
      // --------------------
  
      Vector2f intersection = null;
      boolean intersectionOccurred = false;
      for (Geometry.Mesh.Triangle t : geometry.mesh.interiorTriangles) {
         Vector3f v0Pos = t.edge0.getOppositeVertex().getPosition();
         Vector3f v1Pos = t.edge1.getOppositeVertex().getPosition();
         Vector3f v2Pos = t.edge2.getOppositeVertex().getPosition();

         Vector3f camV0 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v0Pos)).toVector3f();
         Vector3f camV1 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v1Pos)).toVector3f();
         Vector3f camV2 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v2Pos)).toVector3f();

         //System.out.format("Triangle is\n%s---\n%s---\n%s---\n", camV0.toString(), camV1.toString(), camV2.toString());

         Triangle t2 = new Triangle(camV0, camV1, camV2);
         Vector2f intersectionA = VectorAlgebra.intersects(t2,s);
         
         if (intersectionA != null) {
            intersectionOccurred = true;
            if (geometry == demoWorld.mappingModel) {
               Geometry.FlatFaceInfo fi = (Geometry.FlatFaceInfo) t.getData();
               Vector2f t0 = fi.tex0;
               Vector2f tu = fi.tex1.minus(fi.tex0);
               Vector2f tv = fi.tex2.minus(fi.tex0);
               
               intersection = t0.plus(tu.times(intersectionA.x)).plus(tv.times(intersectionA.y));               
            }
         }
      }
      
      if (geometry == demoWorld.mappingModel) {
         if (intersection != null) {
            updateUVPointer(intersection.x, intersection.y);
            intersectionIn3d = true;
         } else {
            if (intersectionIn3d) clearUVPointer();
            intersectionIn3d = false;
         }
      }
      return intersectionOccurred;
   }

   // -------------------------------------------------------------------
   // SENDING SHADER PROGRAM to GL
   // -------------------------------------------------------------------

   enum ShaderType{ VertexShader, FragmentShader }
   
   private void setupProgram(GL3 gl) {
      System.out.format("SETUP-program called\n");
      
      // -----------------------------------------------------
      // we're going to explicitly setup TWO shaders
      // -----------------------------------------------------
      
      for (Shader.Program shaderProgram : Arrays.asList(Shader.TEXTURE_SHADER, 
                                                        Shader.FACE_COLOR_SHADER,
                                                        Shader.POINT_COLOR_SHADER)) {
         
         int v = this.newShaderFromCurrentClass(gl, shaderProgram.vertexShaderName,   ShaderType.VertexShader);
         int f = this.newShaderFromCurrentClass(gl, shaderProgram.fragmentShaderName, ShaderType.FragmentShader);
         System.out.format("Vertex Shader Info Log: [%s]\n", getShaderInfoLog(gl, v));
         System.out.format("Fragent Shader Info Log: [%s]\n", getShaderInfoLog(gl, f));

         // Complete the "shader program"
         int programID = gl.glCreateProgram();
         shaderProgram.setGLProgramID(programID);
         gl.glAttachShader(programID, v);
         gl.glAttachShader(programID, f);
         gl.glLinkProgram(programID);
         
         gl.glUseProgram(programID);      
         gl.glBindFragDataLocation(programID, 0, "outColor");
         System.out.format("Program Info Log: [%s]\n", getProgramInfoLog(gl, programID));
         
         // Extract variable "locations":
         for (Shader.Variable variable : shaderProgram.variables) {
            if (variable instanceof Shader.Variable.Buffer) {
               variable.setGLProgramLocation(gl.glGetAttribLocation(programID, variable.name));
            }
            if (variable instanceof Shader.Variable.Uniform) {
               Shader.Variable.Uniform uniformVariable = (Shader.Variable.Uniform) variable;
               variable.setGLProgramLocation(gl.glGetUniformLocation(programID, variable.name));
               if ((uniformVariable.type == Shader.Variable.Uniform.Type.BGRA_TEXTURE) ||
                   (uniformVariable.type == Shader.Variable.Uniform.Type.GRAY_TEXTURE)) {
                  
                  gl.glUniform1i(variable.getGLPProgramLocation(), 0);
               }
            }
            System.out.format("Program Location for %s is %d\n",  variable.name, variable.getGLPProgramLocation());
         }
      }
   }

   private int newShaderFromCurrentClass(GL3 gl, String fileName, ShaderType type){
      // load the source
      String shaderSource = this.loadStringFileFromCurrentPackage( fileName);
      // define the shaper type from the enum
      int shaderType = (type == ShaderType.VertexShader) ? GL3.GL_VERTEX_SHADER : GL3.GL_FRAGMENT_SHADER;
      // create the shader id
      int id = gl.glCreateShader(shaderType);
      //  link the id and the source
      gl.glShaderSource(id, 1, new String[] { shaderSource }, null);
      //compile the shader
      gl.glCompileShader(id);

      return id;
   }
   
   /** Retrieves the info log for the shader */
   private String getShaderInfoLog(GL3 gl, int obj) {
      // Otherwise, we'll get the GL info log
      final int logLen = getShaderParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
      if (logLen <= 0)
         return "";

      // Get the log
      final int[] retLength = new int[1];
      final byte[] bytes = new byte[logLen + 1];
      gl.glGetShaderInfoLog(obj, logLen, retLength, 0, bytes, 0);
      final String logMessage = new String(bytes);

      return String.format("ShaderLog: %s", logMessage);
   }

   /** Get a shader parameter value. See 'glGetShaderiv' */
   private int getShaderParameter(GL3 gl, int obj, int paramName) {
      final int params[] = new int[1];
      gl.glGetShaderiv(obj, paramName, params, 0);
      return params[0];
   }
   
   
   /** Retrieves the info log for the program */
   private String getProgramInfoLog(GL3 gl, int obj) {
      // get the GL info log
      final int logLen = getProgramParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
      if (logLen <= 0)
         return "";

      // Get the log
      final int[] retLength = new int[1];
      final byte[] bytes = new byte[logLen + 1];
      gl.glGetProgramInfoLog(obj, logLen, retLength, 0, bytes, 0);
      final String logMessage = new String(bytes);

      return logMessage;
   }

   /** Gets a program parameter value */
   private int getProgramParameter(GL3 gl, int obj, int paramName) {
      final int params[] = new int[1];
      gl.glGetProgramiv(obj, paramName, params, 0);
      return params[0];
   }


   // -------------------------------------------------------------------
   // SENDING TEXTURE to GL
   // -------------------------------------------------------------------

   private void setupTextures(GL3 gl) {
      gl.glActiveTexture(GL.GL_TEXTURE0);
      
      HashSet<Shader.Instance> shaderInstances = demoWorld.getShaderInstances();
      HashSet<Shader.ManagedTexture> textures = World.getAllTextures(shaderInstances);
      for (Shader.ManagedTexture texture : textures) {
         texture.setup();
         texture.glTextureID = this.generateTextureId(gl);
         
         gl.glBindTexture(GL.GL_TEXTURE_2D, texture.glTextureID);         
         gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
         gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
         
         gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8,
               texture.image.width, texture.image.height, 0, GL.GL_BGRA,
               GL.GL_UNSIGNED_BYTE, texture.intBuffer);
      }
   }
   
   private int generateTextureId(GL3 gl) {
      // allocate an array of one element in order to store the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenTextures( 1, idArray, 0);

      // return the id
      return idArray[0];
   }

   
   // -------------------------------------------------------------------
   // SENDING GEOMETRY to GL
   // -------------------------------------------------------------------
   
   private void setupBuffers(GL3 gl) {
      HashSet<Shader.Instance> shaderInstances = demoWorld.getShaderInstances();
      HashSet<Shader.ManagedBuffer> buffers = World.getAllBuffers(shaderInstances);
      for (Shader.ManagedBuffer buffer : buffers) {
         buffer.setup();
         buffer.glBufferID = generateBufferId(gl);

         gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer.glBufferID);
         gl.glBufferData(GL.GL_ARRAY_BUFFER,
               buffer.array.length * Float.SIZE / 8,
               buffer.floatBuffer, GL.GL_STATIC_DRAW);
         buffer.glBufferSize = buffer.array.length;
         System.out.format("Created GL buffer %d for %s\n", buffer.glBufferID, buffer.name);
      }

      // Second, for each shader-instance, create a "vertex array object" to hold the vertex array bindings
      for (Shader.Instance shaderInstance : shaderInstances) {
         gl.glUseProgram(shaderInstance.program.getGLProgramID()); 
         
         shaderInstance.setGLVertexArraySetupID(generateVAOId(gl));
         gl.glBindVertexArray(shaderInstance.getGLVertexArraySetupID());
         
         // Walk through all the buffers in this instance:
         for (Shader.Variable variable : shaderInstance.program.variables) {
            if (variable instanceof Shader.Variable.Buffer) {
               Shader.Variable.Binding binding = shaderInstance.boundVariables.get(variable);
               Shader.ManagedBuffer buffer = ((Shader.Variable.Buffer.Binding) binding).buffer;
               int programLocation = variable.getGLPProgramLocation();
               if (programLocation >= 0) {
                  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer.glBufferID);               
                  gl.glEnableVertexAttribArray(variable.getGLPProgramLocation());
                  gl.glVertexAttribPointer(variable.getGLPProgramLocation(), buffer.numFloatsPerElement, GL.GL_FLOAT, false, 0, 0);
               }
            }            
         }
      }
   }
   private void updateBuffers(GL3 gl) {
      HashSet<Shader.Instance> shaderInstances = demoWorld.getShaderInstances();
      HashSet<Shader.ManagedBuffer> buffers = World.getAllBuffers(shaderInstances);
      
      for (Shader.ManagedBuffer buffer : buffers) {
         if (buffer.isModified()) {
            buffer.setup();
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer.glBufferID);
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, 
                  buffer.array.length * Float.SIZE / 8,
                  buffer.floatBuffer);
         }
      }
   }
   
   private int generateVAOId(GL3 gl) {
      // allocate an array of one element in order to store the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenVertexArrays(1, idArray, 0);
      // return the id
      return idArray[0];
   }
   private int generateBufferId(GL3 gl) {
      // allocate an array of one element in order to store the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenBuffers( 1, idArray, 0);

      // return the id
      return idArray[0];
   }

   // -------------------------------------------------------------------
   // GL Error
   // -------------------------------------------------------------------

   private void checkError(GL3 gl, String point) {
      int error = gl.glGetError();
      if (error!=0){
         System.out.format("At [%s] ERROR on render: %d\n", point, error);}
   }
}

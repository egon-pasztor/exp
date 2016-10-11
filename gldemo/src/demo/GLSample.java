package demo;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;

import demo.GLMath.*;

public class GLSample implements GLEventListener, MouseListener, MouseMotionListener {

   private GLCanvas glCanvas;
  
//   boolean cube = false;
//   boolean ico = false;
//   boolean ball = true;
//   int subdivide = 0;

   boolean cube = false;
   boolean ico = true;
   boolean ball = false;
   int subdivide = 3;
   
   // -----------------------------------------------------------
   // -----------------------------------------------------------

   public static void main(String[] args) {
      System.out.format("Hello world..\n");

      GLSample sample = new GLSample();
      
      GLProfile glp = GLProfile.get(GLProfile.GL3);
      GLCapabilities glCapabilities = new GLCapabilities(glp);
      sample.glCanvas = new GLCanvas(glCapabilities);
      sample.glCanvas.addGLEventListener(sample);
      sample.glCanvas.addMouseListener(sample);
      sample.glCanvas.addMouseMotionListener(sample);
      
      JFrame frame = new JFrame("GL Sample");
      frame.setBounds(10, 10, 300, 200);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(sample.glCanvas);
      frame.setVisible(true);
   }

   public GLSample() {
      initModel();
   }

   // -----------------------------------------------------------
   // These 4 methods appear to be GLEventListener methods
   // -----------------------------------------------------------

   /** INIT */
   @Override
   public void init(GLAutoDrawable drawable) {
      System.out.format("GLSample.init called\n");
      
      GL3 gl = drawable.getGL().getGL3();
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

      this.setupProgram(gl);
      this.setupBuffers(gl);
      this.setupTexture(gl);
   }

   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.format("GLSample.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
      
      cameraBall = new CameraBall(width, height,
                                  new Vector3f(0.0f, 0.0f, 0.0f),   // look-at
                                  new Vector3f(0.0f, 0.0f, cube?5.0f:3f),   // camera-pos
                                  new Vector3f(0.0f, 1.0f, 0.0f),   // camera-up
                                  53.13f);
      updateMatrices();
   }

   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      System.out.format("GLSample.display() called\n");
      
      GL3 gl = drawable.getGL().getGL3();
      renderScene(gl);
   }

   /** GL Complete */
   @Override
   public void dispose(GLAutoDrawable drawable) {
      System.out.format("GLSample.dispose() called\n");
   }

   // -----------------------------------------------------------
   // MOUSE Events
   // -----------------------------------------------------------

   @Override
   public void mouseClicked(MouseEvent e) {
      System.out.format("GLSample.mouseClicked() called\n");
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      System.out.format("GLSample.mouseEntered() called\n");
   }

   @Override
   public void mouseExited(MouseEvent e) {
      System.out.format("GLSample.mouseExited() called\n");
   }

   @Override
   public void mousePressed(MouseEvent e) {
      System.out.format("GLSample.mousePressed() called\n");
      cameraBall.grab(e.getX(), e.getY(), CameraBall.GrabType.Rotate);
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      System.out.format("GLSample.mouseReleased() called\n");
      cameraBall.release();
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      System.out.format("GLSample.mouseMoved(%d,%d) called\n", e.getX(), e.getY());
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      System.out.format("GLSample.mouseDragged(%d,%d) called\n", e.getX(), e.getY());
      cameraBall.moveTo(e.getX(), e.getY());
      updateMatrices();
      glCanvas.display();
   }
   
   // -----------------------------------------------------------
   // SETUP program
   // -----------------------------------------------------------

   
   // Program ID
   int programID;

   // Vertex Attribute Locations
   int vsPositionLoc;
   int vsColorLoc;
   int vsTexCoordsLoc;
   int vsBaryCoordsLoc;

   // Uniform variable Locations
   int projMatrixLoc;
   int viewMatrixLoc;
   
   // Texture Locations
   int textureLoc;
   
   enum ShaderType{ VertexShader, FragmentShader }
   
   void setupProgram(GL3 gl) {
      System.out.format("SETUP-program called\n");
      
      // create the two shader and compile them
      int v = this.newShaderFromCurrentClass(gl, "vertex2.shader", ShaderType.VertexShader);
      int f = this.newShaderFromCurrentClass(gl, "fragment2.shader", ShaderType.FragmentShader);

      System.out.format("Vertex Shader Info Log: [%s] WHAT??\n", getShaderInfoLog(gl, v));
      System.out.format("Fragent Shader Info Log: [%s]\n", getShaderInfoLog(gl, f));

      // Complete the "shader program"
      this.programID = gl.glCreateProgram();
      gl.glAttachShader(programID, v);
      gl.glAttachShader(programID, f);
      gl.glLinkProgram(programID);
      
      gl.glUseProgram(this.programID);
      
      gl.glBindFragDataLocation(programID, 0, "outColor");
      System.out.format("Program Info Log: [%s]\n", getProgramInfoLog(gl, programID));
      checkError(gl, "gotProgram");      

      // Extract variable "locations":
      
      this.vsPositionLoc = gl.glGetAttribLocation(programID, "inVertexPosition");
      this.vsColorLoc = gl.glGetAttribLocation(programID, "inVertexColor");
      this.vsBaryCoordsLoc = gl.glGetAttribLocation(programID, "inVertexBaryCoords");
      this.vsTexCoordsLoc = gl.glGetAttribLocation(programID, "inVertexTexCoords");
      System.out.format("Locations: [%d,%d,%d]\n", 
            vsPositionLoc, vsColorLoc, vsTexCoordsLoc);
      checkError(gl, "extractLocs");      
            
      this.projMatrixLoc = gl.glGetUniformLocation(programID, "projMatrix");
      this.viewMatrixLoc = gl.glGetUniformLocation(programID, "viewMatrix");
      
      this.textureLoc = gl.glGetUniformLocation(programID, "myTexture");
      gl.glUniform1i(textureLoc, 0);
   }

   int newShaderFromCurrentClass(GL3 gl, String fileName, ShaderType type){
      // load the source
      String shaderSource = this.loadStringFileFromCurrentPackage( fileName);
      // define the shaper type from the enum
      int shaderType = type==ShaderType.VertexShader?GL3.GL_VERTEX_SHADER:GL3.GL_FRAGMENT_SHADER;
      // create the shader id
      int id = gl.glCreateShader(shaderType);
      //  link the id and the source
      gl.glShaderSource(id, 1, new String[] { shaderSource }, null);
      //compile the shader
      gl.glCompileShader(id);

      return id;
   }
   
   protected String loadStringFileFromCurrentPackage(String fileName){
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
   
   // Getting "LOGs"??

   /** Retrieves the info log for the shader */
   public String getShaderInfoLog(GL3 gl, int obj) {
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
   public String getProgramInfoLog(GL3 gl, int obj) {
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
   public int getProgramParameter(GL3 gl, int obj, int paramName) {
      final int params[] = new int[1];
      gl.glGetProgramiv(obj, paramName, params, 0);
      return params[0];
   }

   
   // -----------------------------------------------------------
   // SETUP buffers
   // -----------------------------------------------------------

   private Model m;

   private Vector3f pos(float lat, float lon) {
      float cosLat = (float) Math.cos(lat);
      float sinLat = (float) Math.sin(lat);
      float cosLon = (float) Math.cos(lon);
      float sinLon = (float) Math.sin(lon);
      return new Vector3f(cosLat * cosLon, cosLat * sinLon, sinLat);
   }
   
   private Vector2f x(Vector2f v) {
      float lat = v.x;
      float lon = v.y;
      return new Vector2f( (float) (lon / (2.0 * Math.PI)),
                           1.0f - (float) (((Math.PI/2.0) + lat) / Math.PI) );
   }
   
   
   private void addTriangleAndSubdivide(int level, 
         Vector3f a, Vector3f b, Vector3f c,
         Vector2f aT, float aW,
         Vector2f bT, float bW,
         Vector2f cT, float cW) {
      
      if (level == 0) {
         m.addTriangle(a, b, c, aT,aW, bT,bW, cT,cW);
      } else {
         Vector3f ab = a.plus(b).times(0.5f);
         Vector3f bc = b.plus(c).times(0.5f);
         Vector3f ac = a.plus(c).times(0.5f);
         Vector2f abT = aT.plus(bT).times(0.5f);
         Vector2f bcT = bT.plus(cT).times(0.5f);
         Vector2f acT = aT.plus(cT).times(0.5f);
         float abW = (aW+bW)/2.0f;
         float bcW = (bW+cW)/2.0f;
         float acW = (aW+cW)/2.0f;
         
         addTriangleAndSubdivide(level-1, a,   ab,  ac,   aT,aW,   abT,abW, acT,acW);
         addTriangleAndSubdivide(level-1, ab,  b,   bc,   abT,abW, bT,bW,   bcT,bcW);
         addTriangleAndSubdivide(level-1, bc,  c,   ac,   bcT,bcW, cT,cW,   acT,acW);
         addTriangleAndSubdivide(level-1, ab,  bc,  ac,   abT,abW, bcT,bcW, acT,acW);
      }
   }
      
   private void addTriangle(Vector3f a, Vector3f b, Vector3f c,
         Vector2f aT, float aW,
         Vector2f bT, float bW,
         Vector2f cT, float cW) {
      
      addTriangleAndSubdivide(subdivide, a,b,c, aT,aW, bT,bW, cT,cW);
   }
   
   private void initModel() {
       m = new Model();
       
       if (cube) {
          Vector3f center = Vector3f.Z;
          Vector3f dx = Vector3f.X;
          Vector3f dy = Vector3f.Y;
   
          float ninety = (float) (Math.PI/2);
          for (int i = 0; i < 4; ++i) {
     	       float angle = i * ninety;
    	        m.addSquare(center.rotated(Vector3f.Y,angle),
                          dx.rotated(Vector3f.Y,angle),
                          dy.rotated(Vector3f.Y,angle));
          }
         
          m.addSquare(center.rotated(Vector3f.X,ninety),
                      dx.rotated(Vector3f.X,ninety),
   		             dy.rotated(Vector3f.X,ninety));
         
          m.addSquare(center.rotated(Vector3f.X,-ninety),
                      dx.rotated(Vector3f.X,-ninety),
                      dy.rotated(Vector3f.X,-ninety));
          
       }
       if (ball) {
          
          int numLat = 20;
          int numLon = 20;
          double globalLatMin = -Math.PI/2;
          double globalLatMax =  Math.PI/2;
          double globalLonMin = 0;
          double globalLonMax = 2*Math.PI;
          
          for (int lat = 0; lat < numLat; lat++) {
             for (int lon = 0; lon < numLon; lon++) {
                
                float latMin = (float) (globalLatMin + ((lat * (globalLatMax - globalLatMin)) / numLat));
                float latMax = (float) (globalLatMin + (((lat+1) * (globalLatMax - globalLatMin)) / numLat));
                float lonMin = (float) (globalLonMin + ((lon * (globalLonMax - globalLonMin)) / numLon));
                float lonMax = (float) (globalLonMin + (((lon+1) * (globalLonMax - globalLonMin)) / numLon));
                
                float wMax = (float) Math.cos(latMax) + .000001f;
                float wMin = (float) Math.cos(latMin) + .000001f;

                Vector3f tR = pos(latMax, lonMax);
                Vector3f tL = pos(latMax, lonMin);
                Vector3f bR = pos(latMin, lonMax);
                Vector3f bL = pos(latMin, lonMin);
                
                if (lat > 0) {
                   addTriangle(tL,bL,bR,
                         x(new Vector2f(latMax, lonMin)), wMax,
                         x(new Vector2f(latMin, lonMin)), wMin,
                         x(new Vector2f(latMin, lonMax)), wMin);
                }
                if (lat < numLat-1) {
                   addTriangle(tL,bR,tR,
                         x(new Vector2f(latMax, lonMin)), wMax,
                         x(new Vector2f(latMin, lonMax)), wMin,
                         x(new Vector2f(latMax, lonMax)), wMax);
                }
                
             }
          }
          
          
          
       }
       if (ico) {
          // Icosahedron

          Vector3f top = new Vector3f(0f,1f,0f);
   
          Vector3f t0,t1,t2,t3,t4;
          Vector3f b0,b1,b2,b3,b4;
          
          float lonDelta = (float) (Math.PI / 5.0) - .000000001f;
          
          float nLat = (float) Math.PI/2 - .00000001f;
          float tLat = (float) Math.atan(0.5);
          float bLat = -tLat;
          float sLat = -nLat;
          
          float nLatW = (float) Math.cos(nLat) + .000001f;
          float tLatW = (float) Math.cos(tLat) + .000001f;
          float bLatW = (float) Math.cos(bLat) + .000001f;
          float sLatW = (float) Math.cos(sLat) + .000001f;

          
          t0 = new Vector3f((float)(2.0/Math.sqrt(5)), (float)(1.0/Math.sqrt(5)), 0f);
          b0 = t0.rotated(Vector3f.Y, (float)(1 * Math.PI/5.0));
          t1 = t0.rotated(Vector3f.Y, (float)(2 * Math.PI/5.0));
          b1 = t0.rotated(Vector3f.Y, (float)(3 * Math.PI/5.0));
          t2 = t0.rotated(Vector3f.Y, (float)(4 * Math.PI/5.0));
          b2 = t0.rotated(Vector3f.Y, (float)(5 * Math.PI/5.0));
          t3 = t0.rotated(Vector3f.Y, (float)(6 * Math.PI/5.0));
          b3 = t0.rotated(Vector3f.Y, (float)(7 * Math.PI/5.0));
          t4 = t0.rotated(Vector3f.Y, (float)(8 * Math.PI/5.0));
          b4 = t0.rotated(Vector3f.Y, (float)(9 * Math.PI/5.0));
          
          b0 = new Vector3f(b0.x,-b0.y,b0.z);
          b1 = new Vector3f(b1.x,-b1.y,b1.z);
          b2 = new Vector3f(b2.x,-b2.y,b2.z);
          b3 = new Vector3f(b3.x,-b3.y,b3.z);
          b4 = new Vector3f(b4.x,-b4.y,b4.z);
          
          Vector3f bottom = new Vector3f(0f,-1f,0f);
          
          // TOP FIVE "CAP" TRIANGLES:
          addTriangle(top, t0,t1,
                x(new Vector2f(nLat, 1 * lonDelta)), nLatW,
                x(new Vector2f(tLat, 0 * lonDelta)), tLatW,
                x(new Vector2f(tLat, 2 * lonDelta)), tLatW);
          addTriangle(top, t1,t2,
                x(new Vector2f(nLat, 3 * lonDelta)), nLatW,
                x(new Vector2f(tLat, 2 * lonDelta)), tLatW,
                x(new Vector2f(tLat, 4 * lonDelta)), tLatW);
          addTriangle(top, t2,t3,
                x(new Vector2f(nLat, 5 * lonDelta)), nLatW,
                x(new Vector2f(tLat, 4 * lonDelta)), tLatW,
                x(new Vector2f(tLat, 6 * lonDelta)), tLatW);
          addTriangle(top, t3,t4,
                x(new Vector2f(nLat, 7 * lonDelta)), nLatW,
                x(new Vector2f(tLat, 6 * lonDelta)), tLatW,
                x(new Vector2f(tLat, 8 * lonDelta)), tLatW);

          addTriangle(top, t4,t0,
                x(new Vector2f(nLat, 9  * lonDelta)), nLatW,
                x(new Vector2f(tLat, 8  * lonDelta)), tLatW,
                x(new Vector2f(tLat, 10  * lonDelta)), tLatW);

          // MIDDLE "INTERIOR" TRIANGLES:
          addTriangle(t0, b0, t1,
                x(new Vector2f(tLat,  0 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  1 * lonDelta)), bLatW,
                x(new Vector2f(tLat,  2 * lonDelta)), tLatW);
          addTriangle(t1, b0, b1,
                x(new Vector2f(tLat,  2 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  1 * lonDelta)), bLatW,
                x(new Vector2f(bLat,  3 * lonDelta)), bLatW);
          
          addTriangle(t1, b1, t2,
                x(new Vector2f(tLat,  2 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  3 * lonDelta)), bLatW,
                x(new Vector2f(tLat,  4 * lonDelta)), tLatW);
          addTriangle(t2, b1, b2,
                x(new Vector2f(tLat,  4 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  3 * lonDelta)), bLatW,
                x(new Vector2f(bLat,  5 * lonDelta)), bLatW);
          
          addTriangle(t2, b2, t3,
                x(new Vector2f(tLat,  4 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  5 * lonDelta)), bLatW,
                x(new Vector2f(tLat,  6 * lonDelta)), tLatW);
          addTriangle(t3, b2, b3,
                x(new Vector2f(tLat,  6 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  5 * lonDelta)), bLatW,
                x(new Vector2f(bLat,  7 * lonDelta)), bLatW);
          
          addTriangle(t3, b3, t4,
                x(new Vector2f(tLat,  6 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  7 * lonDelta)), bLatW,
                x(new Vector2f(tLat,  8 * lonDelta)), tLatW);
          addTriangle(t4, b3, b4,
                x(new Vector2f(tLat,  8 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  7 * lonDelta)), bLatW,
                x(new Vector2f(bLat,  9 * lonDelta)), bLatW);
          
          addTriangle(t4, b4, t0,
                x(new Vector2f(tLat,  8 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  9 * lonDelta)), bLatW,
                x(new Vector2f(tLat,  10 * lonDelta)), tLatW);
          addTriangle(t0, b4, b0,
                x(new Vector2f(tLat,  10 * lonDelta)), tLatW,
                x(new Vector2f(bLat,  9 * lonDelta)), bLatW,
                x(new Vector2f(bLat,  11 * lonDelta)), bLatW);    
          
          // BOTTOM FIVE "CAP" TRIANGLES:
          addTriangle(b0, bottom, b1,
                x(new Vector2f(bLat,  1 * lonDelta)), bLatW,
                x(new Vector2f(sLat,  2 * lonDelta)), sLatW,
                x(new Vector2f(bLat,  3 * lonDelta)), bLatW);
          addTriangle(b1, bottom, b2,
                x(new Vector2f(bLat,  3 * lonDelta)), bLatW,
                x(new Vector2f(sLat,  4 * lonDelta)), sLatW,
                x(new Vector2f(bLat,  5 * lonDelta)), bLatW);
          addTriangle(b2, bottom, b3,
                x(new Vector2f(bLat,  5 * lonDelta)), bLatW,
                x(new Vector2f(sLat,  6 * lonDelta)), sLatW,
                x(new Vector2f(bLat,  7 * lonDelta)), bLatW);
          addTriangle(b3, bottom, b4,
                x(new Vector2f(bLat,  7 * lonDelta)), bLatW,
                x(new Vector2f(sLat,  8 * lonDelta)), sLatW,
                x(new Vector2f(bLat,  9 * lonDelta)), bLatW);
          addTriangle(b4, bottom, b0,
                x(new Vector2f(bLat,   9  * lonDelta)), bLatW,
                x(new Vector2f(sLat,  10 * lonDelta)), sLatW,
                x(new Vector2f(bLat,  11  * lonDelta)), bLatW);
          
          for (Mesh.Vertex<Vector3f,TexInfo> v : m.mesh.vertices) {
             v.setData(v.getData().normalized());
          }
          for (Mesh.Triangle<Vector3f,TexInfo> t : m.mesh.interiorTriangles) {
             Vector3f v0Pos = t.edge0.getOppositeVertex().getData();
             Vector3f v1Pos = t.edge1.getOppositeVertex().getData();
             Vector3f v2Pos = t.edge2.getOppositeVertex().getData();
             TexInfo ti = t.getData();
             
             float v0Lat = (float) Math.asin(v0Pos.y);
             float v0Lon = (float) (Math.PI + Math.atan2(v0Pos.x, v0Pos.z));
             float v1Lat = (float) Math.asin(v1Pos.y);
             float v1Lon = (float) (Math.PI + Math.atan2(v1Pos.x, v1Pos.z));
             float v2Lat = (float) Math.asin(v2Pos.y);
             float v2Lon = (float) (Math.PI + Math.atan2(v2Pos.x, v2Pos.z));
             
             if ((v0Lon>=v1Lon) && (v0Lon>=v2Lon)) {
                if (v0Lon-v1Lon>Math.PI) v1Lon+=(float)(Math.PI*2);
                if (v0Lon-v2Lon>Math.PI) v2Lon+=(float)(Math.PI*2);
             } else
                if ((v1Lon>=v0Lon) && (v1Lon>=v2Lon)) {
                   if (v1Lon-v0Lon>Math.PI) v0Lon+=(float)(Math.PI*2);
                   if (v1Lon-v2Lon>Math.PI) v2Lon+=(float)(Math.PI*2);
                } else
                   if ((v2Lon>=v0Lon) && (v2Lon>=v1Lon)) {
                      if (v2Lon-v0Lon>Math.PI) v0Lon+=(float)(Math.PI*2);
                      if (v2Lon-v1Lon>Math.PI) v1Lon+=(float)(Math.PI*2);
                   }
                
             if ((v0Pos.y==1.0)||(v0Pos.y==-1.0)) { v0Lon = (v1Lon+v2Lon)/2.0f; }
             if ((v1Pos.y==1.0)||(v1Pos.y==-1.0)) { v1Lon = (v0Lon+v2Lon)/2.0f; }
             if ((v2Pos.y==1.0)||(v2Pos.y==-1.0)) { v2Lon = (v0Lon+v1Lon)/2.0f; }
             
             ti.t1 = x(new Vector2f(v0Lat,v0Lon));
             ti.t1w = (float) Math.cos(v0Lat) + .000001f;
             
             ti.t2 = x(new Vector2f(v1Lat,v1Lon));
             ti.t2w = (float) Math.cos(v1Lat) + .000001f;

             ti.t3 = x(new Vector2f(v2Lat,v2Lon));
             ti.t3w = (float) Math.cos(v2Lat) + .000001f;
          }
       }
       
   }

   // Vertex Array Object ID
   int triangleVAO;
   
   void setupBuffers(GL3 gl) {
      System.out.format("SETUP-buffers called\n");
      
      // generate the IDs
      this.triangleVAO = this.generateVAOId(gl);
      gl.glBindVertexArray(triangleVAO);
      
      // Generate slots
      checkError(gl, "bufferIdCreation");
      int positionBufferId = this.generateBufferId(gl);
      int colorBufferId = this.generateBufferId(gl);
      int texCoordsBufferId = this.generateBufferId(gl);
      int baryCoordsBufferId = this.generateBufferId(gl);
   
      // bind the buffers

      Model.Arrays ma = m.getArrays();
      this.bindBuffer(gl, positionBufferId,   ma.positions,  4, vsPositionLoc);
      this.bindBuffer(gl, colorBufferId,      ma.colors,     4, vsColorLoc);
      this.bindBuffer(gl, texCoordsBufferId,  ma.texCoords,  4, vsTexCoordsLoc);
      this.bindBuffer(gl, baryCoordsBufferId, ma.baryCoords, 2, vsBaryCoordsLoc);
   }
   
   void bindBuffer(GL3 gl, int bufferId, float[] dataArray, int componentsPerAttribute, int dataLoc){
      // bind buffer for vertices and copy data into buffer
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      gl.glBufferData(GL.GL_ARRAY_BUFFER, dataArray.length * Float.SIZE / 8,
            Buffers.newDirectFloatBuffer(dataArray), GL.GL_STATIC_DRAW);
      gl.glEnableVertexAttribArray(dataLoc);
      gl.glVertexAttribPointer(dataLoc, componentsPerAttribute, GL.GL_FLOAT, false, 0, 0);
   }

   protected int generateVAOId(GL3 gl) {
      // allocate an array of one element in order to strore 
      // the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenVertexArrays(1, idArray, 0);
      // return the id
      return idArray[0];
   }

   protected int generateBufferId(GL3 gl) {
      // allocate an array of one element in order to strore 
      // the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenBuffers( 1, idArray, 0);

      // return the id
      return idArray[0];
   }


   // -----------------------------------------------------------
   // SETUP texture
   // -----------------------------------------------------------

   public static class Image {
       public Image (int width, int height) {
           this.width = width;
           this.height = height;
           pixels = new int[width*height];
       }

       public void clear() {
           int n=width*height;
           for (int i=0; i < n; ++i) {
               pixels[i] = 0;
           }
       }
       public void set(int x, int y, int color) {
          pixels[x+y*width] = color;
       }
       public void fillRect(int startX, int startY, int w, int h, int color) {
          for (int x = 0; x < w; x++) {
              for (int y = 0; y < h; y++) {
                  set(startX+x, startY+y, color);
              }
          }
       }
       public void setFromResource(String name) {
          System.out.format("Trying to load image named [%s]\n", name);
          BufferedImage im;
          try {
              im = ImageIO.read(this.getClass().getResource(name));
          } catch (IOException e) {
              System.out.format("FAILED - Trying to load image named [%s]\n", name);
              e.printStackTrace();
              return;
          }
          
          for (int row = 0; row < height; row++) {
             for (int col = 0; col < width; col++) {
                int val = im.getRGB(col, row);
                pixels[col+row*width] = val;
                if ((row == 0) && (col == 0)) {
                   System.out.format("At (0,0) we got 0x%8x\n", val);
                }
             }
          }
       }
       
       public final int[] pixels;
       public final int width;
       public final int height;
   }

   void setupTexture(GL3 gl) {
      System.out.format("SETUP-texture called\n");
      
      Image myTexture = new Image(256,256);
      myTexture.setFromResource("teapot.png");
      //myTexture.fillRect(0, 0, 256, 256, 0x00ffffff);
//      myTexture.fillRect(10, 10, 30, 30, 0x00ff0000);
//      myTexture.fillRect(30, 30, 30, 30, 0x0000ff00);
//      myTexture.fillRect(50, 50, 30, 30, 0x000000ff);
      
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect(myTexture.pixels.length * 4);
      byteBuffer.order(ByteOrder.nativeOrder());
      IntBuffer colormapBuffer = byteBuffer.asIntBuffer();
      colormapBuffer.put(myTexture.pixels);
      colormapBuffer.position(0);
      
      int textureId = this.generateTextureId(gl);
      gl.glActiveTexture(GL.GL_TEXTURE0);
      gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
      
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      
      gl.glTexImage2D (
              GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8,
              myTexture.width, myTexture.height, 0, GL.GL_BGRA,
              GL.GL_UNSIGNED_BYTE, byteBuffer);
      
      checkError(gl, "savedTexture");            
   }
   
         
   protected int generateTextureId(GL3 gl) {
      // allocate an array of one element in order to strore 
      // the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenTextures( 1, idArray, 0);

      // return the id
      return idArray[0];
   }

   
   // -----------------------------------------------------------------------
   // CameraBall to Matrices
   // -----------------------------------------------------------------------

   private CameraBall cameraBall;
   
   // storage for Matrices
   float projMatrix[] = new float[16];
   float viewMatrix[] = new float[16];
   
   private void setMatrix(float[] m,
         float xx, float xy, float xz, float xw,
         float yx, float yy, float yz, float yw,
         float zx, float zy, float zz, float zw,
         float wx, float wy, float wz, float ww) {

      m[0]=xx; m[4]=xy; m[8]=xz;  m[12]=xw;
      m[1]=yx; m[5]=yy; m[9]=yz;  m[13]=yw;
      m[2]=zx; m[6]=zy; m[10]=zz; m[14]=zw;
      m[3]=wx; m[7]=wy; m[11]=wz; m[15]=ww;
   }
   
   private void updateMatrices() {
      
      float fov    = cameraBall.getVerticalFOV();
      float aspect = cameraBall.getAspectRatio();
      float zNear = 1.0f;
      float zFar  = 30.0f;
      
      float f = 1.0f / (float) Math.tan(fov * (Math.PI / 360.0));

      // https://unspecified.wordpress.com/2012/06/21/
      //      calculating-the-gluperspective-matrix-and-other-opengl-matrix-maths/      
      setMatrix(projMatrix,
            
         f/aspect,         0.0f,                         0.0f,                              0.0f,
             0.0f,            f,                         0.0f,                              0.0f,
             0.0f,         0.0f,    (zFar+zNear)/(zNear-zFar),    (2.0f*zFar*zNear)/(zNear-zFar),
             0.0f,         0.0f,                        -1.0f,                              0.0f );
            
      
      Vector3f camPos  = cameraBall.getCameraPosition();
      Vector3f camFwd  = cameraBall.getLookatPoint().minus(camPos).normalized();
      Vector3f camRt   = camFwd.cross(cameraBall.getCameraUpVector()).normalized();
      Vector3f camUp   = camRt.cross(camFwd);
      
      setMatrix(viewMatrix,
            
          camRt.x,     camRt.y,      camRt.z,     -camPos.dot(camRt),
          camUp.x,     camUp.y,      camUp.z,     -camPos.dot(camUp),
        -camFwd.x,   -camFwd.y,    -camFwd.z,      camPos.dot(camFwd),
             0.0f,        0.0f,         0.0f,      1.0f);
      
      
      // lookat-point -->
      //    x ==   camRt.lookAt  - camRt.camPos    ==  camRt.(lookAt-camPos)    == 0   (camRt is perp to the line from camPos to lookAt)
      //    y ==   camUp.lookAt  - camUp.camPos    ==  camUp.(lookAt-camPos)    == 0   (camUp is perp to the line from camPos to lookAt)
      //    z == - camFwd.lookAt + camFwd.camPos   == - camFwd.(lookAt-camPos)  == -d  (camFwd is normalized and colinear with (lookAt-camPos))
      //                                                                                so d is the DISTANCE from lookAt to camPos
      //    w == 1
      
      //
      // so if P is camPos,  (viewMatrix x P) is <0,0,0>
      // so if P is lookAt,  (viewMatrix x P) is <0,0,-d> where -d is the distance from lookAt to camPos
      
      //
      // if d is zNear,
      //
      // then projMatrix * viewMatrix * lookAt  == projMatrix * <0,0,-zNear,1>  ==
      //
      //  z ==    ( - zNear * (zFar + zNear) + 2*zFar*zNear ) / (zNear - zFar)  ==  -zNear (zNear-zFar) / (zNear - zFar) == -zNear
      //  w ==    zNear
      //
      // resulting in <0,0,-1>
      
      // if d is zFar,
      //
      // then projMatrix * viewMatrix * lookAt  == projMatrix * <0,0,-zFar,1>  ==
      //
      //  z ==    ( - zFar * (zFar + zNear) + 2*zFar*zNear ) / (zNear - zFar)  ==  zFar (zNear-zFar) / (zNear - zFar) == zFar
      //  w ==    zFar
      //
      // resulting in <0,0,1>
      
      
      
   }

   
   // -----------------------------------------------------------------------
   // Rendering
   // -----------------------------------------------------------------------

   protected void renderScene(GL3 gl) {

      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      // must be called after glUseProgram
      // set the view and the projection matrix 
      gl.glUniformMatrix4fv( this.projMatrixLoc, 1, false, this.projMatrix, 0);
      gl.glUniformMatrix4fv( this.viewMatrixLoc, 1, false, this.viewMatrix, 0);

      gl.glBindVertexArray(this.triangleVAO);
      gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3*m.numTriangles());

      // Check out error
      checkError(gl, "render");
   }

   void checkError(GL3 gl, String point) {
      int error = gl.glGetError();
      if (error!=0){
         System.out.format("At [%s] ERROR on render: %d\n", point, error);}
   }
}

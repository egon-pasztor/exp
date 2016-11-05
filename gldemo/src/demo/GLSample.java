package demo;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.JFrame;

import demo.World.*;
import demo.Raster.*;
import demo.VectorAlgebra.*;

public class GLSample implements GLEventListener, MouseListener, MouseMotionListener {

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
   
   // -----------------------------------------------------------
   // Constructor
   // -----------------------------------------------------------

   private final DemoWorld demoWorld;
   public Camera.Controller cameraController;   
   private final GLCanvas glCanvas;
   
   private final long startTimeMillis;

   
   public GLSample() {
      System.out.println("GLSample constructor BEGIN\n");
      
      //demoWorld = new DemoWorld(/* cube */ false, /* ico */ true, /* ball */ false, /* subdivide */ 2);
      demoWorld = new DemoWorld(/* cube */ true, /* ico */ false, /* ball */ false, /* subdivide */ 0);
      //demoWorld = new DemoWorld(/* cube */ false, /* ico */ false, /* ball */ true, /* subdivide */ 0);
      
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
      System.out.format("----------------------\n");
      
      GL3 gl = drawable.getGL().getGL3();
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

   int hoverX, hoverY;

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
   
   // -----------------------------------------------------------
   // GL-RENDERING
   // -----------------------------------------------------------


   
   // ###################################################################
   // GL-WRANGLING
   // ###################################################################

   //    "initGL" (GL3)   -- bind all the RenderingStrategy / Texture / TexturedMeshs 
   //                           instances to GL-vertex-array-objects
   //                           save a map from Textures and TexturedMeshs to the GL-vertex-array-object IDs
   
   public void initGL(GL3 gl) {
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

      // TODO -- this is sending one program (for texture-mapped-surfaces) to GL
      //    but the different models in the model tree might use different programs
      setupProgram(gl);
      setupTextures(gl);
      setupModels(gl);
   }
   
   //    "renderGL" (GL3)  -- set camera-ball perspective matrix
   //                         for each TexturedMeshInstance
   //                              set the appropriate GL-vertex-array-object IDs
   //                              set the appropriate view matrix
   //                              render
   
   public void renderGL(GL3 gl) {
      updateModifiedModels(gl);

      Camera camera = cameraController.getCamera();
      
      int width = camera.width;
      int height = camera.height;
      
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
      sendMatrixToGL(gl, camera.cameraToClipSpace, projMatrixLoc);
      
      renderSubmodels(gl, demoWorld.getRootModel(), camera.worldToCameraSpace);

      // Check out error
      checkError(gl, "render");
   }
   
   public void renderSubmodels(GL3 gl, Model m, Matrix4f worldToCamera) {
      Matrix4f modelToCamera = Matrix4f.product(worldToCamera, m.getModelToWorld());
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel)m).children) {
            renderSubmodels(gl, child, modelToCamera);
         }
      }
      if (m instanceof TexturedMeshModel) {
         TexturedMeshModel tm = (TexturedMeshModel) m;

         // Save Model-To-Camera-Space Matrix
         sendMatrixToGL(gl, modelToCamera, viewMatrixLoc);

         // Bind texture -- 
         //    we're binding the GL_TEXTURE2D field in GL_TEXTURE0,
         //       because GL_TEXTURE0 is the "active" texture
         //    and the SHADER uses GL_TEXTURE0 
         //       because the shader "myTexture" field is bound to "0".
         //
         // We could set it up differently... right?            
         //  
         int textureId = imageToGLId.get(tm.texture); 
         gl.glBindTexture(GL.GL_TEXTURE_2D, textureId); 

         // Bind model
         GeometryBindings modelId = modelToBindings.get(tm.geometry);
         gl.glBindVertexArray(modelId.vaoID);

         //System.out.format("Testing intersection with [%s]\n", tm.geometry.getName());

         if (intersects(tm.geometry, modelToCamera, cameraController.getCamera(), hoverX, hoverY)) {
            gl.glUniform1i(highlightBoolLoc, 1);
         } else {
            gl.glUniform1i(highlightBoolLoc, 0);
         }

         
         // Draw the bound arrays...
         gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3 * tm.geometry.getNumTriangles());
      }
   }
   
   public void sendMatrixToGL(GL3 gl, Matrix4f m, int glLoc) {
      float arr[] = new float[16];
      m.copyToFloatArray(arr);
      gl.glUniformMatrix4fv(glLoc, 1, false, arr, 0);     
   }

   // --------------------------------------------------------------
   // PICKING VERSION 0 -- let's just answer the question here...

   private boolean intersects(Geometry.Model geometry,  // does this geometry...
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

      // WAIT-CONFUSING SIGN ERROR???
      Vector3f camPos = new Vector3f(xPos,yPos,-1.0f);
      //System.out.format("At %d,%d in %dx%d window -- fWidth %g x fHeight %g\n",
      //                  x,y,width,height,fWidth,fHeight);
      //System.out.format("PixelPos is\n%s\n", camPos.toString());

      Segment s = new Segment(Vector3f.ORIGIN, camPos);
      // --------------------
  
      for (Geometry.Mesh.Triangle<Vector3f,Geometry.Model.TexCoords> t : geometry.mesh.interiorTriangles) {
         Vector3f v0 = t.edge0.getOppositeVertex().getData();
         Vector3f v1 = t.edge1.getOppositeVertex().getData();
         Vector3f v2 = t.edge2.getOppositeVertex().getData();

         Vector3f camV0 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v0)).toVector3f();
         Vector3f camV1 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v1)).toVector3f();
         Vector3f camV2 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v2)).toVector3f();

         //System.out.format("Triangle is\n%s---\n%s---\n%s---\n", camV0.toString(), camV1.toString(), camV2.toString());

         Triangle t2 = new Triangle(camV0, camV1, camV2);
         if (intersects(t2,s)) return true;      
      }
      return false;
   }

   public static class Segment {
      public final Vector3f p0;
      public final Vector3f p1;
      public Segment(Vector3f p0, Vector3f p1) {
         this.p0 = p0;
         this.p1 = p1;
      }
   }
   public static class Triangle {
      public final Vector3f v0;
      public final Vector3f v1;
      public final Vector3f v2;
      public Triangle(Vector3f v0, Vector3f v1, Vector3f v2) {
         this.v0 = v0;
         this.v1 = v1;
         this.v2 = v2;
      }
   }
   public static boolean intersects(Triangle t, Segment s) {
      Vector3f u = t.v1.minus(t.v0);
      Vector3f v = t.v2.minus(t.v0);
      Vector3f n = u.cross(v).normalized();

      // http://geomalgorithms.com/a06-_intersect-2.html

      float den = n.dot(s.p1.minus(s.p0));
      if (den == 0) return false;

      float r1 = n.dot(t.v0.minus(s.p0)) / den;
      //System.out.format("Intersection f=%g\n", r1);

      Vector3f i = s.p0.plus(s.p1.minus(s.p0).times(r1));

      //System.out.format("Plane intersection at\n%s", i.toString());

      Vector3f w = i.minus(t.v0);
 
      float den2 = u.dot(v) * u.dot(v)
                 - u.dot(u) * v.dot(v);
      if (den2 == 0) return false;

      float snum = u.dot(v) * w.dot(v)
                 - v.dot(v) * w.dot(u);

      float tnum = u.dot(v) * w.dot(u)
                 - u.dot(u) * w.dot(v);

      float sc = snum/den2;
      float tc = tnum/den2;
      if (sc<0) return false;
      if (tc<0) return false;
      if (sc+tc>1) return false;
      return true;
   }



   // -------------------------------------------------------------------
   // SENDING SHADER PROGRAM to GL
   // -------------------------------------------------------------------

   // Program ID
   private int programID;

   // Vertex Attribute Locations
   private int vsPositionLoc;
   private int vsColorLoc;
   private int vsTexCoordsLoc;
   private int vsBaryCoordsLoc;

   // Uniform (vertex-shader) variable Locations
   private int projMatrixLoc;
   private int viewMatrixLoc;
   
   // Uniform (fragment-shader) variable Locations
   private int textureLoc;
   private int highlightBoolLoc;

   
   enum ShaderType{ VertexShader, FragmentShader }
   
   private void setupProgram(GL3 gl) {
      System.out.format("SETUP-program called\n");
      
      // create the two shader and compile them
      int v = this.newShaderFromCurrentClass(gl, "vertex.shader", ShaderType.VertexShader);
      int f = this.newShaderFromCurrentClass(gl, "fragment.shader", ShaderType.FragmentShader);

      System.out.format("Vertex Shader Info Log: [%s]\n", getShaderInfoLog(gl, v));
      System.out.format("Fragent Shader Info Log: [%s]\n", getShaderInfoLog(gl, f));

      // Complete the "shader program"
      this.programID = gl.glCreateProgram();
      gl.glAttachShader(programID, v);
      gl.glAttachShader(programID, f);
      gl.glLinkProgram(programID);
      
      gl.glUseProgram(programID);
      
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

      this.textureLoc       = gl.glGetUniformLocation(programID, "myTexture");
      this.highlightBoolLoc = gl.glGetUniformLocation(programID, "highlight");

      gl.glUniform1i(textureLoc, 0);
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

   private HashMap<Image, Integer> imageToGLId;

   private void setupTextures(GL3 gl) {
      System.out.format("SETUP-textures called \n");
      imageToGLId = new HashMap<Image, Integer>();

      for(Image image : demoWorld.getTextures()) {
         int id = setupTexture(gl, image);
         System.out.format("BOUND texture [%s] to id [%d]\n", image.getName(), id);
         imageToGLId.put(image, id);
      }
   }

   private int setupTexture(GL3 gl, Image image) {
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect(image.pixels.length * 4);
      byteBuffer.order(ByteOrder.nativeOrder());
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(image.pixels);
      intBuffer.position(0);
      
      int textureId = this.generateTextureId(gl);
      gl.glActiveTexture(GL.GL_TEXTURE0);
      gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
      
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      
      gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8,
                      image.width, image.height, 0, GL.GL_BGRA,
                      GL.GL_UNSIGNED_BYTE, intBuffer);
      
      checkError(gl, "savedTexture");
      return textureId;         
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
   
   // Vertex Array Object ID
   // TODO: shold be a "map" from Geom to int...
   private HashMap<Geometry.Model, GeometryBindings> modelToBindings;

   private static class GeometryBindings {
      public int vaoID;

      public int positionBufferId;
      public int colorBufferId;
      public int texCoordsBufferId;
      public int baryCoordsBufferId;
   };

   private void setupModels(GL3 gl) {
      System.out.format("SETUP-models called \n");
      modelToBindings = new HashMap<Geometry.Model, GeometryBindings>();

      for(Geometry.Model model : demoWorld.getModels()) {
         GeometryBindings b = setupModel(gl, model);
         System.out.format("BOUND model [%s] to id [%d]\n", model.getName(), b.vaoID);
         modelToBindings.put(model, b);
      }
   }
   private void updateModifiedModels(GL3 gl) {
      // What about new (unregistered) or lost (but still registered) Models?
      // We need a "delete Vertex Array" object..

      for(Geometry.Model model : demoWorld.getModels()) {
         if (model.isModified()) {
            GeometryBindings b = modelToBindings.get(model);
            updateModel(gl, model, b);
         }
      }
   }
   private GeometryBindings setupModel(GL3 gl, Geometry.Model model) {
      // generate the IDs
      GeometryBindings b = new GeometryBindings();
      b.vaoID = generateVAOId(gl);
      gl.glBindVertexArray(b.vaoID);
      
      // Generate slots
      checkError(gl, "bufferIdCreation");
      b.positionBufferId = this.generateBufferId(gl);
      b.colorBufferId = this.generateBufferId(gl);
      b.texCoordsBufferId = this.generateBufferId(gl);
      b.baryCoordsBufferId = this.generateBufferId(gl);
   
      // bind the buffers
      Geometry.Model.Arrays ma = model.getArrays();
      bindBuffer(gl, b.positionBufferId,   ma.positions,  4, vsPositionLoc);
      bindBuffer(gl, b.colorBufferId,      ma.colors,     4, vsColorLoc);
      bindBuffer(gl, b.texCoordsBufferId,  ma.texCoords,  4, vsTexCoordsLoc);
      bindBuffer(gl, b.baryCoordsBufferId, ma.baryCoords, 2, vsBaryCoordsLoc);
      return b;
   }
   private void updateModel(GL3 gl, Geometry.Model model, GeometryBindings b) {
      gl.glBindVertexArray(b.vaoID);

      Geometry.Model.Arrays ma = model.getArrays();
      updateBuffer(gl, b.positionBufferId,   ma.positions,  4, vsPositionLoc);
      updateBuffer(gl, b.colorBufferId,      ma.colors,     4, vsColorLoc);
      updateBuffer(gl, b.texCoordsBufferId,  ma.texCoords,  4, vsTexCoordsLoc);
      updateBuffer(gl, b.baryCoordsBufferId, ma.baryCoords, 2, vsBaryCoordsLoc);
   }
   
   private void bindBuffer(GL3 gl, int bufferId, float[] dataArray, int componentsPerAttribute, int dataLoc){
      // bind buffer for vertices and copy data into buffer
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      gl.glBufferData(GL.GL_ARRAY_BUFFER, dataArray.length * Float.SIZE / 8,
            Buffers.newDirectFloatBuffer(dataArray), GL.GL_STATIC_DRAW);
      gl.glEnableVertexAttribArray(dataLoc);
      gl.glVertexAttribPointer(dataLoc, componentsPerAttribute, GL.GL_FLOAT, false, 0, 0);
   }
   private void updateBuffer(GL3 gl, int bufferId, float[] dataArray, int componentsPerAttribute, int dataLoc){
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, 
            dataArray.length * Float.SIZE / 8,
            Buffers.newDirectFloatBuffer(dataArray));
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

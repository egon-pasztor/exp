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
      
      demoWorld = new DemoWorld(/* cube */ false, /* ico */ true, /* ball */ false, /* subdivide */ 2);
      //demoWorld = new DemoWorld(/* cube */ true, /* ico */ false, /* ball */ false, /* subdivide */ 0);
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
      
      GL3 gl = drawable.getGL().getGL3();
      initGL(gl);
   }

   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.format("GLSample.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
      
      demoWorld.setCamera(new Camera(width, height,
            new Vector3f(0.0f, 0.0f, 0.0f),   // look-at
            new Vector3f(0.0f, 0.0f, 10.0f),   // camera-pos
            new Vector3f(0.0f, 1.0f, 0.0f),   // camera-up
            53.13f));
      
      cameraController = new Camera.Controller(demoWorld.getCamera());
   }

   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      System.out.format("GLSample.display() called\n");
      
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
            e.isShiftDown()   ? (e.isControlDown() ? Camera.Controller.GrabType.FOV : Camera.Controller.GrabType.Zoom)
                              : (e.isControlDown() ? Camera.Controller.GrabType.Pan : Camera.Controller.GrabType.Rotate));
   }

   @Override
   public void mouseDragged(MouseEvent e) {
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
      
      // TODO -- this is sending one texture to GL,
      //    but the different models in the model tree might want different textures!
      setupTexture(gl);
      
      // TODO -- this is sending one geometry model to GL
      //    but the different models in the model tree might have different shapes!
      setupBuffers(gl);
   }
   
   //    "renderGL" (GL3)  -- set camera-ball perspective matrix
   //                         for each TexturedMeshInstance
   //                              set the appropriate GL-vertex-array-object IDs
   //                              set the appropriate view matrix
   //                              render
   
   public void renderGL(GL3 gl) {
      Camera camera = demoWorld.getCamera();
      
      int width = camera.getWidth();
      int height = camera.getHeight();
      
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
      sendMatrixToGL(gl, camera.getCameraToClipSpace(), projMatrixLoc);
      
      // Bind geometry arrays for ONE TEXTURED MODEL
      gl.glBindVertexArray(this.triangleVAO);

      renderSubmodels(gl, demoWorld.getRootModel(), camera.getWorldToCameraSpace());

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
         // Save Model-To-Camera-Space Matrix
         sendMatrixToGL(gl, modelToCamera, viewMatrixLoc);
         
         // Draw the bound arrays...
         gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3 * ((TexturedMeshModel)m).geometry.getNumTriangles());
      }
   }
   
   public void sendMatrixToGL(GL3 gl, Matrix4f m, int glLoc) {
      float arr[] = new float[16];
      m.copyToFloatArray(arr);
      gl.glUniformMatrix4fv(glLoc, 1, false, arr, 0);     
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

   // Uniform variable Locations
   private int projMatrixLoc;
   private int viewMatrixLoc;
   
   // Texture Locations
   private int textureLoc;
   
   enum ShaderType{ VertexShader, FragmentShader }
   
   private void setupProgram(GL3 gl) {
      System.out.format("SETUP-program called\n");
      
      // create the two shader and compile them
      int v = this.newShaderFromCurrentClass(gl, "vertex.shader", ShaderType.VertexShader);
      int f = this.newShaderFromCurrentClass(gl, "fragment.shader", ShaderType.FragmentShader);

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

   private int newShaderFromCurrentClass(GL3 gl, String fileName, ShaderType type){
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
   
   private void setupTexture(GL3 gl) {
      System.out.format("SETUP-texture called\n");
   
      // myTexture.fillRect(0,  0, 10, 128, 0x00ff0000);
      // myTexture.fillRect(10, 0, 10, 128, 0x0000ff00);
      // myTexture.fillRect(20, 0, 10, 128, 0x000000ff);
      
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect(demoWorld.myTexture.pixels.length * 4);
      byteBuffer.order(ByteOrder.nativeOrder());
      IntBuffer colormapBuffer = byteBuffer.asIntBuffer();
      colormapBuffer.put(demoWorld.myTexture.pixels);
      colormapBuffer.position(0);
      
      int textureId = this.generateTextureId(gl);
      gl.glActiveTexture(GL.GL_TEXTURE0);
      gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
      
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      
      gl.glTexImage2D (
              GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8,
              demoWorld.myTexture.width, demoWorld.myTexture.height, 0, GL.GL_BGRA,
              GL.GL_UNSIGNED_BYTE, byteBuffer);
      
      checkError(gl, "savedTexture");            
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

      Geometry.Model.Arrays ma = demoWorld.geom.getArrays();
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
      // allocate an array of one element in order to store the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenVertexArrays(1, idArray, 0);
      // return the id
      return idArray[0];
   }

   protected int generateBufferId(GL3 gl) {
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

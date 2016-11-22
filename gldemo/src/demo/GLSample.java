package demo;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.JFrame;

import demo.World.*;
import demo.Geometry.*;
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
      
      demoWorld = new DemoWorld();
      
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
      demoWorld.bindPositions(camera.cameraToClipSpace, camera.worldToCameraSpace);
      
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

         if (intersects(model, viewMatrix, cameraController.getCamera(), hoverX, hoverY)) {
            shaderInstance.bind(Shader.HIGHLIGHT_BOOL, new Shader.Variable.Uniform.IntBinding(1));
         } else {
            shaderInstance.bind(Shader.HIGHLIGHT_BOOL, new Shader.Variable.Uniform.IntBinding(0));
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
  
      for (Geometry.Mesh.Triangle t : geometry.mesh.interiorTriangles) {
         Vector3f v0Pos = t.edge0.getOppositeVertex().getPosition();
         Vector3f v1Pos = t.edge1.getOppositeVertex().getPosition();
         Vector3f v2Pos = t.edge2.getOppositeVertex().getPosition();

         Vector3f camV0 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v0Pos)).toVector3f();
         Vector3f camV1 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v1Pos)).toVector3f();
         Vector3f camV2 = Matrix4f.product(modelToCamera, Vector4f.fromVector3f(v2Pos)).toVector3f();

         //System.out.format("Triangle is\n%s---\n%s---\n%s---\n", camV0.toString(), camV1.toString(), camV2.toString());

         Triangle t2 = new Triangle(camV0, camV1, camV2);
         if (VectorAlgebra.intersects(t2,s)) return true;      
      }
      return false;
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

   private void setupTextures(GL3 gl) {
      gl.glActiveTexture(GL.GL_TEXTURE0);
      
      HashSet<Shader.Instance> shaderInstances = demoWorld.getShaderInstances();
      HashSet<Shader.ManagedTexture> textures = getAllTextures(shaderInstances);
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
   private HashSet<Shader.ManagedTexture> getAllTextures(Collection<Shader.Instance> shaderInstances) {
      HashSet<Shader.ManagedTexture> textures = new HashSet<Shader.ManagedTexture>();
      for (Shader.Instance shaderInstance : shaderInstances) {
         for (Map.Entry<Shader.Variable, Shader.Variable.Binding> entry : shaderInstance.boundVariables.entrySet()) {
            Shader.Variable.Binding binding = entry.getValue();
            if (binding instanceof Shader.Variable.Uniform.TextureBinding) {
               textures.add(((Shader.Variable.Uniform.TextureBinding) binding).texture);
            }
         }
      }
      return textures;
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
      HashSet<Shader.ManagedBuffer> buffers = getAllBuffers(shaderInstances);
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
      HashSet<Shader.ManagedBuffer> buffers = getAllBuffers(shaderInstances);
      
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
   private HashSet<Shader.ManagedBuffer> getAllBuffers(Collection<Shader.Instance> shaderInstances) {
      HashSet<Shader.ManagedBuffer> buffers = new HashSet<Shader.ManagedBuffer>();
      for (Shader.Instance shaderInstance : shaderInstances) {
         
         // Walk through all the buffers in this instance:
         for (Shader.Variable variable : shaderInstance.program.variables) {
            if (variable instanceof Shader.Variable.Buffer) {
               Shader.Variable.Binding binding = shaderInstance.boundVariables.get(variable);
               Shader.ManagedBuffer buffer = ((Shader.Variable.Buffer.Binding) binding).buffer;
               buffers.add(buffer);
            }
         }
      }
      return buffers;
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

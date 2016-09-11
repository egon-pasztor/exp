package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;

/**
 * inspired from http://www.lighthouse3d.com/cg-topics/code-samples/opengl-3-3-glsl-1-5-sample/
 * 
 */
public class GLSample implements GLEventListener {
   
   // -----------------------------------------------------------------------
   // It just seems simpler, more relaxing, to have it all here in this file
   // -----------------------------------------------------------------------
   
   public static class Vector3f {
      
       public float x;
       public float y;
       public float z;
   
       public Vector3f() {}
   
       public Vector3f(float x, float y, float z) {
           this.x = x;
           this.y = y;
           this.z = z;
       }
      
       public float length() {
          return (float) Math.sqrt(x * x + y * y + z * z);
       }
       public Vector3f normalize() {
          return this.times(1.0f / length());
       }
       
       public Vector3f times(float scalar) {   
          return new Vector3f(x * scalar, y * scalar, z * scalar);
       }
       public Vector3f plus(Vector3f vec3) {
          return new Vector3f(x + vec3.x, y + vec3.y, z + vec3.z);
       }
       public Vector3f minus(Vector3f vec3) {
           return new Vector3f(x - vec3.x, y - vec3.y, z - vec3.z);
       }
       public Vector3f cross(Vector3f vec3) {
          return new Vector3f(y * vec3.z - z * vec3.y, 
                          z * vec3.x - x * vec3.z, 
                          x * vec3.y - y * vec3.x);
       }
       public float dot(Vector3f vec3) {
          return x * vec3.x + y * vec3.y + z * vec3.z;
       }
       public Vector3f negated() {
          return new Vector3f(-x, -y, -z);
       }
       public String toString() {
          return String.format("(%g,%g,%g)",  x,y,z);
       }
   }
   
   public static Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
   public static Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
   public static Vector3f Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);
   
   // -----------------------------------------------------------------------
   
   public static class Vector2f {
      
       public float x;
       public float y;
   
       public Vector2f() {}
   
       public Vector2f(float x, float y) {
           this.x = x;
           this.y = y;
       }
      
       public float length() {
          return (float) Math.sqrt(x * x + y * y);
       }
       public Vector2f normalize() {
          return this.times(1.0f / length());
       }
       
       public Vector2f times(float scalar) {   
          return new Vector2f(x * scalar, y * scalar);
       }
       public Vector2f plus(Vector2f vec3) {
          return new Vector2f(x + vec3.x, y + vec3.y);
       }
       public Vector2f minus(Vector2f vec3) {
           return new Vector2f(x - vec3.x, y - vec3.y);
       }
       public float dot(Vector2f vec2) {
          return x * vec2.x + y * vec2.y;
       }
       public Vector2f negated() {
          return new Vector2f(-x, -y);
       }
       public String toString() {
          return String.format("(%g,%g)",  x,y);
       }
   }

   // -----------------------------------------------------------------------
   
   public static class ColorRGBA {
   
       public byte r;
       public byte g;
       public byte b;
       public byte a;
   
       public ColorRGBA() {}
   
       public ColorRGBA(byte r, byte g, byte b, byte a) {
           this.r = r;
           this.g = g;
           this.b = b;
           this.a = a;
       }
       
       public int toInteger() {
          return ((int)r)<<24 | ((int)g)<<16 | ((int)b)<<8 | ((int)a);
       }
       public String toString() {
          return String.format("#%02x%02x%02x%02x", r,g,b,a);
       }
   }

   

   public static class Triangle {
       private Vector3f  v1,v2,v3;       
       private Vector3f  n1,n2,n3;
       private Vector2f  t1,t2,t3;
       private ColorRGBA c1,c2,c3;
       
   }
   
   // -----------------------------------------------------------------------
   // -----------------------------------------------------------------------

   
   
   
   enum ShaderType{ VertexShader, FragmentShader}

   // Data for triangle  x,y,z,w
   float vertices[] = { 
         0.0f, 0.0f, .0f, 1.0f,  
         0.0f, 1.0f, .0f, 1.0f,
         1.0f, 0.0f, .0f, 1.0f };

   float colorArray[] = {  // RGBA
         0.0f, 0.0f, 1.0f, 1.0f, 
         0.0f, 1.0f, 0.0f, 1.0f, 
         0.0f, 0.0f, 1.0f, 1.0f };
   
   float uvArray[] = { 
         1.0f, 0.0f,  
         0.0f, 1.0f, 
         0.0f, 0.0f, };

   // Program
   int programID;

   // Vertex Attribute Locations
   int vertexLoc, colorLoc, uvLoc;

   // Uniform variable Locations
   int projMatrixLoc, viewMatrixLoc;

   // storage for Matrices
   float projMatrix[] = new float[16];
   float viewMatrix[] = new float[16];

   protected int triangleVAO;

   // ------------------
   // VECTOR STUFF
   //

   // res = a cross b;
   void crossProduct(float a[], float b[], float res[]) {

      res[0] = a[1] * b[2] - b[1] * a[2];
      res[1] = a[2] * b[0] - b[2] * a[0];
      res[2] = a[0] * b[1] - b[0] * a[1];
   }

   // Normalize a vec3
   void normalize(float a[]) {

      float mag = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);

      a[0] /= mag;
      a[1] /= mag;
      a[2] /= mag;
   }

   // ----------------
   // MATRIX STUFF
   //

   // sets the square matrix mat to the identity matrix,
   // size refers to the number of rows (or columns)
   void setIdentityMatrix(float[] mat, int size) {

      // fill matrix with 0s
      for (int i = 0; i < size * size; ++i)
         mat[i] = 0.0f;

      // fill diagonal with 1s
      for (int i = 0; i < size; ++i)
         mat[i + i * size] = 1.0f;
   }

   //
   // a = a * b;
   //
   void multMatrix(float[] a, float[] b) {

      float[] res = new float[16];

      for (int i = 0; i < 4; ++i) {
         for (int j = 0; j < 4; ++j) {
            res[j * 4 + i] = 0.0f;
            for (int k = 0; k < 4; ++k) {
               res[j * 4 + i] += a[k * 4 + i] * b[j * 4 + k];
            }
         }
      }
      System.arraycopy(res, 0, a, 0, 16);
   }

   // Defines a transformation matrix mat with a translation
   void setTranslationMatrix(float[] mat, float x, float y, float z) {

      setIdentityMatrix(mat, 4);
      mat[12] = x;
      mat[13] = y;
      mat[14] = z;
   }

   // ------------------
   // Projection Matrix
   //

   float[] buildProjectionMatrix(float fov, float ratio, float nearP, float farP, float[] projMatrix) {

      float f = 1.0f / (float) Math.tan(fov * (Math.PI / 360.0));

      setIdentityMatrix(projMatrix, 4);

      projMatrix[0] = f / ratio;
      projMatrix[1 * 4 + 1] = f;
      projMatrix[2 * 4 + 2] = (farP + nearP) / (nearP - farP);
      projMatrix[3 * 4 + 2] = (2.0f * farP * nearP) / (nearP - farP);
      projMatrix[2 * 4 + 3] = -1.0f;
      projMatrix[3 * 4 + 3] = 0.0f;

      return projMatrix;
   }

   // ------------------
   // View Matrix
   //
   // note: it assumes the camera is not tilted,
   // i.e. a vertical up vector (remmeber gluLookAt?)
   //

   float[] setCamera(float posX, float posY, float posZ, float lookAtX,
         float lookAtY, float lookAtZ, float[] viewMatrix) {

      float[] dir = new float[3];
      float[] right = new float[3];
      float[] up = new float[3];

      up[0] = 0.0f;
      up[1] = 1.0f;
      up[2] = 0.0f;

      dir[0] = (lookAtX - posX);
      dir[1] = (lookAtY - posY);
      dir[2] = (lookAtZ - posZ);
      normalize(dir);

      crossProduct(dir, up, right);
      normalize(right);

      crossProduct(right, dir, up);
      normalize(up);

      float[] aux = new float[16];

      viewMatrix[0] = right[0];
      viewMatrix[4] = right[1];
      viewMatrix[8] = right[2];
      viewMatrix[12] = 0.0f;

      viewMatrix[1] = up[0];
      viewMatrix[5] = up[1];
      viewMatrix[9] = up[2];
      viewMatrix[13] = 0.0f;

      viewMatrix[2] = -dir[0];
      viewMatrix[6] = -dir[1];
      viewMatrix[10] = -dir[2];
      viewMatrix[14] = 0.0f;

      viewMatrix[3] = 0.0f;
      viewMatrix[7] = 0.0f;
      viewMatrix[11] = 0.0f;
      viewMatrix[15] = 1.0f;

      setTranslationMatrix(aux, -posX, -posY, -posZ);

      multMatrix(viewMatrix, aux);

      return viewMatrix;
   }

   // ------------------

   void changeSize(GL3 gl, int w, int h) {

      float ratio;
      // Prevent a divide by zero, when window is too short
      // (you cant make a window of zero width).
      if (h == 0)
         h = 1;

      // Set the viewport to be the entire window
      //gl.glViewport(0, 0, w, h);

      ratio = (1.0f * w) / h;
      this.projMatrix = buildProjectionMatrix(53.13f, ratio, 1.0f, 30.0f, this.projMatrix);
   }

   void setupBuffers(GL3 gl) {
      // generate the IDs
      this.triangleVAO = this.generateVAOId(gl);

      // create the buffer and link the data with the location inside the vertex shader
//      this.newFloatBuffers3(gl, this.triangleVAO, 
//            this.vertices, this.colorArray, this.uvArray,
//            this.vertexLoc, this.colorLoc, this.uvLoc);
      this.newFloatBuffers(gl, this.triangleVAO, 
           this.vertices, this.colorArray,
           this.vertexLoc, this.colorLoc);
   }

   void newFloatBuffers(GL3 gl, int vaoId, 
         float[] verticesArray, float[] colorArray,
         int verticeLoc, int colorLoc){
      // bind the correct VAO id
      gl.glBindVertexArray( vaoId);
      // Generate two slots for the vertex and color buffers
      int vertexBufferId = this.generateBufferId(gl);
      int colorBufferId = this.generateBufferId(gl);
   
      // bind the two buffer
      this.bindBuffer(gl, vertexBufferId, verticesArray, verticeLoc);
      this.bindBuffer(gl, colorBufferId, colorArray, colorLoc);
   }
   void bindBuffer(GL3 gl, int bufferId, float[] dataArray, int dataLoc){
      // bind buffer for vertices and copy data into buffer
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      gl.glBufferData(GL.GL_ARRAY_BUFFER, dataArray.length * Float.SIZE / 8,
            Buffers.newDirectFloatBuffer(dataArray), GL.GL_STATIC_DRAW);
      gl.glEnableVertexAttribArray(dataLoc);
      gl.glVertexAttribPointer(dataLoc, 4, GL.GL_FLOAT, false, 0, 0);

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

   protected void renderScene(GL3 gl) {

      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      setCamera(  0.5f, 0.5f,  2, 
               0.5f, 0.5f, -1,
               this.viewMatrix);

      gl.glUseProgram(this.programID);

      // must be called after glUseProgram
      // set the view and the projection matrix 
      gl.glUniformMatrix4fv( this.projMatrixLoc, 1, false, this.projMatrix, 0);
      gl.glUniformMatrix4fv( this.viewMatrixLoc, 1, false, this.viewMatrix, 0);

      gl.glBindVertexArray(this.triangleVAO);
      gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);

      // Check out error
      int error = gl.glGetError();
      if(error!=0){
         System.err.println("ERROR on render : " + error);}
   }

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
   public String printProgramInfoLog(GL3 gl, int obj) {
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

   protected String loadStringFileFromCurrentPackage( String fileName){
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

   int newProgram(GL3 gl) {
      // create the two shader and compile them
      int v = this.newShaderFromCurrentClass(gl, "vertex2.shader", ShaderType.VertexShader);
      int f = this.newShaderFromCurrentClass(gl, "fragment2.shader", ShaderType.FragmentShader);

      System.out.println(getShaderInfoLog(gl, v));
      System.out.println(getShaderInfoLog(gl, f));

      int p = this.createProgram(gl, v, f);

      gl.glBindFragDataLocation(p, 0, "outColor");
      printProgramInfoLog(gl, p);

      this.vertexLoc = gl.glGetAttribLocation( p, "position");
      this.colorLoc = gl.glGetAttribLocation( p, "color");

      this.projMatrixLoc = gl.glGetUniformLocation( p, "projMatrix");
      this.viewMatrixLoc = gl.glGetUniformLocation( p, "viewMatrix");

      return p;
   }

   private int createProgram(GL3 gl, int vertexShaderId, int fragmentShaderId) {
      // generate the id of the program
      int programId = gl.glCreateProgram();
      // attach the two shader
      gl.glAttachShader(programId, vertexShaderId);
      gl.glAttachShader(programId, fragmentShaderId);
      // link them
      gl.glLinkProgram(programId);

      return programId;
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

   /** GL Init */
   @Override
   public void init(GLAutoDrawable drawable) {
      GL3 gl = drawable.getGL().getGL3();
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

      this.programID = this.newProgram(gl);
      this.setupBuffers(gl);
   }

   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      float ratio;
      // Prevent a divide by zero, when window is too short
      // (you can't make a window of zero width).
      if (height == 0)
         height = 1;

      ratio = (1.0f * width) / height;
      this.projMatrix = buildProjectionMatrix(53.13f, ratio, 1.0f, 30.0f, this.projMatrix);
   }

   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      GL3 gl = drawable.getGL().getGL3();
      renderScene(gl);
   }

   /** GL Complete */
   @Override
   public void dispose(GLAutoDrawable drawable) {
   }

   public static JFrame newJFrame(String name, GLEventListener sample, int x,
         int y, int width, int height) {
      JFrame frame = new JFrame(name);
      frame.setBounds(x, y, width, height);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      GLProfile glp = GLProfile.get(GLProfile.GL3);
      GLCapabilities glCapabilities = new GLCapabilities(glp);
      GLCanvas glCanvas = new GLCanvas(glCapabilities);

      glCanvas.addGLEventListener(sample);
      frame.add(glCanvas);

      return frame;
   }

   public static void main(String[] args) {
      // allocate the openGL application
      GLSample sample = new GLSample();

      // allocate a frame and display the openGL inside it
      JFrame frame = newJFrame("JOGL3 sample with Shader",
            sample, 10, 10, 300, 200);

      // display it and let's go
      frame.setVisible(true);
   }
}
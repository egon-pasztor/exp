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


/**
 * inspired from http://www.lighthouse3d.com/cg-topics/code-samples/opengl-3-3-glsl-1-5-sample/
 * 
 */
public class GLSample implements GLEventListener, MouseListener, MouseMotionListener {

   private GLCanvas glCanvas;
   
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
                                  new Vector3f(0.5f, 0.5f, -1),   // look-at
                                  new Vector3f(0.5f, 0.5f, 2),    // camera-pos
                                  new Vector3f(0.0f, 1.0f, 0.0f), // camera-up
                                  53.13f);
      updateMatrices();
//      
//      float ratio;
//      // Prevent a divide by zero, when window is too short
//      // (you can't make a window of zero width).
//      if (height == 0)
//         height = 1;
//
//      ratio = (1.0f * width) / height;
//      this.projMatrix = buildProjectionMatrix(53.13f, ratio, 1.0f, 30.0f, this.projMatrix);
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

   // Uniform variable Locations
   int projMatrixLoc;
   int viewMatrixLoc;
   
   // Texture Locations
   int textureLoc;
   
   
   enum ShaderType{ VertexShader, FragmentShader}
   
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

   // Vertex Array Object ID
   int triangleVAO;

   // Data for triangle  x,y,z,w
   float positions[] = { 
         0.0f, 0.0f, 0.0f, 1.0f,  
         0.0f, 1.0f, 0.0f, 1.0f,
         1.0f, 0.0f, 0.0f, 1.0f,
         
         0.1f, 1.0f, 0.0f, 1.0f,  
         1.1f, 0.0f, 0.0f, 1.0f,
         1.1f, 1.0f, 0.0f, 1.0f 
   
   };

   float colors[] = {  // RGBA
         0.0f, 0.0f, 1.0f, 1.0f, 
         0.0f, 1.0f, 0.0f, 1.0f, 
         0.0f, 0.0f, 1.0f, 1.0f,
         
         0.0f, 1.0f, 0.0f, 1.0f,
         0.0f, 0.0f, 1.0f, 1.0f, 
         0.0f, 1.0f, 0.0f, 1.0f,
   };
   
   float texCoords[] = { 
         0.0f, 1.0f,  
         0.0f, 0.0f, 
         1.0f, 1.0f, 

         0.0f, 0.0f,  
         1.0f, 1.0f, 
         1.0f, 0.0f,          
   };

   
   void setupBuffers(GL3 gl) {
      System.out.format("SETUP-buffers called\n");
      
      // generate the IDs
      this.triangleVAO = this.generateVAOId(gl);
      gl.glBindVertexArray( triangleVAO);
      
      // Generate slots
      checkError(gl, "bufferIdCreation");
      int positionBufferId = this.generateBufferId(gl);
      int colorBufferId = this.generateBufferId(gl);
      int texCoordsBufferId = this.generateBufferId(gl);
   
      // bind the buffers
      checkError(gl, "bind0");
      this.bindBuffer(gl, positionBufferId,  positions, 4,vsPositionLoc);
      checkError(gl, "bind1");
      this.bindBuffer(gl, colorBufferId,     colors,    4,vsColorLoc);
      checkError(gl, "bind2");
      this.bindBuffer(gl, texCoordsBufferId, texCoords, 2,vsTexCoordsLoc);
      checkError(gl, "bind3");
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
      myTexture.fillRect(10, 10, 30, 30, 0x00ff0000);
      myTexture.fillRect(30, 30, 30, 30, 0x0000ff00);
      myTexture.fillRect(50, 50, 30, 30, 0x000000ff);
      
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
      gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);

      // Check out error
      checkError(gl, "render");
   }

   void checkError(GL3 gl, String point) {
      int error = gl.glGetError();
      if (error!=0){
         System.out.format("At [%s] ERROR on render: %d\n", point, error);}
   }
}

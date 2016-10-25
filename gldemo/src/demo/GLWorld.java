package demo;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import demo.GLMath.*;
import demo.VectorAlgebra.*;
import demo.Raster.*;
import demo.Geometry.*;


public class GLWorld {

   private boolean cube = false;  
   private boolean ico = true;
   private boolean ball = false;
   private int subdivide = 1;
   
   public GLWorld(boolean cube, boolean ico, boolean ball, int subdivide) {
      this.cube = cube;
      this.ico = ico;
      this.ball = ball;
      this.subdivide = subdivide;
      initModel();
      updateTranslations(0.0f);
   }
   
   public void setWindowSize(int width, int height) {
      camera = new Camera(width, height,
            new Vector3f(0.0f, 0.0f, 0.0f),   // look-at
            new Vector3f(0.0f, 0.0f, 10.0f),   // camera-pos
            new Vector3f(0.0f, 1.0f, 0.0f),   // camera-up
            53.13f);

      cameraController = new CameraController(camera);
   }
      
   private Camera camera;
   public CameraController cameraController;
   
   // -------------------------------------------------------------------
   // RENDER-STRATEGY
   // -------------------------------------------------------------------
   //
   // what we want is...
   // a class representing a rendering strategy
   // GLWorld contains a vector of rendering strategies that are in use
   //
   //    a rendering strategy -- a set of shaders,
   //                            a set of slots for texture-data
   //                            a set of slots for coordinate-data
   //    a rendering strategy 
   //          specifies the format and type of position and texture coordinates expected
   //
   //    to be rendered, a rendering-strategy
   //          needs to be bound to actual texture data
   //          needs to be bound to actual coordinate data
   //
   // what we've got is this one "static" strategy:
   
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


   // -------------------------------------------------------------------
   // TEXTURE
   // -------------------------------------------------------------------
   
   // what we want is...
   // a class representing a Texture
   // GLWorld contains a vector of Textures that are in use
   //
   // what we've got is...
   // a SINGLE method that loads a Texture from a hardcoded resource name
   
   void setupTexture(GL3 gl) {
      System.out.format("SETUP-texture called\n");
      
      Image myTexture = Raster.imageFromResource("teapot.png");
      System.out.format("SETUP-texture loaded texture [%d x %d] texture\n", myTexture.width,myTexture.height);      
      // myTexture.fillRect(0,  0, 10, 128, 0x00ff0000);
      // myTexture.fillRect(10, 0, 10, 128, 0x0000ff00);
      // myTexture.fillRect(20, 0, 10, 128, 0x000000ff);
      
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
   
   
   // -------------------------------------------------------------------
   // GEOMETRY
   // -------------------------------------------------------------------

   // what we want is...
   // a class representing a TexturedMesh
   // GLWorld contains a vector of TexturedMesh that are in use
   //
   //    a TexturedMesh -- ref to a rendering strategy to use
   //                      ref(s) to texture(s) to use
   //                      an actual Mesh of 3d spatial-coordinates and 2d texture-coordinates
   //
   // a class representing a TexturedMeshInstance
   // GLWorld contains a vector of TexturedMeshInstances that are in use
   //
   //    a TexturedMeshInstance -- ref to a TexturedMesh
   //                              ref to a ViewMatrix
   //
   // what we've got is...
   // a SINGLE "Model" and a vector of Vector3f's to render them at:
    

   private Model2 m;
   private ArrayList<Vector3f> modelTranslations;
   
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
         
         // okay critical lat is aTan(0.5)
         float critW = (float) Math.atan(0.5);
         // 
         float angW = (float) Math.acos(aW);
         
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
   
   public void updateTranslations(float phase) {
      modelTranslations = new ArrayList<Vector3f>();
      modelTranslations.add(Vector3f.ORIGIN);
      
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         modelTranslations.add(Vector3f.X.times(3.0f + (float) Math.cos(phase + angle)).rotated(Vector3f.Y, angle));
         modelTranslations.add(Vector3f.X.times(6).rotated(Vector3f.Y, angle));
      }
   }
   
   private void initModel() {
       m = new Model2();
       
       if (cube) {
          Vector3f center = Vector3f.Z;
          Vector3f dx     = Vector3f.X;
          Vector3f dy     = Vector3f.Y;
   
          float ninety = (float) (Math.PI/2);
          
          for (int i = 0; i < 4; ++i) {
             float angle = i * ninety;
              m.addSquare(center.rotated(Vector3f.Y,angle),
                          dx.rotated(Vector3f.Y,angle),
                          dy.rotated(Vector3f.Y,angle), 0.0f);
          }
          
          m.addSquare(center.rotated(Vector3f.X,ninety),
                      dx.rotated(Vector3f.X,ninety),
                      dy.rotated(Vector3f.X,ninety), 0.0f);
         
          m.addSquare(center.rotated(Vector3f.X,-ninety),
                      dx.rotated(Vector3f.X,-ninety),
                      dy.rotated(Vector3f.X,-ninety), 1.0f);
          
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
          float tLat = (float) Math.PI/4; //(float) Math.PI/6; //(float) Math.atan(0.5);//(float) Math.atan(0.5);
          float bLat = -tLat;
          float sLat = -nLat;
          
          float nLatW = (float) Math.cos(nLat) + .000001f;
          float tLatW = (float) Math.cos(tLat) + .000001f;
          float bLatW = (float) Math.cos(bLat) + .000001f;
          float sLatW = (float) Math.cos(sLat) + .000001f;

          
          t0 = //new Vector3f((float)(2.0/Math.sqrt(5)), (float)(1.0/Math.sqrt(5)), 0f);
          t0 = new Vector3f(1,0,0).rotated(Vector3f.Z, tLat);
          
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
          

          float critW = tLat;
          for (Mesh.Vertex<Vector3f,TexInfo> v : m.mesh.vertices) {
             Vector3f pos = v.getData();
             float y = pos.y;
             float lat = (float) Math.asin(y);
             
             Vector2f xz2f = ((y<.9999) && (y>-.9999)) ? 
                   new Vector2f(pos.x, pos.z).normalized().times((float)Math.cos(lat)) : Vector2f.ORIGIN;
                   
             Vector3f pA = new Vector3f(xz2f.x, pos.y, xz2f.y);
             Vector3f pB = pos.normalized();
                   
             float fragB = 0.0f;
             if (lat > critW + .0001) { 
                fragB = (float) ((lat-critW)/((Math.PI/2)-critW));
                //fragB = 1.0f;
             }
             if (lat <= -critW - .0001) { 
                fragB = (float) ((-lat-critW)/((Math.PI/2)-critW));
                //fragB = 1.0f;
             }
             fragB = (float) Math.sqrt(Math.sqrt(fragB));
             
             Vector3f pC = pA.interpolated(pB, fragB).normalized();
             v.setData(pC);
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

      Model2.Arrays ma = m.getArrays();
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

   
   // ===================
   // A "GLWrangler" class would take a GL3
   // ===================
   //
   //    "init" (GL3)   -- bind all the RenderingStrategy / Texture / TexturedMeshs 
   //                               instances to GL-vertex-array-objects
   //                               save a map from Textures and TexturedMeshs to the GL-vertex-array-object IDs
   //
   //    "paint" (GL3)  -- set camera-ball perspective matrix
   //                      for each TexturedMeshInstance
   //                            set the appropriate GL-vertex-array-object IDs
   //                            set the appropriate view matrix
   //                            render
   //

   float projMatrixArr[] = new float[16];
   float viewMatrixArr[] = new float[16];
   
   private void copyMatrix4f(float[] a, Matrix4f m) {
      a[0]=m.xx; a[4]=m.xy; a[8] =m.xz; a[12]=m.xw;
      a[1]=m.yx; a[5]=m.yy; a[9] =m.yz; a[13]=m.yw;
      a[2]=m.zx; a[6]=m.zy; a[10]=m.zz; a[14]=m.zw;
      a[3]=m.wx; a[7]=m.wy; a[11]=m.wz; a[15]=m.ww;
   }
   
   public void render(GL3 gl) {
      int width = camera.getWidth();
      int height = camera.getHeight();
      
      gl.glViewport(0,0,width, height);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      
//      gl.glViewport(0,0,width/2, height/2);
//      gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
//      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      
//      gl.glViewport(width/2,height/2,width/2, height/2);
//      gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
//      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      
      // Transparancy:
      // If these are set, then the shader will honor alpha values set in the "outColor"
      // and blend them with what's already there, but that only works if triangles are
      // drawn from furthest-to-closest!:
      //
      // gl.glEnable(GL.GL_BLEND);
      // gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
      
      // must be called after glUseProgram:
      
      // set the view and the projection matrix 
      
      Matrix4f projMatrix = camera.getCameraToClipSpace();
      Matrix4f baseView = camera.getWorldToCameraSpace();
      
      copyMatrix4f(projMatrixArr, projMatrix);
      gl.glUniformMatrix4fv(projMatrixLoc, 1, false, projMatrixArr, 0);      
      gl.glBindVertexArray(this.triangleVAO);

      for (Vector3f translation : modelTranslations) {
         Matrix4f modelTransform = new Matrix4f(
               1.0f, 0.0f, 0.0f, translation.x,
               0.0f, 1.0f, 0.0f, translation.y,
               0.0f, 0.0f, 1.0f, translation.z,
               0.0f, 0.0f, 0.0f, 1.0f);

         Matrix4f viewMatrix = Matrix4f.product(baseView, modelTransform);
         copyMatrix4f(viewMatrixArr, viewMatrix);
         gl.glUniformMatrix4fv(viewMatrixLoc, 1, false, viewMatrixArr, 0);   
         gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3*m.numTriangles());
      }

      // Check out error
      checkError(gl, "render");
   }
   
   private void checkError(GL3 gl, String point) {
      int error = gl.glGetError();
      if (error!=0){
         System.out.format("At [%s] ERROR on render: %d\n", point, error);}
   }
}

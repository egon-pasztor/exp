package com.generic.base.linux;

import com.generic.base.Data;
import com.generic.base.Rendering;
import com.generic.base.Image;
import com.generic.base.Platform;
import com.generic.base.Algebra.*;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

public class Jogl {
   
   // ======================================================================
   // (Implementation of Platform.Widget.Renderer3D)
   // ======================================================================
   
   private static class Renderer3D implements
            GLEventListener,                                         /* jogamp   */
            MouseListener, MouseMotionListener, MouseWheelListener,  /* awt      */
            Platform.Widget.Renderer3D, Rendering.Listener {         /* platform */

      // Jogl.Renderer3D is a wrapper around this
      // com.jogamp.opengl.awt.GLCanvas,
      //   (extending java.awt.Canvas, extending java.awt.Component):
      private final GLCanvas glCanvas;
      
      // 
      private final Object lock = new Object();
      private boolean gotInitialSize = false;
      
      public Renderer3D () {
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

         // Currently this object constructs its own top-level frame,
         // but eventually this Widget will have to support...
         // ... being "added" to a Platform.Widget.Container.
         //
         // I guess we'll have ... a package com.generic.platform.linux,
         //   where linux.Window will be a class that wraps .. a java.awt.Container?
         //   (currently, this is the code in GLSample.GUI)
         //
         //   when someone calls "addChild" on linux.Window, it will
         //   have to examine the Platform.Widget it's given...
         //   if it's a Jogl.Window instance, it'll need to access "glCanvas"
         //   to call ... java.awt.Container.add 
         //
         //
         // For Android, we'll have .. a package com.generic.platform.droid,
         //   where droid.Window will wrap ... android.view.View?
         //   (see code in EngineAtivity.java)
         //         
         //   when someone calls "addChild" on droid.Window, it will
         //   have to examine the Platform.Widget it's given...
         //   if it's the android-equivalent of this class, it'll be wrapping
         //   an android.opengl.GLSurfaceView, which will be added to the View,,
         //
         // ...
         // 
         // 
         
         final Frame frame = new Frame() {
            private static final long serialVersionUID = 1L;
            public void update(Graphics g) { paint(g); }
         };
         frame.pack();
         frame.setBackground(java.awt.Color.CYAN);
         frame.setTitle("Window-Name");
         frame.addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent evt) {
              System.exit(0); 
            }
         });
         frame.setVisible(true);
         frame.setSize(new Dimension(400,300));
         frame.add(glCanvas); 
         
         // We've created the root window, pause until we get a size.
         // (Not sure how we should be doing this... I guess at any given moment
         // a window mihgt not have a size, and owners just need to check for that?)
         System.out.println("RootWindow Started ... waiting for first size");
         synchronized(lock) {
            while (!gotInitialSize) {
               try {
                  lock.wait();
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
            System.out.format(
               "RootWindow ... my size is (%d x %d)", size.width, size.height);
         }
      }
      
      // ==========================================================
      // Translating a Graphics3D object into GL3 calls
      // ==========================================================
      
      // This Jogl.Window object is displaying the contents of one Graphics3D.
      private Rendering graphics3D;

      public void setRendering (Rendering graphics3D) {
         if (this.graphics3D != graphics3D) {
            if (this.graphics3D != null) {
               
               // Disconnect all Graphics3D objects:
               graphics3D.listeners.remove(this);
               
               for (GLShader shader : shaders.values()) {
                  shader.needsDestruction = true;
               }
               for (GLSampler sampler : samplers.values()) {
                  sampler.needsDestruction = true;
               }
               for (GLVertexBuffer buffer : vertexBuffers.values()) {
                  buffer.needsDestruction = true;
               }
            }
            
            this.graphics3D = graphics3D;
            
            if (this.graphics3D != null) {

               // Connect all Graphics3D objects:
               for (Integer vertexBufferId : graphics3D.vertexBuffers.keySet()) {
                  vertexBufferAdded(vertexBufferId); 
               }
               for (Integer samplerId : graphics3D.samplers.keySet()) {
                  samplerAdded(samplerId); 
               }
               for (Integer shaderId : graphics3D.shaders.keySet()) {
                  shaderAdded(shaderId); 
               }         
               graphics3D.listeners.add(this);
            }
         }
      }
      
      // ------------------------------------------
      // Implementing Graphics3D.Listener
      // ------------------------------------------

      public void vertexBufferAdded(int vertexBufferId) {
         GLVertexBuffer buffer = vertexBuffers.get(vertexBufferId);
         if (buffer == null) {
            buffer = new GLVertexBuffer(vertexBufferId);
            vertexBuffers.put(vertexBufferId,  buffer);
         }
         buffer.needsUpdate      = true;
         buffer.needsDestruction = false;
      }
      public void vertexBufferChanged(int vertexBufferId) {
         GLVertexBuffer buffer = vertexBuffers.get(vertexBufferId);
         if (buffer != null) {
            buffer.needsUpdate = true;
         }
      }
      public void vertexBufferRemoved(int vertexBufferId) {
         GLVertexBuffer buffer = vertexBuffers.get(vertexBufferId);
         if (buffer != null) {
            buffer.needsDestruction = true;
         }
      }
      
      public void samplerAdded(int samplerId) {
         GLSampler sampler = samplers.get(samplerId);
         if (sampler == null) {
            sampler = new GLSampler(samplerId);
            samplers.put(samplerId, sampler);
         }
         sampler.needsDestruction = false;
      }
      public void samplerChanged(int samplerId) {
         GLSampler sampler = samplers.get(samplerId);
         if (sampler != null) {
            sampler.needsUpdate = true;
         }         
      }
      public void samplerRemoved(int samplerId) {
         GLSampler sampler = samplers.get(samplerId);
         if (sampler != null) {
            sampler.needsDestruction = true;
         }         
      }
      
      public void shaderAdded(int shaderId) {
         GLShader shader = shaders.get(shaderId);
         if (shader == null) {
            shader = new GLShader(shaderId);
            shaders.put(shaderId,  shader);
         }
         shader.needsDestruction = false;
      }
      public void shaderRemoved(int shaderId) {
         GLShader shader = shaders.get(shaderId);
         if (shader != null) {
            shader.needsDestruction = true;
         }
      }
      
      public void commandsChanged() {
         glCanvas.display();
      }
      
      // ------------------------------------------------------------
      // VertexBuffer objects
      // ------------------------------------------------------------      
      private final HashMap<Integer, GLVertexBuffer> vertexBuffers = new HashMap<Integer, GLVertexBuffer>();

      private class GLVertexBuffer {
         public final int key;
         public GLVertexBuffer(int key) { 
            this.key = key;
            needsUpdate = true;
            needsDestruction = false;
            
            nativeBuffer = null;
            glBufferID = null;
         }
         public boolean needsUpdate;
         public boolean needsDestruction;
         
         public Buffer nativeBuffer;
         public Integer glBufferID;
         public int glBufferLength;

         public void update(GL3 gl) {
            if (needsDestruction) {
               needsDestruction = false;
               
               if (glBufferID != null) {
                  deleteBufferId(gl, glBufferID);
                  glBufferID = null;
                  glBufferLength = 0;
               }
               if (nativeBuffer != null) {
                  nativeBuffer = null;
               }
               
            } else if (needsUpdate) {
               needsUpdate = false;

               int glBufferLengthNeeded = 0;
               final int bytesPerFloat = 4;
               
               System.out.format("Buffer [%d] changed, updating GL", key);
               
               // The "Data.Array" in Graphics3D has to be FIRST converted
               // into a native Buffer, either IntBuffer or FloatBuffer
               Data.Array array = graphics3D.vertexBuffers.get(key);
               if (array instanceof Data.Array.Floats) {
                  // We want "nativeBuffer" to be a FloatBuffer.
                  // How many floats do we want in our floatBuffer?
                  float[] floats = ((Data.Array.Floats) array).array();
                  int numFloatsNeeded = array.numElements() * array.type.primitivesPerElement;
                  int numBytesNeeded = numFloatsNeeded * bytesPerFloat;
                  glBufferLengthNeeded = numBytesNeeded;
                  
                  // Is "nativeBuffer" already a FloatBuffer with the required number
                  // of elements?  If not, make it so:
                  if (!(nativeBuffer instanceof FloatBuffer) || (nativeBuffer.capacity() != numBytesNeeded)) {
                     ByteBuffer byteBuffer = ByteBuffer.allocateDirect(numBytesNeeded);
                     byteBuffer.order(ByteOrder.nativeOrder());
                     nativeBuffer = byteBuffer.asFloatBuffer();
                     System.out.format("Needed vertex buffer of %d floats -- umm, for %d elements at %d primitivesPerElement,"
                           + " got buffer of %d floats.   But my array len is %d\n",
                           numFloatsNeeded, array.numElements(), array.type.primitivesPerElement, nativeBuffer.capacity(),
                           floats.length);
                  }
                  // Copy the array contents into the native buffer
                  FloatBuffer floatBuffer = (FloatBuffer) nativeBuffer;
                  floatBuffer.position(0);
                  floatBuffer.put(floats, 0, numFloatsNeeded);
                  floatBuffer.position(0);
 
               } else if (array instanceof Data.Array.Integers) {
                  // TODO: Do we need integer vertexbuffers for "faceColor"?
                  // 
               }
               
               // Now that we've updated (or created) the NativeBuffer,
               // we need to update (or create) the GL_ARRAY_BUFFER on the GPU:
               if ((glBufferID == null) || (glBufferLength != glBufferLengthNeeded)) {
                  
                  // Create the buffer-id if we haven't created it yet
                  if (glBufferID == null) {
                     glBufferID = createBufferId(gl);
                  }
                  
                  // Bind and reallocate new storage
                  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBufferID);
                  gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        glBufferLengthNeeded, nativeBuffer, GL.GL_STATIC_DRAW);                  
                  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                  
                  glBufferLength = glBufferLengthNeeded;
                  
               } else {
                  
                  // Apparently the buffer-id already exists and has the correct size
                  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBufferID);
                  gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, 
                        glBufferLengthNeeded, nativeBuffer);
                  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
               }
            }
         }

         private void deleteBufferId(GL3 gl, int id) {
            int[] idArray = new int[1];
            idArray[0] = id;
            gl.glDeleteBuffers( 1, idArray, 0);
         }
         private int createBufferId(GL3 gl) {
            int[] idArray = new int[1];
            gl.glGenBuffers( 1, idArray, 0);
            return idArray[0];
         }
      }

      private void updateBuffers(GL3 gl) {
         for (GLVertexBuffer buffer : vertexBuffers.values()) {
            buffer.update(gl);            
         }
      }
      
      // ------------------------------------------------------------
      // Sampler objects
      // ------------------------------------------------------------      
      private final HashMap<Integer, GLSampler> samplers = new HashMap<Integer, GLSampler>();

      private class GLSampler {
         public final int key;
         public GLSampler(int key) { 
            this.key = key;
            needsUpdate = true;
            needsDestruction = false;
         }
         public boolean needsUpdate;
         public boolean needsDestruction;
         
         public Integer glTextureID;  // ???

         public void update(GL3 gl) {
            if (needsDestruction) {
               needsDestruction = false;
            } else if (needsUpdate) {
               needsUpdate = false;
            }
         }
      }
      
      private void updateSamplers(GL3 gl) {
         for (GLSampler sampler : samplers.values()) {
            sampler.update(gl);
         }
      }      
      
      // ------------------------------------------------------------
      // Shader objects
      // ------------------------------------------------------------      
      private final HashMap<Integer, GLShader> shaders = new HashMap<Integer, GLShader>();

      private class GLShader {
         public final int key;
         public GLShader(int key) { 
            this.key = key;
            needsUpdate = true;
            needsDestruction = false;
            
            programID = null;
         }
         public boolean needsUpdate;
         public boolean needsDestruction;
         
         public Integer programID;
         
         // ------------------------------------
         // what program variables do we need?
         // we'll start with these 5:
         // ------------------------------------         
         int modelToView_ProgramLocation = -1;
         int viewToClip_ProgramLocation = -1;
         int positions_ProgramLocation = -1;
         int normals_ProgramLocation = -1;
         int baryCoords_ProgramLocation = -1;
         // ------------------------------------
         

         public void update(GL3 gl) {
            if (needsDestruction) {
               System.out.format("TODD -- Shader Destruction??\n");
               needsDestruction = false;
               
            } else if (needsUpdate) {
               needsUpdate = false;
               
               int v = newGLShaderObject(gl, Data.loadPackageResource(Jogl.class,
                         "jogl_flat_vertex.shader"), GL3.GL_VERTEX_SHADER);
               int f = newGLShaderObject(gl, Data.loadPackageResource(Jogl.class,
                         "jogl_flat_fragment.shader"), GL3.GL_FRAGMENT_SHADER);
               
               System.out.format("Vertex Shader Info Log: [%s]\n", getShaderInfoLog(gl, v));
               System.out.format("Fragent Shader Info Log: [%s]\n", getShaderInfoLog(gl, f));

               // Complete the "shader program"
               programID = gl.glCreateProgram();
               
               System.out.format("Created Program ID: %d\n", programID);
               gl.glAttachShader(programID, v);
               gl.glAttachShader(programID, f);
               gl.glLinkProgram(programID);
               
               gl.glUseProgram(programID);
               gl.glBindFragDataLocation(programID, 0, "outColor");
               System.out.format("Program Info Log: [%s]\n", getProgramInfoLog(gl, programID));
               
               // Well... in order to "render", we're going to need to bind values to "program locations",
               // which are the input parameters of the shader code.
               //
               // When we start supporting different "kinds" of shaders, we'll need to extract
               // a different set of "program locations" depending on what kind of shader it is.
               // But for now we just start with these 5:
               // 
               modelToView_ProgramLocation = gl.glGetUniformLocation(programID, "viewMatrix");
               viewToClip_ProgramLocation  = gl.glGetUniformLocation(programID, "projMatrix");
               //
               positions_ProgramLocation  = gl.glGetAttribLocation(programID, "vertexPosition");
               normals_ProgramLocation    = gl.glGetAttribLocation(programID, "vertexNormal");
               baryCoords_ProgramLocation = gl.glGetAttribLocation(programID, "vertexBaryCoords");
               
               System.out.format("Shader [%d] created\n", key);
            }
         }

         private int newGLShaderObject (GL3 gl, String code, int shaderType) {
            // create the shader id
            int id = gl.glCreateShader(shaderType);
            //  link the id and the source
            gl.glShaderSource(id, 1, new String[] { code }, null);
            //compile the shader
            gl.glCompileShader(id);            
            return id;
         }
         
         // Retrieves the info log for the shader
         private String getShaderInfoLog(GL3 gl, int obj) {
            // Otherwise, we'll get the GL info log
            final int logLen = getShaderParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
            if (logLen <= 0) return "";

            // Get the log
            final int[] retLength = new int[1];
            final byte[] bytes = new byte[logLen + 1];
            gl.glGetShaderInfoLog(obj, logLen, retLength, 0, bytes, 0);
            final String logMessage = new String(bytes);

            return String.format("ShaderLog: %s", logMessage);
         }
         // Retrieves the info log for the program
         private String getProgramInfoLog(GL3 gl, int obj) {
            // get the GL info log
            final int logLen = getProgramParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
            if (logLen <= 0) return "";

            // Get the log
            final int[] retLength = new int[1];
            final byte[] bytes = new byte[logLen + 1];
            gl.glGetProgramInfoLog(obj, logLen, retLength, 0, bytes, 0);
            final String logMessage = new String(bytes);

            return logMessage;
         }
         // Get a shader parameter value. See 'glGetShaderiv'
         private int getShaderParameter(GL3 gl, int obj, int paramName) {
            final int params[] = new int[1];
            gl.glGetShaderiv(obj, paramName, params, 0);
            return params[0];
         }
         // Gets a program parameter value
         private int getProgramParameter(GL3 gl, int obj, int paramName) {
            final int params[] = new int[1];
            gl.glGetProgramiv(obj, paramName, params, 0);
            return params[0];
         }

      }
      
      private void updateShaders(GL3 gl) {
         for (GLShader shader : shaders.values()) {
            shader.update(gl);
         }
      }      

      // ------------------------------------------------------------
      // ------------------------------------------------------------

      private void renderGL (GL3 gl) {         
         if (graphics3D == null) {
            System.out.format("In renderGL .. but graphics3D == null\n");
            return;
         }
         if (size == null) {
            System.out.format("In renderGL .. but size == null\n");
            return;
         }
         int width = size.width;
         int height = size.height;
         gl.glViewport(0,0,width, height);
         gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
         
         // -------------------------------------------
         updateBuffers(gl);
         updateSamplers(gl);
         updateShaders(gl);
         // -------------------------------------------
         
         Matrix4x4 modelToView = null;
         Matrix4x4 viewToClip = null;
         int positionsBufferId = -1;
         int normalsBufferId = -1;
         int baryBufferId = -1;
                  
         for (Rendering.Shader.Command command : graphics3D.commands) {
            
            if (command instanceof Rendering.Shader.Variable.Binding) {
               if (command instanceof Rendering.Shader.Variable.Matrix4x4.Binding) {
                  Rendering.Shader.Variable.Matrix4x4.Binding b = (Rendering.Shader.Variable.Matrix4x4.Binding) command;
                  if (b.variable == Rendering.Shader.MODEL_TO_VIEW) {
                     modelToView = b.value;
                  }
                  if (b.variable == Rendering.Shader.VIEW_TO_CLIP) {
                     viewToClip = b.value;
                  }
               }
               if (command instanceof Rendering.Shader.Variable.VertexBuffer.Binding) {
                  Rendering.Shader.Variable.VertexBuffer.Binding b = (Rendering.Shader.Variable.VertexBuffer.Binding) command;
                  if (b.variable == Rendering.Shader.POSITIONS) {
                     positionsBufferId = b.vertexBuffer;
                  }
                  if (b.variable == Rendering.Shader.NORMALS) {
                     normalsBufferId = b.vertexBuffer;
                  }
                  if (b.variable == Rendering.Shader.BARYCOORDS) {
                     baryBufferId = b.vertexBuffer;
                  }
               }
            }

            if (command instanceof Rendering.Shader.Command.Execute) {
               Rendering.Shader.Command.Execute b = (Rendering.Shader.Command.Execute) command;
               
               int numTriangles = b.numTriangles;
               GLShader shader = shaders.get(b.shader);               
               GLVertexBuffer positionsBuffer = vertexBuffers.get(positionsBufferId);
               GLVertexBuffer normalsBuffer = vertexBuffers.get(normalsBufferId);
               GLVertexBuffer baryBuffer = vertexBuffers.get(baryBufferId);
               
               // System.out.format("Ready to try to invoke shader: %d,%d,%d buffers .. %d shader .. %d triangles\n",
               //       positionsBuffer.glBufferID,
               //       normalsBuffer.glBufferID,
               //       baryBuffer.glBufferID,
               //       shader.programID,
               //       numTriangles);
               
               // Tell GL to use the shader program for this instance...
               gl.glUseProgram(shader.programID);  
               
               // Bind modelToView
               { float arr[] = new float[16];
                 modelToView.copyToFloatArray(arr);
                 gl.glUniformMatrix4fv(shader.modelToView_ProgramLocation, 1, false, arr, 0);     
               }
               // Bind viewToClip
               { float arr[] = new float[16];
                 viewToClip.copyToFloatArray(arr);
                 gl.glUniformMatrix4fv(shader.viewToClip_ProgramLocation, 1, false, arr, 0);     
               }
               
               // Bind positions vertex-buffer
               { gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionsBuffer.glBufferID);               
                 gl.glEnableVertexAttribArray(shader.positions_ProgramLocation);
                 gl.glVertexAttribPointer(shader.positions_ProgramLocation, 
                     3,           // num elements per vertex, eg, per-vertex vec3's or per-vertex vec4's
                     GL.GL_FLOAT, // apparently GL.GL_UNSIGNED_INT doesn't work??
                     false, 0, 0);
               }               
               // Bind normals vertex-buffer
               { gl.glBindBuffer(GL.GL_ARRAY_BUFFER, normalsBuffer.glBufferID);               
                 gl.glEnableVertexAttribArray(shader.normals_ProgramLocation);
                 gl.glVertexAttribPointer(shader.normals_ProgramLocation, 
                     3,           // num elements per vertex, eg, per-vertex vec3's or per-vertex vec4's
                     GL.GL_FLOAT, // apparently GL.GL_UNSIGNED_INT doesn't work??
                     false, 0, 0);
               }
               // Bind bary vertex-buffer
               { gl.glBindBuffer(GL.GL_ARRAY_BUFFER, baryBuffer.glBufferID);               
                 gl.glEnableVertexAttribArray(shader.baryCoords_ProgramLocation);
                 gl.glVertexAttribPointer(shader.baryCoords_ProgramLocation, 
                     3,           // num elements per vertex, eg, per-vertex vec3's or per-vertex vec4's
                     GL.GL_FLOAT, // apparently GL.GL_UNSIGNED_INT doesn't work??
                     false, 0, 0);
               }
               
               gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3 * numTriangles);
            }
         }
      }
      
      
      // ========================================================
      // Implementing Platform.Widget
      // ========================================================
      
      public Platform.Widget parent() {
         return null;
      }
      public boolean isConnected() {
         return true;
      }
      
      private Image.Size size = null;
      public Image.Size size() {
         return size;
      }
      
      private Platform.Widget.ResizeListener resizeListener = null;
      private Platform.Widget.MouseListener mouseListener = null;
      
      public void setResizeListener(Platform.Widget.ResizeListener resizeListener) {
         this.resizeListener = resizeListener;
      }
      public void setMouseListener(Platform.Widget.MouseListener mouseListener) {
         this.mouseListener = mouseListener;
      }
      
      // -----------------------------------------------------------
      // Implementing MouseListener & MouseMotionListener
      // -----------------------------------------------------------
      public void mouseWheelMoved(MouseWheelEvent e) {
      }
      public void mouseMoved(MouseEvent e) {
         if (mouseListener != null) {
            mouseListener.mouseHover(Image.Position.of(e.getX(), e.getY()));
         }
      }

      public void mousePressed(MouseEvent e) {
         if (mouseListener != null) {
            mouseListener.mouseDown(Image.Position.of(e.getX(), e.getY()), e.isControlDown(), e.isShiftDown());
         }
      }
      public void mouseDragged(MouseEvent e) {
         if (mouseListener != null) {
            mouseListener.mouseDrag(Image.Position.of(e.getX(), e.getY()));
         }
      }
      public void mouseReleased(MouseEvent e) {
         if (mouseListener != null) {
            mouseListener.mouseUp();
         }
      }
      public void mouseClicked(MouseEvent e) {
      }
      public void mouseEntered(MouseEvent e) {
      }
      public void mouseExited(MouseEvent e) {
      }
      
      // -----------------------------------------------------------
      // Implementing GLEventListener
      // -----------------------------------------------------------
      public void init(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.init called\n");
         // Not sure if we have to do anything here
         
         GL3 gl = drawable.getGL().getGL3();
         gl.glEnable(GL.GL_DEPTH_TEST);
         gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
      }
      public void display(GLAutoDrawable drawable) {
         // utimately, this is the function that does the OpenGL work...
       
         GL3 gl = drawable.getGL().getGL3();
         renderGL(gl);

      }
      public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {      
         System.out.format("JOGLWin.reshape(%d,%d,%d,%d) called\n",x,y,width,height);
         Image.Size newSize = Image.Size.of(width, height);
         if ((size == null) || !size.equals(newSize)) {
            size = newSize;
            if (resizeListener != null) {
               resizeListener.resized();
            }
         }
         synchronized(lock) {
            if (!gotInitialSize) {
               gotInitialSize = true;
               lock.notify();
            }
         }
      }
      public void dispose(GLAutoDrawable drawable) {
         System.out.format("JOGLWin.dispose() called\n");
      }
   }
   
   // ======================================================================
   // Platform
   // ======================================================================
   
   private static class JoglPlatform implements Platform {
      
      // -------------------------------
      // Log and LoadResource
      // -------------------------------
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
      
      // Currently Jogl.Window creates a "top-level" window that is a Jogl.Window3D.
      //
      // We believe in the final design we'll still create a "top-level" window,
      // but it will implement Platform.Widget.Container instead.
      // The owner will have to call widgetFactory().newRenderer3D()
      // to construct a Platform.Widget.Renderer3D instance, and he'll call 
      // "addChild" to ADD it to the "top-level" Platform.Widget.Container
      //
      private Jogl.Renderer3D window = null;
      
      public JoglPlatform() {
      }

      public Widget.Renderer3D root3D() {
         if (window == null) {
            window = new Jogl.Renderer3D();
         }
         return window;  
      }
      
      public Widget.Container rootWidget() { return null; }
      public Widget.Factory widgetFactory() { return null; }
      
      
   }   
   
   // ======================================================================
   
   public static Platform platform = null;
   public static Platform platform() {
      if (platform == null) {
         platform = new JoglPlatform(); 
      }
      return platform;
   }
}

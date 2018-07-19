package com.generic.base;

import java.util.ArrayList;
import java.util.HashMap;

public class Graphics3D {
   
   // ------------------------------------------
   // How about this then?
   // ------------------------------------------
   
   public final Object lock = new Object();
   
   
   public final HashMap<Integer, Data.Array> vertexBuffers = new HashMap<Integer, Data.Array>();
   public final HashMap<Integer, Image> samplers = new HashMap<Integer, Image>();
   public final HashMap<Integer, Shader> shaders = new HashMap<Integer, Shader.>();

   public void setVertexBuffer(int id, Data.Array data) {
      vertexBuffers 
   }
   public VertexBuffer vertexBuffer(int id) {
      return vertexBuffers.get(id);
   }
   public Iterable<VertexBuffer> vertexBuffers() {
      return vertexBuffers.values();
   }
   
   
   // ------------------------------------------
   // 
   // ------------------------------------------
   /*
   
   
   public final Object lock;   
   public final Data.Listener.Set listeners;
   
   public Graphics3D() {
      lock = new Object();
      listeners = new Data.Listener.Set();
      nextVertexBufferId = 1;
   }
   public void clear() {
      vertexBuffers.clear();
   }
   
   // ------------------------------------------
   // VertexBuffer
   // ------------------------------------------
   private final HashMap<Integer, VertexBuffer> vertexBuffers = new HashMap<Integer, VertexBuffer>();
   private int nextVertexBufferId;
      
   public static class VertexBuffer {
      public final Graphics3D owner;
      public final int id;
      public final Data.Array data;
      public final Data.Listener.Set listeners;

      private VertexBuffer(Graphics3D owner, int id, Data.Array.Type type) {
         this.owner = owner;
         this.id = id;
         this.data = Data.Array.create(type);
         this.listeners = new Data.Listener.Set();
         
         owner.vertexBuffers.put(id, this);
      }
      public void destroy() {
         if (owner.vertexBuffers.get(id) == this) {
            owner.vertexBuffers.remove(id);
         }
      }
   }
   
   public VertexBuffer newVertexBuffer(Data.Array.Type type) {
      return new VertexBuffer(this, nextVertexBufferId++, type);
   }
   public VertexBuffer vertexBuffer(int id) {
      return vertexBuffers.get(id);
   }
   public Iterable<VertexBuffer> vertexBuffers() {
      return vertexBuffers.values();
   }
   
   // this asks that
   //    USERS of this class who want to change the data held in VertexBuffer
   //       first lock the "lock" object of this Graphics3D
   //         then change the floats or ints in data.array()
   //         then call the "listeners.onChange()" method
   //       finally unlock the "lock" object
   //
   // we could have a HashMap<Integer,Data.Array> instead, and no VertexBuffer class,
   //    but then there would be no listener.onChange to call after changing an array.
   //    But this wouldn't be a problem if *this* class's listeners took an "change-description"
   //    object allowing *this* class's listeners to be told about a change to one vertexbuffer.
   //
   //    
   
   // ------------------------------------------
   // Samplers ... will be like VertexBuffers..
   // ------------------------------------------

   // TODO Samplers

   
   // ------------------------------------------
   // Shaders ... will be like VertexBuffers..
   // ------------------------------------------
   private final HashMap<Integer, Shader> shaders = new HashMap<Integer, Shader>();
   private int nextShaderId;
   */
   
   public interface Shader {

      public static final class Smooth implements Shader {
         public Smooth() {}
      }
      public static final class FlatBordered implements Shader {
         public final float borderThickness;
         public FlatBordered(float borderThickness) {
            this.borderThickness = borderThickness;
         }
      }
      
      // -------------------------------------------------------------------
      // Execution
      // -------------------------------------------------------------------      
      public interface Command {}
      
      public static final class Activate implements Shader.Command {
         public final int shader;
         public Activate (int shader) {
            this.shader = shader;
         }
      }
      public static final class Execute implements Shader.Command {
         public final int numTriangles;
         public Execute (int numTriangles) {
            this.numTriangles = numTriangles;
         }
      }
      
      // -------------------------------------------------------------------
      // Variable
      // -------------------------------------------------------------------
      public static abstract class Variable {
         public final String name;
         private Variable (String name) {
            this.name = name;
         }
         public interface Binding extends Shader.Command {}
         
         // - - - - - - - 
         // Integer
         // - - - - - - - 
         public static final class Integer extends Variable {
            public Integer(String name) {
               super(name);
            }
            
            public static final class Binding implements Variable.Binding {
               public final Shader.Variable.Integer variable;
               public final int value;
               public Binding (Shader.Variable.Integer variable, int value) {
                  this.variable = variable;
                  this.value = value;
               }
            }
         }
         // - - - - - - - 
         // Vector3
         // - - - - - - - 
         public static final class Vector3 extends Variable {
            public Vector3(String name) {
               super(name);
            }
            
            public static final class Binding implements Variable.Binding {
               public final Shader.Variable.Vector3 variable;
               public final Algebra.Vector3 value;
               public Binding (Shader.Variable.Vector3 variable, Algebra.Vector3 value) {
                  this.variable = variable;
                  this.value = value;
               }
            }
         }
         // - - - - - - - 
         // Matrix4x4
         // - - - - - - - 
         public static final class Matrix4x4 extends Variable {
            public Matrix4x4(String name) {
               super(name);
            }
            
            public static final class Binding implements Variable.Binding {
               public final Shader.Variable.Matrix4x4 variable;
               public final Algebra.Matrix4x4 value;
               public Binding (Shader.Variable.Matrix4x4 variable, Algebra.Matrix4x4 value) {
                  this.variable = variable;
                  this.value = value;
               }
            }
         }
         // - - - - - - - 
         // VertexBuffer
         // - - - - - - - 
         public static final class VertexBuffer extends Variable {
            public final Data.Array.Type type;
            public VertexBuffer(String name, Data.Array.Type type) {
               super(name);
               this.type = type;
            }
            
            public static final class Binding implements Variable.Binding {
               public final Shader.Variable.VertexBuffer variable;
               public final int vertexBuffer;
               public Binding (Shader.Variable.VertexBuffer variable, int value) {
                  this.variable = variable;
                  this.value = value;
               }
            }            
         }
      }
      public static Variable.Matrix4x4 MODEL_TO_VIEW = new Variable.Matrix4x4("modelToView");
      public static Variable.Matrix4x4 VIEW_TO_CLIP  = new Variable.Matrix4x4("viewToClip");
      public static Variable.Vector3 FACE_COLOR      = new Variable.Vector3("faceColor");
      public static Variable.Vector3 BORDER_COLOR    = new Variable.Vector3("borderColor");
      public static Variable.VertexBuffer POSITIONS  = new Variable.VertexBuffer("positions", Data.Array.Type.FOUR_FLOATS);
      public static Variable.VertexBuffer NORMALS    = new Variable.VertexBuffer("normals", Data.Array.Type.THREE_FLOATS);
      public static Variable.VertexBuffer BARYCOORDS = new Variable.VertexBuffer("baryCoords", Data.Array.Type.THREE_FLOATS);
   }

   public Shader newShader(Shader.Parameters parameters) {
      return new Shader(this, nextShaderId++, parameters);
   }
   public Shader shader(int id) {
      return shaders.get(id);
   }
   public Iterable<Shader> shaders() {
      return shaders.values();
   }
   
   
   // ------------------------------------------
   // RenderCommands
   // ------------------------------------------
   private final ArrayList<Shader.Command> commands = new ArrayList<Shader.Command>();
   
   public void clearCommands() {
      commands.clear();
   }   
   public void addCommand(Shader.Command command) {
      commands.add(command);
   }
   public Iterable<Shader.Command> commands() {
      return commands;
   }
   
   */
   
   // -------------------------------------------
   // hmm.
   // 
   // Graphics3D contains "state".
   // The "Graphics3D" state contains
   //   (VertexBuffers, Samplers, Shaders ...
   //    and a LIST of RenderCommands where a single RenderCommand
   //    is either ("activate_shader", shader-id)
   //           or ("bind_variable", variable-id, value-to-bind)
   //           or "execute"
   //
   // Graphics3D also has a LOCK (users must lock it before using any other methods)
   //   and <listeners>.
   //
   // Question1:   Do the Listeners convey event-description-objects?
   //              EG:   If I add a VertexBuffer, does that notify the listeners of what VertexBuffer was added
   //                                                     or just that something changed?
   //
   //                    is it:
   //                              changeOccurred()
   //                       or
   //                              vertexBufferAdded   (int id);
   //                              vertexBufferRemoved (int id);
   //                       or
   //                              vertexBufferAdded   (VertexBuffer buffer);
   //                              vertexBufferRemoved (VertexBuffer buffer);
   //                       or
   //                              changeOccurred(Event e)
   //                              where "Event" is <either> (VERTEX_BUFFER_ADDED, id)
   //                                                   <or> (VERTEX_BUFFER_REMOVED, id)
   //
   // Question2:
   //              Internally, do we even need "vertexbuffer ids"?
   //              Why not just use object refs directly?
   //
   //                  Why not HashSet<VertexBuffer>
   //               instead of HashMap<Integer,VertexBuffer> ?
   //
   // what if we DONT use IDs:
   //
   //    then we have HashSet<VertexBuffer>
   //                   and the Binding uses actual references...
   //
   //
   // -----------------------------------------------
   //
   // Graphics2D contains "state".
   // The "Graphics2D" state contains
   //   (Sprites, Paths ... Painters ...
   //    and a LIST of RenderCommands where a single RenderCommand
   //    is either "activate_painter" -- providing a Painter ID
   //           or "bind_variable"    -- providing a painter-variable identifier, and a value of type appropriate to that variable
   //           or "execute"
   //
   
   
   
   
   
   /*
   public interface Renderer {
      public void render(Graphics3D gl);
   }
   
   void setViewport (int width, int height);
   void clear (Color color);
   
   // ==============================================================================
   // Resources
   // ==============================================================================
   public interface Resource {
      void destroy();
   }
   
   // ------------------------------------------
   // VertexBuffer
   // ------------------------------------------
   public interface VertexBuffer extends Resource {
      Data.Array.Type type();
      int numElements();
      void update(Data.Array data);
   }
   VertexBuffer newVertexBuffer (Data.Array data);
   
   // ------------------------------------------
   // Sampler
   // ------------------------------------------
   public interface Sampler extends Resource {
      Data.Array.Type type();
      int width();
      int height();
      void update(Image image);
   }
   Sampler newSampler (Image image);

   // ==============================================================================
   // Shaders
   // ==============================================================================   
   public interface Shader extends Resource {
      void shadeTriangles(int numTriangles);
   }
   
   // ---------------------------------------------------------------
   // Smooth Shader (no borders)
   // ---------------------------------------------------------------   
   public interface SmoothShader extends Shader {
      public void setModelToView (Matrix4x4 transformation);
      public void setViewToClip  (Matrix4x4 transformation);
      public void setFaceColor   (Color faceColor);
      
      public void setPositions (VertexBuffer buffer);
      public void setNormals   (VertexBuffer buffer);
   }
   SmoothShader newSmoothShader ();

   // ---------------------------------------------------------------
   // Flat Shader With Borders
   // ---------------------------------------------------------------   
   public interface FlatBorderedShader extends Shader {
      public void setModelToView (Matrix4x4 transformation);
      public void setViewToClip  (Matrix4x4 transformation);
      public void setFaceColor   (Color faceColor);
      public void setBorderColor (Color faceColor);
      
      public void setPositions  (VertexBuffer buffer);
      public void setNormals    (VertexBuffer buffer);
      public void setBaryCoords (VertexBuffer buffer);
   }
   FlatBorderedShader newFlatBorderedShader (float borderThickness);
   
*/
   
/*
   public abstract class Shader {
      
      public static class Parameter {
         public enum Type { Uniform, VertexBuffer, Sampler };
         
         public final String name;
         public final Type type;
         public final Data.Array.Type elements;
         public Parameter (String name, Type type, Data.Array.Type elements) {
            this.name = name;
            this.type = type;
            this.elements = elements;            
         }
         
         public static Parameter MODEL_TO_VIEW = new Parameter("viewMatrix", Type.Uniform, Data.Array.Type.SIXTEEN_FLOATS);
         public static Parameter VIEW_TO_CLIP  = new Parameter("projMatrix", Type.Uniform, Data.Array.Type.SIXTEEN_FLOATS);
         
         public static Parameter FACE_COLOR    = new Parameter("faceColor",   Type.Uniform, Data.Array.Type.THREE_FLOATS);
         public static Parameter BORDER_COLOR  = new Parameter("borderColor", Type.Uniform, Data.Array.Type.THREE_FLOATS);
         
         public static Parameter POSITIONS  = new Parameter("positions",  Type.VertexBuffer, Data.Array.Type.NINE_FLOATS);
         public static Parameter NORMALS    = new Parameter("normals",    Type.VertexBuffer, Data.Array.Type.NINE_FLOATS);
         public static Parameter BARYCOORDS = new Parameter("baryCoords", Type.VertexBuffer, Data.Array.Type.NINE_FLOATS);
         public static Parameter TEXCOORDS  = new Parameter("texCoords",  Type.VertexBuffer, Data.Array.Type.SIX_FLOATS);
      }
      
      public final List<Parameter> parameters;
      public Shader(Parameter... parameters) {
         this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
      }
      
      abstract void shadeTriangles(int numTriangles);
      abstract void destroy();
   }
   
   void setMatrix  (Shader.Parameter parameter, Matrix4x4 m);
   void setVector4 (Shader.Parameter parameter, Vector4 m);
   void setVector3 (Shader.Parameter parameter, Vector3 m);
   void setVector2 (Shader.Parameter parameter, Vector2 m);
   void setFloat   (Shader.Parameter parameter, float f);
   void setInteger (Shader.Parameter parameter, int i);
   
   void setVertexBuffer (Shader.Parameter parameter, VertexBuffer b);
   void setSampler      (Shader.Parameter parameter, Sampler s);
   
   
   // ---------------------------------------------------------------
   // Smooth Shader (no borders)
   // ---------------------------------------------------------------   
   public abstract class SmoothShader extends Shader {
      public SmoothShader() {
         super(Parameter.MODEL_TO_VIEW,
               Parameter.VIEW_TO_CLIP,
               Parameter.FACE_COLOR,
               Parameter.POSITIONS,
               Parameter.NORMALS);
      }
   }
   SmoothShader newSmoothShader ();

   // ---------------------------------------------------------------
   // Flat Shader With Borders
   // ---------------------------------------------------------------   
   public abstract class FlatBorderedShader extends Shader {
      public FlatBorderedShader() {
         super(Parameter.MODEL_TO_VIEW,
               Parameter.VIEW_TO_CLIP,
               Parameter.FACE_COLOR,
               Parameter.BORDER_COLOR,
               Parameter.POSITIONS,
               Parameter.NORMALS,
               Parameter.BARYCOORDS);
      }
   }
   FlatBorderedShader newFlatBorderedShader (float borderThinkness);
   
   */
   // -- so basically, the shading style is:
   //      Basic 
   //         3x  (uniform bgColor) or 
   //             (per-face bgColor) or
   //             (bg-Color comes from Texturemap)
   //         2x  optionally, borderColor applied or not?
   //         2x  optionally, normals interpolated?
   //        
   //      GridShading
   //      PerlinNoiseExperiment
   //      FractalExperiment
   //
   
   
   // methods to set rendering style?
   // we want to support styles:
   // 
   //  1 flat shading:
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //              meshColor-(vec3)
   //          vertex-buffers:
   //              positions (3-vec3's per triangle)
   //              normals   (3-vec3's per triangle)
   //
   //  2 flat-shading with borders
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //              meshColor-(vec3)
   //              borderColor-(vec3)
   //          vertex-buffers:
   //              positions  (3-vec3's per triangle)
   //              baryCoords (3-vec3's per triangle) <--- note that ALL meshes could share the same baryCoords
   //              normals    (3-vec3's per triangle)
   //
   //  2 per-face / per-edge / per-vertex colors
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //          vertex-buffers:
   //              positions  (3-vec3's per triangle)
   //              normals    (3-vec3's per triangle)
   //              colorInfo  (3-uvec4's per triangle) <-- each uvec4 provides
   //                             1 full int for "face color"
   //
   //  3 texture-shading
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //          vertex-buffers:
   //              positions  (3-vec3's per triangle)
   //              normals    (3-vec3's per triangle)
   //              texCoords  (3-vec2's per triangle)
   //          texture-maps:
   //              texture
   //
   //  4 texture-shading with borders
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //              borderColor-(vec3)
   //          vertex-buffers:
   //              positions  (3-vec3's per triangle)
   //              baryCoords (3-vec3's per triangle) <--- note that ALL meshes could share the same baryCoords
   //              normals    (3-vec3's per triangle)
   //              texCoords  (3-vec2's per triangle)
   //          texture-maps:
   //              texture
   //
   //   -- (NOTE: the above shaders *interpolate* texCoords and normal across each pixel,
   //               so the pixel shader knows its texCoords and normal, but nothing about the full triangle.
   //             below, grid-shading provides each pixel with v0,v1,v2 in both 3d and uv space,
   //               so it can figure out its own normal and texCoords)
   //
   //  5 grid-shading
   //      minimal SHADER needs:
   //          uniforms:
   //              modelToView-(mat4x4)
   //              viewToClip-(mat4x4)
   //          vertex-buffers:
   //              positions    (3-vec3's per triangle)
   //              baryCoords   (3-vec3's per triangle) <--- note that ALL meshes could share the same baryCoords
   //
   //              v0_positions (3-vec3's per triangle)
   //              v1_positions (3-vec3's per triangle)
   //              v2_positions (3-vec3's per triangle)
   //              v0_texCoords (3-vec2's per triangle)
   //              v1_texCoords (3-vec2's per triangle)
   //              v2_texCoords (3-vec2's per triangle)
   
      
   
}

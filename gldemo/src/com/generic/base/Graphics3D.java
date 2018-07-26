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
   public final HashMap<Integer, Shader> shaders = new HashMap<Integer, Shader>();
   public final ArrayList<Shader.Command> commands = new ArrayList<Shader.Command>();

   
   
   // ------------------------------------------
   // Shader Definitions
   // ------------------------------------------
   
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
               public Binding (Shader.Variable.VertexBuffer variable, int vertexBuffer) {
                  this.variable = variable;
                  this.vertexBuffer = vertexBuffer;
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
   
   // Styles we want to support:
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

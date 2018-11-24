package com.generic.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Rendering {
   
   // ------------------------------------------
   // How about this then?
   // ------------------------------------------
   public final HashMap<Integer, Data.Array> vertexBuffers = new HashMap<Integer, Data.Array>();
   public final HashMap<Integer, Image> samplers = new HashMap<Integer, Image>();
   public final HashMap<Integer, Shader> shaders = new HashMap<Integer, Shader>();
   public final ArrayList<Shader.Command> commands = new ArrayList<Shader.Command>();
   
   // ------------------------------------------
   // Shader Definitions
   // ------------------------------------------   
   public interface Shader {

      // "Smooth-Shading"
      public static final class Smooth implements Shader {
         public Smooth() {}
         // Uses:
         //   MODEL_TO_VIEW, VIEW_TO_CLIP
         //   POSITIONS, NORMALS
         //   FACE_COLOR
      }
 
      // "Flat-Shading-with-Borders"
      public static final class FlatBordered implements Shader {
         public final float borderThickness;
         public FlatBordered(float borderThickness) {
            this.borderThickness = borderThickness;
         }
         // Uses:
         //   MODEL_TO_VIEW, VIEW_TO_CLIP
         //   POSITIONS, NORMALS, BARYCOORDS
         //   FACE_COLOR, BORDER_COLOR
      }
      
      // Other shaders we should support eventually
      //
      // * "Texturemapped Color"
      //    (will need uvCoords vertexBuffer, with a sampler)
      //    (might also need parameters to select smooth/flat lighting and or border)
      //
      // * "PerFace/PerEdge/PerVertex Colors"
      //    (caller provides a vertexBuffer of "colors" with one color per face.
      //    and/or other colors for edges or vertices, or lines)
      //    
      // Experimental:
      // 
      // * "GridVisualization"
      //    (caller provides uvCoords vertexBuffer, current-uv-cursor-pos,
      //     and the matrix providing shader access to pixel-level positions)
      //
      // * "Juliabrot"
      //    (caller provides uvCoords vertexBuffer and iteration-limit values)
      //
      // * "PerlinNoise"
      //    (the blob-generator)

      
      // -------------------------------------------------------------------
      // Available Variables
      // -------------------------------------------------------------------      
      public static Variable.Matrix4x4 MODEL_TO_VIEW = new Variable.Matrix4x4("modelToView");
      public static Variable.Matrix4x4 VIEW_TO_CLIP  = new Variable.Matrix4x4("viewToClip");
      public static Variable.Vector3 FACE_COLOR      = new Variable.Vector3("faceColor");
      public static Variable.Vector3 BORDER_COLOR    = new Variable.Vector3("borderColor");
      public static Variable.VertexBuffer POSITIONS  = new Variable.VertexBuffer("positions", Data.Array.Type.FOUR_FLOATS);
      public static Variable.VertexBuffer NORMALS    = new Variable.VertexBuffer("normals", Data.Array.Type.THREE_FLOATS);
      public static Variable.VertexBuffer BARYCOORDS = new Variable.VertexBuffer("baryCoords", Data.Array.Type.THREE_FLOATS);
      
      // -------------------------------------------------------------------
      // Execution
      // -------------------------------------------------------------------      
      public interface Command {  
         public static final class Execute implements Shader.Command {
            public final int shader;
            public final int numTriangles;
            public Execute (int shader, int numTriangles) {
               this.shader = shader;
               this.numTriangles = numTriangles;
            }
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
         // - - - - - - - 
         // Sampler
         // - - - - - - - 
         public static final class Sampler extends Variable {
            public final Data.Array.Type type;
            public Sampler(String name, Data.Array.Type type) {
               super(name);
               this.type = type;
            }
            
            public static final class Binding implements Variable.Binding {
               public final Shader.Variable.Sampler variable;
               public final int sampler;
               public Binding (Shader.Variable.Sampler variable, int sampler) {
                  this.variable = variable;
                  this.sampler = sampler;
               }
            }            
         }
      }
   }
   
   // ------------------------------------------
   // Listeners
   // ------------------------------------------   
   
   public final HashSet<Listener> listeners = new HashSet<Listener>();
   
   // Here we have one way of supporting "listeners":
   // We provide public access to final HashMaps (vertexBuffers, samplers, shaders)
   //   and public access to a final HashSet of listeners.
   // But we declare that the contract for using this object is that any caller that
   //   wants to change our data needs to first acquire our lock, make his changes,
   //   then call the appropriate "listener functions", and only then release the lock.

   public interface Listener {
      // Changes to vertexBuffers
      public void vertexBufferAdded   (int vertexBuffer);
      public void vertexBufferRemoved (int vertexBuffer);
      public void vertexBufferChanged (int vertexBuffer);
      // Changes to samplers
      public void samplerAdded   (int sampler);
      public void samplerRemoved (int sampler);
      public void samplerChanged (int sampler);
      // Changes to shaders
      public void shaderAdded   (int shader);
      public void shaderRemoved (int shader);      
      // Changes to command-list
      public void commandsChanged ();
   }
   
   public void vertexBufferAdded (int vertexBuffer) {
      for (Listener listener : listeners) listener.vertexBufferAdded(vertexBuffer);
   }
   public void vertexBufferRemoved (int vertexBuffer) {
      for (Listener listener : listeners) listener.vertexBufferRemoved(vertexBuffer);
   }
   public void vertexBufferChanged (int vertexBuffer) {
      for (Listener listener : listeners) listener.vertexBufferChanged(vertexBuffer);
   }
   public void samplerAdded (int sampler) {
      for (Listener listener : listeners) listener.samplerAdded(sampler);
   }
   public void samplerRemoved (int sampler) {
      for (Listener listener : listeners) listener.samplerRemoved(sampler);
   }
   public void samplerChanged (int sampler) {
      for (Listener listener : listeners) listener.samplerChanged(sampler);
   }
   public void shaderAdded (int shader) {
      for (Listener listener : listeners) listener.shaderAdded(shader);
   }
   public void shaderRemoved (int shader) {
      for (Listener listener : listeners) listener.shaderRemoved(shader);
   }
   public void commandsChanged () {
      for (Listener listener : listeners) listener.commandsChanged();
   }
   
   // ----------------------------------------------------------------
   // Compare the above to this "different" form of listener:
   // ----------------------------------------------------------------
  
   public interface Listener2 {
      public void changeOccurred (Rendering.Change change);
   }
   public static class Change {
      
      // Consider, if there could be a principled way to "reflectively construct" these "Change" objects?
      // If we could represent that Graphics3D type as:
      //
      // Graphics3D == Record {
      //    "shaders"  : Map<Integer to DataArray>
      //    "samplers" : Map<Integer to DataArray>
      //    "shaders"  : Map<Integer to ShaderSpec>
      //    "commands" : List<Command>
      // }
      //
      // then the proper form of a CHANGE to Graphics3D should be mechanically generated,
      // so long as we know how to represent a CHANGE to DataArray, ShaderSpec, or Command.
      //
      // We would LIKE to be able to "parse" & "print" Graphics3D objects to/from Strings.
      // (Again, so long as we know how to "parse" & "print" the DataArray, ShaderSpec, Command
      // classes, the code to "parse" & "print" Graphics3D, and CHANGE to Graphics3D,
      // could be mechanically generated.)
      
      public final HashSet<Integer> vertexBuffersAdded   = new HashSet<Integer>();
      public final HashSet<Integer> vertexBuffersRemoved = new HashSet<Integer>();
      public final HashSet<Integer> vertexBuffersChanged = new HashSet<Integer>();

      public final HashSet<Integer> shadersAdded   = new HashSet<Integer>();
      public final HashSet<Integer> shadersRemoved = new HashSet<Integer>();
      public final HashSet<Integer> shadersChanged = new HashSet<Integer>();
      
      public final HashSet<Integer> samplersAdded   = new HashSet<Integer>();
      public final HashSet<Integer> samplersRemoved = new HashSet<Integer>();
      public final HashSet<Integer> samplersChanged = new HashSet<Integer>();
      
      // and some other class for "commandsChanged" (reference to Boolean)?
   }
}

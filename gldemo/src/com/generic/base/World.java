package com.generic.base;

import com.generic.base.Geometry.*;
import com.generic.base.Algebra.*;
import com.generic.base.Raster;
import com.generic.base.Raster.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class World {
   
   // ----------------------------------
   // ROOT model:
   // ----------------------------------
   
   public Model getRootModel() {
      return rootModel;
   }      
   public void setRootModel(Model rootModel) {
      this.rootModel = rootModel;
   }
   private Model rootModel;

   
   // ----------------------------------
   // Base model
   // ----------------------------------

   public static abstract class Model {
      public Model() {
         modelToWorld = Matrix4x4.IDENTITY;
      }
      
      private Matrix4x4 modelToWorld;

      public Matrix4x4 getModelToWorld() {
         return modelToWorld;
      }
      public void setModelToWorld(Matrix4x4 modelToWorld) {
         this.modelToWorld = modelToWorld;
      }
      public void translate(Vector3 t) {
         modelToWorld = Matrix4x4.product(Matrix4x4.translation(t), modelToWorld);
      }
      public void rotate(Vector3 axis, float angle) {
         modelToWorld = Matrix4x4.product(Matrix4x4.fromMatrix3f(Matrix3x3.rotation(axis, angle)), modelToWorld);
      }
      public void scale(float s) {
         modelToWorld = Matrix4x4.product(Matrix4x4.fromMatrix3f(Matrix3x3.scaling(s)), modelToWorld);
      }
      public void scale(float sx, float sy, float sz) {
         modelToWorld = Matrix4x4.product(Matrix4x4.fromMatrix3f(Matrix3x3.scaling(sx,sy,sz)), modelToWorld);
      }
      
   }
   
   // ------- Compound model
   
   public static class CompoundModel extends Model {
      public CompoundModel() { 
         children = new ArrayList<Model>();
      }
      
      public final ArrayList<Model> children;
   }
        
   // ------- Shader-Instance Models
   
   public static class ShaderExecutingModel extends Model {
      
      // ------------------------------------------------------------------
      // Let's review:
      // ------------------------------------------------------------------
      //
      // A Shader.Program is the program the GPU will execute.
      // It has a set of Shader.Variables that need to be BOUND first.
      //
      // --------------------
      //
      // A Shader.Instance holds the information necessary to 
      // execute a Shader.Program during the rendering process. 
      //
      // It holds a reference to the particular Shader.Program being executed,
      // plus a map of Shader.Variable.Bindings.   This map connects the set of
      // { uniform, sampler, and vertexBuffer } Shader.Variables that the program
      // needs, with specific { values, textures, or bufferManagers }, respectively.
      //
      // --------------------
      //
      // A ShaderExecutingModel holds a Shader.Instance but adds two things.
      // 
      // First, a ShaderExecutingModel has a specific position as a leaf within 
      // a Model TREE hierarchy.   The function World.bindPositions() traverses 
      // this Model TREE, computes the separate MODEL_TO_WORLD_MATRIX bindings
      // for each ShaderExecutingModel, and sets that uniform variable binding
      // in each Shader.Instance.   Three other uniform variable bindings are also
      // set by that function:  WORLD_TO_CLIP_MATRIX, WINDOW_WIDTH, and WINDOW_HEIGHT.
      //
      // Second, a ShaderExecutingModel holds a reference to a MeshModel, and any 
      // VertexBuffer bindings required by the Shader.Instance are bound, each in turn, 
      // to a BufferManager of the same name, if one is owned by the MeshModel.
      //
      // Note that, for a Shader.Instance to be runnable, all its variables must
      // have bindings, and many Shader.Instances have uniform or vertexBuffer
      // variables in addition to the few that are bound by ShaderExecutingModel,
      // that must be bound by the caller.
      
      
      public ShaderExecutingModel(Shader.Instance instance, MeshModel model) {
         this.model = model;
         this.instance = instance;
         
         // When the ShaderExecutingModel is constructed, we bind any VertexBuffer
         // variables to matching BufferManager's provided by the mesh:
         for (Shader.Variable variable : instance.program.variables) {
            if (variable instanceof Shader.Variable.VertexBuffer) {
               Shader.ManagedBuffer buffer = model.getManagedBuffer(variable.name);
               if (buffer != null) {
                  // Note that binding by "name" does not check that "variable.floatsPerElement" is
                  // the same as "buffer.numFloatsPerElement".  It certainly should be...
                  instance.bind(variable.name, new Shader.Variable.VertexBuffer.Binding(buffer));
               }
            }
         }
      }
      public final MeshModel model;
      public final Shader.Instance instance;
   }
   
   // ---------------------------------------------
   // Gathering the textures and models in use..
   // ---------------------------------------------

   public HashSet<Shader.ManagedBuffer> getBuffers() {
      HashSet<Shader.ManagedBuffer> buffers = new HashSet<Shader.ManagedBuffer>();
      addBuffers(rootModel, buffers);
      return buffers;
   }
   private void addBuffers(Model m, HashSet<Shader.ManagedBuffer> buffers) {
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            addBuffers(child, buffers);
         }
      }
      if (m instanceof ShaderExecutingModel) {
         Shader.Instance shaderInstance = ((ShaderExecutingModel) m).instance;
         for (Map.Entry<Shader.Variable, Shader.Variable.Binding> entry : shaderInstance.boundVariables.entrySet()) {
            Shader.Variable.Binding binding = entry.getValue();
            if (binding instanceof Shader.Variable.VertexBuffer.Binding) {
               Shader.Variable.VertexBuffer.Binding perVertexBinding = (Shader.Variable.VertexBuffer.Binding) binding;
               buffers.add(perVertexBinding.buffer);
            }
         }
      }
   }
   
   public HashSet<Shader.Instance> getShaderInstances() {
      HashSet<Shader.Instance> shaderInstances = new HashSet<Shader.Instance>();
      addShaderInstances(rootModel, shaderInstances);
      return shaderInstances;
   }
   private void addShaderInstances(Model m, HashSet<Shader.Instance> shaderInstances) {
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            addShaderInstances(child, shaderInstances);
         }
      }
      if (m instanceof ShaderExecutingModel) {
         shaderInstances.add(((ShaderExecutingModel) m).instance);
      }
   }

   // ---------------------------------------------
   // Static methods to gather Textures and Buffers
   // ---------------------------------------------
   
   public static HashSet<Shader.ManagedBuffer> getAllBuffers(Collection<Shader.Instance> shaderInstances) {
      HashSet<Shader.ManagedBuffer> buffers = new HashSet<Shader.ManagedBuffer>();
      for (Shader.Instance shaderInstance : shaderInstances) {
         
         // Walk through all the buffers in this instance:
         for (Shader.Variable variable : shaderInstance.program.variables) {
            if (variable instanceof Shader.Variable.VertexBuffer) {
               Shader.Variable.Binding binding = shaderInstance.boundVariables.get(variable);
               if (binding == null) {
                  throw new RuntimeException(String.format("Binding %s not present for program %s", 
                        variable.name, shaderInstance.program.name));
               }
               Shader.ManagedBuffer buffer = ((Shader.Variable.VertexBuffer.Binding) binding).buffer;
               buffers.add(buffer);
            }
         }
      }
      return buffers;
   }

   public static HashSet<Shader.ManagedTexture> getAllTextures(Collection<Shader.Instance> shaderInstances) {
      HashSet<Shader.ManagedTexture> textures = new HashSet<Shader.ManagedTexture>();
      for (Shader.Instance shaderInstance : shaderInstances) {
         for (Map.Entry<Shader.Variable, Shader.Variable.Binding> entry : shaderInstance.boundVariables.entrySet()) {
            Shader.Variable.Binding binding = entry.getValue();
            if (binding instanceof Shader.Variable.Sampler.Binding) {
               textures.add(((Shader.Variable.Sampler.Binding) binding).texture);
            }
         }
      }
      return textures;
   }
   
   
   // ---------------------------------------------
   // Pre-Render-Pass
   // ---------------------------------------------
   
   public void bindPositions(Model m, Matrix4x4 projMatrix, Matrix4x4 viewMatrix, int windowWidth, int windowHeight) {
      viewMatrix = Matrix4x4.product(viewMatrix, m.getModelToWorld());
      if (m instanceof ShaderExecutingModel) {
         Shader.Instance shaderInstance = ((ShaderExecutingModel) m).instance;
         
         shaderInstance.bind(Shader.WORLD_TO_CLIP_MATRIX, new Shader.Variable.Uniform.Mat4Binding(projMatrix));
         shaderInstance.bind(Shader.MODEL_TO_WORLD_MATRIX, new Shader.Variable.Uniform.Mat4Binding(viewMatrix));
         
         shaderInstance.bind(Shader.WINDOW_WIDTH, new Shader.Variable.Uniform.IntBinding(windowWidth));
         shaderInstance.bind(Shader.WINDOW_HEIGHT, new Shader.Variable.Uniform.IntBinding(windowHeight));
      }
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            bindPositions(child, projMatrix, viewMatrix, windowWidth, windowHeight);
         }
      }
   }
   public void bindPositions(Matrix4x4 projMatrix, Matrix4x4 viewMatrix, int windowWidth, int windowHeight) {
      bindPositions(rootModel, projMatrix, viewMatrix, windowWidth, windowHeight);
   }
}

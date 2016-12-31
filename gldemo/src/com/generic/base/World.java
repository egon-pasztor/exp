package com.generic.base;

import com.generic.base.Geometry.*;
import com.generic.base.VectorAlgebra.*;
import com.generic.base.World.ShaderInstanceModel;
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
         modelToWorld = Matrix4f.IDENTITY;
      }
      
      private Matrix4f modelToWorld;

      public Matrix4f getModelToWorld() {
         return modelToWorld;
      }
      public void setModelToWorld(Matrix4f modelToWorld) {
         this.modelToWorld = modelToWorld;
      }
      public void translate(Vector3f t) {
         modelToWorld = Matrix4f.product(Matrix4f.translation(t), modelToWorld);
      }
      public void rotate(Vector3f axis, float angle) {
         modelToWorld = Matrix4f.product(Matrix4f.fromMatrix3f(Matrix3f.rotation(axis, angle)), modelToWorld);
      }
      public void scale(float s) {
         modelToWorld = Matrix4f.product(Matrix4f.fromMatrix3f(Matrix3f.scaling(s)), modelToWorld);
      }
      public void scale(float sx, float sy, float sz) {
         modelToWorld = Matrix4f.product(Matrix4f.fromMatrix3f(Matrix3f.scaling(sx,sy,sz)), modelToWorld);
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
   
   public static class ShaderInstanceModel extends Model {
      public ShaderInstanceModel(Shader.Instance instance, MeshModel model) {
         this.model = model;
         this.instance = instance;
         for (Shader.Variable variable : instance.program.variables) {
            if (variable instanceof Shader.Variable.Buffer) {
               Shader.ManagedBuffer buffer = model.getManagedBuffer(variable.name);
               if (buffer != null) {
                  // Note that binding by "name" does not check that "variable.floatsPerElement" is
                  // the same as "buffer.numFloatsPerElement".  It certainly should be...
                  instance.bind(variable.name, new Shader.Variable.Buffer.Binding(buffer));
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
      if (m instanceof ShaderInstanceModel) {
         Shader.Instance shaderInstance = ((ShaderInstanceModel) m).instance;
         for (Map.Entry<Shader.Variable, Shader.Variable.Binding> entry : shaderInstance.boundVariables.entrySet()) {
            Shader.Variable.Binding binding = entry.getValue();
            if (binding instanceof Shader.Variable.Buffer.Binding) {
               Shader.Variable.Buffer.Binding perVertexBinding = (Shader.Variable.Buffer.Binding) binding;
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
      if (m instanceof ShaderInstanceModel) {
         shaderInstances.add(((ShaderInstanceModel) m).instance);
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
            if (variable instanceof Shader.Variable.Buffer) {
               Shader.Variable.Binding binding = shaderInstance.boundVariables.get(variable);
               Shader.ManagedBuffer buffer = ((Shader.Variable.Buffer.Binding) binding).buffer;
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
            if (binding instanceof Shader.Variable.Uniform.TextureBinding) {
               textures.add(((Shader.Variable.Uniform.TextureBinding) binding).texture);
            }
         }
      }
      return textures;
   }
   
   
   // ---------------------------------------------
   // Pre-Render-Pass
   // ---------------------------------------------
   
   public void bindPositions(Model m, Matrix4f projMatrix, Matrix4f viewMatrix, int windowWidth, int windowHeight) {
      viewMatrix = Matrix4f.product(viewMatrix, m.getModelToWorld());
      if (m instanceof ShaderInstanceModel) {
         Shader.Instance shaderInstance = ((ShaderInstanceModel) m).instance;
         
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
   public void bindPositions(Matrix4f projMatrix, Matrix4f viewMatrix, int windowWidth, int windowHeight) {
      bindPositions(rootModel, projMatrix, viewMatrix, windowWidth, windowHeight);
   }
}

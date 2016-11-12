package demo;

import com.jogamp.opengl.GL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import demo.VectorAlgebra.*;   // vectors/matrices
import demo.World.Shader;
import demo.Geometry.*;        // meshes(cube,sphere,ico)
import demo.Raster.*;          // texture image


public class World {

   public static class Shader {

      
      
      // okay. shader.
      // a shader is a pair of vertex/fragment programs installed into OpenGL together.
      // To work a shader requires a set of variables to be bound, of 3 different classes:
      //
      // uniform values: (lots of different format types -- we currently care only about INT, VEC3, and MAT4)
      //    Uniform-INT     (int)   gl.glUniform1i(int location, int value);
      //    Uniform-VEC3    (vec3)  gl.glUniform3f(int location, float x, float y, float z);
      //    Uniform-MAT4    (mat4)  gl.glUniformMatrix4f(int location, 1      /* count     */, false /* transpose */, 
      //                                                               float* /* 16 floats */, 0     /* offset    */
      //
      //
      // textures: (lots of different format types -- we currently care only about TEXTURE_2D)
      //    1.  the TEXTURE needed must be sent to OpenGL, associated with a textureId
      //                    and that textureId must be associated with one of then TEXTURE0,TEXTURE1,TEXTURE2 enums
      //
      //            gl.glActiveTexture(GL.GL_TEXTURE0);             /* which TEXTUREx enum are we affecting */
      //            gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);  /* link the TEXTUREx enum with a specific textureId object */
      //         
      //               glTexImage2D         <-- SEND the data to OpenGL and associate it with the active bound textureId
      //               glTexSubImage2D
      //
      //    2.  we must call  (int)   gl.glUniform1i(int location, int value);  
      //        where "value" specifies which TEXTURE0,TEXTURE1,TEXTURE2 enum to use.
      //
      //
      // per-vertex buffer data:
      //    1.  the BUFFER needed must be sent to OpenGL, associated with a bufferId
      //
      //              glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      //
      //              glBufferData          <-- SEND the data to OpenGL and associate it with the bound bufferId
      //              glBufferSubData
      //
      //    2.  we must call   glEnableVertexAttribArray(int location);
      //                       gl.glVertexAttribPointer(int location, 
      //                             numComponentsPerElement, GL.GL_FLOAT, false, 0, 0);
      //
      //        while the bufferId is bound
      
      
      // -----------------------------
      // Shader.Variable
      // -----------------------------
      
      public static class Variable {
         public final String name;
         public final Type type;
         
         public Variable(String name, Type type) {
            this.name = name; this.type = type;
         }
         
         public enum Type {
            INT_UNIFORM,
            VEC3_UNIFORM,
            MAT4_UNIFORM,
            
            VEC4_PER_VERTEX_BUFFER,
            VEC3_PER_VERTEX_BUFFER,
            VEC2_PER_VERTEX_BUFFER,
            
            BGRA_TEXTURE,
            GRAY_TEXTURE
         }
         
         // ProgramLocation Int -----------------------
         
         public void setGLProgramLocation(int glProgramLocation) {
            this.glProgramLocation = glProgramLocation;
         }
         public int getGLPProgramLocation() {
            return glProgramLocation;
         }
         private int glProgramLocation;
      }

      // 'cept of course "Variable.Type.VEC4_PER_VERTEX_BUFFER" doesn't specify that it's actually
      // supposed to be bound to a texture-coordiate-generating FloatBufferBuilder, nor that
      // it's supposed to be 4 components per element....
      
      // -----------------------------
      // Shader.Programs
      // -----------------------------

      public abstract static class Program {
         public final String name;
         public ArrayList<Variable> variables;
         
         public Program(String name) {
            this.name = name;
            variables = new ArrayList<Variable>();
            initVariables();
         }
         protected abstract void initVariables();
         
         public int getNumVariables() {
            return variables.size();
         }
         public Variable getVariable(int i) {
            return variables.get(i);
         }
         public Variable getVariable(String name) {
            for (Variable v : variables) {
               if (v.name.equals(name)) {
                  return v;
               }
            }
            return null;
         }
         
         // ProgramID Int -----------------------
         
         public void setGLProgramID(int glProgramID) {
            this.glProgramID = glProgramID;
         }
         public int getGLProgramID() {
            return glProgramID;
         }
         private int glProgramID;
      }
      
      // -----------------------------
      // Shader.Instance
      // -----------------------------

      public static class Instance {
         public final Shader.Program program;
         public HashMap<Variable,Binding> boundVariables;
         
         public Instance(Shader.Program program) {
            this.program = program;
            boundVariables = new HashMap<Variable,Binding>();
         }
         public void bind (String name, Binding binding) {
            //System.out.format(" Bound \"%s\" to \"%s\"\n",
            //      name, binding.toString());
            boundVariables.put(program.getVariable(name), binding);
         }

         // ProgramLocation Int -----------------------
         
         public void setGLVertexArraySetupID(int glVertexArraySetupID) {
            this.glVertexArraySetupID = glVertexArraySetupID;
         }
         public int getGLVertexArraySetupID() {
            return glVertexArraySetupID;
         }
         private int glVertexArraySetupID;
      }
      
      public interface Binding {}
      
      public static class UniformIntBinding implements Binding {
         public final Integer value;
         public UniformIntBinding(int value) {
            this.value = value;
         }
      }
      public static class UniformVec3Binding implements Binding {
         public final Vector3f value;
         public UniformVec3Binding(Vector3f value) {
            this.value = value;
         }
      }
      public static class UniformMat4Binding implements Binding {
         public final Matrix4f value;
         public UniformMat4Binding(Matrix4f value) {
            this.value = value;
         }
      }
      
      public static class PerVertexBinding implements Binding {
         public final ManagedBuffer buffer;
         public PerVertexBinding(ManagedBuffer buffer) {
            this.buffer = buffer;
         }
         public String toString() {
            return String.format("Per-vertex binding([%s])", (buffer!=null) ? buffer.toString():"NULL");
         }
      }
      public static class TextureBinding implements Binding {
         public final ManagedTexture texture;
         public TextureBinding(ManagedTexture texture) {
            this.texture = texture;
         }
      }

      // -----------------------------
      // Managed Buffer
      // -----------------------------
      
      public static abstract class ManagedBuffer {
         public final int numFloatsPerElement;
         public String name;
         
         public ManagedBuffer(int numFloatsPerElement) {
            this.numFloatsPerElement = numFloatsPerElement;
            this.modified = true;
            this.glBufferID = null;
            this.glBufferSize = null;
         }
         
         // Abstract Methods -----------------------
         public abstract int getNumElements();
         public abstract void fillBuffer(float[] array);
            
         // Modified Bool -----------------------
            
         public boolean isModified() {
            return modified;
         }
         public void setModified(boolean modified) {
            this.modified = modified;
         }
         private boolean modified;

         // Buffer Updates-----------------------
         
         public void setup() {
            int newNumElements = getNumElements();
            int newNumFloats = newNumElements * numFloatsPerElement;
            if (newNumFloats == 0) {
               floatBuffer = null;
               array = null;
            } else {
               if ((array == null) || (array.length != newNumFloats)) {
                  array = new float[newNumFloats];
                  
                  ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
                  byteBuffer.order(ByteOrder.nativeOrder());
                  floatBuffer = byteBuffer.asFloatBuffer();
               }
               fillBuffer(array);
               floatBuffer.position(0);
               floatBuffer.put(array);
               floatBuffer.position(0);
            }
            modified = false;
         }
         public float[] array;
         public FloatBuffer floatBuffer;

         // GL Buffer Id ----------------------
         
         public Integer glBufferID;
         public Integer glBufferSize;
      }
      
      // -----------------------------
      // Managed Texture
      // -----------------------------
      
      public static class ManagedTexture {
         public ManagedTexture(Image image) {
            this.image = image;
         }

         // Buffer Updates-----------------------
         
         public void setup() {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(image.pixels.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(image.pixels);
            intBuffer.position(0);
         }
         
         Image image;
         ByteBuffer byteBuffer;
         IntBuffer intBuffer;
         
         // GL Buffer Id ----------------------

         Integer glTextureID;
      }
      
      // -----------------------------
      // Names for Shader Constants
      // -----------------------------

      public static final Program TEXTURE_SHADER = new Program("textured_faces_with_wireframe_shader") {
         @Override
         protected void initVariables() {
            variables.add(new Variable(Shader.MAIN_TEXTURE,    Variable.Type.BGRA_TEXTURE));
            variables.add(new Variable(Shader.POSITION_ARRAY,  Variable.Type.VEC4_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.COLOR_ARRAY,     Variable.Type.VEC4_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.TEX_COORDS,      Variable.Type.VEC4_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.BARY_COORDS,     Variable.Type.VEC2_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.HIGHLIGHT_BOOL,  Variable.Type.INT_UNIFORM));
            variables.add(new Variable(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Type.MAT4_UNIFORM));
            variables.add(new Variable(Shader.MODEL_TO_WORLD_MATRIX, Variable.Type.MAT4_UNIFORM));
         }
      };
      
      public static final Program FACE_COLOR_SHADER = new Program("each_face_a_different_flat_color_shader") {
         @Override
         protected void initVariables() {
            //variables.add(new Variable(Shader.MAIN_TEXTURE,    Variable.Type.BGRA_TEXTURE));
            variables.add(new Variable(Shader.POSITION_ARRAY,  Variable.Type.VEC4_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.COLOR_ARRAY,     Variable.Type.VEC4_PER_VERTEX_BUFFER));
            //variables.add(new Variable(Shader.TEX_COORDS,      Variable.Type.VEC4_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.BARY_COORDS,     Variable.Type.VEC2_PER_VERTEX_BUFFER));
            variables.add(new Variable(Shader.HIGHLIGHT_BOOL,  Variable.Type.INT_UNIFORM));
            variables.add(new Variable(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Type.MAT4_UNIFORM));
            variables.add(new Variable(Shader.MODEL_TO_WORLD_MATRIX, Variable.Type.MAT4_UNIFORM));
         }
      };
      
      public static final String POSITION_ARRAY = "vertexPosition";
      public static final String COLOR_ARRAY    = "vertexColor";
      public static final String TEX_COORDS     = "vertexTexCoords";
      public static final String BARY_COORDS    = "vertexBaryCoords";
      
      public static final String MAIN_TEXTURE = "mainTexture";
      
      public static final String WORLD_TO_CLIP_MATRIX = "projMatrix";
      public static final String MODEL_TO_WORLD_MATRIX = "viewMatrix";

      public static final String HIGHLIGHT_BOOL = "highlight";
   }
   
   
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
            if ((variable.type == Shader.Variable.Type.VEC2_PER_VERTEX_BUFFER) ||
                (variable.type == Shader.Variable.Type.VEC3_PER_VERTEX_BUFFER) ||
                (variable.type == Shader.Variable.Type.VEC4_PER_VERTEX_BUFFER)) {
               
               Shader.ManagedBuffer buffer = model.getManagedBuffer(variable.name);
               if (buffer != null) {
                  instance.bind(variable.name, new Shader.PerVertexBinding(buffer));
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
         for (Map.Entry<Shader.Variable,Shader.Binding> entry : shaderInstance.boundVariables.entrySet()) {
            Shader.Binding binding = entry.getValue();
            if (binding instanceof Shader.PerVertexBinding) {
               Shader.PerVertexBinding perVertexBinding = (Shader.PerVertexBinding) binding;
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
   // Pre-Render-Pass
   // ---------------------------------------------
   
   public void bindPositions(Model m, Matrix4f projMatrix, Matrix4f viewMatrix) {
      viewMatrix = Matrix4f.product(viewMatrix, m.getModelToWorld());
      if (m instanceof ShaderInstanceModel) {
         ((ShaderInstanceModel)m).instance.bind(Shader.WORLD_TO_CLIP_MATRIX, new Shader.UniformMat4Binding(projMatrix));
         ((ShaderInstanceModel)m).instance.bind(Shader.MODEL_TO_WORLD_MATRIX, new Shader.UniformMat4Binding(viewMatrix));
      }
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            bindPositions(child, projMatrix, viewMatrix);
         }
      }
   }
   public void bindPositions(Matrix4f projMatrix, Matrix4f viewMatrix) {
      bindPositions(rootModel, projMatrix, viewMatrix);
   }
}

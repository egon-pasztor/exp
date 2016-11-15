package demo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import demo.VectorAlgebra.*;   // vectors/matrices
import demo.Geometry.*;        // meshes(cube,sphere,ico)
import demo.Raster.*;          // texture image


public class World {

   public static class Shader {

      // -----------------------------
      // Shader.Variable
      // -----------------------------
      
      public static abstract class Variable {
         public final String name;
         
         public Variable(String name) {
            this.name = name;
         }      
         
         public interface Binding {}
         
         // ProgramLocation Int -----------------------
         
         public void setGLProgramLocation(int glProgramLocation) {
            this.glProgramLocation = glProgramLocation;
         }
         public int getGLPProgramLocation() {
            return glProgramLocation;
         }
         private int glProgramLocation;
         
         // ----------------------------------------------
         // "Buffer" Variable Types
         // ----------------------------------------------
         
         public static class Buffer extends Variable {
            public final int floatsPerElement;
            
            public Buffer(String name, int floatsPerElement) {
               super(name);
               this.floatsPerElement = floatsPerElement;
            }
            
            public static class Binding implements Variable.Binding {
               public final ManagedBuffer buffer;
               public Binding(ManagedBuffer buffer) {
                  this.buffer = buffer;
               }
               public String toString() {
                  return String.format("Per-vertex binding([%s])", (buffer!=null) ? buffer.toString():"NULL");
               }
            }
         }
         
         // ----------------------------------------------
         // "Uniform" Variable Types
         // ----------------------------------------------         

         public static class Uniform extends Variable {
            public final Type type;
            
            public Uniform(String name, Type type) {
               super(name);
               this.type = type;
            }
            
            public enum Type {
               INTEGER,
               VECTOR3,
               MATRIX4,
               BGRA_TEXTURE,
               GRAY_TEXTURE
            }
            
            public static class IntBinding implements Variable.Binding {
               public final Integer value;
               public IntBinding(int value) {
                  this.value = value;
               }
            }
            public static class Vec3Binding implements Variable.Binding {
               public final Vector3f value;
               public Vec3Binding(Vector3f value) {
                  this.value = value;
               }
            }
            public static class Mat4Binding implements Variable.Binding {
               public final Matrix4f value;
               public Mat4Binding(Matrix4f value) {
                  this.value = value;
               }
            }
            public static class TextureBinding implements Variable.Binding {
               public final ManagedTexture texture;
               public TextureBinding(ManagedTexture texture) {
                  this.texture = texture;
               }
            }
         }
      }
      
      // -----------------------------
      // Shader.Programs
      // -----------------------------

      public abstract static class Program {
         public final String name;
         public final String vertexShaderName;
         public final String fragmentShaderName;
         public ArrayList<Variable> variables;
         
         public Program(String name, String vertexShaderName, String fragmentShaderName) {
            this.name = name;
            this.vertexShaderName = vertexShaderName;
            this.fragmentShaderName = fragmentShaderName;
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
         public HashMap<Variable, Variable.Binding> boundVariables;
         
         public Instance(Shader.Program program) {
            this.program = program;
            boundVariables = new HashMap<Variable, Variable.Binding>();
         }
         public void bind (String name, Variable.Binding binding) {
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

      public static final Program TEXTURE_SHADER = new Program("textured_faces_with_wireframe_shader", 
            "vertex.shader", "fragment.shader") {
         @Override
         protected void initVariables() {
            variables.add(new Variable.Buffer(Shader.POSITION_ARRAY, 4));
            variables.add(new Variable.Buffer(Shader.COLOR_ARRAY,    4));
            variables.add(new Variable.Buffer(Shader.TEX_COORDS,     4));
            variables.add(new Variable.Buffer(Shader.BARY_COORDS,    2));
            
            variables.add(new Variable.Uniform(Shader.MAIN_TEXTURE,          Variable.Uniform.Type.BGRA_TEXTURE));
            variables.add(new Variable.Uniform(Shader.HIGHLIGHT_BOOL,        Variable.Uniform.Type.INTEGER));
            variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
            variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
         }
      };
      
      public static final Program FACE_COLOR_SHADER = new Program("each_face_a_different_flat_color_shader",
            "vertex2.shader", "fragment2.shader") {
         @Override
         protected void initVariables() {
            variables.add(new Variable.Buffer(Shader.POSITION_ARRAY, 4));
            variables.add(new Variable.Buffer(Shader.COLOR_ARRAY,    4));
            variables.add(new Variable.Buffer(Shader.BARY_COORDS,    2));
            
            variables.add(new Variable.Uniform(Shader.HIGHLIGHT_BOOL,        Variable.Uniform.Type.INTEGER));
            variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
            variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
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
            if (variable instanceof Shader.Variable.Buffer) {
               Shader.ManagedBuffer buffer = model.getManagedBuffer(variable.name);
               if (buffer != null) {
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
   // Pre-Render-Pass
   // ---------------------------------------------
   
   public void bindPositions(Model m, Matrix4f projMatrix, Matrix4f viewMatrix) {
      viewMatrix = Matrix4f.product(viewMatrix, m.getModelToWorld());
      if (m instanceof ShaderInstanceModel) {
         ((ShaderInstanceModel)m).instance.bind(Shader.WORLD_TO_CLIP_MATRIX, new Shader.Variable.Uniform.Mat4Binding(projMatrix));
         ((ShaderInstanceModel)m).instance.bind(Shader.MODEL_TO_WORLD_MATRIX, new Shader.Variable.Uniform.Mat4Binding(viewMatrix));
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

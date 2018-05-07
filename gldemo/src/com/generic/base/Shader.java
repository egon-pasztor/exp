package com.generic.base;

import com.generic.base.Algebra.Matrix4x4;
import com.generic.base.Algebra.Vector2;
import com.generic.base.Algebra.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Shader {
   
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
      
      public static class VertexBuffer extends Variable {
         public final Type type;

         public enum Type {
            UINT  (false, 1), FLOAT (true, 1),
            UVEC2 (false, 2), VEC2  (true, 2), 
            UVEC3 (false, 3), VEC3  (true, 3), 
            UVEC4 (false, 4), VEC4  (true, 4);
            
            private Type(boolean baseIsFloat, int numElementsPerVertex) {
               this.baseIsFloat = baseIsFloat;
               this.numElementsPerVertex = numElementsPerVertex;
            }
            public boolean baseIsFloat;
            public int numElementsPerVertex;
         }
         
         public VertexBuffer(String name, Type type) {
            super(name);
            this.type = type;
         }
         public String toString() {
            return String.format("VertexBuffer variable([%s/(%s x %d)])",
                  name, type.baseIsFloat ? "float":"int", type.numElementsPerVertex);
         }
         
         public static class Binding implements Variable.Binding {
            public final ManagedBuffer buffer;
            public Binding(ManagedBuffer buffer) {
               this.buffer = buffer;
            }
            public String toString() {
               return String.format("binding to [%s]", (buffer!=null) ? buffer.toString():"NULL");
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
         public String toString() {
            return String.format("Uniform variable([%s/%s])", name, type.toString());
         }
         
         public enum Type {
            UINT,  FLOAT,
            UVEC2, VEC2, 
            UVEC3, VEC3, 
            UVEC4, VEC4,
            MATRIX4,
         }
         
         public static class IntBinding implements Variable.Binding {
            public final Integer value;
            public IntBinding(int value) {
               this.value = value;
            }
            public String toString() {
               return String.format("binding to [%s]", (value==null)?"null":Integer.valueOf(value).toString());
            }
         }
         public static class Vec3Binding implements Variable.Binding {
            public final Vector3 value;
            public Vec3Binding(Vector3 value) {
               this.value = value;
            }
            public String toString() {
               return String.format("binding to [%s]", (value==null)?"null":value.toString());
            }
         }
         public static class Vec2Binding implements Variable.Binding {
            public final Vector2 value;
            public Vec2Binding(Vector2 value) {
               this.value = value;
            }
            public String toString() {
               return String.format("binding to [%s]", (value==null)?"null":value.toString());
            }
         }
         public static class Mat4Binding implements Variable.Binding {
            public final Matrix4x4 value;
            public Mat4Binding(Matrix4x4 value) {
               this.value = value;
            }
            public String toString() {
               return String.format("binding to [%s]", (value==null)?"null":value.toString());
            }
         }
      }
         
      // ----------------------------------------------
      // "Sampler" Variable Types
      // ----------------------------------------------         

      public static class Sampler extends Variable {
         public final Type type;
         
         public Sampler(String name, Type type) {
            super(name);
            this.type = type;
         }
         public String toString() {
            return String.format("Uniform SAMPLER variable([%s/%s])", name, type.getClass().getSimpleName());
         }
         
         public enum Type {
            TEXTURE_FLOAT,
            TEXTURE_32BIT,
            TEXTURE_8BIT
         }
         
         public static class Binding implements Variable.Binding {
            public final ManagedTexture texture;
            public Binding(ManagedTexture ico0_mesh_info) {
               this.texture = ico0_mesh_info;
            }
            public String toString() {
               return String.format("binding to ([%s])", (texture==null)?"null":texture.toString());
            }
         }
         public static class Binding2 implements Variable.Binding {
            public final ManagedFloatTexture texture;
            public Binding2(ManagedFloatTexture ico0_mesh_info) {
               this.texture = ico0_mesh_info;
            }
            public String toString() {
               return String.format("binding to ([%s])", (texture==null)?"null":texture.toString());
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
         Variable variable = program.getVariable(name);
         
         // What's wrong with Binding to non-existent names?  We bind "highlight" for every Shader.Instance,
         // even though some Shader.Programs don't ingest "highlight"..
         //
         //if (variable == null) {
         //   throw new RuntimeException(String.format("Binding %s not applicable for program %s", name, program.name));
         //}
         boundVariables.put(program.getVariable(name), binding);
      }
      public void checkAllVariablesBindingsPresent() {
         for (Variable v : program.variables) {
            if (!boundVariables.containsKey(v)) {
               throw new RuntimeException(String.format("Binding %s not present for program %s", v.name, program.name));
            }
         }
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
      public final Variable.VertexBuffer.Type type; 
      
      public ManagedBuffer(Variable.VertexBuffer.Type type) {
         this.type = type;
         this.modified = true;
         this.glBufferID = null;
         this.glBufferSize = null;
      }
      
      // Abstract Methods -----------------------
      public abstract int getNumElements();
         
      // Modified Bool -----------------------
         
      public boolean isModified() {
         return modified;
      }
      public void setModified(boolean modified) {
         this.modified = modified;
      }
      private boolean modified;

      // GL Buffer Id ----------------------
      
      public abstract void setup();
      public Integer glBufferID;
      public Integer glBufferSize;
   }

   public static abstract class ManagedFloatBuffer extends ManagedBuffer {
      public ManagedFloatBuffer(int numFloatsPerVertex) {
         super((numFloatsPerVertex == 4) ? Variable.VertexBuffer.Type.VEC4  :
               (numFloatsPerVertex == 3) ? Variable.VertexBuffer.Type.VEC3  :
               (numFloatsPerVertex == 2) ? Variable.VertexBuffer.Type.VEC2  :
                                           Variable.VertexBuffer.Type.FLOAT);
      }

      public void setup() {
         int newNumElements = getNumElements();
         int newNumFloats = newNumElements * type.numElementsPerVertex;
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
         setModified(false);
      }
      public abstract void fillBuffer(float[] array);
      public float[] array;
      public FloatBuffer floatBuffer;
   }
   
   public static abstract class ManagedIntBuffer extends ManagedBuffer {
      public ManagedIntBuffer(int numIntsPerVertex) {
         super((numIntsPerVertex == 4) ? Variable.VertexBuffer.Type.UVEC4  :
               (numIntsPerVertex == 3) ? Variable.VertexBuffer.Type.UVEC3  :
               (numIntsPerVertex == 2) ? Variable.VertexBuffer.Type.UVEC2  :
                                         Variable.VertexBuffer.Type.UINT);
      }

      public void setup() {
         int newNumElements = getNumElements();
         int newNumInts = newNumElements * type.numElementsPerVertex;
         if (newNumInts == 0) {
            intBuffer = null;
            array = null;
         } else {
            if ((array == null) || (array.length != newNumInts)) {
               array = new int[newNumInts];
               
               ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
               byteBuffer.order(ByteOrder.nativeOrder());
               intBuffer = byteBuffer.asIntBuffer();
            }
            fillBuffer(array);
            intBuffer.position(0);
            intBuffer.put(array);
            intBuffer.position(0);
            System.out.format("Actually filled int-buffer data with put call...\n");
         }
         setModified(false);
      }
      public abstract void fillBuffer(int[] array);
      public int[] array;
      public IntBuffer intBuffer;
   }
   
   // -----------------------------
   // Managed Texture
   // -----------------------------
   
   public static class ManagedTexture {
      public ManagedTexture(Variable.Sampler.Type type, Image image) {
         this.type = type;
         this.image = image;
      }

      // Buffer Updates-----------------------
      
      public void setup() {
         int[] pixels = ((Data.Array.Integers)(image.data)).array();
         
         ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         intBuffer = byteBuffer.asIntBuffer();
         intBuffer.put(pixels);
         intBuffer.position(0);
      }
      
      public Variable.Sampler.Type type;
      public Image image;
      public ByteBuffer byteBuffer;
      public IntBuffer intBuffer;
      
      // GL Buffer Id ----------------------

      public Integer glTextureID;
   }

   public static class ManagedFloatTexture {
      public ManagedFloatTexture(Variable.Sampler.Type type, Image image) {
         this.type = type;
         this.image = image;
      }

      // Buffer Updates-----------------------
      
      public void setup() {
         float[] pixels = ((Data.Array.Floats)(image.data)).array();
         
         ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length * 4);
         byteBuffer.order(ByteOrder.nativeOrder());
         floatBuffer = byteBuffer.asFloatBuffer();
         floatBuffer.put(pixels);
         floatBuffer.position(0);
      }
      
      public Variable.Sampler.Type type;
      public Image image;
      public ByteBuffer byteBuffer;
      public FloatBuffer floatBuffer;
      
      // GL Buffer Id ----------------------

      public Integer glTextureID;
   }
   
   // -----------------------------
   // Names for Shader Constants
   // -----------------------------

   public static final Program FLAT_SHADER = new Program("flat_shader", 
         "flat_vertex.shader", "flat_fragment.shader") {
      @Override
      protected void initVariables() {
         variables.add(new Variable.VertexBuffer(Shader.POSITION_ARRAY,  Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.COLOR_ARRAY,     Variable.VertexBuffer.Type.VEC3));
         variables.add(new Variable.VertexBuffer(Shader.NORMAL_ARRAY,    Variable.VertexBuffer.Type.VEC3));
         variables.add(new Variable.VertexBuffer(Shader.BARY_COORDS,     Variable.VertexBuffer.Type.VEC3));         
         
         variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
         variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
      }
   };
   
   public static final Program TEXTURE_SHADER = new Program("textured_shader", 
         "vertex.shader", "fragment.shader") {
      @Override
      protected void initVariables() {
         variables.add(new Variable.VertexBuffer(Shader.POSITION_ARRAY,  Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.COLOR_ARRAY,     Variable.VertexBuffer.Type.VEC3));
         variables.add(new Variable.VertexBuffer(Shader.BARY_COORDS,     Variable.VertexBuffer.Type.VEC3));         
         variables.add(new Variable.VertexBuffer(Shader.TEX_COORDS,      Variable.VertexBuffer.Type.VEC4));         
         
         variables.add(new Variable.Sampler(Shader.MAIN_TEXTURE, Variable.Sampler.Type.TEXTURE_32BIT));
         
         variables.add(new Variable.Uniform(Shader.HIGHLIGHT_BOOL,        Variable.Uniform.Type.UINT));
         variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
         variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
      }
   };
   public static final Program FACE_COLOR_SHADER = new Program("face_color_shader",
         "vertex2.shader", "fragment2.shader") {
      @Override
      protected void initVariables() {
         variables.add(new Variable.VertexBuffer(Shader.POSITION_ARRAY, Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.COLOR_ARRAY,    Variable.VertexBuffer.Type.VEC3));
         variables.add(new Variable.VertexBuffer(Shader.COLOR_INFO,     Variable.VertexBuffer.Type.UVEC4));
         variables.add(new Variable.VertexBuffer(Shader.BARY_COORDS,    Variable.VertexBuffer.Type.VEC3));         
         
         variables.add(new Variable.VertexBuffer(Shader.V0POS_ARRAY,    Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.V1POS_ARRAY,    Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.V2POS_ARRAY,    Variable.VertexBuffer.Type.VEC4));
         
         variables.add(new Variable.VertexBuffer(Shader.V0UV_ARRAY,     Variable.VertexBuffer.Type.VEC2));
         variables.add(new Variable.VertexBuffer(Shader.V1UV_ARRAY,     Variable.VertexBuffer.Type.VEC2));
         variables.add(new Variable.VertexBuffer(Shader.V2UV_ARRAY,     Variable.VertexBuffer.Type.VEC2));
         
         variables.add(new Variable.Sampler(Shader.MESH_INFO, Variable.Sampler.Type.TEXTURE_FLOAT));
         variables.add(new Variable.VertexBuffer(Shader.TRIANGLE_INDEX,    Variable.VertexBuffer.Type.FLOAT));         

         variables.add(new Variable.VertexBuffer(Shader.DIRECTION_SHADING_ARRAY,  Variable.VertexBuffer.Type.VEC3));
         
         variables.add(new Variable.Uniform(Shader.HIGHLIGHT_BOOL,        Variable.Uniform.Type.UINT));
         variables.add(new Variable.Uniform(Shader.UV_POINTER,            Variable.Uniform.Type.VEC2));
         variables.add(new Variable.Uniform(Shader.WINDOW_WIDTH,          Variable.Uniform.Type.UINT));
         variables.add(new Variable.Uniform(Shader.WINDOW_HEIGHT,         Variable.Uniform.Type.UINT));
         variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
         variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
      }
   };
   public static final Program POINT_COLOR_SHADER = new Program("point_color_shader",
         "vertex3.shader", "fragment3.shader") {
      @Override
      protected void initVariables() {
         variables.add(new Variable.VertexBuffer(Shader.POSITION_ARRAY, Variable.VertexBuffer.Type.VEC4));
         variables.add(new Variable.VertexBuffer(Shader.BARY_COORDS,    Variable.VertexBuffer.Type.VEC3));
         
         variables.add(new Variable.Uniform(Shader.HIGHLIGHT_BOOL,        Variable.Uniform.Type.UINT));
         variables.add(new Variable.Uniform(Shader.TRANSLATION_VEC,       Variable.Uniform.Type.VEC3));
         variables.add(new Variable.Uniform(Shader.WORLD_TO_CLIP_MATRIX,  Variable.Uniform.Type.MATRIX4));
         variables.add(new Variable.Uniform(Shader.MODEL_TO_WORLD_MATRIX, Variable.Uniform.Type.MATRIX4));
      }
   };
   
   public static final String POSITION_ARRAY = "vertexPosition";
   public static final String NORMAL_ARRAY   = "vertexNormal";
   public static final String COLOR_ARRAY    = "vertexColor";
   public static final String COLOR_INFO     = "triColorInfo";
   
   public static final String DIRECTION_SHADING_ARRAY = "directions";
   
   public static final String V0POS_ARRAY    = "vertexV0pos";
   public static final String V1POS_ARRAY    = "vertexV1pos";
   public static final String V2POS_ARRAY    = "vertexV2pos";
   public static final String V0UV_ARRAY     = "vertexV0uv";
   public static final String V1UV_ARRAY     = "vertexV1uv";
   public static final String V2UV_ARRAY     = "vertexV2uv";
   
   public static final String TEX_COORDS     = "vertexTexCoords";
   public static final String BARY_COORDS    = "vertexBaryCoords";
   
   public static final String MAIN_TEXTURE = "mainTexture";
   public static final String MESH_INFO = "meshInfo";
   public static final String TRIANGLE_INDEX = "vertexTriangleIndex";
   
   public static final String WORLD_TO_CLIP_MATRIX = "projMatrix";
   public static final String MODEL_TO_WORLD_MATRIX = "viewMatrix";

   public static final String HIGHLIGHT_BOOL = "highlight";
   public static final String UV_POINTER = "uvPointer";
   public static final String TRANSLATION_VEC = "translation";

   public static final String WINDOW_WIDTH = "windowWidth";
   public static final String WINDOW_HEIGHT = "windowHeight";

}

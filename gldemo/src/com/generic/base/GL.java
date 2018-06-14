package com.generic.base;

import com.generic.base.Algebra.Matrix4x4;
import com.generic.base.Algebra.Vector2;
import com.generic.base.Algebra.Vector3;
import com.generic.base.Algebra.Vector4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface GL {
   
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

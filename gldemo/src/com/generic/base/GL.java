package com.generic.base;

public interface GL {
   
   void setViewport (int width, int height);
   void clear (Color color);
   
   // ------------------------------------------
   // VertexBuffer
   // ------------------------------------------
   public interface VertexBuffer {
      Data.Array.Type type();
      int numElements();
      
      void update(Data.Array data);
      void destroy();
      boolean isDestroyed();
   }
   VertexBuffer newVertexBuffer (Data.Array data);
   
   // ------------------------------------------
   // Sampler
   // ------------------------------------------
   public interface Sampler {
      Data.Array.Type type();
      int width();
      int height();
      
      void update(Image image);
      void destroy();
      boolean isDestroyed();
   }
   Sampler newSampler (Image image);

   // ------------------------------------------------------------------------------
   // Shader
   // ------------------------------------------------------------------------------
   public interface Shader {
      
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
  
      int numParameters();
      Parameter parameter(int i);
      
      void destroy();
      boolean isDestroyed();
   }
   
   void shadeTriangles (int numTriangles, Shader shader,
                        Object... parameterValues);
   
   // ---------------------------------------------------------------
   // Smooth Shader (no borders)
   // ---------------------------------------------------------------   
   // The Shader returned will have parameters:
   //
   //      1. MODEL_TO_VIEW
   //      2. VIEW_TO_CLIP
   //      3. FACE_COLOR
   //      5. POSITIONS
   //      6. NORMALS
   //
   Shader newSmoothShader ();

   // ---------------------------------------------------------------
   // Flat Shader With Borders
   // ---------------------------------------------------------------   
   // The Shader returned will have parameters:
   //      1. MODEL_TO_VIEW
   //      2. VIEW_TO_CLIP
   //      3. FACE_COLOR
   //      4. BORDER_COLOR
   //      5. POSITIONS
   //      6. NORMALS
   //      7. BARYCOORDS
   //
   Shader newFlatShaderWithBorders ();
   
   
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

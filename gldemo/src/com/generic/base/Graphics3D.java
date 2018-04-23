package com.generic.base;

import com.generic.base.Algebra.Matrix3x3;
import com.generic.base.Algebra.Matrix4x4;
import com.generic.base.Algebra.Vector3;

import java.util.ArrayList;

public class Graphics3D {
   
   public static class Scene {
      
      public void render(GL.Renderer gl) {
         // ...
         // seriously?
         // this would.... do what?
      }
      
      // -----------------------
      // Scene has to keep track of ... all the vertexbuffers being used ...
      // 
      
      
      
      
      public void setRootModel(Model rootModel) {
         this.rootModel = rootModel;
      }
      
      private Model rootModel;
   }
   
   // ----------------------------------------------------
   // Model contains a Matrix4x4 modelToWorld transform
   // ----------------------------------------------------
   public static abstract class Model {
      public Model() {
         modelToWorld = Matrix4x4.IDENTITY;
      }
      
      public Matrix4x4 getTransformation() {
         return modelToWorld;
      }
      public void setTransformation(Matrix4x4 modelToWorld) {
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
      
      private Matrix4x4 modelToWorld;
   }
   
   // ----------------------------------------------------
   // CompoundModel holds a list of submodels
   // ----------------------------------------------------
   public static class CompoundModel extends Model {
      public CompoundModel() { 
         children = new ArrayList<Model>();
      }
      public final ArrayList<Model> children;
   }
   
   // ----------------------------------------------------
   // MeshModel renders a mesh in some style..
   // ----------------------------------------------------
   public static class MeshModel extends Model {
      public MeshModel(Mesh2 mesh) { 
         this.mesh = mesh;
      }
      public final Mesh2 mesh;
      
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
      //              meshColor-(vec3)
      //              borderColor-(vec3)
      //          vertex-buffers:
      //              positions  (3-vec3's per triangle)
      //              baryCoords (3-vec3's per triangle) <--- note that ALL meshes could share the same baryCoords
      //              normals    (3-vec3's per triangle)
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
   
}



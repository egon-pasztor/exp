package com.generic.base;

import com.generic.base.Algebra.*;

import java.util.HashMap;
import java.util.HashSet;

public class Scene3D {

   public Scene3D() {
      root = new Model.Group();
   }
   
   public Model root() {
      return root;
   }
   
   private final Model root;
   
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


      // ----------------------------------------------------
      // Group holds a list of submodels
      // ----------------------------------------------------
      public static class Group extends Model {
         public Group() { 
            children = new HashSet<Model>();
         }
         public int numChildren() {
            return children.size();
         }
         public Iterable<Model> children() {
            return children;
         }
         public void addChild(Model child) {
            children.add(child);
         }
         public void removeChild(Model child) {
            children.remove(child);
         }
         
         private final HashSet<Model> children;
      }
      
      // ----------------------------------------------------
      // MeshInstance holds a list of submodels
      // ----------------------------------------------------
      public static class MeshInstance extends Model {
         public MeshInstance(Mesh2 mesh) { 
            this.mesh = mesh;
         }
         public final Mesh2 mesh;
         
         // we've been thinking we should allow any Model to be "annotated"
         // with any Object->Object key/value pair...
         
         private HashMap<Object,Object> annotations;
         
         public Object getAnnotation(Object key) {
            return annotations.get(key);
         }
         public void setAnnotation(Object key, Object value) {
            annotations.put(key, value);
         }
         public void removeAnnotation(Object key) {
            annotations.remove(key);
         }
      }
      

   }
   
   // ---------------------------------------------------------------
   // ---------------------------------------------------------------
   
   
   
   
}



package com.generic.base;

import com.generic.base.Algebra.*;

import java.util.ArrayList;
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
      
      // -------------------------------
      
      protected void disconnect() {
         scene = null;
      }
      protected void connect(Scene3D scene) {
         this.scene = scene;
      }
      protected Scene3D scene;

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
         
         protected void disconnect() {
            super.disconnect();
            for (Model child : children) {
               child.disconnect();
            }
         }
         protected void connect(Scene3D scene) {
            for (Model child : children) {
               child.connect(scene);
            }
            super.connect(scene);
         }
      }
      
      // ----------------------------------------------------
      // MeshInstance holds a list of submodels
      // ----------------------------------------------------
      public static class MeshInstance extends Model {
         public MeshInstance(Mesh2 mesh) { 
            this.mesh = mesh;
         }
         public final Mesh2 mesh;
         
         // -------------------------------------------------
         // How about a separate type of "Style" object
         // for each shader?
         // -------------------------------------------------
         
         public interface Style {}
         private Style style;
         public void setStyle(Style style) {
            this.style = style;
         }
         public Style getStyle() {
            return style;
         }
         
         public static class FlatBorderedStyle implements Style {
            public final Color faceColor;
            public final Color borderColor;
            
            public FlatBorderedStyle (Color faceColor, Color borderColor) {
               this.faceColor = faceColor;
               this.borderColor = borderColor;
            }
         }
         public static class SmoothStyle implements Style {
            public final Color faceColor;
            
            public SmoothStyle (Color faceColor) {
               this.faceColor = faceColor;
            }
         }
          /*        
         // -------------------------------------------------
         // Or would we rather just expose a set of simple methods like this?
         // -------------------------------------------------
         
         private Color faceColor;
         public void setFaceColor(Color faceColor) {
            this.faceColor = faceColor;
         }
         public Color getFaceColor() { 
            return faceColor; 
         }
         
         private Image textureMap;
         public void setTextureMap(Image textureMap) {
            this.textureMap = textureMap;
         }
         public Image getTextureMap() {
            return textureMap;
         }
         
         private Color borderColor;
         public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
         }
         public Color getBorderColor() { 
            return borderColor; 
         }
         
         private boolean borderVisible;
         public void setBorderVisible(boolean borderVisible) {
            this.borderVisible = borderVisible;
         }
         public boolean getBorderVisible() { 
            return borderVisible; 
         }
         
         private boolean smoothShading;
         public void setSmoothShading(boolean smoothShading) {
            this.smoothShading = smoothShading;
         }
         public boolean getSmoothShading() { 
            return smoothShading; 
         }
         */
      }
   }
   
   // ---------------------------------------------------------------
   // Unlike GL (with its one-level set of VertexBuffers & Samplers)
   // or Mesh2 (with its one-level set of DataLayers)..
   //
   // the Scene3D.Models are a full-fledged hierarchy ..
   //    user can and should be able to construct Groups
   //       (filled with sub Groups and MeshInstances)
   //       before adding the Group to the root Group..
   //       then later the user should be able to detach, and re-attach,
   //       to represent adding or removing items from the scene...
   //
   // another Hierarchy we're likely to have is
   //    the GUI.Window hierarchy.  those two should be similar, perhaps..
   //
   //
   // ---------------------------------------------------------------
   
   
   
   // -----------------------------------------
   public static class VertexBufferManager {
      public final Mesh2.DataLayer sourceData;
      
      VertexBufferManager(Mesh2.DataLayer sourceData) {
         this.sourceData = sourceData;
      }      
   }
   private HashSet<VertexBufferManager> vertexBufferManagers;
   
   private interface VertexBufferProvider {
      public GL.VertexBuffer getVertexBuffer(GL gl);
      public void clearVertexBuffer(GL gl);
   }
   private HashSet<VertexBufferProvider> providers;

   
   // -----------------------------------------
   // okay.. prime example of vertex-buffer-manager:
   //   POSITION buffer generation.
   
   public static class PositionBuffer {
      private final Mesh2.DataLayer dataLayer;
      private final Mesh2.DataLayer.Listener listener;
      
      public PositionBuffer(Mesh2.DataLayer dataLayer) {
         this.dataLayer = dataLayer;
         dataLayer.addListener(new Mesh2.DataLayer.Listener() {
            @Override
            public void modified() {
            }
         });
      }
      private final Data.Array.Floats buffer;
      
      
      
      
      
      
   }

   
   
   
   
   
   
   
   
   
   
   
   private static class ExecutionPlan {
      public interface Step {
         void execute(GL gl);
      }
      private ArrayList<Step> steps;
      
      public void addStep(Step step) {
         steps.add(step);
      }
      public void execute(GL gl) {
         for (Step step : steps) {
            step.execute(gl);
         }
      }
   }
  
   
   public void process (ExecutionPlan planToBuild, Model model) {
      if (model instanceof Model.Group) {
         Model.Group group = (Model.Group) model;
         for (Model child : group.children()) {
            process (planToBuild, child);
         }
         return;
      }
      
      Model.MeshInstance meshModel = (Model.MeshInstance) model;
   }
   
   
   
   public void render(GL gl) {
      
      // 1 for each vertexbuffer that our models will need,
      //   reconstruct the vertexbuffer-data if needed,
      //   and create/update the GL.VertexBuffer object if needed.
      
      // ----------------------------------------------------
      // we could iterate over all models.
      //    so each MeshInstance produces some VertexBufferBuilders..
      // 
      // except we'd like the VertexBufferBuilders to stick around,
      //    so we don't have to rebuild them for next frame..
      // 
      // so there's going to be a set of vertex-buffer-managers.
      //    (eg, the ones that stuck around from last frame),
      //    so iterating over all models
      //    (if we do that)
      //    would only be to locate any *new* VertexBUfferBuilder objects..
      //    that have appeared since last frame...
      // ----------------------------------------------------
      
      // 2 for each sampler that our models will need,
      //   reconstruct the sampler-data if needed
      //   and create/update the GL.Sampler object if needed.
      //
      // 3 for each shader that our models need,
      //   alloc the shader...
      //
      // 4 then for each model,
      //   call the appropriate Shader.invoke functions...
      // 
   }
   
   
}



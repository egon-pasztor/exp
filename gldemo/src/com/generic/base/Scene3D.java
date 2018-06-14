package com.generic.base;

import com.generic.base.Algebra.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Scene3D {

   public Scene3D() {
      root = new Model.Group();
      root.connect(this, null);
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
      // so "getTransformation" transforms this model into the space of its parent,
      // but if this model is not the root, we would have to "walk up the tree"
      // in order to compute the transformation between this model and the root..
      // (What we actually care about in rendering is the transformation between
      // this model and the camera)
      // -------------------------------
      
      protected void disconnect() {
         scene = null;
      }
      protected void connect(Scene3D scene, Model parent) {
         this.scene = scene;
         this.parent = parent;
      }
      public boolean connected() {
         return scene != null;
      }
      protected Scene3D scene;
      protected Model parent;
      
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
            if (child.parent != null) {
               throw new RuntimeException(
                  "Attempt to add child that's already connected.");
            }
            children.add(child);
            if (connected()) {
               child.connect(scene, this);
            }
         }
         public void removeChild(Model child) {
            if (child.parent != this) {
               throw new RuntimeException(
                     "Attempt to remove child that's not connected.");
            }
            if (connected()) {
               child.disconnect();
            }
            children.remove(child);
         }
         
         private final HashSet<Model> children;
         
         protected void disconnect() {
            for (Model child : children) {
               child.disconnect();
            }
            super.disconnect();
         }
         protected void connect(Scene3D scene, Model parent) {
            super.connect(scene, parent);
            for (Model child : children) {
               child.connect(scene, this);
            }
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
         
         private String positionLayerName;
         public void setPositionLayerName(String positionLayerName) {
            this.positionLayerName = positionLayerName;
         }
         public String getPositionLayerName() {
            return positionLayerName;
         }
         
         // -------------------------------------------------
         // How about a separate type of "Style" object
         // for each shader?
         // -------------------------------------------------
         
         public interface Style {}
         private Style style;
         private MeshInstanceRenderer renderer;
         
         public void setStyle(Style style) {
            if (this.style != style) {
               if (renderer != null) {
                  renderer.disconnect();
                  renderer = null;
               }
               this.style = style;
               if (renderer != null) {
                  renderer.disconnect();
                  renderer = null;
               }
            }
         }
         public Style getStyle() {
            return style;
         }
         protected void disconnect() {
            if (renderer != null) {
               renderer.disconnect();
               renderer = null;
            }
            super.disconnect();
         }
         protected void connect(Scene3D scene, Model parent) {
            super.connect(scene, parent);
            if (renderer != null) {
               renderer.disconnect();
               renderer = null;
            }
         }
         
         private MeshInstanceRenderer newRendererForStyle(Style style) {
            if (style instanceof FlatBorderedStyle) {
               
            }
         }
         
         
         public static class FlatBorderedStyle implements Style {
            public Color faceColor;
            public Color borderColor;
            public final float borderThickness;
            public FlatBorderedStyle (float borderThickness) {
               this.borderThickness = borderThickness;
            }
         }
         public static class SmoothStyle implements Style {
            public Color faceColor;            
            public SmoothStyle () {}
         }
      }
   }
   
   
   // ##############################################################
   // ResourceProvider
   // ##############################################################
   
   private static abstract class ResourceProvider {      
      protected ResourceProvider(Scene3D scene) {
         this.scene = scene;
         this.references = new HashSet<Object>();
         scene.providers.add(this);
      }
      
      public final Scene3D scene;
      private final HashSet<Object> references;
      
      public void addReference(Object object) {
         references.add(object);
      }
      public void removeReference(Object object) {
         references.remove(object);
      }
      public void destroy() {
         GL.Resource resource = getResource();
         if (resource != null) resource.destroy();
         scene.providers.remove(this);
      }
      
      public abstract GL.Resource getResource();
      public abstract void clearGL  ();
      public abstract void updateGL (GL gl);
   }
   private HashSet<ResourceProvider> providers = new HashSet<ResourceProvider>();
   

   // -----------------------------------------------
   // Shader Providers
   // -----------------------------------------------
   private static abstract class ShaderProvider extends ResourceProvider {      
      protected ShaderProvider(Scene3D scene) {
         super(scene);
      }
      public abstract GL.Shader getResource();
      public abstract void clearGL  ();
      public abstract void updateGL (GL gl);
   }

   // - - - - - - - - - - - - - - - -
   // 1. SmoothShaderProvider
   // - - - - - - - - - - - - - - - -
   private static class SmoothShaderProvider extends ShaderProvider {
      protected SmoothShaderProvider(Scene3D scene) {
         super(scene);
         scene.smoothShaderProvider = this;
      }
      public void destroy() {
         scene.smoothShaderProvider = null;
         super.destroy();
      }
      protected GL.SmoothShader shader;
      public GL.SmoothShader getResource() { return shader; }      
      public void clearGL () { 
         if (shader != null) {
            shader.destroy();  //??
         }
         shader = null;
      }
      public void updateGL (GL gl) {
         if (shader == null) {
            shader = gl.newSmoothShader();
         }
      }
   }
   private SmoothShaderProvider getSmoothShaderProvider() {
      SmoothShaderProvider result = smoothShaderProvider;
      if (result == null) {
         result = new SmoothShaderProvider(this);
      }
      return result;
   }
   private SmoothShaderProvider smoothShaderProvider = null;   
   
   // - - - - - - - - - - - - - - - -
   // 2. FlatBorderedShaderProvider
   // - - - - - - - - - - - - - - - -
   private static class FlatBorderedShaderProvider extends ShaderProvider {
      protected FlatBorderedShaderProvider(Scene3D scene, float borderThickness) {
         super(scene);
         this.borderThickness = borderThickness; 
         scene.flatBorderedShaderProviders.put(borderThickness, this);
      }
      private final float borderThickness;
      
      public void destroy() {
         scene.flatBorderedShaderProviders.remove(borderThickness);
         super.destroy();
      }
      protected GL.FlatBorderedShader shader;
      public GL.FlatBorderedShader getResource() { return shader; }      
      public void clearGL  () { shader = null; }      
      public void updateGL (GL gl) {
         shader = gl.newFlatBorderedShader(borderThickness);
      }
   }
   private FlatBorderedShaderProvider getFlatBorderedShaderProvider(float borderThickness) {
      FlatBorderedShaderProvider result = flatBorderedShaderProviders.get(borderThickness);
      if (result == null) {
         result = new FlatBorderedShaderProvider(this, borderThickness);
      }
      return result;
   }   
   private HashMap<Float, FlatBorderedShaderProvider> flatBorderedShaderProviders
     = new HashMap<Float, FlatBorderedShaderProvider>();
   
   
   // -----------------------------------------------   
   // VertexBuffer Providers
   // -----------------------------------------------
   public static abstract class VertexBufferProvider extends ResourceProvider {
      public static class Key {
         public final Mesh2.DataLayer layer;
         public final Class<? extends VertexBufferProvider> providerClass;
         
         public Key (Mesh2.DataLayer layer,
                     Class<? extends VertexBufferProvider> providerClass) {
            this.layer = layer;
            this.providerClass = providerClass;
         }
      }
      
      
      protected VertexBufferProvider(Scene3D scene, Mesh2.DataLayer layer, int floatsPerVertex) {
         super(scene);
         this.layer = layer;
         this.layerChangeNotifier = new Mesh2.DataLayer.Listener() {
            public void modified() {
               bufferDataNeedsRecomputing = true;
            }
         };
         layer.addListener(layerChangeNotifier);
         bufferData = new Data.Array.Floats(floatsPerVertex);
         bufferDataNeedsRecomputing = true;
         resourceNeedsUpdate = true;
         
         vertexBuffer = null;
      }
      
      private final Mesh2.DataLayer layer;
      private final Mesh2.DataLayer.Listener layerChangeNotifier;
      
      protected final Data.Array.Floats bufferData;
      private boolean bufferDataNeedsRecomputing;
      private boolean resourceNeedsUpdate;      

      public void destroy() {
         // 
         super.destroy();
      }
      
      protected GL.VertexBuffer vertexBuffer;
      public GL.VertexBuffer getResource() { return vertexBuffer; }
      
      public void clearGL () {}
      public void updateGL (GL gl) {}
   }


   // - - - - - - - - - - - - - - - -
   // 2. DataLayer processing..
   // - - - - - - - - - - - - - - - -
   public static class DataLayerTransformer extends VertexBufferProvider {
      public DataLayerTransformer(Scene3D scene, Mesh2.DataLayer layer) {
         super(scene, layer, 4);
      }
   }
   
   
   
   
   
   
   
   
   
   // -----------------------------------------------   
   // So... perhaps each MeshInstance will have a ShaderInvocator
   // -----------------------------------------------   
   
   private abstract static class MeshInstanceRenderer {
      protected final Scene3D scene;
      protected final Model.MeshInstance model;
      
      protected MeshInstanceRenderer(Scene3D scene, Model.MeshInstance model) {
         this.scene = scene;
         this.model = model;
      }      
      public abstract void render();
      public abstract void disconnect();      
   }
   
   public static class FlatBorderedRenderer extends MeshInstanceRenderer {
      FlatBorderedRenderer(Scene3D scene, Model.MeshInstance model) {
         super(scene, model);
      }
      
      private GL.FlatBorderedShader shader;
      private int numTriangles;      
      private Matrix4x4 modelToView;
      private Matrix4x4 viewToClip;
      private Color faceColor;
      private Color borderColor;
      private GL.VertexBuffer positions;
      private GL.VertexBuffer normals;
      private GL.VertexBuffer baryCoords;
      
      public void render() {
         shader.setModelToView(modelToView);
         shader.setViewToClip(viewToClip);
         shader.setFaceColor(faceColor);
         shader.setBorderColor(borderColor);
         shader.setPositions(positions);
         shader.setNormals(normals);
         shader.setBaryCoords(baryCoords);
         shader.shadeTriangles(numTriangles);
      }
      public void disconnect() {}
   }
   
   public static class SmoothRenderer extends MeshInstanceRenderer {
      SmoothRenderer(Scene3D scene, Model.MeshInstance model) {
         super(scene, model);
      }
      
      private GL.SmoothShader shader;
      private int numTriangles;
      
      private Matrix4x4 modelToView;
      private Matrix4x4 viewToClip;
      private Color faceColor;
      private GL.VertexBuffer positions;
      private GL.VertexBuffer normals;
      
      public void render() {
         shader.setModelToView(modelToView);
         shader.setViewToClip(viewToClip);
         shader.setFaceColor(faceColor);
         shader.setPositions(positions);
         shader.setNormals(normals);
         shader.shadeTriangles(numTriangles);
      }
      public void disconnect() {}
   }
   
   
   // -----------------------------------------
   // okay.. prime example of vertex-buffer-manager:
   //   POSITION buffer generation.
   // -----------------------------------------
   public static class PositionBuffer {
      
      private final Mesh2.DataLayer dataLayer;
      private final Mesh2.DataLayer.Listener listener;
      
      public PositionBuffer(Mesh2.DataLayer dataLayer) {
         this.dataLayer = dataLayer;         
         this.rebuildNeeded = true;
         // NOTES: 1.  annoying we have to keep <listener> just so we can delete it..
         listener = new Mesh2.DataLayer.Listener() {
            @Override
            public void modified() {
               rebuildNeeded = true;
            }
         };
         dataLayer.addListener(listener);
      }
      public void destroy() {
         dataLayer.removeListener(listener);
      }      
      private boolean rebuildNeeded;
      private Data.Array.Floats positions;
      
      // -------------------------------------------------
      // we're assuming PositionBuffer will need this "referenceCount" thing..
      // -------------------------------------------------
      public interface Reference {
         public void setVertexBuffer(GL.VertexBuffer buffer);
      }
      private ArrayList<Reference> references;
      public void clearReferences() {
         references.clear();
      }
      public void addReference(Reference ref) {
         references.add(ref);
      }
      
      // -------------------------------------------------      
      private HashMap<GL, GL.VertexBuffer> buffers;      
   }

   private HashMap<Mesh2.DataLayer, PositionBuffer> positionBuffers;
   
   public PositionBuffer getOrCreatePositionBuffer(Mesh2.DataLayer dataLayer) {
      PositionBuffer result = null;
      if (positionBuffers.containsKey(dataLayer)) {
         result = positionBuffers.get(dataLayer);
      } else {
         result = new PositionBuffer(dataLayer);
         positionBuffers.put(dataLayer, result);
      }
      return result;
   }
   

   // okay.. again...
   
   
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
   
   public void traverse (Model model, 
                         Matrix4x4 projMatrix, 
                         Matrix4x4 viewMatrix, 
                         ExecutionPlan accumulator) {
      
      viewMatrix = Matrix4x4.product(viewMatrix, model.getTransformation());
      
      if (model instanceof Model.Group) {
         Model.Group group = (Model.Group) model;
         for (Model child : group.children()) {
            traverse (child, projMatrix, viewMatrix, accumulator);
         }
         return;
      }
      
      Model.MeshInstance meshModel = (Model.MeshInstance) model;
      
      // -----------------------------------
      // locate position-vector-generator
      // -----------------------------------
      // While some models might not need Position in the future,
      // right now every model needs Position.  It's based on a DataLayer:
      
      { Mesh2.DataLayer dataLayer = meshModel.mesh.dataLayer(
            meshModel.getPositionLayerName(), 
            Mesh2.DataLayer.Type.THREE_FLOATS_PER_VERTEX);
      
        if (dataLayer == null) {
           // Mesh doesn't have the requested position-layer
           return;
        }
        
        // Okay the Mesh DOES have the position-layer,
        // this will either lookup the PositionBuffer object, or else create it.
        PositionBuffer positionBuffer =
              getOrCreatePositionBuffer(dataLayer);
        
      }
      
      // ------------------------------------------      
      // Now then... What shader is this model using?
      // ------------------------------------------
      if (meshModel.style instanceof Model.MeshInstance.SmoothStyle) {
         // And this model is using SmoothStyle.
         
         
      }
      if (meshModel.style instanceof Model.MeshInstance.FlatBorderedStyle) {
         // And this model is using FlatBorderedStyle.
         
         
      }
      
   }
   
   
   public void render(Camera camera, GL gl) {
      
      // 1. Set all "gl resources" reference-count to zero.
      //    TODO: this will require clearing All VertexBuffers,
      //                                 and All Shaders,
      //                                 and All Samplers...
      //    but for now we're doing this with *just* "position" VertexBuffers:
      for (PositionBuffer positionBuffer : positionBuffers.values()) {
         positionBuffer.clearReferences();
      }
      
      // 2. Traverse models in a process that prepares an execution-plan,
      //    but also calls getOrCreate for all needed "gl resources".
      ExecutionPlan plan = new ExecutionPlan();
      traverse (root, camera.cameraToClipSpace, camera.worldToCameraSpace, plan);
      
      // 3. All "gl-resources" whose reference count is STILL zero should
      //    be destroyed.
      //
      // 4. All "gl-resources" remaining should (rebuild if needed)
      //    allocate gl-specific object.
      
      // 5. "Sort" execution-plan steps by shader, perhaps..
      //    and secondly by position ..??
      //
      // 6. "Execute" the execution-plan's steps..
      //    Each step will need access to some "gl-resources"..
      //
      // 
   }
   
   
}



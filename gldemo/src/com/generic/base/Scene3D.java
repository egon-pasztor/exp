package com.generic.base;

import com.generic.base.Algebra.*;
import com.generic.base.Data.Array.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

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
         // Style
         // -------------------------------------------------
         
         public interface Style {}
         
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
         
         // -------------------------------------------------
         private Style style;
         private MeshInstanceRenderer renderer;
         
         public Style getStyle() {
            return style;
         }
         public void setStyle(Style style) {
            if (this.style != style) {
               if (connected()) {
                  disconnectRenderer();
               }
               this.style = style;
               if (connected()) {
                  connectRencderer();
               }
            }
         }
         
         protected void disconnect() {
            disconnectRenderer();
            super.disconnect();
         }
         protected void connect(Scene3D scene, Model parent) {
            super.connect(scene, parent);
            connectRencderer();
         }
         private void disconnectRenderer() {
            if (renderer != null) {
               renderer.disconnect();
               renderer = null;
            }
         }
         private void connectRencderer() {
            if (style instanceof FlatBorderedStyle) {
               renderer = new FlatBorderedRenderer(scene, this);
            }
            if (style instanceof SmoothStyle) {
               renderer = new SmoothRenderer(scene, this);
            }
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
         scene.resourceProviders.add(this);
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
         scene.resourceProviders.remove(this);
      }
      
      public abstract GL.Resource getResource();
      public abstract void clearGL  ();
      public abstract void updateGL (GL gl);
   }
   private HashSet<ResourceProvider> resourceProviders = new HashSet<ResourceProvider>();
   

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
      // ----------------------------------      
      protected GL.SmoothShader shader;
      public GL.SmoothShader getResource() { return shader; }
      // ----------------------------------      

      protected SmoothShaderProvider(Scene3D scene) {
         // Constructor is called to connect us to the scene,
         // and initialize the resource to NULL
         super(scene);
         scene.smoothShaderProvider = this;
         shader = null;
      }
      public void destroy() {
         // Destroy is called when we need to disconnect.
         // (super.destroy calls destroy on the resource, if non-null)
         scene.smoothShaderProvider = null;
         super.destroy();
      }
      public void clearGL () {
         // ClearGL is called when GL has LOST all its resources,
         // so we need to forget them on our end...
         shader = null;
      }
      public void updateGL (GL gl) {
         // UpdateGL is called when we need to make sure our
         // resource is ready for use
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
      // ----------------------------------      
      private final float borderThickness;
      protected GL.FlatBorderedShader shader;
      public GL.FlatBorderedShader getResource() { return shader; }      
      // ----------------------------------      
      
      protected FlatBorderedShaderProvider(Scene3D scene, float borderThickness) {
         // Constructor is called to connect us to the scene,
         // and initialize the resource to NULL
         super(scene);
         this.borderThickness = borderThickness; 
         scene.flatBorderedShaderProviders.put(borderThickness, this);
         shader = null;
      }
      public void destroy() {
         // Destroy is called when we need to disconnect.
         // (super.destroy calls destroy on the resource, if non-null)
         scene.flatBorderedShaderProviders.remove(borderThickness);
         super.destroy();
      }
      public void clearGL () {
         // ClearGL is called when GL has LOST all its resources,
         // so we need to forget them on our end...         
         shader = null; 
      }
      public void updateGL (GL gl) {
         // UpdateGL is called when we need to make sure our
         // resource is ready for use
         if (shader == null) {
           shader = gl.newFlatBorderedShader(borderThickness);
         }
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
      
      // ----------------------------------
      // KEY
      // ----------------------------------
      public static class Key {
         public final Mesh2.DataLayer layer;
         public final Class<? extends VertexBufferProvider> providerClass;
         
         public Key (Mesh2.DataLayer layer,
                     Class<? extends VertexBufferProvider> providerClass) {
            this.layer = layer;
            this.providerClass = providerClass;
         }
         public int hashCode() {
            return Objects.hash(layer, providerClass);
         }
         public boolean equals(Key o) {
            return (layer == o.layer)
                && (providerClass == o.providerClass);
         }
         public boolean equals(Object o) {
            return (o != null) && (o instanceof Key) && equals((Key)o);
         }
      }
      
      // ----------------------------------
      private final Key key;
      private final Mesh2.DataLayer.Listener layerChangeNotifier;
      
      protected final Data.Array.Floats bufferData;
      private boolean bufferDataNeedsRecomputing;
      private boolean resourceNeedsUpdate;      

      protected GL.VertexBuffer vertexBuffer;
      public GL.VertexBuffer getResource() { return vertexBuffer; }
      // ----------------------------------      
      
      protected VertexBufferProvider(Scene3D scene, Mesh2.DataLayer layer, int floatsPerVertex) {
         // Constructor is called to connect us to the scene,
         // and initialize the resource to NULL
         super(scene);
         this.key = new Key(layer, getClass());
         this.layerChangeNotifier = new Mesh2.DataLayer.Listener() {
            public void modified() {
               bufferDataNeedsRecomputing = true;
            }
         };
         layer.addListener(layerChangeNotifier);
         bufferData = new Data.Array.Floats(floatsPerVertex);
         bufferDataNeedsRecomputing = true;
         
         scene.vertexBufferProviders.put(key, this);
         vertexBuffer = null;
      }      
      public void destroy() {
         // Destroy is called when we need to disconnect.
         // (super.destroy calls destroy on the resource, if non-null)
         key.layer.removeListener(layerChangeNotifier);
         scene.vertexBufferProviders.remove(key);         
         super.destroy();
      }
      public void clearGL () {
         // ClearGL is called when GL has LOST all its resources,
         // so we need to forget them on our end...
         vertexBuffer = null;         
      }
      public void updateGL (GL gl) {
         // UpdateGL is called when we need to make sure our
         // resource is ready for use.
         
         if (bufferDataNeedsRecomputing) {
            rebuildBufferData();
            bufferDataNeedsRecomputing = false;
            
            if (vertexBuffer != null) {
               vertexBuffer.update(bufferData);
            }
         }
         if (vertexBuffer == null) {
           vertexBuffer = gl.newVertexBuffer(bufferData);
         }
      }
      
      public abstract void rebuildBufferData();
   }

   private HashMap<VertexBufferProvider.Key, VertexBufferProvider> vertexBufferProviders
     = new HashMap<VertexBufferProvider.Key, VertexBufferProvider>();

   // - - - - - - - - - - - - - - - -
   // 2. DataLayer processing..
   // - - - - - - - - - - - - - - - -
   public static class DataLayerTransformer extends VertexBufferProvider {
      public DataLayerTransformer(Scene3D scene, Mesh2.DataLayer layer) {
         super(scene, layer, 4);
      }
   }
   

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
   private HashMap<Mesh2.DataLayer, PositionBuffer> positionBuffers;
   
   
   
   
   
   
   
   
   // -----------------------------------------------   
   // Each MeshInstance connected to this Scene has a MeshInstanceRenderer 
   // -----------------------------------------------      
   private abstract static class MeshInstanceRenderer {
      protected final Scene3D scene;
      protected final Model.MeshInstance model;
      
      protected MeshInstanceRenderer(Scene3D scene, Model.MeshInstance model) {
         this.scene = scene;
         this.model = model;
         scene.renderers.add(this);
      }      
      public abstract void render();
      public abstract void disconnect();      
   }
   private HashSet<MeshInstanceRenderer> renderers = new HashSet<MeshInstanceRenderer>();
   
   
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
   
   
   
   // ======================================================
   // RENDER
   // ======================================================
   
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
   
   private void traverse (Model model, 
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
      
      // We expect "renderers" to be set-up correctly,
      // as "renderers" are added or removed when the user edits the tree.
      //
      // However "resourceProviders"
      
      
      1. Set all "gl resources" reference-count to zero.
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



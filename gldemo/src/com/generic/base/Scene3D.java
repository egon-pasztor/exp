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
      public boolean hasReferences() {
         return !references.isEmpty();
      }
      public void destroy() {
         Graphics3D.Resource resource = getResource();
         if (resource != null) resource.destroy();
         scene.resourceProviders.remove(this);
      }
      
      public abstract Graphics3D.Resource getResource();
      public abstract void clearGL  ();
      public abstract void updateGL (Graphics3D gl);
   }
   private HashSet<ResourceProvider> resourceProviders = new HashSet<ResourceProvider>();
   

   // -----------------------------------------------
   // Shader Providers
   // -----------------------------------------------
   private static abstract class ShaderProvider extends ResourceProvider {      
      protected ShaderProvider(Scene3D scene) {
         super(scene);
      }
      public abstract Graphics3D.Shader getResource();
      public abstract void clearGL  ();
      public abstract void updateGL (Graphics3D gl);
   }

   // - - - - - - - - - - - - - - - -
   // 1. SmoothShaderProvider
   // - - - - - - - - - - - - - - - -
   private static class SmoothShaderProvider extends ShaderProvider {
      // ----------------------------------      
      protected Graphics3D.SmoothShader shader;
      public Graphics3D.SmoothShader getResource() { return shader; }
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
      public void updateGL (Graphics3D gl) {
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
      protected Graphics3D.FlatBorderedShader shader;
      public Graphics3D.FlatBorderedShader getResource() { return shader; }      
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
      public void updateGL (Graphics3D gl) {
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

      protected Graphics3D.VertexBuffer vertexBuffer;
      public Graphics3D.VertexBuffer getResource() { return vertexBuffer; }
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
      public void updateGL (Graphics3D gl) {
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
   // POSITION-BUFFERS
   // - - - - - - - - - - - - - - - -
   public static class PositionVertexBufferProvider extends VertexBufferProvider {
      public PositionVertexBufferProvider(Scene3D scene, Mesh2.DataLayer layer) {
         super(scene, layer, 4);
      }
      public void rebuildBufferData() {
         // 
         // TODO.. build buffer data..
         // 
      }
   }
   public VertexBufferProvider getOrCreatePositionBuffer(Mesh2.DataLayer dataLayer) {
      VertexBufferProvider.Key key = new VertexBufferProvider.Key(dataLayer, PositionVertexBufferProvider.class);
      VertexBufferProvider result = vertexBufferProviders.get(key);
      if (result == null) {
         result = new PositionVertexBufferProvider(this, dataLayer);
      }      
      return result;
   }
   
   // - - - - - - - - - - - - - - - -
   // NORMAL-BUFFERS
   // - - - - - - - - - - - - - - - -
   public static class NormalVertexBufferProvider extends VertexBufferProvider {
      public NormalVertexBufferProvider(Scene3D scene, Mesh2.DataLayer layer) {
         super(scene, layer, 3);
      }
      public void rebuildBufferData() {
         // 
         // TODO.. build buffer data..
         // 
      }
   }
   public VertexBufferProvider getOrCreateNormalBuffer(Mesh2.DataLayer dataLayer) {
      VertexBufferProvider.Key key = new VertexBufferProvider.Key(dataLayer, NormalVertexBufferProvider.class);
      VertexBufferProvider result = vertexBufferProviders.get(key);
      if (result == null) {
         result = new PositionVertexBufferProvider(this, dataLayer);
      }      
      return result;
   }
   
   
   
   
   
   
   
   
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
      
      private Graphics3D.FlatBorderedShader shader;
      private int numTriangles;      
      private Matrix4x4 modelToView;
      private Matrix4x4 viewToClip;
      private Color faceColor;
      private Color borderColor;
      private Graphics3D.VertexBuffer positions;
      private Graphics3D.VertexBuffer normals;
      private Graphics3D.VertexBuffer baryCoords;
      
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
      
      private Graphics3D.SmoothShader shader;
      private int numTriangles;
      
      private Matrix4x4 modelToView;
      private Matrix4x4 viewToClip;
      private Color faceColor;
      private Graphics3D.VertexBuffer positions;
      private Graphics3D.VertexBuffer normals;
      
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
   
   private void setupRenderPositions (Model model, 
                                      Matrix4x4 projMatrix, 
                                      Matrix4x4 viewMatrix) {

      viewMatrix = Matrix4x4.product(viewMatrix, model.getTransformation());
      
      if (model instanceof Model.Group) {
         Model.Group group = (Model.Group) model;
         for (Model child : group.children()) {
            setupRenderPositions (child, projMatrix, viewMatrix);
         }
      }
      if (model instanceof Model.MeshInstance) {
         Model.MeshInstance meshModel = (Model.MeshInstance) model;
         
         // I'm still "indecisive" about what exactly the Renderer represents.
         // Does it need virtual methods?
         
         if (meshModel.renderer instanceof SmoothRenderer) {
            SmoothRenderer renderer = (SmoothRenderer) meshModel.renderer;
            
            renderer.numTriangles = meshModel.mesh.numFaces();
            renderer.modelToView = viewMatrix;
            renderer.viewToClip = projMatrix;
            renderer.faceColor = ((Model.MeshInstance.SmoothStyle) meshModel.style).faceColor;
         }
         if (meshModel.renderer instanceof FlatBorderedRenderer) {
            FlatBorderedRenderer renderer = (FlatBorderedRenderer) meshModel.renderer;
            
            renderer.numTriangles = meshModel.mesh.numFaces();
            renderer.modelToView = viewMatrix;
            renderer.viewToClip = projMatrix;
            renderer.faceColor = ((Model.MeshInstance.FlatBorderedStyle) meshModel.style).faceColor;
            renderer.borderColor = ((Model.MeshInstance.FlatBorderedStyle) meshModel.style).borderColor;
         }
      }
   }
   
   public void render(Camera camera, Graphics3D gl) {
      
      // We expect "renderers" to be set-up correctly,
      // as "renderers" are added or removed when the user edits the tree.
      //
      // However "resourceProviders" might exist with no references.
      // These are resource-providers that may have existed at the last frame
      // and may still have GL resources allocated, but we don't want to delete
      // GL resources until .. well, now, here, in render.
      //
      // The remaining resources, which HAVE references, need to be
      // updated.  The update operation causes the vertexBuffers to be rebuilt
      // if needed and the GL resources actually get allocated:
      //
      HashSet<ResourceProvider> unusedResourceProviders = new HashSet<ResourceProvider>();
      for (ResourceProvider resourceProvider : resourceProviders) {
         if (!resourceProvider.hasReferences()) {
            resourceProvider.destroy();
         } else {
            resourceProvider.updateGL (gl);
         }
      }
      
      // Now we do an actual tree traversal in order to multiply out all the
      // view matrices and save all the "uniforms":
      setupRenderPositions (root, camera.cameraToClipSpace, camera.worldToCameraSpace);

      // Finally, the actual rendering just needs a pass over each
      // renderer object...
      for (MeshInstanceRenderer renderer : renderers) {
         renderer.render();
      }
   }   
}



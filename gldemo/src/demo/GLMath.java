package demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import demo.VectorAlgebra.*;
import demo.Raster.*;

import demo.Geometry.*;
import demo.VectorAlgebra.*;
import demo.Raster.*;



public class GLMath {

   // -----------------------------------------------------------------------
   // CameraController
   // -----------------------------------------------------------------------
  
   public static class CameraController {
      
      public CameraController (Camera camera) {
         this.camera = camera;
      }
      
      public Camera getCamera() {
         return camera;
      }
      
      private Camera camera;
      
      // -------------------------------------------------------------------
      // Camera "Click-and-Drag" code
      // -------------------------------------------------------------------
      
      public enum GrabType { Rotate,   // Move camera around fixed lookat_point
                             Zoom,     // Move camera closer or further from fixed lookat_point
                             Pan,      // Move both camera and lookat_point by the same amount
                             FOV };
         
      private boolean grabbed;
      private GrabType grabType;
      
      // Some of these fields are set when a grab begins,
      // and are used to update the Camera after each mouse movement:
      
      private Vector2f grabWindowPosition;

      private Vector3f grabLookAtPoint, grabLookAtToCamera;
      private Vector3f grabCamX, grabCamY, grabCamZ;
      private float grabVerticalFov;
      
      private boolean roll;
      private float grabAngle;
      private float grabFovTangent;
      private float tScale;
      private float windowScale;
      

      private Vector2f getWindowPosition(int ix, int iy) {
         int width = camera.getWidth();
         int height = camera.getHeight();
         
         float y  = (height/2-iy) / ((float) (height/2));
         float x  = (ix-width/2)  / ((float) (height/2));
         return new Vector2f(x,y);
      }
      
      public void grab(int ix, int iy, GrabType grabType) {
         this.grabbed  = true;
         this.grabType = grabType;
         
         grabWindowPosition = getWindowPosition(ix,iy);
         
         grabLookAtPoint    = camera.getLookAtPoint();
         grabLookAtToCamera = camera.getCameraPosition().minus(grabLookAtPoint);
         grabCamX           = camera.getCamX();
         grabCamY           = camera.getCamY();
         grabCamZ           = camera.getCamZ();
         grabVerticalFov    = camera.getVerticalFOV();
         grabFovTangent     = ((float) Math.tan(grabVerticalFov * (Math.PI / 180.0) * 0.5f));

         if (grabType == GrabType.Rotate) {
            roll = grabWindowPosition.lengthSq() > 1;
            if (roll) grabAngle = (float) Math.atan2(grabWindowPosition.y, grabWindowPosition.x);
            
         } else if (grabType == GrabType.Zoom) {
            tScale = (grabWindowPosition.y < 0.5) ? (1-grabWindowPosition.y) : (grabWindowPosition.y);

         } else if (grabType == GrabType.FOV) {
            tScale = (grabWindowPosition.y < 0.5) ? (1-grabWindowPosition.y) : (grabWindowPosition.y);

         } else if (grabType == GrabType.Pan) {
            windowScale = grabFovTangent * grabLookAtToCamera.length();
         }
      }
      public void moveTo(int ix, int iy) {
         if (!grabbed) return;
         
         Vector2f windowPosition = getWindowPosition(ix,iy);
         Vector2f delta = windowPosition.minus(grabWindowPosition);
         
         Vector3f newCameraLookAtPoint = grabLookAtPoint;
         Vector3f newCameraPosition    = grabLookAtToCamera.plus(grabLookAtPoint);
         Vector3f newCameraUpVector    = grabCamY;
         float newCameraVerticalFOV    = grabVerticalFov;
         
         if (grabType == GrabType.Rotate) {
            
            if ((delta.x == 0) && (delta.y == 0)) {               
               newCameraPosition = grabLookAtToCamera.plus(grabLookAtPoint);
               newCameraUpVector = grabCamY;
               
            } else {
               Vector3f cameraSpaceAxis;
               float angle;
   
               if (roll) {
                  cameraSpaceAxis = Vector3f.Z; 
                  angle = - (float) (Math.atan2(windowPosition.y, windowPosition.x) - grabAngle) * Scale2DRotation;
               } else { 
                  cameraSpaceAxis = new Vector3f ((float) - delta.y,(float) delta.x, 0).normalized();
                  angle = - ((float) delta.length()) * Scale3DRotation;
               }
               Vector3f rotationAxis = grabCamX.times(cameraSpaceAxis.x)
                                 .plus(grabCamY.times(cameraSpaceAxis.y))
                                 .plus(grabCamZ.times(cameraSpaceAxis.z))
                                 .normalized();
               
               newCameraPosition = grabLookAtToCamera.rotated(rotationAxis, angle).plus(grabLookAtPoint);
               newCameraUpVector = grabCamY.rotated(rotationAxis, angle);
            }
            
         } else if (grabType == GrabType.Pan) {

            Vector3f translation = grabCamX.times(-delta.x)
                             .plus(grabCamY.times(-delta.y))
                             .times(windowScale);

            newCameraLookAtPoint = grabLookAtPoint.plus(translation);
            newCameraPosition = grabLookAtToCamera.plus(newCameraLookAtPoint);
            
         } else {            
            // Either "ZOOM" or "FOV", two ways or making the target look bigger or smaller,
            // either by moving the camera closer to the target or by changing the field of view:
            
            float scale = (float) Math.pow(ScaleZoom, delta.y/tScale);            
            if (grabType == GrabType.Zoom) {
               newCameraPosition = grabLookAtToCamera.times(scale).plus(grabLookAtPoint);
            } else {
               float newFovTangent = grabFovTangent * scale;
               newCameraVerticalFOV = (float) (2.0f * (180.0f / Math.PI) * Math.atan(newFovTangent));
            }
         }
         
         camera.setupCamera(newCameraLookAtPoint, newCameraPosition, newCameraUpVector, newCameraVerticalFOV);
      }
      
      public void release() {
         grabbed = false;
      }
      public boolean isGrabbed() {
         return grabbed;
      }
      
      private static final float ScaleZoom       = 3.0f;
      private static final float Scale2DRotation = 1.5f;
      private static final float Scale3DRotation = 2.0f;
   }
   
   // #######################################################################
   // WORLD-BUILDING
   // #######################################################################

   public interface Shader {}
   public interface Model {}
   
   // -----------------------------------------------------------------------
   // GEOMETRY
   // -----------------------------------------------------------------------

   public static class Geometry {      
      public static class VertexInfo {
         public Vector3f position;
         public Vector3f normal;
         public Vector2f tex;
      }
      public static class TriangleInfo {
         public Vector3f normal;
         public Vector2f t1,t2,t3;
         public float t1W,t2W,t3W;
         public float ext;
      }

      public final String name;
      public final Mesh<VertexInfo,TriangleInfo> mesh;
         
      public Geometry(String name, Mesh<VertexInfo,TriangleInfo> mesh) {
         this.name = name;
         this.mesh = mesh;
      }
   }

   public static class MeshModel implements Model {      
      public static class Piece {
         public final Geometry geometry;
         public final Shader shader;
         
         public Piece(Geometry geometry, Shader shader) {
            this.geometry = geometry;
            this.shader = shader;
         }
      }
      
      public final ArrayList<Piece> pieces;
      
      public MeshModel() {
         pieces = new ArrayList<Piece>();
      }
   }
   
   
   // --------------------------------------------
   // Geometry "builders"
   // --------------------------------------------

   // We might have separate methods to create types of geometries...
   // Such as cube/ico/cylinder/sphere/bezier...
   //
   // Except these shapes -- especially cylinder and cube, maybe sphere --
   // can consist of multiple Geometry pieces that might want different textures...
   
   
   public Geometry bezierPatchMaker(Vector3f[] sixteenCtrlPoints, float minCurvature, Shader s) {
      return null;
   }
   
   public Geometry cubeModelMaker(Shader sides[]) {
       return null;
   }
   
   public Geometry sphereModelMaker(int divisions,
                                 Shader shader) {
       return null;
   }
   
   
   public static class SphereMaker {
      public SphereMaker(int subdivisions) {
         
      }
      public MeshModel makeSphere(Shader topShader, Shader sideShader, Shader bottomShader) {
         // TODO...
         return null;
      }
      
      Geometry top, sides, bottom;
   }
   
   public static class CylinderMaker {
      public CylinderMaker(int subdivisions) {
         
      }
      public MeshModel makeCylinder(Shader topShader, Shader sideShader, Shader bottomShader) {
         // TODO...
         return null;
      }
      
      Geometry top, sides, bottom;
   }
   
   

   
   public static class CompoundModel implements Model {
      public Matrix4f transformation;
      public final ArrayList<Model> models;
      

      public CompoundModel() {
         transformation = Matrix4f.IDENTITY;
         models = new ArrayList<Model>();
      }
   }
   
   
   // -----------------------------------------------------------------------
   // RENDERING
   // -----------------------------------------------------------------------
   
   // In principle, a "Model" can be rendered...
   // to render a "Model":
   //
   // [1] Traverse it .. starting with a root Transformation:
   //     for each CompoundModel multiply the transformations...
   //     for each "leaf" MeshModel you get a list of MeshModel.Piece
   //          which contains a Shader and a Geometry to bind with our Transformation
   //
   //     SIMPLY -- the Shader can set itself up, apply transformation, bind its geometry, and render.
   //
   //     ALTERNATIVELY -- we can process the model and count all the Shaders,
   //                        so FOR each Shader, we have 1-or-more Geometries, each with 1-or-more Transformation it occurs in
   //
   //                      
   //                          
   
   
   
   
   // -----------------------------------------------------------------------
   // Triangle
   // -----------------------------------------------------------------------
   
   public static class TexInfo {

       public Vector2f  t1,t2,t3;
       public float ext;
       public float t1w,t2w,t3w;
       public ColorARGB c1,c2,c3;

       public TexInfo() {
          t1=t2=t3=null;
          t1w=t2w=t3w=1.0f;
          c1=c2=c3=null;
       }
       
       public void setTexCoords(Vector2f t1, float t1w, Vector2f t2,float t2w, Vector2f t3, float t3w, float ext) {
          this.t1 = t1;
          this.t2 = t2;
          this.t3 = t3;
          this.t1w = t1w;
          this.t2w = t2w;
          this.t3w = t3w;
          this.ext = ext;
       }         
       public void setColors(ColorARGB c1, ColorARGB c2, ColorARGB c3) {
           this.c1 = c1;
           this.c2 = c2;
           this.c3 = c3;
       }
   }

   // -----------------------------------------------------------------------
   // Model
   // -----------------------------------------------------------------------

   public static class Model2 {

      public Mesh<Vector3f,TexInfo> mesh;
      
      public Model2() {
         mesh = new Mesh<Vector3f,TexInfo>();
      }

      public int numTriangles() {
           return mesh.interiorTriangles.size();
      }


      public Mesh.Vertex<Vector3f,TexInfo> getOrAddVertex(Vector3f position) {
         for (Mesh.Vertex<Vector3f,TexInfo> v : mesh.vertices) {
            Vector3f vPosition = v.getData();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         Mesh.Vertex<Vector3f,TexInfo> v = new Mesh.Vertex<Vector3f,TexInfo>();
         v.setData(position);
         mesh.vertices.add(v);
         return v;
      }
      
       // -- -- -- -- -- -- -- --

       public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Vector2f aTex,
                               float aTexW, Vector2f bTex, float bTexW, Vector2f cTex, float cTexW) {
          addTriangle (a,b,c, aTex,aTexW, bTex,bTexW, cTex,cTexW, 0.0f);
       }
       public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Vector2f aTex,
                                float aTexW, Vector2f bTex, float bTexW, Vector2f cTex, float cTexW, float ext) {

          System.out.format("Adding triangle: POS ==\n%s--\n%s--\n%s\nTEX ==\n%s--\n%s--\n%s######\n",
                a.toString(), b.toString(), c.toString(),
                aTex.toString(), bTex.toString(), cTex.toString());
                
          Mesh.Vertex<Vector3f,TexInfo> va = getOrAddVertex(a);
          Mesh.Vertex<Vector3f,TexInfo> vb = getOrAddVertex(b);
          Mesh.Vertex<Vector3f,TexInfo> vc = getOrAddVertex(c);
          Mesh.Triangle<Vector3f,TexInfo> t = mesh.addTriangle(va, vb, vc);
          
          TexInfo ti = new TexInfo();
          ti.setTexCoords(aTex,aTexW, bTex, bTexW, cTex,cTexW, ext);
          
          ColorARGB col = new ColorARGB((byte)0x00, (byte)0xff, (byte)0x00, (byte)0x00);
          
          ti.setColors(col,col,col);
          t.setData(ti);
             
          System.out.format("MESH has %d vertices, %d interior triangles and %d boundary edges\n", mesh.vertices.size(),
                mesh.interiorTriangles.size(), mesh.boundaryTriangles.size());
          mesh.checkMesh();
       }
       
       public void addSquare (Vector3f center, Vector3f dx, Vector3f dy, float ext) {
           Vector3f tr = center.plus(dx).plus(dy);
           Vector3f tl = center.minus(dx).plus(dy);
           Vector3f br = center.plus(dx).minus(dy);
           Vector3f bl = center.minus(dx).minus(dy);

           addTriangle(bl, br, tl, 
                 new Vector2f(0.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(0.0f, 0.0f), 1.0f, ext);
                
           addTriangle(tl, br, tr,
                 new Vector2f(0.0f, 0.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 0.0f), 1.0f, ext);
       }
       

       // -- -- -- -- -- -- -- --

       public static class Arrays {
           float[] positions;
           float[] texCoords;
           float[] baryCoords;
           float[] colors;
       }

       public Arrays getArrays() {
           Arrays result = new Arrays();
           int n = numTriangles();

           result.positions  = new float[n*3*4];
           result.baryCoords = new float[n*3*2];
           result.texCoords  = new float[n*3*4];
           result.colors     = new float[n*3*4];
           
           int pPos = 0;
           int pBary = 0;
           int pTex = 0;
           int pCol = 0;
           
           for (Mesh.Triangle<Vector3f,TexInfo> t : mesh.interiorTriangles) {
              Vector3f v0Pos = t.edge0.getOppositeVertex().getData();
              Vector3f v1Pos = t.edge1.getOppositeVertex().getData();
              Vector3f v2Pos = t.edge2.getOppositeVertex().getData();
              TexInfo ti = t.getData();

              // pos
              pPos = copyVector3fAs4(result.positions, pPos, v0Pos);
              pPos = copyVector3fAs4(result.positions, pPos, v1Pos);
              pPos = copyVector3fAs4(result.positions, pPos, v2Pos);
              
              // bary
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(0.0f, 0.0f));
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(0.0f, 1.0f));
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(1.0f, 1.0f));
              
              // tex
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t1, ti.t1w, ti.ext);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t2, ti.t2w, ti.ext);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t3, ti.t3w, ti.ext);
              
              // col
              pCol = copyColor(result.colors, pCol, ti.c1);
              pCol = copyColor(result.colors, pCol, ti.c2);
              pCol = copyColor(result.colors, pCol, ti.c3);
           }
           return result;
       }

       private int copyVector3fAs4(float[] arr, int base, Vector3f v) {
           arr[base+0] = v.x;
           arr[base+1] = v.y;
           arr[base+2] = v.z;
           arr[base+3] = 1.0f;
           return base+4;
       }
       private int copyVector2f(float[] arr, int base, Vector2f v) {
           arr[base+0] = v.x;
           arr[base+1] = v.y;
           return base+2;
       }
       private int copyVector2fAs4(float[] arr, int base, Vector2f v, float w, float ext) {
          arr[base+0] = v.x * w;
          arr[base+1] = v.y;
          arr[base+2] = ext;
          arr[base+3] = w;
          return base+4;
       }
       private int copyColor(float[] arr, int base, ColorARGB c) {
           arr[base+0] = ((float)(c.r&0xff))/255.0f;
           arr[base+1] = ((float)(c.g&0xff))/255.0f;
           arr[base+2] = ((float)(c.b&0xff))/255.0f;
           arr[base+3] = ((float)(c.a&0xff))/255.0f;
           return base+4;
       }
   }
}

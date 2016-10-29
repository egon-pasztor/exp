package demo;

import java.util.ArrayList;

import demo.VectorAlgebra.*;
import demo.Raster.*;

import demo.VectorAlgebra.*;   // vectors/matrices
import demo.Geometry.*;        // meshes(cube,sphere,ico)
import demo.Raster.*;          // texture image


public class World {

   // -----------------------------------------------------------------------
   // Camera
   // -----------------------------------------------------------------------

   public static class Camera {
      
      public Camera (int windowWidth, int windowHeight,
                     Vector3f lookAtPoint,
                     Vector3f cameraPosition,
                     Vector3f cameraUpVector,
                     float verticalFovInDegrees) {         

         this.width = windowWidth;
         this.height = windowHeight;
         setupCamera(lookAtPoint, cameraPosition, cameraUpVector, verticalFovInDegrees);
      }
         
      private final int width, height;
      
      public int getWidth() {
         return width;
      }
      public int getHeight() {
         return height;
      }
      
      // -----------------------------------------------
      // These are "free fields" specified by the user:
      // -----------------------------------------------
         
      private Vector3f cameraPosition;
      private Vector3f cameraUpVector;
      private Vector3f lookAtPoint;
      private float verticalFovInDegrees;
         
      public Vector3f getCameraPosition() {
         return cameraPosition;
      }
      public Vector3f getLookAtPoint() {
         return lookAtPoint;
      }      
      public Vector3f getCameraUpVector() {
         return cameraUpVector;
      }
      public float getVerticalFOV() {
         return verticalFovInDegrees;
      }
      
      public void setupCamera(Vector3f lookAtPoint,
                              Vector3f cameraPosition,
                              Vector3f cameraUpVector,
                              float verticalFovInDegrees) { 
         
         this.lookAtPoint = lookAtPoint;
         this.cameraPosition = cameraPosition;
         this.cameraUpVector = cameraUpVector;         
         this.verticalFovInDegrees = verticalFovInDegrees;
         updateDerivedFields();
      }
      
      // ---------------------------------------------
      // These are "derived fields" computed by update:
      // ---------------------------------------------
         
      private Matrix4f worldToCameraSpace;
      private Matrix4f cameraToClipSpace;
      private Vector3f camX, camY, camZ;      
         
      public Matrix4f getCameraToClipSpace() {
         return cameraToClipSpace;
      }
      public Matrix4f getWorldToCameraSpace() {
         return worldToCameraSpace;
      }
      public Vector3f getCamX() { return camX; }
      public Vector3f getCamY() { return camY; }
      public Vector3f getCamZ() { return camZ; }

      private void updateDerivedFields() {
         
         // The objects in the world are in a right-handed-coordinate system.
         // In WORLD-SPACE:
         //    the CAMERA is at "cameraPosition",
         //    the TARGET is at "lookatPoint".
         //
         // The first thing we do is TRANSLATE by "-cameraPosition":
         
         Matrix4f translateCameraToOrigin = new Matrix4f(
            1.0f,    0.0f,    0.0f,   -cameraPosition.x,
            0.0f,    1.0f,    0.0f,   -cameraPosition.y,
            0.0f,    0.0f,    1.0f,   -cameraPosition.z,
            0.0f,    0.0f,    0.0f,    1.0f);
         
         worldToCameraSpace = translateCameraToOrigin;
           
         // Now the CAMERA is at the origin, 
         // and the TARGET is at "cameraToLookat":
         
         Vector3f cameraToLookat = lookAtPoint.minus(cameraPosition);
         float distanceToTarget = cameraToLookat.length();
         
         // We want to ROTATE to put the TARGET on the -Z axis.
         //
         // A unit vector pointing in the opposite direction as "cameraToLookat'
         // will be the new Z axis, and we select X and Y perdendiculer to Z
         // such that "cameraUpVector" is in the Z-Y plane:
         
         camZ = cameraToLookat.times(-1.0f / distanceToTarget);
         camX = Vector3f.crossProduct(cameraUpVector, camZ).normalized();
         camY = Vector3f.crossProduct(camZ, camX).normalized();
           
         Matrix4f rotateSoTargetIsOnNegativeZ = new Matrix4f(
           camX.x,     camX.y,      camX.z,    0.0f,
           camY.x,     camY.y,      camY.z,    0.0f,
           camZ.x,     camZ.y,      camZ.z,    0.0f,
           0.0f,       0.0f,        0.0f,      1.0f);
         
         worldToCameraSpace = Matrix4f.product(rotateSoTargetIsOnNegativeZ, 
                                           worldToCameraSpace);
         
         // Now the CAMERA is at the origin,
         // and the TARGET is at <0,0,-distanceToTarget>
         //
         // The next step is to scale by 1/distanceToTarget:
         
         float scale = 1.0f / distanceToTarget;
         Matrix4f scaleByDistanceToTarget = new Matrix4f(
           scale,   0.0f,    0.0f,    0.0f,
           0.0f,    scale,   0.0f,    0.0f,
           0.0f,    0.0f,    scale,   0.0f,
           0.0f,    0.0f,    0.0f,    1.0f);
         
         worldToCameraSpace = Matrix4f.product(scaleByDistanceToTarget, 
                                           worldToCameraSpace);
         
         // Now we're fully in CAMERA-SPACE:
         // In CAMERA-SPACE:
         //    the CAMERA is at <0,0,  0>
         //    the TARGET is at <0,0, -1>
         
         float aspect = ((float)width) / height;
         float fHeight = (float) Math.tan(verticalFovInDegrees * (Math.PI / 180.0) * 0.5);
         float fWidth  = aspect * fHeight;
         
         // So  <0, 0, -1>  ... is expected to map to the CENTER of the viewport.
         //         
         // Our vertical "field of view in degrees" determines how much
         // of the <x,y> plane at z=-1 we can see in our viewport:
         //
         //    <fWidth,   0, -1>  ... is expected to map to the RIGHT-MIDDLE of the window
         //    <0,  fHeight, -1>  ... is expected to map to the MIDDLE-TOP of the window
         //
         // We're going to scale the x and y dimensions non-linearly so these become +/-1:
         
         Matrix4f scaleXYByFieldOfView = new Matrix4f(
           1/fWidth,   0.0f,        0.0f,    0.0f,
           0.0f,       1/fHeight,   0.0f,    0.0f,
           0.0f,       0.0f,        1.0f,    0.0f,
           0.0f,       0.0f,        0.0f,    1.0f);
         
         cameraToClipSpace = scaleXYByFieldOfView;
           
         // Now:  
         //   <0,  0, -1>  ... is expected to map to the CENTER of the window
         //   <1,  0, -1>  ... is expected to map to the RIGHT-MIDDLE of the window
         //   <0,  1, -1>  ... is expected to map to the MIDDLE-TOP of the window
         // 
         // In this space our "field of view" has become a full 90-degrees,
         // so any point where y is equal to -z should map to the TOP-MIDDLE, or:
         //
         //   y_view  =  y / -z
         //   x_view  =  x / -z
         //
         // so we KNOW we're going to want a final transform like:
         //
         //   new Matrix4f(
         //      1.0f,   0.0f,   0.0f,   0.0f,
         //      0.0f,   1.0f,   0.0f,   0.0f,
         //      0.0f,   0.0f,   ????.   ????,
         //      0.0f,   0.0f,  -1.0f,   0.0f );         
         //
         // This sets x_view and y_view so they're (x/-z) and (y/-z) respectively,
         // determining the (x,y) position of any point on the viewport.
         // This leaves only two unknown values, call them M and C:
         //
         //   new Matrix4f(
         //      1.0f,   0.0f,   0.0f,   0.0f,
         //      0.0f,   1.0f,   0.0f,   0.0f,
         //      0.0f,   0.0f,     M,      C,
         //      0.0f,   0.0f,  -1.0f,   0.0f );         
         //
         // And this will force:
         //
         //   z_view  ==  (zM+C) / -z   ==  -C/z -M
         //
         // This z_view doesn't affect where the point appears on the viewport,
         // (we've already got that with x_view and y_view),
         // but z_view is used for DEPTH-BUFFERING and needs 
         // to be in the -1 to 1 range to prevent OpenGL from dropping it.
         //
         // What we want is:
         //
         //   z == NEAR == -.1   -->   z_view == -1
         //   z == FAR  == -10   -->   z_view == +1
         //
         // So:
         //        -1 == -C/NEAR - M
         //        +1 == -C/FAR  - M
         //
         // Solving for M and C results:
         //
         //   M =  - (FAR+NEAR)/(FAR-NEAR)
         //   C =  (2*FAR*NEAR)/(FAR-NEAR)
         
         float nearZ =  -0.1f;
         float farZ  = -10.0f;
         
         float M =   - (farZ + nearZ)    / (farZ - nearZ);
         float C = (2.0f * farZ * nearZ) / (farZ - nearZ);
         
         Matrix4f perspectiveTransform = new Matrix4f(
           1.0f,   0.0f,   0.0f,   0.0f,
           0.0f,   1.0f,   0.0f,   0.0f,
           0.0f,   0.0f,    M,       C,
           0.0f,   0.0f,  -1.0f,   0.0f);
         
         cameraToClipSpace = Matrix4f.product(perspectiveTransform,
                                          cameraToClipSpace);
      }
      
      // -------------------------------------------------------------
      // Ball Controller
      // -------------------------------------------------------------
      
      public static class Controller {
         
         public Controller (Camera camera) {
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
   }
   
   
   // ----------------------------------
   // Camera:
   // ----------------------------------
   
   private Camera camera;

   public Camera getCamera() {
      return camera;
   }
   public void setCamera(Camera camera) {
      this.camera = camera;
   }
   
   // ----------------------------------
   // ROOT model:
   // ----------------------------------
   
   public Model getRootModel() {
      return root;
   }      
   public void setRootModel(Model root) {
      this.root = root;
   }
   private Model root;

   
   // ----------------------------------
   // Base model
   // ----------------------------------

   public static abstract class Model {
      public Model() {
         modelToWorld = Matrix4f.IDENTITY;
      }
      
      private Matrix4f modelToWorld;

      public Matrix4f getModelToWorld() {
         return modelToWorld;
      }
      public void setModelToWorld(Matrix4f modelToWorld) {
         this.modelToWorld = modelToWorld;
      }
      public void translate(Vector3f t) {
         modelToWorld = Matrix4f.product(Matrix4f.translation(t), modelToWorld);
      }
      public void rotate(Vector3f axis, float angle) {
         modelToWorld = Matrix4f.product(Matrix4f.fromMatrix3f(Matrix3f.rotation(axis, angle)), modelToWorld);
      }
   }
   
   // ------- Compound model
   
   public static class CompoundModel extends Model {
      public CompoundModel() { 
         children = new ArrayList<Model>();
      }
      
      public final ArrayList<Model> children;
   }
        
   // ------- "Mesh" models
   
   public static class TexturedMeshModel extends Model {
      public TexturedMeshModel(Geometry.Model geometry,
                               Image texture) {
         this.geometry = geometry;
         this.texture = texture;
      }
      
      public Geometry.Model geometry;
      public Image texture;
   }
}

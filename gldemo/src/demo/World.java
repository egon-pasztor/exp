package demo;

import java.util.ArrayList;
import java.util.HashSet;

import demo.VectorAlgebra.*;   // vectors/matrices
import demo.Geometry.*;        // meshes(cube,sphere,ico)
import demo.Raster.*;          // texture image


public class World {
   public static class Camera {

      public Camera(int windowWidth, int windowHeight,
                     Vector3f lookAtPoint,
                     Vector3f cameraPosition,
                     Vector3f cameraUpVector,
                     float verticalFovInDegrees) {

         // A Camera provides a method to project from points
         // in a 3D "world space" to points on a 2D window.

         this.width = windowWidth;
         this.height = windowHeight;

         // The objects in "world space" are in a right-handed
         // coordinate system, where:
         //    the CAMERA is at "cameraPosition",
         //    the TARGET is at "lookatPoint".

         this.lookAtPoint = lookAtPoint;
         this.cameraPosition = cameraPosition;

         // We want a transform from "world space" to a different
         // "camera space" coordinate system, where:
         //    the CAMERA is at "<0,0,0>",
         //    the TARGET is at "<0,0,-1>".
         //
         // To build this transform, the first thing we do
         // is TRANSLATE by "-cameraPosition":

         Matrix4f worldToCameraSpaceSoFar = new Matrix4f(
            1.0f, 0.0f, 0.0f, -cameraPosition.x,
            0.0f, 1.0f, 0.0f, -cameraPosition.y,
            0.0f, 0.0f, 1.0f, -cameraPosition.z,
            0.0f, 0.0f, 0.0f, 1.0f);

         // Now the CAMERA is at the origin,
         // and the TARGET is at "cameraToLookat":

         Vector3f cameraToLookat = lookAtPoint.minus(cameraPosition);
         distanceToTarget = cameraToLookat.length();

         // We want to ROTATE to put the TARGET on the -Z axis.
         //
         // A unit vector pointing in the opposite direction as "cameraToLookat'
         // will be the new Z axis, and we select X and Y perpendicular to Z
         // such that "cameraUpVector" is in the Z-Y plane:

         this.cameraUpVector = cameraUpVector;
         camZ = cameraToLookat.times(-1.0f / distanceToTarget);
         camX = Vector3f.crossProduct(cameraUpVector, camZ).normalized();
         camY = Vector3f.crossProduct(camZ, camX).normalized();

         Matrix4f rotateSoTargetIsOnNegativeZ = new Matrix4f(
            camX.x, camX.y, camX.z, 0.0f,
            camY.x, camY.y, camY.z, 0.0f,
            camZ.x, camZ.y, camZ.z, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f);

         worldToCameraSpaceSoFar = Matrix4f.product(rotateSoTargetIsOnNegativeZ,
            worldToCameraSpaceSoFar);

         // Now the CAMERA is at the origin,
         // and the TARGET is at <0,0,-distanceToTarget>
         // The final step is to scale by 1/distanceToTarget:

         float scale = 1.0f / distanceToTarget;
         Matrix4f scaleByDistanceToTarget = new Matrix4f(
            scale, 0.0f, 0.0f, 0.0f,
            0.0f, scale, 0.0f, 0.0f,
            0.0f, 0.0f, scale, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f);

         worldToCameraSpaceSoFar = Matrix4f.product(scaleByDistanceToTarget,
            worldToCameraSpaceSoFar);

         // Now we're fully in CAMERA-SPACE:

         worldToCameraSpace = worldToCameraSpaceSoFar;

         // Now in CAMERA-SPACE:
         //    the CAMERA is at <0,0, 0>
         //    the TARGET is at <0,0,-1>
         //
         // So <0,0,-1> is expected to map to the CENTER of the viewport.
         // Our vertical "field of view in degrees" determines how much
         // of the <x,y> plane at z=-1 we can see in our viewport:

         float aspect = ((float) width) / height;
         this.verticalFovInDegrees = verticalFovInDegrees;
         fHeight = (float) Math.tan(verticalFovInDegrees * (Math.PI / 180.0) * 0.5);
         fWidth = aspect * fHeight;

         //    <fWidth,   0, -1>  ... will be at the RIGHT-MIDDLE of the window
         //    <0,  fHeight, -1>  ... will be at the MIDDLE-TOP of the window
         //
         // We're going to scale the x and y dimensions non-linearly so these become +/-1:

         Matrix4f scaleXYByFieldOfView = new Matrix4f(
            1 / fWidth, 0.0f, 0.0f, 0.0f,
            0.0f, 1 / fHeight, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f);

         // Now:
         //   <0,  0, -1>  ... is expected to map to the CENTER of the window
         //   <1,  0, -1>  ... is expected to map to the RIGHT-MIDDLE of the window
         //   <0,  1, -1>  ... is expected to map to the MIDDLE-TOP of the window
         //
         // In this space our "field of view" has become a full 90-degrees:
         // any point where y is equal to -z should map to the TOP-MIDDLE, or:
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
         // This leaves only two unknown values, call them A and B:
         //
         //   new Matrix4f(
         //      1.0f,   0.0f,   0.0f,   0.0f,
         //      0.0f,   1.0f,   0.0f,   0.0f,
         //      0.0f,   0.0f,     A,      B,
         //      0.0f,   0.0f,  -1.0f,   0.0f );
         //
         // And this will force:
         //
         //   z_view  ==  (zA+B) / -z   ==  (-B/z-A)
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
         //        -1 == -B/NEAR - A
         //        +1 == -B/FAR  - A
         //
         // Solving for A and B results:
         //
         //   A =  - (FAR+NEAR)/(FAR-NEAR)
         //   B =  (2*FAR*NEAR)/(FAR-NEAR)

         float nearZ = -0.1f;
         float farZ = -10.0f;

         float A = -(farZ + nearZ) / (farZ - nearZ);
         float B = (2.0f * farZ * nearZ) / (farZ - nearZ);

         Matrix4f perspectiveTransform = new Matrix4f(
            1.0f, 0.0f,  0.0f,  0.0f,
            0.0f, 1.0f,  0.0f,  0.0f,
            0.0f, 0.0f,   A,     B,
            0.0f, 0.0f, -1.0f,  0.0f);

         cameraToClipSpace = Matrix4f.product(perspectiveTransform,
            scaleXYByFieldOfView);
      }

      // Public final properties describing this Camera

      public final int width, height;

      public final Vector3f cameraPosition;
      public final Vector3f cameraUpVector;
      public final Vector3f lookAtPoint;
      public final float verticalFovInDegrees;

      public final Matrix4f worldToCameraSpace;
      public final Matrix4f cameraToClipSpace;
      public final Vector3f camX, camY, camZ;

      public final float distanceToTarget;
      public final float fHeight, fWidth;

      // -------------------------------------------------------------------
      // Controller
      // -------------------------------------------------------------------

      public static class Controller {

         public Controller (Camera camera) {
            this.camera = camera;
            this.grabState = GrabState.Ungrabbed;
         }

         public Camera getCamera() {
            return camera;
         }

         private Camera camera;

         // -------------------------------------------------------------------
         // Camera "Click-and-Drag" code
         // -------------------------------------------------------------------

         public enum GrabState {
            Ungrabbed,
            Pinch,
            Rotate,   // Move camera around fixed lookat_point
            Zoom,     // Move camera closer or further from fixed lookat_point
            Pan,      // Move both camera and lookat_point by the same amount
            FOV };

         private GrabState grabState;

         // Point(s) where the grab began in Camera Space:
         private Vector3f grabPointCameraSpace;
         private Vector3f grabPointCameraSpace2;

         // If this Controller has been "Grabbed", we recall the
         // Camera parameters when the grab began:

         private Camera grabCamera;

         // Extra params for some grab types..

         private boolean roll;
         private float grabAngle;
         private float lastAngle;
         private float tScale;

         private Vector3f getCameraSpacePosition(int ix, int iy) {
            int width = camera.width;
            int height = camera.height;
            float y  = grabCamera.fHeight * ((height/2-iy) / ((float) (height/2)));
            float x  =  grabCamera.fWidth * ((ix-width/2)  / ((float) (width/2)));
            return new Vector3f(x,y,-1.0f);
         }

         public void grabPinch(int ix, int iy, int ix2, int iy2) {
            grabCamera = camera;
            grabState = GrabState.Pinch;
            grabPointCameraSpace = getCameraSpacePosition(ix,iy);
            grabPointCameraSpace2 = getCameraSpacePosition(ix2,iy2);
            grabAngle = (float) Math.atan2(grabPointCameraSpace2.y-grabPointCameraSpace.y,
               grabPointCameraSpace2.x-grabPointCameraSpace.x);
         }
         public void movePinch(int ix, int iy, int ix2, int iy2) {
            Vector3f pointCameraSpace = getCameraSpacePosition(ix,iy);
            Vector3f pointCameraSpace2 = getCameraSpacePosition(ix2,iy2);

            // so yeah.. there's a base translation:
            Vector3f cameraSpaceTranslation = pointCameraSpace.plus(pointCameraSpace2).times(0.5f)
               .minus(grabPointCameraSpace.plus(grabPointCameraSpace2).times(0.5f));

            // But in addition to this there's a ZOOM
            // if the distance between the fingers gets smaller,
            float d1 = grabPointCameraSpace2.minus(grabPointCameraSpace).length();
            float d2 = pointCameraSpace2.minus(pointCameraSpace).length();
            float distanceToTargetMultiplier = d1/d2;

            // the sum is a translation in all three dimensions
            Vector3f lookAtTranslation = grabCamera.camX.times(-cameraSpaceTranslation.x)
                                   .plus(grabCamera.camY.times(-cameraSpaceTranslation.y))
                                   .times(grabCamera.distanceToTarget);

            Vector3f newCameraLookAtPoint = grabCamera.lookAtPoint.plus(lookAtTranslation);
            Vector3f newCameraPosition = newCameraLookAtPoint.plus(
                  grabCamera.cameraPosition.minus(grabCamera.lookAtPoint)
                                           .times(distanceToTargetMultiplier));

            // There might also be a roll, if the line connecting the fingers is tilted
            float angle = (float) Math.atan2(pointCameraSpace2.y-pointCameraSpace.y,
               pointCameraSpace2.x-pointCameraSpace.x);

            float twopi = (float) (2.0*Math.PI);
            while (Math.abs(angle - lastAngle + twopi) < Math.abs(angle - lastAngle)) {
               angle += twopi;
            }
            while (Math.abs(angle - lastAngle - twopi) < Math.abs(angle - lastAngle)) {
               angle -= twopi;
            }
            lastAngle = angle;
            float deltaAngle = - (angle - grabAngle);

            Vector3f newCameraUpVector = grabCamera.camY.rotated(grabCamera.camZ, deltaAngle);

            camera = new Camera(grabCamera.width, grabCamera.height,
                                newCameraLookAtPoint,
                                newCameraPosition,
                                newCameraUpVector,
                                grabCamera.verticalFovInDegrees);
         }

         public void grab(int ix, int iy, GrabState newGrabState) {

            grabCamera = camera;
            grabState = newGrabState;
            grabPointCameraSpace = getCameraSpacePosition(ix,iy);

            // roll = (new Vector2f(grabPointCameraSpace.x,grabPointCameraSpace.y)).lengthSq()
            //      > grabFHeight*grabFWidth;

            roll = false;
            grabAngle = (float) Math.atan2(grabPointCameraSpace.y, grabPointCameraSpace.x);
            lastAngle = grabAngle;
            tScale = (grabPointCameraSpace.y < 0) ?
                     (grabCamera.fHeight-grabPointCameraSpace.y)
                   : (grabPointCameraSpace.y+grabCamera.fHeight);
         }

         public void moveTo(int ix, int iy) {
            if ((grabState == GrabState.Ungrabbed) &&
                (grabState == GrabState.Pinch)) return;

            Vector3f pointCameraSpace = getCameraSpacePosition(ix,iy);
            Vector3f deltaCameraSpace = pointCameraSpace.minus(grabPointCameraSpace);

            Vector3f newCameraLookAtPoint = grabCamera.lookAtPoint;
            Vector3f newCameraPosition    = grabCamera.cameraPosition;
            Vector3f newCameraUpVector    = grabCamera.camY;
            float newCameraVerticalFOV    = grabCamera.verticalFovInDegrees;

            if (grabState == GrabState.Rotate) {

               if ((deltaCameraSpace.x == 0) && (deltaCameraSpace.y == 0)) {
                  newCameraPosition = grabCamera.cameraPosition;
                  newCameraUpVector = grabCamera.camY;

               } else {
                  Vector3f cameraSpaceAxis;
                  float deltaAngle;

                  if (roll) {
                     float angle = (float) Math.atan2(pointCameraSpace.y, pointCameraSpace.x);
                     float twopi = (float) (2.0*Math.PI);
                     while (Math.abs(angle - lastAngle + twopi) < Math.abs(angle - lastAngle)) {
                        angle += twopi;
                     }
                     while (Math.abs(angle - lastAngle - twopi) < Math.abs(angle - lastAngle)) {
                        angle -= twopi;
                     }
                     lastAngle = angle;
                     deltaAngle = - (angle - grabAngle);

                     newCameraUpVector = grabCamera.camY.rotated(grabCamera.camZ, deltaAngle);
                  } else {
                     cameraSpaceAxis = new Vector3f (-deltaCameraSpace.y,deltaCameraSpace.x, 0).normalized();
                     deltaAngle = - (float)((deltaCameraSpace.length() / grabCamera.fHeight) * (Math.PI/1.5f));

                     Vector3f rotationAxis = grabCamera.camX.times(cameraSpaceAxis.x)
                                       .plus(grabCamera.camY.times(cameraSpaceAxis.y))
                                       .normalized();

                     newCameraPosition = grabCamera.lookAtPoint.plus(
                        grabCamera.cameraPosition.minus(grabCamera.lookAtPoint)
                                                 .rotated(rotationAxis, deltaAngle));

                     newCameraUpVector = grabCamera.camY.rotated(rotationAxis, deltaAngle);
                  }
               }

            } else if (grabState == GrabState.Pan) {
               Vector3f lookAtTranslation = grabCamera.camX.times(-deltaCameraSpace.x)
                                      .plus(grabCamera.camY.times(-deltaCameraSpace.y))
                                      .times(grabCamera.distanceToTarget);

               newCameraLookAtPoint = grabCamera.lookAtPoint.plus(lookAtTranslation);
               newCameraPosition = newCameraLookAtPoint.plus(
                  grabCamera.cameraPosition.minus(grabCamera.lookAtPoint));

            } else {
               // Either "ZOOM" or "FOV", two ways or making the target look bigger or smaller,
               // either by moving the camera closer to the target or by changing the field of view:

               float multiplier = (float) Math.pow(ScaleZoom, deltaCameraSpace.y/tScale);
               if (grabState == GrabState.Zoom) {
                  newCameraPosition = newCameraLookAtPoint.plus(
                     grabCamera.cameraPosition.minus(grabCamera.lookAtPoint)
                                              .times(multiplier));

               } else {
                  float newFHeight = grabCamera.fHeight * multiplier;
                  newCameraVerticalFOV = (float) (2.0f * (180.0f / Math.PI) * Math.atan(newFHeight));
               }
            }

            camera = new Camera(grabCamera.width, grabCamera.height,
               newCameraLookAtPoint,
               newCameraPosition,
               newCameraUpVector,
               newCameraVerticalFOV);
         }

         public void release() {
            grabState = GrabState.Ungrabbed;
         }
         public boolean isGrabbed() {
            return (grabState == GrabState.Ungrabbed);
         }
         public GrabState getGrabState() {
            return grabState;
         }

         private static final float ScaleZoom       = 3.0f;
         private static final float Scale3DRotation = 2.0f;
      }
   }

   // ----------------------------------
   // ROOT model:
   // ----------------------------------
   
   public Model getRootModel() {
      return rootModel;
   }      
   public void setRootModel(Model rootModel) {
      this.rootModel = rootModel;
   }
   private Model rootModel;

   
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
      public TexturedMeshModel(MeshModel geometry,
                               Image texture) {
         this.geometry = geometry;
         this.texture = texture;
      }
      
      public MeshModel geometry;
      public Image texture;
   }

   // ---------------------------------------------
   // Gathering the textures and models in use..
   // ---------------------------------------------

   public HashSet<Image> getTextures() {
      HashSet<Image> textures = new HashSet<Image>();
      addTextures(rootModel, textures);
      return textures;
   }
   public HashSet<MeshModel> getMeshModels() {
      HashSet<MeshModel> models = new HashSet<MeshModel>();
      addModels(rootModel, models);
      return models;
   }

   private void addTextures(Model m, HashSet<Image> textures) {
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            addTextures(child, textures);
         }
      }
      if (m instanceof TexturedMeshModel) {
         Image texture = ((TexturedMeshModel) m).texture;
         textures.add(texture);
      }
   }
   private void addModels(Model m, HashSet<MeshModel> models) {
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            addModels(child, models);
         }
      }
      if (m instanceof TexturedMeshModel) {
         MeshModel model = ((TexturedMeshModel) m).geometry;
         models.add(model);
      }
   }

   // ---------------------------------------------
   // First attempt at picking...
   // ---------------------------------------------

   /*
   public HashSet<Model> getIntersectingModels(Vector3f start, Vector3f end) {
      HashSet<Model> results = new HashSet<Model>();
      getIntersectingModels(rootModel, results, start, end);
      return results;
   }
   private void getIntersectingModels(....) {
      if (m instanceof CompoundModel) {
         for (Model child : ((CompoundModel) m).children) {
            addModels(child, models);
         }
      }
      if (m instanceof TexturedMeshModel) {
         Geometry.Model model = ((TexturedMeshModel) m).geometry;
         models.add(model);
      }
   }
   */

}

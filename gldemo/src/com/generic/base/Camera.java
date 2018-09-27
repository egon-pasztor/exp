package com.generic.base;

import com.generic.base.Algebra.Matrix4x4;
import com.generic.base.Algebra.Vector3;
import com.generic.base.Algebra.Vector2;

public class Camera {

   public Camera(Image.Size size,
                 Vector3 lookAtPoint,
                 Vector3 cameraPosition,
                 Vector3 cameraUpVector,
                 float verticalFovInDegrees) {

      // A Camera provides a method to project from points
      // in a 3D "world space" to points on a 2D window.

      this.size = size;

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

      Matrix4x4 worldToCameraSpaceSoFar = new Matrix4x4(
         1.0f, 0.0f, 0.0f, -cameraPosition.x,
         0.0f, 1.0f, 0.0f, -cameraPosition.y,
         0.0f, 0.0f, 1.0f, -cameraPosition.z,
         0.0f, 0.0f, 0.0f, 1.0f);

      // Now the CAMERA is at the origin,
      // and the TARGET is at "cameraToLookat":

      Vector3 cameraToLookat = lookAtPoint.minus(cameraPosition);
      distanceToTarget = cameraToLookat.length();

      // We want to ROTATE to put the TARGET on the -Z axis.
      //
      // A unit vector pointing in the opposite direction as "cameraToLookat'
      // will be the new Z axis, and we select X and Y perpendicular to Z
      // such that "cameraUpVector" is in the Z-Y plane:

      this.cameraUpVector = cameraUpVector;
      camZ = cameraToLookat.times(-1.0f / distanceToTarget);
      camX = Vector3.crossProduct(cameraUpVector, camZ).normalized();
      camY = Vector3.crossProduct(camZ, camX).normalized();

      Matrix4x4 rotateSoTargetIsOnNegativeZ = new Matrix4x4(
         camX.x, camX.y, camX.z, 0.0f,
         camY.x, camY.y, camY.z, 0.0f,
         camZ.x, camZ.y, camZ.z, 0.0f,
         0.0f, 0.0f, 0.0f, 1.0f);

      worldToCameraSpaceSoFar = Matrix4x4.product(rotateSoTargetIsOnNegativeZ,
         worldToCameraSpaceSoFar);

      // Now the CAMERA is at the origin,
      // and the TARGET is at <0,0,-distanceToTarget>
      // The final step is to scale by 1/distanceToTarget:

      float scale = 1.0f / distanceToTarget;
      Matrix4x4 scaleByDistanceToTarget = new Matrix4x4(
         scale, 0.0f, 0.0f, 0.0f,
         0.0f, scale, 0.0f, 0.0f,
         0.0f, 0.0f, scale, 0.0f,
         0.0f, 0.0f, 0.0f, 1.0f);

      worldToCameraSpaceSoFar = Matrix4x4.product(scaleByDistanceToTarget,
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

      float aspect = ((float) size.width) / size.height;
      this.verticalFovInDegrees = verticalFovInDegrees;
      fHeight = (float) Math.tan(verticalFovInDegrees * (Math.PI / 180.0) * 0.5);
      fWidth = aspect * fHeight;

      //    <fWidth,   0, -1>  ... will be at the RIGHT-MIDDLE of the window
      //    <0,  fHeight, -1>  ... will be at the MIDDLE-TOP of the window
      //
      // We're going to scale the x and y dimensions non-linearly so these become +/-1:

      Matrix4x4 scaleXYByFieldOfView = new Matrix4x4(
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
      //   A =  - (FAR+NEAR) / (FAR-NEAR)
      //   B =  (2*FAR*NEAR) / (FAR-NEAR)

      float nearZ = -0.1f;
      float farZ = -10.0f;

      //float A = -(farZ + nearZ) / (farZ - nearZ);
      //float B = (2.0f * farZ * nearZ) / (farZ - nearZ);
      
      // What we want is:
      //
      //   z == NEAR == -.1   -->   z_view ==  0
      //   z == FAR  == -10   -->   z_view == +1
      //
      // So:
      //         0 == -B/NEAR - A
      //        +1 == -B/FAR  - A
      //
      // Solving for A and B results:
      //
      //   A =    - FAR    / (FAR-NEAR)
      //   B =  (FAR*NEAR) / (FAR-NEAR)

      float A =    - farZ      / (farZ - nearZ);
      float B = (farZ * nearZ) / (farZ - nearZ);

      Matrix4x4 perspectiveTransform = new Matrix4x4(
         1.0f, 0.0f,  0.0f,  0.0f,
         0.0f, 1.0f,  0.0f,  0.0f,
         0.0f, 0.0f,   A,     B,
         0.0f, 0.0f, -1.0f,  0.0f);

      cameraToClipSpace = Matrix4x4.product(perspectiveTransform,
         scaleXYByFieldOfView);
   }

   // Public final properties describing this Camera

   public final Image.Size size;

   public final Vector3 cameraPosition;
   public final Vector3 cameraUpVector;
   public final Vector3 lookAtPoint;
   public final float verticalFovInDegrees;

   public final Matrix4x4 worldToCameraSpace;
   public final Matrix4x4 cameraToClipSpace;
   public final Vector3 camX, camY, camZ;

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
      private Vector3 grabPointCameraSpace;
      private Vector3 grabPointCameraSpace2;

      // If this Controller has been "Grabbed", we recall the
      // Camera parameters when the grab began:

      private Camera grabCamera;

      // Extra params for some grab types..

      private boolean roll;
      private float grabAngle;
      private float lastAngle;
      private float tScale;

      private Vector3 getCameraSpacePosition(int ix, int iy) {
         int width = camera.size.width;
         int height = camera.size.height;
         float y  = grabCamera.fHeight * ((height/2-iy) / ((float) (height/2)));
         float x  =  grabCamera.fWidth * ((ix-width/2)  / ((float) (width/2)));
         return new Vector3(x,y,-1.0f);
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
         Vector3 pointCameraSpace = getCameraSpacePosition(ix,iy);
         Vector3 pointCameraSpace2 = getCameraSpacePosition(ix2,iy2);

         // so yeah.. there's a base translation:
         Vector3 cameraSpaceTranslation = pointCameraSpace.plus(pointCameraSpace2).times(0.5f)
            .minus(grabPointCameraSpace.plus(grabPointCameraSpace2).times(0.5f));

         // But in addition to this there's a ZOOM
         // if the distance between the fingers gets smaller,
         float d1 = grabPointCameraSpace2.minus(grabPointCameraSpace).length();
         float d2 = pointCameraSpace2.minus(pointCameraSpace).length();
         float distanceToTargetMultiplier = d1/d2;

         // the sum is a translation in all three dimensions
         Vector3 lookAtTranslation = grabCamera.camX.times(-cameraSpaceTranslation.x)
                                .plus(grabCamera.camY.times(-cameraSpaceTranslation.y))
                                .times(grabCamera.distanceToTarget);

         Vector3 newCameraLookAtPoint = grabCamera.lookAtPoint.plus(lookAtTranslation);
         Vector3 newCameraPosition = newCameraLookAtPoint.plus(
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

         Vector3 newCameraUpVector = grabCamera.camY.rotated(grabCamera.camZ, deltaAngle);

         camera = new Camera(grabCamera.size,
                             newCameraLookAtPoint,
                             newCameraPosition,
                             newCameraUpVector,
                             grabCamera.verticalFovInDegrees);
      }

      public void grab(int ix, int iy, GrabState newGrabState) {

         grabCamera = camera;
         grabState = newGrabState;
         grabPointCameraSpace = getCameraSpacePosition(ix,iy);

         roll = (new Vector2(grabPointCameraSpace.x,grabPointCameraSpace.y)).lengthSq()
               > grabCamera.fHeight*grabCamera.fWidth;

         grabAngle = (float) Math.atan2(grabPointCameraSpace.y, grabPointCameraSpace.x);
         lastAngle = grabAngle;
         tScale = (grabPointCameraSpace.y < 0) ?
                  (grabCamera.fHeight-grabPointCameraSpace.y)
                : (grabPointCameraSpace.y+grabCamera.fHeight);
      }

      public void moveTo(int ix, int iy) {
         if ((grabState == GrabState.Ungrabbed) ||
             (grabState == GrabState.Pinch)) return;

         Vector3 pointCameraSpace = getCameraSpacePosition(ix,iy);
         Vector3 deltaCameraSpace = pointCameraSpace.minus(grabPointCameraSpace);

         Vector3 newCameraLookAtPoint = grabCamera.lookAtPoint;
         Vector3 newCameraPosition    = grabCamera.cameraPosition;
         Vector3 newCameraUpVector    = grabCamera.camY;
         float newCameraVerticalFOV    = grabCamera.verticalFovInDegrees;

         if (grabState == GrabState.Rotate) {

            if ((deltaCameraSpace.x == 0) && (deltaCameraSpace.y == 0)) {
               newCameraPosition = grabCamera.cameraPosition;
               newCameraUpVector = grabCamera.camY;

            } else {
               Vector3 cameraSpaceAxis;
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
                  cameraSpaceAxis = new Vector3 (-deltaCameraSpace.y,deltaCameraSpace.x, 0).normalized();
                  deltaAngle = - (float)((deltaCameraSpace.length() / grabCamera.fHeight) * (Math.PI/1.5f));

                  Vector3 rotationAxis = grabCamera.camX.times(cameraSpaceAxis.x)
                                    .plus(grabCamera.camY.times(cameraSpaceAxis.y))
                                    .normalized();

                  newCameraPosition = grabCamera.lookAtPoint.plus(
                     grabCamera.cameraPosition.minus(grabCamera.lookAtPoint)
                                              .rotated(rotationAxis, deltaAngle));

                  newCameraUpVector = grabCamera.camY.rotated(rotationAxis, deltaAngle);
               }
            }

         } else if (grabState == GrabState.Pan) {
            Vector3 lookAtTranslation = grabCamera.camX.times(-deltaCameraSpace.x)
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

         camera = new Camera(grabCamera.size,
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

      private static final float ScaleZoom = 3.0f;
   }
}

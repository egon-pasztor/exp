
  // ------------------------------------------------------------------
  // CameraBall.h
  // ------------------------------------------------------------------

#ifndef CameraBall_H
#define CameraBall_H

#include "VecMat3.h"

  // ------------------------------------------------------------------
  // ------------------------------------------------------------------

struct CameraBall {

   int windowWidth, windowHeight;

   Vec3 lookat_Point;
   Vec3 camera_Position;
   Vec3 camera_UpVector;
   Real vertical_fov_in_degrees;

   // -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
  
   virtual ~CameraBall () {}

   enum GrabType { Rotate,   // Move camera around fixed lookat_point
                   Zoom,     // Move camera closer or further from fixed lookat_point
                   Pan,      // Move camera and lookat_point together
                   FovAdjust };

   virtual void grab    (int x, int y, GrabType gt) =0;
   virtual void moveTo  (int x, int y)              =0;
   virtual void release ()                          =0;

   virtual bool isGrabbed() =0;

   // -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
   // TODO -- there should be a separate transformation-class
   //   to handle the camera's effects on the world.  For now, 
   //   we put this here:
   // -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

   virtual void getWorldRay (int x, int y, Vec3& origin, Vec3& direction) =0;

};

  // ------------------------------------------------------------------
  // ------------------------------------------------------------------

CameraBall* Create_CameraBall (int windowWidth, int windowHeight,
                               const Vec3& intial_lookat_Point,
                               const Vec3& intial_camera_Position,
                               const Vec3& intial_camera_UpVector,
                               Real intial_vertical_fov_in_degrees);

  // ------------------------------------------------------------------
  // ------------------------------------------------------------------

#endif

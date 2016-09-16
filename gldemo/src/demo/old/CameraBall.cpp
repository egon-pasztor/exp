
  // ----------------------------------------------------------------------
  // CameraBall                                    Egon Pasztor :: GPL 2003
  // ----------------------------------------------------------------------

#include "CameraBall.h"
#include "VecMat3.h"
#include "Debug.h"

  // ----------------------------------------------------------------------

#define Scale_3D_Rotation  (Real(2.0))
#define Scale_2D_Rotation  (Real(1.5))
#define Scale_Zoom         (Real(3.0))

  // ----------------------------------------------------------------------

struct CameraBall_Impl : CameraBall {

   CameraBall_Impl (int windowWidth, int windowHeight,
                    const Vec3& intial_lookat_point,
                    const Vec3& intial_camera_Position,
                    const Vec3& intial_camera_UpVector,
                    const Real& intial_vertical_fov_in_degrees);

  ~CameraBall_Impl ();

   // ---------------------------------------------

   void grab    (int x, int y, GrabType gt);
   void moveTo  (int x, int y);
   void release ();

   bool isGrabbed(); 

   void getWorldRay (int x, int y, Vec3& origin, Vec3& direction);

   // ---------------------------------------------

   bool grabbed;
   GrabType grabType;
   Real xGrab, yGrab;

   Vec3 grab_lookat_Point, grab_lookat_to_camera;
   Vec3 grab_zVector, grab_xVector, grab_yVector;

   // For rotate grabs
   bool roll;
   Real grabAngle;

   // For zoom grabs
   Real tScale;

   // For pan grabs
   Real windowScale;

   // For fovAdjust grabs
   Real fovTangent;

};

  //---------------------------------------------------------------------

CameraBall* Create_CameraBall (int windowWidth, int windowHeight,
                               const Vec3& intial_lookat_Point,
                               const Vec3& intial_camera_Position,
                               const Vec3& intial_camera_UpVector,
                               Real intial_vertical_fov_in_degrees) 
{
   return new CameraBall_Impl (windowWidth, windowHeight,
                               intial_lookat_Point,
                               intial_camera_Position,
                               intial_camera_UpVector,
                               intial_vertical_fov_in_degrees);
}

  //---------------------------------------------------------------------
  //---------------------------------------------------------------------

CameraBall_Impl::CameraBall_Impl (int windowWidth, int windowHeight,
                                  const Vec3& intial_lookat_Point,
                                  const Vec3& intial_camera_Position,
                                  const Vec3& intial_camera_UpVector,
                                  const Real& intial_vertical_fov_in_degrees)
{
   lookat_Point    = intial_lookat_Point;
   camera_Position = intial_camera_Position;

   Vec3 zVector = (lookat_Point - camera_Position);
   Vec3 xVector = crossProduct(zVector,intial_camera_UpVector);
   Vec3 yVector = crossProduct(xVector,zVector);  
   camera_UpVector = yVector;  camera_UpVector.normalize();

   vertical_fov_in_degrees = intial_vertical_fov_in_degrees;

   this->windowWidth  = windowWidth;
   this->windowHeight = windowHeight;

   grabbed = false;
}

  //---------------------------------------------------------------------
  //---------------------------------------------------------------------

CameraBall_Impl::~CameraBall_Impl () 
{
}

  //---------------------------------------------------------------------
  //---------------------------------------------------------------------

void CameraBall_Impl::grab (int ix, int iy, GrabType grabType) 
{
   Real y = (iy-windowHeight/2) / ((Real) (windowHeight/2));
   Real x = (ix-windowWidth/2)  / ((Real) (windowHeight/2));

   this->grabType = grabType;
   this->grabbed  = true;
   this->xGrab = x;
   this->yGrab = y;
  
   grab_lookat_Point     = lookat_Point;
   grab_lookat_to_camera = camera_Position - lookat_Point;

   grab_zVector = grab_lookat_to_camera;                        grab_zVector.normalize();
   grab_xVector = crossProduct(grab_zVector, camera_UpVector);  grab_xVector.normalize();
   grab_yVector = crossProduct(grab_xVector, grab_zVector);     grab_yVector.normalize();


   if (grabType == CameraBall::Rotate) {

      roll = (x*x+y*y) > 1;
      if (roll) grabAngle = atan2(y,x);

   } else if (grabType == CameraBall::Zoom) {

      tScale = (y < 0.5) ? (1-y) : (y);

   } else if (grabType == CameraBall::FovAdjust) {

      tScale     = (y < 0.5) ? (1-y) : (y);
      fovTangent = ((Real) tan(vertical_fov_in_degrees * 3.14159 / (2 * 180.0)));

   } else if (grabType == CameraBall::Pan) {
 
      windowScale = ((Real) tan(vertical_fov_in_degrees * 3.14159 / (2 * 180.0))
                  * grab_lookat_to_camera.len());

   }
}

  //---------------------------------------------------------------------

void CameraBall_Impl::moveTo  (int ix, int iy)
{ 
   if (!this->grabbed) return;
   Real y = (iy-windowHeight/2) / ((Real) (windowHeight/2));
   Real x = (ix-windowWidth/2)  / ((Real) (windowHeight/2));

   Real dx = x-xGrab;
   Real dy = y-yGrab; 
   if ((dx==0) && (dy==0)) return;

   if (grabType == CameraBall::Rotate) {
   
      Vec3 axis;
      Real amount;

      if (roll) {
                  axis = Vec3 (0,0,1); 
                  amount = (Real) (atan2(y,x) - grabAngle);
                  amount *= Scale_2D_Rotation;
      } else { 
                  axis = Vec3 ((Real) dy,(Real) -dx,0); axis.normalize();
                  amount = ((Real) sqrt(dx*dx+dy*dy));  
                  amount *= Scale_3D_Rotation;
      }

      Vec3 finalAxis = grab_xVector * axis.x 
                     + grab_yVector * axis.y
                     + grab_zVector * axis.z;

      camera_Position = grab_lookat_to_camera;
      camera_Position.rotate(amount,finalAxis);
      camera_Position += lookat_Point;

      camera_UpVector = grab_yVector;
      camera_UpVector.rotate(amount,finalAxis);

   } else if (grabType == CameraBall::Zoom) {

      camera_Position = grab_lookat_to_camera;
      camera_Position *= pow(Scale_Zoom,dy/tScale);
      camera_Position += lookat_Point;

   } else if (grabType == CameraBall::Pan) {

      Vec3 translation = grab_xVector * dx 
                       + grab_yVector * dy;

      translation *= windowScale;
      lookat_Point = grab_lookat_Point + translation;

      camera_Position = grab_lookat_to_camera;
      camera_Position += lookat_Point;

   } else if (grabType == CameraBall::FovAdjust) {

      Real newFovTangent = fovTangent * pow(Scale_Zoom,dy/tScale);
      vertical_fov_in_degrees = Real(2.0 * 180.0 / 3.14159) * atan(newFovTangent);

   }
}

  //---------------------------------------------------------------------

void CameraBall_Impl::release () 
{
   grabbed = false;
}

  //---------------------------------------------------------------------

bool CameraBall_Impl::isGrabbed()
{
   return grabbed;
}

  //---------------------------------------------------------------------
  //  A guess at screen to world space conversion.
  //  TODO -- do this right, with matrices and whatnot.
  //---------------------------------------------------------------------

void CameraBall_Impl::getWorldRay (int ix, int iy, Vec3& origin, Vec3& direction)
{
   Real y = (iy-windowHeight/2) / ((Real) (windowHeight/2));
   Real x = (ix-windowWidth/2)  / ((Real) (windowHeight/2));

   Vec3 grab_lookat_to_camera = camera_Position - lookat_Point;

   Vec3 grab_zVector = grab_lookat_to_camera;                        grab_zVector.normalize();
   Vec3 grab_xVector = crossProduct(grab_zVector, camera_UpVector);  grab_xVector.normalize();
   Vec3 grab_yVector = crossProduct(grab_xVector, grab_zVector);     grab_yVector.normalize();

   Real windowScale = ((Real) tan(vertical_fov_in_degrees * 3.14159 / (2 * 180.0))
                    * grab_lookat_to_camera.len());

   Vec3 clickedPoint = lookat_Point - grab_xVector * x * windowScale
                                    - grab_yVector * y * windowScale;

   origin = camera_Position;

   direction = clickedPoint - origin;
   direction.normalize();
}

  //---------------------------------------------------------------------
  //---------------------------------------------------------------------


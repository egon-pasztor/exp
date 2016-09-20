package demo;

public class GLMath {
   
   // -----------------------------------------------------------------------
   // Vector3f
   // -----------------------------------------------------------------------
   
   public static class Vector3f {
      
       public float x, y, z;
   
       public Vector3f() {
          this(0.0f, 0.0f, 0.0f);
       }
       public Vector3f(float x_, float y_, float z_) {
          x = x_;
          y = y_;
          z = z_;
       }
       public Vector3f(Vector3f v) {
          this(v.x, v.y, v.z);
       }
       public Vector3f copy() {
          return new Vector3f(this);
       }
       
       public Vector3f set(Vector3f v) {
          x = v.x;
          y = v.y;
          z = v.z;
          return this;
       }
       public Vector3f add(Vector3f v) {
          x += v.x;
          y += v.y;
          z += v.z;
          return this;
       }
       public Vector3f subtract(Vector3f v) {
          x -= v.x;
          y -= v.y;
          z -= v.z;
          return this;
       }
       public Vector3f multiply(float s) {
          x *= s;
          y *= s;
          z *= s;
          return this;
       }
       
       public Vector3f plus(Vector3f v) {
          return copy().add(v);
       }
       public Vector3f minus(Vector3f v) {
          return copy().subtract(v);
       }
       public Vector3f times(float s) {   
          return copy().multiply(s);
       }       
       
       public static float innerProduct(Vector3f a, Vector3f b) {
          return a.x * b.x + a.y * b.y + a.z * b.z;
       }
       public static Vector3f termwiseProduct(Vector3f a, Vector3f b) {
          return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
       }
       public static Matrix3f outerProduct(Vector3f a, Vector3f b) {
          return new Matrix3f(
                a.x * b.x, a.x * b.y, a.x * b.z,
                a.y * b.x, a.y * b.y, a.y * b.z,
                a.z * b.x, a.z * b.y, a.z * b.z);
       }
       
       public float dot(Vector3f v) {
          return Vector3f.innerProduct(this, v);
       }       
       public Vector3f cross(Vector3f v) {
          return new Vector3f(y * v.z - z * v.y, 
                              z * v.x - x * v.z, 
                              x * v.y - y * v.x);
       }
      
       public float lengthSq() {
          return this.dot(this);
       }
       public float length() {
          return (float) Math.sqrt(lengthSq());
       }
       public Vector3f normalize() {
          return this.multiply(1.0f / length());
       }
       public Vector3f normalized() {
          return copy().normalize();
       }
       
       public String toString() {
          return String.format("(%g,%g,%g)",  x,y,z);
       }

       public Vector3f rotated(Vector3f normalizedAxis, float angle) {
          Vector3f qv = normalizedAxis.times((float) Math.sin(angle / 2.0f));
          float    qs = (float) Math.cos(angle / 2.0f);
          Vector3f mv = qv.cross(this).plus(this.times(qs));
          return qv.cross(mv).plus(mv.times(qs)).plus(qv.times(this.dot(qv)));
       }
       public Vector3f interpolated(Vector3f target, float fraction) {
          float remainder = 1.0f - fraction;
          return new Vector3f(x * remainder + target.x * fraction,
                              y * remainder + target.y * fraction,
                              z * remainder + target.z * fraction);
       }

       public static final Vector3f ORIGIN = new Vector3f(0.0f, 0.0f, 0.0f);
       public static final Vector3f X      = new Vector3f(1.0f, 0.0f, 0.0f);
       public static final Vector3f Y      = new Vector3f(0.0f, 1.0f, 0.0f);
       public static final Vector3f Z      = new Vector3f(0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // Matrix3f
   // -----------------------------------------------------------------------
   
   public static class Matrix3f {
      
      public float xx, xy, xz,
                   yx, yy, yz,
                   zx, zy, zz;
  
      public Matrix3f() {}
  
      public Matrix3f(float xx_, float xy_, float xz_,
                      float yx_, float yy_, float yz_,
                      float zx_, float zy_, float zz_) {
         
          xx = xx_; xy = xy_; xz = xz_;
          yx = yx_; yy = yy_; yz = yz_;
          zx = zx_; zy = zy_; zz = zz_;
      }
      public Matrix3f(Matrix3f m) {         
         this(m.xx, m.xy, m.xz,
              m.yx, m.yy, m.yz,
              m.zx, m.zy, m.zz);
      }
      
      public Matrix3f copy() {
         return new Matrix3f(this);
      }       
      
      public Matrix3f set(Matrix3f m) {
         xx = m.xx; xy = m.xy; xz = m.xz;
         yx = m.yx; yy = m.yy; yz = m.yz;
         zx = m.zx; zy = m.zy; zz = m.zz;
         return this;
      }
      public Matrix3f add(Matrix3f m) {
         xx += m.xx; xy += m.xy; xz += m.xz;
         yx += m.yx; yy += m.yy; yz += m.yz;
         zx += m.zx; zy += m.zy; zz += m.zz;
         return this;
      }
      public Matrix3f subtract(Matrix3f m) {
         xx -= m.xx; xy -= m.xy; xz -= m.xz;
         yx -= m.yx; yy -= m.yy; yz -= m.yz;
         zx -= m.zx; zy -= m.zy; zz -= m.zz;
         return this;
      }
      public Matrix3f multiply(float s) {
         xx *= s; xy *= s; xz *= s;
         yx *= s; yy *= s; yz *= s;
         zx *= s; zy *= s; zz *= s;
         return this;
      }
      public Matrix3f transpose() {
         float t;
         t = xy; xy = yx; yx = t;
         t = xz; xz = zx; zx = t;
         t = yz; yz = zy; zy = t;
         return this;
      }
      
      public Matrix3f plus(Matrix3f m) {
         return copy().add(m);
      }
      public Matrix3f minus(Matrix3f m) {
         return copy().subtract(m);
      }
      public Matrix3f times(float s) {   
         return copy().multiply(s);
      }       
      public Matrix3f transposed() {
         return copy().transpose();
      }
      
      public static Vector3f product(Vector3f a, Matrix3f b) {
         return new Vector3f (
               a.x * b.xx + a.y * b.yx + a.z * b.zx,
               a.x * b.xy + a.y * b.yy + a.z * b.zy,
               a.x * b.xz + a.y * b.yz + a.z * b.zz); 
      }
      public static Vector3f product(Matrix3f a, Vector3f b) {
         return new Vector3f (
               a.xx * b.x + a.xy * b.y + a.xz * b.z,
               a.yx * b.x + a.yy * b.y + a.yz * b.z,
               a.zx * b.x + a.zy * b.y + a.zz * b.z); 
      }
      public static Matrix3f product(Matrix3f a, Matrix3f b) {
         return new Matrix3f (
               a.xx * b.xx + a.xy * b.yx + a.xz * b.zx,  
               a.xx * b.xy + a.xy * b.yy + a.xz * b.zy,
               a.xx * b.xz + a.xy * b.yz + a.xz * b.zz,
               
               a.yx * b.xx + a.yy * b.yx + a.yz * b.zx,  
               a.yx * b.xy + a.yy * b.yy + a.yz * b.zy,
               a.yx * b.xz + a.yy * b.yz + a.yz * b.zz,
               
               a.zx * b.xx + a.zy * b.yx + a.zz * b.zx,  
               a.zx * b.xy + a.zy * b.yy + a.zz * b.zy,
               a.zx * b.xz + a.zy * b.yz + a.zz * b.zz);         
      }
      
      
      public String toString() {
         return String.format("(%g,%g,%g; %g,%g,%g; %g,%g%g)",  xx,xy,xz, yx,yy,yz, zx,zy,zz);
      }

      public float determinate() {
         return (yy*zz-zy*yz) * xx
              - (yx*zz-zx*yz) * xy
              + (yx*zy-zx*yy) * xz;
      }
      public Matrix3f inverted() {
         Matrix3f cofactorMatrix = new Matrix3f(
              +(yy*zz-zy*yz), -(yx*zz-zx*yz), +(yx*zy-zx*yy),
              -(xy*zz-zy*xz), +(xx*zz-zx*xz), -(xx*zy-zx*xy),
              +(xy*yz-yy*xz), -(xx*yz-yx*xz), +(xx*yy-yx*xy));
         
         return cofactorMatrix.transpose().multiply(1.0f / determinate());
      }
      
      public static Matrix3f scaling(float s) {
         return new Matrix3f (   s, 0.0f, 0.0f,
                              0.0f,    s, 0.0f,
                              0.0f, 0.0f,    s);
      }
      public static Matrix3f rotation(Vector3f normalizedAxis, float angle) {
         float sa = (float) Math.sin(angle);
         float ca = (float) Math.cos(angle);
         float x = normalizedAxis.x, y = normalizedAxis.y, z = normalizedAxis.z;
         return new Matrix3f ( x*x*(1-ca)+ ca,   x*y*(1-ca)- sa*z, x*z*(1-ca)+ sa*y,
                               y*x*(1-ca)+ sa*z, y*y*(1-ca)+ ca,   y*z*(1-ca)- sa*x,
                               z*x*(1-ca)- sa*y, z*y*(1-ca)+ sa*x, z*z*(1-ca)+ ca    );   
      }      
      
      public static final Matrix3f IDENTITY = new Matrix3f(1.0f, 0.0f, 0.0f,
                                                           0.0f, 1.0f, 0.0f,
                                                           0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // CameraBall
   // -----------------------------------------------------------------------
  
   public static class CameraBall {
      
      public CameraBall (int windowWidth, int windowHeight,
                         Vector3f initialLookatPoint,
                         Vector3f initialCameraPosition,
                         Vector3f initialCameraUpVector,
                         float initialVerticalFOV) {         
         
         this.width = windowWidth;
         this.height = windowHeight;
         this.lookatPoint = initialLookatPoint;
         this.cameraPosition = initialCameraPosition;
         
         Vector3f zVector = initialLookatPoint.minus(initialCameraPosition);
         Vector3f xVector = zVector.cross(initialCameraUpVector);
         Vector3f yVector = xVector.cross(zVector);  
         this.cameraUpVector = yVector.normalized();         
         
         this.verticalFOV = initialVerticalFOV;
      }

      public Vector3f getLookatPoint() {
         return lookatPoint.copy();
      }      
      public Vector3f getCameraPosition() {
         return cameraPosition.copy();
      }
      public Vector3f getCameraUpVector() {
         return cameraUpVector.copy();
      }
      public float getVerticalFOV() {
         return verticalFOV;
      }
      public float getAspectRatio() {
         return ((float)width) / height;
      }
      
      public enum GrabType { Rotate,   // Move camera around fixed lookat_point
                             Zoom,     // Move camera closer or further from fixed lookat_point
                             Pan,      // Move camera and lookat_point together
                             FOV };
                             
      public void grab(int ix, int iy, GrabType grabType) {
         float y = (iy-height/2) / ((float) (height/2));
         float x = (ix-width/2)  / ((float) (height/2));

         this.grabType = grabType;
         this.grabbed  = true;
         this.xGrab = x;
         this.yGrab = y;
        
         grab_lookat_Point     = lookatPoint;
         grab_lookat_to_camera = cameraPosition.minus(lookatPoint);

         grab_zVector = grab_lookat_to_camera.copy();        grab_zVector.normalize();
         grab_xVector = grab_zVector.cross(cameraUpVector);  grab_xVector.normalize();
         grab_yVector = grab_xVector.cross(grab_zVector);    grab_yVector.normalize();


         if (grabType == GrabType.Rotate) {

            roll = (x*x+y*y) > 1;
            if (roll) grabAngle = (float) Math.atan2(y,x);

         } else if (grabType == GrabType.Zoom) {

            tScale = (y < 0.5) ? (1-y) : (y);

         } else if (grabType == GrabType.FOV) {

            tScale     = (y < 0.5) ? (1-y) : (y);
            fovTangent = ((float) Math.tan(verticalFOV * Math.PI / 360.0));

         } else if (grabType == GrabType.Pan) {
       
            windowScale = ((float) Math.tan(verticalFOV * Math.PI / 360.0))
                        * grab_lookat_to_camera.length();
         }
      }
      public void moveTo(int ix, int iy) {
         if (!grabbed) return;
         
         float y = (iy-height/2) / ((float) (height/2));
         float x = (ix-width/2)  / ((float) (height/2));

         float dx = x-xGrab;
         float dy = y-yGrab; 
         if ((dx==0) && (dy==0)) return;

         if (grabType == GrabType.Rotate) {
            
            Vector3f axis;
            float amount;

            if (roll) {
                        axis = new Vector3f (0,0,1); 
                        amount = (float) (Math.atan2(y,x) - grabAngle);
                        amount *= Scale2DRotation;
            } else { 
                        axis = new Vector3f ((float) dy,(float) -dx,0).normalize();
                        amount = ((float) Math.sqrt(dx*dx+dy*dy));  
                        amount *= Scale3DRotation;
            }
            
            Vector3f rotationAxis = grab_xVector.times(axis.x)
                              .plus(grab_yVector.times(axis.y))
                              .plus(grab_zVector.times(axis.z))
                              .normalized();
            
            cameraPosition = grab_lookat_to_camera.rotated(rotationAxis, amount).plus(lookatPoint);
            cameraUpVector = grab_yVector.rotated(rotationAxis, amount);
            
         } else if (grabType == GrabType.Zoom) {

            cameraPosition = grab_lookat_to_camera.multiply((float) Math.pow(ScaleZoom, dy/tScale)).plus(lookatPoint);

         } else if (grabType == GrabType.Pan) {

            Vector3f translation = grab_xVector.times(dx)
                             .plus(grab_yVector.times(dy))
                             .times(windowScale);

            lookatPoint = grab_lookat_Point.plus(translation);


         } else if (grabType == GrabType.FOV) {

            float newFovTangent = fovTangent * (float) Math.pow(ScaleZoom,dy/tScale);
            verticalFOV = (float) ((360.0f / Math.PI) * Math.atan(newFovTangent));

         }
      }
      public void release() {
         grabbed = false;
      }
      public boolean isGrabbed() {
         return grabbed;
      }

      
      // ----------------------------------------------------------
      
      private int width, height;
      private Vector3f lookatPoint;
      private Vector3f cameraPosition;
      private Vector3f cameraUpVector;
      private float verticalFOV;
      
      private boolean grabbed;
      private GrabType grabType;
      private float xGrab, yGrab;

      private Vector3f grab_lookat_Point, grab_lookat_to_camera;
      private Vector3f grab_zVector, grab_xVector, grab_yVector;

      // For rotate grabs
      private boolean roll;
      private float grabAngle;

      // For zoom grabs
      private float tScale;

      // For pan grabs
      private float windowScale;

      // For fovAdjust grabs
      private float fovTangent;

      private static final float ScaleZoom       = 3.0f;
      private static final float Scale2DRotation = 1.5f;
      private static final float Scale3DRotation = 2.0f;
      
   }
   
   
   // -----------------------------------------------------------------------
   // Vector2f
   // -----------------------------------------------------------------------
   
   public static class Vector2f {
      
      public float x;
      public float y;
  
      public Vector2f() {
         this(0.0f, 0.0f);
      }  
      public Vector2f(float x, float y) {
          this.x = x;
          this.y = y;
      }
      public Vector2f(Vector2f v) {
          this.x = v.x;
          this.y = v.y;
      }
      public Vector2f copy() {
         return new Vector2f(this);
      }       
      
      public Vector2f set(Vector2f v) {
         x = v.x;
         y = v.y;
         return this;
      }
      public Vector2f add(Vector2f v) {
         x += v.x;
         y += v.y;
         return this;
      }
      public Vector2f subtract(Vector2f v) {
         x -= v.x;
         y -= v.y;
         return this;
      }
      public Vector2f multiply(float s) {
         x *= s;
         y *= s;
         return this;
      }
      
      public Vector2f plus(Vector2f v) {
         return copy().add(v);
      }
      public Vector2f minus(Vector2f v) {
         return copy().subtract(v);
      }
      public Vector2f times(float s) {   
         return copy().multiply(s);
      }       
      
      public float dot(Vector2f v) {
         return x * v.x + y * v.y;
      }

      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector2f normalize() {
         return this.multiply(1.0f / length());
      }
      public Vector2f normalized() {
         return copy().normalize();
      }
      
      public String toString() {
         return String.format("(%g,%g)", x,y);
      }
      
      public static final Vector2f ORIGIN = new Vector2f(0.0f, 0.0f);
      public static final Vector2f X      = new Vector2f(1.0f, 0.0f);
      public static final Vector2f Y      = new Vector2f(0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Misc
   // -----------------------------------------------------------------------
   
   public static class ColorRGBA {
   
       public byte r;
       public byte g;
       public byte b;
       public byte a;
   
       public ColorRGBA() {}
   
       public ColorRGBA(byte r, byte g, byte b, byte a) {
           this.r = r;
           this.g = g;
           this.b = b;
           this.a = a;
       }
       
       public int toInteger() {
          return ((int)r)<<24 | ((int)g)<<16 | ((int)b)<<8 | ((int)a);
       }
       public String toString() {
          return String.format("#%02x%02x%02x%02x", r,g,b,a);
       }
   }

   

   public static class Triangle {
       private Vector3f  v1,v2,v3;       
       private Vector3f  n1,n2,n3;
       private Vector2f  t1,t2,t3;
       private ColorRGBA c1,c2,c3;
       
   }
      
   
}

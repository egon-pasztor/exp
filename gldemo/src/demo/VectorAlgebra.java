package demo;

public class VectorAlgebra {

   // -----------------------------------------------------------------------
   // Vector2f
   // -----------------------------------------------------------------------
   
   public static class Vector2f {
      
      public final float x, y;

      public Vector2f(float x_, float y_) {
         x = x_;  y = y_;
      }
      
      public String toString() {
         return String.format("( %10.3f )\n( %10.3f )\n", x,y);
      }
      public boolean equals(Vector3f v) {
         return (x == v.x) && (y == v.y);
      }
      
      public Vector2f plus(Vector2f v) {
          return new Vector2f(x + v.x, y + v.y);
      }
      public Vector2f minus(Vector2f v) {
          return new Vector2f(x - v.x, y - v.y);
      }
      public Vector2f times(float s) {   
          return new Vector2f(x * s, y * s);
      }       
      
      public static float innerProduct(Vector2f a, Vector2f b) {
         return a.x * b.x + a.y * b.y;
      }
      public static Vector2f termwiseProduct(Vector2f a, Vector2f b) {
         return new Vector2f(a.x * b.x, a.y * b.y);
      }
      public static Matrix2f outerProduct(Vector2f a, Vector2f b) {
         return new Matrix2f(
            a.x * b.x, a.x * b.y,
            a.y * b.x, a.y * b.y);
     }

      public float dot(Vector2f v) {
         return Vector2f.innerProduct(this, v);
      }

      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector2f normalized() {
         return this.times(1.0f / length());
      }

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y;
         return offset+2;
      }

      public static final Vector2f ORIGIN = new Vector2f(0.0f, 0.0f);
      public static final Vector2f X      = new Vector2f(1.0f, 0.0f);
      public static final Vector2f Y      = new Vector2f(0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Matrix2f
   // -----------------------------------------------------------------------
   
   public static class Matrix2f {
      
      public final float xx, xy,
                         yx, yy;
  
      public Matrix2f(float xx_, float xy_,
                      float yx_, float yy_) {

          xx = xx_;  xy = xy_;
          yx = yx_;  yy = yy_;
      }
      
      public String toString() {
         return String.format("( %10.3f %10.f )\n( %10.3f %10.f )\n", 
               xx,xy, yx,yy);
      }
      public boolean equals(Matrix3f m) {
         return (xx == m.xx) && (xy == m.xy)
             && (yx == m.yx) && (yy == m.yy);
      }
      
      public Matrix2f plus(Matrix2f m) {
         return new Matrix2f(
            xx + m.xx, xy + m.xy,
            yx + m.yx, yy + m.yy);
      }
      public Matrix2f minus(Matrix2f m) {
         return new Matrix2f(
            xx - m.xx, xy - m.xy,
            yx - m.yx, yy - m.yy);
      }
      public Matrix2f times(float s) {
         return new Matrix2f(
            xx * s, xy * s,
            yx * s, yy * s);
      }
      public Matrix2f transposed() {
         return new Matrix2f(xx, yx,
                             xy, yy);
      }

      public static Vector2f product(Vector2f a, Matrix2f b) {
         return new Vector2f (
            a.x * b.xx + a.y * b.yx,
            a.x * b.xy + a.y * b.yy); 
      }
      public static Vector2f product(Matrix2f a, Vector2f b) {
         return new Vector2f (
            a.xx * b.x + a.xy * b.y,
            a.yx * b.x + a.yy * b.y); 
      }
      public static Matrix2f product(Matrix2f a, Matrix2f b) {
         return new Matrix2f (
            a.xx * b.xx + a.xy * b.yx,
            a.xx * b.xy + a.xy * b.yy,
               
            a.yx * b.xx + a.yy * b.yx, 
            a.yx * b.xy + a.yy * b.yy);         
      }
      
      public float determinate() {
         return (xx*yy-xy*yx);
      }
      public Matrix2f inverse() {
         final float d = determinate();
         return new Matrix2f(
              +yy/d, -xy/d,
              -yx/d, +xx/d);
      }
      
      public static Matrix2f scaling(float s) {
         return new Matrix2f (   s, 0.0f,
                              0.0f,    s);
      }
      public static Matrix2f rotation(float angle) {
         final float sa = (float) Math.sin(angle);
         final float ca = (float) Math.cos(angle);
         return new Matrix2f (ca, -sa,
                              sa,  ca);
      }      
      
      public static final Matrix2f IDENTITY = new Matrix2f(1.0f, 0.0f,
                                                           0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Vector3f
   // -----------------------------------------------------------------------
   
   public static class Vector3f {
      
      public final float x, y, z;
   
      public Vector3f(float x_, float y_, float z_) {
         x = x_;  y = y_;  z = z_;
      }
      
      public String toString() {
         return String.format("( %10.3f )\n( %10.3f )\n( %10.3f )\n", x,y,z);
      }
      public boolean equals(Vector3f v) {
         return (x == v.x) && (y == v.y) && (z == v.z);
      }
       
      public Vector3f plus(Vector3f v) {
         return new Vector3f(x + v.x, y + v.y, z + v.z);
      }
      public Vector3f minus(Vector3f v) {
         return new Vector3f(x - v.x, y - v.y, z - v.z);
      }
      public Vector3f times(float s) {   
         return new Vector3f(x * s, y * s, z * s);
      }       
       
      public static float innerProduct(Vector3f a, Vector3f b) {
         return a.x * b.x + a.y * b.y + a.z * b.z;
      }
      public static Vector3f termwiseProduct(Vector3f a, Vector3f b) {
         return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
      }
      public static Vector3f crossProduct(Vector3f a, Vector3f b) {
         return new Vector3f(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x);
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
         return Vector3f.crossProduct(this, v);
      }
      
      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector3f normalized() {
         return this.times(1.0f / length());
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

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y; a[offset+2]=z;
         return offset+3;
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
      
      public final float xx, xy, xz,
                         yx, yy, yz,
                         zx, zy, zz;
  
      public Matrix3f(float xx_, float xy_, float xz_,
                      float yx_, float yy_, float yz_,
                      float zx_, float zy_, float zz_) {
         
         xx = xx_;  xy = xy_;  xz = xz_;
         yx = yx_;  yy = yy_;  yz = yz_;
         zx = zx_;  zy = zy_;  zz = zz_;
      }
      
      public String toString() {
         return String.format("( %10.3f %10.3f %10.3f )\n( %10.3f %10.3f %10.3f )\n"
                    +"( %10.3f %10.3f %10.3f )\n", xx,xy,xz, yx,yy,yz, zx,zy,zz);
      }
      public boolean equals(Matrix3f m) {
         return (xx == m.xx) && (xy == m.xy) && (xz == m.xz)
             && (yx == m.yx) && (yy == m.yy) && (yz == m.yz)
             && (zx == m.zx) && (zy == m.zy) && (zz == m.zz);
      }
      
      public Matrix3f plus(Matrix3f m) {
         return new Matrix3f(
            xx + m.xx, xy + m.xy, xz + m.xz,
            yx + m.yx, yy + m.yy, yz + m.yz,
            zx + m.zx, zy + m.zy, zz + m.zz);
      }
      public Matrix3f minus(Matrix3f m) {
         return new Matrix3f(
            xx - m.xx, xy - m.xy, xz - m.xz,
            yx - m.yx, yy - m.yy, yz - m.yz,
            zx - m.zx, zy - m.zy, zz - m.zz);
      }
      public Matrix3f times(float s) {
         return new Matrix3f(
            xx * s, xy * s, xz * s,
            yx * s, yy * s, yz * s,
            zx * s, zy * s, zz * s);
      }
      public Matrix3f transposed() {
         return new Matrix3f(xx, yx, zx,
                             xy, yy, zy,
                             xz, yz, zz);
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
      
      public float determinate() {
         return (yy*zz-zy*yz) * xx
              - (yx*zz-zx*yz) * xy
              + (yx*zy-zx*yy) * xz;
      }
      public Matrix3f inverse() {
         final float d = determinate();
         return new Matrix3f(
              +(yy*zz-zy*yz)/d, -(xy*zz-zy*xz)/d, +(xy*yz-yy*xz)/d, 
              -(yx*zz-zx*yz)/d, +(xx*zz-zx*xz)/d, -(xx*yz-yx*xz)/d,  
              +(yx*zy-zx*yy)/d, -(xx*zy-zx*xy)/d, +(xx*yy-yx*xy)/d);
      }

      // -------------------------------------------------------

      public static Matrix3f scaling(float s) {
         return new Matrix3f (   s,  0.0f,  0.0f,
                              0.0f,     s,  0.0f,
                              0.0f,  0.0f,    s);
      }
      public static Matrix3f nonuniformScaling(Vector3f s) {
         return new Matrix3f ( s.x,  0.0f,  0.0f,
                              0.0f,   s.y,  0.0f,
                              0.0f,  0.0f,   s.z);
      }
      public static Matrix3f rotation(Vector3f normalizedAxis, float angle) {
         final float sa = (float) Math.sin(angle);
         final float ca = (float) Math.cos(angle);
         final float x = normalizedAxis.x, y = normalizedAxis.y, z = normalizedAxis.z;
         return new Matrix3f (x*x*(1-ca)+ ca,   x*y*(1-ca)- sa*z, x*z*(1-ca)+ sa*y,
                              y*x*(1-ca)+ sa*z, y*y*(1-ca)+ ca,   y*z*(1-ca)- sa*x,
                              z*x*(1-ca)- sa*y, z*y*(1-ca)+ sa*x, z*z*(1-ca)+ ca    );   
      }
      public static Matrix3f fromRowVectors(Vector3f x, Vector3f y, Vector3f z) {
	      return new Matrix3f (x.x, x.y, x.z,
                              y.x, y.y, y.z,
                              z.x, z.y, z.z);
      }
      public static Matrix3f fromColumnVectors(Vector3f x, Vector3f y, Vector3f z) {
	      return new Matrix3f (x.x, y.x, z.x,
                              x.y, y.y, z.y,
                              x.z, y.z, z.z);
      }

      // -------------------------------------------------------
      
      public static final Matrix3f IDENTITY = new Matrix3f(1.0f, 0.0f, 0.0f,
                                                           0.0f, 1.0f, 0.0f,
                                                           0.0f, 0.0f, 1.0f);
   }


    /*
   LTrs *LTrsPerspectiveTransform (LTrs *t, double f, double d,
                                 double xh, double yh)
    { Mat4Set(&t->trs,f/xh,0,0,0,0,f/yh,0,0,0,0,f/(f-d),1,0,0,-d*f/(f-d),0);
      Mat4Set(&t->itrs,xh/f,0,0,0,0,yh/f,0,0,0,0,0,-(f-d)/(d*f),0,0,1,1/d);  
     return t; }
    */

   // -----------------------------------------------------------------------
   // Vector4f
   // -----------------------------------------------------------------------
   
   public static class Vector4f {
      
      public final float x, y, z, w;
   
      public Vector4f(float x_, float y_, float z_, float w_) {
         x = x_;  y = y_;  z = z_;  w = w_;
      }
      
      public String toString() {
         return String.format("( %10.3f )\n( %10.3f )\n( %10.3f )\n( %10.3f )\n", x,y,z,w);
      }
      public boolean equals(Vector4f v) {
         return (x == v.x) && (y == v.y) && (z == v.z) && (w == v.w);
      }
       
      public Vector4f plus(Vector4f v) {
         return new Vector4f(x + v.x, y + v.y, z + v.z, w + v.w);
      }
      public Vector4f minus(Vector4f v) {
         return new Vector4f(x - v.x, y - v.y, z - v.z, w - v.w);
      }
      public Vector4f times(float s) {   
         return new Vector4f(x * s, y * s, z * s, w + s);
      }       
       
      public static float innerProduct(Vector4f a, Vector4f b) {
         return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
      }
      public static Vector4f termwiseProduct(Vector4f a, Vector4f b) {
         return new Vector4f(a.x * b.x, a.y * b.y, a.z * b.z, a.w * b.w);
      }
      public static Matrix4f outerProduct(Vector4f a, Vector4f b) {
         return new Matrix4f(
            a.x * b.x, a.x * b.y, a.x * b.z, a.x * b.w,
            a.y * b.x, a.y * b.y, a.y * b.z, a.y * b.w,
            a.z * b.x, a.z * b.y, a.z * b.z, a.z * b.w,
            a.w * b.x, a.w * b.y, a.w * b.z, a.w * b.w);
      }

      public float dot(Vector4f v) {
         return Vector4f.innerProduct(this, v);
      }       
      
      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector4f normalized() {
         return this.times(1.0f / length());
      }
      
      // -------------------------------------------------------
      
      public static Vector4f fromVector3f(Vector3f v) {
         return Vector4f.fromVector3f(v, 1.0f);
      }
      public static Vector4f fromVector3f(Vector3f v, float w) {
         return new Vector4f(v.x * w, v.y * w, v.z * w, w);
      }
      public Vector3f toVector3f() {
         return new Vector3f(x / w, y / w, z / w);
      }

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y; a[offset+2]=z; a[offset+3]=w;
         return offset+4;
      }

      public static final Vector4f ORIGIN = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
      public static final Vector4f X      = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);
      public static final Vector4f Y      = new Vector4f(0.0f, 1.0f, 0.0f, 0.0f);
      public static final Vector4f Z      = new Vector4f(0.0f, 0.0f, 1.0f, 0.0f);
      public static final Vector4f W      = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // Matrix4f
   // -----------------------------------------------------------------------
   
   public static class Matrix4f {
      
      public final float xx, xy, xz, xw,
                         yx, yy, yz, yw,
                         zx, zy, zz, zw,
                         wx, wy, wz, ww;
  
      public Matrix4f(float xx_, float xy_, float xz_, float xw_,
                      float yx_, float yy_, float yz_, float yw_,
                      float zx_, float zy_, float zz_, float zw_,
                      float wx_, float wy_, float wz_, float ww_) {
         
          xx = xx_;  xy = xy_;  xz = xz_;  xw = xw_;
          yx = yx_;  yy = yy_;  yz = yz_;  yw = yw_;
          zx = zx_;  zy = zy_;  zz = zz_;  zw = zw_;
          wx = wx_;  wy = wy_;  wz = wz_;  ww = ww_;
      }
      
      public String toString() {
         return String.format("( %10.3f %10.3f %10.3f %10.3f )\n( %10.3f %10.3f %10.3f %10.3f )\n"
                             +"( %10.3f %10.3f %10.3f %10.3f )\n( %10.3f %10.3f %10.3f %10.3f )\n",
                             xx,xy,xz,xw, yx,yy,yz,yw, zx,zy,zz,zw, wx,wy,wz,ww);
      }
      public boolean equals(Matrix4f m) {
         return (xx == m.xx) && (xy == m.xy) && (xz == m.xz) && (xw == m.xw)
             && (yx == m.yx) && (yy == m.yy) && (yz == m.yz) && (yw == m.yw)
             && (zx == m.zx) && (zy == m.zy) && (zz == m.zz) && (zw == m.zw)
             && (wx == m.wx) && (wy == m.wy) && (wz == m.wz) && (ww == m.ww);
      }
      
      public Matrix4f plus(Matrix4f m) {
         return new Matrix4f(
            xx + m.xx, xy + m.xy, xz + m.xz, xw + m.xw,
            yx + m.yx, yy + m.yy, yz + m.yz, yw + m.yw,
            zx + m.zx, zy + m.zy, zz + m.zz, zw + m.zw,
            wx + m.wx, wy + m.wy, wz + m.wz, ww + m.ww);
      }
      public Matrix4f minus(Matrix4f m) {
         return new Matrix4f(
            xx - m.xx, xy - m.xy, xz - m.xz, xw - m.xw,
            yx - m.yx, yy - m.yy, yz - m.yz, yw - m.yw,
            zx - m.zx, zy - m.zy, zz - m.zz, zw - m.zw,
            wx - m.wx, wy - m.wy, wz - m.wz, ww - m.ww);
      }
      public Matrix4f times(float s) {
         return new Matrix4f(
            xx * s, xy * s, xz * s, xw * s,
            yx * s, yy * s, yz * s, yw * s,
            zx * s, zy * s, zz * s, zw * s,
            wx * s, wy * s, wz * s, ww * s);
      }
      public Matrix4f transposed() {
         return new Matrix4f(xx, yx, zx, wx,
                             xy, yy, zy, wy,
                             xz, yz, zz, wz,
                             xw, yw, zw, ww);
      }

      public static Vector4f product(Vector4f a, Matrix4f b) {
         return new Vector4f (
            a.x * b.xx + a.y * b.yx + a.z * b.zx + a.w * b.wx,
            a.x * b.xy + a.y * b.yy + a.z * b.zy + a.w * b.wy,
            a.x * b.xz + a.y * b.yz + a.z * b.zz + a.w * b.wz,
            a.x * b.xw + a.y * b.yw + a.z * b.zw + a.w * b.ww);
      }
      public static Vector4f product(Matrix4f a, Vector4f b) {
         return new Vector4f (
            a.xx * b.x + a.xy * b.y + a.xz * b.z + a.xw * b.w,
            a.yx * b.x + a.yy * b.y + a.yz * b.z + a.yw * b.w,
            a.zx * b.x + a.zy * b.y + a.zz * b.z + a.zw * b.w,
            a.wx * b.x + a.wy * b.y + a.wz * b.z + a.ww * b.w); 
      }
      public static Matrix4f product(Matrix4f a, Matrix4f b) {
         return new Matrix4f (
            a.xx * b.xx + a.xy * b.yx + a.xz * b.zx + a.xw * b.wx,  
            a.xx * b.xy + a.xy * b.yy + a.xz * b.zy + a.xw * b.wy,
            a.xx * b.xz + a.xy * b.yz + a.xz * b.zz + a.xw * b.wz,
            a.xx * b.xw + a.xy * b.yw + a.xz * b.zw + a.xw * b.ww,
               
            a.yx * b.xx + a.yy * b.yx + a.yz * b.zx + a.yw * b.wx,  
            a.yx * b.xy + a.yy * b.yy + a.yz * b.zy + a.yw * b.wy,
            a.yx * b.xz + a.yy * b.yz + a.yz * b.zz + a.yw * b.wz,
            a.yx * b.xw + a.yy * b.yw + a.yz * b.zw + a.yw * b.ww,
               
            a.zx * b.xx + a.zy * b.yx + a.zz * b.zx + a.zw * b.wx,  
            a.zx * b.xy + a.zy * b.yy + a.zz * b.zy + a.zw * b.wy,
            a.zx * b.xz + a.zy * b.yz + a.zz * b.zz + a.zw * b.wz,
            a.zx * b.xw + a.zy * b.yw + a.zz * b.zw + a.zw * b.ww,
               
            a.wx * b.xx + a.wy * b.yx + a.wz * b.zx + a.ww * b.wx,  
            a.wx * b.xy + a.wy * b.yy + a.wz * b.zy + a.ww * b.wy,
            a.wx * b.xz + a.wy * b.yz + a.wz * b.zz + a.ww * b.wz,
            a.wx * b.xw + a.wy * b.yw + a.wz * b.zw + a.ww * b.ww);
      }
      
      // --------------------
      
      public static Matrix4f fromComponents(Matrix3f m, Vector3f t, Vector4f w) {
         return new Matrix4f (m.xx, m.xy, m.xz, t.x,
			                     m.yx, m.yy, m.yz, t.y,
			                     m.zx, m.zy, m.zz, t.z,
			                     w.x,  w.y,  w.z,  w.w);
      }
      public static Matrix4f fromMatrix3f(Matrix3f m) {
         return Matrix4f.fromComponents(m, Vector3f.ORIGIN, Vector4f.W);
      }
      public static Matrix4f translation(Vector3f t) {
         return Matrix4f.fromComponents(Matrix3f.IDENTITY, t, Vector4f.W);
      }

      public void copyToFloatArray(float[] a) {
         a[0]=xx; a[4]=xy; a[8] =xz; a[12]=xw;
         a[1]=yx; a[5]=yy; a[9] =yz; a[13]=yw;
         a[2]=zx; a[6]=zy; a[10]=zz; a[14]=zw;
         a[3]=wx; a[7]=wy; a[11]=wz; a[15]=ww;
      }
      
      public static final Matrix4f IDENTITY = new Matrix4f(1.0f, 0.0f, 0.0f, 0.0f,
                                                           0.0f, 1.0f, 0.0f, 0.0f,
                                                           0.0f, 0.0f, 1.0f, 0.0f,
                                                           0.0f, 0.0f, 0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Intersections
   // -----------------------------------------------------------------------

   public static class Segment {
      public final Vector3f p0;
      public final Vector3f p1;
      public Segment(Vector3f p0, Vector3f p1) {
         this.p0 = p0;
         this.p1 = p1;
      }
   }
   public static class Triangle {
      public final Vector3f v0;
      public final Vector3f v1;
      public final Vector3f v2;
      public Triangle(Vector3f v0, Vector3f v1, Vector3f v2) {
         this.v0 = v0;
         this.v1 = v1;
         this.v2 = v2;
      }
   }
   public static boolean intersects(Triangle t, Segment s) {
      Vector3f u = t.v1.minus(t.v0);
      Vector3f v = t.v2.minus(t.v0);
      Vector3f n = u.cross(v).normalized();

      // http://geomalgorithms.com/a06-_intersect-2.html

      float den = n.dot(s.p1.minus(s.p0));
      if (den == 0) return false;

      float r1 = n.dot(t.v0.minus(s.p0)) / den;
      Vector3f i = s.p0.plus(s.p1.minus(s.p0).times(r1));
      Vector3f w = i.minus(t.v0);
 
      float den2 = u.dot(v) * u.dot(v)
                 - u.dot(u) * v.dot(v);
      if (den2 == 0) return false;

      float snum = u.dot(v) * w.dot(v)
                 - v.dot(v) * w.dot(u);

      float tnum = u.dot(v) * w.dot(u)
                 - u.dot(u) * w.dot(v);

      float sc = snum/den2;
      float tc = tnum/den2;
      if (sc<0) return false;
      if (tc<0) return false;
      if (sc+tc>1) return false;
      return true;
   }

   // -----------------------------------------------------------------------
   // TESTING
   // -----------------------------------------------------------------------

   public static void testRotation() {
      
      Vector3f v = new Vector3f (randf(),randf(),randf());
      Vector3f axis = new Vector3f (randf(),randf(),randf()).normalized();
      float angle = 10*randf();
      
      Vector3f v1 = v.rotated(axis, angle);
      Vector3f v2 = Matrix3f.product(Matrix3f.rotation(axis, angle), v);
      
      boolean ok = cmp(v1.x,v2.x) && cmp(v1.y,v2.y) && cmp(v1.z,v2.z);
      System.out.format("Rotation: %s\n",  ok?"OK":"PROBLEM");
   }   
   public static void testInverse3() {
      Matrix3f a = new Matrix3f (
            randf(), randf(), randf(),
            randf(), randf(), randf(),
            randf(), randf(), randf());
      
      Matrix3f b = a.inverse();
      
      Matrix3f ab = Matrix3f.product(a, b);
      Matrix3f ba = Matrix3f.product(b, a);
      
      // System.out.format("Matrix A:  %s\n", a.toString());
      // System.out.format("Matrix B:  %s\n", b.toString());
      // System.out.format("Matrix AB: %s\n", ab.toString());
      // System.out.format("Matrix BA: %s\n", ba.toString());
      
      boolean ok = isIdentity(ab) && isIdentity(ba);
      System.out.format("Inverse3D: %s\n",  ok?"OK":"PROBLEM");
   }
   public static void testInverse2() {
      Matrix2f a = new Matrix2f (
            randf(), randf(),
            randf(), randf());
      
      Matrix2f b = a.inverse();
      
      Matrix2f ab = Matrix2f.product(a, b);
      Matrix2f ba = Matrix2f.product(b, a);
      
      // System.out.format("Matrix A:  %s\n", a.toString());
      // System.out.format("Matrix B:  %s\n", b.toString());
      // System.out.format("Matrix AB: %s\n", ab.toString());
      // System.out.format("Matrix BA: %s\n", ba.toString());
      
      boolean ok = isIdentity(ab) && isIdentity(ba);
      System.out.format("Inverse2D: %s\n",  ok?"OK":"PROBLEM");
   }
   
   private static boolean cmp(float a, float b) {
      float eps = 0.0001f;
      float diff = a-b;
      return (diff < eps) && (diff > -eps);
   }   
   private static boolean isIdentity(Matrix3f m) {
      return cmp(m.xx, 1.0f) && cmp(m.xy, 0.0f) && cmp(m.xz, 0.0f)
          && cmp(m.yx, 0.0f) && cmp(m.yy, 1.0f) && cmp(m.yz, 0.0f)
          && cmp(m.zx, 0.0f) && cmp(m.zy, 0.0f) && cmp(m.zz, 1.0f);
   }
   private static boolean isIdentity(Matrix2f m) {
      return cmp(m.xx, 1.0f) && cmp(m.xy, 0.0f) 
          && cmp(m.yx, 0.0f) && cmp(m.yy, 1.0f);
   }
   
   private static float randf() {
      return (float)(Math.random()*4.0 - 2.0);
   }
}

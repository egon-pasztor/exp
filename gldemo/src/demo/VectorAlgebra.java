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
   // CAMERA
   // -----------------------------------------------------------------------
   
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

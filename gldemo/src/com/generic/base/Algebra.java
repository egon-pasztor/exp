package com.generic.base;

import java.util.ArrayList;

public class Algebra {

   
   // -----------------------------------------------------------------------
   // Vector2
   // -----------------------------------------------------------------------
   
   public static class Vector2 {
      
      public final float x, y;

      public Vector2(float x_, float y_) {
         x = x_;  y = y_;
      }
      public static Vector2 of(float x, float y) {
         return new Vector2(x,y);
      }
      
      public String toString() {
         return String.format("( %10.9f )\n( %10.9f )\n", x,y);
      }
      public boolean equals(Vector3 v) {
         return (x == v.x) && (y == v.y);
      }
      
      public Vector2 plus(Vector2 v) {
          return new Vector2(x + v.x, y + v.y);
      }
      public Vector2 minus(Vector2 v) {
          return new Vector2(x - v.x, y - v.y);
      }
      public Vector2 times(float s) {   
          return new Vector2(x * s, y * s);
      }       
      
      public static float innerProduct(Vector2 a, Vector2 b) {
         return a.x * b.x + a.y * b.y;
      }
      public static Vector2 termwiseProduct(Vector2 a, Vector2 b) {
         return new Vector2(a.x * b.x, a.y * b.y);
      }
      public static Matrix2x2 outerProduct(Vector2 a, Vector2 b) {
         return new Matrix2x2(
            a.x * b.x, a.x * b.y,
            a.y * b.x, a.y * b.y);
      }

      public float dot(Vector2 v) {
         return Vector2.innerProduct(this, v);
      }

      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector2 normalized() {
         return this.times(1.0f / length());
      }

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y;
         return offset+2;
      }

      public static final Vector2 ORIGIN = new Vector2(0.0f, 0.0f);
      public static final Vector2 X      = new Vector2(1.0f, 0.0f);
      public static final Vector2 Y      = new Vector2(0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Matrix2x2
   // -----------------------------------------------------------------------
   
   public static class Matrix2x2 {
      
      public final float xx, xy,
                         yx, yy;
  
      public Matrix2x2(float xx_, float xy_,
                      float yx_, float yy_) {

          xx = xx_;  xy = xy_;
          yx = yx_;  yy = yy_;
      }
      
      public String toString() {
         return String.format("( %10.3f %10.f )\n( %10.3f %10.f )\n", 
               xx,xy, yx,yy);
      }
      public boolean equals(Matrix3x3 m) {
         return (xx == m.xx) && (xy == m.xy)
             && (yx == m.yx) && (yy == m.yy);
      }
      
      public Matrix2x2 plus(Matrix2x2 m) {
         return new Matrix2x2(
            xx + m.xx, xy + m.xy,
            yx + m.yx, yy + m.yy);
      }
      public Matrix2x2 minus(Matrix2x2 m) {
         return new Matrix2x2(
            xx - m.xx, xy - m.xy,
            yx - m.yx, yy - m.yy);
      }
      public Matrix2x2 times(float s) {
         return new Matrix2x2(
            xx * s, xy * s,
            yx * s, yy * s);
      }
      public Matrix2x2 transposed() {
         return new Matrix2x2(xx, yx,
                             xy, yy);
      }

      public static Vector2 product(Vector2 a, Matrix2x2 b) {
         return new Vector2 (
            a.x * b.xx + a.y * b.yx,
            a.x * b.xy + a.y * b.yy); 
      }
      public static Vector2 product(Matrix2x2 a, Vector2 b) {
         return new Vector2 (
            a.xx * b.x + a.xy * b.y,
            a.yx * b.x + a.yy * b.y); 
      }
      public static Matrix2x2 product(Matrix2x2 a, Matrix2x2 b) {
         return new Matrix2x2 (
            a.xx * b.xx + a.xy * b.yx,
            a.xx * b.xy + a.xy * b.yy,
               
            a.yx * b.xx + a.yy * b.yx, 
            a.yx * b.xy + a.yy * b.yy);         
      }
      
      public float determinate() {
         return (xx*yy-xy*yx);
      }
      public Matrix2x2 inverse() {
         final float d = determinate();
         return new Matrix2x2(
              +yy/d, -xy/d,
              -yx/d, +xx/d);
      }
      
      public static Matrix2x2 scaling(float s) {
         return new Matrix2x2 (   s, 0.0f,
                              0.0f,    s);
      }
      public static Matrix2x2 rotation(float angle) {
         final float sa = (float) Math.sin(angle);
         final float ca = (float) Math.cos(angle);
         return new Matrix2x2 (ca, -sa,
                              sa,  ca);
      }      
      
      public static final Matrix2x2 IDENTITY = new Matrix2x2(1.0f, 0.0f,
                                                           0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Vector3
   // -----------------------------------------------------------------------
   
   public static class Vector3 {
      
      public final float x, y, z;
   
      public Vector3(float x_, float y_, float z_) {
         x = x_;  y = y_;  z = z_;
      }
      public static Vector3 of(float x, float y, float z) {
         return new Vector3(x,y,z);
      }
      
      public String toString() {
         return String.format("( %10.9f )\n( %10.9f )\n( %10.9f )\n", x,y,z);
      }
      public boolean equals(Vector3 v) {
         return (x == v.x) && (y == v.y) && (z == v.z);
      }
      public boolean equals(Object o) {
         return (o instanceof Vector3) && equals((Vector3)o);
      }
       
      public Vector3 plus(Vector3 v) {
         return new Vector3(x + v.x, y + v.y, z + v.z);
      }
      public Vector3 minus(Vector3 v) {
         return new Vector3(x - v.x, y - v.y, z - v.z);
      }
      public Vector3 times(float s) {   
         return new Vector3(x * s, y * s, z * s);
      }       
       
      public static float innerProduct(Vector3 a, Vector3 b) {
         return a.x * b.x + a.y * b.y + a.z * b.z;
      }
      public static Vector3 termwiseProduct(Vector3 a, Vector3 b) {
         return new Vector3(a.x * b.x, a.y * b.y, a.z * b.z);
      }
      public static Vector3 crossProduct(Vector3 a, Vector3 b) {
         return new Vector3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x);
      }
      public static Matrix3x3 outerProduct(Vector3 a, Vector3 b) {
         return new Matrix3x3(
            a.x * b.x, a.x * b.y, a.x * b.z,
            a.y * b.x, a.y * b.y, a.y * b.z,
            a.z * b.x, a.z * b.y, a.z * b.z);
      }

      public float dot(Vector3 v) {
         return Vector3.innerProduct(this, v);
      }       
      public Vector3 cross(Vector3 v) {
         return Vector3.crossProduct(this, v);
      }
      
      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector3 normalized() {
         return this.times(1.0f / length());
      }

      public Vector3 rotated(Vector3 normalizedAxis, float angle) {
         Vector3 qv = normalizedAxis.times((float) Math.sin(angle / 2.0f));
         float    qs = (float) Math.cos(angle / 2.0f);
         Vector3 mv = qv.cross(this).plus(this.times(qs));
         return qv.cross(mv).plus(mv.times(qs)).plus(qv.times(this.dot(qv)));
      }
      public Vector3 interpolated(Vector3 target, float fraction) {
         float remainder = 1.0f - fraction;
         return new Vector3(x * remainder + target.x * fraction,
                             y * remainder + target.y * fraction,
                            z * remainder + target.z * fraction);
      }

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y; a[offset+2]=z;
         return offset+3;
      }
      public static Vector3 fromFloatArray(float[] a, int offset) {
         return new Vector3(a[offset+0], a[offset+1], a[offset+2]);
      }

      public static final Vector3 ORIGIN = new Vector3(0.0f, 0.0f, 0.0f);
      public static final Vector3 X      = new Vector3(1.0f, 0.0f, 0.0f);
      public static final Vector3 Y      = new Vector3(0.0f, 1.0f, 0.0f);
      public static final Vector3 Z      = new Vector3(0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // Matrix3x3
   // -----------------------------------------------------------------------
   
   public static class Matrix3x3 {
      
      public final float xx, xy, xz,
                         yx, yy, yz,
                         zx, zy, zz;
  
      public Matrix3x3(float xx_, float xy_, float xz_,
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
      public boolean equals(Matrix3x3 m) {
         return (xx == m.xx) && (xy == m.xy) && (xz == m.xz)
             && (yx == m.yx) && (yy == m.yy) && (yz == m.yz)
             && (zx == m.zx) && (zy == m.zy) && (zz == m.zz);
      }
      
      public Matrix3x3 plus(Matrix3x3 m) {
         return new Matrix3x3(
            xx + m.xx, xy + m.xy, xz + m.xz,
            yx + m.yx, yy + m.yy, yz + m.yz,
            zx + m.zx, zy + m.zy, zz + m.zz);
      }
      public Matrix3x3 minus(Matrix3x3 m) {
         return new Matrix3x3(
            xx - m.xx, xy - m.xy, xz - m.xz,
            yx - m.yx, yy - m.yy, yz - m.yz,
            zx - m.zx, zy - m.zy, zz - m.zz);
      }
      public Matrix3x3 times(float s) {
         return new Matrix3x3(
            xx * s, xy * s, xz * s,
            yx * s, yy * s, yz * s,
            zx * s, zy * s, zz * s);
      }
      public Matrix3x3 transposed() {
         return new Matrix3x3(xx, yx, zx,
                             xy, yy, zy,
                             xz, yz, zz);
      }

      public static Vector3 product(Vector3 a, Matrix3x3 b) {
         return new Vector3 (
            a.x * b.xx + a.y * b.yx + a.z * b.zx,
            a.x * b.xy + a.y * b.yy + a.z * b.zy,
            a.x * b.xz + a.y * b.yz + a.z * b.zz); 
      }
      public static Vector3 product(Matrix3x3 a, Vector3 b) {
         return new Vector3 (
            a.xx * b.x + a.xy * b.y + a.xz * b.z,
            a.yx * b.x + a.yy * b.y + a.yz * b.z,
            a.zx * b.x + a.zy * b.y + a.zz * b.z); 
      }
      public static Matrix3x3 product(Matrix3x3 a, Matrix3x3 b) {
         return new Matrix3x3 (
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
         return xx * (yy*zz-yz*zy)
              - xy * (yx*zz-yz*zx)
              + xz * (yx*zy-yy*zx);
      }
      public Matrix3x3 inverse() {
         final float d = determinate();
         return new Matrix3x3(
              +(yy*zz-zy*yz)/d, -(xy*zz-zy*xz)/d, +(xy*yz-yy*xz)/d, 
              -(yx*zz-zx*yz)/d, +(xx*zz-zx*xz)/d, -(xx*yz-yx*xz)/d,  
              +(yx*zy-zx*yy)/d, -(xx*zy-zx*xy)/d, +(xx*yy-yx*xy)/d);
      }

      // -------------------------------------------------------

      public static Matrix3x3 scaling(float s) {
         return new Matrix3x3 (   s,  0.0f,  0.0f,
                              0.0f,     s,  0.0f,
                              0.0f,  0.0f,    s);
      }
      public static Matrix3x3 scaling(float sx, float sy, float sz) {
         return new Matrix3x3 (  sx,  0.0f,  0.0f,
                              0.0f,    sy,  0.0f,
                              0.0f,  0.0f,   sz);
      }
      public static Matrix3x3 nonuniformScaling(Vector3 s) {
         return new Matrix3x3 ( s.x,  0.0f,  0.0f,
                              0.0f,   s.y,  0.0f,
                              0.0f,  0.0f,   s.z);
      }
      public static Matrix3x3 rotation(Vector3 normalizedAxis, float angle) {
         final float sa = (float) Math.sin(angle);
         final float ca = (float) Math.cos(angle);
         final float x = normalizedAxis.x, y = normalizedAxis.y, z = normalizedAxis.z;
         return new Matrix3x3 (x*x*(1-ca)+ ca,   x*y*(1-ca)- sa*z, x*z*(1-ca)+ sa*y,
                              y*x*(1-ca)+ sa*z, y*y*(1-ca)+ ca,   y*z*(1-ca)- sa*x,
                              z*x*(1-ca)- sa*y, z*y*(1-ca)+ sa*x, z*z*(1-ca)+ ca    );   
      }
      public static Matrix3x3 fromRowVectors(Vector3 x, Vector3 y, Vector3 z) {
	      return new Matrix3x3 (x.x, x.y, x.z,
                              y.x, y.y, y.z,
                              z.x, z.y, z.z);
      }
      public static Matrix3x3 fromColumnVectors(Vector3 x, Vector3 y, Vector3 z) {
	      return new Matrix3x3 (x.x, y.x, z.x,
                              x.y, y.y, z.y,
                              x.z, y.z, z.z);
      }

      // -------------------------------------------------------
      
      public static final Matrix3x3 IDENTITY = new Matrix3x3(1.0f, 0.0f, 0.0f,
                                                           0.0f, 1.0f, 0.0f,
                                                           0.0f, 0.0f, 1.0f);
   }

   // -----------------------------------------------------------------------
   // Vector4
   // -----------------------------------------------------------------------
   
   public static class Vector4 {
      
      public final float x, y, z, w;
   
      public Vector4(float x_, float y_, float z_, float w_) {
         x = x_;  y = y_;  z = z_;  w = w_;
      }
      public static Vector4 of(float x, float y, float z, float w) {
         return new Vector4(x,y,z,w);
      }
      
      public String toString() {
         return String.format("( %10.3f )\n( %10.3f )\n( %10.3f )\n( %10.3f )\n", x,y,z,w);
      }
      public boolean equals(Vector4 v) {
         return (x == v.x) && (y == v.y) && (z == v.z) && (w == v.w);
      }
       
      public Vector4 plus(Vector4 v) {
         return new Vector4(x + v.x, y + v.y, z + v.z, w + v.w);
      }
      public Vector4 minus(Vector4 v) {
         return new Vector4(x - v.x, y - v.y, z - v.z, w - v.w);
      }
      public Vector4 times(float s) {   
         return new Vector4(x * s, y * s, z * s, w + s);
      }       
       
      public static float innerProduct(Vector4 a, Vector4 b) {
         return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
      }
      public static Vector4 termwiseProduct(Vector4 a, Vector4 b) {
         return new Vector4(a.x * b.x, a.y * b.y, a.z * b.z, a.w * b.w);
      }
      public static Matrix4x4 outerProduct(Vector4 a, Vector4 b) {
         return new Matrix4x4(
            a.x * b.x, a.x * b.y, a.x * b.z, a.x * b.w,
            a.y * b.x, a.y * b.y, a.y * b.z, a.y * b.w,
            a.z * b.x, a.z * b.y, a.z * b.z, a.z * b.w,
            a.w * b.x, a.w * b.y, a.w * b.z, a.w * b.w);
      }

      public float dot(Vector4 v) {
         return Vector4.innerProduct(this, v);
      }       
      
      public float lengthSq() {
         return this.dot(this);
      }
      public float length() {
         return (float) Math.sqrt(lengthSq());
      }
      public Vector4 normalized() {
         return this.times(1.0f / length());
      }
      
      // -------------------------------------------------------
      
      public static Vector4 fromVector3f(Vector3 v) {
         return Vector4.fromVector3f(v, 1.0f);
      }
      public static Vector4 fromVector3f(Vector3 v, float w) {
         return new Vector4(v.x * w, v.y * w, v.z * w, w);
      }
      public Vector3 toVector3f() {
         return new Vector3(x / w, y / w, z / w);
      }

      // -------------------------------------------------------

      public int copyToFloatArray(float[] a, int offset) {
         a[offset+0]=x; a[offset+1]=y; a[offset+2]=z; a[offset+3]=w;
         return offset+4;
      }

      public static final Vector4 ORIGIN = new Vector4(0.0f, 0.0f, 0.0f, 0.0f);
      public static final Vector4 X      = new Vector4(1.0f, 0.0f, 0.0f, 0.0f);
      public static final Vector4 Y      = new Vector4(0.0f, 1.0f, 0.0f, 0.0f);
      public static final Vector4 Z      = new Vector4(0.0f, 0.0f, 1.0f, 0.0f);
      public static final Vector4 W      = new Vector4(0.0f, 0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // Matrix4x4
   // -----------------------------------------------------------------------
   
   public static class Matrix4x4 {
      
      public final float xx, xy, xz, xw,
                         yx, yy, yz, yw,
                         zx, zy, zz, zw,
                         wx, wy, wz, ww;
  
      public Matrix4x4(float xx_, float xy_, float xz_, float xw_,
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
      public boolean equals(Matrix4x4 m) {
         return (xx == m.xx) && (xy == m.xy) && (xz == m.xz) && (xw == m.xw)
             && (yx == m.yx) && (yy == m.yy) && (yz == m.yz) && (yw == m.yw)
             && (zx == m.zx) && (zy == m.zy) && (zz == m.zz) && (zw == m.zw)
             && (wx == m.wx) && (wy == m.wy) && (wz == m.wz) && (ww == m.ww);
      }
      
      public Matrix4x4 plus(Matrix4x4 m) {
         return new Matrix4x4(
            xx + m.xx, xy + m.xy, xz + m.xz, xw + m.xw,
            yx + m.yx, yy + m.yy, yz + m.yz, yw + m.yw,
            zx + m.zx, zy + m.zy, zz + m.zz, zw + m.zw,
            wx + m.wx, wy + m.wy, wz + m.wz, ww + m.ww);
      }
      public Matrix4x4 minus(Matrix4x4 m) {
         return new Matrix4x4(
            xx - m.xx, xy - m.xy, xz - m.xz, xw - m.xw,
            yx - m.yx, yy - m.yy, yz - m.yz, yw - m.yw,
            zx - m.zx, zy - m.zy, zz - m.zz, zw - m.zw,
            wx - m.wx, wy - m.wy, wz - m.wz, ww - m.ww);
      }
      public Matrix4x4 times(float s) {
         return new Matrix4x4(
            xx * s, xy * s, xz * s, xw * s,
            yx * s, yy * s, yz * s, yw * s,
            zx * s, zy * s, zz * s, zw * s,
            wx * s, wy * s, wz * s, ww * s);
      }
      public Matrix4x4 transposed() {
         return new Matrix4x4(xx, yx, zx, wx,
                             xy, yy, zy, wy,
                             xz, yz, zz, wz,
                             xw, yw, zw, ww);
      }

      public static Vector4 product(Vector4 a, Matrix4x4 b) {
         return new Vector4 (
            a.x * b.xx + a.y * b.yx + a.z * b.zx + a.w * b.wx,
            a.x * b.xy + a.y * b.yy + a.z * b.zy + a.w * b.wy,
            a.x * b.xz + a.y * b.yz + a.z * b.zz + a.w * b.wz,
            a.x * b.xw + a.y * b.yw + a.z * b.zw + a.w * b.ww);
      }
      public static Vector4 product(Matrix4x4 a, Vector4 b) {
         return new Vector4 (
            a.xx * b.x + a.xy * b.y + a.xz * b.z + a.xw * b.w,
            a.yx * b.x + a.yy * b.y + a.yz * b.z + a.yw * b.w,
            a.zx * b.x + a.zy * b.y + a.zz * b.z + a.zw * b.w,
            a.wx * b.x + a.wy * b.y + a.wz * b.z + a.ww * b.w); 
      }
      public static Matrix4x4 product(Matrix4x4 a, Matrix4x4 b) {
         return new Matrix4x4 (
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
      

      public float determinate() {
         return xx * (yy * (zz*ww-wz*zw) - yz * (zy*ww-wy*zw) + yw * (zy*wz-wy*zz))
              - xy * (yx * (zz*ww-wz*zw) - yz * (zx*ww-wx*zw) + yw * (zx*wz-wx*zz))
              + xz * (yx * (zy*ww-wy*zw) - yy * (zx*ww-wx*zw) + yw * (zx*wy-wx*zy))
              - xw * (yx * (zy*wz-wy*zz) - yy * (zx*wz-wx*zz) + yz * (zx*wy-wx*zy));
      }

      public Matrix4x4 inverse() {
         final float d = determinate();
         return null; //new Matrix4f(
           // https://www.mathsisfun.com/algebra/matrix-inverse-minors-cofactors-adjugate.html
               
         // given a 4x4 matrix inverse, i can find the X,Y,Z -> U,V,W transform 
         //   to translate a given <X,Y,Z> vector into <U,V,W> space.
         //
         // (is there any faster way of computing the uv offsets of given xyz offsets?)
         //    it's a simple question, right?
         // given dx,dy,dz, and the knowedge that it's in the u,v plane, how much is du,dv?
      }
      
      
      // --------------------
      
      public static Matrix4x4 fromComponents(Matrix3x3 m, Vector3 t, Vector4 w) {
         return new Matrix4x4 (m.xx, m.xy, m.xz, t.x,
			                     m.yx, m.yy, m.yz, t.y,
			                     m.zx, m.zy, m.zz, t.z,
			                     w.x,  w.y,  w.z,  w.w);
      }
      public static Matrix4x4 fromMatrix3f(Matrix3x3 m) {
         return Matrix4x4.fromComponents(m, Vector3.ORIGIN, Vector4.W);
      }
      public static Matrix4x4 translation(Vector3 t) {
         return Matrix4x4.fromComponents(Matrix3x3.IDENTITY, t, Vector4.W);
      }

      public void copyToFloatArray(float[] a) {
         a[0]=xx; a[4]=xy; a[8] =xz; a[12]=xw;
         a[1]=yx; a[5]=yy; a[9] =yz; a[13]=yw;
         a[2]=zx; a[6]=zy; a[10]=zz; a[14]=zw;
         a[3]=wx; a[7]=wy; a[11]=wz; a[15]=ww;
      }
      
      public static final Matrix4x4 IDENTITY = new Matrix4x4(1.0f, 0.0f, 0.0f, 0.0f,
                                                             0.0f, 1.0f, 0.0f, 0.0f,
                                                             0.0f, 0.0f, 1.0f, 0.0f,
                                                             0.0f, 0.0f, 0.0f, 1.0f);
   }
   
   // -----------------------------------------------------------------------
   // TODO: 
   // These 3D Geometry things should be in the "Geometry" file...
   // 
   // Intersections
   // -----------------------------------------------------------------------
   
   public static class Segment2 {
      public final Vector2 p0;
      public final Vector2 p1;
      public Segment2(Vector2 p0, Vector2 p1) {
         this.p0 = p0;
         this.p1 = p1;
      }
   }
   public static class Triangle2 {
      public final Vector2 v0;
      public final Vector2 v1;
      public final Vector2 v2;
      public Triangle2(Vector2 v0, Vector2 v1, Vector2 v2) {
         this.v0 = v0;
         this.v1 = v1;
         this.v2 = v2;
      }
   }
   
   public static class Segment3 {
      public final Vector3 start;
      public final Vector3 end;
      public Segment3(Vector3 start, Vector3 end) {
         this.start = start;
         this.end = end;
      }
      public Vector3 interpolate(float fraction) {
         return start.times(1.0f-fraction).plus(end.times(fraction));
      }
      public Vector3 getVector() {
         return end.minus(start);
      }
   }
   
   public static class Triangle3 {
      public final Vector3 v0;
      public final Vector3 v1;
      public final Vector3 v2;
      public Triangle3(Vector3 v0, Vector3 v1, Vector3 v2) {
         this.v0 = v0;
         this.v1 = v1;
         this.v2 = v2;
      }
   }
      
   public static Vector4 intersects(Triangle3 t, Segment3 s, boolean intersectionMustBeOnSegment) {
      Vector3 u = t.v1.minus(t.v0);
      Vector3 v = t.v2.minus(t.v0);
      Vector3 n = u.cross(v).normalized();
      
      // Difference in Height above the plane of the triangle, between "s.start" and "s.end"
      float den = n.dot(s.end.minus(s.start));
      if (den == 0) return null;
      if (den > 0) return null;
      
      // Height above the plane of the triangle, of point "s.start" 
      float d = n.dot(s.start.minus(t.v0));
      
      // The segment-parameter of the intersection point
      float frac = - d/den;
      if (intersectionMustBeOnSegment && ((frac < 0.0f) || (frac > 1.0f))) return null;
      
      // The intersection point located
      Vector3 intersectionPoint = s.start.plus(s.end.minus(s.start).times(frac));
      /*
      System.out.format("IntersectionPOINT at %g,%g,%g with frac %g\n", 
            intersectionPoint.x,
            intersectionPoint.y,
            intersectionPoint.z,
            frac);
      */
      // w points to the intersection point from v0,
      Vector3 w = intersectionPoint.minus(t.v0);
      
      // Compute parametric coordinates in triangle
      float dot00 = u.dot(u);
      float dot01 = u.dot(v);
      float dot02 = u.dot(w);
      float dot11 = v.dot(v);
      float dot12 = v.dot(w);

      float scale = 1.0f / (dot00 * dot11 - dot01 * dot01);
      float l1 = (dot11 * dot02 - dot01 * dot12) * scale;
      float l2 = (dot00 * dot12 - dot01 * dot02) * scale;
      float l0 = 1.0f - l1 - l2;
      //System.out.format("Intersection at %g,%g,%g\n",l0,l1,l2);
      
      // Now <l1,l2,l3> are the parametric coordinates of the intersection point in the triangle
      if ((l0 < 0.0f) || (l1 < 0.0f) || (l2 < 0.0f)) return null;      
      
      // Return the Barycentric Coordinates of the intersection
      return new Vector4(l0,l1,l2, frac);
   }

   public static Vector3 contains(Triangle2 t, Vector2 p) {
      Vector2 u = t.v1.minus(t.v0);
      Vector2 v = t.v2.minus(t.v0);
      Vector2 w =    p.minus(t.v0);

      // Compute parametric coordinates in triangle
      float dot00 = u.dot(u);
      float dot01 = u.dot(v);
      float dot02 = u.dot(w);
      float dot11 = v.dot(v);
      float dot12 = v.dot(w);

      float scale = 1.0f / (dot00 * dot11 - dot01 * dot01);
      float l1 = (dot11 * dot02 - dot01 * dot12) * scale;
      float l2 = (dot00 * dot12 - dot01 * dot02) * scale;
      float l0 = 1.0f - l1 - l2;
      
      // Now <l1,l2,l3> are the parametric coordinates of the intersection point in the triangle
      if ((l0 < 0.0f) || (l1 < 0.0f) || (l2 < 0.0f)) return null;      
      
      // Return the Barycentric Coordinates of the intersection
      return new Vector3(l0,l1,l2);
   }
   
   
   // -----------------------------------------------------------------------
   // SPARSE MATRIX
   // -----------------------------------------------------------------------   

   // -----------------------------------------------------------------------   
   /*
   
   i guess it would be nice if...
      we had a common "Matrix" interface
   
   
   */
   // -----------------------------------------------------------------------   
   
   public static class Vector {
      double[] values;
      Vector(int len) {
         this(new double[len]);
      }
      Vector(double[] v) {
         this.values = v;
      }
      static Vector fromArray(double[] v) {
         return new Vector(v);
      }
      double dist(Vector v2) {
         double sumOfSquares = 0;
         for (int i=0; i < values.length; ++i) {
            double diff = values[i] - v2.values[i];
            sumOfSquares += diff*diff;
         }
         return Math.sqrt(sumOfSquares);
      }
      int length() {
         return values.length;
      }
   }
   public static class SparseMatrix {
      int numRows;
      int numCols;
      ArrayList<NonzeroElement> nonzeroElements;
      
      public static class NonzeroElement {
         double value;
         int row;
         int col;
      }
      
      SparseMatrix(int numRows, int numCols) {
         this.numRows = numRows;
         this.numCols = numCols;
         nonzeroElements = new ArrayList<NonzeroElement>();
      }
      double get(int row, int col) {
         for (NonzeroElement nonzeroElement : nonzeroElements) {
            if ((nonzeroElement.row == row) && (nonzeroElement.col == col)) {
               return nonzeroElement.value;
            }
         }
         return 0.0;
      }
      void set(int row, int col, double value) {
         for (NonzeroElement nonzeroElement : nonzeroElements) {
            if ((nonzeroElement.row == row) && (nonzeroElement.col == col)) {
               nonzeroElement.value = value;
            }
         }
         NonzeroElement nnz = new NonzeroElement();
         nonzeroElements.add(nnz);
         nnz.value = value;
         nnz.row = row;
         nnz.col = col;
      }
      Vector multiply(Vector v) {
         if (v.values.length != numCols) {
            throw new RuntimeException(String.format("%d x %d sparse matrix multiplying %d vector",
                  numRows, numCols, v.values.length));
         }
         Vector result = new Vector(numRows);
         for (NonzeroElement nonzeroElement : nonzeroElements) {
            result.values[nonzeroElement.row] += nonzeroElement.value * v.values[nonzeroElement.col];
         }
         return result;
      }
      Vector solve(Vector b) {
         if (numRows != numCols) {
            throw new RuntimeException(String.format("%d x %d sparse matrix cant solve nonsquare",
                  numRows, numCols));
         }
         if (b.values.length != numCols) {
            throw new RuntimeException(String.format("%d x %d sparse matrix solve given %d length b-vector",
                  numRows, numCols, b.values.length));
         }
         Vector x = new Vector(numRows);
         /*
         for (int i = 0; i < x.values.length; ++i) {
            x.values[i] = 0.5f;
         }
         */
         System.out.format("Low Level SOLVER started on a [%d x %d] matrix with %d non-zero elements \n", 
               numRows, numCols, nonzeroElements.size());
         long startTime = System.currentTimeMillis();

         int maxIterations = 50000;
         double eps = 0.0000003;
         
         int iteration = 0;
         double diff = 1.0;
         while ((diff > eps) && (iteration < maxIterations)) {
           Vector Ax = multiply(x);
           diff = Ax.dist(b);
           //System.out.format("Iteration %d -- %g error\n", iteration++, diff);

           for (int i = 0; i < x.values.length; ++i) {
              x.values[i] += 1.0*(b.values[i] - Ax.values[i]);
           }
           iteration++;
         }
         if (iteration == maxIterations) {
            throw new RuntimeException(String.format("Didn't achieve conergence!  After %d iters diff is still %g!", iteration, diff));
         }
         
         long endTime = System.currentTimeMillis();
         System.out.format("Low Level SOLVER done, did %d iterations in %d ms\n", iteration, endTime-startTime);

         return x;
      }
   }
   
   // -----------------------------------------------------------------------
   // TESTING
   // -----------------------------------------------------------------------

   public static void testRotation() {
      
      Vector3 v = new Vector3 (randf(),randf(),randf());
      Vector3 axis = new Vector3 (randf(),randf(),randf()).normalized();
      float angle = 10*randf();
      
      Vector3 v1 = v.rotated(axis, angle);
      Vector3 v2 = Matrix3x3.product(Matrix3x3.rotation(axis, angle), v);
      
      boolean ok = cmp(v1.x,v2.x) && cmp(v1.y,v2.y) && cmp(v1.z,v2.z);
      System.out.format("Rotation: %s\n",  ok?"OK":"PROBLEM");
   }   
   public static void testInverse3() {
      Matrix3x3 a = new Matrix3x3 (
            randf(), randf(), randf(),
            randf(), randf(), randf(),
            randf(), randf(), randf());
      
      Matrix3x3 b = a.inverse();
      
      Matrix3x3 ab = Matrix3x3.product(a, b);
      Matrix3x3 ba = Matrix3x3.product(b, a);
      
      // System.out.format("Matrix A:  %s\n", a.toString());
      // System.out.format("Matrix B:  %s\n", b.toString());
      // System.out.format("Matrix AB: %s\n", ab.toString());
      // System.out.format("Matrix BA: %s\n", ba.toString());
      
      boolean ok = isIdentity(ab) && isIdentity(ba);
      System.out.format("Inverse3D: %s\n",  ok?"OK":"PROBLEM");
   }
   public static void testInverse2() {
      Matrix2x2 a = new Matrix2x2 (
            randf(), randf(),
            randf(), randf());
      
      Matrix2x2 b = a.inverse();
      
      Matrix2x2 ab = Matrix2x2.product(a, b);
      Matrix2x2 ba = Matrix2x2.product(b, a);
      
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
   private static boolean isIdentity(Matrix3x3 m) {
      return cmp(m.xx, 1.0f) && cmp(m.xy, 0.0f) && cmp(m.xz, 0.0f)
          && cmp(m.yx, 0.0f) && cmp(m.yy, 1.0f) && cmp(m.yz, 0.0f)
          && cmp(m.zx, 0.0f) && cmp(m.zy, 0.0f) && cmp(m.zz, 1.0f);
   }
   private static boolean isIdentity(Matrix2x2 m) {
      return cmp(m.xx, 1.0f) && cmp(m.xy, 0.0f) 
          && cmp(m.yx, 0.0f) && cmp(m.yy, 1.0f);
   }
   private static float randf() {
      return (float)(Math.random()*4.0 - 2.0);
   }
}

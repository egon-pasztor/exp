package demo;

public class GLMath {
   
   // -----------------------------------------------------------------------
   // Vector3f
   // -----------------------------------------------------------------------
   
   public static class Vector3f {
      
       public float x, y, z;
   
       public Vector3f() {}
   
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
       public Vector3f times(Matrix3f m) {
          return new Vector3f (
              x * m.xx + y * m.yx + z * m.zx,
              x * m.xy + y * m.yy + z * m.zy,
              x * m.xz + y * m.yz + z * m.zz); 
       }
       
       public float dot(Vector3f v) {
          return x * v.x + y * v.y + z * v.z;
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
       
       public Vector3f negate() {
          return this.multiply(-1.0f);
       }
       public Vector3f negated() {
          return copy().negate();
       }
       
       public String toString() {
          return String.format("(%g,%g,%g)",  x,y,z);
       }

       public Vector3f rotated(Vector3f axis, float angle) {
          Vector3f qv = axis.times((float) Math.sin(angle / 2.0f));
          float    qs = (float) Math.cos(angle / 2.0f);
          Vector3f mv = qv.cross(this).plus(this.times(qs));
          return qv.cross(mv).plus(mv.times(qs)).plus(qv.times(this.dot(qv)));
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
      
      public Matrix3f plus(Matrix3f m) {
         return copy().add(m);
      }
      public Matrix3f minus(Matrix3f m) {
         return copy().subtract(m);
      }
      public Matrix3f times(float s) {   
         return copy().multiply(s);
      }       
      public Matrix3f times(Matrix3f m) {
         return new Matrix3f (
            xx * m.xx + xy * m.yx + xz * m.zx,  
            xx * m.xy + xy * m.yy + xz * m.zy,
            xx * m.xz + xy * m.yz + xz * m.zz,
            yx * m.xx + yy * m.yx + yz * m.zx,  
            yx * m.xy + yy * m.yy + yz * m.zy,
            yx * m.xz + yy * m.yz + yz * m.zz,
            zx * m.xx + zy * m.yx + zz * m.zx,  
            zx * m.xy + zy * m.yy + zz * m.zy,
            zx * m.xz + zy * m.yz + zz * m.zz);
      }
      public Vector3f times(Vector3f v) {
         return new Vector3f (
            xx * v.x + xy * v.y + xz * v.z,
            yx * v.x + yy * v.y + yz * v.z,
            zx * v.x + zy * v.y + zz * v.z); 
      }
      
      public Matrix3f transposed() {
         return new Matrix3f(xx, yx, zx,
                             xy, yy, zy,
                             xz, yz, zz);
      }
      
      public Matrix3f negate() {
         return this.multiply(-1.0f);
      }
      public Matrix3f negated() {
         return copy().negate();
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
         
         return cofactorMatrix.transposed().multiply(1.0f / determinate());
      }
      
      public static final Matrix3f IDENTITY = new Matrix3f(1.0f, 0.0f, 0.0f,
                                                           0.0f, 1.0f, 0.0f,
                                                           0.0f, 0.0f, 1.0f);
   }
  
   // -----------------------------------------------------------------------
   // Vector2f
   // -----------------------------------------------------------------------
   
   public static class Vector2f {
      
      public float x;
      public float y;
  
      public Vector2f() {}
  
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
      
      public float dot(Vector2f v) {
         return x * v.x + y * v.y;
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
      
      public Vector2f negate() {
         return this.multiply(-1.0f);
      }
      public Vector2f negated() {
         return copy().negate();
      }
      
      public String toString() {
         return String.format("(%g,%g)", x,y);
      }
      
      public static final Vector2f ORIGIN = new Vector2f(0.0f, 0.0f);
      public static final Vector2f X      = new Vector2f(1.0f, 0.0f);
      public static final Vector2f Y      = new Vector2f(0.0f, 1.0f);
   }
}

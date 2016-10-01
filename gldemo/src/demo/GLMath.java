package demo;

import java.util.ArrayList;
import java.util.HashSet;

public class GLMath {
   
   // -----------------------------------------------------------------------
   // Vector3f
   // -----------------------------------------------------------------------
   
   public static class Vector3f {
      
       public final float x, y, z;
   
       public Vector3f(float x_, float y_, float z_) {
          x = x_;
          y = y_;
          z = z_;
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
       public Vector3f normalized() {
          return this.times(1.0f / length());
       }
       
       public String toString() {
          return String.format("(%g,%g,%g)", x,y,z);
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
      
      public final float xx, xy, xz,
                         yx, yy, yz,
                         zx, zy, zz;
  
      public Matrix3f(float xx_, float xy_, float xz_,
                      float yx_, float yy_, float yz_,
                      float zx_, float zy_, float zz_) {
         
          xx = xx_; xy = xy_; xz = xz_;
          yx = yx_; yy = yy_; yz = yz_;
          zx = zx_; zy = zy_; zz = zz_;
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
      
      public String toString() {
         return String.format("(%g,%g,%g; %g,%g,%g; %g,%g,%g)",  xx,xy,xz, yx,yy,yz, zx,zy,zz);
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
      
      public static Matrix3f scaling(float s) {
         return new Matrix3f (   s, 0.0f, 0.0f,
                              0.0f,    s, 0.0f,
                              0.0f, 0.0f,    s);
      }
      public static Matrix3f rotation(Vector3f normalizedAxis, float angle) {
         final float sa = (float) Math.sin(angle);
         final float ca = (float) Math.cos(angle);
         final float x = normalizedAxis.x, y = normalizedAxis.y, z = normalizedAxis.z;
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
         return lookatPoint;
      }      
      public Vector3f getCameraPosition() {
         return cameraPosition;
      }
      public Vector3f getCameraUpVector() {
	 return cameraUpVector;
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

         grab_zVector = grab_lookat_to_camera.normalized();
         grab_xVector = grab_zVector.cross(cameraUpVector).normalized();
         grab_yVector = grab_xVector.cross(grab_zVector);

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
	       axis = Vector3f.Z; 
               amount = (float) (Math.atan2(y,x) - grabAngle) * Scale2DRotation;

            } else { 
               axis = new Vector3f ((float) dy,(float) -dx, 0).normalized();
               amount = ((float) Math.sqrt(dx*dx+dy*dy)) * Scale3DRotation;
            }
            
            Vector3f rotationAxis = grab_xVector.times(axis.x)
                              .plus(grab_yVector.times(axis.y))
                              .plus(grab_zVector.times(axis.z))
                              .normalized();
            
            cameraPosition = grab_lookat_to_camera.rotated(rotationAxis, amount).plus(lookatPoint);
            cameraUpVector = grab_yVector.rotated(rotationAxis, amount);
            
         } else if (grabType == GrabType.Zoom) {

            cameraPosition = grab_lookat_to_camera.times((float) Math.pow(ScaleZoom, dy/tScale)).plus(lookatPoint);

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
      
      public final float x, y;

      public Vector2f(float x, float y) {
          this.x = x;
          this.y = y;
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
      
      public float dot(Vector2f v) {
         return x * v.x + y * v.y;
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
       public final byte r,g,b,a;
   
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

   // -----------------------------------------------------------------------
   // Mesh Structure
   // -----------------------------------------------------------------------
/*
   public static class Vertex {
      HalfEdge outgoingEdge;
      
      // vertex-info:
      Vector3f position;
      
   }
   public static class Edge {
      HalfEdge oneEdge;
      
      // edge-info
   }
   
   public static class Face {
      HalfEdge oneEdge;

      // face-info
      ColorRGBA color;
   }
   
   public static class HalfEdge {
      HalfEdge nextEdge;
      Vertex endVertex;
      HalfEdge sibling;   // 
      Face face;          // left side of halfedge
      
      // halfedge-info:                 
   }
   
   public static class Mesh {
      ArrayList<Vertex> vertices;      
      ArrayList<Face> faces; 
      ArrayList<Edge> edges;
   }
  */

   
   
   /*
   public static class Vertex {
      public Vertex(Vector3f position) {
         this.position = position;
         r = null;
      }
      
      // vertex-info
      public Vector3f position;

      public EdgeRef getRep()          { return r;   }
      public void    setRep(EdgeRef r) { this.r = r; }
      
      private EdgeRef r;
   }
   
   public static class Triangle {
      public Vertex v0,v1,v2;      
      public EdgeRef e0,e1,e2;
      
      Triangle(Vertex v0, Vertex v1, Vertex v2, EdgeRef e0, EdgeRef e1, EdgeRef e2) {
         this.v0 = v0; this.v1 = v1; this.v2 = v2;
         this.e0 = e0; this.e1 = e1; this.e2 = e2;
      }
   }
   
   //
   //  examples:   cube
   //              icosahedron
   //              tesselated-bezier-patch
   //              teapot (eg, 5 tesselated-bezier-patches)
   //              plain-old-simple-triangle
   //              plain-old-simple-square
   //              
   
   public static class EdgeRef {
      public final Triangle t;
      public final int i;
      
      public EdgeRef(Triangle triangle, int edgeIndex) {
         this.t = triangle;
         this.i = edgeIndex;
      }
      
      public boolean equals(EdgeRef e) {
         return (e != null) && (t == e.t) && (i == e.i);
      }
      
      public EdgeRef rot()    { return new EdgeRef(t, (i+1)%3); }
      public EdgeRef rotInv() { return new EdgeRef(t, (i+2)%3); }
      public EdgeRef sym()    { return next().rot(); }

      public Vertex dest()   { return rotInv().org(); }
      public Vertex right()  { return rot().org(); }
      public Vertex left()   { return next().dest(); }
      
      public Vertex org()     { return (i==0) ? t.v0 : (i==1) ? t.v1 : t.v2; }
      public EdgeRef next()   { return (i==0) ? t.e0 : (i==1) ? t.e1 : t.e2; }
      public EdgeRef prev()   { return rot().next().rot(); }

      public void setOrg(Vertex v)   { if (i==0) t.v0 = v; else if (i==1) t.v1 = v; else t.v2 = v; }
      public void setNext(EdgeRef e) { if (i==0) t.e0 = e; else if (i==1) t.e1 = e; else t.e2 = e; }
   }
      
   
   public static class Mesh {
      private HashSet<Triangle> triangles; 
      private HashSet<Vertex> vertices;      
      private Vertex inf;
      
      public Mesh() {
         triangles = new HashSet<Triangle>();
         vertices = new HashSet<Vertex>();
         inf = new Vertex(null);
      }
            
      public EdgeRef makeEdge(Vertex v0, Vertex v1) {
         Triangle a = new Triangle(v1,v0,inf,   null,null,null);
         Triangle b = new Triangle(v0,v1,inf,   null,null,null);
         a.e0 = new EdgeRef(b,1); a.e1 = new EdgeRef(b,0); a.e2 = new EdgeRef(b,2);
         b.e0 = new EdgeRef(a,1); b.e1 = new EdgeRef(a,0); b.e2 = new EdgeRef(a,2);
         triangles.add(a);
         triangles.add(b);
         return b.e0;
      }
      
      public void splice(EdgeRef a, EdgeRef b) {
         EdgeRef aNext = a.next();
         EdgeRef bNext = b.next();
         EdgeRef aSym = aNext.rot();
         EdgeRef bSym = bNext.rot();
         EdgeRef aRotInv = a.rotInv();
         EdgeRef bRotInv = b.rotInv();
         
         a.setNext(bNext);
         b.setNext(aNext);
         aSym.setNext(bRotInv);
         bSym.setNext(aRotInv);         
      }
      
      public EdgeRef swap(EdgeRef e) {
         EdgeRef a = e.next();
         EdgeRef b = e.rotInv();
         a.setOrg(b.dest());
         b.setOrg(a.dest());
         splice(a, e);
         splice(a, b.sym());
         return a;
      }
      
      public void AddTriangle(Vertex v0, Vertex v1, Vertex v2) {
         EdgeRef g0 = null,g1 = null,g2 = null;
         
         // phase 0
         for (int i = 0; i < 2; i++) {
            Vertex vi       = (i==0)?v0:(i==1)?v1:v2;
            Vertex viMinus1 = (i==0)?v2:(i==1)?v0:v1;
            Vertex viPlus1  = (i==0)?v1:(i==1)?v2:v0;
            
            // We're proposing a triangle that will contain 'vi',
            // connected on one side to viMinus1, and on the other to viPlus1.
            //
            // But does 'vi' already have edges connected to it?
            
            EdgeRef e = vi.getRep(), g = null;
            if (e!=null) {
               EdgeRef a = null, b = null, c = null;

               // Edge 'e' starts at 'vi'.
               // Walk around vertex 'vi' looking at all the edges
               // that emanate from there:
               
               EdgeRef e0 = e;
               do {
                  Vertex w = e.dest();
                  if (w == viMinus1) {
                     if (e.right() != inf) { 
                        throw new RuntimeException("invalid edge 1");
                     }                     
                     // We found a pre-existing edge that goes from vi to viMinus1
                     b = e;
                  } else if (w == viPlus1) {
                     if (e.left() != inf) { 
                        throw new RuntimeException("invalid edge 2");
                     }
                     // We found a pre-existing edge that goes from vi to viPlus1
                     a = e;
                  } else if (w == inf) {
                     c = e;
                  }
                  
                  e = e.next();
               } while ((e != e0) && ((a==null) || (b==null)));
               
               if (a!=null) {
                  if (b!=null) {
                     // Both "a" (vi->viMinus1) and "b" (vi->viPlus1) already exist
                     if (!a.next().equals(b.prev())) {
                        e = b.next();
                        while (!e.equals(a) && (e.dest() != inf)) {
                           e = e.next();
                        }
                        if (e.equals(a)) {
                           throw new RuntimeException("non manifold vertex 1");
                        }
                        EdgeRef f = b.prev();
                        splice(a.next(), f);
                        splice(e, f);
                     }
                     
                     g = b;
                     
                  } else {
                     // Only "a" (vi->viMinus1) exists
                     g = a.next();
                  }
               } else if (b != null) {
                  // Only "b" (vi->viPlus1) exists
                  g = b;
               } else if (c != null) {
                  g = c;
               } else {
                  throw new RuntimeException("non manifold vertex 2");
               }
               
               if (i==0) g0=g; else if (i==1) g1=g; else g2=g;
            }            
         }
         
        
         // ???
         
      }
   }
   */


   
   public static class Vertex {
      public Vertex(Vector3f position) {
         this.position = position;
         r = null;
      }
      
      // vertex-info
      public Vector3f position;

      public Triangle.Ref getRep()       { return r;   }
      public void setRep(Triangle.Ref r) { this.r = r; }
      
      private Triangle.Ref r;
   }
   
   // Okay this is going to be MY mesh class.
   //   
   public static class Triangle {
      
      // Triangle vertices go counterclockwise.
      //
      // From:       v0 -> v1 -> v2 -> v0
      // Edges are      e2 -> e0 -> e1
      
      public Vertex v0,v1,v2; 
      public Triangle.Ref e0,e1,e2;
      
      public abstract class Ref {
         public Triangle getTriangle() {
            return Triangle.this;
         }
         
         // These methods let you move from one (edge/vertex) to the next going around the Triangle.
         public abstract Ref ccwAroundTriangle();
         public abstract Ref cwAroundTriangle();
         
         // When being used to refer to a vertex, it refers to one of {v0,v1,v2} within its Triangle.
         // This variable points to a Vertex object and we can both read and write this pointer:
         public abstract Vertex getVertex();
         public abstract void setVertex(Vertex v);
         
         // When being used to refer to an edge, it refers to one of {e0,e1,e2} within its Triangle.
         // This variable points to a Triangle.Ref and we can both read and write this pointer:
         public abstract void setOppositeEdge(Ref e);
         public abstract Ref getOppositeEdge();

         // When being used to refer to a edge, the edge has these start and end points:
         public Vertex getStart() { return ccwAroundTriangle().getVertex(); }
         public Vertex getEnd()   { return cwAroundTriangle().getVertex(); }         
         
         public Ref ccwAroundVertex() { return ccwAroundTriangle().getOppositeEdge().cwAroundTriangle(); }
         public Ref cwAroundVertex() { return cwAroundTriangle().getOppositeEdge().ccwAroundTriangle(); }
      }
      
      public final Triangle.Ref ref0, ref1, ref2;
      
      Triangle(Vertex v0, Vertex v1, Vertex v2) {
         this.v0 = v0; this.v1 = v1; this.v2 = v2;
         e0 = e1 = e2 = null;

         // Create three "Ref" objects that can be used 
         // to refer to each side/corner of this Triangle;
         ref0 = new Ref() {
            @Override public Vertex getVertex()            { return Triangle.this.v0;   }
            @Override public void   setVertex(Vertex v)    { Triangle.this.v0 = v;      }
            @Override public void   setOppositeEdge(Ref e) { Triangle.this.e0 = e;      }
            @Override public Ref    getOppositeEdge()      { return Triangle.this.e0;   }
            @Override public Ref    ccwAroundTriangle()    { return Triangle.this.ref1; }
            @Override public Ref    cwAroundTriangle()     { return Triangle.this.ref2; }
         };
         ref1 = new Ref() {
            @Override public Vertex getVertex()            { return Triangle.this.v1;   }
            @Override public void   setVertex(Vertex v)    { Triangle.this.v1 = v;      }
            @Override public void   setOppositeEdge(Ref e) { Triangle.this.e1 = e;      }
            @Override public Ref    getOppositeEdge()      { return Triangle.this.e1;   }
            @Override public Ref    ccwAroundTriangle()    { return Triangle.this.ref2; }
            @Override public Ref    cwAroundTriangle()     { return Triangle.this.ref0; }
         };
         ref2 = new Ref() {
            @Override public Vertex getVertex()            { return Triangle.this.v2;   }
            @Override public void   setVertex(Vertex v)    { Triangle.this.v2 = v;      }
            @Override public void   setOppositeEdge(Ref e) { Triangle.this.e2 = e;      }
            @Override public Ref    getOppositeEdge()      { return Triangle.this.e2;   }
            @Override public Ref    ccwAroundTriangle()    { return Triangle.this.ref0; }
            @Override public Ref    cwAroundTriangle()     { return Triangle.this.ref1; }
         };
      }
   }
   

   public static class Mesh {
      private HashSet<Triangle> triangles; 
      private HashSet<Vertex> vertices;      
      private Vertex inf;
      
      public Mesh() {
         triangles = new HashSet<Triangle>();
         vertices = new HashSet<Vertex>();
         inf = new Vertex(null);
      }
            
      public Triangle.Ref makeEdge(Vertex v0, Vertex v1) {
         Triangle a = new Triangle(v1,v0,inf);
         Triangle b = new Triangle(v0,v1,inf);
         a.e0 = b.ref1;  a.e1 = b.ref0; a.e2 = b.ref2;
         b.e0 = a.ref1;  b.e1 = a.ref0; b.e2 = a.ref2;
         triangles.add(a);
         triangles.add(b);
         return b.e0;
      }      
      
      public Triangle.Ref swap(Triangle.Ref e) {
         Triangle.Ref oe = e.getOppositeEdge();
         
         Triangle.Ref trInner = oe.cwAroundTriangle();
         Triangle.Ref brInner = oe.ccwAroundTriangle();
         Triangle.Ref tlInner = e.ccwAroundTriangle();
         Triangle.Ref blInner = e.cwAroundTriangle();
         
         Triangle.Ref trOuter = trInner.getOppositeEdge();
         Triangle.Ref blOuter = blInner.getOppositeEdge();         
         
         tlInner.setVertex(oe.getVertex());
         brInner.setVertex(e.getVertex());
         
         e.setOppositeEdge(trOuter);
         trOuter.setOppositeEdge(e);
         
         oe.setOppositeEdge(blOuter);
         blOuter.setOppositeEdge(oe);
         
         trInner.setOppositeEdge(blInner);
         blInner.setOppositeEdge(trInner);

         return blInner;
      }
      
      /*
      public void splice(EdgeRef a, EdgeRef b) {
         EdgeRef aNext = a.next();
         EdgeRef bNext = b.next();
         EdgeRef aSym = aNext.rot();
         EdgeRef bSym = bNext.rot();
         EdgeRef aRotInv = a.rotInv();
         EdgeRef bRotInv = b.rotInv();
         
         a.setNext(bNext);
         b.setNext(aNext);
         aSym.setNext(bRotInv);
         bSym.setNext(aRotInv);         
      }
      */
   }
      
   
   // -----------------------------------------------------------------------
   // Triangle
   // -----------------------------------------------------------------------
   
   public static class Triangle2 {

       public Vector3f  v1,v2,v3;       
       public Vector2f  t1,t2,t3;
       public ColorRGBA c1,c2,c3;

       public Triangle2() {
	       v1=v2=v3=null;
	       t1=t2=t3=null;
  	       c1=c2=c3=null;
       }

       public void setPositions(Vector3f v1, Vector3f v2, Vector3f v3) {
           this.v1 = v1;
           this.v2 = v2;
           this.v3 = v3;
       }
       public void setTexCoords(Vector2f t1, Vector2f t2, Vector2f t3) {
           this.t1 = t1;
           this.t2 = t2;
           this.t3 = t3;
       }
       public void setColors(ColorRGBA c1, ColorRGBA c2, ColorRGBA c3) {
           this.c1 = c1;
           this.c2 = c2;
           this.c3 = c3;
       }
   }

   // -----------------------------------------------------------------------
   // Model
   // -----------------------------------------------------------------------

   public static class Model {

       public Model() {
	   triangles = new ArrayList<Triangle2>();
       }

       public void addTriangle(Triangle2 t) {
	   triangles.add(t);
       }
       public void clearTriangles() {
           triangles.clear();
       }
       public int numTriangles() {
           return triangles.size();
       }

       private ArrayList<Triangle2> triangles;

       // -- -- -- -- -- -- -- --

       public void addTriangle (Vector3f a, Vector3f b, Vector3f c) {
          Triangle2 t1 = new Triangle2();
          t1.setPositions(a,b,c);
          t1.setColors(new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff),
                       new ColorRGBA((byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff),
                       new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff));
          t1.setTexCoords(new Vector2f(0.0f, 1.0f),
                          new Vector2f(0.0f, 0.0f),
                          new Vector2f(1.0f, 1.0f));
          addTriangle(t1);
          
       }
       public void addSquare (Vector3f center, Vector3f dx, Vector3f dy) {
           Vector3f tr = center.plus(dx).plus(dy);
           Vector3f tl = center.minus(dx).plus(dy);
           Vector3f br = center.plus(dx).minus(dy);
           Vector3f bl = center.minus(dx).minus(dy);

	   System.out.format("Adding square [%s][%s][%s][%s]\n",
			     tr,tl,br,bl);

           Triangle2 t1 = new Triangle2();
           Triangle2 t2 = new Triangle2();
           t1.setPositions(bl,tl,br);
           t2.setPositions(tl,br,tr);

           t1.setTexCoords(new Vector2f(0.0f, 1.0f),
                           new Vector2f(0.0f, 0.0f),
		           new Vector2f(1.0f, 1.0f));

           t2.setTexCoords(new Vector2f(0.0f, 0.0f),
   	 	           new Vector2f(1.0f, 1.0f),
		           new Vector2f(1.0f, 0.0f));


           t1.setColors(new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff),
                        new ColorRGBA((byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff),
                        new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff));
   
           t2.setColors(new ColorRGBA((byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff),
                        new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff),
                        new ColorRGBA((byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff));

           addTriangle(t1);
           addTriangle(t2);
       }

       // -- -- -- -- -- -- -- --

       public static class Arrays {
	   float[] positions;
           float[] texCoords;
           float[] colors;
       }

       public Arrays getArrays() {
	   Arrays result = new Arrays();
           int n = triangles.size();

           result.positions = new float[n*3*4];
	   { int c = 0;
             for (int i = 0; i < n; ++i) {
		 Triangle2 t = triangles.get(i);
		 c = copyVector3f(result.positions, c, t.v1);
		 c = copyVector3f(result.positions, c, t.v2);
		 c = copyVector3f(result.positions, c, t.v3);
	     }
	   }

           result.texCoords = new float[n*3*2];
	   { int c = 0;
             for (int i = 0; i < n; ++i) {
		 Triangle2 t = triangles.get(i);
		 c = copyVector2f(result.texCoords, c, t.t1);
		 c = copyVector2f(result.texCoords, c, t.t2);
		 c = copyVector2f(result.texCoords, c, t.t3);
	     }
	   }

           result.colors = new float[n*3*4];
	   { int c = 0;
             for (int i = 0; i < n; ++i) {
		 Triangle2 t = triangles.get(i);
		 c = copyColor(result.colors, c, t.c1);
		 c = copyColor(result.colors, c, t.c2);
		 c = copyColor(result.colors, c, t.c3);
	     }
	   }
           
	   return result;
       }

       private int copyVector3f(float[] arr, int base, Vector3f v) {
           arr[base+0] = v.x;
           arr[base+1] = v.y;
           arr[base+2] = v.z;
           arr[base+3] = 1.0f;
           return base+4;
       }
       private int copyVector2f(float[] arr, int base, Vector2f v) {
           arr[base+0] = v.x;
           arr[base+1] = v.y;
           return base+2;
       }
       private int copyColor(float[] arr, int base, ColorRGBA c) {
           arr[base+0] = ((float)(c.r&0xff))/255.0f;
           arr[base+1] = ((float)(c.g&0xff))/255.0f;
           arr[base+2] = ((float)(c.b&0xff))/255.0f;
           arr[base+3] = ((float)(c.a&0xff))/255.0f;
           return base+4;
       }
   }   
}

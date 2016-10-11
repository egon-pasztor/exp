package demo;

import com.jogamp.graph.geom.Triangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
      
      public String toString() {
         return String.format("(%g,%g,%g)", x,y,z);
      }
      public boolean equals(Vector3f v) {
         return (x == v.x) && (y == v.y) && (z == v.z);
      }
      public int hashCode() {
         return (new HashCodeMaker())
            .addFloat(x).addFloat(y).addFloat(z)
            .getHashCode();
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
      
      public String toString() {
         return String.format("(%g,%g,%g; %g,%g,%g; %g,%g,%g)", xx,xy,xz, yx,yy,yz, zx,zy,zz);
      }
      public boolean equals(Matrix3f v) {
         return (xx == v.xx) && (xy == v.xy) && (xz == v.xz)
             && (yx == v.yx) && (yy == v.yy) && (yz == v.yz)
             && (zx == v.zx) && (zy == v.zy) && (zz == v.zz);
      }
      public int hashCode() {
         return (new HashCodeMaker())
            .addFloat(xx).addFloat(xy).addFloat(xz)
            .addFloat(yx).addFloat(yy).addFloat(yz)
            .addFloat(zx).addFloat(zy).addFloat(zz)
            .getHashCode();
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
      
      public String toString() {
         return String.format("(%g,%g)", x,y);
      }
      public boolean equals(Vector3f v) {
         return (x == v.x) && (y == v.y);
      }
      public int hashCode() {
         return (new HashCodeMaker())
            .addFloat(x).addFloat(y)
            .getHashCode();
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
   
   public static class Mesh<V,T> {
            
      public Mesh() {
         boundaryTriangles = new HashSet<Triangle<V,T>>();
         interiorTriangles = new HashSet<Triangle<V,T>>();
         vertices = new HashSet<Vertex<V,T>>();
      }
      
      public HashSet<Triangle<V,T>> boundaryTriangles; 
      public HashSet<Triangle<V,T>> interiorTriangles; 
      public HashSet<Vertex<V,T>> vertices;      
      
      // -----------------------------------------------
      // VERTEX
      // -----------------------------------------------
      
      public static class Vertex<V,T> {
         // Each Vertex holds a pointer to one outgoing Edge
         public Triangle<V,T>.Edge getOneOutgoingEdge()       { return oneOutgoingEdge; }
         public void setOneOutgoingEdge(Triangle<V,T>.Edge e) { oneOutgoingEdge = e;    }
         private Triangle<V,T>.Edge oneOutgoingEdge;
         
         // Vertex data
         public V getData()          { return userdata; }
         public void setData(V data) { userdata = data; }         
         private V userdata;
      }
      
      // -----------------------------------------------
      // TRIANGLE
      // -----------------------------------------------
      
      public static class Triangle<V,T> {

         public Triangle(Vertex<V,T> v0, Vertex<V,T> v1, Vertex<V,T> v2) {
            // Create three final Edges for this Triangle
            edge0 = new Edge() {
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge1; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge2; }
            };
            edge1 = new Edge() {
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge2; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge0; }
            };
            edge2 = new Edge() {
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge0; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge1; }
            };

            // Set the vertices
            edge0.setOppositeVertex(v0);
            edge1.setOppositeVertex(v1);
            edge2.setOppositeVertex(v2);
         }
         
         // Each Triangle has three Edges:
         public final Triangle<V,T>.Edge edge0, edge1, edge2;
         
         // Each Edge has methods to get/set two fields: "oppositeVertex" and "oppositeEdge":
         public abstract class Edge {
            public Triangle<V,T> getTriangle() {
               return Triangle.this;
            }
            
            // Move from this Edge to the next going around this Triangle.
            public abstract Edge ccwAroundTriangle();
            public abstract Edge cwAroundTriangle();
            
            // Each Edge holds a pointer to the Vertex opposite this Edge in this Triangle
            public Vertex<V,T> getOppositeVertex()       { return oppositeVertex; }
            public void setOppositeVertex(Vertex<V,T> v) { oppositeVertex = v;    }
            private Vertex<V,T> oppositeVertex;
            
            // Return the start and end vertices of this Edge
            public Vertex<V,T> getStart() { return ccwAroundTriangle().getOppositeVertex(); }
            public Vertex<V,T> getEnd()   { return cwAroundTriangle().getOppositeVertex(); }         
            
            // Each Edge holds a pointer to the Edge facing the other way in the adjacent Triangle
            public Edge getOppositeEdge()       { return oppositeEdge; }
            public void setOppositeEdge(Edge e) { oppositeEdge = e;    }
            private Edge oppositeEdge;

            // Move from this Edge to the next going around the start or end vertices
            public Edge ccwAroundStart() { return cwAroundTriangle().getOppositeEdge();  }
            public Edge cwAroundStart()  { return getOppositeEdge().ccwAroundTriangle(); }
            public Edge ccwAroundEnd()   { return getOppositeEdge().cwAroundTriangle();  }
            public Edge cwAroundEnd()    { return ccwAroundTriangle().getOppositeEdge(); }
         }

         // Boundary triangles have null edge2
         public boolean isBoundary() {
            return edge2.oppositeVertex == null;
         }
         
         // Triangle data
         public T getData()          { return userdata; }
         public void setData(T data) { userdata = data; }         
         private T userdata;
      }
            
      // --------------------------------------------------------
      // Validation -- is this mesh actually hooked up right?
      // --------------------------------------------------------

      public static void check(boolean cond, String err) {
         if (!cond) throw new RuntimeException("FAILED: " + err);
      }
      public void checkEdge(Triangle<V,T>.Edge e) {
         check(e.oppositeEdge != null, "Edge.oppositeEdge is set");
         check(e != e.oppositeEdge, "Edge.oppositeEdge is different from this Edge");
         check(e == e.oppositeEdge.oppositeEdge, "oppositeEdge points back to us");
         check(e.getStart() == e.oppositeEdge.getEnd(), "start == oppositeEdge.end");
         check(e.getEnd() == e.oppositeEdge.getStart(), "end == oppositeEdge.start");
         
         Triangle<V,T> opposingTriangle = e.oppositeEdge.getTriangle();
         check(e.getTriangle() != opposingTriangle, "oppositeEdge is in a different Triangle");
         if (opposingTriangle.isBoundary()) {
            check(boundaryTriangles.contains(opposingTriangle), "oppositeTriangle is part of our boundary list");
         } else {
            check(interiorTriangles.contains(opposingTriangle), "oppositeTriangle is part of our interior list");
         }
         Vertex<V,T> opposingVertex = e.oppositeVertex;
         if (opposingVertex != null) {
            check(vertices.contains(opposingVertex), "oppositeVertex is part of our vertex list");
         }
      }
      public void checkTriangle(Triangle<V,T> t, boolean isBoundary) {
         checkEdge(t.edge0);
         checkEdge(t.edge1);
         checkEdge(t.edge2);
         check(t.edge0.oppositeVertex != null, "vertex0 not null");
         check(t.edge1.oppositeVertex != null, "vertex1 not null");
         check(t.isBoundary() == isBoundary, "vertex2 is null if boundary and not otherwise");
         check((t.edge0.oppositeVertex != t.edge1.oppositeVertex) &&
               (t.edge0.oppositeVertex != t.edge2.oppositeVertex) &&
               (t.edge1.oppositeVertex != t.edge2.oppositeVertex), "vertices are all different");
      }
      public void checkVertex(Vertex<V,T> v) {
         check(v.oneOutgoingEdge != null, "Vertex.getOneOutgoingEdge is set");
         Triangle<V,T>.Edge e = v.oneOutgoingEdge;
         while (true) {
            check (v == e.getStart(), "Edge is outgoing from this Vertex");
            e = e.ccwAroundStart();
            if (e == v.oneOutgoingEdge) break;
         }
      }
      public void checkMesh() {
         for (Triangle<V,T> t : interiorTriangles) checkTriangle(t, false);
         for (Triangle<V,T> t : boundaryTriangles) checkTriangle(t, true);
         for (Vertex<V,T> v : vertices) checkVertex(v);
      }

      // --------------------------------------------------------
      // Mesh Assembly
      // --------------------------------------------------------
      
      public static <V,T> void linkOpposingEdges(Triangle<V,T>.Edge a, Triangle<V,T>.Edge b) {
         a.setOppositeEdge(b);
         b.setOppositeEdge(a);
      }
      
      // --------------------------------------------------------
      // Buggy
      // --------------------------------------------------------
      
      public Triangle.Edge swap(Triangle.Edge e) {
         Triangle.Edge oe = e.getOppositeEdge();
         
         Triangle.Edge trInner = oe.cwAroundTriangle();
         Triangle.Edge brInner = oe.ccwAroundTriangle();
         Triangle.Edge tlInner = e.ccwAroundTriangle();
         Triangle.Edge blInner = e.cwAroundTriangle();
         
         Triangle.Edge trOuter = trInner.getOppositeEdge();
         Triangle.Edge blOuter = blInner.getOppositeEdge();         
         
         tlInner.setOppositeVertex(oe.getOppositeVertex());
         brInner.setOppositeVertex(e.getOppositeVertex());
         // WAIT WAIT WAIT ... by calling setOppositeVertex, don't we also maybe have to change outOutgoingEdge??
         
         linkOpposingEdges(trOuter, e);
         linkOpposingEdges(blOuter, oe);
         linkOpposingEdges(trInner, blInner);         
         return blInner;
      }
      
      // --------------------------------------------------------
      // AddTriangle
      // --------------------------------------------------------
      
      public Triangle<V,T> addTriangle (Vertex<V,T> v0, Vertex<V,T> v1, Vertex<V,T> v2) {
          check((v0 != null) && (v1 != null) && (v2 != null) &&
               (v1 != v0) && (v2 != v0) && (v2 != v1), "Vertices should be all different");
         
          Triangle<V,T> t = new Triangle<V,T>(v0,v1,v2);
          interiorTriangles.add(t);
          
          // Set the OPPOSITE-EDGE pointers in the new triangle
          for (Triangle<V,T>.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             
             Vertex<V,T> start = ei.getStart();
             Vertex<V,T> end = ei.getEnd();
             Triangle<V,T>.Edge firstOutFromStart = start.getOneOutgoingEdge();
             if (firstOutFromStart != null) {      
                // The start vertex already has edges attached to it.
                // It must be on the boundary then...
                
                Triangle<V,T>.Edge outFromStart = firstOutFromStart;
                boolean onBoundary = false;
                do {
                   boolean isBoundary = outFromStart.getTriangle().isBoundary();
                   if (isBoundary) onBoundary = true;
                   
                   if (outFromStart.getEnd() == end) {
                      // We've found a pre-existing edge from Start -> End.
                      check(isBoundary, "Attached edge must be a boundary edge");
                      
                      // It's a boundary edge.  We're going to end up DELETING this boundary edge
                      // and the edge opposite this boundary will be opposite "ei" instead:
                      ei.setOppositeEdge(outFromStart.getOppositeEdge());
                      break;
                   }
                   outFromStart = outFromStart.ccwAroundStart();
                } while (outFromStart != firstOutFromStart);
             
                check(onBoundary, "Attached vertex must be a boundary vertex");
             }
             
             // If we didn't find a pre-existing edge (whose boundary we will DESTROY),
             // then our edge needs a new boundary we must CREATE
             if (ei.getOppositeEdge() == null) {
                
                // CREATE NEW BOUNDARY TRIANGLES HERE
                Triangle<V,T> b = new Triangle<V,T>(end, start, null);
                ei.setOppositeEdge(b.edge2);
                boundaryTriangles.add(b);
             }
          }
          
          // Now let's consider each VERTEX in turn
          for (Triangle<V,T>.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             Vertex<V,T> v = ei.getOppositeVertex();
             
             // Going CCW around the new triangle, we encounter edges: prevEdge -> v -> nextEdge
             Triangle<V,T>.Edge prevEdge = ei.ccwAroundTriangle();  // (points towards v)
             Triangle<V,T>.Edge nextEdge = ei.cwAroundTriangle();   // (points away from v)
             
             // The "opposite" pointers in these Edges were set above, either to NEW boundary
             // triangles (if unattached), or existing internal triangles (if attached):          
             Triangle<V,T>.Edge oppositePrevEdge = prevEdge.getOppositeEdge();  // (points away from v)
             Triangle<V,T>.Edge oppositeNextEdge = nextEdge.getOppositeEdge();  // (points towards v)             
             boolean prevEdgeAttached = !oppositePrevEdge.getTriangle().isBoundary();
             boolean nextEdgeAttached = !oppositeNextEdge.getTriangle().isBoundary();
             
             // There a 4 cases based on whether the prev and next edges are attached:
             if (!prevEdgeAttached && !nextEdgeAttached) {
                // CASE 1. Link both "unattached" boundary triangles.
                
                Triangle<V,T>.Edge newBoundaryPrev = prevEdge.ccwAroundEnd();    // (points towards v)
                Triangle<V,T>.Edge newBoundaryNext  = nextEdge.cwAroundStart();  // (points away from v)

                // Does v have ANY existing edges?
                Triangle<V,T>.Edge firstOutFromV = v.getOneOutgoingEdge();
                if (firstOutFromV == null) {      
                   // v has NO existing edges, it's a NEW vertex just for this Triangle.
                   // Connect the boundary triangles to each other:
                   linkOpposingEdges(newBoundaryPrev, newBoundaryNext);
                   
                } else {
                   // V does have existing edges.  We know it's on the boundary, so there
                   // must be two consecutive boundary triangles attached to V.  Find them:
                   
                   Triangle<V,T>.Edge outFromV = firstOutFromV;
                   boolean foundDoubleBoundary = false;                   
                   do {
                      if (outFromV.getTriangle().isBoundary() && 
                          outFromV.getOppositeEdge().getTriangle().isBoundary()) {
                         foundDoubleBoundary = true;
                         break;
                      }
                      
                      outFromV = outFromV.ccwAroundStart();
                   } while (outFromV != firstOutFromV);
                
                   check(foundDoubleBoundary, "Attached vertex should have had two consecutive boundary triangles");
                   
                   Triangle<V,T>.Edge inToV = outFromV.getOppositeEdge();
                   linkOpposingEdges(newBoundaryNext, inToV);
                   linkOpposingEdges(newBoundaryPrev, outFromV);
                }
                
             } else if (prevEdgeAttached && !nextEdgeAttached) {
                // CASE 2. Link the "unattached" boundary triangle that's opposite "nextEdge":
                
                Triangle<V,T>.Edge newBoundaryNext = nextEdge.cwAroundStart();          // (points away from v)
                Triangle<V,T>.Edge oldBoundaryPrev = oppositePrevEdge.cwAroundStart();  // (points away from v)
                linkOpposingEdges(newBoundaryNext, oldBoundaryPrev.getOppositeEdge());

             } else if (!prevEdgeAttached && nextEdgeAttached) {
                // CASE 3. Link the "unattached" boundary triangle that's opposite "prevEdge":
                
                Triangle<V,T>.Edge newBoundaryPrev = prevEdge.ccwAroundEnd();           // (points toward v)
                Triangle<V,T>.Edge oldBoundaryNext = oppositeNextEdge.ccwAroundEnd();   // (points toward v)
                linkOpposingEdges(newBoundaryPrev, oldBoundaryNext.getOppositeEdge());
                
             } else {
                // CASE 4. BOTH edges are attached.  We need the old boundaries to be adjacent:
                
                Triangle<V,T>.Edge oldBoundaryPrev = oppositePrevEdge.cwAroundStart();  // (points away from v)
                Triangle<V,T>.Edge oldBoundaryNext = oppositeNextEdge.ccwAroundEnd();   // (points toward v)    
                if (oldBoundaryPrev.getOppositeEdge() != oldBoundaryNext) {
                   
                   // The old boundaries are not adjacent.  However, we can fix this, and there's no problem,
                   // if the vertex v has two consecutive boundary triangles elsewhere:
                   
                   Triangle<V,T>.Edge outFromV = oppositePrevEdge.ccwAroundStart();
                   Triangle<V,T>.Edge outFromVEnd = nextEdge.cwAroundStart();
                   boolean foundDoubleBoundary = false;                   
                   do {
                      if (outFromV.getTriangle().isBoundary() && 
                          outFromV.getOppositeEdge().getTriangle().isBoundary()) {
                         foundDoubleBoundary = true;
                         break;
                      }
                      
                      outFromV = outFromV.ccwAroundStart();
                   } while (outFromV != outFromVEnd);
                   
                   check(foundDoubleBoundary, "Triangle filling corner vertex has un-movable extra triangles");
                   
                   Triangle<V,T>.Edge inToV = outFromV.getOppositeEdge();
                   linkOpposingEdges(inToV, oldBoundaryNext.getOppositeEdge());
                   linkOpposingEdges(outFromV, oldBoundaryPrev.getOppositeEdge());
                }
             }
             
             // Make sure v points to us:
             v.setOneOutgoingEdge(nextEdge);
          }

          // Finally we connect the backlinks from each OPPOSITE-EDGE pointer in the Triangle
          for (Triangle<V,T>.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             Triangle<V,T>.Edge oppositeEi = ei.getOppositeEdge();
             boolean isAttached = !oppositeEi.getTriangle().isBoundary();
             
             if (isAttached) {
                Triangle<V,T> b = oppositeEi.getOppositeEdge().getTriangle();
                // DELETE OLD BOUNDARY TRIANGLES HERE
                boundaryTriangles.remove(b);
             }
             oppositeEi.setOppositeEdge(ei);
          }
          
          return t;
      }
      
      // --------------------------------------------------------
      // RemoveTriangle
      // --------------------------------------------------------
      
      public void removeTriangle (Triangle t) {
         check(interiorTriangles.contains(t), "Triangle to be removed already missing");
         
         // TODO...
      }
      
   }

   
   // -----------------------------------------------------------------------
   // Triangle
   // -----------------------------------------------------------------------
   
   public static class TexInfo {

       public Vector2f  t1,t2,t3;
       public float t1w,t2w,t3w;
       public ColorRGBA c1,c2,c3;

       public TexInfo() {
          t1=t2=t3=null;
          t1w=t2w=t3w=1.0f;
          c1=c2=c3=null;
       }
       
       public void setTexCoords(Vector2f t1, float t1w, Vector2f t2,float t2w, Vector2f t3, float t3w) {
          this.t1 = t1;
          this.t2 = t2;
          this.t3 = t3;
          this.t1w = t1w;
          this.t2w = t2w;
          this.t3w = t3w;
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

      public Mesh<Vector3f,TexInfo> mesh;
      
      public Model() {
         mesh = new Mesh<Vector3f,TexInfo>();
      }

      public int numTriangles() {
           return mesh.interiorTriangles.size();
      }


      public Mesh.Vertex<Vector3f,TexInfo> getOrAddVertex(Vector3f position) {
         for (Mesh.Vertex<Vector3f,TexInfo> v : mesh.vertices) {
            Vector3f vPosition = v.getData();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         Mesh.Vertex<Vector3f,TexInfo> v = new Mesh.Vertex<Vector3f,TexInfo>();
         v.setData(position);
         mesh.vertices.add(v);
         return v;
      }
      
       // -- -- -- -- -- -- -- --

       public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Vector2f aTex,
                                float aTexW, Vector2f bTex, float bTexW, Vector2f cTex, float cTexW) {

          System.out.format("Adding triangle:  POS (%s,%s,%s)\nTEX (%s,%s,%s)\n--\n",
                a.toString(), b.toString(), c.toString(),
                aTex.toString(), bTex.toString(), cTex.toString());
                
          Mesh.Vertex<Vector3f,TexInfo> va = getOrAddVertex(a);
          Mesh.Vertex<Vector3f,TexInfo> vb = getOrAddVertex(b);
          Mesh.Vertex<Vector3f,TexInfo> vc = getOrAddVertex(c);
          Mesh.Triangle<Vector3f,TexInfo> t = mesh.addTriangle(va, vb, vc);
          
          TexInfo ti = new TexInfo();
          ti.setTexCoords(aTex,aTexW, bTex, bTexW, cTex,cTexW);
          ti.setColors(new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff),
                       new ColorRGBA((byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff),
                       new ColorRGBA((byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff));
          t.setData(ti);
             
          System.out.format("MESH has %d vertices, %d interior triangles and %d boundary edges\n", mesh.vertices.size(),
                mesh.interiorTriangles.size(), mesh.boundaryTriangles.size());
          mesh.checkMesh();
       }
       
       public void addSquare (Vector3f center, Vector3f dx, Vector3f dy) {
           Vector3f tr = center.plus(dx).plus(dy);
           Vector3f tl = center.minus(dx).plus(dy);
           Vector3f br = center.plus(dx).minus(dy);
           Vector3f bl = center.minus(dx).minus(dy);

           addTriangle(bl, br, tl, 
                 new Vector2f(0.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(0.0f, 0.0f), 1.0f);
                 
           addTriangle(tl, br, tr,
                 new Vector2f(0.0f, 0.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 0.0f), 1.0f);
       }

       // -- -- -- -- -- -- -- --

       public static class Arrays {
           float[] positions;
           float[] texCoords;
           float[] baryCoords;
           float[] colors;
       }

       public Arrays getArrays() {
           Arrays result = new Arrays();
           int n = numTriangles();

           result.positions  = new float[n*3*4];
           result.baryCoords = new float[n*3*2];
           result.texCoords  = new float[n*3*4];
           result.colors     = new float[n*3*4];
           
           int pPos = 0;
           int pBary = 0;
           int pTex = 0;
           int pCol = 0;
           
           for (Mesh.Triangle<Vector3f,TexInfo> t : mesh.interiorTriangles) {
              Vector3f v0Pos = t.edge0.getOppositeVertex().getData();
              Vector3f v1Pos = t.edge1.getOppositeVertex().getData();
              Vector3f v2Pos = t.edge2.getOppositeVertex().getData();
              TexInfo ti = t.getData();

              // pos
              pPos = copyVector3fAs4(result.positions, pPos, v0Pos);
              pPos = copyVector3fAs4(result.positions, pPos, v1Pos);
              pPos = copyVector3fAs4(result.positions, pPos, v2Pos);
              
              // bary
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(0.0f, 0.0f));
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(0.0f, 1.0f));
              pBary = copyVector2f(result.baryCoords, pBary, new Vector2f(1.0f, 1.0f));
              
              // tex
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t1, ti.t1w);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t2, ti.t2w);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t3, ti.t3w);
              
              // col
              pCol = copyColor(result.colors, pCol, ti.c1);
              pCol = copyColor(result.colors, pCol, ti.c2);
              pCol = copyColor(result.colors, pCol, ti.c3);
           }
           return result;
       }

       private int copyVector3fAs4(float[] arr, int base, Vector3f v) {
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
       private int copyVector2fAs4(float[] arr, int base, Vector2f v, float w) {
          arr[base+0] = v.x * w;
          arr[base+1] = v.y;
          arr[base+2] = 0.0f;
          arr[base+3] = w;
          return base+4;
      }
       private int copyColor(float[] arr, int base, ColorRGBA c) {
           arr[base+0] = ((float)(c.r&0xff))/255.0f;
           arr[base+1] = ((float)(c.g&0xff))/255.0f;
           arr[base+2] = ((float)(c.b&0xff))/255.0f;
           arr[base+3] = ((float)(c.a&0xff))/255.0f;
           return base+4;
       }
   }
   

   // -----------------------------------------------------------------------
   // HashCodeMaker
   // -----------------------------------------------------------------------
   
   private static class HashCodeMaker {
      public HashCodeMaker()    { code = 1;    }
      public int getHashCode()  { return code; }
      
      public HashCodeMaker addInt(int i)         { code = 37 * code + i;  return this;         }
      public HashCodeMaker addBoolean(boolean b) { return addInt(b?1:0);                       }
      public HashCodeMaker addByte(byte b)       { return addInt((int)b);                      }
      public HashCodeMaker addLong(long l)       { return addInt((int)(l ^ (l >>> 32)));       }
      public HashCodeMaker addFloat(float f)     { return addInt(Float.floatToIntBits(f));     }
      public HashCodeMaker addDouble(double d)   { return addLong(Double.doubleToLongBits(d)); }
      
      private int code;
   }   
}

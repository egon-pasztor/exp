package demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import demo.VectorAlgebra.*;
import demo.Raster.*;


public class GLMath {

   // -----------------------------------------------------------------------
   // CameraBall
   // -----------------------------------------------------------------------
  
   public static class CameraBall {
      
      public CameraBall (int windowWidth, int windowHeight,
                         Vector3f lookAtPoint,
                         Vector3f cameraPosition,
                         Vector3f cameraUpVector,
                         float verticalFovInDegrees) {         
         
         this.width = windowWidth;
         this.height = windowHeight;
         this.lookAtPoint = lookAtPoint;
         this.cameraPosition = cameraPosition;
         this.cameraUpVector = cameraUpVector;         
         this.verticalFovInDegrees = verticalFovInDegrees;
         
         updateDerivedFields();
      }
      
      // -----------------------------------------------
      // These are "free fields" specified by the user:
      // -----------------------------------------------
      
      private Vector3f cameraPosition;
      private Vector3f cameraUpVector;
      private Vector3f lookAtPoint;
      
      public Vector3f getCameraPosition() {
         return cameraPosition;
      }
      public Vector3f getLookAtPoint() {
         return lookAtPoint;
      }      
      public Vector3f getCameraUpVector() {
         return cameraUpVector;
      }

      private int width, height;
      private float verticalFovInDegrees;
      
      public int getWidth() {
         return width;
      }
      public int getHeight() {
         return height;
      }
      public float getVerticalFOV() {
         return verticalFovInDegrees;
      }      

      // ---------------------------------------------
      // These are "derived fields" computed by update:
      // ---------------------------------------------
      
      private Matrix4f worldToCameraSpace;
      private Matrix4f cameraToClipSpace;
      private Vector3f camX, camY, camZ;      

      public Matrix4f getCameraToClipSpace() {
         return cameraToClipSpace;
      }
      public Matrix4f getWorldToCameraSpace() {
         return worldToCameraSpace;
      }         
      
      private void updateDerivedFields() {
         
         // The objects in the world are in a right-handed-coordinate system.
         // In WORLD-SPACE:
         //    the CAMERA is at "cameraPosition",
         //    the TARGET is at "lookatPoint".
         //
         // The first thing we do is TRANSLATE by "-cameraPosition":

         Matrix4f translateCameraToOrigin = new Matrix4f(
                1.0f,    0.0f,    0.0f,   -cameraPosition.x,
                0.0f,    1.0f,    0.0f,   -cameraPosition.y,
                0.0f,    0.0f,    1.0f,   -cameraPosition.z,
                0.0f,    0.0f,    0.0f,    1.0f);
         
         worldToCameraSpace = translateCameraToOrigin;
               
         // Now the CAMERA is at the origin, 
         // and the TARGET is at "cameraToLookat":
         
         Vector3f cameraToLookat = lookAtPoint.minus(cameraPosition);
         float distanceToTarget = cameraToLookat.length();
         
         // We want to ROTATE to put the TARGET on the -Z axis.
         //
         // A unit vector pointing in the opposite direction as "cameraToLookat'
         // will be the new Z axis, and we select X and Y perdendiculer to Z
         // such that "cameraUpVector" is in the Z-Y plane:
         
         camZ = cameraToLookat.times(-1.0f / distanceToTarget);
         camX = Vector3f.crossProduct(cameraUpVector, camZ).normalized();
         camY = Vector3f.crossProduct(camZ, camX).normalized();
               
         Matrix4f rotateSoTargetIsOnNegativeZ = new Matrix4f(
               camX.x,     camX.y,      camX.z,    0.0f,
               camY.x,     camY.y,      camY.z,    0.0f,
               camZ.x,     camZ.y,      camZ.z,    0.0f,
               0.0f,       0.0f,        0.0f,      1.0f);

         worldToCameraSpace = Matrix4f.product(rotateSoTargetIsOnNegativeZ, 
                                               worldToCameraSpace);
         
         // Now the CAMERA is at the origin,
         // and the TARGET is at <0,0,-distanceToTarget>
         //
         // The next step is to scale by 1/distanceToTarget:
         
         float scale = 1.0f / distanceToTarget;
         Matrix4f scaleByDistanceToTarget = new Matrix4f(
               scale,   0.0f,    0.0f,    0.0f,
               0.0f,    scale,   0.0f,    0.0f,
               0.0f,    0.0f,    scale,   0.0f,
               0.0f,    0.0f,    0.0f,    1.0f);

         worldToCameraSpace = Matrix4f.product(scaleByDistanceToTarget, 
                                               worldToCameraSpace);
         
         // Now we're fully in CAMERA-SPACE:
         // In CAMERA-SPACE:
         //    the CAMERA is at <0,0,  0>
         //    the TARGET is at <0,0, -1>
         
         float aspect = ((float)width) / height;
         float fHeight = (float) Math.tan(verticalFovInDegrees * (Math.PI / 180.0) * 0.5);
         float fWidth  = aspect * fHeight;

         // So  <0, 0, -1>  ... is expected to map to the CENTER of the viewport.
         //         
         // Our vertical "field of view in degrees" determines how much
         // of the <x,y> plane at z=-1 we can see in our viewport:
         //
         //    <fWidth,   0, -1>  ... is expected to map to the RIGHT-MIDDLE of the window
         //    <0,  fHeight, -1>  ... is expected to map to the MIDDLE-TOP of the window
         //
         // We're going to scale the x and y dimensions non-linearly so these become +/-1:
         
         Matrix4f scaleXYByFieldOfView = new Matrix4f(
               1/fWidth,   0.0f,        0.0f,    0.0f,
               0.0f,       1/fHeight,   0.0f,    0.0f,
               0.0f,       0.0f,        1.0f,    0.0f,
               0.0f,       0.0f,        0.0f,    1.0f);
         
         cameraToClipSpace = scaleXYByFieldOfView;
               
         // Now:  
         //   <0,  0, -1>  ... is expected to map to the CENTER of the window
         //   <1,  0, -1>  ... is expected to map to the RIGHT-MIDDLE of the window
         //   <0,  1, -1>  ... is expected to map to the MIDDLE-TOP of the window
         // 
         // In this space our "field of view" has become a full 90-degrees,
         // so any point where y is equal to -z should map to the TOP-MIDDLE, or:
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
         // This leaves only two unknown values, call them M and C:
         //
         //   new Matrix4f(
         //      1.0f,   0.0f,   0.0f,   0.0f,
         //      0.0f,   1.0f,   0.0f,   0.0f,
         //      0.0f,   0.0f,     M,      C,
         //      0.0f,   0.0f,  -1.0f,   0.0f );         
         //
         // And this will force:
         //
         //   z_view  ==  (zM+C) / -z   ==  -C/z -M
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
         //        -1 == -C/NEAR - M
         //        +1 == -C/FAR  - M
         //
         // Solving for M and C results:
         //
         //   M =  - (FAR+NEAR)/(FAR-NEAR)
         //   C =  (2*FAR*NEAR)/(FAR-NEAR)
         
         float nearZ =  -0.1f;
         float farZ  = -10.0f;
         
         float M =   - (farZ + nearZ)    / (farZ - nearZ);
         float C = (2.0f * farZ * nearZ) / (farZ - nearZ);
         
         Matrix4f perspectiveTransform = new Matrix4f(
               1.0f,   0.0f,   0.0f,   0.0f,
               0.0f,   1.0f,   0.0f,   0.0f,
               0.0f,   0.0f,    M,       C,
               0.0f,   0.0f,  -1.0f,   0.0f);
         
         cameraToClipSpace = Matrix4f.product(perspectiveTransform,
                                              cameraToClipSpace);
      }
      
      // -------------------------------------------------------------------
      // Click-and-Drag the CameraBall
      // -------------------------------------------------------------------
      
      public enum GrabType { Rotate,   // Move camera around fixed lookat_point
                             Zoom,     // Move camera closer or further from fixed lookat_point
                             Pan,      // Move both camera and lookat_point by the same amount
                             FOV };
         
      private boolean grabbed;
      private GrabType grabType;
      
      // Some of these fields are set when a grab begins,
      // and are used to update the free fields after each mouse movement:
      
      private float xGrab, yGrab;

      private Vector3f grabLookAtPoint, grabLookAtToCamera;
      private Vector3f grabCamX, grabCamY, grabCamZ;
      private float grabFovTangent;
      
      // For rotate grabs
      private boolean roll;
      private float grabAngle;
      // For zoom grabs
      private float tScale;
      // For pan grabs
      private float windowScale;

      public void grab(int ix, int iy, GrabType grabType) {
         this.grabbed  = true;
         this.grabType = grabType;
         
         float y = (height/2-iy) / ((float) (height/2));
         float x = (ix-width/2)  / ((float) (height/2));
         xGrab = x;
         yGrab = y;
        
         grabLookAtPoint    = lookAtPoint;
         grabLookAtToCamera = cameraPosition.minus(lookAtPoint);
         grabCamX           = camX;
         grabCamY           = camY;
         grabCamZ           = camZ;
         grabFovTangent     = ((float) Math.tan(verticalFovInDegrees * (Math.PI / 180.0) * 0.5f));
         
         if (grabType == GrabType.Rotate) {
            roll = (x*x+y*y) > 1;
            if (roll) grabAngle = (float) Math.atan2(y,x);
            
         } else if (grabType == GrabType.Zoom) {
            tScale = (y < 0.5) ? (1-y) : (y);

         } else if (grabType == GrabType.FOV) {
            tScale = (y < 0.5) ? (1-y) : (y);

         } else if (grabType == GrabType.Pan) {
            windowScale = grabFovTangent * grabLookAtToCamera.length();
         }
      }
      public void moveTo(int ix, int iy) {
         if (!grabbed) return;
         
         float y  = (height/2-iy) / ((float) (height/2));
         float x  = (ix-width/2)  / ((float) (height/2));         
         float dx = x-xGrab;
         float dy = y-yGrab; 

         if (grabType == GrabType.Rotate) {
            
            if ((dx == 0) && (dy == 0)) {               
               cameraPosition = grabLookAtToCamera.plus(grabLookAtPoint);
               cameraUpVector = grabCamY;
               
            } else {
               Vector3f cameraSpaceAxis;
               float angle;
   
               if (roll) {
                  cameraSpaceAxis = Vector3f.Z; 
                  angle = - (float) (Math.atan2(y,x) - grabAngle) * Scale2DRotation;
               } else { 
                  cameraSpaceAxis = new Vector3f ((float) -dy,(float) dx, 0).normalized();
                  angle = - ((float) Math.sqrt(dx*dx+dy*dy)) * Scale3DRotation;
               }
               Vector3f rotationAxis = grabCamX.times(cameraSpaceAxis.x)
                                 .plus(grabCamY.times(cameraSpaceAxis.y))
                                 .plus(grabCamZ.times(cameraSpaceAxis.z))
                                 .normalized();
               
               cameraPosition = grabLookAtToCamera.rotated(rotationAxis, angle).plus(grabLookAtPoint);
               cameraUpVector = grabCamY.rotated(rotationAxis, angle);
            }
            
         } else if (grabType == GrabType.Pan) {

            Vector3f translation = grabCamX.times(-dx)
                             .plus(grabCamY.times(-dy))
                             .times(windowScale);

            lookAtPoint    = grabLookAtPoint.plus(translation);
            cameraPosition = grabLookAtToCamera.plus(lookAtPoint);
            
         } else {            
            // Either "ZOOM" or "FOV", two ways or making the target look bigger or smaller,
            // either by moving the camera closer to the target or by changing the field of view:
            
            float scale = (float) Math.pow(ScaleZoom, dy/tScale);
            
            if (grabType == GrabType.Zoom) {
               cameraPosition = grabLookAtToCamera.times(scale).plus(grabLookAtPoint);
            } else {
               float newFovTangent = grabFovTangent * scale;
               verticalFovInDegrees = (float) (2.0f * (180.0f / Math.PI) * Math.atan(newFovTangent));
            }
         }
         
         updateDerivedFields();
      }
      
      public void release() {
         grabbed = false;
      }
      public boolean isGrabbed() {
         return grabbed;
      }
      
      private static final float ScaleZoom       = 3.0f;
      private static final float Scale2DRotation = 1.5f;
      private static final float Scale3DRotation = 2.0f;
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
       public float ext;
       public float t1w,t2w,t3w;
       public ColorARGB c1,c2,c3;

       public TexInfo() {
          t1=t2=t3=null;
          t1w=t2w=t3w=1.0f;
          c1=c2=c3=null;
       }
       
       public void setTexCoords(Vector2f t1, float t1w, Vector2f t2,float t2w, Vector2f t3, float t3w, float ext) {
          this.t1 = t1;
          this.t2 = t2;
          this.t3 = t3;
          this.t1w = t1w;
          this.t2w = t2w;
          this.t3w = t3w;
          this.ext = ext;
       }         
       public void setColors(ColorARGB c1, ColorARGB c2, ColorARGB c3) {
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
          addTriangle (a,b,c, aTex,aTexW, bTex,bTexW, cTex,cTexW, 0.0f);
       }
       public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Vector2f aTex,
                                float aTexW, Vector2f bTex, float bTexW, Vector2f cTex, float cTexW, float ext) {

          System.out.format("Adding triangle: POS ==\n%s--\n%s--\n%s\nTEX ==\n%s--\n%s--\n%s######\n",
                a.toString(), b.toString(), c.toString(),
                aTex.toString(), bTex.toString(), cTex.toString());
                
          Mesh.Vertex<Vector3f,TexInfo> va = getOrAddVertex(a);
          Mesh.Vertex<Vector3f,TexInfo> vb = getOrAddVertex(b);
          Mesh.Vertex<Vector3f,TexInfo> vc = getOrAddVertex(c);
          Mesh.Triangle<Vector3f,TexInfo> t = mesh.addTriangle(va, vb, vc);
          
          TexInfo ti = new TexInfo();
          ti.setTexCoords(aTex,aTexW, bTex, bTexW, cTex,cTexW, ext);
          
          ColorARGB col = new ColorARGB((byte)0x00, (byte)0xff, (byte)0x00, (byte)0x00);
          
          ti.setColors(col,col,col);
          t.setData(ti);
             
          System.out.format("MESH has %d vertices, %d interior triangles and %d boundary edges\n", mesh.vertices.size(),
                mesh.interiorTriangles.size(), mesh.boundaryTriangles.size());
          mesh.checkMesh();
       }
       
       public void addSquare (Vector3f center, Vector3f dx, Vector3f dy, float ext) {
           Vector3f tr = center.plus(dx).plus(dy);
           Vector3f tl = center.minus(dx).plus(dy);
           Vector3f br = center.plus(dx).minus(dy);
           Vector3f bl = center.minus(dx).minus(dy);

           addTriangle(bl, br, tl, 
                 new Vector2f(0.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(0.0f, 0.0f), 1.0f, ext);
                
           addTriangle(tl, br, tr,
                 new Vector2f(0.0f, 0.0f), 1.0f,
                 new Vector2f(1.0f, 1.0f), 1.0f,
                 new Vector2f(1.0f, 0.0f), 1.0f, ext);
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
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t1, ti.t1w, ti.ext);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t2, ti.t2w, ti.ext);
              pTex = copyVector2fAs4(result.texCoords, pTex, ti.t3, ti.t3w, ti.ext);
              
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
       private int copyVector2fAs4(float[] arr, int base, Vector2f v, float w, float ext) {
          arr[base+0] = v.x * w;
          arr[base+1] = v.y;
          arr[base+2] = ext;
          arr[base+3] = w;
          return base+4;
      }
       private int copyColor(float[] arr, int base, ColorARGB c) {
           arr[base+0] = ((float)(c.r&0xff))/255.0f;
           arr[base+1] = ((float)(c.g&0xff))/255.0f;
           arr[base+2] = ((float)(c.b&0xff))/255.0f;
           arr[base+3] = ((float)(c.a&0xff))/255.0f;
           return base+4;
       }
   }
   

}

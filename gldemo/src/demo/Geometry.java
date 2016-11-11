package demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import demo.Raster.*;   // for ColorARGB
import demo.VectorAlgebra.*;
import demo.World.*;

public class Geometry {
   
   public static void check(boolean cond, String err) {
      if (!cond) throw new RuntimeException("FAILED: " + err);
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
      
      public Triangle<V,T>.Edge swap(Triangle<V,T>.Edge e) {
         Triangle<V,T>.Edge oe = e.getOppositeEdge();
         
         Triangle<V,T>.Edge trInner = oe.cwAroundTriangle();
         Triangle<V,T>.Edge brInner = oe.ccwAroundTriangle();
         Triangle<V,T>.Edge tlInner = e.ccwAroundTriangle();
         Triangle<V,T>.Edge blInner = e.cwAroundTriangle();
         
         Triangle<V,T>.Edge trOuter = trInner.getOppositeEdge();
         Triangle<V,T>.Edge blOuter = blInner.getOppositeEdge();         
         
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
                   
                   // The old boundaries are not adjacent.  However, we can fix this, there's no topology problem,
                   // so long as the vertex v also has another two consecutive boundary triangles.
                   
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
      
      public void removeTriangle (Triangle<V,T> t) {
         check(interiorTriangles.contains(t), "Triangle to be removed already missing");
         
         // TODO...
      }
   }
   
   // -----------------------------------------------------------------------
   // A Concrete Geometry Class...
   // -----------------------------------------------------------------------
 
   public static class MeshModel {
      public final String name;
      public final Mesh<Vector3f,Object> mesh;
      public float maxRadius;
      
      public int getNumTriangles() {
         return mesh.interiorTriangles.size();
      }
      public int getNumVertices() {
         return mesh.vertices.size();
      }
      public String getName() {
         return name;
      }
      
      public MeshModel(String name) {
         this.name = name;
         this.mesh = new Mesh<Vector3f,Object>();
         
         buffers = new HashMap<String,Shader.ManagedBuffer>();
         buffers.put(Shader.POSITION_ARRAY, defaultPositionBuffer(mesh));
         buffers.put(Shader.BARY_COORDS,    defaultBaryCoords(mesh));
         buffers.put(Shader.COLOR_ARRAY,    defaultColorArray(mesh));
      }

      // ------------------------------------------------------------------------
      // Building Models
      // ------------------------------------------------------------------------
      
      public Mesh.Vertex<Vector3f,Object> getOrAddVertex(Vector3f position) {         
         // Search to see if we already have a Vertex at this position
         // TODO:  Use a 3D index for this...
         for (Mesh.Vertex<Vector3f,Object> v : mesh.vertices) {
            Vector3f vPosition = v.getData();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         
         // Create a new vertex
         Mesh.Vertex<Vector3f,Object> v = new Mesh.Vertex<Vector3f,Object>();
         v.setData(position);
         mesh.vertices.add(v);
         updateMaxRadius(position.length());
         return v;
      }
      
      public void updateMaxRadius(float radius) {
         if (radius > maxRadius) maxRadius = radius;
      }
      
      public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Object ti) {
         Mesh.Vertex<Vector3f,Object> va = getOrAddVertex(a);
         Mesh.Vertex<Vector3f,Object> vb = getOrAddVertex(b);
         Mesh.Vertex<Vector3f,Object> vc = getOrAddVertex(c);
         Mesh.Triangle<Vector3f,Object> t = mesh.addTriangle(va, vb, vc);
         t.setData(ti);
      }
      
      // ------------------------------------------------------------------------
      // Map of Managed Buffers...
      // ------------------------------------------------------------------------

      public Shader.ManagedBuffer getManagedBuffer(String key) {
         return buffers.get(key);
      }
      public void setManagedBuffer(String key, Shader.ManagedBuffer buffer) {
         buffers.put(key, buffer);
      }
      private HashMap<String,Shader.ManagedBuffer> buffers;

   }
   
   // -------------------------------------------------------------------------
   // Basic Array Builders for Geometry / BaryCoords
   //    .. and an "empty" one for Colors
   // -------------------------------------------------------------------------
   
   private static Shader.ManagedBuffer defaultPositionBuffer(final Mesh<Vector3f,Object> mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle<Vector3f,?> t : mesh.interiorTriangles) {
               Vector3f v0Pos = t.edge0.getOppositeVertex().getData();
               Vector3f v1Pos = t.edge1.getOppositeVertex().getData();
               Vector3f v2Pos = t.edge2.getOppositeVertex().getData();
               pPos = Vector4f.fromVector3f(v0Pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(v1Pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(v2Pos).copyToFloatArray(array, pPos);
            }
         }
      };
   }

   private static Shader.ManagedBuffer defaultBaryCoords(final Mesh<Vector3f,Object> mesh) {
      return new Shader.ManagedBuffer(2) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle<Vector3f,?> t : mesh.interiorTriangles) {
               pPos = (new Vector2f(0.0f,0.0f)).copyToFloatArray(array,  pPos);
               pPos = (new Vector2f(0.0f,1.0f)).copyToFloatArray(array,  pPos);
               pPos = (new Vector2f(1.0f,1.0f)).copyToFloatArray(array,  pPos);
            }
         }
      };
   }

   private static Shader.ManagedBuffer defaultColorArray(final Mesh<Vector3f,Object> mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle<Vector3f,?> t : mesh.interiorTriangles) {
               ColorARGB color = new ColorARGB((byte)0x00, (byte)0xff, (byte)0x00, (byte)0x00);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, ColorARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             //arr[base+3] = ((float)(c.a&0xff))/255.0f;
             return base+3;
         }
      };
   }
   
   // -----------------------------------------------------------------------
   // CUBE
   // -----------------------------------------------------------------------
   
   public static MeshModel createUnitCube () {
      final MeshModel m = new MeshModel("UnitCube");
      m.setManagedBuffer(Shader.TEX_COORDS, cubeTextureCoordsArray(m.mesh));
      
      final Vector3f cntr = Vector3f.Z;
      final Vector3f dX   = Vector3f.X;
      final Vector3f dY   = Vector3f.Y;
      final float halfpi = (float) (Math.PI/2);
      
      addSquare(m, cntr.rotated(Vector3f.X, -halfpi),
                     dX.rotated(Vector3f.X, -halfpi),
                     dY.rotated(Vector3f.X, -halfpi), 0);

      for (int i = 0; i < 4; ++i) {
         float angle = i * halfpi;
         addSquare(m, cntr.rotated(Vector3f.Y, angle),
                        dX.rotated(Vector3f.Y, angle),
                        dY.rotated(Vector3f.Y, angle), 1+i);
      }
      
      addSquare(m, cntr.rotated(Vector3f.X, halfpi),
                     dX.rotated(Vector3f.X, halfpi),
                     dY.rotated(Vector3f.X, halfpi), 5);
      
      return m;
   }
   
   private static void addSquare (MeshModel m, Vector3f center, Vector3f dx, Vector3f dy, int face) {
      CubeFaceInfo texBL = null;
      CubeFaceInfo texTR = null;
      if (face >= 0) {
         final Vector2f uv00 = new Vector2f(0.0f, 0.0f);
         final Vector2f uv10 = new Vector2f(1.0f, 0.0f);
         final Vector2f uv01 = new Vector2f(0.0f, 1.0f);
         final Vector2f uv11 = new Vector2f(1.0f, 1.0f);
         
         texBL = new CubeFaceInfo(face, uv01,uv11,uv00);
         texTR = new CubeFaceInfo(face, uv00,uv11,uv10);
      }
      
      Vector3f tr = center.plus(dx).plus(dy);
      Vector3f tl = center.minus(dx).plus(dy);
      Vector3f br = center.plus(dx).minus(dy);
      Vector3f bl = center.minus(dx).minus(dy);
      m.addTriangle(bl, br, tl, texBL);
      m.addTriangle(tl, br, tr, texTR);
   }

   private static class CubeFaceInfo {
      public final int face;
      public final Vector2f tex0,tex1,tex2;
      public CubeFaceInfo(int face, Vector2f tex0, Vector2f tex1, Vector2f tex2) {
         this.face = face; this.tex0 = tex0; this.tex1 = tex1; this.tex2 = tex2;
      }
   }

   private static Shader.ManagedBuffer cubeTextureCoordsArray(final Mesh<Vector3f,Object> mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle<Vector3f,Object> t : mesh.interiorTriangles) {
               CubeFaceInfo faceInfo = (CubeFaceInfo) t.getData();
               pPos = toVector4f(faceInfo.tex0).copyToFloatArray(array, pPos);
               pPos = toVector4f(faceInfo.tex1).copyToFloatArray(array, pPos);
               pPos = toVector4f(faceInfo.tex2).copyToFloatArray(array, pPos);
            }
         }
         private Vector4f toVector4f(Vector2f tex1) {
            return new Vector4f(tex1.x, tex1.y, 0.0f, 1.0f);
         }
      };
   }

   // -----------------------------------------------------------------------
   // SPHERE
   // -----------------------------------------------------------------------

   public static MeshModel createUnitSphere(int numLatDivisions, int numLonDivisions) {
      MeshModel m = new MeshModel("UnitSphere");
      m.setManagedBuffer(Shader.TEX_COORDS, sphereTextureCoordsArray(m.mesh));

      double globalLatMin = -Math.PI/2;
      double globalLatMax =  Math.PI/2;
      double globalLonMin = 0;
      double globalLonMax = 2*Math.PI;
      
      for (int lat = 0; lat < numLatDivisions; lat++) {
         for (int lon = 0; lon < numLonDivisions; lon++) {
            
            float latMin = (float) (globalLatMin + ((lat * (globalLatMax - globalLatMin)) / numLatDivisions));
            float latMax = (float) (globalLatMin + (((lat+1) * (globalLatMax - globalLatMin)) / numLatDivisions));
            float lonMin = (float) (globalLonMin + ((lon * (globalLonMax - globalLonMin)) / numLonDivisions));
            float lonMax = (float) (globalLonMin + (((lon+1) * (globalLonMax - globalLonMin)) / numLonDivisions));
            
            Vector2f tR = new Vector2f(latMax, lonMax);
            Vector2f tL = new Vector2f(latMax, lonMin);
            Vector2f bR = new Vector2f(latMin, lonMax);
            Vector2f bL = new Vector2f(latMin, lonMin);
            
            if (lat > 0) {
               addLatLonTriangle(m, tL,bL,bR);
            }
            if (lat < numLatDivisions-1) {
               addLatLonTriangle(m, tL,bR,tR);
            }
         }
      }
      return m;
   }
   
   private static void addLatLonTriangle(MeshModel m, Vector2f latlon0, Vector2f latlon1, Vector2f latlon2) {
      m.addTriangle(
            latLonToPosition(latlon0.x, latlon0.y),
            latLonToPosition(latlon1.x, latlon1.y),
            latLonToPosition(latlon2.x, latlon2.y), 
            new SphereFaceInfo(latlon0, latlon1, latlon2));
   }
   private static Vector3f latLonToPosition(float lat, float lon) {
      float cosLat = (float) Math.cos(lat);
      float sinLat = (float) Math.sin(lat);
      float cosLon = (float) Math.cos(lon);
      float sinLon = (float) Math.sin(lon);
      return new Vector3f(cosLat * cosLon, cosLat * sinLon, sinLat);
   }
   private static Vector2f positionToLatLon(Vector3f pos) {
      float lat = (float) Math.asin(pos.z);
      float lon = (float) Math.atan2(pos.y,pos.x);
      if (lon < 0) lon += (float)(2 * Math.PI);
      return new Vector2f(lat,lon);
   }
   private static Vector2f latLonToTexCoord(float lat, float lon) {
      while (lon < 0)         lon += (float) (2 * Math.PI);
      while (lon > 2*Math.PI) lon -= (float) (2 * Math.PI);
      float x = (float) (lon / (2.0 * Math.PI));
      float y = 1.0f - (float) (((Math.PI/2.0) + lat) / Math.PI);
      return new Vector2f(x,y);
   }

   private static class SphereFaceInfo {
      public final Vector2f latlon0, latlon1, latlon2;
      public SphereFaceInfo(Vector2f latlon0, Vector2f latlon1, Vector2f latlon2) {
         this.latlon0 = latlon0; this.latlon1 = latlon1; this.latlon2 = latlon2;
      }
   }

   private static Shader.ManagedBuffer sphereTextureCoordsArray(final Mesh<Vector3f,Object> mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle<Vector3f,Object> t : mesh.interiorTriangles) {
               SphereFaceInfo faceInfo = (SphereFaceInfo) t.getData();
               Vector2f tex0 = latLonToTexCoord(faceInfo.latlon0.x, faceInfo.latlon0.y);
               Vector2f tex1 = latLonToTexCoord(faceInfo.latlon1.x, faceInfo.latlon1.y);
               Vector2f tex2 = latLonToTexCoord(faceInfo.latlon2.x, faceInfo.latlon2.y);
               
               // adjusting the trio of tex0/tex1/tex2 to be nearby in longitude
               float eps = .000001f;
               
               { float d1 = (float) Math.abs(tex1.x     -tex0.x);
                 float d2 = (float) Math.abs(tex1.x+1.0f-tex0.x);
                 if (d2 < d1) {
                    tex1 = tex1.plus(Vector2f.X);
                 } else {
                    float d3 = (float) Math.abs(tex1.x-1.0f-tex0.x);
                    if (d3 < d1) {
                       tex1 = tex1.minus(Vector2f.X);
                    }
                 }
               }
               { float d1 = (float) Math.abs(tex2.x    -tex1.x);
                 float d2 = (float) Math.abs(tex2.x+1.0f-tex1.x);
                 if (d2 < d1) {
                    tex2 = tex2.plus(Vector2f.X);
                 } else {
                    float d3 = (float) Math.abs(tex2.x-1.0f-tex1.x);
                    if (d3 < d1) {
                       tex2 = tex2.minus(Vector2f.X);
                    }
                 }
               }

               // emit the 3 texture coords:
               float tex0w = (float) Math.cos((tex0.y-0.5)*Math.PI) +eps;
               float tex1w = (float) Math.cos((tex1.y-0.5)*Math.PI) +eps;
               float tex2w = (float) Math.cos((tex2.y-0.5)*Math.PI) +eps;
               
               pPos = toVector4f(tex0,tex0w).copyToFloatArray(array, pPos);
               pPos = toVector4f(tex1,tex1w).copyToFloatArray(array, pPos);
               pPos = toVector4f(tex2,tex2w).copyToFloatArray(array, pPos);
            }
         }
         private Vector4f toVector4f(Vector2f tex1, float w) {
            return new Vector4f(tex1.x*w, tex1.y*w, 0.0f, w);
         }
      };
   }
   
   // -----------------------------------------------------------------------
   // ICO
   // -----------------------------------------------------------------------

   public static MeshModel createIco(int subdivisions) {
      MeshModel m = new MeshModel(String.format("Ico-Subdivided-%d", subdivisions));
      m.setManagedBuffer(Shader.TEX_COORDS, sphereTextureCoordsArray(m.mesh));

      float pi   = (float)Math.PI;
      float lat0 = pi/2;
      float lat1 = (float) Math.atan(0.5);
      float lat2 = -lat1;
      float lat3 = -lat0;
      
      for (int i = 0; i < 5; ++i) {
         float lon0 = 2*i*(pi/5);
         float lon1 = lon0+(pi/5);
         float lon2 = lon1+(pi/5);
         float lon3 = lon2+(pi/5);
         
         // Top
         addLatLonTriangles(m, new Vector2f(lat0,lon1),
                               new Vector2f(lat1,lon0),
                               new Vector2f(lat1,lon2), subdivisions);
         // "interior"
         addLatLonTriangles(m, new Vector2f(lat1,lon0),
                               new Vector2f(lat2,lon1),
                               new Vector2f(lat1,lon2), subdivisions);
         
         addLatLonTriangles(m, new Vector2f(lat1,lon2),
                               new Vector2f(lat2,lon1),
                               new Vector2f(lat2,lon3), subdivisions);

         // Bottom
         addLatLonTriangles(m, new Vector2f(lat2,lon1),
                               new Vector2f(lat3,lon2),
                               new Vector2f(lat2,lon3), subdivisions);
      }      
      return m;
   }
   
   private static void addLatLonTriangles(MeshModel m, Vector2f latlon0, Vector2f latlon1, Vector2f latlon2, int subdivisions) {
      
      if (subdivisions == 0) {
         addLatLonTriangle(m, latlon0, latlon1, latlon2);
         
      } else {
         Vector2f mid01 = latLonMidpoint(latlon0, latlon1);
         Vector2f mid12 = latLonMidpoint(latlon1, latlon2);
         Vector2f mid02 = latLonMidpoint(latlon0, latlon2);
         addLatLonTriangles(m, latlon0,   mid01,  mid02,    subdivisions-1);
         addLatLonTriangles(m, mid01,   latlon1,  mid12,    subdivisions-1);
         addLatLonTriangles(m, mid02,     mid12,  latlon2,  subdivisions-1);
         addLatLonTriangles(m, mid12,     mid02,  mid01,    subdivisions-1);
      }
   }
   private static Vector2f latLonMidpoint (Vector2f a, Vector2f b) {
      Vector3f ap = latLonToPosition(a.x,a.y);
      Vector3f bp = latLonToPosition(b.x,b.y);
      Vector3f midP = ap.plus(bp).times(0.5f).normalized();
      return positionToLatLon(midP);
   }
   
   // -----------------------------------------------------------------------
   // BEZIER / CYLINDER
   // -----------------------------------------------------------------------

   // todo...
   
   // -----------------------------------------------------------------------
   // Apply modifications..
   // -----------------------------------------------------------------------

   public static void sphereWarp (MeshModel model, float phase, float mag) {
      for (Mesh.Vertex<Vector3f,?> v : model.mesh.vertices) {
         Vector3f p = v.getData().normalized();
 
         Vector2f p2 = positionToLatLon(p);
         float phase2 = (float)(6.0 * p2.y);
         
         p = p.times((float)(1.0 - mag * Math.sin(phase) * Math.sin(phase2)));
         v.setData(p);
      }
      model.getManagedBuffer(Shader.POSITION_ARRAY).setModified(true);
   }
}

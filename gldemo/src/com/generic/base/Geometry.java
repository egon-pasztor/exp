package com.generic.base;

import com.generic.base.Mesh;
import com.generic.base.VectorAlgebra.*;
import com.generic.base.Shader;
import com.generic.base.Mesh.Triangle;
import com.generic.base.Raster.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Geometry {
   
   public static void check(boolean cond, String err) {
      if (!cond) throw new RuntimeException("FAILED: " + err);
   }

   // -----------------------------------------------------------------------
   // New-style Mesh model class
   // -----------------------------------------------------------------------
 
   public static class MeshModel2 {
      public final Mesh mesh;
      public MeshModel2(Mesh mesh) {
         this.mesh = mesh;
         
         buffers = new HashMap<String,Shader.ManagedBuffer>();
         setManagedBuffer(Shader.BARY_COORDS,    newBaryCoordsArrayManager(mesh));
         setManagedBuffer(Shader.COLOR_ARRAY,    newColorArrayManager(mesh));
         setManagedBuffer(Shader.POSITION_ARRAY, newPositionArrayManager(mesh));
         setManagedBuffer(Shader.TEX_COORDS,     newTextureCoordsArrayManager(mesh));
         setManagedBuffer(Shader.V0POS_ARRAY,    newVertexPositionArrayManager(mesh,0));
         setManagedBuffer(Shader.V2POS_ARRAY,    newVertexPositionArrayManager(mesh,1));
         setManagedBuffer(Shader.V1POS_ARRAY,    newVertexPositionArrayManager(mesh,2));
      }

      public Mesh.Vertex getOrAddVertex(Vector3f position) {         
         // Search to see if we already have a Vertex at this position
         // TODO:  Use a 3D index for this...
         for (Mesh.Vertex v : mesh.vertices) {
            Vector3f vPosition = v.getPosition();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         
         // Create a new vertex
         Mesh.Vertex v = mesh.addVertex();
         v.setPosition(position);
         return v;
      }
      public Mesh.Triangle addTriangle (Vector3f a, Vector3f b, Vector3f c) {
         Mesh.Vertex va = getOrAddVertex(a);
         Mesh.Vertex vb = getOrAddVertex(b);
         Mesh.Vertex vc = getOrAddVertex(c);
         System.out.format("Creating triangle with %d,%d,%d\n",
               va.getIndex(), vb.getIndex(), vc.getIndex());
         return addTriangle(va,vb,vc);
      }
      public Mesh.Triangle addTriangle (Mesh.Vertex va, Mesh.Vertex vb, Mesh.Vertex vc) {
         Mesh.Triangle t = mesh.addTriangle(va, vb, vc);
         mesh.checkMesh();
         return t;
      }
      
      // ------------------------------------------------------------------------
      // Map of Managed Buffers...
      // ------------------------------------------------------------------------

      public Shader.ManagedBuffer getManagedBuffer(String key) {
         return buffers.get(key);
      }
      public void setManagedBuffer(String key, Shader.ManagedBuffer buffer) {
         buffer.name = key;
         buffers.put(key, buffer);
      }
      private HashMap<String,Shader.ManagedBuffer> buffers;
   }
   

   // -----------------------------------------------------------------------
   // New-style Buffer-Builders
   // -----------------------------------------------------------------------
   
   public interface TextureCoordProvider {
      public Vector2f getTextureCoordinates(int vertexIndex);
   }
   private static Shader.ManagedBuffer newTextureCoordsArrayManager(final Mesh mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               TextureCoordProvider t = (TextureCoordProvider) tb;
               pPos = toVector4f(t.getTextureCoordinates(0)).copyToFloatArray(array, pPos);
               pPos = toVector4f(t.getTextureCoordinates(1)).copyToFloatArray(array, pPos);
               pPos = toVector4f(t.getTextureCoordinates(2)).copyToFloatArray(array, pPos);
            }
         }
         private Vector4f toVector4f(Vector2f tex1) {
            return new Vector4f(tex1.x, tex1.y, 0.0f, 1.0f);
         }
      };
   }

   private static Shader.ManagedBuffer newPositionArrayManager(final Mesh mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               pPos = Vector4f.fromVector3f(t.vertices[0].getPosition()).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(t.vertices[1].getPosition()).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(t.vertices[2].getPosition()).copyToFloatArray(array, pPos);
            }
         }
      };
   }

   private static Shader.ManagedBuffer newVertexPositionArrayManager(final Mesh mesh, final int index) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               Vector3f pos = t.vertices[index].getPosition();
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   
   private static Shader.ManagedBuffer newBaryCoordsArrayManager(final Mesh mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               pPos = (new Vector3f(1.0f, 0.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3f(0.0f, 1.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3f(0.0f, 0.0f, 1.0f)).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   
   // TOOD:  It sure looks like no-one's using this..
   private static Shader.ManagedBuffer newColorArrayManager(final Mesh mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            int col = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               ColorARGB color = 
                  (col==0) ? new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xff, (byte)0x80) :
                  (col==1) ? new ColorARGB((byte)0x00, (byte)0xc0, (byte)0xd0, (byte)0xb0) :
                  (col==2) ? new ColorARGB((byte)0x00, (byte)0x80, (byte)0xf0, (byte)0xd0) :
                             new ColorARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
               
               color = new ColorARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
               
               col = (col+1)%4;
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, ColorARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             return base+3;
         }
      };
   }   
   
   
   // -----------------------------------------------------------------------
   // New-style Cube?
   // -----------------------------------------------------------------------

   public static class CubeFaceTriangle extends Mesh.Triangle implements TextureCoordProvider {
      public int face;
      public Vector2f[] tex;
      
      public void setTex(Vector2f t0, Vector2f t1, Vector2f t2) {
         tex = new Vector2f[] { t0, t1, t2 };
      }
      @Override
      public Vector2f getTextureCoordinates(int vertexIndex) {
         return tex[vertexIndex % 3];
      }
   }

   public static MeshModel2 createUnitCube2 () {
      final Mesh mesh = new Mesh(new Mesh.Factory(){
         public CubeFaceTriangle newTriangle() { return new CubeFaceTriangle(); }
      });
      final MeshModel2 m = new MeshModel2(mesh);
      
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
      
      m.mesh.testAddAndDelete();
      return m;
   }
   
   private static void addSquare (MeshModel2 m, Vector3f center, Vector3f dx, Vector3f dy, int face) {
      final Vector3f tr = center.plus(dx).plus(dy);
      final Vector3f tl = center.minus(dx).plus(dy);
      final Vector3f br = center.plus(dx).minus(dy);
      final Vector3f bl = center.minus(dx).minus(dy);
      
      CubeFaceTriangle bottomLeft = (CubeFaceTriangle) m.addTriangle(bl, br, tl);
      CubeFaceTriangle topRight   = (CubeFaceTriangle) m.addTriangle(bl, br, tl);
      
      bottomLeft.face = face;
      topRight.face = face;
      
      final Vector2f uv00 = new Vector2f(0.0f, 0.0f);
      final Vector2f uv10 = new Vector2f(1.0f, 0.0f);
      final Vector2f uv01 = new Vector2f(0.0f, 1.0f);
      final Vector2f uv11 = new Vector2f(1.0f, 1.0f);
      
      bottomLeft.setTex(uv01,uv11,uv00);
      topRight.setTex(uv00,uv11,uv10);
   }

   // -----------------------------------------------------------------------
   // New-style Sphere?
   // -----------------------------------------------------------------------

   public static class SphereFaceTriangle extends Mesh.Triangle implements TextureCoordProvider {
      public Vector2f[] latlons;

      public void setLatLons(Vector2f t0, Vector2f t1, Vector2f t2) {
         latlons = new Vector2f[] { t0, t1, t2 };
      }
      @Override
      public Vector2f getTextureCoordinates(int vertexIndex) {
         
         return tex[vertexIndex % 3];
      }
      private static Vector2f latLonToTexCoord(float lat, float lon) {
         while (lon < 0)         lon += (float) (2 * Math.PI);
         while (lon > 2*Math.PI) lon -= (float) (2 * Math.PI);
         float x = (float) (lon / (2.0 * Math.PI));
         float y = 1.0f - (float) (((Math.PI/2.0) + lat) / Math.PI);
         return new Vector2f(x,y);
      }
      
      public SphereFaceTriangle() {
         tex = new Vector2f[] { Vector2f.ORIGIN, Vector2f.ORIGIN, Vector2f.ORIGIN };
      }

      public int face;
      public final Vector2f[] tex;
      
      public CubeFaceTriangle() {
         tex = new Vector2f[] { Vector2f.ORIGIN, Vector2f.ORIGIN, Vector2f.ORIGIN };
      }
      public void setTex(Vector2f t0, Vector2f t1, Vector2f t2) {
         tex[0] = t0; tex[1] = t1; tex[2] = t2;
      }
      @Override
      public Vector2f getTextureCoordinates(int vertexIndex) {
         return tex[vertexIndex % 3];
      }
   }
   
   
   
   
   
   
   
   // -----------------------------------------------------------------------
   // Mesh Structure
   // -----------------------------------------------------------------------
   
   public static class Mesh1 {
  
      // -----------------------------------------------
      // VERTEX
      // -----------------------------------------------
      
      public static class Vertex {
         public Vertex(Vector3f position) {
            this.position = position;
            this.oneOutgoingEdge = null;
         }
         
         // Each Vertex holds a pointer to one outgoing Edge
         public Triangle.Edge getOneOutgoingEdge()       { return oneOutgoingEdge; }
         public void setOneOutgoingEdge(Triangle.Edge e) { oneOutgoingEdge = e;    }
         private Triangle.Edge oneOutgoingEdge;
         
         // 3D Position 
         public Vector3f getPosition()              { return position;          }
         public void setPosition(Vector3f position) { this.position = position; }
         private Vector3f position;

         // Index
         public int getIndex()           { return index;       }
         public void setIndex(int index) { this.index = index; }
         private int index;
         
         // User-specific data
         public Object getData()          { return userdata; }
         public void setData(Object data) { userdata = data; }         
         private Object userdata;
      }

      // -----------------------------------------------
      // TRIANGLE
      // -----------------------------------------------
      
      public static class Triangle {

         public Triangle(Vertex v0, Vertex v1, Vertex v2) {
            // Create three final Edges for this Triangle
            edge0 = new Edge(v0) {
               @Override public int getIndex()           { return 0; }
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge1; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge2; }
            };
            edge1 = new Edge(v1) {
               @Override public int getIndex()           { return 1; }
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge2; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge0; }
            };
            edge2 = new Edge(v2) {
               @Override public int getIndex()           { return 2; }
               @Override public Edge ccwAroundTriangle() { return Triangle.this.edge0; }
               @Override public Edge cwAroundTriangle()  { return Triangle.this.edge1; }
            };
         }
         public Triangle.Edge getEdge(int i) {
            return (i == 0) ? edge0 : (i == 1) ? edge1 : edge2;
         }
         
         // Each Triangle has three Edges:
         public final Triangle.Edge edge0, edge1, edge2;
         
         // Each Edge has methods to get/set two fields: "oppositeVertex" and "oppositeEdge":
         public abstract class Edge {
            private Edge(Vertex oppositeVertex) {
               this.oppositeVertex = oppositeVertex;
               this.oppositeEdge = null;
            }
            public Triangle getTriangle() {
               return Triangle.this;
            }
            
            // Move from this Edge to the next going around this Triangle.
            public abstract int getIndex();
            public abstract Edge ccwAroundTriangle();
            public abstract Edge cwAroundTriangle();
            
            // Each Edge holds a pointer to the Vertex opposite this Edge in this Triangle
            public Vertex getOppositeVertex()       { return oppositeVertex; }
            public void setOppositeVertex(Vertex v) { oppositeVertex = v;    }
            private Vertex oppositeVertex;
            
            // Return the start and end vertices of this Edge
            public Vertex getStart() { return ccwAroundTriangle().getOppositeVertex(); }
            public Vertex getEnd()   { return cwAroundTriangle().getOppositeVertex(); }         
            
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
         public Object getData()          { return userdata; }
         public void setData(Object data) { userdata = data; }         
         private Object userdata;
         
         // Index
         public int getIndex()           { return index;       }
         public void setIndex(int index) { this.index = index; }
         private int index;
      }

      // -----------------------------------------------
      // EDGE  
      //    should we rename Triangle.Edge to Triangle.HalfEdge
      //    and have each Triangle.HalfEdge point to an Edge.Ref?
      // -----------------------------------------------

      public static class Edge {

         public Edge(Triangle.Edge forwardEdge) {
            Triangle.Edge reverseEdge = forwardEdge.getOppositeEdge();
            
            // Create three final Edges for this Triangle
            forward = new Ref() {
               @Override public boolean isForward()             { return true;        }
               @Override public Ref   getReverse()              { return reverse;     }
               @Override public Triangle.Edge getTriangleEdge() { return forwardEdge; }
            };
            reverse = new Ref() {
               @Override public boolean isForward()             { return false;       }
               @Override public Ref   getReverse()              { return forward;     }
               @Override public Triangle.Edge getTriangleEdge() { return reverseEdge; }
            };
         }
         
         // Each Triangle has three Edges:
         public final Ref forward, reverse;
         
         // Each Edge has methods to get/set two fields: "oppositeVertex" and "oppositeEdge":
         public abstract class Ref {
            public Edge getEdge() {
               return Edge.this;
            }
            public abstract boolean       isForward();
            public abstract Ref           getReverse();
            public abstract Triangle.Edge getTriangleEdge();
         }
      }
      
      // --------------------------------------------------------
      // Mesh Constructor
      // --------------------------------------------------------
      
      public Mesh1() {
         boundaryTriangles = new ArrayList<Triangle>();
         interiorTriangles = new ArrayList<Triangle>();
         vertices = new ArrayList<Vertex>();
      }

      public ArrayList<Triangle> boundaryTriangles; 
      public ArrayList<Triangle> interiorTriangles; 
      public ArrayList<Vertex> vertices;
      
      // --------------------------------------------------------
      // Private Add/Remove methods
      // --------------------------------------------------------
      
      private void removeVertex (Vertex vertex) {
         int lastIndex = vertices.size()-1;
         Vertex lastVertex = vertices.get(lastIndex);
         int vertexIndex = vertex.getIndex();
         vertices.set(vertexIndex, lastVertex);
         lastVertex.setIndex(vertexIndex);
         vertices.remove(lastIndex);
      }
      private void addVertex (Vertex vertex) {
         vertex.setIndex(vertices.size());
         vertices.add(vertex);
      }

      private void removeInteriorTriangle (Triangle triangle) {
         int lastIndex = interiorTriangles.size()-1;
         Triangle lastTriangle = interiorTriangles.get(lastIndex);
         int triangleIndex = triangle.getIndex();
         interiorTriangles.set(triangleIndex, lastTriangle);
         lastTriangle.setIndex(triangleIndex);
         interiorTriangles.remove(lastIndex);
      }
      private void addInteriorTriangle (Triangle triangle) {
         triangle.setIndex(interiorTriangles.size());
         interiorTriangles.add(triangle);
      }

      private void removeBoundaryTriangle (Triangle triangle) {
         int lastIndex = boundaryTriangles.size()-1;
         Triangle lastTriangle = boundaryTriangles.get(lastIndex);
         int triangleIndex = triangle.getIndex();
         boundaryTriangles.set(triangleIndex, lastTriangle);
         lastTriangle.setIndex(triangleIndex);
         boundaryTriangles.remove(lastIndex);
      }
      private void addBoundaryTriangle (Triangle triangle) {
         triangle.setIndex(boundaryTriangles.size());
         boundaryTriangles.add(triangle);
      }
      
      // --------------------------------------------------------
      // AddVertex
      // --------------------------------------------------------
      
      public Vertex addVertex (Vector3f position) {
         Vertex vertex = new Vertex(position);
         addVertex(vertex);
         return vertex;
      }
      
      // --------------------------------------------------------
      // AddTriangle
      // --------------------------------------------------------
      
      public Triangle addTriangle (Vertex v0, Vertex v1, Vertex v2) {
          check((v0 != null) && (v1 != null) && (v2 != null) &&
               (v1 != v0) && (v2 != v0) && (v2 != v1), "Vertices should be all different");
         
          Triangle t = new Triangle(v0,v1,v2);
          addInteriorTriangle(t);
          
          // Set the "opposite-edge" pointers in the new triangle.
          for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             
             Vertex start = ei.getStart();
             Vertex end = ei.getEnd();
             
             // For each edge in the NEW TRIANGLE, we want to know if the edge is
             // being placed alongside an existing triangle.   If it is, a boundary
             // edge will exist going from start to end:
             
             Triangle.Edge existingStartToEndBoundaryEdge = null;

             // Walk around the edges connected to the "start" vertex to see if
             // such an edge exists:
             Triangle.Edge firstOutFromStart = start.getOneOutgoingEdge();
             if (firstOutFromStart != null) {
                
                // The start vertex already has edges attached to it.
                // It must be on the boundary then...
                Triangle.Edge outFromStart = firstOutFromStart;
                boolean onBoundary = false;
                do {
                   boolean isBoundary = outFromStart.getTriangle().isBoundary();
                   if (isBoundary) onBoundary = true;
                   
                   if (outFromStart.getEnd() == end) {
                      // We've found a pre-existing edge from Start -> End.
                      // It better be a boundry edge, otherwise we have an ERROR,
                      // since the edge we're trying to add apperently already exists.
                      check(isBoundary, "Attached edge must be a boundary edge");
                      
                      existingStartToEndBoundaryEdge = outFromStart;
                      break;
                   }
                   outFromStart = outFromStart.ccwAroundStart();
                } while (outFromStart != firstOutFromStart);
             
                check(onBoundary, "Attached vertex must be a boundary vertex");
             }
             
             // If the existingStartToEndBoundaryEdge exists, then its "opposite-edge"
             // points to an interior triangle edge that is going to be opposite.
             if (existingStartToEndBoundaryEdge != null) {
                ei.setOppositeEdge(existingStartToEndBoundaryEdge.getOppositeEdge());
                
             } else {
                // On the other hand, if the NEW TRIANGLE edge is not alongside an existing
                // triangle, then we'll have to create a new "BOUNDARY TRIANGLE" and point
                // the "opposite-edge" pointer to that instead.
             
                // CREATE NEW BOUNDARY TRIANGLES HERE
                Triangle b = new Triangle(end, start, null);
                addBoundaryTriangle(b);
                
                ei.setOppositeEdge(b.edge2);
             }
          }
          
          // Now let's consider each VERTEX in turn
          for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             
             // Consider a VERTEX v (== ei.getOppositeVertex()).
             Vertex v = ei.getOppositeVertex();
             
             // When passing this vertex going CCW around the TRIANGLE to be DELETED, 
             //    we encounter edges: prevEdge -> v -> nextEdge
             Triangle.Edge prevEdge = ei.ccwAroundTriangle();  // (points towards v)
             Triangle.Edge nextEdge = ei.cwAroundTriangle();   // (points away from v)
             
             // The "opposite" pointers in these Edges were set above, either to NEW boundary
             // triangles (if unattached), or existing internal triangles (if attached):          
             Triangle.Edge oppositePrevEdge = prevEdge.getOppositeEdge();  // (points away from v)
             Triangle.Edge oppositeNextEdge = nextEdge.getOppositeEdge();  // (points towards v)             
             boolean prevEdgeAttached = !oppositePrevEdge.getTriangle().isBoundary();
             boolean nextEdgeAttached = !oppositeNextEdge.getTriangle().isBoundary();
             
             // There a 4 cases based on whether the prev and next edges are attached:
             if (!prevEdgeAttached && !nextEdgeAttached) {
                // CASE 1. Link both "unattached" boundary triangles.
                
                Triangle.Edge newBoundaryPrev = prevEdge.ccwAroundEnd();    // (points towards v)
                Triangle.Edge newBoundaryNext  = nextEdge.cwAroundStart();  // (points away from v)

                // Does v have ANY existing edges?
                Triangle.Edge firstOutFromV = v.getOneOutgoingEdge();
                if (firstOutFromV == null) {      
                   // v has NO existing edges, it's a NEW vertex just for this Triangle.
                   // Connect the boundary triangles to each other:
                   linkOpposingEdges(newBoundaryPrev, newBoundaryNext);
                   
                   // Since v is a NEW vertex, we're the first one to set its "one-outgoing-edge"
                   v.setOneOutgoingEdge(nextEdge);
                   
                } else {
                   // V does have existing edges.  We know it's on the boundary, so there
                   // must be two consecutive boundary triangles attached to V.  Find them:
                   
                   Triangle.Edge outFromV = firstOutFromV;
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
                   
                   Triangle.Edge inToV = outFromV.getOppositeEdge();
                   linkOpposingEdges(newBoundaryNext, inToV);
                   linkOpposingEdges(newBoundaryPrev, outFromV);
                }
                
             } else if (prevEdgeAttached && !nextEdgeAttached) {
                // CASE 2. Link the "unattached" boundary triangle that's opposite "nextEdge":
                
                Triangle.Edge newBoundaryNext = nextEdge.cwAroundStart();          // (points away from v)
                Triangle.Edge oldBoundaryPrev = oppositePrevEdge.cwAroundStart();  // (points away from v)
                linkOpposingEdges(newBoundaryNext, oldBoundaryPrev.getOppositeEdge());

             } else if (!prevEdgeAttached && nextEdgeAttached) {
                // CASE 3. Link the "unattached" boundary triangle that's opposite "prevEdge":
                
                Triangle.Edge newBoundaryPrev = prevEdge.ccwAroundEnd();           // (points toward v)
                Triangle.Edge oldBoundaryNext = oppositeNextEdge.ccwAroundEnd();   // (points toward v)
                linkOpposingEdges(newBoundaryPrev, oldBoundaryNext.getOppositeEdge());
                
             } else {
                // CASE 4. BOTH edges are attached.  We need the old boundaries to be adjacent:
                
                Triangle.Edge oldBoundaryPrev = oppositePrevEdge.cwAroundStart();  // (points away from v)
                Triangle.Edge oldBoundaryNext = oppositeNextEdge.ccwAroundEnd();   // (points toward v)    
                if (oldBoundaryPrev.getOppositeEdge() != oldBoundaryNext) {
                   
                   // The old boundaries are not adjacent.  However, we can fix this, there's no topology problem,
                   // so long as the vertex v also has another two consecutive boundary triangles.
                   
                   Triangle.Edge outFromV = oppositePrevEdge.ccwAroundStart();
                   Triangle.Edge outFromVEnd = nextEdge.cwAroundStart();
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
                   
                   Triangle.Edge inToV = outFromV.getOppositeEdge();
                   linkOpposingEdges(inToV, oldBoundaryNext.getOppositeEdge());
                   linkOpposingEdges(outFromV, oldBoundaryPrev.getOppositeEdge());
                }
             }
          }

          // Finally we connect the backlinks from each OPPOSITE-EDGE pointer in the Triangle
          for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
             Triangle.Edge oppositeEi = ei.getOppositeEdge();
             boolean isAttached = !oppositeEi.getTriangle().isBoundary();
             
             if (isAttached) {
                Triangle b = oppositeEi.getOppositeEdge().getTriangle();
                removeBoundaryTriangle(b);
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
         
         // First, we consider each each VERTEX in turn
         // and make sure it's not pointing to our edges:
         for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
            Vertex v = ei.getOppositeVertex();
            Triangle.Edge nextEdge = ei.cwAroundTriangle();   // (points away from v)
            
            if (v.getOneOutgoingEdge() == nextEdge) {
               
               Triangle.Edge newOutgoingEdgeForV = null;
               
               // We're going to change v's "out-outgoing-edge" so it points to some OTHER
               // internal triangle that's not 't':
               
               Triangle.Edge firstOutFromStart = v.getOneOutgoingEdge();
               Triangle.Edge outFromStart = firstOutFromStart;
               while(true) {
                  outFromStart = outFromStart.ccwAroundStart();
                  
                  if (outFromStart == firstOutFromStart) {
                     break;
                  }
                  boolean isBoundary = outFromStart.getTriangle().isBoundary();
                  if (!isBoundary) {
                     newOutgoingEdgeForV = outFromStart;
                     break;
                  }
               }

               v.setOneOutgoingEdge(newOutgoingEdgeForV);
            }
         }
         
         // Now for each edge of the TRIANGLE to be DELETED, if the edge is connected to
         // another interior triangle, that edge is going to be a new boundary,
         // so we CREATE a "boundary-triangle" for it.
         
         for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
            Triangle.Edge oppositeEi = ei.getOppositeEdge();
            boolean isAttached = !oppositeEi.getTriangle().isBoundary();
            
            if (isAttached) {
               // CREATE NEW BOUNDARY TRIANGLES HERE
               Triangle b = new Triangle(ei.getStart(), ei.getEnd(), null);
               addBoundaryTriangle(b);
               
               linkOpposingEdges(oppositeEi, b.edge2);
            }
         }
         
         // Now let's consider each VERTEX in turn
         for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
            
            // Consider a VERTEX v (== ei.getOppositeVertex()).
            // When passing this vertex going CCW around the TRIANGLE to be DELETED, 
            //    we encounter edges: prevEdge -> v -> nextEdge
            Triangle.Edge prevEdge = ei.ccwAroundTriangle();  // (points towards v)
            Triangle.Edge nextEdge = ei.cwAroundTriangle();   // (points away from v)
          
            // The "opposite" pointers in these Edges point either to existing triangles
            // which now point to NEW boundary triangles, or they point to OLD boundary triangles
            // which will be deleted below.
            
            Triangle.Edge oppositePrevEdge = prevEdge.getOppositeEdge();  // (points away from v)
            Triangle.Edge oppositeNextEdge = nextEdge.getOppositeEdge();  // (points towards v)             
            boolean prevEdgeAttached = !oppositePrevEdge.getTriangle().isBoundary();
            boolean nextEdgeAttached = !oppositeNextEdge.getTriangle().isBoundary();
            
            // There a 4 cases based on whether the prev and next edges are attached:
            if (!prevEdgeAttached && !nextEdgeAttached) {
               // CASE 1. Link both "unattached" boundary triangles.
               
               // Neither prevEdge nor nextEdge were "attached", they're both boundary edges.
               // So going clockwise around v, we encounter 
               //   (1) the boundary triangle containing "oppositePrevEdge" (pointing away from v)
               //   (2) the triangle-to-be-deleted t, containing "nextEdge" (pointing away from v)
               //                                            and "prevEdge" (pointing toward v)
               //   (3) the boundary triangle containing "oppositeNextEdge" (pointing toward v)
               
               // We're deleting ALL THREE of these triangles from the list of triangles connected to 'v'.
               
               // If v has no other interior triangles attached, then the other edge in (1)
               //        (oppositePrevEdge.cwAroundTriangle()), (pointing toward v)
               // will be adjacant and opposite to the other edge in (3)
               //        (oppositeNextEdge.ccwAroundTriangle()), (pointing away from v)
               
               Triangle.Edge ccwFromPrevEdge = oppositePrevEdge.cwAroundTriangle();  // (points towards v)
               Triangle.Edge cwFromNextEdge = oppositeNextEdge.ccwAroundTriangle();  // (points away from v)             
               
               if (ccwFromPrevEdge.getOppositeEdge() != cwFromNextEdge) {
                  
                  // This means v has other interior triangles attached, in other words it's a point
                  // where two or more boundary curves touch at a single vertex.   By connecting the
                  // triangle cw of (3) with the triangle ccw of (1), we've snipped out 3 triangles..
                  
                  linkOpposingEdges(ccwFromPrevEdge.getOppositeEdge(), cwFromNextEdge.getOppositeEdge());
               }
               
            } else if (prevEdgeAttached && !nextEdgeAttached) {
               // CASE 2. Link the "unattached" boundary triangle that's opposite "nextEdge":

               // So going clockwise around v, we encounter 
               //   (1) the interior triangle containing "oppositePrevEdge" (pointing away from v)
               //   (2) the triangle-to-be-deleted t, containing "nextEdge" (pointing away from v)
               //                                            and "prevEdge" (pointing toward v)
               //   (3) the boundary triangle containing "oppositeNextEdge" (pointing toward v)

               // We're deleting (2) and (3) and replacing them with a new BOUNDARY TRIANGLE,
               // and we've already linked the cw edge of (1) to this new BOUNDARY TRIANGLE,
               
               Triangle.Edge newPrevEdge = oppositePrevEdge.getOppositeEdge();  // (points towards v)
               Triangle.Edge cwFromNextEdge = oppositeNextEdge.ccwAroundTriangle();  // (points away from v)             
               linkOpposingEdges(newPrevEdge.ccwAroundTriangle(), cwFromNextEdge.getOppositeEdge());
               
            } else if (!prevEdgeAttached && nextEdgeAttached) {
               // CASE 3. Link the "unattached" boundary triangle that's opposite "prevEdge":

               // So going clockwise around v, we encounter 
               //   (1) the boundary triangle containing "oppositePrevEdge" (pointing away from v)
               //   (2) the triangle-to-be-deleted t, containing "nextEdge" (pointing away from v)
               //                                            and "prevEdge" (pointing toward v)
               //   (3) the interior triangle containing "oppositeNextEdge" (pointing toward v)

               // We're deleting (2) and (1) and replacing them with a new BOUNDARY TRIANGLE,
               // and we've already linked the ccw edge of (3) to this new BOUNDARY TRIANGLE,
               
               Triangle.Edge ccwFromPrevEdge = oppositePrevEdge.cwAroundTriangle();  // (points towards v)
               Triangle.Edge newNextEdge = oppositeNextEdge.getOppositeEdge();  // (points away from v)
               linkOpposingEdges(ccwFromPrevEdge.getOppositeEdge(), newNextEdge.cwAroundTriangle());

            } else {
               // CASE 4. BOTH edges are attached.
               
               // Both prevEdge nor nextEdge were "attached" to interior triangles
               // So going clockwise around v, we encounter 
               //   (1) the interior triangle containing "oppositePrevEdge" (pointing away from v)
               //   (2) the triangle-to-be-deleted t, containing "nextEdge" (pointing away from v)
               //                                            and "prevEdge" (pointing toward v)
               //   (3) the interior triangle containing "oppositeNextEdge" (pointing toward v)
               
               // We're replacing the middle triangle (2) with TWO new BOUNDARY TRIANGLES...
               // and we've already linked the cw edge of (1) to a new BOUNDARY TRIANGLE,
               // and we've already linked the ccw edge of (3) to a new BOUNDARY TRIANGLE,
               
               Triangle.Edge newPrevEdge = oppositePrevEdge.getOppositeEdge();  // (points towards v)
               Triangle.Edge newNextEdge = oppositeNextEdge.getOppositeEdge();  // (points away from v)
               
               linkOpposingEdges(newPrevEdge.ccwAroundTriangle(), newNextEdge.cwAroundTriangle());
            }
         }

         // Finally we delete OLD boundary edges connected to this triangle
         for (Triangle.Edge ei : Arrays.asList(t.edge0, t.edge1, t.edge2)) {
            Triangle.Edge oppositeEi = ei.getOppositeEdge();
            boolean isAttached = !oppositeEi.getTriangle().isBoundary();
            
            if (!isAttached) {
               // DELETE OLD BOUNDARY TRIANGLES HERE
               Triangle b = oppositeEi.getTriangle();
               removeBoundaryTriangle(b);
            }
            
            ei.setOppositeEdge(null);
         }
         
         // The triangle t is now completely disconnected..
         // adjacent BOUNDARY TRIANGLES were deleted above,
         // adjacent interior triangles are properly connected to new BOUNDARY TRIANGLES,
         // and none of the three vertices refer to us.
         removeInteriorTriangle(t);
      }
      
      // --------------------------------------------------------
      // Mesh Assembly
      // --------------------------------------------------------
      
      private static  void linkOpposingEdges(Triangle.Edge a, Triangle.Edge b) {
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
      // Validation -- is this mesh actually hooked up right?
      // --------------------------------------------------------

      public void checkEdge(Triangle.Edge e, HashSet<Vertex> verticesReferenced) {
         check(e.oppositeEdge != null, "Edge.oppositeEdge is set");
         check(e != e.oppositeEdge, "Edge.oppositeEdge is different from this Edge");
         check(e == e.oppositeEdge.oppositeEdge, "oppositeEdge points back to us");
         check(e.getStart() == e.oppositeEdge.getEnd(), "start == oppositeEdge.end");
         check(e.getEnd() == e.oppositeEdge.getStart(), "end == oppositeEdge.start");
         
         Triangle opposingTriangle = e.oppositeEdge.getTriangle();
         check(e.getTriangle() != opposingTriangle, "oppositeEdge is in a different Triangle");
         if (opposingTriangle.isBoundary()) {
            check(boundaryTriangles.contains(opposingTriangle), "oppositeTriangle is part of our boundary list");
         } else {
            check(interiorTriangles.contains(opposingTriangle), "oppositeTriangle is part of our interior list");
         }
         Vertex opposingVertex = e.oppositeVertex;
         if (opposingVertex != null) {
            verticesReferenced.add(opposingVertex);
            check(vertices.contains(opposingVertex), "oppositeVertex is part of our vertex list");
         }
      }
      public void checkTriangle(Triangle t, boolean isBoundary, HashSet<Vertex> verticesReferenced) {
         checkEdge(t.edge0, verticesReferenced);
         checkEdge(t.edge1, verticesReferenced);
         checkEdge(t.edge2, verticesReferenced);
         check(t.edge0.oppositeVertex != null, "vertex0 not null");
         check(t.edge1.oppositeVertex != null, "vertex1 not null");
         check(t.isBoundary() == isBoundary, "vertex2 is null if boundary and not otherwise");
         check((t.edge0.oppositeVertex != t.edge1.oppositeVertex) &&
               (t.edge0.oppositeVertex != t.edge2.oppositeVertex) &&
               (t.edge1.oppositeVertex != t.edge2.oppositeVertex), "vertices are all different");
      }
      public void checkVertex(Vertex v, HashSet<Vertex> verticesReferenced) {
         if (verticesReferenced.contains(v)) {
            check(v.oneOutgoingEdge != null, "Vertex.getOneOutgoingEdge should be non-null but is null");
            Triangle.Edge e = v.oneOutgoingEdge;
            while (true) {
               check (v == e.getStart(), "Edge is outgoing from this Vertex");
               e = e.ccwAroundStart();
               if (e == v.oneOutgoingEdge) break;
            }
         } else {
            check(v.oneOutgoingEdge == null, "Vertex.getOneOutgoingEdge should be null but is non null");
         }
      }
      public void checkMesh() {
         System.out.format("Mesh checked... %d interior triangles, %d boundary triangles, %d vertices\n",
               interiorTriangles.size(), boundaryTriangles.size(), vertices.size());
         
         HashSet<Vertex> verticesReferenced = new HashSet<Vertex>();
         for (Triangle t : interiorTriangles) checkTriangle(t, false, verticesReferenced);
         for (Triangle t : boundaryTriangles) checkTriangle(t, true, verticesReferenced);
         for (Vertex v : vertices) checkVertex(v, verticesReferenced);
      }

      
      public void testAddAndDelete() {
         class TriangleRecord {
            public final Vertex v0,v1,v2;
            public Triangle t;
            public Object tData;
            public TriangleRecord(Triangle t) {
               this.v0 = t.edge0.oppositeVertex; 
               this.v1 = t.edge1.oppositeVertex; 
               this.v2 = t.edge2.oppositeVertex; 
               this.t = t;
               tData = t.getData();
            }
         }
         
         System.out.format("Beginning sequence of random deletions and additions!\n\n");
         
         int numTriangles = interiorTriangles.size();
         TriangleRecord[] triangles = new TriangleRecord[numTriangles];
         int i = 0;
         for (Triangle t : interiorTriangles) {
            triangles[i++] = new TriangleRecord(t);
         }
         
         int trials = 100;
         
         for (int j = 0; j < trials; ++j) {
            int triangleToAffect = (int)(Math.random() * numTriangles);
            TriangleRecord record = triangles[triangleToAffect];
            
            if (record.t != null) {
               removeTriangle(record.t);
               record.t = null;
            } else {
               record.t = addTriangle(record.v0,record.v1,record.v2);
               record.t.setData(record.tData);
            }
            checkMesh();
         }
         
         for (int j = 0; j < numTriangles; ++j) {
            TriangleRecord record = triangles[j];
            if (record.t != null) {
               removeTriangle(record.t);
               record.t = null;
               checkMesh();
            }
         }
         
         for (int j = 0; j < trials; ++j) {
            int triangleToAffect = (int)(Math.random() * numTriangles);
            TriangleRecord record = triangles[triangleToAffect];
            
            if (record.t != null) {
               removeTriangle(record.t);
               record.t = null;
            } else {
               record.t = addTriangle(record.v0,record.v1,record.v2);
               record.t.setData(record.tData);
            }
            checkMesh();
         }
         
         System.out.format("DONE... putting back any remaining deleted triangles...\n\n");
         
         for (int j = 0; j < numTriangles; ++j) {
            TriangleRecord record = triangles[j];
            if (record.t == null) {
               record.t = addTriangle(record.v0,record.v1,record.v2);
               record.t.setData(record.tData);
               checkMesh();
            }
         }
         System.out.format("Repaired...\n\n");
      }
   }
   
   public static class MeshModel {
      public final String name;
      public final Mesh1 mesh;
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
         this(name, new Mesh1());
      }
      public MeshModel(String name, Mesh1 mesh) { 
         this.name = name;
         this.mesh = mesh;
         
         for (Mesh1.Vertex v : mesh.vertices) {
            updateMaxRadius(v.getPosition().length());
         }
         
         buffers = new HashMap<String,Shader.ManagedBuffer>();
         setManagedBuffer(Shader.POSITION_ARRAY, defaultPositionBuffer(mesh), name);
         setManagedBuffer(Shader.BARY_COORDS,    defaultBaryCoords(mesh), name);
         setManagedBuffer(Shader.COLOR_ARRAY,    defaultColorArray(mesh), name);
         
         setManagedBuffer(Shader.V0POS_ARRAY,    defaultPosArray(mesh,0), name);
         setManagedBuffer(Shader.V2POS_ARRAY,    defaultPosArray(mesh,1), name);
         setManagedBuffer(Shader.V1POS_ARRAY,    defaultPosArray(mesh,2), name);
      }
      
      // ------------------------------------------------------------------------
      // Building Models
      // ------------------------------------------------------------------------
      
      public Mesh1.Vertex getOrAddVertex(Vector3f position) {         
         // Search to see if we already have a Vertex at this position
         // TODO:  Use a 3D index for this...
         for (Mesh1.Vertex v : mesh.vertices) {
            Vector3f vPosition = v.getPosition();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         
         // Create a new vertex
         Mesh1.Vertex v = mesh.addVertex(position);
         updateMaxRadius(position.length());
         return v;
      }
      
      public void updateMaxRadius(float radius) {
         if (radius > maxRadius) maxRadius = radius;
      }
      
      public void addTriangle (Vector3f a, Vector3f b, Vector3f c, Object ti) {
         Mesh1.Vertex va = getOrAddVertex(a);
         Mesh1.Vertex vb = getOrAddVertex(b);
         Mesh1.Vertex vc = getOrAddVertex(c);
         addTriangle(va,vb,vc,ti);
      }
      public void addTriangle (Mesh1.Vertex va, Mesh1.Vertex vb, Mesh1.Vertex vc, Object ti) {
         Mesh1.Triangle t = mesh.addTriangle(va, vb, vc);
         t.setData(ti);
      }
      
      // ------------------------------------------------------------------------
      // Map of Managed Buffers...
      // ------------------------------------------------------------------------

      public Shader.ManagedBuffer getManagedBuffer(String key) {
         return buffers.get(key);
      }
      public void setManagedBuffer(String key, Shader.ManagedBuffer buffer) {
         buffer.name = key;
         buffers.put(key, buffer);
      }
      public void setManagedBuffer(String key, Shader.ManagedBuffer buffer, String name2) {
         buffer.name = key + ":" + name2;
         buffers.put(key, buffer);
      }
      private HashMap<String,Shader.ManagedBuffer> buffers;

   }
   
   // -------------------------------------------------------------------------
   // Basic Array Builders for Geometry / BaryCoords
   //    .. and an "empty" one for Colors
   // -------------------------------------------------------------------------
   
   private static Shader.ManagedBuffer defaultPositionBuffer(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               Vector3f v0Pos = t.edge0.getOppositeVertex().getPosition();
               Vector3f v1Pos = t.edge1.getOppositeVertex().getPosition();
               Vector3f v2Pos = t.edge2.getOppositeVertex().getPosition();
               pPos = Vector4f.fromVector3f(v0Pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(v1Pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(v2Pos).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   private static Shader.ManagedBuffer defaultPosArray(final Mesh1 mesh, final int index) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               Vector3f pos = ((index==0)?t.edge0:(index==2)?t.edge1:t.edge2).getOppositeVertex().getPosition();
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4f.fromVector3f(pos).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   private static Shader.ManagedBuffer defaultBaryCoords(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            float root3 = (float) Math.sqrt(3);
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
//               pPos = (new Vector2f(0.0f,0.0f)).copyToFloatArray(array,  pPos);
//               pPos = (new Vector2f(1.0f,0.0f)).copyToFloatArray(array,  pPos);
//               pPos = (new Vector2f(0.5f,root3 * 0.5f)).copyToFloatArray(array,  pPos);

               pPos = (new Vector3f(1.0f, 0.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3f(0.0f, 1.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3f(0.0f, 0.0f, 1.0f)).copyToFloatArray(array, pPos);
            }
         }
      };
   }

   private static Shader.ManagedBuffer defaultColorArray(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            int col = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               
               ColorARGB color = (col==0) ? new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xff, (byte)0x80) :
                  (col==1) ? new ColorARGB((byte)0x00, (byte)0xc0, (byte)0xd0, (byte)0xb0) :
                  (col==2) ? new ColorARGB((byte)0x00, (byte)0x80, (byte)0xf0, (byte)0xd0) :
                             new ColorARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
                  
                  color = new ColorARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
                  
/*               
               ColorARGB color = (col==0) ? new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xc0, (byte)0xf0) :
                                 (col==1) ? new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xc0, (byte)0xf0) :
                                 (col==2) ? new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xc0, (byte)0xf0) :
                                            new ColorARGB((byte)0x00, (byte)0xb0, (byte)0xc0, (byte)0xf0);
                                            */
               col = (col+1)%4;
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, ColorARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             return base+3;
         }
      };
   }   
   
   // -----------------------------------------------------------------------
   // CUBE
   // -----------------------------------------------------------------------
   
   public static MeshModel createUnitCube () {
      final MeshModel m = new MeshModel("UnitCube");
      
      m.setManagedBuffer(Shader.TEX_COORDS, cubeTextureCoordsArray(m.mesh), "UnitCubeManual");
      
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

   private static Shader.ManagedBuffer cubeTextureCoordsArray(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
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
      m.setManagedBuffer(Shader.TEX_COORDS, sphereTextureCoordsArray(m.mesh), "UnitSphereManual");

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

   private static Shader.ManagedBuffer sphereTextureCoordsArray(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
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
      m.setManagedBuffer(Shader.TEX_COORDS, sphereTextureCoordsArray(m.mesh), "UnitIcoManual");

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
   // BEZIER / CYLINDER ...
   //
   // I wish we had robust intersect / union,
   //   but isn't that extremely hard due to the inevitable degeneracies?
   // 
   // How about some kind of marching-cubes thing that walk over
   //   a potential field with a level constant?
   // -----------------------------------------------------------------------

   // -----------------------------------------------------------------------
   // Tube
   // -----------------------------------------------------------------------

   private static Vector3f leastDimension(Vector3f a) {
      Vector3f result = Vector3f.X;
      if (a.y < a.x) result = Vector3f.Y;
      if ((a.z < a.y) && (a.z < a.x)) result = Vector3f.Z;
      return result;
   }
   public static MeshModel createCylinder(Vector3f start, Vector3f end, float radius1, float radius2, int numDivisions) {
      MeshModel m = new MeshModel(String.format("Tube"));

      Vector3f discFwd = end.minus(start).normalized();
      Vector3f discX = leastDimension(discFwd);
      Vector3f discY = discX.cross(discFwd);
      
      Mesh1.Vertex startVertex = m.mesh.addVertex(start);
      Mesh1.Vertex endVertex = m.mesh.addVertex(end);
      
      ArrayList<Mesh1.Vertex> startVertices = new ArrayList<Mesh1.Vertex>();
      ArrayList<Mesh1.Vertex> endVertices = new ArrayList<Mesh1.Vertex>();
      for (int i = 0; i < numDivisions; ++i) {
         float angle = (float)(Math.PI * 2 * i / numDivisions);
         Vector3f delta = discX.times((float)Math.cos(angle))
                    .plus(discY.times((float)Math.sin(angle)));
         
         Mesh1.Vertex v;
         
         v = m.mesh.addVertex(start.plus(delta));
         startVertices.add(v);
         
         v = m.mesh.addVertex(end.plus(delta));
         endVertices.add(v);
      }

      for (int i = 0; i < numDivisions; ++i) {
         Mesh1.Vertex sv1 = startVertices.get(i);
         Mesh1.Vertex sv0 = startVertices.get((i + numDivisions - 1) % numDivisions);
         
         Mesh1.Vertex ev1 = endVertices.get(i);
         Mesh1.Vertex ev0 = endVertices.get((i + numDivisions - 1) % numDivisions);
         
         m.mesh.addTriangle(startVertex, sv0, sv1);
         m.mesh.addTriangle(sv1, sv0, ev1);
         m.mesh.addTriangle(ev1, sv0, ev0);
         m.mesh.addTriangle(ev1, ev0, endVertex);
      }
      
      return m;
   }
   
   // -----------------------------------------------------------------------
   // Cube-Chopped
   // -----------------------------------------------------------------------

   public static MeshModel createChoppedCube() {
      MeshModel m = new MeshModel("ChoopedCube");
      
      Vector3f v000 = new Vector3f(0.0f,0.0f,0.0f);
      Vector3f v001 = new Vector3f(0.0f,0.0f,1.0f);
      Vector3f v010 = new Vector3f(0.0f,1.0f,0.0f);
      Vector3f v011 = new Vector3f(0.0f,1.0f,1.0f);
      Vector3f v100 = new Vector3f(1.0f,0.0f,0.0f);
      Vector3f v101 = new Vector3f(1.0f,0.0f,1.0f);
      Vector3f v110 = new Vector3f(1.0f,1.0f,0.0f);
      Vector3f v111 = new Vector3f(1.0f,1.0f,1.0f);
      
      addTetrahedron(m, 0, v010,v000,v111,v110);
      addTetrahedron(m, 1, v011,v000,v111,v010);
      addTetrahedron(m, 2, v001,v000,v111,v011);
      addTetrahedron(m, 3, v101,v000,v111,v001);
      addTetrahedron(m, 4, v100,v000,v111,v101);
      addTetrahedron(m, 5, v110,v000,v111,v100);
      
      Vector3f axis = new Vector3f(1.0f,-1.0f,0.0f).normalized();
      float angle = (float) Math.atan(Math.sqrt(2.0));
      
      for (Mesh1.Vertex v : m.mesh.vertices) {
         Vector3f p = v.getPosition();
         p = p.rotated(axis, angle);
         
         TetrahedronCount tc = (TetrahedronCount) v.getData();
         tc.base = tc.base.rotated(axis, angle);
         tc.offset = new Vector3f(tc.offset.x, tc.offset.y, 0.0f);
         
         p = new Vector3f(p.x,p.y,p.z/2);
         if (tc.id == 5) {
            System.out.format("VERTEX: \n%s\n", p);
         }
         tc.base = new Vector3f(tc.base.x, tc.base.y, tc.base.z/2);
         
         v.setPosition(p);
      }
         
      return m;
   }

   private static class TetrahedronCount {
      public int id;
      public Vector3f base;
      public Vector3f offset;
      TetrahedronCount (int id, Vector3f base, Vector3f offset) {
         this.id = id;
         this.base = base;
         this.offset = offset;
      }
   }
   
   public static void addTetrahedron(MeshModel m, int id, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
      Vector3f offset = (a.plus(d).times(0.5f)).minus(b.plus(c).times(0.5f)).normalized();
      TetrahedronCount ti = new TetrahedronCount(id, a, offset);
      
      Mesh1.Vertex va = m.mesh.addVertex(a); va.setData(new TetrahedronCount(id, a, offset));
      Mesh1.Vertex vb = m.mesh.addVertex(b); vb.setData(new TetrahedronCount(id, b, offset));
      Mesh1.Vertex vc = m.mesh.addVertex(c); vc.setData(new TetrahedronCount(id, c, offset));
      Mesh1.Vertex vd = m.mesh.addVertex(d); vd.setData(new TetrahedronCount(id, d, offset));
    
      m.addTriangle(va, vb, vc, ti);
      m.addTriangle(vd, vc, vb, ti);
      m.addTriangle(vc, vd, va, ti);
      m.addTriangle(vd, vb, va, ti);
   }

   public static void warpChoppedCube (MeshModel model, float phase, float mag) {
      for (Mesh1.Vertex v : model.mesh.vertices) {
         TetrahedronCount tc = (TetrahedronCount) v.getData();
         v.setPosition(tc.base.plus(tc.offset.times((1.0f - (float) Math.cos(phase)) * mag)));
      }
      model.getManagedBuffer(Shader.POSITION_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V0POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V1POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V2POS_ARRAY).setModified(true);      
   }
   
   
   
   //public static MeshModel createTube(ArrayList<Vector3f> centers, float radius1, float radius2, int numDivisions) {
   //   MeshModel m = new MeshModel(String.format("Tube"));
   // }
   

   // -----------------------------------------------------------------------
   // We keep thinking that we might want to attach additional types of data
   // to each triangle or each vertex .. or each edge.   Each triangle or vertex
   // has a getData/setData pair, but we could have a HashMap<Triangle,?> or
   // a HashMap<Vertex,?> instead...
   // -----------------------------------------------------------------------

   public static void everyPointGetsAnInteger (MeshModel model, int largestVertexInt) {
      for (Mesh1.Vertex v : model.mesh.vertices) {
         int vertexInt = (int)(largestVertexInt * Math.random());
         v.setData(Integer.valueOf(vertexInt));
      }
      model.setManagedBuffer(Shader.COLOR_ARRAY, pointShadingColorArray(model.mesh));
   }
   
   private static Shader.ManagedBuffer pointShadingColorArray(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               int v0Int = (Integer) t.edge0.getOppositeVertex().getData();
               int v1Int = (Integer) t.edge1.getOppositeVertex().getData();
               int v2Int = (Integer) t.edge2.getOppositeVertex().getData();
               
               ColorARGB color = new ColorARGB((byte)0x00, (byte)v0Int, (byte)v1Int, (byte)v2Int);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, ColorARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             return base+3;
         }
      };
   }

   //==================================================
   
   

   public static class FlatFaceInfo {
      public final Vector2f tex0,tex1,tex2;
      public FlatFaceInfo(Vector2f tex0, Vector2f tex1, Vector2f tex2) {
         this.tex0 = tex0; this.tex1 = tex1; this.tex2 = tex2;
      }
   }

   private static Shader.ManagedBuffer perTriangleTexCoords(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(4) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               FlatFaceInfo faceInfo = (FlatFaceInfo) t.getData();
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
   private static Shader.ManagedBuffer defaultUvArray(final Mesh1 mesh, final int index) {
      return new Shader.ManagedBuffer(2) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               FlatFaceInfo faceInfo = (FlatFaceInfo) t.getData();
               Vector2f pos = ((index==0)?faceInfo.tex0:(index==1)?faceInfo.tex1:faceInfo.tex2);
               pPos = pos.copyToFloatArray(array, pPos);
               pPos = pos.copyToFloatArray(array, pPos);
               pPos = pos.copyToFloatArray(array, pPos);
            }
         }
      };
   }


   private static Shader.ManagedBuffer directionArray(final Vector3f[] directionPerTriangle, final Mesh1 mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               Vector3f v = directionPerTriangle[t.getIndex()];
//               pPos = (new Vector2f(0.0f,0.0f)).copyToFloatArray(array,  pPos);
//               pPos = (new Vector2f(1.0f,0.0f)).copyToFloatArray(array,  pPos);
//               pPos = (new Vector2f(0.5f,root3 * 0.5f)).copyToFloatArray(array,  pPos);

               pPos = (v).copyToFloatArray(array, pPos);
               pPos = (v).copyToFloatArray(array, pPos);
               pPos = (v).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   
   public static void everyTriangleGetsManualMapping (MeshModel model, float xOffset, int div) {
      Vector2f p0 = new Vector2f(0.0f, 0.0f);
      Vector2f p1 = new Vector2f(1.0f, 0.0f);
      Vector2f p2 = new Vector2f(0.5f, 0.866f);
      float margin = 0.1f;
      
      int tCount = 0;
      for (Mesh1.Triangle t : model.mesh.interiorTriangles) {
         int yCount = tCount/div;
         int xCount = tCount - (yCount*div);
         Vector2f base = new Vector2f(xOffset + margin + (1.0f+margin) * xCount, margin + (1.0f+margin) * yCount); 
               
         FlatFaceInfo fi = new FlatFaceInfo(base.plus(p0),base.plus(p1),base.plus(p2));
         t.setData(fi);
         tCount++;
      }
      model.setManagedBuffer(Shader.TEX_COORDS, perTriangleTexCoords(model.mesh));
      model.setManagedBuffer(Shader.V0UV_ARRAY, defaultUvArray(model.mesh,0));
      model.setManagedBuffer(Shader.V1UV_ARRAY, defaultUvArray(model.mesh,1));
      model.setManagedBuffer(Shader.V2UV_ARRAY, defaultUvArray(model.mesh,2));
      
      Vector3f[] directionPerTriangle = Organizer.rearrangeTextureCoords(model.mesh);
      
      model.setManagedBuffer(Shader.DIRECTION_SHADING_ARRAY,
            directionArray(directionPerTriangle, model.mesh));
   }
   
   
   // -----------------------------------------------------------------------
   // Apply modifications..
   // -----------------------------------------------------------------------

   public static void sphereWarp (MeshModel model, float phase, float mag) {
      for (Mesh1.Vertex v : model.mesh.vertices) {
         Vector3f p = v.getPosition().normalized();
 
         Vector2f p2 = positionToLatLon(p);
         float phase2 = (float)(6.0 * p2.y);
         
         p = p.times((float)(1.0 - mag * Math.sin(phase) * Math.sin(phase2)));
         v.setPosition(p);
      }
      model.getManagedBuffer(Shader.POSITION_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V0POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V1POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V2POS_ARRAY).setModified(true);
   }
}

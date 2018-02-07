package com.generic.base;

import com.generic.base.Algebra.Vector3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Mesh {

   public static class Factory {
      public Vertex   newVertex()    { return new Vertex();   }
      public Triangle newTriangle()  { return new Triangle(); }
      public Edge     newEdge()      { return new Edge();     }
      public Boundary newBoundary()  { return new Boundary(); }
   }
   public Mesh() {
      this(new Factory());
   }
   public Mesh(Factory factory) {
      this.factory = factory;
   }

   public final Factory factory;
   public final Indexable.List<Vertex>   vertices   = new Indexable.List<Vertex>();
   public final Indexable.List<Triangle> triangles  = new Indexable.List<Triangle>();
   public final Indexable.List<Edge>     edges      = new Indexable.List<Edge>();
   public final Indexable.List<Boundary> boundaries = new Indexable.List<Boundary>();
   
   // -----------------------------------------------------------------
   // Mesh consists of Vertices, Triangles, Edges, and Boundary edges
   // -----------------------------------------------------------------
   
   public interface Indexable {
      public static class List<INDEXABLE extends Indexable> implements Iterable<INDEXABLE> {
         private final ArrayList<INDEXABLE> items = new ArrayList<INDEXABLE>();
         
         public int size () {
            return items.size();
         }
         public INDEXABLE get (int index) {
            return items.get(index);
         }
         public void set (int index, INDEXABLE item) {
            items.set(index, item);
         }
         public void clear() {
            items.clear();
         }
         @Override
         public Iterator<INDEXABLE> iterator() {
            return items.iterator();
         }
         
         private void add (INDEXABLE item) {
            item.setIndex(items.size());
            items.add(item);
         }
         private void remove (INDEXABLE item) {
            int lastIndex = items.size()-1;
            INDEXABLE lastItem = items.get(lastIndex);
            int itemIndex = item.getIndex();
            
            items.set(itemIndex, lastItem);
            lastItem.setIndex(itemIndex);
            items.remove(lastIndex);
         }
      }
      
      public int getIndex();
      public void setIndex(int index);
   }
   
   // -----------------------------------------------
   // VERTEX
   // -----------------------------------------------
   
   public static class Vertex implements Indexable {
      public Vertex() {
         this(Vector3.ORIGIN);
      }
      public Vertex(Vector3 position) {
         this.position = position;
         this.oneOutgoingEdge = null;
      }

      // Each VERTEX has an index
      public int getIndex()           { return index;       }
      public void setIndex(int index) { this.index = index; }
      private int index;

      // Each Vertex holds a pointer to one outgoing DirectedEdge
      public Triangle.Edge oneOutgoingEdge()          { return oneOutgoingEdge; }
      private void setOneOutgoingEdge(Triangle.Edge e) { oneOutgoingEdge = e;    }
      private Triangle.Edge oneOutgoingEdge;

      // By repeatedly calling "nextAroundStart()" on the outgoing DirectedEdges,
      // we can iterate over all the DirectedEdges pointing outward from this Vertex
      public Iterable<DirectedEdge> outgoingEdges() {
         return new Iterable<DirectedEdge>() {
            @Override
            public Iterator<DirectedEdge> iterator() {
               return new Iterator<DirectedEdge>() {
                  private DirectedEdge nextOutgoingEdge = oneOutgoingEdge;
                  
                  @Override
                  public boolean hasNext() {
                     return (nextOutgoingEdge != null);
                  }
                  @Override
                  public DirectedEdge next() {
                     DirectedEdge result = nextOutgoingEdge;
                     nextOutgoingEdge = nextOutgoingEdge.nextAroundStart();
                     if (nextOutgoingEdge == oneOutgoingEdge) {
                        nextOutgoingEdge = null;
                     }
                     return result;
                  }};
            }};
      }
      
      // 3D Position 
      public Vector3 getPosition()              { return position;          }
      public void setPosition(Vector3 position) { this.position = position; }
      private Vector3 position;

      
      // TODO: no-one cares about "valence", it shouldn't be in the
      // "main" Mesh class.  But if some code did want to calculate a value
      // for each Vertex and cache it with the Vertex, what would be the
      // right way to do that?
      
      
      // Valence
      public int getValence() {
         return valence;
      }
      public void computeValence() {
         valence = 0;            
         DirectedEdge outgoingEdge = oneOutgoingEdge;
         do {
            valence++;
            outgoingEdge = outgoingEdge.nextAroundStart();
         } while (outgoingEdge != oneOutgoingEdge);
      }
      private Integer valence;
   }
   
   // -----------------------------------------------
   // TRIANGLE
   // -----------------------------------------------
   
   public static class Triangle implements Indexable {
      
      // Each Triangle has three final Edges and Vertices
      public final Triangle.Edge[] edges;
      public final Vertex[] vertices;
      
      // A Triangle.Edge extends DirectedEdge with "getTriangle" and "getIndex" methods:
      public class Edge extends DirectedEdge {
         private Edge(int edgeIndex) {
            this.edgeIndex = edgeIndex;
         }
         public Triangle getTriangle() {
            return Triangle.this;
         }
         public int getEdgeIndex() {
            return edgeIndex;
         }
         private final int edgeIndex;
         
         @Override public Vertex start()        { return vertices[(edgeIndex+1) % 3]; }
         @Override public Vertex end()          { return vertices[(edgeIndex+2) % 3]; }
         @Override public Triangle.Edge next()  { return edges[(edgeIndex+1) % 3];    }
         @Override public Triangle.Edge prev()  { return edges[(edgeIndex+2) % 3];    }
      }
      
      // The Triangle constructor just creates its three Triangle.Edge objects.
      protected Triangle() {
         vertices = new Vertex[] { null, null, null };
         edges    = new Triangle.Edge[]
            { new Triangle.Edge(0), new Triangle.Edge(1), new Triangle.Edge(2) };
      }
      
      // Each TRIANGLE has an index.
      public int getIndex()           { return index;       }
      public void setIndex(int index) { this.index = index; }
      private int index;
      
      // Normal and Area
      public Vector3 getNormal() {
         return normal;
      }
      public float getArea() {
         return area;
      }
      private void computeNormalAndArea() {
         Vector3 cross = edges[0].getVector().cross(edges[1].getVector());
         float crossLength = cross.length();
         area = 0.5f * crossLength;
         normal = cross.times(1.0f / crossLength);
      }
      Vector3 normal;
      float area;       
   }
   
   // -----------------------------------------------
   // EDGE
   // -----------------------------------------------
   
   public static class Edge implements Indexable {
      public Edge() {}

      // Each EDGE has an index
      public int getIndex()           { return index;       }
      public void setIndex(int index) { this.index = index; }
      private int index;

      // Each EDGE points either to two Triangle.Edges,
      // or to one Triangle.Edge and one Boundary.
      //
      private Triangle.Edge first;
      private DirectedEdge second;
      
      public Triangle.Edge getFirst() {
         return first;
      }
      public void setFirst(Triangle.Edge edge) {
         edge.setEdge(this, true);
         first = edge;
      }
      public DirectedEdge getSecond() {
         return second;
      }
      public void setSecond(DirectedEdge edge) {
         edge.setEdge(this, false);
         second = edge;
      }
      public boolean isBoundary() {
         return (second instanceof Boundary);
      }
      
      // Length
      public float getLength() {
         return length;
      }
      public void computeLength() {
         length = first.getVector().length();
      }
      float length;
   }
   
   // -----------------------------------------------
   // DirectedEdge
   // -----------------------------------------------
    
   public abstract static class DirectedEdge {
      private Edge edge;
      private boolean isFirst;

      public void setEdge(Edge edge, boolean isFirst) {
         this.edge = edge;
         this.isFirst = isFirst;
      }
      public Edge getEdge() {
         return edge;
      }
      public boolean isFirst() {
         return isFirst;
      }         
      public Vector3 getVector() {
         return end().getPosition().minus(start().getPosition());
      }
      public boolean isBoundary() {
         return (this instanceof Boundary);
      }
      
      public abstract Vertex start();
      public abstract Vertex end();            
      public abstract DirectedEdge next();
      public abstract DirectedEdge prev();
      
      public DirectedEdge opposite() {
         return isFirst ? edge.getSecond() : edge.getFirst();
      }            
      public DirectedEdge nextAroundStart() {
         return prev().opposite();
      }
      public DirectedEdge prevAroundStart() {
         return opposite().next();
      }
      public DirectedEdge nextAroundEnd() {
         return opposite().prev();
      }
      public DirectedEdge prevAroundEnd() {
         return next().opposite();
      }
   }
   
   // -----------------------------------------------
   // BOUNDARY
   // -----------------------------------------------

   public static class Boundary extends DirectedEdge implements Indexable {
      private Boundary next;
      private Boundary prev;
      
      @Override public Vertex start() {
         return opposite().end();
      }
      @Override public Vertex end() {
         return opposite().start();
      }
      @Override public Boundary next() {
         return next;
      }
      @Override public Boundary prev() {
         return prev;
      }

      @Override public Triangle.Edge opposite() {
         return (Triangle.Edge) super.opposite();
      }            
      @Override public Triangle.Edge nextAroundStart() {
         return (Triangle.Edge) super.nextAroundStart();
      }
      @Override public Triangle.Edge prevAroundStart() {
         return (Triangle.Edge) super.prevAroundStart();
      }
      @Override public Triangle.Edge nextAroundEnd() {
         return (Triangle.Edge) super.nextAroundEnd();
      }
      @Override public Triangle.Edge prevAroundEnd() {
         return (Triangle.Edge) super.prevAroundEnd();
      }
      
      // Each BOUNDARY has an index.
      public int getIndex()           { return index;       }
      public void setIndex(int index) { this.index = index; }
      private int index;
   }

   // --------------------------------------------------------
   // AddVertex
   // --------------------------------------------------------
   
   public Vertex addVertex () {
      Vertex newVertex = factory.newVertex();
      vertices.add(newVertex);
      return newVertex;
   }
   
   // --------------------------------------------------------
   // AddTriangle
   // --------------------------------------------------------
   
   public Triangle addTriangle (Vertex v0, Vertex v1, Vertex v2) {
      // NOTE that we are NOT checking/confirming that each of these Vertices
      // are already attached to *this* Mesh, as opposed to some other Mesh.
      // Should we?
      
      check((v0 != null) && (v1 != null) && (v2 != null) &&
            (v1 != v0) && (v2 != v0) && (v2 != v1),
            "Vertices should be all different");
      
      Triangle t = factory.newTriangle();
      t.vertices[0] = v0; t.vertices[1] = v1; t.vertices[2] = v2;
      
      // Look for existing boundaries along our triangle's edges:
      Boundary[] oldBoundaries = new Boundary[3];
      for (Triangle.Edge triangleEdge : t.edges) {
         Vertex start = triangleEdge.start();
         Vertex end   = triangleEdge.end();
          
         // For each edge in the NEW TRIANGLE, we want to know if the
         // edge is being attached alongside an existing triangle.
         // If it is, a boundary-edge will exist from start to end, and we can
         // look for it by iterating over the edges outgoing from "start":
         boolean any_outgoing_edges = false;
         boolean any_outgoing_boundary_edges = false;
          
         for (DirectedEdge outgoingEdge : start.outgoingEdges()) {
            any_outgoing_edges = true;                
            if (outgoingEdge.isBoundary()) {
                any_outgoing_boundary_edges = true;
            }
            if (outgoingEdge.end() == end) {
               // We've found a pre-existing edge from Start -> End.
               // It better be a Boundary, because if it's a Triangle.Edge,
               // then the edge we're trying to add apparently already exists.
               check(outgoingEdge.isBoundary(), String.format(
                     "Attached edge (from %d to %d) must be a boundary edge", 
                     start.getIndex(), end.getIndex()));
               oldBoundaries[triangleEdge.getEdgeIndex()] = (Boundary) outgoingEdge;
               break;
            }
         }
          
         // If this vertex had any edges attached to it, then at least one of those
         // must be a Boundary, otherwise we have an ERROR.  We can't create a triangle
         // if a corner is already completely surrounded by triangles.
         check (!any_outgoing_edges || any_outgoing_boundary_edges,
            "Attached vertex must be a boundary vertex");
       }
       
       // Now that the "checks" above are done, we're committed to adding
       // the new triangle and updating any Boundary and Edge objects:
       triangles.add(t);
       
       // Connect each triangleEdge to either an existing triangle or a new boundary:
       for (Triangle.Edge triangleEdge : t.edges) {
          Boundary existingBoundary = oldBoundaries[triangleEdge.getEdgeIndex()];
          if (existingBoundary == null) {
             // If there's no existingBoundary, then we have to
             // construct the new Boundary for this "triangleEdge" ourselves,
             // as well as an Edge that connects to that Boundary:
             
             Boundary newBoundary = factory.newBoundary();
             boundaries.add(newBoundary);
             
             Edge newEdge = factory.newEdge();
             newEdge.setFirst(triangleEdge);
             newEdge.setSecond(newBoundary);
             edges.add(newEdge);
             
          } else {
             // If there is an existingBoundary, then there's already an Edge
             // connecting that Boundary to an existing Triangle.  This "triangleEdge"
             // will replace the existingBoundary, which gets deleted:
             
             Edge existingEdge = existingBoundary.getEdge();
             if (existingBoundary.isFirst()) {
                existingEdge.setFirst(triangleEdge);
             } else {
                existingEdge.setSecond(triangleEdge);
             }
             boundaries.remove(existingBoundary);
          }
       }
       
       // Now let's consider each VERTEX in turn
       for (Triangle.Edge oppositeEdge : t.edges) {
           
          // Consider a VERTEX
          Vertex vertex = t.vertices[oppositeEdge.getEdgeIndex()];
           
          // When looping counterclockwise around the triangle edges,
          // we first encounter an edge pointing towards "vertex",
          // followed by an edge pointing away from "vertex":
          Triangle.Edge prevEdge = oppositeEdge.next();  // (points towards v)
          Triangle.Edge nextEdge = oppositeEdge.prev();  // (points away from v)
       
          // The "opposite" pointers in these Edges were set above, either to Triangle.Edges
          // of existing triangles (if attached) or to new Boundary objects:
          DirectedEdge prevEdgeOpposite = prevEdge.opposite();  // (points away from v)
          DirectedEdge nextEdgeOpposite = nextEdge.opposite();  // (points towards v)             
          boolean prevEdgeAttached = !prevEdgeOpposite.isBoundary();
          boolean nextEdgeAttached = !nextEdgeOpposite.isBoundary();             
       
          // There are 4 cases based on whether the prev and next edges are attached:
          if (!prevEdgeAttached && !nextEdgeAttached) {
             // CASE 1. We've created two new boundary objects:
             Boundary prevEdgeBoundary = (Boundary) prevEdgeOpposite;
             Boundary nextEdgeBoundary = (Boundary) nextEdgeOpposite;

             // Does v have ANY existing edges?
             if (vertex.oneOutgoingEdge == null) {
                // v has NO existing edges, it's a NEW vertex just for this Triangle.
                nextEdgeBoundary.next = prevEdgeBoundary;
                prevEdgeBoundary.prev = nextEdgeBoundary;
                
                // We're the first one to set "one-outgoing-edge" for this vertex:
                vertex.setOneOutgoingEdge(nextEdge);
                
             } else {
                // v has existing edges, but our new triangle doesn't connect with any of them.
                // We've already confirmed that v is on the boundary, we actually walked right past
                // a Boundary that confirmed it, but we didn't save it anywhere
                //
                // TODO:  now we have to look for it again?  save it at the first scan over vertices..
                Boundary outgoingBoundary = null;
                for (DirectedEdge outgoingEdge : vertex.outgoingEdges()) {
                   if (outgoingEdge.isBoundary()) {
                      outgoingBoundary = (Boundary) outgoingEdge;
                      break;
                   }
                }
                
                // We're inserting ths triangle just clockwise of "outgoingBoundary"
                Boundary afterNextEdgeBoundary = outgoingBoundary;
                Boundary beforePrevEdgeBoundary = afterNextEdgeBoundary.prev;
                afterNextEdgeBoundary.prev = nextEdgeBoundary;
                nextEdgeBoundary.next = afterNextEdgeBoundary;
                
                beforePrevEdgeBoundary.next = prevEdgeBoundary;
                prevEdgeBoundary.prev = beforePrevEdgeBoundary;
             }
          } else if (prevEdgeAttached && !nextEdgeAttached) {
              // CASE 2. Link the "unattached" boundary triangle that's opposite "nextEdge":
             
              Boundary nextEdgeBoundary = (Boundary) nextEdgeOpposite;
              Boundary boundaryReplacedByPrevEdge = oldBoundaries[prevEdge.getEdgeIndex()];

              // The boundary that used to be "next" to the old (deleted) boundaryReplacedByPrevEdge,
              // will now be the "next" of the new (just created) nextEdgeBoundary:
              
              Boundary afterNextEdgeBoundary = boundaryReplacedByPrevEdge.next;
              afterNextEdgeBoundary.prev = nextEdgeBoundary;
              nextEdgeBoundary.next = afterNextEdgeBoundary;
   
           } else if (!prevEdgeAttached && nextEdgeAttached) {
              // CASE 3. Link one unattached boundary:
              
              Boundary boundaryReplacedByNextEdge = oldBoundaries[nextEdge.getEdgeIndex()];
              Boundary prevEdgeBoundary = (Boundary) prevEdgeOpposite;

              // The boundary that used to be "prev" to the old (deleted) boundaryReplacedByNextEdge,
              // will now be the "prev" of the new (just created) prevEdgeBoundary:
              
              Boundary beforePrevEdgeBoundary = boundaryReplacedByNextEdge.prev;
              beforePrevEdgeBoundary.next = prevEdgeBoundary;
              prevEdgeBoundary.prev = beforePrevEdgeBoundary;
              
           } else {
              // CASE 4. BOTH edges are attached.  The two boundaries should be adjacent:
              Boundary boundaryReplacedByNextEdge = oldBoundaries[nextEdge.getEdgeIndex()];
              Boundary boundaryReplacedByPrevEdge = oldBoundaries[prevEdge.getEdgeIndex()];
              
              if (boundaryReplacedByNextEdge.prev != boundaryReplacedByPrevEdge) {
                 // If the deleted boundaries were not adjacent, then the edges between them need
                 // to be re-inserted to the circle of edges connected to this vertex.
                 
                 Boundary orphanSectionFirstIncoming = boundaryReplacedByNextEdge.prev;
                 Boundary orphanSectionLastOutgoing = boundaryReplacedByPrevEdge.next;
                 
                 // TODO:  we should do this "CHECK" up at step 1 where we first check the vertex,
                 //    because we should perform all CHECKS before modifying the mesh..
                 
                 DirectedEdge outFromV    = prevEdgeOpposite.nextAroundStart();
                 DirectedEdge outFromVEnd = nextEdge.prevAroundStart();
                 boolean foundBoundary = false;
                 do {
                    if (outFromV.isBoundary()) {
                       // We've found another outgoing Boundary edge.
                       // We can insert the orphan section here:
                       Boundary startingBoundary = (Boundary) outFromV;
                       Boundary endingBoundary = startingBoundary.prev;
                       
                       startingBoundary.prev = orphanSectionFirstIncoming;
                       orphanSectionFirstIncoming.next = startingBoundary;
                       
                       endingBoundary.next = orphanSectionLastOutgoing;
                       orphanSectionLastOutgoing.prev = endingBoundary;
                       
                       foundBoundary = true;
                       break;
                    }
                    
                    outFromV = outFromV.nextAroundStart();
                 } while (outFromV != outFromVEnd);
                 
                 check(foundBoundary, "Triangle filling corner vertex has un-movable extra triangles");
              }
           }
       }
       return t;
   }

   // --------------------------------------------------------
   // RemoveTriangle
   // --------------------------------------------------------

   public void removeTriangle (Triangle t) {
      // NOTE that we are NOT checking/confirming that this Triangle
      // is in fact currently added to this Mesh.   Should we?
      triangles.remove(t);
      
      // First, we consider each each VERTEX in turn
      // and make sure it's not pointing to the edges of this Triangle:
      for (Triangle.Edge oppositeEdge : t.edges) {
          
         // Consider a VERTEX
         Vertex vertex = t.vertices[oppositeEdge.getEdgeIndex()];
          
         // When looping counterclockwise around the triangle edges,
         // we first encounter an edge pointing towards "vertex",
         // followed by an edge pointing away from "vertex":
         Triangle.Edge prevEdge = oppositeEdge.next();  // (points towards v)
         Triangle.Edge nextEdge = oppositeEdge.prev();  // (points away from v)
      
         if (vertex.oneOutgoingEdge() == nextEdge) {               
            Triangle.Edge newOutgoingEdgeForV = null;
            
            // We're going to change this vertex's "oneOutgoingEdge" so it points
            // any OTHER triangle besides "t", or null if "t" is the only triangle
            // connected to this vertex.
            
            DirectedEdge outFromStart = prevEdge.opposite();
            while (outFromStart != nextEdge) {
               if (!outFromStart.isBoundary()) {
                  newOutgoingEdgeForV = (Triangle.Edge) outFromStart;
                  break;
               }
               outFromStart = outFromStart.nextAroundStart();
            }
            vertex.setOneOutgoingEdge(newOutgoingEdgeForV);
         }
      }
      
      // Now for each edge of the TRIANGLE to be DELETED, if the edge is
      // connected to another triangle, that edge is going to need a new Boundary.
      // On the other hand, if the edge is NOT connected to another triangle,
      // then it already IS connected to a Boundary that we have to delete:
      Boundary[] newBoundaries = new Boundary[3];
      Boundary[] oldBoundaries = new Boundary[3];

      for (Triangle.Edge triangleEdge : t.edges) {
         DirectedEdge oppositeEdge = triangleEdge.opposite();
         boolean isAttached = !oppositeEdge.isBoundary();
         
         if (isAttached) {
            Triangle.Edge oppositeTriangleEdge = (Triangle.Edge) oppositeEdge; 
            
            // There's an existing Triangle opposite this triangleEdge.
            // We're going to have to construct a new Boundary that will
            // replace "triangleEdge" as the opposite to "oppositeTriangleEdge"

            Boundary newBoundary = factory.newBoundary();
            boundaries.add(newBoundary);
            newBoundaries[triangleEdge.getEdgeIndex()] = (Boundary) newBoundary;
            
            Edge existingEdge = triangleEdge.getEdge();
            if (triangleEdge.isFirst()) {
               existingEdge.setFirst(oppositeTriangleEdge);
            }
            existingEdge.setSecond(newBoundary);
            
         } else {
            Boundary oppositeBoundaryEdge = (Boundary) oppositeEdge;
            
            // If there's no existing Triangle opposite this triangleEdge,
            // then this triangleEdge is connected to Edge and Boundary objects
            // that we'll have to delete along with this Triangle:
            Edge oldEdge = oppositeBoundaryEdge.getEdge();
            oldBoundaries[triangleEdge.getEdgeIndex()] = oppositeBoundaryEdge;
            
            boundaries.remove(oppositeBoundaryEdge);
            edges.remove(oldEdge);
         }
      }
      
      // Now let's consider each VERTEX again
      for (Triangle.Edge oppositeEdge : t.edges) {
         
         // Consider a VERTEX
         Vertex vertex = t.vertices[oppositeEdge.getEdgeIndex()];
          
         // When looping counterclockwise around the triangle edges,
         // we first encounter an edge pointing towards "vertex",
         // followed by an edge pointing away from "vertex":
         Triangle.Edge prevEdge = oppositeEdge.next();  // (points towards v)
         Triangle.Edge nextEdge = oppositeEdge.prev();  // (points away from v)
         
         // If there were triangles opposite "prevEdge" and "nextEdge" then they
         // have new Boundary objects that need to be wired up:
         
         boolean prevEdgeFree = (newBoundaries[prevEdge.getEdgeIndex()] == null);
         boolean nextEdgeFree = (newBoundaries[nextEdge.getEdgeIndex()] == null);
         
         // There a 4 cases based on whether the prev and next edges are attached:
         if (prevEdgeFree && nextEdgeFree) {
            // CASE 1. Link both "unattached" boundary triangles.
            
            Boundary oldBoundaryOppositeNextEdge = oldBoundaries[nextEdge.getEdgeIndex()];
            Boundary oldBoundaryOppositePrevEdge = oldBoundaries[prevEdge.getEdgeIndex()];
            
            if (oldBoundaryOppositePrevEdge.prev != oldBoundaryOppositeNextEdge) {
               // This means v has other interior triangles attached, in other words it's a point
               // where two or more boundary curves touch at a single vertex.   We have to
               // reconnect the boundary curves to account for removing two of them:
               
               Boundary beforePrevEdgeBoundary = oldBoundaryOppositePrevEdge.prev;
               Boundary afterNextEdgeBoundary = oldBoundaryOppositeNextEdge.next;
               beforePrevEdgeBoundary.next = afterNextEdgeBoundary;
               afterNextEdgeBoundary.prev = beforePrevEdgeBoundary;
            }
            
         } else if (!prevEdgeFree && nextEdgeFree) {
            
            Boundary newBoundaryReplacingPrevEdge = newBoundaries[prevEdge.getEdgeIndex()];
            Boundary oldBoundaryOppositeNextEdge = oldBoundaries[nextEdge.getEdgeIndex()];

            Boundary afterNextEdgeBoundary = oldBoundaryOppositeNextEdge.next;
            newBoundaryReplacingPrevEdge.next = afterNextEdgeBoundary;
            afterNextEdgeBoundary.prev = newBoundaryReplacingPrevEdge;
            
         } else if (prevEdgeFree && !nextEdgeFree) {
            
            Boundary newBoundaryReplacingNextEdge = newBoundaries[nextEdge.getEdgeIndex()];
            Boundary oldBoundaryOppositePrevEdge = oldBoundaries[prevEdge.getEdgeIndex()];

            Boundary beforePrevEdgeBoundary = oldBoundaryOppositePrevEdge.prev;
            newBoundaryReplacingNextEdge.prev = beforePrevEdgeBoundary;
            beforePrevEdgeBoundary.next = newBoundaryReplacingNextEdge;

         } else {
            // CASE 4. BOTH edges are attached.

            Boundary newBoundaryReplacingNextEdge = newBoundaries[nextEdge.getEdgeIndex()];
            Boundary newBoundaryReplacingPrevEdge = newBoundaries[prevEdge.getEdgeIndex()];
            
            // The new boundaries need to be connected:
            
            newBoundaryReplacingPrevEdge.next = newBoundaryReplacingNextEdge;
            newBoundaryReplacingNextEdge.prev = newBoundaryReplacingPrevEdge;
         }
      }
   }
   
   // --------------------------------------------------------
   // CLEAR
   // --------------------------------------------------------
   
   public void clear() {
      triangles.clear();
      boundaries.clear();
      vertices.clear();
      edges.clear();
   }
   
   // --------------------------------------------------------
   // COPY
   // --------------------------------------------------------
   
   public void copyFrom(Mesh m) {
      clear();
      
      // Copy vertices, setting 3D position
      for (Vertex v : m.vertices) {
         Vertex newVertex = factory.newVertex();
         vertices.add(newVertex);
         newVertex.setPosition(v.getPosition());
      }
      // Copy triangles, setting Vertex pointers
      for (Triangle t : m.triangles) {
         Triangle newTriangle = factory.newTriangle();
         triangles.add(newTriangle);
         newTriangle.vertices[0] = vertices.get(t.vertices[0].getIndex());
         newTriangle.vertices[1] = vertices.get(t.vertices[1].getIndex());
         newTriangle.vertices[2] = vertices.get(t.vertices[2].getIndex());
      }
      // Create boundaries
      for (Boundary b : m.boundaries) {
         Boundary newBoundary = factory.newBoundary();
         boundaries.add(newBoundary);
      }
      // Link the boundaries up in loops
      for (Boundary b : m.boundaries) {
         Boundary boundary = boundaries.get(b.getIndex());
         boundary.next = boundaries.get(b.next.getIndex());
         boundary.prev = boundaries.get(b.prev.getIndex());
      }
      // Create Edges and link them to the Triangles and Boundaries:
      for (Edge e : m.edges) {
         Edge newEdge = factory.newEdge();
         edges.add(newEdge);
         
         Triangle.Edge eFirst = e.first;
         newEdge.setFirst(triangles.get(eFirst.getTriangle().getIndex()).edges[eFirst.getEdgeIndex()]);
         
         DirectedEdge eSecond = e.second;         
         if (!eSecond.isBoundary()) {
            Triangle.Edge teSecond = (Triangle.Edge) eSecond;
            newEdge.setSecond(triangles.get(teSecond.getTriangle().getIndex()).edges[teSecond.getEdgeIndex()]);
         } else {
            newEdge.setSecond(boundaries.get(((Boundary)eSecond).getIndex()));
         }
      }
      for (Vertex v : m.vertices) {
         Vertex vertex = vertices.get(v.getIndex());
         if (v.oneOutgoingEdge != null) {
            Triangle triangle = triangles.get(v.oneOutgoingEdge.getTriangle().getIndex());
            vertex.setOneOutgoingEdge(triangle.edges[v.oneOutgoingEdge.getEdgeIndex()]);
         }
      }
   }
   
   // --------------------------------------------------------
   // LOAD
   // --------------------------------------------------------

   public void loadFromString(String serialized) {
      clear();

      for (String line : serialized.split("\n")) {
         if (line.charAt(0) == '#') {
            continue;
         }
         if (line.charAt(0) == 'v') {
            String restOfLine = line.substring(2);
            String[] pieces = restOfLine.split(" ");
            if (pieces.length == 3) {
               Mesh.Vertex v = addVertex();
               v.setPosition(new Vector3(Float.valueOf(pieces[0]), Float.valueOf(pieces[1]), Float.valueOf(pieces[2])).times(60.0f));
            }            
            continue;
         }
         if (line.charAt(0) == 'f') {
            String restOfLine = line.substring(2);
            String[] pieces = restOfLine.split(" ");
            if (pieces.length == 3) {
               int v0 = Integer.valueOf(pieces[0])-1;
               int v1 = Integer.valueOf(pieces[1])-1;
               int v2 = Integer.valueOf(pieces[2])-1;
               //System.out.format("Triangle %d,%d,%d\n", v0,v1,v2);
               addTriangle(vertices.get(v0),vertices.get(v1),vertices.get(v2));
            }
            continue;
         }
      }  
   }   
   

   // --------------------------------------------------------
   // DATA - LAYERS
   // --------------------------------------------------------
   // okay, what's a data layer?
   //
   // an instance of a class "X" that exists for each Triangle / or Vertex / or Boundary / or Edge
   // a mesh could have data-layers "registered" for each type of object..
   //   whenever onNewTriangle is called a new "X" is created..
   //
   // examples:
   //    per-triangle "lat/lon"-triple for sphere generator
   //    per-triangle texture info 
   //    
   //    per-triangle AND per-edge AND per-vertex info .. AnglesAndCurvature
   //    per-triangle AND per-edge AND per-vertex info .. used for Cut-Graph-Computation
   //    per-triangle AND per-edge AND per-vertex info .. used for Quad-Mesh
   //    
   // however all these examples are data-layers that 
   // do NOT have to be persisted as trinalges are added or destroyed.
   //
   // so, if we're NOT going to add "listeners" or anything...
   //   then, why not just use an Array?
   // 
   
   
   
   // --------------------------------------------------------
   // Buggy
   // --------------------------------------------------------
   /*
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
   */
   
   
   // --------------------------------------------------------
   // Validation -- is this mesh actually hooked up right?
   // --------------------------------------------------------
   
   private static void check(boolean cond, String err) {
      if (!cond) throw new RuntimeException("FAILED: " + err);
   }
   private void checkIndexable(Indexable.List<? extends Indexable> list, String type) {
      for (int i = 0; i < list.size(); ++i) {
         Indexable item = list.get(i);
         check(i == item.getIndex(), "Item with wrong index?!");
      }
   }
   private void checkTriangleEdge(Triangle.Edge te, HashSet<Vertex> verticesReferenced, HashSet<Edge> edgesReferenced) {
      check(te.getEdge() != null, "Null edge discovered");
      check(te.opposite() != null, "Null opposite discovered");
      
      Vertex start = te.start();
      Vertex end = te.end();
      DirectedEdge to = te.opposite();
      check((start != null) && (end != null), "Null vertices discovered");
      check((to.start() == end) && (to.end() == start), "Opposing edges match");
      check(start != end, "Vertices distinct");
      
      verticesReferenced.add(start);
      edgesReferenced.add(te.getEdge());
   }
   private void checkTriangle(Triangle t, HashSet<Vertex> verticesReferenced, HashSet<Edge> edgesReferenced) {
      for (Triangle.Edge edge : t.edges) {
         checkTriangleEdge(edge, verticesReferenced, edgesReferenced);
      }
   }
   private void checkBoundary(Boundary b, HashSet<Vertex> verticesReferenced, HashSet<Edge> edgesReferenced) {         
      check(b.getEdge() != null, "Null edge discovered in boundary");
      check(b.opposite() != null, "Null opposite discovered in boundary");
      
      check((b.next != null) && (b.prev != null), "Null links discovered");
      check(edgesReferenced.contains(b.getEdge()), "Missing edge link");
      check(b.next != b.prev, "Single boundary loop");
      
      int count = 0;
      for (Boundary p = b.next; p != b.prev; p = p.next()) { 
         count++;
         check ((count < 50000), "Boundary overlarge boundary loop");
      }
   }      
   private void checkEdge(Edge e, HashSet<Edge> edgesReferenced) {
      check(edgesReferenced.contains(e), "All edges referenced");
      check(e.first != null, "Null first discovered in edge");
      check(e.second != null, "Null second discovered in edge");
      check((e.first.getEdge() == e) && (e.second.getEdge() == e), "DirectedEdges point back to Edges");
      check(e.first.isFirst() && !e.second.isFirst(), "DirectedEdges know their order in the Edges");
   }      
   private void checkVertex(Vertex v,  HashSet<Vertex> verticesReferenced) {
      if (verticesReferenced.contains(v)) {
         check (v.oneOutgoingEdge != null, "Referenced vertex has outgoing");
         
         int count = 0;
         for (DirectedEdge e : v.outgoingEdges()) {
            check(e.start() == v, "Edges encountered in outgoingEdges-loop all start right");
            count++;
            check ((count < 100), "Vertex outgoings overlarge");
         }
      } else {
         check (v.oneOutgoingEdge == null, "Unreferenced vertex has no outgoing");
      }
   }      
   
   public void checkMesh() {
      System.out.format("Checking Mesh: %d triangles, %d edges, %d vertices, %d boundaries\n",
            triangles.size(), edges.size(), vertices.size(), boundaries.size());
      
      checkIndexable(triangles, "triangles");
      checkIndexable(edges, "edges");
      checkIndexable(vertices, "vertices");
      checkIndexable(boundaries, "boundaries");
      
      HashSet<Vertex> verticesReferenced = new HashSet<Vertex>();
      HashSet<Edge> edgesReferenced = new HashSet<Edge>();
      
      for (Triangle t : triangles) checkTriangle(t, verticesReferenced, edgesReferenced);
      for (Boundary b : boundaries) checkBoundary(b, verticesReferenced, edgesReferenced);
      for (Edge e : edges) checkEdge(e, edgesReferenced);
      for (Vertex v : vertices) checkVertex(v, verticesReferenced);
      
      System.out.format("..Checking Done.\n");
   }

   public void testAddAndDelete() {
      class TriangleRecord {
         public final Vertex v0,v1,v2;
         public Triangle t;
         public TriangleRecord(Triangle t) {
            this.v0 = t.vertices[0];
            this.v1 = t.vertices[1]; 
            this.v2 = t.vertices[2]; 
            this.t = t;
         }
      }
      
      int numTriangles = triangles.size();
      TriangleRecord[] tris = new TriangleRecord[numTriangles];
      int i = 0;
      for (Triangle t : triangles) {
         tris[i++] = new TriangleRecord(t);
      }
      
      System.out.format("Beginning sequence of random deletions and additions!\n\n");
      
      int trials = 100;
      
      for (int j = 0; j < trials; ++j) {
         int triangleToAffect = (int)(Math.random() * numTriangles);
         TriangleRecord record = tris[triangleToAffect];
         
         if (record.t != null) {
            removeTriangle(record.t);
            record.t = null;
         } else {
            record.t = addTriangle(record.v0,record.v1,record.v2);
         }
         checkMesh();
      }
      
      for (int j = 0; j < numTriangles; ++j) {
         TriangleRecord record = tris[j];
         if (record.t != null) {
            removeTriangle(record.t);
            record.t = null;
            checkMesh();
         }
      }
      
      for (int j = 0; j < trials; ++j) {
         int triangleToAffect = (int)(Math.random() * numTriangles);
         TriangleRecord record = tris[triangleToAffect];
         
         if (record.t != null) {
            removeTriangle(record.t);
            record.t = null;
         } else {
            record.t = addTriangle(record.v0,record.v1,record.v2);
         }
         checkMesh();
      }
      
      System.out.format("DONE... putting back any remaining deleted triangles...\n\n");
      
      for (int j = 0; j < numTriangles; ++j) {
         TriangleRecord record = tris[j];
         if (record.t == null) {
            record.t = addTriangle(record.v0,record.v1,record.v2);
            checkMesh();
         }
      }
      System.out.format("Repaired...\n\n");
   }
}

package com.generic.base;

import com.generic.base.Algebra.Vector3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;


public class Mesh2 {

   // ==================================================================
   // A Mesh keeps track of CONNECTIVITY
   // within a set of Vertices, Faces, and Edges
   // ==================================================================
   
   // Each vertex, face, and edge has an integer ID.
   // This tells you how many have been allocated:
   public int numVertexIDs() { return vertexIDManager.getNumReservedIDs(); }
   public int numEdgeIDs()   { return edgeIDManager.getNumReservedIDs();   }
   public int numFaceIDs()   { return faceIDManager.getNumReservedIDs();   }


   // Each edge is associated with two directed-edges pointing in opposite directions.
   // Given an edge-ID, the IDs of its directed-edges can be found by doubling the edge-ID,
   // and optionally adding one:
   public int forwardDirectedEdge (int edge) {
      return 2 * edge;
   }
   public int reverseDirectedEdge (int edge) {
      return 2 * edge + 1;
   }

   // Likewise, divide a directedEdge-ID in half to get its edge-ID:
   public int edgeOf (int directedEdge) {
      return directedEdge / 2;
   }
   // From one directedEdge-ID you can get the directedEdge-ID
   // that's pointing in the opposite direction
   public int opposite (int directedEdge) {
      return directedEdge ^ 1;
   }

   // Given a directedEdge-ID, you can retrieve the vertex-ID
   // of the Vertex that's at the START or END of the directedEdge.
   public int startOf (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge];
   }
   public int endOf (int directedEdge) {
      return startOf(opposite(directedEdge));
   }

   // Every directedEdge is part of a loop.
   // It's either a loop encircling a Face or a boundary-loop.
   // If it encircles a Face, you can get its face-ID, otherwise you'll get -1
   public int faceOf (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 1];
   }
   // A directedEdge that's part of a boundary-loop
   // doesn't have a Face associated with it, so faceOf returns -1
   public boolean isBoundary (int directedEdge) {
      return faceOf(directedEdge) < 0;
   }

   // Every directedEdge is part of a loop, and you can call these methods
   // to go to the next or previous directedEdge along its loop:
   public int nextInLoop (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 2];
   }
   public int prevInLoop (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 3];
   }


   // Given a directedEdge-id, you can also traverse a loop of directedEdges
   // by looping around the outgoing edges of the START vertex
   public int nextAroundStart (int directedEdge) {
      return opposite(prevInLoop(directedEdge));
   }
   public int prevAroundStart (int directedEdge) {
      return nextInLoop(opposite(directedEdge));
   }
      
   // Given a directedEdge-id, you can also traverse a loop of directedEdges
   // by looping around the incoming edges of the END vertex
   public int nextAroundEnd (int directedEdge) {
      return prevInLoop(opposite(directedEdge));
   }
   public int prevAroundEnd (int directedEdge) {
      return opposite(nextInLoop(directedEdge));
   }
   

   // Given a vertex-ID, you can retrieve the ID of an outgoing directedEdge:
   public int outgoingEdgeForVertex (int vertex) {
      return vertexToDirectedEdge.array()[vertex];
   }
   // Given a face-ID, you can retrieve the ID of a directedEdge
   // that's part of the loop encircling it.
   public int directedEdgeForFace (int face) {
      return faceToDirectedEdge.array()[face];
   }


   // A vertex-ID that doesn't have a directedEdge is "disconnected"
   public boolean isVertexConnected (int vertex) {
      return outgoingEdgeForVertex(vertex) >= 0;
   }
   // An face-ID that doesn't have a directedEdge is "disconnected"
   public boolean isFaceConnected (int face) {
      return directedEdgeForFace(face) >= 0;
   }
   // A directedEdge-ID that doesn't have a start-vertex is "disconnected"
   public boolean isDirectedEdgeConnected (int directedEdge) {
      return startOf(directedEdge) >= 0;
   }
   // An edge-ID whose directedEdge is "disconnected" is itself "disconnected"
   public boolean isEdgeConnected (int edge) {
      return isDirectedEdgeConnected(forwardDirectedEdge(edge));
   }
   
   
   // ==================================================================
   // A Mesh can be built up iteratively by adding disconnected
   // vertex-IDs, and connecting them by adding faces.
   // ==================================================================

   // Call this to add a new vertex.
   // A new vertex-ID is returned, but it's not connected to anything,
   // (that is, "outgoingEdgeForVertex" will return -1)
   public int newVertexID() {
      int vertex = vertexIDManager.getNewID();
      setOutgoingEdgeForVertex(vertex, -1);
      return vertex;
   }
   
   // Call this to connect vertices to form a new face.
   // A new face-ID is returned, and it'll be connected to a new loop
   // of directedEdges going around the provided vertices.
   public int addFace(int... vertices) {
      int numVertices = vertices.length;

      // Must have at least 3 vertices
      if (numVertices < 3) {
         throw new RuntimeException(String.format(
            "Cannat add face with only %d vertices", numVertices));
      }
      
      // We'll use these arrays to temporarily store edge indices
      int[] faceEdges = new int[numVertices];
      int[] vertexOutgoingBoundaryEdges = new int[numVertices];
      for (int i = 0; i < numVertices; ++i) {
         faceEdges[i] = -1;
         vertexOutgoingBoundaryEdges[i] = -1;
      }
      
      // We examine each edge in turn to see if it will connect this face
      // to any pre-existing edges.  Along the way we CHECK mesh validity.
      for (int i = 0; i < numVertices; ++i) {
         int startVertex = vertices[i];
         int endVertex = vertices[(i + 1) % numVertices];
         
         // Does "startVertex" have any edges already connected to it?
         int firstOutgoingEdgeFromStart = outgoingEdgeForVertex(startVertex);
         if (firstOutgoingEdgeFromStart != -1) {
            boolean foundBoundaryEdgeFromStart = false;
             
            // Examine each pre-existing edge outgoing from start
            int outgoingEdgeFromStart = firstOutgoingEdgeFromStart;
            do {
               if (endOf(outgoingEdgeFromStart) == endVertex) {
                  // We've discovered there's already a directedEdge going
                  // from startVertex to endVertex.  If this edge already has
                  // a face then it's not valid to add this face!
                  if (!isBoundary(outgoingEdgeFromStart)) {
                     throw new RuntimeException(String.format(
                        "DirectedEdge from %d to %d already exists",
                        startVertex, endVertex));
                  }
                  
                  foundBoundaryEdgeFromStart = true;
                  faceEdges[i] = outgoingEdgeFromStart;
                  break;

               } else {
                  // This edge connects startVertex to some other endpoint
                  // that's not endVertex.  Let's note if any of these edges
                  // are boundaries:
                  if (isBoundary(outgoingEdgeFromStart)) {
                     vertexOutgoingBoundaryEdges[i] = outgoingEdgeFromStart;
                     foundBoundaryEdgeFromStart = true;
                  }
               }

               outgoingEdgeFromStart = nextAroundStart(outgoingEdgeFromStart);               
            } while (outgoingEdgeFromStart != firstOutgoingEdgeFromStart);
            
            // Since startVertex has edges attached to it, then at least one 
            // of must be a boundary, since we can't create a face if a proposed
            // corner vertex is already completely surrounded by faces.
            if (!foundBoundaryEdgeFromStart) {
               throw new RuntimeException(String.format(
                  "Vertex %d already completely surrounded by faces",
                  startVertex));
            }
         }
      }
      
      // Check if two adjacent edges of the new face connect to existing faces
      for (int i = 0; i < numVertices; ++i) {
         int prevEdge = faceEdges[(i + (numVertices-1)) % numVertices];
         int nextEdge = faceEdges[i];
         
         if ((prevEdge != -1) && (nextEdge != -1)) {
            int vertex = startOf(nextEdge);
            
            // We know endOf(firstEdge) == startOf(secondEdge),
            // and both are part of boundary-loops -- but are they
            // adjacent edges in the same boundary-loops?
            if (prevInLoop(nextEdge) != prevEdge) {

               // No, apparently there are other edges touching this vertex
               // that lie between 'firstEdge' and 'secondEdge'.
               // These other edges will have to be moved to a different position
               // around the vertex, which is not a problem so long as there's
               // another pair of boundary edges they can be placed between:

               boolean foundAlternateBoundary = false;
               int outgoingEdgeFromStart = nextAroundStart(opposite(prevEdge));
               while (outgoingEdgeFromStart != nextEdge) {
                  if (isBoundary(outgoingEdgeFromStart)) {
                     vertexOutgoingBoundaryEdges[i] = outgoingEdgeFromStart;
                     foundAlternateBoundary = true;
                     break;
                  }
                  outgoingEdgeFromStart = nextAroundStart(outgoingEdgeFromStart);
               }
               
               if (!foundAlternateBoundary) {
                  throw new RuntimeException(String.format(
                     "Vertex %d has faces that block the addition of this face",
                     vertex));
               }
            }
         }
      }
      
      // Okay we've CHECKED everything and confirmed this face can be added
      // without messing up the mesh.  At this point, we're committed to
      // modifying the mesh, and won't be throwing any more exceptions.
      int newFaceID = faceIDManager.getNewID();

      // For each edge of this new face, if we DIDN'T find a pre-existing
      // edge in the scan above, we have to CREATE the edge here.
      for (int i = 0; i < numVertices; ++i) {
         if (faceEdges[i] == -1) {
            int newEdgeID = edgeIDManager.getNewID();
            faceEdges[i] = forwardDirectedEdge(newEdgeID);
            
            // Creating a new Edge
            int startVertex = vertices[i];
            int endVertex = vertices[(i + 1) % numVertices];
            
            // [1] We're ADDING an Edge to the connected mesh
            initEdge(newEdgeID, startVertex, endVertex);
            this.numEdges++;
         }

         // Point all the edges at the new face:         
         setFaceOf(faceEdges[i], newFaceID);
      }

      // [2] We're ADDING a Face to the connected mesh
      setDirectedEdgeForFace(newFaceID, faceEdges[0]);
      this.numFaces++;

      // Finally, we have to fix up the various edge links --
      // We consider each VERTEX in turn:      
      for (int i = 0; i < numVertices; ++i) {
         int prevEdge = faceEdges[(i + (numVertices-1)) % numVertices];
         int nextEdge = faceEdges[i];
         
         int oppositePrevEdge = opposite(prevEdge);  // outgoing from vertex
         int oppositeNextEdge = opposite(nextEdge);  // incoming to vertex
         boolean prevEdgeFree = isBoundary(oppositePrevEdge);
         boolean nextEdgeFree = isBoundary(oppositeNextEdge);
         
         // There are 4 cases based on whether the prev and next edges are attached:
         if (prevEdgeFree && nextEdgeFree) {
            // CASE 1. BOTH prevEdge and nextEdge were created here just a moment ago.
            //         Their opposites still need to be wired up,
            
            // What we need to do depends on whether the vertex has any other pre-existing edges.
            int vertex = startOf(nextEdge);            
            if (outgoingEdgeForVertex(vertex) < 0) {
               // vertex has NO existing edges, it's NEW being connected to this face first.
               // We have to wire the two opposites edges together:               
               connectEdges (oppositeNextEdge, oppositePrevEdge);
               
               // [3] We're ADDING a Vertex to the connected mesh
               setOutgoingEdgeForVertex(vertex, nextEdge);
               this.numVertices++;
               
            } else {
               // vertex HAS existing edges.  In this case we've already located
               // a boundary-edge outgoing from vertex, and we have to insert this
               // new corner in between the existing boundary edges:
               int outgoingBoundary = vertexOutgoingBoundaryEdges[i];
               int incomingBoundary = prevInLoop(outgoingBoundary);
               connectEdges (oppositeNextEdge, outgoingBoundary);
               connectEdges (incomingBoundary, oppositePrevEdge);               
            }            
         } else if (!prevEdgeFree && nextEdgeFree) {
            // CASE 2. Only oppositeNextEdge needs to be wired up.
            int outgoingBoundary = nextInLoop(prevEdge);
            connectEdges (oppositeNextEdge, outgoingBoundary);

         } else if (prevEdgeFree && !nextEdgeFree) {
            // CASE 3. Only oppositePrevEdge needs to be wired up...            
            int incomingBoundary = prevInLoop(nextEdge);
            connectEdges (incomingBoundary, oppositePrevEdge);

         } else {
            // CASE 4. BOTH edges are attached, so we don't need to change
            // the wiring of the edges opposite to nextEdge and prevEdge.
            
            if (prevInLoop(nextEdge) != prevEdge) {
               // This occurs when (prevInLoop(nextEdge) != prevEdge).
               // We have to move the edges in-between to a different position
               // around the vertex, and we've already located the replacement point..               
               int outgoingBoundary = vertexOutgoingBoundaryEdges[i];
               int incomingBoundary = prevInLoop(outgoingBoundary);               
               int incomingOrphan = prevInLoop(nextEdge);
               int outgoingOrphan = nextInLoop(prevEdge);               
               connectEdges(incomingOrphan, outgoingBoundary);
               connectEdges(incomingBoundary, outgoingOrphan);
            }
         }
         connectEdges(prevEdge, nextEdge);
      }
      return newFaceID;
   }

   // Call this to remove a face, to undo the work of "addFace".
   // Boundary edges of this face are removed as well, along with any
   // vertices that are disconnected by the removal of this faces.
   public void removeFace(int face) {
      int firstFaceEdge = directedEdgeForFace (face);
      if (firstFaceEdge < 0) return;

      // First count the faces
      int numVertices = 1;
      int faceEdge = nextInLoop(firstFaceEdge);
      while (faceEdge != firstFaceEdge) {
         numVertices++;
         faceEdge = nextInLoop(faceEdge);
      }
      
      // We'll use this array to temporarily store edge indices
      int[] faceEdges = new int[numVertices];
      for (int i = 0; i < numVertices; ++i) {
         faceEdges[i] = faceEdge;
         
         // We examine each vertex in turn and make sure the first outgoing edge
         // listed for each vertex is not one of the edges of this face
         int vertex = startOf(faceEdge);
         if (outgoingEdgeForVertex(vertex) == faceEdge) {
            int outgoingEdge = faceEdge;
            do {
               outgoingEdge = nextAroundStart(outgoingEdge);
            } while (isBoundary(outgoingEdge) && (outgoingEdge != faceEdge));
            
            setOutgoingEdgeForVertex(vertex, outgoingEdge);
         }
         faceEdge = nextInLoop(faceEdge);
      }
      
      // First, we have to fix up the various edge links --
      // We consider each VERTEX in turn:
      for (int i = 0; i < numVertices; ++i) {
         int prevEdge = faceEdges[(i + (numVertices-1)) % numVertices];
         int nextEdge = faceEdges[i];

         int oppositePrevEdge = opposite(prevEdge);  // outgoing from vertex
         int oppositeNextEdge = opposite(nextEdge);  // incoming to vertex
         boolean prevEdgeFree = isBoundary(oppositePrevEdge);
         boolean nextEdgeFree = isBoundary(oppositeNextEdge);
         
         // There are 4 cases based on whether the prev and next edges are attached:
         if (prevEdgeFree && nextEdgeFree) {
            // CASE 1. BOTH prevEdge and nextEdge are going to be deleted, they're
            //         connected to this face and nothing else.

            // How we do this depends on whether vertex has any other edges
            if (nextInLoop(oppositeNextEdge) == oppositePrevEdge) {               
               // vertex has NO other edges.  The removal of both of these edges
               // will leave the vertex completely disconnected,
               // so the vertex is being removed as well
               int vertex = startOf(nextEdge);
               
               // [4] We're REMOVING a Vertex from the connected mesh
               setOutgoingEdgeForVertex (vertex, -1);
               this.numVertices--;
               
            } else {
               // vertex HAS other edges -- we just have to hook up the boundary loop
               int outgoingBoundary = nextInLoop(oppositeNextEdge);
               int incomingBoundary = prevInLoop(oppositePrevEdge);
               connectEdges (outgoingBoundary, incomingBoundary);
            }
         } else if (!prevEdgeFree && nextEdgeFree) {            
            // CASE 2. only prevEdge will be preserved, nextEdge is being removed.
            // prevEdge will turn into a boundary edge, it needs to be wired up...
            int outgoingBoundary = nextInLoop(oppositeNextEdge);
            connectEdges(prevEdge, outgoingBoundary);

         } else if (prevEdgeFree && !nextEdgeFree) {            
            // CASE 3. only nextEdge will be preserved, prevEdge is being removed.
            // nextEdge will turn into a boundary edge, it needs to be wired up...
            int incomingBoundary = prevInLoop(oppositePrevEdge);
            connectEdges(incomingBoundary, nextEdge);

         } else {
            // CASE 4. BOTH edges will be preserved.  We will clear their face-IDs
            // in the next loop, so they'll both become boundary edges, but they
            // are both already wired up correctly.  
         }
      }
      
      // [5] We're REMOVING a Face from the connected mesh
      setDirectedEdgeForFace(face, -1);
      this.numFaces--;
      
      // Finally, we have to remove any edges that were
      // not attached to other still-existing faces:
      for (int i = 0; i < numVertices; ++i) {
         faceEdge = faceEdges[i];
         
         // Clear the face-IDs of all the edges of this face
         setFaceOf(faceEdge, -1);
         
         // If our edge was not attached to another face, the edge has to be removed
         if (isBoundary(opposite(faceEdge))) {
            int oldEdgeID = edgeOf(faceEdge);
            
            // [6] We're REMOVING an Edge from the connected mesh
            initEdge(oldEdgeID, -1, -1);
            this.numEdges--;
         }
      }
   }
   
   
   // ========================================================================
   // Given a face-ID or a vertex-ID, these functions will count
   // the number of edges connected to either.
   // ========================================================================
   
   public int numEdgesForFace (int face) {
      int firstEdge = directedEdgeForFace(face);
      if (firstEdge < 0) return 0;
      
      int count = 0;
      int edge = firstEdge;
      do {
         count++;
         edge = nextInLoop(edge);
      } while (edge != firstEdge);
      return count;
   }
   public int numEdgesForVertex (int vertex) {
      int firstEdge = outgoingEdgeForVertex(vertex);
      if (firstEdge < 0) return 0;
      
      int count = 0;
      int edge = firstEdge;
      do {
         count++;
         edge = nextAroundStart(edge);
      } while (edge != firstEdge);
      return count;
   }

   
   // ========================================================================
   // Callers can use these ITERABLES to loop over all Vertices or Faces
   // or Edges, or around the Edges involved in a particular Face or Vertex
   // ========================================================================

   // The largest IDs in use, provided above, 
   // may not be the same as the number of vertices, edges, and faces 

   public int numVertices () { return numVertices; }
   public int numFaces ()    { return numFaces; }
   public int numEdges ()    { return numEdges; }
   
   public Iterable<Integer> vertices() {
      return elements(new SequenceWithGaps(){
         public int maxID() { return numVertexIDs(); }
         public boolean isValid(int vertex) { return isVertexConnected(vertex); }
      });
   }
   public Iterable<Integer> faces() {
      return elements(new SequenceWithGaps(){
         public int maxID() { return numFaceIDs(); }
         public boolean isValid(int face) { return isFaceConnected(face); }
      });
   }
   public Iterable<Integer> edges() {
      return elements(new SequenceWithGaps(){
         public int maxID() { return numEdgeIDs(); }
         public boolean isValid(int edge) { return isEdgeConnected(edge); }
      });
   }
 
   
   // Private utility for the iterables above
   
   private interface SequenceWithGaps {
      int maxID();
      boolean isValid(int id);
   }
   private static Iterable<Integer> elements(final SequenceWithGaps support) {
      return new Iterable<Integer>() {
         public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
               private int maxID = support.maxID();
               private int id = nextValidId(0);
               
               public boolean hasNext() {
                  return (id < maxID);
               }
               public Integer next() {
                  int result = id;
                  id = nextValidId(id+1);
                  return result;
               }
               
               private int nextValidId(int id) {
                  while ((id < maxID) && !support.isValid(id)) id++;
                  return id;
               }
            };
         }
      };
   }

   // The functions return Iterables that iterate over the edges involved
   // in a particular face or vertex:
   
   public Iterable<Integer> faceEdges(final int face) {
      return elements(new CyclicSequence(){
         public int firstID() { return directedEdgeForFace(face); }
         public int next(int id) { return nextInLoop(id); }
      });
   }
   public Iterable<Integer> outgoingEdges(final int vertex) {
      return elements(new CyclicSequence(){
         public int firstID() { return outgoingEdgeForVertex(vertex); }
         public int next(int id) { return nextAroundStart(id); }
      });
   }
   public Iterable<Integer> incomingEdges(final int vertex) {
      return elements(new CyclicSequence(){
         public int firstID() { return opposite(outgoingEdgeForVertex(vertex)); }
         public int next(int id) { return nextAroundEnd(id); }
      });
   }
   
   // Private utility for the iterables above   

   private interface CyclicSequence {
      int firstID();
      int next(int id);
   }
   private static Iterable<Integer> elements(final CyclicSequence support) {
      return new Iterable<Integer>() {
         public Iterator<Integer> iterator() {
            final int firstID = support.firstID();
            if (firstID < 0) return new Iterator<Integer>() {
               public boolean hasNext() { return false; }
               public Integer next()    { return null; }
            };
            
            return new Iterator<Integer>() {
               int id = firstID;
               boolean first = true;
               
               public boolean hasNext() {
                  return first || (id != firstID);
               }
               public Integer next() {
                  int result = id;
                  id = support.next(id);
                  return result;
               }
            }; 
         }
      };
   }

   
   // ==================================================================
   // Private functions for setting connectivity array elements...
   // ==================================================================
   
   private void setFaceOf (int directedEdge, int face) {
      directedEdgeData.array()[4 * directedEdge + 1] = face;
   }
   private void setNextInLoop (int directedEdge, int nextDirectedEdge) {
      directedEdgeData.array()[4 * directedEdge + 2] = nextDirectedEdge;
   }
   private void setPrevInLoop (int directedEdge, int prevDirectedEdge) {
      directedEdgeData.array()[4 * directedEdge + 3] = prevDirectedEdge;
   }
   private void setOutgoingEdgeForVertex (int vertex, int directedEdge) {
      vertexToDirectedEdge.array()[vertex] = directedEdge;
   }
   private void setDirectedEdgeForFace(int face, int directedEdge) {
      faceToDirectedEdge.array()[face] = directedEdge;
   }
   
   // ------------------------------------------   
   private void connectEdges (int prevEdge, int nextEdge) {
      setNextInLoop (prevEdge, nextEdge);
      setPrevInLoop (nextEdge, prevEdge);      
   }
   
   private void initEdge (int edge, int startVertex, int endVertex) {
      int[] edgeArray = directedEdgeData.array();
      edgeArray[8*edge + 0] = startVertex;  // forward-edge start-Vertex-ID
      edgeArray[8*edge + 1] = -1;           // forward-edge face-ID
      edgeArray[8*edge + 2] = -1;           // forward-edge next-edge-in-loop
      edgeArray[8*edge + 3] = -1;           // forward-edge prev-edge-in-loop
      edgeArray[8*edge + 4] = endVertex;    // reverse-edge start-Vertex-ID
      edgeArray[8*edge + 5] = -1;           // reverse-edge face-ID
      edgeArray[8*edge + 6] = -1;           // reverse-edge next-edge-in-loop
      edgeArray[8*edge + 7] = -1;           // reverse-edge prev-edge-in-loop
   }   
   

   // ==================================================================
   // MESH STORAGE
   // ==================================================================

   public static class PrimitiveIntArray {
      public PrimitiveIntArray (int elementSize) {
         this.elementSize = elementSize;
         this.numElements = 0;
         array = new int[4 * elementSize];
      }
      
      public final int elementSize;
      
      public int numElements() { return numElements;  }
      public int[] array()     { return array;        }
      
      public void setNumElements(int newNumElements) {
         int lengthNeeded = elementSize * newNumElements;
         if (array.length < lengthNeeded) {
            int newLength = array.length;
            while (newLength < lengthNeeded) newLength *= 2;
            int[] newArray = new int [newLength];
            System.arraycopy(array, 0, newArray, 0, elementSize * numElements);
            array = newArray;
         }
         numElements = newNumElements;
      }
      
      // - - - - - - - - - - - - - 
      private int[] array;
      private int numElements;
   }

   public static class PrimitiveFloatArray {
      public PrimitiveFloatArray (int elementSize) {
         this.elementSize = elementSize;
         this.numElements = 0;
         array = new float[4 * elementSize];
      }
      
      public final int elementSize;
      
      public int numElements() { return numElements;  }
      public float[] array()   { return array;        }
      
      public void setNumElements(int newNumElements) {
         int lengthNeeded = elementSize * newNumElements;
         if (array.length < lengthNeeded) {
            int newLength = array.length;
            while (newLength < lengthNeeded) newLength *= 2;
            float[] newArray = new float [newLength];
            System.arraycopy(array, 0, newArray, 0, elementSize * numElements);
            array = newArray;
         }
         numElements = newNumElements;
      }
      
      // - - - - - - - - - - - - - 
      private float[] array;
      private int numElements;
   }
   
   // -----------------------------------------------------
   // -----------------------------------------------------

   public static class IDManager {
      public IDManager() {
         numReservedIDs = 0;
         releasedIDs = new PrimitiveIntArray(1);
         intArrays = new HashSet<PrimitiveIntArray>();
         floatArrays = new HashSet<PrimitiveFloatArray>();
      }
      
      public void addIntArray(PrimitiveIntArray intArray) {
         intArray.setNumElements(numReservedIDs);
         intArrays.add(intArray);
      }
      public void removeIntArray(PrimitiveIntArray intArray) {
         intArrays.remove(intArray);
      }
      public void addFloatArray(PrimitiveFloatArray floatArray) {
         floatArray.setNumElements(numReservedIDs);
         floatArrays.add(floatArray);
      }
      public void removeFloatArray(PrimitiveFloatArray floatArray) {
         floatArrays.remove(floatArray);
      }
      
      public int getNewID() {
         int numReleasedIDs = releasedIDs.numElements();
         if (numReleasedIDs > 0) {
            int newID = releasedIDs.array()[numReleasedIDs-1];
            releasedIDs.setNumElements(numReleasedIDs-1);
            return newID;
         } else {
            numReservedIDs = numReservedIDs + 1;
            updateArrayLengths();
            return numReservedIDs-1;
         }
      }
      
      public void releaseID(int releasedID) {
         int numReleasedIDs = releasedIDs.numElements();
         releasedIDs.setNumElements(numReleasedIDs+1);
         releasedIDs.array()[numReleasedIDs] = releasedID;
      }
      
      public int getNumReservedIDs() {
         return numReservedIDs;
      }
      public void reset(int numReservedIDs) {
         this.numReservedIDs = numReservedIDs;
         updateArrayLengths();
         releasedIDs.setNumElements(0);
      }
      public void clear() {
         reset(0);
      }

      // - - - - - - - - - - - - - 
      
      private int numReservedIDs;
      private PrimitiveIntArray releasedIDs;
      private Set<PrimitiveIntArray> intArrays;
      private Set<PrimitiveFloatArray> floatArrays;

      private void updateArrayLengths() {
         for (PrimitiveIntArray intArray : intArrays) {
            intArray.setNumElements(numReservedIDs);
         }
         for (PrimitiveFloatArray floatArray : floatArrays) {
            floatArray.setNumElements(numReservedIDs);
         }
      }
   }

   // -----------------------------------------------------   
   // -----------------------------------------------------
   
   private final PrimitiveIntArray vertexToDirectedEdge;
   private final PrimitiveIntArray faceToDirectedEdge;
   private final PrimitiveIntArray directedEdgeData;
   
   private final IDManager vertexIDManager;
   private final IDManager faceIDManager;
   private final IDManager edgeIDManager;
   
   private int numVertices;
   private int numFaces;
   private int numEdges;
   
   
   public Mesh2() {
      vertexToDirectedEdge = new PrimitiveIntArray(1);
      faceToDirectedEdge = new PrimitiveIntArray(1);
      directedEdgeData = new PrimitiveIntArray(8);
      
      vertexIDManager = new IDManager();
      faceIDManager = new IDManager();
      edgeIDManager = new IDManager();
      
      vertexIDManager.addIntArray(vertexToDirectedEdge);
      faceIDManager.addIntArray(faceToDirectedEdge);
      edgeIDManager.addIntArray(directedEdgeData);
   }

   void clear() {
      numVertices = 0;
      numFaces = 0;
      numEdges = 0;
      vertexIDManager.clear();
      faceIDManager.clear();
      edgeIDManager.clear();
   }
  
   void print() {
      System.out.format("NumVertices: %d  (NumVertexIDs: %d)\n", numVertices, numVertexIDs());
      System.out.format("NumEdges: %d  (NumEdgesIDs: %d)\n", numEdges, numEdgeIDs());
      System.out.format("NumFaces: %d  (NumFacesIDs: %d)\n", numFaces, numFaceIDs());
   }

   // -------------------------------------------------------
   // Now then, the "userdata" segment...
   // -------------------------------------------------------

   enum LayerType   { PER_VERTEX, PER_EDGE, PER_FACE };
   enum ElementType { INTEGER, FLOAT };

   public static class DataLayerType {
      public final int elementCount;
      public final ElementType elementType;
      public final LayerType layerType;
      
      public DataLayerType(
            int elementCount, ElementType elementType, LayerType layerType) {
         this.layerType = layerType;
         this.elementCount = elementCount;
         this.elementType = elementType;
      }
      public boolean equals(DataLayerType other) {
         return (elementCount == other.elementCount)
              && (elementType == other.elementType)
                && (layerType == other.layerType);
      }
      public int hashCode() {
         return Objects.hash(elementCount, elementType, layerType);
      }
   }

   // --------------------------------------------------------
   /*   
   public interface ElementData {
      public int size();
   }
   public interface ElementIntData extends ElementData {
      public int get(int i);
   }
   public interface MutableElementIntData extends ElementIntData {
      public void set(int i, int v);
   }
   public interface ElementFloatData extends ElementData {
      public float get(int i);
   }
   public interface MutableElementFloatData extends ElementFloatData {
      public void set(int i, float v);
   }
   */

   public static abstract class DataLayer {
      public final String name;
      public final DataLayerType type;
      
      private DataLayer(String name, DataLayerType type) {
         this.name = name;
         this.type = type;
      }
   }

   public static abstract class IntDataLayer extends DataLayer {
      private final Mesh2 mesh;
      private final PrimitiveIntArray data;
      
      private IntDataLayer(Mesh2 mesh, String name, DataLayerType type) {
         super (name, type);
         if (type.elementType != ElementType.INTEGER) {
            throw new RuntimeException();
         }
         this.mesh = mesh;
         this.data = new PrimitiveIntArray(type.elementCount);
      }
//      public MutableElementIntData at(int i) {
//         final int offset = i * type.elementCount;
//         return new MutableElementIntData() {
//            public int get(int i) {
//               return 0;
//            }
//            public int size() {
//               return 0;
//            }
//            public void set(int i, int v) {
//            }
//         };
//      }
   }
   
   
   // #############################################################################################
   // Specific Models for Testing
   // #############################################################################################
   
   static class SavedModel {
      public static class Triangle {
         public final int v0,v1,v2;
         public Triangle(int v0, int v1, int v2) {
            this.v0=v0;
            this.v1=v1;
            this.v2=v2;
         }
      }
      public final ArrayList<Vector3> vertexPositions = new ArrayList<Vector3> ();
      public final ArrayList<Triangle> faceIds = new ArrayList<Triangle>();
      
      public void addVertex(Vector3 v) {
         vertexPositions.add(v); 
      }
      public void addFace(int v0, int v1, int v2) { 
         faceIds.add(new Triangle(v0,v1,v2)); 
      }
   }
   
   private String loadStringFileFromCurrentPackage(String fileName){
      InputStream stream = this.getClass().getResourceAsStream(fileName);
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      StringBuilder strBuilder = new StringBuilder();
      try {
         String line = reader.readLine();
         // get text from file, line per line
         while(line != null){
            strBuilder.append(line + "\n");
            line = reader.readLine();  
         }
         // close resources
         reader.close();
         stream.close();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return strBuilder.toString();
   }  
   public static SavedModel loadFromString(String serialized) {
      SavedModel result = new SavedModel();

      for (String line : serialized.split("\n")) {
         if (line.charAt(0) == '#') {
            continue;
         }
         if (line.charAt(0) == 'v') {
            String restOfLine = line.substring(2);
            String[] pieces = restOfLine.split(" ");
            if (pieces.length == 3) {
               result.addVertex(new Vector3(Float.valueOf(pieces[0]), Float.valueOf(pieces[1]), Float.valueOf(pieces[2])).times(60.0f));
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
               result.addFace(v0,v1,v2);
            }
            continue;
         }
      }
      return result;
   }   


   // -----------------------------------------------------------
   // MAIN
   // -----------------------------------------------------------

   public static void main(String[] args) {
      System.out.format("Hello from Mesh2\n");
      
      Mesh2 m2 = new Mesh2();
      Mesh m1 = new Mesh();
      SavedModel model = loadFromString(m2.loadStringFileFromCurrentPackage("bunny.obj"));
      
      PrimitiveFloatArray positions = new PrimitiveFloatArray(3);
      m2.vertexIDManager.addFloatArray(positions);
      
      /*
      int v0 = m.newVertexID();
      int v1 = m.newVertexID();
      int v2 = m.newVertexID();
      
      m.print();
      System.out.format("-----\n");
      
      int f0 = m.addFace(v0,v1,v2);
      
      m.print();
      System.out.format("-----\n");
      
      m.removeFace(f0);
      
      m.print();
      */
      
      long start1 = System.currentTimeMillis();
      { for (Vector3 position : model.vertexPositions) {
           Mesh.Vertex v = m1.addVertex();
           v.setPosition(position);
        }
        for (SavedModel.Triangle t : model.faceIds) {
           m1.addTriangle(m1.vertices.get(t.v0), 
                          m1.vertices.get(t.v1),
                          m1.vertices.get(t.v2));
        }
      }
      long end1 = System.currentTimeMillis();
      System.out.format("Mesh1 init with %d vertices, %d faces, %d edges in .... %d ms\n",
            m1.vertices.size(), m1.triangles.size(), m1.edges.size(),
            end1-start1);

      long start2 = System.currentTimeMillis();
      { for (Vector3 position : model.vertexPositions) {
           int v = m2.newVertexID();
           
           float[] positionsArray = positions.array();
           positionsArray[3*v + 0] = position.x;
           positionsArray[3*v + 1] = position.y;
           positionsArray[3*v + 2] = position.z;
        }
        for (SavedModel.Triangle t : model.faceIds) {
           m2.addFace(t.v0, t.v1, t.v2);
        }
      }
      long end2 = System.currentTimeMillis();
      System.out.format("Mesh2 init with %d vertices, %d faces, %d edges in .... %d ms\n",
            m2.numVertices(), m2.numFaces(), m2.numEdges(),
            end2-start2);


      m2.print();
      System.out.format("-----\n");
   }
   
   
}

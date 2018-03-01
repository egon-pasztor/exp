package com.generic.base;

public class Mesh2 {

   // A Mesh instance keeps track of connectivity information about
   // a set of Vertices, Faces, and Edges
   public int numVertices() { return numVertices; }
   public int numFaces()    { return numFaces;    }
   public int numEdges()    { return numEdges;    }
   
   // Each edge can be considered two directed-edges,
   // so numDirectedEdges() is always 2x numEdges()
   public int numDirectedEdges() { return 2*numEdges; }
   
   // Every directedEdge is part of a loop, either a boundary-loop or one encircling a Face.
   // Call this to get a count of how many directedEdges are in boundary-loops.
   public int numBoundaryEdges() { return numBoundaryEdges; }
   
   // Each directedEdge has an id between 0 and (numDirectedEdges()-1).
   // Given a directedEdge-id, you can retrieve the directedEdge-id
   // pointing the opposite way
   public int opposite (int directedEdge) {
      return directedEdge ^ 1;
   }
   
   // Each vertex has an id between 0 and (numVertices()-1).
   // Given a directedEdge-id, you can retrieve the vertex-id
   // that's at the START or END of the directedEdge.
   public int startOf (int directedEdge) {
      return directedEdgeData[sizeOfDirectedEdgeData * directedEdge];
   }
   public int endOf (int directedEdge) {
      return startOf(opposite(directedEdge));
   }

   // Every directedEdge is part of a loop, either a boundary-loop or one encircling a Face.
   // Given a directedEdge-id, you can retrieve the id of the directedEdge
   // that's next, or previous, going around its loop.
   public int nextDirectedEdge (int directedEdge) {
      return directedEdgeData[sizeOfDirectedEdgeData * directedEdge + 2];
   }
   public int prevDirectedEdge (int directedEdge) {
      return directedEdgeData[sizeOfDirectedEdgeData * directedEdge + 3];
   }
   
   // Given a directedEdge-id, you can retrieve the id of the directedEdge
   // that's counterclockwise, or clockwise, when iterating around
   // the outgoing edges of the START vertex
   public int ccwAroundStart (int directedEdge) {
      return opposite(prevDirectedEdge(directedEdge));
   }
   public int cwAroundStart (int directedEdge) {
      return nextDirectedEdge(opposite(directedEdge));
   }
      
   // Given a directedEdge-id, you can retrieve the id of the directedEdge
   // that's counterclockwise, or clockwise, when iterating around
   // the incoming edges of the END vertex
   public int ccwAroundEnd (int directedEdge) {
      return prevDirectedEdge(opposite(directedEdge));
   }
   public int cwAroundEnd (int directedEdge) {
      return opposite(nextDirectedEdge(directedEdge));
   }
   
   // Each edge has an id between 0 and (numEdges()-1).
   // Given a directedEdge-id, you can retrieve the id of the edge it's part of
   public int edgeOf (int directedEdge) {
      return directedEdge / 2;
   }
   // Given an edge-id, you can retrieve the id of its forward or reverse directedEdges
   public int edgeToForwardDirectedEdge (int edge) {
      return 2 * edge;
   }
   public int edgeToReverseDirectedEdge (int edge) {
      return 2 * edge + 1;
   }   

   // Every directedEdge is part of a loop, either a boundary-loop or one encircling a Face.
   // Given a directedEdge-id, call this to determine if it's part of a boundary loop.
   public boolean isBoundary (int directedEdge) {
      int faceOrBoundaryIndex = directedEdgeData[sizeOfDirectedEdgeData * directedEdge + 1];
      return ((faceOrBoundaryIndex & 0x8000) != 0);
   }
   
   // Each face has an id between 0 and (numFace()-1).
   // Given a directedEdge-id that's part of a loop encircling a Face,
   // you can get the id of the Face:
   public int faceOf (int directedEdge) {
      int faceOrBoundaryIndex = directedEdgeData[sizeOfDirectedEdgeData * directedEdge + 1];
      return ((faceOrBoundaryIndex & 0x8000) == 0) ? (faceOrBoundaryIndex & 0x7fff) : -1;
   }
   
   // Each boundaryEdge has an id between 0 and (numBoundaryEdges()-1).
   // Given a directedEdge-id that's part of a boundary-loop, call this to convert from
   // the directedEdge-id to a boundaryEdge-id:
   public int boundaryEdgeOf (int directedEdge) {
      int faceOrBoundaryIndex = directedEdgeData[sizeOfDirectedEdgeData * directedEdge + 1];
      return ((faceOrBoundaryIndex & 0x8000) != 0) ? (faceOrBoundaryIndex & 0x7fff) : -1;
   }
   
   // Given a vertex-id, you can retrieve the id of an "outgoing" directedEdge
   public int vertexToDirectedEdge (int vertex) {
      return vertexToDirectedEdge[vertex];
   }   
   // Given a face-id, you can retrieve the id of a directedEdge that's part
   // of the loop encircling it:
   public int faceToDirectedEdge (int face) {
      return faceToDirectedEdge[face];
   }
   // Given a boundaryEdge-id, you can retrieve the id of the directedEdge mapped to it:
   public int boundaryEdgeToDirectedEdge (int boundaryEdge) {
      return boundaryEdgeToDirectedEdge[boundaryEdge];
   }   

   // -----------------------------------------------------   
   // A Mesh can be built up iteratively by adding disconnected vertices,
   // then connecting them by adding faces.
   // -----------------------------------------------------

   // Call this to add a new vertex:
   public int addVertex() {   
      // Increase vertex array size,  (fill new element with -1)
      // Increase the sizes of any vertex-based dataLayers
      return -1;
   }
   
   // Call this to connect vertices to form a new Face:
   public int addFace(int... vertices) {
      
      
      return -1;
   }

   public void removeFace(int face) {
   }
   
   // -----------------------------------------------------   
   // -----------------------------------------------------
   
   
   // Given a vertex-id, this function will count the number of 
   // outgoing edges.  This is the "valence" of the vertex.
   public int countOutgoingEdges (int vertex) {
      int firstOutgoingDirectedEdge = vertexToDirectedEdge(vertex);
      if (firstOutgoingDirectedEdge < 0) return 0;
      
      int count = 1;
      int outgoingDirectedEdge = firstOutgoingDirectedEdge;
      while (true) {
         // NOTE: in a properly built Mesh there's no danger of this looping
         // around forever, without returning to the firstOutputDirectedEdge,
         // but should we explicitly detect and break out of the infinite loop
         // that could occur here if the Mesh were invalid?
         //
         // It's hard to know how to handle inconsistencies.  If we check for
         // the possibility of an infinite loop, should we also check that the
         // "ccwAroundStart" function always returns a valid directedEdge?
         // Should we check the return values of *every* function for "validity"?
         
         outgoingDirectedEdge = ccwAroundStart(outgoingDirectedEdge);
         if (outgoingDirectedEdge == firstOutgoingDirectedEdge) return count;
         count++;
      }
   }
   
   // Given a face-id, this function will count the number of 
   // edges going around it.  This is the number of sides of the face.
   
   public int countEdgesAroundFace (int face) {
      int firstDirectedEdge = faceToDirectedEdge(face);
      
      int count = 1;
      int directedEdge = firstDirectedEdge;
      while (true) {
         directedEdge = nextDirectedEdge(directedEdge);
         if (directedEdge == firstDirectedEdge) return count;
         count++;
      }
   }

   // ---------------------------------------------------------------------------------
   // ---------------------------------------------------------------------------------
   
   private int numVertices;
   private int numFaces;
   private int numEdges;
   private int numBoundaryEdges;
   
   private int[] vertexToDirectedEdge;
   private int[] faceToDirectedEdge;
   private int[] boundaryEdgeToDirectedEdge;
   
   private static final int sizeOfDirectedEdgeData = 4;
   private int[] directedEdgeData;
   
   // --------------------------------------------
   // adding a triangular face 
   //    will either add 3 boundary edges,  (adding a new face in empty space)
   //             or add 1 boundary edge,   (if 1 side is connected to an existing face)
   //          or remove 1 boundary edge,   (if 2 sides are connected to existing faces)
   //          or remove 3 boundary edges,  (filling a triangular hole in an existing mesh)
   //
   //    it will either add 3 new edges     (adding a new face in empty space)
   //                or add 2 new edges     (if 1 side is connected to an existing face)
   //                or add 1 new edges     (if 2 sides is connected to an existing face)
   //               or add no new edges     (filling a triangular hole in an existing mesh)
   //
   // if 3 new edges are created, that's 6 new directedEdges --
   //          3 of which point to the "new face"
   //      and 3 of which have "boundary edge ids"
   // 
   // if 2 new edges are created, (the triangle has 1 side adjacent to an existing face)
   //      then 1 existing directedEdge (which has a boundary-id) changes to a face id.
   //          (that makes a boundary-edge-id "unused" -- 
   //          and boundaryEdgeToDirectedEdge[ boundary_edge_id ] should be cleared,
   //              (set to -1?), it should no longer point back to the directedEdge got changed.
   //
   //      creating 2 new edges means increasing
   //      the directedEdgeData array by (4 *  sizeOfDirectedEdgeData),
   //      that is, creating 4 new directedEdges which must be "wired in to existing directedEdges.
   //         (the "inner two" directedEdges connect to the "existing" directedEdge that
   //             was "transformed" in the previous step)
   //          the "outer two" directedEdges are connected to the boundary-loop that was
   //             left broken by the removal of the "existing" directedEdge 
   //             was "transformed" in the previous step)
   //
   //
   //      the "outer" two directedEdges both need "boundary-edge-ids"..
   //
   //      one of the "new" boundary-edges can get the same id as the recently deleted one..
   //      
   
   enum Layer { VERTEX, EDGE, FACE, DIRECTED_EDGE, BOUNDARY_EDGE };
   
   
   // so when we need a new BoundaryEdge id, what do we do?
   
   
   public static class BlockArray {
      public final int elementSize;
      
      
      public BlockArray(int elementSize, int initialCapacity) {
         this.elementSize = elementSize;
         data = new int[elementSize * initialCapacity];
      }
      private int numElements;
      private int currentCapacity;
      private int[] data;  // data.length = elementSize * currentCapacity;
      private int[] deletedIndices;
   }
   
   
}

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
   
   
   // What if we DON'T KEEP TRACK of boundaryEdges?
   // what then?
   
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
   public int boundaryToDirectedEdge (int boundaryEdge) {
      return boundaryToDirectedEdge[boundaryEdge];
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
   private int[] boundaryToDirectedEdge;   
   private int[] directedEdgeData;
   
   
   public static class PrimitiveIntArray {
      public PrimitiveIntArray () {
         array = new int[10];
         size = 0;
      }
      
      public int size()     { return size;  }
      public int[] array()  { return array; }
      
      public void setSize(int newSize) {
         if (array.length < newSize) {
            int newCapacity = array.length;
            while (newCapacity < newSize) newCapacity *= 2;
            int[] newArray = new int [newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
         }
         size = newSize;
      }
      
      private int[] array;
      private int size;
   }

   private PrimitiveIntArray vertexToDirectedEdge_;
   private PrimitiveIntArray faceToDirectedEdge_;
   private PrimitiveIntArray boundaryToDirectedEdge_;

   private static final int sizeOfDirectedEdgeData = 4;
   private PrimitiveIntArray directedEdgeData_;
   
   
   // -------------------------------------------------------
   // So, we need separate arrays keeping track of the "deleted ids"
   // among the vertex/face/edge/boundary lists.
   // -------------------------------------------------------
   private PrimitiveIntArray deletedEdgeIndices;
   private PrimitiveIntArray deletedVertexIndices;
   private PrimitiveIntArray deletedFaceIndices;
   private PrimitiveIntArray deletedBoundaryIndices;
   

   
   // -------------------------------------------------------
   // Now then, the "userdata" segment...
   // -------------------------------------------------------
   
   enum LayerType   { PER_VERTEX, PER_EDGE, PER_FACE, PER_BOUNDARY };
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
   }

   // now then, the addFace/removeFace/addVertex/removeVertex methods
   // make calls to "reserve" and "release" IDs for vertices, edges, faces, boundaries
   // For each of these four, we want to ask "getNewID" and "releaseID"....
   // and also, there is an "operation" we call "compacting", that causes
   // "moves" from upper-indices into the individual spaces..
   //
   // this is like an "id manager" ..
   //
   public static class IDManager {
      private int numReservedIDs;
      private PrimitiveIntArray releasedIDs;

      public IDManager() {
         numReservedIDs = 0;
         releasedIDs = new PrimitiveIntArray();
      }
      
      int getNewID() {
         int numReleasedIDs = releasedIDs.size();
         if (numReleasedIDs > 0) {
            int newID = releasedIDs.array()[numReleasedIDs-1];
            releasedIDs.setSize(numReleasedIDs-1);
            return newID;
         } else {

            // "Increase numReservedIDs??"
            // TODO -- tell all dataLayers to make sure there's space?
            numReservedIDs = numReservedIDs + 1;
            
            return numReservedIDs-1;
         }
      }
      
      void releaseID(int releasedID) {
         int numReleasedIDs = releasedIDs.size();
         releasedIDs.setSize(numReleasedIDs+1);
         releasedIDs.array()[numReleasedIDs] = releasedID;
         
         // --------------------
         // so instead of the above, with an impossible "compact" function to implement,
         // we have to do the remove motion HERE...
         // that is,
         //
         // if (releasedID is not numReservedID-1),
         //    
         //
         
      }
      
      void compact() {
         // For each releasedID,
         // ??? yeah well, what?  do we have to walk all the way over each ID?
         // clearly, if we've delayed.. 
         
      }
      
      
   }
   //
   // 
   
   
   
   
   
   // imagine that the user wants to add a structure with 3-floats-per-vertex...
   // then they call:
   //
   // DataLayer addDataLayer(String name, DataLayerType type);
   //
   // and we could have "getDataLayer(String name)" and "removeDataLayer(String name)" as well...
   //
   // but what's a DataLayer?
   //
   //    well, it knows its name, and it knows the Mesh it's part of...
   //    and for a given elementID, it should provide "read-write" access to an "elementCount" x "elementType" region of data...
   //
   // IF elementType is INTEGER, the DataLayer holds a PrimitiveIntArray
   //    whose size is pegged to elementCount x mesh.numVertices(),
   //    but, come on ... 
   
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
   
   
   public static abstract class DataLayer {
      public final Mesh2 mesh;
      public final String name;
      public final DataLayerType type;
      
      private DataLayer(Mesh2 mesh, String name, DataLayerType type) {
         this.mesh = mesh;
         this.name = name;
         this.type = type;
      }
      public int size() {
         switch (type.layerType) {
            case PER_VERTEX   : return mesh.numVertices();
            case PER_EDGE     : return mesh.numEdges();
            case PER_FACE     : return mesh.numFaces();
            case PER_BOUNDARY : return mesh.numBoundaryEdges();
         }
         throw new RuntimeException();
      }
      public abstract ElementData at(int i);
   }
   
   public static abstract class IntDataLayer extends DataLayer {
      private IntDataLayer(Mesh2 mesh, String name, DataLayerType type) {
         super (mesh, name, type);
         if (type.elementType != ElementType.INTEGER) {
            throw new RuntimeException();
         }
      }
      
      public MutableElementIntData at(int i) {
         final int offset = i * type.elementCount;
         return new MutableElementIntData() {
            public int get(int i) {
               return 0;
            }
            public int size() {
               return 0;
            }
            public void set(int i, int v) {
            }
         };
      }
   }
   
   
   
   
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

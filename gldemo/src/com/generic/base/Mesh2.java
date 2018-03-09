package com.generic.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Mesh2 {

   // -----------------------------------------------------   
   // A Mesh keeps track of connectivity between
   // a number of Vertices, Faces, and Edges
   // -----------------------------------------------------   
   
   // Each vertex, face, and edge has an integer ID.
   // This tells you how many have been allocated:
   public int numVertexIDs() { return vertexIDManager.getNumReservedIDs(); }
   public int numEdgeIDs()   { return edgeIDManager.getNumReservedIDs();   }
   public int numFaceIDs()   { return faceIDManager.getNumReservedIDs();   }

   
   // Each edge is associated with two directed-edges pointing in opposite directions.
   // Given an edge-ID, you can find the IDs of its directed-edges
   // by doubling and optionally adding one:
   public int edgeToForwardDirectedEdge (int edge) {
      return 2 * edge;
   }
   public int edgeToReverseDirectedEdge (int edge) {
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

   // Every directedEdge is part of a loop --
   // (either a loop encircling a Face or a boundary-loop)
   // If it encircles a Face, you can get its face-ID, otherwise -1
   public int faceOf (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 1];
   }
   // A directedEdge that's part of a boundary-loop
   // doesn't have a Face associated with it, so faceOf returns -1
   public boolean isBoundary (int directedEdge) {
      return faceOf(directedEdge) == -1;
   }

   // Every directedEdge is part of a loop, and you can call these methods
   // to go to the next or previous directedEdge along a loop:
   public int nextInLoop (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 2];
   }
   public int prevInLoop (int directedEdge) {
      return directedEdgeData.array()[4 * directedEdge + 3];
   }


   // Given a directedEdge-id, you can also traverse a loop of directedEdges
   // by looping over the outgoing edges of the START vertex
   public int nextAroundStart (int directedEdge) {
      return opposite(prevInLoop(directedEdge));
   }
   public int prevAroundStart (int directedEdge) {
      return nextInLoop(opposite(directedEdge));
   }
      
   // Given a directedEdge-id, you can also traverse a loop of directedEdges
   // by looping over the incoming edges of the END vertex
   public int nextAroundEnd (int directedEdge) {
      return prevInLoop(opposite(directedEdge));
   }
   public int prevAroundEnd (int directedEdge) {
      return opposite(nextInLoop(directedEdge));
   }
   

   // Given a vertex-ID, you can retrieve the ID of an outgoing directedEdge:
   public int vertexToDirectedEdge (int vertex) {
      return vertexToDirectedEdge.array()[vertex];
   }
   // Given a face-ID, you can retrieve the ID of a directedEdge
   // that's part of the loop encircling it.
   public int faceToDirectedEdge (int face) {
      return faceToDirectedEdge.array()[face];
   }
   

   // -----------------------------------------------------   
   // A Mesh can be built up iteratively by adding disconnected,
   // vertices then connecting them by adding faces.
   // -----------------------------------------------------

   // Call this to add a new vertex.
   // A new vertex-ID is returned, but it's not connected to anything,
   // (that is, "vertexToDirectedEdge" will return -1)
   public int addVertex() {
      return vertexIDManager.getNewID();
   }
   
   // Call this to connect vertices to form a new face.
   // A new face-ID is returned, and it'll be connected to a new loop
   // of directedEdges going around the provided vertices.
   public int addFace(int... vertices) {
      int numVertices = vertices.length;
      
      // For each edge of this new face, we want to know if the edge
      // connects this new face to an existing face or not.
      int[] boundaryDirectedEdges = new int[numVertices];
      for (int i = 0; i < numVertices; ++i) {
         boundaryDirectedEdges[i] = -1;
         
         int startVertex = vertices[i];
         int endVertex = vertices[(i + 1) % numVertices];
          
         // Does "startVertex" have any edges already connected to it?
         // If so, are any of them boundary edges?
         boolean anyOutgoingEdgesfromStart = false;
         boolean anyOutgoingBoundaryEdgesFromStart = false;

         int firstEdgeOutgoingFromStart = vertexToDirectedEdge(startVertex);
         if (firstEdgeOutgoingFromStart != -1) {
            anyOutgoingEdgesfromStart = true;
             
            // Examine each pre-existing edge outgoing from start
            int edgeOutgoingFromStart = firstEdgeOutgoingFromStart;
            do {
               if (endOf(edgeOutgoingFromStart) == endVertex) {
                  // We've discovered there's already a directedEdge going
                  // from startVertex to endVertex.  This must be on the boundary,
                  // otherwise a face with this directed edge already exists
                  if (!isBoundary(edgeOutgoingFromStart)) {
                     throw new RuntimeException(String.format(
                        "DirectedEdge from %d to %d already exists",
                        startVertex, endVertex));
                  }
                  
                  boundaryDirectedEdges[i] = edgeOutgoingFromStart;
                  anyOutgoingBoundaryEdgesFromStart = true;
                  break;

               } else {
                  // This edge connects startVertex to some other endpoint
                  // that's not endVertex.  Let's note if any of these edges
                  // are boundaries:
                  if (isBoundary(edgeOutgoingFromStart) ||
                      isBoundary(opposite(edgeOutgoingFromStart))) {
                     anyOutgoingBoundaryEdgesFromStart = true;
                  }
               }
               
               edgeOutgoingFromStart = nextAroundStart(edgeOutgoingFromStart);               
            } while (edgeOutgoingFromStart != firstEdgeOutgoingFromStart);
         }
         
         // If startVertex had any edges attached to it, then at least one 
         // of those must have been a boundary, since we can't create a face
         // with a corner that's already completely surrounded by faces.
         if (anyOutgoingEdgesfromStart && !anyOutgoingBoundaryEdgesFromStart) {
            throw new RuntimeException(String.format(
               "Vertex %d already completely surrounded by faces",
               startVertex));
         }
      }
      
      // Check if two adjacent edges of the new face connect to existing faces
      for (int i = 0; i < numVertices; ++i) {
         int firstEdge = boundaryDirectedEdges[(i + (numVertices-1)) % numVertices];
         int secondEdge = boundaryDirectedEdges[i];
         
         if ((firstEdge != -1) && (secondEdge != -1)) {
            int vertex = startOf(secondEdge);
            
            // Both 'firstEdge' and 'secondEdge' are directedEdges
            // that are part of boundary-loops, but are they adjacent edges
            // in the same boundary-loop?            
            if (prevInLoop(secondEdge) != firstEdge) {
             
               // No, apparently there are other edges touching this vertex
               // that lie between 'firstEdge' and 'secondEdge'.
               // These other edges will have to be moved to a different position
               // around the vertex, which is not a problem so long as there's
               // another pair of boundary edges they can be placed between:
               
               boolean foundAlternateBoundary = false;
               int edgeOutgoingFromStart = nextAroundStart(opposite(firstEdge));
               while (edgeOutgoingFromStart != secondEdge) {
                  if (isBoundary(edgeOutgoingFromStart)) {
                     foundAlternateBoundary = true;
                     break;
                  }
                  edgeOutgoingFromStart = nextAroundStart(edgeOutgoingFromStart);
               }
               
               if (!foundAlternateBoundary) {
                  throw new RuntimeException(String.format(
                     "Vertex %d has faces that block the addition of this face",
                     vertex));
               }
            }            
         }
      }
      
      // Okay the CHECKS are done and we've confirmed this face can be
      // added without messing up the mesh.  At this point, we're
      // committed to modifying the mesh:
      int newFaceID = faceIDManager.getNewID();
      
      // For each edge of this new face, if we DIDNT find a pre-existing boundary
      // edge in the scan above, we have to CREATE the edge here.
      for (int i = 0; i < numVertices; ++i) {
         int directedEdge = boundaryDirectedEdges[i];
         if (directedEdge == -1) {
            
            
         } else {
            
            
         }
      }      
      
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
   
   
   
   private void setStartOf (int directedEdge, int vertex) {
      directedEdgeData.array()[4 * directedEdge] = vertex;
   }
   private void setFaceOf (int directedEdge, int face) {
      directedEdgeData.array()[4 * directedEdge + 1] = face;
   }
   private void setNextInLoop (int directedEdge, int nextDirectedEdge) {
      directedEdgeData.array()[4 * directedEdge + 2] = nextDirectedEdge;
   }
   private void setPrevInLoop (int directedEdge, int prevDirectedEdge) {
      directedEdgeData.array()[4 * directedEdge + 3] = prevDirectedEdge;
   }
   private void setDirectedEdgeForVertex (int vertex, int directedEdge) {
      vertexToDirectedEdge.array()[vertex] = directedEdge;
   }
   private void setDirectedEdgeForFace(int face, int directedEdge) {
      faceToDirectedEdge.array()[face] = directedEdge;
   }
   
   // -----------------------------------------------------   
   // -----------------------------------------------------

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

   
   
   
   
   
   
   
   
   
   
   
   public int numVertices() { return numVertices; }
   public int numFaces()    { return numFaces;    }
   public int numEdges()    { return numEdges;    }
   
   private int numVertices;
   private int numFaces;
   private int numEdges;
   
   
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
         
         outgoingDirectedEdge = nextAroundStart(outgoingDirectedEdge);
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
         directedEdge = nextInLoop(directedEdge);
         if (directedEdge == firstDirectedEdge) return count;
         count++;
      }
   }

   // ---------------------------------------------------------------------------------
   // ---------------------------------------------------------------------------------
   
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
   }

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

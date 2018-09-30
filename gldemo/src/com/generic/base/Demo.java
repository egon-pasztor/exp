package com.generic.base;

import com.generic.base.Algebra.Vector3;
import com.sun.prism.Mesh;

public class Demo {

   private final Platform platform;
   
   private final Graphics3D graphics;
   private final Mesh2 mesh;
   
   private Camera camera;

   
   // -------------------------------------------------
   // Constructor
   // -------------------------------------------------
   public Demo(Platform platform) {
      this.platform = platform;
      
      // Create the Mesh we want to display
      this.mesh = initMesh();
      
      // Access Root window and get its size
      Platform.Widget.Renderer3D window = platform.root3D();
      Image.Size windowSize = window.size();
      platform.log("Platform has a %d x %d root-window", windowSize.width, windowSize.height);
      
      // Define the camera through which we're going to view the mesh,
      // note that this requires the windowSize.
      camera = new Camera(windowSize,
            new Vector3(0.0f, 0.0f, 0.0f),   // look-at
            new Vector3(0.0f, 0.0f, 18.0f),  // camera-pos
            new Vector3(0.0f, 1.0f, 0.0f),   // camera-up
            53.13f/2.0f);
      
      // Now we're going to setup a "Graphics3D" object to represent
      // a rendering of this mesh from the particular angle.
      //
      // Later we'll use something like a MouseListener to listen for grabs
      // and drags, and use these to update the Camera.   When we do this,
      // we'll separate the "render" method into an initial step to set up
      // "shader/vertexBuffer" objects (that's view-independent),
      // and then a second method that writes the small "command-array" portion 
      // (which has to be re-executed for each change in Camera).
      //
      this.graphics = meshToGraphics3D (mesh, camera);

      // Set the graphics3D object of the root window.
      // Eventually we'll require a second call, to "show" the root window
      
      this.graphics = new Graphics3D();     
      
   }
   
   // ----------------------------------------------------------
   // Creating the Mesh
   // ----------------------------------------------------------
   private Mesh2 initMesh() {
      // Currently we're "loading" the bunny from "bunny.obj" but that requires the
      // "bunny.obj" file to be .. where, exactly?
      Mesh2 mesh = Mesh2.loadMesh("bunny.obj");
      platform.log("Mesh loaded with %d vertices", mesh.numVertices());
      return mesh;
   }
   
   // ----------------------------------------------------------
   // Generate a Graphics3D given a Mesh2 and a Camera
   // ----------------------------------------------------------

   private static Graphics3D meshToGraphics3D (Mesh2 mesh, Camera camera) {
      Graphics3D graphics = new Graphics3D();
      int ids = 0;

      // Let's say we'd like to display ONE Mesh, from ONE angle.
      // We will need the Graphics3D object to contain
      //
      // ------------------------------------
      // one shader ("FlatBordered")
      // ------------------------------------
      Graphics3D.Shader shader = new Graphics3D.Shader.FlatBordered(0.1f);
      int shaderId = ids++;
      graphics.shaders.put(shaderId, shader);
      
      // ------------------------------------
      // three vertexBuffers for "positions", "normals", "baryCoords"
      // ------------------------------------
      Data.Array positions  = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      Data.Array normals    = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      Data.Array baryCoords = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      
      {  // Fill the "positions"/"normals"/"baryCoords" vertexBuffers (for the Graphics3D)
         //    (a list of triangles: a three-float triple for each triangle)
         //

         float[] positionsArray  = ((Data.Array.Floats)positions).array();
         float[] normalsArray    = ((Data.Array.Floats)normals).array();
         float[] baryCoordsArray = ((Data.Array.Floats)baryCoords).array();
         
         // from the "positions" dataLayer (in the Mesh)
         // plus the connectivity information (in the Mesh)
         //    (the "positions" dataLayer provides three-floats per vertex,
         //     the connectivity information groups these into faces.
         //     We have to produce triangles here.)
         
         Mesh2.DataLayer meshPositions = mesh.dataLayer("positions",
               Mesh2.DataLayer.Type.THREE_FLOATS_PER_VERTEX);
         if (meshPositions == null) {
            throw new RuntimeException("Failed to find position dataLayer");
         }
         float[] meshPositionsArray = ((Data.Array.Floats)(meshPositions.data)).array();
         
         // Now then, "mesh2.faces" lets us iterate over the mesh faces
         for (Integer faceID : mesh.faces()) {
            
            int edge0 = mesh.directedEdgeForFace(faceID);
            int edge1 = mesh.nextInLoop(edge0);
            int edge2 = mesh.nextInLoop(edge1);
            
            // Now then, if mesh.nextInLoop(edge2) is edge0, then we've got a triangle:
            if (mesh.nextInLoop(edge2) == edge0) {
               
               Vector3 vertex0Pos = Vector3.fromFloatArray(positionsArray, 3 * mesh.startOf(edge0));
               Vector3 vertex1Pos = Vector3.fromFloatArray(positionsArray, 3 * mesh.startOf(edge0));
               Vector3 vertex2Pos = Vector3.fromFloatArray(positionsArray, 3 * mesh.startOf(edge0));
            
               outputArray[p++] = meshPositions[3*vertex0 + 0];
               outputArray[p++] = meshPositions[3*vertex0 + 1];
               outputArray[p++] = meshPositions[3*vertex0 + 2];
               
               outputArray[p++] = positionsArray[3*vertex1 + 0];
               outputArray[p++] = positionsArray[3*vertex1 + 1];
               outputArray[p++] = positionsArray[3*vertex1 + 2];
               outputArray[p++] = 1;
               
               outputArray[p++] = positionsArray[3*vertex2 + 0];
               outputArray[p++] = positionsArray[3*vertex2 + 1];
               outputArray[p++] = positionsArray[3*vertex2 + 2];
               outputArray[p++] = 1;

            }
      }
      
      int p = 0;
      
      for (Integer faceID : mesh2.faces()) {
         int edge0 = mesh2.directedEdgeForFace(faceID);
         int edge1 = mesh2.nextInLoop(edge0);
         int edge2 = mesh2.nextInLoop(edge1);
         //System.out.format("Edges are %d,%d,%d, %d, %d, %d, %d\n",  edge0,edge1,edge2,edge3,edge4,edge5,edge6,edge7);
         if (mesh2.nextInLoop(edge2) == edge0) {
            //System.out.format("Printing triangle consisting of edge %d,%d,%d\n",  edge0,edge1,edge2);
            // Confirmed, this face is a TRIANGLE
            //
            int vertex0 = mesh2.startOf(edge0);
            int vertex1 = mesh2.startOf(edge1);
            int vertex2 = mesh2.startOf(edge2);
            
            Vector3 vertex0Pos = Vector3.fromFloatArray(positionsArray, 3*vertex0);
            Vector3 vertex1Pos = Vector3.fromFloatArray(positionsArray, 3*vertex1);
            Vector3 vertex2Pos = Vector3.fromFloatArray(positionsArray, 3*vertex2);
            //System.out.format("Printing triangle consisting of edge %d,%d,%d -- vertices %d,%d,%d -- positions [%s,%s,%s]\n",
            //      edge0,edge1,edge2,
            //      vertex0,vertex1,vertex2,
            //      vertex0Pos.toString(), vertex1Pos.toString(), vertex2Pos.toString());
            
            outputArray[p++] = positionsArray[3*vertex0 + 0];
            outputArray[p++] = positionsArray[3*vertex0 + 1];
            outputArray[p++] = positionsArray[3*vertex0 + 2];
            outputArray[p++] = 1;
            
            outputArray[p++] = positionsArray[3*vertex1 + 0];
            outputArray[p++] = positionsArray[3*vertex1 + 1];
            outputArray[p++] = positionsArray[3*vertex1 + 2];
            outputArray[p++] = 1;
            
            outputArray[p++] = positionsArray[3*vertex2 + 0];
            outputArray[p++] = positionsArray[3*vertex2 + 1];
            outputArray[p++] = positionsArray[3*vertex2 + 2];
            outputArray[p++] = 1;

         } else {
            // This face is not a triangle!   Maybe it's a quad or a pentagon?
            // TODO: Were we going to add special support for non-triangular faces?
            for (int i = 0; i < 12; ++i) {
               outputArray[p++] = 0;
            }
         }
      }
      
      int positionsId = ids++;

      { positions.setNumElements(3);
        float[] positions_array = ((Data.Array.Floats) positions).array();
        
        Vector3 vertex0Pos = Vector3.of(1,0,0);
        Vector3 vertex1Pos = Vector3.of(0,1,0);
        Vector3 vertex2Pos = Vector3.of(0,0,1);
        
        
      }
      return graphics;

      
      //   a vertexBuffer for "normals"
      //   a vertexBuffer for "baryCoords"
      
   }
   
}

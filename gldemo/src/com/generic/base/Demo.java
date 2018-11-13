package com.generic.base;

import com.generic.base.Algebra.Vector3;

public class Demo {

   private final Platform platform;
   
   private final Mesh2 mesh;
   
   private Camera camera;

   
   // -------------------------------------------------
   // Constructor
   // -------------------------------------------------
   public Demo(Platform platform) {
      this.platform = platform;
      
      // Create the Mesh we want to display, and setup
      mesh = initMesh();
      
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
      

      initGraphics3D ();
      rebuildCommands (camera);
      
      // Later we'll use something like a MouseListener to listen for grabs
      // and drags, and use these to update the Camera.   When we do this,
      // we'll only need to call "rebuildCommands" for each change in Camera.

      // Set the graphics3D object of the root window.
      // Eventually we'll require a second call, to "show" the root window
      window.setGraphics3D(this.graphics);
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
   //
   // Ideas for the future:
   //   Graphics3D should be called Rendering
   //   Graphics3D positions/normals/barycoords should be grouped
   //              into a single vertexbuffer of 27 floats per triangle
   //              rather than 3 vertexbuffers of 9 floats per triangle each.
   //   Graphics3D should support multiple command lists (supposedly to make it 
   //      easier for multiple consumers to share vertexbuffers and samplers?)
   //   Graphics3D vertexbuffer and sampler keys should be strings, not ints
   // ----------------------------------------------------------
   
   private Graphics3D graphics;
   private int shaderId;
   private int positionsId;
   private int normalsId;
   private int baryCoordsId;
   
   
   private void initGraphics3D () {
      graphics = new Graphics3D();
      int ids = 0;

      // Let's say we'd like to display ONE Mesh, from ONE angle.
      // We will need the Graphics3D object to contain
      //
      // ------------------------------------
      // one shader ("FlatBordered")
      // ------------------------------------
      Graphics3D.Shader shader = new Graphics3D.Shader.FlatBordered(0.1f);
      shaderId = ids++;
      graphics.shaders.put(shaderId, shader);
      
      // ------------------------------------
      // three vertexBuffers for "positions", "normals", "baryCoords"
      // ------------------------------------
      Data.Array positions  = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      Data.Array normals    = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      Data.Array baryCoords = Data.Array.create(Data.Array.Type.THREE_FLOATS);
      
      int numTriangles = mesh.numTriangles();
      positions.setNumElements(3 * numTriangles);
      normals.setNumElements(3 * numTriangles);
      baryCoords.setNumElements(3 * numTriangles);
      
      { float[] positionsArray  = ((Data.Array.Floats)positions).array();
        float[] normalsArray    = ((Data.Array.Floats)normals).array();
        float[] baryCoordsArray = ((Data.Array.Floats)baryCoords).array();

        int p = 0, n = 0, b = 0;
        
        // For each triangle in the mesh, we're going to want to access
        // the mesh "positions per vertex" info.
        // We've been thinking this might turn into a "CURSOR" object.. 
        Mesh2.DataLayer meshPositions = mesh.dataLayer(
           "positions", Mesh2.DataLayer.Type.THREE_FLOATS_PER_VERTEX);
        if (meshPositions == null) {
           throw new RuntimeException("Failed to find position dataLayer");
        }
        float[] meshPositionsArray = ((Data.Array.Floats)(meshPositions.data)).array();
         
        // Now then, we iterate over the mesh faces:
        for (Integer faceID : mesh.faces()) {
           int firstEdge = mesh.directedEdgeForFace(faceID);
           int lastEdge = mesh.prevInLoop(firstEdge);
           int vertex0 = mesh.startOf(firstEdge);
            
           int edge = mesh.nextInLoop(firstEdge);
           int vertexS = mesh.startOf(edge);
           boolean edge0SinFace = true;
            
           while (true) {
              int nextEdge = mesh.nextInLoop(edge);
              int vertexT = mesh.startOf(nextEdge);
              boolean edgeT0inFace = (nextEdge == lastEdge);
             
              // -----------------------------
              // Now we've got a triangle:
              //   vertex0 -- 
              //     edge0SinFace -- describes edge from vertex0 to vertexS
              //   vertexS -- 
              //     the edge vertexS to vertexT is always in the face
              //   vertexT -- 
              //     edgeT0inFace -- describes edge from vertexT to vertex0
              //   vertex0
              // -----------------------------
              Vector3 vertex0Pos = Vector3.fromFloatArray(meshPositionsArray, 3 * vertex0);
              Vector3 vertexSPos = Vector3.fromFloatArray(meshPositionsArray, 3 * vertexS);
              Vector3 vertexTPos = Vector3.fromFloatArray(meshPositionsArray, 3 * vertexT);

              // copy 3 vector3's into positionsArray
              vertex0Pos.copyToFloatArray(positionsArray, 3*p++);
              vertexSPos.copyToFloatArray(positionsArray, 3*p++);
              vertexTPos.copyToFloatArray(positionsArray, 3*p++);
               
              // -----------------------------
              // normal
              // -----------------------------               
              Vector3 normal = Vector3.crossProduct(
                    vertexSPos.minus(vertex0Pos),
                    vertexTPos.minus(vertex0Pos)).normalized();
               
              // copy the same normal 3 times into normalArray
              normal.copyToFloatArray(normalsArray, 3*n++);
              normal.copyToFloatArray(normalsArray, 3*n++);
              normal.copyToFloatArray(normalsArray, 3*n++);
               
              // -----------------------------
              // baryCoords
              // -----------------------------               
              (Vector3.of(1.0f, 0.0f, 0.0f)).copyToFloatArray(baryCoordsArray, 3*b++);
              (Vector3.of(0.0f, 1.0f, 0.0f)).copyToFloatArray(baryCoordsArray, 3*b++);
              (Vector3.of(0.0f, 0.0f, 1.0f)).copyToFloatArray(baryCoordsArray, 3*b++);
               
              // -----------------------------               
              if (edgeT0inFace) break;
               
              // We're moving on to the next triangle...
              edge = nextEdge;
              vertexS = vertexT;
              edge0SinFace = false;
           }
        }
      }
      
      positionsId = ids++;
      normalsId = ids++;
      baryCoordsId = ids++;
      graphics.vertexBuffers.put(positionsId,  positions);
      graphics.vertexBuffers.put(normalsId,    normals);
      graphics.vertexBuffers.put(baryCoordsId, baryCoords);
   }
   
   private void rebuildCommands (Camera camera) {
      graphics.commands.clear();

      // ------------------------------------
      // one commands list
      // ------------------------------------
      graphics.commands.add(new Graphics3D.Shader.Variable.Matrix4x4.Binding(
         Graphics3D.Shader.VIEW_TO_CLIP, camera.cameraToClipSpace));
      graphics.commands.add(new Graphics3D.Shader.Variable.Matrix4x4.Binding(
         Graphics3D.Shader.MODEL_TO_VIEW, camera.worldToCameraSpace));
      graphics.commands.add(new Graphics3D.Shader.Variable.VertexBuffer.Binding(
         Graphics3D.Shader.POSITIONS, positionsId));
      graphics.commands.add(new Graphics3D.Shader.Variable.VertexBuffer.Binding(
         Graphics3D.Shader.NORMALS, normalsId));
      graphics.commands.add(new Graphics3D.Shader.Variable.VertexBuffer.Binding(
         Graphics3D.Shader.BARYCOORDS, baryCoordsId));
      graphics.commands.add(new Graphics3D.Shader.Command.Execute(
         shaderId, mesh.numTriangles()));
   }
}

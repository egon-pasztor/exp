package com.generic.base;

import com.generic.base.Algebra.Vector3;
import com.generic.base.Image.Position;
import com.generic.base.Mesh.DataLayer;

public class Demo {

   private final Platform platform;   
   private final Mesh mesh;   
   
   private final Platform.Widget.Container rootWidget;
   private final Platform.Widget.Renderer3D renderWidget;
   
   
   private Camera.Controller cameraController;

   
   // -------------------------------------------------
   // Constructor
   // -------------------------------------------------
   public Demo(Platform platform) {
      this.platform = platform;

      // Load the mesh we want to display
      mesh = initMesh();

      // Initialize the "Graphics3D" object's vertexbuffers and shaders:
      initRendering();

      // Create a renderer-widget
      renderWidget = platform.newRenderer3D();      
      renderWidget.setRendering(this.rendering);
      renderWidget.setResizeListener(new Platform.Widget.ResizeListener() {
         public void resized() {
            Image.Size renderWidgetSize = renderWidget.size();
            platform.log("RenderWidget Resize Handler (%d x %d)", renderWidgetSize.width, renderWidgetSize.height);
            initCameraController(renderWidgetSize);
         }
      });

      // Add renderer-widget to root widget...
      rootWidget = platform.rootWidget();
      rootWidget.addChild(renderWidget);      
      rootWidget.setResizeListener(new Platform.Widget.ResizeListener() {
         public void resized() {
            Image.Size rootWidgetSize = rootWidget.size();
            platform.log("ROOT Resize Handler (%d x %d)", rootWidgetSize.width, rootWidgetSize.height);
            
            int margin = 10;
            Image.Rect renderBounds = Image.Rect.of(margin, rootWidgetSize.width  - margin,
                                                    margin, rootWidgetSize.height - margin);
                  
            rootWidget.setBounds(renderWidget, renderBounds);
         }
      });

      // Get the Root window size, which we need to construct the camera-controller,
      // which completes the "Graphics3D" object by setting the "commands list"      
      Image.Size windowSize = rootWidget.size();
      platform.log("Demo constructor, I have a %d x %d root-window", windowSize.width, windowSize.height);
      initCameraController(windowSize);
      
      // Future window events should notify us here:      
      renderWidget.setMouseListener(new Platform.Widget.MouseListener() {
         public void mouseHover(Position position) {
         }
         public void mouseDown(Position position, boolean ctrlDown, boolean shiftDown) {
            cameraController.grab(position, shiftDown ? 
                (ctrlDown ? Camera.Controller.GrabState.FOV : Camera.Controller.GrabState.Zoom)
              : (ctrlDown ? Camera.Controller.GrabState.Pan : Camera.Controller.GrabState.Rotate));
         }
         public void mouseDrag(Position position) {
            cameraController.moveTo(position);
            rebuildCommands (cameraController.getCamera());
         }
         public void mouseUp() {
            cameraController.release();
         }
      });      
   }
   private void initCameraController (Image.Size windowSize) {
      
      // Define the camera through which we're going to view the mesh,
      // note that this requires the windowSize.
      Camera initialCamera = new Camera(windowSize,
            new Vector3(0.0f, 4.0f, 0.0f),   // look-at
            new Vector3(0.0f, 0.0f, 18.0f),  // camera-pos
            new Vector3(0.0f, 1.0f, 0.0f),   // camera-up
            53.13f/2.0f);
      
      cameraController = new Camera.Controller(initialCamera);
      rebuildCommands (cameraController.getCamera());
   }


   // ----------------------------------------------------------
   // Creating the Mesh
   // ----------------------------------------------------------
   private Mesh initMesh() {
      Mesh mesh;
      
      boolean bunny = false;
      if (bunny) {
         // Currently we're "loading" the bunny from "bunny.obj" but that requires the
         // "bunny.obj" file to be .. where, exactly?
         mesh = Mesh.loadMesh("bunny.obj");
      } else {
         mesh = Mesh.newUnitCube();
      }
      
      platform.log("Mesh loaded with %d vertices, %d faces, %d edges ... %d triangles", 
            mesh.numVertices(), mesh.numFaces(), mesh.numEdges(), mesh.numTriangles());
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
   
   private Rendering rendering;
   private int shaderId;
   private int positionsId;
   private int normalsId;
   private int baryCoordsId;
   
   
   private void initRendering () {
      rendering = new Rendering();
      int ids = 0;

      // Let's say we'd like to display ONE Mesh from a particular angle.
      // We will need the Graphics3D object to contain
      //
      // ------------------------------------
      // one shader ("FlatBordered")
      // ------------------------------------
      Rendering.Shader shader = new Rendering.Shader.FlatBordered(0.1f);
      shaderId = ids++;
      rendering.shaders.put(shaderId, shader);
      
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
        Mesh.DataLayer meshPositions = mesh.dataLayer(
           "positions", Mesh.DataLayer.Type.THREE_FLOATS_PER_VERTEX);
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
              float edgeT0 = (edgeT0inFace ? 1.0f : -1.0f);
              float edgeS0 = (edge0SinFace ? 1.0f : -1.0f);
              
              (Vector3.of(1.0f,   0.0f,   0.0f)).copyToFloatArray(baryCoordsArray, 3*b++);
              (Vector3.of(0.0f, edgeT0,   0.0f)).copyToFloatArray(baryCoordsArray, 3*b++);
              (Vector3.of(0.0f,   0.0f, edgeS0)).copyToFloatArray(baryCoordsArray, 3*b++);
               
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
      rendering.vertexBuffers.put(positionsId,  positions);
      rendering.vertexBuffers.put(normalsId,    normals);
      rendering.vertexBuffers.put(baryCoordsId, baryCoords);
   }
   
   private void rebuildCommands (Camera camera) {
      rendering.commands.clear();

      // ------------------------------------
      // one commands list
      // ------------------------------------
      rendering.commands.add(new Rendering.Shader.Variable.Matrix4x4.Binding(
         Rendering.Shader.VIEW_TO_CLIP, camera.cameraToClipSpace));
      rendering.commands.add(new Rendering.Shader.Variable.Matrix4x4.Binding(
         Rendering.Shader.MODEL_TO_VIEW, camera.worldToCameraSpace));
      rendering.commands.add(new Rendering.Shader.Variable.VertexBuffer.Binding(
         Rendering.Shader.POSITIONS, positionsId));
      rendering.commands.add(new Rendering.Shader.Variable.VertexBuffer.Binding(
         Rendering.Shader.NORMALS, normalsId));
      rendering.commands.add(new Rendering.Shader.Variable.VertexBuffer.Binding(
         Rendering.Shader.BARYCOORDS, baryCoordsId));
      rendering.commands.add(new Rendering.Shader.Command.Execute(
         shaderId, mesh.numTriangles()));
      rendering.commandsChanged();
   }
}

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
      this.graphics = render(mesh, camera);

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

      // Let's say we'd like to display ONE Mesh, from ONE angle.
      // We will need the Graphics3D object to contain
      //   one shader ("FlatBordered")
      
      Data.Array positions = Data.Array.create(Data.Array.Type.FOUR_FLOATS);
      { positions.setNumElements(3);
        float[] positions_array = ((Data.Array.Floats) positions).array();
        
        Vector3 vertex0Pos = Vector3.of(1,0,0);
        Vector3 vertex1Pos = Vector3.of(0,1,0);
        Vector3 vertex2Pos = Vector3.of(0,0,1);
        
        
      }
      return graphics;
   }
   
}

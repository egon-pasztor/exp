package com.generic.base;

import com.generic.base.Algebra.Vector3;
import com.sun.prism.Mesh;

public class Demo {

   private final Platform platform;
   
   private final Mesh2 mesh;
   private final Camera camera;
   private final Graphics3D graphics;
   
   public Demo(Platform platform) {
      this.platform = platform;
      
      Platform.Widget.Renderer3D window = platform.root3D();
      
      // Now we create the Graphics3D object that will hold the 3D scene to be printed...
      graphics = new Graphics3D();
      window.setGraphics3D(graphics);      
      
      // This demo is about displaying ONE mesh...
      // --------------------------------------------------
      // initially we'll be happy to simply display the mesh and finally get this program doing something!
      // later we'll add mouse control over the window, linked to the mesh orientation, so step 2 is the user will turn the mesh
      // step 3 is the addition of "buttons" or other UI elements beyond the window displaying the Graphics3D
      // -----------------------------------------------------
      // Create the mesh.
      mesh = Mesh2.loadMesh("bunny.obj");
      platform.log("Mesh loaded with %d vertices", mesh.numVertices());
      // The vertex positions will be in a DataLayer called "positions"
      
      // We're also going to need a "camera" that'll produce the model_to_view and view_to_clip matrices.
      // Notice this requires width & height, which we get from the window object.
      Image.Size windowSize = window.size();
      platform.log("Platform has a %d x %d root-window", windowSize.width, windowSize.height);
      camera = new Camera(windowSize.width, windowSize.height,
            new Vector3(0.0f, 0.0f, 0.0f),   // look-at
            new Vector3(0.0f, 0.0f, 18.0f),   // camera-pos
            new Vector3(0.0f, 1.0f, 0.0f),   // camera-up
            53.13f/2.0f);
            
      render(graphics);

      // Set the graphics3D object of the root window.
      // Eventually we'll require a second call, to "show" the root window
   }

   public void render(Graphics3D gl) {

      // Let's say we'd like to display ONE Mesh.  For now, h
      
      Data.Array positions = Data.Array.create(Data.Array.Type.FOUR_FLOATS);
      { positions.setNumElements(3);
        float[] positions_array = ((Data.Array.Floats) positions).array();
        
        Vector3 vertex0Pos = Vector3.of(1,0,0);
        Vector3 vertex1Pos = Vector3.of(0,1,0);
        Vector3 vertex2Pos = Vector3.of(0,0,1);
        
        
      }
      
   }
   
}

package demo;

import demo.VectorAlgebra.*;
import demo.Raster.*;


public class DemoWorld extends World {
   
   public DemoWorld(boolean cube, boolean ico, boolean ball, int subdivide) {
      this.cube = cube;
      this.ico = ico;
      this.ball = ball;
      this.subdivide = subdivide;
      initDemoWorld();
   }
      
   private boolean cube = false;  
   private boolean ico = true;
   private boolean ball = false;
   private int subdivide = 1;

   // These are being "accessed" out of band by GLSample to "send" this one geometry, and this one texture,
   // to opengl.  TODO:  GLWranger should traverse the root tree and FIND what textures are being used.
   // For each texture, GLWranger will have to associate a "GL ID" that openGL provides...
   public Image myTexture;
   public Geometry.Model geom;
   
   private World.Model[] mobileBalls;
   
   public void initDemoWorld() {
      System.out.format("Starting InitDemoWorld\n");
      
      myTexture = Raster.imageFromResource("teapot.png");
      System.out.format("InitDemoWorld loaded texture [%d x %d] texture\n", myTexture.width,myTexture.height);   
      
      geom = null;
      if (cube) geom = Geometry.createUnitCube();
      if (ball) geom = Geometry.createUnitSphere(20,20);
      if (ico)  geom = Geometry.createIco(subdivide);
      
      CompoundModel root = new CompoundModel();
      root.children.add(new TexturedMeshModel(geom, myTexture));

      mobileBalls = new World.Model[4];
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         
         TexturedMeshModel m = new TexturedMeshModel(geom, myTexture);
         root.children.add(m);
         mobileBalls[i] = m;
         
         m = new TexturedMeshModel(geom, myTexture);
         m.translate(Vector3f.X.times(6).rotated(Vector3f.Y, angle));
         root.children.add(m);
      }
      updateDemoWorld(0.0f);
      setRootModel(root);
   }
   
   public void updateDemoWorld(float time) {
      final int MillisPerCycle = 2000;
      float phase = (float) ((time / MillisPerCycle) * (Math.PI * 2.0));
      
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         World.Model m = mobileBalls[i];
         m.setModelToWorld(Matrix4f.IDENTITY);
         m.translate(Vector3f.X.times(3.0f + (float) Math.cos(phase + angle)).rotated(Vector3f.Y, angle));
      }
   }
}

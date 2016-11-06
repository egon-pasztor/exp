package demo;

import demo.VectorAlgebra.*;
import demo.Geometry.*;
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
   
   private MeshModel stretchyBall;
   private World.Model[] mobileBalls;
   
   public void initDemoWorld() {
      System.out.format("Starting InitDemoWorld\n");
      
      Image leaImage = Raster.imageFromResource("lea.png");
      Image teapotImage = Raster.imageFromResource("teapot.png");
      
      MeshModel center = null;
      if (cube) center = Geometry.createUnitCube();
      if (ball) center = Geometry.createUnitSphere(20,20);
      if (ico)  center = Geometry.createIco(subdivide);

      MeshModel ball = Geometry.createIco(2);
      stretchyBall = Geometry.createIco(4);
      
      CompoundModel root = new CompoundModel();
      root.children.add(new TexturedMeshModel(center, leaImage));

      mobileBalls = new World.Model[4];
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         
         TexturedMeshModel m = new TexturedMeshModel(ball, teapotImage);
         root.children.add(m);
         mobileBalls[i] = m;
         
         m = new TexturedMeshModel(stretchyBall, teapotImage);
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

      Geometry.sphereWarp(stretchyBall, phase, 0.05f);
   }
}

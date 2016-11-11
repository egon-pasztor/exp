package demo;

import demo.VectorAlgebra.*;
import demo.World.Shader;
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
      
      // ----------------------------------------------------------------------------------
      // consider a version where... the renderable model nodes ARE Shader.Instance
      // ----------------------------------------------------------------------------------
      
      Image leaImage = Raster.imageFromResource("lea.png");
      Image teapotImage = Raster.imageFromResource("teapot.png");
      Shader.ManagedTexture leaImageT = new Shader.ManagedTexture(leaImage);
      Shader.ManagedTexture teapotImageT = new Shader.ManagedTexture(teapotImage);
      
      MeshModel cube0 = Geometry.createUnitCube();
      MeshModel ball0 = Geometry.createUnitSphere(20,20);
      MeshModel ball1 = Geometry.createIco(4);

      stretchyBall = ball1;
      
      CompoundModel root = new CompoundModel();
      
      // Build root instance
      Shader.Instance cube0Instance = new Shader.Instance(Shader.TEXTURE_SHADER);
      System.out.format("Setting up bindings..\n");
      cube0Instance.bind(Shader.POSITION_ARRAY, new Shader.PerVertexBinding(cube0.getManagedBuffer(Shader.POSITION_ARRAY)));
      cube0Instance.bind(Shader.COLOR_ARRAY, new Shader.PerVertexBinding(cube0.getManagedBuffer(Shader.COLOR_ARRAY)));
      cube0Instance.bind(Shader.BARY_COORDS,    new Shader.PerVertexBinding(cube0.getManagedBuffer(Shader.BARY_COORDS)));
      cube0Instance.bind(Shader.TEX_COORDS,     new Shader.PerVertexBinding(cube0.getManagedBuffer(Shader.TEX_COORDS)));
      cube0Instance.bind(Shader.MAIN_TEXTURE,   new Shader.TextureBinding(leaImageT));
      root.children.add(new ShaderInstanceModel(cube0Instance, cube0));

      mobileBalls = new World.Model[4];
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         Model m;
         
         Shader.Instance mobileBallInstance = new Shader.Instance(Shader.TEXTURE_SHADER);
         mobileBallInstance.bind(Shader.POSITION_ARRAY, new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.POSITION_ARRAY)));
         mobileBallInstance.bind(Shader.COLOR_ARRAY, new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.COLOR_ARRAY)));
         mobileBallInstance.bind(Shader.BARY_COORDS,    new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.BARY_COORDS)));
         mobileBallInstance.bind(Shader.TEX_COORDS,     new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.TEX_COORDS)));
         mobileBallInstance.bind(Shader.MAIN_TEXTURE,   new Shader.TextureBinding(teapotImageT));
         // Build mobile flat-shaded ball instance
         // Shader.Instance mobileBallInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
         // mobileBallInstance.bind(Shader.POSITION_ARRAY,  new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.POSITION_ARRAY)));
         // mobileBallInstance.bind(Shader.COLOR_ARRAY,     new Shader.PerVertexBinding(ball0.getManagedBuffer(Shader.COLOR_ARRAY)));
         m = new ShaderInstanceModel(mobileBallInstance, ball0);
         
         root.children.add(m);
         mobileBalls[i] = m;
         
         // Build pulsing textured ball instance
         Shader.Instance texBallInstance = new Shader.Instance(Shader.TEXTURE_SHADER);
         texBallInstance.bind(Shader.POSITION_ARRAY,  new Shader.PerVertexBinding(ball1.getManagedBuffer(Shader.POSITION_ARRAY)));
         texBallInstance.bind(Shader.COLOR_ARRAY, new Shader.PerVertexBinding(ball1.getManagedBuffer(Shader.COLOR_ARRAY)));
         texBallInstance.bind(Shader.BARY_COORDS,     new Shader.PerVertexBinding(ball1.getManagedBuffer(Shader.BARY_COORDS)));
         texBallInstance.bind(Shader.TEX_COORDS,      new Shader.PerVertexBinding(ball1.getManagedBuffer(Shader.TEX_COORDS)));
         texBallInstance.bind(Shader.MAIN_TEXTURE,    new Shader.TextureBinding(teapotImageT));
         m = new ShaderInstanceModel(texBallInstance, ball1);
         
         root.children.add(m);
         m.translate(Vector3f.X.times(6).rotated(Vector3f.Y, angle));
      }
      System.out.format("DONE..\n");

      updateDemoWorld(0.0f);
      setRootModel(root);
   }
   // ----------------------------------------------------------------------------------
   // ----------------------------------------------------------------------------------
   
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

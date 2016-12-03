package com.generic.demo;

import com.generic.base.Geometry;
import com.generic.base.World;
import com.generic.base.Geometry.*;
import com.generic.base.VectorAlgebra.*;
import com.generic.base.Shader;
import com.generic.demo.Raster.*;


public class DemoWorld extends World {
   
   private MeshModel stretchyBall;
   private ShaderInstanceModel[] mobileBalls;
   private MeshModel choppedCube;

   public DemoWorld() {
      System.out.format("Starting InitDemoWorld\n");
      
      // ----------------------------------------------------------------------------------
      // consider a version where... the renderable model nodes ARE Shader.Instance
      // ----------------------------------------------------------------------------------
      
      Image leaImage = Raster.imageFromResource("lea.png");
      Image teapotImage = Raster.imageFromResource("teapot.png");
      Shader.ManagedTexture leaImageT = new Shader.ManagedTexture(leaImage);
      Shader.ManagedTexture teapotImageT = new Shader.ManagedTexture(teapotImage);
      
      MeshModel cube0 = Geometry.createUnitCube();
      MeshModel ball0 = Geometry.createIco(0);
      MeshModel ball1 = Geometry.createUnitSphere(30,30); //Geometry.createIco(4);

      stretchyBall = ball1;
      CompoundModel root = new CompoundModel();
      ShaderInstanceModel m;
      
      // Build root instance
      Shader.Instance cube0Instance = new Shader.Instance(Shader.TEXTURE_SHADER);
      cube0Instance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Uniform.TextureBinding(leaImageT));
      m = new ShaderInstanceModel(cube0Instance, cube0);
      root.children.add(m);
      
      // Demo cylinder instance
      MeshModel cyl = Geometry.createCylinder(Vector3f.Z.times(-1.0f), Vector3f.Z, 1.0f, 1.0f, 8);
      Shader.Instance cylInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      m = new ShaderInstanceModel(cylInstance, cyl);
      root.children.add(m);
      m.scale(0.2f, 0.2f, 2.0f);
      m.translate(Vector3f.Y.times(3.0f));

      // Demo cylinder instance
      choppedCube = Geometry.createChoppedCube();
      Shader.Instance createChoppedCubeInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      m = new ShaderInstanceModel(createChoppedCubeInstance, choppedCube);
      root.children.add(m);
      m.translate(Vector3f.Y.times(7.0f));
      
      // ICO
      Shader.Instance mobileBallInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      m = new ShaderInstanceModel(mobileBallInstance, ball0);
      root.children.add(m);
      m.translate(Vector3f.Y.times(-3.0f));
      
      MeshModel pb = Geometry.createIco(3);
      Geometry.everyPointGetsAnInteger(pb, 255);
      
      mobileBalls = new ShaderInstanceModel[4];
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         
         // Perlin-Ball instance
         Shader.Instance pbInstance = new Shader.Instance(Shader.POINT_COLOR_SHADER);
         m = new ShaderInstanceModel(pbInstance, pb);
         root.children.add(m);
         mobileBalls[i] = m;
         
         // Build pulsing textured ball instance
         Shader.Instance texBallInstance = new Shader.Instance(Shader.TEXTURE_SHADER);
         texBallInstance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Uniform.TextureBinding(teapotImageT));
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
      final int MillisPerCycle = 10000;
      float phase = (float) ((time / MillisPerCycle) * (Math.PI * 2.0));
phase=0;
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         ShaderInstanceModel m = mobileBalls[i];
         m.setModelToWorld(Matrix4f.IDENTITY);
         Vector3f translationVec = Vector3f.X.times(3.0f + (float) Math.cos(phase + angle)).rotated(Vector3f.Y, angle);
         m.translate(translationVec);
         m.instance.bind(Shader.TRANSLATION_VEC, new Shader.Variable.Uniform.Vec3Binding(translationVec));
      }

      Geometry.warpChoppedCube(choppedCube, phase, 1.0f);
      Geometry.sphereWarp(stretchyBall, phase, 0.05f);
   }
}

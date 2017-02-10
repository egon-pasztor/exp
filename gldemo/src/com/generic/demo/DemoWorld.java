package com.generic.demo;

import com.generic.base.Geometry;
import com.generic.base.World;
import com.generic.base.Geometry.*;
import com.generic.base.Mesh;
import com.generic.base.Algebra.*;

import java.util.ArrayList;

import com.generic.base.Shader;
import com.generic.base.Raster.*;


public class DemoWorld extends World {
   
   private MeshModel stretchyBall;
   private ShaderExecutingModel[] mobileBalls;
//   private MeshModel choppedCube;
   
   public MeshModel mappingModel1;
   public MeshModel mappingModel2;
   public Vector2 selectedUVPointIfAny;

   public DemoWorld(Image leaImage, Image teapotImage, Mesh bunny) {
      System.out.format("Starting InitDemoWorld\n");
      
      // ----------------------------------------------------------------------------------
      // consider a version where... the renderable model nodes ARE Shader.Instance
      // ----------------------------------------------------------------------------------
      
      Shader.ManagedTexture leaImageT = new Shader.ManagedTexture(leaImage);
      Shader.ManagedTexture teapotImageT = new Shader.ManagedTexture(teapotImage);
      
      CompoundModel root = new CompoundModel();
      ShaderExecutingModel m;
      
      // Build root instance
      MeshModel cube0 = Geometry.createUnitCube();
      Shader.Instance cube0Instance = new Shader.Instance(Shader.TEXTURE_SHADER);
      cube0Instance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Sampler.Binding(leaImageT));
      m = new ShaderExecutingModel(cube0Instance, cube0);
      root.children.add(m);
      
//      // Demo cylinder instance
//      MeshModel cyl = Geometry.createCylinder(Vector3f.Z.times(-1.0f), Vector3f.Z, 1.0f, 1.0f, 8);
//      Shader.Instance cylInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
//      m = new ShaderInstanceModel(cylInstance, cyl);
//      root.children.add(m);
//      m.scale(0.2f, 0.2f, 2.0f);
//      m.translate(Vector3f.Y.times(3.0f));
//
//      // Demo cylinder instance
//      choppedCube = Geometry.createChoppedCube();
//      Shader.Instance createChoppedCubeInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
//      m = new ShaderInstanceModel(createChoppedCubeInstance, choppedCube);
//      root.children.add(m);
//      m.translate(Vector3f.Y.times(7.0f));
      
      // ICO
      MeshModel ico0 = Geometry.createIco(0);
      ico0.updateMesh(Geometry.newQuadCoverMesh());
      Geometry.doQuadCover(ico0, 0.0f, 4);
      mappingModel1 = ico0;

      Shader.Instance ico0Instance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      m = new ShaderExecutingModel(ico0Instance, ico0);
      root.children.add(m);
      m.translate(Vector3.Y.times(+3.0f));

      MeshModel bunnyModel = new MeshModel(bunny);
      mappingModel2 = bunnyModel;
      Geometry.doQuadCover(bunnyModel, 8.0f, 80);
      Shader.Instance bunnyInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      m = new ShaderExecutingModel(bunnyInstance, bunnyModel);
      root.children.add(m);
      m.translate(Vector3.Y.times(+5.0f));
      
      
      MeshModel perlinNoiseBall = Geometry.createIco(3);
      //
      // TODO: previously 
      //
      //   Geometry.everyPointGetsAnInteger(perlinNoiseBall, 255);
      //
      // The shader we use for perlinNoiseBall no longer uses this data,
      // but if it DID, we'd have trouble, because new Mesh objects no longer allow this.

      
      MeshModel ball1 = Geometry.createUnitSphere(30,30); //Geometry.createIco(4);
      stretchyBall = ball1;      
      mobileBalls = new ShaderExecutingModel[4];
      
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         
         // Perlin-Ball instance
         Shader.Instance pbInstance = new Shader.Instance(Shader.POINT_COLOR_SHADER);
         m = new ShaderExecutingModel(pbInstance, perlinNoiseBall);
         root.children.add(m);
         mobileBalls[i] = m;
         
         // Build pulsing textured ball instance
         Shader.Instance texBallInstance = new Shader.Instance(Shader.TEXTURE_SHADER);
         texBallInstance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Sampler.Binding(teapotImageT));
         m = new ShaderExecutingModel(texBallInstance, ball1);
         
         root.children.add(m);
         m.translate(Vector3.X.times(6).rotated(Vector3.Y, angle));
      }
      
      System.out.format("DONE..\n");
      updateDemoWorld(0.0f);      
      setRootModel(root);
   }
   
   // ----------------------------------------------------------------------------------
   // ----------------------------------------------------------------------------------
   
   public void updateDemoWorld(float time) {
      final int MillisPerCycle = 3000;
      float phase = (float) ((time / MillisPerCycle) * (Math.PI * 2.0));

      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         ShaderExecutingModel m = mobileBalls[i];
         m.setModelToWorld(Matrix4x4.IDENTITY);
         Vector3 translationVec = Vector3.X.times(3.0f + (float) Math.cos(phase + angle)).rotated(Vector3.Y, angle);
         m.translate(translationVec);
         
         // okay.. i see.   "m.translate" above changes the viewMatrix which is passed into vertexShader and 
         // used to move the object, while TRANSLATION_VEC below allows the shader to compute the sphere's intersection
         // with a perlin-solid-noise, by providing the sphere's position..
         m.instance.bind(Shader.TRANSLATION_VEC, new Shader.Variable.Uniform.Vec3Binding(translationVec));
      }

      //Geometry.warpChoppedCube(choppedCube, phase, 1.0f);
      Geometry.sphereWarp2(stretchyBall, phase, 0.05f);
   }
}
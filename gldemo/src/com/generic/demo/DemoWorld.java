package com.generic.demo;

import com.generic.base.Geometry;
import com.generic.base.World;
import com.generic.base.Geometry.*;
import com.generic.base.Mesh;
import com.generic.base.Mesh2;
import com.generic.base.Mesh2.DataLayer;
import com.generic.base.Mesh2.ResizableArray;
import com.generic.base.QuadCover;
import com.generic.base.Algebra.*;

import com.generic.base.Shader;
import com.generic.base.Raster.*;
import com.generic.base.Shader.Variable;


public class DemoWorld extends World {
   
   private MeshModel stretchyBall;
   private ShadedTrianglesModel[] mobileBalls;
//   private MeshModel choppedCube;
   
   public MeshModel mappingModel1;
   public MeshModel mappingModel2;
   public MeshModel mappingModel3;
   
   public Vector2 selectedUVPointIfAny;
   public Mesh.Triangle selectedTriangleIfAny;

   public DemoWorld(Image leaImage, Image teapotImage, Mesh bunny) {
      System.out.format("Starting InitDemoWorld\n");
      
      // ----------------------------------------------------------------------------------
      // consider a version where... the renderable model nodes ARE Shader.Instance
      // ----------------------------------------------------------------------------------
      
      Shader.ManagedTexture leaImageT = new Shader.ManagedTexture(Shader.Variable.Sampler.Type.TEXTURE_32BIT, leaImage);
      Shader.ManagedTexture teapotImageT = new Shader.ManagedTexture(Shader.Variable.Sampler.Type.TEXTURE_32BIT, teapotImage);
      
      CompoundModel root = new CompoundModel();
      ShadedTrianglesModel m;
      
      // Build root instance
      MeshModel cube0 = Geometry.createUnitCube();
      Shader.Instance cube0Instance = new Shader.Instance(Shader.TEXTURE_SHADER);
      cube0Instance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Sampler.Binding(leaImageT));
      m = new ShadedTrianglesModel(cube0Instance, cube0);
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
      
      // --------------------------
      // ICO
      // --------------------------
      
      MeshModel ico0 = Geometry.createIco(3);
      ico0.mesh.removeTriangle(ico0.mesh.triangles.get(ico0.mesh.triangles.size()/3));
      mappingModel1 = ico0;
      QuadCover.run("ICO1 WITH ONE MISSING TRIANGLE", ico0, 0.0f);
      
      Shader.Instance ico0Instance = new Shader.Instance(Shader.FACE_COLOR_SHADER);      
      Shader.ManagedFloatTexture ico0_mesh_info = new Shader.ManagedFloatTexture(Shader.Variable.Sampler.Type.TEXTURE_FLOAT, Geometry.createMeshInfoImage(ico0));      
      ico0Instance.bind(Shader.MESH_INFO, new Shader.Variable.Sampler.Binding2(ico0_mesh_info));
      
      m = new ShadedTrianglesModel(ico0Instance, ico0);
      root.children.add(m);
      m.translate(Vector3.Y.times(+3.0f));
      
      // --------------------------
      // TORUS
      // --------------------------

      MeshModel tor0 = Geometry.createTorus(2.0f, 0.5f, 0.3f, 53, 23);
      tor0.mesh.removeTriangle(tor0.mesh.triangles.get(tor0.mesh.triangles.size()/3));
      mappingModel3 = tor0;
      QuadCover.run("DONUT", tor0, -2.2f);
      
      Shader.Instance tor0Instance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      Shader.ManagedFloatTexture tor0_mesh_info = new Shader.ManagedFloatTexture(Shader.Variable.Sampler.Type.TEXTURE_FLOAT, Geometry.createMeshInfoImage(tor0));
      tor0Instance.bind(Shader.MESH_INFO, new Shader.Variable.Sampler.Binding2(tor0_mesh_info));
      
      m = new ShadedTrianglesModel(tor0Instance, tor0);
      root.children.add(m);
      m.rotate(Vector3.X, (float)(Math.PI/2.0));
      m.translate(Vector3.Y.times(-4.0f));

      // --------------------------
      // BUNNY
      // --------------------------
      
      MeshModel bunnyModel = new MeshModel(bunny);
      mappingModel2 = bunnyModel;
      QuadCover.run("BUNNY", bunnyModel, 2.2f);
      
      Shader.Instance bunnyInstance = new Shader.Instance(Shader.FACE_COLOR_SHADER);
      Shader.ManagedFloatTexture bunny_mesh_info = new Shader.ManagedFloatTexture(Shader.Variable.Sampler.Type.TEXTURE_FLOAT, Geometry.createMeshInfoImage(bunnyModel));      
      bunnyInstance.bind(Shader.MESH_INFO, new Shader.Variable.Sampler.Binding2(bunny_mesh_info));
      
      m = new ShadedTrianglesModel(bunnyInstance, bunnyModel);
      root.children.add(m);
      m.translate(Vector3.Y.times(+5.0f));
      // --------------------------
      // NOISE-BALLS
      // --------------------------
      
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
      mobileBalls = new ShadedTrianglesModel[4];
      
      for (int i = 0; i < 4; i++) {
         float angle = (float)(i * (Math.PI/2.0));
         
         // Perlin-Ball instance
         Shader.Instance pbInstance = new Shader.Instance(Shader.POINT_COLOR_SHADER);
         m = new ShadedTrianglesModel(pbInstance, perlinNoiseBall);
         root.children.add(m);
         mobileBalls[i] = m;
         
         // Build pulsing textured ball instance
         Shader.Instance texBallInstance = new Shader.Instance(Shader.TEXTURE_SHADER);
         texBallInstance.bind(Shader.MAIN_TEXTURE, new Shader.Variable.Sampler.Binding(teapotImageT));
         m = new ShadedTrianglesModel(texBallInstance, ball1);
         
         root.children.add(m);
         m.translate(Vector3.X.times(6).rotated(Vector3.Y, angle));
      }
      // --------------------------
      // MESH2
      // --------------------------
      final Mesh2 mesh2 = Mesh2.loadMesh("bunny.obj");
      System.out.format("---#-#-#-#-#-\nLoaded Mesh2\n");
      mesh2.print();
      
      // ---
      
      Shader.ManagedFloatBuffer positionProducer = new Shader.ManagedFloatBuffer(4) {
         @Override
         public int getNumElements() {
            return mesh2.numFaces() * 3;
         }
         @Override
         public void fillBuffer(float[] outputArray) {
            System.out.format("Mesh2 positionBuffer fillBuffer called!\n");
            System.out.format("We have %d faces, x 3 vertices per face, x 4 floats per vertex, == %d floats in total!\n" +
                  "The array we're supposed to fill contains %d floats\n",
                  mesh2.numFaces(), mesh2.numFaces()*12, outputArray.length);
            
            // Array size should be getNumElements()     * 4 
            //                   == mesh2.numFaces() * 3 * 4
            //                   == 12 floats per triangle:
            if (array.length != (mesh2.numFaces() * 12)) {
               throw new RuntimeException("Wrong array size?");
            }
            
            // Okay, get the positions vector from the mesh...
            DataLayer positions = mesh2.dataLayer("positions",
                  new DataLayer.Type(3, DataLayer.Type.Primitive.FLOAT, DataLayer.Type.Elements.PER_VERTEX));
            if (positions == null) {
               throw new RuntimeException("Failed to find 3-float-per-vertex position buffer");
            }
            float[] positionsArray = ((ResizableArray.Floats)(positions.data)).array();
            System.out.format("We have %d vertices and the positions array should be 3 floats per vertex == %d floats total.\n"+
                  "The positons array contains %d floats\n",
                  mesh2.numVertices(), mesh2.numVertices()*3, positionsArray.length);
            
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
         }
      };
      Shader.ManagedFloatBuffer normalProducer = new Shader.ManagedFloatBuffer(3) {
         @Override
         public int getNumElements() {
            return mesh2.numFaces() * 3;
         }
         @Override
         public void fillBuffer(float[] outputArray) {
            System.out.format("Mesh2 positionBuffer fillBuffer called!\n");
            System.out.format("We have %d faces, x 3 vertices per face, x 3 floats per vertex, == %d floats in total!\n" +
                  "The array we're supposed to fill contains %d floats\n",
                  mesh2.numFaces(), mesh2.numFaces()*9, outputArray.length);
            
            // Array size should be getNumElements()     * 4 
            //                   == mesh2.numFaces() * 3 * 4
            //                   == 12 floats per triangle:
            if (array.length != (mesh2.numFaces() * 9)) {
               throw new RuntimeException("Wrong array size?");
            }
            
            // Okay, get the positions vector from the mesh...
            DataLayer positions = mesh2.dataLayer("positions",
                  new DataLayer.Type(3, DataLayer.Type.Primitive.FLOAT, DataLayer.Type.Elements.PER_VERTEX));
            if (positions == null) {
               throw new RuntimeException("Failed to find 3-float-per-vertex position buffer");
            }
            float[] positionsArray = ((ResizableArray.Floats)(positions.data)).array();
            System.out.format("We have %d vertices and the positions array should be 3 floats per vertex == %d floats total.\n"+
                  "The positons array contains %d floats\n",
                  mesh2.numVertices(), mesh2.numVertices()*3, positionsArray.length);
            
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
                  
                  Vector3 normal = Vector3.crossProduct(vertex2Pos.minus(vertex0Pos), vertex1Pos.minus(vertex0Pos)).normalized();
                  System.out.format("normal is [%s]",  normal.toString());
                  outputArray[p++] = normal.x;
                  outputArray[p++] = normal.y;
                  outputArray[p++] = normal.z;
                  //outputArray[p++] = 0;
                  
                  outputArray[p++] = normal.x;
                  outputArray[p++] = normal.y;
                  outputArray[p++] = normal.z;
                  //outputArray[p++] = 0;
                  
                  outputArray[p++] = normal.x;
                  outputArray[p++] = normal.y;
                  outputArray[p++] = normal.z;
                  //outputArray[p++] = 0;
                  
               } else {
                  // This face is not a triangle!   Maybe it's a quad or a pentagon?
                  // TODO: Were we going to add special support for non-triangular faces?
                  for (int i = 0; i < 12; ++i) {
                     outputArray[p++] = 0;
                  }
               }
            }
         }
      };
      Shader.ManagedIntBuffer colorProducer = new Shader.ManagedIntBuffer(3) {
         @Override
         public int getNumElements() {
            return mesh2.numFaces() * 3;
         }
         @Override
         public void fillBuffer(int[] array) {
         }
      };
      Shader.ManagedFloatBuffer baryProducer = new Shader.ManagedFloatBuffer(3) {
         @Override
         public int getNumElements() {
            return mesh2.numFaces() * 3;
         }
         @Override
         public void fillBuffer(float[] outputArray) {
            
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
                  
                  outputArray[p++] = 1;
                  outputArray[p++] = 0;
                  outputArray[p++] = 0;
                  
                  outputArray[p++] = 0;
                  outputArray[p++] = 1;
                  outputArray[p++] = 0;
                  
                  outputArray[p++] = 0;
                  outputArray[p++] = 0;
                  outputArray[p++] = 1;
                  
               } else {
                  // This face is not a triangle!   Maybe it's a quad or a pentagon?
                  // TODO: Were we going to add special support for non-triangular faces?
                  for (int i = 0; i < 12; ++i) {
                     outputArray[p++] = 0;
                  }
               }
            }
            
         }
      };
 
      Shader.Instance secondBunnyInstance = new Shader.Instance(Shader.FLAT_SHADER);
      secondBunnyInstance.bind(Shader.POSITION_ARRAY,  new Variable.VertexBuffer.Binding(positionProducer));
      secondBunnyInstance.bind(Shader.COLOR_ARRAY,     new Variable.VertexBuffer.Binding(colorProducer));
      secondBunnyInstance.bind(Shader.NORMAL_ARRAY,    new Variable.VertexBuffer.Binding(normalProducer));
      secondBunnyInstance.bind(Shader.BARY_COORDS,     new Variable.VertexBuffer.Binding(baryProducer));         
      m = new ShadedTrianglesModel(secondBunnyInstance, mesh2.numFaces());
      root.children.add(m);
      m.translate(Vector3.Y.times(-17.0f));

      
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
         ShadedTrianglesModel m = mobileBalls[i];
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

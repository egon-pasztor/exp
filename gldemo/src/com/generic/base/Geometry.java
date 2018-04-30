package com.generic.base;

import com.generic.base.Mesh;
import com.generic.base.Algebra.*;
import com.generic.base.Shader;
import com.generic.base.Shader.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Geometry {
   
   public static void check(boolean cond, String err) {
      if (!cond) throw new RuntimeException("FAILED: " + err);
   }

   // -----------------------------------------------------------------------
   // New-style Mesh model class
   // -----------------------------------------------------------------------
 
   public static class MeshModel {
      public Mesh mesh;
      public MeshModel(Mesh mesh) {
         updateMesh(mesh);         
      }
      public void updateMesh(Mesh mesh) {
         Mesh oldMesh = this.mesh;
         this.mesh = mesh;
         if (oldMesh != null) {
            this.mesh.copyFrom(oldMesh);
         }
            
         buffers = new HashMap<String,Shader.ManagedBuffer>();
         
         setManagedBuffer(Shader.POSITION_ARRAY, newPositionArrayManager(mesh));
         setManagedBuffer(Shader.V0POS_ARRAY,    newVertexPositionArrayManager(mesh,0));
         setManagedBuffer(Shader.V1POS_ARRAY,    newVertexPositionArrayManager(mesh,1));
         setManagedBuffer(Shader.V2POS_ARRAY,    newVertexPositionArrayManager(mesh,2));
         
         setManagedBuffer(Shader.TEX_COORDS,     newTextureCoordsArrayManager(mesh));
         setManagedBuffer(Shader.V0UV_ARRAY,     newVertexTextureCoordsArrayManager(mesh, 0));
         setManagedBuffer(Shader.V1UV_ARRAY,     newVertexTextureCoordsArrayManager(mesh, 1));
         setManagedBuffer(Shader.V2UV_ARRAY,     newVertexTextureCoordsArrayManager(mesh, 2));
         
         setManagedBuffer(Shader.BARY_COORDS,    newBaryCoordsArrayManager(mesh));
         setManagedBuffer(Shader.COLOR_ARRAY,    newColorArrayManager(mesh));
         
         setManagedBuffer(Shader.TRIANGLE_INDEX,    newTriangleIndexArrayManager(mesh));         
      }

      public Mesh.Vertex getOrAddVertex(Vector3 position) {         
         // Search to see if we already have a Vertex at this position
         // TODO:  Use a 3D index for this...
         for (Mesh.Vertex v : mesh.vertices) {
            Vector3 vPosition = v.getPosition();
            if (vPosition.minus(position).lengthSq() < .00000001f) return v;
         }
         
         // Create a new vertex
         Mesh.Vertex v = mesh.addVertex();
         v.setPosition(position);
         return v;
      }
      public Mesh.Triangle addTriangle (Vector3 a, Vector3 b, Vector3 c) {
         Mesh.Vertex va = getOrAddVertex(a);
         Mesh.Vertex vb = getOrAddVertex(b);
         Mesh.Vertex vc = getOrAddVertex(c);
         return addTriangle(va,vb,vc);
      }
      public Mesh.Triangle addTriangle (Mesh.Vertex va, Mesh.Vertex vb, Mesh.Vertex vc) {
         Mesh.Triangle t = mesh.addTriangle(va, vb, vc);
         //mesh.checkMesh();
         return t;
      }
      
      // ------------------------------------------------------------------------
      // Map of Managed Buffers...
      // ------------------------------------------------------------------------

      public Shader.ManagedBuffer getManagedBuffer(String key) {
         return buffers.get(key);
      }
      public void setManagedBuffer(String key, Shader.ManagedBuffer buffer) {
         buffers.put(key, buffer);
      }
      private HashMap<String,Shader.ManagedBuffer> buffers;
   }
   

   // -----------------------------------------------------------------------
   // New-style Buffer-Builders
   // -----------------------------------------------------------------------
   
   public interface TextureCoordProvider {
      public Triangle2 getTextureCoords();
   }
   private static Shader.ManagedBuffer newTextureCoordsArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               Triangle2 texCoords = ((TextureCoordProvider) tb).getTextureCoords();
               System.out.format("Hmm.. triangle %d has texCoords %s\n", tb.getIndex(), (texCoords==null)?"null":"non-null");
               pPos = toVector4f(texCoords.v0).copyToFloatArray(array, pPos);
               pPos = toVector4f(texCoords.v1).copyToFloatArray(array, pPos);
               pPos = toVector4f(texCoords.v2).copyToFloatArray(array, pPos);
            }
         }
         private Vector4 toVector4f(Vector2 tex1) {
            return new Vector4(tex1.x, tex1.y, 0.0f, 1.0f);
         }
      };
   }

   private static Shader.ManagedBuffer newVertexTextureCoordsArrayManager(final Mesh mesh, final int index) {
      return new Shader.ManagedFloatBuffer(2) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               Triangle2 texCoords = ((TextureCoordProvider) tb).getTextureCoords();
               Vector2 tex = (index == 0) ? texCoords.v0 : (index == 1) ? texCoords.v1 : texCoords.v2;
               pPos = tex.copyToFloatArray(array, pPos);
               pPos = tex.copyToFloatArray(array, pPos);
               pPos = tex.copyToFloatArray(array, pPos);
            }
         }
      };
   }
   private static Shader.ManagedBuffer newPositionArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               pPos = Vector4.fromVector3f(t.vertices[0].getPosition()).copyToFloatArray(array, pPos);
               pPos = Vector4.fromVector3f(t.vertices[1].getPosition()).copyToFloatArray(array, pPos);
               pPos = Vector4.fromVector3f(t.vertices[2].getPosition()).copyToFloatArray(array, pPos);
            }
         }
      };
   }

   private static Shader.ManagedBuffer newTriangleIndexArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(1) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            int numTriangles = mesh.triangles.size();
            for (Mesh.Triangle t : mesh.triangles) {
               int index = t.getIndex();
               array[pPos++] = (((float)index)+0.5f)/numTriangles;
               array[pPos++] = (((float)index)+0.5f)/numTriangles;
               array[pPos++] = (((float)index)+0.5f)/numTriangles;
            }
         }
      };
   }
   private static Shader.ManagedBuffer newVertexPositionArrayManager(final Mesh mesh, final int index) {
      return new Shader.ManagedFloatBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               Vector3 pos = t.vertices[index].getPosition();
               pPos = Vector4.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4.fromVector3f(pos).copyToFloatArray(array, pPos);
               pPos = Vector4.fromVector3f(pos).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   
   private static Shader.ManagedBuffer newBaryCoordsArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(3) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               pPos = (new Vector3(1.0f, 0.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3(0.0f, 1.0f, 0.0f)).copyToFloatArray(array, pPos);
               pPos = (new Vector3(0.0f, 0.0f, 1.0f)).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   
   // TOOD:  It sure looks like no-one's using this..
   
   private static Shader.ManagedBuffer newColorArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(3) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            int col = 0;
            for (Mesh.Triangle t : mesh.triangles) {
               Color.ARGB color = 
                  (col==0) ? new Color.ARGB((byte)0x00, (byte)0xb0, (byte)0xff, (byte)0x80) :
                  (col==1) ? new Color.ARGB((byte)0x00, (byte)0xc0, (byte)0xd0, (byte)0xb0) :
                  (col==2) ? new Color.ARGB((byte)0x00, (byte)0x80, (byte)0xf0, (byte)0xd0) :
                             new Color.ARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
               
               color = new Color.ARGB((byte)0x00, (byte)0x90, (byte)0xf0, (byte)0xa0);
               
               col = (col+1)%4;
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, Color.ARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             return base+3;
         }
      };
   }   
   
   // -----------------------------------------------------------------------
   // New-style Cube?
   // -----------------------------------------------------------------------

   public static class CubeFaceTriangle extends Mesh.Triangle implements TextureCoordProvider {
      public int face;
      public Triangle2 texCoords;
      
      public void setTextureCoords(Vector2 t0, Vector2 t1, Vector2 t2) {
         texCoords = new Triangle2(t0, t1, t2);
      }
      @Override
      public Triangle2 getTextureCoords() {
         return texCoords;
      }
   }

   public static MeshModel createUnitCube () {
      
      final Mesh mesh = new Mesh(new Mesh.Factory(){
         public CubeFaceTriangle newTriangle() { return new CubeFaceTriangle(); }
      });
      final MeshModel m = new MeshModel(mesh);
      System.out.format("Cube...\n");
      
      final Vector3 cntr = Vector3.Z;
      final Vector3 dX   = Vector3.X;
      final Vector3 dY   = Vector3.Y;
      final float halfpi = (float) (Math.PI/2);
      
      addSquare(m, cntr.rotated(Vector3.X, -halfpi),
                     dX.rotated(Vector3.X, -halfpi),
                     dY.rotated(Vector3.X, -halfpi), 0);

      for (int i = 0; i < 4; ++i) {
         float angle = i * halfpi;
         addSquare(m, cntr.rotated(Vector3.Y, angle),
                        dX.rotated(Vector3.Y, angle),
                        dY.rotated(Vector3.Y, angle), 1+i);
      }
      
      addSquare(m, cntr.rotated(Vector3.X, halfpi),
                     dX.rotated(Vector3.X, halfpi),
                     dY.rotated(Vector3.X, halfpi), 5);
      
      //m.mesh.testAddAndDelete();
      return m;
   }
   
   private static void addSquare (MeshModel m, Vector3 center, Vector3 dx, Vector3 dy, int face) {
      final Vector3 tr = center.plus(dx).plus(dy);
      final Vector3 tl = center.minus(dx).plus(dy);
      final Vector3 br = center.plus(dx).minus(dy);
      final Vector3 bl = center.minus(dx).minus(dy);
      
      CubeFaceTriangle bottomLeft = (CubeFaceTriangle) m.addTriangle(bl, br, tl);
      CubeFaceTriangle topRight   = (CubeFaceTriangle) m.addTriangle(tl, br, tr);
      
      final Vector2 uv00 = new Vector2(0.0f, 0.0f);
      final Vector2 uv10 = new Vector2(1.0f, 0.0f);
      final Vector2 uv01 = new Vector2(0.0f, 1.0f);
      final Vector2 uv11 = new Vector2(1.0f, 1.0f);
      
      bottomLeft.face = face;
      bottomLeft.setTextureCoords(uv01,uv11,uv00);
      
      topRight.face = face;      
      topRight.setTextureCoords(uv00,uv11,uv10);      
   }

   
   // -----------------------------------------------------------------------
   // New-style Sphere?
   // -----------------------------------------------------------------------

   public static class SphereFaceTriangle extends Mesh.Triangle {
      public Vector2[] latlons;

      public void setLatLons(Vector2 t0, Vector2 t1, Vector2 t2) {
         latlons = new Vector2[] { t0, t1, t2 };
      }
   }
   
   private static Vector3 latLonToPosition(Vector2 latlon) {
      float cosLat = (float) Math.cos(latlon.x);
      float sinLat = (float) Math.sin(latlon.x);
      float cosLon = (float) Math.cos(latlon.y);
      float sinLon = (float) Math.sin(latlon.y);
      return new Vector3(cosLat * cosLon, cosLat * sinLon, sinLat);
   }
   private static Vector2 positionToLatLon(Vector3 pos) {
      float lat = (float) Math.asin(pos.z);
      float lon = (float) Math.atan2(pos.y,pos.x);
      if (lon < 0) lon += (float)(2 * Math.PI);
      return new Vector2(lat,lon);
   }
   private static Vector2 latLonMidpoint (Vector2 a, Vector2 b) {
      Vector3 ap = latLonToPosition(a);
      Vector3 bp = latLonToPosition(b);
      Vector3 midP = ap.plus(bp).times(0.5f).normalized();
      return positionToLatLon(midP);
   }
   
   private static void addLatLonTriangle(MeshModel m, Vector2 latlon0, Vector2 latlon1, Vector2 latlon2) {
      SphereFaceTriangle t = (SphereFaceTriangle) m.addTriangle(
            latLonToPosition(latlon0), latLonToPosition(latlon1), latLonToPosition(latlon2));      
      t.setLatLons(latlon0, latlon1, latlon2);
   }

   private static Shader.ManagedBuffer newSphereTextureCoordsArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               SphereFaceTriangle t = (SphereFaceTriangle) tb;
               
               Vector2 tex0 = latLonToTexCoord(t.latlons[0].x, t.latlons[0].y);
               Vector2 tex1 = latLonToTexCoord(t.latlons[1].x, t.latlons[1].y);
               Vector2 tex2 = latLonToTexCoord(t.latlons[2].x, t.latlons[2].y);        

               // Adjusting the tex0/tex1/tex2 LONGITUDES to be nearby each other
               float eps = .000001f;
               
               { float d1 = (float) Math.abs(tex1.x     -tex0.x);
                 float d2 = (float) Math.abs(tex1.x+1.0f-tex0.x);
                 if (d2 < d1) {
                    tex1 = tex1.plus(Vector2.X);
                 } else {
                    float d3 = (float) Math.abs(tex1.x-1.0f-tex0.x);
                    if (d3 < d1) {
                       tex1 = tex1.minus(Vector2.X);
                    }
                 }
               }
               { float d1 = (float) Math.abs(tex2.x    -tex1.x);
                 float d2 = (float) Math.abs(tex2.x+1.0f-tex1.x);
                 if (d2 < d1) {
                    tex2 = tex2.plus(Vector2.X);
                 } else {
                    float d3 = (float) Math.abs(tex2.x-1.0f-tex1.x);
                    if (d3 < d1) {
                       tex2 = tex2.minus(Vector2.X);
                    }
                 }
               }

               // emit the 3 texture coords:
               float tex0w = (float) Math.cos((tex0.y-0.5)*Math.PI) +eps;
               float tex1w = (float) Math.cos((tex1.y-0.5)*Math.PI) +eps;
               float tex2w = (float) Math.cos((tex2.y-0.5)*Math.PI) +eps;
               
               pPos = toVector4f(tex0,tex0w).copyToFloatArray(array, pPos);
               pPos = toVector4f(tex1,tex1w).copyToFloatArray(array, pPos);
               pPos = toVector4f(tex2,tex2w).copyToFloatArray(array, pPos);
            }
         }
         
         private Vector2 latLonToTexCoord(float lat, float lon) {
            while (lon < 0)         lon += (float) (2 * Math.PI);
            while (lon > 2*Math.PI) lon -= (float) (2 * Math.PI);
            float x = (float) (lon / (2.0 * Math.PI));
            float y = 1.0f - (float) (((Math.PI/2.0) + lat) / Math.PI);
            return new Vector2(x,y);
         }
         
         private Vector4 toVector4f(Vector2 tex1, float w) {
            return new Vector4(tex1.x*w, tex1.y*w, 0.0f, w);
         }
      };
   }
   

   public static MeshModel createUnitSphere (int numLatDivisions, int numLonDivisions) {
      final Mesh mesh = new Mesh(new Mesh.Factory(){
         public SphereFaceTriangle newTriangle() { return new SphereFaceTriangle(); }
      });
      final MeshModel m = new MeshModel(mesh);      
      m.setManagedBuffer(Shader.TEX_COORDS, newSphereTextureCoordsArrayManager(m.mesh));

      double globalLatMin = -Math.PI/2;
      double globalLatMax =  Math.PI/2;
      double globalLonMin = 0;
      double globalLonMax = 2*Math.PI;
      
      for (int lat = 0; lat < numLatDivisions; lat++) {
         for (int lon = 0; lon < numLonDivisions; lon++) {
            
            float latMin = (float) (globalLatMin + ((lat * (globalLatMax - globalLatMin)) / numLatDivisions));
            float latMax = (float) (globalLatMin + (((lat+1) * (globalLatMax - globalLatMin)) / numLatDivisions));
            float lonMin = (float) (globalLonMin + ((lon * (globalLonMax - globalLonMin)) / numLonDivisions));
            float lonMax = (float) (globalLonMin + (((lon+1) * (globalLonMax - globalLonMin)) / numLonDivisions));
            
            Vector2 tR = new Vector2(latMax, lonMax);
            Vector2 tL = new Vector2(latMax, lonMin);
            Vector2 bR = new Vector2(latMin, lonMax);
            Vector2 bL = new Vector2(latMin, lonMin);
            
            if (lat > 0) {
               addLatLonTriangle(m, tL,bL,bR);
            }
            if (lat < numLatDivisions-1) {
               addLatLonTriangle(m, tL,bR,tR);
            }
         }
      }
      return m;
   }
   
   // -----------------------------------------------------------------------
   // ICO
   // -----------------------------------------------------------------------

   public static MeshModel createIco(int subdivisions) {
      final Mesh mesh = new Mesh(new Mesh.Factory(){
         public SphereFaceTriangle newTriangle() { return new SphereFaceTriangle(); }
      });
      final MeshModel m = new MeshModel(mesh);      
      m.setManagedBuffer(Shader.TEX_COORDS, newSphereTextureCoordsArrayManager(m.mesh));

      float pi   = (float)Math.PI;
      float lat0 = pi/2;
      float lat1 = (float) Math.atan(0.5);
      float lat2 = -lat1;
      float lat3 = -lat0;
      
      for (int i = 0; i < 5; ++i) {
         float lon0 = 2*i*(pi/5);
         float lon1 = lon0+(pi/5);
         float lon2 = lon1+(pi/5);
         float lon3 = lon2+(pi/5);
         
         // Top
         addLatLonTriangles(m, new Vector2(lat0,lon1),
                               new Vector2(lat1,lon0),
                               new Vector2(lat1,lon2), subdivisions);
         // "interior"
         addLatLonTriangles(m, new Vector2(lat1,lon0),
                               new Vector2(lat2,lon1),
                               new Vector2(lat1,lon2), subdivisions);
         
         addLatLonTriangles(m, new Vector2(lat1,lon2),
                               new Vector2(lat2,lon1),
                               new Vector2(lat2,lon3), subdivisions);

         // Bottom
         addLatLonTriangles(m, new Vector2(lat2,lon1),
                               new Vector2(lat3,lon2),
                               new Vector2(lat2,lon3), subdivisions);
      }      
      return m;
   }
   
   private static void addLatLonTriangles(MeshModel m, Vector2 latlon0, Vector2 latlon1, Vector2 latlon2, int subdivisions) {
      if (subdivisions == 0) {
         addLatLonTriangle(m, latlon0, latlon1, latlon2);
         
      } else {
         Vector2 mid01 = latLonMidpoint(latlon0, latlon1);
         Vector2 mid12 = latLonMidpoint(latlon1, latlon2);
         Vector2 mid02 = latLonMidpoint(latlon0, latlon2);
         addLatLonTriangles(m, latlon0,   mid01,  mid02,    subdivisions-1);
         addLatLonTriangles(m, mid01,   latlon1,  mid12,    subdivisions-1);
         addLatLonTriangles(m, mid02,     mid12,  latlon2,  subdivisions-1);
         addLatLonTriangles(m, mid12,     mid02,  mid01,    subdivisions-1);
      }
   }
   

   // -----------------------------------------------------------------------
   // BEZIER / CYLINDER ...
   //
   // I wish we had robust intersect / union,
   //   but isn't that extremely hard due to the inevitable degeneracies?
   // 
   // How about some kind of marching-cubes thing that walk over
   //   a potential field with a level constant?
   // -----------------------------------------------------------------------

   // -----------------------------------------------------------------------
   // Tube
   // -----------------------------------------------------------------------

   private static Mesh.Vertex[] createVertexRing(Mesh m, Vector3 discCenter, Vector3 discX, Vector3 discY, int numDivisions) {
      Mesh.Vertex[] vertices = new Mesh.Vertex[numDivisions];
      
      for (int i = 0; i < numDivisions; ++i) {
         float minorAangle = (float)(Math.PI * 2 * i / numDivisions);
         
         Mesh.Vertex v;
         v = m.addVertex();
         v.setPosition(discCenter.plus(discX.times((float) Math.cos(minorAangle)))
                                 .plus(discY.times((float) Math.sin(minorAangle))));
         vertices[i] = v;            
      }
      return vertices;
   }
   
   public static MeshModel createTorus (float majorRadius, float minorRadiusA, float minorRadiusB, int majorDivisions, int minorDivisions) {
      MeshModel m = new MeshModel(new Mesh());
      
      
      // Pointing up is Vector3.Z...
      // 

      Mesh.Vertex[] startVertices = new Mesh.Vertex[minorDivisions];
      {
         Vector3 discCenter = new Vector3(majorRadius, 0, 0);
         Vector3 discX = Vector3.X.times(minorRadiusA);
         Vector3 discY = Vector3.Z.times(minorRadiusA);
         startVertices = createVertexRing(m.mesh, discCenter, discX, discY, minorDivisions);
      }
      Mesh.Vertex[] lastVertices = startVertices;      
      for (int i = 1; i <= majorDivisions; ++i) {
         float majorAngle = (float)(Math.PI * 2 * i / majorDivisions);
         Mesh.Vertex[] nextVertices;
         
         if (i == majorDivisions) {
            nextVertices = startVertices;
         } else {
            float rot = (float) (majorAngle/Math.PI);
            if (rot > 1) rot = 2.0f - rot;
            float minorRadius = minorRadiusA * (1.0f-rot) + minorRadiusB * rot;
            
            Vector3 discCenter = new Vector3(majorRadius * (float)Math.cos(majorAngle), majorRadius * (float)Math.sin(majorAngle), 0);
            Vector3 discX = new Vector3((float)Math.cos(majorAngle), (float)Math.sin(majorAngle), 0).times(minorRadius);
            Vector3 discY = Vector3.Z.times(minorRadius);
            nextVertices = createVertexRing(m.mesh, discCenter, discX, discY, minorDivisions);
         }
         for (int j = 0; j < minorDivisions; ++j) {
            Mesh.Vertex sv1 = lastVertices[j];
            Mesh.Vertex sv0 = lastVertices[(j + minorDivisions - 1) % minorDivisions];
            
            Mesh.Vertex ev1 = nextVertices[j];
            Mesh.Vertex ev0 = nextVertices[(j + minorDivisions - 1) % minorDivisions];
            
            m.mesh.addTriangle(sv1, sv0, ev1);
            m.mesh.addTriangle(ev1, sv0, ev0);
         }
         
         lastVertices = nextVertices;
      }
      return m;
   }
   
   
   
   public static MeshModel createCylinder (Vector3 start, Vector3 end, float radius1, float radius2, int numDivisions) {
      MeshModel m = new MeshModel(new Mesh());

      Vector3 discFwd = end.minus(start).normalized();
      Vector3 discX = leastDimension(discFwd);
      Vector3 discY = discX.cross(discFwd);
      
      // Centers of the starting and ending discs
      Mesh.Vertex startVertex = m.mesh.addVertex();
      startVertex.setPosition(start);
      Mesh.Vertex endVertex = m.mesh.addVertex();
      endVertex.setPosition(end);
      
      ArrayList<Mesh.Vertex> startVertices = new ArrayList<Mesh.Vertex>();
      ArrayList<Mesh.Vertex> endVertices = new ArrayList<Mesh.Vertex>();
      
      for (int i = 0; i < numDivisions; ++i) {
         float angle = (float)(Math.PI * 2 * i / numDivisions);
         Vector3 delta = discX.times((float)Math.cos(angle))
                    .plus(discY.times((float)Math.sin(angle)));
         
         Mesh.Vertex v;
         
         v = m.mesh.addVertex();
         v.setPosition(start.plus(delta));
         startVertices.add(v);
         
         v = m.mesh.addVertex();
         v.setPosition(end.plus(delta));
         endVertices.add(v);
      }

      for (int i = 0; i < numDivisions; ++i) {
         Mesh.Vertex sv1 = startVertices.get(i);
         Mesh.Vertex sv0 = startVertices.get((i + numDivisions - 1) % numDivisions);
         
         Mesh.Vertex ev1 = endVertices.get(i);
         Mesh.Vertex ev0 = endVertices.get((i + numDivisions - 1) % numDivisions);
         
         m.mesh.addTriangle(startVertex, sv0, sv1);
         m.mesh.addTriangle(sv1, sv0, ev1);
         m.mesh.addTriangle(ev1, sv0, ev0);
         m.mesh.addTriangle(ev1, ev0, endVertex);
      }
      
      return m;
   }

   private static Vector3 leastDimension(Vector3 a) {
      Vector3 result = Vector3.X;
      if (a.y < a.x) result = Vector3.Y;
      if ((a.z < a.y) && (a.z < a.x)) result = Vector3.Z;
      return result;
   }
   
   // -----------------------------------------------------------------------
   // LEGACY STUFF 1
   // "chopped cube"
   // -----------------------------------------------------------------------
   /*

   public static MeshModel createChoppedCube() {
      MeshModel m = new MeshModel("ChoopedCube");
      
      Vector3 v000 = new Vector3(0.0f,0.0f,0.0f);
      Vector3 v001 = new Vector3(0.0f,0.0f,1.0f);
      Vector3 v010 = new Vector3(0.0f,1.0f,0.0f);
      Vector3 v011 = new Vector3(0.0f,1.0f,1.0f);
      Vector3 v100 = new Vector3(1.0f,0.0f,0.0f);
      Vector3 v101 = new Vector3(1.0f,0.0f,1.0f);
      Vector3 v110 = new Vector3(1.0f,1.0f,0.0f);
      Vector3 v111 = new Vector3(1.0f,1.0f,1.0f);
      
      addTetrahedron(m, 0, v010,v000,v111,v110);
      addTetrahedron(m, 1, v011,v000,v111,v010);
      addTetrahedron(m, 2, v001,v000,v111,v011);
      addTetrahedron(m, 3, v101,v000,v111,v001);
      addTetrahedron(m, 4, v100,v000,v111,v101);
      addTetrahedron(m, 5, v110,v000,v111,v100);
      
      Vector3 axis = new Vector3(1.0f,-1.0f,0.0f).normalized();
      float angle = (float) Math.atan(Math.sqrt(2.0));
      
      for (Mesh1.Vertex v : m.mesh.vertices) {
         Vector3 p = v.getPosition();
         p = p.rotated(axis, angle);
         
         TetrahedronCount tc = (TetrahedronCount) v.getData();
         tc.base = tc.base.rotated(axis, angle);
         tc.offset = new Vector3(tc.offset.x, tc.offset.y, 0.0f);
         
         p = new Vector3(p.x,p.y,p.z/2);
         if (tc.id == 5) {
            System.out.format("VERTEX: \n%s\n", p);
         }
         tc.base = new Vector3(tc.base.x, tc.base.y, tc.base.z/2);
         
         v.setPosition(p);
      }
         
      return m;
   }

   private static class TetrahedronCount {
      public int id;
      public Vector3 base;
      public Vector3 offset;
      TetrahedronCount (int id, Vector3 base, Vector3 offset) {
         this.id = id;
         this.base = base;
         this.offset = offset;
      }
   }
   
   public static void addTetrahedron(MeshModel m, int id, Vector3 a, Vector3 b, Vector3 c, Vector3 d) {
      Vector3 offset = (a.plus(d).times(0.5f)).minus(b.plus(c).times(0.5f)).normalized();
      TetrahedronCount ti = new TetrahedronCount(id, a, offset);
      
      Mesh1.Vertex va = m.mesh.addVertex(a); va.setData(new TetrahedronCount(id, a, offset));
      Mesh1.Vertex vb = m.mesh.addVertex(b); vb.setData(new TetrahedronCount(id, b, offset));
      Mesh1.Vertex vc = m.mesh.addVertex(c); vc.setData(new TetrahedronCount(id, c, offset));
      Mesh1.Vertex vd = m.mesh.addVertex(d); vd.setData(new TetrahedronCount(id, d, offset));
    
      m.addTriangle(va, vb, vc, ti);
      m.addTriangle(vd, vc, vb, ti);
      m.addTriangle(vc, vd, va, ti);
      m.addTriangle(vd, vb, va, ti);
   }

   public static void warpChoppedCube (MeshModel model, float phase, float mag) {
      for (Mesh1.Vertex v : model.mesh.vertices) {
         TetrahedronCount tc = (TetrahedronCount) v.getData();
         v.setPosition(tc.base.plus(tc.offset.times((1.0f - (float) Math.cos(phase)) * mag)));
      }
      model.getManagedBuffer(Shader.POSITION_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V0POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V1POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V2POS_ARRAY).setModified(true);      
   }
   */


   
   // -----------------------------------------------------------------------
   // LEGACY STUFF 2
   // ..
   // from Mesh1.   In Mesh1, Vertex/Triangle objects in the Mesh supported "getData/setData"
   // allowing arbitrary data to be "hung" off each one,
   //
   // For example "everyPointGetsAnInteger" attached an Integer to each Vertex,
   // which was used by a novel shader that painted each triangle in three colors
   // painting a polygon around each vertex.
   //
   // In the New Mesh, it's no longer possible to just "attach" numbers to each Vertex
   // unless the MESH specifically creates a Vertex subclass with that ability.
   // Was this a good design decision?
   //
   // Of course, the ManagedBuffer object could hold an ARRAY of integers, which would
   // work for static meshes.   But if a ManagedBuffer stored per-vertex info in an ARRAY,
   // it would fall out of sync if the Mesh were modified afterward....
   // -----------------------------------------------------------------------
   /*
   
   public static void everyPointGetsAnInteger (MeshModel model, int largestVertexInt) {
      for (Mesh1.Vertex v : model.mesh.vertices) {
         int vertexInt = (int)(largestVertexInt * Math.random());
         v.setData(Integer.valueOf(vertexInt));
      }
      model.setManagedBuffer(Shader.COLOR_ARRAY, pointShadingColorArray(model.mesh));
   }
   
   private static Shader.ManagedBuffer pointShadingColorArray(final Mesh1 mesh) {
      return new Shader.ManagedBuffer(3) {
         @Override public int getNumElements() { return mesh.interiorTriangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh1.Triangle t : mesh.interiorTriangles) {
               int v0Int = (Integer) t.edge0.getOppositeVertex().getData();
               int v1Int = (Integer) t.edge1.getOppositeVertex().getData();
               int v2Int = (Integer) t.edge2.getOppositeVertex().getData();
               
               ColorARGB color = new ColorARGB((byte)0x00, (byte)v0Int, (byte)v1Int, (byte)v2Int);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
               pPos = copyColor(array, pPos, color);
            }
         }
         private int copyColor(float[] arr, int base, ColorARGB c) {
             arr[base+0] = ((float)(c.r&0xff))/255.0f;
             arr[base+1] = ((float)(c.g&0xff))/255.0f;
             arr[base+2] = ((float)(c.b&0xff))/255.0f;
             return base+3;
         }
      };
   }
   */
   public static Image.Floats createMeshInfoImage(MeshModel model) {
      Image.Floats result = new Image.Floats(9, model.mesh.triangles.size());
      
      int row = 0;
      for (Mesh.Triangle tb : model.mesh.triangles) {
         Triangle2 texCoords = ((TextureCoordProvider) tb).getTextureCoords();
         {
            Vector2 p0 = texCoords.v0;
            Vector2 p1 = texCoords.v1;
            Vector2 p2 = texCoords.v2;
            
            double A,B,C,D,E,F,G,H,I;

            double u0 = p0.x, v0 = p0.y;
            double u1 = p1.x, v1 = p1.y;
            double u2 = p2.x, v2 = p2.y;
            
            double den = u0*v1 - u0*v2 + u1*v2 - u1*v0 + u2*v0 - u2*v1;
            A = (v1-v2)/den;  B = (u2-u1)/den;  C = (u1*v2-u2*v1)/(den);
            D = (v2-v0)/den;  E = (u0-u2)/den;  F = (u2*v0-u0*v2)/(den);
            G = (v0-v1)/den;  H = (u1-u0)/den;  I = (u0*v1-u1*v0)/(den);

//            result.set((row%2==0)?0:1, row, -1.0f);
//            result.set((row%2==0)?1:0, row, 0);
            
//            result.set(0, row, Float.floatToIntBits((float)A));
//            result.set(1, row, Float.floatToIntBits((float)B));
            //result.set(0, row, (float)A);
/*
             result.set(0, row, (row%2==0) ? 2.0f : 0.0f);
 
            result.set(1, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(2, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(3, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(4, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(5, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(6, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(7, row, (row%2==0) ? 2.0f : 0.0f);
            result.set(8, row, (row%2==0) ? 2.0f : 0.0f);
 */
            result.set(0, row, (float)A);
            result.set(1, row, (float)B);
            result.set(2, row, (float)C);
            result.set(3, row, (float)D);
            result.set(4, row, (float)E);
            result.set(5, row, (float)F);
            result.set(6, row, (float)G);
            result.set(7, row, (float)H);
            result.set(8, row, (float)I);
         }
         
         row++;
      }
      return result;
   }
   
   // -----------------------------------------------------------------------
   // Apply modifications..
   // -----------------------------------------------------------------------
   
   public static void sphereWarp2 (MeshModel model, float phase, float mag) {
      for (Mesh.Vertex v : model.mesh.vertices) {
         Vector3 p = v.getPosition().normalized();
 
         Vector2 p2 = positionToLatLon(p);
         float phase2 = (float)(6.0 * p2.y);
         
         p = p.times((float)(1.0 - mag * Math.sin(phase) * Math.sin(phase2)));
         v.setPosition(p);
      }
      model.getManagedBuffer(Shader.POSITION_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V0POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V1POS_ARRAY).setModified(true);
      model.getManagedBuffer(Shader.V2POS_ARRAY).setModified(true);
   }
   
}

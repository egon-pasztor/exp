package com.generic.base;

import com.generic.base.Algebra.Triangle2;
import com.generic.base.Algebra.Vector2;
import com.generic.base.Algebra.Vector3;
import com.generic.base.Geometry.MeshModel;
import com.generic.base.Geometry.TextureCoordProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

public class QuadCover {

   public static class Vertex extends Mesh.Vertex {
      public int colorCode;
      // Every vertex has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the VERTEX a specific color..
      
      public int cutGraphValence;
      public boolean needsDuplication;
   }
   public static class Edge extends Mesh.Edge {
      public int colorCode;
      
      // This is the distance between the centroids of the two triangles
      // on either side of this Edge, or null if this Edge is on a boundary.
      
      public Float dualLen;
      
      
      // --------------------------------
      // Cut graph stuff:
      // --------------------------------
      
      public boolean inCutGraph;
      public boolean isEndOfCutGraph() {
         if (!inCutGraph) return false;
         
         QuadCover.Vertex v1 = (QuadCover.Vertex) getFirst().start();
         QuadCover.Vertex v2 = (QuadCover.Vertex) getFirst().end();            
         return ((v1.cutGraphValence == 1) || (v2.cutGraphValence == 1));
      }
      
   }
   public static class Boundary extends Mesh.Boundary {
      public int colorCode;

      // Every boundary has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the adjacent-triangle's edge a specific color..      
   }
   public static class Triangle extends Mesh.Triangle implements TextureCoordProvider {
      public int colorCode;
      
      // --------------------------------

      public Vector3 center;
      public boolean fixed;
      public float distanceToRoot;
      public Integer spanningTreeIndexTowardsRoot;

      // Every triangle has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the triangle a specific color..
      
      public Triangle2 texCoords;
      public Vector3 direction;
      
      public void setTextureCoords(Vector2 t0, Vector2 t1, Vector2 t2) {
         texCoords = new Triangle2(t0, t1, t2);
      }
      @Override
      public Triangle2 getTextureCoords() {
         return texCoords;
      }
   }
   
   public static Mesh newMesh() {
      return new Mesh(new Mesh.Factory(){
         public QuadCover.Triangle newTriangle() { return new QuadCover.Triangle(); }
         public QuadCover.Edge newEdge()         { return new QuadCover.Edge(); }
         public QuadCover.Vertex newVertex()     { return new QuadCover.Vertex(); }
         public QuadCover.Boundary newBoundary() { return new QuadCover.Boundary(); }
      });      
   } 


   // -----------------------------------------------------------------------------------
   // Array Assembly for Shaders
   // -----------------------------------------------------------------------------------
   
   
   private static Shader.ManagedBuffer newDirectionArrayManager(final Mesh mesh) {
      return new Shader.ManagedFloatBuffer(3) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(float[] array) {
            int pPos = 0;
            for (Mesh.Triangle tb : mesh.triangles) {
               QuadCover.Triangle t = (QuadCover.Triangle) tb;
               Vector3 v = t.direction;
               pPos = (v).copyToFloatArray(array, pPos);
               pPos = (v).copyToFloatArray(array, pPos);
               pPos = (v).copyToFloatArray(array, pPos);
            }
         }
      };
   }
   private static Shader.ManagedBuffer newTriangleColoringInfo(final Mesh mesh) {
      return new Shader.ManagedIntBuffer(4) {
         @Override public int getNumElements() { return mesh.triangles.size() * 3; }
         @Override public void fillBuffer(int[] array) {
            int pPos = 0;
            int numA=0, numB=0, numT=0;
            for (Mesh.Triangle tb : mesh.triangles) {
               QuadCover.Triangle t = (QuadCover.Triangle) tb;
               
               // For triangle t we need to "aggregate" the colorCodes of all
               // vertices, edges, and boundaries, in addition to the one
               // from this triangle t:
               
               int tColorCode = t.colorCode;
               
               int v0ColorCode = ((QuadCover.Vertex)(t.vertices[0])).colorCode;
               int v1ColorCode = ((QuadCover.Vertex)(t.vertices[1])).colorCode;
               int v2ColorCode = ((QuadCover.Vertex)(t.vertices[2])).colorCode;
               
               int e0ColorCode = ((QuadCover.Edge)(t.edges[0].getEdge())).colorCode;
               int e1ColorCode = ((QuadCover.Edge)(t.edges[1].getEdge())).colorCode;
               int e2ColorCode = ((QuadCover.Edge)(t.edges[2].getEdge())).colorCode;
               
               int b0ColorCode = (!t.edges[0].opposite().isBoundary()) ? 0 : ((QuadCover.Boundary)(t.edges[0].opposite())).colorCode;
               int b1ColorCode = (!t.edges[1].opposite().isBoundary()) ? 0 : ((QuadCover.Boundary)(t.edges[1].opposite())).colorCode;
               int b2ColorCode = (!t.edges[2].opposite().isBoundary()) ? 0 : ((QuadCover.Boundary)(t.edges[2].opposite())).colorCode;
               
               for (int i = 0; i < 3; ++i) {
                  array[pPos+0] = tColorCode;
                  array[pPos+1] = (v0ColorCode << 16) | (v1ColorCode << 8) | (v2ColorCode);
                  array[pPos+2] = (e0ColorCode << 16) | (e1ColorCode << 8) | (e2ColorCode);
                  array[pPos+3] = (b0ColorCode << 16) | (b1ColorCode << 8) | (b2ColorCode);
                  pPos += 4;
               }
            }
            System.out.format("FILL-BUFFER called yay:  %d numA, %d numB, %d numT\n", numA,numB,numT);
         }
      };
   }
   
   public static void run(MeshModel model, float xOffset, int div) {
      setTextureCoords(model.mesh);
      
      // Organizer.setTextureCoords doesn't actually set anything yet,
      // so here's some "default" texture setting code:

      Vector2 p0 = new Vector2(0.0f, 0.0f);
      Vector2 p1 = new Vector2(1.0f, 0.0f);
      Vector2 p2 = new Vector2(0.5f, 0.866f);
      float margin = 0.1f;
      
      int tCount = 0;
      for (Mesh.Triangle tb : model.mesh.triangles) {
         int yCount = tCount/div;
         int xCount = tCount - (yCount*div);
         Vector2 base = new Vector2(xOffset + margin + (1.0f+margin) * xCount, margin + (1.0f+margin) * yCount); 
               
         QuadCover.Triangle t = (QuadCover.Triangle) tb;
         t.setTextureCoords(base.plus(p0), base.plus(p1), base.plus(p2));
         tCount++;
      }
      
      model.setManagedBuffer(Shader.DIRECTION_SHADING_ARRAY, newDirectionArrayManager(model.mesh));
      model.setManagedBuffer(Shader.COLOR_INFO, newTriangleColoringInfo(model.mesh));
      
      
   }
   
   
   
   /*
   the PAPER talks about discrete DIV and discrete CURL

   vector fields are RESTRICTED to being piecewise constant... with "one vector in the tangent space of each triangle"..
   
   
          div U at vertex v_i   == - SUM (for all triangles T around vertex v_i)
          
                                           return    U  (vector for triangle T)
                                                   DOT  (grad of S_h basis function for v_i)
                                                 TIMES  (area of triangle T)

         curl U at vertex v_i   == - SUM (for all triangles T around vertex v_i)
          
                                           return   JU  (vector for triangle T that's ROTATED counterclockwise 90-degrees)
                                                   DOT  (grad of S_h basis function for v_i)
                                                 TIMES  (area of triangle T)

                                                
                 (GRAD of S_h basis function for v_i) IS nonzero in triangles T containing v_i 
                     in a triangle T containing v_i, the vector lies in the triangle,
                         is perpendicular to the edge OPPOSITE vertex v_i,
                         and its magnituide is ???
                         
 

           div* U at edge e_i   == - SUM (for two (or one) triangles T on either side of e_i)
          
                                           return    U  (vector for triangle T)
                                                   DOT  (grad of S*_h basis function for e_i)
                                                 TIMES  (area of triangle T)

          curl* U at edge e_i   == - SUM (for two (or one) triangles T on either side of e_i)
          
                                           return   JU  (vector for triangle T that's ROTATED counterclockwise 90-degrees)
                                                   DOT  (grad of S*_h basis function for e_i)
                                                 TIMES  (area of triangle T)

                               == (1/2) { (U_l - U_r) DOT e_i
                                      
                                      

                 (GRAD of S*_h basis function for e_i) IS nonzero in triangles T containing e_i 
                     in a triangle T containing e_i, the vector lies in the triangle,
                         is perpendicular to the edge e_i,
                         and its magnituide is ??
                         
     
                         
     ..................................................................
     the old GRAD paper says it best:
          if T is a triangle with vertices v0,v1,v2
                               and edges e0,e1,e2 such that e_i = v_(i-1) - v_(i+1)
                                                            e0 = v2 - v1
                                                            e1 = v0 - v2
                                                            e2 = v1 - v0
                                                            
         let u in S_h be a piecewise linear function -- u_i = u(v_i)
              
         then          GRAD u  = (1/(2 * area-T))  SUM(0,1,2):{  u_i J e_i }
                               = (1/(2 * area-T))  { u(v_0) J e_0  + u(v_1) J e_1 + u(v_2) J e_2 }
         
         
     this is important:
     
          GRAD U  DOT  e_i  == u_(i-1) - U_(i+1)
          
          
          ==  (1/(2 * area-T))  { u(v_0) (J e_0) DOT e_0  + u(v_1) (J e_1) DOT e_0 + u(v_2) (J e_2) DOT e_0 }
          ==  (1/(2 * area-T))  {                           u(v_1) (J e_1) DOT e_0 + u(v_2) (J e_2) DOT e_0 }
         
     (near page 170 of PIECEWISE)
     ..................................................................
                         
     ..................................................................
     ..................................................................
     SUMMARY 
 
        if f(v_i) is a FLOAT on each VERTEX ..
        
               ... f is a member of S_h, a scalar valued function, linear on each triangle, continuous
               ... we can compute GRAD f,
                           which is a piecewise-linear vector field.
               
               
        if f(v_i) is a FLOAT on each EDGE ..
        
               ... f is a member of S*_h, a scalar valued function, linear on each triangle, continuous on edge-midpoints only
               ...    can we compute GRAD f ??  we HOPE but I DON"T KNOW how yet...
                      it would be a piecewise-linear vector field.
                       
               
               
        if U is a piecewise-linear vector field               
            
            then:   div_U (v_i)   is a scalar at each vertex (a member of S_h)
                   div*_U (e_i)   is a scalar at each edge   (a member of S*_h)
                    curl_U (v_i)  is a scalar at each vertex (a member of S_h)
                  curl*_U (e_i)   is a scalar at each edge   (a member of S*_h)
                
                         
                         
  ===============================================================
    Foundations page 22 says:   for vector field X defined on each X_ti
    
                             
         divX (v_i)  =   (-1/2) SUM(for all triangles t_i containing v_i)(X_ti DOT J e_i)
                         
             (go around the "STAR" visit all triangles t_i looping around vertex v_i, 
                                                   add X_ti dotted with vector pointing INTO STAR (that's J e_i)
                         
         div*X (e_i) =   X_s DOT (J e_i)  -  X_t DOT (J e_i)
         
             (for e_i, the vector J e_i is normal to the edge.   Dot the two X vectors with these in each triangle..)
             
             
        curl X (v_i)  =   (+1/2) SUM(for all triangles t_i containing v_i)(X_ti DOT e_i)
         
             (go around the "STAR" visit all triangles t_i looping around vertex v_i, 
                                                   add X_ti dotted with STAR BOUNDARY EDGE (e_i))

                      =  -div JX(v_i)
                                                   
        curl* X (e_i)  =  X_s DOT e_i  -  X_t DOT e_i
                                                   
                      =  -div* JX(e_i)
                         
                         
    point is, these are all "functions we can write an evaluator for"..
    given "Vector in Face" list,
    
         we can compute "Value at any Vertex" (for divX and curlX)
                     or "Value at any Edge"   (for div*X and curl*X)
                     
                     
     ..................................................................
     ..................................................................
                         
       
   also curl  (U) is div  (JU)
        curl* (U) is div* (JU)
   
   
                            
    ===========================================

   note (page 18):
   
       In order to COMPUTE the potential u in S_h of a curl*-free field U.
       
          pick a ROOT vertex to set u(v0) = 0.
          for any other vertex,
                                    u(v_i) is INTEGRAL of (U DOT dPath/Ds) * Ds.
                                    
                                           or SUM of (p_i+1,p_i)  (vector from p_i to p_i+1 == edge vector)
                                                     DOT
                                                     U            (in either triangle.  it shouldn't matter which one,
                                                                   because U is curl*-free which MEANS that U dotted with an edge
                                                                                                is the same on both sides of each edge.
  
   
      so if we HAD a curl*-free field U,
      
         we'd get u_i = u(v_i) by making an arbitrary PATH (or, shortest path)
                                                 from v_0 to v_i,
                                                 for each EDGE e_i along that path,
                                                   sum U in EITHER triangle adjacent to e_i
                                                       and DOT with e_i
                                                       
    ===========================================
       officially the STEPS are:
       
            Given Arbitrary input vector field U.
       
       
            Solve   L* ga* = dU
            
                 where  L* is a mesh-connectivity matrix,      ExE matrix
                       ga* is a vector of values on each edge  (E dimensional vector of unknowns)
                       dU  is a vector of values on each edge  (E dimensional vector of curl*U at each edge midpoint)
                       
        (1)  Find the ga* unknowns .. 
                         this gives you a function in S*_h
                         
        (2)  Find the GRADIENT (note, we still don't know how to find gradient of S*_h functions)
        
        (3)  Subtract this GRADIENT from vector field U, 
                 resulting in curl*-free vector field that we call "GRAD u" because, 
                    since it's curl-free, it must be the GRADIENT of some u
                 
        (4)  Compute the CUT-GRAPH / Shortest-path-tree analysis...
                 and INTEGRATE "along edges not in the CUT-GRAPH"
                 to assign u to all vertices.
                 
        ....
        (5)  Repeat to find v for all vertices from input vector field V.
        
        

        (6) ... not every vertex has (u,v), but don't they overlap?
                 maybe "remove overlaps and arrange on page" is a separate not-described step?
                 
                 
        
        
                         
    ===========================================

     so, point is there's gonna be steps:
     
       1.   GET a shortest-path tree that allows us to compute the shortest set of edges to get from v_0 to ANY v_i.
       
       2.   GET a curl-*-free field U       
       
       3.   use the algorithm above to assign u-coordinates to each vertex 
     
                                 
     repeat the above with curl*-free field V
     now each vertex will have (u,v), let's see what it looks like
   
    ===========================================
       1.   GET a shortest-path tree that allows us to compute the shortest set of edges to get from v_0 to ANY v_i.
       
       
               this means we will have "ROOT" vertex v_0,
               all other vertices will have a "route_toward_ROOT" variable, selecting which outgoing edge to take        
               given vertex_v, follow "route_toward_ROOT" edge to the next vertex, until we reach ROOT
        
        
            to COMPUTE this,
            
               select "ROOT" vertex v_0
               add v_0 to "nodes to consider" (value 0)
               
               SELECT v_i from "nodes to consider" the node with lowest value
               
                  this v_i is now FIXED, its value(v_i) is the best possible
                  for each outgoing edge e from v_i
                        consider destination vertex v.
                            if it's not FIXED, 
                               it's either already seen (present in "nodes to consider")
                                                 or new (never seen in "nodes to consider")
                                                 
                        compare   length(e) + value(v_i)   with the value that vertex has if any...
    
    ===========================================
      okay.   i believe if we had curl*-free field U,V, 
              that I could assign a <u,v> pair to each triangle.
              
              but, wouldn't that overlap?
              
   */
   
   
   
   public static void setTextureCoords(Mesh mesh) {
      int triangleCount = mesh.triangles.size();
      
      System.out.format("\nWe have a MESH with %d triangles, %d vertices, %d edges, %d boundaries\n", 
            mesh.triangles.size(), mesh.vertices.size(), mesh.edges.size(), mesh.boundaries.size());
      
      mesh.checkMesh();
      
      for (Mesh.Triangle tb : mesh.triangles) {
         QuadCover.Triangle t = (QuadCover.Triangle) tb;
         
         Vector3 selectedDirection;
         selectedDirection = new Vector3(
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f));
         
         selectedDirection = Vector3.X;
         t.direction = selectedDirection;
      }
      System.out.format("Okay, STARTED setTextureCoords for a mesh with %d triangles...\n",  mesh.triangles.size());
      
      // -----------------------------------------------------------
      // CUT-GRAPH
      // -----------------------------------------------------------
      
      // STEP 1:  compute "dualLen" for each non-boundary edge.
      
      for (Mesh.Triangle tb : mesh.triangles) {
         QuadCover.Triangle t = (QuadCover.Triangle) tb;
         t.center = (t.vertices[0].getPosition()
               .plus(t.vertices[0].getPosition())
               .plus(t.vertices[0].getPosition())).times(1.0f/3.0f);
         
         t.fixed = false;
         t.spanningTreeIndexTowardsRoot = null;
      }
      for (Mesh.Edge eb : mesh.edges) {
         QuadCover.Edge e = (QuadCover.Edge) eb;
         if (!e.isBoundary()) {
            QuadCover.Triangle t1 = (QuadCover.Triangle) e.getFirst().getTriangle();
            QuadCover.Triangle t2 = (QuadCover.Triangle) ((Mesh.Triangle.Edge) e.getSecond()).getTriangle();
            e.dualLen = t1.center.minus(t2.center).length();
         }
         e.inCutGraph = true;
      }

      // STEP 2:  compute a shortest-path-tree for triangles from some root triangle
      
      PriorityQueue<QuadCover.Triangle> queue = new PriorityQueue<QuadCover.Triangle>(new Comparator<QuadCover.Triangle>() {
         @Override
         public int compare(QuadCover.Triangle o1, QuadCover.Triangle o2) {
            return Float.compare(o1.distanceToRoot, o2.distanceToRoot);
         }});
      
      QuadCover.Triangle t0 = (QuadCover.Triangle) mesh.triangles.get(0);
      t0.distanceToRoot = 0.0f;
      queue.add(t0);
      
      while (!queue.isEmpty()) {
         QuadCover.Triangle t = queue.remove();
         t.fixed = true;
         
         for (Mesh.Triangle.Edge et : t.edges) {
            QuadCover.Edge e = (QuadCover.Edge) et.getEdge();
            // okay... we're using the "e" object to represent the DUAL edge between this triangle t
            // and some other triangle maybe on the other side of "et"
            
            if (!e.isBoundary()) {
               Mesh.Triangle.Edge etOpposite = (Mesh.Triangle.Edge) et.opposite();
               QuadCover.Triangle otherTriangle = (QuadCover.Triangle) etOpposite.getTriangle();
               if (!otherTriangle.fixed) {
                  float proposedDistanceToRootForOtherTriangle = t.distanceToRoot + e.dualLen;
                  if ((otherTriangle.spanningTreeIndexTowardsRoot != null) && 
                      (otherTriangle.distanceToRoot > proposedDistanceToRootForOtherTriangle)) {
                  
                     otherTriangle.spanningTreeIndexTowardsRoot = null;
                     queue.remove(otherTriangle);
                  }
                  if (otherTriangle.spanningTreeIndexTowardsRoot == null) {
                     otherTriangle.spanningTreeIndexTowardsRoot = etOpposite.getEdgeIndex();
                     otherTriangle.distanceToRoot = proposedDistanceToRootForOtherTriangle;
                     queue.add(otherTriangle);
                  }
               }
            }
         }
      }
      
      // STEP 2:  edges that cross the shortest-path-tree are not in the cut-graph

      for (Mesh.Triangle tb : mesh.triangles) {
         QuadCover.Triangle t = (QuadCover.Triangle) tb;
         if (t.spanningTreeIndexTowardsRoot != null) {
            Triangle.Edge edgeCrossingPathToRoot = t.edges[t.spanningTreeIndexTowardsRoot];
            QuadCover.Edge e = (QuadCover.Edge) edgeCrossingPathToRoot.getEdge();
            e.inCutGraph = false;
         }
      }
      
      
      // compute cut graph valence for each vertex
      
      for (Mesh.Vertex vb : mesh.vertices) {
         QuadCover.Vertex v = (QuadCover.Vertex) vb;
         int valence = 0;
         for (Mesh.DirectedEdge outgoingEdge : v.outgoingEdges()) {
            QuadCover.Edge e = (QuadCover.Edge) outgoingEdge.getEdge();
            if (e.inCutGraph) valence++;
         }
         v.cutGraphValence = valence;
         v.needsDuplication = false;
      }
      
      // locate all cut-graph with a valence=1 vertex
      
      int numEdges = mesh.edges.size();
      int numFullCutGraphEdges = 0;
      
      ArrayList<QuadCover.Edge> fakeCutGraphEdges = new ArrayList<QuadCover.Edge>();
      for (Mesh.Edge eb : mesh.edges) {
         QuadCover.Edge e = (QuadCover.Edge) eb;
         if (e.inCutGraph) numFullCutGraphEdges++;
         if (e.isEndOfCutGraph()) fakeCutGraphEdges.add(e);
      }
      
      //System.out.format("Starting brute force with %d out of %d edges\n", numFullCutGraphEdges, numEdges);

      int it = 0;
      while (!fakeCutGraphEdges.isEmpty()) {
         ArrayList<QuadCover.Edge> newFakeCutGraphEdges = new ArrayList<QuadCover.Edge>();
         int numEdgesToRemove = fakeCutGraphEdges.size();
         int numEdgesRemoved = 0;
         for (QuadCover.Edge edgeToRemove : fakeCutGraphEdges) {
            if (edgeToRemove.inCutGraph) {
               QuadCover.Vertex v1 = (QuadCover.Vertex) edgeToRemove.getFirst().start();
               QuadCover.Vertex v2 = (QuadCover.Vertex) edgeToRemove.getFirst().end();
               
               numEdgesRemoved++;
               edgeToRemove.inCutGraph = false;
               v1.cutGraphValence--;
               v2.cutGraphValence--;

               for (QuadCover.Vertex v : Arrays.asList(v1,v2)) {
                  for (Mesh.DirectedEdge outgoingEdge : v.outgoingEdges()) {
                     QuadCover.Edge e = (QuadCover.Edge) outgoingEdge.getEdge();
                     if (e.isEndOfCutGraph()) newFakeCutGraphEdges.add(e);
                  }
               }
            }
         }
         
         //System.out.format("Iteration %d:  Visiting %d edges in fake-list, removed %d of them, exposing %d edges in the next fake-list\n", 
         //      it++, numEdgesToRemove, numEdgesRemoved, newFakeCutGraphEdges.size());
         
         fakeCutGraphEdges = newFakeCutGraphEdges;
      }
      
      
      // -----------------------------------------------------------
      // FUN coloring around triangle0
      // -----------------------------------------------------------
      
      t0.colorCode = 2;
      //System.out.format("  .. Set ROOT triangle colorCode to 1.\n");
      
      // nearby triangles get we're going to want a queue here...      
      HashSet<Mesh.Vertex> verticesToProcess = new HashSet<Mesh.Vertex>();
      for (Mesh.Vertex v : t0.vertices) {
         verticesToProcess.add(v);
         ((QuadCover.Vertex) v).colorCode = 4;
      }
      int nextColorCode = 3;
      while (!verticesToProcess.isEmpty() && nextColorCode < 5) {
         HashSet<Mesh.Vertex> nextVerticesToProcess = new HashSet<Mesh.Vertex>();
         for (Mesh.Vertex v : verticesToProcess) {
            for (Mesh.DirectedEdge outgoingFromV : v.outgoingEdges()) {
               if (!outgoingFromV.isBoundary()) {
                  QuadCover.Triangle t = (QuadCover.Triangle) ((Mesh.Triangle.Edge) outgoingFromV).getTriangle();
                  if (t.colorCode == 0) {
                     t.colorCode = nextColorCode;
                     //System.out.format("  .. Set a triangle colorCode to %d.\n", nextColorCode);
                     for (Mesh.Vertex va : t.vertices) {
                        if (!verticesToProcess.contains(va)) {
                           nextVerticesToProcess.add(va);
                        }
                     }
                  }
               }
            }
         }
         verticesToProcess = nextVerticesToProcess;
         nextColorCode++;
      }
      
      for (Mesh.Boundary e : mesh.boundaries) {
         ((QuadCover.Boundary) e).colorCode = 2;
      }
      System.out.format("Okay, DONE.\n");


      // -----------------------------------------------------------
      // CUT-GRAPH consideration...
      // -----------------------------------------------------------
      // okay, now we have a "cut-graph", hackily using colorCode for
      // this, so all edges with colorCode == specialColor are part of the "cut-graph".
      
      // NEXT we want to "walk around" the cut graph...
      // so, find a directed-edge whose getEdge() os a cut-graph edge. 
      //
      // this directedEdge is the START...
      //
      // look for the next CUT-GRAPH outgoing directedEdge that's CW around END
      //    (all CUT-GRAPH  vertices are valance 2 or higher, so there's guaranteed to be another CUT-GRAPH around END)
      //    
      //    we CREATE a new "boundary vertex" -- 
      //    for each TRIANGLE  between this CUT-GRAPH directedEdge (going into END) 
      //                       and the next CUT-GRAPH directedEdge we just found going OUT,
      //      the TRIANGLE will have 3 uv-texture refs for its 3 vertices,
      //          we must SET the uv-texture that corresponds to the vertex "END", set to point to this "boundary vertex".
      //
      //    now the current directedEdge is set to this "next" directedEdge...
      //    until we're back at START.
      // 
      // and that's it, we've got our canonical set of BOUNDARY VERTICES...
      //
      //
      // then for each VERTEX that's NOT on a CUT-GRAPH,
      //    we assicuate this VERTEX with new "interior vertex" variables,
      //    all TRIANGLE neightbors of this VERTEX gets the appropriacte uv-textures pointed to this "interior vertex".
      //
      // the net result is ...
      //    some number of "boundary vertex" objects that are associated with some triangles on the boundary..
      //    and a number of "interior vertex" objects each one pointing to a Mesh.Vertex that's not on the CUT-GRAPH.
      
      
      // -----------------------------------------------------------
      // MESH cut..
      // -----------------------------------------------------------
      
      class BoundaryRef {
         Mesh.Triangle.Edge edge;
         QuadCover.Vertex newStartVertex;
      }
      ArrayList<BoundaryRef> boundaryLoop = new ArrayList<BoundaryRef>();
      
      Mesh.Triangle.Edge startingEdge = null;
      for (Mesh.Edge eb : mesh.edges) {
         QuadCover.Edge e = (QuadCover.Edge) eb;
         if (e.inCutGraph) {
            e.colorCode = 4;
            startingEdge = e.getFirst();
         } else {
            e.colorCode = 1;
         }
      }
      if (startingEdge == null) {
         throw new RuntimeException("hmm, no edge remaining in cut graph?");
      }
      
      Mesh.Triangle.Edge currentEdge = startingEdge;
      int extraVertices = 0;
      do {
         BoundaryRef b = new BoundaryRef();
         b.edge = currentEdge;
         
         QuadCover.Vertex startVertex = (QuadCover.Vertex) currentEdge.start();
         if (startVertex.needsDuplication) {
            startVertex.colorCode = 7;
            extraVertices++;
            
            b.newStartVertex = (QuadCover.Vertex) mesh.addVertex();
            b.newStartVertex.setPosition(startVertex.getPosition());
            b.newStartVertex.colorCode = 7;
            b.newStartVertex.needsDuplication = true;            
         } else {
            b.newStartVertex = startVertex;
            startVertex.needsDuplication = true;
         }
         boundaryLoop.add(b);
         
         // Okay, we're doing a CUT here..
         // Specifically, we want to REMOVE-and-replace the triangle
         //   to the left of "currentEdge",
         // CAN we even do that?  CurrentEdge may be DESTROYED!
         // 
         // I think.. on balance, that we do not.
         
         Mesh.DirectedEdge nextEdge = currentEdge.prevAroundEnd();
         while (!((QuadCover.Edge)nextEdge.getEdge()).inCutGraph) {
            nextEdge = nextEdge.prevAroundEnd();
         }
         nextEdge = nextEdge.opposite();
         
         currentEdge = (Mesh.Triangle.Edge) nextEdge;
      } while (currentEdge != startingEdge);

      System.out.format("Yeah, looks like we went ALL THE WAY AROUND in %d directedEdges, creating %d extra Vertices\n", 
            boundaryLoop.size(), extraVertices);
      
      
      class VertexTriple {
         Vertex v0,v1,v2;
      }
      ArrayList<Mesh.Triangle> trianglesToDelete = new ArrayList<Mesh.Triangle>();
      ArrayList<VertexTriple> newTrianglesToMake = new ArrayList<VertexTriple>();
      
      // because we dare not modify the mesh while we walk around on it,
      // we try to "boil down" the situation to specific instructions.
      
      for (BoundaryRef br : boundaryLoop) {
         
         // okay.. we DO want to delete br.edge.getTriangle(), sure sure...
         //    but what vertices do we replace them with?
         // we know that br.newStartVertex and [successor to br in boundaryLoop].newStartVertex
         // will be two of the "new vertices" in the replacement triangle...
         //    but is the third vertex also to be replaced?
         //    (In other words, can we tell if BoundaryLopp goes around and also touches the
         //    OTHER vertex of this triangle?  no...)
         
         
      }
   }
   
   
}

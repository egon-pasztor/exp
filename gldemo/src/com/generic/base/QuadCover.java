package com.generic.base;

import com.generic.base.Algebra.*;
import com.generic.base.Geometry.MeshModel;
import com.generic.base.Geometry.TextureCoordProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class QuadCover {

   public static class Vertex extends Mesh.Vertex {
      // Every vertex has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the VERTEX a specific color..
      public int colorCode;
      
      // --------------------------------
      // Cut graph stuff:
      // --------------------------------
      
      public int cutGraphValence;
      
      // --------------------------------
      // Floater-coords
      // --------------------------------
      
      public Vector2 texCoords;
      public boolean onBoundary; 
   }
   public static class Edge extends Mesh.Edge {
      // Every edge has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the EDGE a specific color..
      public int colorCode;
      
      // --------------------------------
      // Cut graph stuff:
      // --------------------------------
      
      // This is the distance between the centroids of the two triangles
      // on either side of this Edge, or null if this Edge is on a boundary.      
      public Float dualLen;      
      
      public boolean inCutGraph;
      public boolean isEndOfCutGraph() {
         if (!inCutGraph) return false;
         
         Vertex v1 = (Vertex) getFirst().start();
         Vertex v2 = (Vertex) getFirst().end();            
         return ((v1.cutGraphValence == 1) || (v2.cutGraphValence == 1));
      }
      
      public int cutPathIndex;
   }
   public static class Boundary extends Mesh.Boundary {
      // Every boundary has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the adjacent-triangle's edge a specific color..      
      public int colorCode;

   }
   public static class Triangle extends Mesh.Triangle implements TextureCoordProvider {
      // Every triangle has an associated <INT>
      // If zero no effect, but if 1 or 2 etc, the shader COLORS the triangle a specific color..
      public int colorCode;
      
      // --------------------------------
      // Cut graph stuff:
      // --------------------------------

      public Vector3 center;
      public boolean fixed;
      public float distanceToRoot;
      public Integer spanningTreeIndexTowardsRoot;
      
      // --------------------------------
      // Angles
      // --------------------------------

      public float[] angles;

      // --------------------------------
      // Final Texture Coords
      // --------------------------------
      
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
         public Triangle newTriangle() { return new Triangle(); }
         public Edge newEdge()         { return new Edge(); }
         public Vertex newVertex()     { return new Vertex(); }
         public Boundary newBoundary() { return new Boundary(); }
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
               Triangle t = (Triangle) tb;
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
               Triangle t = (Triangle) tb;
               
               // For triangle t we need to "aggregate" the colorCodes of all
               // vertices, edges, and boundaries, in addition to the one
               // from this triangle t:
               
               int tColorCode = t.colorCode;
               
               int v0ColorCode = ((Vertex)(t.vertices[0])).colorCode;
               int v1ColorCode = ((Vertex)(t.vertices[1])).colorCode;
               int v2ColorCode = ((Vertex)(t.vertices[2])).colorCode;
               
               int e0ColorCode = ((Edge)(t.edges[0].getEdge())).colorCode;
               int e1ColorCode = ((Edge)(t.edges[1].getEdge())).colorCode;
               int e2ColorCode = ((Edge)(t.edges[2].getEdge())).colorCode;
               
               int b0ColorCode = (!t.edges[0].opposite().isBoundary()) ? 0 : ((Boundary)(t.edges[0].opposite())).colorCode;
               int b1ColorCode = (!t.edges[1].opposite().isBoundary()) ? 0 : ((Boundary)(t.edges[1].opposite())).colorCode;
               int b2ColorCode = (!t.edges[2].opposite().isBoundary()) ? 0 : ((Boundary)(t.edges[2].opposite())).colorCode;
               
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
   
   public static void run(String name, MeshModel model, float xOffset) {
      model.updateMesh(newMesh());
      model.setManagedBuffer(Shader.DIRECTION_SHADING_ARRAY, newDirectionArrayManager(model.mesh));
      model.setManagedBuffer(Shader.COLOR_INFO, newTriangleColoringInfo(model.mesh));
      
      System.out.format("\nQUAD-COVER on \"%s\"\n",  name);
      
      boolean useQuadCoverCoords = true;
      setTextureCoords(model.mesh, xOffset, useQuadCoverCoords);
      
      // Organizer.setTextureCoords doesn't actually set anything yet,
      // so here's some "default" texture setting code:
      if (useQuadCoverCoords) {
         Vector2 offset = new Vector2(xOffset,0.0f);
         for (Mesh.Triangle tb : model.mesh.triangles) {
            Triangle t = (Triangle) tb;
            
            Vertex v0 = (Vertex)t.vertices[0];
            Vertex v1 = (Vertex)t.vertices[1];
            Vertex v2 = (Vertex)t.vertices[2];         
            t.setTextureCoords(v0.texCoords.plus(offset),v1.texCoords.plus(offset),v2.texCoords.plus(offset));
         }
      } else {
         Vector2 p0 = new Vector2(0.0f, 0.0f);
         Vector2 p1 = new Vector2(1.0f, 0.0f);
         Vector2 p2 = new Vector2(0.5f, 0.866f);
         float margin = 0.1f;
         
         int tCount = 0;
         int div = (int) Math.sqrt(model.mesh.triangles.size());
         
         for (Mesh.Triangle tb : model.mesh.triangles) {
            int yCount = tCount/div;
            int xCount = tCount - (yCount*div);
            Vector2 base = new Vector2(xOffset * 50 + margin + (1.0f+margin) * xCount, margin + (1.0f+margin) * yCount); 
                  
            Triangle t = (Triangle) tb;
            t.setTextureCoords(base.plus(p0), base.plus(p1), base.plus(p2));
            tCount++;
         }
      }
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
   
   
   public static void setTextureCoords(Mesh mesh, float texXOffset, boolean doFloaterCoords) {
      System.out.format("Starting with a MESH with %d triangles, %d vertices, %d edges, %d boundaries\n", 
            mesh.triangles.size(), mesh.vertices.size(), mesh.edges.size(), mesh.boundaries.size());
      
      mesh.checkMesh();
      
      // ####################################################################
      // CUT-GRAPH
      // ####################################################################
      
      // STEP 1:  compute the length of every edge and the "dualLen"
      //          for every non-boundary edge.
      
      for (Mesh.Triangle tb : mesh.triangles) {
         Triangle t = (Triangle) tb;
         t.center = (t.vertices[0].getPosition()
               .plus(t.vertices[1].getPosition())
               .plus(t.vertices[2].getPosition())).times(1.0f/3.0f);
      }
      for (Mesh.Edge eb : mesh.edges) {
         Edge e = (Edge) eb;
         e.computeLength();
         if (!e.isBoundary()) {
            Triangle t1 = (Triangle) e.getFirst().getTriangle();
            Triangle t2 = (Triangle) ((Mesh.Triangle.Edge) e.getSecond()).getTriangle();
            e.dualLen = t1.center.minus(t2.center).length();
         }
      }

      // STEP 2:  compute a shortest-path-tree for triangles from the root triangle
            
      for (Mesh.Triangle tb : mesh.triangles) {
         Triangle t = (Triangle) tb;
         t.fixed = false;
         t.spanningTreeIndexTowardsRoot = null;
      }
         
      PriorityQueue<Triangle> queue = new PriorityQueue<Triangle>(new Comparator<Triangle>() {
         @Override
         public int compare(Triangle o1, Triangle o2) {
            return Float.compare(o1.distanceToRoot, o2.distanceToRoot);
         }});

      Triangle rootTriangle = (Triangle) mesh.triangles.get(0);
      rootTriangle.distanceToRoot = 0.0f;
      queue.add(rootTriangle);
      
      while (!queue.isEmpty()) {
         Triangle t = queue.remove();
         t.fixed = true;
         
         for (Mesh.Triangle.Edge et : t.edges) {
            Edge e = (Edge) et.getEdge();
            // okay... we're using the "e" object to represent the DUAL edge between this triangle t
            // and some other triangle maybe on the other side of "et"
            
            if (!e.isBoundary()) {
               Mesh.Triangle.Edge etOpposite = (Mesh.Triangle.Edge) et.opposite();
               Triangle otherTriangle = (Triangle) etOpposite.getTriangle();
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
      
      // STEP 3:  Mark all edges that don't cross the cut-graph

      for (Mesh.Edge eb : mesh.edges) {
         Edge e = (Edge) eb;
         e.inCutGraph = true;
      }
      for (Mesh.Triangle tb : mesh.triangles) {
         Triangle t = (Triangle) tb;
         if (t.spanningTreeIndexTowardsRoot != null) {
            Triangle.Edge edgeCrossingPathToRoot = t.edges[t.spanningTreeIndexTowardsRoot];
            Edge e = (Edge) edgeCrossingPathToRoot.getEdge();
            e.inCutGraph = false;
         }
      }
      
      // STEP 4:  Remove cut-graph edges with valence 1
            
      // compute cut graph valence for each vertex      
      for (Mesh.Vertex vb : mesh.vertices) {
         Vertex v = (Vertex) vb;
         int valence = 0;
         for (Mesh.DirectedEdge outgoingEdge : v.outgoingEdges()) {
            Edge e = (Edge) outgoingEdge.getEdge();
            if (e.inCutGraph) valence++;
         }
         v.cutGraphValence = valence;
      }
      
      // locate all cut-graph with a valence=1 vertex
      
      int numEdges = mesh.edges.size();
      int numFullCutGraphEdges = 0;
      
      ArrayList<Edge> cutGraphEdgesToRemove = new ArrayList<Edge>();
      for (Mesh.Edge eb : mesh.edges) {
         Edge e = (Edge) eb;
         if (e.inCutGraph) numFullCutGraphEdges++;
         if (e.isEndOfCutGraph()) cutGraphEdgesToRemove.add(e);
      }
      
      //System.out.format("Starting brute force with %d out of %d edges\n", numFullCutGraphEdges, numEdges);

      while (!cutGraphEdgesToRemove.isEmpty()) {
         ArrayList<Edge> moreCutGraphEdgesToRemove = new ArrayList<Edge>();
         int numEdgesToRemove = cutGraphEdgesToRemove.size();
         int numEdgesRemoved = 0;
         for (Edge edgeToRemove : cutGraphEdgesToRemove) {
            if (edgeToRemove.inCutGraph) {
               Vertex v1 = (Vertex) edgeToRemove.getFirst().start();
               Vertex v2 = (Vertex) edgeToRemove.getFirst().end();
               
               numEdgesRemoved++;
               edgeToRemove.inCutGraph = false;
               v1.cutGraphValence--;
               v2.cutGraphValence--;

               for (Vertex v : Arrays.asList(v1,v2)) {
                  for (Mesh.DirectedEdge outgoingEdge : v.outgoingEdges()) {
                     Edge e = (Edge) outgoingEdge.getEdge();
                     if (e.isEndOfCutGraph()) moreCutGraphEdgesToRemove.add(e);
                  }
               }
            }
         }
         
         //System.out.format("Iteration %d:  Visiting %d edges in fake-list, removed %d of them, exposing %d edges in the next fake-list\n", 
         //      it++, numEdgesToRemove, numEdgesRemoved, newFakeCutGraphEdges.size());
         
         cutGraphEdgesToRemove = moreCutGraphEdgesToRemove;
      }

      
      // STEP 5:  Cut-graph OPTIMIZER -
      
      // --------------------------------------------------------------
      // At this point the edges with "inCutGraph" set form a VALID cut graph,
      // but perhaps not a good one.   We'd like to improve it...
      // --------------------------------------------------------------
      
      ArrayList<Mesh.Triangle.Edge> boundaryLoop = new ArrayList<Mesh.Triangle.Edge>();
      boolean furtherOptimizationPossible = true;
      boolean cuttingNeeded = false;
      
      // We/re going to iteratively improve the longest path in the cut graph,
      // so we need to divide the cut-graph into paths....
      
      do {
         furtherOptimizationPossible = false;
               
         // Find a cut-graph edge to start with:
         Mesh.Triangle.Edge startingEdge = null;
         for (Mesh.Edge eb : mesh.edges) {
            Edge e = (Edge) eb;
            if (e.inCutGraph) {
               startingEdge = e.getFirst();
               break;
            }
         }
         if (startingEdge == null) {
            throw new RuntimeException("hmm, no cut graph edges at all?");
         }

         // Assemble "boundaryLoop", a list of triangle edges in sequence around 
         // the common boundary that we'll get after the cut
         
         boundaryLoop.clear();
         Mesh.Triangle.Edge currentEdge = startingEdge;
         do {
            boundaryLoop.add(currentEdge);
            
            Mesh.DirectedEdge nextEdge = currentEdge.prevAroundEnd();
            while (!((Edge)nextEdge.getEdge()).inCutGraph) {
               nextEdge = nextEdge.prevAroundEnd();
            }
            nextEdge = nextEdge.opposite();
            
            currentEdge = (Mesh.Triangle.Edge) nextEdge;
         } while (currentEdge != startingEdge);

         
         // We know all the vertices connected to the cut graph have valence of 2 or greater,
         // since we removed all the edges that connected to a valence 1 vertex.
         //
         //  
         
         
         // Do any of the vertices in the cut-graph have valance > 2?  Find one.
         boolean hasCriticalPoints = false;
         int criticalPointIndex = 0;
         int numCriticalPoints = 0;
         int numBoundaryEdges = 0;
         for (int i = 0; i < boundaryLoop.size(); ++i) {
            if (((Vertex)boundaryLoop.get(i).start()).cutGraphValence > 2) {
               if (!hasCriticalPoints) {
                  criticalPointIndex = i;
                  hasCriticalPoints = true;
               }               
               ((Vertex)boundaryLoop.get(i).start()).colorCode = 1;
               numCriticalPoints++;
            }
            if (boundaryLoop.get(i).getEdge().isBoundary()) {
               numBoundaryEdges++;
            }
         }
         
         System.out.format("So... we have %d directedEdges in boundaryLoop -- with %d critical points, %d boundaryEdges\n", 
               boundaryLoop.size(), numCriticalPoints, numBoundaryEdges);

         
         if (hasCriticalPoints) {
            cuttingNeeded = true;
            
            // Make a list of the individual cut paths within the cut graph
            // by starting from the (valence>2) point at "criticalPointIndex"
            // and following the boundaryLoop until the next (valence>2) point..
            
            for (int i = 0; i < boundaryLoop.size(); ++i) {
               ((Edge)(boundaryLoop.get(i).getEdge())).cutPathIndex = -1;
            }
            
            class CutPath {
               public CutPath(int index, boolean isBoundary) {
                  this.index = index;
                  this.isBoundary = isBoundary;
                  edges = new ArrayList<Edge>();
               }
               
               public final int index;
               public final boolean isBoundary;
               public ArrayList<Edge> edges = new ArrayList<Edge>();
               public float length;
            }
            
            TreeSet<CutPath> cutPaths = new TreeSet<CutPath>(new Comparator<CutPath>(){
               @Override
               public int compare(CutPath o1, CutPath o2) {
                  return Float.compare(o2.length, o1.length);
               }
            });
            
            int cutPathIndex = 0;
            CutPath currentCutPath = null;
            for (int i = 0; i < boundaryLoop.size(); ++i) {
               int index = (criticalPointIndex + i) % boundaryLoop.size();
               
               Edge ithEdge = ((Edge)(boundaryLoop.get(index).getEdge()));
//               System.out.format("In edge %d we are %s -- curPathIndex = %d\n",
//                     ithEdge.getIndex(), ithEdge.isBoundary() ? "boundary":"interior",
//                           ithEdge.cutPathIndex);
               
               if (ithEdge.cutPathIndex == -1) {
                  
                  if (currentCutPath == null) {
                     currentCutPath = new CutPath(cutPathIndex++, ithEdge.isBoundary());
                  }
                  ithEdge.cutPathIndex = currentCutPath.index;
                  ithEdge.colorCode = currentCutPath.index+1;
                  currentCutPath.edges.add(ithEdge);
                  currentCutPath.length += ithEdge.length;
               }
               if (((Vertex)boundaryLoop.get(index).end()).cutGraphValence > 2) {
                  if (currentCutPath != null) {
                     cutPaths.add(currentCutPath);
                     currentCutPath = null;
                  }
               }
            }
            
            System.out.format("FOUND %d cut-paths\n",cutPaths.size());
            for (CutPath path : cutPaths) {
               System.out.format("Path of length %g, %s\n",
                     path.length, path.isBoundary ? "boundary" : "interior");
            }
            System.out.format("End of cut-path list\n");
            
            // -------------------------------------------------------------------
            // Now, then, for each cutPath, try to optimize it.
            // If we can find a shorter way to connect the two ends of this cutPath,
            //    we shrink the cutPath and drop back to the start,
            //    (furtherOptimizationPossible=true).
            // We don't exit until we've tried to optimize each cut path in order
            // of length and there's been no change to any path.
            // -------------------------------------------------------------------
            
            boolean pathWasShrunk = false;
            for (CutPath path : cutPaths) {
               if (path.isBoundary) continue;
               
               Mesh.Triangle.Edge firstEdge;
               Mesh.Triangle.Edge lastEdge;
               
               if (path.edges.size() == 1) {
                  firstEdge = path.edges.get(0).getFirst();
                  lastEdge  = (Mesh.Triangle.Edge)firstEdge.opposite();
               } else {
                  Edge e0  = path.edges.get(0);
                  Edge e1  = path.edges.get(1);
                  firstEdge = ((e0.getFirst().end() == e1.getFirst().start()) ||
                               (e0.getFirst().end() == e1.getFirst().end())) 
                             ? e0.getFirst() : (Mesh.Triangle.Edge)e0.getSecond();
                             
                  Edge eN_1  = path.edges.get(path.edges.size()-1);
                  Edge eN_2  = path.edges.get(path.edges.size()-2);
                  lastEdge = ((eN_1.getFirst().start() == eN_2.getFirst().start()) ||
                              (eN_1.getFirst().start() == eN_2.getFirst().end()))
                             ? eN_1.getFirst() : (Mesh.Triangle.Edge)eN_1.getSecond();
               }
               
               // ------------------------------------------------------
               // OPTIMIZE a single INTERIOR cut PATH
               // ------------------------------------------------------
               //
               // so, we expand two FRONTIERS from either SIDE of the cut-path
               // into the available space...
               //
               // the cut graph is identified by edges whose "cutPathIndex" is "path.index"
               // a better (shorter) cut graph may NOT touch any vertices indicent to 
               // edges from OTHER cut graph paths...
               // ... except at the start and the end.
               //
               // the first step is to collect the possible "start" and "end" points
               // for this path.   we imagine SLIDING the path start along the
               // other cut-graph path it's connected to...
               //
               
               // ------------------------------------------------------
               // COLLECT all the possible path "start" edges...
               // ------------------------------------------------------
               ArrayList<Mesh.Triangle.Edge> possibleStartEdges = new ArrayList<Mesh.Triangle.Edge>();
               
               // Obviously "firstEdge" is a possible start edge...
               // But also are all other non-cut-path edges
               //    starting at the same start-vertex as "firstEdge",
               //    between other cut-graph-edges...
               
               Mesh.DirectedEdge nextCutGraphEdgeAroundStart = firstEdge.nextAroundStart();
               while (!((Edge)nextCutGraphEdgeAroundStart.getEdge()).inCutGraph) {
                  nextCutGraphEdgeAroundStart = nextCutGraphEdgeAroundStart.prevAroundStart();
               }
               Mesh.DirectedEdge prevCutGraphEdgeAroundStart = firstEdge.prevAroundStart();
               while (!((Edge)prevCutGraphEdgeAroundStart.getEdge()).inCutGraph) {
                  prevCutGraphEdgeAroundStart = prevCutGraphEdgeAroundStart.prevAroundStart();
               }
               
               // We want to TRAVERSE a LOOP, going along the "prevCutGraphEdgeAroundStart"
               // ....

               // Gather Inward Pointing Edges along either side:
               
               ArrayList<Mesh.Triangle.Edge> possibleFirstEdges = new ArrayList<Mesh.Triangle.Edge>();
               ArrayList<Mesh.Triangle.Edge> possibleLastEdges = new ArrayList<Mesh.Triangle.Edge>();
               /*
               for (int i = 0; i < 2; i++) {
                  Mesh.DirectedEdge firstEdgeInLoop = nextCutGraphEdgeAroundStart.opposite();

                  // we are walking around a loop,
                  //    one edge of which is "nextCutGraphEdgeAroundStart.opposite()"
                  //    the next edge is "prevCutGraphEdgeAroundStart"
                  //
                  // (note the odd naming... the edges are called "next" and "prev"
                  // because they are the next and previous cut-graph-edges 
                  // encountered when rotating CounterCloseWise around the vertex)
                  //
                  Mesh.DirectedEdge nCutGraphEdge = nextCutGraphEdgeAroundStart;
                  Mesh.DirectedEdge pCutGraphEdge = prevCutGraphEdgeAroundStart;

                  do {
                     // we label as a "possibleStartEdge" all outgoing edges around "pCutGraphEdge.start()"
                     // starting from pCutGraphEdge (but not including it),
                     // and ending at nCutGraphEdge (but not including it),
                     //
                     Mesh.DirectedEdge e = pCutGraphEdge.nextAroundStart();
                     while (e != nCutGraphEdge) {
                        // we believe that we will never encounter a boundary edge because of where the cut graph edges are...
                        possibleFirstEdges.add((Mesh.Triangle.Edge)e);
                        e = e.nextAroundStart();
                     }
                     
                     // Now, if "pCutGraphEdge" is "firstEdgeInLoop", then we've 
                  
                  
                  
                  
               }
               */
               
               
            }

            if (pathWasShrunk) {
               furtherOptimizationPossible = true;
               System.out.format("REPEATING -----\n");
            } else {
               System.out.format("EXITING-OPTIMIZATION ----- (no changes after looking at each edge)\n");
            }
         } else {
            System.out.format("SKIPPING-OPTIMIZATION ----- (no points above 2)\n");
         }
      } while(furtherOptimizationPossible);
      
      
      
      // --------------------------------------------------------------
      // Let's assume we've done all we can to improve the cut graph..
      // --------------------------------------------------------------

      if (cuttingNeeded) {

         // Do we need to recompute boundaryLoop?
         // It's possible we already HAVE "boundaryLoop" filled in correctly?..
         
         // Step 6a:  Find a cut-graph edge to start with:
         Mesh.Triangle.Edge startingEdge = null;
         for (Mesh.Edge eb : mesh.edges) {
            Edge e = (Edge) eb;
            if (e.inCutGraph) {
               startingEdge = e.getFirst();
               break;
            }
         }
         if (startingEdge == null) {
            throw new RuntimeException("hmm, no edge remaining in cut graph?");
         }
         
         // Step 6b:  WALK around the cut graph edges

         boundaryLoop.clear();
         Mesh.Triangle.Edge currentEdge = startingEdge;
         do {
            boundaryLoop.add(currentEdge);
            
            Mesh.DirectedEdge nextEdge = currentEdge.prevAroundEnd();
            while (!((Edge)nextEdge.getEdge()).inCutGraph) {
               nextEdge = nextEdge.prevAroundEnd();
            }
            nextEdge = nextEdge.opposite();
            
            currentEdge = (Mesh.Triangle.Edge) nextEdge;
         } while (currentEdge != startingEdge);
       
         
         // -------------------------------------------
         // "cutting" the mesh
         // -------------------------------------------
         
         // SPLIT each vertex on the cut graph.

         int extraVertices = 0;
         HashSet<Vertex> verticesInCutGraph = new HashSet<Vertex>();
         HashMap<Mesh.Triangle.Edge, Vertex> splitVerticesAlongCutGraph = new HashMap<Mesh.Triangle.Edge, Vertex>();
         
         for (Mesh.Triangle.Edge e : boundaryLoop) {
            Vertex v = (Vertex) e.start();
            if (!verticesInCutGraph.contains(v)) {
               verticesInCutGraph.add(v);
               splitVerticesAlongCutGraph.put(e, v);
            } else {
               Vertex splitV = (Vertex) mesh.addVertex();
               splitV.colorCode = v.colorCode;
               extraVertices++;
               splitV.setPosition(v.getPosition());
               splitVerticesAlongCutGraph.put(e, splitV);
            }
         }
         System.out.format("We went ALL THE WAY AROUND in %d directedEdges, creating %d extra Vertices\n", 
               boundaryLoop.size(), extraVertices);
         

         // Make a list of the triangles we will DELETE
         
         HashSet<Mesh.Triangle> trianglesBorderingCutGraph = new HashSet<Mesh.Triangle>();
         for (Mesh.Vertex v : verticesInCutGraph) {
            for (Mesh.DirectedEdge e : v.outgoingEdges()) {
               if (!e.isBoundary()) {
                  trianglesBorderingCutGraph.add(((Mesh.Triangle.Edge)e).getTriangle());
               }
            }
         }
         
         // Step 6e:  For each triangle we will delete, identify the triangle that will replace it
         
         class VertexTriple {
            public VertexTriple() {
               vertices = new Vertex[3];
               edgeColorCodes = new int[3];
            }
            Vertex[] vertices;
            int[] edgeColorCodes;
         }
         ArrayList<VertexTriple> replacementTrianglesVertices = new ArrayList<VertexTriple>();
         for (Mesh.Triangle t : trianglesBorderingCutGraph) {
            VertexTriple replacementTriangleVertices = new VertexTriple();
            
            for (int i = 0; i < 3; i++) {
               replacementTriangleVertices.edgeColorCodes[i] = ((Edge) t.edges[i].getEdge()).colorCode;
               
               Vertex v = (Vertex) t.vertices[i];
               Vertex splitV = v;
               
               if (verticesInCutGraph.contains(v)) {
                  //
                  Mesh.Triangle.Edge outgoingFromVInT = t.edges[i].prev();
                  Mesh.Triangle.Edge outgoingFromV = outgoingFromVInT;
                  while (!((Edge)outgoingFromV.getEdge()).inCutGraph) {
                     Mesh.DirectedEdge next = outgoingFromV.prevAroundStart();
                     
                     if (next.isBoundary()) {
                        throw new RuntimeException("We hit a boundary on a vertex in the cut-graph without finding the cut graph");
                     }
                     if (next == outgoingFromVInT) {
                        throw new RuntimeException("We went all the way around a vertex in the cut-graph without finding the cut graph");
                     }
                     outgoingFromV = (Mesh.Triangle.Edge)next;
                  }
                  splitV = splitVerticesAlongCutGraph.get(outgoingFromV);
               }
               replacementTriangleVertices.vertices[i] = splitV;
            }
            replacementTrianglesVertices.add(replacementTriangleVertices);
         }
         
         // Step 6f:  Finally do the triangle deletions and additions
         
         for (Mesh.Triangle t : trianglesBorderingCutGraph) {
            mesh.removeTriangle(t);
         }
         for (VertexTriple t : replacementTrianglesVertices) {
            Triangle tr = (Triangle) mesh.addTriangle(t.vertices[0],t.vertices[1],t.vertices[2]);
            for (int i = 0; i < 3; i++) {
               ((Edge) tr.edges[i].getEdge()).colorCode = t.edgeColorCodes[i];
            }
         }
      }
      
      
      // ##########################################################################
      // Okay we now have effectively a NEW MESH ...
      // ##########################################################################
      
      mesh.checkMesh();

      // ##########################################################################
      // the vertices have changed, edges have been deleted and recreated,
      // the cutGraph boolean is no longer even set ... brand new mesh.
      // Let's reset the rootTriangle since there's no guarantee it's even still around.
      // ##########################################################################
      
      rootTriangle = (Triangle) mesh.triangles.get(0);
      
      // -----------------------------------------------------------
      // Color the triangles around "rootTriangle"
      // -----------------------------------------------------------
      
      rootTriangle.colorCode = 2;
      
      HashSet<Mesh.Vertex> verticesToProcess = new HashSet<Mesh.Vertex>();
      for (Mesh.Vertex v : rootTriangle.vertices) {
         verticesToProcess.add(v);
         ((Vertex) v).colorCode = 4;
      }
      int nextColorCode = 3;
      while (!verticesToProcess.isEmpty() && nextColorCode < 5) {
         HashSet<Mesh.Vertex> nextVerticesToProcess = new HashSet<Mesh.Vertex>();
         for (Mesh.Vertex v : verticesToProcess) {
            for (Mesh.DirectedEdge outgoingFromV : v.outgoingEdges()) {
               if (!outgoingFromV.isBoundary()) {
                  Triangle t = (Triangle) ((Mesh.Triangle.Edge) outgoingFromV).getTriangle();
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

      // -----------------------------------------------------------
      // Color all the boundaries
      // -----------------------------------------------------------

      for (Mesh.Edge eb : mesh.edges) {
         if (((Edge) eb).colorCode == 0) {
            ((Edge) eb).colorCode = 1;
         }
      }
      for (Mesh.Boundary eb : mesh.boundaries) {
         ((Boundary) eb).colorCode = 2;
      }

      // -----------------------------------------------------------
      // Set "selectedDirection" to X projection..
      // -----------------------------------------------------------
      
      for (Mesh.Triangle tb : mesh.triangles) {
         Triangle t = (Triangle) tb;
         
         Vector3 selectedDirection;
         selectedDirection = new Vector3(
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f));
         
         selectedDirection = Vector3.X;
         t.direction = selectedDirection;
      }

      
      // ####################################################################
      // FLOATER COORDS
      // ####################################################################

      if (doFloaterCoords) {
   
         // STEP 1:  compute the length of every edge and the angles in every face
         
         for (Mesh.Vertex eb : mesh.vertices) {
            eb.computeValence();  // needed for calculating the per-vertex lambdas
         }
         for (Mesh.Edge eb : mesh.edges) {
            eb.computeLength();  // needed in the angle-computation below
         }
         for (Mesh.Triangle tb : mesh.triangles) {
            Triangle t = (Triangle) tb;
            t.angles = new float[3];
   
   //         System.out.format("TRIANGLE %d lengths (%g,%g,%g)\n", t.getIndex(),
   //               t.edges[0].getEdge().getLength(),
   //               t.edges[1].getEdge().getLength(),
   //               t.edges[2].getEdge().getLength());
                  
   
            for (int i = 0; i < 2; ++i) {
               Vertex v = (Vertex) t.vertices[i];
               Mesh.Triangle.Edge eAdjacentR = t.edges[i].next();
               Mesh.Triangle.Edge eAdjacentL = t.edges[i].prev();
               Mesh.Triangle.Edge eOpposite  = t.edges[i];
               
               float a = eAdjacentR.getEdge().length;
               float b = eAdjacentL.getEdge().length;
               float c = eOpposite.getEdge().length;
               float angle = (float) Math.acos((a*a + b*b - c*c) / (2*a*b));
   
               // angles needed for calculating the per-vertex lambdas
               t.angles[i] = angle;
               
   //            System.out.format("Vertex %d has adj lengthd %g,%g, opp len %g, angle = Math.PI * %g\n",
   //                  i, a,b,c, angle/Math.PI);
   
            }
            t.angles[2] = (float) Math.PI - t.angles[0] - t.angles[1];
            
   //         System.out.format("Vertex 2 set to Math.PI * %g\n",
   //               t.angles[2]/Math.PI);
            
            for (int i = 0; i < 3; ++i) {
               if ((t.angles[i] <= 0.0) || (t.angles[i] >= Math.PI)) {
                  throw new RuntimeException("Angle out of bounds!\n");
               }
            }
         }
   
         
         // -----------------------------------------------------------
         // SEPARATE internal and boundary vertices
         // -----------------------------------------------------------
         
         ArrayList<Vertex> internalVertices = new ArrayList<Vertex>();
         ArrayList<Vertex> boundaryVertices = new ArrayList<Vertex>();
         
         // this loop sets "internalVertices".  
         // (we'll fill "boundaryVertices" in the next section)
         for (Mesh.Vertex vb : mesh.vertices) {
            Vertex v = (Vertex) vb;
            
            v.onBoundary = false;
            if (v.oneOutgoingEdge() == null) {
               throw new RuntimeException("Mesh error, vertex with no outgoing edges");
            }
            for (Mesh.DirectedEdge outgoingEdge : v.outgoingEdges()) {
               if (outgoingEdge.isBoundary()) {
                  v.onBoundary = true;
               }
            }
            if (!v.onBoundary) {
               internalVertices.add(v);
            }
         }
         
         // this loop sets "boundaryVertices".        
         int numBoundaries = mesh.boundaries.size();
         if (numBoundaries == 0) {
            throw new RuntimeException("Mesh error, no boundaries after cut-graph cutting");
         }
         Mesh.Boundary eStart = mesh.boundaries.get(0);
         int boundariesPassed = 0;
         Mesh.Boundary e = eStart;
         do {
            boundariesPassed++;
            e = e.next();         
            boundaryVertices.add((Vertex)e.start()); 
   
         } while (e != eStart);
         if (numBoundaries != boundariesPassed) {
            throw new RuntimeException(String.format("Mesh error, boundariePassed = %d but numBoundaries = %d",
                  boundariesPassed, numBoundaries));
         }
               
         System.out.format("Okay, our new SINGLE-LOOP mesh has %d triangles, %d edges, %d vertices, %d boundary edges -- %d vertices on the boundary, %d vertices internal\n",
               mesh.triangles.size(), mesh.edges.size(), mesh.vertices.size(), mesh.boundaries.size(),
               boundaryVertices.size(),
               internalVertices.size());
         
         // Now RENUMBER the vertices, so the interior ones are first
         // followed by the boundary ones
         
         int newIndex = 0;
         for (Vertex v : internalVertices) {
            v.setIndex(newIndex);
            mesh.vertices.set(newIndex, v);
            newIndex++;
         }
         for (Vertex v : boundaryVertices) {
            v.setIndex(newIndex);
            mesh.vertices.set(newIndex, v);
            newIndex++;
         }
   
         // -----------------------------------------------------------
         // ASSEMBLE a sparse linear-system (Ax=b) to solve:
         // -----------------------------------------------------------
         
         int numInternalVertices = internalVertices.size();
         int numBoundaryVertices = boundaryVertices.size();
         
         // First SET the position of every BOUNDARY vertex to points on a circle...
         int ind = 0;
         for (Vertex v : boundaryVertices) {
            float angle = (float)((2 * Math.PI * ind) / numBoundaryVertices);
            v.texCoords = new Vector2((float)Math.cos(angle),(float)Math.sin(angle));
            ind++;
         }
         
         // create two large sparse matrices
         SparseMatrix m = new SparseMatrix(numInternalVertices,numInternalVertices);
         SparseMatrix mb = new SparseMatrix(numInternalVertices,numBoundaryVertices);
         
         // now compute for every INTERNAL vertex a vector of LAMBDAs
         // describing it as an affine combination of its neighbors.
         // store these lambda values into m and mb:
         //
         for (Vertex v : internalVertices) {
            Vertex[] neighbors = new Vertex[v.getValence()];
            float[] lambda = new float[neighbors.length];
   
            int i = 0;
            for (Mesh.DirectedEdge teb : v.outgoingEdges()) {
               Mesh.Triangle.Edge te = (Mesh.Triangle.Edge)teb;
               Mesh.Triangle.Edge teo = (Mesh.Triangle.Edge)te.opposite();
               
               neighbors[i] = (Vertex) te.end();
               
               // angle to the left
               float angleToTheLeft  = ((Triangle)te.getTriangle()).angles[ te.next().getEdgeIndex() ];
               float angleToTheRight = ((Triangle)teo.getTriangle()).angles[ teo.prev().getEdgeIndex() ];
               float edgeLength = te.getEdge().getLength();
               
               lambda[i] = (float) (Math.tan(angleToTheLeft/2.0) + Math.tan(angleToTheRight/2.0)) / edgeLength;
               i++;
            }
            float lambdaSum = 0.0f;
            for (i=0; i < lambda.length; ++i) {
               lambdaSum += lambda[i];
            }
            for (i=0; i < lambda.length; ++i) {
               lambda[i] /= lambdaSum;
            }
            String a = "{ ";
            for (i=0; i < lambda.length; ++i) {
               a = a + String.format("%g%s", lambda[i], (i == lambda.length-1)?" }":", ");
            }
            //System.out.format("INTERNAL vertex %d has %d lambdas %s\n", v.getIndex(), lambda.length, a);
            // The values computed in lambda must now be PLACED
            // into the sparse matrices ma and mb,
            // This internal vertex i is setting the nonzero elements of the i'th row of ma and mb:
            
            int vIndex = v.getIndex();
            m.set(vIndex, vIndex, 1.0);
            
            for (i=0; i < lambda.length; ++i) {
               int neighborIndex = neighbors[i].getIndex();
               
               if (neighborIndex < numInternalVertices) {
                  m.set(vIndex, neighborIndex, -lambda[i]);
               } else {
                  mb.set(vIndex, neighborIndex-numInternalVertices, lambda[i]);
               }
            }
         }
         
         // --------------------------------------
         // Is this matrix symmetric?
         // It's not symmetric is it?
         // --------------------------------------
         
         System.out.format("SET -- we're ready for the solving to start??\n");
   
         double[] bbase = new double[numBoundaryVertices];
         ind = 0;
         for (Vertex v : boundaryVertices) {
            bbase[ind++] = v.texCoords.x;
         }      
         Vector b_x = mb.multiply(Vector.fromArray(bbase));
         
         ind = 0;
         for (Vertex v : boundaryVertices) {
            bbase[ind++] = v.texCoords.y;
         }      
         Vector b_y = mb.multiply(Vector.fromArray(bbase));
         
         // ----------------------------------------
         // We expect b is of length "internalVertices"
         
         if (b_x.length() != numInternalVertices) {
            throw new RuntimeException(String.format("With %d internalVertices and %d boundaryVertices, b ended up %d",
                  numInternalVertices, numBoundaryVertices, b_x.length()));
         }
         
         System.out.format("CALLING withSolver on the %d x %d matrix\n", numInternalVertices, numInternalVertices);
         System.out.format("Now calling solve...\n");
         long midTime = System.currentTimeMillis();
         Vector result_x = m.solve(b_x);  //solver.solve(b_x);
         Vector result_y = m.solve(b_y);
         long finalTime = System.currentTimeMillis();
         System.out.format("Done -- in %d ms!\n", (finalTime-midTime));
   
         for (int i = 0; i < numInternalVertices; ++i) {
            internalVertices.get(i).texCoords = new Vector2((float) result_x.values[i], (float) result_y.values[i]);
         }
         
         // DONE!   We've set the Vector2 uv coordinates of each internal vertex.  Congrats!
         // Let's copy them onto each triangle:
         
         Vector2 offset = new Vector2(texXOffset,0.0f);
         for (Mesh.Triangle tb : mesh.triangles) {
            Triangle t = (Triangle) tb;
            
            Vertex v0 = (Vertex)t.vertices[0];
            Vertex v1 = (Vertex)t.vertices[1];
            Vertex v2 = (Vertex)t.vertices[2];         
            t.setTextureCoords(v0.texCoords.plus(offset),v1.texCoords.plus(offset),v2.texCoords.plus(offset));
         }
      } else {
         
         
         
      }
   }
}

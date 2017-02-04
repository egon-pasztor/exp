package com.generic.base;

import com.generic.base.VectorAlgebra.*;

import java.util.ArrayList;

import com.generic.base.Geometry.Mesh1;
import com.generic.base.Raster.*;

// as if.

public class Organizer {
   
   // pdf output.. is what?
   // if the "BOUNDS" as taken as implicit,
   // the minimal interface is:
   // 
   public class DisplayList {
      
      public void setColor(ColorARGB a) {}
      public void moveTo(Vector2f point) {}
      public void lineTo(Vector2f point) {}
      public void curveTo(Vector2f point) {}
      public void fillPath() {}
      
   }
   /*
     
    If we want to support "strokePath", we also need to add "lineWidth", "joinPolicy", state
    (Or, we could have a "PolyLineStroker", a class which given a DisplayList is primed to "dump" lines into it.)
    (Or, we could have a "TextStroker", a class which given a DisplayList is primed to "dump" text into it.)
    (We could also support a "Transform" feature where 2x2 transforms could be chained ...)

    .... but at the end of the day, this is a CONSUMER, something that takes actual DisplayList calls
    .... and doesn't do anything with them.
    
    We could make it an INTERFACE, then different things could support it..
    Or a concrete class that assembles a LIST of DisplayList.Step objects that can be iterated over..
     
    ...
    
    then we end up with PDFOutputter that takes a DisplayList and outputs data into a FileWriter or
    something equivalent.
     
   */   
   /// --------------------------------------------------------------------------
   /*

     
   meanwhile, the SURFACE BROWSER...
   is at a dilemma.
   
   we have several processes the produce generic 2D manifolds...
      (a)  the sphere.
              vs
      (b)  the sphere when applied to a { solid-noise function + threshold }, is chopped into "continents",
           blobby shapes, each "blob" of which has the solid-noise function under threshold
              vs
      (c)  the sphere, when INTERSECTED with another mesh, produces a form consisting of different sub-surfaces,
           bounded by edges.   like, you know, the cube.
   
   in general, then, OBJECTS are composed of one or more 2D manifolds with boundaries.
      
      an OBJECT may be a "solid" OBJECT with an inside and an outside,
                or it may be a "2d surface" ...
                
   either way, an OBJECT is bounded by 2D-manifold PIECES
   
     ---------------------------------------------------------------------
   
   a 2D-manifold PIECE is imagined to be textured in a CELLULAR way,
       we want the computer to pseudorandomly generate the CELL CENTERS, 
       so that each point on the surface is assigned to a specific CELL

   if each point on the 2D-manifold PIECE is colored according to how close it is to
       its CELL CENTER, the effect would be to paint "polka dots" on the surface.
       
       imagine, for example, a 2D-manifold PIECE that might be part of an OBJECT,
       imagine a specific surface like a doughnut shape
       
       the CELLULAR TEXTURE covers this curvy surface with polka dots,
       so every point on the surface has either a "nearest" polka dot,
         or it's on a boundary equidistant from two or more polka dots.

       around each polka dot, there's a defined orientation, so when zoomed in close to the surface,
       there's a polar coordinate system surrounding each polka dot.

     ........................................................
   
   the SIZE/SPACING and ORIENTATION-PATTERN of the polka dots are free parameters...
   
       if the polka dots occur on a regular grid ("default" ORIENTATION-PATTERN)
          then the only parameter is the spacing of the grid,
          
       there's TWO WAYS of interpreting this -- 
           either the grid is a 2D grid that conforms to the 2D surface...
           or a 3D grid through which the 2D surface is cutting ...
           
    
  ===================================================================================================
  
     /*

     
   meanwhile, the SURFACE BROWSER...
   is at a dilemma.
   
   we have several processes the produce generic 2D manifolds...
      (a)  the sphere.
              vs
      (b)  the sphere when applied to a { solid-noise function + threshold }, is chopped into "continents",
           blobby shapes, each "blob" of which has the solid-noise function under threshold
              vs
      (c)  the sphere, when INTERSECTED with another mesh, produces a form consisting of different sub-surfaces,
           bounded by edges.   like, you know, the cube.
   
   in general, then, OBJECTS are composed of one or more 2D manifolds with boundaries.
      
      an OBJECT may be a "solid" OBJECT with an inside and an outside,
                or it may be a "2d surface" ...
                
   either way, an OBJECT is bounded by 2D-manifold PIECES
   
     ---------------------------------------------------------------------
   
   a 2D-manifold PIECE is imagined to be textured in a CELLULAR way,
       we want the computer to pseudorandomly generate the CELL CENTERS, 
       so that each point on the surface is assigned to a specific CELL

   if each point on the 2D-manifold PIECE is colored according to how close it is to
       its CELL CENTER, the effect would be to paint "polka dots" on the surface.
       
       imagine, for example, a 2D-manifold PIECE that might be part of an OBJECT,
       imagine a specific surface like a doughnut shape
       
       the CELLULAR TEXTURE covers this curvy surface with polka dots,
       so every point on the surface has either a "nearest" polka dot,
         or it's on a boundary equidistant from two or more polka dots.

       around each polka dot, there's a defined orientation, so when zoomed in close to the surface,
       there's a polar coordinate system surrounding each polka dot.

     ........................................................
   
   the SIZE/SPACING and ORIENTATION-PATTERN of the polka dots are free parameters...
   
       if the polka dots occur on a regular grid ("default" ORIENTATION-PATTERN)
          then the only parameter is the spacing of the grid,
          
       there's TWO WAYS of interpreting this -- 
           either the grid is a 2D grid that conforms to the 2D surface...
           or a 3D grid through which the 2D surface is cutting ...
           
    
  ===================================================================================================
  
  
   so .. the 2D surface { a collection of linked triangles }
   
         will be divided into regions.
            the regions must "LINK" to each other,
            the regions linking together have the same topology as the underlying surface topology
            but each region consists of a "MAP".
            
         (simplification:  a separate MAP for each triangle.
                           that's SO much simpler.)
                           
         okay.  QUADCOVER.   agreement that each triangle is its own MAP
         
         input:   triangle mesh M_h,    guiding fields U,V
         
            (elsewhere called "manifold M with charts M_i"
                           or "let M_h be a discrete manifold with triangles (charts) T"
            
            U,V then, are two unit length axes on each triangle.
               they do not need to be mutually orthogonal though it probably helps..
            
            for bunny, we can "select" U,V abitrarily, i guess...
               
              
         output:    cut graph G, parametrization (u,v) in S^2_G,h.  so.
                                                       
      
      
    this code calls a parametrization:
   
         for each triangle T_i,
                 provide a PHI_i : triangle->R^2   p->(u,v)
                 
                 wherever two triangles T_i and T_j intersect at a line,
                              it's important that PHI_i and PHI_j along that line be the same length & slope modulo right-angle-flips,
               
                 specifically, for points P on an edge E_ij which connects T_i and T_j,
                 
                         PHI_i(P) = J_ij PHI_j(P) + W_ij
                         
                 here W_ij is a (du,dv) pair, a gap,
                  and J_ij is a 90-degree rotation ([0,-1;1,0]) raised to either 0,1,2 or 3.
                  
                 so:  each EDGE E_ij posesses (a) a 2D gap W_ij 
                                          and (b) a rotation number 0,1,2 or 3
    
                 
                 but if we SPECIFY the (u,v) map on each triangle, we can COMPUTE the W_ij and rotation-number values...
                 ... so the W_ij and rotation-number are not "extra info", they're CONSTRAINTS on the freedom to choose
                 any PHI_i(P) map for each triangle...
                 
        let's be clear:
        
              ONE linear PHI_i(P) map is a set of 3 coordinate pairs (u0,v0) (u1,v1) (u2,v2)
              so, 6 numbers in all.
              
        for a mesh with T triangles, we want to compute 3T numbers.
                 
               a mesh has T triangles, E edges, V vertices
                  fr example, our icosahedron...  has 20 faces, 12 vertices, 30 edges.
                     each face needs 6 numbers so we're looking for 120 numbers output.
                     each edge adds 2 contraints...

    ===========================================
   
   the PAPER talks about two function-spaces...
   
        S_h which is { u : M_h -> R, u linear on each triangle and continuous on M_h }
       
          an "element" of S_h consists of { one value of u for each VERTEX } 
          for any point P in triangle T_i compute barycentric coordinates from the 3 vertex values and interpolate...          
          (so, exactly V numbers. for the icosahedron a member of S_h consists of 12 numbers)
          
           
   and  S*_h which is { u : M_h -> R, u linear on each triangle and continuous on M_h at edge-midpoints only }
   
          an "element" fo S*_h conists of { one value of u for each EDGE }
          for any point P in triangle T_i compute barycentric coordinates from the 3 edge values and interpolate...
          (so, exactly V numbers. for the icosahedron a member of S*_h consists of 30 numbers)
           
    ===========================================
    
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
   
  
   // ================================================================
   //  This "non-Java" file seems similar to StandardModel.java..
   // ================================================================
   
   //  somehow we'll translate this into java...
   //
   /*

      --------------------------------------------------
      
      matter is organized into:
      
      
         L -- "lepton"
         U -- "up-quark" 
         D -- "down-quark" 
         
      at scales < -18   --  1 attometer == 1 "billionth of a billionth of a meter" == 10^-18 m
      Either we are "near" a single L, U, or D point, or not.
   
      If we are "near" an L, U, or D point, 
         we can get arbitrarily closer to it for all scales < -18.

      --------------------------------------------------
      
      All U's and D's are arranged into triads:
       
          2 U's and a D  -- "proton"     .8 femtometers == .8 "millionth of a billionth of a meter" == .8 * 10^-15 m
          2 D's and a U  -- "neutron"
          
      These are around scale -15.   
      A "scale-15" cube is 1000 "scale-18" cubes to a side
      

         
         
         
    */
   

   public static class FaceInfo {
      public final float    area;
      public final Vector3f normal;
      public Vector3f selectedDirection;
      
      public Mesh1.Edge.Ref edge0, edge1, edge2;
      
      public FaceInfo(Mesh1.Vertex v0, Mesh1.Vertex v1, Mesh1.Vertex v2) {
         area = 0.0f;
         normal = Vector3f.ORIGIN;
         selectedDirection = new Vector3f(
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f),
               2.0f * ((float)Math.random() - 0.5f));
         
         selectedDirection = Vector3f.X;
      }
      public void setMeshEdgeRef(int index, Mesh1.Edge.Ref edge) {
         if (index == 0) edge0 = edge;
         if (index == 1) edge1 = edge;
         if (index == 2) edge2 = edge;
      }
      public Mesh1.Edge.Ref getMeshEdgeRef(int index) {
         return (index == 0) ? edge0 : (index == 1) ? edge1 : edge2;
      }
   }
   public static class VertexInfo {
      public final float curvature;
      
      public VertexInfo() {
         curvature = 0.0f;
      }
   }
   
   
   
   public static Vector3f[] rearrangeTextureCoords(Geometry.Mesh1 mesh) {
      int numTriangles = mesh.interiorTriangles.size();
      int numVertices  = mesh.vertices.size();
      
      
      // Copy the COORDS into an array, freeing up the Triangle pointers
      Geometry.FlatFaceInfo[] coords = new Geometry.FlatFaceInfo[numTriangles];
      for (Mesh1.Triangle t : mesh.interiorTriangles) {
         Geometry.FlatFaceInfo fi = (Geometry.FlatFaceInfo) t.getData();
         coords[t.getIndex()] = fi;
         t.setData(null);
      }
      
      FaceInfo[] faceInfo = new FaceInfo[numTriangles];
      for (Mesh1.Triangle t : mesh.interiorTriangles) {
         int triangleIndex = t.getIndex();
         FaceInfo fi = new FaceInfo(t.edge0.getOppositeVertex(), t.edge1.getOppositeVertex(), t.edge2.getOppositeVertex());
         faceInfo[triangleIndex] = fi;
      }
      
      ArrayList<Mesh1.Edge> interiorEdges = new ArrayList<Mesh1.Edge>();
      ArrayList<Mesh1.Edge> boundaryEdges = new ArrayList<Mesh1.Edge>();
      
      int triangleCount = mesh.interiorTriangles.size();
      
      for (Mesh1.Triangle thisTriangle : mesh.interiorTriangles) {
         int thisTriangleIndex = thisTriangle.getIndex();
         FaceInfo thisTriangleInfo = faceInfo[thisTriangleIndex];
         
         for (int i = 0; i < 3; ++i) {
            Mesh1.Triangle.Edge thisTriangleEdge = thisTriangle.getEdge(i);
            if (thisTriangleInfo.getMeshEdgeRef(i) == null) {
               Mesh1.Edge newEdge = new Mesh1.Edge(thisTriangleEdge);
               thisTriangleInfo.setMeshEdgeRef(i, newEdge.forward);
               
               Mesh1.Triangle.Edge oppositeTriangleEdge = thisTriangleEdge.getOppositeEdge();
               Mesh1.Triangle oppositeTriangle = oppositeTriangleEdge.getTriangle();
               
               if (oppositeTriangle.isBoundary()) {
                  // newEdge is a BOUNDARY EDGE
                  boundaryEdges.add(newEdge);
                  
               } else {
                  int otherTriangleIndex = oppositeTriangle.getIndex();
                  FaceInfo otherTriangleInfo = faceInfo[otherTriangleIndex];
                  otherTriangleInfo.setMeshEdgeRef(oppositeTriangleEdge.getIndex(), newEdge.reverse);
                  
                  interiorEdges.add(newEdge);
               }
            }
         }
      }
      
      // -----------------------------------------------------------
      // "todo": set "selectedDirection" to sensible values
      // -----------------------------------------------------------
      
      
      
      // -----------------------------------------------------------
      // -----------------------------------------------------------
      
      Vector3f[] directions = new Vector3f[numTriangles];
      for (int i = 0; i < directions.length; ++i) {
         directions[i] = faceInfo[i].selectedDirection;
      }
            
      System.out.format("\nWe have a MESH with %d interior-triangles, %d vertices, %d interior-edges, %d boundary-triangles, %d boundary-edges\n\n", 
            numTriangles, numVertices, interiorEdges.size(), mesh.boundaryTriangles.size(), boundaryEdges.size());
      
      
      // Return the new COORDS to the Triangle pointers
      for (Mesh1.Triangle t : mesh.interiorTriangles) {
         t.setData(coords[t.getIndex()]);
      }
      return directions;
   }
   
   
   
   // -----------------------------------------------
   // ------ here in organizer...
   //
   // we can write what we want, so:  After we bring back Type/Variable/Value code,
   // can we have Loop and Statement and other program structures?
   //
   // a "Function" would have a set of commands.. but the language as a whole would probably
   // be more like LISP..
   //----------------------------------------------   
   
   // affine-springs
   //    locate boundary vertices.
   //    put the boundary vertices somewhere in UV space  <<-- that's a subproblem
   //    then for each interior point vi
   //       compute lambda vi_j for all adjacent j, (kindof like barycentric coordinates but with J neighbors)
   //    finally the interior point's uv vertices are given,
   //       by solving a linear system  [All interior Points 3D coords] = [rectanglar matrix of lambdas] [All Points 3D coords]
   //       in principle this has a single solution,
   //
   //    (THIS should be equivalent to the idea of "imagine all edges are springs",
   //       fix the boundary curves, and see where the interior points are physically pulled by their springs)
   //
   //     CONS:  huge distortions.  
   //              each ways to fix bopundary positions, like on a square or a circle, will look horrible)
   //     PROS:  actually not that hard.
   //
   //
   // "stepwise layout"..
   //     locate one triangle in place, (add vertices to queue)
   //     then for the next in a queue of vertices,
   //         position the triangles around that vertex,
   //         so that in 2D their angles divide 360 by the same proportions as they do in 3D space
   //             (presumably the SIDE LENGTHS can be chosen too??
   //     I'm.... not sure this works
   //
   //  
   //
   // QuadCover:
   //     start with a "good" direction K_i on each triangle, with unit length   <-- that's a subproblem
   //            this is the DIRECTION-FIELD.   ("unlike a vector field, a direction field does not have magnitude
   //                                            and does not distinguish between the two directions")
   //
   //
   //     we find a scalar field (scalar u_i on each v_i)
   //        that minimizes SUM of LENGTH((scalar-field-GRADIENT_i - K)) * AREA_i
   //
   //     we do this by fist finding the scalar-field-GRADIENT_i for all i, 
   //        (by taking the K vector field and "subtracting something to make it curl*-free!!!")
   //
   //     then we "integrate" this scalar-gradient-field for all vertices
   //        (along the shortest path to a root vertex.  somehow the sum is supposed to be independent of the path chosen)
   //
   //     THEN repeat for v, using "good" directions at 90% to the first
   //
   //     also,incorporate "matchings" between triangles if you want to add singularities
   //
   //        <<--- but you have to "provide" the matchings to start with, so that's a subproblem too
   //        <<--- and where do you add singularities?   these papers seem to describe higher-level loops
   //                 that "try out" different singularity placement
   //
   //
   //     CONS:   lots of subproblems ... 
   //                selecting a good field (is pretty easy -- use curvature, weighted by magnitude.
   //                                        blur a little and normalize)
   //
   //                but, selecting the matchings??
   //                          no, not really sure of that.
   //
   //                and I'm worried that at the end, the u,v result,
   //                    is going to include all manner of triangle flips, both local and global overlaps.
   //                    after all, nothing is forcing the U,V values NOT to overlap, right?
   //
   //      PRO:    the results look good?
   //              requires more robust treatment of vector fields on meshes,
   //                 computing curl,div,grad...
   //
   //  there's actual source code?  http://www3.cs.stonybrook.edu/~gu/software/RiemannMapper/

   
   // one subproblem of QuadCover (and a TON of the other methods out there)
   //    involves computing homology-group-curves...
   //
   //   apparently there's an O(gN) algorithm for this, g=genus, N=vertices? triangles?
   //
   
   //
   
}

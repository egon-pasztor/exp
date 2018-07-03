package com.generic.base;

import com.generic.base.Algebra.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

// as if.

public class Organizer {
   
   // App is going to want to create windows...
   
   public static class GUI {
      
      public static class Window {
         
         public static class Size {
            public final int width;
            public final int height;
            public Size(int width, int height) {
               this.width = width; this.height = height;
            }
         }
         public static class Position {
            public final int x;
            public final int y;
            public Position(int x, int y) {
               this.x = x;
               this.y = y;
            }
         }
      }
   }

   
   // to provide a full windowing system,
   // we'd like:
   //
   //                   Window.Image
   //                   Window.Viewport2D
   //                   Window.Viewport3D
   //
   //                   Window.Label
   //                   Window.Button
   //                   Window.Toggle
   //                   Window.Pulldown
   //                   Window.TextEntry
   
   // --------------------------------------------------------
   // but, critically, it also needs Window.Container and Window.TopLevel
   //
   // we'd like to keep 
   //    
   
   
   

   // but today we've been thinking of "Mutable<Contents>"
   //
   public static abstract class Mutable<Contents> {

      private Contents contents = null;
      
      // -------------------------------------------------
      // Step 1: SETUP
      //   Owner creates an instance of an derived class,
      //     implementing the "rebuild" method,
      //     and adding "needsRebuildingListener" to any mutables
      //     that the "rebuild" output depends on.
      // -------------------------------------------------
      protected abstract boolean rebuild();
      
      // -------------------------------------------------
      private boolean needsRebuilding = true;
      public final Data.Listener needsRebuildingListener = new Data.Listener () {
         public void onChange() {
            needsRebuilding = true;
         }
      };
      // -------------------------------------------------      
      public final Data.Listener.Set listeners = new Data.Listener.Set();
         
      // -------------------------------------------------
      // Step 2: Call rebuildIfNeeded regularly.
      //   There could be a thread that calls it at a fixed rate?
      //   Or users of this class are encouraged to call it before use?
      //   Or else, we could modify the "needsRebuildingListener" to
      //     "schedule" a call to rebuildIfNeeded on a worker thread?
      //   Or else, we could modify the "needsRebuildingListener" to
      //     call to rebuildIfNeeded directly.
      // -------------------------------------------------
      public void rebuildIfNeeded() { 
         if (needsRebuilding) {
            needsRebuilding = false;
            if (rebuild()) {
               listeners.changeOccurred();
            }
         }         
      }      
   }

   // --------------------------------------------------
   
   public abstract static class Generated<Contents,Source> extends Mutable<Contents> {
      public final Mutable<Source> source;
      
      public Generated(Mutable<Source> source) {
         this.source = source;
         source.listeners.add(needsRebuildingListener);
      }
      
      // We have no idea how to go further on this, because of the unsolved
      // problem of WHY calls "rebuildIfNeeded" on these things and why!
   }
   // --------------------------------------------------
   // --------------------------------------------------
   public static void main(String[] args) {
      System.out.format("Hello.67\n");
   }
   
   
   // pdf output.. is what?
   // if the "BOUNDS" as taken as implicit,
   // the minimal interface is:
   // 
   public static class DisplayList {
      
      public void setColor(Color.RGB a) {}   // Should setcolor support "alpha?"
      public void moveTo(Vector2 point) {}
      public void lineTo(Vector2 point) {}
      public void curveTo(Vector2 point) {}
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
       
          2 U's and a D  -- "proton"   .8 femtometers == .8 "millionth of a billionth of a meter" == .8 * 10^-15 m
          2 D's and a U  -- "neutron"
          
      These are around scale -15.   
      A "scale-15" cube is 1000 "scale-18" cubes to a side
      

         
         
         
    */


   
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

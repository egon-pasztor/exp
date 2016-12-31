package com.generic.base;

import com.generic.base.VectorAlgebra.*;
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
  
  
   so .. the 2D surface { a collection of linked triangles }
   
         will be divided into regions.
            the regions must "LINK" to each other,
            the regions linking together have the same topology as the underlying surface topology
            but each region consists of a "MAP".
            
         (simplification:  a separate MAP for each triangle.
                           that's SO much simpler.)
                           
         so:  the MAP 
                           
                           
          
    
       
       
   
   
   
    
   
   
   
   
*/
}

package com.generic.base;

public interface Platform {

   // -------------------------------
   // Log and LoadResource
   // -------------------------------
   public void log(String s, Object... args);
   public String loadResource(String name);

   // ---------------------------------------------
   // Widgets
   // ---------------------------------------------
   public interface Widget {
      public Widget.Container parent();
      public Image.Size size();

      public interface ResizeListener {
         public void resized();
      }
      public void setResizeListener(ResizeListener listener);
      
      public interface PointerListener {
         public void hover (Image.Position position);
         public void down  (Image.Position position, boolean ctrlDown, boolean shiftDown);
         public void drag  (Image.Position position);
         public void up();
      }
      public void setPointerListener(PointerListener listener);
      
      // -------------------------------
      // Widget-Types
      // -------------------------------
      public interface Container extends Widget {
         public Iterable<Widget> children();
         public void addChild(Widget child);
         public void removeChild(Widget child);
         
         // Set child bounds, presumably in response to ResizeListener call.
         public Image.Rect getBounds (Widget child);
         public void setBounds (Widget child, Image.Rect bounds);
      }
      
      public interface Renderer3D extends Widget {
         // Rendering contains all the vertex-buffers and texture-images
         public void setRendering (Rendering g);
      }
   }
   // -------------------------------------
   public Widget.Container rootWidget();
   
   public Widget.Container  newContainer();
   public Widget.Renderer3D newRenderer3D();
}



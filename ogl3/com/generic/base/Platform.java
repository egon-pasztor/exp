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
      public Widget parent();
      public boolean isConnected();
      public Image.Size size();

      public interface ResizeListener {
         public void resized();
      }
      public void setResizeListener(ResizeListener listener);
      
      public interface MouseListener {
         public void mouseHover (Image.Position position);
         public void mouseDown  (Image.Position position, boolean ctrlDown, boolean shiftDown);
         public void mouseDrag  (Image.Position position);
         public void mouseUp();
      }
      public void setMouseListener(MouseListener listener);
      
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
         // Okay, a Widget.Renderer3D is a Widget
         // that accepts a Graphics3D and "renders" it
         public void setRendering (Rendering g);
      }
   }
   // -------------------------------------
   public Widget.Container rootWidget();
   
   public Widget.Container  newContainer();
   public Widget.Renderer3D newRenderer3D();
}



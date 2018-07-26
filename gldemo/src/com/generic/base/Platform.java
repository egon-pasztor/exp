package com.generic.base;

public interface Platform {

   public void log(String s, Object... args);
   public String loadResource(String name);

   // -------------------------------------
   // Eventual Plan
   // -------------------------------------
   public interface Widget {
      public Widget parent();
      public boolean isConnected();
      public Image.Size size();

      // Something to set a resizeListener??
      public interface ResizeListener {
         public void resized();
      }
      public void setResizeListener(ResizeListener listener);
      
      // But if we support resizeListener,
      //   why not windowVisibility listeners?
      //        or windowClosing listeners?
      // (Or maybe those are listeners we can only support
      //  on the ROOT window?)
      
      // -------------------------------
      // Specific Widget-Types
      // -------------------------------
      public interface Container extends Widget {
         public Iterable<Widget> children();
         public void addChild(Widget child);
         public void removeChild(Widget child);
         
         // Some way for ower to provide a layout method?
         // Container must keep a MAP providing a Rect
         // for each child, maybe also a borderspec.
      }
      public interface Graphics2D extends Widget {
         // Okay, a Widget.Graphics2D is a Widget that
         // provides a 2D graphics interface to painting its
         // contents .. a wrapper around Java's Graphics2D
         // I guess?  
      }
      public interface Graphics3D extends Widget {
         // Now a Graphics3D provices access to ...
         // well, it must provide a "GL" object, right?
      }
      
      // -------------------------------
      // A factory that creates specific Widget types
      // -------------------------------
      public interface Factory {
         public Container  newContainer();
         public Graphics2D newGraphics2D();
         public Graphics3D newGraphics3D();
      }
   }
   // -------------------------------------
   public Widget.Container rootWidget();
   public Widget.Factory widgetFactory();
   // -------------------------------------

   // -------------------------------------
   // We're not really sure how the above will work...
   // as an introductory step, we're proposing just a root-widget:
   // -------------------------------------
   public interface Root3DWidget {
      public void setGraphics3D(Graphics3D g);
   }
   public Root3DWidget root3D();
   // -------------------------------------
}



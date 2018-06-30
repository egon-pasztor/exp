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
      // Maybe also a method to get Widget Size,
      // or a way to register size-change handlers?      
      
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
   // as an introductory step, we're proposing platforms
   // whose "rootWidget" is a Graphics3D:
   public interface Root3DWidget {
      // A 3D widget will provide call "render(GL gl)"
      // on something... that does rendering?
      // so caller has to setRenderer", perhaps?
      public interface Renderer {
         public void render(GL gl);
      }
   }
   public Root3DWidget root3D();
   // -------------------------------------

}

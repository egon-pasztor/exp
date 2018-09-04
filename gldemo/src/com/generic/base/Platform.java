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
         //
         // We believe Container should support
         // setBounds (Widget child, Rect bounds)
         // and setBorder (Widget child, BorderDescription....)
      }
      public interface Renderer2D extends Widget {
         // Okay, a Widget.Renderer2D is a Widget
         // that accepts a Graphics2D and "renders" it
      }
      public interface Renderer3D extends Widget {
         // Okay, a Widget.Renderer3D is a Widget
         // that accepts a Graphics3D and "renders" it
         public void setGraphics3D(Graphics3D g);
      }
      
      // -------------------------------
      // A factory that creates specific Widget types
      // -------------------------------
      public interface Factory {
         public Container  newContainer();
         public Renderer2D newRenderer2D();
         public Renderer3D newRenderer3D();
      }
   }
   // -------------------------------------
   public Widget.Container rootWidget();
   public Widget.Factory widgetFactory();
   // -------------------------------------

   // -------------------------------------
   // We're not really sure how the above will work.
   // As an introductory step, we're proposing
   // the root-widget will be a Widget.Renderer3D
   // -------------------------------------
   public Widget.Renderer3D root3D();
   // -------------------------------------
}



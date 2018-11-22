package com.generic.base;

import java.util.Objects;

public class Image {

   public final int width;
   public final int height;
   public final Data.Array data;
   
   public Image (int width, int height, Data.Array.Type type) {
      this.width = width;
      this.height = height;
      this.data = Data.Array.create(type);
      data.setNumElements(width * height);
   }
   
   // -------------------------------------------------------------------
   // Color images
   // -------------------------------------------------------------------   
   public abstract static class RGB extends Image {

      public RGB (int width, int height, Data.Array.Type type) {
         super (width, height, type);
      }
      public Color get (int x, int y) {
         return newCursor().setPosition(x, y).getColor();
      }
      public void set (int x, int y, Color color) {
         newCursor().setPosition(x, y).setColor(color);
      }      
      public void drawLine (int x0, int y0, int x1, int y1, Color color) {
         Image.drawLine(x0, y0, x1, y1, newPainter(color));
      }
      public void fillRect (int left, int top, int width, int height, Color color) {
         Image.fillRect(left, top, width, height, newPainter(color));
      }
      
      public interface Cursor {
         Cursor setPosition(int x, int y);
         Color getColor();
         void setColor(Color c);
      }
      public abstract Cursor newCursor();      
      public abstract Painter newPainter(Color c);
      
      // -------------------------------------------------------------------
      // 3 RGB bytes per pixel
      // -------------------------------------------------------------------
      public static class Bytes extends Image.RGB  {

         public Bytes (int width, int height) {
            super (width, height, Data.Array.Type.THREE_BYTES);
         }
         public byte[] array() {
            return ((Data.Array.Bytes) data).array();
         }
         
         public Cursor newCursor() {
            return new Cursor(this);
         }
         public Painter newPainter(Color color) {
            return new Image.Painter() {
               private Color.RGB.Bytes rgb = color.rgbBytes();
               private Cursor cursor = newCursor();
               
               public void paint(int x, int y) {
                  cursor.setPosition(x, y).setColor(rgb);
               }
            };
         }
         private static class Cursor implements Image.RGB.Cursor {
            private final byte[] arr;
            private int width;
            private int loc;
            
            public Cursor(Image.RGB.Bytes image) {
               arr = image.array();
               width = image.width;
               loc = 0;
            }
            public Cursor setPosition(int x, int y) {
               loc = (y * width + x) * 3;
               return this;
            }
            public Color getColor() {
               return Color.rgbBytes(arr[loc], arr[loc+1], arr[loc+2]);
            }
            public void setColor(Color color) {
               Color.RGB.Bytes rgb = color.rgbBytes();
               arr[loc] = rgb.r;
               arr[loc] = rgb.g;
               arr[loc] = rgb.b;
            }
         }
      }
      
      // -------------------------------------------------------------------
      // 1 integer per pixel
      // -------------------------------------------------------------------
      public static class Integers extends Image.RGB  {

         public Integers (int width, int height) {
            super (width, height, Data.Array.Type.ONE_INTEGER);
         }
         public int[] array() {
            return ((Data.Array.Integers) data).array();
         }

         private static class Cursor implements Image.RGB.Cursor {
            private final int[] arr;
            private int width;
            private int loc;
            
            public Cursor(Image.RGB.Integers image) {
               arr = image.array();
               width = image.width;
               loc = 0;
            }
            public Cursor setPosition(int x, int y) {
               loc = (y * width + x);
               return this;
            }
            public Color getColor() {
               return Color.RGB.Bytes.fromInteger(arr[loc]);
            }
            public void setColor(Color color) {
               arr[loc] = color.rgbBytes().toInteger();
            }
            public void setValue(int val) {
               arr[loc] = val;
            }
         }
         public Cursor newCursor() {
            return new Cursor(this);
         }
         public Painter newPainter(Color color) {
            return new Image.Painter() {
               private int val = color.rgbBytes().toInteger();
               private Cursor cursor = newCursor();
               
               public void paint(int x, int y) {
                  cursor.setPosition(x, y).setValue(val);
               }
            };
         }
      }
   }
   
   // -------------------------------------------------------------------
   // Grayscale images
   // -------------------------------------------------------------------   
   public abstract static class Grayscale extends Image {
      
      public Grayscale (int width, int height, Data.Array.Type type) {
         super (width, height, type);
      }
      public float get (int x, int y) {
         return newCursor().setPosition(x, y).getValue();
      }
      public void set (int x, int y, float value) {
         newCursor().setPosition(x, y).setValue(value);
      }      
      public void drawLine (int x0, int y0, int x1, int y1, float value) {
         Image.drawLine(x0, y0, x1, y1, newPainter(value));
      }
      public void fillRect (int left, int top, int width, int height, float value) {
         Image.fillRect(left, top, width, height, newPainter(value));
      }
      
      public interface Cursor {
         Cursor setPosition(int x, int y);
         float getValue();
         void setValue(float val);
      }
      public abstract Cursor newCursor();      
      public abstract Painter newPainter(float value);
      
      // -------------------------------------------------------------------
      // 1 float per pixel
      // -------------------------------------------------------------------
      public static class Floats extends Image.Grayscale {

         public Floats (int width, int height) {
            super (width, height, Data.Array.Type.ONE_FLOAT);
         }
         public float[] array() {
            return ((Data.Array.Floats) data).array();
         }

         private static class Cursor implements Image.Grayscale.Cursor {
            private final float[] arr;
            private int width;
            private int loc;
            
            public Cursor(Image.Grayscale.Floats image) {
               arr = image.array();
               width = image.width;
               loc = 0;
            }
            public Cursor setPosition(int x, int y) {
               loc = (y * width + x);
               return this;
            }
            public float getValue() {
               return arr[loc];
            }
            public void setValue(float val) {
               arr[loc] = val;
            }
         }
         public Cursor newCursor() {
            return new Cursor(this);
         }
         public Painter newPainter(float value) {
            return new Image.Painter() {
               private Cursor cursor = newCursor();
               
               public void paint(int x, int y) {
                  cursor.setPosition(x, y).setValue(value);
               }
            };
         }
      }
   }
   
   // -------------------------------------------------------------------
   // Drawing Functions
   // -------------------------------------------------------------------
   
   public interface Painter {
      void paint(int x, int y);
   }
   public static void fillRect (int left, int top, int width, int height, Painter painter) {
      
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            painter.paint(left + x, top + y);
         }
      }
   }
   public static void drawLine (int x0, int y0, int x1, int y1, Painter painter) {
      
      int x = x0, y = y0;               // starting point
      int dx = x1 - x0, dy = y1 - y0;   // total delta
      
      int sx = (dx > 0 ? 1 : (dx < 0 ? -1 : 0));  // direction of x-step
      int sy = (dy > 0 ? 1 : (dy < 0 ? -1 : 0));  // direction of y-step
      
      if (dx < 0) dx = -dx;  // decision parameters
      if (dy < 0) dy = -dy;
      int ax = 2*dx, ay = 2*dy;
      int decx, decy;
      
      if (dy > dx) {
         // the line is more vertical than horizontal.
         decx = ax - dy;
         while (true) {
            painter.paint(x,y);
            if (y == y1) break;
            
            // each step moves "y" to the next pixel,
            // but only some steps move "x" to the next pixel:
            y += sy;
            if (decx >= 0) { decx -= ay; x += sx; }
            decx += ax;
         }
      } else {
         // the line is more horizontal than vertical
         decy = ay - dx;
         while (true) {
            painter.paint(x,y);
            if (x == x1) break;
            
            // each step moves "x" to the next pixel,
            // but only some steps move "y" to the next pixel:
            x += sx;
            if (decy >= 0) { decy -= ax; y += sy; }
            decy += ay;
         }
      }
   }
   
   
   // ---------------------
   // LISTENERS
   // (Not sure about adding "Listeners" to "Images".
   //  Maybe we "Image" should be a dumb-data-class,
   //  we could use Mutable<Image> to hold listeners, mutexes...
   // ---------------------
   //
   // Recent though suggests NO, we should not have listeners in Image.
   //
   // Maybe "Mutable<Image>" wraps Image along with a lock and a Listener.Set,
   // or maybe we go all the way to defining "Value.Item<Image,ImageChange>"
   //
   // which explicitly adds "print" & "parse" methods, and a "type" method which reveals
   //
   // public final Data.Listener.Set listeners = new Data.Listener.Set();
   // ---------------------
   
   
   
   // -----------------------------------------------------------------------
   // Integer Size / Position / Rect classes?
   // -----------------------------------------------------------------------
   
   public static class Size {
      public final int width, height;

      public Size (int width, int height) {
         this.width = width;
         this.height = height;
      }
      public int hashCode() {
         return Objects.hash(width, height);
      }
      public boolean equals(Size o) {
         return (width == o.width) && (height == o.height);
      }
      public boolean equals(Object o) {
         return (o != null) && (o instanceof Size) && equals((Size)o);
      }
      public String toString() {
         return String.format("(%d,%d)", width, height);
      }
      public static Size of (int width, int height) {
         return new Size(width, height);
      }
   }
   public static class Position {
      public final int x, y;

      public Position (int x, int y) {
         this.x = x;
         this.y = y;
      }
      public int hashCode() {
         return Objects.hash(x, y);
      }
      public boolean equals(Position o) {
         return (x == o.x) && (y == o.y);
      }
      public boolean equals(Object o) {
         return (o != null) && (o instanceof Position) && equals((Position)o);
      }
      public String toString() {
         return String.format("(%d,%d)", x, y);
      }
      public static Position of (int x, int y) {
         return new Position(x, y);
      }      
   }
   public static class Rect {
      public final int left, right, top, bottom;

      public Rect (int left, int right, int top, int bottom) {
         this.left = left;
         this.right = right;
         this.top = top;
         this.bottom = bottom;
      }
      public Size size() {
         return new Size(right-left, bottom-top);
      }
      public int hashCode() {
         return Objects.hash(left, right, top, bottom);
      }
      public boolean equals(Rect o) {
         return (left == o.left) && (right == o.right)
             && (top == o.top) && (bottom == o.bottom);
      }
      public boolean equals(Object o) {
         return (o != null) && (o instanceof Rect) && equals((Rect)o);
      }
      public String toString() {
         return String.format("(%d,%d)-(%d,%d)", left,top,right,bottom);
      }
      public static Rect of (int left, int right, int top, int bottom) {
         return new Rect(left, right, top, bottom);
      }
   }   
}

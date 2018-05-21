package com.generic.base;

import java.util.HashSet;

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
   // ---------------------
  
   public interface Listener {
      public void modified();
   }
   private HashSet<Listener> listeners;
   
   public void addListener(Listener listener) {
      listeners.add(listener);
   }
   public void removeListener(Listener listener) {
      listeners.remove(listener);
   }
   public void notifyListeners() {
      for (Listener listener : listeners) {
         listener.modified();
      }
   }
}

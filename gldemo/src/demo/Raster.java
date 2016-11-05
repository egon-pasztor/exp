package demo;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Raster {

   // -------------------------------------------------------------------
   // Utilities for managing raster images
   // -------------------------------------------------------------------

   public static class Image {
      public Image (String name, int width, int height) {
         this.name = name;
         this.width = width;
         this.height = height;
         pixels = new int[width*height];
      }
      
      public void clear() {
         int n=width*height;
         for (int i=0; i < n; ++i) {
            pixels[i] = 0;
         }
      }
      public void set(int x, int y, int color) {
         pixels[x+y*width] = color;
      }
      public void fillRect(int startX, int startY, int w, int h, int color) {
         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               set(startX+x, startY+y, color);
            }
         }
      }
      
      public String getName() {
         return name;
      }

      public String name;       
      public final int[] pixels;
      public final int width;
      public final int height;
   }
   
   // -------------------------------------------------------------------
   // From Resource
   // -------------------------------------------------------------------

   public static Image imageFromResource(String name) {
      System.out.format("Trying to load image named [%s]\n", name);
      BufferedImage im;
      try {
          im = ImageIO.read(Image.class.getResource(name));
      } catch (IOException e) {
          System.out.format("FAILED - Trying to load image named [%s]\n", name);
          e.printStackTrace();
          return null;
      }

      Image res = new Image(name, im.getWidth(), im.getHeight());
      for (int row = 0; row < res.height; row++) {
         for (int col = 0; col < res.width; col++) {
            int val = im.getRGB(col, row);
            res.pixels[col+row*res.width] = val;
            if ((row == 0) && (col == 0)) {
               System.out.format("At (0,0) we got 0x%8x\n", val);
            }
         }
      }
      return res;
   }
   
   // -------------------------------------------------------------------
   // Color
   // -------------------------------------------------------------------
   
   public static class ColorARGB {
       public final byte a,r,g,b;
   
       public ColorARGB (byte a, byte r, byte g, byte b) {
           this.a = a;
           this.r = r;
           this.g = g;
           this.b = b;
       }       

       public int toInteger() {
          return ((int)a)<<24 | ((int)r)<<16 | ((int)g)<<8 | ((int)b);
       }
       public String toString() {
          return String.format("#%02x%02x%02x%02x", r,g,b,a);
       }
   }
}

package com.generic.base;

import java.awt.Color;

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
       
       public Color color() {
          float rf = ((float)((int)r&0xff))/255.0f;
          float gf = ((float)((int)g&0xff))/255.0f;
          float bf = ((float)((int)b&0xff))/255.0f;
          return new Color(rf,gf,bf);
       }
   }
   
}

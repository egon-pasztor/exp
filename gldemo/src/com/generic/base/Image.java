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
   
   /*
   // -------------------------------------------------------------------
   // Image with one Integer per pixel
   // -------------------------------------------------------------------
   public static class Integers extends Image {
      public final int[] pixels;
      
      public Integers (int width, int height) {
         super(width, height);
         pixels = new int[width*height];
      }      
      public void set(int x, int y, int color) {
         pixels[x+y*width] = color;
      }
      public int get(int x, int y) {
         return pixels[x+y*width];
      }

      public void clear() {
         int n = pixels.length;
         for (int i = 0; i < n; ++i) {
            pixels[i] = 0;
         }
      }
      public void fillRect(int startX, int startY, int w, int h, int color) {
         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               set(startX+x, startY+y, color);
            }
         }
      }
   }

   // -------------------------------------------------------------------
   // Image with one Float per pixel
   // -------------------------------------------------------------------
   public static class Floats extends Image {
      public final float[] pixels;
      
      public Floats (int width, int height) {
         super(width, height);
         pixels = new float[width*height];
      }
      public void set(int x, int y, float color) {
         pixels[x+y*width] = color;
      }
      public float get(int x, int y) {
         return pixels[x+y*width];
      }
      
      public void clear() {
         int n=width*height;
         for (int i=0; i < n; ++i) {
            pixels[i] = 0;
         }
      }
      public void fillRect(int startX, int startY, int w, int h, float color) {
         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               set(startX+x, startY+y, color);
            }
         }
      }
   }
   */
   
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

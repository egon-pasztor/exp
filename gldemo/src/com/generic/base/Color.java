package com.generic.base;

import java.util.Objects;

public interface Color {

   public RGB.Floats rgbFloats();
   public RGB.Bytes  rgbBytes();
   
   public static RGB.Floats rgbFloats(float r, float g, float b) {
      return new RGB.Floats(r,g,b);
   }
   public static RGB.Bytes rgbBytes(byte r, byte g, byte b) {
      return new RGB.Bytes(r,g,b);
   }
   
   // -------------------------------------------------------------------
   // RGB-Based Color
   // -------------------------------------------------------------------
   public static class RGB {
      
      // -------------------------------------------------------------------
      // 3 RGB bytes (0-255)
      // -------------------------------------------------------------------
      public static class Bytes implements Color {
         public final byte r,g,b;
         
         public Bytes(byte r, byte g, byte b) {
            this.r = r; 
            this.g = g;
            this.b = b;
         }         
         public int hashCode() {
            return Objects.hash(r,g,b);
         }
         public boolean equals(RGB.Bytes o) {
            return (r == o.r) && (g == o.g) && (b == o.b);
         }
         public boolean equals(Object o) {
            return (o != null) && (o instanceof RGB.Bytes) && equals((RGB.Bytes)o);
         }
         
         public int toInteger() {
            return ((r & 0xff) << 16) 
                 | ((g & 0xff) << 8)
                 | ((b & 0xff));
         }
         public String toString() {
            return String.format("#%02x%02x%02x", r,g,b);
         }

         public RGB.Floats rgbFloats() { 
            return Color.rgbFloats(((int)(r & 0xff)) / ((float)255.0),
                                   ((int)(g & 0xff)) / ((float)255.0),
                                   ((int)(b & 0xff)) / ((float)255.0));
         }
         public RGB.Bytes rgbBytes() {
            return this;
         }         
      }

      // -------------------------------------------------------------------
      // 3 RGB floats (0.0-1.0)
      // -------------------------------------------------------------------
      public static class Floats implements Color {
         public final float r,g,b;
         
         public Floats (float r, float g, float b) {
            this.r = r; 
            this.g = g;
            this.b = b;
         }         
         public int hashCode() {
            return Objects.hash(r,g,b);
         }
         public boolean equals(RGB.Floats o) {
            return (r == o.r) && (g == o.g) && (b == o.b);
         }
         public boolean equals(Object o) {
            return (o != null) && (o instanceof RGB.Floats) && equals((RGB.Floats)o);
         }
         
         public RGB.Floats rgbFloats() { 
            return this;
         }
         public RGB.Bytes rgbBytes() {
            return Color.rgbBytes(((byte)(255 * r)),
                                  ((byte)(255 * g)),
                                  ((byte)(255 * b)));
         }
      }
   }
   
   // -------------------------------------------------------------------
   // Color as Hue/Saturation/Value
   // -------------------------------------------------------------------
   public static class HSV implements Color {
      public final float h,s,v;
      
      public HSV(float h, float s, float v) {
         this.h = h; 
         this.s = s;
         this.v = v;
      }
      public int hashCode() {
         return Objects.hash(h,s,v);
      }
      public boolean equals(HSV o) {
         return (h == o.h) && (s == o.s) && (v == o.v);
      }
      public boolean equals(Object o) {
         return (o != null) && (o instanceof HSV) && equals((HSV)o);
      }
      
      // -------------------------------------------------------------------
      // RGB to HSV conversion
      // -------------------------------------------------------------------
      public static HSV fromRGB (RGB.Floats rgb) {
         
         float max,min;
         max = ((max = (rgb.r > rgb.g) ? rgb.r : rgb.g) > rgb.b) ? max : rgb.b;
         min = ((min = (rgb.r < rgb.g) ? rgb.r : rgb.g) < rgb.b) ? max : rgb.b;

         float v = max;
         float s = (max == 0.0f) ? 0 : ((max-min) / max);

         float rc = 1.0f, gc = 1.0f, bc = 1.0f;
         if (max != min) {
            rc = (max - rgb.r) / (max - min);
            gc = (max - rgb.g) / (max - min);
            bc = (max - rgb.b) / (max - min); 
         }

         float h = (max == rgb.r) ? (float) ((1.0/6.0) * (bc-gc))
                 : (max == rgb.g) ? (float) ((1.0/6.0) * (2+rc-bc))
                                  : (float) ((1.0/6.0) * (4+gc-rc));
         while (h < 0) h += 1.0;

         return new HSV(h,s,v);
      }
      
      // -------------------------------------------------------------------
      // HSV to RGB conversion
      // -------------------------------------------------------------------
      public RGB.Floats rgbFloats () {
         
         float w = v;
         if (s == 0.0f) return Color.rgbFloats(w,w,w);
         
         int i = (int) (h * 6);
         float dH = h * 6 - i;
         
         float p  = v * (1.0f - s);
         float q  = v * (1.0f - (s * dH));
         float t  = v * (1.0f - (s * (1.0f - dH)));
         
         switch (i) {
             case 0: return Color.rgbFloats(w,t,p); 
             case 1: return Color.rgbFloats(q,w,p); 
             case 2: return Color.rgbFloats(p,w,t);
             case 3: return Color.rgbFloats(p,q,w); 
             case 4: return Color.rgbFloats(t,p,w);
            default: return Color.rgbFloats(w,p,q); 
         }
      }
      public RGB.Bytes rgbBytes () {
         return rgbFloats().rgbBytes();
      }
   }

   /*
   // -------------------------------------------------------------------
   // Color as 4 ARGB bytes
   // -------------------------------------------------------------------
   public static class ARGB implements Color {
      public final byte a,r,g,b;
      
      public ARGB(byte a, byte r, byte g, byte b) {
         this.a = a; 
         this.r = r; 
         this.g = g;
         this.b = b;
      }

      public int toInteger() {
         return ((int)a)<<24 | ((int)r)<<16 | ((int)g)<<8 | ((int)b);
      }
      public String toString() {
         return String.format("#%02x%02x%02x%02x", a,r,g,b);
      }

      public int hashCode() {
         return Objects.hash(r,g,b);
      }
      public boolean equals(RGB o) {
         return (r == o.r) && (g == o.g) && (b == o.b);
      }
      public boolean equals(Object o) {
         return (o != null) && (o instanceof RGB) && equals((RGB)o);
      }
   }
   */

   
   
}

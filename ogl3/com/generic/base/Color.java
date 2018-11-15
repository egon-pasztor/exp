package com.generic.base;

import java.util.Objects;

public abstract class Color {

   public static RGB.Floats rgbFloats(float r, float g, float b) {
      return new RGB.Floats(r,g,b);
   }
   public static RGB.Bytes rgbBytes(int r, int g, int b) {
      return new RGB.Bytes((byte)r, (byte)g, (byte)b);
   }
   public static HSV.Floats hsvFloats(float h, float s, float v) {
      return new HSV.Floats (h,s,v);
   }

   public static final RGB.Floats WHITE   = rgbFloats (1.0f, 1.0f, 1.0f);
   public static final RGB.Floats RED     = rgbFloats (1.0f, 0.0f, 0.0f);
   public static final RGB.Floats YELLOW  = rgbFloats (1.0f, 1.0f, 0.0f);
   public static final RGB.Floats GREEN   = rgbFloats (0.0f, 1.0f, 0.0f);
   public static final RGB.Floats CYAN    = rgbFloats (0.0f, 1.0f, 1.0f);
   public static final RGB.Floats BLUE    = rgbFloats (0.0f, 0.0f, 1.0f);
   public static final RGB.Floats VIOLET  = rgbFloats (1.0f, 0.0f, 1.0f);
   public static final RGB.Floats BLACK   = rgbFloats (0.0f, 0.0f, 0.0f);
   
   // -------------------------------------------------------------------
   // -------------------------------------------------------------------
   
   public abstract RGB.Floats rgbFloats();
   
   public RGB.Bytes rgbBytes() {
      return rgbFloats().rgbBytes();
   }
   public HSV.Floats hsvFloats() {
      return HSV.Floats.fromRGB(rgbFloats());
   }
   
   public String toHex() {
      return rgbBytes().toHex();
   }
   public int toInteger() {
      return rgbBytes().toInteger();
   }
   
   // -------------------------------------------------------------------
   // RGB-Based Color
   // -------------------------------------------------------------------
   public abstract static class RGB extends Color {
      
      // -------------------------------------------------------------------
      // 3 RGB bytes (0-255)
      // -------------------------------------------------------------------
      public static class Bytes extends Color.RGB {
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
         
         public String toHex() {
            return String.format("#%02x%02x%02x", r,g,b);
         }
         public int toInteger() {
            return ((r & 0xff) << 16) 
                 | ((g & 0xff) << 8)
                 | ((b & 0xff));
         }
         public static Color.RGB.Bytes fromInteger(int i) {
            return Color.rgbBytes((i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff);
         }

         public RGB.Floats rgbFloats() { 
            return Color.rgbFloats((r & 0xff) / ((float)255.0),
                                   (g & 0xff) / ((float)255.0),
                                   (b & 0xff) / ((float)255.0));
         }
         public RGB.Bytes rgbBytes() {
            return this;
         }         
      }

      // -------------------------------------------------------------------
      // 3 RGB floats (0.0-1.0)
      // -------------------------------------------------------------------
      public static class Floats extends Color.RGB {
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
            return Color.rgbBytes((int) (255 * r), (int) (255 * g), (int) (255 * b));
         }
      }
   }
   
   // -------------------------------------------------------------------
   // HSV-Based Color
   // -------------------------------------------------------------------
   public abstract static class HSV extends Color {
      
      public static class Floats extends Color.HSV {
         public final float h,s,v;
         
         public Floats(float h, float s, float v) {
            this.h = h; 
            this.s = s;
            this.v = v;
         }
         public int hashCode() {
            return Objects.hash(h,s,v);
         }
         public boolean equals(HSV.Floats o) {
            return (h == o.h) && (s == o.s) && (v == o.v);
         }
         public boolean equals(Object o) {
            return (o != null) && (o instanceof HSV.Floats) && equals((HSV.Floats)o);
         }
         
         // -------------------------------------------------------------------
         // RGB to HSV conversion
         // -------------------------------------------------------------------
         public static HSV.Floats fromRGB (RGB.Floats rgb) {
            
            float max,min;
            max = ((max = (rgb.r > rgb.g) ? rgb.r : rgb.g) > rgb.b) ? max : rgb.b;
            min = ((min = (rgb.r < rgb.g) ? rgb.r : rgb.g) < rgb.b) ? max : rgb.b;
   
            float v = max;
            float s = (max == 0) ? 0 : ((max-min) / max);
   
            float rc = 1, gc = 1, bc = 1;
            if (max != min) {
               rc = (max - rgb.r) / (max - min);
               gc = (max - rgb.g) / (max - min);
               bc = (max - rgb.b) / (max - min); 
            }

            float h = (max == rgb.r) ? ((bc-gc)   / 6)
                    : (max == rgb.g) ? ((2+rc-bc) / 6)
                                     : ((4+gc-rc) / 6);
            if (h < 0) h += 1;
   
            return new HSV.Floats(h,s,v);
         }
         
         // -------------------------------------------------------------------
         // HSV to RGB conversion
         // -------------------------------------------------------------------
         public RGB.Floats rgbFloats () {
            
            float w = v;
            if (s == 0) return Color.rgbFloats(w,w,w);
            
            int i = (int) (h * 6);
            float dH = (h * 6) - i;
            
            float p  = v * (1 - s);
            float q  = v * (1 - (s * dH));
            float t  = v * (1 - (s * (1 - dH)));
            
            switch (i) {
                case 0: return Color.rgbFloats(w,t,p); 
                case 1: return Color.rgbFloats(q,w,p); 
                case 2: return Color.rgbFloats(p,w,t);
                case 3: return Color.rgbFloats(p,q,w); 
                case 4: return Color.rgbFloats(t,p,w);
               default: return Color.rgbFloats(w,p,q); 
            }
         }
         public HSV.Floats hsvFloats() {
            return this;
         }
      }
   }
      
   // -------------------------------------------------------------------
   // -------------------------------------------------------------------

   // This is where we're going to but the "color-schemes" code for computing
   // window bevel colors..
   
}

package com.generic.base;

import java.util.Objects;

public interface Color {

   // -------------------------------------------------------------------
   // Color as 3 RGB bytes
   // -------------------------------------------------------------------
   public static class RGB implements Color {
      public final byte r,g,b;
      
      public RGB(byte r, byte g, byte b) {
         this.r = r; 
         this.g = g;
         this.b = b;
      }

      public int toInteger() {
         return ((int)r)<<16 | ((int)g)<<8 | ((int)b);
      }
      public String toString() {
         return String.format("#%02x%02x%02x", r,g,b);
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

   // -------------------------------------------------------------------
   // Color as Hue/Saturation/Value
   // -------------------------------------------------------------------
   public static class HSV implements Color {
      public final byte h,s,v;
      
      public HSV(byte h, byte s, byte v) {
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
   }

   
   
}

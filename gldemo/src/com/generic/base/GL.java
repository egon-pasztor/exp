package com.generic.base;

public class GL {

   public enum PrimitiveType {
      FLOAT, INT
   }
   public static class VertexBuffer {

      // --------------------------------------
      // First, a Type class
      // --------------------------------------
      public static class Type {
         public final int numElements;
         public final PrimitiveType primitive;
         public final int primitivesPerElement;
         
         public Type (int numElements, PrimitiveType primitive, int primitivesPerElement) {
            this.numElements = numElements;
            this.primitive = primitive;
            this.primitivesPerElement = primitivesPerElement;
         }
         // TODO: hashcode, equals...
      }
      // -------------------------------------
      // Now, a VertexBuffer is what?
      // It has a Type:

      public final Type type;
      public Integer id;
      
      
      and (once it's setup) a GL ID
      // It has 
      
   }
   
}

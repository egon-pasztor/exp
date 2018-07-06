package com.generic.base;

import java.util.HashSet;
import java.util.Objects;

public class Data {

   // ----------------------------------------
   // The "Primitive Array" concept
   // ----------------------------------------
   public static abstract class Array {
      
      public enum Primitive { INTEGERS, FLOATS, BYTES };

      // --------------------------------
      // Type
      // --------------------------------
      public static class Type {

         public final int primitivesPerElement;
         public final Primitive primitive;
         
         public static Type of (int primitivesPerElement, Primitive primitive) {
            return new Type(primitivesPerElement, primitive);
         }
         public Type (int primitivesPerElement, Primitive primitive) {
            this.primitivesPerElement = primitivesPerElement;
            this.primitive = primitive;
         }
         public int hashCode() {
            return Objects.hash(primitive, primitivesPerElement);
         }
         public boolean equals(Type o) {
            return (primitive            == o.primitive)
                && (primitivesPerElement == o.primitivesPerElement);
         }
         public boolean equals(Object o) {
            return (o != null) && (o instanceof Type) && equals((Type)o);
         }
         
         public static final Type ONE_INTEGER    = Type.of(1, Primitive.INTEGERS);
         public static final Type TWO_INTEGERS   = Type.of(2, Primitive.INTEGERS);
         public static final Type THREE_INTEGERS = Type.of(3, Primitive.INTEGERS);
         public static final Type FOUR_INTEGERS  = Type.of(4, Primitive.INTEGERS);
         
         public static final Type ONE_FLOAT      = Type.of(1, Primitive.FLOATS);
         public static final Type TWO_FLOATS     = Type.of(2, Primitive.FLOATS);
         public static final Type THREE_FLOATS   = Type.of(3, Primitive.FLOATS);
         public static final Type FOUR_FLOATS    = Type.of(4, Primitive.FLOATS);         
         public static final Type SIX_FLOATS     = Type.of(6, Primitive.FLOATS);
         public static final Type NINE_FLOATS    = Type.of(9, Primitive.FLOATS);
         public static final Type SIXTEEN_FLOATS = Type.of(16, Primitive.FLOATS);

         public static final Type THREE_BYTES = Type.of(3, Primitive.BYTES);
      }
      
      public static Array create(Type type) {
         switch (type.primitive) {
           case INTEGERS: return new Array.Integers(type.primitivesPerElement);
           case FLOATS:   return new Array.Floats(type.primitivesPerElement);
           case BYTES:    return new Array.Bytes(type.primitivesPerElement);
         };
         throw new RuntimeException();
      }

      // --------------------------------
      // Array
      // --------------------------------
      protected Array (Type type) {
         this.type = type;
         this.numElements = 0;
      }
      public final Type type;
      public abstract Object array();
      
      protected int numElements;
      public int numElements() { return numElements; }
      public abstract void setNumElements(int newNumElements);
      
      // -----------------------------------------------------------------
      public static class Integers extends Array {
         public Integers (int primitivesPerElement) {
            super(new Type(primitivesPerElement, Primitive.INTEGERS));
            array = new int[type.primitivesPerElement * INITIAL_CAPACITY];
         }
         private int[] array = null;
         public int[] array() { return array; }
         
         // - - - - - - - - - - - - - 
         public void setNumElements(int newNumElements) {
            int lengthNeeded = type.primitivesPerElement * newNumElements;
            if (array.length < lengthNeeded) {
               int newLength = array.length;
               while (newLength < lengthNeeded) newLength *= 2;
               int[] newArray = new int [newLength];
               System.arraycopy(array, 0, newArray, 0, type.primitivesPerElement * numElements);
               array = newArray;
            }
            numElements = newNumElements;
         }
      }
      // -----------------------------------------------------------------
      public static class Floats extends Array {
         public Floats (int primitivesPerElement) {
            super(new Type(primitivesPerElement, Primitive.FLOATS));
            array = new float[type.primitivesPerElement * INITIAL_CAPACITY];
         }
         private float[] array = null;
         public float[] array() { return array; }
         
         // - - - - - - - - - - - - - 
         public void setNumElements(int newNumElements) {
            int lengthNeeded = type.primitivesPerElement * newNumElements;
            if (array.length < lengthNeeded) {
               int newLength = array.length;
               while (newLength < lengthNeeded) newLength *= 2;
               float[] newArray = new float [newLength];
               System.arraycopy(array, 0, newArray, 0, type.primitivesPerElement * numElements);
               array = newArray;
            }
            numElements = newNumElements;
         }
      }
      // -----------------------------------------------------------------
      public static class Bytes extends Array {
         public Bytes (int primitivesPerElement) {
            super(new Type(primitivesPerElement, Primitive.BYTES));
            array = new byte[type.primitivesPerElement * INITIAL_CAPACITY];
         }
         private byte[] array = null;
         public byte[] array() { return array; }
         
         // - - - - - - - - - - - - - 
         public void setNumElements(int newNumElements) {
            int lengthNeeded = type.primitivesPerElement * newNumElements;
            if (array.length < lengthNeeded) {
               int newLength = array.length;
               while (newLength < lengthNeeded) newLength *= 2;
               byte[] newArray = new byte [newLength];
               System.arraycopy(array, 0, newArray, 0, type.primitivesPerElement * numElements);
               array = newArray;
            }
            numElements = newNumElements;
         }
      }
      // -----------------------------------------------------------------
      private static int INITIAL_CAPACITY = 4;
   }
   
   // -----------------------------------
   // The Listener concept
   // -----------------------------------
   public interface Listener {
      public void onChange();
      
      public static class Set {
         private HashSet<Listener> listeners = new HashSet<Listener>();
         
         public void add(Listener listener) {
            listeners.add(listener);
         }
         public void remove(Listener listener) {
            listeners.remove(listener);
         }
         public void changeOccurred() {
            for (Listener listener : listeners) {
               listener.onChange();
            }
         }
      }
   }
   
   // -----------------------------------
   // Owner/Mutable
   // -----------------------------------
   // A Data.Owner provides a lock
   // -----------------------------------
   public interface Owner {
      public Object lock();
   }
   // -----------------------------------
   // A Data.Mutable<Content> has
   // an Owner, a Content, and Listeners
   // -----------------------------------
   public static class Mutable<Content> {
      public final Owner owner;
      public final Listener.Set listeners;
      
      public Mutable(Owner owner) {
         this.owner = owner;
         this.listeners = new Listener.Set();
      }
      
      private Content content;
      public Content content() { return content; }
      public void setContent(Content newContent) { 
         if (!Objects.equals(content, newContent)) {
            content = newContent;
            listeners.changeOccurred();
         }
      }
   }   
}

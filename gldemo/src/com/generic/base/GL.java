package com.generic.base;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GL {

   public static class State {
      private final HashSet<VertexBuffer> buffers;
      
      public State() {
         buffers = new HashSet<VertexBuffer>();
      }
      
      // So, given a VertexBuffer, the Type is:
      //     { primitive / #-primitives-per-element / #-elements }
      // and the length of the array, #-elements, is part of the type and fixed.
      //
      // except:
      //    seriously, this object is meant to reflect how GL.State actually works,
      //    and a GL vertexbuffer cannot change size.   so, a rebinding is necessary.
      //    either GL.Implementation does it, or whatever calls GL.State methods...
      //
      // -----------------
      // so we have a vote towards no-resize.
      //
      // that means, if mesh2 size changes,
      //   it's up to Scene to change the GL.State if a Mesh in the scene changes..
      // 
      
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
            public int hashCode() {
               return Objects.hash(numElements, primitive, primitivesPerElement);
            }
            public boolean equals(Type o) {
               return (numElements          == o.numElements)
                   && (primitive            == o.primitive)
                   && (primitivesPerElement == o.primitivesPerElement);
            }
            public boolean equals(Object o) {
               return (o != null) && (o instanceof Type) && equals((Type)o);
            }
         }
         public enum PrimitiveType {
            FLOAT, INT
         }
     
         // -------------------------------------
         // Now, a VertexBuffer is what?
         // It has a Type:
   
         public Type type;
         public int[] array;
   
         // A vertex buffer usually has a Filler, an object which can
         // check if the array needs to be refreshed and do so..
         public interface Filler {
            boolean refreshIfNeeded(int[] array);
         }
         public Filler filler;
         public void refreshIfNeeded() {
           if ((filler != null) && filler.refreshIfNeeded(array)) modified();
         }
      
         // ---------------------
         // OBSERVABLE
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
         public void modified() {
            for (Listener listener : listeners) {
               listener.modified();
            }
         }
      }

      
      
      public void addVertexBuffer(VertexBuffer buffer) {
         buffers.add(buffer);
         for (Listener listener : listeners) {
            listener.vertexBufferAdded(buffer);
         }
      }
      public void deleteVertexBuffer(VertexBuffer buffer) {
         buffers.remove(buffer);
         for (Listener listener : listeners) {
            listener.vertexBufferDeleted(buffer);
         }
      }
      
      public interface Listener {
         public void vertexBufferAdded(VertexBuffer buffer);
         public void vertexBufferDeleted(VertexBuffer buffer);
      }
      private HashSet<Listener> listeners;
      public void addListener(Listener listener) {
         listeners.add(listener);
      }
      public void removeListener(Listener listener) {
         listeners.remove(listener);
      }
   }
   
   
   
   
   public static abstract class Implementation {
      
      private final State state;
      private final State.Listener listener;
      private final HashMap<GL.State.VertexBuffer, VertexBuffer> buffers;
      
      
      public static class VertexBuffer {
         private GL.State.VertexBuffer state;
         private GL.State.VertexBuffer.Listener listener;
         private boolean modified, deleted;
         
         VertexBuffer(GL.State.VertexBuffer state) {
            this.state = state;
            this.modified = true;
            this.deleted = false;
            this.listener = new GL.State.VertexBuffer.Listener() {
               public void modified() {
                  modified = true;
               }
            };
            state.addListener(listener);
         }
         public void disconnect() {
            state.removeListener(listener);
            state = null;
            listener = null;
            deleted = true;
         }
      }
      private VertexBuffer newVertexBuffer(GL.State.VertexBuffer buffer) {
         return new VertexBuffer(buffer);
      }
            
      public Implementation(State state) {
         this.state = state;
         this.buffers = new HashMap<GL.State.VertexBuffer, VertexBuffer>();
         this.listener = new GL.State.Listener() {
            public void vertexBufferAdded (GL.State.VertexBuffer buffer) {
               buffers.put(buffer, Implementation.this.newVertexBuffer(buffer));
            }
            public void vertexBufferDeleted(GL.State.VertexBuffer buffer) {
               buffers.get(buffer).disconnect();
            }
         };
         state.addListener(listener);
         
      }
      
      
      
   }
}

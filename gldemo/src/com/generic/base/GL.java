package com.generic.base;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GL {

   public static class State {
      private final HashSet<VertexBuffer> buffers;
      
      public State() {
         buffers = new HashSet<VertexBuffer>();
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
      
         // The point is, there's a boolean attached to each VertexBuffer
         // that reveals whether it's been changed since the last time...
         // .. something looked in on it.  And that's the crux of my stuckness..
         // I keep feeling that GL Scene should support multiple GL Renderers..
         //    ..but maybe that's wrong.  I mean, we do eventually want a program
         //    that computes a 3d world and tells a remote set of clients.
         //    for example using the google-interactive api ...
         //    in addition to the local gpu ..
         //
         // Hm.. we could, for example, record a version number that incremented...
         //
         // public boolean arrayModified;
        
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
            listener.newVertexBuffer(buffer);
         }
      }
      public void deleteVertexBuffer(VertexBuffer buffer) {
         buffers.remove(buffer);
         for (Listener listener : listeners) {
            listener.vertexBufferDeleted(buffer);
         }
      }
      
      public interface Listener {
         public void newVertexBuffer(VertexBuffer buffer);
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
            public void newVertexBuffer(GL.State.VertexBuffer buffer) {
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

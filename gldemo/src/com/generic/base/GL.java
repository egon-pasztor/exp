package com.generic.base;

import com.generic.base.Mesh2.PrimitiveArray;
import com.generic.base.Mesh2.DataLayer.Type;

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
      
      // ------------------------------------------
      // VertexBuffer
      // ------------------------------------------
      public static class VertexBuffer {

         public static class Type {
            public enum Primitive { INTEGER, FLOAT };
            
            public final int primitivesPerElement;
            public final Primitive primitive;
            public final int numElements;
            
            public Type (int primitivesPerElement, Primitive primitive, int numElements) {
               this.primitivesPerElement = primitivesPerElement;
               this.primitive = primitive;
               this.numElements = numElements;
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
     
         // -------------------------------------
         // Now, a VertexBuffer is what?
         // It has a Type:
   
         public final GL.State parent;
         public final Type type;
         
         //
         public int[] array;
   
         private VertexBuffer(GL.State parent, Type type, PrimitiveArray data) {
            this.parent = parent;
            this.type = type;
            this.data = data;
         }

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

      
      
      public VertexBuffer createVertexBuffer(VertexBuffer.Type type) {
         VertexBuffer buffer = new VertexBuffer(this, type);
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
   
   //
   // so, if mesh2 size changes,
   //   the mesh2 will notify the Scene.Model ...
   //   a Scene will probably has a HashMap from Mesh2 to Scene.MeshInfo 
   //       which will be responsible for calling createVertexBuffer on the GL.State,
   //       and it will be responsible for creating the "Filler" classes that 
   //        Position / Texture / Color vertexarrays
   //     
   //   
   //   it's up to Scene to change the GL.State if a Mesh in the scene changes..
   // 
   
   
   
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

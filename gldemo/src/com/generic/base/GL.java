package com.generic.base;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public interface GL {
   
   // ------------------------------------------
   // VertexBuffer
   // ------------------------------------------
   public interface VertexBuffer {

      Data.Array.Type type();
      int numElements();      
      void update (Data.Array data);
      
      void disconnect   ();
      boolean connected ();
   }
   public VertexBuffer initVertexBuffer (Data.Array data);
   
   // ------------------------------------------
   // Sampler
   // ------------------------------------------
   public interface Sampler {

      Data.Array.Type type();
      int width();
      int height();
      
      void update (Image image);
      
      void disconnect   ();
      boolean connected ();
   }
   public Sampler initSampler (Image image);


   // ------------------------------------------------------------------------------
   // Likewise for samplers... then..
   // ------------------------------------------------------------------------------
   
   
   public static interface Shader {
      public enum Type { INTEGER, FLOAT };
      
   }
   
   public Shader initShader (Shader.Type type);
   
   
   
   
   /*

      protected VertexBuffer(Type type) {
         this.type = type;
      }


      // -----------------------------------------------------------------
      // Creating int[] and float[] VertexBuffers
      // -----------------------------------------------------------------
      public static VertexBuffer create(Type type, int numElements) {
         return (type.primitive == Type.Primitive.INTEGER) 
              ? new VertexBuffer.Integers(type.primitivesPerElement, numElements)
              : new VertexBuffer.Floats(type.primitivesPerElement, numElements);
      }
      // -----------------------------------------------------------------
      public static class Integers extends VertexBuffer {
         public Integers (int primitivesPerElement, int numElements) {
            super(new Type(primitivesPerElement, Type.Primitive.INTEGER));
            array = new int[primitivesPerElement * numElements];
         }      
         public final int[] array;
      }
      // -----------------------------------------------------------------
      public static class Floats extends VertexBuffer {
         public Floats (int primitivesPerElement, int numElements) {
            super(new Type(primitivesPerElement, Type.Primitive.FLOAT));
            array = new float[primitivesPerElement * numElements];
         }      
         public final float[] array;
      }
   }
   
   
   
   
   */
   
   
   /*
   // ------------------------------------------
   // State
   // ------------------------------------------
   public static class State {
      private final HashSet<VertexBuffer> buffers;
      
      public State() {
         buffers = new HashSet<VertexBuffer>();
      }
  
      public VertexBuffer createVertexBuffer(VertexBuffer.Type type) {
         VertexBuffer buffer = new VertexBuffer(this, type);
         buffers.add(buffer);
         for (Listener listener : listeners) {
            listener.vertexBufferAdded(buffer);
         }
         return buffer;
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
   
   
   public static abstract class Renderer {
      
      private final State state;
      private final State.Listener listener;
      private final HashMap<GL.VertexBuffer, VertexBuffer> buffers;
      
      
      public static class VertexBuffer {
         private GL.VertexBuffer state;
         private GL.VertexBuffer.Listener listener;
         private boolean modified, deleted;
         
         VertexBuffer(GL.VertexBuffer state) {
            this.state = state;
            this.modified = true;
            this.deleted = false;
            this.listener = new GL.VertexBuffer.Listener() {
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
      private VertexBuffer newVertexBuffer(GL.VertexBuffer buffer) {
         return new VertexBuffer(buffer);
      }
            
      public Renderer(State state) {
         this.state = state;
         this.buffers = new HashMap<GL.VertexBuffer, VertexBuffer>();
         this.listener = new GL.State.Listener() {
            public void vertexBufferAdded (GL.VertexBuffer buffer) {
               buffers.put(buffer, Renderer.this.newVertexBuffer(buffer));
            }
            public void vertexBufferDeleted(GL.VertexBuffer buffer) {
               buffers.get(buffer).disconnect();
            }
         };
         state.addListener(listener);
         
      }
      
      
      
   }
   */
}

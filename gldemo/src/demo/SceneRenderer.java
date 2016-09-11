package demo;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.util.Random;

class SceneRenderer extends GLCanvas implements GLEventListener
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final static double CAMERA_Z = 4.0;
   private final static double FOVY = 45.0; // field-of-view angle around Y

   private final static double NEAR = 1.0; // Z values < NEAR are clipped
   private final static double FAR = 7.0;  // Z values > FAR are clipped

   private GLU glu = new GLU ();

   // Do not generate a rotation increment greater than MAX_ROT_INC degrees.

   private final static float MAX_ROT_INC = 15.0f;

   // Total and increment rotation variables.

   private float rotAngleX, rotAngleY, rotAngleZ;
   private float incX, incY, incZ;

   private Texture [] textures;

   SceneRenderer (GLCapabilities capabilities)
   {
      super(capabilities);
      addGLEventListener (this);
   }

   public void init (GLAutoDrawable drawable)
   {
      GL gl = drawable.getGL();
      
      if (gl.isGL2()) {
         GL2 gl2 = gl.getGL2();
         System.out.format("Got GL2\n");
         
         gl2.glEnable (GL2.GL_DEPTH_TEST);

         // Initialize rotation accumulator and increment variables.

         rotAngleX = rotAngleY = rotAngleZ = 0.0f;
         Random rand = new Random ();
         incX = rand.nextFloat ()*MAX_ROT_INC;
         incY = rand.nextFloat ()*MAX_ROT_INC;
         incZ = rand.nextFloat ()*MAX_ROT_INC;

         // Load six 2D textures to decal the cube. If the image file on which the
         // texture is based does not exist, load() returns null.

         textures = new Texture [6];
         textures [0] = load ("Texture.jpg", gl2);
         textures [1] = load ("Texture.jpg", gl2);
         textures [2] = load ("Texture.jpg", gl2);
         textures [3] = load ("Texture.jpg", gl2);
         textures [4] = load ("Texture.jpg", gl2);
         textures [5] = load ("Texture.jpg", gl2);         
      }
      
      if (gl.isGL3()) {
         GL3 gl3 = drawable.getGL().getGL3();
         System.out.format("Got GL3\n");
         
      }
   }

   public void display (GLAutoDrawable drawable)
   {
      // Compute total rotations around X, Y, and Z axes.

      rotAngleX += incX; rotAngleX %= 360.0f;
      rotAngleY += incY; rotAngleY %= 360.0f;
      rotAngleZ += incZ; rotAngleZ %= 360.0f;

      GL2 gl = drawable.getGL().getGL2();

      // Clear the drawing surface to the background color, which defaults to
      // black.

      gl.glClear (GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

      // Reset the modelview matrix.

      gl.glLoadIdentity ();

      // The camera is currently positioned at the (0, 0, 0) origin, its lens
      // is pointing along the negative Z axis (0, 0, -1) into the screen, and
      // its orientation up-vector is (0, 1, 0). The following call positions
      // the camera at (0, 0, CAMERA_Z), points the lens towards the origin,
      // and keeps the same up-vector.

      glu.gluLookAt (0.0, 0.0, CAMERA_Z, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);

      // Perform the rotations.

      gl.glRotatef (rotAngleX, 1.0f, 0.0f, 0.0f);
      gl.glRotatef (rotAngleY, 0.0f, 1.0f, 0.0f);
      gl.glRotatef (rotAngleZ, 0.0f, 0.0f, 1.0f);

      // Draw the textured cube.

      drawCube (gl);
   }

   void drawCube (GL2 gl)
   {
      for (Texture texture: textures)
      {
           if (texture == null)
               continue;

           // Extract the texture's coordinates.

           TextureCoords tc = texture.getImageTexCoords ();
           float tx1 = tc.left ();
           float ty1 = tc.top ();
           float tx2 = tc.right ();
           float ty2 = tc.bottom ();

           // Enable two-dimensional texturing.

           texture.enable (gl);

           // Bind this texture to the current rendering context.

           texture.bind (gl);

           gl.glBegin (GL2.GL_QUADS);

           // Associate the upper-left corner texture coordinates with the
           // upper-left corner cube face coordinates. Continue to do so for
           // the remaining coordinates.

           gl.glTexCoord2f (tx1, ty1);

           if (texture == textures [0])
           {
               gl.glVertex3f (-1.0f, 1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (-1.0f, -1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (1.0f, -1.0f, 1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (1.0f, 1.0f, 1.0f);
           }
           else
           if (texture == textures [1])
           {
               gl.glVertex3f (1.0f, 1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (1.0f, -1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (1.0f, 1.0f, -1.0f);
           }
           else
           if (texture == textures [2])
           {
               gl.glVertex3f (1.0f, 1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (-1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (-1.0f, 1.0f, -1.0f);
           }
           else
           if (texture == textures [3])
           {
               gl.glVertex3f (-1.0f, 1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (-1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (-1.0f, -1.0f, 1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (-1.0f, 1.0f, 1.0f);
           }
           else
           if (texture == textures [4])
           {
               gl.glVertex3f (-1.0f, -1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (-1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (1.0f, -1.0f, -1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (1.0f, -1.0f, 1.0f);
           }
           else
           {
               gl.glVertex3f (-1.0f, 1.0f, -1.0f);
               gl.glTexCoord2f (tx2, ty1);
               gl.glVertex3f (-1.0f, 1.0f, 1.0f);
               gl.glTexCoord2f (tx2, ty2);
               gl.glVertex3f (1.0f, 1.0f, 1.0f);
               gl.glTexCoord2f (tx1, ty2);
               gl.glVertex3f (1.0f, 1.0f, -1.0f);
           }

           gl.glEnd ();

           // Disable two-dimensional texturing.

           texture.disable (gl);
      }
   }

   Texture load (String filename, GL2 gl)
   {
      filename = "/demo/data/" + filename;
      Texture texture = null;

      try
      {
          // Create an OpenGL texture from the specified file. Do not create
          // mipmaps.

          texture = TextureIO.newTexture(
                this.getClass().getResource(filename), false, ".jpg");

          //texture = TextureIO.newTexture (new File (filename), false);

          // Use the NEAREST magnification function when the pixel being
          // textured maps to an area less than or equal to one texture
          // element (texel).

          texture.setTexParameteri (gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);

          // Use the NEAREST minification function when the pixel being
          // textured maps to an area greater than one texel.

          texture.setTexParameteri (gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
      }
      catch (Exception e)
      {
          System.out.println ("error loading texture from "+filename+": "+e);
      }

      return texture;
   }

   public void reshape (GLAutoDrawable drawable, int x, int y, int width,
                        int height)
   {
      GL2 gl = drawable.getGL().getGL2 ();

      // We don't need to invoke gl.glViewport(x, y, width, height) because
      // this is done automatically by JOGL prior to invoking reshape().

      // Because the modelview matrix is currently in effect, we need to
      // switch to the projection matrix before we can establish a perspective
      // view.

      gl.glMatrixMode (GL2.GL_PROJECTION);

      // Reset the projection matrix.

      gl.glLoadIdentity ();

      // Establish a perspective view with an FOVY-degree viewing angle based
      // on the drawable's width and height. Furthermore, set the near and far
      // clipping planes to NEAR and FAR, respectively. All drawing must take
      // place between these planes. The view volume is assigned the same
      // aspect ratio as the viewport, to prevent distortion.

      float aspect = (float) width/(float) height;
      glu.gluPerspective (FOVY, aspect, NEAR, FAR);

      // From now on, we'll work with the modelview matrix.

      gl.glMatrixMode (GL2.GL_MODELVIEW);
   }

   public void displayChanged (GLAutoDrawable drawable, boolean modeChanged,
                               boolean deviceChanged)
   {
   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
      // TODO Auto-generated method stub
      
   }
}

// JOGLDemo5.java
package demo;
// This demo renders a textured cube, with each side displaying a different
// texture.

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;


public class JOGLDemo5 extends JFrame
{
   final static int WIDTH = 400;
   final static int HEIGHT = 400;

   final static int FPS = 10;

   FPSAnimator animator;

   public JOGLDemo5 ()
   {
      super ("JOGLDemo #5");

      addWindowListener (new WindowAdapter ()
                         {
                             public void windowClosing (WindowEvent we)
                             {
                                new Thread ()
                                {
                                    public void run ()
                                    {
                                       animator.stop ();
                                       System.exit (0);
                                    }
                                }.start ();
                             }
                         });

//      GLProfile profile = GLProfile.getMaxProgrammable(true);
      GLProfile profile = GLProfile.getDefault();
      GLCapabilities capabilities = new GLCapabilities(profile);
      
      SceneRenderer sr = new SceneRenderer (capabilities);
      sr.setPreferredSize (new Dimension (WIDTH, HEIGHT));

      getContentPane ().add (sr);

      pack ();
      setVisible (true);

      animator = new FPSAnimator (sr, FPS, true);
      animator.start ();
   }

   public static void main (String [] args)
   {
      Runnable r = new Runnable ()
                   {
                       public void run ()
                       {
                          new JOGLDemo5 ();
                       }
                   };
      EventQueue.invokeLater (r);
   }
}

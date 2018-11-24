
package com.generic.demo.linux;

import com.generic.base.linux.Jogl;
import com.generic.demo.Demo;

public class DemoMain {

   // -----------------------------------------------------------
   // MAIN
   // -----------------------------------------------------------
   public static void main(String[] args) {
      System.out.format("-*- Hello world from DemoMain.main(String[] args).\n");
      new Demo(Jogl.platform());
   }
}

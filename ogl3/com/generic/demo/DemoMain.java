
package com.generic.demo;

import com.generic.base.Demo;
import com.generic.base.Platform;

public class DemoMain {

   // -----------------------------------------------------------
   // MAIN
   // -----------------------------------------------------------
   public static void main(String[] args) {
      System.out.format("-*- Hello world from DemoMain.main(String[] args).\n");
      Platform platform = Jogl.platform();
      Demo demo = new Demo(platform);
   }
}

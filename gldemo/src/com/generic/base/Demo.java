package com.generic.base;

import com.generic.base.Algebra.Vector3;

public class Demo {

   private final Platform platform;
   private final Graphics3D graphics;
   
   public Demo(Platform platform) {
      this.platform = platform;
      graphics = new Graphics3D();
      platform.root3D().setRenderer(graphics);
   }

   public void render(Graphics3D gl) {
      Data.Array positions = Data.Array.create(Data.Array.Type.FOUR_FLOATS);
      { positions.setNumElements(3);
        float[] positions_array = ((Data.Array.Floats) positions).array();
        
        Vector3 vertex0Pos = Vector3.of(1,0,0);
        Vector3 vertex1Pos = Vector3.of(0,1,0);
        Vector3 vertex2Pos = Vector3.of(0,0,1);
        
        
      }
      
   }
   
}

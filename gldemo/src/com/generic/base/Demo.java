package com.generic.base;

public class Demo implements Graphics3D.Renderer {

   private final Platform platform;
   
   public Demo(Platform platform) {
      this.platform = platform;
      platform.root3D().setRenderer(this);
   }

   public void render(Graphics3D gl) {
      Data.Array positions = Data.Array.create(Data.Array.Type.FOUR_FLOATS);
      { positions.setNumElements(3);
        floats[] positions_array = ((Data.Array.Floats) positions).array();
        
      }
      
   }
   
}

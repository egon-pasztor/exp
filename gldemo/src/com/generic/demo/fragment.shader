#version 330

uniform sampler2D mainTexture;
uniform int highlight;

in vec3 fragColor;
in vec4 fragTexCoords;
in vec3 fragBaryCoords;
out vec4 outColor;
 
float distance(float sx, float sy,
               float ex, float ey,
               float px, float py) {
   
  float dx=ex-sx;
  float dy=ey-sy;
  float dd=sqrt(dx*dx+dy*dy);
  
  float nx=-dy/dd;
  float ny= dx/dd;
  
  float vx=px-sx;
  float vy=py-sy;
 
  float dot = nx*vx+ny*vy;
  float wx=nx*dot; 
  float wy=ny*dot; 
  
  return sqrt(wx*wx+wy*wy);
}
float edgeDistance(float x, float y) {

    float p0x = 0;
    float p0y = 0;
    
    float p1x = 1;
    float p1y = 0;
    
    float p2x = 0.5;
    float p2y = 0.8660254;
    
    float d01 = distance(p0x,p0y, p1x,p1y, x,y);
    float d12 = distance(p1x,p1y, p2x,p2y, x,y);
    float d20 = distance(p2x,p2y, p0x,p0y, x,y);
    
    float lowestD = d01;
    if (d12 < lowestD) lowestD = d12;
    if (d20 < lowestD) lowestD = d20;
    
    return lowestD;
}
 
void main()
{
    float bary0 = fragBaryCoords.x;
    float bary1 = fragBaryCoords.y;
    float bary2 = fragBaryCoords.z;
	  
	float thresh = .02f;
    if ((bary0 < thresh) || (bary1 < thresh) || (bary2 < thresh)) {
      if (highlight != 0) {
	    outColor.r = 0.8f;
	    outColor.g = 0.2f;
	    outColor.b = 0.2f;
	  } else {
	    outColor.r = 0.2f;
	    outColor.g = 0.8f;
	    outColor.b = 0.2f;
	  }
      return;
    }
    
    vec2 lk;
    lk.x = fragTexCoords.x / fragTexCoords.w;
    lk.y = fragTexCoords.y / fragTexCoords.w;
    
    outColor = texture2D(mainTexture, lk);
}
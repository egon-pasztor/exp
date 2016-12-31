#version 330

uniform bool highlight;
uniform int windowWidth;
uniform int windowHeight;

in vec4 gl_FragCoord;

in vec4 fragmentV0Pos;
in vec4 fragmentV1Pos;
in vec4 fragmentV2Pos;
in vec4 fragmentVPos;


in vec3 fragColor;
in vec2 fragBaryCoords;
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

float edgeDistance2() {
    float u  = fragmentVPos.x/fragmentVPos.w;
    float v  = fragmentVPos.y/fragmentVPos.w;
    u = windowWidth *(u+1.0)/2.0;
    v = windowHeight*(v+1.0)/2.0;
    
    float up = gl_FragCoord.x;
    float vp = gl_FragCoord.y;    
    
    float dif;
    dif = (u - up);
    if (dif < 0) dif = -dif;
    if (dif > 0.005) return 0.0;
    
    dif = (v - vp);
    if (dif < 0) dif = -dif;
    if (dif > 0.005) return 0.0;
    
    
    float u0 = fragmentV0Pos.x/fragmentV0Pos.w;
    float v0 = fragmentV0Pos.y/fragmentV0Pos.w;
    u0 = windowWidth *(u0+1.0)/2.0;
    v0 = windowHeight*(v0+1.0)/2.0;

    float u1 = fragmentV1Pos.x/fragmentV1Pos.w;
    float v1 = fragmentV1Pos.y/fragmentV1Pos.w;
    u1 = windowWidth *(u1+1.0)/2.0;
    v1 = windowHeight*(v1+1.0)/2.0;

    float u2 = fragmentV2Pos.x/fragmentV2Pos.w;
    float v2 = fragmentV2Pos.y/fragmentV2Pos.w;
    u2 = windowWidth *(u2+1.0)/2.0;
    v2 = windowHeight*(v2+1.0)/2.0;


    float d01 = distance(u0,v0, u1,v1, u,v);
    float d12 = distance(u1,v1, u2,v2, u,v);
    float d20 = distance(u2,v2, u0,v0, u,v);
    
    float lowestD = d01;
    if (d12 < lowestD) lowestD = d12;
    if (d20 < lowestD) lowestD = d20;
    
    return lowestD;
}


void main()
{
/*
    float u = fragmentVPos.x/fragmentVPos.w;
    float v = fragmentVPos.y/fragmentVPos.w;

    int px = int(windowWidth*(u+1.0)/2.0);
    int py = int(windowHeight*(v+1.0)/2.0);
    if (  ( (px/4)%2 ) == 
          ( (py/4)%2 )    ) {
         outColor.r = 0.8f;
         outColor.g = 0.4f;
         outColor.b = 0.2f;
         return;
    }
*/
/*
    int px = int(gl_FragCoord.x);
    int py = int(gl_FragCoord.y);
    if (  ( (px/4)%2 ) == 
          ( (py/4)%2 )    ) {
         outColor.r = 0.8f;
         outColor.g = 0.4f;
         outColor.b = 0.2f;
         return;
    }
    
    float x = fragBaryCoords.x;
    float y = fragBaryCoords.y;
    bool h = (edgeDistance(x,y) < 0.01);
*/
    float e2 = edgeDistance2();
    bool h = (e2 <= 1.0);

    if (h) {
         outColor.r = 0.0;
         outColor.g = 0.0;
         outColor.b = 0.0;
    } else {
       if (highlight) {
         outColor.r = 0.9;
         outColor.g = 0.1;
         outColor.b = 0.1;
       } else {
         outColor.r = fragColor.r;
         outColor.g = fragColor.g;
         outColor.b = fragColor.b;
       }
       if (e2 < 20) {
         float f = (e2-1.0)/19.0;
         outColor.r = (0.7)*(1-f) + outColor.r*f;
         outColor.g = (0.1)*(1-f) + outColor.g*f;
         outColor.b = (0.3)*(1-f) + outColor.b*f;
       }
    }   
}
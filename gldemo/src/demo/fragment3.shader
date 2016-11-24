#version 330

uniform bool highlight;

in vec3 fragColor;
in vec2 fragBaryCoords;
out vec4 outColor;

 vec3 mod289(vec3 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
     return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
  return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v)
  { 
  const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
  const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

// First corner
  vec3 i  = floor(v + dot(v, C.yyy) );
  vec3 x0 =   v - i + dot(i, C.xxx) ;

// Other corners
  vec3 g = step(x0.yzx, x0.xyz);
  vec3 l = 1.0 - g;
  vec3 i1 = min( g.xyz, l.zxy );
  vec3 i2 = max( g.xyz, l.zxy );

  //   x0 = x0 - 0.0 + 0.0 * C.xxx;
  //   x1 = x0 - i1  + 1.0 * C.xxx;
  //   x2 = x0 - i2  + 2.0 * C.xxx;
  //   x3 = x0 - 1.0 + 3.0 * C.xxx;
  vec3 x1 = x0 - i1 + C.xxx;
  vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
  vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

// Permutations
  i = mod289(i); 
  vec4 p = permute( permute( permute( 
             i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
           + i.y + vec4(0.0, i1.y, i2.y, 1.0 )) 
           + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

// Gradients: 7x7 points over a square, mapped onto an octahedron.
// The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
  float n_ = 0.142857142857; // 1.0/7.0
  vec3  ns = n_ * D.wyz - D.xzx;

  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

  vec4 x_ = floor(j * ns.z);
  vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

  vec4 x = x_ *ns.x + ns.yyyy;
  vec4 y = y_ *ns.x + ns.yyyy;
  vec4 h = 1.0 - abs(x) - abs(y);

  vec4 b0 = vec4( x.xy, y.xy );
  vec4 b1 = vec4( x.zw, y.zw );

  //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
  //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
  vec4 s0 = floor(b0)*2.0 + 1.0;
  vec4 s1 = floor(b1)*2.0 + 1.0;
  vec4 sh = -step(h, vec4(0.0));

  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

  vec3 p0 = vec3(a0.xy,h.x);
  vec3 p1 = vec3(a0.zw,h.y);
  vec3 p2 = vec3(a1.xy,h.z);
  vec3 p3 = vec3(a1.zw,h.w);

//Normalise gradients
  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
  p0 *= norm.x;
  p1 *= norm.y;
  p2 *= norm.z;
  p3 *= norm.w;

// Mix final noise value
  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
  m = m * m;
  return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), 
                                dot(p2,x2), dot(p3,x3) ) );
  }
  
  
  
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
    float x = fragBaryCoords.x;
    float y = fragBaryCoords.y;
    
    float p0x = 0;
    float p0y = 0;
    float p1x = 1;
    float p1y = 0;
    float p2x = 0.5;
    float p2y = 0.8660254;
    
    bool h = (edgeDistance(x,y) < 0.005);
   
    float d0 = (x-p0x)*(x-p0x) + (y-p0y)*(y-p0y);
    float d1 = (x-p1x)*(x-p1x) + (y-p1y)*(y-p1y);
    float d2 = (x-p2x)*(x-p2x) + (y-p2y)*(y-p2y);
    
    float colval = (1+snoise(
      vec3(1.5*fragColor)))/2;
    
    if (h) {
         outColor.r = 0.2;
         outColor.g = 0.2;
         outColor.b = 0.2;
    } else {
         
         if (colval < 0.25) {
           outColor.r = 1.0;
           outColor.g = 1.0;
           outColor.b = 0.0;
         } else if (colval < 0.5) {
           outColor.r = 0.8;
           outColor.g = 0.2;
           outColor.b = 0.2;
         } else if (colval < 0.75) {
           outColor.r = 0.0;
           outColor.g = 1.0;
           outColor.b = 0.0;
         } else if (colval < 1.0) {
           outColor.r = 0.0;
           outColor.g = 0.0;
           outColor.b = 1.0;
         } else {
           outColor.r = 1.0;
           outColor.g = 1.0;
           outColor.b = 0.0;
         }
    }   
}

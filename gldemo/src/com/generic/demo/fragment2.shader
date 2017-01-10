#version 330

uniform int highlight;
uniform int windowWidth;
uniform int windowHeight;

uniform vec2 uvPointer;

in vec4 gl_FragCoord;

in vec3 fragColor;
in vec3 fragBaryCoords;

in vec4 V0pos;
in vec4 V1pos;
in vec4 V2pos;

in vec2 V0uv;
in vec2 V1uv;
in vec2 V2uv;

out vec4 outColor;


// ------------------------
// Basic GEOMETRY Primitives
// ------------------------

float min(float a, float b) {
    if (a < b) return a;
    return b;
}
float absdiff(float a, float b) {
    float diff = a - b;
    if (diff < 0.0) diff = -diff;
	return diff;
}
bool isMultipleOf (float a, float b, float thresh) {
    float nearest = b * int(a/b);
    float diff = a - nearest;
    return (absdiff(a,nearest) < thresh);
}

float nearestMultipleOf (float a, float factor) {
    float candidate0  = factor * int(a/factor);
    float candidate1 = candidate0 - factor;
    float candidate2 = candidate0 + factor;
    
    float score0 = absdiff(a, candidate0);
    float score1 = absdiff(a, candidate1);
    float score2 = absdiff(a, candidate2);
    
    return ((score0 <= score1) && (score0 <= score2)) ? candidate0
        : ( ((score1 <= score2) && (score1 <= score0)) ? candidate1
                                                       : candidate2);
}
float geometricDistance(float sx, float sy,
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


// ------------------------
// Pixel information
// ------------------------

float myXPixel;
float myYPixel;
float myUValue;
float myVValue;

float A,B,C,D,E,F,G,H,I;

float pixelsPerDU;
float pixelsPerDV;

void computeInfo() {

    // this is our location in actual window pixel coordinates
    myXPixel = gl_FragCoord.x;
    myYPixel = gl_FragCoord.y;
    
    float u0 = V0uv.x, v0 = V0uv.y;
    float u1 = V1uv.x, v1 = V1uv.y;
    float u2 = V2uv.x, v2 = V2uv.y;
    
    // we can compute our <u,v> position using fragBaryCoords
    myUValue = u0 * fragBaryCoords.x + u1 * fragBaryCoords.y + u2 * fragBaryCoords.z;
    myVValue = v0 * fragBaryCoords.x + v1 * fragBaryCoords.y + v2 * fragBaryCoords.z;
    
    // we can compute the matrix that produces barycentric coordinates for an arbitrary <u,v> point
    float den = u0 * (v1-v2) + u1 * (v2-v0) + u2 * (v0-v1);
    A = (v1-v2)/den;  B = (u2-u1)/den;  C = (u1*v2-u2*v1)/den;
    D = (v2-v0)/den;  E = (u0-u2)/den;  F = (u2*v0-u0*v2)/den;
    G = (v0-v1)/den;  H = (u1-u0)/den;  I = (u0*v1-u1*v0)/den;
    
    float x0 = V0pos.x;
    float y0 = V0pos.y;
    float w0 = V0pos.w;
   
    float x1 = V1pos.x;
    float y1 = V1pos.y;
    float w1 = V1pos.w;
    
    float x2 = V2pos.x;
    float y2 = V2pos.y;
    float w2 = V2pos.w;
    
    float Xh = fragBaryCoords.x * x0 + fragBaryCoords.y * x1 +  fragBaryCoords.z * x2;
    float Yh = fragBaryCoords.x * y0 + fragBaryCoords.y * y1 +  fragBaryCoords.z * y2;
    float W  = fragBaryCoords.x * w0 + fragBaryCoords.y * w1 +  fragBaryCoords.z * w2;
    // (note (Xh/W) will equal myXPixel and (Yh/W) will equal myYPixel)
    
    float dxdu = (windowWidth/2.0) *  ((1.0/W) * (A*x0 + D*x1 + G*x2) - (Xh/(W*W)) * (A*w0 + D*w1 + G*w2));
    float dxdv = (windowWidth/2.0) *  ((1.0/W) * (B*x0 + E*x1 + H*x2) - (Xh/(W*W)) * (B*w0 + E*w1 + H*w2));
    float dydu = (windowHeight/2.0) * ((1.0/W) * (A*y0 + D*y1 + G*y2) - (Yh/(W*W)) * (A*w0 + D*w1 + G*w2));
    float dydv = (windowHeight/2.0) * ((1.0/W) * (B*y0 + E*y1 + H*y2) - (Yh/(W*W)) * (B*w0 + E*w1 + H*w2));
    
    pixelsPerDU = sqrt(dxdu*dxdu + dydu*dydu);
    pixelsPerDV = sqrt(dxdv*dxdv + dydv*dydv);
}

vec2 pixelAtUV(vec2 uv) {

    // compute barycentric coords for arbitrary <u,v> point
    float lambda0 = A*uv.x + B*uv.y + C;
    float lambda1 = D*uv.x + E*uv.y + F;
    float lambda2 = G*uv.x + H*uv.y + I;
    
    // use barycentric coordinates to interpolate from corner's X,Y, and W values
    float point0Xh = lambda0 * V0pos.x + lambda1 * V1pos.x +  lambda2 * V2pos.x;
    float point0Yh = lambda0 * V0pos.y + lambda1 * V1pos.y +  lambda2 * V2pos.y;
    float point0W  = lambda0 * V0pos.w + lambda1 * V1pos.w +  lambda2 * V2pos.w;
    
    // Divide by W and scale to window-size to get the pixel location of <u,v>
    float point0X  = windowWidth  * ((point0Xh/point0W) +1.0)/2.0;
    float point0Y  = windowHeight * ((point0Yh/point0W) +1.0)/2.0;
    
    return vec2(point0X, point0Y);
}

float pixelDistanceToULine(float uCritical) {
    vec2 pixelStart = pixelAtUV(vec2(uCritical, -1.0f));
    vec2 pixelEnd   = pixelAtUV(vec2(uCritical,  1.0f));
    return geometricDistance(pixelStart.x,pixelStart.y, pixelEnd.x,pixelEnd.y, myXPixel,myYPixel);
}
float pixelDistanceToVLine(float vCritical) {
    vec2 pixelStart = pixelAtUV(vec2(-1.0f, vCritical));
    vec2 pixelEnd   = pixelAtUV(vec2( 1.0f, vCritical));
    return geometricDistance(pixelStart.x,pixelStart.y, pixelEnd.x,pixelEnd.y, myXPixel,myYPixel);
}



float pixelDistanceToEdge() {

    // get the WINDOW-PIXEL positions of the three vertices
    float x0Pixel = windowWidth  *((V0pos.x/V0pos.w) +1.0)/2.0;
    float y0Pixel = windowHeight *((V0pos.y/V0pos.w) +1.0)/2.0;

    float x1Pixel = windowWidth  *((V1pos.x/V1pos.w) +1.0)/2.0;
    float y1Pixel = windowHeight *((V1pos.y/V1pos.w) +1.0)/2.0;

    float x2Pixel = windowWidth  *((V2pos.x/V2pos.w) +1.0)/2.0;
    float y2Pixel = windowHeight *((V2pos.y/V2pos.w) +1.0)/2.0;
    
	// we return the minimum pixel distance to any edge..
    float d01 = geometricDistance(x0Pixel,y0Pixel, x1Pixel,y1Pixel, myXPixel,myYPixel);
    float d12 = geometricDistance(x1Pixel,y1Pixel, x2Pixel,y2Pixel, myXPixel,myYPixel);
    float d20 = geometricDistance(x2Pixel,y2Pixel, x0Pixel,y0Pixel, myXPixel,myYPixel);
    return min(d01, min(d12, d20));
}


// ------------------------

float scaleToShadingLevel(float scale) {
   // scale == "how many times larger than the smallest-line-difference is this line?"
   // TODO: optimize this?
   if (scale < 3.0f) {
      return ((scale - 1.0f) / 2.0f) * 0.2f;
   }
   if (scale < 9.0f) {
      return 0.2f + ((scale - 3.0f) / (9.0f - 3.0f)) * 0.2f;
   }
   if (scale < 27.0f) {
      return 0.4f + ((scale - 9.0f) / (27.0f - 9.0f)) * 0.2f;
   }
   if (scale < 81.0f) {
      return 0.6f + ((scale - 27.0f) / (81.0f - 27.0f)) * 0.2f;
   }
}

float makeShadingLevel(bool useU, float amt) {
    float criticalPixelDifference = 5.0f;
    float pixelDistanceFromHereToLine = useU ? pixelDistanceToULine(nearestMultipleOf(myUValue, amt))
                                       : pixelDistanceToVLine(nearestMultipleOf(myVValue, amt));
    float pixelDistanceFromLineToLine = useU ? (pixelsPerDU * amt) : (pixelsPerDV * amt);
    if (pixelDistanceFromLineToLine < criticalPixelDifference) return 0.0f;
    if (pixelDistanceFromHereToLine < 1.0) {
       return scaleToShadingLevel(pixelDistanceFromLineToLine / criticalPixelDifference);
    }
    if ((pixelDistanceFromHereToLine < 2.0) &&
        (pixelDistanceFromLineToLine > (27.0f * criticalPixelDifference))) {
       return scaleToShadingLevel(pixelDistanceFromLineToLine / (27.0f * criticalPixelDifference));
    }
    return -1.0f;
}

float getGridShadingLevel(bool useU) {
    
    /* too many registers?
    float maxMultiple = 1.00f;
    bool multipleOf10 = true;
    
    while (maxMultiple > 0.000001) {
    
      float amt = maxMultiple;
      float criticalPixelDifference = 5.0f;
      float pixelDistanceFromHereToLine = useU ? pixelDistanceToULine(nearestMultipleOf(myUValue, amt))
                                               : pixelDistanceToVLine(nearestMultipleOf(myVValue, amt));
      float pixelDistanceFromLineToLine = useU ? (pixelsPerDU * amt) : (pixelsPerDV * amt);
      if (pixelDistanceFromLineToLine < criticalPixelDifference) return 0.0f;
      
      if (pixelDistanceFromHereToLine < 1.0) {
         return scaleToShadingLevel(pixelDistanceFromLineToLine / criticalPixelDifference);
      }    
      if ((pixelDistanceFromHereToLine < 2.0) &&
          (pixelDistanceFromLineToLine > (27.0f * criticalPixelDifference))) {
         return scaleToShadingLevel(pixelDistanceFromLineToLine / (27.0f * criticalPixelDifference));
      }
      
      maxMultiple *= (multipleOf10 ? 0.5 : 0.2);
      multipleOf10 = !multipleOf10;
    }
    */
    
    float shadingLevel;
    shadingLevel = makeShadingLevel(useU, 1.000f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.500f);
    if (shadingLevel >= 0.0) return shadingLevel;

    shadingLevel = makeShadingLevel(useU, 0.100f);
    if (shadingLevel >= 0.0) return shadingLevel;

    shadingLevel = makeShadingLevel(useU, 0.050f);
    if (shadingLevel >= 0.0) return shadingLevel;

    shadingLevel = makeShadingLevel(useU, 0.010f);
    if (shadingLevel >= 0.0) return shadingLevel;

    shadingLevel = makeShadingLevel(useU, 0.005f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.001f);
    if (shadingLevel >= 0.0) return shadingLevel;

    return 0.0f;
}

float getGridShadingLevel() {
    float a = getGridShadingLevel(true);
    float b = getGridShadingLevel(false);
    return (a > b) ? a : b;
}

void main()
{
   computeInfo();
   
   float pde = pixelDistanceToEdge();
   if (pde < 2.0f) {
     if ((pde < 1.0f) && (highlight != 0)) {
         outColor.r = 0.9;
         outColor.g = 0.1;
         outColor.b = 0.1;
     } else {
         outColor.r = 0.1;
         outColor.g = 0.1;
         outColor.b = 0.1;
     }
     return;
   } 

   float pixelDistanceToSelectedULine = pixelDistanceToULine(uvPointer.x);
   float pixelDistanceToSelectedVLine = pixelDistanceToVLine(uvPointer.y);
   
   if ((pixelDistanceToSelectedULine < 3.0f) && (pixelDistanceToSelectedVLine < 3.0f)) {
     outColor.r = 1.0f;
     outColor.g = 1.0f;
     outColor.b = 0.0f;         
     return;
   } 
   if (((pixelDistanceToSelectedULine < 4.0f) && ((pixelDistanceToSelectedVLine >= 3.0f) && (pixelDistanceToSelectedVLine < 4.0f))) ||
      ((pixelDistanceToSelectedVLine < 4.0f) && ((pixelDistanceToSelectedULine >= 3.0f) && (pixelDistanceToSelectedULine < 4.0f)))) {
      
     outColor.r = 0.0f;
     outColor.g = 0.0f;
     outColor.b = 0.0f;         
     return;
   }
   if ((pixelDistanceToSelectedULine < 1.0f) || (pixelDistanceToSelectedVLine < 1.0f)) {
     outColor.r = 1.0f;
     outColor.g = 0.0f;
     outColor.b = 0.0f;         
     return;
   } 
   
   
   float frac = getGridShadingLevel();
   outColor.r = fragColor.r * (1.0f-frac);
   outColor.g = fragColor.g * (1.0f-frac);
   outColor.b = fragColor.b * (1.0f-frac);
}


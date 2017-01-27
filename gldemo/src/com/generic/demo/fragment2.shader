#version 330

uniform int highlight;
uniform int windowWidth;
uniform int windowHeight;

uniform vec2 uvPointer;

in vec4 gl_FragCoord;

in vec3 fragColor;
in vec3 fragBaryCoords;

in vec3 triangleShape;
in vec3 direction1;
in vec3 direction2;

in vec4 V0pos;
in vec4 V1pos;
in vec4 V2pos;

in vec2 V0uv;
in vec2 V1uv;
in vec2 V2uv;

out vec4 outColor;


// ------------------------
// Pixel information
// ------------------------

vec2 myXYPixel;
vec2 myUVVal;

float A,B,C,D,E,F,G,H,I;

float pixelsPerDU;
float pixelsPerDV;

float dxdu,dxdv,dydu,dydv;

vec3 edge01;
vec3 edge02;
vec3 normal;
float tnz;

void computeInfo() {

    // this is our location in actual window pixel coordinates
    myXYPixel = vec2(gl_FragCoord.x, gl_FragCoord.y);
    
    float u0 = V0uv.x, v0 = V0uv.y;
    float u1 = V1uv.x, v1 = V1uv.y;
    float u2 = V2uv.x, v2 = V2uv.y;
    
    // we can compute our <u,v> position using fragBaryCoords
    myUVVal = vec2(u0 * fragBaryCoords.x + u1 * fragBaryCoords.y + u2 * fragBaryCoords.z, 
                   v0 * fragBaryCoords.x + v1 * fragBaryCoords.y + v2 * fragBaryCoords.z);
    
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
    
     dxdu = (windowWidth/2.0) *  ((1.0/W) * (A*x0 + D*x1 + G*x2) - (Xh/(W*W)) * (A*w0 + D*w1 + G*w2));
     dxdv = (windowWidth/2.0) *  ((1.0/W) * (B*x0 + E*x1 + H*x2) - (Xh/(W*W)) * (B*w0 + E*w1 + H*w2));
     dydu = (windowHeight/2.0) * ((1.0/W) * (A*y0 + D*y1 + G*y2) - (Yh/(W*W)) * (A*w0 + D*w1 + G*w2));
     dydv = (windowHeight/2.0) * ((1.0/W) * (B*y0 + E*y1 + H*y2) - (Yh/(W*W)) * (B*w0 + E*w1 + H*w2));
    
    pixelsPerDU = sqrt(dxdu*dxdu + dydu*dydu);
    pixelsPerDV = sqrt(dxdv*dxdv + dydv*dydv);
    
    edge01 = vec3(V1pos.x-V0pos.x, V1pos.y-V0pos.y, V1pos.z-V0pos.z);
    edge02 = vec3(V2pos.x-V0pos.x, V2pos.y-V0pos.y, V2pos.z-V0pos.z);
    normal = normalize(cross(edge01, edge02));
    
    tnz = normal.z;
    if (tnz<0.0f) tnz = -tnz;
    if (tnz>1.0f) tnz = 1.0f;
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

float geometricDistance(vec2 s, vec2 e, vec2 p) {

   vec2 d = e-s;
   float dd = length(d);
   vec2 n = vec2(-d.y/dd, d.x/dd);
   vec2 v = p-s;
   return abs(n.x*v.x+n.y*v.y);
}

float pixelDistanceToULine(float uCritical) {
    vec2 pixelStart = pixelAtUV(vec2(uCritical, -1.0f));
    vec2 pixelEnd   = pixelAtUV(vec2(uCritical,  1.0f));
    return geometricDistance(pixelStart, pixelEnd, myXYPixel);
}
float pixelDistanceToVLine(float vCritical) {
    vec2 pixelStart = pixelAtUV(vec2(-1.0f, vCritical));
    vec2 pixelEnd   = pixelAtUV(vec2( 1.0f, vCritical));
    return geometricDistance(pixelStart, pixelEnd, myXYPixel);
}
float pixelDistanceToEdge() {

    // get the WINDOW-PIXEL positions of the three vertices
    vec2 pixel0 = vec2(windowWidth  *((V0pos.x/V0pos.w) +1.0)/2.0, windowHeight *((V0pos.y/V0pos.w) +1.0)/2.0);
    vec2 pixel1 = vec2(windowWidth  *((V1pos.x/V1pos.w) +1.0)/2.0, windowHeight *((V1pos.y/V1pos.w) +1.0)/2.0);
    vec2 pixel2 = vec2(windowWidth  *((V2pos.x/V2pos.w) +1.0)/2.0, windowHeight *((V2pos.y/V2pos.w) +1.0)/2.0);
    
	// we return the minimum pixel distance to any edge..
    float d01 = geometricDistance(pixel0, pixel1, myXYPixel);
    float d12 = geometricDistance(pixel1, pixel2, myXYPixel);
    float d20 = geometricDistance(pixel2, pixel0, myXYPixel);
    return min(d01, min(d12, d20));
}


// ------------------------
// GRID-SHADING Logic
// ------------------------

float min(float a, float b) {
    if (a < b) return a;
    return b;
}
float nearestMultipleOf (float a, float factor) {
    float candidate0  = factor * int(a/factor);
    float candidate1 = candidate0 - factor;
    float candidate2 = candidate0 + factor;
    
    float score0 = abs(a-candidate0);
    float score1 = abs(a-candidate1);
    float score2 = abs(a-candidate2);
    
    return ((score0 <= score1) && (score0 <= score2)) ? candidate0
        : ( ((score1 <= score2) && (score1 <= score0)) ? candidate1
                                                       : candidate2);
}

float scaleToShadingLevel(float scale) {
   // scale == "how many times larger than the smallest-line-difference is this line?"

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
   return 0.8f;
}

float makeShadingLevel(bool useU, float amt) {
    float criticalPixelDifference = 6.0f;
    float pixelDistanceFromHereToLine = useU ? pixelDistanceToULine(nearestMultipleOf(myUVVal.x, amt))
                                             : pixelDistanceToVLine(nearestMultipleOf(myUVVal.y, amt));
                                             
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


// ------------------------
// Selected Face?
// ------------------------

bool inSelectedFace() {
    // compute barycentric coords for arbitrary <u,v> point
    float lambda0 = A*uvPointer.x + B*uvPointer.y + C;
    float lambda1 = D*uvPointer.x + E*uvPointer.y + F;
    float lambda2 = G*uvPointer.x + H*uvPointer.y + I;
    return (lambda0 >= 0) && (lambda1 >= 0) && (lambda2 >= 0);
}


// --------------------------
// DirectionField rendering
// --------------------------

float Value2D( vec2 P )
{
    //  https://github.com/BrianSharpe/Wombat/blob/master/Value2D.glsl

    //	establish our grid cell and unit position
    vec2 Pi = floor(P);
    vec2 Pf = P - Pi;

    //	calculate the hash.
    vec4 Pt = vec4( Pi.xy, Pi.xy + 1.0 );
    Pt = Pt - floor(Pt * ( 1.0 / 71.0 )) * 71.0;
    Pt += vec2( 26.0, 161.0 ).xyxy;
    Pt *= Pt;
    Pt = Pt.xzxz * Pt.yyww;
    vec4 hash = fract( Pt * ( 1.0 / 951.135664 ) );

    //	blend the results and return
    //vec2 blend = Pf * Pf * Pf * (Pf * (Pf * 6.0 - 15.0) + 10.0);
    //vec4 blend2 = vec4( blend, vec2( 1.0 - blend ) );
    //return dot( hash, blend2.zxzx * blend2.wwyy );
    
    return hash.x;
}

// okay so.... cpu is going to send us an XYZ vector.
// we're probably ignoring W (??!)

vec2 directionalShading() {
    
    int uvScale = 30;
    float thresh = 100.0f;
    int n = 20;  
   
    float u0 = 0.0f,                      v0 = 0.0f;
    float u1 = uvScale * triangleShape.x, v1 = 0.0f;
    float u2 = uvScale * triangleShape.y, v2 = uvScale * triangleShape.z;
          
    // we can compute our <u,v> position using fragBaryCoords
    vec2 pmyUVVal   = vec2(u0 * fragBaryCoords.x + u1 * fragBaryCoords.y + u2 * fragBaryCoords.z, 
                           v0 * fragBaryCoords.x + v1 * fragBaryCoords.y + v2 * fragBaryCoords.z);
                            
    // we can compute the matrix that produces barycentric coordinates for an arbitrary <u,v> point
    float den = u0 * (v1-v2) + u1 * (v2-v0) + u2 * (v0-v1);
    float pA = (v1-v2)/den;  float pB = (u2-u1)/den;  float pC = (u1*v2-u2*v1)/den;
    float pD = (v2-v0)/den;  float pE = (u0-u2)/den;  float pF = (u2*v0-u0*v2)/den;
    float pG = (v0-v1)/den;  float pH = (u1-u0)/den;  float pI = (u0*v1-u1*v0)/den;
    
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
    
    //if (abs((windowWidth*((Xh/W)+1.0)/2.0) - myXYPixel.x)  > 0.5f) return 1.0f;
    //if (abs((windowHeight*((Yh/W)+1.0)/2.0) - myXYPixel.y) > 0.5f) return 1.0f;
    /*
    float pdxdu = (windowWidth/2.0) *  ((1.0/W) * (pA*x0 + pD*x1 + pG*x2) - (Xh/(W*W)) * (pA*w0 + pD*w1 + pG*w2));
    float pdxdv = (windowWidth/2.0) *  ((1.0/W) * (pB*x0 + pE*x1 + pH*x2) - (Xh/(W*W)) * (pB*w0 + pE*w1 + pH*w2));
    float pdydu = (windowHeight/2.0) * ((1.0/W) * (pA*y0 + pD*y1 + pG*y2) - (Yh/(W*W)) * (pA*w0 + pD*w1 + pG*w2));
    float pdydv = (windowHeight/2.0) * ((1.0/W) * (pB*y0 + pE*y1 + pH*y2) - (Yh/(W*W)) * (pB*w0 + pE*w1 + pH*w2));
    */
    float drXhDu = (pA*x0 + pD*x1 + pG*x2);
    float drYhDu = (pA*y0 + pD*y1 + pG*y2);
    float drWDu  = (pA*w0 + pD*w1 + pG*w2);
    
    float drXhDv = (pB*x0 + pE*x1 + pH*x2);
    float drYhDv = (pB*y0 + pE*y1 + pH*y2);
    float drWDv  = (pB*w0 + pE*w1 + pH*w2);
    
    float pdxdu = (windowWidth/2.0) *  ((1.0/W) * drXhDu - (Xh/(W*W)) * drWDu);
    float pdxdv = (windowWidth/2.0) *  ((1.0/W) * drXhDv - (Xh/(W*W)) * drWDv);
    float pdydu = (windowHeight/2.0) * ((1.0/W) * drYhDu - (Yh/(W*W)) * drWDu);
    float pdydv = (windowHeight/2.0) * ((1.0/W) * drYhDv - (Yh/(W*W)) * drWDv);
    
    // =============================
    // okay...
    // matrix [ dxdu dxdv ] x [ du ] == [ dx ]
    //        [ dydu dydv ]   [ dv ]    [ dy ]
    // =============================
    
    float det = pdxdu * pdydv - pdydu * pdxdv;
    float pdudx =  pdydv / det;
    float pdudy = -pdxdv / det;
    float pdvdx = -pdydu / det;
    float pdvdy =  pdxdu / det;
    
    float pixelsPerDU = sqrt(pdxdu*pdxdu + pdydu*pdydu); 
    float pixelsPerDV = sqrt(pdxdv*pdxdv + pdydv*pdydv); 
    
   
   // ================================
   
   float dirX1 = (1.0 / W) * direction1.x  - (Xh / (W*W)) * direction1.z;
   float dirY1 = (1.0 / W) * direction1.y  - (Yh / (W*W)) * direction1.z;
   vec2 dir1 = normalize(vec2(windowWidth*dirX1,windowHeight*dirY1));
   
   float dirX2 = (1.0 / W) * direction2.x  - (Xh / (W*W)) * direction2.z;
   float dirY2 = (1.0 / W) * direction2.y  - (Yh / (W*W)) * direction2.z;
   vec2 dir2 = normalize(vec2(windowWidth*dirX2,windowHeight*dirY2));   
   
   
   float du2 = dir2.x * pdudx + dir2.y * pdudy;
   float dv2 = dir2.x * pdvdx + dir2.y * pdvdy;
   vec2 fd2 = normalize(vec2(du2,dv2));   
   
   float du1 = dir1.x * pdudx + dir1.y * pdudy;
   float dv1 = dir1.x * pdvdx + dir1.y * pdvdy;
   vec2 fd1 = normalize(vec2(du1,dv1));

   
   // this should be the change in u,v that moves us one 
   //
   // the streaks will be approx 1 u in width, 
   //
   float blend1 = 0.0f;
   float blend2 = 0.0f;
   float blend3 = 0.0f;
   float scale = pixelsPerDU*pixelsPerDV * (uvScale * uvScale);
   float tc = thresh*thresh;
   if (scale > tc) {
     if (scale < 4*tc) {
       blend1 = (scale-tc)/(3*tc);
     } else {
       blend1 = 1.0f;
       if (scale > 9*tc) {
         if (scale < 36*tc) {
           blend2 = (scale-9*tc)/(27*tc);
         } else {
           blend2 = 1.0f;
           if (scale > 81*tc) {
             if (scale < 4*81*tc) {
                blend3 = (scale-81*tc)/(3*81*tc);
             } else {
                blend3 = 1.0f;
             }
           }
         }
       }
     }
   }
   //blend1 = 0.0f;
   //blend2 = 0.0f;
   //blend3 = 0.0f;

   int fn = 2*n;    

   float final1 = 0.0f;
   float final1y = 0.0f;
   for (int i = 0; i < n; ++i) {
      final1 += Value2D(pmyUVVal + fd1*i);
   }   
   for (int i = 0; i < n; ++i) {
      final1y += Value2D(pmyUVVal + fd2*i);
   }   
   float fin1 = final1/n;
   float fin1y = final1y/n;
   
   float final2 = 0.0f;
   float final2y = 0.0f;
   for (int i = 0; i < n; ++i) {
      final2 += Value2D(3 * pmyUVVal + fd1*i);
   }   
   for (int i = 0; i < n; ++i) {
      final2y += Value2D(3 * pmyUVVal + fd2*i);
   }   
   float fin2 = final2/n;
   float fin2y = final2y/n;
   
   float final3 = 0.0f;
   float final3y = 0.0f;
   for (int i = 0; i < n; ++i) {
      final3 += Value2D(9 * pmyUVVal + fd1*i);
   }   
   for (int i = 0; i < n; ++i) {
      final3y += Value2D(9 * pmyUVVal + fd2*i);
   }   
   float fin3 = final3/n;
   float fin3y = final3y/n;

   float final4 = 0.0f;
   float final4y = 0.0f;
   for (int i = 0; i < n; ++i) {
      final4 += Value2D(27 * pmyUVVal + fd1*i);
   }   
   for (int i = 0; i < n; ++i) {
      final4y += Value2D(27 * pmyUVVal + fd2*i);
   }   
   float fin4 = final4/n;
   float fin4y = final4y/n;
   
   float fa = (fin1 * (1.0 - blend1)) + fin2 * blend1;
   float fb = (fa   * (1.0 - blend2)) + fin3 * blend2;
   float fc = (fb   * (1.0 - blend3)) + fin4 * blend3;

   float fay = (fin1y * (1.0 - blend1)) + fin2y * blend1;
   float fby = (fay   * (1.0 - blend2)) + fin3y * blend2;
   float fcy = (fby   * (1.0 - blend3)) + fin4y * blend3;
   
   //float fin4 = sqrt(fn) * (((final4 + final4y)/(fn)) - 0.5f) + 0.5f;

   fc = (fc-0.5)*sqrt(n) + 0.5;
   fcy = (fcy-0.5)*sqrt(n) + 0.5;
   if (fc < 0) fc = 0;
   if (fc > 1) fc = 1;
   if (fcy < 0) fcy = 0;
   if (fcy > 1) fcy = 1;
   return vec2(fc,fcy);
   
   //float res = ((fc+fcy)/2.0 - 0.5)*sqrt(2*n) + 0.5;
   //if (res < 0) res = 0;
   //if (res > 1) res = 1;
   // return res;
}


// ------------------------
// MAIN
// ------------------------

void main()
{
   computeInfo();
   float pde = pixelDistanceToEdge();
   if (pde < 1.0f) {
     if ((pde < 0.5f) && (highlight != 0)) {
         outColor.r = 0.2;
         outColor.g = 0.1;
         outColor.b = 0.2;
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

   vec2 dd = directionalShading();
   
   float baseR = fragColor.r;
   float baseG = (fragColor.g * (1.0 - dd.x) + 0.3 * dd.x) * (1.0 - dd.y/2.0);
   float baseB = (fragColor.b * (1.0 - dd.y) + 0.0 * dd.y) * (1.0 - dd.x/3.0);
   
   float frac = getGridShadingLevel();
   frac = 1.0f - ((1.0f - frac) * tnz);
   outColor.r = baseR * (1.0f-frac);
   outColor.g = baseG * (1.0f-frac);
   outColor.b = baseB * (1.0f-frac);
   
   
//   if (inSelectedFace()) {
//      frac = 0.3;
//      outColor.r = outColor.r * (1.0f-frac) + 1.0f * frac;
//      outColor.g = outColor.g * (1.0f-frac) + 0.0f * frac;
//      outColor.b = outColor.b * (1.0f-frac) + 0.0f * frac;
//   }
}


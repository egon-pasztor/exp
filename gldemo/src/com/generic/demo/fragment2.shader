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
in mat4 matt4;

// ------------------------
// Pixel information
// ------------------------

float Xh,Yh,Zh,W;
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
    
    // Compute the homogenous coords of this pixel.  (we almost have that information in "myXYPixel",
    // (the fraction (Xh/W) will equal myXPixel and (Yh/W) will equal myYPixel), this gives us
    // the w component explicitly:    
    Xh = fragBaryCoords.x * V0pos.x + fragBaryCoords.y * V1pos.x +  fragBaryCoords.z * V2pos.x;
    Yh = fragBaryCoords.x * V0pos.y + fragBaryCoords.y * V1pos.y +  fragBaryCoords.z * V2pos.y;
    Zh = fragBaryCoords.x * V0pos.z + fragBaryCoords.y * V1pos.z +  fragBaryCoords.z * V2pos.z;
    W  = fragBaryCoords.x * V0pos.w + fragBaryCoords.y * V1pos.w +  fragBaryCoords.z * V2pos.w;
    
    // Compute the relationship between <x,y> (pixels in the window) and <u,v> (texture coords)
    dxdu = (windowWidth/2.0) *  ((1.0/W) * (A*V0pos.x + D*V1pos.x + G*V2pos.x) - (Xh/(W*W)) * (A*V0pos.w + D*V1pos.w + G*V2pos.w));
    dxdv = (windowWidth/2.0) *  ((1.0/W) * (B*V0pos.x + E*V1pos.x + H*V2pos.x) - (Xh/(W*W)) * (B*V0pos.w + E*V1pos.w + H*V2pos.w));
    dydu = (windowHeight/2.0) * ((1.0/W) * (A*V0pos.y + D*V1pos.y + G*V2pos.y) - (Yh/(W*W)) * (A*V0pos.w + D*V1pos.w + G*V2pos.w));
    dydv = (windowHeight/2.0) * ((1.0/W) * (B*V0pos.y + E*V1pos.y + H*V2pos.y) - (Yh/(W*W)) * (B*V0pos.w + E*V1pos.w + H*V2pos.w));
    
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
   //P = P * 2;
   int p = int(P.x);
   if (P.x<0) p =p - 1;
   float f = P.x - p;
   
   float pa = 1.0;
   if (p % 2 == 0) pa = 0.0;
   
   float pb = 1.0 - pa;
   float z =  f + pa * (1 - 2 * f);
   if (z<0.5) z = 0; else z = 1;
   return z;
}

/*
float Value2D (vec2 st) { 
    return fract(sin(dot(vec2(int(st.x), int(st.y)),
                         vec2(12.9898,78.233)))* 
        43758.5453123);
}
*/

float Value3D (vec3 st) { 
    return fract(sin(dot(vec3(int(st.x), int(st.y), int(st.z)),
                         vec3(12.9898,78.233, 34.342)))* 
        43758.5453123);
}


vec2 directionalShading() {
    int n = 10;  
/*
    float x = Xh/W;
    float y = Yh/W;
    float z = Zh/W;
    
    vec4 ph = vec4(Xh,Yh,Zh,W);
    vec4 d1h = vec4(direction1.x,direction1.y,direction1.z,0);
    vec4 d2h = vec4(direction2.x,direction2.y,direction2.z,0);
    
    ph = matt4 * ph;
    //d1h = matt4 * d1h;
    //d2h = matt4 * d2h;
    
    vec3 p = 100 * vec3(ph.x/ph.w, ph.y/ph.w, ph.z/ph.w);
     
    vec3 dir1 = normalize(vec3(d1h.x,d1h.y,d1h.z))*0.6;
    vec3 dir2 = normalize(vec3(d2h.x,d2h.y,d2h.z))*0.6;
     
    //vec3 p = 110*vec3(x,y,z);
     
   //float dirX1 = (1.0 / W) * direction1.x  - (Xh / (W*W)) * direction1.z;
   //float dirY1 = (1.0 / W) * direction1.y  - (Yh / (W*W)) * direction1.z;
   //float dirZ1 = (1.0 / W) * direction1.z  - (Zh / (W*W)) * direction1.z;
   
   //float dirX2 = (1.0 / W) * direction2.x  - (Xh / (W*W)) * direction2.z;
   //float dirY2 = (1.0 / W) * direction2.y  - (Yh / (W*W)) * direction2.z;
   //float dirZ2 = (1.0 / W) * direction2.z  - (Zh / (W*W)) * direction2.z;
   
   //vec3 dir1 = normalize(vec3(windowWidth*dirX1,windowHeight*dirY1,dirZ1))*0.6;
   //vec3 dir2 = normalize(vec3(windowWidth*dirX2,windowHeight*dirY2,dirZ2))*0.6;

   float f = 0;
   float fy = 0;
   int i = 0;
   
   //for (int i = 0; i < n; ++i) {
   //  f += Value3D(p+i*dir1);
   //  fy += Value3D(p+i*dir2);
  // }
        f += Value3D(p+0*dir1);
        f += Value3D(p+1*dir1);
        f += Value3D(p+2*dir1);
        f += Value3D(p+3*dir1);
        f += Value3D(p+4*dir1);
        f += Value3D(p+5*dir1);
        f += Value3D(p+6*dir1);
        f += Value3D(p+7*dir1);
        f += Value3D(p+8*dir1);
        f += Value3D(p+9*dir1);
        
     fy += Value3D(p+0*dir2);
     fy += Value3D(p+1*dir2);
     fy += Value3D(p+2*dir2);
     fy += Value3D(p+3*dir2);
     fy += Value3D(p+4*dir2);
     fy += Value3D(p+5*dir2);
     fy += Value3D(p+6*dir2);
     fy += Value3D(p+7*dir2);
     fy += Value3D(p+8*dir2);
     fy += Value3D(p+9*dir2);
             
   f = f/n;
   f = (f-0.5)*sqrt(n)+0.5;
   if (f < 0) f = 0;
   if (f > 1) f = 1;
   
   fy = fy/n;
   fy = (fy-0.5)*sqrt(n)+0.5;
   if (fy < 0) fy = 0;
   if (fy > 1) fy = 1;
   
   return vec2(f,fy);
*/

    int uvScale = 25;
    //float thresh = 100.0f;
    float thresh = 300.0f;
   
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
   
   
   // ------------------------------------
   
   float angle1 = atan(fd1.y,fd1.x);//angle1=0;
   float sinangle1 = sin(angle1);
   float cosangle1 = cos(angle1);
   
   vec2 r  = vec2(pmyUVVal.x * cosangle1 + pmyUVVal.y * sinangle1,
                - pmyUVVal.x * sinangle1 + pmyUVVal.y * cosangle1);
            
   float angle2 = atan(fd2.y,fd2.x);//angle2=0;
   float sinangle2 = sin(angle2);
   float cosangle2 = cos(angle2);
   
   vec2 r2 = vec2(pmyUVVal.x * cosangle2 + pmyUVVal.y * sinangle2,
                - pmyUVVal.x * sinangle2 + pmyUVVal.y * cosangle2);
   
   // ------------------------------------
   
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

 // ------------------------------------

   float fin1 = Value2D(r);
   float fin1y = Value2D(r2);
   
   float fin2 = Value2D(3*r);
   float fin2y = Value2D(3*r2);
   
   float fin3 = Value2D(9*r);
   float fin3y = Value2D(9*r2);

   float fin4 = Value2D(27*r);
   float fin4y = Value2D(27*r2);
 
 // ------------------------------------
 /*
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
*/

   float fa = (fin1 * (1.0 - blend1)) + fin2 * blend1;
   float fb = (fa   * (1.0 - blend2)) + fin3 * blend2;
   float fc = (fb   * (1.0 - blend3)) + fin4 * blend3;

   float fay = (fin1y * (1.0 - blend1)) + fin2y * blend1;
   float fby = (fay   * (1.0 - blend2)) + fin3y * blend2;
   float fcy = (fby   * (1.0 - blend3)) + fin4y * blend3;

   fc = (fc-0.5)*sqrt(n) + 0.5;
   fcy = (fcy-0.5)*sqrt(n) + 0.5;
   if (fc < 0) fc = 0;
   if (fc > 1) fc = 1;
   if (fcy < 0) fcy = 0;
   if (fcy > 1) fcy = 1;
   return vec2(fc,fcy);
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
   dd = dd/2;
   
   float avg = (dd.x + dd.y)/2;
   dd.x=avg; dd.y=avg;
   float baseR = fragColor.r;
   float baseG = (fragColor.g * (1.0 - dd.x) + 0.3 * dd.x) * (1.0 - dd.y/2.0);
   float baseB = (fragColor.b * (1.0 - dd.y) + 0.0 * dd.y) * (1.0 - dd.x/3.0);
   
   float frac = getGridShadingLevel();
   //float frac = 0;
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


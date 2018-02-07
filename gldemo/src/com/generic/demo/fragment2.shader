#version 330

uniform int highlight;
uniform int windowWidth;
uniform int windowHeight;

// Info about other UV structures like a test-circle drawn on the UV surface,
// would be communicated through "uniforms" like this one:
uniform vec2 uvPointer;
vec2 uvPointer2;

// Input varying values include:

uniform sampler2D meshInfo;  // okay.. this texture has rows x cols .. nRows == numTriangles, nCols == .. well, 9.  
                             // each ROW should hold the A,B,C,D,E,F,G,H,I values for that triangle...

in vec4 gl_FragCoord;

flat in float tnz;
in vec3 fragColor;
in vec3 fragBaryCoords;

flat in vec3 triangleShape;
flat in vec3 direction1;
flat in vec3 direction2;

flat in vec4 V0pos;
flat in vec4 V1pos;
flat in vec4 V2pos;

flat in uvec4 fragTriColorInfo;
flat in float triangleIndex;
flat in vec2 V0uv;
flat in vec2 V1uv;
flat in vec2 V2uv;
vec2 V0uv_;
vec2 V1uv_;
vec2 V2uv_;


out vec4 outColor;

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

bool done;

float A2,B2,C2,D2,E2,F2,G2,H2,I2;

float minU,maxU;
float minV,maxV;

int altTColor;



// ------------------------
// Basic Info code
// ------------------------

void computeInfo() {

    // this is our location in actual window pixel coordinates
    myXYPixel = vec2(gl_FragCoord.x, gl_FragCoord.y);
    
    float u0 = V0uv_.x, v0 = V0uv_.y;
    float u1 = V1uv_.x, v1 = V1uv_.y;
    float u2 = V2uv_.x, v2 = V2uv_.y;
    
    // we can compute the matrix that produces barycentric coordinates for an arbitrary <u,v> point
    float den = u0*v1 - u0*v2 + u1*v2 - u1*v0 + u2*v0 - u2*v1;
    A = (v1-v2)/den;  B = (u2-u1)/den;  C = (u1*v2-u2*v1)/(den);
    D = (v2-v0)/den;  E = (u0-u2)/den;  F = (u2*v0-u0*v2)/(den);
    G = (v0-v1)/den;  H = (u1-u0)/den;  I = (u0*v1-u1*v0)/(den);
    
    /*
    if (abs(den) > 0.000001f) {
       altTColor=7;
    } else {
       altTColor=3;
    }
    */
    
/*    //altTColor = triangleIndex - 8 * int(triangleIndex / 8);
    float r = texture(meshInfo, vec2(0,triangleIndex)).r;
    if (r > 0) {
       if (r > 1) {
          altTColor=4;
       } else {
          altTColor=5;
       }
    } else if (r < 0) {
       altTColor=3;
    } else if (r == 0) {
       altTColor=1;
    } else {
       altTColor=2;
    }
*/

    A = texture(meshInfo, vec2(0.5f/9.0f,triangleIndex)).r;
    B = texture(meshInfo, vec2(1.5f/9.0f,triangleIndex)).r;
    C = texture(meshInfo, vec2(2.5f/9.0f,triangleIndex)).r;
    D = texture(meshInfo, vec2(3.5f/9.0f,triangleIndex)).r;
    E = texture(meshInfo, vec2(4.5f/9.0f,triangleIndex)).r;
    F = texture(meshInfo, vec2(5.5f/9.0f,triangleIndex)).r;
    G = texture(meshInfo, vec2(6.5f/9.0f,triangleIndex)).r;
    H = texture(meshInfo, vec2(7.5f/9.0f,triangleIndex)).r;
    I = texture(meshInfo, vec2(8.5f/9.0f,triangleIndex)).r;

/*    
    A = length(texture(meshInfo, vec2(0,triangleIndex)));
    B = length(texture(meshInfo, vec2(1,triangleIndex)));
    C = length(texture(meshInfo, vec2(2,triangleIndex)));
    D = length(texture(meshInfo, vec2(3,triangleIndex)));
    E = length(texture(meshInfo, vec2(4,triangleIndex)));
    F = length(texture(meshInfo, vec2(5,triangleIndex)));
    G = length(texture(meshInfo, vec2(6,triangleIndex)));
    H = length(texture(meshInfo, vec2(7,triangleIndex)));
    I = length(texture(meshInfo, vec2(8,triangleIndex)));
*/ 
    
    // note that if u0/u2 are merely "offsets", the resulting
    // matrix A..I will still work, right?
    
    // we can compute our <u,v> position using fragBaryCoords
    myUVVal = vec2(u0 * fragBaryCoords.x + u1 * fragBaryCoords.y + u2 * fragBaryCoords.z, 
                   v0 * fragBaryCoords.x + v1 * fragBaryCoords.y + v2 * fragBaryCoords.z);
    
    
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
    
    // ---------------------------------------------------
    // let's try computing the <u,v> -> pixel values diferently:
    
    /*
    float x0 = V0pos.x;
    float x1 = V1pos.x;
    float x2 = V2pos.x;
    
    xU = A*x0+D*x1+G*x2;
    xV = B*x0+E*x1+H*x2;
    xC = C*x0+F*x1+I*x2;
    
    float y0 = V0pos.y;
    float y1 = V1pos.y;
    float y2 = V2pos.y;
    
    yU = A*y0+D*y1+G*y2;
    yV = B*y0+E*y1+H*y2;
    yC = C*y0+F*y1+I*y2;
    
    float w0 = V0pos.w;
    float w1 = V1pos.w;
    float w2 = V2pos.w;
    
    wU = A*w0+D*w1+G*w2;
    wV = B*w0+E*w1+H*w2;
    wC = C*w0+F*w1+I*w2;
    */

    minU = min(min(u0,u1),u2);
    minV = min(min(v0,v1),v2);
    maxU = max(max(u0,u1),u2);
    maxV = max(max(v0,v1),v2);
}
float geometricDistance(vec2 s, vec2 e) {

   vec2 p = myXYPixel;
   vec2 d = e-s;
   float dd = length(d);

   vec2 v = p-s;
   vec2 n = vec2(-d.y/dd, d.x/dd);
   float normDistance = abs(dot(n,v));
   
   return normDistance;
}   
float geometricDistanceFromSegment(vec2 s, vec2 e) {

   vec2 p = myXYPixel;
   vec2 d = e-s;
   float dd = length(d);

   vec2 v = p-s;
   vec2 n = vec2(-d.y/dd, d.x/dd);
   float normDistance = abs(dot(n,v));
   
   vec2 a = vec2(d.x/dd, d.y/dd);
   float alongDistance = dot(a,v);
   
   if (alongDistance < 0) {
      return length(p-s);
   }
   if (alongDistance > dd) {
      return length(p-e);
   }
   return normDistance;
}
float geometricDistance(vec2 o) {

   vec2 p = myXYPixel;
   vec2 d = p-o;
   return length(d);
}
vec2 pixelFromPos(float x, float y, float w) {
   return vec2(windowWidth  *((x/w) +1.0)/2.0, windowHeight *((y/w) +1.0)/2.0);
}

// ------------------------
// EDGE CODES
// ------------------------

const vec3 COLORS[8] = vec3[8]  ( vec3( 0.4, 0.5, 0.4 ),
                                  vec3( 1.0, 0.5, 0.5 ),
                                  vec3( 0.5, 1.0, 0.5 ),
                                  vec3( 0.5, 0.5, 1.0 ),
                                  vec3( 0.5, 1.0, 1.0 ),
                                  vec3( 1.0, 0.5, 1.0 ),
                                  vec3( 1.0, 1.0, 0.5 ),
                                  vec3( 1.0, 1.0, 1.0 ) );
void set(int colorCode) {
   vec3 rgb = COLORS[colorCode-1];
   outColor.r = rgb.x;
   outColor.g = rgb.y;
   outColor.b = rgb.z;
   done = true;  
}


void apply_EdgeAndVertexColors() {

    // get the WINDOW-PIXEL positions of the three vertices
    vec2 pixel0 = pixelFromPos(V0pos.x, V0pos.y, V0pos.w);
    vec2 pixel1 = pixelFromPos(V1pos.x, V1pos.y, V1pos.w);
    vec2 pixel2 = pixelFromPos(V2pos.x, V2pos.y, V2pos.w);
    
    vec2 pixel01mid = pixelFromPos((V0pos.x+V1pos.x)/2, (V0pos.y+V1pos.y)/2, (V0pos.w+V1pos.w)/2);
    vec2 pixel12mid = pixelFromPos((V1pos.x+V2pos.x)/2, (V1pos.y+V2pos.y)/2, (V0pos.w+V1pos.w)/2);
    vec2 pixel20mid = pixelFromPos((V2pos.x+V0pos.x)/2, (V2pos.y+V0pos.y)/2, (V0pos.w+V1pos.w)/2);
    
    vec2 pixelmid = pixelFromPos((V0pos.x+V1pos.x+V2pos.x)/3, (V0pos.y+V1pos.y+V2pos.y)/3, (V0pos.w+V1pos.w+V2pos.w)/3);
    
	// we return the minimum pixel distance to any edge..
    float d01 = geometricDistance(pixel0, pixel1);
    float d12 = geometricDistance(pixel1, pixel2);
    float d20 = geometricDistance(pixel2, pixel0);
    
    float d0 = geometricDistance(pixel0);
    float d1 = geometricDistance(pixel1);
    float d2 = geometricDistance(pixel2);

    int tColorCode  = int(fragTriColorInfo.x) & 0xff;
    //tColorCode = altTColor;
    
    int v0ColorCode = int(fragTriColorInfo.y >> 16) & 0xff;
    int v1ColorCode = int(fragTriColorInfo.y >>  8) & 0xff;
    int v2ColorCode = int(fragTriColorInfo.y)       & 0xff;
    
    //v0ColorCode = 3;
    //v1ColorCode = 3;
    //v2ColorCode = 3;
    
    int e0ColorCode = int(fragTriColorInfo.z >> 16) & 0xff;
    int e1ColorCode = int(fragTriColorInfo.z >>  8) & 0xff;
    int e2ColorCode = int(fragTriColorInfo.z)       & 0xff;

    //e0ColorCode = 4;
    //e1ColorCode = 5;
    //e2ColorCode = 6;
    
    int b0ColorCode = int(fragTriColorInfo.w >> 16) & 0xff;
    int b1ColorCode = int(fragTriColorInfo.w >>  8) & 0xff;
    int b2ColorCode = int(fragTriColorInfo.w)       & 0xff;
    
    int pixelScale = 1;
    int vertexScale = 8;
    
    int boundaryScale = 4;
    int localEdgeScale = 2;
      
    if ((d0 < vertexScale*pixelScale) && (v0ColorCode > 0)) {
       set(v0ColorCode);
       return;       
    }
    if ((d1 < vertexScale*pixelScale) && (v1ColorCode > 0)) {
       set(v1ColorCode);
       return;
    }
    if ((d2 < vertexScale*pixelScale) && (v2ColorCode > 0)) {
       set(v2ColorCode);
       return;       
    }
    
    localEdgeScale = 3;
    if (e2ColorCode == 1) localEdgeScale = 1;
    
    if (b2ColorCode > 0) {
      if (d01 < boundaryScale*pixelScale) {
        set(b2ColorCode);
        return;       
      }
      if ((d01 < (boundaryScale+localEdgeScale)*pixelScale) && (e2ColorCode > 0)) {
        set(e2ColorCode);
        return;       
      }
    } else {
      if ((d01 < localEdgeScale*pixelScale) && (e2ColorCode > 0)) {
        set(e2ColorCode);
        return;       
      }
    }
    
    localEdgeScale = 3;
    if (e0ColorCode == 1) localEdgeScale = 1;
      
    if (b0ColorCode > 0) {
      if (d12 < boundaryScale*pixelScale) {
        set(b0ColorCode);
        return;       
      }
      if ((d12 < (boundaryScale+localEdgeScale)*pixelScale) && (e0ColorCode > 0)) {
        set(e0ColorCode);
        return;       
      }
    } else {
      if ((d12 < localEdgeScale*pixelScale) && (e0ColorCode > 0)) {
        set(e0ColorCode);
        return;       
      }
    }

    localEdgeScale = 3;
    if (e1ColorCode == 1) localEdgeScale = 1;
      
    if (b1ColorCode > 0) {
      if (d20 < boundaryScale*pixelScale) {
        set(b1ColorCode);
        return;       
      }
      if ((d20 < (boundaryScale+localEdgeScale)*pixelScale) && (e1ColorCode > 0)) {
        set(e1ColorCode);
        return;       
      }
    } else {
      if ((d20 < localEdgeScale*pixelScale) && (e1ColorCode > 0)) {
        set(e1ColorCode);
        return;       
      }
    }
       
    if (tColorCode > 0) {
       set(tColorCode);
       return;       
    }
}
   

// ------------------------
// UV-SELECTED-LINE
// ------------------------

vec2 pixelAtUV(vec2 uv) {;
    // compute barycentric coords for arbitrary <u,v> point
    float lambda0 = A*uv.x + B*uv.y + C;
    float lambda1 = D*uv.x + E*uv.y + F;
    float lambda2 = G*uv.x + H*uv.y + I;
    
    // use barycentric coordinates to interpolate from corner's X,Y, and W values
    float point0Xh = lambda0 * V0pos.x + lambda1 * V1pos.x +  lambda2 * V2pos.x;
    float point0Yh = lambda0 * V0pos.y + lambda1 * V1pos.y +  lambda2 * V2pos.y;
    float point0W  = lambda0 * V0pos.w + lambda1 * V1pos.w +  lambda2 * V2pos.w;

    return pixelFromPos(point0Xh, point0Yh, point0W);
}
float pixelDistanceToULine(float uCritical) {
    vec2 pixelStart = pixelAtUV(vec2(uCritical, minV));
    vec2 pixelEnd   = pixelAtUV(vec2(uCritical, maxV));
    return geometricDistance(pixelStart, pixelEnd);
}
float pixelDistanceToVLine(float vCritical) {
    vec2 pixelStart = pixelAtUV(vec2(minU, vCritical));
    vec2 pixelEnd   = pixelAtUV(vec2(maxU, vCritical));
    return geometricDistance(pixelStart, pixelEnd);
}

void apply_SelectedUVLine() {
   float pixelDistanceToSelectedULine = pixelDistanceToULine(uvPointer.x);
   float pixelDistanceToSelectedVLine = pixelDistanceToVLine(uvPointer.y);
   
   if ((pixelDistanceToSelectedULine < 3.0f) && (pixelDistanceToSelectedVLine < 3.0f)) {
     outColor.r = 1.0f;
     outColor.g = 1.0f;
     outColor.b = 0.0f;
     done = true;
     return;
   } 
   if (((pixelDistanceToSelectedULine < 4.0f) && ((pixelDistanceToSelectedVLine >= 3.0f) && (pixelDistanceToSelectedVLine < 4.0f))) ||
      ((pixelDistanceToSelectedVLine < 4.0f) && ((pixelDistanceToSelectedULine >= 3.0f) && (pixelDistanceToSelectedULine < 4.0f)))) {
      
     outColor.r = 0.0f;
     outColor.g = 0.0f;
     outColor.b = 0.0f;         
     done = true;
     return;
   }
   if ((pixelDistanceToSelectedULine < 1.0f) || (pixelDistanceToSelectedVLine < 1.0f)) {
     outColor.r = 0.9f;
     outColor.g = 0.8f;
     outColor.b = 0.2f;         
     done = true;
     return;
   } 
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
      return ((scale - 1.0f) / 2.0f) * 0.25f;
   }
   if (scale < 9.0f) {
      return 0.25f + ((scale - 3.0f) / (9.0f - 3.0f)) * 0.25f;
   }
   if (scale < 27.0f) {
      return 0.5f + ((scale - 9.0f) / (27.0f - 9.0f)) * 0.25f;
   }
   if (scale < 81.0f) {
      return 0.75f + ((scale - 27.0f) / (81.0f - 27.0f)) * 0.25f;
   }
   return 1.0f;
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

    shadingLevel = makeShadingLevel(useU, 0.0005f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.0001f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.00005f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.00001f);
    if (shadingLevel >= 0.0) return shadingLevel;

    shadingLevel = makeShadingLevel(useU, 0.000005f);
    if (shadingLevel >= 0.0) return shadingLevel;
    
    shadingLevel = makeShadingLevel(useU, 0.000001f);
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

bool inSelectedFace2() {
    // compute barycentric coords for arbitrary <u,v> point
    float lambda0 = A*uvPointer2.x + B*uvPointer2.y + C;
    float lambda1 = D*uvPointer2.x + E*uvPointer2.y + F;
    float lambda2 = G*uvPointer2.x + H*uvPointer2.y + I;
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

float Value3D (vec3 st) { 
    return fract(sin(dot(vec3(int(st.x), int(st.y), int(st.z)),
                         vec3(12.9898,78.233, 34.342)))* 43758.5453123);
}


vec2 directionalShading() {
    int n = 10;  

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
   done = false;
   
   V0uv_ = V0uv;
   V1uv_ = V1uv;
   V2uv_ = V2uv;
   
   computeInfo();
      
   // 1st:  Edge and Vertex Colors
   apply_EdgeAndVertexColors();
   apply_SelectedUVLine();
   
   if (done == false) { 
	   vec2 dd = directionalShading();
	   dd = dd/2;
	   
	   float avg = (dd.x + dd.y)/2;
	   dd.x=avg; dd.y=avg;
	   float baseR = fragColor.r;
	   float baseG = (fragColor.g * (1.0 - dd.x) + 0.3 * dd.x) * (1.0 - dd.y/2.0);
	   float baseB = (fragColor.b * (1.0 - dd.y) + 0.0 * dd.y) * (1.0 - dd.x/3.0);
       outColor.r = baseR;
       outColor.g = baseG;
       outColor.b = baseB;
   }
    
   float frac = getGridShadingLevel();
   frac = 1.0f - (1.0f-frac)*(1.0f-frac);
   //float frac = 0;
   //frac = 1.0f - ((1.0f - frac) * tnz);
   
   outColor.r = (outColor.r * (1.0f-frac) + 1.0 * frac) * tnz;
   outColor.g = (outColor.g * (1.0f-frac) + 0.0 * frac) * tnz;
   outColor.b = (outColor.b * (1.0f-frac) + 0.1 * frac) * tnz;
   
   if (inSelectedFace()) {
      frac = 0.3;
      outColor.r = outColor.r * (1.0f-frac) + 0.0f * frac;
      outColor.g = outColor.g * (1.0f-frac) + 0.0f * frac;
      outColor.b = outColor.b * (1.0f-frac) + 1.0f * frac;
   }

   /*
   
   if (debugFace) {
      frac = 0.6;
      outColor.r = outColor.r * (1.0f-frac) + 1.0f * frac;
      outColor.g = outColor.g * (1.0f-frac) + 0.5f * frac;
      outColor.b = outColor.b * (1.0f-frac) + 0.5f * frac;
   }
   */
}


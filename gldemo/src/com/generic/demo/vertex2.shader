#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexV0pos;
in vec4 vertexV1pos;
in vec4 vertexV2pos;
in vec2 vertexV0uv;
in vec2 vertexV1uv;
in vec2 vertexV2uv;
in float vertexTriangleIndex;

in vec3 directions;

flat out vec4 V0pos;
flat out vec4 V1pos;
flat out vec4 V2pos;
flat out vec2 V0uv;
flat out vec2 V1uv;
flat out vec2 V2uv;
flat out float triangleIndex;

in uvec4 triColorInfo;

in vec3 vertexColor;
in vec3 vertexBaryCoords;
in vec4 vertexPosition;

flat out float tnz;
flat out uvec4 fragTriColorInfo;

out vec3 fragColor;
out vec3 fragBaryCoords;

flat out vec3 triangleShape;
flat out vec3 direction1;
flat out vec3 direction2;
 
void main()
{
    fragColor = vertexColor;
    fragBaryCoords = vertexBaryCoords;    
    
    V0pos = projMatrix * viewMatrix * vertexV0pos;
    V1pos = projMatrix * viewMatrix * vertexV1pos;
    V2pos = projMatrix * viewMatrix * vertexV2pos;
    
    V0uv = vertexV0uv;
    V1uv = vertexV1uv;
    V2uv = vertexV2uv;
    
    triangleIndex = vertexTriangleIndex;
    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
    

    vec3 v0 = vec3(vertexV0pos.x, vertexV0pos.y, vertexV0pos.z);
    vec3 v1 = vec3(vertexV1pos.x, vertexV1pos.y, vertexV1pos.z);
    vec3 v2 = vec3(vertexV2pos.x, vertexV2pos.y, vertexV2pos.z);
    vec3 e21 = v2 - v1;
    vec3 e10 = v1 - v0;
    vec3 e02 = v0 - v2;
    
    float e10l = length(e10);
    float e21l = length(e21);
    float e02l = length(e02);
    
    vec3 modelSpaceNormal = normalize(cross(e10,e02));
    vec3 newX = e10 / e10l;
    vec3 newY = cross(newX, modelSpaceNormal);
    
    triangleShape = vec3(e10l, -dot(e02, newX), dot(e02, newY));
    
    vec3 d1_3 =   newX * dot(directions, newX) + newY * dot(directions, newY);
    vec3 d2_3 = - newX * dot(directions, newY) + newY * dot(directions, newX);
    //vec3 d1_3 = (newX + newY)/sqrt(2);
    //vec3 d2_3 = (-newX + newY)/sqrt(2);
    
    vec4 d1 = projMatrix * viewMatrix * vec4(d1_3.x,d1_3.y,d1_3.z,0);
    vec4 d2 = projMatrix * viewMatrix * vec4(d2_3.x,d2_3.y,d2_3.z,0);
    direction1 = vec3(d1.x,d1.y,d1.z);
    direction2 = vec3(d2.x,d2.y,d2.z);
    
    // ----------------------------
    
    vec4 normalH = normalize(viewMatrix * vec4(modelSpaceNormal.x,modelSpaceNormal.y,modelSpaceNormal.z,0));
        
    float nv = normalH.z;
    if (nv<0.0f) nv = -nv;
    if (nv>1.0f) nv = 1.0f;
    tnz = nv;
    
    fragTriColorInfo = triColorInfo;    
    /*
    if ((triColorInfo.x != 0.0) || (triColorInfo.y != 0.0) || (triColorInfo.z != 0.0) || (triColorInfo.w != 0.0)) {
      fragTriColorInfo = uvec4(2,  (3 << 16)|(3 << 8)|(3),  (1 << 16)|(1 << 8)|(1),  (1 << 16)|(1 << 8)|(1));
    } else {
      fragTriColorInfo = uvec4(0,  (3 << 16)|(3 << 8)|(3),  (4 << 16)|(4 << 8)|(4),  (1 << 16)|(1 << 8)|(1));
    }
    */
    
}


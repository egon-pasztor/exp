#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexV0pos;
in vec4 vertexV1pos;
in vec4 vertexV2pos;
in vec2 vertexV0uv;
in vec2 vertexV1uv;
in vec2 vertexV2uv;

in vec3 directions;

out vec4 V0pos;
out vec4 V1pos;
out vec4 V2pos;
out vec2 V0uv;
out vec2 V1uv;
out vec2 V2uv;

in vec3 vertexColor;
in vec3 vertexBaryCoords;
in vec4 vertexPosition;

out vec3 fragColor;
out vec3 fragBaryCoords;

out vec3 triangleShape;
out vec3 direction1;
out vec3 direction2;
out mat4 matt4;
 
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
    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
    
    vec4 viewspace_V0pos = vertexV0pos;
    vec4 viewspace_V1pos = vertexV1pos;
    vec4 viewspace_V2pos = vertexV2pos;
    
    vec3 v0 = vec3(viewspace_V0pos.x, viewspace_V0pos.y, viewspace_V0pos.z);
    vec3 v1 = vec3(viewspace_V1pos.x, viewspace_V1pos.y, viewspace_V1pos.z);
    vec3 v2 = vec3(viewspace_V2pos.x, viewspace_V2pos.y, viewspace_V2pos.z);
    vec3 e21 = v2 - v1;
    vec3 e10 = v1 - v0;
    vec3 e02 = v0 - v2;
    
    float e10l = length(e10);
    float e21l = length(e21);
    float e02l = length(e02);
    /*
    if (e10l > e21l) {
      vec3 temp = e21;
      float templ = e21l;
      e21 = e10;
      e21l = e10l;
      e10 = temp;
      e10l = templ;
    }
    if (e02l > e10l) {
      vec3 temp = e10;
      float templ = e10l;
      e10 = e02;
      e10l = e02l;
      e02 = temp;
      e02l = templ;
    }
    if (e10l > e21l) {
      vec3 temp = e21;
      float templ = e21l;
      e21 = e10;
      e21l = e10l;
      e10 = temp;
      e10l = templ;
    }
    */
    
    vec3 normal = normalize(cross(e10,e02));
    vec3 newX = e10 / e10l;
    vec3 newY = cross(newX, normal);
    
    triangleShape = vec3(e10l, -dot(e02, newX), dot(e02, newY));
    
    vec3 d1_3 =   newX * dot(directions, newX) + newY * dot(directions, newY);
    vec3 d2_3 = - newX * dot(directions, newY) + newY * dot(directions, newX);
    //vec3 d1_3 = (newX + newY)/sqrt(2);
    //vec3 d2_3 = (-newX + newY)/sqrt(2);
    
    /*
    vec4 d1 = projMatrix * viewMatrix * vec4(d1_3.x,d1_3.y,d1_3.z,0);
    vec4 d2 = projMatrix * viewMatrix * vec4(d2_3.x,d2_3.y,d2_3.z,0);
    direction1 = vec3(d1.x,d1.y,d1.z);
    direction2 = vec3(d2.x,d2.y,d2.z);
    */
    direction1 = d1_3;
    direction2 = d2_3;
    
    matt4 = inverse (projMatrix * viewMatrix);
    
    // ----------------------------
    
}


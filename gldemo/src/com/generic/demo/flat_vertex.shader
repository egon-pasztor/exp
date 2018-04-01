#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexPosition;
in vec3 vertexColor;
in vec3 vertexNormal;
in vec3 vertexBaryCoords;
 
out vec3 fragColor;
out vec3 fragBaryCoords;
//flat out float tnz;
 
void main()
{
    //fragColor = vertexColor;
    fragBaryCoords = vertexBaryCoords;

    vec4 normalH = normalize(viewMatrix * vec4(vertexNormal.x,vertexNormal.y,vertexNormal.z,0));
    float nv = normalH.z;
    if (nv<0.0f) nv = -nv;
    if (nv>1.0f) nv = 1.0f;
    fragColor = vec3(0.5f,0.7f,0.8f) * nv;
    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
}

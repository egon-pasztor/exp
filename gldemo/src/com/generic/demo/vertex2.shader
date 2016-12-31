#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexPosition;
in vec3 vertexColor;
in vec2 vertexBaryCoords;

in vec4 vertexV0Pos;
in vec4 vertexV1Pos;
in vec4 vertexV2Pos;

out vec3 fragColor;
out vec4 fragTexCoords;
out vec2 fragBaryCoords;
 
out vec4 fragmentV0Pos;
out vec4 fragmentV1Pos;
out vec4 fragmentV2Pos;
out vec4 fragmentVPos;
 
 
void main()
{
    fragColor = vertexColor;
    fragBaryCoords = vertexBaryCoords;    
    
    fragmentV0Pos = projMatrix * viewMatrix * vertexV0Pos;
    fragmentV1Pos = projMatrix * viewMatrix * vertexV1Pos;
    fragmentV2Pos = projMatrix * viewMatrix * vertexV2Pos;
    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
    fragmentVPos = gl_Position;
}

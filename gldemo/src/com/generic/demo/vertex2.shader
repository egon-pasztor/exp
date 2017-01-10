#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexV0pos;
in vec4 vertexV1pos;
in vec4 vertexV2pos;
in vec2 vertexV0uv;
in vec2 vertexV1uv;
in vec2 vertexV2uv;

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
}

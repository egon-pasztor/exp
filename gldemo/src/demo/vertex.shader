#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 inVertexPosition;
in vec3 inVertexColor;
in vec4 inVertexTexCoords;
in vec2 inVertexBaryCoords;
 
out vec3 fragColor;
out vec4 fragTexCoords;
out vec2 fragBaryCoords;
 
void main()
{
    fragColor = inVertexColor;
    fragTexCoords = inVertexTexCoords;    
    fragBaryCoords = inVertexBaryCoords;    
    gl_Position = projMatrix * viewMatrix * inVertexPosition;
}

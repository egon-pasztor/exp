#version 150
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 inVertexPosition;
in vec3 inVertexColor;
in vec2 inVertexTexCoords;
 
out vec3 fragColor;
out vec2 fragTexCoords;
 
void main()
{
    fragColor = inVertexColor;
    fragTexCoords = inVertexTexCoords;    
    gl_Position = projMatrix * viewMatrix * inVertexPosition;
}

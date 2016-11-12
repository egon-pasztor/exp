#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexPosition;
in vec3 vertexColor;
//in vec4 vertexTexCoords;
in vec2 vertexBaryCoords;
 
out vec3 fragColor;
out vec4 fragTexCoords;
out vec2 fragBaryCoords;
 
void main()
{
    fragColor = vertexColor;
    //fragTexCoords = vertexTexCoords;    
    fragBaryCoords = vertexBaryCoords;    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
}

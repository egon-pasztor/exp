#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 vertexPosition;
in vec3 vertexColor;
in vec2 vertexBaryCoords;

uniform vec3 translation;

out vec3 fragColor;
out vec4 fragTexCoords;
out vec2 fragBaryCoords;
 
void main()
{
    fragColor = vec3(translation.x + vertexPosition.x,
                     translation.y + vertexPosition.y,
                     translation.z + vertexPosition.z);
    
    fragBaryCoords = vertexBaryCoords;    
    gl_Position = projMatrix * viewMatrix * vertexPosition;
}

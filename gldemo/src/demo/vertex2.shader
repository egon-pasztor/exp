#version 150
 
uniform mat4 viewMatrix, projMatrix;
 
in vec4 position;
in vec3 color;
in vec2 vertexUV;
 
out vec3 Color;
out vec2 fragmentUV;
 
void main()
{
    Color = color;
    fragmentUV = vertexUV;    
    gl_Position = projMatrix * viewMatrix * position ;
}

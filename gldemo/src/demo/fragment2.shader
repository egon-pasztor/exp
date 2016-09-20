#version 150

uniform sampler2D myTexture;
 
in vec3 fragColor;
in vec2 fragTexCoords;
out vec4 outColor;
 
void main()
{
    vec4 texColor = texture2D(myTexture, fragTexCoords);
    outColor = (texColor.a > 0.5) ? texColor : vec4(fragColor, 1.0);
}
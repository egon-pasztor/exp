#version 330

uniform sampler2D myTexture;
 
in vec3 fragColor;
in vec2 fragTexCoords;
out vec4 outColor;
 
void main()
{
    vec4 texColor = texture2D(myTexture, fragTexCoords);
    outColor = (texColor.a > 0.5) ? texColor : vec4(fragColor, 1.0);
    
    float d1 = fragTexCoords.x;
    float d2 = fragTexCoords.y;
    float d3 = (fragTexCoords.x-fragTexCoords.y)/1.414;
    float d4 = 1-fragTexCoords.x;
    float d5 = 1-fragTexCoords.y;
    if (d3 < 0) d3 = -d3;
    
    float lowestD = d1;
    if (d2 < lowestD) lowestD = d2;
    if (d3 < lowestD) lowestD = d3;
    if (d4 < lowestD) lowestD = d4;
    if (d5 < lowestD) lowestD = d5;
    
    if (lowestD < 0.01) outColor = vec4(0,0,0,0);
}
#version 330

//uniform sampler2D mainTexture;
uniform bool highlight;

in vec3 fragColor;
//in vec4 fragTexCoords;
in vec2 fragBaryCoords;
out vec4 outColor;
 
void main()
{
//    vec2 lk;
//    lk.x = fragTexCoords.x / fragTexCoords.w;
//    lk.y = fragTexCoords.y;
//    outColor = texture2D(mainTexture, lk);
    
    float d1 = fragBaryCoords.x;
    float d2 = fragBaryCoords.y;
    float d3 = (fragBaryCoords.x-fragBaryCoords.y)/1.414;
    float d4 = 1-fragBaryCoords.x;
    float d5 = 1-fragBaryCoords.y;
    if (d3 < 0) d3 = -d3;
    
    float lowestD = d1;
    if (d2 < lowestD) lowestD = d2;
    if (d3 < lowestD) lowestD = d3;
    if (d4 < lowestD) lowestD = d4;
    if (d5 < lowestD) lowestD = d5;
    
    if (lowestD < 0.01) {
         outColor.r = 0.0;
         outColor.g = 0.0;
         outColor.b = 0.0;
    } else {
       if (highlight) {
         outColor.r = 0.9;
         outColor.g = 0.1;
         outColor.b = 0.1;
       } else {
         outColor.r = fragColor.r;
         outColor.g = fragColor.g;
         outColor.b = fragColor.b;
       }
    }   
}
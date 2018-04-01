#version 330

uniform int windowWidth;
uniform int windowHeight;

//flat in float tnz;
in vec3 fragColor;
in vec3 fragBaryCoords;
 
out vec4 outColor;

void main()
{
    float bary0  = fragBaryCoords.x;
    float bary1  = fragBaryCoords.y;
    float bary2  = fragBaryCoords.z;	  
	float thresh = .03f;
    if ((bary0 < thresh) || (bary1 < thresh) || (bary2 < thresh)) {
    
	  outColor.r = 0.2f;
	  outColor.g = 0.2f;
	  outColor.b = 0.2f;
      return;
    }

    outColor = vec4(fragColor.x,fragColor.y,fragColor.z,0);
}
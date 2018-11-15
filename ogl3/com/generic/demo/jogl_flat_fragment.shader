#version 330

in vec3 fragColor;
in vec3 fragBaryCoords;

out vec4 outColor;

void main()
{
    float bary0 = fragBaryCoords.x;
    float bary1 = fragBaryCoords.y;
    float bary2 = fragBaryCoords.z;

	float thresh = 0.03f;
    if (((bary0 >= 0) && (bary0 < thresh)) ||
        ((bary1 >= 0) && (bary1 < thresh)) ||
        ((bary2 >= 0) && (bary2 < thresh))) {
 
      outColor.r = 0.0f;
      outColor.g = 0.0f;
      outColor.b = 0.0f;

    } else {
      outColor.r = fragColor.r;
      outColor.g = fragColor.g;
      outColor.b = fragColor.b;
    }
}
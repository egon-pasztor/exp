#version 330
 
uniform mat4 viewMatrix, projMatrix;
 
in vec3 vertexPosition;
in vec3 vertexNormal;
in vec3 vertexBaryCoords;

out vec3 fragColor;
out vec3 fragBaryCoords;

void main()
{
	gl_Position = projMatrix * viewMatrix * vec4(vertexPosition.x, vertexPosition.y, vertexPosition.z, 1.0f);
	
	vec4 normalH = normalize(viewMatrix * vec4(vertexNormal.x, vertexNormal.y, vertexNormal.z, 0.0f));
	float nv = normalH.z;
	if (nv<0.0f) nv = -nv;
	if (nv>1.0f) nv = 1.0f;
	fragColor = vec3(0.5f,0.7f,0.8f) * nv;
	
	fragBaryCoords = vertexBaryCoords;
}

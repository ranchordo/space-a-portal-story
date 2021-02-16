#version 120
#define pi 3.1415926535897932384
#define MAX_LIGHTS 50

uniform int types[MAX_LIGHTS]=int[MAX_LIGHTS](0);
uniform vec3 prop[MAX_LIGHTS]=vec3[MAX_LIGHTS](vec3(0,0,0));
uniform vec4 intensities[MAX_LIGHTS]=vec4[MAX_LIGHTS](vec4(0,0,0,1));
uniform mat3 view_matrix=mat3(1.0);
uniform mat4 master_matrix=mat4(1.0);
uniform float useLighting=2;
uniform float altValue=0.15;

varying vec4 intensity;
varying vec4 ft;
varying vec2 texCoords;
varying vec4 col;
varying vec3 world_position;
void main() {
	ft=ftransform();
	gl_Position=ft;
	texCoords=gl_MultiTexCoord0.st;
	intensity=vec4(0,0,0,1);
	world_position=(master_matrix*gl_Vertex).xyz;
	if(useLighting<1) {
		intensity=vec4(altValue,altValue,altValue,1);
	}
	col=gl_Color;//vec4(((tangent/2.0)+0.5)*0.15,1);
}
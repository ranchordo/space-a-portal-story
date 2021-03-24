#version 330 core
#define pi 3.1415926535897932384


uniform mat4 master_matrix=mat4(1.0);
uniform mat4 world2view=mat4(1.0);
uniform mat4 proj_matrix=mat4(1.0);
uniform float altValue=0.15;
uniform float useLighting=2;

attribute vec3 glv;
attribute vec3 gln;
attribute vec4 glc;
attribute vec2 mtc0;
attribute vec4 material;

varying vec2 texCoords;
varying vec4 col;
varying vec3 world_position;
varying vec3 normal_orig;
varying vec3 campos;
varying vec4 intensity;
void main() {
	campos=(inverse(world2view)[3]).xyz;
	mat4 mvp=proj_matrix*world2view*master_matrix;
	gl_Position=mvp*vec4(glv,1.0);
	texCoords=mtc0.st;
	world_position=(master_matrix*vec4(glv,1.0)).xyz;
	if(useLighting<1) {
		intensity=vec4(altValue,altValue,altValue,1);
	}
	col=glc;
}
#version 330 core
#define pi 3.1415926535897932384
#define MAX_LIGHTS 50

uniform mat4 master_matrix=mat4(1.0);
uniform mat4 world2view=mat4(1.0);
uniform mat4 proj_matrix=mat4(1.0);
uniform float useLighting=2;

attribute vec3 glv;
attribute vec3 gln;
attribute vec4 glc;
attribute vec2 mtc0;
attribute vec3 tangent;
attribute vec3 bitangent;
attribute vec4 material;

varying vec4 col;
void main() {
	mat4 mvp=proj_matrix*world2view*master_matrix;
	gl_Position=proj_matrix*world2view*vec4(1.0*((master_matrix*vec4(glv,1.0)).xyz),1.0);
	col=glc;//vec4(((tangent/2.0)+0.5)*0.15,1);
}
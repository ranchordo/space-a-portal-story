#version 430 core
#define pi 3.1415926535897932384

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
varying float alive;

struct Particle {
	vec4 pos_active;
	vec4 vel;
};
layout (std140) buffer particles_buffer {
	Particle[] particles;
};

mat3 lookAt(vec3 origin, vec3 target, float roll) {
	vec3 rr=vec3(sin(roll),cos(roll),0.0);
	vec3 ww=normalize(target-origin);
	vec3 uu=normalize(cross(ww,rr));
	vec3 vv=normalize(cross(uu,ww));
	return mat3(uu,vv,ww);
}


void main() {
	campos=(inverse(world2view)[3]).xyz;
	vec3 particleCenter=particles[gl_InstanceID].pos_active.xyz;
	vec3 glvm=(lookAt(particleCenter,campos,0.0)*glv)+particleCenter;
	world_position=glvm;
	mat4 mvp=proj_matrix*world2view;
	gl_Position=mvp*vec4(glvm,1.0);
	texCoords=mtc0.st;
	if(useLighting<1) {
		intensity=vec4(altValue,altValue,altValue,1);
	}
	col=glc;
	alive=particles[gl_InstanceID].pos_active.w;
}
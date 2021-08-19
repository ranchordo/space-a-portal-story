#version 330 core

attribute vec3 glv;
attribute vec2 mtc0;

varying vec2 texcoords;
uniform mat4 proj_matrix;
uniform vec4 info;
uniform vec4 texinfo=vec4(0,0,1,1);
void main() {
	gl_Position=proj_matrix*vec4((glv.x*info.z)+info.x,(glv.y*info.w)+info.y,-2.0,1.0);
	texcoords=(mtc0*texinfo.zw)+texinfo.xy;
}
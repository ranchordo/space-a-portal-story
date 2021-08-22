#version 330 core
#define pi 3.1415926535897932384
#define MAX_LIGHTS 50

//struct Info {
//	mat4 obj2world;
//	vec4 shapeinfo;
//	vec4 texinfo;
//};

//layout (std140) buffer info_buffer {
//	Info infos[];
//};

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

varying vec2 texCoords;
void main() {
	//Info info=infos[gl_InstanceID];
	mat4 master_matrix=mat4(1);//info.obj2world;
	mat4 mvp=proj_matrix*world2view*master_matrix;
	gl_Position=mvp*vec4(glv,1.0);//*info.shapeinfo.xyz,1.0);
}
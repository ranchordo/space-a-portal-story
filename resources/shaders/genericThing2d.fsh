#version 430 core

layout (location = 0) out vec4 FragColor;

struct Info {
	vec4 info;
	vec4 texinfo;
	vec4 colinfo;
};

layout (std140) buffer info_buffer {
	Info infos[];
};

varying vec2 texcoords;
varying float instanceID;

uniform sampler2D tex;
void main() {
	Info info=infos[int(instanceID)];
	FragColor=texture2D(tex,texcoords);
}	
#version 330 core

layout (location = 0) out vec4 FragColor;

varying vec2 texcoords;

uniform sampler2D tex;
void main() {
	float c=texture2D(tex,texcoords).w;
	FragColor=vec4(0.15,0.15,0.15,c);
}	
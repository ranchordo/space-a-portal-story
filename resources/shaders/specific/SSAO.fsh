#version 330 core

layout (location = 0) out vec4 FragColor;

varying vec2 texcoords;

uniform sampler2D iPosition;
uniform sampler2D iNormal;
uniform sampler2D iNoise;
uniform vec3 samples[64];
uniform int numSamples=64;
uniform mat4 proj_matrix;
uniform int scrWidth;
uniform int scrHeight;
uniform int noiseWidth;
uniform float radius=0.5;

void main() {
	vec3 randomVec=texture2D(iNoise,vec2(texcoords.x*scrWidth/noiseWidth,texcoords.y*scrHeight/noiseWidth)).xyz;
	vec3 fragPos=texture2D(iPosition,texcoords).xyz;
	vec3 normal=texture2D(iNormal,texcoords).xyz;
	vec3 tangent=normalize(randomVec-normal*dot(randomVec,normal));
	vec3 bitangent=cross(normal,tangent);
	mat3 TBN=mat3(tangent,bitangent,normal);
	float occlusion=0.0;
	for(int i=0;i<numSamples;i++) {
		vec3 samplePos=TBN * samples[i];
		samplePos=fragPos+(samplePos*radius);
		vec4 offset=vec4(samplePos, 1.0);
		offset=proj_matrix * offset;
		offset.xyz/=offset.w;
		offset.xyz=offset.xyz*0.5+0.5;
		float sampleDepth=texture(iPosition, offset.xy).z;
		float rangeCheck=smoothstep(0.0,1.0,radius/abs(fragPos.z-sampleDepth));
		occlusion+=(sampleDepth>=samplePos.z+0.025 ? 1.0 : 0.0)*rangeCheck; 
	}
	occlusion=(occlusion / numSamples);
	FragColor=vec4(occlusion,occlusion,occlusion,1.0);  
}	
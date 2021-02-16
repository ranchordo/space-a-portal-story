#version 120

#define pi 3.1415926535897932384
#define MAX_LIGHTS 50

uniform int types[MAX_LIGHTS]=int[MAX_LIGHTS](0);
uniform vec3 prop[MAX_LIGHTS]=vec3[MAX_LIGHTS](vec3(0,0,0));
uniform vec4 intensities[MAX_LIGHTS]=vec4[MAX_LIGHTS](vec4(0,0,0,1));
uniform float useLighting=2;

uniform mat3 view_matrix;

uniform float useDetail=0;

varying vec3 world_position;
varying vec2 texCoords;
varying vec4 intensity;
varying vec4 col;
varying vec4 ft;
uniform sampler2D tex;
uniform float useTextures=0;
float stop(float v) {
	if(v>1) {return 1;}
	return v;
}
void main() {
	vec4 intensity_in=intensity;
	if(useLighting>=1) {
		for(int i=0;i<MAX_LIGHTS;i++) {
			if(types[i]==0) {
				break;
			} else if(types[i]==1) {
				//Ambient light
				intensity_in=intensity_in+vec4(intensities[i].xyz,0);
			} else if(types[i]==2) {
				//Directional light
				intensity_in=intensity_in+vec4(intensities[i].xyz,0);
			} else if(types[i]==3) {
				//Positional light
				float r=sqrt(pow(world_position.x-prop[i].x,2)+pow(world_position.y-prop[i].y,2)+pow(world_position.z-prop[i].z,2));
				intensity_in=intensity_in+vec4(intensities[i].xyz*(1.0/pow(r,2)),0);
			}
		}
	}
	vec4 fcol=vec4((intensity_in * col).xyz,col.w);
	if(useTextures>=1) {
		fcol=fcol*texture2D(tex,texCoords);
	}
	gl_FragColor=vec4(fcol.xyz,stop(fcol.w));//vec4(((normal/2.0)+0.5)*0.15,1);
}
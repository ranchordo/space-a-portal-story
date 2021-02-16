package particles;

import java.util.HashSet;
import java.util.Random;

//import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import graphics.Shader;

public class ParticleWorld {
	public static float rcomp() {
		return (ParticleWorld.smokeRand.nextInt(1000)/500.0f)-1.0f;
	}
	public static Random smokeRand=new Random();
	public static HashSet<ParticleEmitter> sims=new HashSet<ParticleEmitter>();
	
	public static Shader particleShader=new Shader("specific/particle");
	
	public static void init() {
		//Nothing required
	}
	
//	public static Vector3f u(Particle r) {
//		if(r.nv==null) {r.nv=new Vector3f(0,0,0);}
//		Vector3f offset=new Vector3f(rcomp(),rcomp(),rcomp());
//		offset.normalize();
//		offset.scale(r.e.noiseMul*rcomp());
//		r.nv.add(offset);
//		//Vector3f ret=new Vector3f(new Vector3d(Math.cos(5*r.p.y),0.5f,Math.sin(5*r.p.y)));
//		//ret.scale(2);
//		return new Vector3f(r.nv.x,0.8f,r.nv.z);
//	}
}

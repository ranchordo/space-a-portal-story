package particles;

import static org.lwjgl.opengl.GL46.*;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Tri;
import logger.Logger;
import pooling.PoolElement;
import pooling.Pools;
import util.Util;

public class Particle {
	Vector3f p;
	Vector3f nv; //Just a vector so that the vel alg can keep track of particles
	public ParticleEmitter e; //parent emitter
	public boolean alive=true;
	public long startTime;
	public long lastMillis;
	public float scale=1;
	public int life_offset=0;
	public Particle(Vector3f pos,ParticleEmitter e) {
		this.e=e;
		startTime=RenderUtils.millis();
		p=(Vector3f)pos.clone();
		
	}
	Vector3f vel=new Vector3f();
	public void step(long millis) {
		lastMillis=millis;
		vel=u();
		vel.scale(1.0f/RenderUtils.fr);
		p.add(vel);
		if(millis-startTime>=(e.life+life_offset)) {
			alive=false;
		}
	}
	Vector3f offset=new Vector3f();
	Vector3f ret=new Vector3f();
	public Vector3f u() {
		if(nv==null) {
			nv=new Vector3f(ParticleWorld.rcomp(),ParticleWorld.rcomp(),ParticleWorld.rcomp());
			nv.normalize();
			nv.scale(ParticleWorld.rcomp());
			nv=new Vector3f(nv.x*e.noiseMul.x, nv.y*e.noiseMul.y, nv.z*e.noiseMul.z);
			nv.scale(10f);
		}
		offset.set(ParticleWorld.rcomp(),ParticleWorld.rcomp(),ParticleWorld.rcomp());
		offset.normalize();
		offset.scale(ParticleWorld.rcomp());
		offset.set(offset.x*e.noiseMul.x, offset.y*e.noiseMul.y, offset.z*e.noiseMul.z);
		nv.add(offset);
		//Vector3f ret=new Vector3f(new Vector3d(Math.cos(5*r.p.y),0.5f,Math.sin(5*r.p.y)));
		//ret.scale(2);
		ret.set(nv.x,nv.y,nv.z);
		ret.add(e.addvel);
		return ret;
	}
	Vector3f player_local=new Vector3f();
	Vector3f axis=new Vector3f();
	float angle;
	Transform mm=new Transform();
	Vector3f normal=new Vector3f(0,0,-1);
	public void render() {
		//if(true) {return;}
		e.handleColors(this);
		e.geo.scale.set(scale,scale,scale);
		player_local.set(Renderer.camera.pos_out);
		player_local.sub(p);
		//axis=new Vector3f();
		axis.cross(player_local,normal);
		angle=player_local.angle(normal);
		axis.normalize();
//		//System.out.println(axis+",    "+angle);
		glPushMatrix();
		PoolElement<Matrix4f> mm_mat=Pools.matrix4f.alloc();
		PoolElement<AxisAngle4f> aa4f=Pools.axisAngle4f.alloc();
		aa4f.o().set(axis.x,axis.y,axis.z,-angle);
		PoolElement<Quat4f> q=Util.AxisAngle(aa4f.o());
		mm_mat.o().set(q.o(),p,1);
		mm.set(mm_mat.o());
		e.geo.highRender_noPushPop_customTransform(mm);
		glPopMatrix();
		mm_mat.free();
		aa4f.free();
		q.free();
	}
}

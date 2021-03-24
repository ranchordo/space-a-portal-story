package particles;

import static org.lwjgl.opengl.GL46.*;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import graphics.GObject;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Shader;
import graphics.Tri;
import lighting.Lighting;
import logger.Logger;

public abstract class ParticleEmitter {
	public HashSet<Particle> particles=new HashSet<Particle>();
	public HashSet<Particle> recycling=new HashSet<Particle>();
	public Vector3f emitter;
	public Color col;
	public Vector3f radius=new Vector3f(1,1,1);
	public float size=0.4f;
	public long life=0;
	public Vector3f noiseMul;
	public Vector3f addvel;
	public boolean useLighting;
	public long startTime=0;
	public GObject geo;
	boolean hollow;
	public ParticleEmitter(Vector3f n, Color col, Vector3f radius, float size, long life, Vector3f addvel, Vector3f noiseMul, boolean ul) {
		this.startTime=RenderUtils.millis();
		this.emitter=n;
		this.col=col;
		this.life=life;
		this.radius=radius;
		this.noiseMul=noiseMul;
		this.size=size; //Particle size
		this.addvel=addvel;
		useLighting=ul;
		
		this.geo=new GObject();
		this.geo.useLighting=useLighting;
		this.geo.useTex=true;
		this.geo.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.vmap.vertices.add(new Vector3f(-size,-size,0));
		this.geo.vmap.vertices.add(new Vector3f(+size,-size,0));
		this.geo.vmap.vertices.add(new Vector3f(+size,+size,0));
		this.geo.vmap.vertices.add(new Vector3f(-size,+size,0));
		this.geo.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.vmap.texcoords.add(new Vector2f(0,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,0));
		this.geo.vmap.texcoords.add(new Vector2f(0,0));
		this.geo.vmap.normals=new ArrayList<Vector3f>();
		this.geo.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.clearTris();
		this.geo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.geo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		this.geo.setColor(col.getRed()/256.0f,col.getGreen()/256.0f,col.getBlue()/256.0f,col.getAlpha()/256.0f);
		geo.lock();
		geo.hasAlpha=true;
		
		this.geo.vmap.tex.colorLoaded=true;
		try {
			this.geo.loadTexture("3d/particles/generic_medres.png");
		} catch (IOException ex) {
			Logger.log(4,ex.toString(),ex);
		}
		
		this.geo.initVBO();
		this.geo.refresh(); //Copy data
	}
	public ParticleEmitter setEmitter(Vector3f n) {
		emitter=n;
		return this;
	}
	public void handleColors(Particle p) {
		
	}
	public ParticleEmitter setHollow(boolean h) {
		this.hollow=h;
		return this;
	}
	public void step() {
		//if(true) {return;}
		HashSet<Particle> dead=new HashSet<Particle>();
		for(Particle particle : particles) {
			if(particle.alive) {particle.step(RenderUtils.millis());}
			else {dead.add(particle);}
		}
		for(Particle deadParticle : dead) {
			if(!particles.remove(deadParticle)) {Logger.log(2,"ParticleSimulation: Dead particle wasn't in particles hashmap.");}
			recycling.add(deadParticle); //Out with the old
		}
	}
	Shader prevShader;
	public void render() {
		//System.out.println(particles.size());
		prevShader=Renderer.activeShader;
		ParticleWorld.particleShader.bind();
		Lighting.apply();
		glDepthMask(false);
		for(Particle particle : particles) {
			//if(particle.alive) {particle.render();}
		}
		glDepthMask(true);
		prevShader.bind();
		Lighting.apply();
		prevShader=null;
	}
	public void onAdd(Particle p) {
		
	}
	Vector3f nloc;
	Vector3f offset;
	Particle ta;
	Particle old;
	public void add() {
		//if(true) {return;}
		nloc=(Vector3f)emitter.clone();
		offset=new Vector3f(ParticleWorld.rcomp(),ParticleWorld.rcomp(),ParticleWorld.rcomp());
		offset.normalize();
		if(!hollow) {offset.scale((ParticleWorld.smokeRand.nextInt(1000)/1000.0f));}
		offset=new Vector3f(offset.x*radius.x,offset.y*radius.y,offset.z*radius.z);
		nloc.add(offset);
		if(recycling.size()==0) {
			ta=new Particle(nloc,this);
			onAdd(ta);
			particles.add(ta);
		} else {
			old=recycling.iterator().next(); //Get a particle from recycling
			old.scale=1;	//Reset the particle
			old.startTime=RenderUtils.millis();
			old.lastMillis=old.startTime;
			handleColors(old);
			old.p=nloc;
			old.nv=null;
			old.alive=true;
			recycling.remove(old);
			onAdd(old);
			particles.add(old); //In with the new
		}
	}
	public void add(int n) {
		for(int i=0;i<n;i++) {add();}
	}
	public void clear() {
		particles.clear();
	}
	public void refresh() {
		geo.refresh();
	}
}

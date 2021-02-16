package objects;

import java.awt.Color;

import javax.vecmath.Vector3f;

import particles.Particle;
import particles.ParticleEmitter;

public class ParticleSystem extends Thing {
	private static final long serialVersionUID = -7979784390699090641L;
	public transient ParticleEmitter emitter;
	Vector3f pos;
	Color col;
	long life;
	Vector3f rad;
	Vector3f noiseMul;
	Vector3f addvel;
	float size;
	public float ppfr;
	boolean useLighting;
	boolean burstDone=false;
	int burstAmount;
	public ParticleSystem(Vector3f pos, Color col, Vector3f rad, float fr, float size, long life, Vector3f addvel, Vector3f nm, boolean ul, int ba) {
		this.pos=pos;
		this.col=col;
		this.rad=rad;
		this.life=life;
		this.type="Particle_system";
		noiseMul=nm;
		this.size=size;
		this.addvel=addvel;
		ppfr=fr;
		useLighting=ul;
		burstAmount=ba;
	}
	
	@Override
	public void initPhysics() {}
	public void handleColors(Particle p) {}
	public void onAdd(Particle p) {}
	public void handleThings(ParticleEmitter e) {}
	public void initEmitter(ParticleEmitter e) {}
	@Override
	public void initVBO() {
		
	}
	@Override
	public void initGeo() {
		emitter=new ParticleEmitter(pos, col, rad, size, life, addvel, noiseMul, useLighting) {
			@Override
			public void handleColors(Particle p) {
				ParticleSystem.this.handleColors(p);
			}
			@Override
			public void onAdd(Particle p) {
				ParticleSystem.this.onAdd(p);
			}
		};
		this.initEmitter(emitter);
		declareResource(emitter.geo);
	}
	@Override
	public void refresh() {
		emitter.refresh();
	}
	@Override
	public void alphaRender() {
		emitter.render();
	}
	@Override
	public void render() {}
	@Override
	public void interact() {
		this.interacted=false;
	}
	private int counter=0;
	@Override
	public void logic() {
		handleThings(emitter);
		if(!burstDone) {
			burstDone=true;
			emitter.add(burstAmount);
		}
		if(ppfr<0) {
			//Do nothing
		} else if(ppfr>=1) {
			emitter.add(Math.round(ppfr));
		} else {
			int targ=Math.round(1.0f/ppfr);
			counter++;
			if(counter==targ) {
				emitter.add();
				counter=0;
			}
		}
		emitter.step();
		portalCounter++;
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
}

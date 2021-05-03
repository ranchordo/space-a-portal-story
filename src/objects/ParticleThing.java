package objects;

import java.util.ArrayList;

import particles.ParticleEmitter;
import particles.ParticleSystem;

public class ParticleThing extends Thing {
	private static final long serialVersionUID = -7979784390699090641L;
	public transient ParticleSystem system;
	public ArrayList<ParticleEmitter> emitters=new ArrayList<ParticleEmitter>();
	
	public ParticleThing(ParticleEmitter... emitters) {
		for(ParticleEmitter e : emitters) {
			this.emitters.add(e);
		}
	}
	@Override
	public void initPhysics() {}
	@Override
	public void initVBO() {
		
	}
	@Override
	public void initGeo() {
		system=new ParticleSystem();
		for(ParticleEmitter e : emitters) {
			system.addEmitter(e);
		}
		system.lock();
		system.initialize();
	}
	@Override
	public void refresh() {
		
	}
	@Override
	public void alphaRender() {
		system.render();
	}
	@Override
	public void render() {}
	@Override
	public void interact() {
		this.interacted=false;
	}
	@Override
	public void logic() {
		system.stepParticles();
		portalCounter++;
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
}

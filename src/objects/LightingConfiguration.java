package objects;

import lepton.engine.rendering.lighting.Light;
import lepton.engine.rendering.lighting.Lighting;

public class LightingConfiguration extends Thing {
	private static final long serialVersionUID = -7979784390699090641L;
	private Light[] lights;
	public LightingConfiguration(Light... lights) {
		this.type="Lighting_Configuration";
		this.lights=lights;
		this.doPhysicsOnSerialization=true;
	}
	@Override
	public void initPhysics() {}
	@Override
	public void initVBO() {}
	@Override
	public void initGeo() {}
	@Override
	public void refresh() {
	}
	@Override
	public void render() {
		//Do nothing
	}
	@Override
	public void alphaRender() {
		
	}
	@Override
	public void logic() {
		if(activations>=activationThreshold) {
			sendingActivations=true;
		} else {
			sendingActivations=false;
		}
		portalCounter++;
	}
	@Override
	public void addPhysics() {
		for(Light l : lights) {
			Lighting.addLight(l);
		}
	}
	@Override
	public void addPhysics(short group, short mask) {
		addPhysics();
	}
}


package particles;

import java.io.Serializable;

import javax.vecmath.Vector3f;

public class ParticleEmitter implements Serializable {
	private static final long serialVersionUID = -4421414638503669392L;
	public Vector3f worldLocation;
	public float radius;
	public Vector3f addedVelocity;
	public float particlesPerSecond=1;
	public Vector3f randomVelocity;
	public ParticleEmitter(Vector3f wl, float r, Vector3f vel, float rate, Vector3f rvel) {
		worldLocation=wl;
		radius=r;
		addedVelocity=vel;
		particlesPerSecond=rate;
		randomVelocity=rvel;
	}
}

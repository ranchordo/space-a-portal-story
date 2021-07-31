package physics;

import java.util.HashSet;

import lepton.engine.physics.PhysicsStepModifier;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;

public class PortalPhysicsStepModifier extends PhysicsStepModifier {
	private PhysicsWorld physics;
	public PortalPhysicsStepModifier(PhysicsWorld physicsWorld) {
		this.physics=physicsWorld;
	}
	@Override
	public void postStepProcess(HashSet<RigidBodyEntry> bodies) {
	}
	
	@Override
	public void preStepProcess(HashSet<RigidBodyEntry> bodies) {
		
	}

}

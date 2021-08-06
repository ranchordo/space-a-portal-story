package physics;

import java.util.HashSet;

import lepton.engine.physics.PhysicsStepModifier;
import lepton.engine.physics.RigidBodyEntry;

public abstract class GenericPhysicsStepModifier extends PhysicsStepModifier {
	abstract void postStepProcessNew(HashSet<RigidBodyEntry> arg0);
	abstract void preStepProcessNew(HashSet<RigidBodyEntry> arg0);
	@Override
	public final void postStepProcess(HashSet<RigidBodyEntry> arg0) {
		postStepProcessNew(arg0);
	}

	@Override
	public final void preStepProcess(HashSet<RigidBodyEntry> arg0) {
		preStepProcessNew(arg0);
	}
	
}

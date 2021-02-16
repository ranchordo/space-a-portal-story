package util;

import com.bulletphysics.dynamics.RigidBody;

public class RigidBodyEntry {
	public RigidBody b;
	public short group;
	public short mask;
	public RigidBodyEntry(RigidBody b, short group, short mask) {
		this.b=b;
		this.mask=mask;
		this.group=group;
	}
}

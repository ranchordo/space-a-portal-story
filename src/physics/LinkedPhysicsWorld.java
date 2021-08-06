package physics;

import java.util.HashSet;
import java.util.Map.Entry;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import util.Util;

public class LinkedPhysicsWorld {
	
	public static RigidBodyConstructionInfo getConstructionInfo(RigidBody b) {
		MotionState motionState=new DefaultMotionState(b.getMotionState().getWorldTransform(new Transform()));
		Vector3f inertia=new Vector3f();
		CollisionShape shape=b.getCollisionShape();
		if((1.0f/b.getInvMass())!=0) {
			shape.calculateLocalInertia((1.0f/b.getInvMass()),inertia);
		}
		RigidBodyConstructionInfo ret=new RigidBodyConstructionInfo((1.0f/b.getInvMass()),motionState,shape,inertia);
		ret.angularDamping=b.getAngularDamping();
		ret.angularSleepingThreshold=b.getAngularSleepingThreshold();
		ret.friction=b.getFriction();
		ret.linearDamping=b.getLinearDamping();
		ret.linearSleepingThreshold=b.getLinearSleepingThreshold();
		ret.mass=(1.0f/b.getInvMass());
		ret.restitution=b.getRestitution();
		return ret;
	}
	
	private PhysicsWorld world1;
	private PhysicsWorld world2;
	public PhysicsWorld getWorld1() {
		return world1;
	}
	public PhysicsWorld getWorld2() {
		return world2;
	}
	public void setWorld1(PhysicsWorld n) {
		world1=n;
	}
	public void setWorld2(PhysicsWorld n) {
		world2=n;
	}
	public void createAndLinkWorld2(GenericPhysicsStepModifier stepModifier) {
		world2=new PhysicsWorld();
		if(stepModifier==null) {
			stepModifier=new GenericPhysicsStepModifier() {
				@Override void postStepProcessNew(HashSet<RigidBodyEntry> arg0) {}
				@Override void preStepProcessNew(HashSet<RigidBodyEntry> arg0) {}
			};
		}
		world2.activePhysicsStepModifier=stepModifier;
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBody newb=new RigidBody(getConstructionInfo(rbe.b));
			RigidBodyEntry linked_rbe=new RigidBodyEntry(newb,rbe.group,rbe.mask);
			((UserPointerStructure)rbe.b.getUserPointer()).addUserPointer("linked_rbe",linked_rbe);
			((UserPointerStructure)linked_rbe.b.getUserPointer()).addUserPointer("linked_rbe",rbe);
			world2.add(linked_rbe);
		}
	}
	public void clearWorld2() {
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
			if(world2.getBodies().contains(lrbe)) {
				world2.remove(lrbe.b);
			}
		}
	}
	public void rebuildWorld2() {
		clearWorld2();
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBody newb=new RigidBody(getConstructionInfo(rbe.b));
			RigidBodyEntry linked_rbe=new RigidBodyEntry(newb,rbe.group,rbe.mask);
			((UserPointerStructure)rbe.b.getUserPointer()).addUserPointer("linked_rbe",linked_rbe);
			for(Entry<String,Object> userPointer : ((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().entrySet()) {
				((UserPointerStructure)linked_rbe.b.getUserPointer()).addUserPointer(userPointer.getKey(),userPointer.getValue());
			}
			((UserPointerStructure)linked_rbe.b.getUserPointer()).addUserPointer("linked_rbe",rbe);
			world2.add(linked_rbe);
		}
	}
	private Transform tr=new Transform();
	private Vector3f a=new Vector3f();
	private Vector3f b=new Vector3f();
	public void step_linked() {
		world1.step();
		world2.step();
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
			if(!world2.getBodies().contains(lrbe)) {
				continue;
			}
			Vector3f rbe_loc=rbe.b.getWorldTransform(tr).origin;
			Vector3f lrbe_loc=lrbe.b.getWorldTransform(tr).origin;
			float dist=Util.distance(rbe_loc,lrbe_loc);
			if(dist>1.0e-2f) {
				RigidBodyEntry max_vel=(rbe.b.getLinearVelocity(a).length()>lrbe.b.getLinearVelocity(b).length())?rbe:lrbe;
				((max_vel==rbe)?lrbe:rbe).b.setWorldTransform(((max_vel==rbe)?rbe:lrbe).b.getWorldTransform(tr));
				((max_vel==rbe)?lrbe:rbe).b.setLinearVelocity(((max_vel==rbe)?rbe:lrbe).b.getLinearVelocity(a));
			}
		}
	}
	public void add_linked(RigidBodyEntry rb) {
		world1.add(rb);
		RigidBodyEntry linked_rbe=((RigidBodyEntry)((UserPointerStructure)rb.b.getUserPointer()).getUserPointers().get("linked_rbe"));
		if(linked_rbe==null) {
			linked_rbe=new RigidBodyEntry(rb.b,rb.group,rb.mask);
			((UserPointerStructure)rb.b.getUserPointer()).getUserPointers().put("linked_rbe",linked_rbe);
			((UserPointerStructure)linked_rbe.b.getUserPointer()).getUserPointers().put("linked_rbe",rb);
		}
		world2.add(linked_rbe);
	}
}

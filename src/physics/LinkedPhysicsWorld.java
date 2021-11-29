package physics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

import game.PlayerInitializer;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.util.LeptonUtil;
import objects.PortalPair;
import objects.PortalTunnel;
import objects.Thing;
import util.Util;

public class LinkedPhysicsWorld {
	public static RigidBodyConstructionInfo getConstructionInfo(RigidBody b, Transform tr) {
		MotionState motionState=new DefaultMotionState(tr);
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
	public static RigidBodyConstructionInfo getConstructionInfo(RigidBody b) {
		return getConstructionInfo(b,b.getMotionState().getWorldTransform(new Transform()));
	}
	
	private Thing portalTunnel;

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
	public void createWorld2(GenericPhysicsStepModifier stepModifier) {
		world2=new PhysicsWorld();
		if(stepModifier==null) {
			stepModifier=new GenericPhysicsStepModifier() {
				@Override void postStepProcessNew(HashSet<RigidBodyEntry> arg0) {}
				@Override void preStepProcessNew(HashSet<RigidBodyEntry> arg0) {}
			};
		}
		world2.activePhysicsStepModifier=stepModifier;
	}
	public void clearWorld2() {
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
			if(world2.getBodies().contains(lrbe)) {
				world2.remove(lrbe.b);
			}
			if(lrbe!=null) {
				RigidBodyEntry alrbe=((RigidBodyEntry)((UserPointerStructure)lrbe.b.getUserPointer()).getUserPointers().get("additional_linked_rbe"));
				if(alrbe!=null && world2.getBodies().contains(alrbe)) {
					world2.remove(alrbe.b);
				}
			}
		}
		for(RigidBodyEntry rbe : world2.getBodies()) {
			if(((UserPointerStructure)rbe.b.getUserPointer()).ParentRBEntryPointer!=rbe) {
				System.out.println("OK");
			}
			if(((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("portal_tunnel")!=null) {
				world2.remove(rbe.b);
			}
		}
		System.out.println(world2.getBodies().size());
	}
	public void rebuildWorld2WithoutDuplicateStructures() {
		clearWorld2();
		for(RigidBodyEntry rbe : world1.getBodies()) {
			RigidBody newb=new RigidBody(getConstructionInfo(rbe.b));
			RigidBodyEntry linked_rbe=new RigidBodyEntry(newb,rbe.group,rbe.mask);
			for(Entry<String,Object> userPointer : ((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().entrySet()) {
				((UserPointerStructure)linked_rbe.b.getUserPointer()).addUserPointer(userPointer.getKey(),userPointer.getValue());
			}

			((UserPointerStructure)rbe.b.getUserPointer()).addUserPointer("linked_rbe",linked_rbe);
			((UserPointerStructure)linked_rbe.b.getUserPointer()).addUserPointer("linked_rbe",rbe);
			world2.add(linked_rbe);
		}
	}
	public void rebuildWorld2WithDuplicateStructures() {
		rebuildWorld2WithoutDuplicateStructures();
		buildDuplicateStructures(PlayerInitializer.player.portalPair.difference_inv());
	}
	protected void buildDuplicateStructures(Transform difference) {
		if(portalTunnel==null) {
			portalTunnel=new PortalTunnel();
			portalTunnel.init();
			portalTunnel.addPhysics();
			portalTunnel.setPhysicsWorld(world2);
		} else {
			world2.add(((UserPointerStructure)portalTunnel.geo.p.body.getUserPointer()).ParentRBEntryPointer);
		}
		portalTunnel.geo.p.body.setWorldTransform(PlayerInitializer.player.portalPair.p1());
		((UserPointerStructure)portalTunnel.geo.p.body.getUserPointer()).addUserPointer("portal_tunnel",(Boolean)true);
		ArrayList<RigidBodyEntry> toAdd=new ArrayList<RigidBodyEntry>();
		for(RigidBodyEntry rbe : world2.getBodies()) {
			RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
			if(!world1.getBodies().contains(lrbe)) {
				continue;
			}
			RigidBody newb=new RigidBody(getConstructionInfo(rbe.b));
			RigidBodyEntry additional_linked_rbe=new RigidBodyEntry(newb,rbe.group,rbe.mask);
			for(Entry<String,Object> userPointer : ((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().entrySet()) {
				((UserPointerStructure)additional_linked_rbe.b.getUserPointer()).addUserPointer(userPointer.getKey(),userPointer.getValue());
			}
			((UserPointerStructure)rbe.b.getUserPointer()).addUserPointer("additional_linked_rbe",additional_linked_rbe);
			((UserPointerStructure)additional_linked_rbe.b.getUserPointer()).addUserPointer("additional_linked_rbe",rbe);
			((UserPointerStructure)rbe.b.getUserPointer()).addUserPointer("other_world",(Boolean)false);
			((UserPointerStructure)additional_linked_rbe.b.getUserPointer()).addUserPointer("other_world",(Boolean)true);
			toAdd.add(additional_linked_rbe);
		}
		for(RigidBodyEntry rbe : toAdd) {
			world2.add(rbe);
		}
	}
	private Transform tr=new Transform();
	private Transform flip=null;
	private Vector3f a=new Vector3f();
	private Vector3f b=new Vector3f();
	private Matrix4f c=new Matrix4f();
	public void step_linked() {
		if(flip==null) {
			flip=new Transform(new Matrix4f(LeptonUtil.AxisAngle_np(new AxisAngle4f(0,0,1,(float)Math.toRadians(180))),new Vector3f(0,0,0),1.0f));
		}
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
			rbe.b.getWorldTransform(tr);
			rbe.b.getLinearVelocity(a);
			if(lrbe!=null) {
				RigidBodyEntry alrbe=((RigidBodyEntry)((UserPointerStructure)lrbe.b.getUserPointer()).getUserPointers().get("additional_linked_rbe"));
				PlayerInitializer.player.portalPair.difference().getMatrix(c).transform(a);
				
				tr.mul(PlayerInitializer.player.portalPair.difference(),tr);
				alrbe.b.setWorldTransform(tr);
				alrbe.b.setLinearVelocity(a);
			}
		}
	}
}

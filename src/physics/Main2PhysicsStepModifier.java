package physics;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import lepton.engine.physics.PhysicsStepModifier;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import objects.PortalPair;
import objects.Thing;
import util.Util;

public class Main2PhysicsStepModifier extends GenericPhysicsStepModifier {
	private PhysicsWorld physics;
	public void setPhysicsWorld(PhysicsWorld p) {
		physics=p;
	}
	public Main2PhysicsStepModifier(PhysicsWorld physicsWorld) {
		this.physics=physicsWorld;
	}
	@Override
	public void postStepProcessNew(HashSet<RigidBodyEntry> bodies) {
	}
	
	private static Vector3f a=new Vector3f();
	private static Transform mm=new Transform();
	@Override
	public void preStepProcessNew(HashSet<RigidBodyEntry> bodies) {
		for(RigidBodyEntry rbe : bodies) {
			Thing thing=((Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing"));
			a.set(thing.getConstForce());
			a.scale(thing.geo.p.mass);
			rbe.b.applyCentralForce(a);
		}
		ArrayList<RigidBodyEntry> totransfer=null;
		PortalPair pp=PlayerInitializer.player.portalPair;
		LinkedPhysicsWorld portalWorld=Main.portalWorld;
		if(pp.placed1 && pp.placed2) {
			for(RigidBodyEntry be : bodies) {
				RigidBody b=be.b;
				Thing bthing=((Thing)((UserPointerStructure)be.b.getUserPointer()).getUserPointers().get("thing"));
				if(!PortalPair.PASSES.contains(bthing.type)) {
					bthing.currentPortalStatus=0;
					continue;
				}
				Transform btr=b.getMotionState().getWorldTransform(mm);
				Vector3f pos=btr.origin;
				double r1=Util.distance(pos,pp.p1().origin);
				double r2=Util.distance(pos,pp.p2().origin);
				float t=pp.getShape().length()+bthing.getShape().length();
				if((r1>t && r2>t)) {
					bthing.currentPortalStatus=0;
					continue;
				}
				
				int p=(r1>r2)?2:1;
				bthing.currentPortalStatus=p;
			}
		}
		for(RigidBodyEntry rbe : bodies) {
			Thing thing=((Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing"));
			if(thing.currentPortalStatus==0 && thing.previousPortalStatus!=0) {
				totransfer=new ArrayList<RigidBodyEntry>();
				totransfer.add(rbe);
			}
			thing.previousPortalStatus=thing.currentPortalStatus;
		}
		if(totransfer!=null) {
			for(RigidBodyEntry rbe : totransfer) {
				if(physics.getBodies().contains(rbe)) {
					Thing thing=((Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing"));
//					System.out.println("HEY");
					if(thing!=null) {
						thing.setPhysicsWorld(portalWorld.getWorld1());
					} else {
						physics.remove(rbe.b);
						RigidBodyEntry alrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("additional_linked_rbe"));
						if(physics.getBodies().contains(alrbe)) {
							physics.remove(alrbe.b);
						}
						portalWorld.getWorld1().add(rbe);
					}
				}
			}
		}
		
	}

}

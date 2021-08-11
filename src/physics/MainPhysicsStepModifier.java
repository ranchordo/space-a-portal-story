package physics;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import objects.PortalPair;
import objects.Thing;
import util.Util;

public class MainPhysicsStepModifier extends GenericPhysicsStepModifier {
	private PhysicsWorld physics;
	public MainPhysicsStepModifier(PhysicsWorld physicsWorld) {
		this.physics=physicsWorld;
	}
	@Override
	public void postStepProcessNew(HashSet<RigidBodyEntry> arg0) {
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
					continue;
				}
				Transform btr=b.getMotionState().getWorldTransform(mm);
				Vector3f pos=btr.origin;
				double r1=Util.distance(pos,pp.p1.origin);
				double r2=Util.distance(pos,pp.p2.origin);
				float t=pp.getShape().length()+bthing.getShape().length();
				if((r1>t && r2>t)) {
					continue;
				}
				
				int p=(r1>r2)?2:1;
				if(p!=0) {
					pos.sub(p==1?pp.p1.origin:pp.p2.origin);
					float dot=(p==1?pp.normal1:pp.normal2).dot(pos);
					p=dot>=0?p:0;
				}
				((Thing)((UserPointerStructure)be.b.getUserPointer()).getUserPointers().get("thing")).currentPortalStatus=p;
				if(p!=0) {
					totransfer=new ArrayList<RigidBodyEntry>();
					totransfer.add(be);
				}
			}
		}
		if(totransfer!=null) {
			for(RigidBodyEntry rbe : totransfer) {
				Thing thing=((Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing"));
				if(thing!=null) {
					thing.setPhysicsWorld(portalWorld.getWorld2());
				} else {
					physics.remove(rbe.b);
					portalWorld.getWorld2().add(rbe);
				}
			}
		}
		for(RigidBodyEntry rbe : bodies) {
			Thing thing=((Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing"));
			thing.previousPortalStatus=thing.currentPortalStatus;
		}
	}

}

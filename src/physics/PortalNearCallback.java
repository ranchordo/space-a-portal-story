package physics;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphasePair;
import com.bulletphysics.collision.broadphase.DispatchFunc;
import com.bulletphysics.collision.broadphase.DispatcherInfo;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.ManifoldResult;
import com.bulletphysics.collision.dispatch.NearCallback;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.RigidBody;

import debug.ContactPoint;
import game.PlayerInitializer;
import lepton.engine.physics.UserPointerStructure;
import lepton.optim.objpoollib.PoolElement;
import objects.PortalPair;
import util.Util;

public class PortalNearCallback extends NearCallback {
	public ArrayList<PoolElement<ContactPoint>> contactPoints;
	private final ManifoldResult contactPointResult = new ManifoldResult();
	private final Vector3f a=new Vector3f();
	private final Vector3f b=new Vector3f();
	
	private static final float EPSILON=PortalPair.WALL_DISTANCE*2f;
	public static boolean removeContactPoint(Vector3f a, Vector3f b, RigidBody b1, RigidBody b2) {
		PortalPair pp=PlayerInitializer.player.portalPair;
		float r1=Util.distance(pp.p1.origin,a);
		float r2=Util.distance(pp.p2.origin,a);
		if(r1>pp.getShape().length()+2.0f && r2>pp.getShape().length()+2.0f) {
			return false;
		}
		int rayTest=(pp.rayTest(pp.p1,a,1,-1)?1:0)+(pp.rayTest(pp.p2,a,1,-1)?2:0);
		if(rayTest!=0) {
			return true;
		}
		Boolean otherWorld1=(Boolean)((UserPointerStructure)b1.getUserPointer()).getUserPointers().get("other_world");
		Boolean otherWorld2=(Boolean)((UserPointerStructure)b2.getUserPointer()).getUserPointers().get("other_world");
		
		if(!(otherWorld1==null ^ otherWorld2==null)) {
			return false;
		}
		boolean otherWorld=otherWorld1==null?otherWorld2:otherWorld1;
		a.sub(pp.p1.origin);
		b.sub(pp.p1.origin);
		float dotA=pp.normal1.dot(a);
		float dotB=pp.normal1.dot(b);
		if((dotA>EPSILON || dotB>EPSILON) && otherWorld) {
			return true;
		}
		if((dotA<-EPSILON || dotB<-EPSILON) && !otherWorld) {
			return true;
		}
		return false;
	}
	/**
	 * Portalified DefaultNearCallback
	 */
	public void handleCollision(BroadphasePair collisionPair, CollisionDispatcher dispatcher, DispatcherInfo dispatchInfo) {
		CollisionObject colObj0=(CollisionObject)collisionPair.pProxy0.clientObject;
		CollisionObject colObj1=(CollisionObject)collisionPair.pProxy1.clientObject;

		if (dispatcher.needsCollision(colObj0,colObj1)) {
			if (collisionPair.algorithm==null) {
				collisionPair.algorithm=dispatcher.findAlgorithm(colObj0,colObj1);
			}
			if (collisionPair.algorithm!=null) {
				contactPointResult.init(colObj0,colObj1);

				if (dispatchInfo.dispatchFunc==DispatchFunc.DISPATCH_DISCRETE) {
					collisionPair.algorithm.processCollision(colObj0,colObj1,dispatchInfo,contactPointResult);
					
					PersistentManifold m=contactPointResult.getPersistentManifold();
					if(m!=null) {
						for(int i=m.getNumContacts()-1;i>=0;i--) {
							ManifoldPoint point=m.getContactPoint(i);
							point.getPositionWorldOnA(a);
							point.getPositionWorldOnB(b);
							boolean r=false;
							if(removeContactPoint(a,b,(RigidBody)m.getBody0(),(RigidBody)m.getBody1())) {
								r=true;
								m.removeContactPoint(i);
							}
							if(contactPoints!=null) {
								PoolElement<ContactPoint> p=ContactPoint.contactPointPool.alloc();
								p.o().set(a,b,r);
								contactPoints.add(p);
							}
						}
					}
				} else {
					float toi=collisionPair.algorithm.calculateTimeOfImpact(colObj0,colObj1,dispatchInfo,contactPointResult);
					if (dispatchInfo.timeOfImpact>toi) {
						dispatchInfo.timeOfImpact=toi;
					}
				}
			}
		}
	}

}

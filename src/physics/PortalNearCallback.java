package physics;

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

import game.PlayerInitializer;
import objects.PortalPair;
import util.Util;

public class PortalNearCallback extends NearCallback {
	private final ManifoldResult contactPointResult = new ManifoldResult();
	private final Vector3f a=new Vector3f();
	private final Vector3f b=new Vector3f();

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
							PortalPair pp=PlayerInitializer.player.portalPair;
							float r1=Util.distance(pp.p1.origin,a);
							float r2=Util.distance(pp.p2.origin,a);
							if(r1>pp.getShape().length() && r2>pp.getShape().length()) {
								continue;
							}
							int rayTest=(pp.rayTest(pp.p1,a,1,-1)?1:0)+(pp.rayTest(pp.p2,a,1,-1)?2:0);
							if(rayTest!=0) {
								m.removeContactPoint(i);
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

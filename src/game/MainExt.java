package game;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

import java.util.ArrayList;

import javax.vecmath.Matrix4f;

import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.linearmath.Transform;

import static game.Main.*;
import debug.ContactPoint;
import debug.GenericCubeFactory;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import lepton.engine.rendering.GObject;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.util.advancedLogger.Logger;
import objects.Thing;
import physics.PortalNearCallback;
import util.Util;

public class MainExt {
	private static int pPortalBodies=0;
	public static boolean stepPortalPhysics() {
		int portalWorldSize=Main.portalWorld.getWorld2().getBodies().size();
		if(portalWorldSize>0) {
			for(RigidBodyEntry rbe : portalWorld.getWorld2().getBodies()) {
				if(((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("portal_tunnel")!=null) {
					portalWorldSize--;
				}
			}
			for(RigidBodyEntry rbe : portalWorld.getWorld1().getBodies()) {
				RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
				if(portalWorld.getWorld2().getBodies().contains(lrbe)) {
					RigidBodyEntry alrbe=((RigidBodyEntry)((UserPointerStructure)lrbe.b.getUserPointer()).getUserPointers().get("additional_linked_rbe"));
					if(alrbe!=null) {
						portalWorldSize--;
					}
					portalWorldSize--;
				}
			}
		}
		if(portalWorldSize==0 && pPortalBodies>0) {
			Logger.log(0,"Deconstructing portal physics world");
			portalWorld.clearWorld2();
			if(portalWorld.getWorld2().getBodies().size()>0) {
				Logger.log(4,"Extra weird bodies in virtual portal physics world");
			}
		}
		if(portalWorldSize>0 && pPortalBodies==0) {
			Logger.log(0,"Rebuilding portal physics world");
			portalWorld.rebuildWorld2WithDuplicateStructures();
		}
		if(dbgRenderWorld!=null) {
			if(((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback() instanceof PortalNearCallback) {
				if(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints!=null) {
					Util.clearsafe(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints);
				}
			}
		}
		pPortalBodies=portalWorldSize;
		return portalWorldSize!=0;
	}
	private static GObject genericCube=null;
	public static void renderDBGWorld() {
		if(dbgRenderWorld!=null) {
			glDisable(GL_DEPTH_TEST);
			if(genericCube==null) {
				genericCube=GenericCubeFactory.createGenericCube();
				genericCube.wireframe=true;
			}
			if(((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback() instanceof PortalNearCallback) {
				if(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints==null) {
					((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints=new ArrayList<PoolElement<ContactPoint>>();
				} else {
					for(PoolElement<ContactPoint> cp : ((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints) {
						PoolElement<Matrix4f> p=DefaultVecmathPools.matrix4f.alloc();
						PoolElement<Transform> tr=DefaultVecmathPools.transform.alloc();
						Util.clear(p.o());
						p.o().m00=0.04f;
						p.o().m11=0.04f;
						p.o().m22=0.04f;
						p.o().m33=1f;
						p.o().m03=cp.o().posA.x;
						p.o().m13=cp.o().posA.y;
						p.o().m23=cp.o().posA.z;
						tr.o().set(p.o());
						genericCube.setColor(cp.o().removed?1:0,cp.o().removed?0:1,0);
						genericCube.copyData(GObject.COLOR_DATA, GL_DYNAMIC_DRAW);
						genericCube.highRender_customTransform(tr.o());
						
						p.o().m03=cp.o().posB.x;
						p.o().m13=cp.o().posB.y;
						p.o().m23=cp.o().posB.z;
						tr.o().set(p.o());
						genericCube.setColor(cp.o().removed?1:0,cp.o().removed?0:1,1);
						genericCube.copyData(GObject.COLOR_DATA, GL_DYNAMIC_DRAW);
						genericCube.highRender_customTransform(tr.o());
						p.free();
						tr.free();
					}
				}
			}
			for(RigidBodyEntry rbe : dbgRenderWorld.getBodies()) {
				Thing thing=(Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing");
				if(thing!=null) {
					PoolElement<Matrix4f> p=DefaultVecmathPools.matrix4f.alloc();
					PoolElement<Matrix4f> p1=DefaultVecmathPools.matrix4f.alloc();
					PoolElement<Transform> tr=DefaultVecmathPools.transform.alloc();
					Util.clear(p.o());
					p.o().m00=thing.getShape().x;
					p.o().m11=thing.getShape().y;
					p.o().m22=thing.getShape().z;
					p.o().m33=1;
					rbe.b.getWorldTransform(tr.o()).getMatrix(p1.o());
					p.o().mul(p1.o(),p.o());
					tr.o().set(p.o());
					Boolean otherWorld=(Boolean)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("other_world");
					if(otherWorld!=null) {
						genericCube.setColor(1,otherWorld?1:0,otherWorld?0:1);
					} else {
						genericCube.setColor(0,1,1);
					}
					genericCube.copyData(GObject.COLOR_DATA, GL_DYNAMIC_DRAW);
					genericCube.highRender_customTransform(tr.o());
					
					p.free();
					p1.free();
					tr.free();
				}
			}
			glEnable(GL_DEPTH_TEST);
		}
	}
}

package physics;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.CollisionAlgorithm;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;

import graphics.GraphicsInit;
import graphics.RenderUtils;
import graphics.Renderer;
import logger.Logger;
import objectTypes.GObject;
import objectTypes.PhysicsObject;
import objects.Player;
import objects.PortalPair;
import objects.Thing;
import portalcasting.Segment;
import util.RigidBodyEntry;
import util.Util;

public class Physics {
	public static DiscreteDynamicsWorld dynamicsWorld;
//	private static final Vector3f[] permutations=new Vector3f[] {
//		new Vector3f(-1,-1,-1),
//		new Vector3f(-1,-1, 1),
//		new Vector3f(-1, 1,-1),
//		new Vector3f(-1, 1, 1),
//		new Vector3f( 1,-1,-1),
//		new Vector3f( 1,-1, 1),
//		new Vector3f( 1, 1,-1),
//		new Vector3f( 1, 1, 1),
//	};
	private static HashSet<RigidBodyEntry> bodies=new HashSet<RigidBodyEntry>();
	private static ArrayList<LocalRayResult> rayTest=new ArrayList<LocalRayResult>();
	private static Vector3f gravity=new Vector3f(0,-10,0);
	public static final float gravity_magnitude=10;
	private static final RayResultCallback rayResultCallback=new RayResultCallback() {
		@Override
		public float addSingleResult(LocalRayResult arg0, boolean arg1) {
			Physics.rayTest.add(arg0);
			return 0;
		}
		
	};
	public static HashSet<RigidBodyEntry> getBodies() {
		return bodies;
	}
	public static void initPhysics() {
		BroadphaseInterface broadphase=new DbvtBroadphase();
		CollisionConfiguration collisionConfiguration=new DefaultCollisionConfiguration();
		CollisionDispatcher dispatcher=new CollisionDispatcher(collisionConfiguration);
		ConstraintSolver solver=new SequentialImpulseConstraintSolver();
		dynamicsWorld=new DiscreteDynamicsWorld(dispatcher,broadphase,solver,collisionConfiguration);
		dynamicsWorld.setGravity(gravity);
		
	}
	private static Vector3f axis=new Vector3f();
	private static Vector3f segOrigin=new Vector3f();
	private static Vector3f thing_local=new Vector3f();
	private static Vector3f thingcomp=new Vector3f();
	private static Transform mm=new Transform();
	private static Vector3f linvel=new Vector3f();
	private static Vector3f linvel1=new Vector3f();
	//private static Vector3f targvel=new Vector3f();
	public static void step() {
		for(RigidBodyEntry rb : bodies) {
			((Thing)rb.b.getUserPointer()).npflag=false;
		}
		for(RigidBodyEntry be : bodies) {
			RigidBody b=be.b;
			Vector3f cf=((Thing)b.getUserPointer()).getConstForce();
			float mass=((Thing)b.getUserPointer()).geo.p.mass;
			linvel.set(cf.x*mass,cf.y*mass,cf.z*mass);
			b.applyCentralForce(linvel);
			Thing thing=((Thing)b.getUserPointer());
			thing.collisions.clear();
			thing.collisionVels.clear();
			if(thing.funnelInFunnel) {
				Segment seg=thing.funnelActiveSegment;
				axis.set(seg.b);
				axis.sub(seg.a);
				axis.normalize();
				segOrigin.set(seg.a);
				segOrigin.add(seg.b);
				segOrigin.scale(0.5f);
				thing_local.set(b.getWorldTransform(mm).origin);
				if(thing.type.equals("Player")) {
					thing_local.set(Renderer.camera.pos_out);
				}
				thing_local.sub(segOrigin);
				thingcomp.set(axis);
				thingcomp.scale(axis.dot(thing_local));
				thing_local.sub(thingcomp);
				thing_local.normalize();
				thing_local.scale(-0.5f);
				axis.scale(2);
				thing_local.add(axis);
				b.getLinearVelocity(linvel);
				thing_local.sub(linvel);
				thing_local.scale(1.5f/b.getInvMass());
				b.applyCentralForce(thing_local);
			}
		}
		int manifolds=dynamicsWorld.getDispatcher().getNumManifolds();
		for(int i=0;i<manifolds;i++) {
			PersistentManifold m=dynamicsWorld.getDispatcher().getManifoldByIndexInternal(i);
			boolean hit=false;
			for(int j=0;j<m.getNumContacts();j++) {
				if(m.getContactPoint(j).getDistance()<0.0f) {
					hit=true;
					break;
				}
			}
			if(!hit) {continue;}
			RigidBody rb1=(RigidBody)m.getBody0();
			RigidBody rb2=(RigidBody)m.getBody1();
			CollisionAlgorithm ca=dynamicsWorld.getDispatcher().findAlgorithm(rb1,rb2,m);
			//System.out.println(ca.internalGetCreateFunc());
			rb1.getLinearVelocity(linvel);
			rb2.getLinearVelocity(linvel1);
			linvel.sub(linvel1);
			linvel1.set(rb1.getMotionState().getWorldTransform(mm).origin);
			linvel1.sub(rb2.getMotionState().getWorldTransform(mm).origin);
			linvel1.normalize();
			Thing th1=(Thing)rb1.getUserPointer();
			Thing th2=(Thing)rb2.getUserPointer();
			th1.collisions.add(th2);
			th2.collisions.add(th1);
			th1.collisionVels.add(linvel.dot(linvel1));
			th2.collisionVels.add(linvel.dot(linvel1));
		}
		PortalPair pp=((Player)GraphicsInit.player).portalPair;
		boolean portalcoldbg=true;
		if(pp.placed1 && pp.placed2) {
			boolean ac1=true;
			boolean ac2=true;
			for(RigidBodyEntry be : bodies) {
				RigidBody b=be.b;
				if(!PortalPair.PASSES.contains(((Thing)b.getUserPointer()).type)) {
					continue;
				}
				Transform btr=b.getMotionState().getWorldTransform(mm);
				Thing bthing=((Thing)b.getUserPointer());
				Vector3f pos=btr.origin;
				if(bthing.type.equals("Player")) {
					pos=(Vector3f)Renderer.camera.pos_out.clone();
				}
				float boxRadiusMultiplier=bthing.type.equals("Player")?1.5f:1.0f;
				double r1=Math.sqrt(Math.pow(pos.x-pp.p1.origin.x,2)+Math.pow(pos.y-pp.p1.origin.y,2)+Math.pow(pos.z-pp.p1.origin.z,2));
				double r2=Math.sqrt(Math.pow(pos.x-pp.p2.origin.x,2)+Math.pow(pos.y-pp.p2.origin.y,2)+Math.pow(pos.z-pp.p2.origin.z,2));
				float t=pp.getShape().length()+bthing.getShape().length()+5.0f;
				if((r1>t && r2>t)) {
//					if(!bthing.portalingCollisionsEnabled) {
//						if(portalcoldbg) {Logger.log(0,"Cleared NEARPORTAL group of "+bthing.type+" from distance");}
//						bthing.portalingCollisionsEnabled=true;
//						Physics.reAdd(be,bthing.oshgroup,be.mask);
//					}
					continue;
				}
				
				float bt=Math.min(bthing.getShape().x,Math.min(bthing.getShape().y,bthing.getShape().z));
				float at=Math.min(pp.getShape().x,pp.getShape().y);
				float st=(at-2*bt)/at;
				
				Vector3f dir=(Vector3f)pp.normal1.clone();
				dir.scale(-(bthing.getShape().length())*1.2f*boxRadiusMultiplier);
				Vector3f npos_nt=new Vector3f();
				npos_nt.add(pos,dir);
				Vector3f npos_ntb=new Vector3f();
				npos_ntb.sub(pos,dir);
				int f_nt1=pp.rayTest_sc(pos,npos_nt,st);
				int f_nt2=pp.rayTest_sc(pos,npos_nt,PortalPair.CLIP_SCALE*0.9f);
				f_nt1+=pp.rayTest_sc(pos,npos_ntb,st);
				f_nt2+=pp.rayTest_sc(pos,npos_ntb,PortalPair.CLIP_SCALE*0.9f);
				
				dir=(Vector3f)pp.normal2.clone();
				dir.scale(-(bthing.getShape().length())*1.2f*boxRadiusMultiplier);
				npos_nt.add(pos,dir);
				npos_ntb.sub(pos,dir);
				int f_nt1_2=pp.rayTest_sc(pos,npos_nt,st);
				f_nt1_2+=pp.rayTest_sc(pos,npos_ntb,st);
				int f_nt2_2=pp.rayTest_sc(pos,npos_nt,PortalPair.CLIP_SCALE*0.9f);
				f_nt2_2+=pp.rayTest_sc(pos,npos_ntb,PortalPair.CLIP_SCALE*0.9f);
				//System.out.println(f_nt2);
				
				if((f_nt1+f_nt1_2)!=0) {
					bthing.npflag=true;
					if(bthing.portalingCollisionsEnabled) {
						if(portalcoldbg) {Logger.log(0,"Set NEARPORTAL group of "+bthing.type);}
						bthing.portalingCollisionsEnabled=false;
						bthing.oshgroup=be.group;
						bthing.oshmask=2;
						Physics.reAdd(be,Thing.NEARPORTAL,be.mask);
					}
				}
				if((f_nt2+f_nt2_2)==0) {
					if(!bthing.portalingCollisionsEnabled && !bthing.npflag2) {
						if(portalcoldbg) {Logger.log(0,"Cleared NEARPORTAL group of "+bthing.type);}
						bthing.portalingCollisionsEnabled=true;
						Physics.reAdd(be,bthing.oshgroup,be.mask);
					}
				} else {bthing.npflag=true;}
				if(f_nt2!=0) {
					ac1=false;
				}
				if(f_nt2_2!=0) {
					ac2=false;
				}
				
				
				Vector3f vel=(Vector3f)b.getLinearVelocity(linvel).clone();
				vel.sub(pp.attached1.geo.p.body.getLinearVelocity(linvel1));
				vel.scale(1.0f/RenderUtils.fr);
				float len=vel.length();
				vel.normalize();
				if(len<PortalPair.VEL_MAG_STOP) {
					vel.scale(PortalPair.VEL_CLIP_MULTIPLIER*len+PortalPair.CLIP_DISTANCE);
				} else {
					vel.scale(PortalPair.ALT_CLIP*len);
				}
				Vector3f npos=new Vector3f();
				npos.add(pos,vel);
				int f1=pp.rayTest_sc(pos,npos,PortalPair.CLIP_SCALE);
				
				
				vel=(Vector3f)b.getLinearVelocity(linvel).clone();
				vel.sub(pp.attached2.geo.p.body.getLinearVelocity(linvel1));
				vel.scale(1.0f/RenderUtils.fr);
				len=vel.length();
				vel.normalize();
				if(len<PortalPair.VEL_MAG_STOP) {
					vel.scale(PortalPair.VEL_CLIP_MULTIPLIER*len+PortalPair.CLIP_DISTANCE);
				} else {
					vel.scale(PortalPair.ALT_CLIP*len);
				}
				npos.add(pos,vel);
				int f2=pp.rayTest_sc(pos,npos,PortalPair.CLIP_SCALE);
				
				int f=0;
				if(f1==1||f1==3) {
					f+=1;
				}
				if(f2==2||f2==3) {
					f+=2;
				}
				
				bthing.prevPos=(Vector3f)pos.clone();
				boolean portaldbg=false;
				if(f!=0) {
					Vector3f lin_vel=(Vector3f)b.getLinearVelocity(new Vector3f()).clone();
					float dot1=lin_vel.dot(pp.normal1);
					float dot2=lin_vel.dot(pp.normal2);
					if(((Thing)b.getUserPointer()).portalCounter<=Thing.PORTAL_IMMUNITY) {
						continue;
					}
					((Thing)b.getUserPointer()).portalCounter=0;
					if(f==3) {
						float rp1=Util.distance(pp.p1.origin,pos);
						float rp2=Util.distance(pp.p2.origin,pos);
						f=(rp1<rp2)?1:2;
						Logger.log(0,"Dual portal clip, choosing "+f+" from distance.");
					}
					if(f==1?dot1>0:dot2>0) {Logger.log(0,"Cancelled portal "+f+" collision");continue;}
					Logger.log(0,"PORTAL COLLISION: "+f);
					Transform tr=b.getMotionState().getWorldTransform(new Transform());
					Transform ntr=new Transform();
					Transform mtr=f==1?pp.difference_inv():pp.difference();
					if(portaldbg) {Logger.log(0,"Transform: "+mtr.origin);
					Logger.log(0,"From this: "+tr.origin);}
					Matrix4f tr_mat=tr.getMatrix(new Matrix4f());
					tr_mat.mul(mtr.getMatrix(new Matrix4f()),tr_mat);
					ntr=new Transform(tr_mat);
					if(portaldbg) {Logger.log(0,"To this: "+ntr.origin);}
					b.setWorldTransform(ntr);
					b.getMotionState().setWorldTransform(ntr);
					lin_vel=(Vector3f)b.getLinearVelocity(new Vector3f()).clone();
					if(portaldbg) {Logger.log(0,"Linear velocity: "+lin_vel);}
					if((pp.attached1.geo.p.getTransformSource()!=PhysicsObject.PHYSICS)||(pp.attached2.geo.p.getTransformSource()!=PhysicsObject.PHYSICS)) {Logger.log(4,"Portal attachments need physics anchorings. Otherwise I'm dumb.");}
					lin_vel.sub((f==1?pp.attached1:pp.attached2).geo.p.body.getLinearVelocity(new Vector3f()));
					lin_vel.add((f==1?pp.attached2:pp.attached1).geo.p.body.getLinearVelocity(new Vector3f()));
					if(portaldbg) {Logger.log(0,"Relative linear velocity: "+lin_vel);}
					Matrix3f trs=new Matrix3f();
					mtr.getMatrix(new Matrix4f()).getRotationScale(trs);
					trs.transform(lin_vel);
					if(portaldbg) {Logger.log(0,"New linear velocity: "+lin_vel);}
					b.setLinearVelocity(lin_vel);
				}
			}
			if(PortalPair.FUNNELING_ENABLE) {
				for(RigidBodyEntry be : bodies)  {
					RigidBody b=be.b;
					if(!PortalPair.PASSES.contains(((Thing)b.getUserPointer()).type)) {
						continue;
					}
					Transform btr=b.getMotionState().getWorldTransform(new Transform());
					Thing bthing=((Thing)b.getUserPointer());
					Vector3f pos=btr.origin;
					if(bthing.type.equals("Player")) {
						pos=(Vector3f)Renderer.camera.pos_out.clone();
					}
					double r1=Math.sqrt(Math.pow(pos.x-pp.p1.origin.x,2)+Math.pow(pos.y-pp.p1.origin.y,2)+Math.pow(pos.z-pp.p1.origin.z,2));
					double r2=Math.sqrt(Math.pow(pos.x-pp.p2.origin.x,2)+Math.pow(pos.y-pp.p2.origin.y,2)+Math.pow(pos.z-pp.p2.origin.z,2));
					//float t=pp.getShape().length()+bthing.getShape().length();
					if((r1>PortalPair.FUNNELING_DIST && r2>PortalPair.FUNNELING_DIST)) {
						continue;
					}
					Vector3f dir=(Vector3f)pp.normal1.clone();
					dir.scale(-PortalPair.FUNNELING_DIST);
					Vector3f npos_tt=new Vector3f();
					npos_tt.add(pos,dir);
					int f_tt1=pp.funnelingRayTest(pos,npos_tt);
					
					dir=(Vector3f)pp.normal2.clone();
					dir.scale(-PortalPair.FUNNELING_DIST);
					npos_tt.add(pos,dir);
					int f_tt2=pp.funnelingRayTest(pos,npos_tt);
					int f_tt=0;
					Vector3f up=(Vector3f)gravity.clone();
					up.scale(-1);
					up.normalize();
					if((up.angle(pp.normal1)<=PortalPair.FUNNELING_ANGLE_THSHLD) && (f_tt1==1 || f_tt1==3)) {f_tt+=1;}
					if((up.angle(pp.normal2)<=PortalPair.FUNNELING_ANGLE_THSHLD) && (f_tt2==2 || f_tt2==3)) {f_tt+=2;}
					if(f_tt!=0) {
						if(f_tt!=1 && f_tt!=2) {
							float rp1=Util.distance(pp.p1.origin,pos);
							float rp2=Util.distance(pp.p2.origin,pos);
							f_tt=(rp1<rp2)?1:2;
						}
						Vector3f lin_vel=(Vector3f)b.getLinearVelocity(new Vector3f()).clone();
						float dot=-1.0f*lin_vel.dot(f_tt==1?pp.normal1:pp.normal2);
						if(dot>PortalPair.FUNNELING_VEL_THSHLD) {
							Vector3f force=new Vector3f();
							force.sub((f_tt==1?pp.p1:pp.p2).origin,pos);
							//force.normalize();
							//force.scale(1.0f/dist);
							force.scale(PortalPair.FUNNELING_TARG_VEL_MAG);
							force.sub(lin_vel);
							force.scale(PortalPair.FUNNELING_FRC_SCALE/b.getInvMass());
							
							dir=(Vector3f)(f_tt==1?pp.normal1:pp.normal2).clone(); //Remove normal component
							dir.scale(-1);
							dir.scale(dir.dot(force));
							force.sub(dir);
							
							b.applyCentralForce(force);
						}
					}
				}
			}
			if(pp.attached1!=null && pp.attached2!=null) {
				if(!pp.attached1.npflag2 && (pp.attached1!=pp.attached2)?ac1:(ac1 && ac2)) {
					if(!pp.attached1.portalingCollisionsEnabled) {
						pp.attached1.portalingCollisionsEnabled=true;
						RigidBodyEntry entry=entrySearch(pp.attached1.geo.p.body);
						Physics.reAdd(entry,entry.group,pp.attached1.oshmask);
						if(portalcoldbg) {Logger.log(0,"Added portal collisions to portal1 attachment: "+pp.attached1.type);}
					}
				} else {
					pp.attached1.npflag=true;
					if(pp.attached1.portalingCollisionsEnabled) {
						pp.attached1.portalingCollisionsEnabled=false;
						RigidBodyEntry entry=entrySearch(pp.attached1.geo.p.body);
						pp.attached1.oshmask=entry.mask;
						pp.attached1.oshgroup=entry.group;
						Physics.reAdd(entry,entry.group,(short) (entry.mask & Thing.AWAYPORTAL));
						if(portalcoldbg) {Logger.log(0,"Removed portal collisions from portal1 attachment: "+pp.attached1.type);}
					}
				}
				if(!pp.attached2.npflag2 && ac2 && (pp.attached1 != pp.attached2)) {
					if(!pp.attached2.portalingCollisionsEnabled) {
						pp.attached2.portalingCollisionsEnabled=true;
						RigidBodyEntry entry=entrySearch(pp.attached2.geo.p.body);
						Physics.reAdd(entry,entry.group,pp.attached2.oshmask);
						if(portalcoldbg) {Logger.log(0,"Added portal collisions to portal2 attachment: "+pp.attached2.type);}
					}
				} else if(pp.attached1!=pp.attached2) {
					pp.attached2.npflag=true;
					if(pp.attached2.portalingCollisionsEnabled) {
						pp.attached2.portalingCollisionsEnabled=false;
						RigidBodyEntry entry=entrySearch(pp.attached2.geo.p.body);
						pp.attached2.oshmask=entry.mask;
						pp.attached2.oshgroup=entry.group;
						Physics.reAdd(entry,entry.group,(short) (entry.mask & Thing.AWAYPORTAL));
						if(portalcoldbg) {Logger.log(0,"Removed portal collisions from portal2 attachment: "+pp.attached2.type);}
					}
				}
			}
		}
		for(RigidBodyEntry rb : bodies) {
			((Thing)rb.b.getUserPointer()).npflag2=false;
		}
		for(RigidBodyEntry be : bodies) {
			RigidBody b=be.b;
			Thing bthing=(Thing)b.getUserPointer();
			if(!PortalPair.PASSES.contains(bthing.type)) {continue;}
			Vector3f pos=b.getWorldTransform(mm).origin;
			boolean npflag=false;
			for(Thing thing : Renderer.things) {
				if(!thing.useModifiedCollision) {continue;}
				if(thing.runCollisionRayTest(pos)) {
					npflag=true;
					thing.npflag2=true;
				} else {
				}
			}
			if(npflag) {
				bthing.npflag2=true;
				if(bthing.portalingCollisionsEnabled) {
					if(portalcoldbg) {Logger.log(0,"Set np group of "+bthing.type);}
					bthing.portalingCollisionsEnabled=false;
					bthing.oshgroup=be.group;
					Physics.reAdd(be,Thing.NEARPORTAL,be.mask);
				}
			} else {
				if(!bthing.portalingCollisionsEnabled && !bthing.npflag) {
					bthing.portalingCollisionsEnabled=true;
					Physics.reAdd(be,bthing.oshgroup,be.mask);
					System.out.println("Cleared np group of "+bthing.type);
				}
			}
		}
		for(Thing thing : Renderer.things) {
			if(!thing.useModifiedCollision) {continue;}
			thing.setAttachedNPCollisionFlag(!thing.npflag2);
		}
		dynamicsWorld.stepSimulation(1.0f/RenderUtils.fr);
	}
	public static void add(RigidBody b, short group) {
		dynamicsWorld.addRigidBody(b,Thing.EVERYTHING,Thing.EVERYTHING);
		bodies.add(new RigidBodyEntry(b,Thing.EVERYTHING,Thing.EVERYTHING));
	}
	public static void add(RigidBody b, short group, short mask) {
		dynamicsWorld.addRigidBody(b,group,mask);
		bodies.add(new RigidBodyEntry(b,group,mask));
	}
	public static void remove(RigidBody b) {
		if(b==null) {
			Logger.log(2,"Removing null body.");
			System.exit(1);
		}
		try {
			dynamicsWorld.removeRigidBody(b);
		} catch (NullPointerException e) {
			Logger.log(4, "Concave-static to concave-static collision, NPE on freeCollisionAlgorithm.", e);
		}
		for(RigidBodyEntry be : bodies) {
			if(be.b==b) {
				bodies.remove(be);
				break;
			}
		}
	}
	public static RigidBodyEntry entrySearch(RigidBody b) {
		for(RigidBodyEntry be : bodies) {
			if(be.b==b) {
				return be;
			}
		}
		return null;
	}
	public static void reAdd(RigidBody b, short group, short mask) {
		dynamicsWorld.removeRigidBody(b);
		dynamicsWorld.addRigidBody(b,group,mask);
		RigidBodyEntry entry=entrySearch(b);
		if(entry!=null) {
			entry.group=group;
			entry.mask=mask;
		}
	}
	public static void reAdd(RigidBodyEntry b, short group, short mask) {
		dynamicsWorld.removeRigidBody(b.b);
		dynamicsWorld.addRigidBody(b.b,group,mask);
		b.group=group;
		b.mask=mask;
	}
	public static void wake() {
		for(RigidBodyEntry be : bodies) {
			be.b.setActivationState(CollisionObject.ACTIVE_TAG);
		}
	}
	public static Vector3f getGravity() {
		return (Vector3f)gravity.clone();
	}
	public static void setGravity(Vector3f gravity) {
		Physics.gravity=gravity;
		dynamicsWorld.setGravity(gravity);
		for(RigidBodyEntry be : bodies) {
			((Thing)be.b.getUserPointer()).refreshGravity();
		}
	}
}

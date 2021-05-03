package objects;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import anim.AnimParser;
import anim.AnimTrack;
import objectTypes.GObject;
import objectTypes.PhysicsObject;
import objectTypes.WorldObject;
import physics.Physics;
import util.RigidBodyEntry;

public class Door extends Thing {
	private static final long serialVersionUID = 6359925412640608823L;
	public static final float COLLISION_DISTANCE=2.0f;
	private transient WorldObject doorr;
	private transient WorldObject doorl;
	private transient WorldObject doorcr;
	private transient WorldObject doorcl;
	private transient WorldObject mask;
	private Vector3f origin;
	private Quat4f quat;
	protected Vector3f maskNormal;
	@Thing.SerializeByID
	public transient Thing attached;
	public boolean useActivation=false;
	public Door(Vector3f origin, Quat4f quat, boolean useActivation) {
		this.shape=new Vector3f();
		this.type="Exit_door";
		this.origin=origin;
		this.quat=quat;
		this.useModifiedCollision=true;
		this.initIdField();
		this.useActivation=useActivation;
	}
	private boolean pGotActivation=false;
	private transient Vector3f a;
	private transient Vector3f b;
	@Override
	public void setAttachedNPCollisionFlag(boolean in) {
		if(in!=portalingCollisionsEnabled && (!useActivation || pGotActivation)) {
			portalingCollisionsEnabled=in;
			if(in) {
				//add collisions
				setAllReversed(in);
				System.out.println("Add collisions to exit door attachment");
				attached.portalingCollisionsEnabled=true;
				RigidBodyEntry entry=Physics.entrySearch(attached.geo.p.body);
				Physics.reAdd(entry,entry.group,attached.oshmask);
			} else {
				//remove collisions
				setAllReversed(in);
				System.out.println("Remove collisions from exit door attachment");
				attached.portalingCollisionsEnabled=false;
				RigidBodyEntry entry=Physics.entrySearch(attached.geo.p.body);
				attached.oshmask=entry.mask;
				Physics.reAdd(entry,entry.group,(short) (entry.mask & Thing.AWAYPORTAL));
			}
		}
	}
	@Override
	public boolean runCollisionRayTest(Vector3f pos) {
		if(a==null) {a=new Vector3f();}
		if(b==null) {b=new Vector3f();}
		a.set(maskNormal);
		b.set(maskNormal);
		a.scale(COLLISION_DISTANCE);
		b.scale(-COLLISION_DISTANCE);
		a.add(pos);
		b.add(pos);
		boolean ret=mask.g.rayTest(a,b,mask.p.getTransform());
		return ret;
	}
	transient WorldObject[] list;
	public void setAttached(Thing thing) {
		attached=thing;
	}
	private void setAllReversed(boolean in) {
		if(list==null) {
			list=new WorldObject[] {doorr,doorl,doorcr,doorcl};
		}
		for(WorldObject g : list) {
			g.p.animator.getTrack().setReversed(in);
		}
	}
	@Override
	public void logic() {
		stepWorldObject(doorr);
		stepWorldObject(doorl);
		stepWorldObject(doorcr);
		stepWorldObject(doorcl);
		pGotActivation=(this.activations>=this.activationThreshold);
		useModifiedCollision=true;//pGotActivation;
	}
	private void stepWorldObject(WorldObject in) {
		in.p.animator.synchronizedTransform.set(in.p.transformObject.getTransform());
		in.p.animator.advanceTrack();
	}
	private void initWorldObject(WorldObject in, String name) {
		in.g.loadOBJ("exitdoor/"+name);
		in.g.useTex=false;
		in.g.lock();
		in.g.initVBO();
		in.g.refresh();
		in.p.setMotionSource(PhysicsObject.ANIMATION);
		in.p.animator.add("Open",AnimParser.parse("3d/exitdoor/"+name).setEndMode(AnimTrack.STAY));
		in.p.transformObject=this.geo.p;
		in.p.animator.synchronizedTransform=new Transform();
		in.p.animator.copyTransformPointer();
		in.p.animator.setActiveAnim("Open");
		in.p.animator.getTrack().setReversed(true);
		//in.animator.getTrack().setReversed(true);
	}
	@Override
	public void render() {
		geo.highRender();
		doorr.highRender();
		doorl.highRender();
		doorcr.highRender();
		doorcl.highRender();
		mask.highRender();
	}
	@Override
	public void initPhysics() {
		this.geo.p.mass=0;
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_mesh(this.geo.g,origin,quat);
		this.geo.p.doBody(body);
		Matrix3f rs=new Matrix3f();
		this.geo.p.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
		maskNormal=new Vector3f(1,0,0);
		rs.transform(maskNormal);
	}
	@Override
	public void initGeo() {
		this.geo=new WorldObject(true);
		geo.g.loadOBJ("exitdoor/arch");
		geo.g.useTex=false;
		geo.g.setColor(0.3f,0.3f,0.3f);
		geo.g.lock();
		
		mask=new WorldObject(true);
		mask.g.loadOBJ("exitdoor/wallmask");
		mask.g.useTex=false;
		mask.g.lock();
		mask.p.setMotionSource(PhysicsObject.OTHER);
		mask.p.transformObject=this.geo.p;
		mask.g.setColor(1,0,1,0);
		mask.g.initVBO();
		mask.g.refresh();
		
		doorr=new WorldObject(true);
		doorl=new WorldObject(true);
		doorcr=new WorldObject(true);
		doorcl=new WorldObject(true);
		initWorldObject(doorr,"doorr");
		initWorldObject(doorl,"doorl");
		initWorldObject(doorcr,"doorcr");
		initWorldObject(doorcl,"doorcl");
		doorr.g.setColor(1.0f,0.6f,0.6f);
		doorl.g.setColor(0.6f,0.6f,1.0f);
		doorcr.g.setColor(1.0f,0.6f,0.6f);
		doorcl.g.setColor(0.6f,0.6f,1.0f);
		doorr.g.refresh();
		doorl.g.refresh();
		doorcr.g.refresh();
		doorcl.g.refresh();
		declareResource(doorr);
		declareResource(doorl);
		declareResource(doorcr);
		declareResource(doorcl);
		declareResource(mask);
	}
}

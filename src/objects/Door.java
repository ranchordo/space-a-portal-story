package objects;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import anim.AnimParser;
import anim.AnimTrack;
import graphics.GObject;
import physics.Physics;
import util.RigidBodyEntry;

public class Door extends Thing {
	private static final long serialVersionUID = 6359925412640608823L;
	public static final float COLLISION_DISTANCE=2.0f;
	private transient GObject doorr;
	private transient GObject doorl;
	private transient GObject doorcr;
	private transient GObject doorcl;
	private transient GObject mask;
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
				RigidBodyEntry entry=Physics.entrySearch(attached.geo.body);
				Physics.reAdd(entry,entry.group,attached.oshmask);
			} else {
				//remove collisions
				setAllReversed(in);
				System.out.println("Remove collisions from exit door attachment");
				attached.portalingCollisionsEnabled=false;
				RigidBodyEntry entry=Physics.entrySearch(attached.geo.body);
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
		boolean ret=mask.rayTest(a,b,mask.getTransform());
		return ret;
	}
	transient GObject[] list;
	public void setAttached(Thing thing) {
		attached=thing;
	}
	private void setAllReversed(boolean in) {
		if(list==null) {
			list=new GObject[] {doorr,doorl,doorcr,doorcl};
		}
		for(GObject g : list) {
			g.animator.getTrack().setReversed(in);
		}
	}
	@Override
	public void logic() {
		stepGObject(doorr);
		stepGObject(doorl);
		stepGObject(doorcr);
		stepGObject(doorcl);
		pGotActivation=(this.activations>=this.activationThreshold);
		useModifiedCollision=true;//pGotActivation;
	}
	private void stepGObject(GObject in) {
		in.animator.synchronizedTransform.set(in.transformObject.getTransform());
		in.animator.advanceTrack();
	}
	private void initGObject(GObject in, String name) {
		in.loadOBJ("exitdoor/"+name);
		in.useTex=false;
		in.lock();
		in.initVBO();
		in.refresh();
		in.setMotionSource(GObject.ANIMATION);
		in.animator.add("Open",AnimParser.parse("3d/exitdoor/"+name).setEndMode(AnimTrack.STAY));
		in.transformObject=this.geo;
		in.animator.synchronizedTransform=new Transform();
		in.animator.copyTransformPointer();
		in.animator.setActiveAnim("Open");
		in.animator.getTrack().setReversed(true);
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
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
		Matrix3f rs=new Matrix3f();
		this.geo.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
		maskNormal=new Vector3f(1,0,0);
		rs.transform(maskNormal);
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		geo.loadOBJ("exitdoor/arch");
		geo.useTex=false;
		geo.setColor(0.3f,0.3f,0.3f);
		geo.lock();
		
		mask=new GObject();
		mask.loadOBJ("exitdoor/wallmask");
		mask.useTex=false;
		mask.lock();
		mask.setMotionSource(GObject.OTHER);
		mask.transformObject=this.geo;
		mask.setColor(1,0,1,0);
		mask.initVBO();
		mask.refresh();
		
		doorr=new GObject();
		doorl=new GObject();
		doorcr=new GObject();
		doorcl=new GObject();
		initGObject(doorr,"doorr");
		initGObject(doorl,"doorl");
		initGObject(doorcr,"doorcr");
		initGObject(doorcl,"doorcl");
		doorr.setColor(1.0f,0.6f,0.6f);
		doorl.setColor(0.6f,0.6f,1.0f);
		doorcr.setColor(1.0f,0.6f,0.6f);
		doorcl.setColor(0.6f,0.6f,1.0f);
		doorr.refresh();
		doorl.refresh();
		doorcr.refresh();
		doorcl.refresh();
		declareResource(doorr);
		declareResource(doorl);
		declareResource(doorcr);
		declareResource(doorcl);
		declareResource(mask);
	}
}

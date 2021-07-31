package objects;
import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import game.Main;
import static game.Main.in;
import game.SaveState;
import lepton.engine.physics.WorldObject;
import lepton.engine.rendering.Tri;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class Player extends Thing {
	private static final long serialVersionUID = 1458724178644370708L;
	public static final float height=1.4f;
	public static final float floor_thshld=0.05f;
	private boolean inGodMode=false;
	private int rayTest=0;
	public boolean onFloor=false;
	private Vector3f origin;
	private Quat4f quat;
	public transient PortalPair portalPair;
	public boolean bindToObject=true;
	transient Vector3f currDown=null;
	
	public Player(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f(0.25f,height/2.0f,0.2f);
		this.type="Player";
		this.origin=origin;
		this.quat=quat;
		this.portalsIgnore=true;
	}
	public void handleNewSSPP(PortalPair pp) {
		this.portalPair=pp;
	}
	public void setRotY(float angrad) {
		this.geo.p.body.setWorldTransform(new Transform(new Matrix4f(
				LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,angrad))),
				this.geo.p.body.getWorldTransform(new Transform()).origin,1.0f)));
	}
	public void rotY(float angrad) {
		Matrix4f mat=this.geo.p.body.getWorldTransform(new Transform()).getMatrix(new Matrix4f());
		Matrix4f transmat=new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,angrad))),new Vector3f(0,0,0),1.0f);
		transmat.mul(mat,transmat);
		this.geo.p.body.setWorldTransform(new Transform(transmat));
	}
	public void transform(Matrix4f tr) {
		Matrix4f mat=this.geo.p.body.getWorldTransform(new Transform()).getMatrix(new Matrix4f());
		Matrix4f transmat=(Matrix4f)tr.clone();
		transmat.mul(mat,transmat);
		this.geo.p.body.setWorldTransform(new Transform(transmat));
	}
	public void transform_world(Matrix4f tr) {
		Matrix4f mat=this.geo.p.body.getWorldTransform(new Transform()).getMatrix(new Matrix4f());
		Matrix4f transmat=(Matrix4f)tr.clone();
		transmat.mul(transmat,mat);
		this.geo.p.body.setWorldTransform(new Transform(transmat));
	}
	private transient Transform b;
	private transient Vector3f c;
	private transient Vector3f d;
	public void updateOnFloor() {
		if(b==null) {b=new Transform();}
		if(c==null) {c=new Vector3f();}
		if(d==null) {d=new Vector3f();}
		RayResultCallback f=new RayResultCallback() {
			@Override
			public float addSingleResult(LocalRayResult res, boolean b) {
				Player.this.rayTest++;
				return 0;
			}
			
		};
		rayTest=0;
		Vector3f pos=this.geo.p.body.getWorldTransform(new Transform()).origin;
		c.set(
				pos.x+(height/2)*currDown.x*(1-floor_thshld),
				pos.y+(height/2)*currDown.y*(1-floor_thshld),
				pos.z+(height/2)*currDown.z*(1-floor_thshld));
		d.set(
				pos.x+(height/2)*currDown.x*(1+floor_thshld),
				pos.y+(height/2)*currDown.y*(1+floor_thshld),
				pos.z+(height/2)*currDown.z*(1+floor_thshld));
		getPhysicsWorld().dynamicsWorld.rayTest(c,d,f);
		if(rayTest>0) {
			onFloor=true;
		} else {
			onFloor=false;
		}
	}
	public boolean isInGodMode() {
		return inGodMode;
	}
	public void setGodMode(boolean godmode) {
		inGodMode=godmode;
		if(godmode) {
			getPhysicsWorld().reAdd(this.geo.p.body,Thing.NOTHING,Thing.NOTHING);
		} else {
			getPhysicsWorld().reAdd(this.geo.p.body,Thing.PLAYER ,Thing.PLAYER );
		}
		this.setUsesGravity(!godmode);
	}
	private Vector3f[] perms=new Vector3f[6];
	private Matrix4f trmat=new Matrix4f();
	private transient Transform tr;
	private void placePortal(int portal) {
		if(tr==null) {tr=new Transform();}
		if(perms[0]==null) {
			for(int i=0;i<6;i++) {
				perms[i]=new Vector3f();
			}
		}
		Vector3f pt=new Vector3f(0,0,-1);
		Matrix3f rs=new Matrix3f();
		Main.camera.getInvTransform().getMatrix(trmat).getRotationScale(rs);
		tr=geo.p.getTransform();
		perms[0].set(shape.x,0,0);
		perms[1].set(-shape.x,0,0);
		perms[2].set(0,shape.y,0);
		perms[3].set(0,-shape.y,0);
		perms[4].set(0,0,shape.z);
		perms[5].set(0,0,-shape.z);
		for(Vector3f p : perms) {
			p.scale(0.8f);
			tr.transform(p);
		}
		int g=0;
		for(int i=0;i<2;i++) {
			g+=portalPair.rayTest_sc(perms[2*i],perms[(2*i)+1],PortalPair.CLIP_SCALE);
		}
		if(g>0) {
			Logger.log(0,"Placement of portal "+portal+" was cancelled.");
			return;
		}
		rs.invert();
		rs.transform(pt);
		Vector3f targ_world=(Vector3f)Main.camera.pos_out.clone();
		targ_world.add(pt);
		Tri cdt=null;
		Thing cdt_thing=null;
		float minDistance=Tri.CLIP_DISTANCE+2;
		for(Thing thing : Main.things) {
			if(thing.geo==null) {continue;}
			if(thing.geo.p.body==null) {continue;}
			if(thing.portalsIgnore) {continue;}
			//float f=thing.geo.rayTest_distance(Renderer.camera.pos_out,targ_world,thing.geo.body.getMotionState().getWorldTransform(new Transform()));
			Tri t=thing.geo.g.rayTest_closest(Main.camera.pos_out,targ_world,thing.geo.p.body.getMotionState().getWorldTransform(new Transform()));
			if(t!=null) {
				float f=t.raytest_t;
				if(f>=0-1e-6) {
					if(f<minDistance) {
						minDistance=f;
						cdt=t;
						cdt_thing=thing;
					}
				}
			}
		}
		if(cdt!=null && cdt_thing.portalable) {
			Vector3f intersection=(Vector3f)cdt.raytest_intersection.clone();
			Vector3f localNormal=(Vector3f)cdt_thing.geo.g.vmap.normals.get(cdt.normals[0]).clone();
			Matrix3f normTransform=new Matrix3f();
			cdt_thing.geo.p.body.getMotionState().getWorldTransform(new Transform()).getMatrix(new Matrix4f()).getRotationScale(normTransform);
			localNormal.normalize(); //Just making sure...
			normTransform.transform(localNormal);
			Vector3f normOffset=(Vector3f)localNormal.clone();
			normOffset.scale(PortalPair.WALL_DISTANCE);
			intersection.add(normOffset);
			Vector3f inter_axis=new Vector3f();
			Vector3f portalNormal=new Vector3f(0,0,1);
			inter_axis.cross(localNormal,portalNormal);
			inter_axis.normalize();
			float angrad=localNormal.angle(portalNormal);
			if(angrad<=1e-6) {
				inter_axis=new Vector3f(1,0,0);
				//angrad+=(float)Math.PI;
			} else if(Math.abs(angrad-Math.PI)<1e-6) {
				inter_axis=new Vector3f(0,1,0);
				//angrad+=(float)Math.PI;
			}
			angrad+=(float)Math.PI;
//			intersection.scale(cdt.raytest_t);
//			intersection.add(Renderer.camera.pos_out);
//			Logger.log(0,"Fire Portal "+portal);
			Transform newtrans=new Transform(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(inter_axis.x,inter_axis.y,inter_axis.z,-angrad))),intersection,1.0f));
			if(portal==1) {
				portalPair.placed1=true;
				portalPair.p1=newtrans;
				if(portalPair.attached1!=null) {
					portalPair.attached1.lastPortalCheckin1=null;
//					portalPair.attached1.npflag=false;
//					portalPair.attached1.npflag2=false;
//					portalPair.attached1.portalingCollisionsEnabled=true;
//					Physics.reAdd(portalPair.attached1.geo.body, portalPair.attached1.oshgroup, portalPair.attached1.oshmask);
				}
				portalPair.attached1=cdt_thing;
			} else if(portal==2) {
				portalPair.placed2=true;
				portalPair.p2=newtrans;
				if(portalPair.attached2!=null) {
					portalPair.attached2.lastPortalCheckin2=null;
//					portalPair.attached2.npflag=false;
//					portalPair.attached2.npflag2=false;
//					portalPair.attached2.portalingCollisionsEnabled=true;
//					Physics.reAdd(portalPair.attached2.geo.body, portalPair.attached2.oshgroup, portalPair.attached2.oshmask);
				}
				portalPair.attached2=cdt_thing;
			}
			
		} else if(cdt_thing!=null) {
			if(!cdt_thing.portalable) {
				Logger.log(0,"Portal firing cancelled due to a stopsPortals-enabled surface. ("+portal+")");
			}
		}
	}
	private Matrix4f mat=new Matrix4f();
	private Matrix3f mat1=new Matrix3f();
	private Vector3f inter_axis=new Vector3f();
	private Matrix4f pendingtransform=new Matrix4f();
	private Vector3f nothing=new Vector3f();
	private Vector3f linvel=new Vector3f();
	private Matrix4f inverseTransform=new Matrix4f();
	public Matrix4f getInverseTransform() {return inverseTransform;}
	@Override
	public void logic() {
		geo.p.body.getLinearVelocity(linvel);
		linvel.set(linvel.x,0,linvel.z);
		//System.out.println(linvel.length());
		//System.out.println(npflag+"1, "+npflag2+"2, "+portalingCollisionsEnabled+"col");
		//if(inverseTransform==null) {inverseTransform=new Matrix4f();}
		if(in.mr(GLFW_MOUSE_BUTTON_LEFT)) {
			placePortal(1);
		}
		if(in.mr(GLFW_MOUSE_BUTTON_RIGHT)) {
			placePortal(2);
		}
		
		if(game.Main.isDesigner) {
			if(in.ir(GLFW_KEY_GRAVE_ACCENT)) {
				setGodMode(!inGodMode);
			}
		}
		if(in.ir(GLFW_KEY_T)) {
			SaveState teststate=new SaveState();
			teststate.create();
			teststate.output();
		}
		if(in.ir(GLFW_KEY_L)) {
			SaveState testIn=SaveState.input();
			Main.scheduledReplacement=testIn;
		}
		this.geo.p.body.getMotionState().getWorldTransform(new Transform()).getMatrix(mat);
		mat.getRotationScale(mat1);
		currDown.set(0,-1,0);
		mat1.transform(currDown);
		//currDown=(Vector3f)Physics.getGravity().clone();
		currDown.normalize();
		float grav_angle=getPhysicsWorld().getGravity().angle(currDown);
		inter_axis.cross(getPhysicsWorld().getGravity(),this.currDown);
		if(grav_angle==0) {
			inter_axis.set(0,-1,0);
		}
		inter_axis.normalize();
		mat1.invert();
		mat1.transform(inter_axis);
		pendingtransform.set(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(inter_axis.x,inter_axis.y,inter_axis.z,-grav_angle*0.02f))),nothing,1.0f);
		//Matrix4f pendingtransform=new Matrix4f(LeptonUtil.AxisAngle(new AxisAngle4f(1,0,0,0.01f)),new Vector3f(0,0,0),1.0f);
		this.transform(pendingtransform);
		this.geo.p.getTransform().getMatrix(inverseTransform);
		inverseTransform.invert();
		//System.out.println(inverseTransform);
		portalCounter++;
	}
	public Vector3f getCurrDown() {
		return currDown;
	}
	private static transient Transform a;
	public Vector3f getCameraPositioning() {
		if(a==null) {a=new Transform();}
		Vector3f center=this.geo.p.body.getWorldTransform(a).origin;
		Vector3f currUp=(Vector3f)currDown.clone();
		currUp.normalize();
		currUp.scale(-(height/2.0f)*0.8f);
//		currUp.scale(0.0f);
		currUp.add(center);
		return currUp;
	}
	@Override
	public void render() {}
	@Override
	public void alphaRender() {
		if(Main.activePortalTransform==0) {
			return;
		}
		//this.geo.highRender();
	}
	@Override
	public void initPhysics() {
		this.geo.p.mass=60;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_shape(s, origin, quat);
		body.restitution=0f;
		body.friction=0.1f;
		body.angularDamping=0;
		body.linearDamping=0.16f;
		this.geo.p.doBody(body);
		this.geo.p.body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		this.geo.p.body.setAngularFactor(0);
		this.currDown=new Vector3f();
		this.currDown.set(getPhysicsWorld().getGravity());
	}
	@Override
	public void initGeo() {
		initGeo_nopp();
		this.portalPair=new PortalPair();
		this.portalPair.init();
	}
	public void initGeo_nopp() {
		this.geo=new WorldObject(true);
		this.geo.g.useTex=false;
		this.geo.g.useCulling=false;
		this.portalable=false;
		this.geo.g.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,-getShape().z));
		
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,+getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,+getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,+getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,+getShape().z));
		
		this.geo.g.vmap.normals=new ArrayList<Vector3f>();
		this.geo.g.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.g.vmap.normals.add(new Vector3f(0,0,1));
		this.geo.g.vmap.normals.add(new Vector3f(0,-1,0));
		this.geo.g.vmap.normals.add(new Vector3f(0,1,0));
		this.geo.g.vmap.normals.add(new Vector3f(-1,0,0));
		this.geo.g.vmap.normals.add(new Vector3f(1,0,0));
		this.geo.g.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.g.vmap.texcoords.add(new Vector2f(0,1));
		this.geo.g.vmap.texcoords.add(new Vector2f(1,1));
		this.geo.g.vmap.texcoords.add(new Vector2f(1,0));
		this.geo.g.vmap.texcoords.add(new Vector2f(0,0));
		this.geo.g.clearTris();
		this.geo.g.addTri(new Tri(0,1,2, 0,0,0));
		this.geo.g.addTri(new Tri(2,3,0, 0,0,0));
		
		this.geo.g.addTri(new Tri(4,5,6, 1,1,1));
		this.geo.g.addTri(new Tri(6,7,4, 1,1,1));
		
		this.geo.g.addTri(new Tri(3,2,6, 3,3,3));
		this.geo.g.addTri(new Tri(6,7,3, 3,3,3));
        
		this.geo.g.addTri(new Tri(0,1,5, 2,2,2));
		this.geo.g.addTri(new Tri(5,4,0, 2,2,2));
		
		this.geo.g.addTri(new Tri(1,5,6, 5,5,5));
		this.geo.g.addTri(new Tri(6,2,1, 5,5,5));
		
		this.geo.g.addTri(new Tri(0,4,7, 4,4,4));
		this.geo.g.addTri(new Tri(7,3,0, 4,4,4));
		
		this.geo.g.setColor(0.7f,0.0f,0.7f,0.2f);
		
		geo.g.lock();
		geo.p.restrictNonPhysics();
	}
}

package objects;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.GraphicsInit;
import graphics.Renderer;
import logger.Logger;
import physics.Physics;
import util.Util;

public class Turret extends Thing {
	private static final long serialVersionUID = 4966615532982569610L;
	public static final float SHOOTING_ANGLE_THSHLD=(float)Math.toRadians(40.0f);
	public static final float TIPPING_ANGLE_THSHLD=(float)Math.toRadians(50.0f);
	private Vector3f origin;
	private Quat4f quat;
	private boolean pickedUp=false;
	private boolean e_rise=true;
	private boolean p_picked=false;
	/*
	 * 0 - normal cube
	 * 1 - companion cube
	 * 2 - laser cube
	 */
	Matrix3f gravrot=new Matrix3f();
	@Override
	public void processInteraction() {
		if(this.in.i(GLFW_KEY_E)) {
			if(e_rise) {
				e_rise=false;
				if(this.interacted || pickedUp) {pickedUp=!pickedUp;}
			}
		} else {e_rise=true;}
		if(this.pickedUp) {
			if(!p_picked) {
				//First frame we are picked up
				Physics.reAdd(this.geo.body,Thing.EVERYTHING,Thing.NPLAYER);
				this.portalingCollisionsEnabled=true;
			}
			this.geo.body.setActivationState(CollisionObject.ACTIVE_TAG);
			Vector3f pt=new Vector3f(0,0,-1);
			Matrix3f rs=new Matrix3f();
			Matrix4f ttr=new Matrix4f();
			Transform tr=new Transform();
			Renderer.camera.getInvTransform().getMatrix(ttr).getRotationScale(rs);
			rs.invert();
			rs.transform(pt);
			PortalPair pp=((Player)GraphicsInit.player).portalPair;
			Vector3f force=(Vector3f)Renderer.camera.pos_out.clone();
			//Vector3f campos=new Vector3f(force);
			force.add(pt);
			Vector3f gravrel=Physics.getGravity();
			Vector3f gravnorm=new Vector3f();
			gravrel.normalize();
			gravnorm.set(gravrel);
			gravrel.scale(gravrel.dot(pt));
			pt.sub(gravrel);
			pt.normalize();
			Vector3f perpgrav=Physics.getGravity();
			gravrot.transform(perpgrav);
			perpgrav.normalize();
			//force is the location of the target in world space.
			tr=geo.getTransform();
			Vector3f pos=tr.origin;
			tr.getMatrix(ttr);
			Vector3f c=new Vector3f();
			c.cross(pt,perpgrav);
			float dotComp=pt.dot(perpgrav);
			float crossComp=gravnorm.dot(c);
			ttr.set(Util.AxisAngle_np(new AxisAngle4f(Physics.getGravity(),(float)Math.atan2(dotComp,crossComp))),tr.origin,1.0f);
			this.geo.body.setWorldTransform(new Transform(ttr));
			this.geo.body.setAngularVelocity(new Vector3f(0,0,0));
			
			Vector3f targcdt2=(Vector3f)force.clone();
			Vector3f targcdt3=(Vector3f)force.clone();
			pp.difference_inv().transform(targcdt2);
			pp.difference().transform(targcdt3);
			
			float r1=Util.distance(pos,force);
			float r2=Util.distance(pos,targcdt2);
			float r3=Util.distance(pos,targcdt3);
			
			force=new Vector3f[] {force,targcdt2,targcdt3}[Util.indexMin(r1,r2,r3)];
			
			force.sub(pos);
			if(force.length()>=2.0f) {
				Logger.log(1,"Player moved too far away from a stuck turret, turret was dropped.");
				this.pickedUp=false;
			}
			force.scale(10.0f);
			force.sub(this.geo.body.getLinearVelocity(new Vector3f()));
			force.scale(100);
			this.geo.body.applyCentralForce(force);
			p_picked=true;
		} else {
			if(p_picked) {
				//First frame we are put down
				Physics.reAdd(this.geo.body,Thing.EVERYTHING,Thing.EVERYTHING);
				this.portalingCollisionsEnabled=true;
			}
			p_picked=false;
		}
	}
	public Turret(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f(0.54f*0.5f,1.48f*0.5f,0.875f*0.5f);
		this.type="Turret";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		gravrot.set(Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90))));
	}
	@Override
	public void initPhysics() {
		this.geo.mass=7;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.initPhysics_shape(s, origin, quat);
		body.restitution=0f;
		body.friction=0.74f;
		body.angularDamping=0.0f;
		body.linearDamping=0.16f;
		this.geo.doBody(body);
	}
	private boolean active=true;
	private Vector3f currDown=new Vector3f();
	private Vector3f local_player_pos=new Vector3f();
	private transient Transform tr;
	private Vector3f mean_end=new Vector3f();
	private Matrix4f trmat=new Matrix4f();
	private Matrix3f trmatrs=new Matrix3f();
	private boolean shooting=false;
	@Override
	public void logic() {
		if(!active) {return;}
		if(tr==null) {tr=new Transform();}
		tr=GraphicsInit.player.geo.getTransform();
		local_player_pos.set(tr.origin);
		tr=this.geo.getTransform();
		local_player_pos.sub(tr.origin);
		mean_end.set(-1,0,0);
		currDown.set(0,-1,0);
		tr.getMatrix(trmat);
		trmat.getRotationScale(trmatrs);
		trmatrs.transform(mean_end);
		trmatrs.transform(currDown);
		if(currDown.angle(Physics.getGravity())>TIPPING_ANGLE_THSHLD) {
			active=false;
			geo.setColor(1,0,0);
			geo.copyData(GObject.COLOR_DATA,GL_STATIC_DRAW);
			Logger.log(0,"Turret disabled by rotation limits.");
		}
		if(mean_end.angle(local_player_pos)<SHOOTING_ANGLE_THSHLD && active) {
			if(!shooting) {
				shooting=true;
				geo.setColor(0,0,1);
				geo.copyData(GObject.COLOR_DATA,GL_STATIC_DRAW);
			}
		} else {
			if(shooting) {
				shooting=false;
				if(active) {geo.setColor(1,1,1);}
				else {geo.setColor(1,0,0);}
				geo.copyData(GObject.COLOR_DATA,GL_STATIC_DRAW);
			}
		}
	}
	@Override
	public void render() {
		if(!this.geo.hasAlpha) {
			this.geo.highRender();
		}
	}
	@Override
	public void alphaRender() {
		if(this.geo.hasAlpha) {
			this.geo.highRender();
		}
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("turret/turret");
		//this.geo.scale(0.27f*(getShape().x/0.31f));
		this.geo.setColor(1,1,1);
		this.geo.lock();
		
		this.geo.setMaterial(0.1f,256.0f,0,0);
	}
//	public void initGeo_old() {
//		this.geo=new GObject();
//		//this.geo.vmap.tex=walltex;
//		this.geo.useTex=false;
//		this.geo.vmap.vertices=new ArrayList<Vector3f>();
//		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,-getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,-getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,-getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,-getShape().z));
//		
//		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,+getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,+getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,+getShape().z));
//		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,+getShape().z));
//		
//		this.geo.vmap.normals=new ArrayList<Vector3f>();
//		this.geo.vmap.normals.add(new Vector3f(0,0,-1));
//		this.geo.vmap.normals.add(new Vector3f(0,0,1));
//		this.geo.vmap.normals.add(new Vector3f(0,-1,0));
//		this.geo.vmap.normals.add(new Vector3f(0,1,0));
//		this.geo.vmap.normals.add(new Vector3f(-1,0,0));
//		this.geo.vmap.normals.add(new Vector3f(1,0,0));
//		this.geo.vmap.texcoords=new ArrayList<Vector2f>();
//		this.geo.vmap.texcoords.add(new Vector2f(0,1));
//		this.geo.vmap.texcoords.add(new Vector2f(1,1));
//		this.geo.vmap.texcoords.add(new Vector2f(1,0));
//		this.geo.vmap.texcoords.add(new Vector2f(0,0));
//		this.geo.tris=new ArrayList<Tri>();
//		this.geo.tris.add(new Tri(0,1,2, 0,0,0));
//		this.geo.tris.add(new Tri(2,3,0, 0,0,0));
//		
//		this.geo.tris.add(new Tri(4,5,6, 1,1,1));
//		this.geo.tris.add(new Tri(6,7,4, 1,1,1));
//		
//		this.geo.tris.add(new Tri(3,2,6, 3,3,3));
//		this.geo.tris.add(new Tri(6,7,3, 3,3,3));
//
//		this.geo.tris.add(new Tri(0,1,5, 2,2,2));
//		this.geo.tris.add(new Tri(5,4,0, 2,2,2));
//		
//		this.geo.tris.add(new Tri(1,5,6, 5,5,5));
//		this.geo.tris.add(new Tri(6,2,1, 5,5,5));
//		
//		this.geo.tris.add(new Tri(0,4,7, 4,4,4));
//		this.geo.tris.add(new Tri(7,3,0, 4,4,4));
//		
//		this.geo.setColor(0.0f,0.7f,0.0f);
//	}
}

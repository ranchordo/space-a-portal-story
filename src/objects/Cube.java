package objects;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

import java.util.Arrays;
import java.util.List;

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

import anim.AnimParser;
import anim.AnimTrack;
import graphics.GObject;
import graphics.GraphicsInit;
import graphics.Renderer;
import logger.Logger;
import physics.Physics;
import util.Util;

public class Cube extends Thing {
	public static final int NORMAL=0;
	public static final int LASER=2;
	public static final int COMPANION=1;
	private static final long serialVersionUID = 4966615532982569610L;
	public static final List<String> DOES_NOT_CLANK=Arrays.asList(new String[] {"Player"});
	public static final float side_len=0.62f;
	private Vector3f origin;
	private Quat4f quat;
	private boolean pickedUp=false;
	private boolean e_rise=true;
	private boolean p_picked=false;
	public int cubeType=0;
	transient GObject ggeo;
	transient GObject alphaGeo;
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
			tr=this.geo.getTransform();
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
				Logger.log(1,"Player moved too far away from a stuck cube, cube was dropped.");
				this.pickedUp=false;
			}
			force.scale(10.0f);
			force.sub(this.geo.body.getLinearVelocity(new Vector3f()));
			force.scale(700);
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
	public Cube(Vector3f origin, Quat4f quat, int type) {
		this.shape=new Vector3f(side_len/2.0f,side_len/2.0f,side_len/2.0f);
		this.type="Cube";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		cubeType=type;
		gravrot.set(Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90))));
	}
	@Override
	public void initPhysics() {
		this.geo.mass=2.7f*0.642f*6.0f*(float)Math.pow(side_len*100.0f,2)*0.001f;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.initPhysics_shape(s, origin, quat);
		body.restitution=0f;
		body.friction=0.74f;
		body.angularDamping=0.0f;
		body.linearDamping=0.16f;
		this.geo.doBody(body);
		this.geo.body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		//tr=new Transform();
		//this.gravity=false;
	}
	//private transient Transform tr;
	@Override
	public void render() {
		if(!this.ggeo.hasAlpha) {
			this.ggeo.highRender();
		} else {
			throw new IllegalStateException("Cube.ggeo needs no alpha.");
		}
	}
	@Override
	public void alphaRender() {
		if(alphaGeo==null) {return;}
		glDepthMask(false);
		this.alphaGeo.highRender();
		glDepthMask(true);
	}
	int counter=0;
	@Override
	public void logic() {
		//ggeo.animator.advanceTrack();
		for(int i=0;i<collisions.size();i++) {
			Thing thing=collisions.get(i);
			if(DOES_NOT_CLANK.contains(thing.type)) {continue;}
			if(!pcollisions.contains(thing)) {
				float v=Math.abs(collisionVels.get(i));
				if(v>0) {
					sourcePool.play("Clank",soundtrack);
					sourcePool.getPlaying("Clank").o().setGain(v);
				}
			}
		}
		pcollisions.clear();
		for(int i=0;i<collisions.size();i++) {
			pcollisions.add(collisions.get(i));
		}
		portalCounter++;
	}
	@Override
	public void initSoundtrack() {
		soundtrack.put("Clank","cube/clank");
	}
	@Override
	public void initGeo() {
		this.ggeo=new GObject();
		this.geo=new GObject();
		this.geo.loadOBJ("cube/cubecollgeo");
		this.geo.scale(0.27f*(getShape().x/0.31f));
		this.geo.setColor(1,0,1);
		
		this.geo.lock();
		if(cubeType==Cube.NORMAL) {
			this.ggeo.loadOBJ("cube/cube");
			this.ggeo.setMaterial(0.10f,32,0,0);
		} else if(cubeType==Cube.COMPANION) {
			this.ggeo.loadOBJ("cube/cube","3d/cube/cube_comp.png");
			this.ggeo.setMaterial(0.10f,32,0,0);
		} else if(cubeType==Cube.LASER) {
			this.alphaGeo=new GObject();
			this.alphaGeo.loadOBJ("cube/laserlenses","3d/cube/lasercube.png");
			this.alphaGeo.lock();
			this.alphaGeo.useTex=true;
			this.alphaGeo.setColor(1,1,1);
			this.alphaGeo.scale(0.27f*(getShape().x/0.31f));
			alphaGeo.initVBO();
			alphaGeo.refresh();
			this.ggeo.loadOBJ("cube/lasercube");
		}
		this.ggeo.useTex=true;
		this.ggeo.scale(0.27f*(getShape().x/0.31f));
		this.ggeo.setColor(1,1,1);
		ggeo.lock();
		ggeo.initVBO();
		ggeo.refresh();
//		ggeo.animator.add("Main", AnimParser.parse("3d/testcoords").setEndMode(AnimTrack.LOOP));
		ggeo.transformObject=this.geo;
		ggeo.setMotionSource(GObject.OTHER);
//		ggeo.setMotionSource(GObject.ANIMATION);
//		ggeo.animator.setActiveAnim("Main");
		declareResource(ggeo);
		declareResource(alphaGeo);
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

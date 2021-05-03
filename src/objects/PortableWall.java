package objects;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import anim.AnimParser;
import anim.AnimTrack;
import graphics.GraphicsInit;
import graphics.Renderer;
import graphics.Tri;
import logger.Logger;
import objectTypes.GObject;
import objectTypes.WorldObject;
import physics.Physics;
import util.Util;

public class PortableWall extends Thing {
	private static final long serialVersionUID = 4966615532982569610L;
	public static final List<String> DOES_NOT_CLANK=Arrays.asList(new String[] {"Player"});
	private Vector3f origin;
	private Quat4f quat;
	private boolean pickedUp=false;
	private boolean e_rise=true;
	private boolean p_picked=false;
	public Vector2f aspect=new Vector2f(1,1);
	public Vector2f texOffset=new Vector2f(0,0);
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
				Physics.reAdd(this.geo.p.body,Thing.EVERYTHING,Thing.NPLAYER);
				this.portalingCollisionsEnabled=true;
			}
			this.geo.p.body.setActivationState(CollisionObject.ACTIVE_TAG);
			Vector3f pt=new Vector3f(0,0,-2);
			Matrix3f rs=new Matrix3f();
			Matrix4f ttr=new Matrix4f();
			Transform tr=new Transform();
			Renderer.camera.getInvTransform().getMatrix(ttr).getRotationScale(rs);
			rs.invert();
			rs.transform(pt);
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
			tr=this.geo.p.getTransform();
			Vector3f pos=tr.origin;
			tr.getMatrix(ttr);
			Vector3f c=new Vector3f();
			c.cross(pt,perpgrav);
			float dotComp=pt.dot(perpgrav);
			float crossComp=gravnorm.dot(c);
			ttr.set(Util.AxisAngle_np(new AxisAngle4f(Physics.getGravity(),(float)Math.atan2(dotComp,crossComp))),tr.origin,1.0f);
			this.geo.p.body.setWorldTransform(new Transform(ttr));
			this.geo.p.body.setAngularVelocity(new Vector3f(0,0,0));

			force.sub(pos);
			if(force.length()>=4.0f) {
				Logger.log(1,"Player moved too far away from a stuck portable wall, it was dropped.");
				this.pickedUp=false;
			}
			force.scale(10.0f);
			force.sub(this.geo.p.body.getLinearVelocity(new Vector3f()));
			force.scale(700);
			this.geo.p.body.applyCentralForce(force);
			p_picked=true;
		} else {
			if(p_picked) {
				//First frame we are put down
				Physics.reAdd(this.geo.p.body,Thing.EVERYTHING,Thing.EVERYTHING);
				this.portalingCollisionsEnabled=true;
			}
			p_picked=false;
		}
	}
	public PortableWall(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f(0.1f,PortalPair.PORTAL_HEIGHT,PortalPair.PORTAL_WIDTH);
		this.type="Portable_wall";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		gravrot.set(Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90))));
		this.portalable=true;
	}
	@Override
	public void initPhysics() {
		this.geo.p.mass=10;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_shape(s, origin, quat);
		body.restitution=0f;
		body.friction=0.74f;
		body.angularDamping=0.0f;
		body.linearDamping=0.16f;
		this.geo.p.doBody(body);
		this.geo.p.body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		//tr=new Transform();
		//this.gravity=false;
	}
	//private transient Transform tr;
	@Override
	public void render() {
		if(!this.geo.g.hasAlpha) {
			this.geo.highRender();
		} else {
			throw new IllegalStateException("PortableWall.geo needs no alpha.");
		}
	}
	@Override
	public void alphaRender() {
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
		this.geo=new WorldObject(true);
		this.geo.g.useTex=false;
		this.geo.g.useBump=false;
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
		this.geo.g.vmap.normals.add(new Vector3f(0,0, 1));
		this.geo.g.vmap.normals.add(new Vector3f(0,-1,0));
		this.geo.g.vmap.normals.add(new Vector3f(0, 1,0));
		this.geo.g.vmap.normals.add(new Vector3f(-1,0,0));
		this.geo.g.vmap.normals.add(new Vector3f( 1,0,0));
		this.geo.g.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x                 ,texOffset.y+shape.y/aspect.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x+shape.x/aspect.x,texOffset.y+shape.y/aspect.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x+shape.x/aspect.x,texOffset.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x                 ,texOffset.y));
		this.geo.g.clearTris();
		this.geo.g.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.geo.g.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		
		this.geo.g.addTri(new Tri(4,5,6, 1,1,1).setTexCoords(3,2,1));
		this.geo.g.addTri(new Tri(6,7,4, 1,1,1).setTexCoords(1,0,3));
		
		this.geo.g.addTri(new Tri(2,3,6, 3,3,3).setTexCoords(3,2,1));
		this.geo.g.addTri(new Tri(7,6,3, 3,3,3).setTexCoords(1,0,3));
        
		this.geo.g.addTri(new Tri(0,1,5, 2,2,2).setTexCoords(3,2,1));
		this.geo.g.addTri(new Tri(5,4,0, 2,2,2).setTexCoords(1,0,3));
        
		this.geo.g.addTri(new Tri(5,1,6, 5,5,5).setTexCoords(3,2,1));
		this.geo.g.addTri(new Tri(2,6,1, 5,5,5).setTexCoords(1,0,3));
        
		this.geo.g.addTri(new Tri(0,4,7, 4,4,4).setTexCoords(3,2,1));
		this.geo.g.addTri(new Tri(7,3,0, 4,4,4).setTexCoords(1,0,3));
		
		this.geo.g.lock();
		
		this.geo.g.setColor(1.2f,1.2f,1.2f);
		this.geo.g.setMaterial(0.008f,16f,0,0);
        
		this.geo.g.vmap.tex.colorLoaded=true;
		try {
			this.geo.g.loadTexture("3d/wall/cropped_border.jpg");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

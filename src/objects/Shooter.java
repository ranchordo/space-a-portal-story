package objects;

import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL46.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.Renderer;
import util.Util;

public class Shooter extends Thing {
	public static final List<String> ACTIVATES=Arrays.asList(new String[] {"Cube", "Player"});
	private static final long serialVersionUID = -6341205524711175965L;
	private Vector3f origin;
	private Quat4f quat;
	private transient GObject p1;
	private transient GObject p2;
	private transient GObject p3;
	
	private transient GObject bounce;
	
	private Pellet activePellet=null;
	
	public boolean killCurrentPellet=true;
	public boolean isGravityPelletEmitter=false;
	
	public long lifetime=0;
	private float open=0;
	
	private float centralImpulse=6.0f;
	
	public Shooter(Vector3f origin, Quat4f quat, long lifetime, boolean isGravityPelletEmitter) {
		this.isGravityPelletEmitter=isGravityPelletEmitter;
		this.lifetime=lifetime;
		this.shape=new Vector3f();
		this.type="Pellet_shooter";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	@Override
	public void initVBO() {
		geo.initVBO();
		p1.initVBO();
		p2.initVBO();
		p3.initVBO();
	}
	@Override
	public void refresh() {
		geo.refresh();
		p1.refresh();
		p2.refresh();
		p3.refresh();
	}
	@Override
	public void render() {
		glPushMatrix();
		this.geo.highRender_noPushPop();
		
		glTranslatef(0,0,open);
		this.p1.render();
		glTranslatef(0,0,-open);
		glRotatef(120,0,1,0);
		glTranslatef(0,0,open);
		this.p2.render();
		glTranslatef(0,0,-open);
		glRotatef(120,0,1,0);
		glTranslatef(0,0,open);
		this.p3.render();
		glPopMatrix();
	}
//	@Override
//	public void onSerialization() {
//		this.init();
//		if(doPhysicsOnSerialization) {
//			this.addPhysics();
//		}
//	}
	public void addPellet() {
		if(activePellet!=null && killCurrentPellet) {activePellet.die();}
		Vector3f dir=new Vector3f(0,1f,0);
		Matrix4f trans=new Matrix4f(quat,new Vector3f(0,0,0),1.0f);
		trans.transform(dir);
		Thing pellet=new Pellet(new Vector3f(origin.x+dir.x,origin.y+dir.y,origin.z+dir.z), Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,0))),lifetime,isGravityPelletEmitter);
		pellet.init();
		pellet.addPhysics();
		dir.normalize();
		dir.scale(centralImpulse);
		pellet.geo.body.applyCentralImpulse(dir);
		Renderer.addSched.add(pellet);
		activePellet=(Pellet)pellet;
	}
	public Shooter setCentralImpulse(float i) {
		this.centralImpulse=i;
		return this;
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		body.restitution=0f;
		this.geo.doBody(body);
		
		this.bounce.mass=0;
		RigidBodyConstructionInfo bounceBody=this.bounce.initPhysics_mesh(origin,quat);
		bounceBody.restitution=1f;
		this.bounce.doBody(bounceBody);
	}
	@Override
	public void processActivation() {
		this.sendingActivations=false;
	}
	private int pactivations=0;
	@Override
	public void logic() {
		if(activations>=activationThreshold && pactivations<activationThreshold) {
			addPellet();
		}
		float targOpen=0;
		if(activations>=activationThreshold) {
			targOpen=0.15f;
		}
		open+=(targOpen-open)*0.1f;
		pactivations=activations;
		portalCounter++;
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("shooter/base");
		this.geo.useTex=false;
		if(!isGravityPelletEmitter) {this.geo.setColor(0.7f, 0.7f, 0.75f);}
		else {this.geo.setColor(0.2f,0.2f,1f);}
		geo.lock();
		
		this.p1=new GObject();
		this.p1.loadOBJ("shooter/piece");
		this.p1.setColor(0.3f,0.3f,0.35f);
		p1.lock();
		
		this.p2=new GObject();
		this.p2.loadOBJ("shooter/piece");
		this.p2.setColor(0.3f,0.3f,0.35f);
		p2.lock();
		
		this.p3=new GObject();
		this.p3.loadOBJ("shooter/piece");
		this.p3.setColor(0.3f,0.3f,0.35f);
		p3.lock();
		
		this.bounce=new GObject();
		this.bounce.loadOBJ("shooter/bounce");
		this.bounce.setColor(0.3f,0.3f,0.35f);
		bounce.lock();
		
		declareResource(p1);
		declareResource(p2);
		declareResource(p3);
		declareResource(bounce);
	}
	@Override
	public void addPhysics() {
		this.addPhysics(Thing.TESTGEO,Thing.EVERYTHING);
	}
	@Override
	public void addPhysics(short group, short mask) {
		this.geo.addToSimulation(group,mask);
		this.bounce.addToSimulation(group,mask);
		this.geo.body.setUserPointer((Thing)this);
		this.bounce.body.setUserPointer((Thing)this);
		//addCube();
	}
}

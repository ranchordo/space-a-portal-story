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

import graphics.Renderer;
import objectTypes.GObject;
import objectTypes.WorldObject;
import util.Util;

public class Dropper extends Thing {
	public static final List<String> ACTIVATES=Arrays.asList(new String[] {"Cube", "Player"});
	private static final long serialVersionUID = -6341205524711175965L;
	private Vector3f origin;
	private Quat4f quat;
	private transient WorldObject p1;
	private transient WorldObject p2;
	private transient WorldObject p3;
	private transient WorldObject stop;
	
	private float open=0;
	private int cubeType;
	
	public Dropper(Vector3f origin, Quat4f quat, int ct) {
		this.shape=new Vector3f();
		this.type="Dropper";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		cubeType=ct;
	}
	@Override
	public void initVBO() {
		geo.g.initVBO();
		p1.g.initVBO();
		p2.g.initVBO();
		p3.g.initVBO();
	}
	@Override
	public void refresh() {
		geo.g.refresh();
		p1.g.refresh();
		p2.g.refresh();
		p3.g.refresh();
	}
	@Override
	public void render() {
		glPushMatrix();
		geo.highRender();
		
		glTranslatef(0,0,open);
		this.p1.g.render();
		glTranslatef(0,0,-open);
		glRotatef(120,0,1,0);
		glTranslatef(0,0,open);
		this.p2.g.render();
		glTranslatef(0,0,-open);
		glRotatef(120,0,1,0);
		glTranslatef(0,0,open);
		this.p3.g.render();
		glPopMatrix();
	}
//	@Override
//	public void onSerialization() {
//		this.init();
//		if(doPhysicsOnSerialization) {
//			this.addPhysics();
//		}
//	}
	public void addCube() {
		Thing cube=new Cube(new Vector3f(origin.x,origin.y+1,origin.z), Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,0))),cubeType);
		cube.init();
		cube.addPhysics();
		Renderer.addSched.add(cube);
	}
	@Override
	public void initPhysics() {
		this.geo.p.mass=0;
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_mesh(this.geo.g,origin,quat);
		body.restitution=1.0f;
		this.geo.p.doBody(body);
		
		this.stop.p.mass=0;
		RigidBodyConstructionInfo stopBody=this.stop.p.initPhysics_mesh(this.geo.g,origin,quat);
		this.stop.p.doBody(stopBody);
	}
	@Override
	public void processActivation() {
		this.sendingActivations=false;
	}
	private int pactivations=0;
	@Override
	public void logic() {
		if(activations>=activationThreshold && pactivations<activationThreshold) {
			stop.p.removePhysics();
		}
		if(activations<activationThreshold && pactivations>=activationThreshold) {
			stop.p.addToSimulation(Thing.TESTGEO,Thing.EVERYTHING);
			addCube();
		}
		float targOpen=0;
		if(activations>=activationThreshold) {
			targOpen=0.5f;
		}
		open+=(targOpen-open)*0.1f;
		pactivations=activations;
		portalCounter++;
	}
	@Override
	public void initGeo() {
		this.geo=new WorldObject(true);
		this.geo.g.loadOBJ("dropper/cylinder");
		this.geo.g.useTex=false;
		this.geo.g.setColor(0.7f, 0.7f, 0.75f);
		geo.g.lock();
		
		this.p1=new WorldObject(true,false);
		this.p1.g.loadOBJ("dropper/piece");
		this.p1.g.setColor(0.3f,0.3f,0.35f);
		p1.g.lock();
		
		this.p2=new WorldObject(true,false);
		this.p2.g.loadOBJ("dropper/piece");
		this.p2.g.setColor(0.3f,0.3f,0.35f);
		p2.g.lock();
		
		this.p3=new WorldObject(true,false);
		this.p3.g.loadOBJ("dropper/piece");
		this.p3.g.setColor(0.3f,0.3f,0.35f);
		p3.g.lock();
		
		this.stop=new WorldObject(true);
		this.stop.g.loadOBJ("dropper/stop");
		this.stop.g.setColor(1, 0, 1);
		stop.g.lock();
		
		declareResource(p1);
		declareResource(p2);
		declareResource(p3);
		declareResource(stop);
	}
	@Override
	public void addPhysics() {
		this.addPhysics(Thing.TESTGEO,Thing.EVERYTHING);
	}
	@Override
	public void addPhysics(short group, short mask) {
		this.geo.p.addToSimulation(group,mask);
		this.stop.p.addToSimulation(group,mask);
		this.geo.p.body.setUserPointer((Thing)this);
		this.stop.p.body.setUserPointer((Thing)this);
		addCube();
	}
}

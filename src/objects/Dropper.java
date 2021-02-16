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

public class Dropper extends Thing {
	public static final List<String> ACTIVATES=Arrays.asList(new String[] {"Cube", "Player"});
	private static final long serialVersionUID = -6341205524711175965L;
	private Vector3f origin;
	private Quat4f quat;
	private transient GObject p1;
	private transient GObject p2;
	private transient GObject p3;
	private transient GObject stop;
	
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
		geo.highRender_noPushPop();
		
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
	public void addCube() {
		Thing cube=new Cube(new Vector3f(origin.x,origin.y+1,origin.z), Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,0))),cubeType);
		cube.init();
		cube.addPhysics();
		Renderer.addSched.add(cube);
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		body.restitution=1.0f;
		this.geo.doBody(body);
		
		this.stop.mass=0;
		RigidBodyConstructionInfo stopBody=this.stop.initPhysics_mesh(origin,quat);
		this.stop.doBody(stopBody);
	}
	@Override
	public void processActivation() {
		this.sendingActivations=false;
	}
	private int pactivations=0;
	@Override
	public void logic() {
		if(activations>=activationThreshold && pactivations<activationThreshold) {
			stop.removePhysics();
		}
		if(activations<activationThreshold && pactivations>=activationThreshold) {
			stop.addToSimulation(Thing.TESTGEO,Thing.EVERYTHING);
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
		this.geo=new GObject();
		this.geo.loadOBJ("dropper/cylinder");
		this.geo.useTex=false;
		this.geo.setColor(0.7f, 0.7f, 0.75f);
		geo.lock();
		
		this.p1=new GObject();
		this.p1.loadOBJ("dropper/piece");
		this.p1.setColor(0.3f,0.3f,0.35f);
		p1.lock();
		
		this.p2=new GObject();
		this.p2.loadOBJ("dropper/piece");
		this.p2.setColor(0.3f,0.3f,0.35f);
		p2.lock();
		
		this.p3=new GObject();
		this.p3.loadOBJ("dropper/piece");
		this.p3.setColor(0.3f,0.3f,0.35f);
		p3.lock();
		
		this.stop=new GObject();
		this.stop.loadOBJ("dropper/stop");
		this.stop.setColor(1, 0, 1);
		stop.lock();
		
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
		this.geo.addToSimulation(group,mask);
		this.stop.addToSimulation(group,mask);
		this.geo.body.setUserPointer((Thing)this);
		this.stop.body.setUserPointer((Thing)this);
		addCube();
	}
}

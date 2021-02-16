package objects;

import static org.lwjgl.opengl.GL46.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.Renderer;
import lighting.Light;
import lighting.Lighting;
import physics.Physics;
import util.Util;

public class FloorButton extends Thing {
	public static final List<String> ACTIVATES=Arrays.asList(new String[] {"Cube", "Player", "Turret"});
	private static final long serialVersionUID = -6341205524711175965L;
	private Vector3f origin;
	private Quat4f quat;
	private transient GObject button;
	
	private transient Light buttonLight=null;
	
	private float buttonDepress=0;
	
	private transient ArrayList<LocalRayResult> rayTest=new ArrayList<LocalRayResult>();
	private transient RayResultCallback f=new RayResultCallback() {
		@Override
		public float addSingleResult(LocalRayResult res, boolean b) {
			FloorButton.this.rayTest.add(res);
			return 0;
		}
		
	};
	public FloorButton(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Floor_button";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	@Override
	public void initVBO() {
		geo.initVBO();
		button.initVBO();
	}
	@Override
	public void refresh() {
		geo.refresh();
		button.refresh();
	}
	@Override
	public void render() {
		glPushMatrix();
		this.geo.highRender_noPushPop();
		glTranslatef(0,buttonDepress,0);
		Renderer.activeShader.setUniform1f("useTextures", 0);
		this.button.render();
		glPopMatrix();
	}
	@Override
	protected void onSerializationAdditional() {
		this.f=new RayResultCallback() {
			@Override
			public float addSingleResult(LocalRayResult res, boolean b) {
				FloorButton.this.rayTest.add(res);
				return 0;
			}
			
		};
		this.rayTest=new ArrayList<LocalRayResult>();
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
		
		
		Vector3f up=new Vector3f(0,1,0);
		Matrix3f rs=new Matrix3f();
		this.geo.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
		rs.transform(up);
		buttonLight=new Light(Light.LIGHT_POSITION,origin.x-(0.2f*up.x),origin.y-(0.2f*up.x),origin.z-(0.2f*up.x), 0.3f,0.1f,0.1f,1);
	}
	@Override
	public void processActivation() {
		this.sendingActivations=false;
		this.rayTest=new ArrayList<LocalRayResult>();
		Vector3f up=new Vector3f(0,1,0);
		Matrix3f rs=new Matrix3f();
		this.geo.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
		rs.transform(up);
		Physics.dynamicsWorld.rayTest(new Vector3f(origin.x-(0.5f*up.x),origin.y-(0.5f*up.y),origin.z-(0.5f*up.z)), new Vector3f(origin.x,0.2f+origin.y,origin.z), f);
		for(LocalRayResult p : this.rayTest) {
			if(ACTIVATES.contains(((Thing)p.collisionObject.getUserPointer()).type)) {
				this.sendingActivations=true;
			}
		}
		
	}
	private boolean pactivations=false;
	@Override
	public void logic() {
		float targetState=0.1f;
		if(this.sendingActivations) {
			targetState=0.0f;
			this.button.setColor(0.8f, 0.2f, 0.2f);
		} else {
			this.button.setColor(0.6f, 0.2f, 0.2f);
		}
		if(this.sendingActivations!=this.pactivations) {
			this.button.copyData(GObject.COLOR_DATA,GL_STATIC_DRAW);
			if(this.sendingActivations) {
				Lighting.addLight(buttonLight);
			} else {
				Lighting.removeLight(buttonLight);
			}
		}
		buttonDepress+=(targetState-buttonDepress)*0.1f;
		portalCounter++;
		pactivations=this.sendingActivations;
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("floorbutton/base");
		this.geo.useTex=true;
		this.geo.setColor(1,1,1);
		geo.lock();
		
		this.button=new GObject();
		this.button.loadOBJ("floorbutton/button");
		this.button.setColor(0.6f,0.2f,0.2f);
		button.lock();
		
		declareResource(button);
	}
	@Override
	public void addPhysics() {
		this.geo.addToSimulation(Thing.EVERYTHING,Thing.EVERYTHING);
		//this.button.addToSimulation(Thing.EVERYTHING,Thing.EVERYTHING);
		this.geo.body.setUserPointer((Thing)this);
		//this.button.body.setUserPointer((Thing)this);
	}
	@Override
	public void addPhysics(short group, short mask) {
		this.geo.addToSimulation(group,mask);
		//this.button.addToSimulation(group, mask);
		this.geo.body.setUserPointer((Thing)this);
		//this.button.body.setUserPointer((Thing)this);
	}
}

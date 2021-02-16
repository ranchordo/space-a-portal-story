package objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.GraphicsInit;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Tri;
import graphics.VertexMap;
import lighting.Light;
import lighting.Lighting;
import logger.Logger;
import physics.Physics;
import util.RigidBodyEntry;

public class Pellet extends Thing {
	public static final List<String> GRAV_COLLISIONS=Arrays.asList(new String[] {"Wall"});
	public static final float PELLET_INTENSITY=0.1f;
	public static final float PELLET_COLOR_MULTIPLIER=1.7f;
	private Vector3f origin;
	private Quat4f quat;
	public boolean isGravityPellet=false;
	public float radius=0.1f;
	private long startTime=0;
	private long life;
	private Light light;
	private static final long serialVersionUID = 1L;
	public Pellet(Vector3f origin, Quat4f quat, long life, boolean isGravityPellet) {
		this.isGravityPellet=isGravityPellet;
		this.life=life;
		this.shape=new Vector3f(radius,radius,radius);
		this.type="Pellet";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	private void generateLight() {
		if(isGravityPellet) {
			light=new Light(Light.LIGHT_POSITION,0,0,0, 0,0,PELLET_INTENSITY,1);
		} else {
			light=new Light(Light.LIGHT_POSITION,0,0,0, PELLET_INTENSITY,PELLET_INTENSITY,0,1);
		}
	}
	@Override
	public void initPhysics() {
		this.geo.mass=1;
		CollisionShape s=new SphereShape(radius);
		RigidBodyConstructionInfo body=this.geo.initPhysics_shape(s, origin, quat);
		body.restitution=1;
		body.friction=0.74f;
		body.angularDamping=0.3f;
		this.geo.doBody(body);
		this.geo.body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		this.gravity=false;
		startTime=RenderUtils.millis();
		generateLight();
		Lighting.addLight(light);
	}
	protected void die() {
		this.geo.removePhysics();
		Renderer.remSched.add(this);
		Lighting.removeLight(light);
		light=null;
	}
	@Override
	public void logic() {
		Vector3f pos=geo.getTransform().origin;
		if(light!=null) {
			light.prop=pos;
			Lighting.updateUniforms(light);
		}
		if(isGravityPellet) {
			Vector3f vel=(Vector3f)geo.body.getLinearVelocity(new Vector3f()).clone();
			vel.normalize();
			ArrayList<Thing> physics_collisions=new ArrayList<Thing>();
			for(RigidBodyEntry be : Physics.getBodies()) {
				if(GRAV_COLLISIONS.contains(((Thing)be.b.getUserPointer()).type)) {
					physics_collisions.add((Thing)be.b.getUserPointer());
				}
			}
//			vel=(Vector3f)geo.body.getLinearVelocity(new Vector3f()).clone();
			vel.scale(0.5f);
			Vector3f npos=new Vector3f(
					pos.x+vel.x,
					pos.y+vel.y,
					pos.z+vel.z);
			PortalPair pp=((Player)GraphicsInit.player).portalPair;
			if(pp.placed1 && pp.placed2) {
				if(pp.rayTest(pos,npos)!=0) {
					physics_collisions=new ArrayList<Thing>();
					Logger.log(0,"GRAV PELLET: Portal detected. Disabling collisions.");
				}
			}
			ArrayList<Tri> total_tris=new ArrayList<Tri>();
			for(Thing t : physics_collisions) {
				if(t.geo==null) {continue;}
				ArrayList<Tri> intersecting=t.geo.rayTest_list(pos,npos,t.geo.getTransform());
				for(Tri tr : intersecting) {
					tr.userPointer=t;
				}
				total_tris.addAll(intersecting);
			}
			Tri winner=null;
			if(total_tris.size()==1) {
				winner=total_tris.get(0);
			}
			if(total_tris.size()>1) {
				double min_t=99999;
				for(Tri t : total_tris) {
					if(t.raytest_t<min_t) {
						min_t=t.raytest_t;
						winner=t;
					}
				}
			}
			if(winner!=null) {
				VertexMap vmap_up=((Thing)winner.userPointer).geo.vmap;
				Vector3f pre_tr=vmap_up.normals.get(winner.normals[0]);
				Vector3f tr=new Vector3f(pre_tr.x,pre_tr.y,pre_tr.z);
				Matrix3f rot_sc=new Matrix3f();
				((Thing)winner.userPointer).geo.getTransform().getMatrix(new Matrix4f()).getRotationScale(rot_sc);
				rot_sc.transform(tr);
				tr.normalize();
				Logger.log(0,"Gravity pellet collision with "+((Thing)winner.userPointer).type+", normal is "+tr);
				tr.scale(-1.0f*Physics.gravity_magnitude);
				Physics.setGravity(tr);
				die();
			}
		}
		if(life!=-1) {
			if(RenderUtils.millis()-startTime>life) {
				die();
			}
		}
		portalCounter++;
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("pellet/pellet");
		this.geo.useTex=false;
		this.geo.scale(radius);
		this.geo.setColor(1*PELLET_COLOR_MULTIPLIER,1*PELLET_COLOR_MULTIPLIER,0.05f*PELLET_COLOR_MULTIPLIER);
		if(isGravityPellet) {
			this.geo.setColor(0.05f*PELLET_COLOR_MULTIPLIER,0.1f*PELLET_COLOR_MULTIPLIER,1f*PELLET_COLOR_MULTIPLIER);
		}
		geo.useLighting=false;
		geo.lock();
	}
	public Light getLight() {
		return light;
	}
	
}

package objects;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import objectTypes.GObject;

public class LaserReceiver extends Thing {
	private static final long serialVersionUID = 1237435512380248141L;
	private Quat4f quat;
	private Vector3f origin;
	public LaserReceiver(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Laser_Receiver";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		this.pcasterSendOnHit=true;
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
	}
	@Override
	public void render() {
		this.geo.highRender();
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("laser_emitter/laser_emitter");
		this.geo.useTex=false;
		this.geo.setColor(0.9f,0.3f,0.31f);
		geo.lock();
	}

}

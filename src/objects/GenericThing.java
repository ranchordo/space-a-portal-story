package objects;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import objectTypes.GObject;

public class GenericThing extends Thing {
	private static final long serialVersionUID = 6058389346063051060L;
	String fname;
	Quat4f rot;
	Vector3f pos;
	public GenericThing(String fname, Vector3f pos, Quat4f rot) {
		this.fname=fname;
		this.type="Generic";
		this.rot=rot;
		this.pos=pos;
	}
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ(fname);
		this.geo.setColor(1, 1, 1);
		geo.lock();
	}
	public void initPhysics() {
		//CollisionShape sphere=new SphereShape(3.0f);
		this.geo.mass=1;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(pos,rot);
		this.geo.doBody(body);
	}
}

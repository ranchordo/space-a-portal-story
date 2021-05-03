package objectTypes;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

import anim.Animator;
import logger.Logger;
import physics.Physics;
import util.OBJReturnObject;

public class PhysicsObject {
	public static final int PHYSICS=0;
	public static final int ANIMATION=1;
	public static final int NONE=2;
	public static final int OTHER=3;
	public static final int VARIABLE=4;
	
	public RigidBody body;
	public Animator animator=new Animator();
	public PhysicsObject transformObject;
	public Transform transformVariable;
	
	private Vector3f inertia=new Vector3f(0,0,0);
	public float mass=1;
	public boolean dynamic=false;
	
	private boolean rnp=false;
	public void restrictNonPhysics() {rnp=true;}
	private int transformSource=PHYSICS;
	public void setMotionSource(int motionSource) {
		if(!rnp) {
			this.transformSource=motionSource;
		}
	}
	public int getTransformSource() {return transformSource;}
	private transient Transform ret;
	public Transform getTransform() {
		if(ret==null) {ret=new Transform();}
		switch(transformSource) {
		case PHYSICS:
			if(body==null) {
				Logger.log(3,"Body is null");
				return null;
			}
			body.getWorldTransform(ret);
			return ret;
		case ANIMATION:
			return animator.getFrame();
		case NONE:
			return null;
		case OTHER:
			return transformObject.getTransform();
		case VARIABLE:
			ret.set(transformVariable);
			return ret;
		default:
			return null;
		}
	}
	public void doBody(RigidBodyConstructionInfo bodyConstructionInfo) {
		this.body=new RigidBody(bodyConstructionInfo);
	}
	public void addToSimulation(short group) {
		Physics.add(body,group);
	}
	public void addToSimulation(short group, short mask) {
		Physics.add(body,group,mask);
	}
	public void removePhysics() {
		Physics.remove(body);
	}
	
	public RigidBodyConstructionInfo initPhysics_mesh(GObject vertdata, Vector3f pos, Quat4f rot) {
		CollisionShape shape=null;
		if(vertdata.fromOBJ) {
			OBJReturnObject o=new OBJReturnObject();
			o.fromVMap(vertdata.vmap, vertdata.getTris());
			TriangleIndexVertexArray mesh=new TriangleIndexVertexArray(o.numTriangles,o.indices,3*4,vertdata.vmap.vertices.size(),o.vertices,3*4);
			shape=new BvhTriangleMeshShape(mesh,true);
			MotionState motionState=new DefaultMotionState(new Transform(new Matrix4f(
					rot,
					pos,1.0f)));
			dynamic=mass!=0;
			if(dynamic) {
				shape.calculateLocalInertia(mass,inertia);
			}
			RigidBodyConstructionInfo bodyConstructionInfo=new RigidBodyConstructionInfo(mass,motionState,shape,inertia);
			return bodyConstructionInfo;
		}
		return null;
	}
	public RigidBodyConstructionInfo initPhysics_shape(CollisionShape shape, Vector3f pos, Quat4f rot) {
		MotionState motionState=new DefaultMotionState(new Transform(new Matrix4f(
				rot,
				pos,1.0f)));
		dynamic=mass!=0;
		if(dynamic) {
			shape.calculateLocalInertia(mass,inertia);
		}
		RigidBodyConstructionInfo bodyConstructionInfo=new RigidBodyConstructionInfo(mass,motionState,shape,inertia);
		bodyConstructionInfo.restitution=0.25f;
		return bodyConstructionInfo;
	}
}

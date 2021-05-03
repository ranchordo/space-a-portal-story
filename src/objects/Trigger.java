package objects;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL46.*;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import graphics.Renderer;
import graphics.Tri;
import objectTypes.GObject;

public class Trigger extends Thing {
	private static final long serialVersionUID = -21766941867476496L;
	public Quat4f quat;
	public Vector3f origin;
	private transient Transform tr;
	private transient Matrix4f trmat;
	public Trigger(Vector3f shape, Quat4f quat, Vector3f origin) {
		this.type="Trigger";
		this.shape=shape;
		this.quat=quat;
		this.origin=origin;
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		//this.geo.vmap.tex=walltex;
		this.geo.useTex=false;
		this.geo.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,-getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,-getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,-getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,-getShape().z));

		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,+getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,+getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,+getShape().z));
		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,+getShape().z));

		this.geo.vmap.normals=new ArrayList<Vector3f>();
		this.geo.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.vmap.normals.add(new Vector3f(0,0,1));
		this.geo.vmap.normals.add(new Vector3f(0,-1,0));
		this.geo.vmap.normals.add(new Vector3f(0,1,0));
		this.geo.vmap.normals.add(new Vector3f(-1,0,0));
		this.geo.vmap.normals.add(new Vector3f(1,0,0));
		this.geo.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.vmap.texcoords.add(new Vector2f(0,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,0));
		this.geo.vmap.texcoords.add(new Vector2f(0,0));
		this.geo.clearTris();
		this.geo.addTri(new Tri(0,1,2, 0,0,0));
		this.geo.addTri(new Tri(2,3,0, 0,0,0));

		this.geo.addTri(new Tri(4,5,6, 1,1,1));
		this.geo.addTri(new Tri(6,7,4, 1,1,1));

		this.geo.addTri(new Tri(3,2,6, 3,3,3));
		this.geo.addTri(new Tri(6,7,3, 3,3,3));

		this.geo.addTri(new Tri(0,1,5, 2,2,2));
		this.geo.addTri(new Tri(5,4,0, 2,2,2));

		this.geo.addTri(new Tri(1,5,6, 5,5,5));
		this.geo.addTri(new Tri(6,2,1, 5,5,5));

		this.geo.addTri(new Tri(0,4,7, 4,4,4));
		this.geo.addTri(new Tri(7,3,0, 4,4,4));

		this.geo.setColor(1.0f,1.0f,0.0f,0.5f);
		tr=new Transform();
		trmat=new Matrix4f();
		geo.useCulling=false;
		geo.useLighting=false;
		geo.lock();
	}
	@Override
	public void interact() {
		this.interacted=false;
	}
	@Override
	public void render() {}
	@Override
	public void alphaRender() {
		if(Renderer.debugRendering) {
			glPushMatrix();
			this.geo.highRender_noPushPop_customTransform(tr);
			glPopMatrix();
		}
	}
	@Override
	public void logic() {
		trmat.set(quat,origin,1.0f);
		tr.set(trmat);
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
	@Override
	public void initPhysics() {
		// TODO Auto-generated method stub

	}

}

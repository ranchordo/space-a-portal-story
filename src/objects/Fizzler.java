package objects;
import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.GraphicsInit;
import graphics.Renderer;
import graphics.Tri;
import logger.Logger;

public class Fizzler extends Thing {
	public static final List<String> FIZZLES=Arrays.asList(new String[] {"Cube", "Pellet", "Turret"});
	private static final long serialVersionUID = -4731923936294407139L;
	private Vector3f origin;
	private Quat4f quat;
	private transient Vector3f normal;
	private transient Transform tr;
	private transient Matrix4f trmat;
	public Fizzler(Vector2f shape, Vector3f origin, Quat4f quat) {
		this.type="Fizzler";
		this.shape=new Vector3f(shape.x,shape.y,0.3f); //shape.z - wall "thickness"
		this.origin=origin;
		this.quat=quat;
	}
	public void initPhysics() {
	}
	public void initGeo() {
		this.geo=new GObject();
		//this.geo.vmap.tex=walltex;
		this.geo.useTex=false;
		this.geo.useBump=false;
		this.geo.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,0));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,0));
		this.geo.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,0));
		this.geo.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,0));
		this.geo.vmap.normals=new ArrayList<Vector3f>();
		this.geo.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.vmap.texcoords.add(new Vector2f(0,shape.y));
		this.geo.vmap.texcoords.add(new Vector2f(shape.x,shape.y));
		this.geo.vmap.texcoords.add(new Vector2f(shape.x,0));
		this.geo.vmap.texcoords.add(new Vector2f(0,0));
		this.geo.clearTris();
		this.geo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.geo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		
		this.geo.setColor(0.8f,0.8f,1.5f,0.5f);
		tr=new Transform();
		trmat=new Matrix4f();
		normal=new Vector3f(0,0,-1);
		geo.transformVariable=tr;
		geo.lock();
		geo.setMotionSource(GObject.VARIABLE);
		this.stopsPortals=true;
		this.portalable=true;
		geo.useLighting=false;

		this.geo.vmap.tex.colorLoaded=true;
//		try {
//			this.geo.loadTexture(textures[textureType]);
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
	@Override
	public void render() {}
	@Override
	public void alphaRender() {
		if(Renderer.debugRendering) {
			glPushMatrix();
			glDisable(GL_CULL_FACE);
			this.geo.highRender_noPushPop_customTransform(tr);
			glEnable(GL_CULL_FACE);
			glPopMatrix();
		}
	}
	private transient Vector3f pos;
	private transient Vector3f a;
	private transient Vector3f b;
	private boolean check(Thing thing) {
		pos.set(thing.geo.getTransform().origin);
		a.set(normal);
		b.set(normal);
		a.scale(-thing.getShape().length()*0.5f);
		b.scale(thing.getShape().length()*0.5f);
		a.add(pos);
		b.add(pos);
		return geo.rayTest(b,a,tr);
	}
	@Override
	public void logic() {
		if(pos==null) {pos=new Vector3f();}
		if(a==null) {a=new Vector3f();}
		if(b==null) {b=new Vector3f();}
		trmat.set(quat,origin,1.0f);
		tr.set(trmat);
		normal.set(0,0,-1);
		tr.transform(normal);
		for(Thing thing : Renderer.things) {
			if(!FIZZLES.contains(thing.type)) {continue;}
			if(check(thing)) {
				Logger.log(0,"Fizzled a "+thing.type);
				Renderer.remSched.add(thing);
			}
		}
		if(check(GraphicsInit.player) && (GraphicsInit.player.portalPair.placed1 || GraphicsInit.player.portalPair.placed2)) {
			Logger.log(0,"Removed player portals.");
			GraphicsInit.player.portalPair.placed1=false;
			GraphicsInit.player.portalPair.placed2=false;
		}
	}
}

package graphics2d.things;

import java.util.ArrayList;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.Tri;

public abstract class Thing2d {
	public static enum PosMode {
		CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}
	public static float computeActualX(PosMode mode, float x, Vector4f bb) {
		switch(mode) {
		case CENTER:
			return x-((bb.z-bb.x)/2.0f);
		case TOP_RIGHT:
		case BOTTOM_RIGHT:
			return x-(bb.z-bb.x);
		case TOP_LEFT:
		case BOTTOM_LEFT:
		default:
			return x;
		}
	}
	public static float computeActualY(PosMode mode, float y, Vector4f bb) {
		switch(mode) {
		case CENTER:
			return y-((bb.w-bb.y)/2.0f);
		case TOP_RIGHT:
		case TOP_LEFT:
			return y-(bb.w-bb.y);
		case BOTTOM_RIGHT:
		case BOTTOM_LEFT:
		default:
			return y;
		}
	}
	public static GObject createGenericSquare() {
		GObject ret=new GObject();
		ret.vmap.vertices=new ArrayList<Vector3f>();
		ret.vmap.vertices.add(new Vector3f(0,0,0));
		ret.vmap.vertices.add(new Vector3f(1,0,0));
		ret.vmap.vertices.add(new Vector3f(1,1,0));
		ret.vmap.vertices.add(new Vector3f(0,1,0));
		ret.vmap.normals=new ArrayList<Vector3f>();
		ret.vmap.normals.add(new Vector3f(0,0,-1));
		ret.vmap.texcoords=new ArrayList<Vector2f>();
		ret.vmap.texcoords.add(new Vector2f(0,1));
		ret.vmap.texcoords.add(new Vector2f(1,1));
		ret.vmap.texcoords.add(new Vector2f(1,0));
		ret.vmap.texcoords.add(new Vector2f(0,0));
		
		ret.clearTris();
		ret.addTri(new Tri(0,1,2, 0,0,0).setTexCoords(3,2,1));
		ret.addTri(new Tri(2,3,0, 0,0,0).setTexCoords(1,0,3));
		
		ret.lock();
		
		ret.initVBO();
		ret.refresh();
		return ret;
	}
	public static float ratio2viewportX(float x) {
		return (float) (2.0f*x*(GLContextInitializer.aspectRatio)*Math.tan(Math.toRadians(GLContextInitializer.fov/2.0f)));
	}
	public static float ratio2viewportY(float y) {
		return (float) (2.0f*y*Math.tan(Math.toRadians(GLContextInitializer.fov/2.0f)));
	}
	public float x;
	public float y;
	public PosMode posMode=PosMode.CENTER;
	public Thing2d parent;
	public static GObject genericSquare=createGenericSquare();
	public GObject shape=genericSquare;
	public void init() {
		
	}
	public void add() {
		
	}
	public void logic() {
		
	}
	public void render() {
		
	}
	public final Thing2d initThis() {
		this.init();
		return this;
	}
	public abstract Thing2d setParent(Thing2d parent);
	public Thing2d setPosMode(PosMode npm) {
		this.posMode=npm;
		return this;
	}
	public Thing2d setAsParent() {
		this.setParent(this);
		return this;
	}
	public abstract Vector4f getBoundingBox();
}

package objects;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.Renderer;
import graphics.Tri;
import pooling.PoolElement;
import pooling.Pools;
import portalcasting.PortalCaster;
import portalcasting.Segment;
import util.Util;

public class LightBridge extends Thing {
	private static final Vector3f startOffset=new Vector3f(0,0.1f,0);
	private static final long serialVersionUID = 1237435512380248141L;
	private Quat4f quat;
	private Vector3f origin;
	private transient PortalCaster portalCaster;
	private transient GObject bridgegeo=new GObject();
	private Vector3f bridgeShape=new Vector3f();
	public LightBridge(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Light_Bridge";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
		portalCaster=new PortalCaster() {};
		mm=new Transform();
	}
	Vector3f control=new Vector3f();
	Vector3f offsetOrigin=new Vector3f();
	Matrix3f rs=new Matrix3f();
	Matrix4f mmm=new Matrix4f();
	Vector3f ilz=new Vector3f();
	@Override
	public void processActivation() {
		mm=geo.getTransform();
		mm.getMatrix(mmm).getRotationScale(rs);
		ilz.set(0,0,1);
		offsetOrigin.set(startOffset);
		rs.transform(offsetOrigin);
		rs.transform(ilz);
		control.set(offsetOrigin);
		control.normalize();
		offsetOrigin.add(mm.origin);
		control.add(offsetOrigin);
		
		portalCaster.initialLocalZ.set(ilz);
		portalCaster.cast(offsetOrigin,control);
	}
	private transient Transform mm=new Transform();
	private Vector3f axis=new Vector3f();
	private Vector3f rotAxis=new Vector3f();
	private Vector3f defaultAxis=new Vector3f(1,0,0);
	private Vector3f defaultZ=new Vector3f(0,0,1);
	private float rotAngle;
	private Vector3f nothing=new Vector3f(0,0,0);
	private Vector3f segOrigin=new Vector3f();
	private Matrix3f inner=new Matrix3f();
	@Override
	public void render() {
		this.geo.highRender();
	}
	@Override
	public void alphaRender() {
		for(Segment seg : portalCaster.segments) {
			renderSegment(seg);
		}
	}
	private void renderSegment(Segment seg) {
		segOrigin.set(seg.a);
		segOrigin.add(seg.b);
		segOrigin.scale(0.5f);
		axis.set(seg.b);
		axis.sub(seg.a);
		float half=axis.length()*0.5f;
		axis.normalize();
		//System.out.println(player_local+", "+angle);
		rotAxis.cross(defaultAxis,axis);
		rotAxis.normalize();
		rotAngle=axis.angle(defaultAxis);
//		//System.out.println(axis+",    "+angle);
		glDepthMask(false);
		glPushMatrix();
		PoolElement<Matrix4f> mm_mat=Pools.matrix4f.alloc();
		PoolElement<Matrix4f> mm_mat2=Pools.matrix4f.alloc();
		PoolElement<AxisAngle4f> aa4f=Pools.axisAngle4f.alloc();
		PoolElement<AxisAngle4f> aa4f2=Pools.axisAngle4f.alloc();
		aa4f.o().set(defaultAxis,defaultZ.angle(seg.localz));
		aa4f2.o().set(rotAxis,rotAngle);
		PoolElement<Quat4f> q=Util.AxisAngle(aa4f.o());
		PoolElement<Quat4f> q2=Util.AxisAngle(aa4f2.o());
		mm_mat.o().set(q2.o(),segOrigin,1);
		mm_mat2.o().set(q.o(),nothing,1);
		mm_mat.o().mul(mm_mat2.o());
		mm_mat.o().getRotationScale(inner);
		inner.m00*=half;
		inner.m10*=half;
		inner.m20*=half;
		mm_mat.o().setRotation(inner);
		mm.set(mm_mat.o());
		bridgegeo.highRender_noPushPop_customTransform(mm);
		glPopMatrix();
		glDepthMask(true);
		mm_mat.free();
		mm_mat2.free();
		aa4f.free();
		aa4f2.free();
		q.free();
		q2.free();
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("bridge/emitter");
		this.geo.useTex=false;
		this.geo.setColor(1,1,1);
		geo.lock();
		
		this.bridgeShape.set(1,1f,0.01f);
		
		this.bridgegeo=new GObject();
		bridgegeo.clearTris();
		this.bridgegeo.vmap.vertices=new ArrayList<Vector3f>();
		this.bridgegeo.vmap.vertices.add(new Vector3f(-bridgeShape.x,-bridgeShape.y,0));
		this.bridgegeo.vmap.vertices.add(new Vector3f(+bridgeShape.x,-bridgeShape.y,0));
		this.bridgegeo.vmap.vertices.add(new Vector3f(+bridgeShape.x,+bridgeShape.y,0));
		this.bridgegeo.vmap.vertices.add(new Vector3f(-bridgeShape.x,+bridgeShape.y,0));
		this.bridgegeo.vmap.normals=new ArrayList<Vector3f>();
		this.bridgegeo.vmap.normals.add(new Vector3f(0,0,-1));
		this.bridgegeo.vmap.texcoords=new ArrayList<Vector2f>();
		this.bridgegeo.vmap.texcoords.add(new Vector2f(0,1));
		this.bridgegeo.vmap.texcoords.add(new Vector2f(1,1));
		this.bridgegeo.vmap.texcoords.add(new Vector2f(1,0));
		this.bridgegeo.vmap.texcoords.add(new Vector2f(0,0));
		this.bridgegeo.clearTris();
		this.bridgegeo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.bridgegeo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		this.bridgegeo.vmap.tex.colorLoaded=true;
		this.bridgegeo.lock();
		this.bridgegeo.useLighting=false;
		this.bridgegeo.useCulling=false;
		this.bridgegeo.setColor(0f,0f,2.0f,0.2f);
		this.bridgegeo.useTex=false;
		bridgegeo.hasAlpha=true;
		
		
		bridgegeo.initVBO();
		bridgegeo.refresh();
		
		declareResource(bridgegeo);
	}

}

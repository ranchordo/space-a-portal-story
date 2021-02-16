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
import graphics.GraphicsInit;
import graphics.Renderer;
import graphics.Tri;
import logger.Logger;
import pooling.PoolElement;
import pooling.Pools;
import portalcasting.PortalCaster;
import portalcasting.Segment;
import util.Util;

public class Funnel extends Thing {
	private static final Vector3f startOffset=new Vector3f(0,0.1f,0);
	private static final long serialVersionUID = 1237435512380248141L;
	private Quat4f quat;
	private Vector3f origin;
	private transient PortalCaster portalCaster;
	private transient GObject funnelgeo=new GObject();
	public float radius=1.0f;
	public Funnel(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Funnel";
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
		portalCaster.DOES_NOT_STOP=PortalPair.PASSES;
	}
	Vector3f control=new Vector3f();
	Vector3f offsetOrigin=new Vector3f();
	Vector3f thing_local=new Vector3f();
	Vector3f thingcomp=new Vector3f();
	private void runFunnelCheck(Segment seg,Thing thing) {
		if(!PortalPair.PASSES.contains(thing.type)) {return;}
		if(thing.funnelInFunnel) {return;}
		if(thing.geo==null) {return;}
		if(thing.geo.body==null) {return;}
		axis.set(seg.b);
		axis.sub(seg.a);
		float d=(axis.length()*0.5f)+1.0f;
		axis.normalize();
		segOrigin.set(seg.a);
		segOrigin.add(seg.b);
		segOrigin.scale(0.5f);
		thing_local.set(thing.geo.getTransform().origin);
		if(thing.type.equals("Player")) {
			thing_local.set(Renderer.camera.pos_out);
		}
		thing_local.sub(segOrigin);
		thingcomp.set(axis);
		thingcomp.scale(axis.dot(thing_local));
		thing_local.sub(thingcomp);
		if(thing_local.length()<=radius && thingcomp.length()<d) {
			//thing is inside funnel
			thing.funnelInFunnel=true;
			thing.funnelActiveSegment.set(seg);
		}
	}
	private void handleFunnelResults(Thing thing) {
		if(thing.funnelInFunnel && !thing.funnelCheckYes) {
			//Rising edge
			thing.funnelPrevUseGravity=thing.usesGravity();
			
			thing.setUsesGravity(false);
			Logger.log(0,"ENTER FUNNEL");
		} else if(!thing.funnelInFunnel && thing.funnelCheckYes) {
			//Falling edge
			thing.funnelInFunnel=false;
			thing.setUsesGravity(thing.funnelPrevUseGravity);
			Logger.log(0,"EXIT FUNNEL");
		}
		thing.funnelCheckYes=thing.funnelInFunnel;
	}
	Matrix3f rs=new Matrix3f();
	Matrix4f mmm=new Matrix4f();
	@Override
	public void processActivation() {
		mm=this.geo.getTransform();
		mm.getMatrix(mmm).getRotationScale(rs);
		offsetOrigin.set(startOffset);
		rs.transform(offsetOrigin);
		control.set(offsetOrigin);
		control.normalize();
		offsetOrigin.add(mm.origin);
		control.add(offsetOrigin);
		
		portalCaster.cast(offsetOrigin,control);
		for(int i=0;i<Renderer.things.size();i++) {
			Thing thing=Renderer.things.get(i);
			if(!PortalPair.PASSES.contains(thing.type)) {continue;}
			thing.funnelInFunnel=false;
		}
		GraphicsInit.player.funnelInFunnel=false;
		for(Segment seg : portalCaster.segments) {
			for(int i=0;i<Renderer.things.size();i++) {
				Thing thing=Renderer.things.get(i);
				runFunnelCheck(seg,thing);
			}
			runFunnelCheck(seg,GraphicsInit.player);
			//System.out.println(seg.a+", "+seg.b);
		}
		for(int i=0;i<Renderer.things.size();i++) {
			Thing thing=Renderer.things.get(i);
			handleFunnelResults(thing);
		}
		handleFunnelResults(GraphicsInit.player);
	}
	private transient Transform mm;
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
		funnelgeo.scale.set(1.0f,radius,radius);
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
		inner.m01*=radius;
		inner.m11*=radius;
		inner.m21*=radius;
		inner.m02*=radius;
		inner.m12*=radius;
		inner.m22*=radius;
		mm_mat.o().setRotation(inner);
		mm.set(mm_mat.o());
		funnelgeo.highRender_noPushPop_customTransform(mm);
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
		this.geo.loadOBJ("funnel/emitter");
		this.geo.useTex=false;
		this.geo.setColor(1,1,1);
		geo.lock();
		
		
		this.funnelgeo=new GObject();
		this.funnelgeo.loadOBJ("funnel/beam");
		this.funnelgeo.lock();
		this.funnelgeo.useLighting=true;
		this.funnelgeo.useCulling=false;
		this.funnelgeo.setColor(0f,0f,2.0f,0.2f);
		this.funnelgeo.useTex=false;
		funnelgeo.hasAlpha=true;
		
		
		funnelgeo.initVBO();
		funnelgeo.refresh();
		declareResource(funnelgeo);
	}

}

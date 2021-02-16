package objects;

import static org.lwjgl.opengl.GL46.*;

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

import anim.AnimParser;
import anim.AnimTrack;
import graphics.GObject;
import graphics.Renderer;
import graphics.Tri;
import pooling.PoolElement;
import pooling.Pools;
import portalcasting.PortalCaster;
import portalcasting.Segment;
import portalcasting.SingleCastResult;
import util.Util;

public class Laser extends Thing {
	private static final Vector3f startOffset=new Vector3f(0,0.9f,0);
	private static final long serialVersionUID = 1237435512380248141L;
	private Quat4f quat;
	private Vector3f origin;
	private transient PortalCaster portalCaster;
	private transient GObject lasergeo=new GObject();
	private Vector3f beamShape=new Vector3f();
	private transient GObject piston;
	private transient GObject casing;
	private transient GObject slidy;
	private transient GObject ball;
	public Laser(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Laser";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
		portalCaster=new PortalCaster() {
			private Transform tr=new Transform();
			private Matrix4f trm=new Matrix4f();
			private Matrix3f trmrs=new Matrix3f();
			@Override
			public boolean handleSpecial(SingleCastResult r, Vector3f start, Vector3f control) {
				if(r.hit==null) {return false;}
				if(r.hit.type.equals("Cube")) {
					if(((Cube)r.hit).cubeType!=Cube.LASER) {return false;}
					control.set(-1,0,0);
					tr=r.hit.geo.getTransform();
					tr.getMatrix(trm);
					trm.getRotationScale(trmrs);
					trmrs.transform(control);
					start.set(control);
					start.scale(0.3f);
					start.add(tr.origin);
					control.add(start);
					return true;
				}
				return false;
			}
		};
		portalCaster.ACTIVATES.add("Laser_Receiver");
		mm=new Transform();
	}
	Vector3f control=new Vector3f();
	Vector3f offsetOrigin=new Vector3f();
	Matrix3f rs=new Matrix3f();
	Matrix4f mmm=new Matrix4f();
	@Override
	public void processActivation() {
		mm=geo.getTransform();
		mm.getMatrix(mmm).getRotationScale(rs);
		offsetOrigin.set(startOffset);
		rs.transform(offsetOrigin);
		control.set(offsetOrigin);
		control.normalize();
		offsetOrigin.add(ball.getTransform().origin);
		control.add(offsetOrigin);
		
		portalCaster.cast(offsetOrigin,control);
		widthMultiplier=1.0f+(Util.randint(100)/2000.0f);
	}
	@Override
	public void logic() {
		toRenderSegs=activations>=activationThreshold;
		piston.animator.getTrack().setReversed(!toRenderSegs);
		casing.animator.getTrack().setReversed(!toRenderSegs);
		slidy.animator.getTrack().setReversed(!toRenderSegs);
		ball.animator.getTrack().setReversed(!toRenderSegs);
		
		piston.animator.synchronizedTransform.set(piston.transformObject.getTransform());
		piston.animator.advanceTrack();
		casing.animator.synchronizedTransform.set(casing.transformObject.getTransform());
		casing.animator.advanceTrack();
		slidy.animator.synchronizedTransform.set(slidy.transformObject.getTransform());
		slidy.animator.advanceTrack();
		ball.animator.synchronizedTransform.set(ball.transformObject.getTransform());
		ball.animator.advanceTrack();
		//System.out.println(collisions.size());
	}
	private transient Transform mm=new Transform();
	private Vector3f player_local=new Vector3f();
	private Vector3f axis=new Vector3f();
	private float angle;
	private Vector3f normal=new Vector3f(0,0,-1);
	private Vector3f playercomp=new Vector3f();
	private Vector3f rotAxis=new Vector3f();
	private Vector3f defaultAxis=new Vector3f(1,0,0);
	private float rotAngle;
	private Vector3f nothing=new Vector3f(0,0,0);
	private Vector3f ncp=new Vector3f();
	private Vector3f segOrigin=new Vector3f();
	private boolean toRenderSegs=false;
	@Override
	public void render() {
		mm=geo.getTransform();
		mm.getMatrix(mmm).getRotationScale(rs);
		axis.set(0,1,0);
		rs.transform(axis);
		axis.normalize();
		this.geo.highRender();
		glPushMatrix();
		this.casing.highRender();
		this.piston.highRender();
		glTranslatef(mm.origin.x,mm.origin.y,mm.origin.z);
		glRotatef(90,axis.x,axis.y,axis.z);
		glTranslatef(-mm.origin.x,-mm.origin.y,-mm.origin.z);
		this.casing.highRender();
		this.piston.highRender();
		glTranslatef(mm.origin.x,mm.origin.y,mm.origin.z);
		glRotatef(90,axis.x,axis.y,axis.z);
		glTranslatef(-mm.origin.x,-mm.origin.y,-mm.origin.z);
		this.casing.highRender();
		this.piston.highRender();
		glTranslatef(mm.origin.x,mm.origin.y,mm.origin.z);
		glRotatef(90,axis.x,axis.y,axis.z);
		glTranslatef(-mm.origin.x,-mm.origin.y,-mm.origin.z);
		this.casing.highRender();
		this.piston.highRender();
		glPopMatrix();
		this.slidy.highRender();
	}
	@Override
	public void alphaRender() {
		glDepthMask(false);
		this.ball.highRender();
		glDepthMask(true);
		if(toRenderSegs) {
			for(Segment seg : portalCaster.segments) {
				renderSegment(seg);
			}
		}
	}
	private float widthMultiplier=1.0f;
	private Matrix3f inner=new Matrix3f();
	private void renderSegment(Segment seg) {
		segOrigin.set(seg.a);
		segOrigin.add(seg.b);
		segOrigin.scale(0.5f);
		player_local.set(Renderer.camera.pos_out);
		player_local.sub(segOrigin);
		axis.set(seg.b);
		axis.sub(seg.a);
		float half=axis.length()*0.5f;
		axis.normalize();
		playercomp.set(axis);
		playercomp.scale(playercomp.dot(player_local));
		player_local.sub(playercomp);
		player_local.normalize();
		ncp.cross(normal,player_local);
		angle=(float)Math.atan2(ncp.dot(axis),normal.dot(player_local));
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
		aa4f.o().set(axis,angle);
		aa4f2.o().set(rotAxis,rotAngle);
		PoolElement<Quat4f> q=Util.AxisAngle(aa4f.o());
		PoolElement<Quat4f> q2=Util.AxisAngle(aa4f2.o());
		mm_mat.o().set(q.o(),segOrigin,1);
		mm_mat2.o().set(q2.o(),nothing,1);
		mm_mat.o().mul(mm_mat2.o());
		mm_mat.o().getRotationScale(inner);
		inner.m00*=half;
		inner.m10*=half;
		inner.m20*=half;
		inner.m01*=widthMultiplier;
		inner.m11*=widthMultiplier;
		inner.m21*=widthMultiplier;
		mm_mat.o().setRotation(inner);
		mm.set(mm_mat.o());
		lasergeo.highRender_noPushPop_customTransform(mm);
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
		this.geo.loadOBJ("laser_emitter/laser_emitter");
		this.geo.useTex=false;
		this.geo.setColor(0.3f,0.3f,0.31f);
		geo.lock();
		
		this.piston=new GObject();
		piston.loadOBJ("laser_emitter/piston");
		piston.useTex=false;
		piston.setColor(0.65f,0.65f,0.65f);
		piston.lock();
		
		this.casing=new GObject();
		casing.loadOBJ("laser_emitter/casing");
		casing.useTex=false;
		casing.setColor(0.26f,0.56f,0.96f);
		casing.lock();
		
		this.slidy=new GObject();
		slidy.loadOBJ("laser_emitter/slidy");
		slidy.useTex=false;
		slidy.setColor(0.55f,0.55f,0.55f);
		slidy.lock();
		
		this.ball=new GObject();
		ball.loadOBJ("laser_emitter/ball");
		ball.useTex=false;
		ball.setColor(1.0f,0.4f,0.4f,0.4f);
		ball.lock();
		
		this.beamShape.set(1,0.3f,0.01f);
		
		this.lasergeo=new GObject();
		lasergeo.clearTris();
		this.lasergeo.vmap.vertices=new ArrayList<Vector3f>();
		this.lasergeo.vmap.vertices.add(new Vector3f(-beamShape.x,-beamShape.y,0));
		this.lasergeo.vmap.vertices.add(new Vector3f(+beamShape.x,-beamShape.y,0));
		this.lasergeo.vmap.vertices.add(new Vector3f(+beamShape.x,+beamShape.y,0));
		this.lasergeo.vmap.vertices.add(new Vector3f(-beamShape.x,+beamShape.y,0));
		this.lasergeo.vmap.normals=new ArrayList<Vector3f>();
		this.lasergeo.vmap.normals.add(new Vector3f(0,0,-1));
		this.lasergeo.vmap.texcoords=new ArrayList<Vector2f>();
		this.lasergeo.vmap.texcoords.add(new Vector2f(0,1));
		this.lasergeo.vmap.texcoords.add(new Vector2f(1,1));
		this.lasergeo.vmap.texcoords.add(new Vector2f(1,0));
		this.lasergeo.vmap.texcoords.add(new Vector2f(0,0));
		this.lasergeo.clearTris();
		this.lasergeo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.lasergeo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		this.lasergeo.vmap.tex.colorLoaded=true;
		this.lasergeo.lock();
		this.lasergeo.useLighting=false;
		this.lasergeo.useCulling=false;
		this.lasergeo.setColor(2.3f,2.3f,2.3f);
		this.lasergeo.useTex=true;
		lasergeo.hasAlpha=true;
		
		this.lasergeo.vmap.tex.colorLoaded=true;
		try {
			this.lasergeo.loadTexture("3d/laser/beam.png");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		lasergeo.initVBO();
		lasergeo.refresh();
		
		piston.initVBO();
		piston.refresh();
		piston.setMotionSource(GObject.ANIMATION);
		piston.animator.add("Extension",AnimParser.parse("3d/laser_emitter/piston").setEndMode(AnimTrack.STAY));
		piston.transformObject=this.geo;
		piston.animator.synchronizedTransform=new Transform();
		piston.animator.copyTransformPointer();
		piston.animator.setActiveAnim("Extension");
		
		casing.initVBO();
		casing.refresh();
		casing.setMotionSource(GObject.ANIMATION);
		casing.animator.add("Extension",AnimParser.parse("3d/laser_emitter/casing").setEndMode(AnimTrack.STAY));
		casing.transformObject=this.geo;
		casing.animator.synchronizedTransform=new Transform();
		casing.animator.copyTransformPointer();
		casing.animator.setActiveAnim("Extension");
		
		slidy.initVBO();
		slidy.refresh();
		slidy.setMotionSource(GObject.ANIMATION);
		slidy.animator.add("Extension",AnimParser.parse("3d/laser_emitter/slidy").setEndMode(AnimTrack.STAY));
		slidy.transformObject=this.geo;
		slidy.animator.synchronizedTransform=new Transform();
		slidy.animator.copyTransformPointer();
		slidy.animator.setActiveAnim("Extension");
		
		ball.initVBO();
		ball.refresh();
		ball.setMotionSource(GObject.ANIMATION);
		ball.animator.add("Extension",AnimParser.parse("3d/laser_emitter/ball").setEndMode(AnimTrack.STAY));
		ball.transformObject=this.geo;
		ball.animator.synchronizedTransform=new Transform();
		ball.animator.copyTransformPointer();
		ball.animator.setActiveAnim("Extension");
		
		declareResource(piston);
		declareResource(casing);
		declareResource(slidy);
		declareResource(ball);
		declareResource(lasergeo);
	}

}

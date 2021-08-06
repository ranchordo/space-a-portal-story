package objects;

import static org.lwjgl.opengl.GL15.*;

import java.util.Arrays;
import java.util.List;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import game.Main;
import lepton.engine.physics.PhysicsObject;
import lepton.engine.physics.WorldObject;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.Shader;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;
import util.AdditionalVecmathPools;
import util.SaveStateComponent;
import util.Util;

public class PortalPair extends Thing {
	//Stencil space partitioning:
	/*
	 * 0 - Unused
	 * [1,128) - Portal 1
	 * 128 - Reality
	 * [192,255] - Portal 2
	 */
	//Based on this partition schema, 63 stencil iterations is the maximum allowed.
	private static final long serialVersionUID = 2380326466588862899L;
	public static final int STENCIL_ITERATIONS=2;
	public static final float FUNNELING_SCALE=3.0f;
	public static final float FUNNELING_DIST=20.0f;
	public static final float FUNNELING_VEL_THSHLD=2.0f;
	public static final float FUNNELING_FRC_SCALE=2.0f;
	public static final float FUNNELING_TARG_VEL_MAG=1.0f;
	public static final float FUNNELING_ANGLE_THSHLD=(float)Math.toRadians(20);
	public static final boolean FUNNELING_ENABLE=true;
	public static final float CLIP_SCALE=1.5f;
	public static final float CLIP_DISTANCE=0.0f;
	public static final float WALL_DISTANCE=0.05f;
	public static final float VEL_CLIP_MULTIPLIER=4.0f;
	public static final float VEL_MAG_STOP=2.0f;
	public static final float ALT_CLIP=2f;
	public static final float OPENING_ANIM_LEN_MICROS=500000.0f;
	public static final float PORTAL_WIDTH=1.0f;
	public static final float PORTAL_HEIGHT=1.8f;
	public static final List<String> PASSES=Arrays.asList(new String[] {"Cube", "Player", "Pellet", "Turret", "Portable_wall"});
	public transient Transform p1;
	public transient Transform p2;
	private transient Transform diff;
	private transient Transform diffi;
	public boolean placed1=false;
	public boolean placed2=false;
	public Vector3f normal1=new Vector3f(0,0,-1);
	public Vector3f normal2=new Vector3f(0,0,-1);
	
	public transient WorldObject geo2;
	private transient WorldObject geofx;
	private transient WorldObject geo2fx;
	protected SaveStateComponent geo2ss;
	
//	private transient ParticleSystem psys1;
//	private transient ParticleSystem psys2;
	
	@Thing.SerializeByID
	public transient Thing attached1;
	@Thing.SerializeByID
	public transient Thing attached2;
	@Override
	public void doSaveState() {
		saveStateComponent=new SaveStateComponent();
		saveStateComponent.transform=new Matrix4f();
		saveStateComponent.velocity=new Vector3f();
		geo2ss=new SaveStateComponent();
		geo2ss.transform=new Matrix4f();
		geo2ss.velocity=new Vector3f();
		this.p1.getMatrix(saveStateComponent.transform);
		this.p2.getMatrix(geo2ss.transform);
	}
	@Override
	public void unpackSaveState() {
		if(this.saveStateComponent!=null) {
			p1=new Transform(saveStateComponent.transform);
			p2=new Transform(geo2ss.transform);
			updateDifferences();
			Logger.log(0,type+": Restoring rbstate from local saveStateComponent");
		}
	}
	private void drawInsides_older(int portal, int offset) {
		int base=(portal==1) ? 0 : 192;
		//int mask=(portal==1) ? 0x80 : 0xC0;
		glClear(GL_DEPTH_BUFFER_BIT);
		if(offset>STENCIL_ITERATIONS) {return;}
//		glColorMask(false,false,false,false);
//		Main.useGraphics=false;
//		glStencilFunc(GL_EQUAL,128,0xFF);
//		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
//		for(Thing thing : Main.things) { //       Render a depth pass
//			Main.renderRoutine(thing,stop(portal+(2*offset)-2));
//		}
//		Main.useGraphics=true;
//		glColorMask(true, true, true, true);
		
		
		glStencilMask(0xFF);
		if(offset!=0) {
			glStencilFunc(GL_EQUAL,offset+base,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_INCR);
		} else {
			glStencilFunc(GL_ALWAYS,offset+1+base,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_REPLACE);
		}
		
		glColorMask(false,false,false,false);
		GLContextInitializer.useGraphics=false;
		Main.portalRenderRoutine(this,stop(portal+(2*offset)-2),portal); //              Render portal into stencil and depth
		
//		if(offset!=0) {glStencilFunc(GL_EQUAL,offset+1+base,0xFF);}
//		else {glStencilFunc(GL_EQUAL,offset+1+base,0xFF);}
//		glStencilOp(GL_KEEP,GL_KEEP,GL_DECR);
//		for(Thing thing : Main.things) { //       Inversely render our parent's reality to the stencil buffer
//			Main.renderRoutine(thing,stop(portal+(2*offset)-2));
//		}
//		glClear(GL_DEPTH_BUFFER_BIT);
//		for(int i=1;i<offset+1;i++) {
//			glStencilFunc(GL_ALWAYS,i+1+base,0xFF);
//			glStencilOp(GL_KEEP,GL_KEEP,GL_REPLACE);
//			for(Thing thing : Main.things) { //       Render a depth pass
//				Main.renderRoutine(thing,stop(portal+(2*i)-2));
//			}
//		}
		glColorMask(true, true, true, true);
		GLContextInitializer.useGraphics=true;
		
		this.drawInsides(portal,offset+1); //Recurse
		
		glClear(GL_DEPTH_BUFFER_BIT);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Main.things) { //                 Fake render step
			Main.renderRoutine(thing,portal+(2*offset));
		}
		for(Thing thing : Main.things) { //                 Fake render step
			Main.alphaRenderRoutine(thing,portal+(2*offset));
		}
		
		Main.portalRenderRoutine(this,stop(portal+(2*offset)-2),portal);
		
//		glStencilFunc(GL_LEQUAL,offset+1+base,0xFF);
//		for(Thing thing : Main.things) { //       Render our reality on top
//			Main.renderRoutine(thing,stop(portal+(2*offset)-2));
//			Main.alphaRenderRoutine(thing,stop(portal+(2*offset)-2));
//		}
		
		glClear(GL_DEPTH_BUFFER_BIT);
		glStencilFunc(GL_EQUAL,128,0xFF);
	}
	private void drawInsides(int portal, int offset) {
		int inside=portal+(2*offset);
		int outside=stop(portal+(2*offset)-2);
		int base=(portal==1) ? 0 : 192;
		glClear(GL_DEPTH_BUFFER_BIT);
		if(offset>STENCIL_ITERATIONS) {return;}
		glStencilMask(0xFF);
		if(offset!=0) {
			glStencilFunc(GL_EQUAL,offset+base,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_INCR);
		} else {
			glStencilFunc(GL_ALWAYS,offset+1+base,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_REPLACE);
		}
		
		glColorMask(false,false,false,false);
		GLContextInitializer.useGraphics=false;
		Main.portalRenderRoutine(this,outside,portal); //              Render portal into stencil and depth
		
		glColorMask(true, true, true, true);
		GLContextInitializer.useGraphics=true;
		
		this.drawInsides(portal,offset+1); //Recurse
		
		glClear(GL_DEPTH_BUFFER_BIT);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Main.things) { //                 Fake render step
			Main.renderRoutine(thing,inside);
		}
		for(Thing thing : Main.things) { //                 Fake render step
			Main.alphaRenderRoutine(thing,inside);
		}
		Main.alphaRenderRoutine(this,inside);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		Main.portalRenderRoutine(this,outside,portal);
//		glStencilFunc(GL_EQUAL,0,0xFF);
		glStencilFunc(GL_ALWAYS,0x00,0xFF);
	}
	private void drawInsides_old(int portal, int offset) {
		int inside=portal+(2*offset);
		int outside=stop(portal+(2*offset)-2);
		if(offset>STENCIL_ITERATIONS) {return;}
		drawInsides(portal,offset+1);
		glClearStencil(0x00);
		glClear(GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glColorMask(false,false,false,false);
		glStencilFunc(GL_ALWAYS,0x01,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_REPLACE);
		glDepthMask(false);
		Main.portalRenderRoutine(this,outside,portal);
		glStencilFunc(GL_ALWAYS,0x02,0xFF);
		Main.portalRenderRoutine(this,inside,portal);
		glDepthMask(true);
		glColorMask(true, true, true, true);
		glStencilFunc(GL_EQUAL,0x01,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Main.things) {
			Main.renderRoutine(thing,inside);
		}
		for(Thing thing : Main.things) {
			Main.alphaRenderRoutine(thing,inside);
		}
		Main.alphaRenderRoutine(this,inside);
		glStencilFunc(GL_ALWAYS,0x00,0xFF);
		Main.portalRenderRoutine(this,outside,portal);
	}
	public void portal(int portal) {
		drawInsides(portal,0);
		glColorMask(false,false,false,false);
		GLContextInitializer.useGraphics=false;
		Main.portalRenderRoutine(this,0,1);
		Main.portalRenderRoutine(this,0,2);
		glColorMask(true, true, true, true);
		GLContextInitializer.useGraphics=true;
	}
	private int stop(int v) {
		if(v<0) {return 0;}
		return v;
	}
//	private int zero(int v) {
//		if(v==0) {return 128;}
//		return v;
//	}
//	private int value(int v, int c, int t) {
//		if(v==c) {return t;}
//		return v;
//	}
	public PortalPair() {
		this.type="Portal_pair";
		this.shape=new Vector3f(PortalPair.PORTAL_WIDTH,PortalPair.PORTAL_HEIGHT,0.1f); //shape.z - wall "thickness"
		p1=new Transform(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))),new Vector3f(0,19,9.85f),1.0f));
		p2=new Transform(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,19,-9.85f),1.0f));
		updateDifferences();
	}
	public void onPlacement(int portal) {
		if(Main.portalWorld.getWorld2().getBodies().size()>0) {
			Main.portalWorld.rebuildWorld2WithDuplicateStructures();
		}
	}
	private transient Matrix3f mat3=new Matrix3f();
	private transient Matrix4f i=new Matrix4f();
	private transient Matrix4f t=new Matrix4f();
	public void updateDifferences() {
		if(mat3==null) {mat3=new Matrix3f();}
		if(i==null) {i=new Matrix4f();}
		if(t==null) {t=new Matrix4f();}
		
		p2.getMatrix(i);
		i.invert();
		p1.getMatrix(t);
		
		t.getRotationScale(mat3);
		normal1.set(0,0,-1);
		mat3.transform(normal1);
		normal1.normalize();
		
		PoolElement<AxisAngle4f> aape=DefaultVecmathPools.axisAngle4f.alloc();
		PoolElement<Vector3f> v3pe=DefaultVecmathPools.vector3f.alloc();
		PoolElement<Matrix4f> m44=DefaultVecmathPools.matrix4f.alloc();
		
		v3pe.o().set(0,0,0);
		aape.o().set(0,1,0,(float)Math.toRadians(180));
		PoolElement<Quat4f> quat=LeptonUtil.AxisAngle(aape.o());
		aape.free();
		aape=null;
		m44.o().set(quat.o(),v3pe.o(),1.0f);
		quat.free();
		quat=null;
		v3pe.free();
		v3pe=null;
		t.mul(m44.o());
		t.mul(i);
		
		if(diff==null) {diff=new Transform();}
		diff.set(t);
		
		//-------------------------------
		
		p1.getMatrix(i);
		i.invert();
		p2.getMatrix(t);
		
		t.getRotationScale(mat3);
		normal2.set(0,0,-1);
		mat3.transform(normal2);
		normal2.normalize();
		
		t.mul(m44.o());
		t.mul(i);
		if(diffi==null) {diffi=new Transform();}
		diffi.set(t);
		m44.free();
		m44=null;
	}
	public Transform difference() {
		return diff;
	}
	public Transform difference_inv() {
		return diffi;
	}
	public int rayTest(Vector3f a, Vector3f b) {
		return rayTest_sc(a,b,1);
	}
	public boolean rayTest(Transform p, Vector3f a, float sc, float maxDist) {
		PoolElement<Matrix4f> m=DefaultVecmathPools.matrix4f.alloc();
		p.getMatrix(m.o());
		m.o().invert(); //m is world-to-object
		PoolElement<Point3f> pe1=AdditionalVecmathPools.point3f.alloc();
		pe1.o().set(a);
		m.o().transform(pe1.o());
		float x=pe1.o().x;
		float y=pe1.o().y;
		x/=getShape().x;
		y/=getShape().y;
		m.free();
		pe1.free();
		float r=(float)Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
		return r<=sc && (maxDist<0)?true:(Math.abs(pe1.o().z)<=maxDist);
	}
	public boolean rayTest(Transform p, Vector3f a, Vector3f b, float sc) {
		PoolElement<Matrix4f> m=DefaultVecmathPools.matrix4f.alloc();
		p.getMatrix(m.o());
		m.o().invert(); //m is world-to-object
		PoolElement<Point3f> pe1=AdditionalVecmathPools.point3f.alloc();
		PoolElement<Point3f> pe2=AdditionalVecmathPools.point3f.alloc();
		pe1.o().set(a);
		pe2.o().set(b);
		m.o().transform(pe1.o());
		m.o().transform(pe2.o());
		if(Util.sign(pe1.o().z)==Util.sign(pe2.o().z)) {
			m.free();
			pe1.free();
			pe2.free();
			return false;
		}
		float dz=pe1.o().z-pe2.o().z;
		float x=(-((pe1.o().x-pe2.o().x)/dz)*pe1.o().z)+pe1.o().x;
		float y=(-((pe1.o().y-pe2.o().y)/dz)*pe1.o().z)+pe1.o().y;
		x/=getShape().x;
		y/=getShape().y;
		m.free();
		pe1.free();
		pe2.free();
		float r=(float)Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
		return r<=sc;
	}
	public int rayTest_sc(Vector3f a, Vector3f b, float sc) {
		return (rayTest(p1,a,b,sc)?1:0) + (rayTest(p2,a,b,sc)?2:0);
	}
	public int funnelingRayTest(Vector3f a, Vector3f b) {
		return rayTest_sc(a,b,FUNNELING_SCALE);
	}
	private boolean pplaced;
	private long placedMicros=-1;
	private float fadeAnim=0;
	@Override
	public void logic() {
//		psys1.stepParticles();
//		psys2.stepParticles();
		if(placed1 && placed2) {
			if(!pplaced) {
				placedMicros=LeptonUtil.micros();
				fadeAnim=0;
			}
		}
		if(placedMicros!=-1) {
			fadeAnim=(LeptonUtil.micros()-placedMicros)/OPENING_ANIM_LEN_MICROS;
			if(fadeAnim>=1) {
				placedMicros=-1;
			}
		}
		pplaced=placed1&&placed2;
		if(attached1!=null) {handlePortalFollowing(attached1,1);}
		if(attached2!=null) {handlePortalFollowing(attached2,2);}
		updateDifferences();
	}
	private transient Matrix4f invertedCheckin;
	private transient Transform trtemp;
	private void handlePortalFollowing(Thing attached, int portal) {
		if(invertedCheckin==null) {invertedCheckin=new Matrix4f();}
		if(trtemp==null) {trtemp=new Transform();}
		Matrix4f lastPortalCheckin=(portal==1)?attached.lastPortalCheckin1:attached.lastPortalCheckin2;
		if(((portal==1)?attached.lastPortalCheckin1:attached.lastPortalCheckin2)==null) {
			if(portal==1) {
				attached.lastPortalCheckin1=new Matrix4f();
			} else if(portal==2) {
				attached.lastPortalCheckin2=new Matrix4f();
			} else {
				Logger.log(4,"What? Portal id was somehow "+portal);
			}
			lastPortalCheckin=(portal==1)?attached.lastPortalCheckin1:attached.lastPortalCheckin2;
			attached.geo.p.getTransform().getMatrix(lastPortalCheckin);
			return;
		}
		Transform tr=(portal==1)?p1:p2;
		invertedCheckin.set(lastPortalCheckin);
		invertedCheckin.invert();
		trtemp.set(invertedCheckin);
		tr.mul(trtemp,tr);
		tr.mul(attached.geo.p.getTransform(),tr);
		attached.geo.p.getTransform().getMatrix(lastPortalCheckin);
	}
	@Override
	public void alphaRender() {
		glDepthMask(false);
		if(placed1) {
			this.geofx.g.getRenderingShader().bind();
			this.geofx.g.getRenderingShader().setUniform1f("offset",2.3459837459f);
			this.geofx.g.highRender_customTransform(p1);
		}
		if(placed2) {
			this.geo2fx.g.getRenderingShader().bind();
			this.geo2fx.g.getRenderingShader().setUniform1f("offset",0f);
			this.geo2fx.g.highRender_customTransform(p2);
		}
		glDepthMask(true);
//		psys1.render();
//		psys2.render();
	}
	@Override
	public void render() {
		render(1);
		render(2);
	}
	public void apply() {
		if(placed1 && placed2) {
			portal(1);
			portal(2);
			return;
		}
		if(placed1) {
			Main.portalRenderRoutine(this,0,1);
		}
		if(placed2) {
			Main.portalRenderRoutine(this,0,2);
		}
	}
	public void render(int p) {
//		System.out.println(fadeAnim);
		if(p==1) {
			if(placed1) {
				this.geo.g.getRenderingShader().bind();
				this.geo.g.getRenderingShader().setUniform1f("offset",2.3459837459f);
				this.geo.g.getRenderingShader().setUniform1f("block",Math.min(fadeAnim,1.0f));
				this.geo.g.highRender_customTransform(p1);
			}
		} else if(p==2) {
			if(placed2) {
				this.geo2.g.getRenderingShader().bind();
				this.geo2.g.getRenderingShader().setUniform1f("offset",0f);
				this.geo2.g.getRenderingShader().setUniform1f("block",Math.min(fadeAnim,1.0f));
				this.geo2.g.highRender_customTransform(p2);
			}
		}
	}
	@Override
	public void initPhysics() {
		p1=new Transform(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))),new Vector3f(0,19,9.85f),1.0f));
		p2=new Transform(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,19,-9.85f),1.0f));
		updateDifferences();
	}
	@Override
	public void initGeo() {
//		psys1=new ParticleSystem();
//		psys2=new ParticleSystem();
//		
//		ParticleEmitter e1=new ParticleEmitter(new Vector3f(5,2,0),0.0f,new Vector3f(-1,-1,-1),200,new Vector3f(2,2,2));
//		ParticleEmitter e2=new ParticleEmitter(new Vector3f(-5,2,0),0.0f,new Vector3f(-1,-1,-1),200,new Vector3f(2,2,2));
//		psys1.addEmitter(e1);
//		psys2.addEmitter(e2);
//		
//		psys1.lock();
//		psys2.lock();
//		
//		psys1.initialize();
//		psys2.initialize();
		
//		psys1.setActive(false);
//		psys2.setActive(false);
		
		this.geo=new WorldObject(true);
		this.geo2=new WorldObject(true);
		this.geofx=new WorldObject(true,false);
		this.geo2fx=new WorldObject(true,false);
		this.geo.g.useTex=true;
		this.geo2.g.useTex=true;
		this.geofx.g.useTex=false;
		this.geo2fx.g.useTex=false;
		this.geo.g.loadOBJ("assets/3d/portal/portal_plane","assets/3d/portal/portal","png");
		this.geo2.g.loadOBJ("assets/3d/portal/portal_plane","assets/3d/portal/portal","png");
		this.geofx.g.loadOBJ("assets/3d/portal/portal_fx");
		this.geo2fx.g.loadOBJ("assets/3d/portal/portal_fx");
		float i=16.6f;
		float i2=6.6f;
		this.geo2.g.setColor(i,i*0.10f,0);
		this.geo.g.setColor(0,i*0.14f,i);
		this.geo2fx.g.setColor(i2,i2*0.10f,0,0.2f);
		this.geofx.g.setColor(0,i2*0.20f,i2,0.8f);
		this.geo.g.useLighting=false;
		this.geo2.g.useLighting=false;
		this.geofx.g.useLighting=false;
		this.geo2fx.g.useLighting=false;
		
		this.geofx.g.useCulling=false;
		this.geo2fx.g.useCulling=false;
		
		this.geo.g.scale(shape.x,shape.y,1);
		this.geo2.g.scale(shape.x,shape.y,1);
		this.geofx.g.scale(shape.x,shape.y,1);
		this.geo2fx.g.scale(shape.x,shape.y,1);
		
		geo.g.lock();
		geo2.g.lock();
		geofx.g.lock();
		geo2fx.g.lock();
		geo.p.setMotionSource(PhysicsObject.NONE);
		geo2.p.setMotionSource(PhysicsObject.NONE);
		initGObject(geo2);
		initGObject(geofx);
		initGObject(geo2fx);
		
		Shader portalShader=new Shader("specific/portal");
		Shader portalFXShader=new Shader("specific/portalfx");
		
		geo.g.setRenderingShader(portalShader);
		geo2.g.setRenderingShader(portalShader);
		geofx.g.setRenderingShader(portalFXShader);
		geo2fx.g.setRenderingShader(portalFXShader);
		
//		System.out.println(geo.g.vmap.tex.get(0));
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
	
}

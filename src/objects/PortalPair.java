package objects;

import static org.lwjgl.opengl.GL11.*;

import java.util.Arrays;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Shader;
import logger.Logger;
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
	public static final int STENCIL_ITERATIONS=5;
	public static final float FUNNELING_SCALE=3.0f;
	public static final float FUNNELING_DIST=20.0f;
	public static final float FUNNELING_VEL_THSHLD=2.0f;
	public static final float FUNNELING_FRC_SCALE=2.0f;
	public static final float FUNNELING_TARG_VEL_MAG=1.0f;
	public static final float FUNNELING_ANGLE_THSHLD=(float)Math.toRadians(20);
	public static final boolean FUNNELING_ENABLE=true;
	public static final float CLIP_SCALE=1.5f;
	public static final float CLIP_DISTANCE=0.0f;
	public static final float WALL_DISTANCE=0.005f;
	public static final float VEL_CLIP_MULTIPLIER=4.0f;
	public static final float VEL_MAG_STOP=2.0f;
	public static final float ALT_CLIP=2f;
	public static final float OPENING_ANIM_LEN_MICROS=500000.0f;
	public static final List<String> PASSES=Arrays.asList(new String[] {"Cube", "Player", "Pellet", "Turret"});
	public transient Transform p1;
	public transient Transform p2;
	private transient Transform diff;
	private transient Transform diffi;
	public boolean placed1=false;
	public boolean placed2=false;
	public Vector3f normal1=new Vector3f(0,0,-1);
	public Vector3f normal2=new Vector3f(0,0,-1);
	
	public Vector3f vel1=new Vector3f(0,0,0);
	public Vector3f vel2=new Vector3f(0,0,0);
	
	public transient GObject geo2;
	private transient GObject geofx;
	private transient GObject geo2fx;
	protected SaveStateComponent geo2ss;
	
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
//		Renderer.useGraphics=false;
//		glStencilFunc(GL_EQUAL,128,0xFF);
//		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
//		for(Thing thing : Renderer.things) { //       Render a depth pass
//			Renderer.renderRoutine(thing,stop(portal+(2*offset)-2));
//		}
//		Renderer.useGraphics=true;
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
		Renderer.useGraphics=false;
		Renderer.portalRenderRoutine(this,stop(portal+(2*offset)-2),portal); //              Render portal into stencil and depth
		
//		if(offset!=0) {glStencilFunc(GL_EQUAL,offset+1+base,0xFF);}
//		else {glStencilFunc(GL_EQUAL,offset+1+base,0xFF);}
//		glStencilOp(GL_KEEP,GL_KEEP,GL_DECR);
//		for(Thing thing : Renderer.things) { //       Inversely render our parent's reality to the stencil buffer
//			Renderer.renderRoutine(thing,stop(portal+(2*offset)-2));
//		}
//		glClear(GL_DEPTH_BUFFER_BIT);
//		for(int i=1;i<offset+1;i++) {
//			glStencilFunc(GL_ALWAYS,i+1+base,0xFF);
//			glStencilOp(GL_KEEP,GL_KEEP,GL_REPLACE);
//			for(Thing thing : Renderer.things) { //       Render a depth pass
//				Renderer.renderRoutine(thing,stop(portal+(2*i)-2));
//			}
//		}
		glColorMask(true, true, true, true);
		Renderer.useGraphics=true;
		
		this.drawInsides(portal,offset+1); //Recurse
		
		glClear(GL_DEPTH_BUFFER_BIT);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Renderer.things) { //                 Fake render step
			Renderer.renderRoutine(thing,portal+(2*offset));
		}
		for(Thing thing : Renderer.things) { //                 Fake render step
			Renderer.alphaRenderRoutine(thing,portal+(2*offset));
		}
		
		Renderer.portalRenderRoutine(this,stop(portal+(2*offset)-2),portal);
		
//		glStencilFunc(GL_LEQUAL,offset+1+base,0xFF);
//		for(Thing thing : Renderer.things) { //       Render our reality on top
//			Renderer.renderRoutine(thing,stop(portal+(2*offset)-2));
//			Renderer.alphaRenderRoutine(thing,stop(portal+(2*offset)-2));
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
		Renderer.useGraphics=false;
		Renderer.portalRenderRoutine(this,outside,portal); //              Render portal into stencil and depth
		
		glColorMask(true, true, true, true);
		Renderer.useGraphics=true;
		
		this.drawInsides(portal,offset+1); //Recurse
		
		glClear(GL_DEPTH_BUFFER_BIT);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Renderer.things) { //                 Fake render step
			Renderer.renderRoutine(thing,inside);
		}
		for(Thing thing : Renderer.things) { //                 Fake render step
			Renderer.alphaRenderRoutine(thing,inside);
		}
		Renderer.alphaRenderRoutine(this,inside);
		glStencilFunc(GL_EQUAL,offset+1+base,0xFF);
		Renderer.portalRenderRoutine(this,outside,portal);
		glStencilFunc(GL_EQUAL,0,0xFF);
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
		Renderer.portalRenderRoutine(this,outside,portal);
		glStencilFunc(GL_ALWAYS,0x02,0xFF);
		Renderer.portalRenderRoutine(this,inside,portal);
		glDepthMask(true);
		glColorMask(true, true, true, true);
		glStencilFunc(GL_EQUAL,0x01,0xFF);
		glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
		for(Thing thing : Renderer.things) {
			Renderer.renderRoutine(thing,inside);
		}
		for(Thing thing : Renderer.things) {
			Renderer.alphaRenderRoutine(thing,inside);
		}
		Renderer.alphaRenderRoutine(this,inside);
		glStencilFunc(GL_ALWAYS,0x00,0xFF);
		Renderer.portalRenderRoutine(this,outside,portal);
	}
	public void portal(int portal) {
		drawInsides(portal,0);
		glColorMask(false,false,false,false);
		Renderer.useGraphics=false;
		Renderer.portalRenderRoutine(this,0,1);
		Renderer.portalRenderRoutine(this,0,2);
		glColorMask(true, true, true, true);
		Renderer.useGraphics=true;
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
		this.shape=new Vector3f(1,1.8f,0.1f); //shape.z - wall "thickness"
		p1=new Transform(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))),new Vector3f(0,19,9.85f),1.0f));
		p2=new Transform(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,19,-9.85f),1.0f));
		isTest=false;
		updateDifferences();
	}
	public void updateDifferences() {
		Matrix4f i=new Matrix4f();
		i.invert(p2.getMatrix(new Matrix4f()));
		Matrix4f t=(Matrix4f)p1.getMatrix(new Matrix4f()).clone();
		
		Matrix3f mat3=new Matrix3f();
		t.getRotationScale(mat3);
		normal1=new Vector3f(0,0,-1);
		mat3.transform(normal1);
		normal1.normalize();
		
		t.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,0,0),1.0f));
		t.mul(i);
		
		diff=new Transform(t);
		
		//-------------------------------
		
		i=new Matrix4f();
		i.invert(p1.getMatrix(new Matrix4f()));
		t=(Matrix4f)p2.getMatrix(new Matrix4f()).clone();
		
		mat3=new Matrix3f();
		t.getRotationScale(mat3);
		normal2=new Vector3f(0,0,-1);
		mat3.transform(normal2);
		normal2.normalize();
		
		t.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,0,0),1.0f));
		t.mul(i);
		diffi=new Transform(t);
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
	public int rayTest_sc(Vector3f a, Vector3f b, float sc) {
		Transform np1=new Transform(new Matrix4f(p1.getRotation(new Quat4f()),p1.origin,sc));
		Transform np2=new Transform(new Matrix4f(p2.getRotation(new Quat4f()),p2.origin,sc));
		boolean po1=geo.rayTest(a,b,np1);
		boolean po2=geo2.rayTest(a,b,np2);
		if(po1 && !po2) {return 1;}
		if(po2 && !po1) {return 2;}
		if(po1 && po2) {return 3;}
		return 0;
	}
	public int funnelingRayTest(Vector3f a, Vector3f b) {
		return rayTest_sc(a,b,FUNNELING_SCALE);
	}
	private boolean pplaced;
	private long placedMicros=-1;
	private float fadeAnim=0;
	@Override
	public void logic() {
		if(placed1 && placed2) {
			if(!pplaced) {
				placedMicros=RenderUtils.micros();
				fadeAnim=0;
			}
		}
		if(placedMicros!=-1) {
			fadeAnim=(RenderUtils.micros()-placedMicros)/OPENING_ANIM_LEN_MICROS;
			if(fadeAnim>=1) {
				placedMicros=-1;
			}
		}
		pplaced=placed1&&placed2;
//		Vector3f mv1=(Vector3f)vel1.clone();
//		mv1.scale(1/RenderUtils.fr);
//		p1.mul(new Transform(new Matrix4f(Util.AxisAngle(new AxisAngle4f(1,0,0,0)),mv1,1.0f)));
//		
//		Vector3f mv2=(Vector3f)vel2.clone();
//		mv2.scale(1/RenderUtils.fr);
//		p2.mul(new Transform(new Matrix4f(Util.AxisAngle(new AxisAngle4f(1,0,0,0)),mv2,1.0f)));
	}
	@Override
	public void initVBO() {
		geo.initVBO();
		geo2.initVBO();
		geofx.initVBO();
		geo2fx.initVBO();
	}
	@Override
	public void refresh() {
		geo.refresh();
		geo2.refresh();
		geofx.refresh();
		geo2fx.refresh();
	}
	@Override
	public void alphaRender() {
		glDepthMask(false);
		if(placed1) {
			this.geofx.getRenderingShader().bind();
			this.geofx.getRenderingShader().setUniform1f("offset",2.3459837459f);
			this.geofx.highRender_noPushPop_customTransform(p1);
		}
		if(placed2) {
			this.geo2fx.getRenderingShader().bind();
			this.geo2fx.getRenderingShader().setUniform1f("offset",0f);
			this.geo2fx.highRender_noPushPop_customTransform(p2);
		}
		glDepthMask(true);
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
			Renderer.portalRenderRoutine(this,0,1);
		}
		if(placed2) {
			Renderer.portalRenderRoutine(this,0,2);
		}
	}
	public void render(int p) {
		if(p==1) {
			if(placed1) {
				this.geo.getRenderingShader().bind();
				this.geo.getRenderingShader().setUniform1f("offset",2.3459837459f);
				this.geo.getRenderingShader().setUniform1f("block",Math.min(fadeAnim,1.0f));
				this.geo.highRender_noPushPop_customTransform(p1);
			}
		} else if(p==2) {
			if(placed2) {
				this.geo2.getRenderingShader().bind();
				this.geo.getRenderingShader().setUniform1f("offset",0f);
				this.geo.getRenderingShader().setUniform1f("block",Math.min(fadeAnim,1.0f));
				this.geo2.highRender_noPushPop_customTransform(p2);
			}
		}
	}
	@Override
	public void initPhysics() {
		p1=new Transform(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))),new Vector3f(0,19,9.85f),1.0f));
		p2=new Transform(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,(float)Math.toRadians(180)))),new Vector3f(0,19,-9.85f),1.0f));
		updateDifferences();
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo2=new GObject();
		this.geofx=new GObject();
		this.geo2fx=new GObject();
		this.geo.useTex=true;
		this.geo2.useTex=true;
		this.geofx.useTex=false;
		this.geo2fx.useTex=false;
		this.geo.loadOBJ("portal/portal_plane","3d/portal/portal.png");
		this.geo2.loadOBJ("portal/portal_plane","3d/portal/portal.png");
		this.geofx.loadOBJ("portal/portal_fx");
		this.geo2fx.loadOBJ("portal/portal_fx");
		float i=16.6f;
		float i2=6.6f;
		this.geo2.setColor(i,i*0.10f,0);
		this.geo.setColor(0,i*0.14f,i);
		this.geo2fx.setColor(i2,i2*0.10f,0,0.2f);
		this.geofx.setColor(0,i2*0.20f,i2,0.8f);
		this.geo.useLighting=false;
		this.geo2.useLighting=false;
		this.geofx.useLighting=false;
		this.geo2fx.useLighting=false;
		
		this.geofx.useCulling=false;
		this.geo2fx.useCulling=false;
		
		this.geo.scale(1,1.8f,1);
		this.geo2.scale(1,1.8f,1);
		this.geofx.scale(1,1.8f,1);
		this.geo2fx.scale(1,1.8f,1);
		
		geo.lock();
		geo2.lock();
		geofx.lock();
		geo2fx.lock();
		geo.setMotionSource(GObject.NONE);
		geo2.setMotionSource(GObject.NONE);
		geofx.setMotionSource(GObject.NONE);
		geo2fx.setMotionSource(GObject.NONE);
		declareResource(geo2);
		declareResource(geofx);
		declareResource(geo2fx);
		
		Shader portalShader=new Shader("specific/portal");
		Shader portalFXShader=new Shader("specific/portalfx");
		
		geo.setRenderingShader(portalShader);
		geo2.setRenderingShader(portalShader);
		geofx.setRenderingShader(portalFXShader);
		geo2fx.setRenderingShader(portalFXShader);
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
	
}

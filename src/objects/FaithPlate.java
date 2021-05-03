package objects;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import anim.AnimParser;
import anim.AnimTrack;
import audio.Sound;
import graphics.GraphicsInit;
import graphics.Renderer;
import objectTypes.GObject;

public class FaithPlate extends Thing {
	private static final long serialVersionUID = 1237435512380248141L;
	public static final float ACTIVATION_RADIUS=0.5f;
	private Quat4f quat;
	private Vector3f origin;
	private Vector3f vel;
	
	private transient GObject camcase;
	private transient GObject campush;
	private transient GObject pcase;
	private transient GObject drumbase;
	private transient GObject piston;
	private transient GObject plate;
	private transient GObject sidecam;
	private transient GObject transparent;
	private transient GObject wallmask;
	private transient GObject wallhole;
	public FaithPlate(Vector3f origin, Quat4f quat, Vector3f velocity) {
		this.shape=new Vector3f();
		this.type="Faith_Plate";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
		vel=velocity;
	}
	private void initGObject(GObject in, String name) {
		in.loadOBJ("faithplate/"+name);
		in.useTex=false;
		in.lock();
		in.initVBO();
		in.refresh();
		in.setMotionSource(GObject.ANIMATION);
		in.animator.add("Launch",AnimParser.parse("3d/faithplate/"+name).setEndMode(AnimTrack.STAY));
		in.transformObject=this.geo;
		in.animator.synchronizedTransform=new Transform();
		in.animator.copyTransformPointer();
		in.animator.setActiveAnim("Launch");
		in.animator.getTrack().setReversed(true);
	}
	transient GObject[] list;
	private void setAllReversed(boolean in) {
		if(list==null) {
			list=new GObject[] {camcase,campush,pcase,drumbase,piston,plate,sidecam,transparent};
		}
		for(GObject g : list) {
			g.animator.getTrack().setReversed(in);
		}
	}
	private void stepGObject(GObject in) {
		in.animator.synchronizedTransform.set(in.transformObject.getTransform());
		in.animator.advanceTrack();
	}
	private Vector3f thingpos=new Vector3f();
	private boolean activated=false;
	private void runCheck(Thing thing) {
		if(!PortalPair.PASSES.contains(thing.type)) {
			return;
		}
		thingpos.set(thing.geo.getTransform().origin);
		thingpos.sub(geo.getTransform().origin);
		if(thingpos.length()<=(ACTIVATION_RADIUS+thing.getShape().length())) {
			thing.geo.body.setLinearVelocity(vel);
			activated=true;
		}
	}
	private boolean pactivated=false;
	@Override
	public void logic() {
		stepGObject(camcase);
		stepGObject(campush);
		stepGObject(pcase);
		stepGObject(drumbase);
		stepGObject(piston);
		stepGObject(plate);
		stepGObject(sidecam);
		stepGObject(transparent);
		activated=false;
		for(int i=0;i<Renderer.things.size();i++) {
			Thing thing=Renderer.things.get(i);
			runCheck(thing);
		}
		runCheck(GraphicsInit.player);
		if(activated) {
			setAllReversed(false);
		}
		if(camcase.animator.getTrack().animIsDone() && !camcase.animator.getTrack().isReversed()) {
			setAllReversed(true);
		}
		if(activated && !pactivated) {
			sourcePool.play("Trigger",soundtrack);
		}
		pactivated=activated;
	}
	@Override
	public void initSoundtrack() {
		soundtrack.put("Trigger","faithplate/trigger");
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
	}
	@Override
	public void render() {
		this.geo.highRender();
		camcase.highRender();
		campush.highRender();
		pcase.highRender();
		drumbase.highRender();
		piston.highRender();
		plate.highRender();
		sidecam.highRender();
		wallhole.highRender();
		wallmask.highRender();
		//transparent.highRender();
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("faithplate/base");
		this.geo.useTex=false;
		this.geo.setColor(0.3f,0.3f,0.31f,1.0f);
		geo.lock();
		
		this.wallmask=new GObject();
		wallmask.loadOBJ("faithplate/wallmask");
		wallmask.useTex=false;
		wallmask.setColor(1,0,1,0);
		wallmask.lock();
		wallmask.initVBO();
		wallmask.refresh();
		wallmask.transformObject=this.geo;
		wallmask.setMotionSource(GObject.OTHER);
		
		this.wallhole=new GObject();
		wallhole.loadOBJ("faithplate/wallhole");
		wallhole.useTex=false;
		wallhole.setColor(0.3f,0.3f,0.4f);
		wallhole.lock();
		wallhole.initVBO();
		wallhole.refresh();
		wallhole.transformObject=this.geo;
		wallhole.setMotionSource(GObject.OTHER);
		
		this.camcase=new GObject();
		this.campush=new GObject();
		this.pcase=new GObject();
		this.drumbase=new GObject();
		this.piston=new GObject();
		this.plate=new GObject();
		this.sidecam=new GObject();
		this.transparent=new GObject();
		initGObject(this.camcase,"camcase");
		camcase.setColor(0.26f,0.56f,0.96f);
		initGObject(this.campush,"campush");
		campush.setColor(0.65f,0.65f,0.65f);
		initGObject(this.pcase,"case");
		pcase.setColor(0.26f,0.56f,0.96f);
		initGObject(this.drumbase,"drumbase");
		drumbase.setColor(0.26f,0.56f,0.96f);
		initGObject(this.piston,"piston");
		piston.setColor(0.65f,0.65f,0.65f);
		initGObject(this.plate,"plate");
		plate.setColor(0.15f,0.15f,0.15f);
		initGObject(this.sidecam,"sidecam");
		sidecam.setColor(0.2f,0.2f,0.2f);
		initGObject(this.transparent,"transparent");
		
		camcase.refresh();
		campush.refresh();
		pcase.refresh();
		drumbase.refresh();
		piston.refresh();
		plate.refresh();
		sidecam.refresh();
		transparent.refresh();
		
		declareResource(camcase);
		declareResource(campush);
		declareResource(pcase);
		declareResource(drumbase);
		declareResource(piston);
		declareResource(plate);
		declareResource(sidecam);
		declareResource(transparent);
		declareResource(wallmask);
		declareResource(wallhole);
	}

}
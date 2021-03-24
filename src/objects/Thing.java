package objects;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import audio.Soundtrack;
import audio.SourcePool;
import graphics.GObject;
import graphics.GraphicsInit;
import graphics.Renderer;
import graphics.Shader;
import logger.Logger;
import physics.InputHandler;
import physics.Physics;
import portalcasting.Segment;
import util.SaveStateComponent;

public abstract class Thing implements Serializable {
	protected static final long serialVersionUID = 8139848476402960972L;
	
	public static int PORTAL_IMMUNITY=30;
	//If we want it to interact with: (masks)
	public static final short NOTHING   =0b000000000000000;
	public static final short PLAYER    =0b000000000000001;
	public static final short NPLAYER   =0b111111111111010;
	public static final short TESTGEO   =0b000000000000010;
	public static final short EVERYTHING=0b111111111111111;
	public static final short NEARPORTAL=0b000000000000100;
	public static final short AWAYPORTAL=0b111111111111011;
	
	protected static final RayResultCallback f=new RayResultCallback() {
		@Override
		public float addSingleResult(LocalRayResult res, boolean b) {
			Thing.rayTest.add(res);
			return 0;
		}
		
	};
	public static void runRayTest() {
		rayTest=new ArrayList<LocalRayResult>();
		Vector3f pt=new Vector3f(0,0,-2);
		Matrix3f rs=new Matrix3f();
		Renderer.camera.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
//		try {
//			rs.invert();
//		} catch (SingularMatrixException e) {
//			System.err.println("Thing.runRayTest: SingularMatrixException!");
//			System.out.println(rs);
//			return;
//		}
		rs.transform(pt);
		Physics.dynamicsWorld.rayTest(Renderer.camera.pos_out,new Vector3f(
				Renderer.camera.pos_out.x+pt.x,
				Renderer.camera.pos_out.y+pt.y,
				Renderer.camera.pos_out.z+pt.z)  ,f);
	}
	private HashMap<String,Integer> IdFieldAssoc;
	private transient ArrayList<GObject> resources;
	protected SaveStateComponent saveStateComponent;
	protected transient Soundtrack soundtrack;
	public transient SourcePool sourcePool;
	protected static ArrayList<LocalRayResult> rayTest=new ArrayList<LocalRayResult>();
	protected boolean funnelPrevUseGravity=true;
	public boolean funnelInFunnel=false;
	public transient Segment funnelActiveSegment=new Segment();
	protected boolean funnelCheckYes=false;
	protected boolean gravity=true;
	public boolean portalable=false;
	public boolean stopsPortals=false;
	public boolean portalingCollisionsEnabled=true;
	public boolean npflag=false;
	public boolean npflag2=false;
	public short oshmask=0;
	public short oshgroup=0;
	protected Vector3f constForce=new Vector3f(0,0,0);
	public boolean isTest=true;
	public boolean doPhysicsOnSerialization=true;
	public transient GObject geo;
	public String type;
	protected Vector3f shape;
	public Vector3f prevPos=new Vector3f();
	public short group=EVERYTHING;
	public int id;
	public int portalCounter;
	protected transient InputHandler in;
	public abstract void initPhysics();
	public abstract void initGeo();
	public void initSoundtrack() {}
	protected boolean interacted=false;
	public void processInteraction() {}
	public void logic() {portalCounter++;} //What do we do if we're activated?
	public void applyBackForce() {
		if(this.geo==null) {return;}
		if(this.geo.body==null) {return;}
		if(this.geo.dynamic) {this.geo.body.applyCentralForce(constForce);}
	}
	public void processActivation() {} //Process the following: Should we send out activations?
	public boolean sendingActivations=false;
	public int activations=0;
	public void generic_init() {}
	public transient HashSet<Thing> activates=new HashSet<Thing>();
	public HashSet<Integer> activates_ser=new HashSet<Integer>();
	public int activationThreshold=1;
	private int portalAttached=0;
	//public int selectionStage=0; //0: None. 1: Candidate. 2: Selected.
	public int pcasterHits=0;
	public boolean pcasterSendOnHit=false;
	public transient ArrayList<Thing> collisions;
	public transient ArrayList<Float> collisionVels;
	public transient ArrayList<Thing> pcollisions;
	public final void clearActivations() {this.activations=0; this.pcasterHits=0; this.sendingActivations=false;}
	private static Point3f tranVectorPointer=new Point3f();
	public boolean useModifiedCollision=false;
	public boolean runCollisionRayTest(Vector3f pos) {return false;}
	public void setAttachedNPCollisionFlag(boolean in) {}
	public ArrayList<GObject> getResources() {
		return resources;
	}
	public void declareResource(GObject g) {
		if(g==null) {return;}
		resources.add(g);
	}
	public void doSaveState() { //Due to a mandatory test protocol, we will stop taking transforms in constructors in 3, 2, 1.
		if(this.geo==null || this.geo.getTransformSource()!=GObject.PHYSICS) {return;}
		saveStateComponent=new SaveStateComponent();
		saveStateComponent.transform=new Matrix4f();
		saveStateComponent.velocity=new Vector3f();
		this.geo.getTransform().getMatrix(saveStateComponent.transform);
		this.geo.body.getLinearVelocity(saveStateComponent.velocity);
	}
	public final void requiredLogic() {
		if(this.geo==null) {return;}
		tranVectorPointer.set(this.geo.getTransform().origin);
		//System.out.println("B "+tranVectorPointer);
		GraphicsInit.player.getInverseTransform().transform(tranVectorPointer);
		//System.out.println("A "+tranVectorPointer);
		this.sourcePool.logic(tranVectorPointer);
	}
	public void sendActivations() {
		if(sendingActivations) {
			for(Thing t : activates) {
				t.activations+=1;
			}
		}
	}
//	public void clearSelectionStage(int i) {
//		if(i==0) {
//			selectionStage=0;
//		} else {
//			if(selectionStage==i) {
//				selectionStage=0;
//			}
//		}
//	}
//	public void handleSelectionStage() {
//		if(this.geo==null) {return;}
//		if(selectionStage==2) {
//			this.geo.wireframe=true;
//		} else {
//			this.geo.wireframe=false;
//		}
//	}
	public Thing addToActivates(Thing in) {
		this.activates.add(in);
		return this;
	}
	public Thing setPortalAttached(int i) { //DEBUG METHOD, only for testing hard-coded portal positioning
		this.portalAttached=i;
		return this;
	}
	public Thing setActivationThshld(int i) {
		this.activationThreshold=i;
		return this;
	}
	public void refresh() {
		geo.refresh();
	}
	public void render() {
		if(!this.geo.hasAlpha) {this.geo.highRender();}
	}
	public void alphaRender() {
		if(this.geo.hasAlpha) {this.geo.highRender();}
	}
	protected void onSerializationAdditional() {} //Trump just got sworn out of office. I have no idea why I'm typing that here, but I am so happy.
	public final void onSerialization() {
		this.init();
		if(doPhysicsOnSerialization) {
			this.addPhysics();
		}
		unpackSaveState();
		onSerializationAdditional();
	}
	public void unpackSaveState() {
		if(this.saveStateComponent!=null) {
			if(this.geo.getTransformSource()==GObject.PHYSICS) {
				this.geo.body.setWorldTransform(new Transform(saveStateComponent.transform));
				this.geo.body.setLinearVelocity(saveStateComponent.velocity);
				Logger.log(0,type+": Restoring rbstate from local saveStateComponent");
			}
		}
	}
	public void addPhysics() {
		addPhysics(group,Thing.EVERYTHING);
		if(portalAttached==1) {
			((Player)GraphicsInit.player).portalPair.attached1=this;
			Logger.log(0,"Setting portal attached 1 to type "+type);
		} else if(portalAttached==2) {
			((Player)GraphicsInit.player).portalPair.attached2=this;
			Logger.log(0,"Setting portal attached 2 to type "+type);
		}
	}
	public void addPhysics(short group, short mask) {
		this.geo.addToSimulation(group,mask);
		this.geo.body.setUserPointer(this);
	}
	public void initVBO() {
		geo.initVBO();
	}
	public void interact() {
		this.interacted=false;
		for(LocalRayResult l : rayTest) {
			if(((RigidBody)l.collisionObject).equals(this.geo.body)) {
				this.interacted=true;
			}
		}
		this.processInteraction();
	}
	protected final void initIdField() {IdFieldAssoc=new HashMap<String,Integer>();}
	public final void init() {
		if(this.geo!=null) {
			prevPos=geo.getTransform().origin;
		}
		resources=new ArrayList<GObject>();
		collisions=new ArrayList<Thing>();
		pcollisions=new ArrayList<Thing>();
		collisionVels=new ArrayList<Float>();
		if(IdFieldAssoc==null) {initIdField();}
		sourcePool=new SourcePool(this.type);
		soundtrack=new Soundtrack();
		funnelActiveSegment=new Segment();
		in=new InputHandler(Renderer.activeWindow);
		this.generic_init();
		this.initGeo();
		this.initPhysics();
		this.initSoundtrack();
		initVBO();
		declareResource(geo);
		
		refresh();
		refreshGravity();
	}
	public final void clean() {
		sourcePool.free();
		sourcePool.die();
		for(GObject g : resources) {
			g.clean();
			if(g.body!=null) {
				Physics.remove(g.body);
			}
		}
	}
	public Vector3f getShape() {
		return shape;
	}
	public void refreshGravity() {
		if(this.geo==null) {return;}
		if(this.geo.body==null) {return;}
		if(!this.gravity) {
			Vector3f g=Physics.getGravity();
			constForce=new Vector3f(-g.x,-g.y,-g.z);
		} else {
			constForce=new Vector3f(0,0,0);
		}
	}
	public Vector3f getConstForce() {
		return constForce;
	}
	public boolean usesGravity() {
		return gravity;
	}
	public void setUsesGravity(boolean gravity) {
		this.gravity=gravity;
		refreshGravity();
	}
	
	public HashMap<String,Integer> getIdFieldAssoc() {
		return IdFieldAssoc;
	}

	//SerializeByID
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface SerializeByID {
	}
}

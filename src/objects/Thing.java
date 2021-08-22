package objects;

import static lepton.engine.physics.WorldObject.pnull;

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
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import graphics.InstancedRenderConfig3d;
import graphics.PortalViewMatrixModifier;
import lepton.engine.audio.Soundtrack;
import lepton.engine.audio.SourcePool;
import lepton.engine.physics.PhysicsObject;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import lepton.engine.physics.WorldObject;
import lepton.util.advancedLogger.Logger;
import portalcasting.Segment;
import util.SaveStateComponent;

public abstract class Thing implements Serializable {
	public static PhysicsWorld defaultPhysicsWorld;
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
	private static transient Vector3f a=new Vector3f();
	public static void runRayTest(PhysicsWorld physicsWorld) {
		rayTest=new ArrayList<LocalRayResult>();
		Vector3f pt=new Vector3f(0,0,-2);
		Matrix3f rs=new Matrix3f();
		Main.camera.getTransform().getMatrix(new Matrix4f()).getRotationScale(rs);
		rs.transform(pt);
		a.set(
				Main.camera.pos_out.x+pt.x,
				Main.camera.pos_out.y+pt.y,
				Main.camera.pos_out.z+pt.z);
		physicsWorld.dynamicsWorld.rayTest(Main.camera.pos_out,a,f);
	}
	public transient InstancedThingParent instancedThingParent=null;
	public transient InstancedRenderConfig3d instancedRenderConfig=null;
	private transient PhysicsWorld physicsWorld=defaultPhysicsWorld;
	private HashMap<String,Integer> IdFieldAssoc;
	private transient ArrayList<WorldObject> gobjects;
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
	public boolean portalsIgnore=false;
	public boolean portalingCollisionsEnabled=true;
	
	public int currentPortalStatus=0;
	public int previousPortalStatus=0;
	
	public boolean npflag=false;
	public boolean npflag2=false;
	
	public short oshmask=0;
	public short oshgroup=0;
	
	public int id;
	
	public transient Matrix4f lastPortalCheckin1;
	public transient Matrix4f lastPortalCheckin2;
	
	protected Vector3f constForce=new Vector3f(0,0,0);
	
//	public boolean isTest=true;
	public boolean doPhysicsOnSerialization=true;
	
	public transient WorldObject geo;
	
	public String type;
	protected Vector3f shape;
	
	public Vector3f prevPos=new Vector3f();
	
	public short group=EVERYTHING;
	
//	public int id;
	
	public int portalCounter;
	
	
	public void initPhysics() {}
	public void initGeo() {}
	public void initSoundtrack() {}
	
	protected boolean interacted=false;
	public void processInteraction() {}
	
	public void logic() {} //What do we do if we're activated?
	public final void requiredLogic() {
		portalCounter++;
		if(this.geo==null) {return;}
		tranVectorPointer.set(this.geo.p.getTransform().origin);
		//System.out.println("B "+tranVectorPointer);
		PlayerInitializer.player.getInverseTransform().transform(tranVectorPointer);
		//System.out.println("A "+tranVectorPointer);
		this.sourcePool.logic(tranVectorPointer);
	}
	
	public void applyBackForce() {
		if(this.geo==null) {return;}
		if(this.geo.p.body==null) {return;}
		this.geo.p.body.applyCentralForce(constForce);
	}
	
	public void processActivation() {} //Process the following: Should we send out activations?
	public boolean sendingActivations=false;
	public int activations=0;
	public transient HashSet<Thing> activates=new HashSet<Thing>();
	public HashSet<Integer> activates_ser=new HashSet<Integer>();
	public int activationThreshold=1;
	public final void clearActivations() {this.activations=0; this.pcasterHits=0; this.sendingActivations=false;}
	public void sendActivations() {
		if(sendingActivations) {
			for(Thing t : activates) {
				t.activations+=1;
			}
		}
	}
	public Thing setActivationThshld(int i) {
		this.activationThreshold=i;
		return this;
	}
	
	private int portalAttached=0;
	
	public int pcasterHits=0;
	public boolean pcasterSendOnHit=false;
	
	public transient ArrayList<Thing> collisions;
	public transient ArrayList<Float> collisionVels;
	public transient ArrayList<Thing> pcollisions;
	
	private static Point3f tranVectorPointer=new Point3f();
	public boolean useModifiedCollision=false;
	
//	public boolean runCollisionRayTest(Vector3f pos) {return false;}
	
//	public void setAttachedNPCollisionFlag(boolean in) {}
	
	public ArrayList<WorldObject> getResources() {
		return gobjects;
	}
	
	public void initGObject(WorldObject geo2) {
		if(geo2==null) {return;}
		if(geo2!=geo) {
			gobjects.add(geo2);
		}

		if(geo2.g==null) {return;}
		PortalViewMatrixModifier p=new PortalViewMatrixModifier();
		geo2.g.viewMatrixModifier=p;
	}
	public boolean exemptFromChamberFeed=false;
	public void doSaveState() { //Due to a mandatory test protocol, we will stop taking transforms in constructors in 3, 2, 1.
		if(pnull(this.geo) || this.geo.p.getTransformSource()!=PhysicsObject.PHYSICS) {return;}
		saveStateComponent=new SaveStateComponent();
		saveStateComponent.transform=new Matrix4f();
		saveStateComponent.velocity=new Vector3f();
		this.geo.p.getTransform().getMatrix(saveStateComponent.transform);
		this.geo.p.body.getLinearVelocity(saveStateComponent.velocity);
	}
	
	public Thing addToActivates(Thing in) {
		this.activates.add(in);
		return this;
	}
	
	public Thing setPortalAttached(int i) { //DEBUG METHOD, only for testing hard-coded portal positioning
		this.portalAttached=i;
		return this;
	}
	public void refresh() {
		if(geo.g!=null) {
			geo.g.refresh();
			for(WorldObject g : gobjects) {
				g.g.refresh();
			}
		}
	}
	public void render() {
		if(!this.geo.g.hasAlpha) {this.geo.highRender();}
	}
	public void alphaRender() {
		if(this.geo.g.hasAlpha) {this.geo.highRender();}
	}
//	protected void onSerializationAdditional() {}
	
	public final void onSerialization() {
		physicsWorld=defaultPhysicsWorld;
		this.init();
		if(doPhysicsOnSerialization) {
			this.addPhysics();
		}
		unpackSaveState();
//		onSerializationAdditional();
	}
	
	public void unpackSaveState() {
		if(this.saveStateComponent!=null) {
			if(this.geo.p.getTransformSource()==PhysicsObject.PHYSICS) {
				this.geo.p.body.setWorldTransform(new Transform(saveStateComponent.transform));
				this.geo.p.body.setLinearVelocity(saveStateComponent.velocity);
				Logger.log(0,type+": Restoring rbstate from local saveStateComponent");
			}
		}
	}
	
	public void addPhysics() {
		addPhysics(group,Thing.EVERYTHING);
		if(portalAttached==1) {
			PlayerInitializer.player.portalPair.attached1=this;
			Logger.log(0,"Setting portal attached 1 to type "+type);
		} else if(portalAttached==2) {
			PlayerInitializer.player.portalPair.attached2=this;
			Logger.log(0,"Setting portal attached 2 to type "+type);
		}
	}
	protected void addPhysics(WorldObject w, short group, short mask) {
		w.p.addToSimulation(group,mask,physicsWorld);
		((UserPointerStructure)w.p.body.getUserPointer()).addUserPointer("thing",this);
	}
	public void addPhysics(short group, short mask) {
		addPhysics(geo,group,mask);
	}
	public void initVBO() {
		if(geo.g!=null) {
			geo.g.initVBO();
			for(WorldObject g : gobjects) {
				g.g.initVBO();
			}
		}
	}
	public void interact() {
		this.interacted=false;
		for(LocalRayResult l : rayTest) {
			if(((RigidBody)l.collisionObject).equals(this.geo.p.body)) {
				this.interacted=true;
			}
		}
		this.processInteraction();
	}
	protected final void initIdField() {IdFieldAssoc=new HashMap<String,Integer>();}
	public final void init() {
		if(this.geo!=null) {
			prevPos=geo.p.getTransform().origin;
		}
		gobjects=new ArrayList<WorldObject>();
		collisions=new ArrayList<Thing>();
		pcollisions=new ArrayList<Thing>();
		collisionVels=new ArrayList<Float>();
		if(IdFieldAssoc==null) {initIdField();}
		sourcePool=new SourcePool(this.type);
		soundtrack=new Soundtrack();
		funnelActiveSegment=new Segment();
		this.initGeo();
		this.initPhysics();
		this.initSoundtrack();
		initVBO();
		initGObject(geo);
		
		refresh();
		refreshGravity();
	}
	public final void clean() {
		sourcePool.free();
		sourcePool.die();
		for(WorldObject g : gobjects) {
			g.g.clean();
			if(g.p.body!=null) {
				physicsWorld.remove(g.p.body);
			}
		}
	}
	public PhysicsWorld getPhysicsWorld() {
		return physicsWorld;
	}
	public void setPhysicsWorld(WorldObject w, PhysicsWorld p) {
		RigidBodyEntry e=PhysicsWorld.getEntryFromRigidBody(w.p.body);
		if(physicsWorld.getBodies().contains(e)) {physicsWorld.remove(e.b);}
		if(!p.getBodies().contains(e)) {
			p.add(e);
		}
		physicsWorld=p;
	}
	public void setPhysicsWorld(PhysicsWorld p) {
		setPhysicsWorld(geo,p);
	}
	public Vector3f getShape() {
		return shape;
	}
	public void refreshGravity() {
		if(this.geo==null) {return;}
		if(this.geo.p.body==null) {return;}
		if(!this.gravity) {
			Vector3f g=physicsWorld.getGravity();
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

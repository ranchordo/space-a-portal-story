package game;

import static lepton.engine.rendering.GLContextInitializer.doCursor;
import static lepton.engine.rendering.GLContextInitializer.fr;
import static lepton.engine.rendering.GLContextInitializer.win;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.lwjgl.system.MemoryStack;

import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.NearCallback;

import console.Commands;
import console.JythonConsoleManager;
import debug.ContactPoint;
import debug.GenericCubeFactory;
import graphics.Camera;
import graphics.RenderFeeder;
import lepton.cpshlib.ComputeShader;
import lepton.engine.audio.Audio;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.physics.RigidBodyEntry;
import lepton.engine.physics.UserPointerStructure;
import lepton.engine.rendering.FrameBuffer;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.Screen;
import lepton.engine.rendering.Shader;
import lepton.engine.rendering.TextureCache;
import lepton.engine.rendering.lighting.BloomHandler;
import lepton.engine.rendering.lighting.Light;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.optim.objpoollib.PoolStrainer;
import lepton.util.CleanupTasks;
import lepton.util.InputHandler;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.ConsoleWindowHandler;
import lepton.util.advancedLogger.LogHandler;
import lepton.util.advancedLogger.LogLevel;
import lepton.util.advancedLogger.Logger;
import lepton.util.console.ConsoleWindow;
import objects.LightingConfiguration;
import objects.Player;
import objects.PortalPair;
import objects.Thing;
import objects.Wall;
import physics.LinkedPhysicsWorld;
import physics.MainPhysicsStepModifier;
import physics.Movement;
import physics.PortalNearCallback;
import util.Util;
import physics.Main2PhysicsStepModifier;

public class Main {
	public static final float volume=1.0f;
	public static final boolean isDesigner=true;
	public static void main(String[] args) {
		MainLoop();
	}
	
//	public static final int COMPUTE_SHADER=0x87;
//	public static final int GRAPHICAL_SHADER=0x88;
	public static long activeWindow;
	public static ArrayList<Thing> things=new ArrayList<Thing>();
	public static ArrayList<Thing> addSched=new ArrayList<Thing>();
	public static ArrayList<Thing> remSched=new ArrayList<Thing>();
//	public static boolean vr=false;
//	public static boolean useGraphics=true;
//	public static Texture activeTex=null;
	public static TextureCache activeCache=new TextureCache();
	public static Chamber activeChamber=new Chamber();
	public static Camera camera=new Camera();
//	public static Shader activeShader=null;
	public static ComputeShader activeComputeShader=null;
//	public static int shaderSwitch=GRAPHICAL_SHADER;
//	public static String dbg;
//	public static Shader main;
	private static boolean gcreq=false;
	public static void requestGC() {gcreq=true;}

	public static boolean debugRendering=true;

	public static float exposure=1.0f;
	public static float gamma=2.2f;
	public static float bloom_thshld=0.8f;

	public static int activePortalTransform=0;
//	public static long[] ts;

	public static SaveState scheduledReplacement=null;
	public static InputHandler in;
	
	public static JythonConsoleManager jythonConsoleHandler;
	public static LogHandler consoleWindowHandler;
	public static void renderRoutine(Thing thing, int portal) {
		activePortalTransform=portal;
		thing.render();
	}
	public static void alphaRenderRoutine(Thing thing, int portal) {
		activePortalTransform=portal;
		thing.alphaRender();
	}
	public static void portalRenderRoutine(PortalPair thing, int portal, int param) {
		activePortalTransform=portal;
		thing.render(param);
	}
	public static MemoryStack stack;
	public static PhysicsWorld physics;
	public static LinkedPhysicsWorld portalWorld;
	public static int pPortalBodies=0;
	public static PhysicsWorld dbgRenderWorld=null;
	private static GObject genericCube=null;
	public static void MainLoop() {
		stack=stackPush();
		Logger.setCleanupTask(()->CleanupTasks.cleanUp());
		CleanupTasks.add(()->Logger.log(0,"Cleaning up..."));
		CleanupTasks.add(()->GLContextInitializer.doCursor(win,false,false));
		CleanupTasks.add(()->GLContextInitializer.destroyGLContext());
		CleanupTasks.add(()->stack.close());
		CleanupTasks.add(()->Audio.cleanUp());
		
		physics=new PhysicsWorld();
		portalWorld=new LinkedPhysicsWorld();
		portalWorld.setWorld1(physics);
		Main2PhysicsStepModifier portalPhysicsStepModifier=new Main2PhysicsStepModifier(null);
//		PhysicsWorld portalWorld2=new PhysicsWorld();
//		portalWorld.setWorld2(portalWorld2);
		portalWorld.createWorld2(portalPhysicsStepModifier);
		portalPhysicsStepModifier.setPhysicsWorld(portalWorld.getWorld2());
		
		Thing.defaultPhysicsWorld=physics;
				
		ConsoleWindow mainConsoleWindow=new ConsoleWindow(false,800,600,"Space: A Portal Story - Debug console",(s)->Main.jythonConsoleHandler.recv(s),"Debug console ready.\nStarting the game shortly...");
		mainConsoleWindow.setVisible(true);
		consoleWindowHandler=new ConsoleWindowHandler(mainConsoleWindow);
		Logger.handlers.add(consoleWindowHandler);
		CleanupTasks.add(()->{if(LogLevel.isFatal()) {mainConsoleWindow.waitForClose();}});
		CleanupTasks.add(()->Main.jythonConsoleHandler.stopJython());
		CleanupTasks.add(()->mainConsoleWindow.close());
		CleanupTasks.add(()->Logger.handlers.remove(consoleWindowHandler));
		CleanupTasks.add(()->System.exit(0));
		
		jythonConsoleHandler=new JythonConsoleManager(mainConsoleWindow);
		jythonConsoleHandler.initJython();
		
		((CollisionDispatcher)portalWorld.getWorld2().dynamicsWorld.getDispatcher()).setNearCallback(new PortalNearCallback());
		
		GLContextInitializer.initializeGLContext(true,864,486,false,"Space - A Portal Story");
		LeptonUtil.locationReference=Main.class;
		activeWindow=win;
		
		MainPhysicsStepModifier mainPhysicsStepModifier=new MainPhysicsStepModifier(physics);
		physics.activePhysicsStepModifier=mainPhysicsStepModifier;
		
		in=new InputHandler(win);

		Audio.init();

		//			if(vr) {
			//				Logger.log(0,"Initializing VR, performing some info fetches:");
		//				vr=vr && VR_IsRuntimeInstalled();
		//				Logger.log(0,"VR_IsRuntimeInstalled() = " + VR_IsRuntimeInstalled());
		//		        Logger.log(0,"VR_RuntimePath() = " + VR_RuntimePath());
		//		        vr=vr && VR_IsHmdPresent();
		//		        Logger.log(0,"VR_IsHmdPresent() = " + VR_IsHmdPresent());
		//		        if(!vr) {
		//		        	Logger.log(1,"VR Check failed. Check the HmdPresent and RuntimeInstalled flags for more info.");
		//		        }
		//			}
		//			if(vr) {
		//		        IntBuffer peError=stack.mallocInt(1);
		//		        int token=VR_InitInternal(peError,0);
		//		        if(peError.get(0)==0) {
		//		        	OpenVR.create(token);
		//			        Logger.log(0,"Model Number : " + VRSystem_GetStringTrackedDeviceProperty(
		//		                    k_unTrackedDeviceIndex_Hmd,
		//		                    ETrackedDeviceProperty_Prop_ModelNumber_String,
		//		                    peError
		//		            ));
		//		            Logger.log(0,"Serial Number: " + VRSystem_GetStringTrackedDeviceProperty(
		//		            		k_unTrackedDeviceIndex_Hmd,
		//		                    ETrackedDeviceProperty_Prop_SerialNumber_String,
		//		                    peError
		//		            ));
		//		        } else {
		//		        	Logger.log(2,"VR INIT ERROR SYMB: "+VR_GetVRInitErrorAsSymbol(peError.get(0)));
		//		        	Logger.log(2,"VR INIT ERROR DESC: "+VR_GetVRInitErrorAsEnglishDescription(peError.get(0)));
		//		        	vr=false;
		//		        }
		//			}

		IntBuffer testAv=stack.mallocInt(1);
		glGetIntegerv(0x9048,testAv);


		float inte=10f;
		float r=255/255.0f;
		float g=250/255.0f;
		float b=244/255.0f;
		float amb=0.02f;

		Chamber test=new Chamber();
//		Thing hld=null;


		//PUT CHAMBER CONTENTS HERE:
		//hld=test.add(new Door(new Vector3f(9.7f,0.2f,5),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(0))),false));
		//test.add(new FaithPlate(new Vector3f(4,0.04f,-6),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(0))),new Vector3f(0,20,0)));
		test.add(new Wall(new Vector2f(50,10), new Vector3f(0,0,0), LeptonUtil.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90)))).setAspect(new Vector2f(0.5f,0.5f)).setTextureType(2));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(0,20,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(270)))).setAspect(new Vector2f(0.5f,0.5f)).setTextureType(2));
		test.add(new Wall(new Vector2f(10,10), new Vector3f(0,10,10), LeptonUtil.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(0,10,-10), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(180)))));
		//((Door)hld).setAttached(test.add(new Wall(new Vector2f(10,10), new Vector3f(10,10,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(90))))));
		//test.add(new Fizzler(new Vector2f(2,2), new Vector3f(5,2,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(90)))));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(-10,10,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(270)))));
		//test.add(new PortableWall(new Vector3f(0,6,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40)))));
//		test.add(new Cube(new Vector3f(0,5,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40))),Cube.NORMAL));
		//test.add(new ParticleThing(new ParticleEmitter(new Vector3f(0,2,0),0.0f,new Vector3f(-1,-1,-1),200,new Vector3f(2,2,2))));
		//test.add(new Cube(new Vector3f(0,2,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40))),0));
		//test.add(new Cube(new Vector3f(0,3,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40))),0));
		//test.add(new Shooter(new Vector3f(-1,5.15f,-6),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(270))),40000,true));
		//hld=test.add(new ConnectionFilter().addToActivates(hld));
		//hld=test.add(new Laser(new Vector3f(0,0.2f,1),Util.AxisAngle_np(new AxisAngle4f())));
		//test.add(new FloorButton(new Vector3f(2,0.8f,0),Util.AxisAngle_np(new AxisAngle4f(1,0,0,0))).addToActivates(hld));
		//test.add(new Trigger(new Vector3f(1,1,1),Util.AxisAngle_np(new AxisAngle4f()),new Vector3f(0,1,0)));
		//test.add(new Funnel(new Vector3f(-4,0.27f,-6),Util.AxisAngle_np(new AxisAngle4f())));
		//test.add(new LightBridge(new Vector3f(9,5f,1),Util.AxisAngle_np(new AxisAngle4f(0,0,1,(float)Math.toRadians(90)))));
		//test.add(new Turret(new Vector3f(0,1.2f,-6),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(0)))));
		//test.add(new ParticleSystem(new Vector3f(5,1.2f,-6), new Color(100,100,100,20), new Vector3f(0.2f,2,0.2f), 0.5f, 0.6f, 5000, new Vector3f(0,1,0), new Vector3f(0.02f,0.02f,0.02f), false, 0) {});
		//test.add(new ParticleSystem(new Vector3f(0,1.2f,0), new Color(242, 145, 53, 20), new Vector3f(0.2f,2,0.2f), 100, 0.2f, 1000, new Vector3f(0,0.4f,0), new Vector3f(0.02f,0.02f,0.02f), false, 0) {});
		test.add(new LightingConfiguration(
				new Light(Light.LIGHT_POSITION,-9,7,-9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,-9,7,9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,9,7,-9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,9,7,9, inte*r,inte*g,inte*b,1),
				//new Light(Light.LIGHT_DIRECTION,1,1,1, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_AMBIENT,0,0,0, amb,amb,amb,1)));

		//STOP PUTTING CHAMBER CONTENTS HERE
		String outputname="test"; //CHANGE CHMB NAME ("TEST" WILL EXPORT AS "TEST.CHMB")
		test.output(outputname);
		test=null;
		RenderFeeder.feed(Chamber.input(outputname));

		glEnable(GL_TEXTURE_2D);

		InputHandler in=new InputHandler(win);
		Movement.initHandler(win);
		Movement.initMovement();
		BloomHandler.init();
//		SSAOHandler.init();
		Shader screen=new Shader("screen");
		Shader screen_basic=new Shader("screen_basic");
		Screen blindfold=new Screen();
		GLContextInitializer.defaultMainShader=new Shader("mainShader");
//		main.bind();
		FrameBuffer fbo=new FrameBuffer(16,4,GL_RGBA16F);
		FrameBuffer interfbo=new FrameBuffer(0,2,GL_RGBA16F);
		FrameBuffer ssaoFBO=new FrameBuffer(0);
		FrameBuffer ssaoMulFBO=new FrameBuffer(0);
		FrameBuffer interfbo2=new FrameBuffer(0,2,GL_RGBA8);
		FrameBuffer interfbo3=new FrameBuffer(0,1,GL_RGBA8);
		camera.position(0.0f,Player.height,0.0f);
		//			Lighting.addLight(new Light(Light.LIGHT_DIRECTION,0,1,0, 0.8f,0.8f,0.8f,1));
		//			Lighting.addLight(new Light(Light.LIGHT_DIRECTION,0,-1,0, 0.2f,0.2f,0.2f,1));
		exposure=8.0f;
		gamma=1.0f;
//		Lighting.addLight(new Light(Light.LIGHT_POSITION, 0,-15,-7, inte*r, inte*g, inte*b, 1));
//		Lighting.addLight(new Light(Light.LIGHT_AMBIENT,0,0,0, amb,amb,amb,1));
		//Lighting.addLight(new Light(Light.LIGHT_POSITION, 0, 20, 0, inte*r, inte*g, inte*b, 1));
		//Lighting.addLight(new Light(Light.LIGHT_POSITION,0,7,0, inte*r,inte*g,inte*b,1));
		doCursor(win,true,true);
		int fcc=0;
		//glfwSwapInterval(0);
		//activeAtlas.atlas_tex.bind();
		
		PlayerInitializer.player=PlayerInitializer.initializePlayer();
		GLContextInitializer.cameraTransform=new Transform();
		
		while(!glfwWindowShouldClose(win)) {
			GLContextInitializer.timeCalcStart();
			PoolStrainer.clean();

			glfwPollEvents();
			if(in.i(GLFW_KEY_ESCAPE)) {
				Commands.requestClose();
			}
			int portalWorldSize=portalWorld.getWorld2().getBodies().size();
			if(portalWorldSize>0) {
				for(RigidBodyEntry rbe : portalWorld.getWorld1().getBodies()) {
					RigidBodyEntry lrbe=((RigidBodyEntry)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("linked_rbe"));
					if(portalWorld.getWorld2().getBodies().contains(lrbe)) {
						RigidBodyEntry alrbe=((RigidBodyEntry)((UserPointerStructure)lrbe.b.getUserPointer()).getUserPointers().get("additional_linked_rbe"));
						if(alrbe!=null) {
							portalWorldSize--;
						}
						portalWorldSize--;
					}
				}
			}
			if(portalWorldSize==0 && pPortalBodies>0) {
				Logger.log(0,"Deconstructing portal physics world");
				portalWorld.clearWorld2();
				if(portalWorld.getWorld2().getBodies().size()>0) {
					Logger.log(4,"Extra weird bodies in virtual portal physics world");
				}
			}
			if(portalWorldSize>0 && pPortalBodies==0) {
				Logger.log(0,"Rebuilding portal physics world");
				portalWorld.rebuildWorld2WithDuplicateStructures();
			}
			if(dbgRenderWorld!=null) {
				if(((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback() instanceof PortalNearCallback) {
					if(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints!=null) {
						Util.clearsafe(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints);
					}
				}
			}
			pPortalBodies=portalWorldSize;
			if(portalWorldSize==0) {
				physics.step();
			} else {
				portalWorld.step_linked();
			}
			
			Movement.movement();
			Thing.runRayTest(physics);

			fbo.bind();
//			main.bind();
			glEnable(GL_DEPTH_TEST);
			//activeAtlas.atlas_tex.upload();
			glStencilMask(0xFF);
			glClearColor(0,0,0,1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
//			Lighting.apply();
			glPushMatrix();
			//glColor4f(1,1,1,1);
			//camera.grender(true);
			GLContextInitializer.cameraTransform.set(camera.getInvTransform());
//			System.out.println(camera.getTransform().origin.y);
			glEnable(GL_TEXTURE_2D);
			fcc++;
			if(fcc==100) {
				fcc=0;
				StringBuilder dbg=new StringBuilder();
				dbg.append(String.format("%3.2f",((float)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(float)Runtime.getRuntime().totalMemory())*100.0f));
				dbg.append("% of ");
				dbg.append(Runtime.getRuntime().totalMemory()/1048576.0f);
				dbg.append("M, fr=");
				dbg.append(String.format("%3.2f", fr));
				dbg.append("fps");
				System.out.println(dbg.toString());
			}
			for(Thing thing : things) { //Interaction step
				thing.interact();
			}
			for(Thing thing : things) { //Process, send step
				thing.processActivation();
				thing.sendActivations();
			}
			for(Thing thing : things) { //Logic step
				thing.requiredLogic();
				thing.logic();
			}
			PlayerInitializer.player.logic();
			PortalPair pp=PlayerInitializer.player.portalPair;
//			pp.updateDifferences();
			pp.logic();
			for(Thing thing : things) { //Clear step
				thing.clearActivations();
			}
				
			pp.apply();
			for(Thing thing : things) { //Render step
				renderRoutine(thing,0);
			}
			for(Thing thing : things) { //Render step
				alphaRenderRoutine(thing,0);
			}
//			pp.alphaRender();
			glStencilFunc(GL_ALWAYS,128,0xFF);
			//renderRoutine(pp,0);
			glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
			
			if(dbgRenderWorld!=null) {
				if(((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback() instanceof PortalNearCallback) {
					if(((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints==null) {
						((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints=new ArrayList<PoolElement<ContactPoint>>();
					} else {
						for(PoolElement<ContactPoint> cp : ((PortalNearCallback)((CollisionDispatcher)dbgRenderWorld.dynamicsWorld.getDispatcher()).getNearCallback()).contactPoints) {
							PoolElement<Matrix4f> p=DefaultVecmathPools.matrix4f.alloc();
							PoolElement<Transform> tr=DefaultVecmathPools.transform.alloc();
							Util.clear(p.o());
							p.o().m00=0.1f;
							p.o().m11=0.1f;
							p.o().m22=0.1f;
							p.o().m33=0.1f;
							p.o().m03=cp.o().posA.x;
							p.o().m13=cp.o().posA.y;
							p.o().m23=cp.o().posA.z;
							tr.o().set(p.o());
							genericCube.setColor(cp.o().removed?1:0,cp.o().removed?0:1,0);
							genericCube.copyData(GObject.COLOR_DATA, GL_DYNAMIC_DRAW);
							genericCube.highRender_customTransform(tr.o());
							p.free();
							tr.free();
						}
					}
				}
				if(genericCube==null) {
					genericCube=GenericCubeFactory.createGenericCube();
					genericCube.wireframe=true;
				}
				glDisable(GL_DEPTH_TEST);
				for(RigidBodyEntry rbe : dbgRenderWorld.getBodies()) {
					Thing thing=(Thing)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("thing");
					if(thing!=null) {
						PoolElement<Matrix4f> p=DefaultVecmathPools.matrix4f.alloc();
						PoolElement<Matrix4f> p1=DefaultVecmathPools.matrix4f.alloc();
						PoolElement<Transform> tr=DefaultVecmathPools.transform.alloc();
						Util.clear(p.o());
						p.o().m00=thing.getShape().x;
						p.o().m11=thing.getShape().y;
						p.o().m22=thing.getShape().z;
						p.o().m33=1;
						rbe.b.getWorldTransform(tr.o()).getMatrix(p1.o());
						p.o().mul(p1.o(),p.o());
						tr.o().set(p.o());
						Boolean otherWorld=(Boolean)((UserPointerStructure)rbe.b.getUserPointer()).getUserPointers().get("other_world");
						if(otherWorld!=null) {
							genericCube.setColor(1,otherWorld?1:0,otherWorld?0:1);
						} else {
							genericCube.setColor(0,1,1);
						}
						genericCube.copyData(GObject.COLOR_DATA, GL_DYNAMIC_DRAW);
						genericCube.highRender_customTransform(tr.o());
						
						p.free();
						p1.free();
						tr.free();
					}
				}
				glEnable(GL_DEPTH_TEST);
			}

			fbo.blitTo(interfbo,0,0);
			//fbo.blitTo(ssaoMulFBO,3,0);
			interfbo2.bind();
			glClearColor(1,1,0,1);
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			screen_basic.setUniform1f("bloom_thshld",bloom_thshld);
			screen_basic.bind();
			interfbo.bindTexture(0);
			blindfold.render();
			glEnable(GL_DEPTH_TEST);

			interfbo2.blitTo(interfbo3,1);
			BloomHandler.blur(interfbo3,5);
			interfbo2.unbind();
			glClearColor(1,1,0,1);
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			screen.bind();
			screen.setUniform1f("exposure",exposure);
			screen.setUniform1f("gamma",gamma);
			interfbo2.bindTexture(0,0);
			interfbo3.bindTexture(0,1);
			ssaoFBO.bindTexture(0,2);
			ssaoMulFBO.bindTexture(0,3);
			blindfold.render();
			glEnable(GL_DEPTH_TEST);


			for(Thing s : remSched) {
				s.clean();
				if(!things.remove(s)) {
					Logger.log(2,"Scheduled Thing removal ("+s.type+") did not modify the Renderer set.");
				}
			}
			for(Thing s : addSched) {
				things.add(s);
			}
			remSched.clear();
			addSched.clear();
			//System.gc();
			//int targ_delta=1000000/(targ_fr+1); //Target frame time (us)
			//while(micros()-t1 < targ_delta) {} //Accurate sleep routine (to regulate framerate)


			glfwSwapBuffers(win);//Enforce V-SYNC
			//Handle replacement schedule
			if(scheduledReplacement!=null) {
				//Replace EVERYTHING
				for(Thing thing : things) {
					thing.clean();
				}
				PlayerInitializer.player.clean();
				PlayerInitializer.player.portalPair.clean();
				things.clear();
				requestGC();
				RenderFeeder.feed(scheduledReplacement.currentEnvironment);
				PlayerInitializer.player=scheduledReplacement.currentPlayer;
				things.add(PlayerInitializer.player);
				scheduledReplacement=null;
			}
			if(gcreq) {
				System.gc();
				gcreq=false;
			}
			GLContextInitializer.timeCalcEnd();
		}
		CleanupTasks.cleanUp();
	}
}
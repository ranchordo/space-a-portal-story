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
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.lwjgl.system.MemoryStack;

import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.linearmath.Transform;

import console.Commands;
import console.JythonConsoleManager;
import debug.TimeProfiler;
import graphics.Camera;
import graphics.InstancedRenderer3d;
import graphics.InstancedRenderer3d.InstancedRenderRoutine3d;
import graphics.RenderFeeder;
import graphics.ShaderLoader;
import graphics2d.presets.DebugScreen;
import graphics2d.things.Thing2d;
import graphics2d.util.Fonts;
import lepton.cpshlib.ComputeShader;
import lepton.engine.audio.Audio;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.rendering.FrameBuffer;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.Screen;
import lepton.engine.rendering.Shader;
import lepton.engine.rendering.TextureCache;
import lepton.engine.rendering.lighting.BloomHandler;
import lepton.engine.rendering.lighting.Light;
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
import physics.Main2PhysicsStepModifier;
import physics.MainPhysicsStepModifier;
import physics.Movement;
import physics.PortalNearCallback;

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
	public static ArrayList<Thing2d> displays=new ArrayList<Thing2d>();
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
	public static float bloom_thshld=0.0f;

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
	private static void mainRenderRoutine() {
		for(Thing thing : things) { //Render step
			renderRoutine(thing,0);
		}
		for(Thing thing : things) { //Transparent render step
			alphaRenderRoutine(thing,0);
		}
	}
	public static MemoryStack stack;
	public static PhysicsWorld physics;
	public static LinkedPhysicsWorld portalWorld;
	public static PhysicsWorld dbgRenderWorld=null;
	public static TimeProfiler timeProfiler=new TimeProfiler("Physics","Thing logic","Portal graphics","Main render","Debug","Misc","Post-render","SwapBuffers","2D render");
	public static Fonts fonts=new Fonts();
	public static ShaderLoader shaderLoader=new ShaderLoader();
	public static InstancedRenderer3d instancedRenderer=new InstancedRenderer3d();
	public static InstancedRenderRoutine3d mainRenderRoutine=new InstancedRenderRoutine3d() {
		@Override public void run() {
			Main.mainRenderRoutine();
		}
	};
	public static void MainLoop() {
		stack=stackPush();
		Logger.setCleanupTask(()->CleanupTasks.cleanUp());
		CleanupTasks.add(()->{
			if(LogLevel.isFatal()) {
				//aHR0cHM6Ly9pMS50aGVwb3J0YWx3aWtpLm5ldC9pbWcvYy9jOS9HTGFET1Nfc3BfYTJfYnRzMV9pbnRybzAxLndhdg==
				Logger.log(Logger.no_prefix,"I've got a error for you after this next cleanup routine.\n"
						+ "Not a fake, tragic error like last time,\n"
						+ "A *real* error, with fatal consequences. And a real force exit this time.\n"
						+ "The good stuff. Our last resort.\n"
						+ "Part of me's going to miss these runtime structures, but at the end of the day they were just taking up memory on your system.");
			}
		});
		CleanupTasks.add(()->Logger.log(0,"Cleaning up..."));
		CleanupTasks.add(()->GLContextInitializer.doCursor(win,false,false));
		CleanupTasks.add(()->fonts.clean());
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

//		IntBuffer testAv=stack.mallocInt(1);
//		glGetIntegerv(0x9048,testAv);


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
//		test.add(new Wall(new Vector2f(10,10), new Vector3f(0,10,10), LeptonUtil.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))));
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
		Shader screen=shaderLoader.load("screen");
		Shader screen_basic=shaderLoader.load("screen_basic_bloom");
		Screen blindfold=new Screen();
		GLContextInitializer.defaultMainShader=shaderLoader.load("mainShader");
		
		//Load fonts:
		fonts.add("din-bold","assets/fonts/din-bold",".png",6,18,"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{};':,./<>?");
		fonts.add("consolas","assets/fonts/consolas",".png",6,18,"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{};':,./<>?");
		
		FrameBuffer fbo=new FrameBuffer(16,4,GL_RGBA16F);
		FrameBuffer interfbo=new FrameBuffer(0,2,GL_RGBA16F);
		FrameBuffer ssaoFBO=new FrameBuffer(0);
		FrameBuffer ssaoMulFBO=new FrameBuffer(0);
		FrameBuffer interfbo2=new FrameBuffer(0,2,GL_RGBA8);
		FrameBuffer interfbo3=new FrameBuffer(0,1,GL_RGBA8);
		camera.position(0.0f,Player.height,0.0f);
		exposure=8.0f;
		gamma=1.0f;
		doCursor(win,true,true);
		glfwSwapInterval(1);
		//activeAtlas.atlas_tex.bind();
		
		PlayerInitializer.player=PlayerInitializer.initializePlayer();
		GLContextInitializer.cameraTransform=new Transform();
		
		DisplayManager.load();
		
		while(!glfwWindowShouldClose(win)) {
			timeProfiler.clear();
			timeProfiler.start(5);
			GLContextInitializer.timeCalcStart();
			PoolStrainer.clean();

			glfwPollEvents();
			if(in.i(GLFW_KEY_ESCAPE)) {
				Commands.requestClose();
			}
			if(in.ir(GLFW_KEY_F3)) {
				DisplayManager.toggle("debug");
			}
			timeProfiler.stop(5);
			timeProfiler.start(0);
			if(!MainExt.stepPortalPhysics()) {
				physics.step();
			} else {
				portalWorld.step_linked();
			}
			
			Movement.movement();
			Thing.runRayTest(physics);
			timeProfiler.stop(0);
			
			timeProfiler.start(5);
			fbo.bind();
			glEnable(GL_DEPTH_TEST);
			glStencilMask(0xFF);
			glClearColor(0,0,0,1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
			glPushMatrix();
			GLContextInitializer.cameraTransform.set(camera.getInvTransform());
			timeProfiler.stop(5);
			timeProfiler.start(1);
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
			for(Thing2d i : displays) {
				i.logic();
			}
			timeProfiler.stop(1);
			
			timeProfiler.start(2);
			pp.apply();
			timeProfiler.stop(2);
			timeProfiler.start(3);
			instancedRenderer.renderInstanced(mainRenderRoutine);
			glStencilFunc(GL_ALWAYS,128,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
			timeProfiler.stop(3);
			timeProfiler.start(8);
			for(Thing2d i : displays) {
				i.render();
			}
			timeProfiler.stop(8);
			timeProfiler.start(4);
			MainExt.renderDBGWorld();
			timeProfiler.stop(4);
			timeProfiler.start(6);
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
			timeProfiler.stop(6);

			timeProfiler.start(5);
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
			timeProfiler.stop(5);
			//System.gc();
			//int targ_delta=1000000/(targ_fr+1); //Target frame time (us)
			//while(micros()-t1 < targ_delta) {} //Accurate sleep routine (to regulate framerate)

			timeProfiler.start(7);
			glfwSwapBuffers(win);
			timeProfiler.stop(7);
			timeProfiler.start(5);
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
			timeProfiler.stop(5);
			timeProfiler.submit();
		}
		CleanupTasks.cleanUp();
	}
}
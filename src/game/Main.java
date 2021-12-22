package game;

import static lepton.engine.rendering.GLContextInitializer.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.util.ArrayList;
import java.util.Random;

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
import graphics2d.things.Thing2d;
import graphics2d.util.Fonts;
import lepton.cpshlib.ComputeShader;
import lepton.engine.audio.Audio;
import lepton.engine.physics.PhysicsWorld;
import lepton.engine.rendering.FrameBuffer;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.InstanceAccumulator;
import lepton.engine.rendering.Screen;
import lepton.engine.rendering.Shader;
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
import leveldesigner.LevelDesigner;
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
	public static float volume=1.0f;
	public static final boolean isDesigner=true;
	public static void main(String[] args) {
		try {
			System.out.println("Starting...");
			MainLoop();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.log(4,e.toString(),e);
		}
	}
	
	public static long activeWindow;
	public static ArrayList<Thing> things=new ArrayList<Thing>();
	public static ArrayList<Thing2d> displays=new ArrayList<Thing2d>();
	public static ArrayList<Thing> addSched=new ArrayList<Thing>();
	public static ArrayList<Thing> remSched=new ArrayList<Thing>();
	public static Chamber activeChamber=null;
	public static Camera camera=new Camera();
	public static ComputeShader activeComputeShader=null;
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
	
	public static boolean inDesignerCycle=false;
	
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
		PlayerInitializer.player.portalPair.apply();
		timeProfiler.stop(3);
		timeProfiler.start(9);
	}
	public static MemoryStack stack;
	public static PhysicsWorld physics;
	public static LinkedPhysicsWorld portalWorld;
	public static PhysicsWorld dbgRenderWorld=null;
	public static TimeProfiler timeProfiler=new TimeProfiler("Physics","Thing logic","Portal graphics","Main render","Debug","Misc","Post-render","SwapBuffers","2D render","Main render - instGPU");
	public static Fonts fonts=new Fonts();
	public static ShaderLoader shaderLoader=new ShaderLoader();
	public static InstancedRenderer3d instancedRenderer=new InstancedRenderer3d();
	public static boolean randomClear=false;
	public static Random mainRandom=new Random();
	public static long frameCount=0;
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
		
//		InstanceAccumulator.runAggressiveChangeCheckDefault=true;
		InstanceAccumulator.mergeSSBOsOnDuplicate=InstanceAccumulator.NO_MERGE;
		
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
		
//		String[] extensions=glGetString(GL_EXTENSIONS).split(" ");
//		for(String s : extensions) {
//			Logger.log(0,"Found GL extension: "+s);
//		}
		
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
		
		if(Main.isDesigner) {
			if(!Chamber.fileExists(LevelDesigner.outputname)) {
				Chamber designerTemplate=new Chamber();
				LevelDesigner.initDesignerChamber(designerTemplate);
			}
			RenderFeeder.feed(Chamber.input(LevelDesigner.outputname));
		}
		
		
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
//		activeAtlas.atlas_tex.bind();
		
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
			
			Thing.runRayTest(physics);
			timeProfiler.stop(0);
			
			timeProfiler.start(5);
			fbo.bind();
			glEnable(GL_DEPTH_TEST);
			glStencilMask(0xFF);
			if(!randomClear) {
				glClearColor(0,0,0,1);
			} else {
				glClearColor(mainRandom.nextInt(1000)/1000.0f,mainRandom.nextInt(1000)/1000.0f,mainRandom.nextInt(1000)/1000.0f,1);
			}
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
			//Portal graphics would go here
			timeProfiler.stop(2);
			timeProfiler.start(3);
			instancedRenderer.renderInstanced(mainRenderRoutine);
			glStencilFunc(GL_ALWAYS,128,0xFF);
			glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
			timeProfiler.stop(9);
			timeProfiler.start(8);
			for(Thing2d i : displays) {
				i.render();
			}
			timeProfiler.stop(8);
			timeProfiler.start(4);
			MainExt.renderDBGWorld();
			if(Main.isDesigner) {
				LevelDesigner.onFrame();
			}
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
			frameCount++;
		}
		CleanupTasks.cleanUp();
	}
}
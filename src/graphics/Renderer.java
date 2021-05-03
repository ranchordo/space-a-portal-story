package graphics;

import static graphics.RenderUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
//import static org.lwjgl.openvr.VR.*;
//import static org.lwjgl.openvr.VRSystem.*;
import static org.lwjgl.system.MemoryStack.*;

//import org.lwjgl.openvr.OpenVR;
import org.lwjgl.system.*;

import particles.*;

import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import audio.Audio;
import chamber.Chamber;
import game.SaveState;
import graphics.handlers.BloomHandler;
import graphics.handlers.SSAOHandler;
import lighting.Light;
import lighting.Lighting;
import logger.Logger;
import objects.*;
import physics.InputHandler;
import physics.Movement;
import physics.Physics;
import physics.State;
import pooling.PoolStrainer;
import util.ComputeShader;
import util.TextureCache;
import util.Util;

public class Renderer {
	public static final int COMPUTE_SHADER=0x87;
	public static final int GRAPHICAL_SHADER=0x88;
	public static long activeWindow;
	public static ArrayList<Thing> things=new ArrayList<Thing>();
	public static ArrayList<Thing> addSched=new ArrayList<Thing>();
	public static ArrayList<Thing> remSched=new ArrayList<Thing>();
	public static boolean vr=false;
	public static boolean useGraphics=true;
	public static Texture activeTex=null;
	public static TextureArray activeTexArray=null;
	public static TextureCache activeCache=new TextureCache();
	public static Chamber activeChamber=new Chamber();
	public static State camera=new State();
	public static Shader activeShader=null;
	public static ComputeShader activeComputeShader=null;
	public static int shaderSwitch=GRAPHICAL_SHADER;
	public static String dbg;
	public static Transform camtr=new Transform();
	public static Shader main;
	private static boolean gcreq=false;
	public static void requestGC() {gcreq=true;}
	
	public static boolean debugRendering=true;
	
	public static float exposure=1.0f;
	public static float gamma=2.2f;
	public static float bloom_thshld=0.8f;
	
	public static int activePortalTransform=0;
	public static long[] ts;
	
	public static SaveState scheduledReplacement=null;
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
	public static void Main() {
		long win=initGL(false);
		activeWindow=win;
		Physics.initPhysics();
		GraphicsInit.InitGraphics(win);
		stack=stackPush();
		
		Audio.init();
		
//		if(vr) {
//			Logger.log(0,"Initializing VR, performing some info fetches:");
//			vr=vr && VR_IsRuntimeInstalled();
//			Logger.log(0,"VR_IsRuntimeInstalled() = " + VR_IsRuntimeInstalled());
//	        Logger.log(0,"VR_RuntimePath() = " + VR_RuntimePath());
//	        vr=vr && VR_IsHmdPresent();
//	        Logger.log(0,"VR_IsHmdPresent() = " + VR_IsHmdPresent());
//	        if(!vr) {
//	        	Logger.log(1,"VR Check failed. Check the HmdPresent and RuntimeInstalled flags for more info.");
//	        }
//		}
//		if(vr) {
//	        IntBuffer peError=stack.mallocInt(1);
//	        int token=VR_InitInternal(peError,0);
//	        if(peError.get(0)==0) {
//	        	OpenVR.create(token);
//		        Logger.log(0,"Model Number : " + VRSystem_GetStringTrackedDeviceProperty(
//	                    k_unTrackedDeviceIndex_Hmd,
//	                    ETrackedDeviceProperty_Prop_ModelNumber_String,
//	                    peError
//	            ));
//	            Logger.log(0,"Serial Number: " + VRSystem_GetStringTrackedDeviceProperty(
//	            		k_unTrackedDeviceIndex_Hmd,
//	                    ETrackedDeviceProperty_Prop_SerialNumber_String,
//	                    peError
//	            ));
//	        } else {
//	        	Logger.log(2,"VR INIT ERROR SYMB: "+VR_GetVRInitErrorAsSymbol(peError.get(0)));
//	        	Logger.log(2,"VR INIT ERROR DESC: "+VR_GetVRInitErrorAsEnglishDescription(peError.get(0)));
//	        	vr=false;
//	        }
//		}
		
		IntBuffer testAv=stack.mallocInt(1);
		glGetIntegerv(0x9048,testAv);
		
		
		float inte=10f;
		float r=255/255.0f;
		float g=250/255.0f;
		float b=244/255.0f;
		float amb=0.02f;
		
		Chamber test=new Chamber();
		Thing hld=null;
		
		
		//PUT CHAMBER CONTENTS HERE:
		//hld=test.add(new Door(new Vector3f(9.7f,0.2f,5),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(0))),false));
		//test.add(new FaithPlate(new Vector3f(4,0.04f,-6),Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(0))),new Vector3f(0,20,0)));
		test.add(new Wall(new Vector2f(50,10), new Vector3f(0,0,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90)))).setAspect(new Vector2f(0.5f,0.5f)).setTextureType(2));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(0,20,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(270)))).setAspect(new Vector2f(0.5f,0.5f)).setTextureType(2));
		test.add(new Wall(new Vector2f(10,10), new Vector3f(0,10,10), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(0)))));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(0,10,-10), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(180)))));
		//((Door)hld).setAttached(test.add(new Wall(new Vector2f(10,10), new Vector3f(10,10,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(90))))));
		//test.add(new Fizzler(new Vector2f(2,2), new Vector3f(5,2,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(90)))));
		//test.add(new Wall(new Vector2f(10,10), new Vector3f(-10,10,0), Util.AxisAngle_np(new AxisAngle4f(0,1,0,(float)Math.toRadians(270)))));
		//test.add(new PortableWall(new Vector3f(0,6,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40)))));
		test.add(new Cube(new Vector3f(0,5,0), Util.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(40))),Cube.NORMAL));
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
		SSAOHandler.init();
		main=new Shader("main");
		Shader screen=new Shader("screen");
		Shader screen_basic=new Shader("screen_basic");
		Screen blindfold=new Screen();
		main.bind();
		FrameBuffer fbo=new FrameBuffer(16,4,GL_RGBA16F);
		FrameBuffer interfbo=new FrameBuffer(0,2,GL_RGBA16F);
		FrameBuffer ssaoFBO=new FrameBuffer(0);
		FrameBuffer ssaoMulFBO=new FrameBuffer(0);
		FrameBuffer interfbo2=new FrameBuffer(0,2,GL_RGBA8);
		FrameBuffer interfbo3=new FrameBuffer(0,1,GL_RGBA8);
		activeTexArray=new TextureArray();
		camera.position(0.0f,Player.height,0.0f);
//		Lighting.addLight(new Light(Light.LIGHT_DIRECTION,0,1,0, 0.8f,0.8f,0.8f,1));
//		Lighting.addLight(new Light(Light.LIGHT_DIRECTION,0,-1,0, 0.2f,0.2f,0.2f,1));
		exposure=8.0f;
		gamma=1.0f;
		//Lighting.addLight(new Light(Light.LIGHT_POSITION, 0, 20, 0, inte*r, inte*g, inte*b, 1));
		//Lighting.addLight(new Light(Light.LIGHT_POSITION,0,7,0, inte*r,inte*g,inte*b,1));
		doCursor(win,true,true);
		int fcc=0;
		//glfwSwapInterval(0);
		//activeAtlas.atlas_tex.bind();
		while(!glfwWindowShouldClose(win)) {
			RenderUtils.frameCount++;
			
			ts=new long[]{micros()};
			long t1=micros();
			
			PoolStrainer.clean();
			
			glfwPollEvents();
			if(in.i(GLFW_KEY_ESCAPE)) {
				glfwSetWindowShouldClose(win,true);
			}
			Physics.step();
			Movement.movement();
			Thing.runRayTest();
			
			fbo.bind();
			main.bind();
			glEnable(GL_DEPTH_TEST);
			//activeAtlas.atlas_tex.upload();
			glStencilMask(0xFF);
			glClearColor(0,0,0,1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
			Lighting.apply();
			glPushMatrix();
			//glColor4f(1,1,1,1);
			//camera.grender(true);
			camtr.set(camera.getInvTransform());
			glEnable(GL_TEXTURE_2D);
			fcc++;
			if(fcc==100) {
				fcc=0;
				dbg=""; //Debug text!
				dbg+=String.format("%3.2f",((float)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(float)Runtime.getRuntime().totalMemory())*100.0f);
				dbg+="% of ";
				dbg+=Runtime.getRuntime().totalMemory()/1048576.0f;
				dbg+="M, fr=";
				dbg+=String.format("%3.2f", fr);
				dbg+="/"+targ_fr+"fps, ufr=";
				dbg+=String.format("%3.2f", ufr);
				System.out.println(dbg);
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
			GraphicsInit.player.logic();
			PortalPair pp=((Player)GraphicsInit.player).portalPair;
			pp.logic();
			for(Thing thing : things) { //Clear step
				thing.clearActivations();
			}
			activeTex=null;
			
			pp.apply();
			for(Thing thing : things) { //Render step
				renderRoutine(thing,0);
			}
			for(Thing thing : things) { //Render step
				alphaRenderRoutine(thing,0);
			}
			pp.alphaRender();
			glStencilFunc(GL_ALWAYS,128,0xFF);
			//renderRoutine(pp,0);
			glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
			
			
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
			//Bunch o' stuff about framerate
			float ufp=(micros()-t1);
			ufr=1000000.0f/ufp;
			
			fp=(micros()-t1);
			fr=1000000.0f/fp; //Calculate framerate and framerate compensation
			frc=fr/60.0f;
			if(frc<0.33f) {
				frc=0.33f;
			}
			//Handle replacement schedule
			if(scheduledReplacement!=null) {
				//Replace EVERYTHING
				for(Thing thing : things) {
					thing.clean();
				}
				GraphicsInit.player.clean();
				GraphicsInit.player.portalPair.clean();
				things.clear();
				requestGC();
				RenderFeeder.feed(scheduledReplacement.currentEnvironment);
				GraphicsInit.player=scheduledReplacement.currentPlayer;
				things.add(GraphicsInit.player);
				scheduledReplacement=null;
			}
			if(gcreq) {
				System.gc();
				gcreq=false;
			}
		}
		doCursor(win,false,false);
		glfwTerminate();
		stack.close();
		Audio.cleanUp();
	}
	
}

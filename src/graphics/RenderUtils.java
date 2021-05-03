package graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

import java.nio.FloatBuffer;

import javax.vecmath.Matrix4f;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;


import audio.Audio;
import audio.Source;
import logger.Logger;
import util.ImageUtil;

public class RenderUtils {
	public static int windowedW=864;
	public static int windowedH=486;
	public static int windowedX=200;
	public static int windowedY=200;
	public static int fullW=0;
	public static int fullH=0;
	public static int winW=1920;
	public static int winH=1080;

	public static long fp=0;
	public static float fr=60;
	public static float frc=1;
	
	public static float ufr=60;

	public static long frameCount=0;

	public static float aspectRatio=16.0f/9.0f;
	public static float hrc=1;
	public static float wrc=1;
	public static float mrc=1;

	public static int targ_fr=60;

	public static boolean fullscreen=true;
	public static float fov=70;

	public static Matrix4f proj_matrix;
	private static void glPerspective(float fov, float aspect, float n, float f) {
		//float fH = (float) Math.tan(fov / 360 * Math.PI) * n;
		//float fW = fH * aspect;
		//glFrustum( -fW, fW, -fH, fH, n, f );
		//           l,  r,   b,  t, n, f
		Matrix4f res=new Matrix4f();
		float tanHalfFovy = (float) Math.tan(Math.toRadians(fov) * 0.5);
        res.m00 = 1.0f / (aspect * tanHalfFovy);
        res.m01 = 0.0f;
        res.m02 = 0.0f;
        res.m03 = 0.0f;
        res.m10 = 0.0f;
        res.m11 = 1.0f / tanHalfFovy;
        res.m12 = 0.0f;
        res.m13 = 0.0f;
        res.m20 = 0.0f;
        res.m21 = 0.0f;
        res.m22 = -(f + n) / (f - n);
        res.m23 = -1.0f;
        res.m30 = 0.0f;
        res.m31 = 0.0f;
        res.m32 = -2.0f * f * n / (f - n);
        res.m33 = 0.0f;
        //res.transpose();
        //res=Util.setPerspective(fov,aspect,n,f);
//        res=new Matrix4f(
//        		1,  0,  0    ,  0,
//        		0,  1,  0    ,  0,
//        		0,  0, -1.222f, -1,
//        		0,  0, -2.222f,  0
//        		);
        //res.transpose();
		proj_matrix=res;
	}
	static FloatBuffer fm=BufferUtils.createFloatBuffer(16);
	static float[] mma=new float[16];
	public static void setFOV(float f) {
		glMatrixMode(GL_PROJECTION_MATRIX); 
		glLoadIdentity();
		glPerspective(f,((float)winW)/((float)winH),0.05f,500f);
		
		glMatrixMode(GL_MODELVIEW_MATRIX);
	}
	public static void getFullscDimensions() {
		GLFWVidMode d=glfwGetVideoMode(glfwGetPrimaryMonitor());
		fullW=d.width();
		fullH=d.height();
		targ_fr=d.refreshRate();
	}
	public static long doWindow(boolean fullscreen, long prevwin) {
		if(!glfwInit()) {
			System.err.println("GLFWInit Error.");
			System.exit(1);
		}
		Logger.log(0,"InitGL: Getting specifications of your monitor:");
		getFullscDimensions();
		winW=fullW;
		winH=fullH;
		aspectRatio=(float)winW/(float)winH;
		hrc=winH/1080.0f;
		wrc=winW/1920.0f;
		mrc=Math.min(hrc,wrc);
		Logger.log(0,"Resolution: "+winW+"x"+winH);
		Logger.log(0,"Refresh rate / target frame rate: "+targ_fr+"Hz");
		long win;
		if(fullscreen) {
			win=glfwCreateWindow(winW,winH,"Space: A Portal Story",glfwGetPrimaryMonitor(),prevwin);
		} else {
			win=glfwCreateWindow(windowedW,windowedH,"Space: A Portal Story",0,prevwin);
			winW=windowedW;
			winH=windowedH;
		}
		if(win==0) {
			Logger.log(4,"win is 0, maybe there was an init error?");
		}
		glfwShowWindow(win);
		glfwMakeContextCurrent(win);
		GL.createCapabilities();
		return win;
	}
	static GLFWErrorCallback onError=new GLFWErrorCallback() {

		@Override
		public void invoke(int arg0, long arg1) {
			Logger.log(3,"GLFW error callback: "+arg0+", long "+arg1);
		}

	};
	static GLFWWindowSizeCallback onResize=new GLFWWindowSizeCallback() {
		public void invoke(long win, int w,  int h) {
			if(w==fullW && h==fullH) {
				glfwSetWindowMonitor(win,glfwGetPrimaryMonitor(),0,0,w,h,targ_fr);
				fullscreen=true;
			} else {
				glfwSetWindowMonitor(win,0,windowedX,windowedY,w,h,targ_fr);
				windowedW=w;
				windowedH=h;
				fullscreen=false;
			}
			winW=w;
			winH=h;
			aspectRatio=(float)winW/(float)winH;
			hrc=winH/1080.0f;
			wrc=winW/1920.0f;
			mrc=Math.min(hrc,wrc);
			glViewport(0,0,winW,winH);
			setFOV(fov);
			ImageUtil.clearCache();
		}
	};
	static GLFWWindowPosCallback onMove=new GLFWWindowPosCallback() {
		public void invoke(long win, int x, int y) {
			windowedX=x;
			windowedY=y;
			if(fullscreen) {
				glfwSetWindowMonitor(win,glfwGetPrimaryMonitor(),x,y,winW,winH,targ_fr);
			} else {
				glfwSetWindowMonitor(win,0,x,y,winW,winH,targ_fr);
			}
		}
	};
	public static long initGL(boolean fullscreen) { //Initialize our OpenGL bindings (Camera...)
		long win=doWindow(fullscreen,0);
		//glClearColor(1.0f,0.0f,0.0f,0.0f);

		glShadeModel(GL_SMOOTH);
		//glEnable(GL_LIGHT2);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glEnable(GL_COLOR_MATERIAL);
		glColorMaterial(GL_FRONT_AND_BACK, GL_POSITION);
		glEnable(GL_COLOR_MATERIAL);
		glEnable(GL_MULTISAMPLE);

		glClearColor(0.0f, 0.0f, 0.0f, 0.0f); 
		glClearDepth(1.0); 
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL); 

		glEnable(GL_STENCIL_TEST);
		glEnable(GL_CULL_FACE);

		setFOV(fov);
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

		glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
		glfwSetWindowSizeCallback(win,onResize);
		glfwSetWindowPosCallback(win, onMove);
		Logger.log(1,"InitGL: Completed successfully. Window "+win+" created.");
		return win;
	}
	public static void doCursor(long win, boolean grabbed, boolean hidden) {
		if(!grabbed && !hidden) {glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL);}
		if(!grabbed && hidden) {glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);}
		if(grabbed && hidden) {glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);}
	}
	public static Source initAL() { //Init audio
		Audio.init();
		return new Source();
	}
	public static long millis() { //I'm used to timing with Arduino, okay?
		return System.nanoTime()/1000000l;
	}
	public static long micros() { //I'm used to timing with Arduino, okay?
		return System.nanoTime()/1000l;
	}
}

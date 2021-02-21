package graphics.handlers;

import java.nio.FloatBuffer;
import java.util.Random;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL46.*;

import graphics.FrameBuffer;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Screen;
import graphics.Shader;
import graphics.TextureImage;

public class SSAOHandler {
	public static final int numSamples=64;
	public static final int noise_side=6;
	public static Screen screen;
	public static Shader ssaoShader;
	private static FloatBuffer samples;
	private static Random samplingRand=new Random();
	private static FrameBuffer unMultiSample;
	private static int noiseTexture;
	private static float nextFloat_abs() {
		return samplingRand.nextInt(1000)/1000.0f;
	}
	private static float nextFloat() {
		return (samplingRand.nextInt(2000)/1000.0f)-1.0f;
	}
	public static void update() {
		screen=new Screen();
		ssaoShader=new Shader("specific/SSAO");
		unMultiSample=new FrameBuffer(0,2,GL_RGBA16F);
	}
	public static void init() {
		update();
		samples=BufferUtils.createFloatBuffer(numSamples*3);
		noiseTexture=glGenTextures();
		//Generate the noise texture:
		FloatBuffer pixels=BufferUtils.createFloatBuffer(noise_side*noise_side*3);
		for(int i=0;i<noise_side*noise_side;i++) {
			pixels.put(nextFloat());
			pixels.put(nextFloat());
			pixels.put(0);
		}
		pixels.flip();
		glActiveTexture(GL_TEXTURE0+2);
		glBindTexture(GL_TEXTURE_2D,noiseTexture);
		glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA16F,noise_side,noise_side,0,GL_RGB,GL_FLOAT,pixels);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);  
		glBindTexture(GL_TEXTURE_2D,0);
		// done
		Vector3f hld=new Vector3f();
		float scale=0;
		for(int i=0;i<numSamples;i++) {
			hld.set(nextFloat(),nextFloat(),nextFloat_abs());
			hld.normalize();
			scale=i/(float)numSamples;
			hld.scale(lerp(0.1f,1.0f,scale*scale));
			samples.put(hld.x);
			samples.put(hld.y);
			samples.put(hld.z);
		}
		samples.flip();
	}
	private static float lerp(float a, float b, float f) {
		return a-f*(b-a);
	}
	public static void runSSAOTo(FrameBuffer in, int gPos, int gNorm, FrameBuffer out, int indexOut) {
		if(samples.capacity()!=numSamples*3) {
			throw new IllegalStateException("In this town you gotta init stuff first, dumbo. Nice try.");
		}
		Shader prevShader=Renderer.activeShader;
		ssaoShader.bind();
		ssaoShader.setUniform1i("scrWidth",RenderUtils.winW);
		ssaoShader.setUniform1i("scrHeight",RenderUtils.winH);
		ssaoShader.setUniform1i("noiseWidth",noise_side);
		ssaoShader.setUniform3fv("samples",samples);
		in.blitTo(unMultiSample, gPos, 0);
		in.blitTo(unMultiSample, gNorm,1);
		out.bind();
		unMultiSample.bindTexture(0,0);
		unMultiSample.bindTexture(1,1);
		glActiveTexture(GL_TEXTURE0+2);
		glBindTexture(GL_TEXTURE_2D,noiseTexture);
		glDisable(GL_DEPTH_TEST);
		screen.render();
		glEnable(GL_DEPTH_TEST);
		FrameBuffer.unbind_all();
		prevShader.bind();
	}
}

package graphics.handlers;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

import graphics.FrameBuffer;
import graphics.Screen;
import graphics.Shader;

public class SSAOHandler {
	public static final int numSamples=64;
	public static Screen screen;
	public static Shader ssaoShader;
	private static FloatBuffer samples;
	private static Random samplingRand=new Random();
	private static float nextFloat_abs() {
		return samplingRand.nextInt(1000)/1000.0f;
	}
	private static float nextFloat() {
		return (samplingRand.nextInt(2000)/1000.0f)-1.0f;
	}
	public static void update() {
		screen=new Screen();
		ssaoShader=new Shader("SSAO");
	}
	public static void init() {
		update();
		samples=BufferUtils.createFloatBuffer(numSamples*3);
	}
	public static void runSSAO(FrameBuffer in, int gPos, int gNorm) {
		if(samples.capacity()!=numSamples*3) {
			throw new IllegalStateException("In this town you gotta init stuff first, dumbo. Nice try.");
		}
		
	}
}

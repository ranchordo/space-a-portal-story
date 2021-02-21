package graphics.handlers;

import static org.lwjgl.opengl.GL11.*;
import graphics.FrameBuffer;
import graphics.Renderer;
import graphics.Screen;
import graphics.Shader;

public class BloomHandler {
	public static Screen screen;
	public static Shader blurShader;
	public static FrameBuffer fbo1;
	public static FrameBuffer fbo2;
	public static void update() {
		screen=new Screen();
		blurShader=new Shader("specific/blur");
		fbo2=new FrameBuffer(0);
	}
	public static void init() {
		update();
	}
	public static FrameBuffer blur(FrameBuffer in, int amount) {
		boolean horizontal=true;
		fbo1=in;
		Shader prevShader=Renderer.activeShader;
		blurShader.bind();
		for(int i=0;i<amount*2;i++) {
			blurShader.setUniform1i("horizontal",horizontal?1:0);
			(horizontal?fbo2:fbo1).bind();
			(horizontal?fbo1:fbo2).bindTexture(0);
			glClearColor(1,0,1,1);
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			screen.render();
			glEnable(GL_DEPTH_TEST);
			horizontal=!horizontal;
		}
		fbo1.unbind();
		prevShader.bind();
		return fbo1;
	}
}

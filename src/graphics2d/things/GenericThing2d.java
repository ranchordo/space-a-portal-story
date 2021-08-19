package graphics2d.things;

import java.nio.FloatBuffer;

import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;

import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.Shader;
import lepton.engine.rendering.TextureImage;
import lepton.util.LeptonUtil;

public class GenericThing2d extends Thing2d {
	public static Shader defaultGenericThing2dShader=new Shader("genericThing2d");
	public float width;
	public float height;
	public TextureImage image;
	public Shader renderingShader=defaultGenericThing2dShader;
	public float texX=0;
	public float texY=0;
	public float texW=1;
	public float texH=1;
	
	private float[] mma=new float[16];
	private FloatBuffer fm=BufferUtils.createFloatBuffer(16);
	@Override
	public void render() {
		renderingShader.bind();
		LeptonUtil.openGLMatrix(GLContextInitializer.proj_matrix,mma);
		fm=LeptonUtil.asFloatBuffer(mma,fm);
		GLContextInitializer.activeShader.setUniformMatrix4fv("proj_matrix",fm);
		renderingShader.applyAllSSBOs();
		
		Vector4f bb=(parent==null?getBoundingBox():parent.getBoundingBox());
		Thing2d.PosMode pm=(parent==null?posMode:parent.posMode);
		float xc=Thing2d.computeActualX(pm,x,bb);
		float yc=Thing2d.computeActualY(pm,y,bb);
		float hc=height*GLContextInitializer.aspectRatio;
		
		float xu=Thing2d.ratio2viewportX(xc);
		float yu=Thing2d.ratio2viewportY(yc);
		float mxu=Thing2d.ratio2viewportX(xc+width);
		float myu=Thing2d.ratio2viewportY(yc+hc);
		renderingShader.setUniform4f("info",xu,yu,mxu-xu,myu-yu);
		renderingShader.setUniform4f("texinfo",texX,texY,texW,texH);
		if(image!=null) {image.bind();}
		shape.render_raw();
	}
	private Vector4f a=new Vector4f();
	@Override
	public Vector4f getBoundingBox() {
		a.set(x,y,width+x,height+y);
		return a;
	}
	@Override
	public Thing2d setParent(Thing2d parent) {
		this.parent=parent;
		return this;
	}
}

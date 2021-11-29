package graphics2d.things;

import javax.vecmath.Vector4f;

import game.Main;
import graphics2d.util.InstancedRenderConfig2d;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.InstanceAccumulator;
import lepton.engine.rendering.Shader;

public class GenericInstancedThing2d extends Thing2d {
	public static final int defaultObjectSize=8;
	public float width;
	public float height;
//	public Shader renderingShader=Main.shaderLoader.load("genericThing2d");
	public float texX=0;
	public float texY=0;
	public float texW=1;
	public float texH=1;
	public int objectSize=defaultObjectSize;
	
	public GenericInstancedThing2d(InstanceAccumulator ia) {
		this.ia=ia;
	}
	
	public void refreshDataLength() {
		if(data.length!=objectSize) {
			data=new float[objectSize];
			additionalData=new float[objectSize-defaultObjectSize];
		}
	}
	public float[] additionalData=new float[0];
	public float[] data=new float[objectSize];
	private InstanceAccumulator ia=null;
	@Override
	public void render() {
		refreshDataLength();
		Vector4f bb=(parent==null?getBoundingBox():parent.getBoundingBox());
		Thing2d.PosMode pm=(parent==null?posMode:parent.posMode);
		float xc=Thing2d.computeActualX(pm,x,bb);
		float yc=Thing2d.computeActualY(pm,y,bb);
		float hc=height*GLContextInitializer.aspectRatio;
		
		float xu=Thing2d.ratio2viewportX(xc);
		float yu=Thing2d.ratio2viewportY(yc);
		float mxu=Thing2d.ratio2viewportX(xc+width);
		float myu=Thing2d.ratio2viewportY(yc+hc);
		data[0]=xu; data[1]=yu; data[2]=mxu-xu; data[3]=myu-yu;
		data[4]=texX; data[5]=texY; data[6]=texW; data[7]=texH;
		if(objectSize>defaultObjectSize) {
			for(int i=0;i<additionalData.length;i++) {
				data[i+defaultObjectSize]=additionalData[i];
			}
		}
		ia.add(data);
	}
	@Override public void logic() {
		runEventListeners();
	}
	private Vector4f a=new Vector4f();
	@Override
	public Vector4f getBoundingBox() {
		switch(posMode) {
		case TOP_RIGHT:
			a.set(x-width,y-(height*GLContextInitializer.aspectRatio),x,y);
			break;
		case TOP_LEFT:
			a.set(x,y-(height*GLContextInitializer.aspectRatio),x+width,y);
			break;
		case BOTTOM_RIGHT:
			a.set(x-width,y,x,y+(height*GLContextInitializer.aspectRatio));
			break;
		case BOTTOM_LEFT:
			a.set(x,y,x+width,y+(height*GLContextInitializer.aspectRatio));
			break;
		case CENTER:
			a.set(x-(width*0.5f),y-((height*GLContextInitializer.aspectRatio)*0.5f),x+(width*0.5f),y+((height*GLContextInitializer.aspectRatio)*0.5f));
			break;
		}
		return a;
	}
	@Override
	public Thing2d setParent(Thing2d parent) {
		this.parent=parent;
		return this;
	}
}

package graphics2d.things;

import javax.vecmath.Vector4f;

import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.Shader;

public class GenericInstancedThing2d extends Thing2d {
	public static final int defaultObjectSize=8;
	public static final Shader defaultGenericThing2dShader=new Shader("genericThing2d");
	public float width;
	public float height;
	public Shader renderingShader=defaultGenericThing2dShader;
	public float texX=0;
	public float texY=0;
	public float texW=1;
	public float texH=1;
	public int objectSize=defaultObjectSize;
	
	public void refreshDataLength() {
		if(data.length!=objectSize) {
			data=new float[objectSize];
			additionalData=new float[objectSize-defaultObjectSize];
		}
	}
	public float[] additionalData=new float[0];
	public float[] data=new float[objectSize];
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
		renderingShader.instanceAccumulator.add(data);
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

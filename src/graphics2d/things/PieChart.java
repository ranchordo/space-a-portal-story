package graphics2d.things;

import java.awt.Color;
import java.util.HashSet;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import graphics2d.util.InstancedRenderConfig2d;
import lepton.engine.rendering.InstanceAccumulator;
import lepton.engine.rendering.Shader;

public class PieChart extends Thing2d {
	public static Shader pieChartShader;
	public Group2d group=new Group2d();
	public float[] data=null;
	public Vector3f[] rgbs=null;
	public float width=0.1f;
	public PieChart(int elements, float nx, float ny, float nw, HashSet<InstancedRenderConfig2d> sl) {
		if(pieChartShader==null) {
			pieChartShader=new Shader("specific/piechart");
			pieChartShader.instanceAccumulator=new InstanceAccumulator(pieChartShader,16,"info_buffer");
			InstancedRenderConfig2d ircd=new InstancedRenderConfig2d(pieChartShader,null);
			if(!sl.contains(ircd)) {
				sl.add(ircd);
			}
		}
		data=new float[elements];
		rgbs=new Vector3f[elements];
		x=nx;
		y=ny;
		width=nw;
	}
	public void init() {
		boolean tog=false;
		for(int i=0;i<data.length;i++) {
			data[i]=1;
			GenericInstancedThing2d r=new GenericInstancedThing2d();
			r.renderingShader=pieChartShader;
			r.objectSize=GenericInstancedThing2d.defaultObjectSize+8;
			tog=!tog;
			int c=Color.HSBtoRGB(i*(1.0f/data.length),tog?1:0.90f,tog?0.3f:1);
			rgbs[i]=new Vector3f(((c>>0)&0xFF)/255.0f,((c>>8)&0xFF)/255.0f,((c>>16)&0xFF)/255.0f);
			r.setParent(parent);
			System.out.println(rgbs[i]);
			group.add(r);
		}
		group.init();
	}
	public void logic() {
		for(Thing2d thing : group.getList()) {
			GenericInstancedThing2d gthing=(GenericInstancedThing2d)thing;
			gthing.x=x;
			gthing.y=y;
			gthing.width=width;
			gthing.height=width;
		}
		group.logic();
	}
	@Override
	public void render() {
		float total=0;
		for(float f : data) {
			total+=f;
		}
		float offset=0;
		for(int i=0;i<group.getList().size();i++) {
			GenericInstancedThing2d thing=(GenericInstancedThing2d)group.getList().get(i);
			thing.refreshDataLength();
			thing.additionalData[0]=offset/total;
			thing.additionalData[1]=data[i]/total;
			thing.additionalData[2]=0;
			thing.additionalData[3]=0;
			thing.additionalData[4]=rgbs[i].x;
			thing.additionalData[5]=rgbs[i].y;
			thing.additionalData[6]=rgbs[i].z;
			thing.additionalData[7]=0;
			thing.render();
			offset+=data[i];
		}
	}
	@Override
	public Thing2d setParent(Thing2d parent) {
		group.setParent(parent);
		this.parent=parent;
		return this;
	}

	@Override
	public Vector4f getBoundingBox() {
		return group.getList().get(0).getBoundingBox();
	}
	
}

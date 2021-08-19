package graphics2d.presets;

import static lepton.engine.rendering.GLContextInitializer.fr;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.nio.FloatBuffer;
import java.util.HashSet;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;

import game.Main;
import graphics2d.things.Group2d;
import graphics2d.things.PieChart;
import graphics2d.things.TextGroup;
import graphics2d.things.Thing2d;
import graphics2d.util.InstancedRenderConfig2d;
import lepton.engine.rendering.GLContextInitializer;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class DebugScreen extends Thing2d {
	public Group2d group=new Group2d();
	public HashSet<InstancedRenderConfig2d> renderConfigs=new HashSet<InstancedRenderConfig2d>();
	public void init() {
		int expConfigs=2;
		group.add(new TextGroup("No data",Main.fonts.get("consolas"),-1,0.9f,0.05f,0.2f,0.2f,0.2f,renderConfigs).setAsParent().setPosMode(Thing2d.PosMode.TOP_LEFT).initThis());
//		group.add(new TextGroup("Hi, this is a test",Main.fonts.get("consolas"),-1,0.9f-0.1f,0.07f).setAsParent().setPosMode(Thing2d.PosMode.TOP_LEFT));
		group.add(new PieChart(Main.timeProfiler.times.length,0.9f,-0.9f,0.4f,renderConfigs).setAsParent().setPosMode(Thing2d.PosMode.BOTTOM_RIGHT).initThis());
		float height=-0.9f;
		for(int i=Main.timeProfiler.times.length-1;i>=0;i--) {
			Vector3f col=((PieChart)group.getList().get(1)).rgbs[i];
			height+=0.05;
			group.add(new TextGroup(Main.timeProfiler.time_names[i],Main.fonts.get("consolas"),0.4f,height,0.03f,col.x,col.y,col.z,renderConfigs).setAsParent().setPosMode(Thing2d.PosMode.BOTTOM_RIGHT).initThis());
		}
		if(renderConfigs.size()!=expConfigs) {
			Logger.log(2,"Found "+renderConfigs.size()+" 2d instanced render configurations for this parent element. Expected: "+expConfigs+". You may experience issues.");
		}
	}
	private StringBuilder dbg=new StringBuilder();
	private int fc=0;
	public void logic() {
		fc++;
		if(fc%20==0) {
			dbg.setLength(0);
			dbg.append(String.format("%3.2f",((float)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(float)Runtime.getRuntime().totalMemory())*100.0f));
			dbg.append("% of ");
			dbg.append(Runtime.getRuntime().totalMemory()/1048576.0f);
			dbg.append("M, fr=");
			dbg.append(String.format("%3.2f", fr));
			dbg.append("fps");
			((TextGroup)group.getList().get(0)).setString(dbg.toString());
			PieChart pc=((PieChart)group.getList().get(1));
			for(int i=0;i<Main.timeProfiler.times.length;i++) {
				pc.data[i]=(int)Main.timeProfiler.times[i];
			}
		}
		group.logic();
	}
	private float[] mma=new float[16];
	private FloatBuffer fm=BufferUtils.createFloatBuffer(16);
	public void render() {
		for(InstancedRenderConfig2d s : renderConfigs) {
			s.shader.instanceAccumulator.reset();
		}
		group.render();
		glDisable(GL_DEPTH_TEST);
		for(InstancedRenderConfig2d s : renderConfigs) {
			if(s.shader.instanceAccumulator.getBuffer().capacity()==0) {
				Logger.log(0,"Instanced buffer capacity for shader with name "+s.shader.getFname()+" was empty. Skipping rendering for this round.");
				continue;
			}
			s.shader.instanceAccumulator.submit();
			s.shader.bind();
			LeptonUtil.openGLMatrix(GLContextInitializer.proj_matrix,mma);
			fm=LeptonUtil.asFloatBuffer(mma,fm);
			s.shader.setUniformMatrix4fv("proj_matrix",fm);
			s.shader.applyAllSSBOs();
			int prevInstances=Thing2d.genericSquare.instances;
			if(s.image!=null) {
				s.image.bind();
			}
			Thing2d.genericSquare.instances=(s.shader.instanceAccumulator.getBuffer().capacity())/s.shader.instanceAccumulator.objectSize;
			Thing2d.genericSquare.render_raw();
			Thing2d.genericSquare.instances=prevInstances;
		}
		glEnable(GL_DEPTH_TEST);
	}
	@Override
	public Vector4f getBoundingBox() {
		return group.getBoundingBox();
	}
	@Override
	public Thing2d setParent(Thing2d parent) {
		group.setParent(parent);
		this.parent=parent;
		return this;
	}
}

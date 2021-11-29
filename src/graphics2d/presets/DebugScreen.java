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
import graphics2d.util.InstancedRenderer2d;
import lepton.engine.rendering.GLContextInitializer;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class DebugScreen extends Thing2d {
	public Group2d group=new Group2d();
	public void init() {
		Thing2d t=group.add(new TextGroup("No data",Main.fonts.get("consolas"),-1,0.9f,0.05f,0.2f,0.2f,0.2f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.TOP_LEFT).initThis());
		//		group.add(new TextGroup("Hi, this is a test",Main.fonts.get("consolas"),-1,0.9f-0.1f,0.07f).setAsParent().setPosMode(Thing2d.PosMode.TOP_LEFT));
		group.add(new PieChart(Main.timeProfiler.times.length,0.9f,-0.9f,0.4f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.BOTTOM_RIGHT).initThis());
		t.mouseOn=new Thing2d.EventListener() {@Override public void onEvent() {System.out.println("dbgtext.event.mouseOn");}};
		t.mouseOff=new Thing2d.EventListener() {@Override public void onEvent() {System.out.println("dbgtext.event.mouseOff");}};
		t.mouseClick=new Thing2d.EventListener() {@Override public void onEvent() {System.out.println("dbgtext.event.mouseClick");}};
		t.mouseClickRight=new Thing2d.EventListener() {@Override public void onEvent() {System.out.println("dbgtext.event.mouseClickRight");}};
		t.mouseClickMiddle=new Thing2d.EventListener() {@Override public void onEvent() {System.out.println("dbgtext.event.mouseClickMiddle");}};
		float height=-0.9f;
		for(int i=Main.timeProfiler.times.length-1;i>=0;i--) {
			Vector3f col=((PieChart)group.getList().get(1)).rgbs[i];
			height+=0.05;
			group.add(new TextGroup(Main.timeProfiler.time_names[i],Main.fonts.get("consolas"),0.4f,height,0.03f,col.x,col.y,col.z,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.BOTTOM_RIGHT).initThis());
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
	private InstancedRenderer2d.InstancedRenderRoutine2d renderRoutine=new InstancedRenderer2d.InstancedRenderRoutine2d() {
		@Override public void run() {
			group.render();
		}
	};
	public void render() {
		glDisable(GL_DEPTH_TEST);
		Thing2d.renderer.renderInstanced(renderRoutine);
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

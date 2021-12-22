package leveldesigner;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL15;

import com.bulletphysics.linearmath.Transform;

import debug.GenericCubeFactory;
import game.Chamber;
import game.Main;
import graphics.RenderFeeder;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.lighting.Light;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;
import objects.LightingConfiguration;
import objects.Thing;
import objects.Wall;
import util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LevelDesigner {
	private static GObject genericCube;
	private static Transform genericCubeTransform=new Transform();
	private static Thing selected=null;
	public static String outputname="designer output";
	private static void createGenericCube() {
		if(genericCube!=null) {
			return;
		}
		genericCube=GenericCubeFactory.createGenericCube();
//		genericCube.wireframe=true;
		genericCube.setColor(1,1,0,0.2f);
		genericCube.copyData(GObject.COLOR_DATA,GL15.GL_STATIC_DRAW);
	}
	public static void refreshSelectedBox() {
		PoolElement<Matrix4f> p1=DefaultVecmathPools.matrix4f.alloc();
		PoolElement<Matrix4f> p2=DefaultVecmathPools.matrix4f.alloc();
		Util.clear(p1.o());
		p1.o().m00=selected.getShape().x;
		p1.o().m11=selected.getShape().y;
		p1.o().m22=selected.getShape().z;
		p1.o().m33=1;
		selected.geo.p.getTransform().getMatrix(p2.o());
		p2.o().mul(p2.o(),p1.o());
		genericCubeTransform.set(p2.o());
		p1.free();
		p2.free();
	}
	public static void setSelected(Thing selected) {
		if(selected==null) {
			menu.refreshParams();
			LevelDesigner.selected=null;
			return;
		}
		createGenericCube();
		if(selected!=LevelDesigner.selected) {
//			if(LevelDesigner.selected!=null) {
//				LevelDesigner.selected.geo.g.wireframe=!LevelDesigner.selected.geo.g.wireframe;
//			}
//			selected.geo.g.wireframe=!selected.geo.g.wireframe;
			LevelDesigner.selected=selected;
			refreshSelectedBox();
			if(insertable.contains(selected.getClass())) {
				menu.refreshParams();
			}
		}
	}
	public static Chamber initDesignerChamber(Chamber designerTemplate) {
		float inte=10f;
		float r=255/255.0f;
		float g=250/255.0f;
		float b=244/255.0f;
		float amb=0.02f;
		designerTemplate.add(new Wall(new Vector2f(10,10), new Vector3f(0,0,0), LeptonUtil.AxisAngle_np(new AxisAngle4f(1,0,0,(float)Math.toRadians(90)))).setAspect(new Vector2f(0.5f,0.5f)).setTextureType(2).setSideMode(Wall.DOUBLE));
		designerTemplate.add(new LightingConfiguration(
				new Light(Light.LIGHT_POSITION,-9,3,-9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,-9,3,9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,9,3,-9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_POSITION,9,3,9, inte*r,inte*g,inte*b,1),
				new Light(Light.LIGHT_AMBIENT,0,0,0, amb,amb,amb,1)));
		designerTemplate.output(LevelDesigner.outputname);
		return Chamber.input(LevelDesigner.outputname);
	}
	public static Thing getSelected() {
		return selected;
	}
	public static void onFrame() {
		if(Main.in.ir(GLFW.GLFW_KEY_F2)) {
			RenderFeeder.feed(Chamber.input(LevelDesigner.outputname));
		}
		if(Main.in.ir(GLFW.GLFW_KEY_F4)) {
			Logger.log(0,"Clear designer selection");
			LevelDesigner.setSelected(null);
		}
		if(Main.in.i(GLFW.GLFW_KEY_LEFT_SHIFT)) {
			if(Main.in.ir(GLFW.GLFW_KEY_F5)) {
				Chamber d=new Chamber();
				d=initDesignerChamber(d);
				RenderFeeder.feed(d);
			}
		}
		if(genericCube!=null && selected!=null) {
			GL15.glDisable(GL15.GL_DEPTH_TEST);
			genericCube.highRender_customTransform(genericCubeTransform);
			GL15.glEnable(GL15.GL_DEPTH_TEST);
		}
	}
	public static ArrayList<Class> insertable=new ArrayList<Class>();
	public static HashMap<Class,InsertableObjectDescriptor> insertables=new HashMap<Class,InsertableObjectDescriptor>();
	public static DesignerInsertMenu menu;
	static {
		if(Main.isDesigner) {
			insertable.add(Wall.class);
			for(Class c : insertable) {
				InsertableObjectDescriptor i=new InsertableObjectDescriptor(c);
				i.scanParameters();
				insertables.put(c,i);
			}
		}
	}
	
}

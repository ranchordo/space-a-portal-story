package leveldesigner;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4f;

import org.lwjgl.opengl.GL15;

import com.bulletphysics.linearmath.Transform;

import debug.GenericCubeFactory;
import game.Main;
import graphics2d.presets.DesignerInsertMenu;
import lepton.engine.rendering.GObject;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import objects.Thing;
import objects.Wall;
import util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LevelDesigner {
	private static GObject genericCube;
	private static Transform genericCubeTransform=new Transform();
	private static Thing selected=null;
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
	public static Thing getSelected() {
		return selected;
	}
	public static void onFrame() {
		if(genericCube!=null) {
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

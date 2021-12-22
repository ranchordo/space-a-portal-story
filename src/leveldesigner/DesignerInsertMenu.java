package leveldesigner;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;

import javax.vecmath.Vector4f;

import game.Main;
import graphics2d.things.GenericInstancedThing2d;
import graphics2d.things.Group2d;
import graphics2d.things.TextGroup;
import graphics2d.things.Thing2d;
import graphics2d.util.InstancedRenderer2d;
import lepton.engine.rendering.GLContextInitializer;
import lepton.util.advancedLogger.Logger;

public class DesignerInsertMenu extends Thing2d {
	private Group2d group=new Group2d();
	private ArrayList<TextGroup> labels=new ArrayList<TextGroup>();
	private ArrayList<ArrayList<TextGroup>> params=new ArrayList<ArrayList<TextGroup>>();
	private static final int MAX_PARAMS=9;
	private static final int MAX_SUBPARAMS=4;
	public DesignerInsertMenu() {
		super();
		LevelDesigner.menu=this;
	}
	@Override public void init() {
		GenericInstancedThing2d t=(GenericInstancedThing2d)group.add(new GenericInstancedThing2d(Thing2d.renderer.loadConfiguration(Main.shaderLoader.load("genericNontexThing2d"),null,Thing2d.genericSquare,12,"info_buffer").instanceAccumulator).initThis());
		t.posMode=Thing2d.PosMode.TOP_LEFT;
		t.x=-1;
		t.y=1f;
		t.width=0.32f;
		t.height=1.5f/GLContextInitializer.aspectRatio;
		t.objectSize=12;
		t.refreshDataLength();
		t.additionalData[0]=1;
		t.additionalData[1]=1;
		t.additionalData[2]=1;
		t.additionalData[3]=0.2f;
		t=(GenericInstancedThing2d)group.add(new GenericInstancedThing2d(Thing2d.renderer.loadConfiguration(Main.shaderLoader.load("genericNontexThing2d"),null,Thing2d.genericSquare,12,"info_buffer").instanceAccumulator).initThis());
		t.posMode=Thing2d.PosMode.BOTTOM_LEFT;
		t.x=-1;
		t.y=-1;
		t.width=2f;
		t.height=0.5f/GLContextInitializer.aspectRatio;
		t.objectSize=12;
		t.refreshDataLength();
		t.additionalData[0]=1;
		t.additionalData[1]=1;
		t.additionalData[2]=1;
		t.additionalData[3]=0.2f;
		for(int i=-1;i<LevelDesigner.insertables.size();i++) {
			Thing2d d=group.add(new TextGroup(i<0?"Insert:":LevelDesigner.insertable.get(i).getName(),Main.fonts.get("consolas"),-0.84f,0.8f-(0.13f*(i+1)),0.05f,i<0?0.0f:1.0f,0.0f,0.0f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.CENTER).initThis());
			if(i>=0) {
				final int f=i; //Stupid pointer escape trick. Don't do this kids
				d.mouseClick=new Thing2d.EventListener() {private int i=f; public void onEvent() {
					Logger.log(0,"Inserting object "+LevelDesigner.insertable.get(i).getName());
					LevelDesigner.insertables.get(LevelDesigner.insertable.get(i)).insertNew();
				}};
			}
		}
		Thing2d e=group.add(new TextGroup("Duplicate",Main.fonts.get("consolas"),-0.84f,-0.4f,0.05f,0.0f,0.5f,0.5f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.CENTER).initThis());
		e.mouseClick=()->{LevelDesigner.insertables.get(LevelDesigner.getSelected().getClass()).insertFromExisting(LevelDesigner.getSelected());};
		for(int i=0;i<MAX_PARAMS;i++) {
			labels.add((TextGroup)group.add(new TextGroup("",Main.fonts.get("consolas"),(2.0f*(i+0.5f)/MAX_PARAMS)-1,0.45f-1,0.04f,0.0f,0.0f,0.0f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.CENTER).initThis()));
		}
		for(int i=0;i<MAX_PARAMS;i++) {
			ArrayList<TextGroup> column=new ArrayList<TextGroup>();
			for(int j=0;j<MAX_SUBPARAMS;j++) {
				TextGroup tg=(TextGroup)group.add(new TextGroup("",Main.fonts.get("consolas"),(2.0f*(i+0.5f)/MAX_PARAMS)-1,0.45f-1-0.1f-(0.1f*j),0.06f,1.0f,0.0f,0.0f,Thing2d.renderer).setAsParent().setPosMode(Thing2d.PosMode.CENTER).initThis());
				final int i1=i; //More dumbery
				final int j1=j;
				tg.mouseClick=new Thing2d.EventListener() {private int i=i1; private int j=j1;
				@Override public void onEvent() {
					InsertableObjectDescriptor idesc=LevelDesigner.insertables.get(LevelDesigner.getSelected().getClass());
					InsertableObjectDescriptor.Thingparameter p=idesc.dataparams.get(i);
					Float[] data=p.get(LevelDesigner.getSelected());
					data[j]+=0.5f;
					p.set(LevelDesigner.getSelected(),data);
					DesignerInsertMenu.this.refreshParams();
				}};
				tg.mouseClickRight=new Thing2d.EventListener() {private int i=i1; private int j=j1;
				@Override public void onEvent() {
					InsertableObjectDescriptor idesc=LevelDesigner.insertables.get(LevelDesigner.getSelected().getClass());
					InsertableObjectDescriptor.Thingparameter p=idesc.dataparams.get(i);
					Float[] data=p.get(LevelDesigner.getSelected());
					data[j]-=0.5f;
					p.set(LevelDesigner.getSelected(),data);
					DesignerInsertMenu.this.refreshParams();
				}};
				column.add(tg);
			}
			params.add(column);
		}
	}
	public void refreshParams() {
		if(LevelDesigner.getSelected()==null) {
			for(int i=0;i<MAX_PARAMS;i++) {
				labels.get(i).setString("");
				for(int j=0;j<MAX_SUBPARAMS;j++) {
					params.get(i).get(j).setString("");
				}
			}
			return;
		}
		InsertableObjectDescriptor idesc=LevelDesigner.insertables.get(LevelDesigner.getSelected().getClass());
		for(int i=0;i<labels.size();i++) {
			if(i<idesc.dataparams.size()) {continue;}
			labels.get(i).setString("");
			for(int j=0;j<MAX_SUBPARAMS;j++) {
				params.get(i).get(j).setString("");
			}
		}
		if(idesc==null) {Logger.log(4,"InsertableObjectDescriptor for class "+LevelDesigner.getSelected().getClass().getName()+" is null. Someone is clearly a complete idiot. Oh wait, I'm the only programmer. Nvm.");}
		if(idesc.dataparams.size()>labels.size()) {
			Logger.log(4,"Too many parameters for type "+LevelDesigner.getSelected().getClass().getName());
		}
		for(int i=0;i<idesc.dataparams.size();i++) {
			InsertableObjectDescriptor.Thingparameter p=idesc.dataparams.get(i);
			labels.get(i).setString(p.annotation.name());
			Float[] d=p.get(LevelDesigner.getSelected());
			for(int j=0;j<Math.min(d.length,MAX_SUBPARAMS);j++) {
				params.get(i).get(j).setString(""+(float)d[j]);
			}
		}

	}
	@Override public void logic() {
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

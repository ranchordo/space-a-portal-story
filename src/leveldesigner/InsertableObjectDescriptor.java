package leveldesigner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import game.Main;
import graphics.RenderFeeder;
import lepton.util.advancedLogger.Logger;
import objects.Thing;

public class InsertableObjectDescriptor {
	public Class <? extends Thing> cl;
	public static class Thingparameter {
		private Float[] dataget;
		private Object[] objlist;
		public void set(Thing thing, Float... data) {
			if(data.length!=datalen) {
				Logger.log(4,"Wrong data length for method: "+method.getDeclaringClass().getName()+"."+method.getName());
			}
			try {
				for(int i=0;i<datalen;i++) {
					objlist[i]=(Object)data[i];
				}
				objlist[datalen]=(Boolean)true;
				method.invoke(thing, objlist);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Logger.log(3,e.getCause().toString());
				e.getCause().printStackTrace();
				Logger.log(4,e.toString(),e);
			}
			Main.activeChamber.output(LevelDesigner.outputname);
			LevelDesigner.refreshSelectedBox();
		}
		public Float[] get(Thing thing) {
			if(dataget==null) {dataget=new Float[datalen];}
			if(objlist==null) {objlist=new Object[datalen+1];}
			try {
				for(int i=0;i<datalen;i++) {
					objlist[i]=(Object)dataget[i];
				}
				objlist[datalen]=(Boolean)false;
				Float[] ret=(Float[])method.invoke(thing, objlist);
				for(int i=0;i<datalen;i++) {
					dataget[i]=ret[i]; //Copy
				}
				return dataget;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Logger.log(3,annotation.name()+", datalen "+dataget.length);
				if(e.getCause()!=null) {
					Logger.log(3,e.getCause().toString());
					e.getCause().printStackTrace();
				}
				Logger.log(4,e.toString(),e);
			}
			return null;
		}
		public Method method=null;
		public int datalen=-1;
		public Thing.DesignerParameter annotation=null;
	}
	public ArrayList<Thingparameter> dataparams=null;
	public InsertableObjectDescriptor(Class<? extends Thing> obj) {
		cl=obj;
	}
	private Thing newInstance() {
		try {
			return cl.getConstructor().newInstance();
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Logger.log(4,e.toString(),e);
		}
		return null;
	}
	public void insertNew() {
		Thing t=newInstance();
		t.init();
		RenderFeeder.addThing(t);
		Main.activeChamber.output(LevelDesigner.outputname);
		t.addPhysics();
		LevelDesigner.setSelected(t);
	}
	public void insertFromExisting(Thing thing) {
		Thing t=newInstance();
		t.init();
		RenderFeeder.addThing(t);
		Main.activeChamber.output(LevelDesigner.outputname);
		t.addPhysics();
		for(Thingparameter p : dataparams) {
			p.set(t,p.get(thing));
		}
		LevelDesigner.setSelected(t);
	}
	public void scanParameters() {
		if(dataparams!=null) {
			return;
		}
		dataparams=new ArrayList<Thingparameter>();
		for(Method m : cl.getDeclaredMethods()) {
			if(m.isAnnotationPresent(Thing.DesignerParameter.class)) {
				Thing.DesignerParameter annotation=m.getAnnotation(Thing.DesignerParameter.class);
				Class<?>[] params=m.getParameterTypes();
				for(int i=0;i<params.length-1;i++) {
					if(params[i]!=Float.class || params[params.length-1]!=Boolean.class) {
						Logger.log(2,"Rejecting designer parameter "+m.getDeclaringClass().getName()+"."+m.getName()+" (named \""+annotation.name()+"\") due to parameter mismatch.");
						continue;
					}
				}
				Thingparameter t=new Thingparameter();
				t.method=m;
				t.datalen=params.length-1;
				t.annotation=annotation;
				dataparams.add(t);
			}
		}
	}

}

package leveldesigner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import game.Main;
import lepton.util.advancedLogger.Logger;
import objects.Thing;

public class InsertableObjectDescriptor {
	public Class <? extends Thing> cl;
	public static class Thingparameter {
		public HashMap<Thing,Float[]> data=new HashMap<Thing,Float[]>();
		public void set(Thing thing, Float... data) {
			if(data.length!=datalen) {
				Logger.log(4,"Wrong data length for method: "+method.getDeclaringClass().getName()+"."+method.getName());
			}
			this.data.put(thing,data);
			try {
				method.invoke(thing, (Object[])data);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Logger.log(3,e.getCause().toString());
				e.getCause().printStackTrace();
				Logger.log(4,e.toString(),e);
			}
			LevelDesigner.refreshSelectedBox();
		}
		public Float[] get(Thing thing) {
			Float[] ret=data.get(thing);
			if(ret==null) {
				ret=new Float[datalen];
				for(int i=0;i<datalen;i++) {
					ret[i]=0.0f;
				}
			}
			return ret;
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
		Main.things.add(t);
		t.addPhysics();
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
				for(Class<?> param : params) {
					if(param!=Float.class) {
						Logger.log(2,"Rejecting designer parameter "+m.getDeclaringClass().getName()+"."+m.getName()+" (named \""+annotation.name()+"\") due to parameter mismatch.");
						continue;
					}
				}
				Thingparameter t=new Thingparameter();
				t.method=m;
				t.datalen=params.length;
				t.annotation=annotation;
				dataparams.add(t);
			}
		}
	}
	
}

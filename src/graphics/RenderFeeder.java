package graphics;

import java.util.HashSet;

import chamber.Chamber;
import lighting.Lighting;
import objects.Thing;

public class RenderFeeder {
	public static void addThing(Thing thing) {
		Renderer.things.add(thing);
		Renderer.activeChamber.add(thing);
	}
	public static void feed(Chamber chamber) {
		HashSet<Thing> toremove=new HashSet<Thing>();
		for(Thing t : Renderer.things) {
			if(t.isTest) {
				toremove.add(t);
			}
		}
		for(Thing t : chamber.stuff) {
			Renderer.things.add(t);
			if(t.geo==null) {continue;}
			if(t.geo.vmap==null) {continue;}
			if(t.geo.vmap.tex==null) {continue;}
			if(!t.geo.vmap.tex.name.equals("")) {continue;}
			Renderer.activeCache.cache.add(t.geo.vmap.tex);
		}
	}
	
}

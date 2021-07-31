package graphics;

import java.util.HashSet;

import game.Chamber;
import game.Main;
import objects.Thing;

public class RenderFeeder {
	public static void addThing(Thing thing) {
		Main.things.add(thing);
		Main.activeChamber.add(thing);
	}
	public static void feed(Chamber chamber) {
//		HashSet<Thing> toremove=new HashSet<Thing>();
//		for(Thing t : Main.things) {
//			if(t.exemptFromChamberFeed) {
//				toremove.add(t);
//			}
//		}
		for(Thing t : chamber.stuff) {
			Main.things.add(t);
			if(t.geo==null) {continue;}
			if(t.geo.g==null) {continue;}
			if(t.geo.g.vmap==null) {continue;}
			if(t.geo.g.vmap.tex==null) {continue;}
//			if(!t.geo.g.vmap.tex.name.equals("")) {continue;}
			Main.activeCache.cache.add(t.geo.g.vmap.tex);
		}
	}
	
}

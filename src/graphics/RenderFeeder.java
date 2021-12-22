package graphics;

import java.util.ArrayList;

import game.Chamber;
import game.Main;
import lepton.util.advancedLogger.Logger;
import objects.Thing;

public class RenderFeeder {
	public static void addThing(Thing thing) {
		Main.things.add(thing);
		Main.activeChamber.add(thing);
	}
	public static void feed(Chamber chamber) {
		ArrayList<Thing> toRemove=new ArrayList<Thing>();
		for(Thing t : Main.things) {
			if(!t.exemptFromChamberFeed) {
				t.clean();
				toRemove.add(t);
			}
		}
		for(Thing t : toRemove) {
			if(!Main.things.remove(t)) {
				Logger.log(3,"RenderFeeder.feed: Failed to remove object with type "+t.type);
			}
		}
		for(Thing t : chamber.stuff) {
			Main.things.add(t);
		}
		Main.activeChamber=chamber;
	}
}

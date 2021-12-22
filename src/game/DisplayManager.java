package game;

import java.util.ArrayList;
import java.util.HashMap;

import game.DisplayManager.DisplayEntry;
import graphics2d.presets.DebugScreen;
import graphics2d.things.Thing2d;
import lepton.util.advancedLogger.Logger;
import leveldesigner.DesignerInsertMenu;

public class DisplayManager {
	public static class DisplayEntry {
		Class<? extends Thing2d> c;
		String name;
		public DisplayEntry(Class<? extends Thing2d> cn, String namen) {
			c=cn;
			name=namen;
		}
	}
	public static ArrayList<DisplayEntry> displayTypes=new ArrayList<DisplayEntry>();
	static {
		displayTypes.add(new DisplayEntry(DebugScreen.class,"debug"));
		if(Main.isDesigner) {
			DisplayManager.displayTypes.add(new DisplayEntry(DesignerInsertMenu.class,"designerinsert"));
		}
	}
	
	public static HashMap<String,Thing2d> displays=new HashMap<String,Thing2d>();
	public static void show(String s) {
		Thing2d display=displays.get(s);
		if(!Main.displays.contains(display)) {
			Main.displays.add(display);
		}
	}
	public static void hide(String s) {
		Thing2d display=displays.get(s);
		if(Main.displays.contains(display)) {
			Main.displays.remove(display);
		}
	}
	public static void toggle(String s) {
		Thing2d display=displays.get(s);
		if(Main.displays.contains(display)) {
			Main.displays.remove(display);
		} else {
			Main.displays.add(display);
		}
	}
	public static void load() {
		displays.clear();
		for(DisplayEntry de : displayTypes) {
			Logger.log(0,"Loading new display, named "+de.c.getName());
			try {
				Thing2d d=de.c.newInstance();
				d.init();
				displays.put(de.name,d);
			} catch (InstantiationException | IllegalAccessException e) {
				Logger.log(4,e.toString(),e);
			}
		}
	}
}

package console;

import static lepton.engine.rendering.GLContextInitializer.win;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import game.Main;
import lepton.util.CleanupTasks;
import lepton.util.advancedLogger.Logger;

public class Commands {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Command {
		public String desc() default "No description provided.";
	}
	private static void print(String s) {
		if(Logger.handlers.getInternalArrayList().contains(Main.consoleWindowHandler)) {
			Logger.log(0,s);
		} else {
			Main.jythonConsoleHandler.println(s);
		}
	}
	@Command(desc="Request a gentle close of the game.")
	public static void requestClose() {
		glfwSetWindowShouldClose(win,true);
	}
	@Command(desc="Force close everything by running cleanup tasks while main loop is active. Will likely lead to runtime errors.")
	public static void forceTerminate() {
		glfwSetWindowShouldClose(win,true);
		CleanupTasks.cleanUp();
	}
	@Command(desc="Unhook the logger output from the console window.")
	public static void unhookConsoleHandler() {
		print("Unhooking logger console handler...");
		if(Logger.handlers.getInternalArrayList().contains(Main.consoleWindowHandler)) {
			Logger.handlers.remove(Main.consoleWindowHandler);
		}
	}
	@Command(desc="If not already hooked, hook the logger output to the console window.")
	public static void hookConsoleHandler() {
		if(!Logger.handlers.getInternalArrayList().contains(Main.consoleWindowHandler)) {
			Logger.handlers.add(Main.consoleWindowHandler);
		}
		print("Logger console handler re-hooked");
	}
	@Command(desc="Show bounding boxes from the main physics world.")
	public static void showMainPhysicsWorldBBs() {
		print("Showing bounding boxes from physics world 1");
		Main.dbgRenderWorld=Main.portalWorld.getWorld1();
	}
	@Command(desc="Show bounding boxes from the alternate portal physics world.")
	public static void showPortalPhysicsWorldBBs() {
		print("Showing bounding boxes from physics world 2");
		Main.dbgRenderWorld=Main.portalWorld.getWorld2();
	}
	@Command(desc="Hide all physics bounding boxes.")
	public static void hidePhysicsBBs() {
		print("Hiding physics bounding boxes");
		Main.dbgRenderWorld=null;
	}
	@Command(desc="Show help menu.")
	public static void help() {
		StringBuilder helpMenu=new StringBuilder();
		for(Method m : Commands.class.getDeclaredMethods()) {
			if(m.isAnnotationPresent(Command.class)) {
				Class<?>[] params=m.getParameterTypes();
				Command c=m.getAnnotation(Command.class);
				helpMenu.append(m.getName());
				helpMenu.append("(");
				for(int i=0;i<params.length;i++) {
					if(i!=0) {
						helpMenu.append(", ");
					}
					helpMenu.append(params[i].getName());
				}
				helpMenu.append(") - ");
				helpMenu.append(c.desc());
				helpMenu.append("\n");
			}
		}
		Main.jythonConsoleHandler.println(helpMenu.toString());
	}
}

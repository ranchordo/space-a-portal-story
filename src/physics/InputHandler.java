package physics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

import java.io.Serializable;
import java.util.HashMap;

public class InputHandler {
	private long win;
	private HashMap<Integer,Boolean> rise=new HashMap<Integer,Boolean>();
	public InputHandler(long win_) {
		win=win_;
	}
	public boolean i(int key) {
		boolean ret=glfwGetKey(win,key)==GLFW_PRESS;
		return ret;
	}
	public boolean ir(int key) {
		boolean ret=i(key);
		boolean sret=false;
		if(rise.containsKey(key)) {
			if(ret && !rise.get(key)) {
				sret=true;
			}
		}
		rise.put(key,ret);
		return sret;
	}
	public boolean m(int key) {
		return glfwGetMouseButton(win,key)==GLFW_PRESS;
	}
	public boolean mr(int key) {
		boolean ret=m(key);
		boolean sret=false;
		if(rise.containsKey(key)) {
			if(ret && !rise.get(key)) {
				sret=true;
			}
		}
		rise.put(key,ret);
		return sret;
	}
}

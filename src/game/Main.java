package game;

import graphics.Renderer;
import logger.Logger;
import util.Util;

public class Main {
	public static final boolean isDesigner=true;
	public static void main(String[] args) {
		Util.initUtil();
		Renderer.Main();
	}
}
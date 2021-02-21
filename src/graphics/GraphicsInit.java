package graphics;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;

import objects.Player;
import objects.Thing;
import util.Util;

public class GraphicsInit {
	public static Player player;
	public static void InitGraphics(long win) {
		player=new Player(new Vector3f(0,1+Player.height/2.0f,0), Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,0))));
		player.init();
		player.addPhysics(Thing.PLAYER,Thing.PLAYER);
		Renderer.things.add(player);
	}
}

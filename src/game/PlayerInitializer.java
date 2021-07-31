package game;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;

import lepton.util.LeptonUtil;
import objects.Player;
import objects.Thing;

public class PlayerInitializer {
	public static Player player;
	public static Player initializePlayer() {
		Player player=new Player(new Vector3f(0,1+Player.height/2.0f,0), LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(1,0,0,0))));
		player.init();
		player.addPhysics(Thing.PLAYER,Thing.PLAYER);
		Main.things.add(player);
		return player;
	}
}

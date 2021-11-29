package graphics;

import java.util.ArrayList;

import javax.vecmath.Matrix4f;

import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import lepton.engine.rendering.ViewMatrixModifier;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class PortalViewMatrixModifier implements ViewMatrixModifier {
	public static final int LEVELS_CACHE=20;
	private static ArrayList<Transform> cache_diff=new ArrayList<Transform>(LEVELS_CACHE);
	private static ArrayList<Transform> cache_diff_inv=new ArrayList<Transform>(LEVELS_CACHE);
	private static boolean cacheDone=false;
	private static void cacheLevels(Transform mul, ArrayList<Transform> cache) {
		Matrix4f f=new Matrix4f();
		Matrix4f g=new Matrix4f();
		mul.getMatrix(f);
		mul.getMatrix(g);
		for(int i=0;i<LEVELS_CACHE;i++) {
			cache.add(i,new Transform(new Matrix4f(f)));
			f.mul(f,g);
		}
	}
	public static void cacheLevels() {
		cacheLevels(PlayerInitializer.player.portalPair.difference(),cache_diff);
		cacheLevels(PlayerInitializer.player.portalPair.difference_inv(),cache_diff_inv);
		Logger.log(0,"Generating portal transformation cache to "+LEVELS_CACHE+" levels.");
		cacheDone=true;
	}
	private static void runCacheTransform(Transform i, int levels, ArrayList<Transform> cache) {
		while(levels>0) {
			int depth=Math.min(levels,LEVELS_CACHE);
			i.mul(i,cache.get(depth-1));
			levels-=depth;
		}
	}
	public Transform modifyViewMatrix(Transform view) {
		if(!cacheDone) {
			cacheLevels();
		}
		if(Main.activePortalTransform!=0) {
			int usePortal=Math.round(LeptonUtil.mod(Main.activePortalTransform-1,2)+1);
			int loop=Math.round((Main.activePortalTransform-usePortal)/2.0f)+1;
			runCacheTransform(view,loop,(usePortal==1)?cache_diff:cache_diff_inv);
		}
		return view;
	}
}

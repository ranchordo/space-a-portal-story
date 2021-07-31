package graphics;

import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import lepton.engine.rendering.ViewMatrixModifier;
import lepton.util.LeptonUtil;

public class PortalViewMatrixModifier implements ViewMatrixModifier {
//	private GObject parent;
//	public PortalViewMatrixModifier(GObject p) {
//		parent=p;
//	}
	public Transform modifyViewMatrix(Transform view) {
		if(Main.activePortalTransform!=0) {
			int usePortal=Math.round(LeptonUtil.mod(Main.activePortalTransform-1,2)+1);
			int loop=Math.round((Main.activePortalTransform-usePortal)/2.0f)+1;
			Transform comp=(usePortal==1)?PlayerInitializer.player.portalPair.difference():PlayerInitializer.player.portalPair.difference_inv();
			for(int i=0;i<loop;i++) {
				view.mul(view,comp);
			}
		}
		return view;
	}
}

package debug;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

import lepton.engine.rendering.TextureImage;
import lepton.util.ImageUtil;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class DebugUtil {
	public static final TextureImage placeholderTexture=createPlaceholderTexture(0);
	public static TextureImage createPlaceholderTexture(int b) {
		try {
			BufferedImage img=ImageUtil.getImage(LeptonUtil.getOptionallyIntegratedStream("assets/3d/dbg/texture-test",".png"));
			TextureImage ret=new TextureImage(b);
			ret.create(img);
			return ret;
		} catch (FileNotFoundException e) {
			Logger.log(4,e.toString(),e);
		}
		return null;
	}
}
